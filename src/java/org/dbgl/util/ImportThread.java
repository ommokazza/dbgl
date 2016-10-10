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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.dbgl.model.DosboxVersion;
import org.dbgl.model.ExpProfile;
import org.dbgl.model.KeyValuePair;
import org.dbgl.model.Profile;
import org.dbgl.model.Constants;
import org.dbgl.model.conf.Conf;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;
import SevenZip.MyRandomAccessFile;
import SevenZip.Archive.IInArchive;
import SevenZip.Archive.SevenZip.Handler;


public final class ImportThread extends UIThread {

	private boolean importCaptures;
	private boolean importMapperfiles;
	private boolean importNativecommands;
	private boolean importGameData;
	private boolean importFullSettings;
	private boolean customValues;
	private String[] customFields;
	private boolean useExistingConf;
	private File zipfile;

	private boolean sevenzip;
	private int[] entryIdsToBeExtracted;
	private Map<Integer, File> sevenzipDstFileMap = new TreeMap<Integer, File>();
	private IInArchive zArchive;

	public ImportThread(Text log, ProgressBar progressBar, Label status, List<ExpProfile> profs, File zipfile, boolean captures, boolean mapperfiles, boolean nativecommands, boolean useExistingConf,
			boolean gamedata, boolean fullSettings, boolean customValues, String[] customFields) throws IOException, SQLException {
		super(log, progressBar, status);

		this.zipfile = zipfile;
		this.importCaptures = captures;
		this.importMapperfiles = mapperfiles;
		this.importNativecommands = nativecommands;
		this.useExistingConf = useExistingConf;
		this.importGameData = gamedata;
		this.importFullSettings = fullSettings;
		this.customValues = customValues;
		this.customFields = customFields != null ? customFields.clone(): null;
		this.sevenzip = zipfile.getPath().toLowerCase().endsWith(FileUtils.ARCHIVES[1]);
		long bytes = 0;
		if (sevenzip) {
			MyRandomAccessFile istream = new MyRandomAccessFile(zipfile.getPath(), "r");
			zArchive = new Handler();
			if (zArchive.Open(istream) != 0)
				throw new IOException(settings.msg("general.error.opensevenzip", new Object[] {zipfile.getPath()}));
			for (int i = 0; i < zArchive.size(); i++)
				bytes += zArchive.getEntry(i).getSize();
		} else {
			for (final ExpProfile ep: profs) {
				if (importCaptures) {
					bytes += FileUtils.extractZipSizeInBytes(zipfile, new File(ep.getCaptures()));
				}
				if (importMapperfiles && (ep.getMapperfile() != null)) {
					bytes += FileUtils.extractZipEntrySizeInBytes(zipfile, PlatformUtils.toArchivePath(ep.getMapperfile()));
				}
				if (importGameData) {
					bytes += FileUtils.extractZipSizeInBytes(zipfile, new File(FileUtils.DOSROOT_DIR, new File(String.valueOf(ep.getImportedId()), ep.getGameDir().getPath()).getPath()));
				}
			}
		}

		objects = profs;
		setTotal((int)(bytes / 1024));
		extensiveLogging = true;
	}

	public void doFancyStuff(Object obj, StringBuffer messageLog) throws IOException, SQLException {
		ExpProfile prof = (ExpProfile)obj;
		displayTitle(settings.msg("dialog.import.importing", new Object[] {prof.getTitle()}));

		int devId = KeyValuePair.findIdByValue(dbase.readDevelopersList(), prof.getDeveloperName());
		int publId = KeyValuePair.findIdByValue(dbase.readPublishersList(), prof.getPublisherName());
		int genId = KeyValuePair.findIdByValue(dbase.readGenresList(), prof.getGenre());
		int yrId = KeyValuePair.findIdByValue(dbase.readYearsList(), prof.getYear());
		int statId = KeyValuePair.findIdByValue(dbase.readStatusList(), prof.getStatus());
		String[] customStrings = DFendImportThread.CUST_STRINGS;
		int[] customInts = DFendImportThread.CUST_INTS;
		int[] custIDs = DFendImportThread.CUST_IDS;
		if (customValues) {
			customStrings = prof.getCustomStrings();
			customInts = prof.getCustomInts();
			for (int i = 0; i < 4; i++) {
				custIDs[i] = KeyValuePair.findIdByValue(dbase.readCustomList(i), prof.getCustomString(i));
			}
		}

		Profile addedProfile = dbase.addOrEditProfile(prof.getTitle(), prof.getDeveloperName(), prof.getPublisherName(), prof.getGenre(), prof.getYear(), prof.getStatus(), prof.getNotes(),
			prof.isDefault(), prof.getSetup(), prof.getSetupParameters(), devId, publId, genId, yrId, statId, prof.getDbversionId(), prof.getLinks(), prof.getLinkTitles(), customStrings, customInts,
			custIDs, -1);

		String newCapturesString = FileUtils.constructCapturesDir(addedProfile.getId());
		File relativeCapturesDirInZip = new File(prof.getCaptures());
		File canonicalCapturesDir = FileUtils.canonicalToData(newCapturesString);
		if (!canonicalCapturesDir.exists()) {
			FileUtils.createDir(canonicalCapturesDir);
			messageLog.append(PREFIX_OK).append(settings.msg("dialog.import.notice.createddir", new Object[] {canonicalCapturesDir})).append('\n');
			if (importCaptures) {
				if (sevenzip) {
					for (int i: FileUtils.findRelatedEntryIds(zArchive, relativeCapturesDirInZip))
						sevenzipDstFileMap.put(i, FileUtils.determineDstSevenzipFile(relativeCapturesDirInZip, canonicalCapturesDir, zArchive.getEntry(i).getName()));
				} else {
					try {
						FileUtils.extractZip(zipfile, relativeCapturesDirInZip, canonicalCapturesDir, this);
						messageLog.append(PREFIX_OK).append(settings.msg("dialog.import.notice.extractedcaptures", new Object[] {canonicalCapturesDir})).append('\n');
					} catch (IOException e) {
						messageLog.append(PREFIX_ERR).append(settings.msg("dialog.import.error.capturesextraction", new Object[] {StringRelatedUtils.toString(e)})).append('\n');
					}
				}
			}
		} else {
			messageLog.append(PREFIX_ERR).append(settings.msg("dialog.import.error.capturesdirexists", new Object[] {canonicalCapturesDir})).append('\n');
		}

		File relativeGameDir = prof.getGameDir();
		File relativeGameDirInZip = new File(FileUtils.DOSROOT_DIR, new File(String.valueOf(prof.getImportedId()), relativeGameDir.getPath()).getPath());
		File canonicalGameDir = FileUtils.canonicalToDosroot(new File(prof.getBaseDir(), prof.getGameDir().getPath()).getPath());
		if (importGameData) {
			if (!canonicalGameDir.exists()) {
				FileUtils.createDir(canonicalGameDir);
				messageLog.append(PREFIX_OK).append(settings.msg("dialog.import.notice.createddir", new Object[] {canonicalGameDir})).append('\n');
				if (sevenzip) {
					for (int i: FileUtils.findRelatedEntryIds(zArchive, relativeGameDirInZip))
						sevenzipDstFileMap.put(i, FileUtils.determineDstSevenzipFile(relativeGameDirInZip, canonicalGameDir, zArchive.getEntry(i).getName()));
				} else {
					try {
						FileUtils.extractZip(zipfile, relativeGameDirInZip, canonicalGameDir, this);
						messageLog.append(PREFIX_OK).append(settings.msg("dialog.import.notice.extractedgamedata", new Object[] {canonicalGameDir})).append('\n');
					} catch (IOException e) {
						throw new IOException(settings.msg("dialog.import.error.gamedataextraction", new Object[] {StringRelatedUtils.toString(e)}), e);
					}
				}
			}
		}

		DosboxVersion assocDBVersion = DosboxVersion.findById(dbversions, prof.getDbversionId());
		String newConfString = null;
		if (useExistingConf && FileUtils.areRelated(new File(FileUtils.getDosRoot()), prof.getCanonicalConfFile()) && FileUtils.isExistingFile(prof.getCanonicalConfFile())) {
			newConfString = prof.getCanonicalConfFile().getPath();
			messageLog.append(PREFIX_OK).append(settings.msg("dialog.import.notice.usingexistingconf", new Object[] {prof.getCanonicalConfFile()})).append('\n');
		} else {
			Conf gameConf = new Conf(prof.getImportedFullConfig(), prof.getImportedIncrConfig(), importFullSettings, zipfile.getPath(), prof, addedProfile.getId(), assocDBVersion, ps);
			newConfString = FileUtils.makeRelativeToData(gameConf.getConfFile()).getPath();
			Profile newProfile = new Profile(addedProfile.getId(), newConfString, newCapturesString, prof);
			if (!newProfile.getCanonicalConfFile().getParentFile().exists()) {
				FileUtils.createDir(newProfile.getCanonicalConfFile().getParentFile());
				messageLog.append(PREFIX_OK).append(settings.msg("dialog.import.notice.createddir", new Object[] {newProfile.getCanonicalConfFile().getParentFile()})).append('\n');
			}
			String dstCapRelative = FileUtils.constructRelativeCapturesDir(addedProfile.getId(), newProfile.getCanonicalConfFile().getParentFile(),
				gameConf.getSettings().detectDosboxVersionGeneration());
			gameConf.getSettings().setValue("dosbox", "captures", dstCapRelative);

			String mapperfileEntry = prof.getMapperfile();
			if (importMapperfiles && (mapperfileEntry != null)) {
				File dstFile = new File(StringUtils.replace(newProfile.getCanonicalConfFile().getPath(), FileUtils.CONF_EXT, FileUtils.MAPPER_EXT));
				gameConf.getSettings().setValue("sdl", "mapperfile", dstFile.getName());
				if (sevenzip) {
					int i = FileUtils.findEntryId(zArchive, PlatformUtils.toArchivePath(mapperfileEntry));
					if (i != -1)
						sevenzipDstFileMap.put(i, dstFile);
				} else {
					try {
						FileUtils.extractZip(zipfile, PlatformUtils.toArchivePath(mapperfileEntry), dstFile, this);
						messageLog.append(PREFIX_OK).append(settings.msg("dialog.import.notice.extractedmapperfile", new Object[] {dstFile})).append('\n');
					} catch (IOException e) {
						messageLog.append(PREFIX_ERR).append(settings.msg("dialog.import.error.mapperfileextraction", new Object[] {StringRelatedUtils.toString(e)})).append('\n');
					}
				}
			}

			gameConf.save();
			messageLog.append(PREFIX_OK).append(settings.msg("dialog.import.notice.createdconf", new Object[] {newProfile.getCanonicalConfFile()})).append('\n');
		}
		addedProfile = dbase.updateProfileConf(newConfString, newCapturesString, addedProfile.getId());

		String[] setup = prof.getSetup();
		for (int i = 0; i < setup.length; i++) {
			if (setup[i].length() > 0)
				setup[i] = FileUtils.prefixAndSanitizeToDosroot(prof.getBaseDir(), new File(setup[i])).getPath();
		}
		String[] links = prof.getLinks();
		for (int i = 0; i < links.length; i++) {
			if (!links[i].equals("") && !links[i].contains("://")) {
				links[i] = FileUtils.makeRelativeToDosroot(FileUtils.canonicalToData(links[i])).getPath();
				links[i] = FileUtils.prefixAndSanitizeToDosroot(prof.getBaseDir(), new File(links[i])).getPath();
				if (!prof.getBaseDir().isAbsolute()) {
					links[i] = FileUtils.DOSROOT_DIR + links[i];
				}
			}
		}
		addedProfile = dbase.updateProfileSetupAndLinks(setup, links, addedProfile.getId());

		if (importNativecommands) {
			dbase.saveNativeCommands(prof.getNativeCommandsList(), addedProfile.getId(), -1);
		}

		messageLog.append(PREFIX_OK).append(
			settings.msg("dialog.import.notice.createddbentry", new Object[] {addedProfile.getId(), newConfString, newCapturesString, assocDBVersion.getTitle()})).append('\n');
	}

	public String getTitle(Object o) {
		return ((ExpProfile)o).getTitle();
	}

	public void preFinish() throws IOException {
		if (customFields != null) {
			for (int i = 0; i < Constants.EDIT_COLUMN_NAMES; i++) {
				if (!customFields[i].equalsIgnoreCase("Custom" + (i + 1))) {
					settings.getSettings().setValue("gui", "custom" + (i + 1), customFields[i]);
				}
			}
		}
		if (sevenzip) {
			entryIdsToBeExtracted = ArrayUtils.toPrimitive(sevenzipDstFileMap.keySet().toArray(new Integer[0]));
			SevenzipExtractFilesCallback extractCallback = new SevenzipExtractFilesCallback(this, zArchive, sevenzipDstFileMap);
			zArchive.Extract(entryIdsToBeExtracted, entryIdsToBeExtracted.length, IInArchive.NExtract_NAskMode_kExtract, extractCallback);
			for (int i = 0; i < entryIdsToBeExtracted.length; i++) {
				int id = entryIdsToBeExtracted[i];
				FileUtils.fileSetLastModified(sevenzipDstFileMap.get(id), zArchive.getEntry(id).getTime());
			}
			zArchive.close();
		}
	}

	public String[] getCustomFields() {
		return customFields != null ? customFields.clone(): null;
	}
}
