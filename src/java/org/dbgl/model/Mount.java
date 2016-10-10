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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.dbgl.exception.DrivelettersExhaustedException;
import org.dbgl.exception.InvalidHostfileException;
import org.dbgl.exception.InvalidMountstringException;
import org.dbgl.util.FileUtils;
import org.dbgl.util.PlatformUtils;
import org.dbgl.util.StringRelatedUtils;


public class Mount {

	private final static Pattern MOUNT_PATRN = Pattern.compile(
		"^(?:mount)(?:(?:\\s+-u\\s+([a-y]))|(?:(?:\\s+([a-y]))(?:\\s+((?:\\S+)|(?:\"[^\"]+\")))(?:(?:(?:\\s+-t\\s+(dir|floppy|cdrom|iso))|(?:\\s+-label (\\S+))|(?:\\s+-(ioctl(?:_dx|_dio|_mci)?|noioctl|aspi))|(?:\\s+-freesize\\s+(\\d+))|(?:\\s+-usecd\\s+(\\d+))|(?:\\s+-size\\s+(\\d+,\\d+,\\d+,\\d+))){0,6})))\\s*$",
		Pattern.CASE_INSENSITIVE);
	private final static Pattern IMGMOUNT_PATRN = Pattern.compile(
		"^(?:imgmount)(?:\\s+([a-y0-3]))((?:\\s+(?:(?:[^-][^\\s]+)|(?:\"[^\"]+\")))+)(?:(?:(?:\\s+-t\\s+(hdd|floppy|cdrom|iso))|(?:\\s+-fs\\s+(fat|iso|none))|(?:\\s+-size\\s+(\\d+,\\d+,\\d+,\\d+))){0,3})\\s*$",
		Pattern.CASE_INSENSITIVE);

	public enum MountingType {
		DIR, IMAGE, PHYSFS
	};

	private MountingType mountingType;
	private String mountAs;
	private char driveletter;
	private String label;
	private File[] path;
	private File write;
	private String lowlevelCD;
	private String useCD;
	private String freesize;
	private String fs;
	private String size;
	private boolean unmounted;

	public Mount(final MountingType mountingType, final String mountAs, final String driveletter, final String[] paths, final String label, final String low, final String usecd, final String write,
			final String freesize, final String fs, final String size) {
		this.mountingType = mountingType;
		this.mountAs = mountAs.toLowerCase();
		this.driveletter = Character.toUpperCase(driveletter.charAt(0));
		this.path = new File[paths.length];
		for (int i = 0; i < paths.length; i++) {
			this.path[i] = new File(paths[i]);
		}
		this.write = (write == null) ? null: new File(write);
		this.label = label;
		this.lowlevelCD = low;
		this.useCD = usecd;
		this.freesize = freesize;
		this.fs = fs;
		this.size = size;
		this.unmounted = false;
	}

	public Mount(final Mount otherMount) {
		this.mountingType = otherMount.mountingType;
		this.mountAs = otherMount.mountAs;
		this.driveletter = otherMount.driveletter;
		this.label = otherMount.label;
		this.path = otherMount.path.clone();
		this.write = otherMount.write;
		this.lowlevelCD = otherMount.lowlevelCD;
		this.useCD = otherMount.useCD;
		this.freesize = otherMount.freesize;
		this.fs = otherMount.fs;
		this.size = otherMount.size;
		this.unmounted = otherMount.unmounted;
	}

	private void init() {
		mountingType = MountingType.DIR;
		mountAs = "";
		driveletter = '\0';
		label = "";
		path = new File[0];
		write = null;
		lowlevelCD = "";
		useCD = "";
		freesize = "";
		fs = "";
		size = "";
		unmounted = false;
	}

	private void initForPhysFS(final String physFSPath) {
		mountingType = MountingType.PHYSFS;
		int colonIndex1 = physFSPath.indexOf(':');
		if (colonIndex1 == 1) {
			colonIndex1 = physFSPath.indexOf(":", colonIndex1 + 1);
		}
		int colonIndex2 = physFSPath.lastIndexOf(":");
		path = new File[1];
		if (colonIndex1 == colonIndex2) {
			path[0] = new File(physFSPath.substring(0, colonIndex2));
		} else {
			write = new File(physFSPath.substring(0, colonIndex1));
			path[0] = new File(physFSPath.substring(colonIndex1 + 1, colonIndex2));
		}
	}

	private void initForIso(final String IsoPath) {
		mountingType = MountingType.IMAGE;
		mountAs = "iso";
		path = new File[1];
		path[0] = new File(IsoPath.substring(0, FileUtils.containsIso(IsoPath)));
	}

	private void initForFat(final String fatImagePath) {
		mountingType = MountingType.IMAGE;
		mountAs = "floppy";
		path = new File[1];
		path[0] = new File(fatImagePath.substring(0, FileUtils.containsFatImage(fatImagePath)));
	}

	public Mount(final boolean floppy, final String hostFileLocation, final Set<Character> usedDriveLetters) throws InvalidHostfileException, DrivelettersExhaustedException {
		init();
		if ("".equals(hostFileLocation)) {
			throw new InvalidHostfileException();
		}

		driveletter = getFreeDriveletter(floppy, usedDriveLetters);

		if (FileUtils.containsPhysFS(hostFileLocation) != -1) {
			initForPhysFS(hostFileLocation);
		} else if (FileUtils.containsIso(hostFileLocation) != -1) {
			initForIso(hostFileLocation);
		} else if (FileUtils.containsFatImage(hostFileLocation) != -1) {
			driveletter = getFreeDriveletter(true, usedDriveLetters);
			initForFat(hostFileLocation);
		} else {
			File hostFile = new File(hostFileLocation);
			path = new File[1];
			path[0] = hostFile.getParentFile();
			if (path[0] == null) {
				path[0] = new File(".");
			}
			if (FileUtils.isStoredOnFloppyDrive(hostFile))
				mountAs = "floppy";
			else if (FileUtils.isStoredOnCDRomDrive(hostFile))
				mountAs = "cdrom";
		}
	}

	public Mount(String hostFileLocation, final Set<Character> usedDriveLetters, File[] overrideFilesToMount) throws InvalidHostfileException, DrivelettersExhaustedException {
		this(false, hostFileLocation, usedDriveLetters);
		if (overrideFilesToMount != null && overrideFilesToMount.length > 0) {
			path = overrideFilesToMount;
		}
	}

	public Mount(final String mount) throws InvalidMountstringException {
		init();
		Matcher mountMatcher = MOUNT_PATRN.matcher(mount);
		Matcher imgmountMatcher = IMGMOUNT_PATRN.matcher(mount);

		if (mountMatcher.matches()) {
			if (mountMatcher.group(1) != null) {
				unmounted = true;
				driveletter = Character.toUpperCase(mountMatcher.group(1).charAt(0));
				return;
			}
			driveletter = Character.toUpperCase(mountMatcher.group(2).charAt(0));
			String mountLocation = StringUtils.strip(PlatformUtils.toNativePath(mountMatcher.group(3)), "\"");
			if (FileUtils.isPhysFS(mountLocation))
				initForPhysFS(mountLocation);
			else
				path = new File[] {FileUtils.makeRelativeToDosroot(FileUtils.canonicalToDosroot(mountLocation))};
			mountAs = StringUtils.defaultString(mountMatcher.group(4));
			label = StringUtils.defaultString(mountMatcher.group(5));
			lowlevelCD = StringUtils.defaultString(mountMatcher.group(6));
			freesize = StringUtils.defaultString(mountMatcher.group(7));
			useCD = StringUtils.defaultString(mountMatcher.group(8));
		} else if (imgmountMatcher.matches()) {
			mountingType = MountingType.IMAGE;
			driveletter = Character.toUpperCase(imgmountMatcher.group(1).charAt(0));
			String[] paths = StringUtils.stripAll(PlatformUtils.toNativePath(imgmountMatcher.group(2)).trim().split("\\s(?=([^\"]*\"[^\"]*\")*[^\"]*$)"), "\"");
			path = new File[paths.length];
			for (int i = 0; i < paths.length; i++)
				path[i] = FileUtils.makeRelativeToDosroot(FileUtils.canonicalToDosroot(paths[i]));
			mountAs = StringUtils.defaultString(imgmountMatcher.group(3));
			if (mountAs.equalsIgnoreCase("cdrom"))
				mountAs = "iso";
			fs = StringUtils.defaultString(imgmountMatcher.group(4));
			size = StringUtils.defaultString(imgmountMatcher.group(5));
		} else {
			throw new InvalidMountstringException();
		}
	}

	public static char getFreeDriveletter(final boolean floppy, final Set<Character> usedDriveLetters) throws DrivelettersExhaustedException {
		List<Character> freeDriveletters = new ArrayList<Character>();
		char start = floppy ? 'A': 'C';
		for (char i = start; i < 'Z'; i++) {
			freeDriveletters.add(i);
		}
		if (!floppy) {
			freeDriveletters.add('A');
			freeDriveletters.add('B');
		}
		for (Character c: usedDriveLetters) {
			freeDriveletters.remove(c);
		}
		if (freeDriveletters.isEmpty()) {
			throw new DrivelettersExhaustedException();
		}
		return freeDriveletters.get(0);
	}

	public String toString(final boolean forList) {
		StringBuffer result = new StringBuffer();
		switch (mountingType) {
			case DIR:
				result.append("mount ").append(driveletter).append(" \"").append(getPathAsString()).append('"');
				if (lowlevelCD.length() > 0) {
					result.append(" -").append(lowlevelCD);
				}
				if (useCD.length() > 0) {
					result.append(" -usecd ").append(useCD);
				}
				break;
			case PHYSFS:
				result.append("mount ").append(driveletter).append(" \"");
				if (write != null) {
					result.append(write.getPath()).append(':');
				}
				result.append(path[0].getPath()).append(":\\\"");
				break;
			case IMAGE:
				result.append("imgmount ").append(driveletter);
				for (int i = 0; i < path.length; i++) {
					result.append(" \"").append(path[i].getPath()).append('"');
				}
				break;
			default:
		}
		if (!"".equals(label)) {
			result.append(" -label ").append(label);
		}
		if (!"".equals(mountAs)) {
			result.append(" -t ").append(mountAs);
		}
		if (!"".equals(freesize)) {
			result.append(" -freesize ").append(freesize);
		}
		if (!"".equals(fs)) {
			result.append(" -fs ").append(fs);
		}
		if (!"".equals(size)) {
			result.append(" -size ").append(size);
		}
		if (unmounted) {
			if (forList) {
				result.append(" (UNMOUNTED)");
			} else {
				result = new StringBuffer("mount -u ").append(driveletter);
			}
		}
		return result.toString();
	}

	public String toString() {
		return toString(false);
	}

	public void toggleMount() {
		unmounted = !unmounted;
	}

	public char getDriveletter() {
		return driveletter;
	}

	public String getDriveletterString() {
		return String.valueOf(driveletter);
	}

	public String getLowlevelCD() {
		return lowlevelCD;
	}

	public String getUseCD() {
		return useCD;
	}

	public boolean isUnmounted() {
		return unmounted;
	}

	public String getLabel() {
		return label;
	}

	public String getPathAsString() {
		if (path.length <= 0) {
			return "";
		}
		return path[0].getPath();
	}

	public File getWrite() {
		return write;
	}

	public char getDriveletterFromPath() {
		return Character.toUpperCase(path[0].getAbsolutePath().charAt(0));
	}

	public String getImgMountAsString(final String delimiter) {
		String[] paths = new String[path.length];
		for (int i = 0; i < path.length; i++) {
			paths[i] = path[i].getPath();
		}
		return StringRelatedUtils.stringArrayToString(paths, delimiter);
	}

	public File[] getPath() {
		return path;
	}

	public String getHostPathAsString() {
		return (mountingType == MountingType.PHYSFS) ? getPathAsString() + ':': getPathAsString();
	}

	public MountingType getMountingType() {
		return mountingType;
	}

	public String getMountAs() {
		return mountAs;
	}

	public String getFreesize() {
		return freesize;
	}

	public String getFs() {
		return fs;
	}

	public String getSize() {
		return size;
	}

	public File canBeUsedFor(final File hostFile) {
		if (!unmounted) {
			File canHostFile = FileUtils.getCanMainFile(hostFile);
			File mountFile = FileUtils.canonicalToDosroot(getPathAsString());
			if (mountingType != MountingType.DIR && (FileUtils.isIsoFile(canHostFile.getPath()) || FileUtils.isArchive(canHostFile.getPath()) || FileUtils.isFatImage(canHostFile.getPath()))) {
				return (mountFile.equals(canHostFile)) ? FileUtils.makeRelativeTo(FileUtils.canonicalToDosroot(hostFile.getPath()), FileUtils.canonicalToDosroot(getHostPathAsString())): null;
			}
			if (FileUtils.areRelated(mountFile, canHostFile)) {
				if ((hostFile.getPath().indexOf('~') > 0)) {
					if (hostFile.isAbsolute())
						canHostFile = hostFile;
					else
						canHostFile = new File(FileUtils.getDosRoot(), hostFile.getPath());
				}
				return FileUtils.makeRelativeTo(canHostFile, mountFile);
			}
		}
		return null;
	}

	public boolean matchesImageMountPath(final String hostFile) {
		return (mountingType == MountingType.IMAGE && getPathAsString().equals(hostFile));
	}

	public void migrateToDosroot(final File fromPath, final boolean prefixDosroot) {
		for (int i = 0; i < path.length; i++) {
			path[i] = FileUtils.makeRelativeToDosroot(FileUtils.makeRelativeTo(prefixDosroot ? FileUtils.canonicalToDosroot(path[i].getPath()): path[i], fromPath));
		}
		if (write != null) {
			write = FileUtils.makeRelativeToDosroot(FileUtils.makeRelativeTo(prefixDosroot ? FileUtils.canonicalToDosroot(write.getPath()): write, fromPath));
		}
	}

	public void migrateTo(final File fromPath, final File toPath) {
		File from = FileUtils.makeRelativeToDosroot(fromPath);
		File to = FileUtils.makeRelativeToDosroot(toPath);
		for (int i = 0; i < path.length; i++) {
			if (path[i].getPath().equals(from.getPath()))
				path[i] = to;
		}
		if (write != null) {
			if (write.getPath().equals(from.getPath()))
				write = to;
		}
	}

	public void updateForTargetImportBaseDir(final File basePath) {
		for (int i = 0; i < path.length; i++) {
			path[i] = FileUtils.prefixAndSanitizeToDosroot(basePath, path[i]);
		}
		if (write != null) {
			write = FileUtils.prefixAndSanitizeToDosroot(basePath, write);
		}
	}

	public int hashCode() {
		return toString(false).hashCode();
	}

	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Mount otherMount = (Mount)obj;
		return (driveletter == otherMount.driveletter && unmounted == otherMount.unmounted);
	}
}
