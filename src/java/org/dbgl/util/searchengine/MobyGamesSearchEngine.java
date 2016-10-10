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
import org.dbgl.model.SearchEngineImageInformation.SearchEngineImageType;
import org.dbgl.swtdesigner.SWTImageManager;


public class MobyGamesSearchEngine extends WebSearchEngine {

	private static final String HTML_MULTIPLE_RESULT_MARKER_START = "<div class=\"searchResult\">";
	private static final String HTML_GAME_TITLE_START = "<div class=\"searchTitle\">";
	private static final String HTML_GAME_END_MARKER = "<br clear=\"all\"></div>";

	private final static String MOBY_GAMES_HOST_NAME = "www.mobygames.com";
	private final static int RESULTS_PER_PAGE = 50;

	private MobyGamesSearchEngine() {}

	private static class SearchEngineHolder {
		private static WebSearchEngine instance = new MobyGamesSearchEngine();
	}

	public static WebSearchEngine getInstance() {
		return SearchEngineHolder.instance;
	}

	public String getIcon() {
		return SWTImageManager.IMG_MOBYGAMES;
	}

	public String getName() {
		return "MobyGames";
	}

	public String getSimpleName() {
		return "mobygames";
	}

	public List<WebProfile> getEntries(final String title, String[] platforms) throws IOException {
		int pageIdx = 0;
		int pages = 1;
		List<WebProfile> allEntries = new ArrayList<WebProfile>();

		while (pageIdx < pages) {
			String content = getResponseContent(
				HTTP_PROTOCOL + MOBY_GAMES_HOST_NAME + "/search/quick?sFilter=1&p=-1&sG=on&q=" + URLEncoder.encode(title.replaceAll("/", " "), "UTF-8") + "&offset=" + (pageIdx * RESULTS_PER_PAGE),
				"UTF-8");
			if (pageIdx == 0)
				pages = getPages(content);
			allEntries.addAll(extractEntries(content));
			pageIdx++;
		}

		return filterEntries(platforms, allEntries);
	}

	private static int getPages(String htmlChunk) {
		int i = htmlChunk.indexOf("\"> Games (");
		int j = htmlChunk.indexOf(")</label>", i);
		if (i == -1 || j == -1)
			return 0;
		int entries = Integer.parseInt(htmlChunk.substring(i + 10, j));
		return (entries + RESULTS_PER_PAGE - 1) / RESULTS_PER_PAGE;
	}

	private static List<WebProfile> extractEntries(String html) {
		List<WebProfile> allEntries = new ArrayList<WebProfile>();
		html = html.replaceAll("\\\\\"", "\"");
		int gameMatchEntryIndex = html.indexOf(HTML_MULTIPLE_RESULT_MARKER_START);
		if (gameMatchEntryIndex != -1)
			gameMatchEntryIndex += HTML_MULTIPLE_RESULT_MARKER_START.length();

		while (gameMatchEntryIndex != -1) {

			gameMatchEntryIndex = html.indexOf(HTML_DIV_OPEN, gameMatchEntryIndex);

			int gameTitleIdx = html.indexOf(HTML_GAME_TITLE_START, gameMatchEntryIndex);
			String gameTitleData = extractNextContent(html, gameTitleIdx, HTML_DIV_OPEN, HTML_DIV_CLOSE);
			String gameTitle = unescapeHtml(removeAllTags(gameTitleData)).substring(6);
			String url = extractNextHrefContent(html, gameTitleIdx);

			String details = extractNextContent(html, gameTitleIdx + gameTitleData.length(), HTML_DIV_OPEN, HTML_DIV_CLOSE);
			int platformIdx = details.indexOf(HTML_SPAN_OPEN);

			while (platformIdx != -1) {
				String platform = extractNextContent(details, platformIdx, HTML_SPAN_OPEN, HTML_SPAN_CLOSE);
				if (platform.indexOf(HTML_ANCHOR_OPEN) != -1) {
					platform = extractNextContent(details, platformIdx, HTML_ANCHOR_OPEN, HTML_ANCHOR_CLOSE);
					url = extractNextHrefContent(details, platformIdx);
				} else {
					int yrIdx = platform.indexOf(" (");
					if (yrIdx != -1) {
						platform = platform.substring(0, yrIdx);
					}
				}
				url = absoluteUrl(MOBY_GAMES_HOST_NAME, url);
				String year = extractNextContent(details, platformIdx, HTML_EM_OPEN, HTML_EM_CLOSE);

				WebProfile gameEntry = new WebProfile();
				gameEntry.setTitle(gameTitle);
				gameEntry.setUrl(url);
				gameEntry.setPlatform(platform);
				gameEntry.setPublisherName("");
				gameEntry.setYear(year);
				allEntries.add(gameEntry);
				platformIdx = details.indexOf(HTML_SPAN_OPEN, platformIdx + 1);
			}

			int endIdx = html.indexOf(HTML_GAME_END_MARKER, gameTitleIdx);
			gameMatchEntryIndex = html.indexOf(HTML_MULTIPLE_RESULT_MARKER_START, endIdx + HTML_GAME_END_MARKER.length());
		}
		return allEntries;
	}

	public WebProfile getEntryDetailedInformation(final WebProfile entry) throws UnknownHostException, IOException {
		WebProfile result = new WebProfile();

		result.setTitle(entry.getTitle());
		result.setYear(entry.getYear());
		result.setUrl(entry.getUrl());

		String responseEntry = getResponseContent(entry.getUrl(), "UTF-8");

		result.setDeveloperName(extractCategory(responseEntry, ">Developed by</div>"));
		result.setPublisherName(extractCategory(responseEntry, ">Published by</div>"));
		result.setGenre(extractCategory(responseEntry, ">Genre</div>"));
		result.setNotes(extractDescription(responseEntry));
		result.setRank(extractRank(responseEntry));
		result.setCoreGameCoverUrl(extractCoreGameCoverUrl(responseEntry));

		if (StringUtils.isEmpty(result.getDeveloperName()))
			result.setDeveloperName(extractCredits(responseEntry, "Credits</h2>"));

		return result;
	}

	private String extractCredits(String htmlChunk, String marker) {
		int startIndex = htmlChunk.indexOf(marker);
		if (startIndex != -1) {
			int endIndex = htmlChunk.indexOf(HTML_DIV_CLOSE + HTML_DIV_CLOSE, startIndex + marker.length());
			if (endIndex != -1) {
				String credits = htmlChunk.substring(startIndex + marker.length(), endIndex);
				startIndex = idxNextHrefContent(credits, 0);
				String result = extractNextContent(credits, startIndex, HTML_ANCHOR_OPEN, HTML_ANCHOR_CLOSE);
				if ((idxNextHrefContent(credits, startIndex + result.length()) == -1) && (!result.equalsIgnoreCase("add credits")))
					return unescapeHtml(result);
			}
		}
		return StringUtils.EMPTY;
	}

	public SearchEngineImageInformation[] getEntryImages(final WebProfile entry, int coverArtMax, int screenshotsMax, boolean forceAllRegionsCoverArt) throws IOException {
		List<SearchEngineImageInformation> result = new ArrayList<SearchEngineImageInformation>();
		if (coverArtMax > 0) {
			result.addAll(getEntryCoverArtInformation(entry, coverArtMax, forceAllRegionsCoverArt));
		}
		if (screenshotsMax > 0) {
			result.addAll(getEntryScreenshotInformation(entry, screenshotsMax));
		}
		entry.setWebImages(result.toArray(new SearchEngineImageInformation[0]));
		return entry.getWebImages();
	}

	private static String extractCoreGameCoverUrl(String htmlChunk) {
		String beginMarker = "<div id=\"coreGameCover\">";
		String endMarker = "<div class=\"links\">";
		int beginIndex = htmlChunk.indexOf(beginMarker) + beginMarker.length();
		int endIndex = htmlChunk.indexOf(endMarker, beginIndex);
		if (beginIndex == -1 || endIndex == -1)
			return null;
		String smallChunk = htmlChunk.substring(beginIndex, endIndex);
		beginIndex = idxNextHrefContent(smallChunk, 0);
		if (beginIndex == -1)
			return null;
		return extractNextHrefContent(smallChunk, beginIndex);
	}

	private static String extractCategory(final String htmlChunk, final String marker) {
		int startIndex = htmlChunk.indexOf(marker);
		if (startIndex != -1) {
			int endIndex = htmlChunk.indexOf(HTML_DIV_CLOSE, startIndex + marker.length());
			return unescapeHtml(removeAllTags(htmlChunk.substring(startIndex + marker.length(), endIndex + HTML_DIV_CLOSE.length())));
		} else {
			return "";
		}
	}

	private static String extractDescription(final String htmlChunk) {
		String marker = "<h2>Description</h2>";
		int startIndex = htmlChunk.indexOf(marker) + marker.length();
		int endIndex = htmlChunk.indexOf(HTML_DIV_OPEN, startIndex);
		return unescapeHtml(removeAllTags(htmlChunk.substring(startIndex, endIndex)));
	}

	private static Integer extractRank(final String htmlChunk) {
		String header = "scoreBoxBig";
		int startIndex = htmlChunk.indexOf(">", htmlChunk.indexOf(header) + header.length()) + 1;
		int endIndex = htmlChunk.indexOf("<", startIndex);
		try {
			return new Integer(htmlChunk.substring(startIndex, endIndex));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private List<SearchEngineImageInformation> getEntryScreenshotInformation(final WebProfile entry, final int max) throws IOException {
		List<SearchEngineImageInformation> result = new ArrayList<SearchEngineImageInformation>();
		String htmlChunk = getResponseContent(entry.getScreenshotsUrl(), "UTF-8");
		String marker = "<div class=\"thumbnail\">";
		int startIndex = htmlChunk.indexOf(marker);

		if (startIndex != -1) {
			String endMarker = "<div class=\"lifesupport-footer\">";
			int endIndex = htmlChunk.indexOf(endMarker, startIndex);

			if (endIndex != -1) {
				htmlChunk = htmlChunk.substring(startIndex, endIndex);
				startIndex = htmlChunk.indexOf("background-image:url(");
				int found = 0;

				while ((found < max) && (startIndex != -1)) {
					String imgUrl = extractNextSrcContentParentheses(htmlChunk, startIndex);
					imgUrl = absoluteUrl(MOBY_GAMES_HOST_NAME, imgUrl.replaceAll("/images/shots/s/", "/images/shots/l/"));
					String imgDescription = extractNextContent(htmlChunk, startIndex, HTML_SMALL_OPEN, HTML_SMALL_CLOSE).replace(HTML_BR_UNCLOSED, " ").replace(HTML_BR_CLOSED, " ").trim();
					result.add(new SearchEngineImageInformation(SearchEngineImageType.Screenshot, imgUrl, imgDescription));
					startIndex = htmlChunk.indexOf("background-image:url(", startIndex + 1);
					found++;
				}
			}
		}
		return result;
	}

	private List<SearchEngineImageInformation> getEntryCoverArtInformation(final WebProfile entry, final int max, final boolean forceAllRegionsCoverArt) throws IOException {
		List<SearchEngineImageInformation> result = new ArrayList<SearchEngineImageInformation>();
		if (entry.getCoreGameCoverUrl() == null)
			return result;

		int found = 0;

		String htmlChunk = getResponseContent(entry.getCoverArtUrl(), "UTF-8");

		String marker = forceAllRegionsCoverArt ? "<div class=\"thumbnail\">": entry.getCoreGameCoverUrlWithoutPathPrefix();
		int startIndex = htmlChunk.indexOf(marker);

		while (startIndex != -1) {
			String endMarker = "</div>    </div>  </div></div>";
			int endIndex = htmlChunk.indexOf(endMarker, startIndex);

			if (endIndex != -1) {
				String divPart = htmlChunk.substring(startIndex, endIndex);
				startIndex = divPart.indexOf("background-image:url(");

				while ((found < max) && (startIndex != -1)) {
					String imgUrl = extractNextSrcContentParentheses(divPart, startIndex);
					imgUrl = absoluteUrl(MOBY_GAMES_HOST_NAME, imgUrl.replaceAll("/images/covers/s/", "/images/covers/l/"));
					String imgDescription = extractNextContent(divPart, startIndex, HTML_P_OPEN, HTML_P_CLOSE).replace(HTML_BR_UNCLOSED, " ").replace(HTML_BR_CLOSED, " ").trim();
					result.add(new SearchEngineImageInformation(SearchEngineImageType.CoverArt, imgUrl, imgDescription));
					startIndex = divPart.indexOf("background-image:url(", startIndex + 1);
					found++;
				}
			}

			startIndex = forceAllRegionsCoverArt ? htmlChunk.indexOf(marker, endIndex): -1;
		}
		return result;
	}
}
