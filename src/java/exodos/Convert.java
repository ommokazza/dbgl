package exodos;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.dbgl.db.Database;
import org.dbgl.model.DosboxVersion;
import org.dbgl.model.ExpProfile;
import org.dbgl.model.Mount;
import org.dbgl.model.Mount.MountingType;
import org.dbgl.model.Profile;
import org.dbgl.model.SearchResult;
import org.dbgl.model.SearchResult.ResultType;
import org.dbgl.model.conf.Autoexec;
import org.dbgl.model.conf.Conf;
import org.dbgl.model.conf.SectionsWrapper;
import org.dbgl.model.conf.Settings;
import org.dbgl.util.FileUtils;
import org.dbgl.util.PlatformUtils;
import org.dbgl.util.ProgressNotifyable;
import org.dbgl.util.StringRelatedUtils;
import org.dbgl.util.XmlUtils;
import org.w3c.dom.Document;
import exodos.model.GameData;


public class Convert {

	private static final String GAMES_DIR = "games";
	private static final String UTIL_DIR = "util";

	private static final String MEAGRE_DIR = "Meagre";
	private static final String INIFILE_DIR = "IniFile";
	private static final String ABOUT_DIR = "About";

	private static final String EXTRAS_DIR = "Extras";
	private static final String MANUAL_DIR = "Manual";

	private static final String FRONT_DIR = "Front";
	private static final String BACK_DIR = "Back";
	private static final String MEDIA_DIR = "Media";
	private static final String ADVERT_DIR = "Advert";
	private static final String TITLE_DIR = "Title";
	private static final String SCREEN_DIR = "Screen";
	private static final String[] CAP_DIRS = {TITLE_DIR, SCREEN_DIR, FRONT_DIR, BACK_DIR, MEDIA_DIR, ADVERT_DIR};

	private static final String GPA_TITLE = "eXoDOS conversion";
	private static final String GPA_NOTES = "";
	private static final String GPA_AUTHOR = "";

	private static final long BYTES_IN_MB = 1024L * 1024L;
	private static final long MAX_PART_SIZE_DEFAULT_IN_MB = 1024L * 16L;
	private static final String[] CDIMAGES = {".iso", ".cue", ".bin", ".img", ".gog"};

	private static final Settings settings = Settings.getInstance();

	private static boolean verboseOutput = false;
	private static long maxPartSizeInMB = MAX_PART_SIZE_DEFAULT_IN_MB;

	public static final class FileIgnoreCaseComparator implements Comparator<File> {
		public int compare(final File file1, final File file2) {
			return file1.getPath().compareToIgnoreCase(file2.getPath());
		}
	}

	public static void main(String[] args) {
		System.out.println("Converts eXoDOS game packages into DBGL GamePackArchives (v0.7)\n");

		if (args.length < 2 || args.length > 6)
			displaySyntax();

		File inputDir = new File(args[0]);
		File tmpDir = new File(args[1]);

		boolean analyzeOnly = false;
		boolean keepExtractedMetaData = false;

		if (args.length > 2) {
			for (int i = 2; i < args.length; i++) {
				if (args[i].equalsIgnoreCase("-a"))
					analyzeOnly = true;
				else if (args[i].equalsIgnoreCase("-k"))
					keepExtractedMetaData = true;
				else if (args[i].equalsIgnoreCase("-v"))
					verboseOutput = true;
				else if (args[i].toLowerCase().startsWith("-s:")) {
					try {
						maxPartSizeInMB = Long.parseLong(args[i].substring(3));
					} catch (NumberFormatException e) {
						// ignore, use the default value
					}
				} else
					displaySyntax();
			}
		}

		File inputGamesZip = validateParameters(inputDir, tmpDir);
		File gamesDir = new File(inputDir, GAMES_DIR);

		if (analyzeOnly)
			System.out.println("* Analyze only");
		if (keepExtractedMetaData)
			System.out.println("* Keeping extracted data after processing");
		if (verboseOutput)
			System.out.println("* Verbose output");
		if (maxPartSizeInMB != MAX_PART_SIZE_DEFAULT_IN_MB)
			System.out.println("* Target size of the GamePackArchives: " + maxPartSizeInMB + "MB");

		if (analyzeOnly || keepExtractedMetaData || verboseOutput || (maxPartSizeInMB != MAX_PART_SIZE_DEFAULT_IN_MB))
			System.out.println();

		List<DosboxVersion> dbversionsList = null;
		try {
			Database dbase = Database.getInstance();
			dbversionsList = dbase.readDosboxVersionsList();

			if (DosboxVersion.findDefault(dbversionsList) == null) {
				SearchResult result = PlatformUtils.findDosbox();
				if (result.result == ResultType.COMPLETE) {
					dbase.addOrEditDosboxVersion(result.dosbox.getTitle(), result.dosbox.getPath(), result.dosbox.getConf(), result.dosbox.isMultiConfig(), result.dosbox.isUsingCurses(),
						result.dosbox.isDefault(), result.dosbox.getParameters(), result.dosbox.getVersion(), result.dosbox.getId());
					dbversionsList = dbase.readDosboxVersionsList();
				}
				if (DosboxVersion.findDefault(dbversionsList) == null) {
					System.out.println("DOSBox installation could not be located, exiting.");
					System.exit(1);
				}
			}

			if (verboseOutput)
				System.out.println("Using DOSBox installation located in: [" + DosboxVersion.findDefault(dbversionsList).getPath() + "]");
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(1);
		}

		File gamesGamesDir = extractMeagreMetaData(inputGamesZip, tmpDir);

		List<GameData> expProfileList = analyzeMeagreMetaData(gamesDir, gamesGamesDir, dbversionsList);

		if (!analyzeOnly)
			generateGamePackArchives(expProfileList, tmpDir, FilenameUtils.removeExtension(inputGamesZip.getName()), dbversionsList);

		if (!keepExtractedMetaData)
			cleanup(gamesGamesDir);
	}

	private static void displaySyntax() {
		System.out.println("Use: Convert <inputexodosdir> <dstdir> [-a] [-k] [-v] [-s:size]");
		System.out.println("-a\t\tAnalyze only, don't generate GamePackArchives");
		System.out.println("-k\t\tKeep extracted meta data files after processing");
		System.out.println("-v\t\tVerbose output");
		System.out.println("-s:size\t\tTarget size of the GamePackArchives in MB, " + MAX_PART_SIZE_DEFAULT_IN_MB + " is the default (= " + MAX_PART_SIZE_DEFAULT_IN_MB / 1024L + " GB packages)");
		System.exit(1);
	}

	private static File validateParameters(File inputDir, File tmpDir) {
		if (!inputDir.exists()) {
			System.out.println("The directory [" + inputDir + "] does not exist, exiting.");
			System.exit(1);
		}
		File gamesDir = new File(inputDir, GAMES_DIR);
		if (!gamesDir.exists()) {
			System.out.println("The directory [" + inputDir + "] does not contain the [" + GAMES_DIR + "] directory, exiting.");
			System.exit(1);
		}
		File utilDir = new File(inputDir, UTIL_DIR);
		if (!utilDir.exists()) {
			System.out.println("The directory [" + inputDir + "] does not contain the [" + UTIL_DIR + "] directory, exiting.");
			System.exit(1);
		}

		File inputGamesZip = new File(gamesDir, "GamesSTR.zip");
		if (!FileUtils.isExistingFile(inputGamesZip))
			inputGamesZip = new File(gamesDir, "GamesSIM.zip");
		if (!FileUtils.isExistingFile(inputGamesZip))
			inputGamesZip = new File(gamesDir, "GamesRPG.zip");
		if (!FileUtils.isExistingFile(inputGamesZip))
			inputGamesZip = new File(gamesDir, "GamesADV.zip");
		if (!FileUtils.isExistingFile(inputGamesZip))
			inputGamesZip = new File(gamesDir, "GamesACT.zip");
		if (!FileUtils.isExistingFile(inputGamesZip))
			inputGamesZip = new File(gamesDir, "GamesWIN.zip");
		if (!FileUtils.isExistingFile(inputGamesZip))
			inputGamesZip = new File(gamesDir, "Games.zip");

		if (!FileUtils.isExistingFile(inputGamesZip)) {
			System.out.println("The file [" + inputGamesZip + "] does not exist, exiting.");
			System.exit(1);
		}
		if (!tmpDir.exists()) {
			System.out.println("The directory [" + tmpDir + "] does not exist, exiting.");
			System.exit(1);
		}
		return inputGamesZip;
	}

	private static File extractMeagreMetaData(File inputGamesZip, File tmpDir) {
		System.out.println();
		System.out.println("===========================================");
		System.out.println(" Phase 1 of 3: Extracting Meagre meta-data");
		System.out.println("===========================================");
		System.out.println("Reading from: [" + inputGamesZip + "]");
		System.out.println("Writing to:   [" + tmpDir + "]");

		try {
			String mainGamesDir = findMainFolder(inputGamesZip);
			if (mainGamesDir == null) {
				System.out.println("The file [" + inputGamesZip + "] does not seem to have an inner games directory, exiting.");
				System.exit(1);
			}
			File gamesGamesDir = new File(tmpDir, mainGamesDir);
			if (!gamesGamesDir.exists()) {
				unzip(inputGamesZip, tmpDir);
			} else {
				System.out.println("Skipping extraction of [" + inputGamesZip + "] since [" + gamesGamesDir + "] already exists");
			}
			return gamesGamesDir;
		} catch (IOException e) {
			System.out.println("The file [" + inputGamesZip + "] did not fully extract into the [" + tmpDir + "] directory, exiting.");
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	private static List<GameData> analyzeMeagreMetaData(File gamesDir, File gamesGamesDir, List<DosboxVersion> dbversionsList) {
		System.out.println();
		System.out.println("==========================================");
		System.out.println(" Phase 2 of 3: Analyzing Meagre meta-data");
		System.out.println("==========================================");
		System.out.println("Reading from: [" + gamesGamesDir + "]");

		PrintStream ps = new PrintStream(System.out);
		List<GameData> expProfileList = new ArrayList<GameData>();

		File[] gameDirs = gamesGamesDir.listFiles();
		Arrays.sort(gameDirs, new FileIgnoreCaseComparator());

		for (int i = 0; i < gameDirs.length; i++) {

			File gameDir = gameDirs[i];
			File meagreDir = new File(gameDir, MEAGRE_DIR);
			File iniFileDir = new File(meagreDir, INIFILE_DIR);

			File[] iniFiles = iniFileDir.listFiles(new FileFilter() {
				public boolean accept(File file) {
					return file.isFile() && file.getName().toLowerCase().endsWith(".ini");
				}
			});
			if (iniFiles.length != 1) {
				System.out.println("SKIPPED " + gameDir.getName() + ": Not exactly 1 ini file found (" + iniFiles.length + ")");
				continue;
			}
			File iniFile = iniFiles[0];

			try {
				Conf iniConf = new Conf(iniFile, ps);
				SectionsWrapper ini = iniConf.getSettings();

				String title = ini.getValue("Main", "name");
				String devName = join(new String[] {ini.getValue("Main", "developer"), ini.getValue("Main", "designer"), ini.getValue("Main", "designer2")}, ", ");
				String publName = ini.getValue("Main", "publisher");
				String genre = join(new String[] {ini.getValue("Main", "genre"), ini.getValue("Main", "subgenre"), ini.getValue("Main", "subgenre2")}, ", ");
				String year = ini.getValue("Main", "year");
				String status = "";
				String aboutFilename = ini.getValue("Main", "about");
				String notes = StringUtils.isBlank(aboutFilename) ? "": org.apache.commons.io.FileUtils.readFileToString(new File(meagreDir, ABOUT_DIR + File.separatorChar + aboutFilename));

				boolean favorite = false;
				String[] setup = {"", "", ""};
				String[] setupParams = {"", "", ""};
				String confPathAndFile = new File(gameDir, "dosbox.conf").getPath();
				String captures = FileUtils.constructCapturesDir(i);
				int dbversionId = DosboxVersion.findDefault(dbversionsList).getId();
				String[] links = {ini.getValue("Main", "extralink1"), ini.getValue("Main", "manual"), ini.getValue("Main", "extralink2"), ini.getValue("Main", "extralink3"),
						ini.getValue("Main", "extralink4"), ini.getValue("Main", "extralink5"), ini.getValue("Main", "extralink6"), ini.getValue("Main", "extralink7")};
				String[] linkTitles = {ini.getValue("Main", "extra1"), "Manual", ini.getValue("Main", "extra2"), ini.getValue("Main", "extra3"), ini.getValue("Main", "extra4"),
						ini.getValue("Main", "extra5"), ini.getValue("Main", "extra6"), ini.getValue("Main", "extra7")};
				String[] customStrings = {"", "", "", "", "", "", "", ""};
				int[] customInts = {0, 0};

				String[] capFilenames = {ini.getValue("Main", "title01"), ini.getValue("Main", "screen01"), ini.getValue("Main", "front01"), ini.getValue("Main", "back01"),
						ini.getValue("Main", "media01"), ini.getValue("Main", "adv01")};
				List<File> capFiles = new ArrayList<File>();
				for (int c = 0; c < capFilenames.length; c++) {
					if (StringUtils.isNotEmpty(capFilenames[c])) {
						File capFile = new File(meagreDir, CAP_DIRS[c] + File.separatorChar + capFilenames[c]);
						if (FileUtils.isExistingFile(capFile))
							capFiles.add(capFile);
						else
							System.out.println("WARNING: " + gameDir.getName() + ": capture [" + capFile + "] not found, skipped");
					}
				}

				List<File> extraFiles = new ArrayList<File>();
				for (int c = 0; c < links.length; c++) {
					String link = links[c];
					if (StringUtils.isNotEmpty(link)) {
						if (!link.toLowerCase().startsWith("http")) {
							File extraFile = new File(meagreDir, ((c == 1) ? MANUAL_DIR: EXTRAS_DIR) + File.separatorChar + link);
							if (FileUtils.isExistingFile(extraFile)) {
								extraFiles.add(extraFile);
								links[c] = FileUtils.DOSROOT_DIR + gameDir.getName() + File.separatorChar + EXTRAS_DIR + File.separatorChar + extraFile.getName();
							} else
								System.out.println("WARNING: " + gameDir.getName() + ": linked file [" + extraFile + "] not found, skipped");
						}
					}
				}

				Profile profile = new Profile(i, title, devName, publName, genre, year, status, notes, favorite, setup, setupParams, confPathAndFile, captures, dbversionId, links, linkTitles,
						customStrings, customInts, null, null, null, null, 0, 0);

				Conf conf = new Conf(profile, DosboxVersion.findById(dbversionsList, profile.getDbversionId()), ps);

				Autoexec autoexec = conf.getAutoexec();
				if (autoexec.isIncomplete()) {
					System.out.println("WARNING: " + gameDir.getName() + ": This profile's autoexec section seems incomplete");
				}

				autoexec.migrateToDosroot(FileUtils.canonicalToDosroot(GAMES_DIR), true);

				File zipFile = new File(gamesDir, FilenameUtils.getBaseName(ini.getValue("Main", "executable")) + ".zip");

				// Some extra sanity-checking on install.bat
				File gameFolderInstallbat = null, zipFileInstallbat = null;

				File installFile = new File(gameDir, "Install.bat");
				if (FileUtils.isExistingFile(installFile)) {
					List<String> lines = org.apache.commons.io.FileUtils.readLines(installFile);
					for (String s: lines) {
						if (s.startsWith("IF EXIST \"")) {
							int secondQuotes = s.lastIndexOf('\"');
							if (secondQuotes != -1) {
								gameFolderInstallbat = new File(s.substring(10, secondQuotes));
								if (!gameFolderInstallbat.getName().equalsIgnoreCase(gameDir.getName()))
									System.out.println("WARNING: " + gameDir.getName()
											+ ": This game's folder as found in games???.zip does not match the folder being checked for existence in install.bat (" + gameFolderInstallbat + ")");
							}
						} else if (s.startsWith("unzip \"")) {
							int secondQuotes = s.lastIndexOf('\"');
							if (secondQuotes != -1) {
								zipFileInstallbat = new File(gamesDir, s.substring(7, secondQuotes));
								if (!zipFile.equals(zipFileInstallbat))
									System.out.println("WARNING: " + gameDir.getName() + ": This game's 'Main Executable' referenced in iniFile (" + zipFile
											+ ") does not match the file being unzipped in install.bat (" + zipFileInstallbat + ")");
							}
						}
					}
				} else {
					System.out.println("WARNING: " + gameDir.getName() + ": This game's install.bat not found");
				}

				if (!FileUtils.isExistingFile(zipFile)) {
					if (verboseOutput)
						System.out.println("Zip file " + zipFile + " not found, reverting to Install.bat to determine game zip");
					if (zipFileInstallbat != null && FileUtils.isExistingFile(zipFileInstallbat)) {
						zipFile = zipFileInstallbat;
						if (verboseOutput)
							System.out.println("Zip file " + zipFile + " referenced in Install.bat, using that file instead");
					}
				}

				List<File> list = readFilesInZip(zipFile);
				if (!fixupFileLocations(list, autoexec, zipFile)) {
					String main = autoexec.getActualMain();
					if (StringUtils.isNotEmpty(main))
						System.out.println("WARNING: " + gameDir.getName() + ": Main file [" + main + "] not found inside [" + zipFile + "]");
				}

				List<File> listFolders = readFoldersInZip(zipFile);
				if (gameFolderInstallbat != null && !listFolders.contains(new File(gameFolderInstallbat.getName()))) {
					System.out.println("WARNING: " + gameDir.getName() + ": Game folder [" + gameFolderInstallbat.getName() + "] not found inside [" + zipFile + "]");
				}

				boolean multipleRootEntries = isMultipleRootEntries(list);
				if (multipleRootEntries) {
					autoexec.updateForTargetImportBaseDir(gameDir);
					if (verboseOutput)
						System.out.println("INFO: " + gameDir.getName() + " is moved one directory level deeper");
				}

				expProfileList.add(new GameData(new ExpProfile(i, conf, new File(gameDir.getName()), null, profile), capFiles, extraFiles, zipFile, multipleRootEntries));

			} catch (IOException e) {
				System.out.println("SKIPPED " + gameDir.getName() + " " + e.toString());
			}
		}
		Collections.sort(expProfileList);

		System.out.println("Analysis done");
		return expProfileList;
	}

	private static void generateGamePackArchives(List<GameData> expProfileList, File tmpDir, String baseFilename, List<DosboxVersion> dbversionsList) {
		try {
			System.out.println();
			System.out.println("===========================================");
			System.out.println(" Phase 3 of 3: Generating GamePackArchives");
			System.out.println("===========================================");

			while (!expProfileList.isEmpty()) {

				long totalSize = 0L;
				List<GameData> gpaProfileList = new ArrayList<GameData>();
				boolean reachedMaxSize = false;

				while (!expProfileList.isEmpty() && !reachedMaxSize) {
					GameData game = expProfileList.get(0);
					try {
						long gameSize = determineSize(game);
						if (gpaProfileList.isEmpty() || ((totalSize + gameSize) < (maxPartSizeInMB * BYTES_IN_MB))) {
							gpaProfileList.add(game);
							expProfileList.remove(0);
							totalSize += gameSize;
						} else {
							reachedMaxSize = true;
						}
					} catch (IOException e) {
						System.out.println("skipping " + game.getExpProfile().getTitle() + ", " + e.toString());
						expProfileList.remove(0);
					}
				}

				List<ExpProfile> pList = new ArrayList<ExpProfile>();
				for (GameData game: gpaProfileList) {
					pList.add(game.getExpProfile());
				}

				File currentOutputGpa = new File(tmpDir, baseFilename + "__" + FileUtils.fileSystemSafe(gpaProfileList.get(0).getExpProfile().getTitle())
						+ (gpaProfileList.size() > 1 ? " - " + FileUtils.fileSystemSafe(gpaProfileList.get(gpaProfileList.size() - 1).getExpProfile().getTitle()): "") + FileUtils.GAMEPACKARCHIVE_EXT);

				ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(currentOutputGpa));

				for (GameData game: gpaProfileList) {
					try {
						ExpProfile prof = game.getExpProfile();
						System.out.print("Exporting " + prof.getTitle() + " ");

						File captures = new File(FileUtils.constructCapturesDir(prof.getId()));
						try {
							List<File> capsList = game.getCapturesList();
							for (int c = 0; c < capsList.size(); c++) {
								File srcCapFile = capsList.get(c);
								FileUtils.zipEntry(srcCapFile, new File(captures, String.valueOf(c) + "_" + srcCapFile.getName()), zos);
							}
						} catch (IOException e) {
							throw new IOException(settings.msg("dialog.export.error.exportcaptures", new Object[] {prof.getTitle(), StringRelatedUtils.toString(e)}), e);
						}

						File relativeGameDirInZip = new File(FileUtils.DOSROOT_DIR, String.valueOf(prof.getId()));
						File relativeExtrasGameDirInZip = new File(new File(relativeGameDirInZip, prof.getGameDir().getPath()), EXTRAS_DIR);
						try {
							List<File> extrasList = game.getExtrasList();
							for (int c = 0; c < extrasList.size(); c++) {
								File srcExtraFile = extrasList.get(c);
								FileUtils.zipEntry(srcExtraFile, new File(relativeExtrasGameDirInZip, srcExtraFile.getName()), zos);
							}
							System.out.print('.');
							copyZipData(game.getZipFile(), relativeGameDirInZip, zos);
						} catch (IOException e) {
							throw new IOException(settings.msg("dialog.export.error.exportgamedata", new Object[] {prof.getTitle(), StringRelatedUtils.toString(e)}), e);
						}

					} catch (IOException e2) {
						System.out.println("WARNING: The file [" + game.getZipFile() + "] could not be copied (completely) properly into the [" + currentOutputGpa + "], this game may be corrupt");
						e2.printStackTrace();
					}
				}

				Document xmlDoc = XmlUtils.getFullProfilesXML(pList, dbversionsList, GPA_TITLE, GPA_NOTES, GPA_AUTHOR, true, false, false, true);
				XmlUtils.domToZipOutputStream(xmlDoc, new File(FileUtils.PROFILES_XML), zos);

				zos.close();
				System.out.println("DBGL GamePackArchive " + currentOutputGpa + " succesfully generated");
			}

			System.out.println("Finished.");

		} catch (ParserConfigurationException | TransformerException | IOException e) {
			e.printStackTrace();
		}
	}

	private static void cleanup(File gamesDir) {
		try {
			org.apache.commons.io.FileUtils.deleteDirectory(gamesDir);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static boolean isMultipleRootEntries(final List<File> list) {
		int found = 0;
		for (File file: list) {
			if (file.getParentFile() == null)
				found++;
		}
		return found >= 1;
	}

	private static boolean fixupFileLocations(final List<File> list, final Autoexec autoexec, final File zipFile) {
		Map<Character, Mount> mounts = autoexec.nettoMounts();
		for (Mount m: mounts.values()) {
			if (m.getMountingType() == MountingType.IMAGE) {
				File[] files = m.getPath();
				for (File f: files) {
					if (!list.contains(f)) {
						File dst = findDir(list, f);
						if (dst != null) {
							m.migrateTo(f, dst);
							if (verboseOutput)
								System.out.println("Image-mounted file [" + f + "] redirected to [" + dst + "]");
						} else {
							System.out.println("WARNING: Image-mounted file [" + f + "] not found inside [" + zipFile + "]");
						}
					}
				}
			}
		}

		String main = autoexec.getActualMain();
		File mainFile = new File(main);

		int isoIdx = containsIso(mainFile.getPath());
		if (isoIdx != -1)
			mainFile = new File(mainFile.getPath().substring(0, isoIdx));

		int fatIdx = FileUtils.containsFatImage(mainFile.getPath());
		if (fatIdx != -1)
			mainFile = new File(mainFile.getPath().substring(0, fatIdx));

		if (findMainFile(list, autoexec, mainFile))
			return true;

		if (mainFile.getName().contains("~")) {
			List<File> shortFilesList = FileUtils.convertToShortFileSet(list);
			return findMainFile(shortFilesList, autoexec, mainFile);
		}

		return false;
	}

	private static boolean findMainFile(final List<File> list, final Autoexec autoexec, File mainFile) {
		if (list.contains(mainFile))
			return true;

		File newMainFile = findSuitableExtension(mainFile, list);
		if (newMainFile != null) {
			autoexec.setActualMain(newMainFile);
			if (verboseOutput)
				System.out.println("Main file [" + mainFile + "] was using wrong file extension, changed to [" + newMainFile.getPath() + "]");
			return true;
		}

		File parent1 = mainFile.getParentFile();
		if (parent1 != null) {
			File parent2 = parent1.getParentFile();
			if (parent2 != null) {
				newMainFile = new File(parent2, mainFile.getName());
				if (list.contains(newMainFile)) {
					autoexec.setActualMainPath(parent2);
					if (verboseOutput)
						System.out.println("Main file [" + mainFile + "] redirected to parent directory [" + parent2.getPath() + "]");
					return true;
				}

				newMainFile = findSuitableExtension(newMainFile, list);
				if (newMainFile != null) {
					autoexec.setActualMain(newMainFile);
					if (verboseOutput)
						System.out.println("Main file [" + mainFile + "] was using wrong file extension and dir, changed to [" + newMainFile.getPath() + "]");
					return true;
				}
			}
		}

		String[] setPaths = autoexec.getSetPaths();
		if (setPaths != null && mainFile.getName().toLowerCase().startsWith("win")) {
			File mainBaseFolder = mainFile.getParentFile();
			for (String setPath: setPaths) {
				char pd = setPath.toUpperCase().charAt(0);

				Map<Character, Mount> mounts = autoexec.nettoMounts();
				for (Mount m: mounts.values()) {
					if (m.getMountingType() == MountingType.DIR && m.getDriveletter() == pd) {
						File cp = new File(m.getPathAsString(), setPath.substring(3));
						File f1 = new File(cp, mainFile.getName());
						newMainFile = findSuitableExtension(f1, list);
						if (newMainFile != null) {
							autoexec.setActualMain(newMainFile);
							if (verboseOutput)
								System.out.println("Main file [" + mainFile + "] located using set path, changed to [" + newMainFile.getPath() + "]");

							// Check and fix path to Windows parameter executable(s)
							String params = autoexec.getMainParameters();
							if (StringUtils.isNotEmpty(params)) {
								String[] paramArray = StringUtils.split(params);
								String[] fixedParamArray = StringUtils.split(params);
								for (int i = 0; i < paramArray.length; i++) {
									if (paramArray[i].startsWith("/") || paramArray[i].startsWith("-"))
										continue; // unlikely to be file parameter, accept in any case

									String p = fixParameterPath(list, mainBaseFolder, m.getPathAsString(), paramArray[i]);
									if (p == null) {
										if (verboseOutput)
											System.out.println("INFO: Parameter [" + paramArray[i] + "] not found, might not be a file or folder");
									} else {
										fixedParamArray[i] = p;
									}
								}
								autoexec.updateMainParameters(StringUtils.join(fixedParamArray, ' '));
								if (verboseOutput)
									System.out.println("Main file parameter(s) [" + params + "] changed to [" + autoexec.getMainParameters() + "]");
							}
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	private static String fixParameterPath(final List<File> list, File mainBaseFolder, String mountPath, String param) {
		File newParamFile = findSuitableExtension(new File(FilenameUtils.normalize(new File(mainBaseFolder, param).getPath())), list);
		if (newParamFile != null)
			return newParamFile.getPath().substring(mountPath.length());
		newParamFile = findSuitableExtension(new File(FilenameUtils.normalize(new File(mainBaseFolder.getParentFile(), param).getPath())), list);
		if (newParamFile != null)
			return newParamFile.getPath().substring(mountPath.length());
		newParamFile = findSuitableExtension(new File(FilenameUtils.normalize(new File(mountPath, param).getPath())), list);
		if (newParamFile != null)
			return newParamFile.getPath().substring(mountPath.length());
		return null;
	}

	private static File findDir(final List<File> list, final File file) {
		for (File f: list)
			if (f.getName().equals(file.getName()))
				return new File(f.getParentFile(), f.getName());
		return null;
	}

	private static int containsIso(final String mountPath) {
		for (String ext: CDIMAGES) {
			int idx = mountPath.toLowerCase().indexOf(ext + File.separatorChar);
			if (idx != -1) {
				return idx + ext.length();
			}
		}
		return -1;
	}

	private static List<File> readFilesInZip(File zipFile) throws IOException {
		List<File> result = new ArrayList<File>();
		ZipFile zfile = null;
		try {
			zfile = new ZipFile(zipFile);
			for (Enumeration<? extends ZipEntry> entries = zfile.entries(); entries.hasMoreElements();) {
				try {
					ZipEntry entry = entries.nextElement();
					String name = entry.getName();
					if (!entry.isDirectory()) {
						result.add(new File(PlatformUtils.archiveToNativePath(name)));
					}
				} catch (IllegalArgumentException e) {
					System.out.println("WARNING: Zip file [" + zipFile + "] contains an entry with problematic characters in its filename");
				}
			}
		} finally {
			if (zfile != null)
				zfile.close();
		}
		return result;
	}

	private static List<File> readFoldersInZip(File zipFile) throws IOException {
		List<File> result = new ArrayList<File>();
		ZipFile zfile = null;
		try {
			zfile = new ZipFile(zipFile);
			for (Enumeration<? extends ZipEntry> entries = zfile.entries(); entries.hasMoreElements();) {
				try {
					ZipEntry entry = entries.nextElement();
					File filename = new File(PlatformUtils.archiveToNativePath(entry.getName()));
					if (entry.isDirectory() && !result.contains(filename)) {
						result.add(filename);
					} else {
						File folder = filename.getParentFile();
						if (folder != null && !result.contains(folder))
							result.add(folder);
					}
				} catch (IllegalArgumentException e) {
					// Ignore, warning already given
				}
			}
		} finally {
			if (zfile != null)
				zfile.close();
		}
		return result;
	}

	private static long determineSize(GameData game) throws IOException {
		try {
			long result = org.apache.commons.io.FileUtils.sizeOf(game.getZipFile());
			for (File file: game.getExtrasList())
				result = result + org.apache.commons.io.FileUtils.sizeOf(file);
			for (File file: game.getCapturesList())
				result = result + org.apache.commons.io.FileUtils.sizeOf(file);
			result += 1024 * 4; // reserved 4 KB for profiles.xml data
			return result;
		} catch (Exception e) {
			throw new IOException("Could not determine game size " + e.toString());
		}
	}

	private static int sizeInKB(final ZipFile zf) throws IOException {
		int kb = 0;
		for (Enumeration<? extends ZipEntry> entries = zf.entries(); entries.hasMoreElements();) {
			try {
				ZipEntry entry = entries.nextElement();
				kb += (entry.getSize() / 1024);
			} catch (IllegalArgumentException e) {
				System.out.println("WARNING: Zip file [" + zf.getName() + "] contains an entry with problematic characters in its filename");
			}
		}
		return kb;
	}

	private static String findMainFolder(final File zipFile) throws IOException {
		ZipFile zfile = null;
		try {
			zfile = new ZipFile(zipFile);
			for (Enumeration<? extends ZipEntry> entries = zfile.entries(); entries.hasMoreElements();) {
				try {
					ZipEntry entry = entries.nextElement();
					if (entry.isDirectory())
						return entry.getName();
				} catch (IllegalArgumentException e) {
					System.out.println("WARNING: Zip file [" + zipFile + "] contains an entry with problematic characters in its filename");
				}
			}
		} finally {
			if (zfile != null)
				zfile.close();
		}
		return null;
	}

	private static void unzip(final File zipFile, final File dstDir) throws IOException {
		ProgressNotifyable prog = new ProgressNotifyable() {
			private int tot = 0;
			private int prog = 0;

			public void setTotal(int total) {
				tot = total;
			}

			public void incrProgress(int progress) {
				prog += progress;
				System.out.printf("\rExtracting %s: %3.1f%%", zipFile, (double)prog * 100.0 / (double)tot);
			}

			public void setProgress(int progress) {}
		};

		ZipFile zfile = null;
		try {
			zfile = new ZipFile(zipFile);
			prog.setTotal(sizeInKB(zfile));
			for (Enumeration<? extends ZipEntry> entries = zfile.entries(); entries.hasMoreElements();) {
				try {
					ZipEntry entry = entries.nextElement();
					File dstFile = new File(dstDir, entry.getName());
					FileUtils.extractEntry(zfile, entry, dstFile, prog);
				} catch (IllegalArgumentException e) {
					System.out.println("WARNING: Zip file [" + zipFile + "] contains an entry with problematic characters in its filename");
				}
			}
			System.out.printf("\rExtracting %s: Done  \n", zipFile);
		} finally {
			if (zfile != null)
				zfile.close();
		}
	}

	private static void copyZipData(final File srcFile, final File baseDirectory, final ZipOutputStream zos) throws ZipException, IOException {
		ZipFile srcZipFile = null;
		try {
			srcZipFile = new ZipFile(srcFile);
			int sizeInKB = sizeInKB(srcZipFile);
			for (Enumeration<? extends ZipEntry> entries = srcZipFile.entries(); entries.hasMoreElements();) {
				try {
					ZipEntry srcEntry = entries.nextElement();
					File dstFilename = new File(baseDirectory, srcEntry.getName());
					ZipEntry dstEntry = new ZipEntry(PlatformUtils.toArchivePath(dstFilename, srcEntry.isDirectory()));
					dstEntry.setComment(srcEntry.getComment());
					dstEntry.setTime(srcEntry.getTime());
					zos.putNextEntry(dstEntry);
					if (!srcEntry.isDirectory()) {
						IOUtils.copyLarge(srcZipFile.getInputStream(srcEntry), zos);
					}
					zos.closeEntry();

					int progress = (int)(srcEntry.getSize() / 1024);
					if (((double)progress / (double)sizeInKB) > 0.03)
						System.out.print('.');
				} catch (IllegalArgumentException e) {
					System.out.println("WARNING: Zip file [" + srcFile + "] contains an entry with problematic characters in its filename");
				}
			}
			System.out.println(". Done");
		} finally {
			if (srcZipFile != null)
				srcZipFile.close();
		}
	}

	private static File findSuitableExtension(final File mainFile, final List<File> list) {
		for (int i = 0; i < FileUtils.EXECUTABLES.length; i++) {
			File newMainFile = new File(FilenameUtils.removeExtension(mainFile.getPath()) + FileUtils.EXECUTABLES[i]);
			if (list.contains(newMainFile))
				return newMainFile;
		}
		return null; // not found
	}

	public static String join(final String[] array, final String separator) {
		StringBuffer buf = new StringBuffer();
		boolean first = true;
		for (String s: array) {
			if (StringUtils.isNotEmpty(s)) {
				if (!first)
					buf.append(separator);
				buf.append(s);
				first = false;
			}
		}
		return buf.toString();
	}
}
