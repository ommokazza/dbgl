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
package org.dbgl.model;

import java.io.File;
import java.util.Date;
import java.util.List;
import org.dbgl.util.FileUtils;
import org.dbgl.util.PlatformUtils;


public class DosboxVersion extends KeyTitleDefault implements Comparable<DosboxVersion> {

	public final static String[] SUPP_RELEASES = {"0.63", "0.65", "0.70", "0.71", "0.72", "0.73", "0.74"};
	public static final String LATEST = SUPP_RELEASES[SUPP_RELEASES.length - 1];

	private final String path, conf;
	private final boolean multiConfig, usingCurses;
	private final String parameters;
	private final String version;
	protected GenericStats stats;

	public DosboxVersion(final int id, final String title, final String path, final String conf, final boolean multiConfig, final boolean usingCurses, final boolean isDefault, final String parameters,
			final String version, final Date created, final Date modified, final Date lastrun, final int runs) {
		super(id, title, isDefault);
		this.path = PlatformUtils.pathToNativePath(path);
		this.conf = PlatformUtils.pathToNativePath(conf);
		this.multiConfig = multiConfig;
		this.usingCurses = usingCurses;
		this.parameters = parameters;
		this.version = version;
		this.stats = new GenericStats(created, modified, lastrun, runs);
	}

	public boolean isMultiConfig() {
		return multiConfig;
	}

	public boolean isUsingCurses() {
		return usingCurses;
	}

	public String getPath() {
		return path;
	}

	public File getCanonicalExecutable() {
		return FileUtils.constructCanonicalDBExeLocation(path);
	}

	public String getConf() {
		return conf;
	}

	public File getCanonicalConfFile() {
		return FileUtils.canonicalToDosbox(conf);
	}

	public String getParameters() {
		return parameters;
	}

	public String getVersion() {
		return version;
	}

	public GenericStats getStats() {
		return stats;
	}

	public int findBestMatchId(final List<DosboxVersion> dbversionsList) {
		for (DosboxVersion dbv: dbversionsList) {
			if (distance(dbv) == 0 && getTitle().equals(dbv.getTitle())) {
				return dbv.getId();
			}
		}

		DosboxVersion theDefault = findDefault(dbversionsList);
		if (distance(theDefault) == 0) {
			return theDefault.getId();
		}

		int result = dbversionsList.get(0).getId();
		int distance = distance(dbversionsList.get(0));
		for (DosboxVersion dbv: dbversionsList) {
			if (distance(dbv) < distance) {
				result = dbv.getId();
				distance = distance(dbv);
			}
		}
		return result;
	}

	private int getVersionInt() {
		return Integer.valueOf(version.substring(2));
	}

	private int distance(final DosboxVersion ver) {
		return Math.abs(ver.getVersionInt() - getVersionInt());
	}

	public int hashCode() {
		return super.hashCode();
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DosboxVersion otherDBVersion = (DosboxVersion)obj;
		return (getVersionInt() == otherDBVersion.getVersionInt() && getTitle().equals(otherDBVersion.getTitle()));
	}

	public int compareTo(final DosboxVersion arg0) {
		DosboxVersion comp = (DosboxVersion)arg0;
		int ver1 = this.getVersionInt();
		int ver2 = comp.getVersionInt();
		if (ver1 != ver2) {
			return (ver1 - ver2);
		}
		return this.getTitle().compareTo(comp.getTitle());
	}
}
