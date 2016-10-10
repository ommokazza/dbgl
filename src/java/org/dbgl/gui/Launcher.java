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
package org.dbgl.gui;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import org.dbgl.util.FileUtils;
import org.dbgl.util.PlatformUtils;
import org.eclipse.swt.SWT;
import org.hsqldb.persist.HsqlDatabaseProperties;


public class Launcher {

	public static void main(final String[] args) {
		// load the appropriate SWT JAR for the architecture
		System.out.print("Launching DBGL using " + PlatformUtils.JVM_ARCH + "-Bit VM " + PlatformUtils.JVM_VERSION + " on " + PlatformUtils.OS_NAME + " v" + PlatformUtils.OS_VERSION
				+ PlatformUtils.OS_ARCH + ", " + HsqlDatabaseProperties.PRODUCT_NAME + " " + HsqlDatabaseProperties.THIS_FULL_VERSION);
		File homeDir = new File(System.getProperty("user.home")).getAbsoluteFile();
		if (System.getProperty("swt.library.path") == null && homeDir.isDirectory() && !PlatformUtils.isDirectoryWritable(homeDir)) {
			String dbglLibPath = "lib";
			File libDir = new File(dbglLibPath).getAbsoluteFile();
			if (libDir.isDirectory() && PlatformUtils.isDirectoryWritable(libDir)) {
				System.out.print(", user.home '" + homeDir + "' appears unwritable - switched swt.library.path to '" + dbglLibPath + "'");
				System.setProperty("swt.library.path", dbglLibPath);
			}
		}
		loadSWT();
		System.out.println(", SWT v" + SWT.getVersion() + SWT.getPlatform());
		new MainWindow().open();
	}

	public static void loadSWT() {
		try {
			File file = null;
			if (PlatformUtils.IS_WINDOWS) {
				file = new File("lib/swtwin32.jar"); // x86
				if (PlatformUtils.JVM_ARCH.equals("64")) {
					file = new File("lib/swtwin64.jar"); // x64
				}
			} else if (PlatformUtils.IS_OSX) {
				file = new File("lib/swtmac32.jar"); // x86
				if (PlatformUtils.JVM_ARCH.equals("64")) {
					file = new File("lib/swtmac64.jar"); // x64
				} else if (PlatformUtils.OS_ARCH.startsWith("ppc")) {
					file = new File("lib/swtmaccb.jar"); // carbon
				}
			} else if (PlatformUtils.IS_LINUX) {
				file = new File("lib/swtlin32.jar"); // x86
				if (PlatformUtils.JVM_ARCH.equals("64")) {
					file = new File("lib/swtlin64.jar"); // x64
				}
			}

			if (file == null || !FileUtils.isExistingFile(file)) {
				file = new File("lib/swt.jar"); // old system
			}

			Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] {URL.class});
			method.setAccessible(true);
			method.invoke(ClassLoader.getSystemClassLoader(), new Object[] {file.toURI().toURL()});

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
