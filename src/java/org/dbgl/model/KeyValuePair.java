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
package org.dbgl.model;

import java.util.List;


public class KeyValuePair {

	private final int id;
	private final String value;

	public KeyValuePair(final int id, final String value) {
		this.id = id;
		this.value = value;
	}

	public int getId() {
		return id;
	}

	public String getValue() {
		return value;
	}

	public static int findIdByValue(final List<KeyValuePair> list, final String value) {
		for (KeyValuePair element: list) {
			if (element.value.equals(value)) {
				return element.id;
			}
		}
		return -1; // value not found
	}

	public static <T extends KeyValuePair> int findIndexById(final List<T> list, final int id) {
		int result = 0;
		for (T element: list) {
			if (element.getId() == id) {
				return result;
			}
			result++;
		}
		return -1; // id not found
	}

	public static <T extends KeyValuePair> T findById(final List<T> list, final int id) {
		for (T element: list) {
			if (element.getId() == id) {
				return element;
			}
		}
		return null; // id not found
	}
}
