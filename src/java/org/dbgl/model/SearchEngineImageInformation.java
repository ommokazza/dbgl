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

import org.eclipse.swt.graphics.ImageData;


public class SearchEngineImageInformation {

	public enum SearchEngineImageType {
		CoverArt, Screenshot
	};

	public ImageData data;
	public SearchEngineImageType type;
	public String url;
	public String description;

	public SearchEngineImageInformation(final SearchEngineImageType type, final String url, final String description) {
		this.type = type;
		this.url = url;
		this.description = description;
	}

	public void setData(ImageData data) {
		this.data = data;
	}
}