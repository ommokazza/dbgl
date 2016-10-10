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
package org.dbgl.loopy.iso9660;

import org.dbgl.loopy.util.Util;


public final class ISO9660FileEntry {

	private static final char ID_SEPARATOR = ';';

	private ISO9660FileSystem fileSystem;
	private String parentPath;
	private final int entryLength;
	private final long startSector;
	private final int dataLength;
	private final int flags;
	private final String identifier;

	public ISO9660FileEntry(final ISO9660FileSystem fileSystem, final byte[] block, final int pos) {
		this(fileSystem, null, block, pos);
	}

	/**
	 * Initialize this instance.
	 * 
	 * @param fileSystem the parent file system
	 * @param parentPath the path of the parent directory
	 * @param block the bytes of the sector containing this file entry
	 * @param startPos the starting position of this file entry
	 */
	public ISO9660FileEntry(final ISO9660FileSystem fileSystem, final String parentPath, final byte[] block, final int startPos) {
		this.fileSystem = fileSystem;
		this.parentPath = parentPath;
		final int offset = startPos - 1;
		this.entryLength = Util.getUInt8(block, offset + 1);
		this.startSector = Util.getUInt32LE(block, offset + 3);
		this.dataLength = (int)Util.getUInt32LE(block, offset + 11);
		this.flags = Util.getUInt8(block, offset + 26);
		this.identifier = getFileIdentifier(block, offset, isDirectory());
	}

	private String getFileIdentifier(final byte[] block, final int offset, final boolean isDir) {
		final int fidLength = Util.getUInt8(block, offset + 33);
		if (isDir) {
			final int buff34 = Util.getUInt8(block, offset + 34);
			if ((fidLength == 1) && (buff34 == 0x00)) {
				return ".";
			} else if ((fidLength == 1) && (buff34 == 0x01)) {
				return "..";
			}
		}
		final String id = Util.getDChars(block, offset + 34, fidLength, fileSystem.getEncoding());
		final int sepIdx = id.indexOf(ID_SEPARATOR);
		if (sepIdx >= 0) {
			return id.substring(0, sepIdx);
		} else {
			return id;
		}
	}

	public String getPath() {
		if (".".equals(getName())) {
			return "";
		}
		StringBuffer buf = new StringBuffer();
		if (parentPath != null) {
			buf.append(parentPath);
		}
		buf.append(getName());
		if (isDirectory()) {
			buf.append("/");
		}
		return buf.toString();
	}

	public boolean isDirectory() {
		return (flags & 0x02) != 0;
	}

	String getName() {
		return identifier;
	}

	int getSize() {
		return dataLength;
	}

	long getStartBlock() {
		return startSector;
	}

	int getEntryLength() {
		return entryLength;
	}
}
