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
package org.dbgl.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;


public final class StringRelatedUtils {

	public static String[] textAreaToStringArray(final String contents, final String del) {
		return StringUtils.splitByWholeSeparator(StringUtils.strip(contents, del), del);
	}

	public static String textAreaToString(final String contents, final String del, final String eoln) {
		return StringUtils.replace(StringUtils.strip(contents, del), del, eoln);
	}

	public static String stringToTextArea(final String string, final String del, final String eoln) {
		return StringUtils.replace(string, eoln, del);
	}

	public static String[] mountToStringArray(final String paths, final boolean quotes) {
		return quotes ? StringUtils.splitByWholeSeparator(paths, "\" \""): StringUtils.split(paths, ' ');
	}

	public static String join(final int[] values) {
		return StringUtils.replaceChars(Arrays.toString(values), "[,]", "");
	}

	public static String join(final boolean[] values) {
		return StringUtils.replaceChars(Arrays.toString(values), "[,]", "");
	}

	public static int findBestMatchIndex(final String search, final String[] titles) {
		if (titles == null || titles.length == 0)
			return -1;
		String s = search.toLowerCase();
		int minDistance = Integer.MAX_VALUE;
		int result = 0;
		for (int i = 0; i < titles.length; i++) {
			String title = FilenameUtils.removeExtension(titles[i].toLowerCase());
			int distance = (i == 0) ? StringUtils.getLevenshteinDistance(s, title): StringUtils.getLevenshteinDistance(s, title, minDistance - 1);
			if (distance == 0)
				return i;
			if (distance != -1) {
				minDistance = distance;
				result = i;
			}
		}
		return result;
	}

	public static String stringArrayToString(final String[] values, final String delimiter) {
		if (values == null || values.length <= 0)
			return "";
		return StringUtils.join(values, delimiter) + delimiter;
	}

	public static Map<String, String> stringArrayToMap(final String[] list) {
		Map<String, String> result = new HashMap<String, String>();
		for (String entry: list) {
			String[] pair = entry.split("=");
			if (pair.length == 2) {
				String key = pair[0].trim();
				String value = pair[1].trim();
				if (key.length() > 0 && value.length() > 0) {
					result.put(key, value);
				}
			}
		}
		return result;
	}

	public static int[] stringToIntArray(final String input) {
		if (input.length() <= 0) {
			return new int[0];
		}
		String[] values = input.split(" ");
		int[] result = new int[values.length];
		for (int i = 0; i < values.length; i++) {
			try {
				result[i] = Integer.parseInt(values[i]);
			} catch (NumberFormatException e) {
				result[i] = -1;
			}
		}
		return result;
	}

	public static boolean[] stringToBooleanArray(final String input) {
		if (input.length() <= 0) {
			return new boolean[0];
		}
		String[] values = input.split(" ");
		boolean[] result = new boolean[values.length];
		for (int i = 0; i < values.length; i++) {
			result[i] = Boolean.parseBoolean(values[i]);
		}
		return result;
	}

	public static String onOffValue(final boolean b) {
		return b ? "on": "off";
	}

	public static String toString(Exception exception) {
		return exception.getMessage() != null ? exception.getMessage(): exception.toString();
	}
}
