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

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;
import org.dbgl.model.DosboxVersion;
import org.dbgl.model.ExpProfile;
import org.dbgl.model.NativeCommand;
import org.dbgl.model.Profile;
import org.dbgl.model.conf.Conf;


public final class LoaderThread extends UIThread {

	private Conf targetDBConf = null;
	private boolean combine;
	private ExpProfile multiProfileCombined = null;
	private List<ExpProfile> result = new ArrayList<ExpProfile>();
	private int index;

	public LoaderThread(final Text log, final ProgressBar progressBar, final Label status, List<Profile> profs, boolean combine) throws SQLException {
		super(log, progressBar, status);

		objects = profs;
		this.combine = combine;
		this.index = 0;
		this.progressBar.setMaximum(objects.size());
	}

	public void doFancyStuff(Object obj, StringBuffer messageLog) throws IOException {
		Profile profile = (Profile)obj;
		displayTitle(settings.msg("dialog.profileloader.reading", new Object[] {profile.getTitle()}));

		List<NativeCommand> nativeCommands = null;
		try {
			nativeCommands = dbase.readNativeCommandsList(profile.getId(), -1);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		Conf conf = new Conf(profile, DosboxVersion.findById(dbversions, profile.getDbversionId()), ps);
		if (conf.getAutoexec().isIncomplete()) {
			throw new IOException(settings.msg("dialog.multiprofile.error.profileincomplete"));
		} else {
			if (combine) {
				ExpProfile expProfile = new ExpProfile(profile.getId(), conf, FileUtils.makeRelativeToDosroot(conf.getAutoexec().getCanonicalMainDir()), nativeCommands, profile);
				if (result.isEmpty()) {
					multiProfileCombined = expProfile;
					targetDBConf = Conf.extractDBVersionConf(multiProfileCombined.getConf());
					result.add(expProfile);
				} else {
					Conf c = new Conf(expProfile.getConf());
					boolean same = true;
					if (multiProfileCombined.getDbversionId() != expProfile.getDbversionId()) {
						c.alterToDosboxVersionGeneration(targetDBConf);
						same = false;
					}
					c = new Conf(multiProfileCombined.getConf(), c, same);
					multiProfileCombined = new ExpProfile(multiProfileCombined, expProfile, c);
					if (bos.size() == 0) {
						result.add(expProfile);
					}
				}
			} else {
				result.add(new ExpProfile(index++, conf, FileUtils.makeRelativeToDosroot(conf.getAutoexec().getCanonicalMainDir()), nativeCommands, profile));
			}
		}
	}

	public String getTitle(Object obj) {
		return ((Profile)(obj)).getTitle();
	}

	public void preFinish() {}

	public List<ExpProfile> getResult() {
		return result;
	}

	public ExpProfile getMultiProfileCombined() {
		return multiProfileCombined;
	}
}
