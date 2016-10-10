/*
 *  Copyright (C) 2006-2015  Ronald Blankendaal
 *  
 *  Many thanks to Manuel J. Gallego for his work on MobyGames querying
 *  for TincoreADB. This file is based on his code.
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
import org.apache.commons.lang3.StringUtils;
import org.dbgl.model.SearchEngineImageInformation;
import org.dbgl.model.WebProfile;
import org.dbgl.swtdesigner.SWTImageManager;


public class HotudSearchEngine extends WebSearchEngine {

	private static final String HTML_MULTIPLE_RESULT_MARKER_START = "<table class=\"jrResults\" width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\">";
	private static final String HTML_GAME_TITLE_START = "<div class=\"jrContentTitle\">";
	private static final String HTML_GAME_END_MARKER = "<td class=\"jrTableColumnLast\">";

	private final static String HOTUD_ORG_HOST_NAME = "www.hotud.org";
	private final static int RESULTS_PER_PAGE = 50;

	private HotudSearchEngine() {}

	private static class SearchEngineHolder {
		private static WebSearchEngine instance = new HotudSearchEngine();
	}

	public static WebSearchEngine getInstance() {
		return SearchEngineHolder.instance;
	}

	public String getIcon() {
		return SWTImageManager.IMG_HOTUD;
	}

	public String getName() {
		return "Home o/t Underdogs";
	}

	public String getSimpleName() {
		return "hotud";
	}

	public List<WebProfile> getEntries(final String title, String[] platforms) throws IOException {
		int pageIdx = 0;
		int pages = 1;
		List<WebProfile> allEntries = new ArrayList<WebProfile>();
		String platformFilter = platforms.length == 1 ? "/tag/reqs/" + platforms[0]: "";

		while (pageIdx < pages) {
			String content = getResponseContent(HTTP_PROTOCOL + HOTUD_ORG_HOST_NAME + "/component/jreviews/search-results" + platformFilter + "?criteria=1&query=all&scope=title&keywords="
					+ URLEncoder.encode(title.replaceAll("/", " "), "UTF-8") + "&order=alpha&page=" + (pageIdx + 1) + "&limit=" + RESULTS_PER_PAGE,
				"UTF-8");
			if (pageIdx == 0)
				pages = getPages(content);
			if (pages > 0) {
				allEntries.addAll(extractEntries(content));
			} else if (content.indexOf("<h1 class=\"contentheading\">") != -1) {
				allEntries.add(extractSingleEntry(content));
			}
			pageIdx++;
		}

		if (platforms.length == 1) {
			for (WebProfile p: allEntries) {
				p.setPlatform(platforms[0].toUpperCase());
			}
		}

		return allEntries;
	}

	private static int getPages(String htmlChunk) {
		int i = htmlChunk.indexOf("<div class=\"jrCol4 jrPagenavResults\">");
		int j = htmlChunk.indexOf(" results", i);
		if (i == -1 || j == -1)
			return 0;
		return (int)Math.ceil(Integer.parseInt(htmlChunk.substring(i + 37, j).trim()) / (double)RESULTS_PER_PAGE);
	}

	private static List<WebProfile> extractEntries(String html) {
		List<WebProfile> allEntries = new ArrayList<WebProfile>();
		html = html.replaceAll("\\\\\"", "\"");
		int gameMatchEntryIndex = html.indexOf(HTML_MULTIPLE_RESULT_MARKER_START);
		if (gameMatchEntryIndex != -1)
			gameMatchEntryIndex += HTML_MULTIPLE_RESULT_MARKER_START.length();

		while (gameMatchEntryIndex != -1) {

			gameMatchEntryIndex = html.indexOf("<tr class=\"row" + (allEntries.size() % 2 + 1) + "\">", gameMatchEntryIndex);

			int gameTitleIdx = html.indexOf(HTML_GAME_TITLE_START, gameMatchEntryIndex);
			String gameTitle = extractNextContent(html, gameTitleIdx, HTML_ANCHOR_OPEN, HTML_ANCHOR_CLOSE);
			String url = absoluteUrl(HOTUD_ORG_HOST_NAME, extractNextHrefContent(html, gameTitleIdx));

			int detailsIdx = html.indexOf("<div class=\"jrCustomFields\">", gameTitleIdx + HTML_GAME_TITLE_START.length());

			int yearIdx = html.indexOf("<div class=\"jrFieldLabel\">Year released</div><div class=\"jrFieldValue \">", detailsIdx);
			String year = extractNextContent(html, yearIdx, HTML_ANCHOR_OPEN, HTML_ANCHOR_CLOSE);

			WebProfile gameEntry = new WebProfile();
			gameEntry.setTitle(gameTitle);
			gameEntry.setYear(year);
			gameEntry.setPlatform(StringUtils.EMPTY);
			gameEntry.setUrl(url);
			allEntries.add(gameEntry);

			int endIdx = html.indexOf(HTML_GAME_END_MARKER, gameTitleIdx);
			gameMatchEntryIndex = html.indexOf("<tr class=\"row" + (allEntries.size() % 2 + 1) + "\">", endIdx + HTML_GAME_END_MARKER.length());
		}
		return allEntries;
	}

	private WebProfile extractSingleEntry(String html) {
		int baseIdx = html.indexOf("<base href=");
		String base = extractNextHrefContent(html, baseIdx);

		int titleIdx = html.indexOf("<span itemprop=\"name\">");
		String title = extractNextContent(html, titleIdx, HTML_SPAN_OPEN, HTML_SPAN_CLOSE);

		int detailsIdx = html.indexOf("<div class=\"jrListingInfoContainer\">", titleIdx);

		int yearIdx = html.indexOf("<div class=\"jrFieldLabel\">Year released</div><div class=\"jrFieldValue \">", detailsIdx);
		String year = extractNextContent(html, yearIdx, HTML_ANCHOR_OPEN, HTML_ANCHOR_CLOSE);

		WebProfile gameEntry = new WebProfile();
		gameEntry.setTitle(title);
		gameEntry.setYear(year);
		gameEntry.setPlatform(StringUtils.EMPTY);
		gameEntry.setUrl(base);

		return setAdditionalFields(gameEntry, html);
	}

	public WebProfile getEntryDetailedInformation(final WebProfile entry) throws UnknownHostException, IOException {
		if (entry.getNotes() != null)
			return entry;
		return setAdditionalFields(entry, getResponseContent(entry.getUrl(), "UTF-8"));
	}

	private WebProfile setAdditionalFields(WebProfile result, String html) {
		int ratingIdx = html.indexOf("<span class=\"jrRatingValue\"");
		ratingIdx = html.indexOf(">", ratingIdx);
		int ratingEndIdx = html.indexOf(" ", ratingIdx + 1);
		String ratingData = html.substring(ratingIdx + 1, ratingEndIdx).replace(".", "");
		int rating = 0;
		try {
			rating = new Integer(ratingData);
		} catch (NumberFormatException e) {
			// do nothing
		}

		int detailsIdx = html.indexOf("<div class=\"jrFieldGroup game-information\">", ratingIdx);

		int devIdx = html.indexOf("<div class=\"jrFieldLabel\">Developer</div><div class=\"jrFieldValue \">", detailsIdx);
		String developer = unescapeHtml(removeAllTags(extractNextContent(html, devIdx, HTML_ANCHOR_OPEN, HTML_ANCHOR_CLOSE)));

		int pubIdx = html.indexOf("<div class=\"jrFieldLabel\">Publisher</div><div class=\"jrFieldValue \">", detailsIdx);
		String publisher = unescapeHtml(removeAllTags(extractNextContent(html, pubIdx, HTML_ANCHOR_OPEN, HTML_ANCHOR_CLOSE)));

		int genreIdx = html.indexOf("<div class=\"jrFieldLabel\">Genre</div><div class=\"jrFieldValue \">", detailsIdx);
		String genre = stripStars(extractNextContent(html, genreIdx, HTML_ANCHOR_OPEN, HTML_ANCHOR_CLOSE));

		int platformIdx = html.indexOf("<div class=\"jrFieldLabel\">Platform</div><div class=\"jrFieldValue \">", detailsIdx);
		String platform = extractNextContent(html, platformIdx, HTML_ANCHOR_OPEN, HTML_ANCHOR_CLOSE);

		int notesIdx = html.indexOf("<div class=\"jrListingFulltext\" itemprop=\"description\">", detailsIdx);
		String notes = unescapeHtml(removeAllTags(extractNextContent(html, notesIdx, HTML_DIV_OPEN, HTML_DIV_CLOSE)));

		result.setDeveloperName(developer);
		result.setPublisherName(publisher);
		result.setGenre(genre);
		result.setPlatform(platform);
		result.setNotes(notes);
		result.setRank(rating);

		return result;
	}

	private String stripStars(String s) {
		if (s.startsWith("*") && s.endsWith("*"))
			return s.substring(1, s.length() - 1);
		return s;
	}

	public SearchEngineImageInformation[] getEntryImages(final WebProfile entry, int coverArtMax, int screenshotsMax, boolean forceAllRegionsCoverArt) throws IOException {
		entry.setWebImages(new SearchEngineImageInformation[0]);
		return entry.getWebImages();
	}
}
