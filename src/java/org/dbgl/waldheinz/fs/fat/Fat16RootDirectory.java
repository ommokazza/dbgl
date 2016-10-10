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
import java.nio.ByteBuffer;


/**
 * The root directory of a FAT12/16 partition.
 * 
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
final class Fat16RootDirectory extends AbstractDirectory {
	private final BlockDevice device;
	private final long deviceOffset;

	private Fat16RootDirectory(Fat16BootSector bs) {
		super(bs.getRootDirEntryCount(), true);

		if (bs.getRootDirEntryCount() <= 0)
			throw new IllegalArgumentException("root directory size is " + bs.getRootDirEntryCount());

		this.deviceOffset = bs.getRootDirOffset();
		this.device = bs.getDevice();
	}

	/**
	 * Reads a {@code Fat16RootDirectory} as indicated by the specified {@code Fat16BootSector}.
	 * 
	 * @param bs the boot sector that describes the root directory to read
	 * @return the directory that was read
	 * @throws IOException on read error
	 */
	public static Fat16RootDirectory read(Fat16BootSector bs) throws IOException {

		final Fat16RootDirectory result = new Fat16RootDirectory(bs);
		result.read();
		return result;
	}

	@Override
	protected void read(ByteBuffer data) throws IOException {
		this.device.read(deviceOffset, data);
	}
}
