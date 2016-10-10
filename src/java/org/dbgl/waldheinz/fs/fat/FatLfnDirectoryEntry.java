/*
 * Copyright (C) 2003-2009 JNode.org
 *               2009,2010 Matthias Treydte <mt@waldheinz.de>
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; If not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.dbgl.waldheinz.fs.fat;

import java.io.IOException;


/**
 * Represents an entry in a {@link FatLfnDirectory}. Besides implementing the {@link FsDirectoryEntry} interface for FAT file systems, it allows access to the {@link #setArchiveFlag(boolean) archive},
 * {@link #setHiddenFlag(boolean) hidden}, {@link #setReadOnlyFlag(boolean) read-only} and {@link #setSystemFlag(boolean) system} flags specifed for the FAT file system.
 * 
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 * @since 0.6
 */
public final class FatLfnDirectoryEntry {

	final FatDirectoryEntry realEntry;

	private FatLfnDirectory parent;
	private String fileName;

	FatLfnDirectoryEntry(FatLfnDirectory parent, FatDirectoryEntry realEntry, String fileName) {
		this.parent = parent;
		this.realEntry = realEntry;
		this.fileName = fileName;
	}

	static FatLfnDirectoryEntry extract(FatLfnDirectory dir, int offset, int len) {

		final FatDirectoryEntry realEntry = dir.dir.getEntry(offset + len - 1);
		final String fileName;

		if (len == 1) {
			/* this is just an old plain 8.3 entry */
			fileName = realEntry.getShortName().asSimpleString();
		} else {
			/* stored in reverse order */
			final StringBuilder name = new StringBuilder(13 * (len - 1));

			for (int i = len - 2; i >= 0; i--) {
				FatDirectoryEntry entry = dir.dir.getEntry(i + offset);
				name.append(entry.getLfnPart());
			}

			fileName = name.toString().trim();
		}

		return new FatLfnDirectoryEntry(dir, realEntry, fileName);
	}

	/**
	 * Returns if this directory entry has the FAT "hidden" flag set.
	 * 
	 * @return if this is a hidden directory entry
	 * @see #setHiddenFlag(boolean)
	 */
	public boolean isHiddenFlag() {
		return this.realEntry.isHiddenFlag();
	}

	/**
	 * Returns if this directory entry has the FAT "system" flag set.
	 * 
	 * @return if this is a "system" directory entry
	 * @see #setSystemFlag(boolean)
	 */
	public boolean isSystemFlag() {
		return this.realEntry.isSystemFlag();
	}

	/**
	 * Returns if this directory entry has the FAT "read-only" flag set. This entry may still modified if {@link #isReadOnly()} returns {@code true}.
	 * 
	 * @return if this entry has the read-only flag set
	 * @see #setReadOnlyFlag(boolean)
	 */
	public boolean isReadOnlyFlag() {
		return this.realEntry.isReadonlyFlag();
	}

	/**
	 * Returns if this directory entry has the FAT "archive" flag set.
	 * 
	 * @return if this entry has the archive flag set
	 */
	public boolean isArchiveFlag() {
		return this.realEntry.isArchiveFlag();
	}

	public String getName() {
		return fileName;
	}

	public String getShortName() {
		return realEntry.getShortName().asSimpleString();
	}

	public FatLfnDirectory getDirectory() throws IOException {
		return parent.getDirectory(realEntry);
	}

	@Override
	public String toString() {
		return "LFN = " + fileName + " / SFN = " + realEntry.getShortName();
	}

	public boolean isFile() {
		return realEntry.isFile();
	}

	public boolean isDirectory() {
		return realEntry.isDirectory();
	}
}
