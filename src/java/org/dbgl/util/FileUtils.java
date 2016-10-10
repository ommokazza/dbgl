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
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.swing.filechooser.FileSystemView;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.dbgl.gui.GeneralPurposeDialogs;
import org.dbgl.gui.StreamGobbler;
import org.dbgl.loopy.iso9660.ISO9660FileEntry;
import org.dbgl.loopy.iso9660.ISO9660FileSystem;
import org.dbgl.model.DosboxVersion;
import org.dbgl.model.NativeCommand;
import org.dbgl.model.Profile;
import org.dbgl.model.ShortFile;
import org.dbgl.model.Template;
import org.dbgl.model.conf.Conf;
import org.dbgl.model.conf.SectionsWrapper;
import org.dbgl.model.conf.Settings;
import org.dbgl.waldheinz.fs.fat.BlockDevice;
import org.dbgl.waldheinz.fs.fat.FatFileSystem;
import org.dbgl.waldheinz.fs.fat.FatLfnDirectory;
import org.dbgl.waldheinz.fs.fat.FatLfnDirectoryEntry;
import org.dbgl.waldheinz.fs.util.FileDisk;
import org.eclipse.swt.widgets.Display;
import SevenZip.MyRandomAccessFile;
import SevenZip.Archive.IInArchive;
import SevenZip.Archive.SevenZipEntry;
import SevenZip.Archive.SevenZip.Handler;


public final class FileUtils {

	public static final class FilenameComparator implements Comparator<String> {
		public int compare(final String string1, final String string2) {
			int count1 = StringUtils.countMatches(string1, "\\");
			int count2 = StringUtils.countMatches(string2, "\\");
			if (count1 == count2) {
				return string1.compareTo(string2);
			} else {
				return count1 - count2;
			}
		}
	}

	public static final class FileComparator implements Comparator<File> {
		public int compare(final File file1, final File file2) {
			return new FilenameComparator().compare(file1.getPath(), file2.getPath());
		}
	}

	private static final int ZIP_BUFFER = 10240;

	private static final File DATA_DIR_FILE;
	private static final File DOSROOT_DIR_FILE;
	private static final File DOSBOX_DIR_FILE;
	private static final File TMPINST_DIR_FILE;
	private static final String TEMPLATES_DIR = "templates" + File.separatorChar;
	private static final String SETUP_CONF = "setup.conf";
	private static final String TEMPLATE_CONF = "template.conf";

	public static final String PROFILES_XML = "profiles.xml";
	public static final String DOSROOT_DIR = "dosroot" + File.separatorChar;
	public static final String CAPTURES_DIR = "captures" + File.separatorChar;
	public static final String MAPPER_DIR = "mapper" + File.separatorChar;
	public static final String PROFILES_DIR = "profiles" + File.separatorChar;
	public static final String EXPORT_DIR = "export" + File.separatorChar;
	public static final String XSL_DIR = "xsl" + File.separatorChar;
	public static final String XSL_EXT = ".xsl";
	public static final String XML_EXT = ".xml";
	public static final String CONF_EXT = ".conf";
	public static final String MAPPER_EXT = ".map";
	public static final String GAMEPACKARCHIVE_EXT = ".dbgl.zip";
	public static final String[] CDIMAGES = {".iso", ".cue", ".bin"};
	public static final String[] EXECUTABLES = {".exe", ".com", ".bat"};
	public static final String[] EXECUTABLES_UPPERCASE = {"EXE", "COM", "BAT"};
	public static final String[] ARCHIVES = {".zip", ".7z"};
	static final String[] FATIMAGES = {".ima"};
	static final String[] BOOTERIMAGES = {".cp2", ".dcf", ".img", ".jrc", ".td0"};
	static final String[] PICTURES = {".png", ".gif", ".jpg", ".tif", ".tiff", ".ico", ".bmp"};
	static final String[] SETUPFILES = {"setup.exe", "install.exe", "setsound.exe", "setup.bat", "config.exe", "setsound.bat", "sound.bat", "sound.exe", "install.com", "install.bat", "sndsetup.exe",
			"soundset.exe", "config.bat", "setup.com", "setsnd.exe", "setd.exe", "configur.exe", "uwsound.exe"};
	static final String[] UNLIKELYMAINFILES = {"dos4gw.exe", "readme.bat", "intro.exe", "loadpats.exe", "uvconfig.exe", "soundrv.com", "sblaster.com", "sound.bat", "univbe.exe", "midpak.com",
			"ultramid.exe", "mpscopy.exe", "readme.exe", "sbpro.com", "help.bat", "exists.com", "helpme.exe", "paudio.com", "bootdisk.exe", "pas16.com", "mssw95.exe", "setd.exe", "adlib.com",
			"sbclone.com", "sb16.com", "godir.com", "ibmsnd.com", "crack.com", "gf166.com", "__insth.bat", "adlibg.com", "space.com", "instgame.bat", "ibmbak.com", "pkunzjr.com", "nosound.com",
			"sview.exe", "mgraphic.exe", "title.exe", "misc.exe", "checkcd.bat", "patch.exe", "tansltl.com", "readme.com", "uninstal.exe", "source.com", "cmidpak.com", "vector.com", "view.exe",
			"rtm.exe", "eregcard.exe", "sndsys.com", "info.exe", "docshell.exe", "catalog.exe", "ipxsetup.exe", "yes.com", "stfx.com", "getkey.com", "lsize.com", "makepath.com", "sersetup.exe",
			"commit.exe", "_setup.exe", "end.exe", "what.exe", "setm.exe", "cvxsnd.com", "aria.com", "tgraphic.exe", "egraphic.exe", "smidpak.com", "tlivesa.com", "vmsnd.com", "detect.exe",
			"digvesa.com", "cwsdpmi.exe", "vesa.exe", "havevesa.exe", "_install.bat", "smsnd.com", "insticon.exe", "installh.bat", "install2.bat", "info.bat", "setmain.exe", "swcbbs.exe",
			"vbetest.exe", "pmidpak.com", "inst.exe", "cleardrv.exe", "winstall.exe", "ibm1bit.com", "tmidpak.com", "dealers.exe", "digisp.com", "drv_bz.com", "drv_sb.com", "drv_ss.com",
			"convert.exe", "editor.exe", "cgraphic.exe", "update.bat", "smackply.exe", "univesa.exe", "lha.exe", "makeboot.bat", "nnansi.com", "setblast.exe", "autoexec.bat", "helpme.bat",
			"exist.com", "fixboot.exe", "ask.com", "vesatest.exe", "manual.exe", "sbwave.com", "rmidpak.com", "diagnost.exe", "pkunzip.exe", "sinstall.exe", "megaem.exe", "vesa.com", "getdrv.exe",
			"drv_sbd.com", "chkvesa.exe", "chkmem.com", "setstick.exe"};

	public static final String DOSBOX_CONF = "dosbox.conf";
	public static final String CNF_FILTER = "*.conf;*.CONF";
	public static final String EXE_FILTER = "*.com;*.COM;*.exe;*.EXE;*.bat;*.BAT";
	public static final String ARC_FILTER = "*.zip;*.ZIP;*.7z;*.7Z";
	public static final String DBGLZIP_FILTER = "*.dbgl.zip;*.DBGL.ZIP";
	public static final String BTR_FILTER = "*.cp2;*.CP2;*.dcf;*.DCF;*.img;*.IMG;*.jrc;*.JRC;*.td0;*.TD0";
	public static final String CDI_FILTER = "*.iso;*.ISO;*.cue;*.CUE";
	public static final String FATI_FILTER = "*.ima;*.IMA;";
	public static final String ALL_FILTER = "*";

	public static final String INVALID_FILENAME_CHARS_REGEXP = "[^a-zA-Z_0-9()]";

	static {
		final SectionsWrapper settings = Settings.getInstance().getSettings();
		final String data = replaceTildeInPath(settings.getValue("directory", "data"));
		boolean dataInUserDir = PlatformUtils.USE_USER_HOME_DIR || !PlatformUtils.isDirectoryWritable(new File(data));
		DATA_DIR_FILE = canonical(dataInUserDir ? new File(PlatformUtils.USER_DATA_DIR_FILE, data).getPath(): data);
		DOSROOT_DIR_FILE = new File(DATA_DIR_FILE, DOSROOT_DIR);
		DOSBOX_DIR_FILE = canonical(replaceTildeInPath(settings.getValue("directory", "dosbox")));
		TMPINST_DIR_FILE = new File(DOSROOT_DIR_FILE, settings.getValue("directory", "tmpinstall"));

		if (dataInUserDir)
			copyDirectoriesIfNecessary(data);
		createDirIfNecessary(DOSROOT_DIR_FILE);
	}

	public static String replaceTildeInPath(final String path) {
		if ((PlatformUtils.IS_LINUX || PlatformUtils.IS_OSX) && (path.startsWith("~/") || (path.length() == 1 && path.charAt(0) == '~'))) {
			// Linux and OSX have ~/ for homedirectory. Allow single ~ as well
			return path.replaceAll("^~", System.getProperty("user.home"));
		}
		return path;
	}

	private static void copyDirectoriesIfNecessary(final String data) {
		copyDirIfNecessary(data, CAPTURES_DIR);
		File databaseFile = getDatabaseFile(Settings.getInstance().getSettings().getValue("database", "connectionstring"));
		if (databaseFile != null)
			copyDirIfNecessary(data, databaseFile.getParent());
		copyDirIfNecessary(data, DOSROOT_DIR);
		copyDirIfNecessary(data, EXPORT_DIR);
		copyDirIfNecessary(data, PROFILES_DIR);
		copyDirIfNecessary(data, TEMPLATES_DIR);
		copyDirIfNecessary(data, XSL_DIR);
	}

	private static void copyDirIfNecessary(final String data, final String dir) {
		File src = new File(data, dir);
		try {
			if (!isExistingDirectory(canonicalToData(dir)) && isExistingDirectory(src)) {
				org.apache.commons.io.FileUtils.copyDirectoryToDirectory(src, DATA_DIR_FILE);
			}
		} catch (IOException e) {
			System.err.println(Settings.getInstance().msg("general.error.copydirtodir", new String[] {src.getPath(), DATA_DIR_FILE.getPath()}));
		}
	}

	public static File getDatabaseFile(final String connString) {
		if (connString.contains("file:")) {
			// Some magic on the connection string
			int start = connString.indexOf("file:") + 5; // skip 'file:'
			int end = connString.indexOf(';', start);
			if (end == -1) {
				end = connString.length();
			}
			String filename = replaceTildeInPath(connString.substring(start, end));
			return canonicalToData(filename);
		} else {
			return null;
		}
	}

	public static boolean isExistingFile(final File file) {
		return file.isFile() && file.exists();
	}

	public static boolean isExistingDirectory(final File dir) {
		return dir.isDirectory() && dir.exists();
	}

	private static List<String> initCommands(final DosboxVersion dbversion, boolean forceDBConf) {
		List<String> execCommands = new ArrayList<String>();
		if (dbversion.isUsingCurses()) {
			if (PlatformUtils.IS_WINDOWS) {
				execCommands.add("rundll32");
				execCommands.add("SHELL32.DLL,ShellExec_RunDLL");
			} else if (PlatformUtils.IS_LINUX) {
				execCommands.add("xterm");
				execCommands.add("-e");
			}
		}
		execCommands.add(dbversion.getCanonicalExecutable().getPath());
		if ((dbversion.isMultiConfig() && isReadableFile(dbversion.getCanonicalConfFile())) || forceDBConf) {
			// selected default dosbox config file
			execCommands.add("-conf");
			execCommands.add(dbversion.getCanonicalConfFile().getPath());
		}
		return execCommands;
	}

	private static void postCommands(final DosboxVersion dbversion, List<String> execCommands) {
		if (dbversion.getParameters().length() > 0) {
			for (String p: dbversion.getParameters().split(" ")) {
				execCommands.add(p);
			}
		}
		if (Settings.getInstance().getSettings().getBooleanValue("dosbox", "hideconsole")) {
			execCommands.add("-noconsole");
		}
	}

	private static void executeCommand(final DosboxVersion dbversion, final List<String> execCommands, final File cwd, Map<String, String> env, final boolean waitFor) throws IOException {
		StringBuffer cmd = new StringBuffer();
		try {
			File dir = (cwd == null) ? DOSROOT_DIR_FILE: cwd;
			if (PlatformUtils.IS_OSX && dbversion != null && dbversion.isUsingCurses()) {
				String terminalCommand = StringUtils.join(execCommands, ' ');
				execCommands.clear();
				execCommands.add("osascript");
				execCommands.add("-e");
				execCommands.add("tell application \"Terminal\" to do script \"cd '" + dir + "'; " + terminalCommand + "; exit;\"");
			}
			System.out.print(StringUtils.join(execCommands, ' '));
			ProcessBuilder pb = new ProcessBuilder(execCommands.toArray(new String[execCommands.size()]));
			pb.directory(dir);
			Map<String, String> environment = pb.environment();
			if (env != null) {
				environment.putAll(env);
				System.out.print(env);
			}
			System.out.println();
			Process proc = pb.start();
			StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "DOSBox stderr");
			StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), "DOSBox stdout");
			outputGobbler.start();
			errorGobbler.start();
			if (waitFor) {
				try {
					proc.waitFor();
				} catch (InterruptedException e) {}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new IOException(Settings.getInstance().msg("general.error.startdosbox", new Object[] {cmd}));
		}
	}

	private static void doRunDosbox(final DosboxVersion dbversion, final String[] parameters, final boolean forceDBConf, final File cwd, final Map<String, String> env, final boolean waitFor)
			throws IOException {
		List<String> commandItems = initCommands(dbversion, forceDBConf);
		commandItems.addAll(Arrays.asList(parameters));
		postCommands(dbversion, commandItems);
		executeCommand(dbversion, commandItems, cwd, env, waitFor);
	}

	private static File canonicalTo(final File base, final String path) {
		File file = new File(path);
		if (file.isAbsolute()) {
			try {
				return file.getCanonicalFile();
			} catch (IOException e) {
				return file.getAbsoluteFile();
			}
		} else {
			try {
				return new File(base, file.getPath()).getCanonicalFile();
			} catch (IOException e) {
				return new File(base, file.getPath()).getAbsoluteFile();
			}
		}
	}

	public static boolean areRelated(final File parent, final File child) {
		File remainder = child.getParentFile();
		while (remainder != null) {
			if (parent.equals(remainder)) {
				return true;
			}
			remainder = remainder.getParentFile();
		}
		return false;
	}

	public static String getDosRoot() {
		return DOSROOT_DIR_FILE.getPath();
	}

	public static File makeRelativeTo(final File file, final File basePath) {
		if (!file.isAbsolute()) {
			return file;
		}
		if (file.equals(basePath)) {
			return new File(".");
		}
		File remainder = new File(file.getName());
		File parent = file.getParentFile();
		while (parent != null) {
			if (parent.equals(basePath)) {
				return remainder;
			}
			remainder = new File(parent.getName(), remainder.getPath());
			parent = parent.getParentFile();
		}
		return file;
	}

	public static File makeRelativeToData(final File file) {
		return makeRelativeTo(file, DATA_DIR_FILE);
	}

	public static File makeRelativeToDosroot(final File file) {
		return makeRelativeTo(file, DOSROOT_DIR_FILE);
	}

	public static File makeRelativeToDosbox(final File file) {
		return makeRelativeTo(file, DOSBOX_DIR_FILE);
	}

	public static File canonical(final String path) {
		return canonicalTo(new File("."), path);
	}

	public static File canonicalToData(final String path) {
		return canonicalTo(DATA_DIR_FILE, path);
	}

	public static File canonicalToDosbox(final String path) {
		return canonicalTo(DOSBOX_DIR_FILE, path);
	}

	public static File canonicalToDosroot(final String path) {
		return canonicalTo(DOSROOT_DIR_FILE, path);
	}

	public static String sanitizeToDosroot(final String path) {
		return makeRelativeToDosroot(canonicalToDosroot(path)).getPath();
	}

	public static File prefixAndSanitizeToDosroot(final File basePath, final File file) {
		if (!file.isAbsolute())
			return makeRelativeToDosroot(canonicalToDosroot(new File(basePath, file.getPath()).getPath()));
		return file;
	}

	public static File constructRelativeDBConfLocation(final String path) {
		return makeRelativeToDosbox(new File(path, DOSBOX_CONF));
	}

	public static File constructCanonicalDBExeLocation(final String path) {
		return canonicalToDosbox(new File(path, PlatformUtils.DB_EXECUTABLE).getPath());
	}

	public static String constructCapturesDir(final int profileId) {
		return CAPTURES_DIR + profileId;
	}

	public static String constructMapperFile(final int profileId) {
		return MAPPER_DIR + profileId + MAPPER_EXT;
	}

	public static String getRelativePath(final File base, final File name) throws IOException {
		File parent = base.getParentFile();
		if (parent == null)
			return name.getPath();

		String bpath = base.getCanonicalPath();
		String fpath = name.getCanonicalPath();

		if (fpath.startsWith(bpath)) {
			return fpath.substring(bpath.length() + 1);
		} else {
			return ".." + File.separatorChar + getRelativePath(parent, name);
		}
	}

	public static String constructRelativeCapturesDir(final int profileId, final File confFileDirectory, final int dosboxVersionGeneration) {
		File baseDir = null;
		if (dosboxVersionGeneration < 3) // older than 0.73
			baseDir = DOSROOT_DIR_FILE; // cwd when starting DOSBox
		else
			baseDir = confFileDirectory;// directory containing the profile's .conf

		File canCaps = canonicalToData(constructCapturesDir(profileId));
		try {
			return getRelativePath(baseDir, canCaps);
		} catch (IOException e) {
			return canCaps.getPath();
		}
	}

	public static File constructCanonicalTemplateFileLocation(final int templateId) {
		return canonicalToData(TEMPLATES_DIR + templateId + CONF_EXT);
	}

	public static File getDefaultTemplatesXmlFile() {
		return canonicalToData(TEMPLATES_DIR + "default.xml");
	}

	public static String constructUniqueConfigFileString(final int profileId, final String profileTitle, final File canonicalMainDir) {
		SectionsWrapper set = Settings.getInstance().getSettings();
		File path;
		if ((set.getIntValue("profiledefaults", "confpath") == 0) || (canonicalMainDir == null))
			path = new File(PROFILES_DIR);
		else
			path = canonicalMainDir;
		String prefix = (set.getIntValue("profiledefaults", "conffile") == 0) ? String.valueOf(profileId): profileTitle;
		File candidate = null;
		int nr = 1;
		do {
			candidate = new File(path, fileSystemSafe(prefix + ((nr > 1) ? "(" + nr + ")": "")) + CONF_EXT);
			nr++;
		} while (isExistingFile(canonicalToData(candidate.getPath())));
		return candidate.getPath();
	}

	public static void doRunDosbox(final DosboxVersion dbversion, final Map<String, String> env) throws IOException {
		doRunDosbox(dbversion, new String[] {}, true, null, env, false);
	}

	public static void doCreateDosboxConf(final DosboxVersion dbversion) throws IOException {
		char q = PlatformUtils.IS_WINDOWS ? '\'': '"';
		doRunDosbox(dbversion, new String[] {"-c", "config -writeconf " + q + dbversion.getCanonicalConfFile() + q, "-c", "exit"}, false, dbversion.getCanonicalExecutable().getParentFile(), null,
			true);
	}

	public static void doRunProfile(final Profile prof, final List<DosboxVersion> dbversions, final Map<String, String> env, final int setup, final boolean prepareOnly, final List<NativeCommand> cmds,
			final Display display) throws IOException {
		DosboxVersion dbv = DosboxVersion.findById(dbversions, prof.getDbversionId());
		doRunProfile(prof, dbv, env, setup, prepareOnly, cmds, display);
	}

	public static void doRunTemplate(final Template template, final List<DosboxVersion> dbversions, final Map<String, String> env, final List<NativeCommand> cmds, final Display display)
			throws IOException {
		DosboxVersion dbv = DosboxVersion.findById(dbversions, template.getDbversionId());
		doRunTemplate(template, dbv, env, cmds, display);
	}

	public static void doRunProfile(final Profile prof, final DosboxVersion dbversion, final Map<String, String> env, final int setup, final boolean prepareOnly, final List<NativeCommand> cmds,
			final Display display) throws IOException {
		final File file;
		boolean runSetup = (setup != -1) && prof.hasSetup(setup);
		if (runSetup || prepareOnly) {
			file = canonicalToData(PROFILES_DIR + SETUP_CONF);
			Conf conf = prepareOnly ? new Conf(prof, null, null, file, dbversion, System.err): new Conf(prof, prof.getSetup(setup), prof.getSetupParameters(setup), file, dbversion, System.err);
			conf.save(prepareOnly);
		} else {
			file = prof.getCanonicalConfFile();
		}

		new Thread() {
			public void run() {
				try {
					for (NativeCommand cmd: cmds) {
						if (cmd.getCommand() == null) {
							doRunDosbox(dbversion, new String[] {"-conf", file.getPath()}, false, null, env, true);
						} else {
							executeCommand(null, cmd.getExecCommandsCanToData(), cmd.getCwdCanToData(), env, cmd.isWaitFor());
						}
					}
				} catch (final IOException e) {
					if (!display.isDisposed()) {
						display.syncExec(new Runnable() {
							public void run() {
								GeneralPurposeDialogs.warningMessage(display.getActiveShell(), e);
							}
						});
					}
				}
			}
		}.start();
	}

	public static void doRunInstaller(final Profile prof, final Conf conf, final List<DosboxVersion> dbversions, final Map<String, String> env, final boolean prepareOnly) throws IOException {
		DosboxVersion dbv = DosboxVersion.findById(dbversions, prof.getDbversionId());
		File file = canonicalToData(PROFILES_DIR + SETUP_CONF);
		Conf tmpConf = new Conf(conf, file);
		tmpConf.save(prepareOnly);
		doRunDosbox(dbv, new String[] {"-conf", file.getPath()}, false, null, env, true);
	}

	public static void doRunTemplate(final Template template, final DosboxVersion dbversion, final Map<String, String> env, final List<NativeCommand> cmds, final Display display) throws IOException {
		final File file = canonicalToData(PROFILES_DIR + TEMPLATE_CONF);
		Conf conf = new Conf(template, file, dbversion, System.err);
		conf.save();

		new Thread() {
			public void run() {
				try {
					for (NativeCommand cmd: cmds) {
						if (cmd.getCommand() == null) {
							doRunDosbox(dbversion, new String[] {"-conf", file.getPath()}, false, null, env, true);
						} else {
							executeCommand(null, cmd.getExecCommandsCanToData(), cmd.getCwdCanToData(), env, cmd.isWaitFor());
						}
					}
				} catch (final IOException e) {
					if (!display.isDisposed()) {
						display.syncExec(new Runnable() {
							public void run() {
								GeneralPurposeDialogs.warningMessage(display.getActiveShell(), e);
							}
						});
					}
				}
			}
		}.start();
	}

	public static void createDir(final File dir) {
		if (!dir.exists() && !dir.mkdirs()) {
			System.err.println(Settings.getInstance().msg("general.error.createdir", new Object[] {dir}));
		}
	}

	public static void copyFiles(final File srcDir, final File dstDir) {
		File[] srcFiles = srcDir.listFiles();
		if (srcFiles != null) {
			for (File src: srcFiles) {
				if (src.isFile()) {
					File dst = new File(dstDir, src.getName());
					try {
						org.apache.commons.io.FileUtils.copyFile(src, dst, true);
					} catch (IOException e) {
						System.err.println(Settings.getInstance().msg("general.error.copyfile", new Object[] {src, dst}));
					}
				}
			}
		}
	}

	public static void fileSetLastModified(final File file, final long time) {
		if (!file.setLastModified(time)) {
			System.err.println(Settings.getInstance().msg("general.error.setlastmodifiedfile", new Object[] {file.getPath()}));
		}
	}

	public static void fileSetReadOnly(final File file) {
		if (!file.setReadOnly())
			System.err.println(Settings.getInstance().msg("general.error.setreadonlyfile", new Object[] {file.getPath()}));
	}

	public static void removeFile(final File file) {
		if (!(file.isFile() && file.delete())) {
			System.err.println(Settings.getInstance().msg("general.error.deletefile", new Object[] {file.getPath()}));
		}
	}

	public static void removeFilesInDirAndDir(final File dir) {
		File[] files = dir.listFiles();
		if (files != null) {
			for (File file: files) {
				if (file.isDirectory()) {
					System.err.println(Settings.getInstance().msg("general.error.dirtobedeletedcontainsdir", new Object[] {dir.getPath()}));
					return;
				}
			}
			for (File file: files) {
				if (!file.delete()) {
					System.err.println(Settings.getInstance().msg("general.error.deletefile", new Object[] {file}));
				}
			}
		}
		if (!(dir.isDirectory() && dir.delete())) {
			System.err.println(Settings.getInstance().msg("general.error.deletedir", new Object[] {dir.getPath()}));
		}
	}

	public static boolean isReadableFile(final File file) {
		return file.isFile() && file.canRead();
	}

	public static void createDirIfNecessary(File dir) {
		if (!dir.isDirectory()) {
			System.out.println(Settings.getInstance().msg("general.notice.createdir", new Object[] {dir.getPath()}));
			if (!dir.mkdirs()) {
				System.err.println(Settings.getInstance().msg("general.error.createdir", new Object[] {dir.getPath()}));
			}
		}
	}

	public static boolean isExecutable(final String filename) {
		for (String ext: EXECUTABLES) {
			if (filename.toLowerCase().endsWith(ext)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isArchive(final String filename) {
		for (String ext: ARCHIVES) {
			if (filename.toLowerCase().endsWith(ext)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isPhysFS(final String mountPath) {
		for (String ext: ARCHIVES) {
			if (mountPath.toLowerCase().endsWith(ext + ':' + File.separatorChar)) {
				return true;
			}
		}
		return false;
	}

	public static int containsPhysFS(final String mountPath) {
		for (String ext: ARCHIVES) {
			int idx = mountPath.toLowerCase().indexOf(ext + ':' + File.separatorChar);
			if (idx != -1) {
				return idx + ext.length();
			}
		}
		return -1;
	}

	public static boolean isFatImage(final String filename) {
		for (String ext: FATIMAGES) {
			if (filename.toLowerCase().endsWith(ext)) {
				return true;
			}
		}
		return false;
	}

	public static int containsFatImage(final String mountPath) {
		for (String ext: FATIMAGES) {
			int idx = mountPath.toLowerCase().indexOf(ext + File.separatorChar);
			if (idx != -1) {
				return idx + ext.length();
			}
		}
		return -1;
	}

	public static boolean isBooterImage(final String filename) {
		for (String ext: BOOTERIMAGES) {
			if (filename.toLowerCase().endsWith(ext)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isConfFile(final String filename) {
		return filename.toLowerCase().endsWith(CONF_EXT);
	}

	public static boolean isGamePackArchiveFile(final String filename) {
		return filename.toLowerCase().endsWith(GAMEPACKARCHIVE_EXT);
	}

	public static boolean isIsoFile(final String filename) {
		for (String ext: CDIMAGES) {
			if (filename.toLowerCase().endsWith(ext)) {
				return true;
			}
		}
		return false;
	}

	public static int containsIso(final String mountPath) {
		for (String ext: CDIMAGES) {
			int idx = mountPath.toLowerCase().indexOf(ext + File.separatorChar);
			if (idx != -1) {
				return idx + ext.length();
			}
		}
		return -1;
	}

	public static boolean isPicture(final String filename) {
		for (String ext: PICTURES) {
			if (filename.toLowerCase().endsWith(ext)) {
				return true;
			}
		}
		return false;
	}

	public static int findSetupIndex(final java.util.List<File> files) {
		for (String setupFileName: SETUPFILES) {
			for (int i = 0; i < files.size(); i++) {
				if (files.get(i).getName().toLowerCase().equals(setupFileName))
					return i;
			}
		}
		return -1;
	}

	public static int findMostLikelyMainIndex(final String title, final java.util.List<File> files) {
		List<File> mostLikelyFiles = new ArrayList<File>();
		for (File f: files) {
			if (!Arrays.asList(SETUPFILES).contains(f.getName().toLowerCase()) && !Arrays.asList(UNLIKELYMAINFILES).contains(f.getName().toLowerCase())) {
				mostLikelyFiles.add(f);
			}
		}
		if (mostLikelyFiles.isEmpty())
			return StringRelatedUtils.findBestMatchIndex(title, getNames(files.toArray(new File[0])));
		return files.indexOf(mostLikelyFiles.get(StringRelatedUtils.findBestMatchIndex(title, getNames(mostLikelyFiles.toArray(new File[0])))));
	}

	public static File getCanMainFile(final File file) {
		String f = file.getPath();
		int isoIdx = containsIso(f);
		int pfsIdx = containsPhysFS(f);
		int fatIdx = containsFatImage(f);
		if (isoIdx != -1) {
			return canonicalToDosroot(f.substring(0, isoIdx));
		} else if (pfsIdx != -1) {
			return canonicalToDosroot(f.substring(0, pfsIdx));
		} else if (fatIdx != -1) {
			return canonicalToDosroot(f.substring(0, fatIdx));
		}
		return canonicalToDosroot(file.getPath());
	}

	public static List<File> getExecutablesInDirRecursive(final File dir) {
		List<File> executables = new ArrayList<File>(org.apache.commons.io.FileUtils.listFiles(dir, EXECUTABLES_UPPERCASE, true));
		Collections.sort(executables, new FileComparator());
		return executables;
	}

	public static String[] getExecutablesInZipOrIsoOrFat(final String archive) throws IOException {
		List<String> result = new ArrayList<String>();
		File arcFile = new File(archive);

		if (archive.toLowerCase().endsWith(ARCHIVES[0])) { // zip
			ZipFile zfile = new ZipFile(arcFile);
			for (Enumeration<? extends ZipEntry> entries = zfile.entries(); entries.hasMoreElements();) {
				ZipEntry entry = entries.nextElement();
				String name = entry.getName();
				if (!entry.isDirectory() && isExecutable(name)) {
					result.add(PlatformUtils.archiveToNativePath(name));
				}
			}
			zfile.close();
		} else if (archive.toLowerCase().endsWith(ARCHIVES[1])) { // 7-zip
			MyRandomAccessFile istream = new MyRandomAccessFile(archive, "r");
			IInArchive zArchive = new Handler();
			if (zArchive.Open(istream) != 0) {
				throw new IOException(Settings.getInstance().msg("general.error.opensevenzip", new Object[] {archive}));
			}
			for (int i = 0; i < zArchive.size(); i++) {
				SevenZipEntry entry = zArchive.getEntry(i);
				String name = entry.getName();
				if (!entry.isDirectory() && isExecutable(name)) {
					result.add(PlatformUtils.archiveToNativePath(name));
				}
			}
			zArchive.close();
		} else if (isIsoFile(archive)) {
			ISO9660FileSystem iso = new ISO9660FileSystem(new File(archive));
			for (Enumeration<ISO9660FileEntry> entries = iso.getEntries(); entries.hasMoreElements();) {
				ISO9660FileEntry entry = entries.nextElement();
				String name = entry.getPath();
				if (!entry.isDirectory() && isExecutable(name)) {
					result.add(PlatformUtils.archiveToNativePath(name));
				}
			}
			iso.close();
		} else if (isFatImage(archive)) {
			BlockDevice device = new FileDisk(new File(archive));
			result.addAll(readFatEntries(new FatFileSystem(device).getRoot(), ""));
			device.close();
		}

		Collections.sort(result, new FilenameComparator());
		return result.toArray(new String[result.size()]);
	}

	private static String fsDirectoryEntryNameToFileName(final String name) {
		if (name.length() > 8)
			return name.substring(0, 8).trim() + '.' + name.substring(8).trim();
		else
			return name;
	}

	private static List<String> readFatEntries(final FatLfnDirectory dir, final String dirPath) throws IOException {
		List<String> result = new ArrayList<String>();
		for (Iterator<FatLfnDirectoryEntry> entries = dir.iterator(); entries.hasNext();) {
			FatLfnDirectoryEntry entry = entries.next();
			String name = fsDirectoryEntryNameToFileName(entry.getShortName());
			if (entry.isDirectory() && !name.equals(".") && !name.equals("..")) {
				result.addAll(readFatEntries(entry.getDirectory(), dirPath + name + File.separatorChar));
			} else if (entry.isFile() && isExecutable(name)) {
				result.add(dirPath + name);
			}
		}
		return result;
	}

	public static void zipEntry(final File orgFile, final File fileEntry, final ZipOutputStream zos) throws IOException {
		ZipEntry anEntry = new ZipEntry(PlatformUtils.toArchivePath(fileEntry, orgFile.isDirectory()));
		anEntry.setTime(orgFile.lastModified());
		if (orgFile.isFile() && !orgFile.canWrite())
			anEntry.setExtra(new byte[] {1});
		zos.putNextEntry(anEntry);

		if (orgFile.isFile()) {
			byte[] readBuffer = new byte[ZIP_BUFFER];
			int bytes = 0;
			FileInputStream is = new FileInputStream(orgFile);
			while ((bytes = is.read(readBuffer)) != -1)
				zos.write(readBuffer, 0, bytes);
			is.close();
		}
		zos.closeEntry();
	}

	public static void zipDir(final File dirToZip, final ZipOutputStream zos, final File dstDirInZip) throws IOException {
		zipDir(dirToZip, zos, dirToZip, dstDirInZip);
	}

	private static void zipDir(final File dirToZip, final ZipOutputStream zos, final File zipRootDir, final File dstDirInZip) throws IOException {
		String[] dirList = dirToZip.list();
		if (dirList == null)
			throw new IOException(Settings.getInstance().msg("general.error.opendir", new Object[] {dirToZip}));
		for (int i = 0; i < dirList.length; i++) {
			File f = new File(dirToZip, dirList[i]);
			zipEntry(f, new File(dstDirInZip, makeRelativeTo(f, zipRootDir).getPath()), zos);
			if (f.isDirectory()) {
				zipDir(f, zos, zipRootDir, dstDirInZip);
			}
		}
	}

	public static long extractZipSizeInBytes(final File archive, final File dirToBeExtracted) throws IOException {
		long bytes = 0;
		ZipFile zf = new ZipFile(archive);
		for (Enumeration<? extends ZipEntry> entries = zf.entries(); entries.hasMoreElements();) {
			ZipEntry entry = entries.nextElement();
			if (areRelated(dirToBeExtracted, new File(entry.getName())))
				bytes += entry.getSize();
		}
		zf.close();
		return bytes;
	}

	public static long extractZipEntrySizeInBytes(final File archive, final String zipEntryToBeExtracted) throws IOException {
		ZipFile zf = new ZipFile(archive);
		ZipEntry entry = zf.getEntry(zipEntryToBeExtracted);
		zf.close();
		if (entry != null)
			return entry.getSize();
		return 0;
	}

	public static void extractZip(final File archive, final File dirToBeExtracted, final File dstDir, final ProgressNotifyable prog) throws IOException {
		ZipFile zf = new ZipFile(archive);
		for (Enumeration<? extends ZipEntry> entries = zf.entries(); entries.hasMoreElements();) {
			ZipEntry entry = entries.nextElement();
			if (areRelated(dirToBeExtracted, new File(entry.getName()))) {
				File dstFile = new File(dstDir, strip(new File(entry.getName()), dirToBeExtracted).getPath());
				extractEntry(zf, entry, dstFile, prog);
			}
		}
		zf.close();
	}

	public static void extractZip(final File archive, final String zipEntryToBeExtracted, final File dstFile, final ProgressNotifyable prog) throws IOException {
		ZipFile zf = new ZipFile(archive);
		ZipEntry entry = zf.getEntry(zipEntryToBeExtracted);
		if (entry != null)
			extractEntry(zf, entry, dstFile, prog);
		zf.close();
	}

	public static void extractEntry(final ZipFile zf, final ZipEntry srcEntry, final File dstFile, final ProgressNotifyable prog) throws IOException {
		File foundDstFile = null, temporarilyRenamedFile = null;
		if (PlatformUtils.IS_WINDOWS && dstFile.getName().contains("~")) {
			foundDstFile = dstFile.getCanonicalFile();
			if (!foundDstFile.getName().equals(dstFile.getName()) && foundDstFile.exists()) {
				temporarilyRenamedFile = getUniqueFileName(foundDstFile);
				foundDstFile.renameTo(temporarilyRenamedFile);
			}
		}

		if (dstFile.exists())
			throw new IOException(Settings.getInstance().msg("general.error.filetobeextractedexists", new Object[] {dstFile}));
		if (srcEntry.isDirectory()) {
			if (!dstFile.exists())
				createDir(dstFile);
		} else {
			if (dstFile.getParentFile() != null)
				createDir(dstFile.getParentFile());
			FileOutputStream fos = new FileOutputStream(dstFile);
			InputStream is = zf.getInputStream(srcEntry);
			byte[] readBuffer = new byte[ZIP_BUFFER];
			int bytesIn;
			while ((bytesIn = is.read(readBuffer)) != -1)
				fos.write(readBuffer, 0, bytesIn);
			fos.flush();
			fos.close();
			is.close();
			byte[] extra = srcEntry.getExtra();
			if ((extra != null) && (extra.length == 1) && (extra[0] == 1))
				fileSetReadOnly(dstFile);
		}
		fileSetLastModified(dstFile, srcEntry.getTime());

		if (foundDstFile != null && temporarilyRenamedFile != null)
			temporarilyRenamedFile.renameTo(foundDstFile);

		prog.incrProgress((int)(srcEntry.getSize() / 1024));
	}

	public static File determineDstSevenzipFile(File srcDir, File dstDir, String entryName) {
		return canonicalTo(dstDir, strip(new File(entryName), srcDir).getPath());
	}

	public static int[] findRelatedEntryIds(IInArchive zArchive, File dirToBeExtracted) {
		List<Integer> result = new ArrayList<Integer>();
		for (int i = 0; i < zArchive.size(); i++)
			if (areRelated(dirToBeExtracted, new File(zArchive.getEntry(i).getName())))
				result.add(i);
		return ArrayUtils.toPrimitive(result.toArray(new Integer[0]));
	}

	public static int findEntryId(IInArchive zArchive, String filenameToBeExtracted) {
		for (int i = 0; i < zArchive.size(); i++)
			if (zArchive.getEntry(i).getName().equals(filenameToBeExtracted))
				return i;
		return -1;
	}

	private static File strip(final File file, final File basePath) {
		if (file.equals(basePath))
			return new File(".");
		File remainder = new File(file.getName());
		File parent = file.getParentFile();
		while (parent != null) {
			if (parent.equals(basePath))
				return remainder;
			remainder = new File(parent.getName(), remainder.getPath());
			parent = parent.getParentFile();
		}
		return file;
	}

	public static String getUrlFromFile(final File file) throws MalformedURLException {
		return file.toURI().toURL().toString();
	}

	public static String fileSystemSafe(String s) {
		return s.replaceAll(INVALID_FILENAME_CHARS_REGEXP, "");
	}

	public static String fileSystemSafeWebImages(String s) {
		return s.replaceAll(" ", "_").replaceAll(INVALID_FILENAME_CHARS_REGEXP, "");
	}

	public static File getUniqueFileName(final File src) {
		return new File(src.getParentFile(), UUID.randomUUID() + "__" + src.getName());
	}

	public static File getTmpInstallFile() {
		return TMPINST_DIR_FILE;
	}

	public static boolean isStoredOnFloppyDrive(File file) {
		if (PlatformUtils.IS_OSX)
			return false; // FileSystemView doesn't work on OSX and leads to freeze on application exit

		FileSystemView fsv = FileSystemView.getFileSystemView();
		for (File f: File.listRoots()) {
			if (areRelated(f, file)) {
				return fsv.isFloppyDrive(f);
			}
		}
		return false;
	}

	public static boolean isStoredOnCDRomDrive(File file) {
		if (PlatformUtils.IS_OSX)
			return false; // FileSystemView doesn't work on OSX and leads to freeze on application exit

		FileSystemView fsv = FileSystemView.getFileSystemView();
		for (File f: File.listRoots()) {
			if (areRelated(f, file)) {
				return fsv.isDrive(f) && fsv.getSystemTypeDescription(f).toUpperCase().contains("CD");
			}
		}
		return false;
	}

	private static String[] getNames(File[] files) {
		String[] result = new String[files.length];
		for (int i = 0; i < files.length; i++)
			result[i] = files[i].getName();
		return result;
	}

	public static File[] findFileSequence(File f) {
		List<File> result = new ArrayList<File>();
		result.add(f);

		int i = 1;
		String name = FilenameUtils.removeExtension(f.getName());
		String ext = FilenameUtils.getExtension(f.getName());

		if (name.endsWith(String.valueOf(i))) {
			File dir = f.getParentFile();
			if (dir != null) {
				File[] files = dir.listFiles(new FileFilter() {
					public boolean accept(File f) {
						return f.isFile();
					}
				});
				String[] fileNames = getNames(files);
				if (files != null) {
					int index;
					do {
						i++;
						String nextFileName = StringUtils.chop(name) + String.valueOf(i) + FilenameUtils.EXTENSION_SEPARATOR + ext;
						index = ArrayUtils.indexOf(fileNames, nextFileName);
						if (index >= 0) {
							result.add(files[index]);
						}
					} while (index >= 0);
				}
			}
		}

		return result.toArray(new File[0]);
	}

	public static String[] getShaders() {
		File shadersDir = new File(DOSROOT_DIR_FILE, "SHADERS");
		if (shadersDir.exists() && shadersDir.isDirectory()) {
			List<String> result = new ArrayList<String>();
			Iterator<File> it = org.apache.commons.io.FileUtils.iterateFiles(shadersDir, new String[] {"fx"}, false);
			while (it.hasNext())
				result.add(it.next().getName());
			Collections.sort(result);
			return result.toArray(new String[0]);
		}
		return null;
	}

	public static Set<ShortFile> listShortFiles(File dir) {
		Set<ShortFile> result = new TreeSet<ShortFile>();
		for (File file: dir.listFiles())
			result.add(createShortFile(file, result));
		return result;
	}

	public static List<File> convertToShortFileSet(List<File> fileList) {
		Set<ShortFile> result = new TreeSet<ShortFile>();
		for (File file: fileList)
			result.add(createShortFile(file, result));
		List<File> res = new ArrayList<File>();
		for (ShortFile file: result)
			res.add(new File(file.getFile().getParentFile(), file.getName()));
		return res;
	}

	private static ShortFile createShortFile(File file, Set<ShortFile> curDir) {
		String filename = file.getName().toUpperCase();
		boolean createShort = false;
		if (StringUtils.contains(filename, ' ')) {
			filename = StringUtils.remove(filename, ' ');
			createShort = true;
		}
		int len = 0;
		int idx = filename.indexOf('.');
		if (idx != -1) {
			if (filename.length() - idx - 1 > 3) {
				filename = StringUtils.stripStart(filename, ".");
				createShort = true;
			}
			idx = filename.indexOf('.');
			len = (idx != -1) ? idx: filename.length();
		} else {
			len = filename.length();
		}
		createShort |= len > 8;

		ShortFile shortFile = null;
		if (!createShort) {
			shortFile = new ShortFile(file, StringUtils.removeEnd(filename, "."));
		} else {
			int i = 1;
			do {
				String nr = String.valueOf(i++);
				StringBuffer sb = new StringBuffer(StringUtils.left(filename, Math.min(8 - nr.length() - 1, len)));
				sb.append('~').append(nr);
				idx = filename.lastIndexOf('.');
				if (idx != -1)
					sb.append(StringUtils.left(filename.substring(idx), 4));
				shortFile = new ShortFile(file, StringUtils.removeEnd(sb.toString(), "."));
			} while (shortFile.isContainedIn(curDir));
		}
		return shortFile;
	}
}
