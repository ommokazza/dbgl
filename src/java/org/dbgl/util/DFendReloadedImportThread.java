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
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.dbgl.model.DosboxVersion;
import org.dbgl.model.KeyValuePair;
import org.dbgl.model.Profile;
import org.dbgl.model.conf.Conf;


public final class DFendReloadedImportThread extends UIThread {

	private static final int[] CUST_INTS = new int[] {0, 0};
	private static final int[] CUST_IDS = new int[] {0, 0, 0, 0};

	private final boolean performCleanup;
	private final DosboxVersion defaultDBVersion;
	private File dfendPath, profsPath, confsPath;

	public DFendReloadedImportThread(final Text log, final ProgressBar progressBar, final Label status, final File dfendPath, final File confsPath, final boolean performCleanup,
			final DosboxVersion defaultDBVersion) throws IOException, SQLException {
		super(log, progressBar, status);

		this.performCleanup = performCleanup;
		this.defaultDBVersion = defaultDBVersion;

		this.dfendPath = dfendPath;
		this.profsPath = new File(this.dfendPath, "Confs");

		File settingsPath = new File(this.dfendPath, "Settings");
		File settingsFile = new File(settingsPath, "DFend.ini");
		Conf dfendSettings = new Conf(settingsFile, ps);
		String defLoc = dfendSettings.getSettings().getValue("ProgramSets", "defloc");
		if (StringUtils.isNotBlank(defLoc)) {
			this.dfendPath = new File(defLoc);
		}

		this.confsPath = confsPath;
		objects = new ArrayList<File>(org.apache.commons.io.FileUtils.listFiles(profsPath, new String[] {"prof"}, false));
		progressBar.setMaximum(objects.size());
	}

	public void doFancyStuff(Object obj, StringBuffer messageLog) throws IOException, SQLException {
		File profFile = (File)obj;

		Conf dfendExtra = new Conf(profFile, ps);
		String title = dfendExtra.getSettings().getValue("ExtraInfo", "name");

		displayTitle(settings.msg("dialog.dfendimport.importing", new Object[] {title}));

		boolean favorite = dfendExtra.getSettings().getValue("ExtraInfo", "favorite").equals("1");
		String setup = dfendExtra.getSettings().getValue("Extra", "setup");
		if (StringUtils.isNotEmpty(setup)) {
			setup = FileUtils.canonical(new File(dfendPath, dfendExtra.getSettings().getValue("Extra", "setup")).getPath()).getPath();
		}
		String setupParams = dfendExtra.getSettings().getValue("Extra", "setupparameters");
		String notes = fixCRLF(dfendExtra.getSettings().getValue("ExtraInfo", "notes"));
		String dev = dfendExtra.getSettings().getValue("ExtraInfo", "developer");
		String pub = dfendExtra.getSettings().getValue("ExtraInfo", "publisher");
		String gen = dfendExtra.getSettings().getValue("ExtraInfo", "genre");
		String year = dfendExtra.getSettings().getValue("ExtraInfo", "year");
		String language = dfendExtra.getSettings().getValue("ExtraInfo", "language");
		String userInfo = dfendExtra.getSettings().getValue("ExtraInfo", "userinfo");
		if (StringUtils.isNotEmpty(userInfo)) {
			userInfo = StringUtils.join(StringUtils.split(fixCRLF(userInfo), "\n"), ", ");
		}
		String status = settings.msg("dialog.dfendimport.defaultprofilestatus");
		int devId = KeyValuePair.findIdByValue(dbase.readDevelopersList(), dev);
		int publId = KeyValuePair.findIdByValue(dbase.readPublishersList(), pub);
		int genId = KeyValuePair.findIdByValue(dbase.readGenresList(), gen);
		int yrId = KeyValuePair.findIdByValue(dbase.readYearsList(), year);
		int statId = KeyValuePair.findIdByValue(dbase.readStatusList(), status);

		String[] customStrings = new String[] {language, "", "", "", userInfo, "", "", ""};

		int[] custIDs = CUST_IDS;
		for (int i = 0; i < 4; i++) {
			custIDs[i] = KeyValuePair.findIdByValue(dbase.readCustomList(i), customStrings[i]);
		}

		String[] links = {fixWWW(dfendExtra.getSettings().getValue("ExtraInfo", "www")), fixWWW(dfendExtra.getSettings().getValue("ExtraInfo", "www2")),
				fixWWW(dfendExtra.getSettings().getValue("ExtraInfo", "www3")), fixWWW(dfendExtra.getSettings().getValue("ExtraInfo", "www4")),
				fixWWW(dfendExtra.getSettings().getValue("ExtraInfo", "www5")), fixWWW(dfendExtra.getSettings().getValue("ExtraInfo", "www6")),
				fixWWW(dfendExtra.getSettings().getValue("ExtraInfo", "www7")), fixWWW(dfendExtra.getSettings().getValue("ExtraInfo", "www8"))};
		String[] linkTitles = {dfendExtra.getSettings().getValue("ExtraInfo", "wwwname"), dfendExtra.getSettings().getValue("ExtraInfo", "www2name"),
				dfendExtra.getSettings().getValue("ExtraInfo", "www3name"), dfendExtra.getSettings().getValue("ExtraInfo", "www4name"), dfendExtra.getSettings().getValue("ExtraInfo", "www5name"),
				dfendExtra.getSettings().getValue("ExtraInfo", "www6name"), dfendExtra.getSettings().getValue("ExtraInfo", "www7name"), dfendExtra.getSettings().getValue("ExtraInfo", "www8name")};

		Profile newProfile = dbase.addOrEditProfile(title, dev, pub, gen, year, status, notes, favorite, new String[] {setup, "", ""}, new String[] {setupParams, "", ""}, devId, publId, genId, yrId,
			statId, defaultDBVersion.getId(), links, linkTitles, customStrings, CUST_INTS, custIDs, -1);

		Conf dfendProfile = new Conf(new File(confsPath, FilenameUtils.removeExtension(profFile.getName()) + FileUtils.CONF_EXT), title, newProfile.getId(), defaultDBVersion, ps);

		String cap = dfendProfile.getSettings().getValue("dosbox", "captures");

		String dstCap = FileUtils.constructCapturesDir(newProfile.getId());
		String dstCapRelative = FileUtils.constructRelativeCapturesDir(newProfile.getId(), dfendProfile.getConfFile().getParentFile(), dfendProfile.getSettings().detectDosboxVersionGeneration());
		File dstCapAbsolute = FileUtils.canonicalToData(dstCap);
		FileUtils.createDir(dstCapAbsolute);
		FileUtils.copyFiles(new File(cap), dstCapAbsolute);
		dfendProfile.getSettings().setValue("dosbox", "captures", dstCapRelative);

		if (performCleanup) {
			dfendProfile.getSettings().removeSection("joystick");
			dfendProfile.getSettings().removeSection("sdl");
		}

		// The new profile is associated to the Default DOSBox version
		// However, the imported profile may be associated to another DB version
		// Therefore, update the settings to defaultDBVersion
		dfendProfile.alterToDosboxVersionGeneration(dfendProfile);

		dfendProfile.save();

		newProfile = dbase.updateProfileConf(FileUtils.makeRelativeToData(dfendProfile.getConfFile()).getPath(), dstCap, newProfile.getId());

		if (dfendProfile.getAutoexec().isIncomplete()) {
			ps.println(settings.msg("dialog.multiprofile.error.profileincomplete"));
		}
	}

	private String fixCRLF(final String s) {
		return StringUtils.replace(StringUtils.replace(s, "[13][10]", "\n"), "[13]", "").trim();
	}

	private String fixWWW(final String s) {
		if (StringUtils.isNotEmpty(s) && !s.toLowerCase().startsWith("http://") && !s.toLowerCase().startsWith("https://"))
			return "http://" + s;
		return s;
	}

	public String getTitle(Object o) {
		return ((File)o).getName();
	}

	public void preFinish() {}
}
