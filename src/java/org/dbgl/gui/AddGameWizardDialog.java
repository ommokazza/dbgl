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
package org.dbgl.gui;

/*
 * 0. title, choose A) install game, B) pre-installed game
 * 
 * Install:
 * 1 choose install.exe, add mount c data\tmpinst to mounts, start installer, end with exit
 * 2 ask for patch, if available have the user copy the files into data\tmpinst and run exe
 * 
 * Pre-installed:
 * 3 choose main+setup for just installed game using combo
 * 4 choose main+setup for pre-installed game using browse
 * 5 suggest moving game into dosroot, have the user choose the exact location
 * 6 choose template, machine, core, cycles, mounts
 */

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.dbgl.db.Database;
import org.dbgl.gui.BrowseButton.BrowseType;
import org.dbgl.gui.BrowseButton.CanonicalType;
import org.dbgl.gui.LoadSharedConfDialog.SharedConfLoading;
import org.dbgl.loopy.iso9660.ISO9660FileSystem;
import org.dbgl.model.DosboxVersion;
import org.dbgl.model.ExpProfile;
import org.dbgl.model.KeyValuePair;
import org.dbgl.model.Mount;
import org.dbgl.model.NativeCommand;
import org.dbgl.model.Profile;
import org.dbgl.model.Template;
import org.dbgl.model.WebProfile;
import org.dbgl.model.conf.Conf;
import org.dbgl.model.conf.SectionsWrapper;
import org.dbgl.model.conf.Settings;
import org.dbgl.model.conf.SharedConf;
import org.dbgl.swtdesigner.SWTImageManager;
import org.dbgl.util.FileUtils;
import org.dbgl.util.StringRelatedUtils;
import org.dbgl.util.searchengine.HotudSearchEngine;
import org.dbgl.util.searchengine.MobyGamesSearchEngine;
import org.dbgl.util.searchengine.PouetSearchEngine;
import org.dbgl.util.searchengine.TheGamesDBSearchEngine;
import org.dbgl.util.searchengine.WebSearchEngine;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;


public class AddGameWizardDialog extends Wizard {

	final static java.util.List<WebSearchEngine> webSearchEngines = Arrays.asList(MobyGamesSearchEngine.getInstance(), PouetSearchEngine.getInstance(), HotudSearchEngine.getInstance(),
		TheGamesDBSearchEngine.getInstance());

	private static final String[] CUST_STRINGS = new String[] {"", "", "", "", "", "", "", ""};

	private Text title;
	private String developer, publisher, year, genre, notes;
	private String[] link = new String[] {"", "", "", "", "", "", "", ""};
	private String[] linkTitle = new String[] {"", "", "", "", "", "", "", ""};
	private final int[] customInts = new int[] {0, 0};

	private Button moveImages, templateReload;
	private ToolItem loadSharedConfButton, engineSelector;
	private Button btnPreinstalledGame, btnGameNeedsToBeInstalled, btnInstallManual, btnPatchManual;
	private Combo main, setup;
	private Text mainText, setupText;
	private Text installExe, installParameters, patchExe, patchParameters, dstDirectory, imagesDstDirectory;
	private List mountingpoints;
	private File[] installedFiles;
	private java.util.List<File> orgImages;
	private List installedFilesList, orgImagesList;
	private Combo template;
	private Combo machine, core, cycles;

	private Database dbase;
	private java.util.List<DosboxVersion> dbversionsList;
	private java.util.List<Template> templatesList;
	private ExpProfile profile;
	private int templateIndex = -1;

	public AddGameWizardDialog(final Shell parent, final int style) {
		super(parent, style, Settings.getInstance().msg("dialog.addgamewizard.title"), "addgamewizard", false);
	}

	protected boolean init() {
		dbase = Database.getInstance();

		try {
			dbversionsList = dbase.readDosboxVersionsList();
			templatesList = dbase.readTemplatesList();

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(bos);

			DosboxVersion dbversion = DosboxVersion.findDefault(dbversionsList);
			templateIndex = Template.indexOfDefault(templatesList);
			if (templateIndex != -1) {
				java.util.List<NativeCommand> nativeCommands = dbase.readNativeCommandsList(-1, templatesList.get(templateIndex).getId());
				profile = new ExpProfile(new Conf((File)null, templatesList.get(templateIndex), dbversion, ps), nativeCommands);
			} else {
				java.util.List<NativeCommand> nativeCommands = dbase.readNativeCommandsList(-1, -1);
				profile = new ExpProfile(new Conf((File)null, (Template)null, dbversion, ps), nativeCommands);
			}

			developer = "";
			publisher = "";
			year = "";
			genre = "";
			notes = "";

			return true;
		} catch (Exception e) {
			GeneralPurposeDialogs.warningMessage(getParent(), e);
			return false;
		}
	}

	protected void onExit() {
		try {
			org.apache.commons.io.FileUtils.deleteDirectory(FileUtils.getTmpInstallFile());
		} catch (IOException e) {
			GeneralPurposeDialogs.warningMessage(shell, e);
		}
	}

	protected int stepSize(boolean up) {
		if (btnPreinstalledGame.getSelection()) {
			if ((up && stepNr == 0) || (!up && stepNr == 4))
				return 4; // skip installing and patching and maincombo
			if ((up && stepNr == 4) || (!up && stepNr == 6))
				return 2; // skip moving game data
		} else {
			if ((up && stepNr == 3) || (!up && stepNr == 5))
				return 2; // skip maintext
		}
		return super.stepSize(up);
	}

	protected boolean isValidInput() {
		GeneralPurposeDialogs.initErrorDialog();
		if (stepNr == 0) {
			return titleEntered();
		} else if (stepNr == 1) {
			return installExeEntered();
		} else if (stepNr == 2) {
			return true;
		} else if (stepNr == 3) {
			return mainExeEntered();
		} else if (stepNr == 4) {
			return mainExeEntered();
		} else if (stepNr == 5) {
			return conditionsOkForStep5();
		} else if (stepNr == 6) {
			return true;
		}
		return true;
	}

	protected boolean actionAfterNext() {
		if (stepNr == 0) {
			return doWebSearch();
		} else if (stepNr == 1) {
			return runInstallerAndCheckResults();
		} else if (stepNr == 2) {
			return determineMainAndSetup();
		} else if (stepNr == 3) {
			return setMain();
		} else if (stepNr == 4) {
			return setMain();
		} else if (stepNr == 5) {
			return true;
		} else if (stepNr == 6) {
			return createProfile();
		}
		return false;
	}

	protected void doReloadTemplate(int templateIndex) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(bos);

			Conf templateConf = new Conf(templatesList.get(templateIndex), profile.getConf().getDbversion(), ps);
			profile.getConf().reloadTemplate(templateConf);

			selectSettingsByConfiguration(profile.getConf());
			profile.setNativeCommandsList(dbase.readNativeCommandsList(-1, templatesList.get(templateIndex).getId()));

			if (bos.size() > 0) {
				GeneralPurposeDialogs.warningMessage(getParent(), bos.toString());
				bos.reset();
			}
		} catch (IOException | SQLException e) {
			GeneralPurposeDialogs.warningMessage(getParent(), e);
		}
	}

	protected void selectSettingsByConfiguration(Conf conf) {
		SectionsWrapper sections = conf.getSettings();
		machine.setText(sections.getValue("dosbox", "machine"));
		core.setText(sections.getValue("cpu", "core"));
		cycles.setText(sections.getValue("cpu", "cycles"));
	}

	protected void updateConfigurationBySettings(Conf conf) {
		conf.updateValue("dosbox", "machine", machine.getText(), true);
		conf.updateValue("cpu", "core", core.getText(), true);
		conf.updateValue("cpu", "cycles", cycles.getText(), true);
	}

	protected void doAddMount() {
		final EditMountDialog addMountDialog = new EditMountDialog(shell);
		try {
			addMountDialog.setDefaultDriveletter(Mount.getFreeDriveletter(false, profile.getConf().nettoMountedDriveLetters()));
		} catch (Exception e) {
			// nothing we can do, just take default 'C'
		}
		String mount = (String)addMountDialog.open();
		if (mount != null) {
			mountingpoints.setItems(profile.getConf().addMount(mount));
			mountingpoints.select(mountingpoints.getItemCount() - 1);
		}
	}

	protected void doEditMount() {
		int mounts = mountingpoints.getItemCount();
		int sel = mountingpoints.getSelectionIndex();
		if (sel != -1) {
			final EditMountDialog editMountDialog = new EditMountDialog(shell);
			editMountDialog.setMount(mountingpoints.getItem(sel));
			String mount = (String)editMountDialog.open();
			if (mount != null) {
				mountingpoints.setItems(profile.getConf().editMount(sel, mount));
				if (mountingpoints.getItemCount() == mounts) {
					mountingpoints.select(sel);
				} else {
					mountingpoints.select(mountingpoints.getItemCount() - 1);
				}
			}
		}
	}

	protected void doRemoveMount() {
		int mounts = mountingpoints.getItemCount();
		int sel = mountingpoints.getSelectionIndex();
		if (sel == -1 && mounts == 1) {
			sel = 0;
			mountingpoints.select(sel);
		}
		if (sel != -1) {
			mountingpoints.setItems(profile.getConf().removeMount(sel));
			if (mountingpoints.getItemCount() == mounts) {
				mountingpoints.select(sel);
			} else {
				if (mountingpoints.getItemCount() > 0) {
					mountingpoints.select(mountingpoints.getItemCount() - 1);
				}
			}
		}
	}

	private boolean titleEntered() {
		if (title.getText().trim().length() <= 0)
			GeneralPurposeDialogs.addError(settings.msg("dialog.profile.required.title"), title);
		return !GeneralPurposeDialogs.displayErrorDialog(shell);
	}

	private boolean doWebSearch() {
		if ((Boolean)loadSharedConfButton.getData("selected")) {
			String currTitle = title.getText();
			if (currTitle.length() >= 1) {
				try {
					Client client = ClientBuilder.newClient();
					GenericType<java.util.List<SharedConf>> confType = new GenericType<java.util.List<SharedConf>>() {};
					java.util.List<SharedConf> confs = client.target(settings.getSettings().getValue("confsharing", "endpoint")).path("/configurations/bytitle/{i}").resolveTemplate("i",
						currTitle).request().accept(MediaType.APPLICATION_XML).get(confType);
					client.close();

					if (confs.size() == 0) {
						GeneralPurposeDialogs.infoMessage(shell, settings.msg("general.notice.searchenginenoresults", new String[] {EditProfileDialog.DBCONFWS, currTitle}));
					} else {
						SharedConfLoading result = (SharedConfLoading)new LoadSharedConfDialog(shell, currTitle, confs).open();
						if (result != null) {
							profile.getConf().loadSharedConf(result.conf.getIncrConf(), result.reloadDosboxDefaults);
							selectSettingsByConfiguration(profile.getConf());
						}
					}
				} catch (Exception e) {
					GeneralPurposeDialogs.warningMessage(shell,
						settings.msg("general.error.retrieveinfosearchengine", new String[] {EditProfileDialog.DBCONFWS, currTitle, StringRelatedUtils.toString(e)}), e);
				}
			}
		}

		if ((Boolean)engineSelector.getData("selected")) {
			String currTitle = title.getText();
			if (currTitle.length() >= 1) {
				WebSearchEngine engine = getSelectedSearchEngine();
				try {
					WebProfile thisGame = null;
					java.util.List<WebProfile> webGamesList = engine.getEntries(currTitle, settings.getSettings().getValues(engine.getSimpleName(), "platform_filter"));
					if (webGamesList.size() >= 1) {
						BrowseSearchEngineDialog mobyDialog = new BrowseSearchEngineDialog(shell);
						mobyDialog.setProfilesToBrowse(currTitle, webGamesList);
						mobyDialog.setEngine(engine);
						Integer idx = (Integer)mobyDialog.open();
						if (idx != null) {
							thisGame = webGamesList.get(idx);
						}
					}
					if (thisGame != null) {
						final WebProfile profExt = engine.getEntryDetailedInformation(thisGame);
						if (settings.getSettings().getBooleanValue(engine.getSimpleName(), "set_title"))
							title.setText(profExt.getTitle());
						if (settings.getSettings().getBooleanValue(engine.getSimpleName(), "set_developer"))
							developer = profExt.getDeveloperName();
						if (settings.getSettings().getBooleanValue(engine.getSimpleName(), "set_publisher"))
							publisher = profExt.getPublisherName();
						if (settings.getSettings().getBooleanValue(engine.getSimpleName(), "set_year"))
							year = profExt.getYear();
						if (settings.getSettings().getBooleanValue(engine.getSimpleName(), "set_genre"))
							genre = profExt.getGenre();
						if (settings.getSettings().getBooleanValue(engine.getSimpleName(), "set_link")) {
							link[0] = profExt.getUrl();
							linkTitle[0] = settings.msg("dialog.profile.searchengine.link.maininfo", new String[] {engine.getName()});
						}
						if (settings.getSettings().getBooleanValue(engine.getSimpleName(), "set_description")) {
							notes = profExt.getNotes();
						}
						if (settings.getSettings().getBooleanValue(engine.getSimpleName(), "set_rank")) {
							customInts[0] = profExt.getRank();
						}
					}
				} catch (Exception e) {
					GeneralPurposeDialogs.warningMessage(shell, settings.msg("general.error.retrieveinfosearchengine", new String[] {engine.getName(), currTitle, StringRelatedUtils.toString(e)}), e);
				}
			}
		}

		if (btnGameNeedsToBeInstalled.getSelection() && StringUtils.isBlank(installExe.getText())) {
			profile.getConf().unmountDosboxMounts();
			profile.getConf().addMount("mount C \"" + FileUtils.getTmpInstallFile() + "\"");
			mountingpoints.setItems(profile.getConf().getAutoexec().getMountingpoints());
		}
		return true;
	}

	private boolean installExeEntered() {
		if (installExe.getText().trim().length() <= 0)
			GeneralPurposeDialogs.addError(settings.msg("dialog.addgamewizard.required.installexe"), installExe);
		return !GeneralPurposeDialogs.displayErrorDialog(shell);
	}

	private boolean runInstallerAndCheckResults() {
		try {
			org.apache.commons.io.FileUtils.deleteDirectory(FileUtils.getTmpInstallFile());
			FileUtils.createDir(FileUtils.getTmpInstallFile());

			profile.getConf().getAutoexec().setMainExecutable(installExe.getText(), installParameters.getText());

			FileUtils.doRunInstaller(profile, profile.getConf(), dbversionsList, null, btnInstallManual.getSelection());
			shell.forceFocus();
			shell.forceActive();

			orgImages = new ArrayList<File>();
			File[] firstImageMountPath = profile.getConf().getFirstImageMountPath();
			if (firstImageMountPath != null) {
				for (File f: firstImageMountPath)
					orgImages.add(FileUtils.canonicalToDosroot(f.getPath()));
			}
			java.util.List<File> additionalImageFiles = new ArrayList<File>();
			for (File file: orgImages) {
				if (file.getName().toLowerCase().endsWith(FileUtils.CDIMAGES[1])) { // cue sheet
					File binFile = ISO9660FileSystem.parseCueSheet(file);
					if (binFile != null && file.getParentFile().equals(binFile.getParentFile())) {
						additionalImageFiles.add(FileUtils.canonicalToDosroot(binFile.getPath()));
					}
				}
			}
			orgImages.addAll(additionalImageFiles);

			installedFiles = FileUtils.getTmpInstallFile().listFiles();
			if (installedFiles.length == 0) {
				GeneralPurposeDialogs.warningMessage(shell, settings.msg("dialog.addgamewizard.error.nofilesinstalled"));
				return false;
			}

			return true;
		} catch (IOException e) {
			GeneralPurposeDialogs.warningMessage(shell, e);
			return false;
		}
	}

	private boolean determineMainAndSetup() {
		if (patchExe.getText().trim().length() > 0) {
			try {
				profile.getConf().getAutoexec().setMainExecutable(patchExe.getText(), patchParameters.getText());

				FileUtils.doRunInstaller(profile, profile.getConf(), dbversionsList, null, btnPatchManual.getSelection());
				shell.forceFocus();
				shell.forceActive();
			} catch (IOException e) {
				GeneralPurposeDialogs.warningMessage(shell, e);
				return false;
			}
		}

		orgImagesList.removeAll();
		if (orgImages != null) {
			for (File file: orgImages) {
				orgImagesList.add(FileUtils.makeRelativeToDosroot(file).getPath());
			}
		}
		orgImagesList.selectAll();
		orgImagesList.pack();
		orgImagesList.getParent().layout();

		installedFilesList.removeAll();
		File gameDir = null;
		for (File file: installedFiles) {
			if (file.isDirectory()) {
				installedFilesList.add("[ " + FileUtils.makeRelativeToDosroot(file).getPath() + " ]");
				if (gameDir == null)
					gameDir = file;
			} else {
				installedFilesList.add(FileUtils.makeRelativeToDosroot(file).getPath());
			}
		}
		installedFilesList.selectAll();
		installedFilesList.pack();
		installedFilesList.getParent().layout();

		moveImages.setEnabled(profile.getConf().countImageMounts() == 1);
		String imagesDirString = settings.getSettings().getValue("directory", "orgimages");
		File imagesSubDir = gameDir != null ? new File(gameDir.getName(), imagesDirString): new File(imagesDirString);
		imagesDstDirectory.setText(imagesSubDir.getPath());

		java.util.List<File> executables = FileUtils.getExecutablesInDirRecursive(FileUtils.getTmpInstallFile());

		main.removeAll();
		setup.removeAll();
		setup.add("");
		for (File f: executables) {
			main.add(FileUtils.makeRelativeToDosroot(f).getPath());
			setup.add(FileUtils.makeRelativeToDosroot(f).getPath());
		}
		if (executables.isEmpty())
			main.add(installExe.getText());
		int mainFileIndex = FileUtils.findMostLikelyMainIndex(title.getText(), executables);
		if (mainFileIndex != -1) {
			main.select(mainFileIndex);
		} else {
			main.select(0);
		}
		int setupFileIndex = FileUtils.findSetupIndex(executables);
		if (setupFileIndex != -1) {
			setup.select(setupFileIndex + 1);
		} else {
			setup.select(0);
		}
		setup.setEnabled(setup.getItemCount() > 1);
		return true;
	}

	private boolean mainExeEntered() {
		if (btnPreinstalledGame.getSelection()) {
			if (mainText.getText().trim().length() <= 0)
				GeneralPurposeDialogs.addError(settings.msg("dialog.profile.required.mainexe"), mainText);
		} else {
			if (main.getText().trim().length() <= 0)
				GeneralPurposeDialogs.addError(settings.msg("dialog.profile.required.mainexe"), main);
		}
		return !GeneralPurposeDialogs.displayErrorDialog(shell);
	}

	private boolean setMain() {
		if (btnPreinstalledGame.getSelection()) {
			if (profile.getConf().getRequiredMount(false, mainText.getText()) != null) {
				profile.getConf().addRequiredMount(false, mainText.getText());
			}
			profile.getConf().getAutoexec().setMainExecutable(mainText.getText(), "");
		} else {
			if (profile.getConf().getRequiredMount(false, main.getText()) != null) {
				profile.getConf().addRequiredMount(false, main.getText());
			}
			profile.getConf().getAutoexec().setMainExecutable(main.getText(), "");
		}
		return true;
	}

	private boolean conditionsOkForStep5() {
		if (btnPreinstalledGame.getSelection())
			return true;

		try {
			if (installedFilesList.getSelectionCount() > 0) {
				File destDir = new File(dstDirectory.getText());
				if (!destDir.isDirectory()) {
					if (GeneralPurposeDialogs.confirmMessage(shell, settings.msg("dialog.addgamewizard.confirm.createdestinationdir", new String[] {destDir.toString()}))) {
						destDir.mkdirs();
					}
				}
				if (!destDir.isDirectory()) {
					GeneralPurposeDialogs.addError(settings.msg("dialog.addgamewizard.error.destinationdirmissing", new String[] {destDir.toString()}), dstDirectory);
				} else {
					for (int i = 0; i < installedFiles.length; i++) {
						if (installedFilesList.isSelected(i)) {
							File destFile = new File(destDir, installedFiles[i].getName());
							if (org.apache.commons.io.FileUtils.directoryContains(destDir, destFile)) {
								GeneralPurposeDialogs.addError(settings.msg("dialog.addgamewizard.error.gamedatadirexists", new String[] {destFile.toString()}), dstDirectory);
							}
						}
					}
				}
			} else {
				GeneralPurposeDialogs.addError(settings.msg("dialog.addgamewizard.error.gamedatamustbemoved"), installedFilesList);
			}

			return !GeneralPurposeDialogs.displayErrorDialog(shell);
		} catch (IOException e) {
			GeneralPurposeDialogs.warningMessage(shell, e);
			return false;
		}
	}

	private boolean createProfile() {
		try {

			String setupString = btnPreinstalledGame.getSelection() ? setupText.getText(): setup.getText();

			if (installedFilesList.getSelectionCount() > 0) {
				File destDir = new File(dstDirectory.getText());
				profile.getConf().getAutoexec().migrateTo(FileUtils.getTmpInstallFile(), destDir);
				profile.getConf().removeFloppyMounts();
				profile.getConf().removeUnnecessaryMounts();

				if (StringUtils.isNotBlank(setupString)) {
					setupString = FileUtils.makeRelativeTo(FileUtils.canonicalToDosroot(setupString), FileUtils.getTmpInstallFile()).getPath();
					setupString = FileUtils.makeRelativeToDosroot(new File(destDir, setupString)).getPath();
				}

				for (int i = 0; i < installedFiles.length; i++) {
					File src = installedFiles[i];
					if (installedFilesList.isSelected(i)) {
						org.apache.commons.io.FileUtils.moveToDirectory(src, destDir, true);
					}
				}

				if (moveImages.getSelection()) {
					File imgDestDir = new File(destDir, imagesDstDirectory.getText());
					FileUtils.createDir(imgDestDir);

					for (int i = 0; i < orgImages.size(); i++) {
						File src = orgImages.get(i);
						if (orgImagesList.isSelected(i)) {
							org.apache.commons.io.FileUtils.moveToDirectory(src, imgDestDir, true);
							profile.getConf().getAutoexec().migrateTo(src, new File(imgDestDir, src.getName()));
						}
					}
				}
			}

			String status = "";
			boolean favorite = false;
			int devId = KeyValuePair.findIdByValue(dbase.readDevelopersList(), developer);
			int publId = KeyValuePair.findIdByValue(dbase.readPublishersList(), publisher);
			int genId = KeyValuePair.findIdByValue(dbase.readGenresList(), genre);
			int yrId = KeyValuePair.findIdByValue(dbase.readYearsList(), year);
			int statId = KeyValuePair.findIdByValue(dbase.readStatusList(), status);

			int[] custIDs = new int[] {0, 0, 0, 0};
			for (int i = 0; i < 4; i++) {
				custIDs[i] = KeyValuePair.findIdByValue(dbase.readCustomList(i), "");
			}

			dbase.startTransaction();

			Profile newProfile = dbase.addOrEditProfile(title.getText(), developer, publisher, genre, year, status, notes, favorite, new String[] {setupString, "", ""}, new String[] {"", "", ""},
				devId, publId, genId, yrId, statId, profile.getDbversionId(), link, linkTitle, CUST_STRINGS, customInts, custIDs, -1);
			dbase.saveNativeCommands(profile.getNativeCommandsList(), newProfile.getId(), -1);

			String confString = FileUtils.constructUniqueConfigFileString(newProfile.getId(), title.getText(),
				profile.getConf().getAutoexec().isIncomplete() ? null: profile.getConf().getAutoexec().getCanonicalMainDir());
			String capturesString = FileUtils.constructCapturesDir(newProfile.getId());
			FileUtils.createDir(FileUtils.canonicalToData(capturesString));
			newProfile = dbase.updateProfileConf(confString, capturesString, newProfile.getId());

			updateConfigurationBySettings(profile.getConf());
			profile.getConf().injectOrUpdateProfile(newProfile);
			profile.getConf().save();

			dbase.commitTransaction();

			result = newProfile;

		} catch (IOException | SQLException e) {
			GeneralPurposeDialogs.warningMessage(shell, e);
			try {
				dbase.rollbackTransaction();
			} catch (SQLException se) {
				GeneralPurposeDialogs.warningMessage(shell, se);
			}
		} finally {
			dbase.finishTransaction();
		}

		return true;
	}

	private WebSearchEngine getSearchEngineBySimpleName(String simpleName) {
		for (WebSearchEngine engn: webSearchEngines) {
			if (engn.getSimpleName().equalsIgnoreCase(simpleName))
				return engn;
		}
		return null;
	}

	private WebSearchEngine getSelectedSearchEngine() {
		WebSearchEngine engine = null;
		for (WebSearchEngine engn: webSearchEngines) {
			if (engn.getName().equals(engineSelector.getData("engine")))
				engine = engn;
		}
		return engine;
	}

	protected void fillPages() {
		addStep(page0());
		addStep(page1());
		addStep(page2());
		addStep(page3());
		addStep(page4());
		addStep(page5());
		addStep(page6());
	}

	private Control page0() {
		final Group group = new Group(shell, SWT.NONE);
		group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		group.setText(settings.msg("dialog.addgamewizard.step1"));
		group.setLayout(new GridLayout(4, false));

		final Label titleLabel = new Label(group, SWT.NONE);
		titleLabel.setText(settings.msg("dialog.profile.title"));
		title = new Text(group, SWT.BORDER);
		title.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		title.setFocus();

		WebSearchEngine defaultEngine = getSearchEngineBySimpleName(settings.getSettings().getValue("gui", "searchengine"));
		if (defaultEngine == null)
			defaultEngine = webSearchEngines.get(0);

		final ToolBar toolBar = new ToolBar(group, SWT.FLAT);
		engineSelector = new ToolItem(toolBar, SWT.DROP_DOWN);
		boolean engineEnabledByDefault = settings.getSettings().getBooleanValue("addgamewizard", "consultsearchengine");
		if (engineEnabledByDefault)
			engineSelector.setImage(SWTImageManager.getResourceImage(shell.getDisplay(), defaultEngine.getIcon()));
		else
			engineSelector.setImage(SWTImageManager.createDisabledImage(SWTImageManager.getResourceImage(shell.getDisplay(), defaultEngine.getIcon())));
		engineSelector.setData("selected", engineEnabledByDefault);
		engineSelector.setToolTipText(settings.msg("dialog.profile.consultsearchengine", new String[] {defaultEngine.getName()}));
		engineSelector.setData("engine", defaultEngine.getName());

		final Menu menu = new Menu(shell, SWT.POP_UP);
		for (final WebSearchEngine engine: webSearchEngines) {
			MenuItem item = new MenuItem(menu, SWT.PUSH);
			item.setImage(SWTImageManager.getResourceImage(shell.getDisplay(), engine.getIcon()));
			item.setText(engine.getName());
			item.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(final SelectionEvent event) {
					MenuItem sel = (MenuItem)event.widget;
					engineSelector.setImage(sel.getImage());
					engineSelector.setData("engine", sel.getText());
					engineSelector.setToolTipText(settings.msg("dialog.profile.consultsearchengine", new String[] {engineSelector.getData("engine").toString()}));
					engineSelector.setData("selected", true);
					settings.getSettings().setBooleanValue("addgamewizard", "consultsearchengine", true);
					settings.getSettings().setValue("gui", "searchengine", engine.getSimpleName());
				}
			});
		}

		engineSelector.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (event.detail == SWT.ARROW) {
					Rectangle rect = engineSelector.getBounds();
					Point pt = new Point(rect.x, rect.y + rect.height);
					pt = toolBar.toDisplay(pt);
					menu.setLocation(pt.x, pt.y);
					menu.setVisible(true);
				} else {
					WebSearchEngine engine = getSelectedSearchEngine();
					boolean engineEnabled = !((Boolean)engineSelector.getData("selected"));
					if (engineEnabled)
						engineSelector.setImage(SWTImageManager.getResourceImage(shell.getDisplay(), engine.getIcon()));
					else
						engineSelector.setImage(SWTImageManager.createDisabledImage(SWTImageManager.getResourceImage(shell.getDisplay(), engine.getIcon())));
					engineSelector.setData("selected", engineEnabled);
					settings.getSettings().setBooleanValue("addgamewizard", "consultsearchengine", engineEnabled);
				}
			}
		});

		loadSharedConfButton = new ToolItem(toolBar, SWT.PUSH);
		boolean loadSharedConfEnabledByDefault = settings.getSettings().getBooleanValue("addgamewizard", "consultdbconfws");
		if (loadSharedConfEnabledByDefault)
			loadSharedConfButton.setImage(SWTImageManager.getResourceImage(shell.getDisplay(), SWTImageManager.IMG_SHARE));
		else
			loadSharedConfButton.setImage(SWTImageManager.createDisabledImage(SWTImageManager.getResourceImage(shell.getDisplay(), SWTImageManager.IMG_SHARE)));
		loadSharedConfButton.setData("selected", loadSharedConfEnabledByDefault);
		loadSharedConfButton.setToolTipText(settings.msg("button.consultconfsearchengine", new String[] {EditProfileDialog.DBCONFWS}));
		loadSharedConfButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				if ((Boolean)loadSharedConfButton.getData("selected")) {
					loadSharedConfButton.setImage(SWTImageManager.createDisabledImage(SWTImageManager.getResourceImage(shell.getDisplay(), SWTImageManager.IMG_SHARE)));
					loadSharedConfButton.setData("selected", false);
					settings.getSettings().setBooleanValue("addgamewizard", "consultdbconfws", false);
				} else {
					loadSharedConfButton.setImage(SWTImageManager.getResourceImage(shell.getDisplay(), SWTImageManager.IMG_SHARE));
					loadSharedConfButton.setData("selected", true);
					settings.getSettings().setBooleanValue("addgamewizard", "consultdbconfws", true);
				}
			}
		});

		Composite composite = new Composite(group, SWT.NONE);
		composite.setLayout(new GridLayout());
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1);
		gd.verticalIndent = 20;
		composite.setLayoutData(gd);
		final Label thisGameLabel = new Label(composite, SWT.NONE);
		thisGameLabel.setText(settings.msg("dialog.addgamewizard.thisgame"));
		btnPreinstalledGame = new Button(composite, SWT.RADIO);
		btnPreinstalledGame.setText(settings.msg("dialog.addgamewizard.preinstalled"));
		btnGameNeedsToBeInstalled = new Button(composite, SWT.RADIO);
		btnGameNeedsToBeInstalled.setText(settings.msg("dialog.addgamewizard.notyetinstalled"));
		boolean requiresInstallation = settings.getSettings().getBooleanValue("addgamewizard", "requiresinstallation");
		btnPreinstalledGame.setSelection(!requiresInstallation);
		btnGameNeedsToBeInstalled.setSelection(requiresInstallation);
		SelectionAdapter adapter = new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				settings.getSettings().setBooleanValue("addgamewizard", "requiresinstallation", event.widget == btnGameNeedsToBeInstalled);
			}
		};
		btnPreinstalledGame.addSelectionListener(adapter);
		btnGameNeedsToBeInstalled.addSelectionListener(adapter);

		return group;
	}

	private Control page1() {
		final Group group = new Group(shell, SWT.NONE);
		group.setText(settings.msg("dialog.addgamewizard.step2"));
		group.setLayout(new GridLayout());

		Composite installexeComposite = new Composite(group, SWT.NONE);
		installexeComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		installexeComposite.setLayout(new GridLayout(3, false));
		final Label mainExeLabel = new Label(installexeComposite, SWT.NONE);
		mainExeLabel.setText(settings.msg("dialog.addgamewizard.installexe"));
		installExe = new Text(installexeComposite, SWT.BORDER);
		installExe.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		final BrowseButton installexeBrowseButton = new BrowseButton(installexeComposite, SWT.NONE);
		installexeBrowseButton.connect(shell, installExe, null, BrowseType.FILE, CanonicalType.INSTALLER, false, null);

		final Label parametersLabel = new Label(installexeComposite, SWT.NONE);
		parametersLabel.setText(settings.msg("dialog.profile.mainparameters"));
		installParameters = new Text(installexeComposite, SWT.BORDER);
		installParameters.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		final Label manualLabel = new Label(installexeComposite, SWT.NONE);
		manualLabel.setText(settings.msg("dialog.addgamewizard.manualmode"));
		btnInstallManual = new Button(installexeComposite, SWT.CHECK);
		btnInstallManual.setText(settings.msg("dialog.addgamewizard.manualmodeinfo"));
		btnInstallManual.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));

		final Group mountGroup = new Group(group, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.verticalIndent = 20;
		mountGroup.setLayoutData(gd);
		mountGroup.setText(settings.msg("dialog.template.mountingoverview"));
		mountGroup.setLayout(new GridLayout(2, false));

		mountingpoints = new List(mountGroup, SWT.V_SCROLL | SWT.BORDER);
		mountingpoints.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 3));
		mountingpoints.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(final MouseEvent event) {
				if (mountingpoints.getSelectionIndex() == -1) {
					doAddMount();
				} else {
					doEditMount();
				}
			}
		});

		final Button addButton = new Button(mountGroup, SWT.NONE);
		addButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		addButton.setText(settings.msg("dialog.template.mount.add"));
		addButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				doAddMount();
			}
		});

		final Button editButton = new Button(mountGroup, SWT.NONE);
		editButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		editButton.setText(settings.msg("dialog.template.mount.edit"));
		editButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				doEditMount();
			}
		});

		final Button removeButton = new Button(mountGroup, SWT.NONE);
		removeButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		removeButton.setText(settings.msg("dialog.template.mount.remove"));
		removeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				doRemoveMount();
			}
		});

		VerifyListener addMountListener = new VerifyListener() {
			public void verifyText(final VerifyEvent event) {
				if (event.text.length() > 1) {
					addMountIfNoMounts(event.text);
				}
			}
		};
		installExe.addVerifyListener(addMountListener);

		Group associationGroup = new Group(group, SWT.NONE);
		associationGroup.setText(settings.msg("dialog.template.association"));
		associationGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		associationGroup.setLayout(new GridLayout(2, false));

		final Label dbversionLabel = new Label(associationGroup, SWT.NONE);
		dbversionLabel.setText(settings.msg("dialog.template.dosboxversion"));
		final Combo dbversion = new Combo(associationGroup, SWT.READ_ONLY);
		dbversion.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		dbversion.setVisibleItemCount(20);

		for (DosboxVersion dbv: dbversionsList) {
			dbversion.add(dbv.getTitle());
		}
		int dbversionIndex = DosboxVersion.findIndexById(dbversionsList, profile.getDbversionId());
		dbversion.select(dbversionIndex);

		dbversion.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				try {
					String[] mounts = profile.getConf().getAutoexec().getMountingpoints();

					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					PrintStream ps = new PrintStream(bos);
					Conf newDosboxVersion = new Conf(dbversionsList.get(dbversion.getSelectionIndex()), ps);
					profile.getConf().switchToDosboxVersion(newDosboxVersion);
					profile.setDbversionId(newDosboxVersion.getDbversion().getId());

					profile.getConf().unmountDosboxMounts();
					for (String m: mounts)
						profile.getConf().addMount(m);
					mountingpoints.setItems(profile.getConf().getAutoexec().getMountingpoints());

					if (bos.size() > 0) {
						GeneralPurposeDialogs.warningMessage(getParent(), bos.toString());
						bos.reset();
					}
				} catch (IOException e) {
					GeneralPurposeDialogs.warningMessage(getParent(), e);
				}
			}
		});

		return group;
	}

	private void addMountIfNoMounts(final String hostFileLocation) {
		profile.getConf().unmountDosboxMounts();
		profile.getConf().addMount("mount C \"" + FileUtils.getTmpInstallFile() + "\"");
		mountingpoints.setItems(profile.getConf().addRequiredMountForInstaller(hostFileLocation));
	}

	private Control page2() {
		final Group group = new Group(shell, SWT.NONE);
		group.setText(settings.msg("dialog.addgamewizard.step3")); // patch
		group.setLayout(new GridLayout());

		Composite patchexeComposite = new Composite(group, SWT.NONE);
		patchexeComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		patchexeComposite.setLayout(new GridLayout(3, false));
		final Label patchExeLabel = new Label(patchexeComposite, SWT.NONE);
		patchExeLabel.setText(settings.msg("dialog.addgamewizard.patcherexe"));
		patchExe = new Text(patchexeComposite, SWT.BORDER);
		patchExe.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		final BrowseButton patchexeBrowseButton = new BrowseButton(patchexeComposite, SWT.NONE);
		patchexeBrowseButton.connect(shell, patchExe, null, BrowseType.FILE, CanonicalType.INSTALLER, false, null);

		final Label parametersLabel = new Label(patchexeComposite, SWT.NONE);
		parametersLabel.setText(settings.msg("dialog.profile.mainparameters"));
		patchParameters = new Text(patchexeComposite, SWT.BORDER);
		patchParameters.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		final Label manualLabel = new Label(patchexeComposite, SWT.NONE);
		manualLabel.setText(settings.msg("dialog.addgamewizard.manualmode"));
		btnPatchManual = new Button(patchexeComposite, SWT.CHECK);
		btnPatchManual.setText(settings.msg("dialog.addgamewizard.manualpatchmodeinfo"));
		btnPatchManual.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));

		return group;
	}

	private Control page3() {
		final Group group = new Group(shell, SWT.NONE);

		group.setText(settings.msg("dialog.addgamewizard.step4"));
		group.setLayout(new GridLayout());

		Composite composite = new Composite(group, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		composite.setLayout(new GridLayout(2, false));

		final Label mainExeLabel = new Label(composite, SWT.NONE);
		mainExeLabel.setText(settings.msg("dialog.profile.mainexe"));
		main = new Combo(composite, SWT.READ_ONLY);
		main.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		final Label setupExeLabel = new Label(composite, SWT.NONE);
		setupExeLabel.setText(settings.msg("dialog.profile.setupexe"));
		setup = new Combo(composite, SWT.READ_ONLY);
		setup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		return group;
	}

	private Control page4() {
		final Group group = new Group(shell, SWT.NONE);

		group.setText(settings.msg("dialog.addgamewizard.step4"));
		group.setLayout(new GridLayout());

		Composite composite = new Composite(group, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		composite.setLayout(new GridLayout(3, false));

		final Label mainExeLabel = new Label(composite, SWT.NONE);
		mainExeLabel.setText(settings.msg("dialog.profile.mainexe"));
		mainText = new Text(composite, SWT.BORDER);
		mainText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		final BrowseButton mainBrowseButton = new BrowseButton(composite, SWT.NONE);
		mainBrowseButton.connect(shell, mainText, null, BrowseType.FILE, CanonicalType.EXE, false, null);

		final Label setupExeLabel = new Label(composite, SWT.NONE);
		setupExeLabel.setText(settings.msg("dialog.profile.setupexe"));
		setupText = new Text(composite, SWT.BORDER);
		setupText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		final BrowseButton setupBrowseButton = new BrowseButton(composite, SWT.NONE);
		setupBrowseButton.connect(shell, setupText, mainText, BrowseType.FILE, CanonicalType.EXE, false, null);

		return group;
	}

	private Control page5() {
		final Group group = new Group(shell, SWT.NONE);

		group.setText(settings.msg("dialog.addgamewizard.step5"));
		group.setLayout(new GridLayout());

		final Group installedFilesGroup = new Group(group, SWT.NONE);
		installedFilesGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		installedFilesGroup.setLayout(new GridLayout(3, false));
		installedFilesGroup.setText(settings.msg("dialog.addgamewizard.installedfiles"));

		installedFilesList = new List(installedFilesGroup, SWT.V_SCROLL | SWT.BORDER | SWT.MULTI);
		installedFilesList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));

		final Label toLabel = new Label(installedFilesGroup, SWT.NONE);
		toLabel.setText(settings.msg("dialog.migration.to"));

		dstDirectory = new Text(installedFilesGroup, SWT.BORDER);
		dstDirectory.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		dstDirectory.setText(FileUtils.getDosRoot());

		final BrowseButton fromBrowseButton = new BrowseButton(installedFilesGroup, SWT.NONE);
		fromBrowseButton.connect(shell, dstDirectory, null, BrowseType.DIR, CanonicalType.NONE, false, null);

		final Group orgImagesGroup = new Group(group, SWT.NONE);
		orgImagesGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		orgImagesGroup.setLayout(new GridLayout(3, false));
		orgImagesGroup.setText(settings.msg("dialog.addgamewizard.originalimages"));

		orgImagesList = new List(orgImagesGroup, SWT.V_SCROLL | SWT.BORDER | SWT.MULTI);
		orgImagesList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));

		final Label moveImagesLabel = new Label(orgImagesGroup, SWT.NONE);
		moveImagesLabel.setText(settings.msg("dialog.addgamewizard.moveimages"));
		moveImages = new Button(orgImagesGroup, SWT.CHECK);
		new Label(orgImagesGroup, SWT.NONE);

		final Label imagesToLabel = new Label(orgImagesGroup, SWT.NONE);
		imagesToLabel.setText(settings.msg("dialog.migration.to"));
		imagesDstDirectory = new Text(orgImagesGroup, SWT.BORDER);
		imagesDstDirectory.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		new Label(orgImagesGroup, SWT.NONE);

		moveImages.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				imagesDstDirectory.setEnabled(moveImages.getSelection());
			}
		});

		imagesDstDirectory.setEnabled(moveImages.getSelection());

		return group;
	}

	private Control page6() {
		final Group group = new Group(shell, SWT.NONE);
		group.setText(settings.msg("dialog.addgamewizard.step6"));
		group.setLayout(new GridLayout(3, false));

		final Label templateLabel = new Label(group, SWT.NONE);
		templateLabel.setText(settings.msg("dialog.profile.template"));
		template = new Combo(group, SWT.READ_ONLY);
		template.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		template.setVisibleItemCount(10);
		for (Template templ: templatesList) {
			template.add(templ.getTitle());
		}
		if (templateIndex != -1) {
			template.select(templateIndex);
		}
		templateReload = new Button(group, SWT.NONE);
		templateReload.setText(settings.msg("dialog.profile.reloadsettings"));
		templateReload.setToolTipText(settings.msg("dialog.profile.reloadsettings.tooltip"));
		templateReload.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				if (template.getSelectionIndex() != -1) {
					doReloadTemplate(template.getSelectionIndex());
				}
			}
		});

		final Label machineLabel = new Label(group, SWT.NONE);
		machineLabel.setText(settings.msg("dialog.template.machine"));
		machine = new Combo(group, SWT.READ_ONLY);
		machine.setLayoutData(new GridData());
		machine.setVisibleItemCount(20);
		machine.setToolTipText(settings.msg("dialog.template.machine.tooltip"));
		if (profile.getConf().getSettings().detectDosboxVersionGeneration() >= 3) {
			machine.setItems(settings.getSettings().getValues("profile", "machine073"));
		} else {
			machine.setItems(settings.getSettings().getValues("profile", "machine"));
		}
		new Label(group, SWT.NONE);

		final Label coreLabel = new Label(group, SWT.NONE);
		coreLabel.setText(settings.msg("dialog.template.core"));
		core = new Combo(group, SWT.READ_ONLY);
		core.setLayoutData(new GridData());
		core.setToolTipText(settings.msg("dialog.template.core.tooltip"));
		core.setItems(settings.getSettings().getValues("profile", "core"));
		new Label(group, SWT.NONE);

		final Label cyclesLabel = new Label(group, SWT.NONE);
		cyclesLabel.setText(settings.msg("dialog.template.cycles"));
		cycles = new Combo(group, SWT.NONE);
		cycles.setLayoutData(new GridData(100, SWT.DEFAULT));
		cycles.setVisibleItemCount(15);
		cycles.setToolTipText(settings.msg("dialog.template.cycles.tooltip"));
		cycles.setItems(settings.getSettings().getValues("profile", "cycles"));
		new Label(group, SWT.NONE);

		selectSettingsByConfiguration(profile.getConf());

		return group;
	}
}
