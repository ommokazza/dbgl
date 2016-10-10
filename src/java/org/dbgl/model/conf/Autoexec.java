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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.dbgl.exception.InvalidMountstringException;
import org.dbgl.model.Mount;
import org.dbgl.model.Mount.MountingType;
import org.dbgl.util.FileUtils;
import org.dbgl.util.PlatformUtils;


public final class Autoexec {

	private final static String[] CUSTOM_SECTION_MARKERS = {"@REM START", "@REM /START", "@REM PRE-LAUNCH", "@REM /PRE-LAUNCH", "@REM POST-LAUNCH", "@REM /POST-LAUNCH", "@REM FINISH", "@REM /FINISH"};
	public final static int SECTIONS = CUSTOM_SECTION_MARKERS.length / 2;

	private final static Pattern LOADHIGH_PATRN = Pattern.compile("^(?:lh|loadhigh)\\s+(.*)$", Pattern.CASE_INSENSITIVE);
	private final static Pattern LOADFIX_PATRN = Pattern.compile("^(?:loadfix\\s+(-f)\\s*|loadfix(\\s+-\\d+)?(\\s+.*)?)$", Pattern.CASE_INSENSITIVE);
	private final static Pattern BOOT_PATRN = Pattern.compile("^(?:boot)(?=\\s+.\\S+)((?:\\s+[^-][^\\s]*)*)(?:\\s+-l\\s+([acd]):?)?\\s*$", Pattern.CASE_INSENSITIVE);
	private final static Pattern BOOTIMGS_PATRN = Pattern.compile("^((?:\"[^\"]+\")|(?:[^\\s]+))(?:\\s((?:\"[^\"]+\")|(?:[^\\s]+)))?(?:\\s((?:\"[^\"]+\")|(?:[^\\s]+)))?$", Pattern.CASE_INSENSITIVE);

	List<Mount> mountingpoints;
	Boolean loadfix, loadhigh;
	int loadfixValue;
	String main;
	String img1, img2, img3;
	String imgDriveletter;
	Boolean exit;
	String params;
	String mixer;
	String keyb;
	String ipxnet;
	Boolean pause;
	String[] customSections;

	Autoexec() {
		init();
	}

	Autoexec(final Autoexec other) {
		mountingpoints = new ArrayList<Mount>();
		for (Mount m: other.mountingpoints)
			mountingpoints.add(new Mount(m));
		loadfix = other.loadfix;
		loadhigh = other.loadhigh;
		loadfixValue = other.loadfixValue;
		main = other.main;
		img1 = other.img1;
		img2 = other.img2;
		img3 = other.img3;
		imgDriveletter = other.imgDriveletter;
		exit = other.exit;
		params = other.params;
		mixer = other.mixer;
		keyb = other.keyb;
		ipxnet = other.ipxnet;
		pause = other.pause;
		customSections = new String[SECTIONS];
		for (int i = 0; i < SECTIONS; i++)
			customSections[i] = other.customSections[i];
	}

	void init() {
		mountingpoints = new ArrayList<Mount>();
		loadfix = false;
		loadhigh = false;
		loadfixValue = 0;
		main = StringUtils.EMPTY;
		img1 = StringUtils.EMPTY;
		img2 = StringUtils.EMPTY;
		img3 = StringUtils.EMPTY;
		imgDriveletter = StringUtils.EMPTY;
		exit = false;
		params = StringUtils.EMPTY;
		mixer = StringUtils.EMPTY;
		keyb = StringUtils.EMPTY;
		ipxnet = StringUtils.EMPTY;
		pause = false;
		customSections = new String[SECTIONS];
		for (int i = 0; i < SECTIONS; i++)
			customSections[i] = StringUtils.EMPTY;
	}

	static List<Mount> getUniqueMountingpoints(final Autoexec base, final Autoexec ext) {
		List<Mount> result = new ArrayList<Mount>();
		int start = base == null ? 0: base.mountingpoints.size();
		for (int i = start; i < ext.mountingpoints.size(); i++) {
			result.add(new Mount(ext.mountingpoints.get(i)));
		}
		return result;
	}

	void parseLines(final List<String> orgLines) {
		char driveletter = '\0';
		String remainder = StringUtils.EMPTY;
		String executable = StringUtils.EMPTY;
		String image1 = StringUtils.EMPTY;
		String image2 = StringUtils.EMPTY;
		String image3 = StringUtils.EMPTY;
		int exeIndex = -1;

		String customEchos = StringUtils.EMPTY;
		List<String> leftOvers = new ArrayList<String>();
		int customSection = -1;

		line: for (String orgLine: orgLines) {
			orgLine = orgLine.trim();

			for (int i = 0; i < SECTIONS; i++) {
				if (orgLine.startsWith(CUSTOM_SECTION_MARKERS[i * 2])) {
					customSection = i;
					continue line;
				}
			}

			if (customSection > -1) {
				if (orgLine.startsWith(CUSTOM_SECTION_MARKERS[customSection * 2 + 1]))
					customSection = -1;
				else
					customSections[customSection] += orgLine + PlatformUtils.EOLN;
				continue;
			}

			orgLine = StringUtils.stripStart(orgLine, "@").trim();

			Matcher loadhighMatcher = LOADHIGH_PATRN.matcher(orgLine);
			Matcher loadfixMatcher = LOADFIX_PATRN.matcher(orgLine);
			Matcher bootMatcher = BOOT_PATRN.matcher(orgLine);

			if (loadhighMatcher.matches()) {
				loadhigh = true;
				orgLine = loadhighMatcher.group(1).trim();
			}

			if (loadfixMatcher.matches()) {
				if (!loadfix) {
					loadfixValue = 64;
					loadfix = true;
				}
				if (loadfixMatcher.group(1) != null) // -f
					continue;
				if (loadfixMatcher.group(2) != null) {
					try {
						loadfixValue = Integer.parseInt(loadfixMatcher.group(2).trim().substring(1));
					} catch (NumberFormatException e) {
						// use default value of 64
					}
				}
				if (loadfixMatcher.group(3) == null)
					continue;
				orgLine = loadfixMatcher.group(3).trim();
			}

			String line = orgLine.toLowerCase();

			if (StringUtils.isEmpty(line)) {
				continue;
			} else if (line.startsWith("mount ") || line.startsWith("imgmount ")) {
				addMount(orgLine);
			} else if ((line.endsWith(":") && line.length() == 2) || (line.endsWith(":\\") && line.length() == 3)) {
				driveletter = Character.toUpperCase(line.charAt(0));
				remainder = StringUtils.EMPTY;
			} else if (line.startsWith("cd\\")) {
				if (driveletter != '\0') {
					String add = PlatformUtils.toNativePath(orgLine).substring(2);
					if (add.startsWith("\\"))
						remainder = add;
					else
						remainder = new File(remainder, add).getPath();
				}
			} else if (line.startsWith("cd ")) {
				if (driveletter != '\0') {
					String add = PlatformUtils.toNativePath(orgLine).substring(3);
					if (add.startsWith("\\"))
						remainder = add;
					else
						remainder = new File(remainder, add).getPath();
				}
			} else if (line.startsWith("keyb ") || line.startsWith("keyb.com ")) {
				keyb = orgLine.substring(line.indexOf(' ') + 1);
			} else if (line.startsWith("mixer ") || line.startsWith("mixer.com ")) {
				mixer = orgLine.substring(line.indexOf(' ') + 1);
			} else if (line.startsWith("ipxnet ") || line.startsWith("ipxnet.com ")) {
				ipxnet = orgLine.substring(line.indexOf(' ') + 1);
			} else if (line.equals("pause")) {
				pause = true;
			} else if (line.startsWith("z:\\config.com")) {
				// just ignore
			} else if ((exeIndex = StringUtils.indexOfAny(line, FileUtils.EXECUTABLES)) != -1) {
				executable = orgLine;
				// If there is a space BEFORE executable name, strip everything before it
				int spaceBeforeIndex = executable.lastIndexOf(' ', exeIndex);
				if (spaceBeforeIndex != -1) {
					executable = executable.substring(spaceBeforeIndex + 1);
				}
				// If there is a space AFTER executable name, define it as being parameters
				int spaceAfterIndex = executable.indexOf(' ');
				if (spaceAfterIndex != -1) {
					params = orgLine.substring(spaceBeforeIndex + spaceAfterIndex + 2);
					executable = executable.substring(0, spaceAfterIndex);
				}
			} else if (bootMatcher.matches()) {
				Matcher bootImgsMatcher = BOOTIMGS_PATRN.matcher(bootMatcher.group(1).trim());
				if (bootImgsMatcher.matches()) {
					if (bootImgsMatcher.group(1) != null)
						image1 = StringUtils.strip(bootImgsMatcher.group(1), "\"");
					if (bootImgsMatcher.group(2) != null)
						image2 = StringUtils.strip(bootImgsMatcher.group(2), "\"");
					if (bootImgsMatcher.group(3) != null)
						image3 = StringUtils.strip(bootImgsMatcher.group(3), "\"");
				}
				if (bootMatcher.group(2) != null)
					imgDriveletter = bootMatcher.group(2).trim().toUpperCase();
				if (StringUtils.isEmpty(image1) && StringUtils.isNotEmpty(imgDriveletter)) {
					char driveNumber = (char)((int)imgDriveletter.charAt(0) - 17);
					String matchingImageMount = findImageMatchByDrive(driveNumber, imgDriveletter.charAt(0));
					if (matchingImageMount != null)
						image1 = matchingImageMount;
				}
				if ("\\file".equals(image1)) {
					img1 = "file"; // for template if . was unavailable
				}
			} else if (line.equals("exit") || line.startsWith("exit ")) {
				exit = true;
			} else if (line.equals("echo off")) {
				// just ignore
			} else if (line.equals("echo") || line.startsWith("echo ") || line.startsWith("echo.")) {
				customEchos += orgLine + PlatformUtils.EOLN;
			} else if (line.startsWith(":") || line.equals("cd") || line.equals("cls") || line.startsWith("cls ") || line.startsWith("cls\\") || line.equals("rem") || line.startsWith("rem ")
					|| line.startsWith("goto ") || line.startsWith("if errorlevel ")) {
				// just ignore
			} else {
				leftOvers.add(orgLine);
			}
		}

		if (StringUtils.isNotEmpty(customEchos) && !customEchos.equalsIgnoreCase("echo." + PlatformUtils.EOLN)) {
			customSections[1] += "@echo off" + PlatformUtils.EOLN + customEchos; // add echo commands to pre-launch custom section
			customSections[1] += "pause" + PlatformUtils.EOLN; // add pause statement to make it all readable
		}

		if (executable.equals(StringUtils.EMPTY) && !leftOvers.isEmpty()) {
			for (int i = 0; i < leftOvers.size(); i++) {
				executable = leftOvers.get(i).trim();
				if (i == (leftOvers.size() - 1)) { // the last executable should be the main game
					boolean isCalledBatch = executable.toLowerCase().startsWith("call ");
					if (isCalledBatch)
						executable = executable.substring(5);
					int spaceAfterIndex = executable.indexOf(' ');
					if (spaceAfterIndex != -1) {
						params = executable.substring(spaceAfterIndex + 1);
						executable = executable.substring(0, spaceAfterIndex);
					}
					executable += isCalledBatch ? FileUtils.EXECUTABLES[2]: FileUtils.EXECUTABLES[0];
				} else {
					customSections[1] += executable + PlatformUtils.EOLN; // add other statements to pre-launch custom section
				}
			}
		}

		for (Mount mount: mountingpoints) {
			char mount_drive = mount.getDriveletter();
			String mountPath = mount.getHostPathAsString();
			if (driveMatches(executable, mount_drive, driveletter))
				main = splitInThree(executable, mountPath, remainder);
			else {
				if (mount.matchesImageMountPath(image1))
					img1 = image1;
				else if (driveMatches(image1, mount_drive, driveletter))
					img1 = splitInThree(image1, mountPath, remainder);
				if (mount.matchesImageMountPath(image2))
					img2 = image2;
				else if (driveMatches(image2, mount_drive, driveletter))
					img2 = splitInThree(image2, mountPath, remainder);
				if (mount.matchesImageMountPath(image3))
					img3 = image1;
				else if (driveMatches(image3, mount_drive, driveletter))
					img3 = splitInThree(image3, mountPath, remainder);
			}
		}

		if (exit == null)
			exit = false;
	}

	private String findImageMatchByDrive(char driveNumber, char driveLetter) {
		for (Mount m: mountingpoints) {
			if (m.getMountingType() == MountingType.IMAGE && (driveNumber == m.getDriveletter()))
				return m.getPathAsString();
		}
		for (Mount m: mountingpoints) {
			if (m.getMountingType() == MountingType.IMAGE && (driveLetter == m.getDriveletter()))
				return m.getPathAsString();
		}
		return null;
	}

	private boolean driveMatches(String main, char mount_drive, char driveletter) {
		if (StringUtils.isEmpty(main))
			return false;
		if (main.length() > 2 && main.charAt(1) == ':')
			return Character.toUpperCase(main.charAt(0)) == mount_drive;
		return driveletter == mount_drive;
	}

	private String splitInThree(String main, String mountPath, String remainder) {
		String[] result = {null, null};
		if (main.length() > 2 && main.charAt(1) == ':')
			main = main.substring(2);
		File f1 = new File(main);
		String parent = f1.getParent();
		File mountPathFile = new File(mountPath);
		if (main.charAt(0) == '\\') { // absolute path, don't use cwd (remainder)
			if (parent == null) {
				result[0] = mountPath;
			} else {
				result[0] = new File(mountPathFile, parent).getPath();
			}
		} else { // relative path, do use cwd (remainder)
			if (parent == null) {
				result[0] = new File(mountPathFile, remainder).getPath();
			} else {
				result[0] = new File(new File(mountPathFile, remainder), parent).getPath();
			}
		}
		result[1] = f1.getName();

		if (remainder.indexOf('~') > 0) {
			String s = FileUtils.makeRelativeToDosroot(new File(result[0], result[1])).getPath();
			if (s.startsWith("./") || s.startsWith(".\\"))
				s = s.substring(2);
			return s;
		} else {
			return FileUtils.sanitizeToDosroot(new File(result[0], result[1]).getPath());
		}
	}

	public String toString() {
		return toString((Autoexec)null, false);
	}

	public String toString(final Autoexec base, final Boolean prepareOnly) {
		StringBuffer result = new StringBuffer();

		insertCustomSection(result, 0);
		if (!StringUtils.EMPTY.equals(keyb) && (base == null || !base.keyb.equals(keyb))) {
			result.append("keyb.com ").append(keyb).append(PlatformUtils.EOLN);
		}
		if (!StringUtils.EMPTY.equals(ipxnet) && (base == null || !base.ipxnet.equals(ipxnet))) {
			result.append("ipxnet.com ").append(ipxnet).append(PlatformUtils.EOLN);
		}

		List<Mount> mnts = null;
		if (base == null) {
			mnts = mountingpoints;
		} else {
			mnts = new ArrayList<Mount>();
			for (int i = 0; i < mountingpoints.size(); i++) {
				if (i >= base.mountingpoints.size() || !mountingpoints.get(i).equals(base.mountingpoints.get(i))) {
					mnts.add(mountingpoints.get(i));
				}
			}
		}

		for (Mount mount: mnts) {
			result.append(mount.toString()).append(PlatformUtils.EOLN);
		}
		if (!StringUtils.EMPTY.equals(mixer) && (base == null || !base.mixer.equals(mixer))) {
			result.append("mixer.com ").append(mixer).append(PlatformUtils.EOLN);
		}

		if (!StringUtils.EMPTY.equals(main)) {

			String[] dosboxLocation = convertToDosboxPath(main);
			result.append(dosboxLocation[0] + PlatformUtils.EOLN); // move to drive
			result.append("cd \\" + dosboxLocation[1] + PlatformUtils.EOLN); // move to dir
			if (loadfix) {
				result.append("loadfix -").append(loadfixValue > 0 ? loadfixValue: 64).append(PlatformUtils.EOLN);
			}
			insertCustomSection(result, 1);
			if (!prepareOnly) {
				if (loadhigh) {
					result.append("loadhigh ");
				}
				if (dosboxLocation[2].toLowerCase().endsWith(FileUtils.EXECUTABLES[2])) {
					result.append("call ");
				}
				result.append(dosboxLocation[2]);
				if (!StringUtils.EMPTY.equals(params)) {
					result.append(' ').append(params);
				}
				result.append(PlatformUtils.EOLN);
				insertCustomSection(result, 2);
			}

		} else if (!StringUtils.EMPTY.equals(img1)) {

			if (loadfix) {
				result.append("loadfix -").append(loadfixValue > 0 ? loadfixValue: 64).append(PlatformUtils.EOLN);
			}
			insertCustomSection(result, 1);
			if (!prepareOnly) {
				if (matchesMountedImage(img1)) {
					result.append("boot \"").append(img1).append("\"");
				} else {
					String[] dosboxLocation = convertToDosboxPath(img1);
					result.append("boot ").append(dosboxLocation[0]).append('\\').append(dosboxLocation[1]);
					if (!dosboxLocation[1].equals(StringUtils.EMPTY)) {
						result.append('\\');
					}
					result.append(dosboxLocation[2]);
				}
				if (matchesMountedImage(img2)) {
					result.append(" \"").append(img2).append("\"");
				} else {
					if (!StringUtils.EMPTY.equals(img2)) {
						String[] dosboxLocation = convertToDosboxPath(img2);
						result.append(' ').append(dosboxLocation[0]).append('\\').append(dosboxLocation[1]);
						if (!dosboxLocation[1].equals(StringUtils.EMPTY)) {
							result.append('\\');
						}
						result.append(dosboxLocation[2]);
					}
				}
				if (matchesMountedImage(img3)) {
					result.append(" \"").append(img3).append("\"");
				} else {
					if (!StringUtils.EMPTY.equals(img3)) {
						String[] dosboxLocation = convertToDosboxPath(img3);
						result.append(' ').append(dosboxLocation[0]).append('\\').append(dosboxLocation[1]);
						if (!dosboxLocation[1].equals(StringUtils.EMPTY)) {
							result.append('\\');
						}
						result.append(dosboxLocation[2]);
					}
				}
				if (!StringUtils.EMPTY.equals(imgDriveletter)) {
					result.append(" -l ").append(imgDriveletter);
				}
				result.append(PlatformUtils.EOLN);
				insertCustomSection(result, 2);
			}

		} else {

			// template
			insertCustomSection(result, 1);
			insertCustomSection(result, 2);

		}

		if (!prepareOnly) {
			if (loadfix) {
				result.append("loadfix -f").append(PlatformUtils.EOLN);
			}

			insertCustomSection(result, 3);

			if (pause) {
				result.append("pause").append(PlatformUtils.EOLN);
			}

			if (exit) {
				result.append("exit").append(PlatformUtils.EOLN);
			}
		}

		if (result.length() > 0) {
			result.insert(0, "[autoexec]" + PlatformUtils.EOLN);
		}

		return result.toString();
	}

	private void insertCustomSection(final StringBuffer sb, final int sectionNr) {
		if (StringUtils.isNotEmpty(customSections[sectionNr])) {
			sb.append(CUSTOM_SECTION_MARKERS[sectionNr * 2 + 0]).append(PlatformUtils.EOLN);
			sb.append(StringUtils.chomp(customSections[sectionNr])).append(PlatformUtils.EOLN);
			sb.append(CUSTOM_SECTION_MARKERS[sectionNr * 2 + 1]).append(PlatformUtils.EOLN);
		}
	}

	String[] convertToDosboxPath(final String hostFileLocation) {
		File hostFile = new File(hostFileLocation);
		String[] result = {StringUtils.EMPTY, StringUtils.EMPTY, hostFile.getName()};
		int maxLengthMount = 0;
		for (Mount mount: mountingpoints) {
			File dosboxDir = mount.canBeUsedFor(hostFile);
			if (dosboxDir != null && (mount.getPathAsString().length() > maxLengthMount)) {
				result[0] = mount.getDriveletter() + ":";
				result[1] = (dosboxDir.getParent() == null) ? StringUtils.EMPTY: dosboxDir.getParent();
				maxLengthMount = mount.getPathAsString().length();
			}
		}
		// translate *nix paths to dosbox paths
		result[1] = PlatformUtils.toDosboxPath(result[1]);
		return result;
	}

	public boolean matchesMountedImage(final String hostFileLocation) {
		for (Mount mount: mountingpoints)
			if (mount.matchesImageMountPath(hostFileLocation))
				return true;
		return false;
	}

	void addMount(final String mount) {
		try {
			Mount mnt = new Mount(mount);
			if (mnt.isUnmounted()) {
				for (Mount m: mountingpoints) {
					if (m.getDriveletter() == mnt.getDriveletter()) {
						m.toggleMount();
						break;
					}
				}
			} else {
				mountingpoints.add(mnt);
			}
		} catch (InvalidMountstringException e) {
			// nothing we can do
		}
	}

	public void setMainExecutable(final String main, final String params) {
		this.main = main;
		this.params = params;
	}

	public File getCanonicalMainDir() {
		if (isBooter()) {
			return FileUtils.getCanMainFile(new File(img1)).getParentFile();
		} else {
			return FileUtils.getCanMainFile(new File(main)).getParentFile();
		}
	}

	public String getActualMain() {
		return isBooter() ? img1: main;
	}

	public void setActualMainPath(final File mainParentFile) {
		if (isBooter()) {
			this.img1 = new File(mainParentFile, new File(img1).getName()).getPath();
			if (StringUtils.isNotEmpty(this.img2))
				this.img2 = new File(mainParentFile, new File(img2).getName()).getPath();
			if (StringUtils.isNotEmpty(this.img3))
				this.img3 = new File(mainParentFile, new File(img3).getName()).getPath();
		} else {
			this.main = new File(mainParentFile, new File(main).getName()).getPath();
		}
	}

	public void setActualMain(final File mainFile) {
		if (isBooter()) {
			this.img1 = mainFile.getPath();
		} else {
			this.main = mainFile.getPath();
		}
	}

	public void updateMainParameters(final String parameters) {
		this.params = parameters;
	}

	public void migrateToDosroot(final File fromPath, final boolean prefixDosroot) {
		for (Mount mount: mountingpoints) {
			mount.migrateToDosroot(fromPath, prefixDosroot);
		}
		if (isBooter()) {
			String newImg1 = FileUtils.makeRelativeTo(prefixDosroot ? FileUtils.canonicalToDosroot(img1): new File(img1), fromPath).getPath();
			if ((convertToDosboxPath(newImg1)[0].length() > 0) || matchesMountedImage(newImg1))
				img1 = newImg1;
			String newImg2 = FileUtils.makeRelativeTo(prefixDosroot ? FileUtils.canonicalToDosroot(img2): new File(img2), fromPath).getPath();
			if ((convertToDosboxPath(newImg2)[0].length() > 0) || matchesMountedImage(newImg2))
				img2 = newImg2;
			String newImg3 = FileUtils.makeRelativeTo(prefixDosroot ? FileUtils.canonicalToDosroot(img3): new File(img3), fromPath).getPath();
			if ((convertToDosboxPath(newImg3)[0].length() > 0) || matchesMountedImage(newImg3))
				img3 = newImg3;
		} else {
			String newMain = FileUtils.makeRelativeTo(prefixDosroot ? FileUtils.canonicalToDosroot(main): new File(main), fromPath).getPath();
			if (convertToDosboxPath(newMain)[0].length() > 0)
				main = newMain;
		}
	}

	public void migrateMountsTo(final File fromPath, final File toPath) {
		for (Mount mount: mountingpoints)
			mount.migrateTo(fromPath, toPath);
	}

	public void migrateTo(final File fromPath, final File toPath) {
		migrateMountsTo(fromPath, toPath);
		String newMain = FileUtils.makeRelativeTo(FileUtils.canonicalToDosroot(main), fromPath).getPath();
		if (!new File(newMain).isAbsolute()) {
			newMain = FileUtils.makeRelativeToDosroot(new File(toPath, newMain)).getPath();
			if (convertToDosboxPath(newMain)[0].length() > 0)
				main = newMain;
		}
	}

	public void updateForTargetImportBaseDir(File baseDir) {
		for (Mount mount: mountingpoints) {
			mount.updateForTargetImportBaseDir(baseDir);
		}
		if (isBooter()) {
			img1 = FileUtils.prefixAndSanitizeToDosroot(baseDir, new File(img1)).getPath();
			if (StringUtils.isNotBlank(img2))
				img2 = FileUtils.prefixAndSanitizeToDosroot(baseDir, new File(img2)).getPath();
			if (StringUtils.isNotBlank(img3))
				img3 = FileUtils.prefixAndSanitizeToDosroot(baseDir, new File(img3)).getPath();
		} else {
			main = FileUtils.prefixAndSanitizeToDosroot(baseDir, new File(main)).getPath();
		}
	}

	void updateMountingPoints(final Autoexec base, final List<Mount> additionalMounts) {
		this.mountingpoints = new ArrayList<Mount>();
		for (Mount m: base.mountingpoints)
			this.mountingpoints.add(new Mount(m));
		for (Mount m: additionalMounts)
			this.mountingpoints.add(new Mount(m));
	}

	public boolean isIncomplete() {
		if (img1.length() == 0 && main.length() == 0)
			return true;
		if (isBooter()) {
			if ((convertToDosboxPath(img1)[0].length() == 0) && !matchesMountedImage(img1))
				return true;
		} else {
			if (convertToDosboxPath(main)[0].length() == 0)
				return true;
		}
		return false;
	}

	public Boolean isExit() {
		return exit;
	}

	public boolean isBooter() {
		return img1.length() > 0;
	}

	public Boolean isLoadfix() {
		return loadfix;
	}

	public Boolean isLoadhigh() {
		return loadhigh;
	}

	public int getLoadfixValue() {
		return loadfixValue;
	}

	public String getMixer() {
		return mixer;
	}

	public String getKeyb() {
		return keyb;
	}

	public String getIpxnet() {
		return ipxnet;
	}

	public String getImg1() {
		if ("file".equals(img1)) {
			return StringUtils.EMPTY;
		}
		return img1;
	}

	public String getImg2() {
		return img2;
	}

	public String getImg3() {
		return img3;
	}

	public String getImgDriveletter() {
		return imgDriveletter;
	}

	public String getMain() {
		return main;
	}

	public String getMainParameters() {
		return params;
	}

	public String[] getMountingpoints() {
		String[] result = new String[mountingpoints.size()];
		int mountIndex = 0;
		for (Mount mount: mountingpoints) {
			result[mountIndex++] = mount.toString(true);
		}
		return result;
	}

	public String getCustomSection(final int i) {
		return customSections[i];
	}

	static Autoexec createCombination(Autoexec a1, Autoexec a2) {
		Autoexec result = new Autoexec();
		result.mountingpoints = new ArrayList<Mount>();
		for (Mount m1: a1.mountingpoints) {
			for (Mount m2: a2.mountingpoints) {
				if (m1.toString(false).equals(m2.toString(false))) {
					result.mountingpoints.add(new Mount(m1));
				}
			}
		}
		result.loadfix = a1.loadfix != Conf.CONFLICTING_BOOL_SETTING && a2.loadfix != Conf.CONFLICTING_BOOL_SETTING && a1.loadfix.equals(a2.loadfix) ? a1.loadfix: Conf.CONFLICTING_BOOL_SETTING;
		result.loadfixValue = (a1.loadfixValue == a2.loadfixValue) ? a1.loadfixValue: Conf.CONFLICTING_INT_SETTING;
		result.main = a1.main.equals(a2.main) ? a1.main: Conf.CONFLICTING_STRING_SETTING;
		result.img1 = a1.img1.equals(a2.img1) ? a1.img1: Conf.CONFLICTING_STRING_SETTING;
		result.img2 = a1.img2.equals(a2.img2) ? a1.img2: Conf.CONFLICTING_STRING_SETTING;
		result.img3 = a1.img3.equals(a2.img3) ? a1.img3: Conf.CONFLICTING_STRING_SETTING;
		result.imgDriveletter = a1.imgDriveletter.equals(a2.imgDriveletter) ? a1.imgDriveletter: Conf.CONFLICTING_STRING_SETTING;
		result.exit = a1.exit != Conf.CONFLICTING_BOOL_SETTING && a2.exit != Conf.CONFLICTING_BOOL_SETTING && a1.exit.equals(a2.exit) ? a1.exit: Conf.CONFLICTING_BOOL_SETTING;
		result.params = a1.params.equals(a2.params) ? a1.params: Conf.CONFLICTING_STRING_SETTING;
		result.mixer = a1.mixer.equals(a2.mixer) ? a1.mixer: Conf.CONFLICTING_STRING_SETTING;
		result.keyb = a1.keyb.equals(a2.keyb) ? a1.keyb: Conf.CONFLICTING_STRING_SETTING;
		result.ipxnet = a1.ipxnet.equals(a2.ipxnet) ? a1.ipxnet: Conf.CONFLICTING_STRING_SETTING;
		result.pause = a1.pause != Conf.CONFLICTING_BOOL_SETTING && a2.pause != Conf.CONFLICTING_BOOL_SETTING && a1.pause.equals(a2.pause) ? a1.pause: Conf.CONFLICTING_BOOL_SETTING;
		for (int i = 0; i < a1.customSections.length; i++)
			result.customSections[i] = a1.customSections[i].equals(a2.customSections[i]) ? a1.customSections[i]: Conf.CONFLICTING_STRING_SETTING;
		return result;
	}

	/**
	 * @return mounts as they would be effective after execution in DOSBox, keeping the original order
	 */
	public Map<Character, Mount> nettoMounts() {
		return nettoMounts(new LinkedHashMap<Character, Mount>());
	}

	/**
	 * @param existingMap containing netto DOSBox mounts
	 * @return mounts as they would be effective after execution in DOSBox, starting with the DOSBox mounts and continuing with this Autoexec's mounts, keeping the original order
	 */
	Map<Character, Mount> nettoMounts(Map<Character, Mount> existingMap) {
		Map<Character, Mount> map = new LinkedHashMap<Character, Mount>();
		for (Map.Entry<Character, Mount> entry: existingMap.entrySet()) {
			map.put(entry.getKey(), entry.getValue());
		}
		for (Mount m: mountingpoints) {
			if (map.containsKey(m.getDriveletter())) {
				if (m.isUnmounted()) {
					map.remove(m.getDriveletter());
				}
			} else {
				if (!m.isUnmounted()) {
					map.put(m.getDriveletter(), m);
				}
			}
		}
		return map;
	}

	void removeFloppyMounts() {
		for (Iterator<Mount> it = mountingpoints.iterator(); it.hasNext();)
			if (it.next().getMountAs().equals("floppy"))
				it.remove();
	}

	/**
	 * @param dosboxAutoexec Set the mountingpoints for this Autoexec so that it effectively unmounts all existing netto DOSBox mounts, with the minimum required mounts.
	 */
	void setUnmountsFor(Autoexec dosboxAutoexec) {
		Map<Character, Mount> dbMap = dosboxAutoexec.nettoMounts();

		List<Mount> result = new ArrayList<Mount>();

		for (Map.Entry<Character, Mount> entry: dbMap.entrySet()) {
			Mount u = new Mount(entry.getValue());
			u.toggleMount();
			result.add(u);
		}

		mountingpoints = result;
	}

	/**
	 * @param dosboxAutoexec Set the mountingpoints for this Autoexec so that it reaches the same nettoMounts with the minimum required mounts. In other words, unnecessary mounts are removed. The
	 *            order of mounts is likely to be changed, unless dosboxAutoexec has no mounts, then the original order will be kept.
	 */
	void removeUnnecessaryMounts(Autoexec dosboxAutoexec) {
		Map<Character, Mount> dbMap = dosboxAutoexec.nettoMounts();
		Map<Character, Mount> fullMap = this.nettoMounts(dbMap);

		List<Mount> result = new ArrayList<Mount>();

		// determine mounts from dosbox that have been removed - unmounts, or have remained the same, or have changed
		for (Map.Entry<Character, Mount> entry: dbMap.entrySet()) {
			Mount mnt = entry.getValue();
			if (!fullMap.containsKey(entry.getKey())) {
				Mount umnt = new Mount(mnt);
				umnt.toggleMount();
				result.add(umnt);
			} else {
				if (mnt.toString().equals(fullMap.get(entry.getKey()).toString())) {
					result.add(mnt);
				} else {
					Mount umnt = new Mount(mnt);
					umnt.toggleMount();
					result.add(umnt);
					Mount m = new Mount(fullMap.get(entry.getKey()));
					result.add(m);
				}
			}
		}

		// determine added mounts
		for (Map.Entry<Character, Mount> entry: fullMap.entrySet()) {
			Mount mnt = entry.getValue();
			if (!dbMap.containsKey(entry.getKey())) {
				result.add(mnt);
			}
		}

		mountingpoints = result;
	}

	public String[] getSetPaths() {
		for (String section: customSections) {
			String[] commands = section.split(PlatformUtils.EOLN);
			for (String command: commands) {
				if (command.toLowerCase().startsWith("set path=")) {
					return command.substring(9).trim().split(";");
				} else if (command.toLowerCase().startsWith("path=")) {
					return command.substring(5).trim().split(";");
				}
			}
		}
		return null;
	}
}
