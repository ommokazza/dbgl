/*
 * Copyright (C) 2009,2010 Matthias Treydte <mt@waldheinz.de>
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
 * Contains the FAT32 specific parts of the boot sector.
 * 
 * @author Matthias Treydte &lt;matthias.treydte at meetwise.com&gt;
 */
final class Fat32BootSector extends BootSector {

	/**
	 * The offset to the entry specifying the first cluster of the FAT32 root directory.
	 */
	public final static int ROOT_DIR_FIRST_CLUSTER_OFFSET = 0x2c;

	/**
	 * The offset to the 4 bytes specifying the sectors per FAT value.
	 */
	public static final int SECTORS_PER_FAT_OFFSET = 0x24;

	/**
	 * Offset to the file system type label.
	 */
	public static final int FILE_SYSTEM_TYPE_OFFSET = 0x52;

	public static final int VERSION_OFFSET = 0x2a;
	public static final int VERSION = 0;

	public static final int FS_INFO_SECTOR_OFFSET = 0x30;
	public static final int BOOT_SECTOR_COPY_OFFSET = 0x32;
	public static final int EXTENDED_BOOT_SIGNATURE_OFFSET = 0x42;

	Fat32BootSector(BlockDevice device, long offset) throws IOException {
		super(device, offset);
	}

	/**
	 * Returns the first cluster in the FAT that contains the root directory.
	 * 
	 * @return the root directory's first cluster
	 */
	public long getRootDirFirstCluster() {
		return get32(ROOT_DIR_FIRST_CLUSTER_OFFSET);
	}

	/**
	 * Returns the sector that contains a copy of the boot sector, or 0 if there is no copy.
	 * 
	 * @return the sector number of the boot sector copy
	 */
	public int getBootSectorCopySector() {
		return get16(BOOT_SECTOR_COPY_OFFSET);
	}

	public int getFsInfoSectorNr() {
		return get16(FS_INFO_SECTOR_OFFSET);
	}

	@Override
	public long getSectorsPerFat() {
		return get32(SECTORS_PER_FAT_OFFSET);
	}

	@Override
	public FatType getFatType() {
		return FatType.FAT32;
	}

	@Override
	public long getSectorCount() {
		return super.getNrTotalSectors();
	}

	/**
	 * This is always 0 for FAT32.
	 * 
	 * @return always 0
	 */
	@Override
	public int getRootDirEntryCount() {
		return 0;
	}

	public int getFileSystemId() {
		return (int)super.get32(0x43);
	}

	@Override
	public int getFileSystemTypeLabelOffset() {
		return FILE_SYSTEM_TYPE_OFFSET;
	}

	@Override
	public int getExtendedBootSignatureOffset() {
		return EXTENDED_BOOT_SIGNATURE_OFFSET;
	}
}
