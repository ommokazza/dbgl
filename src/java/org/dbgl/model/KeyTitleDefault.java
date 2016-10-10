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
import org.dbgl.model.conf.Settings;


public class KeyTitleDefault extends KeyValuePair {

	private Boolean defaultChoice;

	public KeyTitleDefault(final int id, final String value, final Boolean isDefault) {
		super(id, value);
		this.defaultChoice = isDefault;
	}

	public String getTitle() {
		return getValue();
	}

	public Boolean isDefault() {
		return defaultChoice;
	}

	public String isDefaultString() {
		return defaultChoice ? Settings.getInstance().msg("general.yes"): Settings.getInstance().msg("general.no");
	}

	public void toggleDefault() {
		defaultChoice = !defaultChoice;
	}

	public static <T extends KeyTitleDefault> int indexOfDefault(final List<T> list) {
		int result = 0;
		for (T element: list) {
			if (element.isDefault()) {
				return result;
			}
			result++;
		}
		return -1; // no default found
	}

	public static <T extends KeyTitleDefault> T findDefault(final List<T> list) {
		for (T element: list) {
			if (element.isDefault()) {
				return element;
			}
		}
		return null; // no default found
	}
}
