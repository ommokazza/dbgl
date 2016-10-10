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
import java.nio.ByteOrder;


/**
 * 
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
class Sector {
	private final BlockDevice device;
	protected final long offset;
	protected final ByteBuffer buffer;

	protected Sector(BlockDevice device, long offset, int size) {
		this.offset = offset;
		this.device = device;
		this.buffer = ByteBuffer.allocate(size);
		this.buffer.order(ByteOrder.LITTLE_ENDIAN);
	}

	/**
	 * Reads the contents of this {@code Sector} from the device into the internal buffer and resets the "dirty" state.
	 * 
	 * @throws IOException on read error
	 * @see #isDirty()
	 */
	protected void read() throws IOException {
		buffer.rewind();
		buffer.limit(buffer.capacity());
		device.read(offset, buffer);
	}

	/**
	 * Returns the {@code BlockDevice} where this {@code Sector} is stored.
	 * 
	 * @return this {@code Sector}'s device
	 */
	public BlockDevice getDevice() {
		return this.device;
	}

	protected int get16(int offset) {
		return buffer.getShort(offset) & 0xffff;
	}

	protected long get32(int offset) {
		return buffer.getInt(offset);
	}

	protected int get8(int offset) {
		return buffer.get(offset) & 0xff;
	}
}
