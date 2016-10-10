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
package org.dbgl.model.conf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.dbgl.exception.DrivelettersExhaustedException;
import org.dbgl.exception.InvalidHostfileException;
import org.dbgl.exception.InvalidMountstringException;
import org.dbgl.model.DosboxVersion;
import org.dbgl.model.ExpProfile;
import org.dbgl.model.Mount;
import org.dbgl.model.Mount.MountingType;
import org.dbgl.model.Profile;
import org.dbgl.model.Template;
import org.dbgl.util.FileUtils;


public class Conf {

	public final static String CONFLICTING_STRING_SETTING = "CONFLICTING_SETTING";
	public final static int CONFLICTING_INT_SETTING = Integer.MIN_VALUE;
	public final static Boolean CONFLICTING_BOOL_SETTING = null;

	private enum ConfType {
		PROFILE, TEMPLATE, SINGULAR, SETTINGS
	};

	private ConfType confType = null;
	private PrintStream warningsLog = null;
	private File extendedFile = null;

	private DosboxVersion dbversion = null;
	private SectionsWrapper dosboxSections = null;
	private Autoexec dosboxAutoexec = null;
	private SectionsWrapper extendedSections = null;
	private Autoexec extendedAutoexec = null;

	protected Conf() {}

	public Conf(final Conf other) {
		this.confType = other.confType;
		this.warningsLog = other.warningsLog;
		this.extendedFile = other.extendedFile;
		this.dbversion = other.dbversion;
		this.dosboxSections = new SectionsWrapper(other.dosboxSections);
		this.dosboxAutoexec = new Autoexec(other.dosboxAutoexec);
		this.extendedSections = new SectionsWrapper(other.extendedSections);
		this.extendedAutoexec = new Autoexec(other.extendedAutoexec);
	}

	private void initBaseData(final ConfType type, final PrintStream ps, final File extFile) {
		confType = type;
		warningsLog = ps;
		extendedFile = extFile;
	}

	private void initDosboxData(final DosboxVersion db) throws IOException {
		dbversion = db;
		dosboxSections = new SectionsWrapper();
		dosboxAutoexec = new Autoexec();
		parseInto(dosboxSections, dosboxAutoexec, new FileReader(db.getCanonicalConfFile()), db.getCanonicalConfFile().getPath(), warningsLog);
	}

	// used for adding and editing templates
	// use template = null when creating a new template with default settings
	public Conf(final Template template, final DosboxVersion db, final PrintStream ps) throws IOException {
		initBaseData(ConfType.TEMPLATE, ps, template == null ? null: FileUtils.constructCanonicalTemplateFileLocation(template.getId()));
		initDosboxData(db);

		extendedSections = new SectionsWrapper(dosboxSections);
		extendedAutoexec = new Autoexec(dosboxAutoexec);

		if (template == null) {
			extendedAutoexec.exit = true; // templates have exit by default
		} else {
			parseInto(extendedSections, extendedAutoexec, new FileReader(extendedFile), extendedFile.getPath(), warningsLog);
		}
	}

	// used when running a template so that the temporary file can be named explicitly and booting and exit are disabled
	public Conf(final Template template, final File file, final DosboxVersion db, final PrintStream ps) throws IOException {
		this(template, db, ps);
		extendedFile = file;
		extendedAutoexec.img1 = "";
		extendedAutoexec.exit = false;
	}

	// used for opening an existing profile
	public Conf(final Profile profile, final DosboxVersion db, final PrintStream ps) throws IOException {
		initBaseData(ConfType.PROFILE, ps, profile.getCanonicalConfFile());
		initDosboxData(db);

		extendedSections = new SectionsWrapper(dosboxSections);
		extendedAutoexec = new Autoexec(dosboxAutoexec);
		parseInto(extendedSections, extendedAutoexec, new FileReader(extendedFile), extendedFile.getPath(), warningsLog);
	}

	// used for adding a new profile, possibly based on an existing conf file, or based on a template, or neither
	public Conf(final File existingConfFile, final Template template, final DosboxVersion db, final PrintStream ps) throws IOException {
		initBaseData(ConfType.PROFILE, ps, null);
		initDosboxData(db);

		extendedSections = new SectionsWrapper(dosboxSections);
		extendedAutoexec = new Autoexec(dosboxAutoexec);

		if (existingConfFile != null) {
			parseInto(extendedSections, extendedAutoexec, new FileReader(existingConfFile), existingConfFile.getPath(), warningsLog);
		} else if (template != null) {
			File templateFile = FileUtils.constructCanonicalTemplateFileLocation(template.getId());
			parseInto(extendedSections, extendedAutoexec, new FileReader(templateFile), templateFile.getPath(), warningsLog);
		} else {
			extendedAutoexec.exit = true; // profiles have exit by default
		}
	}

	// used when opening an existing d-fend .conf file
	public Conf(final File orgConfFile, final String title, final int profileId, final DosboxVersion db, final PrintStream ps) throws IOException {
		initBaseData(ConfType.PROFILE, ps, orgConfFile);
		initDosboxData(db);

		extendedSections = new SectionsWrapper();
		extendedAutoexec = new Autoexec();
		parseInto(extendedSections, extendedAutoexec, new FileReader(extendedFile), extendedFile.getPath(), warningsLog);

		extendedAutoexec.removeUnnecessaryMounts(dosboxAutoexec);

		extendedFile = FileUtils.canonicalToData(FileUtils.constructUniqueConfigFileString(profileId, title, extendedAutoexec.isIncomplete() ? null: extendedAutoexec.getCanonicalMainDir()));
	}

	// used to create a configuration when importing a gamepackarchive
	public Conf(final String fullConf, final String incrConf, final boolean importFullSettings, final String importFilename, final ExpProfile prof, final int profileId, final DosboxVersion db,
			final PrintStream ps) throws IOException {
		initBaseData(ConfType.PROFILE, ps, null);
		initDosboxData(db);

		extendedSections = new SectionsWrapper(dosboxSections);
		extendedAutoexec = new Autoexec(dosboxAutoexec);
		parseInto(extendedSections, extendedAutoexec, new StringReader(importFullSettings ? fullConf: incrConf), importFilename, warningsLog);

		extendedFile = FileUtils.canonicalToData(FileUtils.constructUniqueConfigFileString(profileId, prof.getTitle(), extendedAutoexec.isIncomplete() ? null: extendedAutoexec.getCanonicalMainDir()));

		File baseDir = prof.getBaseDir();
		extendedAutoexec.updateForTargetImportBaseDir(baseDir);

		SectionsWrapper fullSecs = new SectionsWrapper();
		Autoexec fullAutoexec = new Autoexec();
		parseInto(fullSecs, fullAutoexec, new StringReader(fullConf), importFilename, warningsLog);

		extendedSections.alterToDosboxVersionGeneration(dosboxSections, fullSecs.detectDosboxVersionGeneration(), dosboxSections.detectDosboxVersionGeneration());
	}

	// used for importing (default) templates
	public Conf(final String fullConf, final String incrConf, final boolean importFullSettings, final String importFilename, final int templateId, final DosboxVersion db, final PrintStream ps)
			throws IOException {
		initBaseData(ConfType.TEMPLATE, ps, FileUtils.constructCanonicalTemplateFileLocation(templateId));
		initDosboxData(db);

		extendedSections = new SectionsWrapper(dosboxSections);
		extendedAutoexec = new Autoexec(dosboxAutoexec);
		parseInto(extendedSections, extendedAutoexec, new StringReader(importFullSettings ? fullConf: incrConf), importFilename, warningsLog);

		SectionsWrapper fullSecs = new SectionsWrapper();
		Autoexec fullAutoexec = new Autoexec();
		parseInto(fullSecs, fullAutoexec, new StringReader(fullConf), importFilename, warningsLog);

		extendedSections.alterToDosboxVersionGeneration(dosboxSections, fullSecs.detectDosboxVersionGeneration(), dosboxSections.detectDosboxVersionGeneration());
	}

	// used when running a profile's setup so that the temporary file can be named explicitly and executable and parameters are set
	public Conf(final Profile profile, final String setup, final String setupParameters, final File file, final DosboxVersion db, final PrintStream ps) throws IOException {
		this(profile, db, ps);
		extendedFile = file;
		if (setup != null && setupParameters != null)
			extendedAutoexec.setMainExecutable(setup, setupParameters);
	}

	// used when running an installer so that the temporary file can be named explicitly and a pause is issued after installation
	public Conf(final Conf org, final File file) throws IOException {
		this(org);
		extendedFile = file;
		extendedAutoexec.pause = true;
		extendedAutoexec.exit = true;
	}

	// used to create a settings instance
	Conf(final SectionsWrapper sec, final File file, final PrintStream ps) {
		initBaseData(ConfType.SETTINGS, ps, file);

		extendedSections = new SectionsWrapper(sec);
		if (extendedFile.isFile() && extendedFile.canRead()) {
			try {
				parseInto(extendedSections, extendedAutoexec, new FileReader(extendedFile), extendedFile.getPath(), warningsLog);
			} catch (IOException e) {
				// if settings could not be read, use only the defaults
			}
		}
	}

	// used to open d-fend singular prof files (profiles.dat and all .prof files)
	public Conf(final File file, final PrintStream ps) throws IOException {
		initBaseData(ConfType.SINGULAR, ps, file);

		extendedSections = new SectionsWrapper();
		parseInto(extendedSections, extendedAutoexec, new FileReader(extendedFile), extendedFile.getPath(), warningsLog);
	}

	// used to load a dosbox conf file
	public Conf(final DosboxVersion db, final PrintStream ps) throws IOException {
		initBaseData(ConfType.SINGULAR, ps, null);
		initDosboxData(db);
	}

	// used to update the profile's conf-file and the dosbox[captures] value in its sections
	public void injectOrUpdateProfile(final Profile profile) {
		extendedFile = profile.getCanonicalConfFile();
		extendedSections.setValue("dosbox", "captures", FileUtils.constructRelativeCapturesDir(profile.getId(), extendedFile.getParentFile(), extendedSections.detectDosboxVersionGeneration()));
	}

	// used to update the template's conf-file
	public void injectOrUpdateTemplate(final Template template) {
		extendedFile = FileUtils.constructCanonicalTemplateFileLocation(template.getId());
	}

	// used to extract a dosbox configuration from profileConf
	public static Conf extractDBVersionConf(final Conf profileConf) {
		Conf result = new Conf();
		result.initBaseData(ConfType.SINGULAR, profileConf.warningsLog, null);
		result.dbversion = profileConf.dbversion;
		result.dosboxSections = profileConf.dosboxSections;
		result.dosboxAutoexec = profileConf.dosboxAutoexec;
		return result;
	}

	// used to create a combination of the two given configurations
	public Conf(final Conf conf1, final Conf conf2, final boolean sameDosboxVersion) {
		this.confType = conf1.confType;
		this.warningsLog = conf1.warningsLog;

		if (sameDosboxVersion) {
			this.dbversion = conf1.dbversion;
			this.dosboxSections = conf1.dosboxSections;
			this.dosboxAutoexec = conf1.dosboxAutoexec;
		}

		this.extendedSections = SectionsWrapper.createCombination(conf1.extendedSections, conf2.extendedSections);
		this.extendedAutoexec = Autoexec.createCombination(conf1.extendedAutoexec, conf2.extendedAutoexec);
	}

	public void save() throws IOException {
		save(false);
	}

	public void save(final Boolean prepareOnly) throws IOException {
		BufferedWriter configData = new BufferedWriter(new FileWriter(extendedFile));
		configData.write(toString(prepareOnly));
		configData.close();
	}

	public File getConfFile() {
		return extendedFile;
	}

	public SectionsWrapper getDosboxSettings() {
		return dosboxSections;
	}

	public SectionsWrapper getSettings() {
		return extendedSections;
	}

	public Autoexec getAutoexec() {
		return extendedAutoexec;
	}

	public String toString() {
		return toString(false);
	}

	public String toString(final Boolean prepareOnly) {
		if (confType == ConfType.SETTINGS) {
			return extendedSections.toString();
		} else if (confType == ConfType.PROFILE) {
			if (dbversion == null || !dbversion.isMultiConfig()) {
				return toFullConfString(prepareOnly);
			} else {
				return toIncrConfString(prepareOnly);
			}
		} else if (confType == ConfType.TEMPLATE) {
			return toIncrConfString(prepareOnly);
		}
		return "";
	}

	public String toFullConfString(final Boolean prepareOnly) {
		return extendedSections.toString() + extendedAutoexec.toString((Autoexec)null, prepareOnly);
	}

	public String toIncrConfString(final Boolean prepareOnly) {
		return new SectionsWrapper(dosboxSections, extendedSections).toString() + extendedAutoexec.toString(dosboxAutoexec, prepareOnly);
	}

	public String toShareString() {
		SectionsWrapper sectionsToShare = new SectionsWrapper(dosboxSections, extendedSections);
		sectionsToShare.removeValue("sdl", "fullscreen");
		sectionsToShare.removeValue("sdl", "fulldouble");
		sectionsToShare.removeValue("sdl", "fullresolution");
		sectionsToShare.removeValue("sdl", "windowresolution");
		sectionsToShare.removeValue("sdl", "output");
		sectionsToShare.removeValue("sdl", "mapperfile");
		sectionsToShare.removeValue("dosbox", "language");
		sectionsToShare.removeValue("dosbox", "captures");
		sectionsToShare.removeValue("render", "scaler");
		sectionsToShare.removeValue("midi", "midiconfig");
		return sectionsToShare.toString();
	}

	public void unmountDosboxMounts() {
		extendedAutoexec.setUnmountsFor(dosboxAutoexec);
	}

	public void removeFloppyMounts() {
		extendedAutoexec.removeFloppyMounts();
	}

	public void removeUnnecessaryMounts() {
		extendedAutoexec.removeUnnecessaryMounts(dosboxAutoexec);
	}

	public void updateValue(final String sectionTitle, final String sectionItem, final String value, final boolean condition) {
		if (condition)
			extendedSections.updateValue(sectionTitle, sectionItem, value);
	}

	public void updateValue(final String section, final String oldItem, final String newItem, final String value, final boolean condition) {
		if (condition && dbversion != null && dosboxSections != null) {
			if (dosboxSections.hasValue(section, newItem)) {
				extendedSections.updateValue(section, newItem, value);
			} else {
				extendedSections.updateValue(section, oldItem, value);
			}
		}
	}

	public void updateScalerValue(final String scalerType, final boolean forced, final boolean condition) {
		if (condition && dbversion != null && dosboxSections != null && !scalerType.equals("")) {
			String newValue = scalerType;
			if (forced && !scalerType.endsWith("forced"))
				newValue += " forced";
			extendedSections.updateValue("render", "scaler", newValue);
		}
	}

	public void updateMidiValue(final String value, final boolean condition) {
		if (condition && dbversion != null && dosboxSections != null) {
			if (dosboxSections.hasValue("midi", "intelligent")) {
				extendedSections.updateValue("midi", "mpu401", String.valueOf(!value.equalsIgnoreCase("none")));
				if (value.equalsIgnoreCase("none")) {
					extendedSections.updateValue("midi", "intelligent", String.valueOf(true));
				} else {
					extendedSections.updateValue("midi", "intelligent", String.valueOf(value.equalsIgnoreCase("intelligent")));
				}
			} else {
				extendedSections.updateValue("midi", "mpu401", value);
			}
		}
	}

	public void updateJoystickValue(final String joystickType, final boolean timed, final boolean autofire, final boolean swap34, final boolean buttonwrap, final boolean c1, final boolean c2,
			final boolean c3, final boolean c4, final boolean c5) {
		if (dbversion != null && dosboxSections != null) {
			if (dosboxSections.hasValue("joystick", "joysticktype")) {
				updateValue("joystick", "joysticktype", joystickType, c1);
				updateValue("joystick", "timed", String.valueOf(timed), c2);
				updateValue("joystick", "autofire", String.valueOf(autofire), c3);
				updateValue("joystick", "swap34", String.valueOf(swap34), c4);
				updateValue("joystick", "buttonwrap", String.valueOf(buttonwrap), c5);
			} else {
				updateValue("bios", "joysticktype", joystickType, c1);
			}
		}
	}

	public void setAutoexecSettingsForTemplate(final Boolean exit, final String mixer, final String keyb, final String ipxnet, final Boolean booter, final String[] customSections) {
		extendedAutoexec.exit = exit;
		extendedAutoexec.mixer = mixer;
		extendedAutoexec.keyb = keyb;
		extendedAutoexec.ipxnet = ipxnet;
		extendedAutoexec.img1 = booter ? "file": "";
		extendedAutoexec.customSections = customSections;
	}

	public void setAutoexecSettingsForProfile(final Boolean loadhigh, final Boolean loadfix, final String loadfixValue, final String main, final String params, final String img1, final String img2,
			final String img3, final String imgDriveletter) {
		extendedAutoexec.main = main;
		extendedAutoexec.params = params;
		extendedAutoexec.loadhigh = loadhigh;
		extendedAutoexec.loadfix = loadfix;
		try {
			extendedAutoexec.loadfixValue = Integer.parseInt(loadfixValue);
		} catch (NumberFormatException e) {
			// no change
		}
		extendedAutoexec.img1 = img1;
		extendedAutoexec.img2 = img2;
		extendedAutoexec.img3 = img3;
		extendedAutoexec.imgDriveletter = imgDriveletter;
	}

	public void setAutoexecSettingsForProfileMultiEdit(final Boolean loadhigh, final Boolean loadfix, final String loadfixValue, final Boolean exit, final String mixer, final String keyb,
			final String ipxnet, final String[] customSections) {
		if (loadhigh != null)
			extendedAutoexec.loadhigh = loadhigh;
		if (loadfix != null)
			extendedAutoexec.loadfix = loadfix;
		try {
			if (loadfixValue != null)
				extendedAutoexec.loadfixValue = Integer.parseInt(loadfixValue);
		} catch (NumberFormatException e) {
			// no change
		}
		if (exit != null)
			extendedAutoexec.exit = exit;
		if (mixer != null)
			extendedAutoexec.mixer = mixer;
		if (keyb != null)
			extendedAutoexec.keyb = keyb;
		if (ipxnet != null)
			extendedAutoexec.ipxnet = ipxnet;
		for (int i = 0; i < customSections.length; i++)
			if (customSections[i] != null)
				extendedAutoexec.customSections[i] = customSections[i];
	}

	public void alterToDosboxVersionGeneration(final Conf dosboxConf) {
		this.dbversion = dosboxConf.dbversion;
		this.extendedSections.alterToDosboxVersionGeneration(dosboxConf.dosboxSections);
	}

	// set to another dosbox version but try to maintain as many settings as possible
	public void setToDosboxVersion(final Conf dosboxConf) {
		if ((this.dbversion != null) && (this.dbversion.getId() == dosboxConf.getDbversion().getId()))
			return;

		List<Mount> uniqueMounts = Autoexec.getUniqueMountingpoints(dosboxAutoexec, extendedAutoexec);
		this.dbversion = dosboxConf.dbversion;
		this.dosboxSections = dosboxConf.dosboxSections;
		this.dosboxAutoexec = dosboxConf.dosboxAutoexec;
		this.extendedSections.alterToDosboxVersionGeneration(dosboxConf.dosboxSections);
		this.extendedAutoexec.updateMountingPoints(dosboxAutoexec, uniqueMounts);
	}

	// set to another dosbox version while maintaining only the changes from the defaults
	public void switchToDosboxVersion(final Conf dosboxConf) {
		if ((this.dbversion != null) && (this.dbversion.getId() == dosboxConf.getDbversion().getId()))
			return;

		List<Mount> uniqueMounts = Autoexec.getUniqueMountingpoints(dosboxAutoexec, extendedAutoexec);
		this.dbversion = dosboxConf.dbversion;
		this.dosboxAutoexec = dosboxConf.dosboxAutoexec;
		this.extendedSections.switchToDosboxVersionGeneration(dosboxSections, dosboxConf.dosboxSections);
		this.extendedAutoexec.updateMountingPoints(dosboxAutoexec, uniqueMounts);
		this.dosboxSections = dosboxConf.dosboxSections;
	}

	// reloads all dosbox settings, but keeps exit and unique mounts and booter/executable data
	public void reloadDosboxVersion(final Conf dosboxConf) {
		List<Mount> uniqueMounts = Autoexec.getUniqueMountingpoints(dosboxAutoexec, extendedAutoexec);
		Boolean exit = extendedAutoexec.exit;
		String main = extendedAutoexec.main;
		String params = extendedAutoexec.params;
		String img1 = extendedAutoexec.img1;
		String img2 = extendedAutoexec.img2;
		String img3 = extendedAutoexec.img3;
		dbversion = dosboxConf.dbversion;
		dosboxSections = dosboxConf.dosboxSections;
		dosboxAutoexec = dosboxConf.dosboxAutoexec;
		extendedSections = new SectionsWrapper(dosboxSections);
		extendedAutoexec = new Autoexec(dosboxAutoexec);
		extendedAutoexec.updateMountingPoints(dosboxAutoexec, uniqueMounts);
		extendedAutoexec.exit = exit;
		extendedAutoexec.main = main;
		extendedAutoexec.params = params;
		extendedAutoexec.img1 = img1;
		extendedAutoexec.img2 = img2;
		extendedAutoexec.img3 = img3;
	}

	// reloads all template settings, but keeps mounts if they exist
	public void reloadTemplate(final Conf templateConf) {
		Autoexec orgMounts = new Autoexec(extendedAutoexec);
		String main = extendedAutoexec.main;
		String params = extendedAutoexec.params;
		String img1 = extendedAutoexec.img1;
		String img2 = extendedAutoexec.img2;
		String img3 = extendedAutoexec.img3;

		dbversion = templateConf.dbversion;
		dosboxSections = templateConf.dosboxSections;
		dosboxAutoexec = templateConf.dosboxAutoexec;
		extendedSections = templateConf.extendedSections;
		extendedAutoexec = templateConf.extendedAutoexec;

		extendedAutoexec.main = main;
		extendedAutoexec.params = params;
		if (StringUtils.isNotEmpty(img1)) {
			extendedAutoexec.img1 = img1;
			extendedAutoexec.img2 = img2;
			extendedAutoexec.img3 = img3;
		}
		if (orgMounts.mountingpoints.size() > 0) {
			extendedAutoexec.mountingpoints = orgMounts.mountingpoints;
		}
	}

	// injects loaded shared incremental conf, and optionally reload dosbox settings, but don't touch autoexec section
	public void loadSharedConf(final String incrConf, final boolean reloadDosboxDefaults) throws IOException {
		if (reloadDosboxDefaults) {
			List<Mount> uniqueMounts = Autoexec.getUniqueMountingpoints(dosboxAutoexec, extendedAutoexec);
			Boolean exit = extendedAutoexec.exit;
			String main = extendedAutoexec.main;
			String params = extendedAutoexec.params;
			String img1 = extendedAutoexec.img1;
			String img2 = extendedAutoexec.img2;
			String img3 = extendedAutoexec.img3;
			extendedSections = new SectionsWrapper(dosboxSections);
			extendedAutoexec = new Autoexec(dosboxAutoexec);
			extendedAutoexec.updateMountingPoints(dosboxAutoexec, uniqueMounts);
			extendedAutoexec.exit = exit;
			extendedAutoexec.main = main;
			extendedAutoexec.params = params;
			extendedAutoexec.img1 = img1;
			extendedAutoexec.img2 = img2;
			extendedAutoexec.img3 = img3;
		}
		parseInto(extendedSections, extendedAutoexec, new StringReader(incrConf), "DBConf", warningsLog);
	}

	public String[] addMount(final String mount) {
		extendedAutoexec.addMount(mount);
		return extendedAutoexec.getMountingpoints();
	}

	public String[] editMount(final int index, final String mount) {
		if (index < dosboxAutoexec.mountingpoints.size()) {
			Mount mnt = extendedAutoexec.mountingpoints.get(index);
			if (!mnt.isUnmounted()) {
				mnt.toggleMount();
			}
			extendedAutoexec.addMount(mount);
		} else {
			try {
				extendedAutoexec.mountingpoints.set(index, new Mount(mount));
			} catch (InvalidMountstringException e) {
				// nothing we can do
			}
		}
		return extendedAutoexec.getMountingpoints();
	}

	public String[] removeMount(final int index) {
		if (index < dosboxAutoexec.mountingpoints.size()) {
			extendedAutoexec.mountingpoints.get(index).toggleMount();
		} else {
			extendedAutoexec.mountingpoints.remove(index);
		}
		return extendedAutoexec.getMountingpoints();
	}

	private Map<Character, Mount> nettoMounts() {
		return extendedAutoexec.nettoMounts(dosboxAutoexec.nettoMounts());
	}

	public Set<Character> nettoMountedDriveLetters() {
		return nettoMounts().keySet();
	}

	public String getRequiredMount(final boolean booter, final String main) {
		if (extendedAutoexec.convertToDosboxPath(main)[0].length() > 0)
			return null;
		if (booter && extendedAutoexec.matchesMountedImage(main))
			return null;
		try {
			return new Mount(booter, main, nettoMountedDriveLetters()).getPathAsString();
		} catch (Exception e) {
			// this is not entirely correct; returning null assumes no mounts required
			// but this should never happen anyway
			return null;
		}
	}

	public String[] addRequiredMount(final boolean booter, final String main) {
		try {
			if (FileUtils.containsIso(main) != -1) {
				Mount tmp = new Mount(booter, ".", nettoMountedDriveLetters());
				if (tmp.getDriveletter() == 'C')
					addMount(tmp.toString());
				return addMount(new Mount(booter, main, nettoMountedDriveLetters()).toString());
			} else {
				return addMount(new Mount(booter, main, nettoMountedDriveLetters()).toString());
			}
		} catch (InvalidHostfileException | DrivelettersExhaustedException e) {
			return extendedAutoexec.getMountingpoints(); // could not add, just return previous values
		}
	}

	public String[] addRequiredMountForInstaller(final String main) {
		try {
			File[] extraFilesToMount = FileUtils.findFileSequence(FileUtils.getCanMainFile(new File(main)));
			if (extraFilesToMount.length > 1)
				return addMount(new Mount(main, nettoMountedDriveLetters(), extraFilesToMount).toString());
			else
				return addMount(new Mount(main, nettoMountedDriveLetters(), null).toString());
		} catch (InvalidHostfileException | DrivelettersExhaustedException e) {
			return extendedAutoexec.getMountingpoints(); // could not add, just return previous values
		}
	}

	public DosboxVersion getDbversion() {
		return dbversion;
	}

	public static boolean isConflictingValue(final String s) {
		return s.equals(CONFLICTING_STRING_SETTING);
	}

	public static boolean isConflictingValue(final int i) {
		return i == CONFLICTING_INT_SETTING;
	}

	public static boolean isConflictingValue(final Boolean b) {
		return b == CONFLICTING_BOOL_SETTING;
	}

	public File getCustomMapperFile() {
		if (confType == ConfType.PROFILE) {
			String dbMapperfile = dosboxSections.getValue("sdl", "mapperfile");
			String customMapperfile = extendedSections.getValue("sdl", "mapperfile");
			if (extendedSections.hasValue("sdl", "mapperfile") && !customMapperfile.equals(dbMapperfile)) {
				File mapper = new File(extendedFile.getParentFile(), customMapperfile);
				if (FileUtils.isExistingFile(mapper)) {
					return mapper;
				}
			}
		}
		return null;
	}

	private static void parseInto(final SectionsWrapper sections, final Autoexec autoexec, final Reader reader, final String filename, final PrintStream warningsLog) throws IOException {
		final Settings settings = Settings.getInstance();

		BufferedReader configData = new BufferedReader(reader);
		int lineNumber = 1;
		String textLine;
		String currSectionTitle = null;
		boolean lastItemHadMissingSection = false;
		List<String> autoexecLines = new ArrayList<String>();

		while ((textLine = configData.readLine()) != null) {
			textLine = textLine.trim();
			if ((textLine.length() > 0) && textLine.charAt(0) != '#') {
				if (textLine.charAt(0) == '[') { // a new section starts here
					int start = textLine.indexOf(('['));
					int end = textLine.lastIndexOf(']');
					if (end == -1) {
						warningsLog.println(settings.msg("general.error.parseconf", new Object[] {filename, lineNumber, textLine}));
					} else {
						currSectionTitle = textLine.substring(start + 1, end);
					}
				} else { // an item starts here
					if (currSectionTitle == null) { // value before section
						if (!lastItemHadMissingSection) {
							warningsLog.println(settings.msg("general.error.sectionmissing", new Object[] {filename, lineNumber, textLine}));
						}
						lastItemHadMissingSection = true;
					} else {
						if ("autoexec".equals(currSectionTitle)) { // autoexec config item
							autoexecLines.add(textLine);
						} else { // normal config item
							int end = textLine.indexOf('=');
							if (end == -1) {
								warningsLog.println(settings.msg("general.error.parseconf", new Object[] {filename, lineNumber, textLine}));
							} else {
								String name = textLine.substring(0, end).trim();
								String value = textLine.substring(end + 1).trim();
								sections.setValue(currSectionTitle, name.toLowerCase(), value);
							}
						}
						lastItemHadMissingSection = false;
					}
				}
			}
			lineNumber++;
		}
		configData.close();

		if (autoexecLines.size() > 0) {
			autoexec.parseLines(autoexecLines);
		}
	}

	public boolean hasDifferentBaseMountsThan(Conf conf) {
		return !Arrays.equals(dosboxAutoexec.getMountingpoints(), conf.dosboxAutoexec.getMountingpoints());
	}

	public int countImageMounts() {
		int result = 0;
		for (Mount m: extendedAutoexec.mountingpoints) {
			if (m.getMountingType() == MountingType.IMAGE && !m.isUnmounted())
				result++;
		}
		return result;
	}

	public File[] getFirstImageMountPath() {
		for (Mount m: extendedAutoexec.mountingpoints) {
			if (m.getMountingType() == MountingType.IMAGE && !m.isUnmounted())
				return m.getPath();
		}
		return null;
	}
}
