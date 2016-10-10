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

import java.io.File;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;


public class ShortFile implements Comparable<ShortFile> {
	private File file;
	private String name;

	public ShortFile(File file, String name) {
		this.file = file;
		this.name = name;
	}

	public File getFile() {
		return file;
	}

	public String getName() {
		return name;
	}

	public String getFormattedName() {
		return file.isDirectory() ? '[' + name + ']': name;
	}

	public boolean isContainedIn(Set<ShortFile> set) {
		int count = StringUtils.countMatches(file.getPath(), "\\");
		int idx1 = name.indexOf('~');
		int idx2 = (idx1 == -1) ? -1: name.indexOf('.', idx1 + 2);
		if (idx2 == -1)
			idx2 = Math.min(name.length(), 8);

		for (ShortFile shortFile: set) {
			if (count != StringUtils.countMatches(shortFile.file.getPath(), "\\"))
				return false;
			if (idx1 != -1 && idx1 == shortFile.name.indexOf('~')) {
				int idx3 = shortFile.name.indexOf('.', idx1 + 2);
				if (idx3 == -1)
					idx3 = Math.min(shortFile.name.length(), 8);
				if (idx2 == idx3 && name.substring(0, idx2).equals(shortFile.name.substring(0, idx2)))
					return true;
			} else if (name.equals(shortFile.name)) {
				return true;
			}
		}

		return false;
	}

	public int compareTo(ShortFile o) {
		int count1 = StringUtils.countMatches(o.file.getPath(), "\\");
		int count2 = StringUtils.countMatches(this.file.getPath(), "\\");
		if (count1 != count2)
			return count1 - count2;
		int res = Boolean.valueOf(o.file.isDirectory()).compareTo(this.file.isDirectory());
		if (res != 0)
			return res;
		int idx1 = this.name.indexOf('~');
		if (idx1 != -1 && idx1 == o.name.indexOf('~')) {
			int idx2 = this.name.indexOf('.', idx1 + 2);
			if (idx2 != -1 && idx2 == o.name.indexOf('.', idx1 + 2))
				return this.name.substring(0, idx2).compareTo(o.name.substring(0, idx2));
		}
		return this.name.compareTo(o.name);
	}
}