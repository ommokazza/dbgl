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
package org.dbgl.loopy.util;

import java.io.UnsupportedEncodingException;


public abstract class Util {
	/**
	 * Gets an unsigned 8-bit value LSB first. See section 7.1.1.
	 *
	 * @param block
	 * @param pos
	 * @return
	 */
	public static int getUInt8(byte[] block, int pos) {
		return LittleEndian.getUInt8(block, pos - 1);
	}

	/**
	 * Gets an unsigned 16-bit value in both byteorders. See section 7.2.3.
	 *
	 * @param block
	 * @param pos
	 * @return
	 */
	public static int getUInt16Both(byte[] block, int pos) {
		return LittleEndian.getUInt16(block, pos - 1);
	}

	/**
	 * Gets an unsigned 32-bit value LSB first. See section 7.3.1.
	 *
	 * @param block
	 * @param pos
	 * @return
	 */
	public static long getUInt32LE(byte[] block, int pos) {
		return LittleEndian.getUInt32(block, pos - 1);
	}

	/**
	 * Gets a string of d-characters. See section 7.4.1.
	 *
	 * @param block
	 * @param pos
	 * @param length
	 * @return
	 */
	public static String getDChars(byte[] block, int pos, int length) {
		return new String(block, pos - 1, length).trim();
	}

	/**
	 * Gets a string of d-characters. See section 7.4.1.
	 *
	 * @param block
	 * @param pos
	 * @param length
	 * @param encoding
	 * @return
	 */
	public static String getDChars(byte[] block, int pos, int length, String encoding) {
		try {
			return new String(block, pos - 1, length, encoding).trim();
		} catch (UnsupportedEncodingException ex) {
			throw new RuntimeException(ex);
		}
	}
}