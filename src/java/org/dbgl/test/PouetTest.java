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
import java.io.IOException;
import java.util.List;
import org.dbgl.model.SearchEngineImageInformation;
import org.dbgl.model.WebProfile;
import org.dbgl.util.searchengine.PouetSearchEngine;
import org.junit.Test;


public class PouetTest {

	@Test
	public void testGetEntries() {
		try {
			List<WebProfile> entries1 = PouetSearchEngine.getInstance().getEntries("purple", new String[] {"ms-dos", "ms-dos/gus"});
			assertEquals(6, entries1.size());

			List<WebProfile> entries2 = PouetSearchEngine.getInstance().getEntries("second", new String[] {});
			assertEquals(80, entries2.size());

			List<WebProfile> entries3 = PouetSearchEngine.getInstance().getEntries("noresultsplease", new String[] {"ms-dos", "ms-dos/gus"});
			assertEquals(0, entries3.size());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testGetEntryDetailsTransgression() {
		try {
			List<WebProfile> entries = PouetSearchEngine.getInstance().getEntries("Transgression 2", new String[] {"ms-dos", "ms-dos/gus"});
			assertEquals(1, entries.size());

			WebProfile transgression = entries.get(0);
			transgression = PouetSearchEngine.getInstance().getEntryDetailedInformation(transgression);
			assertEquals("Mfx", transgression.getDeveloperName());
			assertEquals("", transgression.getPublisherName());
			assertEquals("64k", transgression.getGenre());
			assertEquals("1996", transgression.getYear());
			assertEquals(94, transgression.getRank());
			assertEquals("", transgression.getNotes());

			SearchEngineImageInformation[] images = PouetSearchEngine.getInstance().getEntryImages(transgression, Integer.MAX_VALUE, Integer.MAX_VALUE, true);
			assertEquals(5, images.length);

			images = PouetSearchEngine.getInstance().getEntryImages(transgression, Integer.MAX_VALUE, Integer.MAX_VALUE, false);
			assertEquals(5, images.length);

			for (SearchEngineImageInformation img: images)
				assertEquals("pouet", img.description);

			images = PouetSearchEngine.getInstance().getEntryImages(transgression, 2, 2, true);
			assertEquals(2, images.length);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testGetEntryDetailsPlasma() {
		try {
			List<WebProfile> entries = PouetSearchEngine.getInstance().getEntries("plasma", new String[] {"ms-dos", "ms-dos/gus"});
			assertTrue(entries.size() > 30);

			entries = PouetSearchEngine.getInstance().getEntries("Wired Plasma", new String[] {"ms-dos", "ms-dos/gus"});
			assertEquals(1, entries.size());
			WebProfile wiredPlasma = entries.get(0);

			wiredPlasma = PouetSearchEngine.getInstance().getEntryDetailedInformation(wiredPlasma);
			assertEquals("Hypernova", wiredPlasma.getDeveloperName());
			assertEquals("", wiredPlasma.getPublisherName());
			assertEquals("4k", wiredPlasma.getGenre());
			assertEquals("1994", wiredPlasma.getYear());
			assertEquals(60, wiredPlasma.getRank());
			assertEquals("", wiredPlasma.getNotes());

			SearchEngineImageInformation[] images = PouetSearchEngine.getInstance().getEntryImages(wiredPlasma, Integer.MAX_VALUE, Integer.MAX_VALUE, true);
			assertEquals(1, images.length);

			images = PouetSearchEngine.getInstance().getEntryImages(wiredPlasma, 1, 2, false);
			assertEquals(1, images.length);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
