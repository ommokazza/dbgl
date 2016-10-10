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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.util.List;
import org.dbgl.model.SearchEngineImageInformation;
import org.dbgl.model.WebProfile;
import org.dbgl.util.searchengine.TheGamesDBSearchEngine;
import org.junit.Test;


public class TheGamesDBTest {

	@Test
	public void testGetEntries() {
		try {
			List<WebProfile> entries1 = TheGamesDBSearchEngine.getInstance().getEntries("doom", new String[] {"pc"});
			assertEquals(15, entries1.size());

			List<WebProfile> entries2 = TheGamesDBSearchEngine.getInstance().getEntries("mario", new String[] {});
			assertEquals(100, entries2.size());

			List<WebProfile> entries3 = TheGamesDBSearchEngine.getInstance().getEntries("noresultsplease", new String[] {"pc"});
			assertEquals(0, entries3.size());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testGetEntryDetailsDoom() {
		try {
			WebProfile doom = new WebProfile();
			doom.setTitle("Doom");
			doom.setPlatform("PC");
			doom.setYear("1993");
			doom.setUrl("http://thegamesdb.net/api/GetGame.php?id=745");
			doom = TheGamesDBSearchEngine.getInstance().getEntryDetailedInformation(doom);
			assertEquals("id Software", doom.getDeveloperName());
			assertEquals("id Software", doom.getPublisherName());
			assertEquals("Shooter", doom.getGenre());
			assertEquals("1993", doom.getYear());
			assertEquals(88, doom.getRank());
			assertEquals("In Doom, a nameless space marine, gets punitively posted to Mars after assaulting a commanding officer, who ordered his unit to fire upon "
					+ "civilians. The Martian marine base acts as security for the Union Aerospace Corporation UAC, a multi-planetary conglomerate, which is performing "
					+ "secret experiments with teleportation by creating gateways between the two moons of Mars, Phobos and Deimos. Suddenly, one of these UAC "
					+ "experiments goes horribly wrong; computer systems on Phobos malfunction, Deimos disappears entirely and \"something fragging evil\" starts "
					+ "pouring out of the gateways, killing or possessing all UAC personnel! \nResponding to a frantic distress call from the overrun scientists, "
					+ "the Martian marine unit is quickly sent to Phobos to investigate, where you, the space marine, are left to guard the hangar with only a pistol "
					+ "while the rest of the group proceeds inside to discover their worst nightmare. As you advance further, terrifying screams echo through the vast "
					+ "halls, followed by a disturbing silence ... it seems, all your buddies are dead and you're all on your own now - fight back, exterminate every "
					+ "evil creature and get your ticket back home to earth!",
				doom.getNotes());

			SearchEngineImageInformation[] images = TheGamesDBSearchEngine.getInstance().getEntryImages(doom, Integer.MAX_VALUE, Integer.MAX_VALUE, true);
			assertEquals(2, images.length);

			images = TheGamesDBSearchEngine.getInstance().getEntryImages(doom, Integer.MAX_VALUE, Integer.MAX_VALUE, false);
			assertEquals(2, images.length);

			assertEquals("back", images[0].description);
			assertEquals("front", images[1].description);

			images = TheGamesDBSearchEngine.getInstance().getEntryImages(doom, 2, 2, true);
			assertEquals(2, images.length);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testGetEntryDetailsZelda() {
		try {
			WebProfile zelda = new WebProfile();
			zelda.setTitle("The Legend of Zelda");
			zelda.setPlatform("Nintendo Entertainment System (NES)");
			zelda.setYear("1986");
			zelda.setUrl("http://thegamesdb.net/api/GetGame.php?id=113");
			zelda = TheGamesDBSearchEngine.getInstance().getEntryDetailedInformation(zelda);
			assertEquals("Nintendo", zelda.getDeveloperName());
			assertEquals("Nintendo", zelda.getPublisherName());
			assertEquals("Action, Adventure", zelda.getGenre());
			assertEquals("1986", zelda.getYear());
			assertEquals(74, zelda.getRank());
			assertTrue(zelda.getNotes().startsWith("Welcome to the Legend of Zelda."));

			SearchEngineImageInformation[] images = TheGamesDBSearchEngine.getInstance().getEntryImages(zelda, Integer.MAX_VALUE, Integer.MAX_VALUE, true);
			assertEquals(7, images.length);

			assertEquals("back", images[0].description);
			assertEquals("front", images[1].description);
			assertEquals("", images[2].description);
			assertEquals("", images[4].description);
			assertEquals("", images[5].description);
			assertEquals("", images[6].description);

			images = TheGamesDBSearchEngine.getInstance().getEntryImages(zelda, 1, 2, false);
			assertEquals(3, images.length);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
