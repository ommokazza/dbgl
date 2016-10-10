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
import org.dbgl.model.SearchEngineImageInformation;
import org.dbgl.model.WebProfile;
import org.dbgl.model.SearchEngineImageInformation.SearchEngineImageType;
import org.dbgl.swtdesigner.SWTImageManager;
import org.eclipse.swt.graphics.ImageData;
import org.apache.commons.lang3.StringUtils;


public class PouetSearchEngine extends WebSearchEngine {

	private static final String HTML_MULTIPLE_RESULT_MARKER_START = "<span class='typeiconlist'>";
	private static final String HTML_NEXT_RESULT_MARKER_START = HTML_MULTIPLE_RESULT_MARKER_START;
	private static final String HTML_GAME_END_MARKER = "</div></td></tr>";
	private static final String HTML_PLATFORM_MARKER = "<span class='platformi ";

	private final static String HOST_NAME = "www.pouet.net";

	private PouetSearchEngine() {}

	private static class SearchEngineHolder {
		private static WebSearchEngine instance = new PouetSearchEngine();
	}

	public static WebSearchEngine getInstance() {
		return SearchEngineHolder.instance;
	}

	public String getIcon() {
		return SWTImageManager.IMG_POUET;
	}

	public String getName() {
		return "Pou\u00EBt.net";
	}

	public String getSimpleName() {
		return "pouet";
	}

	public List<WebProfile> getEntries(final String title, String[] platforms) throws IOException {
		int pageIdx = 0;
		int pages = 1;
		List<WebProfile> allEntries = new ArrayList<WebProfile>();

		while (pageIdx < pages) {
			String content = getResponseContent(HTTP_PROTOCOL + HOST_NAME + "/search.php?type=prod&what=" + URLEncoder.encode(title.replaceAll("/", " "), "UTF-8") + "&page=" + (pageIdx + 1), "UTF-8");
			if (pageIdx == 0)
				pages = getPages(content);
			if (pages > 0) {
				allEntries.addAll(extractEntries(content));
			} else if (content.indexOf("<div id='prodpagecontainer'>") != -1) {
				allEntries.addAll(extractSingleEntry(content));
			}
			pageIdx++;
		}

		return filterEntries(platforms, allEntries);
	}

	private static int getPages(String htmlChunk) {
		int i = htmlChunk.indexOf("<select name='page'>");
		if (i != -1) {
			int j = htmlChunk.indexOf("</select>", i);
			String pages = htmlChunk.substring(i, j);
			int amountOfPages = 0;
			i = 0;
			while ((i = pages.indexOf("<option", i + 7)) != -1) {
				amountOfPages++;
			}
			return amountOfPages;
		} else {
			return 0;
		}
	}

	private static List<WebProfile> extractEntries(String html) {
		List<WebProfile> allEntries = new ArrayList<WebProfile>();
		html = html.replaceAll("\\\\\"", "\"");
		int gameMatchEntryIndex = html.indexOf(HTML_MULTIPLE_RESULT_MARKER_START);
		if (gameMatchEntryIndex != -1)
			gameMatchEntryIndex += HTML_MULTIPLE_RESULT_MARKER_START.length();

		while (gameMatchEntryIndex != -1) {

			String category = StringUtils.capitalize(extractNextContent(html, gameMatchEntryIndex, HTML_SPAN_OPEN, HTML_SPAN_CLOSE));

			int startPlatformIdx = html.indexOf("<span class='platformiconlist'>", gameMatchEntryIndex);
			int gameTitleIdx = html.indexOf("<span class='prod'>", gameMatchEntryIndex);

			String gameTitleData = extractNextContent(html, gameTitleIdx, HTML_ANCHOR_OPEN, HTML_ANCHOR_CLOSE);
			String gameTitle = StringUtils.capitalize(unescapeHtml(removeAllTags(gameTitleData)));
			String url = extractNextHrefContentSingleQuotes(html, gameTitleIdx);
			url = absoluteUrl(HOST_NAME, url);

			gameMatchEntryIndex = html.indexOf(HTML_TD_OPEN, gameMatchEntryIndex);

			String developerName = extractNextContent(html, gameMatchEntryIndex + HTML_TD_OPEN.length(), HTML_ANCHOR_OPEN, HTML_ANCHOR_CLOSE);
			developerName = StringUtils.capitalize(developerName);

			String year = StringUtils.right(extractNextContent(html, gameMatchEntryIndex, "<td class='date'>", HTML_TD_CLOSE), 4);

			int rank = 0;
			try {
				String score = extractNextContent(html, gameMatchEntryIndex, "<td class='votes'>", "&nbsp;<img");
				rank = (int)((Double.parseDouble(score) + 1.0) * 50.0);
			} catch (Exception e) {}

			String details = html.substring(startPlatformIdx, gameTitleIdx);
			int platformIdx = details.indexOf(HTML_PLATFORM_MARKER);

			while (platformIdx != -1) {
				String platform = extractNextContent(details, platformIdx, HTML_SPAN_OPEN, HTML_SPAN_CLOSE);

				WebProfile gameEntry = new WebProfile();
				gameEntry.setTitle(gameTitle);
				gameEntry.setUrl(url);
				gameEntry.setPlatform(platform);
				gameEntry.setPublisherName("");
				gameEntry.setDeveloperName(developerName);
				gameEntry.setYear(year);
				gameEntry.setGenre(category);
				gameEntry.setRank(rank);
				gameEntry.setNotes("");
				allEntries.add(gameEntry);

				platformIdx = details.indexOf(HTML_PLATFORM_MARKER, platformIdx + 1);
			}

			int endIdx = html.indexOf(HTML_GAME_END_MARKER, gameTitleIdx);
			gameMatchEntryIndex = html.indexOf(HTML_NEXT_RESULT_MARKER_START, endIdx + HTML_GAME_END_MARKER.length());
			if (gameMatchEntryIndex != -1)
				gameMatchEntryIndex += HTML_NEXT_RESULT_MARKER_START.length();
		}
		return allEntries;
	}

	private List<WebProfile> extractSingleEntry(String html) {
		List<WebProfile> allEntries = new ArrayList<WebProfile>();

		int canonicalIndex = html.indexOf("<link rel=\"canonical\"");
		String url = extractNextHrefContent(html, canonicalIndex);

		int titleIdx = html.indexOf("<span id='title'>", canonicalIndex);
		String gameTitleData = extractNextContent(html, titleIdx, "<big>", "</big>");
		String title = StringUtils.capitalize(unescapeHtml(removeAllTags(gameTitleData)));

		String developerName = (html.indexOf("</big> by ", titleIdx) != -1) ? StringUtils.capitalize(extractNextContent(html, titleIdx, HTML_ANCHOR_OPEN, HTML_ANCHOR_CLOSE)): "";

		int platformIdx = html.indexOf("<td>platform :</td>", titleIdx);

		int categoryIdx = html.indexOf("<td>type :</td>", platformIdx);
		String category = StringUtils.capitalize(extractNextContent(html, categoryIdx, HTML_SPAN_OPEN, HTML_SPAN_CLOSE));

		int yearIdx = html.indexOf("<td>release date :</td>", categoryIdx);
		String dateValue = extractNextContent(html, yearIdx + 24, HTML_TD_OPEN, HTML_TD_CLOSE);
		String year = !dateValue.contains("n/a") ? StringUtils.right(dateValue, 4): "";

		int scoreEndIdx = html.indexOf("<div id='alltimerank'>", yearIdx);
		int scoreStartIdx = html.lastIndexOf("&nbsp;", scoreEndIdx);
		int rank = 0;
		try {
			String score = html.substring(scoreStartIdx + 6, scoreEndIdx);
			int l = score.indexOf("</li>");
			if (l != -1)
				score = score.substring(0, l);
			rank = (int)((Double.parseDouble(score) + 1.0) * 50.0);
		} catch (Exception e) {}

		String details = html.substring(platformIdx, categoryIdx);
		platformIdx = details.indexOf("<span class='platform ");

		while (platformIdx != -1) {
			String platform = extractNextContent(details, platformIdx, HTML_SPAN_OPEN, HTML_SPAN_CLOSE);

			WebProfile gameEntry = new WebProfile();
			gameEntry.setTitle(title);
			gameEntry.setUrl(url);
			gameEntry.setPlatform(platform);
			gameEntry.setPublisherName("");
			gameEntry.setDeveloperName(developerName);
			gameEntry.setYear(year);
			gameEntry.setGenre(category);
			gameEntry.setRank(rank);
			gameEntry.setNotes("");
			allEntries.add(gameEntry);

			platformIdx = details.indexOf("<span class='platform ", platformIdx + 1);
		}

		return allEntries;
	}

	public WebProfile getEntryDetailedInformation(final WebProfile entry) throws UnknownHostException, IOException {
		return entry;
	}

	public SearchEngineImageInformation[] getEntryImages(WebProfile entry, int coverArtMax, int screenshotsMax, boolean forceAllRegionsCoverArt) throws IOException {
		String html = getResponseContent(entry.getUrl(), "ISO-8859-1");
		int imgMatchEntryIndex = html.indexOf("<td rowspan='3' id='screenshot'><img src=");
		if (imgMatchEntryIndex == -1) {
			entry.setWebImages(new SearchEngineImageInformation[0]);
		} else {
			String imgUrl = absoluteUrl(HOST_NAME, extractNextSrcContentSingleQuotes(html, imgMatchEntryIndex));
			String imgDescription = getSimpleName();
			if (imgUrl.toLowerCase().endsWith(".gif")) {
				ImageData[] images = SWTImageManager.getAnimatedImageData(imgUrl);
				int nrOfImages = Math.min(images.length, Math.max(screenshotsMax, 0));
				SearchEngineImageInformation[] result = new SearchEngineImageInformation[nrOfImages];
				for (int i = 0; i < nrOfImages; i++) {
					result[i] = new SearchEngineImageInformation(SearchEngineImageType.Screenshot, imgUrl, imgDescription);
					result[i].setData(images[i]);
				}
				entry.setWebImages(result);
			} else {
				entry.setWebImages(new SearchEngineImageInformation[] {new SearchEngineImageInformation(SearchEngineImageType.Screenshot, imgUrl, imgDescription)});
			}
		}
		return entry.getWebImages();
	}
}
