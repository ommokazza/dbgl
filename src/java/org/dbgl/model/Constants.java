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
package org.dbgl.model;

import java.io.IOException;
import java.util.Properties;


public class Constants {

	public static final String PROGRAM_NAME_FULL = "DOSBox Game Launcher";

	public static final String PROGRAM_VERSION;

	static {
		Properties prop = new Properties();
		try {
			prop.load(Constants.class.getClassLoader().getResourceAsStream("version.properties"));
		} catch (IOException ex) {}
		PROGRAM_VERSION = prop.getProperty("majorversion", "?") + '.' + prop.getProperty("minorversion", "?");
	}

	public static final int RO_COLUMN_NAMES = 10;
	public static final int EDIT_COLUMN_NAMES = 10;

}
