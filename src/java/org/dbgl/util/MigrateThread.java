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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;
import org.dbgl.model.DosboxVersion;
import org.dbgl.model.Profile;
import org.dbgl.model.conf.Conf;


public final class MigrateThread extends UIThread {

	private final File from;

	public MigrateThread(final Text log, final ProgressBar progressBar, final Label status, final File from) throws SQLException {
		super(log, progressBar, status);
		this.from = from;

		objects = dbase.readProfilesList(" ORDER BY LOWER(GAM.TITLE)", null);
		this.progressBar.setMaximum(objects.size());
	}

	public void doFancyStuff(Object obj, StringBuffer messageLog) throws IOException, SQLException {
		Profile prof = (Profile)obj;
		displayTitle(settings.msg("dialog.migration.migrating", new Object[] {prof.getTitle()}));

		File file = prof.getCanonicalConfFile();
		DosboxVersion assocDBVersion = DosboxVersion.findById(dbversions, prof.getDbversionId());
		Conf profileConf = new Conf(prof, assocDBVersion, ps);
		profileConf.getAutoexec().migrateToDosroot(from, false);
		profileConf.save();

		String newConfigPath = prof.getConfPathAndFile();
		if (new File(prof.getConfPathAndFile()).isAbsolute()) {
			File newFile = FileUtils.makeRelativeTo(file, from);
			if (!newFile.isAbsolute()) {
				newConfigPath = FileUtils.DOSROOT_DIR + newFile.getPath();
			}
		}
		prof = dbase.updateProfileConf(newConfigPath, prof.getCaptures(), prof.getId());

		String[] setup = prof.getSetup();
		for (int i = 0; i < setup.length; i++) {
			if (setup[i].length() > 0)
				setup[i] = FileUtils.makeRelativeTo(new File(setup[i]), from).getPath();
		}
		String[] links = prof.getLinks();
		for (int i = 0; i < links.length; i++) {
			if (links[i].toLowerCase().startsWith("file://")) {
				links[i] = links[i].substring(7);
			}
			if (!links[i].equals("") && !links[i].contains("://")) {
				File newFile = FileUtils.makeRelativeTo(new File(links[i]), from);
				if (!newFile.isAbsolute() && !newFile.getPath().startsWith(FileUtils.DOSROOT_DIR)) {
					links[i] = FileUtils.DOSROOT_DIR + newFile.getPath();
				}
			}
		}
		prof = dbase.updateProfileSetupAndLinks(setup, links, prof.getId());
	}

	public String getTitle(Object obj) {
		return ((Profile)(obj)).getTitle();
	}

	public void preFinish() {}
}
