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

/**
 * Enumerates the different entry sizes of 12, 16 and 32 bits for the different FAT flavours.
 * 
 * @author Ewout Prangsma &lt;epr at jnode.org&gt;
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public enum FatType {

	/**
	 * Represents a 12-bit file allocation table.
	 */
	FAT12((1 << 12) - 16, 0xFFFL, 1.5f) { // NOI18N

		@Override
		public long readEntry(byte[] data, int index) {
			final int idx = (int)(index * 1.5);
			final int b1 = data[idx] & 0xFF;
			final int b2 = data[idx + 1] & 0xFF;
			final int v = (b2 << 8) | b1;

			if ((index % 2) == 0) {
				return v & 0xFFF;
			} else {
				return v >> 4;
			}
		}
	},

	/**
	 * Represents a 16-bit file allocation table.
	 */
	FAT16((1 << 16) - 16, 0xFFFFL, 2.0f) { // NOI18N

		@Override
		public long readEntry(byte[] data, int index) {
			final int idx = index << 1;
			final int b1 = data[idx] & 0xFF;
			final int b2 = data[idx + 1] & 0xFF;
			return (b2 << 8) | b1;
		}
	},

	/**
	 * Represents a 32-bit file allocation table.
	 */
	FAT32((1 << 28) - 16, 0xFFFFFFFFL, 4.0f) { // NOI18N

		@Override
		public long readEntry(byte[] data, int index) {
			final int idx = index * 4;
			final long l1 = data[idx] & 0xFF;
			final long l2 = data[idx + 1] & 0xFF;
			final long l3 = data[idx + 2] & 0xFF;
			final long l4 = data[idx + 3] & 0xFF;
			return (l4 << 24) | (l3 << 16) | (l2 << 8) | l1;
		}
	};

	private final long eofCluster;
	private final float entrySize;

	private FatType(int maxClusters, long bitMask, float entrySize) {
		this.eofCluster = (0xFFFFFF8L & bitMask);
		this.entrySize = entrySize;
	}

	public boolean isEofCluster(long entry) {
		return (entry >= eofCluster);
	}

	public float getEntrySize() {
		return entrySize;
	}

	public abstract long readEntry(byte[] data, int index);
}
