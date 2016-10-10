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
import java.io.IOException;
import java.util.List;
import org.dbgl.model.SearchEngineImageInformation;
import org.dbgl.model.WebProfile;
import org.dbgl.util.searchengine.HotudSearchEngine;
import org.junit.Test;


public class HotudTest {

	@Test
	public void testGetEntries() {
		try {
			List<WebProfile> entries1 = HotudSearchEngine.getInstance().getEntries("doom", new String[] {"dos"});
			assertEquals(17, entries1.size());

			List<WebProfile> entries2 = HotudSearchEngine.getInstance().getEntries("mario", new String[] {});
			assertEquals(12, entries2.size());

			List<WebProfile> entries3 = HotudSearchEngine.getInstance().getEntries("noresultsplease", new String[] {"dos"});
			assertEquals(0, entries3.size());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testGetEntryDetailsMutantSpaceBats() {
		try {
			WebProfile mutantSpaceBats = new WebProfile();
			mutantSpaceBats.setTitle("Invasion of the Mutant Space Bats of Doom");
			mutantSpaceBats.setPlatform("dos");
			mutantSpaceBats.setYear("1995");
			mutantSpaceBats.setUrl("http://hotud.org/component/content/article/43-content/games/action/24511-sp-634101148");
			mutantSpaceBats = HotudSearchEngine.getInstance().getEntryDetailedInformation(mutantSpaceBats);
			assertEquals("Pop Software", mutantSpaceBats.getDeveloperName());
			assertEquals("Pop Software", mutantSpaceBats.getPublisherName());
			assertEquals("Action", mutantSpaceBats.getGenre());
			assertEquals("1995", mutantSpaceBats.getYear());
			assertEquals(0, mutantSpaceBats.getRank());
			assertEquals("Invasion of the Mutant Space Bats of Doom and the sequel Return of the Mutant Space Bats of Doom are two rare 'retro'-style shareware shooters. "
					+ "From description at MobyGames: \"[Invasion] consists of 66 stages, arranged in some sort of pattern (for example, every type of bats you encounter "
					+ "takes 3 stages to pass, each of three introducing new challenges). Once every several bats you kill you get one of four bonuses - purple gems, 6 "
					+ "of which grant you another life, a green gem which makes your missiles faster, a red one which gives you additional missiles per round, or a white "
					+ "gem which grants you a time-limited \"hyper\" mode. [In the sequel,] the mutant space bats of doom return yet again to invade earth. Only you stand "
					+ "in their way of achieving their nefarious goals. As usual collect the different colored gems that can increase your ship's firepower, slow down the "
					+ "enemies, or add to the number of lives. In the bonus stage you turn into a bat and fly around collecting the maximum amount of gems to increase the "
					+ "chances of success in battle.\" \nBoth games are fun \"old school\" shooters that offer solid gameplay despite outdated graphics and cliche plot, as "
					+ "well as 'cute' enemies. Pop Software struck the right difficulty balance - the game is always challenging, but never frustrating. Neither of them "
					+ "matches the quality of the best shareware shooters (which must go to Raptor and Tyrian, no doubt), but they are fun enough to keep you occupied for "
					+ "minutes at a time. Worth a look. After selling the game as shareware by mail-order in late 1990s, Pop Software resurfaced briefly in 2001 to sell "
					+ "both games via PayPal on-line, but they have disappeared again since 2002. Download the registered version below :)",
				mutantSpaceBats.getNotes());

			SearchEngineImageInformation[] images = HotudSearchEngine.getInstance().getEntryImages(mutantSpaceBats, Integer.MAX_VALUE, Integer.MAX_VALUE, true);
			assertEquals(0, images.length);

			images = HotudSearchEngine.getInstance().getEntryImages(mutantSpaceBats, Integer.MAX_VALUE, Integer.MAX_VALUE, false);
			assertEquals(0, images.length);

			images = HotudSearchEngine.getInstance().getEntryImages(mutantSpaceBats, 2, 2, true);
			assertEquals(0, images.length);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testGetEntryDetailsZeliard() {
		try {
			WebProfile zeliard = new WebProfile();
			zeliard.setTitle("Zeliard");
			zeliard.setPlatform("dos");
			zeliard.setYear("1993");
			zeliard.setUrl("http://hotud.org/component/content/article/43-content/games/action/20995-sp-1465409910");
			zeliard = HotudSearchEngine.getInstance().getEntryDetailedInformation(zeliard);
			assertEquals("Game Arts", zeliard.getDeveloperName());
			assertEquals("Sierra On-Line", zeliard.getPublisherName());
			assertEquals("Action", zeliard.getGenre());
			assertEquals("1993", zeliard.getYear());
			assertEquals(0, zeliard.getRank());
			assertEquals("Amidst the flurry of imported games that resulted from Sierra's deal with Japanese publisher Gamearts are Sorcerian and "
					+ "Zeliard, two quirky action/RPG titles with strong \"console\" feel. Definitely \"light\" games in that there is few statistics (although characters do "
					+ "gain levels), and the arcade-style platform action may not be every RPGer's cup of tea. Still, interesting plots, spells, and lots of \"secrets\" make "
					+ "these games above average despite chunky graphics. \nAlthough both games are RPG with heavy arcade elements, Sorcerian has a stronger RPG flavor because "
					+ "you get to control a four-character party, comprising the traditional retinue of priest, fighter, and wizard, each of whom have access to unique "
					+ "weapons and/or spells. Zeliard, on the other hand, is a solo-player RPG with much less emphasis on character development than nonstop platform action.",
				zeliard.getNotes());

			SearchEngineImageInformation[] images = HotudSearchEngine.getInstance().getEntryImages(zeliard, Integer.MAX_VALUE, Integer.MAX_VALUE, true);
			assertEquals(0, images.length);

			images = HotudSearchEngine.getInstance().getEntryImages(zeliard, 1, 2, false);
			assertEquals(0, images.length);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
