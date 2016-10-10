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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import org.eclipse.swt.program.Program;
import org.apache.commons.lang3.StringUtils;
import org.dbgl.gui.StreamGobbler;
import org.dbgl.model.DosboxVersion;
import org.dbgl.model.Profile;
import org.dbgl.model.SearchResult;
import org.dbgl.model.SearchResult.ResultType;
import org.dbgl.model.conf.Settings;
import javax.swing.filechooser.FileSystemView;


public final class PlatformUtils {

	private static final String DEFAULT_DOSBOX_DIR = "DOSBox-" + DosboxVersion.LATEST;

	public static final String OS_NAME = System.getProperty("os.name");
	public static final String OS_ARCH = System.getProperty("os.arch");
	public static final String OS_VERSION = System.getProperty("os.version");
	public static final String JVM_ARCH = System.getProperty("sun.arch.data.model");
	public static final String JVM_VERSION = System.getProperty("java.version");
	public static final boolean IS_WINDOWS = OS_NAME.startsWith("Windows");
	public static final boolean IS_LINUX = OS_NAME.startsWith("Linux");
	public static final boolean IS_OSX = OS_NAME.startsWith("Mac OS");
	public static final String OSX_COCOA = "cocoa";
	public static final String DB_EXECUTABLE = IS_WINDOWS ? "DOSBox.exe": IS_OSX ? "DOSBox": "dosbox";
	public static final String DB_APP_EXT = ".app";
	public static final String DB_APP = "DOSBox" + DB_APP_EXT;
	public static final String DB_APP_EXE = "/Contents/MacOS/" + DB_EXECUTABLE;
	public static final String EOLN = System.getProperty("line.separator");
	public static final String NATIVE_EXE_FILTER = IS_WINDOWS ? "*.exe;*.EXE": IS_OSX ? "*.app": "*";
	public static final boolean USE_USER_HOME_DIR = Boolean.valueOf(System.getProperty("dbgl.data.localappdata")) || Boolean.valueOf(System.getProperty("dbgl.data.userhome"));
	public static final String DFEND_PATH = System.getenv("ProgramFiles") + "\\D-Fend\\";
	public static final String DFEND_PROFILES = "Profiles.dat";
	public static final File DFEND_RELOADED_PATH = new File(System.getProperty("user.home"), "D-Fend Reloaded");

	public static final File USER_DATA_DIR_FILE;

	static {
		if (IS_WINDOWS) {
			USER_DATA_DIR_FILE = new File(System.getenv("LOCALAPPDATA"), "/DBGL");
		} else if (IS_LINUX || IS_OSX) {
			USER_DATA_DIR_FILE = new File(System.getProperty("user.home"), "/.dbgl");
		} else {
			USER_DATA_DIR_FILE = new File(".");
		}
	}

	public static String toNativePath(final String dbFileLocation) {
		return dbFileLocation.replace('\\', File.separatorChar);
	}

	public static String toDosboxPath(final String hostFileLocation) {
		return hostFileLocation.replace(File.separatorChar, '\\');
	}

	public static String archiveToNativePath(final String archiveEntry) {
		// 7zip uses / as file seperator but to be safe in the future
		// both \\ and / are converted to the host seperator
		return archiveEntry.replace('/', File.separatorChar).replace('\\', File.separatorChar);
	}

	public static String toArchivePath(final String archiveFileEntry) {
		return toArchivePath(new File(archiveFileEntry), false);
	}

	public static String toArchivePath(final File archiveEntry, final boolean isDirectory) {
		String result = archiveEntry.getPath().replace('\\', '/');
		if (isDirectory) {
			result += "/";
		}
		return result;
	}

	public static String pathToNativePath(final String path) {
		if (path != null && path.indexOf("://") == -1)
			return archiveToNativePath(path);
		return path;
	}

	private static boolean tryToRun(String[] executables, String param) {
		for (String exe: executables) {
			try {
				Runtime.getRuntime().exec(new String[] {exe, param}, null, null);
				return true;
			} catch (Exception e) {
				// try next in line
			}
		}
		return false;
	}

	public static void openForEditing(final File file) {
		if (!Program.launch(file.getPath()) && (IS_LINUX && !tryToRun(new String[] {"xdg-open", "gnome-open"}, file.getPath())))
			System.err.println(Settings.getInstance().msg("general.error.openfile", new Object[] {file}));
	}

	public static void openForBrowsing(final String url) {
		if (!Program.launch(url) && (IS_LINUX && !tryToRun(new String[] {"xdg-open", "gnome-open"}, url)))
			System.err.println(Settings.getInstance().msg("general.error.openurl", new Object[] {url}));
	}

	public static void openDirForViewing(final File file) {
		if (!Program.launch(file.getPath()) && (IS_LINUX && !tryToRun(new String[] {"nautilus", "dolphin", "kfmclient"}, file.getPath())))
			System.err.println(Settings.getInstance().msg("general.error.opendir", new Object[] {file}));
	}

	public static void createShortcut(final Profile profile, final List<DosboxVersion> dbversionsList) throws IOException {
		DosboxVersion dbversion = DosboxVersion.findById(dbversionsList, profile.getDbversionId());
		String strictFilename = profile.getTitle().replaceAll("[\\/:*?\"<>|]", " ").trim();
		StringBuffer params = new StringBuffer(128);
		if (dbversion.isMultiConfig()) {
			params.append("-conf \"\"").append(dbversion.getCanonicalConfFile()).append("\"\" ");
		}
		params.append("-conf \"\"").append(profile.getCanonicalConfFile()).append("\"\"");
		if (Settings.getInstance().getSettings().getBooleanValue("dosbox", "hideconsole")) {
			params.append(" -noconsole");
		}

		if (IS_WINDOWS) {

			File desktopDir = FileSystemView.getFileSystemView().getHomeDirectory();
			File lnkFile = new File(desktopDir, strictFilename + ".lnk");
			File vbsFile = FileUtils.canonicalToData("shortcut.vbs");
			BufferedWriter vbsWriter = new BufferedWriter(new FileWriter(vbsFile));
			vbsWriter.write("Set oWS = WScript.CreateObject(\"WScript.Shell\")" + EOLN);
			vbsWriter.write("Set oLink = oWS.CreateShortcut(\"" + lnkFile.getCanonicalPath() + "\")" + EOLN);
			vbsWriter.write("oLink.TargetPath = \"" + dbversion.getCanonicalExecutable().getPath() + "\"" + EOLN);
			vbsWriter.write("oLink.Arguments = \"" + params.toString() + "\"" + EOLN);
			vbsWriter.write("oLink.Description = \"" + Settings.getInstance().msg("general.shortcut.title", new Object[] {strictFilename}) + "\"" + EOLN);
			vbsWriter.write("oLink.WorkingDirectory = \"" + FileUtils.getDosRoot() + "\"" + EOLN);
			vbsWriter.write("oLink.Save" + EOLN);
			vbsWriter.close();
			Process proc = Runtime.getRuntime().exec(new String[] {"CSCRIPT", vbsFile.getCanonicalPath()}, null, vbsFile.getParentFile());
			StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "CSCRIPT stderr");
			StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), "CSCRIPT stdout");
			outputGobbler.start();
			errorGobbler.start();
			try {
				proc.waitFor();
			} catch (InterruptedException e) {}
			FileUtils.removeFile(vbsFile);

		} else if (IS_LINUX) {

			File desktopDir = new File(System.getProperty("user.home"), "/Desktop");
			File desktopFile = new File(desktopDir, strictFilename + ".desktop");
			BufferedWriter desktopWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(desktopFile), "UTF-8"));
			desktopWriter.write("[Desktop Entry]" + EOLN);
			desktopWriter.write("Version=1.0" + EOLN);
			desktopWriter.write("Type=Application" + EOLN);
			desktopWriter.write("Name=" + strictFilename + EOLN);
			desktopWriter.write("Comment=" + Settings.getInstance().msg("general.shortcut.title", new Object[] {strictFilename}) + EOLN);
			desktopWriter.write("Icon=" + new File(dbversion.getCanonicalExecutable().getParent(), "dosbox.ico").getPath() + EOLN);
			desktopWriter.write("TryExec=" + dbversion.getCanonicalExecutable().getPath() + EOLN);
			desktopWriter.write("Exec=" + dbversion.getCanonicalExecutable().getPath() + " " + StringUtils.replace(params.toString(), "\"\"", "\"") + EOLN);
			desktopWriter.write("Path=" + FileUtils.getDosRoot() + EOLN);
			desktopWriter.close();
			desktopFile.setExecutable(true);

		}
	}

	public static boolean isDirectoryWritable(final File dir) {
		try {
			File.createTempFile("chkperm", null, dir).delete();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public static SearchResult findDosbox() {
		File canExePath = null;
		File canConf = null;
		File canConfSuggestion = null;
		String version = null;

		if (FileUtils.isExistingDirectory(FileUtils.canonicalToDosbox(DEFAULT_DOSBOX_DIR))) {
			File exe = FileUtils.constructCanonicalDBExeLocation(DEFAULT_DOSBOX_DIR);
			if (FileUtils.isExistingFile(exe)) {
				canExePath = exe.getParentFile();
				version = DosboxVersion.LATEST;
			}
		}

		if (canExePath == null) {
			if (IS_WINDOWS) {
				String programFiles = System.getenv("ProgramFiles(x86)");
				if (programFiles == null)
					programFiles = System.getenv("ProgramFiles");

				File exePF = new File(new File(programFiles, DEFAULT_DOSBOX_DIR), DB_EXECUTABLE);
				if (FileUtils.isExistingFile(exePF)) {
					canExePath = exePF.getParentFile();
					version = DosboxVersion.LATEST;
				}
			} else if (IS_LINUX) {
				File exePF = new File("/usr/bin", DB_EXECUTABLE);
				if (FileUtils.isExistingFile(exePF)) {
					canExePath = exePF.getParentFile();
				}
			} else if (IS_OSX) {
				File exePF = new File("/Applications/DOSBox.app/Contents/MacOS", DB_EXECUTABLE);
				if (FileUtils.isExistingFile(exePF)) {
					canExePath = exePF.getParentFile();
				}
			}
		}

		if (canExePath != null) {
			File conf = new File(canExePath, FileUtils.DOSBOX_CONF);
			if (FileUtils.isExistingFile(conf)) {
				canConf = conf;
			} else {
				if (IS_WINDOWS) {
					File confLAD = new File(new File(System.getenv("LOCALAPPDATA"), "DOSBox"), "dosbox-" + DosboxVersion.LATEST + ".conf");
					if (FileUtils.isExistingFile(confLAD)) {
						canConf = confLAD;
						version = DosboxVersion.LATEST;
					} else {
						canConfSuggestion = confLAD;
					}
				} else if (IS_LINUX) {
					File confLAD = new File(new File(System.getProperty("user.home"), ".dosbox"), "dosbox-" + DosboxVersion.LATEST + ".conf");
					if (FileUtils.isExistingFile(confLAD)) {
						canConf = confLAD;
						version = DosboxVersion.LATEST;
					} else {
						canConfSuggestion = confLAD;
					}
				} else if (IS_OSX) {
					File confLAD = new File(new File(System.getProperty("user.home"), "Library/Preferences"), "DOSBox " + DosboxVersion.LATEST + " Preferences");
					if (FileUtils.isExistingFile(confLAD)) {
						canConf = confLAD;
						version = DosboxVersion.LATEST;
					} else {
						canConfSuggestion = confLAD;
					}
				}
			}
		}

		DosboxVersion db = new DosboxVersion(-1, "DOSBox " + (version != null ? version: DosboxVersion.LATEST), canExePath != null ? FileUtils.makeRelativeToDosbox(canExePath).getPath(): "",
				canConf != null ? FileUtils.makeRelativeToDosbox(canConf).getPath(): canConfSuggestion != null ? FileUtils.makeRelativeToDosbox(canConfSuggestion).getPath(): "", true, false, true, "",
				version != null ? version: DosboxVersion.LATEST, null, null, null, 0);

		ResultType res = ResultType.NOTFOUND;
		if (canExePath != null) {
			res = ResultType.EXEONLY;

			if (canConf != null && version != null) {
				res = ResultType.COMPLETE;
			}
		}

		return new SearchResult(res, db);
	}
}
