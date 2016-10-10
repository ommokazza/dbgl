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

import java.util.Arrays;
import org.dbgl.loopy.util.LittleEndian;


/**
 * Represents a "short" (8.3) file name as used by DOS.
 * 
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
final class ShortName {

	private final char[] name;

	public ShortName(byte[] data) {
		final char[] nameArr = new char[8];

		for (int i = 0; i < nameArr.length; i++) {
			nameArr[i] = (char)LittleEndian.getUInt8(data, i);
		}

		if (LittleEndian.getUInt8(data, 0) == 0x05) {
			nameArr[0] = (char)0xe5;
		}

		final char[] extArr = new char[3];
		for (int i = 0; i < extArr.length; i++) {
			extArr[i] = (char)LittleEndian.getUInt8(data, 0x08 + i);
		}

		this.name = toCharArray(new String(nameArr).trim(), new String(extArr).trim());
	}

	private static char[] toCharArray(String name, String ext) {
		checkValidName(name);
		checkValidExt(ext);

		final char[] result = new char[11];
		Arrays.fill(result, ' ');
		System.arraycopy(name.toCharArray(), 0, result, 0, name.length());
		System.arraycopy(ext.toCharArray(), 0, result, 8, ext.length());

		return result;
	}

	public String asSimpleString() {
		return new String(this.name).trim();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [" + asSimpleString() + "]"; // NOI18N
	}

	private static void checkValidName(String name) {
		checkString(name, "name", 1, 8);
	}

	private static void checkValidExt(String ext) {
		checkString(ext, "extension", 0, 3);
	}

	private static void checkString(String str, String strType, int minLength, int maxLength) {
		if (str == null)
			throw new IllegalArgumentException(strType + " is null");
		if (str.length() < minLength)
			throw new IllegalArgumentException(strType + " must have at least " + minLength + " characters: " + str);
		if (str.length() > maxLength)
			throw new IllegalArgumentException(strType + " has more than " + maxLength + " characters: " + str);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ShortName)) {
			return false;
		}

		final ShortName other = (ShortName)obj;
		return Arrays.equals(name, other.name);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.name);
	}
}
