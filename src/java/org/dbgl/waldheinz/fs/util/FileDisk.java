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

package org.dbgl.waldheinz.fs.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.dbgl.waldheinz.fs.fat.BlockDevice;


/**
 * This is a {@code BlockDevice} that uses a {@link File} as it's backing store.
 * 
 * @author Matthias Treydte &lt;matthias.treydte at meetwise.com&gt;
 */
public final class FileDisk implements BlockDevice {

	/**
	 * The number of bytes per sector for all {@code FileDisk} instances.
	 */
	public final static int BYTES_PER_SECTOR = 512;

	private final RandomAccessFile raf;
	private final FileChannel fc;
	private boolean closed;

	/**
	 * Creates a new instance of {@code FileDisk} for the specified {@code File}.
	 * 
	 * @param file the file that holds the disk contents
	 * @throws FileNotFoundException if the specified file does not exist
	 */
	public FileDisk(File file) throws FileNotFoundException {
		this.raf = new RandomAccessFile(file, "r");
		this.fc = raf.getChannel();
		this.closed = false;
	}

	@Override
	public long getSize() throws IOException {
		checkClosed();

		return raf.length();
	}

	@Override
	public void read(long devOffset, ByteBuffer dest) throws IOException {
		checkClosed();

		int toRead = dest.remaining();
		if ((devOffset + toRead) > getSize())
			throw new IOException("reading past end of device");

		while (toRead > 0) {
			final int read = fc.read(dest, devOffset);
			if (read < 0)
				throw new IOException();
			toRead -= read;
			devOffset += read;
		}
	}

	@Override
	public int getSectorSize() {
		checkClosed();

		return BYTES_PER_SECTOR;
	}

	@Override
	public void close() throws IOException {
		if (isClosed())
			return;

		this.closed = true;
		this.fc.close();
		this.raf.close();
	}

	@Override
	public boolean isClosed() {
		return this.closed;
	}

	private void checkClosed() {
		if (closed)
			throw new IllegalStateException("device already closed");
	}
}
