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

import org.apache.commons.lang3.StringUtils;


public class ImgSizeCommand {

	private int bytesPerSector, sectorsPerTrack, heads, cylinders;

	public ImgSizeCommand(final String command) {
		String[] elements = StringUtils.split(command, ',');
		if (elements.length == 4) {
			bytesPerSector = tryParse(elements[0]);
			sectorsPerTrack = tryParse(elements[1]);
			heads = tryParse(elements[2]);
			cylinders = tryParse(elements[3]);
		}
		if (bytesPerSector == 0 && sectorsPerTrack == 0 && heads == 0 && cylinders == 0) {
			bytesPerSector = 512;
			sectorsPerTrack = 63;
			heads = 16;
			cylinders = 142;
		}
	}

	public ImgSizeCommand(final int bytesPerSector, final int sectorsPerTrack, final int heads, final int cylinders) {
		this.bytesPerSector = bytesPerSector;
		this.sectorsPerTrack = sectorsPerTrack;
		this.heads = heads;
		this.cylinders = cylinders;
	}

	private int tryParse(final String s) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException nfe) {
			return 0;
		}
	}

	public int getBytesPerSector() {
		return bytesPerSector;
	}

	public int getSectorsPerTrack() {
		return sectorsPerTrack;
	}

	public int getHeads() {
		return heads;
	}

	public int getCylinders() {
		return cylinders;
	}

	public long getTotalSize() {
		return bytesPerSector * sectorsPerTrack * heads * cylinders;
	}

	public long getTotalSizeInMB() {
		return getTotalSize() / (1024 * 1024);
	}

	public String toString() {
		return bytesPerSector + "," + sectorsPerTrack + "," + heads + "," + cylinders;
	}
}
