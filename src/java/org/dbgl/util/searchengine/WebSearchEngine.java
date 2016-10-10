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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.dbgl.model.SearchEngineImageInformation;
import org.dbgl.model.Constants;
import org.dbgl.model.WebProfile;
import org.dbgl.util.StringRelatedUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;


public abstract class WebSearchEngine {

	protected static final String HTTP_PROTOCOL = "http://";
	protected static final String HTML_HREF_OPEN = " href=\"";
	protected static final String HTML_HREF_OPEN_SQ = " href='";
	protected static final String HTML_SRC_OPEN = " src=\"";
	protected static final String HTML_SRC_OPEN_SQ = " src='";
	protected static final String HTML_QUOTE = "\"";
	protected static final String HTML_QUOTE_SQ = "'";

	protected static final String HTML_MOBY_OPEN = "</moby ";
	protected static final String HTML_MOBY_CLOSE = "</moby>";
	protected static final String HTML_SPAN_OPEN = "<span ";
	protected static final String HTML_SPAN_CLOSE = "</span>";
	protected static final String HTML_ANCHOR_OPEN = "<a ";
	protected static final String HTML_ANCHOR_CLOSE = "</a>";
	protected static final String HTML_DIV_OPEN = "<div";
	protected static final String HTML_DIV_CLOSE = "</div>";
	protected static final String HTML_BLOCKQUOTE_OPEN = "<blockquote>";
	protected static final String HTML_BLOCKQUOTE_CLOSE = "</blockquote>";
	protected static final String HTML_I_OPEN = "<i>";
	protected static final String HTML_I_CLOSE = "</i>";
	protected static final String HTML_UL_OPEN = "<ul>";
	protected static final String HTML_UL_CLOSE = "</ul>";
	protected static final String HTML_OL_OPEN = "<ol>";
	protected static final String HTML_OL_CLOSE = "</ol>";
	protected static final String HTML_LI_OPEN = "<li>";
	protected static final String HTML_LI_CLOSE = "</li>";
	protected static final String HTML_B_OPEN = "<b>";
	protected static final String HTML_B_CLOSE = "</b>";
	protected static final String HTML_STRONG_OPEN = "<strong>";
	protected static final String HTML_STRONG_CLOSE = "</strong>";
	protected static final String HTML_P_OPEN = "<p>";
	protected static final String HTML_PU_OPEN = "<p ";
	protected static final String HTML_P_CLOSE = "</p>";
	protected static final String HTML_EM_OPEN = "<em>";
	protected static final String HTML_EM_CLOSE = "</em>";
	protected static final String HTML_BR_UNCLOSED = "<br>";
	protected static final String HTML_BR_CLOSED = "<br/>";
	protected static final String HTML_BR_CLOSED_ALT = "<br />";
	protected static final String HTML_TD_OPEN = "<td>";
	protected static final String HTML_TD_CLOSE = "</td>";
	protected static final String HTML_TITLE_OPEN = "<title>";
	protected static final String HTML_TITLE_CLOSE = "</title>";
	protected static final String HTML_SMALL_OPEN = "<small>";
	protected static final String HTML_SMALL_CLOSE = "</small>";

	public abstract String getIcon();

	public abstract String getName();

	public abstract String getSimpleName();

	public abstract WebProfile getEntryDetailedInformation(final WebProfile entry) throws UnknownHostException, IOException;

	public abstract SearchEngineImageInformation[] getEntryImages(final WebProfile entry, int coverArtMax, int screenshotsMax, boolean forceAllRegionsCoverArt) throws IOException;

	public abstract List<WebProfile> getEntries(final String title, String[] platforms) throws IOException;

	public static int getEntryFirstExactMatchIndex(final String title, final List<WebProfile> profs) {
		for (int i = 0; i < profs.size(); i++) {
			if (title.equalsIgnoreCase(profs.get(i).getTitle()))
				return i;
		}
		return -1;
	}

	public static int getEntryBestMatchIndex(final String search, final List<WebProfile> profs) {
		String[] titles = new String[profs.size()];
		for (int i = 0; i < profs.size(); i++)
			titles[i] = profs.get(i).getTitle();
		return StringRelatedUtils.findBestMatchIndex(search, titles);
	}

	public static InputStream getInputStream(final String url) throws IOException {
		try {
			URL urlConnection = new URL(url);
			HttpURLConnection conn = (HttpURLConnection)urlConnection.openConnection();
			conn.setConnectTimeout(10000); // 10 seconds
			conn.setReadTimeout(20000); // 20 seconds
			conn.setRequestProperty("User-Agent", Constants.PROGRAM_NAME_FULL);
			return conn.getInputStream();
		} catch (MalformedURLException e) {
			throw new IOException(e);
		}
	}

	protected static String getResponseContent(final String url, final String charsetName) throws IOException {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(getInputStream(url), charsetName));
			StringBuffer result = new StringBuffer(8192);
			String str;
			while ((str = in.readLine()) != null) {
				result.append(str);
			}
			in.close();
			return result.toString();
		} catch (MalformedURLException e) {
			return null;
		}
	}

	protected static String absoluteUrl(final String hostName, final String url) {
		return url.startsWith(HTTP_PROTOCOL) ? url: HTTP_PROTOCOL + hostName + (url.charAt(0) == '/' ? "": '/') + url;
	}

	protected static String extractNextContent(final String htmlChunk, final int startIndex, final String openTag, final String closeTag) {
		int divStartIndex = htmlChunk.indexOf(openTag, startIndex);
		divStartIndex = htmlChunk.indexOf(">", divStartIndex) + 1;
		int divEndIndex = htmlChunk.indexOf(closeTag, divStartIndex);
		return htmlChunk.substring(divStartIndex, divEndIndex);
	}

	protected static String extractNextHrefContent(final String htmlChunk, final int startIndex) {
		int aStartIndex = htmlChunk.indexOf(HTML_HREF_OPEN, startIndex) + HTML_HREF_OPEN.length();
		int aEndIndex = htmlChunk.indexOf(HTML_QUOTE, aStartIndex);
		return htmlChunk.substring(aStartIndex, aEndIndex);
	}

	protected static String extractNextHrefContentSingleQuotes(final String htmlChunk, final int startIndex) {
		int aStartIndex = htmlChunk.indexOf(HTML_HREF_OPEN_SQ, startIndex) + HTML_HREF_OPEN_SQ.length();
		int aEndIndex = htmlChunk.indexOf(HTML_QUOTE_SQ, aStartIndex);
		return htmlChunk.substring(aStartIndex, aEndIndex);
	}

	protected static int idxNextHrefContent(final String htmlChunk, final int startIndex) {
		int idx = htmlChunk.indexOf(HTML_ANCHOR_OPEN, startIndex);
		if ((idx != -1) && (htmlChunk.indexOf(HTML_ANCHOR_CLOSE, idx + HTML_ANCHOR_OPEN.length()) != -1))
			return idx;
		return -1;
	}

	protected static String extractNextSrcContent(final String htmlChunk, final int startIndex) {
		int aStartIndex = htmlChunk.indexOf(HTML_SRC_OPEN, startIndex) + HTML_SRC_OPEN.length();
		int aEndIndex = htmlChunk.indexOf(HTML_QUOTE, aStartIndex);
		return htmlChunk.substring(aStartIndex, aEndIndex);
	}

	protected static String extractNextDoubleQuotedContent(final String htmlChunk, final int startIndex) {
		int aStartIndex = htmlChunk.indexOf("\"", startIndex) + 1;
		int aEndIndex = htmlChunk.indexOf("\"", aStartIndex);
		return htmlChunk.substring(aStartIndex, aEndIndex);
	}

	protected static String extractNextSrcContentSingleQuotes(final String htmlChunk, final int startIndex) {
		int aStartIndex = htmlChunk.indexOf(HTML_SRC_OPEN_SQ, startIndex) + HTML_SRC_OPEN_SQ.length();
		int aEndIndex = htmlChunk.indexOf(HTML_QUOTE_SQ, aStartIndex);
		return htmlChunk.substring(aStartIndex, aEndIndex);
	}

	protected static String extractNextSrcContentParentheses(final String htmlChunk, final int startIndex) {
		int aStartIndex = htmlChunk.indexOf('(', startIndex) + 1;
		int aEndIndex = htmlChunk.indexOf(')', aStartIndex);
		return htmlChunk.substring(aStartIndex, aEndIndex);
	}

	protected static String removeAllTags(final String htmlChunk) {
		String result = removeTag(HTML_DIV_OPEN, HTML_DIV_CLOSE, htmlChunk);
		result = removeTag(HTML_ANCHOR_OPEN, HTML_ANCHOR_CLOSE, result);
		result = removeTag(HTML_MOBY_OPEN, HTML_MOBY_CLOSE, result);
		result = replaceTag(HTML_I_OPEN, HTML_I_CLOSE, "", "", result);
		result = replaceTag(HTML_B_OPEN, HTML_B_CLOSE, "", "", result);
		result = replaceTag(HTML_STRONG_OPEN, HTML_STRONG_CLOSE, "", "", result);
		result = replaceTag(HTML_LI_OPEN, HTML_LI_CLOSE, "", "\n", result);
		result = replaceTag(HTML_EM_OPEN, HTML_EM_CLOSE, "", "", result);
		result = replaceTag(HTML_UL_OPEN, HTML_UL_CLOSE, "\n\n", "\n", result);
		result = replaceTag(HTML_OL_OPEN, HTML_OL_CLOSE, "\n\n", "\n", result);
		result = replaceTag(HTML_BLOCKQUOTE_OPEN, HTML_BLOCKQUOTE_CLOSE, "\n\n", "\n", result);
		result = result.replaceAll(HTML_P_CLOSE + "\\s*" + HTML_P_OPEN, "\n\n");
		result = replaceTag(HTML_P_OPEN, HTML_P_CLOSE, "\n", "", result);
		return result;
	}

	protected static String replaceTag(final String openTag, final String closeTag, final String r1, final String r2, final String htmlChunk) {
		return replaceTag(closeTag, r2, replaceTag(openTag, r1, htmlChunk));
	}

	protected static String replaceTag(final String openTag, final String r1, final String htmlChunk) {
		return htmlChunk.replace(openTag, r1).replace(openTag.toUpperCase(), r1);
	}

	protected static String removeTag(final String openTag, final String closeTag, final String htmlChunk) {
		StringBuffer result = new StringBuffer(htmlChunk);
		int openingIndex = StringUtils.indexOfIgnoreCase(result, openTag);
		while (openingIndex != -1) {
			result.delete(openingIndex, result.indexOf(">", openingIndex + openTag.length()) + 1);
			int closingIndex = StringUtils.indexOfIgnoreCase(result, closeTag);
			result.delete(closingIndex, closingIndex + closeTag.length());
			openingIndex = StringUtils.indexOfIgnoreCase(result, openTag);
		}
		return result.toString();
	}

	protected static String unescapeHtml(final String htmlChunk) {
		String result = replaceTag(HTML_BR_UNCLOSED, "\n", htmlChunk);
		result = replaceTag(HTML_BR_CLOSED, "\n", result);
		result = replaceTag(HTML_BR_CLOSED_ALT, "\n", result);
		result = replaceTag("&nbsp;", " ", result);
		result = replaceTag("&apos;", "'", result);
		return StringEscapeUtils.unescapeHtml4(StringUtils.strip(result));
	}

	private static boolean isAllowed(final WebProfile prof, final String[] platforms) {
		boolean allowed = (platforms.length == 0);
		for (String p: platforms) {
			if (prof.getPlatform().equalsIgnoreCase(p))
				return true;
		}
		return allowed;
	}

	protected static List<WebProfile> filterEntries(String[] platforms, Collection<WebProfile> allEntries) {
		List<WebProfile> entries = new ArrayList<WebProfile>();
		for (WebProfile prof: allEntries)
			if (isAllowed(prof, platforms))
				entries.add(prof);
		if (entries.isEmpty())
			entries.addAll(allEntries);
		Collections.sort(entries);
		return entries;
	}
}