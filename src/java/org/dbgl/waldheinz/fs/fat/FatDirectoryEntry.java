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

import java.nio.ByteBuffer;
import org.dbgl.loopy.util.LittleEndian;


/**
 * 
 * 
 * @author Ewout Prangsma &lt;epr at jnode.org&gt;
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public final class FatDirectoryEntry {

	/**
	 * The size in bytes of an FAT directory entry.
	 */
	public final static int SIZE = 32;

	/**
	 * The offset to the attributes byte.
	 */
	private static final int OFFSET_ATTRIBUTES = 0x0b;

	/**
	 * The offset to the file size dword.
	 */
	private static final int OFFSET_FILE_SIZE = 0x1c;

	private static final int F_READONLY = 0x01;
	private static final int F_HIDDEN = 0x02;
	private static final int F_SYSTEM = 0x04;
	private static final int F_VOLUME_ID = 0x08;
	private static final int F_DIRECTORY = 0x10;
	private static final int F_ARCHIVE = 0x20;

	/**
	 * The magic byte denoting that this entry was deleted and is free for reuse.
	 * 
	 * @see #isDeleted()
	 */
	public static final int ENTRY_DELETED_MAGIC = 0xe5;

	private final byte[] data;

	FatDirectoryEntry(byte[] data) {
		this.data = data;
	}

	/**
	 * Reads a {@code FatDirectoryEntry} from the specified {@code ByteBuffer}. The buffer must have at least {@link #SIZE} bytes remaining. The entry is read from the buffer's current position, and
	 * if this method returns non-null the position will have advanced by {@link #SIZE} bytes, otherwise the position will remain unchanged.
	 * 
	 * @param buff the buffer to read the entry from
	 * @return the directory entry that was read from the buffer or {@code null} if there was no entry to read from the specified position (first byte was 0)
	 */
	public static FatDirectoryEntry read(ByteBuffer buff) {
		assert(buff.remaining() >= SIZE);

		/* peek into the buffer to see if we're done with reading */

		if (buff.get(buff.position()) == 0)
			return null;

		/* read the directory entry */

		final byte[] data = new byte[SIZE];
		buff.get(data);
		return new FatDirectoryEntry(data);
	}

	/**
	 * Decides if this entry is a "volume label" entry according to the FAT specification.
	 * 
	 * @return if this is a volume label entry
	 */
	public boolean isVolumeLabel() {
		if (isLfnEntry())
			return false;
		else
			return ((getFlags() & (F_DIRECTORY | F_VOLUME_ID)) == F_VOLUME_ID);
	}

	public boolean isSystemFlag() {
		return ((getFlags() & F_SYSTEM) != 0);
	}

	public boolean isArchiveFlag() {
		return ((getFlags() & F_ARCHIVE) != 0);
	}

	public boolean isHiddenFlag() {
		return ((getFlags() & F_HIDDEN) != 0);
	}

	public boolean isReadonlyFlag() {
		return ((getFlags() & F_READONLY) != 0);
	}

	public boolean isVolumeIdFlag() {
		return ((getFlags() & F_VOLUME_ID) != 0);
	}

	public boolean isLfnEntry() {
		return isReadonlyFlag() && isSystemFlag() && isHiddenFlag() && isVolumeIdFlag();
	}

	private int getFlags() {
		return LittleEndian.getUInt8(data, OFFSET_ATTRIBUTES);
	}

	public boolean isDirectory() {
		return ((getFlags() & (F_DIRECTORY | F_VOLUME_ID)) == F_DIRECTORY);
	}

	/**
	 * Returns if this entry has been marked as deleted. A deleted entry has its first byte set to the magic {@link #ENTRY_DELETED_MAGIC} value.
	 * 
	 * @return if this entry is marked as deleted
	 */
	public boolean isDeleted() {
		return (LittleEndian.getUInt8(data, 0) == ENTRY_DELETED_MAGIC);
	}

	/**
	 * Returns the size of this entry as stored at {@link #OFFSET_FILE_SIZE}.
	 * 
	 * @return the size of the file represented by this entry
	 */
	public long getLength() {
		return LittleEndian.getUInt32(data, OFFSET_FILE_SIZE);
	}

	/**
	 * Returns the {@code ShortName} that is stored in this directory entry or {@code null} if this entry has not been initialized.
	 * 
	 * @return the {@code ShortName} stored in this entry or {@code null}
	 */
	public ShortName getShortName() {
		if (this.data[0] == 0) {
			return null;
		} else {
			return new ShortName(this.data);
		}
	}

	/**
	 * Does this entry refer to a file?
	 * 
	 * @return
	 * @see org.jnode.fs.FSDirectoryEntry#isFile()
	 */
	public boolean isFile() {
		return ((getFlags() & (F_DIRECTORY | F_VOLUME_ID)) == 0);
	}

	/**
	 * Returns the startCluster.
	 * 
	 * @return int
	 */
	public long getStartCluster() {
		return LittleEndian.getUInt16(data, 0x1a);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [name=" + getShortName() + "]"; // NOI18N
	}

	String getLfnPart() {
		final char[] unicodechar = new char[13];

		unicodechar[0] = (char)LittleEndian.getUInt16(data, 1);
		unicodechar[1] = (char)LittleEndian.getUInt16(data, 3);
		unicodechar[2] = (char)LittleEndian.getUInt16(data, 5);
		unicodechar[3] = (char)LittleEndian.getUInt16(data, 7);
		unicodechar[4] = (char)LittleEndian.getUInt16(data, 9);
		unicodechar[5] = (char)LittleEndian.getUInt16(data, 14);
		unicodechar[6] = (char)LittleEndian.getUInt16(data, 16);
		unicodechar[7] = (char)LittleEndian.getUInt16(data, 18);
		unicodechar[8] = (char)LittleEndian.getUInt16(data, 20);
		unicodechar[9] = (char)LittleEndian.getUInt16(data, 22);
		unicodechar[10] = (char)LittleEndian.getUInt16(data, 24);
		unicodechar[11] = (char)LittleEndian.getUInt16(data, 28);
		unicodechar[12] = (char)LittleEndian.getUInt16(data, 30);

		int end = 0;

		while ((end < 13) && (unicodechar[end] != '\0')) {
			end++;
		}

		return new String(unicodechar).substring(0, end);
	}
}
