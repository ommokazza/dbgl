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
package org.dbgl.model.conf;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.dbgl.util.PlatformUtils;


final class Section {

	final Map<String, String> items;

	Section() {
		items = new LinkedHashMap<String, String>();
	}

	Section(final Section sec) {
		this();
		for (String key: sec.items.keySet()) {
			items.put(key, sec.items.get(key));
		}
	}

	String toString(final boolean ordered) {
		StringBuffer result = new StringBuffer();
		Set<String> keys = ordered ? new TreeMap<String, String>(items).keySet(): items.keySet();
		for (String key: keys) {
			result.append(key).append('=');
			result.append(items.get(key)).append(PlatformUtils.EOLN);
		}
		return result.toString();
	}
}
