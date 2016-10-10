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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.util.Enumeration;
import org.dbgl.util.FileUtils;


public class ISO9660FileSystem {

	public static final int COOKED_SECTOR_SIZE = 2 * 1024;
	public static final int VCD_SECTOR_SIZE = COOKED_SECTOR_SIZE + 288;
	public static final int RAW_SECTOR_SIZE = VCD_SECTOR_SIZE + 16;

	public static final String DEFAULT_ENCODING = "US-ASCII";

	private static final int RESERVED_SECTORS = 16;

	private int blockSize;
	private boolean mode2;
	private ISO9660VolumeDescriptorSet volumeDescriptorSet;
	private RandomAccessFile channel;

	public ISO9660FileSystem(File file) throws IOException {
		if (!file.exists()) {
			throw new FileNotFoundException("File not found");
		}

		if (file.getName().toLowerCase().endsWith(FileUtils.CDIMAGES[1])) {
			File binFile = parseCueSheet(file);
			if (binFile == null) {
				throw new IOException("Couldn't parse cue sheet");
			} else {
				file = binFile;
			}
		}

		this.channel = new RandomAccessFile(file, "r");
		this.volumeDescriptorSet = new ISO9660VolumeDescriptorSet(this);

		if (!(canReadVolumeDescriptors(COOKED_SECTOR_SIZE, false) || canReadVolumeDescriptors(RAW_SECTOR_SIZE, false) || canReadVolumeDescriptors(VCD_SECTOR_SIZE, true)
				|| canReadVolumeDescriptors(RAW_SECTOR_SIZE, true))) {
			throw new IOException("Couldn't find volume descriptor, the cd-image doesn't seem to be in ISO9660 file format");
		}
	}

	public void close() throws IOException {
		if (isClosed()) {
			return;
		}
		try {
			channel.close();
		} catch (IOException ex) {
			throw new IOException("Couldn't close file channel");
		} finally {
			channel = null;
		}
	}

	public Enumeration<ISO9660FileEntry> getEntries() {
		ensureOpen();
		return new EntryEnumeration(this, volumeDescriptorSet.getRootEntry());
	}

	public String getEncoding() {
		return volumeDescriptorSet.getEncoding();
	}

	byte[] getBytes(ISO9660FileEntry entry) throws IOException {
		int size = entry.getSize();
		int sectorsToRead = (size + COOKED_SECTOR_SIZE - 1) / COOKED_SECTOR_SIZE;
		byte[] buf = new byte[sectorsToRead * COOKED_SECTOR_SIZE];
		for (int i = 0; i < sectorsToRead; i++) {
			readBlock(entry.getStartBlock() + i, buf, i * COOKED_SECTOR_SIZE);
		}
		return buf;
	}

	public static File parseCueSheet(File file) throws IOException {
		File result = null;
		Reader reader = new FileReader(file);
		BufferedReader configData = new BufferedReader(reader);
		String orgTextLine;
		while ((orgTextLine = configData.readLine()) != null) {
			orgTextLine = orgTextLine.trim();
			if ((orgTextLine.length() > 0) && orgTextLine.startsWith("FILE")) {
				int idx1 = orgTextLine.indexOf('"', 5);
				int idx2 = orgTextLine.lastIndexOf('"');
				if (idx1 != -1 && idx2 != -1 && idx1 < idx2) {
					String binFilename = orgTextLine.substring(idx1 + 1, idx2);
					if (new File(binFilename).isAbsolute()) {
						result = new File(binFilename);
					} else {
						result = new File(file.getParent(), binFilename);
					}
					break;
				}
			}
		}
		configData.close();
		reader.close();
		return result;
	}

	private boolean canReadVolumeDescriptors(int sectorSize, boolean mode2) {
		this.blockSize = sectorSize;
		this.mode2 = mode2;
		final byte[] buffer = new byte[COOKED_SECTOR_SIZE];
		// skip the reserved blocks, then read volume descriptor blocks sequentially and add them to the VolumeDescriptorSet
		try {
			for (int block = RESERVED_SECTORS; readBlock(block, buffer, 0) && !volumeDescriptorSet.deserialize(buffer); block++);
		} catch (IOException e) {
			return false;
		}
		return volumeDescriptorSet.getRootEntry() != null;
	}

	private boolean readBlock(final long block, final byte[] buffer, final int bufOffset) throws IOException {
		final int bytesRead = readData(block * blockSize, buffer, bufOffset, COOKED_SECTOR_SIZE);
		if (bytesRead <= 0) {
			return false;
		}
		if (COOKED_SECTOR_SIZE != bytesRead) {
			throw new IOException("Could not deserialize a complete block");
		}
		return true;
	}

	private int readData(final long startPos, final byte[] buffer, final int offset, final int len) throws IOException {
		ensureOpen();
		long pos = startPos;
		if (blockSize == RAW_SECTOR_SIZE && !mode2) {
			pos += 16;
		}
		if (mode2) {
			pos += 24;
		}
		channel.seek(pos);
		return channel.read(buffer, offset, len);
	}

	private boolean isClosed() {
		return channel == null;
	}

	private void ensureOpen() throws IllegalStateException {
		if (isClosed()) {
			throw new IllegalStateException("File has been closed");
		}
	}
}
