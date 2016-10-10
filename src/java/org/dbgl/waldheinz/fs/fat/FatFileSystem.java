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
 * <p>
 * Implements the {@code FileSystem} interface for the FAT family of file systems. This class always uses the "long file name" specification when writing directory entries.
 * </p>
 * 
 * @author Ewout Prangsma &lt;epr at jnode.org&gt;
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public final class FatFileSystem {

	private final Fat fat;
	private final BootSector bs;
	private final FatLfnDirectory rootDir;
	private final AbstractDirectory rootDirStore;
	private final FatType fatType;

	public FatFileSystem(BlockDevice device) throws IOException {
		this.bs = BootSector.read(device);

		if (bs.getNrFats() <= 0)
			throw new IOException("boot sector says there are no FATs");

		this.fatType = bs.getFatType();
		this.fat = Fat.read(bs, 0);

		for (int i = 1; i < bs.getNrFats(); i++) {
			final Fat tmpFat = Fat.read(bs, i);
			if (!fat.equals(tmpFat)) {
				System.err.println("FAT " + i + " differs from FAT 0");
			}
		}

		if (fatType == FatType.FAT32) {
			final Fat32BootSector f32bs = (Fat32BootSector)bs;
			ClusterChain rootDirFile = new ClusterChain(fat, f32bs.getRootDirFirstCluster());
			this.rootDirStore = ClusterChainDirectory.readRoot(rootDirFile);
		} else {
			this.rootDirStore = Fat16RootDirectory.read((Fat16BootSector)bs);
		}

		this.rootDir = new FatLfnDirectory(rootDirStore, fat);

	}

	public FatLfnDirectory getRoot() {
		return rootDir;
	}
}
