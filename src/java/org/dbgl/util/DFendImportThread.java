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
package org.dbgl.util;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;
import org.dbgl.model.DosboxVersion;
import org.dbgl.model.KeyValuePair;
import org.dbgl.model.Profile;
import org.dbgl.model.conf.Conf;


public final class DFendImportThread extends UIThread {

	public static final String[] CUST_STRINGS = new String[] {"", "", "", "", "", "", "", ""};
	public static final int[] CUST_INTS = new int[] {0, 0};
	public static final int[] CUST_IDS = new int[] {0, 0, 0, 0};

	private static final String[] LINKS = new String[] {"", "", "", "", "", "", "", ""};

	private final boolean performCleanup;
	private final DosboxVersion defaultDBVersion;
	private Conf profiles;

	public DFendImportThread(final Text log, final ProgressBar progressBar, final Label status, final File dfendProfilesFile, final boolean performCleanup, final DosboxVersion defaultDBVersion)
			throws IOException, SQLException {
		super(log, progressBar, status);

		this.performCleanup = performCleanup;
		this.defaultDBVersion = defaultDBVersion;

		profiles = new Conf(dfendProfilesFile, ps);
		objects = new ArrayList<String>(profiles.getSettings().getAllSectionNames());
		progressBar.setMaximum(objects.size());
	}

	public void doFancyStuff(Object obj, StringBuffer messageLog) throws IOException, SQLException {
		String title = (String)obj;
		displayTitle(settings.msg("dialog.dfendimport.importing", new Object[] {title}));

		String profFile = profiles.getSettings().getValue(title, "prof");
		String confFile = profiles.getSettings().getValue(title, "conf");
		boolean favorite = profiles.getSettings().getValue(title, "fav").equals("1");

		Conf dfendExtra = new Conf(new File(profFile), ps);
		String setup = dfendExtra.getSettings().getValue("Extra", "setup");
		String setupParams = dfendExtra.getSettings().getValue("Extra", "setupparameters");
		String notes = dfendExtra.getSettings().getValue("ExtraInfo", "notes");
		String dev = dfendExtra.getSettings().getValue("ExtraInfo", "developer");
		String pub = dfendExtra.getSettings().getValue("ExtraInfo", "publisher");
		String gen = dfendExtra.getSettings().getValue("ExtraInfo", "genre");
		String year = dfendExtra.getSettings().getValue("ExtraInfo", "year");
		String status = settings.msg("dialog.dfendimport.defaultprofilestatus");
		String cap = dfendExtra.getSettings().getValue("dosbox", "captures");
		int devId = KeyValuePair.findIdByValue(dbase.readDevelopersList(), dev);
		int publId = KeyValuePair.findIdByValue(dbase.readPublishersList(), pub);
		int genId = KeyValuePair.findIdByValue(dbase.readGenresList(), gen);
		int yrId = KeyValuePair.findIdByValue(dbase.readYearsList(), year);
		int statId = KeyValuePair.findIdByValue(dbase.readStatusList(), status);
		int[] custIDs = CUST_IDS;
		for (int i = 0; i < 4; i++) {
			custIDs[i] = KeyValuePair.findIdByValue(dbase.readCustomList(i), "");
		}

		Profile newProfile = dbase.addOrEditProfile(title, dev, pub, gen, year, status, notes, favorite, new String[] {setup, "", ""}, new String[] {setupParams, "", ""}, devId, publId, genId, yrId,
			statId, defaultDBVersion.getId(), LINKS, LINKS, CUST_STRINGS, CUST_INTS, custIDs, -1);

		Conf dfendProfile = new Conf(new File(confFile), title, newProfile.getId(), defaultDBVersion, ps);

		String dstCap = FileUtils.constructCapturesDir(newProfile.getId());
		String dstCapRelative = FileUtils.constructRelativeCapturesDir(newProfile.getId(), dfendProfile.getConfFile().getParentFile(), dfendProfile.getSettings().detectDosboxVersionGeneration());
		File dstCapAbsolute = FileUtils.canonicalToData(dstCap);
		FileUtils.createDir(dstCapAbsolute);
		FileUtils.copyFiles(new File(cap), dstCapAbsolute);
		dfendProfile.getSettings().setValue("dosbox", "captures", dstCapRelative);

		if (performCleanup) {
			dfendProfile.getSettings().removeSection("directserial");
			dfendProfile.getSettings().removeSection("modem");
			dfendProfile.getSettings().removeSection("ipx");
			dfendProfile.getSettings().removeSection("sdl");
		}

		// The new profile is associated to the Default DOSBox version
		// However, the imported profile is probably still associated to an older DB version
		// Therefore, update the settings to defaultDBVersion
		dfendProfile.alterToDosboxVersionGeneration(dfendProfile);

		dfendProfile.save();

		newProfile = dbase.updateProfileConf(FileUtils.makeRelativeToData(dfendProfile.getConfFile()).getPath(), dstCap, newProfile.getId());

		if (dfendProfile.getAutoexec().isIncomplete()) {
			ps.println(settings.msg("dialog.multiprofile.error.profileincomplete"));
		}
	}

	public String getTitle(Object o) {
		return (String)o;
	}

	public void preFinish() {}
}
