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
import org.dbgl.util.searchengine.MobyGamesSearchEngine;
import org.junit.Test;


public class MobyGamesTest {

	@Test
	public void testGetEntries() {
		try {
			List<WebProfile> entries1 = MobyGamesSearchEngine.getInstance().getEntries("doom", new String[] {"dos", "pc booter"});
			assertTrue(entries1.size() > 30);

			List<WebProfile> entries2 = MobyGamesSearchEngine.getInstance().getEntries("mario", new String[] {"dos", "pc booter"});
			assertTrue(entries2.size() > 8);

			List<WebProfile> entries3 = MobyGamesSearchEngine.getInstance().getEntries("noresultsplease", new String[] {"dos", "pc booter"});
			assertEquals(0, entries3.size());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testGetEntryDetailsDoom() {
		try {
			WebProfile doom = new WebProfile();
			doom.setTitle("DOOM");
			doom.setPlatform("DOS");
			doom.setYear("1993");
			doom.setUrl("http://www.mobygames.com/game/dos/doom");
			doom = MobyGamesSearchEngine.getInstance().getEntryDetailedInformation(doom);
			assertEquals("id Software, Inc.", doom.getDeveloperName());
			assertEquals("id Software, Inc.", doom.getPublisherName());
			assertEquals("Action", doom.getGenre());
			assertEquals("1993", doom.getYear());
			assertEquals(88, doom.getRank());
			assertEquals("/game/dos/doom/cover-art/gameCoverId,3907/", doom.getCoreGameCoverUrl());
			assertEquals(
				"The Union Aerospace Corporation has been experimenting with teleportation technology on Mars' moons Phobos and Deimos. After early successes, something goes wrong. It seems the scientists have opened a gateway straight to Hell. Phobos base is overrun with demonic creatures, and the whole of Deimos simply vanishes. A squad of marines is sent to Phobos, but all except one are quickly slaughtered. It falls to the surviving marine to grab some guns and strike back at the demons.\n\n"
						+ "id Software's follow-up to their genre-defining Wolfenstein 3D, DOOM is another first-person 3D shooter: full-on action seen from the space marine's perspective. Like Wolfenstein, the game consists of distinct episodes, playable in any order. The first episode, Knee-Deep in the Dead, takes place in the Phobos base and is freely available as shareware. The full game continues on Deimos in The Shores of Hell and culminates in Inferno, the final episode which takes place in Hell itself (the Sega 32x version lacks this episode).\n\n"
						+ "The basic objective in each level is simply to reach the exit. Since dozens of enemies stand in the way, the only way to get there is through killing them. Switches and buttons must be pressed to advance at certain points and often color-coded locked doors will block the way - matching keycards or skull keys must be found to pass.\n\n"
						+ "The game's engine technology is more advanced than Wolfenstein's, and thus the levels are more varied and complex. The engine simulates different heights (stairs and lifts appear frequently) and different lighting conditions (some rooms are pitch black, others only barely illuminated). There are outdoor areas, pools of radioactive waste that hurt the player, ceilings that come down and crush him, and unlike Wolfenstein's orthogonally aligned corridors, the walls in DOOM can be in any angle to each other. An automap helps in navigating the levels.\n\n"
						+ "Stylistically, the levels begin with a futuristic theme in the military base on Phobos and gradually change to a hellish environment, complete with satanic symbols (pentagrams, upside-down-crosses and portraits of horned demons), hung-up mutilated corpses and the distorted faces of the damned.\n\n"
						+ "DOOM features a large weapon arsenal, with most weapons having both advantages and drawbacks. The starting weapons are the fists and a simple pistol. Also available are a shotgun (high damage, slow reload, not good at distances), a chaingun (high firing rate, but slightly inaccurate in longer bursts) and a plasma rifle (combining a high firing rate and large damage). The rocket launcher also deals out lots of damage, but the explosion causes blast damage and must be used with care in confined areas or it might prove deadly to the player as well as the enemies. Two further weapons in the game are the chainsaw for close-quarter carnage, and the BFG9000 energy gun, that, while taking some practice to fire correctly, can destroy most enemies in a single burst. The different weapons use four different ammunition types (bullets, shells, rockets and energy cells), so collecting the right type for a certain gun is important.\n\n"
						+ "The game drops some of Wolfenstein's arcade-inspired aspects, so there are no extra lives or treasures to be collected for points, but many other power-ups are still available. Medpacks heal damage while armor protects from receiving it in the first place. Backpacks allow more ammunition to be carried, a computer map reveals the whole layout of the level on the automap (including any secret areas), light amplification visors illuminate dark areas and radiation suits allow travel over waste without taking damage. Also available are berserk packs (which radically increase the damage inflicted by the fists) as well as short-time invisibility and invulnerability power-ups.\n\n"
						+ "The enemies to be destroyed include former humans corrupted during the invasion, plus demons in all shapes and sizes: fireball-throwing imps, floating skulls, pink-skinned demons with powerful bite attacks and large one-eyed flying monstrosities called Cacodemons. Each episode ends with a boss battle against one or two especially powerful creatures.\n\n"
						+ "DOOM popularized multiplayer in the genre with two different modes: Cooperative allows players to move through the single-player game together, while Deathmatch is a competitive game type where players blast at each other to collect 'frag' points for a kill and re-spawn in a random location after being killed.\n\n"
						+ "The 3DO and Sega32x ports lack any multiplayer modes, though the other ports retain the DOS versions multiplayer to varying degree. The various console ports all feature simplified levels and omit some levels, enemies and features from the original DOS release. The SNES and Gameboy Advanced versions of the game actually use different engines and hence feature numerous small gameplay differences.",
				doom.getNotes());

			SearchEngineImageInformation[] images = MobyGamesSearchEngine.getInstance().getEntryImages(doom, Integer.MAX_VALUE, Integer.MAX_VALUE, true);
			assertEquals(68, images.length);

			images = MobyGamesSearchEngine.getInstance().getEntryImages(doom, Integer.MAX_VALUE, Integer.MAX_VALUE, false);
			assertEquals(48, images.length);

			assertEquals("Front Cover", images[0].description);
			assertEquals("Front Cover Doom version 1.9", images[1].description);
			assertEquals("Back Cover", images[2].description);
			assertEquals("Media Disk 1/4", images[3].description);

			images = MobyGamesSearchEngine.getInstance().getEntryImages(doom, 2, 2, true);
			assertEquals(4, images.length);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testGetEntryDetailsKingdomOfKroz() {
		try {
			WebProfile kingdomofkroz = new WebProfile();
			kingdomofkroz.setTitle("Kingdom of Kroz");
			kingdomofkroz.setPlatform("DOS");
			kingdomofkroz.setYear("1988");
			kingdomofkroz.setUrl("http://www.mobygames.com/game/dos/kingdom-of-kroz");
			kingdomofkroz = MobyGamesSearchEngine.getInstance().getEntryDetailedInformation(kingdomofkroz);
			assertEquals("Scott Miller", kingdomofkroz.getDeveloperName());
			assertEquals("Softdisk Publishing", kingdomofkroz.getPublisherName());
			assertEquals("Action", kingdomofkroz.getGenre());
			assertEquals("1988", kingdomofkroz.getYear());
			assertEquals(0, kingdomofkroz.getRank());
			assertEquals(null, kingdomofkroz.getCoreGameCoverUrl());
			assertEquals(
				"A text-mode action/puzzle game. You have to get to the exit on each level, which is made more difficult by obstacles such as trees (which can be destroyed) and monsters. Various puzzles (in the form of one-off objects) are also used.",
				kingdomofkroz.getNotes());

			SearchEngineImageInformation[] images = MobyGamesSearchEngine.getInstance().getEntryImages(kingdomofkroz, Integer.MAX_VALUE, Integer.MAX_VALUE, true);
			assertEquals(3, images.length);

			assertEquals("Level 1", images[0].description);
			assertEquals("Instructions", images[1].description);
			assertEquals("High score table", images[2].description);

			images = MobyGamesSearchEngine.getInstance().getEntryImages(kingdomofkroz, Integer.MAX_VALUE, Integer.MAX_VALUE, false);
			assertEquals(3, images.length);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testGetEntryDetailsXargon() {
		try {
			WebProfile xargon = new WebProfile();
			xargon.setTitle("Xargon: The Mystery of the Blue Builders - Beyond Reality");
			xargon.setPlatform("DOS");
			xargon.setYear("1994");
			xargon.setUrl("http://www.mobygames.com/game/dos/xargon-the-mystery-of-the-blue-builders-beyond-reality");
			xargon = MobyGamesSearchEngine.getInstance().getEntryDetailedInformation(xargon);
			assertEquals("Epic MegaGames, Inc.", xargon.getDeveloperName());
			assertEquals("Wiz Technology, Inc.", xargon.getPublisherName());
			assertEquals("Action", xargon.getGenre());
			assertEquals("1994", xargon.getYear());
			assertEquals(0, xargon.getRank());
			assertEquals("/game/dos/xargon-the-mystery-of-the-blue-builders-beyond-reality/cover-art/gameCoverId,256102/", xargon.getCoreGameCoverUrl());
			assertEquals(
				"Beyond Reality is the first episode of three that make up the side-scrolling platformer Xargon. This first episode was freely distributed as shareware with the option to register the other two, but also sold separately.  The company B&N also released each episode separately and commercially under their Monkey Business label. The B&N version can be run directly from the floppy disk by just typing GO.",
				xargon.getNotes());

			SearchEngineImageInformation[] images = MobyGamesSearchEngine.getInstance().getEntryImages(xargon, Integer.MAX_VALUE, Integer.MAX_VALUE, true);
			assertEquals(7, images.length);

			images = MobyGamesSearchEngine.getInstance().getEntryImages(xargon, Integer.MAX_VALUE, Integer.MAX_VALUE, false);
			assertEquals(3, images.length);

			images = MobyGamesSearchEngine.getInstance().getEntryImages(xargon, 1, 1, true);
			assertEquals(1, images.length);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testGetEntryDetailsFinalFury() {
		try {
			WebProfile finalfury = new WebProfile();
			finalfury.setTitle("The Complete Great Naval Battles: The Final Fury");
			finalfury.setPlatform("DOS");
			finalfury.setYear("1996");
			finalfury.setUrl("http://www.mobygames.com/game/dos/complete-great-naval-battles-the-final-fury");
			finalfury = MobyGamesSearchEngine.getInstance().getEntryDetailedInformation(finalfury);
			assertEquals("", finalfury.getDeveloperName());
			assertEquals("Mindscape, Inc., Strategic Simulations, Inc.", finalfury.getPublisherName());
			assertEquals("Simulation, Strategy", finalfury.getGenre());
			assertEquals("1996", finalfury.getYear());
			assertEquals(0, finalfury.getRank());
			assertEquals("/game/dos/complete-great-naval-battles-the-final-fury/cover-art/gameCoverId,305555/", finalfury.getCoreGameCoverUrl());
			assertEquals(
				"The Final Fury is a compilation of all previous titles from the Great Naval Battles series:Great Naval Battles: North Atlantic 1939-1943 with its three add-ons:Great Naval Battles: North Atlantic 1939-1943 - Super Ships of the AtlanticGreat Naval Battles: North Atlantic 1939-1943 - America in the AtlanticGreat Naval Battles: North Atlantic 1939-1943 - Scenario BuilderGreat Naval Battles Vol. II: Guadalcanal 1942-43Great Naval Battles Vol. III: Fury in the Pacific, 1941-1944Great Naval Battles Vol. IV: Burning Steel, 1939-1942\n\nIt also exclusively includes the ultimate part of the series:Great Naval Battles Vol. V: Demise of the Dreadnoughts 1914-18\nwhich is set during the World War I; its scenarios include famous battles between the German Hochseeflotte and the British Royal Navy (battle of Jutland, battle of Coronel, battles of Dogger Bank, battle of the Falkland Islands). A mission editor is also available.",
				finalfury.getNotes());

			SearchEngineImageInformation[] images = MobyGamesSearchEngine.getInstance().getEntryImages(finalfury, Integer.MAX_VALUE, Integer.MAX_VALUE, true);
			assertEquals(6, images.length);

			images = MobyGamesSearchEngine.getInstance().getEntryImages(finalfury, Integer.MAX_VALUE, Integer.MAX_VALUE, false);
			assertEquals(6, images.length);

			images = MobyGamesSearchEngine.getInstance().getEntryImages(finalfury, 1, 1, true);
			assertEquals(1, images.length);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
