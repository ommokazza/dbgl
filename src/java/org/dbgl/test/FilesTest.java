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
package org.dbgl.test;

import static org.junit.Assert.*;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import org.junit.Test;
import org.apache.commons.lang3.StringUtils;
import org.dbgl.model.SearchResult;
import org.dbgl.model.ShortFile;
import org.dbgl.model.SearchResult.ResultType;
import org.dbgl.util.FileUtils;
import org.dbgl.util.PlatformUtils;


public class FilesTest {

	@Test
	public void testGetCanonicalPath() {
		assertEquals("C:\\data\\bla.exe", FileUtils.canonical("c:\\data\\bla.exe").getPath());
		assertEquals("C:\\Projects\\DBGL\\data\\bla.exe", FileUtils.canonical("data\\bla.exe").getPath());
		assertEquals("C:\\data2\\bla.exe", FileUtils.canonical("c:\\data\\..\\data2\\bla.exe").getPath());
		assertEquals("C:\\data\\bla.exe", FileUtils.canonical("c:\\data\\..\\data\\bla.exe").getPath());
	}

	@Test
	public void testIsExistingFile() throws IOException {
		assertTrue(FileUtils.isExistingFile(new File("src\\java\\org\\dbgl\\test\\FilesTest.java")));
		assertFalse(FileUtils.isExistingFile(new File("org\\dbgl\\test")));

		File f1 = new File("c:\\Projects\\DBGL\\dosroot\\Inves Spectrum +.scr");
		File f2 = new File("c:\\Projects\\DBGL\\dosroot\\INVESS~1.SCR");

		assertTrue(f1.createNewFile());

		File f1_temp = FileUtils.getUniqueFileName(f1);
		assertTrue(f1.renameTo(f1_temp));

		assertTrue(f2.createNewFile());

		assertTrue(f1_temp.renameTo(f1));

		assertTrue(f1.delete());
		assertTrue(f2.delete());
	}

	@Test
	public void testAreRelated() {
		File f1 = FileUtils.canonicalToDosroot(".");
		File f2 = FileUtils.canonicalToDosroot("ACTION\\KEEN4\\KEEN4.EXE");
		assertTrue(FileUtils.areRelated(f1, f2));
		assertFalse(FileUtils.areRelated(f2, f1));
	}

	@Test
	public void testGetDosRoot() {
		assertEquals("C:\\Projects\\DBGL\\dosroot", FileUtils.getDosRoot());
	}

	@Test
	public void testMakeRelativeTo() {
		File f1 = FileUtils.canonicalToDosroot(".");
		File f2 = FileUtils.canonicalToDosroot("ACTION\\KEEN4\\KEEN4.EXE");
		assertEquals("ACTION\\KEEN4\\KEEN4.EXE", FileUtils.makeRelativeTo(f2, f1).getPath());
	}

	@Test
	public void testIsAbsolute() {
		File f1 = new File("c:\\bla.txt");

		File f2 = new File("\\bla.txt");
		File f3 = new File("c:bla.txt");
		File f4 = new File("bla.txt");

		assertEquals(f1.isAbsolute(), true);
		assertEquals(f2.isAbsolute(), false);
		assertEquals(f3.isAbsolute(), false);
		assertEquals(f4.isAbsolute(), false);
	}

	@Test
	public void testFindDosbox() {
		SearchResult result = PlatformUtils.findDosbox();
		assertEquals(ResultType.COMPLETE, result.result);
		assertEquals("DOSBox 0.74", result.dosbox.getTitle());
		assertEquals("0.74", result.dosbox.getVersion());
		// assertEquals("DOSBox-0.74", result.dosbox.getPath());
	}

	/*
	 * @Test public void testShortPaths() throws IOException { assertEquals("C:\\Users\\ronald\\Desktop\\D-Fend Reloaded\\VirtualHD", new
	 * File("C:\\Users\\ronald\\Desktop\\D-FEND~1\\VIRTUA~1").getCanonicalPath()); }
	 */

	@Test
	public void testListShortFiles() throws IOException {
		Set<ShortFile> files = FileUtils.listShortFiles(new File("."));
		assertTrue(files.size() > 0);

		for (ShortFile shortFile: files) {
			System.out.print(StringUtils.rightPad(shortFile.getFormattedName(), 15));
			System.out.print(shortFile.getFile().getName());
			System.out.println();
		}
	}

	/*
	 * @Test public void testListShortFiles2() throws IOException { Set<ShortFile> files = FileUtils.listShortFiles(new File("c:\\Projects\\DBGL\\dist\\dbgl078b\\dosroot\\games\\alpond2"));
	 * assertTrue(files.size() > 0);
	 * 
	 * for (ShortFile shortFile: files) { System.out.print(StringUtils.rightPad(shortFile.getFormattedName(), 15)); System.out.print(shortFile.getFile().getName()); System.out.println(); } }
	 */
}
