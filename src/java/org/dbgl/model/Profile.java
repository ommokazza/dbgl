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
import java.net.MalformedURLException;
import java.util.Date;
import org.dbgl.model.conf.Conf;
import org.dbgl.model.conf.Settings;
import org.dbgl.util.FileUtils;
import org.dbgl.util.PlatformUtils;


public class Profile extends KeyTitleDefault {

	protected String developerName, publisherName, genre, year, status, notes, confPathAndFile, captures;
	protected int dbversionId;
	protected String[] setup, setupParams, link, linkTitle, customString;
	protected int[] customInt;
	protected ProfileStats stats;

	protected Profile(final String title, final boolean favorite) {
		super(-1, title, favorite);
		this.stats = new ProfileStats(null, null, null, null, 0, 0);
	}

	public Profile(final int id, final String title, final String devName, final String publName, final String genre, final String year, final String status, final String notes,
			final Boolean favorite, final String[] setup, final String[] setupParams, final String confPathAndFile, final String captures, final int dbversionId, final String[] links,
			final String[] linkTitles, final String[] customStrings, final int[] customInts, Date created, Date modified, Date lastrun, Date lastsetup, int runs, int setups) {
		super(id, title, favorite);
		this.developerName = devName;
		this.publisherName = publName;
		this.genre = genre;
		this.year = year;
		this.status = status;
		this.notes = notes;
		this.setup = new String[setup.length];
		for (int i = 0; i < setup.length; i++) {
			this.setup[i] = PlatformUtils.pathToNativePath(setup[i]);
		}
		this.setupParams = setupParams.clone();
		this.confPathAndFile = PlatformUtils.pathToNativePath(confPathAndFile);
		this.captures = PlatformUtils.pathToNativePath(captures);
		this.dbversionId = dbversionId;
		this.link = new String[links.length];
		for (int i = 0; i < links.length; i++) {
			this.link[i] = PlatformUtils.pathToNativePath(links[i]);
		}
		this.linkTitle = linkTitles.clone();
		this.customString = customStrings.clone();
		this.customInt = customInts.clone();
		this.stats = new ProfileStats(created, modified, lastrun, lastsetup, runs, setups);
	}

	public Profile(final int id, final String confFile, final String captures, final Profile prof) {
		this(id, prof.getTitle(), prof.getDeveloperName(), prof.getPublisherName(), prof.getGenre(), prof.getYear(), prof.getStatus(), prof.getNotes(), prof.isDefault(), prof.getSetup(),
				prof.getSetupParameters(), confFile, captures, prof.getDbversionId(), prof.getLinks(), prof.getLinkTitles(), prof.getCustomStrings(), prof.getCustomInts(), prof.stats.created,
				prof.stats.modified, prof.stats.lastrun, prof.stats.lastsetup, prof.stats.runs, prof.stats.setups);
	}

	private static String test(String s1, String s2) {
		return s1.equals(s2) ? s1: Conf.CONFLICTING_STRING_SETTING;
	}

	private static Boolean test(Boolean b1, Boolean b2) {
		return b1 == b2 ? b1: Conf.CONFLICTING_BOOL_SETTING;
	}

	private static int test(int i1, int i2) {
		return i1 == i2 ? i1: Conf.CONFLICTING_INT_SETTING;
	}

	private static String[] test(String[] s1, String[] s2) {
		String[] result = new String[s1.length];
		for (int i = 0; i < s1.length; i++) {
			result[i] = test(s1[i], s2[i]);
		}
		return result;
	}

	private static int[] test(int[] i1, int[] i2) {
		int[] result = new int[i1.length];
		for (int i = 0; i < i1.length; i++) {
			result[i] = test(i1[i], i2[i]);
		}
		return result;
	}

	private static String either(String s1, String s2) {
		return s2 == null ? s1: s2;
	}

	private static Boolean either(Boolean b1, Boolean b2) {
		return b2 == null ? b1: b2;
	}

	private static int either(int i1, int i2) {
		return i2 == -1 ? i1: i2;
	}

	private static String[] either(String[] s1, String[] s2) {
		String[] result = new String[s1.length];
		for (int i = 0; i < s1.length; i++) {
			result[i] = either(s1[i], s2[i]);
		}
		return result;
	}

	private static int[] either(int[] i1, int[] i2) {
		int[] result = new int[i1.length];
		for (int i = 0; i < i1.length; i++) {
			result[i] = either(i1[i], i2[i]);
		}
		return result;
	}

	public Profile(final Profile prof1, final Profile prof2) {
		this(-1, test(prof1.getTitle(), prof2.getTitle()), test(prof1.getDeveloperName(), prof2.getDeveloperName()), test(prof1.getPublisherName(), prof2.getPublisherName()),
				test(prof1.getGenre(), prof2.getGenre()), test(prof1.getYear(), prof2.getYear()), test(prof1.getStatus(), prof2.getStatus()), test(prof1.getNotes(), prof2.getNotes()),
				test(prof1.isDefault(), prof2.isDefault()), test(prof1.getSetup(), prof2.getSetup()), test(prof1.getSetupParameters(), prof2.getSetupParameters()),
				test(prof1.getConfPathAndFile(), prof2.getConfPathAndFile()), test(prof1.getCaptures(), prof2.getCaptures()), test(prof1.getDbversionId(), prof2.getDbversionId()),
				test(prof1.getLinks(), prof2.getLinks()), test(prof1.getLinkTitles(), prof2.getLinkTitles()), test(prof1.getCustomStrings(), prof2.getCustomStrings()),
				test(prof1.getCustomInts(), prof2.getCustomInts()), null, null, null, null, 0, 0);
	}

	public Profile(final Profile prof1, final Profile prof2, boolean bogus) {
		this(prof1.getId(), either(prof1.getTitle(), prof2.getTitle()), either(prof1.getDeveloperName(), prof2.getDeveloperName()), either(prof1.getPublisherName(), prof2.getPublisherName()),
				either(prof1.getGenre(), prof2.getGenre()), either(prof1.getYear(), prof2.getYear()), either(prof1.getStatus(), prof2.getStatus()), either(prof1.getNotes(), prof2.getNotes()),
				either(prof1.isDefault(), prof2.isDefault()), either(prof1.getSetup(), prof2.getSetup()), either(prof1.getSetupParameters(), prof2.getSetupParameters()),
				either(prof1.getConfPathAndFile(), prof2.getConfPathAndFile()), either(prof1.getCaptures(), prof2.getCaptures()), either(prof1.getDbversionId(), prof2.getDbversionId()),
				either(prof1.getLinks(), prof2.getLinks()), either(prof1.getLinkTitles(), prof2.getLinkTitles()), either(prof1.getCustomStrings(), prof2.getCustomStrings()),
				either(prof1.getCustomInts(), prof2.getCustomInts()), null, null, null, null, 0, 0);
	}

	public String getDeveloperName() {
		return developerName;
	}

	public String getGenre() {
		return genre;
	}

	public String getPublisherName() {
		return publisherName;
	}

	public String getYear() {
		return year;
	}

	public int getDbversionId() {
		return dbversionId;
	}

	public boolean hasSetup(final int index) {
		return !"".equals(setup[index]);
	}

	public String hasSetupString() {
		return hasSetup(0) ? Settings.getInstance().msg("general.yes"): Settings.getInstance().msg("general.no");
	}

	public String[] getLinks() {
		return link.clone();
	}

	public String getLink(final int index) {
		return link[index];
	}

	public String getLinkTitle(final int index) {
		return linkTitle[index];
	}

	public String[] getLinkTitles() {
		return linkTitle.clone();
	}

	public String getLinkAsUrl(final int index) {
		String res = link[index];
		if ((res != null) && res.length() > 0 && res.indexOf("://") == -1) {
			try {
				res = FileUtils.getUrlFromFile(FileUtils.canonicalToData(res));
			} catch (MalformedURLException e1) {
				// nothing we can do, but to keep the current url
			}
		}
		return res;
	}

	public String getNotes() {
		return notes;
	}

	public String[] getSetup() {
		return setup.clone();
	}

	public String[] getSetupParameters() {
		return setupParams.clone();
	}

	public String getSetup(final int index) {
		return setup[index];
	}

	public String getSetupParameters(final int index) {
		return setupParams[index];
	}

	public String getStatus() {
		return status;
	}

	public String getConfPathAndFile() {
		return confPathAndFile;
	}

	public File getCanonicalConfFile() {
		return FileUtils.canonicalToData(confPathAndFile);
	}

	public String getConfFileAsUrl() {
		try {
			return FileUtils.getUrlFromFile(getCanonicalConfFile());
		} catch (MalformedURLException e1) {
			return getCanonicalConfFile().getPath();
		}
	}

	public String getCaptures() {
		return captures;
	}

	public File getCanonicalCaptures() {
		return FileUtils.canonicalToData(captures);
	}

	public String getCapturesAsUrl() {
		try {
			return FileUtils.getUrlFromFile(getCanonicalCaptures());
		} catch (MalformedURLException e1) {
			return getCanonicalCaptures().getPath();
		}
	}

	public String getCustomString(final int index) {
		return customString[index];
	}

	public String[] getCustomStrings() {
		return customString.clone();
	}

	public int getCustomInt(final int index) {
		return customInt[index];
	}

	public int[] getCustomInts() {
		return customInt.clone();
	}

	public ProfileStats getStats() {
		return stats;
	}
}
