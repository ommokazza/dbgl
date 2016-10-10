/*
 *  Copyright (C) 2006-2015  Ronald Blankendaal
 *  
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.dbgl.util.searchengine;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.lang3.StringUtils;
import org.dbgl.model.SearchEngineImageInformation;
import org.dbgl.model.SearchEngineImageInformation.SearchEngineImageType;
import org.dbgl.model.WebProfile;
import org.dbgl.swtdesigner.SWTImageManager;
import org.dbgl.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class TheGamesDBSearchEngine extends WebSearchEngine {

	private final static String THE_GAMES_DB_HOST_NAME = "thegamesdb.net";

	private TheGamesDBSearchEngine() {}

	private static class SearchEngineHolder {
		private static WebSearchEngine instance = new TheGamesDBSearchEngine();
	}

	public static WebSearchEngine getInstance() {
		return SearchEngineHolder.instance;
	}

	public String getIcon() {
		return SWTImageManager.IMG_THEGAMESDB;
	}

	public String getName() {
		return "TheGamesDB.net";
	}

	public String getSimpleName() {
		return "thegamesdb";
	}

	public List<WebProfile> getEntries(final String title, String[] platforms) throws IOException {
		try {
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(getInputStream(HTTP_PROTOCOL + THE_GAMES_DB_HOST_NAME + "/api/GetGamesList.php?name="
					+ URLEncoder.encode(title.replaceAll("/", " "), "UTF-8") + ((platforms.length == 1) ? "&platform=" + platforms[0]: "")));

			NodeList gameNodes = (NodeList)XPathFactory.newInstance().newXPath().evaluate("/Data/Game", doc, XPathConstants.NODESET);

			List<WebProfile> allEntries = new ArrayList<WebProfile>();
			for (int i = 0; i < gameNodes.getLength(); i++) {
				Element gameNode = (Element)gameNodes.item(i);
				WebProfile gameEntry = new WebProfile();
				gameEntry.setTitle(XmlUtils.getTextValue(gameNode, "GameTitle"));
				gameEntry.setUrl(absoluteUrl(THE_GAMES_DB_HOST_NAME, "/api/GetGame.php?id=" + XmlUtils.getTextValue(gameNode, "id")));
				gameEntry.setPlatform(XmlUtils.getTextValue(gameNode, "Platform"));
				gameEntry.setPublisherName("");
				String date = XmlUtils.getTextValue(gameNode, "ReleaseDate");
				if (date != null && date.length() == 10)
					gameEntry.setYear(date.substring(date.length() - 4));
				else
					gameEntry.setYear("");
				allEntries.add(gameEntry);
			}
			return allEntries;
		} catch (ParserConfigurationException | SAXException | XPathExpressionException e) {
			throw new IOException(e);
		}
	}

	public WebProfile getEntryDetailedInformation(final WebProfile entry) throws UnknownHostException, IOException {
		WebProfile result = new WebProfile();

		result.setTitle(entry.getTitle());
		result.setYear(entry.getYear());
		result.setUrl(entry.getUrl());
		result.setPlatform(entry.getPlatform());

		try {
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(getInputStream(entry.getUrl()));

			Element gameNode = (Element)doc.getFirstChild();

			result.setDeveloperName(StringUtils.defaultString(XmlUtils.getTextValue(gameNode, "Developer")));
			result.setPublisherName(StringUtils.defaultString(XmlUtils.getTextValue(gameNode, "Publisher")));
			result.setNotes(StringUtils.defaultString(XmlUtils.getTextValue(gameNode, "Overview")));
			String rating = XmlUtils.getTextValue(gameNode, "Rating");
			if (rating != null)
				result.setRank((int)((Double.parseDouble(rating) * 10.0) + 0.5));
			else
				result.setRank(0);
			result.setCoreGameCoverUrl(XmlUtils.getTextValue(gameNode, "baseImgUrl"));
			result.setXmlElementWithAllImages(XmlUtils.getNode(gameNode, "Images"));

			StringBuffer genre = new StringBuffer();
			Element el = XmlUtils.getNode(gameNode, "Genres");
			if (el != null) {
				NodeList genreNodes = el.getChildNodes();
				for (int i = 0; i < genreNodes.getLength(); i++) {
					if (i > 0)
						genre.append(", ");
					genre.append(genreNodes.item(i).getFirstChild().getNodeValue());
				}
			}
			result.setGenre(genre.toString());

			result.setUrl(absoluteUrl(THE_GAMES_DB_HOST_NAME, "/game/" + XmlUtils.getTextValue(gameNode, "id")));

		} catch (ParserConfigurationException | SAXException e) {
			throw new IOException(e);
		}

		return result;
	}

	public SearchEngineImageInformation[] getEntryImages(final WebProfile entry, int coverArtMax, int screenshotsMax, boolean forceAllRegionsCoverArt) throws IOException {
		List<SearchEngineImageInformation> result = new ArrayList<SearchEngineImageInformation>();
		if (coverArtMax > 0) {
			try {
				result.addAll(getEntryCoverArtInformation(entry, coverArtMax, forceAllRegionsCoverArt));
			} catch (XPathExpressionException e) {
				throw new IOException(e);
			}
		}
		if (screenshotsMax > 0) {
			try {
				result.addAll(getEntryScreenshotInformation(entry, screenshotsMax));
			} catch (XPathExpressionException e) {
				throw new IOException(e);
			}
		}
		entry.setWebImages(result.toArray(new SearchEngineImageInformation[0]));
		return entry.getWebImages();
	}

	private List<SearchEngineImageInformation> getEntryScreenshotInformation(final WebProfile entry, final int max) throws IOException, XPathExpressionException {
		List<SearchEngineImageInformation> result = new ArrayList<SearchEngineImageInformation>();
		NodeList screenshotNodes = (NodeList)XPathFactory.newInstance().newXPath().evaluate("screenshot", entry.getXmlElementWithAllImages(), XPathConstants.NODESET);
		for (int i = 0; i < Math.min(screenshotNodes.getLength(), max); i++) {
			Element screenshotNode = (Element)screenshotNodes.item(i);
			String url = entry.getCoreGameCoverUrl() + XmlUtils.getTextValue(screenshotNode, "original");
			result.add(new SearchEngineImageInformation(SearchEngineImageType.Screenshot, url, StringUtils.EMPTY));
		}
		return result;
	}

	private List<SearchEngineImageInformation> getEntryCoverArtInformation(final WebProfile entry, final int max, final boolean forceAllRegionsCoverArt) throws IOException, XPathExpressionException {
		List<SearchEngineImageInformation> result = new ArrayList<SearchEngineImageInformation>();
		NodeList screenshotNodes = (NodeList)XPathFactory.newInstance().newXPath().evaluate("boxart", entry.getXmlElementWithAllImages(), XPathConstants.NODESET);
		for (int i = 0; i < Math.min(screenshotNodes.getLength(), max); i++) {
			Element screenshotNode = (Element)screenshotNodes.item(i);
			String url = entry.getCoreGameCoverUrl() + screenshotNode.getFirstChild().getNodeValue();
			String descr = screenshotNode.getAttribute("side");
			result.add(new SearchEngineImageInformation(SearchEngineImageType.CoverArt, url, descr));
		}
		return result;
	}
}
