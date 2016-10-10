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

import java.io.IOException;
import org.dbgl.loopy.util.Util;


/**
 * Layout of Primary Volume Descriptor:
 * 
 * <pre/>
     length     pos     contents
     ---------  ------  ---------------------------------------------------------
     1          1       volume descriptor type: 1
     5          2       standard identifier: 67, 68, 48, 48, 49 (CD001)
     1          8       0
     32         9       system identifier
     32         41      volume identifier
     8          73      zeroes
     8          81      total number of blocks, as a both endian double word
     32         89      zeroes
     4          121     1, as a both endian word [volume set size]
     4          125     1, as a both endian word [volume sequence number]
     4          129     2048 (the block size), as a both endian word
     8          133     path table length in bytes, as a both endian double word
     4          141     number of first sector in first little endian path table, as a little endian double word
     4          145     number of first sector in second little endian path table, as a little endian double word, or
                        zero if there is no second little endian path table
     4          149     number of first sector in first big endian path table, as a big endian double word
     4          153     number of first sector in second big endian path table, as a big endian double word, or zero if
                        there is no second big endian path table
     34         157     root directory record, as described below
     128        191     volume set identifier
     128        319     publisher identifier
     128        447     data preparer identifier
     128        575     application identifier
     37         703     copyright file identifier
     37         740     abstract file identifier
     37         777     bibliographical file identifier
     17         814     date and time of volume creation
     17         831     date and time of most recent modification
     17         848     date and time when volume expires
     17         865     date and time when volume is effective
     1          882     1
     1          883     0
     512        884     reserved for application use (usually zeros)
     653        1396    zeroes
 * </pre>
 */
public class ISO9660VolumeDescriptorSet {

	private static final int TYPE_PRIMARY_DESCRIPTOR = 1;
	private static final int TYPE_SUPPLEMENTARY_DESCRIPTOR = 2;
	private static final int TYPE_TERMINATOR = 255;

	private final ISO9660FileSystem fileSystem;
	private ISO9660FileEntry rootDirectoryEntry;
	private String encoding = ISO9660FileSystem.DEFAULT_ENCODING;
	private boolean hasPrimary = false;
	private boolean hasSupplementary = false;

	public ISO9660VolumeDescriptorSet(ISO9660FileSystem fileSystem) {
		this.fileSystem = fileSystem;
	}

	/**
	 * Load a volume descriptor from the specified byte array.
	 *
	 * @param volumeDescriptor the volume descriptor to deserialize
	 * @return true if the volume descriptor is a terminator
	 * @throws IOException if there is an error deserializing the volume descriptor
	 */
	public boolean deserialize(byte[] descriptor) throws IOException {
		final int type = Util.getUInt8(descriptor, 1);
		boolean terminator = false;
		switch (type) {
			case TYPE_TERMINATOR:
				if (!this.hasPrimary) {
					throw new IOException("No primary volume descriptor found");
				}
				terminator = true;
				break;
			case TYPE_PRIMARY_DESCRIPTOR:
				deserializePrimary(descriptor);
				break;
			case TYPE_SUPPLEMENTARY_DESCRIPTOR:
				deserializeSupplementary(descriptor);
				break;
			default:
		}
		return terminator;
	}

	public ISO9660FileEntry getRootEntry() {
		return this.rootDirectoryEntry;
	}

	public String getEncoding() {
		return this.encoding;
	}

	private void deserializePrimary(byte[] descriptor) throws IOException {
		// some ISO 9660 file systems can contain multiple identical primary volume descriptors
		if (hasPrimary) {
			return;
		}
		final String identifier = Util.getDChars(descriptor, 2, 5, fileSystem.getEncoding());
		final int marker = Util.getUInt8(descriptor, 7);
		if (!identifier.equals("CD001") || marker != 1) {
			throw new IOException("Invalid primary volume descriptor");
		}
		validateBlockSize(descriptor);
		if (!hasSupplementary) {
			deserializeCommon(descriptor);
		}
		hasPrimary = true;
	}

	private void deserializeCommon(byte[] descriptor) throws IOException {
		rootDirectoryEntry = new ISO9660FileEntry(fileSystem, descriptor, 157);
	}

	private void deserializeSupplementary(byte[] descriptor) throws IOException {
		if (hasSupplementary) {
			return;
		}
		validateBlockSize(descriptor);
		String escapeSequences = Util.getDChars(descriptor, 89, 32);
		String enc = getEncoding(escapeSequences);
		if (enc != null) {
			encoding = enc;
			deserializeCommon(descriptor);
			hasSupplementary = true;
		}
	}

	private void validateBlockSize(byte[] descriptor) throws IOException {
		int blockSize = Util.getUInt16Both(descriptor, 129);
		if (blockSize != ISO9660FileSystem.COOKED_SECTOR_SIZE) {
			throw new IOException("Invalid block size: " + blockSize);
		}
	}

	private String getEncoding(String escapeSequences) {
		String encoding = null;
		if (escapeSequences.equals("%/@")) {
			encoding = "UTF-16BE"; // UCS-2 level 1
		} else if (escapeSequences.equals("%/C")) {
			encoding = "UTF-16BE"; // UCS-2 level 2
		} else if (escapeSequences.equals("%/E")) {
			encoding = "UTF-16BE"; // UCS-2 level 3
		}
		return encoding;
	}
}