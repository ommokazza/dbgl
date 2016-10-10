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
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.apache.commons.lang3.StringUtils;
import org.dbgl.db.Database;
import org.dbgl.gui.BrowseButton.BrowseType;
import org.dbgl.gui.BrowseButton.CanonicalType;
import org.dbgl.gui.LoadSharedConfDialog.SharedConfLoading;
import org.dbgl.interfaces.Configurable;
import org.dbgl.model.DosboxVersion;
import org.dbgl.model.ExpProfile;
import org.dbgl.model.KeyValuePair;
import org.dbgl.model.NativeCommand;
import org.dbgl.model.SearchEngineImageInformation;
import org.dbgl.model.WebProfile;
import org.dbgl.model.Profile;
import org.dbgl.model.Template;
import org.dbgl.model.SearchEngineImageInformation.SearchEngineImageType;
import org.dbgl.model.conf.Conf;
import org.dbgl.model.conf.Settings;
import org.dbgl.model.conf.SharedConf;
import org.dbgl.swtdesigner.SWTImageManager;
import org.dbgl.util.FileUtils;
import org.dbgl.util.PlatformUtils;
import org.dbgl.util.StringRelatedUtils;
import org.dbgl.util.searchengine.HotudSearchEngine;
import org.dbgl.util.searchengine.MobyGamesSearchEngine;
import org.dbgl.util.searchengine.PouetSearchEngine;
import org.dbgl.util.searchengine.TheGamesDBSearchEngine;
import org.dbgl.util.searchengine.WebSearchEngine;
import swing2swt.layout.BorderLayout;


public class EditProfileDialog extends EditTemplateDialog {

	final static java.util.List<WebSearchEngine> webSearchEngines = Arrays.asList(MobyGamesSearchEngine.getInstance(), PouetSearchEngine.getInstance(), HotudSearchEngine.getInstance(),
		TheGamesDBSearchEngine.getInstance());

	public final static String DBCONFWS = "DBConfWS";
	public final static int AMOUNT_OF_LINKS = 8;
	private final static int AMOUNT_OF_CUSTOM_STRINGS = 4;

	private boolean focusOnTitle = false;

	private java.util.List<KeyValuePair> developersList, publishersList, genresList, yearsList, statusList;
	private java.util.List<Template> templatesList;
	private java.util.List<java.util.List<KeyValuePair>> customList = new java.util.ArrayList<java.util.List<KeyValuePair>>();

	private SizeControlAdapter sizeControlAdapter;
	private SearchEngineImageInformation[] imageInformation = null;
	private Button[] imgButtons;
	private AutoSelectCombo developer, publisher, genre, year, status;
	private Button favorite, templateReload, loadfix, loadhigh;
	private ToolItem engineSelector;
	private Composite webImagesSpaceHolder;
	private ScrolledComposite webImagesSpace;
	private Text[] link = new Text[AMOUNT_OF_LINKS];
	private Text[] linkTitle = new Text[AMOUNT_OF_LINKS];
	private BrowseButton[] linkBrowseButton = new BrowseButton[AMOUNT_OF_LINKS];
	private AutoSelectCombo[] customCombo = new AutoSelectCombo[AMOUNT_OF_CUSTOM_STRINGS];
	private Text[] customText = new Text[AMOUNT_OF_CUSTOM_STRINGS];
	private Scale custom9;
	private Spinner custom10;
	private Combo template, loadfix_value;
	private Text notes, main, main_params, setup, setup_params, alt1, alt1_params, alt2, alt2_params, img1, img2, img3;
	private Combo imgDriveletter;
	protected ExpProfile multiProfileCombined;
	private int templateIndex = -1;

	public EditProfileDialog(final Shell parent) {
		super(parent);
	}

	public void setProfile(final Profile prof) {
		this.result = prof;
	}

	public void setConfigurables(final java.util.List<Configurable> configurables) {
		this.multiProfileList = configurables;
		if (multiProfileList.size() == 1) {
			setProfile((ExpProfile)multiProfileList.get(0));
			multiProfileList.remove(0);
		}
	}

	public void setMultiProfileCombined(final ExpProfile multiProfileCombined) {
		this.multiProfileCombined = multiProfileCombined;
	}

	public void sendToProfile(final String file) {
		this.result = FileUtils.makeRelativeToDosroot(new File(file)).getPath();
	}

	public void focusTitle() {
		this.focusOnTitle = true;
	}

	protected boolean init() {
		try {
			dbversionsList = dbase.readDosboxVersionsList();
			developersList = dbase.readDevelopersList();
			publishersList = dbase.readPublishersList();
			genresList = dbase.readGenresList();
			yearsList = dbase.readYearsList();
			templatesList = dbase.readTemplatesList();
			statusList = dbase.readStatusList();
			for (int i = 0; i < AMOUNT_OF_CUSTOM_STRINGS; i++) {
				customList.add(dbase.readCustomList(i));
			}

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(bos);

			if (result instanceof Profile) {
				Profile profile = (Profile)result;
				dbversionIndex = DosboxVersion.findIndexById(dbversionsList, profile.getDbversionId());
				DosboxVersion dbversion = dbversionsList.get(dbversionIndex);
				Conf cc = new Conf(profile, dbversion, ps);
				if ((cc.getAutoexec().isIncomplete()) && (!GeneralPurposeDialogs.confirmMessage(getParent(),
					settings.msg("dialog.profile.confirm.profileincomplete", new Object[] {FileUtils.DOSBOX_CONF, dbversion.getCanonicalConfFile()})))) {
					return false;
				}
				java.util.List<NativeCommand> nativeCommands = dbase.readNativeCommandsList(profile.getId(), -1);
				multiProfileList.add(new ExpProfile(profile.getId(), cc, FileUtils.makeRelativeToDosroot(cc.getAutoexec().getCanonicalMainDir()), nativeCommands, profile));
			} else if (isMultiEdit()) {
				dbversionIndex = DosboxVersion.findIndexById(dbversionsList, multiProfileCombined.getDbversionId());
			} else {
				dbversionIndex = DosboxVersion.indexOfDefault(dbversionsList);
				DosboxVersion dbversion = dbversionsList.get(dbversionIndex);
				templateIndex = Template.indexOfDefault(templatesList);
				Conf cc = null;
				java.util.List<NativeCommand> nativeCommands = null;
				if ((result instanceof String) && FileUtils.isConfFile((String)result)) {
					cc = new Conf(FileUtils.canonicalToDosroot((String)result), (Template)null, dbversion, ps);
					nativeCommands = dbase.readNativeCommandsList(-1, -1);
				} else if (templateIndex != -1) {
					cc = new Conf((File)null, templatesList.get(templateIndex), dbversion, ps);
					nativeCommands = dbase.readNativeCommandsList(-1, templatesList.get(templateIndex).getId());
				} else {
					cc = new Conf((File)null, (Template)null, dbversion, ps);
					nativeCommands = dbase.readNativeCommandsList(-1, -1);
				}
				multiProfileList.add(new ExpProfile(cc, nativeCommands));
			}
			if (bos.size() > 0) {
				GeneralPurposeDialogs.warningMessage(getParent(), bos.toString());
				bos.reset();
			}
			return true;

		} catch (Exception e) {
			GeneralPurposeDialogs.warningMessage(getParent(), e);
			return false;
		}
	}

	protected void createContents() {
		shell = new Shell(getParent(), SWT.TITLE | SWT.CLOSE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setLayout(new BorderLayout(0, 0));
		sizeControlAdapter = new SizeControlAdapter(shell, "profiledialog");
		shell.addControlListener(sizeControlAdapter);

		if (result instanceof Profile) {
			// meaning we are essentially editing an existing profile
			shell.setText(settings.msg("dialog.profile.title.edit", new Object[] {((Profile)result).getTitle(), ((Profile)result).getId()}));
		} else if (isMultiEdit()) {
			// meaning we are essentially editing multiple existing profiles
			shell.setText(settings.msg("dialog.multiprofile.title.edit", new Object[] {multiProfileList.size()}));
		} else if (result instanceof String) {
			// meaning we are adding a new Profile with 'Send to...'
			shell.setText(settings.msg("dialog.profile.title.send", new Object[] {(String)result}));
		} else {
			shell.setText(settings.msg("dialog.profile.title.add"));
		}

		final TabFolder tabFolder = new TabFolder(shell, SWT.NONE);
		createInfoTab(tabFolder);
		createCustomTab(tabFolder);
		createGeneralTab(tabFolder);
		createDisplayTab(tabFolder);
		createMachineTab(tabFolder);
		createAudioTab(tabFolder);
		createIOTab(tabFolder);
		createCustomCommandsTab(tabFolder);
		createMountingTab(tabFolder);

		final Composite composite_7 = new Composite(shell, SWT.NONE);
		composite_7.setLayout(new GridLayout(3, false));
		composite_7.setLayoutData(BorderLayout.SOUTH);

		okButton = new Button(composite_7, SWT.NONE);
		shell.setDefaultButton(okButton);

		okButton.setText(settings.msg("button.ok"));

		cancelButton = new Button(composite_7, SWT.NONE);
		cancelButton.setText(settings.msg("button.cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				if (cancelButton.getText().equals(settings.msg("button.cancel"))) {
					result = null;
				}
				shell.close();
			}
		});

		final GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.BEGINNING;
		gridData.widthHint = GeneralPurposeGUI.getWidth(okButton, cancelButton);
		okButton.setLayoutData(gridData);
		final GridData gridData_1 = new GridData();
		gridData_1.horizontalAlignment = SWT.BEGINNING;
		gridData_1.widthHint = gridData.widthHint;
		gridData_1.grabExcessHorizontalSpace = true;
		cancelButton.setLayoutData(gridData_1);

		final Button shareButton = new Button(composite_7, SWT.NONE);
		final GridData gridData_2 = new GridData();
		gridData_2.horizontalAlignment = SWT.RIGHT;
		shareButton.setLayoutData(gridData_2);
		shareButton.setImage(SWTImageManager.getResourceImage(shell.getDisplay(), SWTImageManager.IMG_SHARE));
		shareButton.setText(settings.msg("button.shareconf"));
		shareButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				updateAllConfigurationsBySettings();
				new ShareConfDialog(shell, title.getText(), year.getText(), multiProfileList.get(0).getConf().toShareString(), multiProfileList.get(0).getConf().toFullConfString(false),
						multiProfileList.get(0).getConf().getDbversion()).open();
			}
		});
		shareButton.setEnabled(!isMultiEdit());

		okButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				if (!isValid()) {
					return;
				}

				if (isMultiEdit()) {
					updateAllConfigurationsBySettings();

					EditMultiProfileDialog empDialog = new EditMultiProfileDialog(shell);
					DosboxVersion newDbversion = null;
					if (dbversion.getSelectionIndex() != -1) {
						newDbversion = dbversionsList.get(dbversion.getSelectionIndex());
					}
					Profile newProfile = new Profile(-1, fetch(title), fetch(developer), fetch(publisher), fetch(genre), fetch(year), fetch(status), fetch(notes), fetch(favorite),
							new String[] {fetch(setup), fetch(alt1), fetch(alt2)}, new String[] {fetch(setup_params), fetch(alt1_params), fetch(alt2_params)}, null, null,
							newDbversion != null ? newDbversion.getId(): -1,
							new String[] {fetch(link[0]), fetch(link[1]), fetch(link[2]), fetch(link[3]), fetch(link[4]), fetch(link[5]), fetch(link[6]), fetch(link[7])},
							new String[] {fetch(linkTitle[0]), fetch(linkTitle[1]), fetch(linkTitle[2]), fetch(linkTitle[3]), fetch(linkTitle[4]), fetch(linkTitle[5]), fetch(linkTitle[6]),
									fetch(linkTitle[7])},
							new String[] {fetch(customCombo[0]), fetch(customCombo[1]), fetch(customCombo[2]), fetch(customCombo[3]), fetch(customText[0]), fetch(customText[1]), fetch(customText[2]),
									fetch(customText[3])},
							new int[] {fetch(custom9), fetch(custom10)}, null, null, null, null, 0, 0);
					for (int i = 0; i < multiProfileList.size(); i++) {
						ExpProfile ep = (ExpProfile)multiProfileList.get(i);
						ep = new ExpProfile(ep, newProfile);
						multiProfileList.set(i, ep);
					}
					empDialog.setData(multiProfileList, (Boolean)engineSelector.getData("selected") ? getSelectedSearchEngine(): null);
					if (empDialog.open() != null) {
						result = multiProfileList;
						shell.close();
					}

				} else {

					try {
						if (dosExpandItem.getExpanded()) {
							img1.setText(StringUtils.EMPTY);
							img2.setText(StringUtils.EMPTY);
							img3.setText(StringUtils.EMPTY);
						} else {
							main.setText(StringUtils.EMPTY);
							main_params.setText(StringUtils.EMPTY);
						}

						dbase.startTransaction();
						Conf compConf = multiProfileList.get(0).getConf();
						Profile newProfile = dbase.addOrEditProfile(title.getText(), developer.getText(), publisher.getText(), genre.getText(), year.getText(), status.getText(), notes.getText(),
							favorite.getSelection(), new String[] {setup.getText(), alt1.getText(), alt2.getText()},
							new String[] {setup_params.getText(), alt1_params.getText(), alt2_params.getText()}, compConf.getDbversion().getId(),
							new String[] {link[0].getText(), link[1].getText(), link[2].getText(), link[3].getText(), link[4].getText(), link[5].getText(), link[6].getText(), link[7].getText()},
							new String[] {linkTitle[0].getText(), linkTitle[1].getText(), linkTitle[2].getText(), linkTitle[3].getText(), linkTitle[4].getText(), linkTitle[5].getText(),
									linkTitle[6].getText(), linkTitle[7].getText()},
							new String[] {customCombo[0].getText(), customCombo[1].getText(), customCombo[2].getText(), customCombo[3].getText(), customText[0].getText(), customText[1].getText(),
									customText[2].getText(), customText[3].getText()},
							new int[] {custom9.getSelection(), custom10.getSelection()}, result instanceof Profile ? ((Profile)result).getId(): -1);
						dbase.saveNativeCommands(multiProfileList.get(0).getNativeCommandsList(), newProfile.getId(), -1);
						updateAllConfigurationsBySettings();

						String confString;
						if (result instanceof Profile) {
							confString = ((Profile)result).getConfPathAndFile();
						} else {
							confString = FileUtils.constructUniqueConfigFileString(newProfile.getId(), title.getText(),
								compConf.getAutoexec().isIncomplete() ? null: compConf.getAutoexec().getCanonicalMainDir());
						}

						String capturesString;
						if (result instanceof Profile) {
							capturesString = ((Profile)result).getCaptures();
						} else {
							capturesString = FileUtils.constructCapturesDir(newProfile.getId());
							FileUtils.createDir(FileUtils.canonicalToData(capturesString));
						}

						if (!(result instanceof Profile)) {
							newProfile = dbase.updateProfileConf(confString, capturesString, newProfile.getId());
						}

						result = newProfile;

						compConf.injectOrUpdateProfile((Profile)result);
						compConf.save();

						if (imageInformation != null)
							saveWebImages(FileUtils.canonicalToData(capturesString));

						dbase.commitTransaction();

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
					shell.close();
				}
			}
		});

		// init values
		for (DosboxVersion dbv: dbversionsList) {
			dbversion.add(dbv.getTitle());
		}
		for (Template templ: templatesList) {
			template.add(templ.getTitle());
		}
		dbversion.select(dbversionIndex);
		templateReload.setEnabled(dbversionIndex != -1);

		if (result instanceof Profile) {
			// meaning we are essentially editing an existing profile
			// so we need to set previous values
			setProfileMetaData((Profile)result);

			if (focusOnTitle) {
				title.selectAll();
				title.setFocus();
			}

		} else if (isMultiEdit()) {

			// meaning we are essentially editing multiple existing profiles
			// so we need to set previous values
			setProfileMetaData(multiProfileCombined);

		} else {
			// set default values for new profile
			title.setFocus();
			if (templateIndex != -1) {
				template.select(templateIndex);
			}
		}

		if (isMultiEdit()) {
			enableSettingsByConfiguration(multiProfileCombined.getConf().getDosboxSettings());
			selectSettingsByConfiguration(multiProfileCombined.getConf());
		} else {
			enableSettingsByConfiguration(multiProfileList.get(0).getConf().getDosboxSettings());
			selectSettingsByConfiguration(multiProfileList.get(0).getConf());
		}

		startListeners();

		if (result instanceof String) {
			// send to profile
			if (FileUtils.isExecutable((String)result)) {
				main.setText((String)result);
			} else if (FileUtils.isBooterImage((String)result)) {
				img1.setText((String)result);
			}
		}
	}

	protected void createMachineTab(final TabFolder tabFolder) {
		super.createMachineTab(tabFolder);

		final Label loadfixLabel = new Label(memoryGroup, SWT.NONE);
		loadfixLabel.setText(settings.msg("dialog.profile.loadfix"));
		loadfix = new Button(memoryGroup, SWT.CHECK);
		loadfix.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				loadfix_value.setEnabled(loadfix.getSelection());
			}
		});
		loadfix_value = new Combo(memoryGroup, SWT.NONE);
		loadfix_value.setItems(settings.getSettings().getValues("profile", "loadfix_value"));
		loadfix_value.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		final Label kbLabel = new Label(memoryGroup, SWT.NONE);
		kbLabel.setText(settings.msg("dialog.profile.kb"));

		final Label loadhighLabel = new Label(memoryGroup, SWT.NONE);
		loadhighLabel.setText(settings.msg("dialog.profile.loadhigh"));
		loadhigh = new Button(memoryGroup, SWT.CHECK);
		new Label(memoryGroup, SWT.NONE);
		new Label(memoryGroup, SWT.NONE);
	}

	protected void createInfoTab(final TabFolder tabFolder) {
		infoTabItem = new TabItem(tabFolder, SWT.NONE);
		infoTabItem.setText(settings.msg("dialog.profile.tab.info"));

		final Composite composite = new Composite(tabFolder, SWT.NONE);
		composite.setLayout(new GridLayout(8, false));
		infoTabItem.setControl(composite);

		final Label titleLabel = new Label(composite, SWT.NONE);
		titleLabel.setText(settings.msg("dialog.profile.title"));
		title = new Text(composite, SWT.BORDER);
		title.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 6, 1));

		WebSearchEngine defaultEngine = getSearchEngineBySimpleName(settings.getSettings().getValue("gui", "searchengine"));
		if (defaultEngine == null)
			defaultEngine = webSearchEngines.get(0);

		final ToolBar toolBar = new ToolBar(composite, SWT.FLAT);
		engineSelector = new ToolItem(toolBar, SWT.DROP_DOWN);
		engineSelector.setImage(SWTImageManager.getResourceImage(shell.getDisplay(), defaultEngine.getIcon()));
		engineSelector.setToolTipText(settings.msg("dialog.profile.consultsearchengine", new String[] {defaultEngine.getName()}));
		engineSelector.setData("engine", defaultEngine.getName());

		if (isMultiEdit()) {
			engineSelector.setImage(SWTImageManager.createDisabledImage(SWTImageManager.getResourceImage(shell.getDisplay(), defaultEngine.getIcon())));
			engineSelector.setData("selected", false);
		}

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
					if (isMultiEdit())
						engineSelector.setData("selected", true);
					settings.getSettings().setValue("gui", "searchengine", engine.getSimpleName());
				}
			});
		}

		engineSelector.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				if (event.detail == SWT.ARROW) {
					if (engineSelector.getData("profile") == null) {
						Rectangle rect = engineSelector.getBounds();
						Point pt = new Point(rect.x, rect.y + rect.height);
						pt = toolBar.toDisplay(pt);
						menu.setLocation(pt.x, pt.y);
						menu.setVisible(true);
					}
				} else {
					if (isMultiEdit()) {
						WebSearchEngine engine = getSelectedSearchEngine();
						if ((Boolean)engineSelector.getData("selected")) {
							engineSelector.setImage(SWTImageManager.createDisabledImage(SWTImageManager.getResourceImage(shell.getDisplay(), engine.getIcon())));
							engineSelector.setData("selected", false);
						} else {
							engineSelector.setImage(SWTImageManager.getResourceImage(shell.getDisplay(), engine.getIcon()));
							engineSelector.setData("selected", true);
						}
					} else {
						final int WEB_IMAGE_WIDTH = settings.getSettings().getIntValue("mobygames", "image_width");
						final int WEB_IMAGE_HEIGHT = settings.getSettings().getIntValue("mobygames", "image_height");
						final int WEB_IMAGE_COLUMNS = settings.getSettings().getIntValue("mobygames", "image_columns");
						final int DIALOG_RESIZE_WIDTH = ((WEB_IMAGE_WIDTH + 10) * WEB_IMAGE_COLUMNS) + (3 * (WEB_IMAGE_COLUMNS - 1)) + 19;

						WebProfile orgProf = (WebProfile)engineSelector.getData("profile");
						if (orgProf == null) {
							String currTitle = title.getText();
							if (currTitle.length() >= 1) {
								WebSearchEngine engine = getSelectedSearchEngine();
								try {
									WebProfile thisGame = null;
									java.util.List<WebProfile> webGamesList = engine.getEntries(currTitle, settings.getSettings().getValues(engine.getSimpleName(), "platform_filter"));
									if (webGamesList.size() == 0) {
										GeneralPurposeDialogs.infoMessage(shell, settings.msg("general.notice.searchenginenoresults", new String[] {engine.getName(), currTitle}));
									} else if (webGamesList.size() == 1) {
										thisGame = webGamesList.get(0);
									} else {
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

										WebProfile currentProf = new WebProfile();
										currentProf.setTitle(title.getText());
										currentProf.setDeveloperName(developer.getText());
										currentProf.setPublisherName(publisher.getText());
										currentProf.setYear(year.getText());
										currentProf.setGenre(genre.getText());
										currentProf.setUrl(link[0].getText());
										currentProf.setPlatform(linkTitle[0].getText());
										currentProf.setNotes(notes.getText());
										currentProf.setRank(custom9.getSelection());
										engineSelector.setData("profile", currentProf);
										engineSelector.setImage(SWTImageManager.getResourceImage(shell.getDisplay(), SWTImageManager.IMG_UNDO));
										engineSelector.setToolTipText(settings.msg("dialog.profile.undosearchengine"));

										if (settings.getSettings().getBooleanValue(engine.getSimpleName(), "set_title"))
											title.setText(profExt.getTitle());
										if (settings.getSettings().getBooleanValue(engine.getSimpleName(), "set_developer"))
											developer.setText(profExt.getDeveloperName());
										if (settings.getSettings().getBooleanValue(engine.getSimpleName(), "set_publisher"))
											publisher.setText(profExt.getPublisherName());
										if (settings.getSettings().getBooleanValue(engine.getSimpleName(), "set_year"))
											year.setText(profExt.getYear());
										if (settings.getSettings().getBooleanValue(engine.getSimpleName(), "set_genre"))
											genre.setText(profExt.getGenre());
										if (settings.getSettings().getBooleanValue(engine.getSimpleName(), "set_link")) {
											link[0].setText(profExt.getUrl());
											linkTitle[0].setText(settings.msg("dialog.profile.searchengine.link.maininfo", new String[] {engine.getName()}));
										}
										if (settings.getSettings().getBooleanValue(engine.getSimpleName(), "set_description")) {
											String n = notes.getText();
											String p = profExt.getNotes().replaceAll("\n", notes.getLineDelimiter());
											if (!n.endsWith(p)) {
												if (n.length() > 0) {
													notes.append(notes.getLineDelimiter() + notes.getLineDelimiter());
												}
												notes.append(p);
											}
										}
										if (settings.getSettings().getBooleanValue(engine.getSimpleName(), "set_rank"))
											custom9.setSelection(profExt.getRank());

										int ca = settings.getSettings().getBooleanValue(engine.getSimpleName(), "choose_coverart") ? Integer.MAX_VALUE: 0;
										int ss = settings.getSettings().getBooleanValue(engine.getSimpleName(), "choose_screenshot") ? Integer.MAX_VALUE: 0;
										if ((ca > 0) || (ss > 0)) {
											boolean forceAllRegionsCoverArt = settings.getSettings().getBooleanValue(engine.getSimpleName(), "force_all_regions_coverart");
											imageInformation = engine.getEntryImages(profExt, ca, ss, forceAllRegionsCoverArt);

											webImagesSpaceHolder = new Composite(shell, SWT.NONE);
											webImagesSpaceHolder.setLayoutData(BorderLayout.EAST);

											final GridLayout holderLayout = new GridLayout();
											holderLayout.numColumns = 2;
											holderLayout.marginHeight = 0;
											holderLayout.marginWidth = 0;
											holderLayout.horizontalSpacing = 0;
											holderLayout.verticalSpacing = 0;
											webImagesSpaceHolder.setLayout(holderLayout);

											if (imageInformation.length > 0) {
												final Button allButton = new Button(webImagesSpaceHolder, SWT.NONE);
												allButton.setLayoutData(new GridData(50, SWT.DEFAULT));
												allButton.setText(settings.msg("button.all"));
												allButton.addSelectionListener(new SelectionAdapter() {
													public void widgetSelected(final SelectionEvent e) {
														for (Button but: imgButtons)
															but.setSelection(true);
													}
												});

												final Button noneButton = new Button(webImagesSpaceHolder, SWT.NONE);
												noneButton.setLayoutData(new GridData(50, SWT.DEFAULT));
												noneButton.setText(settings.msg("button.none"));
												noneButton.addSelectionListener(new SelectionAdapter() {
													public void widgetSelected(final SelectionEvent e) {
														for (Button but: imgButtons)
															but.setSelection(false);
													}
												});
											}

											webImagesSpace = new ScrolledComposite(webImagesSpaceHolder, SWT.V_SCROLL);
											webImagesSpace.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
											webImagesSpace.setLayout(new GridLayout());
											webImagesSpace.getVerticalBar().setIncrement(WEB_IMAGE_HEIGHT / WEB_IMAGE_COLUMNS);
											webImagesSpace.getVerticalBar().setPageIncrement((WEB_IMAGE_HEIGHT / WEB_IMAGE_COLUMNS) * 8);

											final Composite webImagesComposite = new Composite(webImagesSpace, SWT.NONE);
											final GridLayout gridLayoutImagesGroup = new GridLayout();
											gridLayoutImagesGroup.numColumns = WEB_IMAGE_COLUMNS;
											gridLayoutImagesGroup.marginHeight = 0;
											gridLayoutImagesGroup.marginWidth = 0;
											gridLayoutImagesGroup.horizontalSpacing = 1;
											gridLayoutImagesGroup.verticalSpacing = 1;
											gridLayoutImagesGroup.makeColumnsEqualWidth = true;
											webImagesComposite.setLayout(gridLayoutImagesGroup);

											webImagesSpace.setContent(webImagesComposite);

											if (imageInformation.length > 0) {
												imgButtons = new Button[imageInformation.length];

												for (int i = 0; i < imageInformation.length; i++) {
													imgButtons[i] = new Button(webImagesComposite, SWT.TOGGLE | SWT.FLAT);
													imgButtons[i].setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, true));
													imgButtons[i].setToolTipText(imageInformation[i].description);
													imgButtons[i].setImage(SWTImageManager.getEmptyImage(shell.getDisplay(), WEB_IMAGE_WIDTH, WEB_IMAGE_HEIGHT));
													imgButtons[i].addDisposeListener(new DisposeListener() {
														public void widgetDisposed(DisposeEvent e) {
															((Button)e.getSource()).getImage().dispose();
														}
													});

													final int j = i;
													Thread thread = new Thread() {
														public void run() {
															try {
																final ImageData imgData = profExt.getWebImage(j);
																if (!shell.isDisposed() && !imgButtons[j].isDisposed()) {
																	final Image img = SWTImageManager.getWidthLimitedImage(shell.getDisplay(), WEB_IMAGE_WIDTH, imgData);
																	if (!shell.isDisposed() && !imgButtons[j].isDisposed()) {
																		shell.getDisplay().syncExec(new Runnable() {
																			public void run() {
																				imgButtons[j].getImage().dispose();
																				imgButtons[j].setImage(img);
																				webImagesComposite.setSize(webImagesComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
																				webImagesComposite.layout();
																			}
																		});
																	}
																}
															} catch (IOException e) {}
														}
													};
													thread.start();
												}
											} else {
												Label noneFoundLabel = new Label(webImagesComposite, SWT.WRAP | SWT.CENTER);
												noneFoundLabel.setText(settings.msg("dialog.profile.notice.noimagesfound", new String[] {engine.getName()}));
												GridData gd = new GridData(SWT.CENTER, SWT.FILL, true, true, WEB_IMAGE_COLUMNS, 1);
												gd.widthHint = (WEB_IMAGE_WIDTH + 10) * WEB_IMAGE_COLUMNS + (3 * (WEB_IMAGE_COLUMNS - 1)) + 2;
												gd.verticalIndent = WEB_IMAGE_HEIGHT / 2;
												noneFoundLabel.setLayoutData(gd);
											}

											sizeControlAdapter.setEnabled(false);
											webImagesComposite.pack();
											shell.setSize(shell.getSize().x + DIALOG_RESIZE_WIDTH, shell.getSize().y);
											shell.layout();
										}
									}
								} catch (Exception e) {
									GeneralPurposeDialogs.warningMessage(shell,
										settings.msg("general.error.retrieveinfosearchengine", new String[] {engine.getName(), currTitle, StringRelatedUtils.toString(e)}), e);
								}
							}
						} else {
							title.setText(orgProf.getTitle());
							developer.setText(orgProf.getDeveloperName());
							publisher.setText(orgProf.getPublisherName());
							year.setText(orgProf.getYear());
							genre.setText(orgProf.getGenre());
							link[0].setText(orgProf.getUrl());
							linkTitle[0].setText(orgProf.getPlatform());
							notes.setText(orgProf.getNotes());
							custom9.setSelection(orgProf.getRank());

							engineSelector.setData("profile", null);
							engineSelector.setImage(SWTImageManager.getResourceImage(shell.getDisplay(), getSelectedSearchEngine().getIcon()));
							engineSelector.setToolTipText(settings.msg("dialog.profile.consultsearchengine", new String[] {engineSelector.getData("engine").toString()}));

							if (webImagesSpaceHolder != null) {
								webImagesSpaceHolder.dispose();
								webImagesSpaceHolder = null;
								shell.setSize(shell.getSize().x - DIALOG_RESIZE_WIDTH, shell.getSize().y);
								shell.layout();
								sizeControlAdapter.setEnabled(true);
								imageInformation = null;
							}
						}
					}
				}
			}
		});

		final ToolItem loadSharedConfButton = new ToolItem(toolBar, SWT.PUSH);
		final GridData gridData_3 = new GridData();
		gridData_3.horizontalAlignment = SWT.FILL;
		loadSharedConfButton.setImage(SWTImageManager.getResourceImage(shell.getDisplay(), SWTImageManager.IMG_SHARE));
		loadSharedConfButton.setToolTipText(settings.msg("button.consultconfsearchengine", new String[] {DBCONFWS}));
		loadSharedConfButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				String currTitle = title.getText();
				if (currTitle.length() >= 1) {
					try {
						Client client = ClientBuilder.newClient();
						GenericType<java.util.List<SharedConf>> confType = new GenericType<java.util.List<SharedConf>>() {};
						java.util.List<SharedConf> confs = client.target(settings.getSettings().getValue("confsharing", "endpoint")).path("/configurations/bytitle/{i}").resolveTemplate("i",
							currTitle).request().accept(MediaType.APPLICATION_XML).get(confType);
						client.close();

						if (confs.size() == 0) {
							GeneralPurposeDialogs.infoMessage(shell, settings.msg("general.notice.searchenginenoresults", new String[] {DBCONFWS, currTitle}));
							return;
						} else {
							SharedConfLoading result = (SharedConfLoading)new LoadSharedConfDialog(shell, currTitle, confs).open();
							if (result != null) {
								updateAllConfigurationsBySettings();
								multiProfileList.get(0).getConf().loadSharedConf(result.conf.getIncrConf(), result.reloadDosboxDefaults);
								enableSettingsByConfiguration(multiProfileList.get(0).getConf().getDosboxSettings());
								selectSettingsByConfiguration(multiProfileList.get(0).getConf());
							}
						}
					} catch (Exception e) {
						GeneralPurposeDialogs.warningMessage(shell, settings.msg("general.error.retrieveinfosearchengine", new String[] {DBCONFWS, currTitle, StringRelatedUtils.toString(e)}), e);
					}
				}
			}
		});
		loadSharedConfButton.setEnabled(!isMultiEdit());

		final int minimumComboWidth = settings.getSettings().getIntValue("gui", "profiledialog_width") / 3;

		final Label developerLabel = new Label(composite, SWT.NONE);
		developerLabel.setText(settings.msg("dialog.profile.developer"));
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		gd.minimumWidth = minimumComboWidth;
		developer = new AutoSelectCombo(composite, SWT.NONE, developersList, gd);

		final Label publisherLabel = new Label(composite, SWT.NONE);
		publisherLabel.setLayoutData(new GridData());
		publisherLabel.setText(settings.msg("dialog.profile.publisher"));
		GridData gd2 = new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1);
		gd2.minimumWidth = minimumComboWidth;
		publisher = new AutoSelectCombo(composite, SWT.NONE, publishersList, gd2);

		final Label genreLabel = new Label(composite, SWT.NONE);
		genreLabel.setText(settings.msg("dialog.profile.genre"));
		genre = new AutoSelectCombo(composite, SWT.NONE, genresList, new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		final Label yearLabel = new Label(composite, SWT.NONE);
		yearLabel.setLayoutData(new GridData());
		yearLabel.setText(settings.msg("dialog.profile.year"));
		year = new AutoSelectCombo(composite, SWT.NONE, yearsList, new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));

		for (int i = 0; i < AMOUNT_OF_LINKS / 2; i++) {
			final Label linkLabel = new Label(composite, SWT.NONE);
			linkLabel.setText(settings.msg("dialog.profile.link", new Object[] {(i + 1)}));
			link[i] = new Text(composite, SWT.BORDER);
			link[i].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			linkBrowseButton[i] = new BrowseButton(composite, SWT.NONE);

			final Label linkTitleLabel = new Label(composite, SWT.NONE);
			linkTitleLabel.setText(settings.msg("dialog.profile.linktitle"));
			linkTitle[i] = new Text(composite, SWT.BORDER);
			linkTitle[i].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		}

		final Label statusLabel = new Label(composite, SWT.NONE);
		statusLabel.setText(settings.msg("dialog.profile.status"));
		status = new AutoSelectCombo(composite, SWT.NONE, statusList, new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		final Label favoriteLabel = new Label(composite, SWT.NONE);
		favoriteLabel.setText(settings.msg("dialog.profile.favorite"));
		favorite = new Button(composite, SWT.CHECK);
		Label label = new Label(composite, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);

		final Label notesLabel = new Label(composite, SWT.NONE);
		notesLabel.setText(settings.msg("dialog.profile.notes"));
		notes = new Text(composite, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.WRAP);
		notes.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 7, 1));
		notes.setFont(GeneralPurposeGUI.stringToFont(shell.getDisplay(), notes.getFont(), settings.getSettings().getValues("gui", "notesfont")));
	}

	protected void createCustomTab(final TabFolder tabFolder) {
		final TabItem customTabItem = new TabItem(tabFolder, SWT.NONE);
		customTabItem.setText(settings.msg("dialog.profile.tab.custominfo"));

		final Composite composite_8 = new Composite(tabFolder, SWT.NONE);
		composite_8.setLayout(new GridLayout(5, false));
		customTabItem.setControl(composite_8);

		for (int i = 0; i < AMOUNT_OF_CUSTOM_STRINGS; i++) {
			final Label customLabel = new Label(composite_8, SWT.NONE);
			customLabel.setText(settings.getSettings().getValue("gui", "custom" + (i + 1)));
			customCombo[i] = new AutoSelectCombo(composite_8, SWT.NONE, customList.get(i), new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		}

		for (int i = 0; i < AMOUNT_OF_CUSTOM_STRINGS; i++) {
			final Label customLabel = new Label(composite_8, SWT.NONE);
			customLabel.setText(settings.getSettings().getValue("gui", "custom" + (i + 1 + AMOUNT_OF_CUSTOM_STRINGS)));
			customText[i] = new Text(composite_8, SWT.BORDER);
			customText[i].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		}

		final Label custom9Label = new Label(composite_8, SWT.NONE);
		custom9Label.setText(settings.getSettings().getValue("gui", "custom9"));
		custom9 = new Scale(composite_8, SWT.NONE);
		custom9.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		final Label custom10Label = new Label(composite_8, SWT.NONE);
		custom10Label.setText(settings.getSettings().getValue("gui", "custom10"));
		custom10 = new Spinner(composite_8, SWT.BORDER);
		custom10.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		custom10.setMinimum(Integer.MIN_VALUE);
		custom10.setMaximum(Integer.MAX_VALUE);

		for (int i = AMOUNT_OF_LINKS / 2; i < AMOUNT_OF_LINKS; i++) {
			final Label linkLabel = new Label(composite_8, SWT.NONE);
			linkLabel.setText(settings.msg("dialog.profile.link", new Object[] {(i + 1)}));
			link[i] = new Text(composite_8, SWT.BORDER);
			link[i].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			linkBrowseButton[i] = new BrowseButton(composite_8, SWT.NONE);

			final Label linkTitleLabel = new Label(composite_8, SWT.NONE);
			linkTitleLabel.setText(settings.msg("dialog.profile.linktitle"));
			linkTitle[i] = new Text(composite_8, SWT.BORDER);
			linkTitle[i].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}
	}

	protected void createGeneralTab(final TabFolder tabFolder) {
		super.createGeneralTab(tabFolder);

		final Label templateLabel = new Label(associationGroup, SWT.NONE);
		templateLabel.setText(settings.msg("dialog.profile.template"));
		template = new Combo(associationGroup, SWT.READ_ONLY);
		template.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		template.setVisibleItemCount(10);
		templateReload = new Button(associationGroup, SWT.NONE);
		templateReload.setText(settings.msg("dialog.profile.reloadsettings"));
		templateReload.setToolTipText(settings.msg("dialog.profile.reloadsettings.tooltip"));
		templateReload.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				if (template.getSelectionIndex() != -1) {
					if (setButton.isEnabled()) {
						GeneralPurposeDialogs.initErrorDialog();
						GeneralPurposeDialogs.addError(settings.msg("dialog.template.required.dosboxassociation"), setButton, generalTabItem);
						GeneralPurposeDialogs.displayErrorDialog(shell);
						return;
					}
					doPerformDosboxConfAction(DosboxConfAction.RELOAD_TEMPLATE);
				}
			}
		});

		config_file.setText(result instanceof Profile ? ((Profile)result).getConfPathAndFile()
				: SettingsDialog.confLocations[settings.getSettings().getIntValue("profiledefaults", "confpath")] + ", "
						+ SettingsDialog.confFilenames[settings.getSettings().getIntValue("profiledefaults", "conffile")]);
	}

	protected void createBooterComposite(Composite booterComposite) {
		booterComposite.setLayout(new GridLayout(4, false));
		final Label image1Label = new Label(booterComposite, SWT.NONE);
		image1Label.setText(settings.msg("dialog.profile.booterimage1"));
		img1 = new Text(booterComposite, SWT.BORDER);
		img1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		final BrowseButton img1BrowseButton = new BrowseButton(booterComposite, SWT.NONE);
		img1BrowseButton.connect(shell, img1, main, BrowseType.FILE, CanonicalType.BOOTER, false, null);
		final GrabButton grab1 = new GrabButton(booterComposite, SWT.NONE);
		grab1.connect(img1, mountingpoints, true);

		final Label image2Label = new Label(booterComposite, SWT.NONE);
		image2Label.setText(settings.msg("dialog.profile.booterimage2"));
		img2 = new Text(booterComposite, SWT.BORDER);
		img2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		final BrowseButton img2BrowseButton = new BrowseButton(booterComposite, SWT.NONE);
		img2BrowseButton.connect(shell, img2, img1, BrowseType.FILE, CanonicalType.BOOTER, false, null);
		final GrabButton grab2 = new GrabButton(booterComposite, SWT.NONE);
		grab2.connect(img2, mountingpoints, true);

		final Label image3Label = new Label(booterComposite, SWT.NONE);
		image3Label.setText(settings.msg("dialog.profile.booterimage3"));
		img3 = new Text(booterComposite, SWT.BORDER);
		img3.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		final BrowseButton img3BrowseButton = new BrowseButton(booterComposite, SWT.NONE);
		img3BrowseButton.connect(shell, img3, img1, BrowseType.FILE, CanonicalType.BOOTER, false, null);
		final GrabButton grab3 = new GrabButton(booterComposite, SWT.NONE);
		grab3.connect(img3, mountingpoints, true);

		final Label imageDriveletterLabel = new Label(booterComposite, SWT.NONE);
		imageDriveletterLabel.setText(settings.msg("dialog.profile.booterdriveletter"));
		imgDriveletter = new Combo(booterComposite, SWT.READ_ONLY);
		imgDriveletter.setItems(new String[] {"", "A", "C", "D"});
		new Label(booterComposite, SWT.NONE);
		new Label(booterComposite, SWT.NONE);
	}

	protected void createDosComposite(Composite dosComposite) {
		dosComposite.setLayout(new GridLayout(6, false));
		final Label mainExeLabel = new Label(dosComposite, SWT.NONE);
		mainExeLabel.setText(settings.msg("dialog.profile.mainexe"));
		main = new Text(dosComposite, SWT.BORDER);
		main.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		for (int i = 0; i < AMOUNT_OF_LINKS; i++) {
			linkBrowseButton[i].connect(shell, link[i], main, BrowseType.FILE, CanonicalType.DOC, false, null);
		}
		final BrowseButton mainBrowseButton = new BrowseButton(dosComposite, SWT.NONE);
		mainBrowseButton.connect(shell, main, null, BrowseType.FILE, CanonicalType.EXE, false, pixelshader);
		final GrabButton grab1 = new GrabButton(dosComposite, SWT.NONE);
		grab1.connect(main, mountingpoints, false);

		new Label(dosComposite, SWT.NONE);
		final Label parametersLabel = new Label(dosComposite, SWT.NONE);
		parametersLabel.setText(settings.msg("dialog.profile.mainparameters"));
		main_params = new Text(dosComposite, SWT.BORDER);
		main_params.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		new Label(dosComposite, SWT.NONE);
		new Label(dosComposite, SWT.NONE);

		final Label setupLabel = new Label(dosComposite, SWT.NONE);
		setupLabel.setText(settings.msg("dialog.profile.setupexe"));
		setup = new Text(dosComposite, SWT.BORDER);
		setup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		final BrowseButton setupBrowseButton = new BrowseButton(dosComposite, SWT.NONE);
		setupBrowseButton.connect(shell, setup, main, BrowseType.FILE, CanonicalType.EXE, false, pixelshader);
		final GrabButton grab2 = new GrabButton(dosComposite, SWT.NONE);
		grab2.connect(setup, mountingpoints, false);

		new Label(dosComposite, SWT.NONE);
		final Label parametersLabel_1 = new Label(dosComposite, SWT.NONE);
		parametersLabel_1.setText(settings.msg("dialog.profile.setupparameters"));
		setup_params = new Text(dosComposite, SWT.BORDER);
		setup_params.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		new Label(dosComposite, SWT.NONE);
		new Label(dosComposite, SWT.NONE);

		final Label alt1Label = new Label(dosComposite, SWT.NONE);
		alt1Label.setText(settings.msg("dialog.profile.altexe", new Object[] {1}));
		alt1 = new Text(dosComposite, SWT.BORDER);
		alt1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		alt1_params = new Text(dosComposite, SWT.BORDER);
		final BrowseButton alt1BrowseButton = new BrowseButton(dosComposite, SWT.NONE);
		alt1BrowseButton.connect(shell, alt1, main, BrowseType.FILE, CanonicalType.EXE, false, pixelshader);
		final GrabButton grab3 = new GrabButton(dosComposite, SWT.NONE);
		grab3.connect(alt1, mountingpoints, false);

		final Label alt2Label = new Label(dosComposite, SWT.NONE);
		alt2Label.setText(settings.msg("dialog.profile.altexe", new Object[] {2}));
		alt2 = new Text(dosComposite, SWT.BORDER);
		alt2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		alt2_params = new Text(dosComposite, SWT.BORDER);
		final BrowseButton alt2BrowseButton = new BrowseButton(dosComposite, SWT.NONE);
		alt2BrowseButton.connect(shell, alt2, main, BrowseType.FILE, CanonicalType.EXE, false, pixelshader);
		final GrabButton grab4 = new GrabButton(dosComposite, SWT.NONE);
		grab4.connect(alt2, mountingpoints, false);
	}

	private void startListeners() {
		VerifyListener addMountListener = new VerifyListener() {
			public void verifyText(final VerifyEvent event) {
				if (event.text.length() > 1) {
					boolean booter = event.widget == img1 || event.widget == img2 || event.widget == img3;
					addMountIfNoMounts(event.text, booter);
				}
			}
		};

		main.addVerifyListener(addMountListener);
		setup.addVerifyListener(addMountListener);
		img1.addVerifyListener(addMountListener);
		img2.addVerifyListener(addMountListener);
		img3.addVerifyListener(addMountListener);

		if (isMultiEdit()) {
			ModifyListener changeMarker = new ModifyListener() {
				public void modifyText(ModifyEvent arg0) {
					Control cntrl = (Control)arg0.widget;
					cntrl.setData(true);
					highlight(cntrl, SWT.COLOR_RED);
				}
			};

			SelectionListener selectionMarker = new SelectionListener() {
				public void widgetSelected(SelectionEvent arg0) {
					Control cntrl = (Control)arg0.widget;
					cntrl.setData(true);
					highlight(cntrl, SWT.COLOR_RED);
					if (cntrl instanceof Button)
						((Button)cntrl).setGrayed(false);
				}

				public void widgetDefaultSelected(SelectionEvent arg0) {
					widgetSelected(arg0);
				}
			};

			for (Control child: shell.getChildren()) {
				if (child instanceof TabFolder) {
					TabFolder tabs = (TabFolder)child;
					for (TabItem tab: tabs.getItems()) {
						Composite composite = (Composite)tab.getControl();
						for (Control c: getChangeables(composite)) {
							if (c instanceof Combo) {
								final Combo cntrl = (Combo)c;
								cntrl.setData(false);
								cntrl.addModifyListener(changeMarker);
							} else if (c instanceof Text) {
								final Text cntrl = (Text)c;
								cntrl.setData(false);
								cntrl.addModifyListener(changeMarker);
							} else if (c instanceof Button) {
								final Button cntrl = (Button)c;
								cntrl.setData(false);
								cntrl.addSelectionListener(selectionMarker);
							} else if (c instanceof Spinner) {
								final Spinner cntrl = (Spinner)c;
								cntrl.setData(false);
								cntrl.addModifyListener(changeMarker);
							} else if (c instanceof Scale) {
								final Scale cntrl = (Scale)c;
								cntrl.setData(false);
								cntrl.addSelectionListener(selectionMarker);
							} else if (c instanceof List) {
								final List cntrl = (List)c;
								cntrl.setData(false);
								cntrl.addSelectionListener(selectionMarker);
							}
						}
					}
				}
			}
		}
	}

	private void addMountIfNoMounts(final String hostFileLocation, final boolean booter) {
		if (mountingpoints.getItemCount() == 0) {
			mountingpoints.setItems(multiProfileList.get(0).getConf().addRequiredMount(booter, hostFileLocation));
			mountingpoints.select(mountingpoints.getItemCount() - 1);
		}
	}

	public static Profile duplicateProfile(final Profile prof, final java.util.List<DosboxVersion> dbversionsList, final Database dbase, final Shell shell) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(bos);

		try {
			dbase.startTransaction();
			DosboxVersion dbversion = DosboxVersion.findById(dbversionsList, prof.getDbversionId());
			Conf newCompositeConf = new Conf(prof, dbversion, ps);
			Profile newProfile = dbase.duplicateProfile(prof);
			dbase.saveNativeCommands(dbase.readNativeCommandsList(prof.getId(), -1), newProfile.getId(), -1);
			String newCapturesString = FileUtils.constructCapturesDir(newProfile.getId());
			File newCaptures = FileUtils.canonicalToData(newCapturesString);
			FileUtils.createDir(newCaptures);
			String newConfFile = FileUtils.constructUniqueConfigFileString(newProfile.getId(), prof.getTitle(),
				newCompositeConf.getAutoexec().isIncomplete() ? null: newCompositeConf.getAutoexec().getCanonicalMainDir());

			newProfile = new Profile(newProfile.getId(), newConfFile, newCapturesString, newProfile);

			newCompositeConf.injectOrUpdateProfile(newProfile);
			newCompositeConf.save();

			newProfile = dbase.updateProfileConf(newConfFile, newCapturesString, newProfile.getId());
			dbase.commitTransaction();
			if (GeneralPurposeDialogs.confirmMessage(shell, Settings.getInstance().msg("dialog.profile.confirm.capturesduplication"))) {
				FileUtils.copyFiles(prof.getCanonicalCaptures(), newCaptures);
			}
			if (bos.size() > 0) {
				GeneralPurposeDialogs.warningMessage(shell, bos.toString());
				bos.reset();
			}
			return newProfile;
		} catch (Exception e) {
			GeneralPurposeDialogs.warningMessage(shell, e);
			try {
				dbase.rollbackTransaction();
			} catch (SQLException se) {
				GeneralPurposeDialogs.warningMessage(shell, se);
			}
			return null;
		} finally {
			dbase.finishTransaction();
		}
	}

	protected void doPerformDosboxConfAction(DosboxConfAction action) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(bos);
			updateAllConfigurationsBySettings();

			Conf newDosboxVersion = null;
			if (action == DosboxConfAction.RELOAD_TEMPLATE) {
				newDosboxVersion = new Conf(templatesList.get(template.getSelectionIndex()), dbversionsList.get(dbversion.getSelectionIndex()), ps);
			} else {
				newDosboxVersion = new Conf(dbversionsList.get(dbversion.getSelectionIndex()), ps);
			}

			if (isMultiEdit()) {
				for (Configurable c: multiProfileList) {
					if (c.getConf().hasDifferentBaseMountsThan(newDosboxVersion)) {
						ps.println(Settings.getInstance().msg("dialog.multiprofile.notice.basemountsconflicting"));
						break;
					}
				}
			}

			for (Configurable c: multiProfileList) {
				doPerformdosboxConfAction(action, newDosboxVersion, c);
			}

			if (isMultiEdit()) {
				doPerformdosboxConfAction(action, newDosboxVersion, multiProfileCombined);
				enableSettingsByConfiguration(multiProfileCombined.getConf().getDosboxSettings());
				selectSettingsByConfiguration(multiProfileCombined.getConf());
			} else {
				if (action == DosboxConfAction.RELOAD_TEMPLATE) {
					if (multiProfileList.get(0).getNativeCommandsList().size() == 1) {
						multiProfileList.get(0).setNativeCommandsList(dbase.readNativeCommandsList(-1, templatesList.get(template.getSelectionIndex()).getId()));
						updateNativeCommands(-1);
					}
				}
				enableSettingsByConfiguration(multiProfileList.get(0).getConf().getDosboxSettings());
				selectSettingsByConfiguration(multiProfileList.get(0).getConf());
			}

			if (bos.size() > 0) {
				GeneralPurposeDialogs.warningMessage(getParent(), bos.toString());
				bos.reset();
			}
		} catch (IOException | SQLException e) {
			GeneralPurposeDialogs.warningMessage(getParent(), e);
		}
	}

	private void doPerformdosboxConfAction(DosboxConfAction action, Conf newDosboxVersion, Configurable c) {
		if (action == DosboxConfAction.SET) {
			c.getConf().setToDosboxVersion(newDosboxVersion);
		} else if (action == DosboxConfAction.SWITCH) {
			c.getConf().switchToDosboxVersion(newDosboxVersion);
		} else if (action == DosboxConfAction.RELOAD) {
			c.getConf().reloadDosboxVersion(newDosboxVersion);
		} else if (action == DosboxConfAction.RELOAD_TEMPLATE) {
			c.getConf().reloadTemplate(newDosboxVersion);
		}
	}

	protected void selectSettingsByConfiguration(final Conf conf) {
		super.selectSettingsByConfiguration(conf);

		setFieldIfEnabled(result instanceof Profile ? ((Profile)result).getCaptures(): settings.msg("dialog.profile.automatic"), false, captures);
		setFieldIfEnabled(String.valueOf(conf.getAutoexec().isLoadfix()), Conf.isConflictingValue(conf.getAutoexec().isLoadfix()), loadfix);
		setFieldIfEnabled(String.valueOf(conf.getAutoexec().isLoadhigh()), Conf.isConflictingValue(conf.getAutoexec().isLoadhigh()), loadhigh);
		loadfix_value.setEnabled(loadfix.getSelection());
		setFieldIfEnabled(String.valueOf(conf.getAutoexec().getLoadfixValue()), Conf.isConflictingValue(conf.getAutoexec().getLoadfixValue()), loadfix_value);
		if (!isMultiEdit()) {
			if (conf.getAutoexec().isBooter()) {
				img1.setText(conf.getAutoexec().getImg1());
				img2.setText(conf.getAutoexec().getImg2());
				img3.setText(conf.getAutoexec().getImg3());
				imgDriveletter.setText(conf.getAutoexec().getImgDriveletter());
			} else {
				main.setText(conf.getAutoexec().getMain());
				main_params.setText(conf.getAutoexec().getMainParameters());
			}
		}
	}

	protected void updateAllConfigurationsBySettings() {
		if (isMultiEdit()) {
			String[] customSections = new String[customCommands.length];
			for (int i = 0; i < customCommands.length; i++)
				customSections[i] = StringRelatedUtils.textAreaToString(fetch(customCommands[i]), customCommands[i].getLineDelimiter(), PlatformUtils.EOLN);;
			for (Configurable c: multiProfileList) {
				super.updateConfigurationBySettings(c.getConf());
				c.getConf().setAutoexecSettingsForProfileMultiEdit(fetch(loadhigh), fetch(loadfix), fetch(loadfix_value), fetch(exit), fetch(mixer_config), fetch(keyb), fetch(ipxnet), customSections);
			}
			super.updateConfigurationBySettings(multiProfileCombined.getConf());
			multiProfileCombined.getConf().setAutoexecSettingsForProfileMultiEdit(fetch(loadhigh), fetch(loadfix), fetch(loadfix_value), fetch(exit), fetch(mixer_config), fetch(keyb), fetch(ipxnet),
				customSections);
		} else {
			super.updateConfigurationBySettings(multiProfileList.get(0).getConf());
			multiProfileList.get(0).getConf().setAutoexecSettingsForProfile(loadhigh.getSelection(), loadfix.getSelection(), loadfix_value.getText(), main.getText(), main_params.getText(),
				img1.getText(), img2.getText(), img3.getText(), imgDriveletter.getText());
		}
	}

	protected boolean isValid() {
		GeneralPurposeDialogs.initErrorDialog();
		if (!isMultiEdit()) {
			String requiredMount = null;
			if (title.getText().length() == 0) {
				GeneralPurposeDialogs.addError(settings.msg("dialog.profile.required.title"), title, infoTabItem);
			}
			if (dosExpandItem.getExpanded()) {
				if (FileUtils.isExecutable(main.getText())) {
					requiredMount = dealWithField(false, main.getText());
				} else {
					GeneralPurposeDialogs.addError(settings.msg("dialog.profile.required.mainexe"), main, mountingTabItem);
				}
				if (FileUtils.isExecutable(setup.getText())) {
					requiredMount = dealWithField(false, setup.getText());
				}
			} else {
				if (img1.getText().length() == 0) {
					GeneralPurposeDialogs.addError(settings.msg("dialog.profile.required.booterimage"), img1, mountingTabItem);
				} else {
					requiredMount = dealWithField(true, img1.getText());
				}
				if (img2.getText().length() > 0) {
					requiredMount = dealWithField(true, img2.getText());
				}
				if (img3.getText().length() > 0) {
					requiredMount = dealWithField(true, img3.getText());
				}
			}
			if (requiredMount != null) {
				GeneralPurposeDialogs.addError(settings.msg("dialog.profile.required.mountlocation"), mountingpoints, mountingTabItem);
			}
		}
		if (setButton.isEnabled()) {
			GeneralPurposeDialogs.addError(settings.msg("dialog.template.required.dosboxassociation"), setButton, generalTabItem);
		}
		return !GeneralPurposeDialogs.displayErrorDialog(shell);
	}

	private String dealWithField(final boolean booter, final String loc) {
		String requiredMount = multiProfileList.get(0).getConf().getRequiredMount(booter, loc);
		if (requiredMount != null && GeneralPurposeDialogs.confirmMessage(shell, settings.msg("dialog.profile.confirm.addmountlocation", new Object[] {requiredMount}))) {
			mountingpoints.setItems(multiProfileList.get(0).getConf().addRequiredMount(booter, loc));
		}
		return multiProfileList.get(0).getConf().getRequiredMount(booter, loc);
	}

	private void setProfileMetaData(final Profile prof) {
		setFieldIfEnabled(prof.getTitle(), Conf.isConflictingValue(prof.getTitle()), title);
		setFieldIfEnabled(prof.getDeveloperName(), Conf.isConflictingValue(prof.getDeveloperName()), developer);
		setFieldIfEnabled(prof.getPublisherName(), Conf.isConflictingValue(prof.getPublisherName()), publisher);
		setFieldIfEnabled(prof.getGenre(), Conf.isConflictingValue(prof.getGenre()), genre);
		setFieldIfEnabled(prof.getYear(), Conf.isConflictingValue(prof.getYear()), year);
		for (int i = 0; i < AMOUNT_OF_LINKS; i++) {
			setFieldIfEnabled(prof.getLink(i), Conf.isConflictingValue(prof.getLink(i)), link[i]);
			setFieldIfEnabled(prof.getLinkTitle(i), Conf.isConflictingValue(prof.getLinkTitle(i)), linkTitle[i]);
		}
		setFieldIfEnabled(prof.getStatus(), Conf.isConflictingValue(prof.getStatus()), status);
		setFieldIfEnabled(String.valueOf(prof.isDefault()), Conf.isConflictingValue(prof.isDefault()), favorite);
		setFieldIfEnabled(prof.getNotes(), Conf.isConflictingValue(prof.getNotes()), notes);
		setFieldIfEnabled(prof.getSetup(0), Conf.isConflictingValue(prof.getSetup(0)), setup);
		setFieldIfEnabled(prof.getSetupParameters(0), Conf.isConflictingValue(prof.getSetupParameters(0)), setup_params);
		setFieldIfEnabled(prof.getSetup(1), Conf.isConflictingValue(prof.getSetup(1)), alt1);
		setFieldIfEnabled(prof.getSetupParameters(1), Conf.isConflictingValue(prof.getSetupParameters(1)), alt1_params);
		setFieldIfEnabled(prof.getSetup(2), Conf.isConflictingValue(prof.getSetup(2)), alt2);
		setFieldIfEnabled(prof.getSetupParameters(2), Conf.isConflictingValue(prof.getSetupParameters(2)), alt2_params);
		for (int i = 0; i < AMOUNT_OF_CUSTOM_STRINGS; i++) {
			setFieldIfEnabled(prof.getCustomString(i), Conf.isConflictingValue(prof.getCustomString(i)), customCombo[i]);
			setFieldIfEnabled(prof.getCustomString(AMOUNT_OF_CUSTOM_STRINGS + i), Conf.isConflictingValue(prof.getCustomString(AMOUNT_OF_CUSTOM_STRINGS + i)), customText[i]);
		}
		setFieldIfEnabled(String.valueOf(prof.getCustomInt(0)), Conf.isConflictingValue(prof.getCustomInt(0)), custom9);
		setFieldIfEnabled(String.valueOf(prof.getCustomInt(1)), Conf.isConflictingValue(prof.getCustomInt(1)), custom10);
	}

	private static java.util.List<Control> getChangeables(Composite composite) {
		Control[] children = composite.getChildren();
		java.util.List<Control> result = new ArrayList<Control>();
		for (Control c: children) {
			if ((c instanceof Button && ((c.getStyle() & SWT.CHECK) != 0)) || c instanceof Combo || c instanceof Text || c instanceof Spinner || c instanceof Scale || c instanceof List) {
				result.add(c);
			} else if (c instanceof Group || c instanceof Composite) {
				result.addAll(getChangeables((Composite)c));
			}
		}
		return result;
	}

	private void saveWebImages(File canonicalCapturesDir) {
		for (int i = 0; i < imageInformation.length; i++) {
			if (imgButtons[i].getSelection()) {
				String description = FileUtils.fileSystemSafeWebImages(imageInformation[i].description);
				File file;
				if (imageInformation[i].type == SearchEngineImageType.CoverArt) {
					String filename = settings.msg("dialog.profile.mobygames.coverartfilename", new Object[] {i, description});
					file = new File(canonicalCapturesDir, filename + ".jpg");
				} else {
					String filename = settings.msg("dialog.profile.mobygames.screenshotfilename", new Object[] {i, description});
					file = new File(canonicalCapturesDir, filename + ".png");
				}
				if (!FileUtils.isExistingFile(file)) {
					try {
						SWTImageManager.save(imageInformation[i].data, file.getPath());
					} catch (SWTException e) {
						GeneralPurposeDialogs.warningMessage(shell, settings.msg("general.error.savefile", new Object[] {file.getPath()}), e);
					}
				} else {
					GeneralPurposeDialogs.warningMessage(shell, settings.msg("dialog.profile.error.imagealreadyexists", new Object[] {file.getPath(), getSelectedSearchEngine().getName()}));
				}
			}
		}
	}

	private WebSearchEngine getSelectedSearchEngine() {
		WebSearchEngine engine = null;
		for (WebSearchEngine engn: webSearchEngines) {
			if (engn.getName().equals(engineSelector.getData("engine")))
				engine = engn;
		}
		return engine;
	}

	private WebSearchEngine getSearchEngineBySimpleName(String simpleName) {
		for (WebSearchEngine engn: webSearchEngines) {
			if (engn.getSimpleName().equalsIgnoreCase(simpleName))
				return engn;
		}
		return null;
	}
}
