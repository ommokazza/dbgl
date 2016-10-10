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

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.io.IOException;
import org.dbgl.loopy.util.LittleEndian;


/**
 * A breadth-first Enumeration of the entries in a ISO9660 file system.
 */
class EntryEnumeration implements Enumeration<ISO9660FileEntry> {

	private final ISO9660FileSystem fileSystem;
	private final List<ISO9660FileEntry> queue;

	public EntryEnumeration(final ISO9660FileSystem fileSystem, final ISO9660FileEntry rootEntry) {
		this.fileSystem = fileSystem;
		queue = new LinkedList<ISO9660FileEntry>();
		queue.add(rootEntry);
	}

	public boolean hasMoreElements() {
		return !queue.isEmpty();
	}

	public ISO9660FileEntry nextElement() {
		if (!hasMoreElements()) {
			throw new NoSuchElementException();
		}
		// pop next entry from the queue
		final ISO9660FileEntry entry = queue.remove(0);
		// if the entry is a directory, queue all its children
		if (entry.isDirectory()) {
			final byte[] content;
			try {
				content = fileSystem.getBytes(entry);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
			int offset = 0;
			boolean paddingMode = false;
			while (offset < content.length) {
				if (LittleEndian.getUInt8(content, offset) <= 0) {
					paddingMode = true;
					offset += 2;
					continue;
				}
				ISO9660FileEntry child = new ISO9660FileEntry(fileSystem, entry.getPath(), content, offset + 1);
				if (paddingMode && child.getSize() < 0) {
					continue;
				}
				offset += child.getEntryLength();
				// It doesn't seem useful to include the . and .. entries
				if (!".".equals(child.getName()) && !"..".equals(child.getName())) {
					queue.add(child);
				}
			}
		}
		return entry;
	}
}
