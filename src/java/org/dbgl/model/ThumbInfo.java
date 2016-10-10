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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.dbgl.util.FileUtils;
import org.dbgl.util.PlatformUtils;


public class ThumbInfo {

	private String captures;
	private File canonicalCaptures;
	private File[] files;
	private String mainThumb;
	private boolean updated;

	public ThumbInfo(String captures) {
		this.captures = captures;
	}

	private void prepareInfo() {
		if (canonicalCaptures == null) {
			canonicalCaptures = FileUtils.canonicalToData(captures);
			updated = true;
		}
		if (files == null) {
			List<File> filesList = new ArrayList<File>();
			File[] allFiles = canonicalCaptures.listFiles();
			if (allFiles != null && allFiles.length > 0) {
				if (PlatformUtils.IS_LINUX)
					Arrays.sort(allFiles, new FileUtils.FileComparator());
				for (File f: allFiles) {
					if (FileUtils.isPicture(f.getName()))
						filesList.add(f);
				}
			}
			files = filesList.toArray(new File[filesList.size()]);
			updated = true;
		}
	}

	public String getMainThumb() {
		prepareInfo();
		if (mainThumb == null) {
			if (files.length == 0)
				return null;
			mainThumb = files[0].getPath();
			updated = true;
		}
		return mainThumb;
	}

	public File[] getAllThumbs() {
		prepareInfo();
		return files;
	}

	public void resetCachedInfo() {
		files = null;
		mainThumb = null;
	}

	public boolean isUpdated() {
		boolean isUpdated = updated;
		updated = false;
		return isUpdated;
	}
}
