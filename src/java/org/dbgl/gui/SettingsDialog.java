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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.dbgl.model.Constants;
import org.dbgl.model.conf.SectionsWrapper;
import org.dbgl.model.conf.Settings;
import org.dbgl.util.searchengine.WebSearchEngine;
import swing2swt.layout.BorderLayout;


public class SettingsDialog extends Dialog {

	public static final java.util.List<String> SUPPORTED_LANGUAGES = Arrays.asList("ar", "da", "de", "el", "en", "es", "es__Capitalizado", "fi", "fr", "it", "ko", "nl", "pl", "pt_BR", "ru", "sl",
		"sv", "zh", "zh_TW");
	private static int NR_OF_ENGINES = EditProfileDialog.webSearchEngines.size();

	private final static SortedMap<String, Locale> allLocales;

	static {
		allLocales = new TreeMap<String, Locale>();
		for (Locale loc: Locale.getAvailableLocales()) {
			allLocales.put(loc.toString(), loc);
		}
	}

	private static final int EDITABLE_COLUMN = 0;

	final static String[] confLocations = {Settings.getInstance().msg("dialog.settings.confindbgldir"), Settings.getInstance().msg("dialog.settings.confingamedir")};
	final static String[] confFilenames = {Settings.getInstance().msg("dialog.settings.conffilebyid"), Settings.getInstance().msg("dialog.settings.conffilebytitle")};
	final static String[] buttonDisplayOptions = {Settings.getInstance().msg("dialog.settings.displaybuttonimageandtext"), Settings.getInstance().msg("dialog.settings.displaybuttontextonly"),
			Settings.getInstance().msg("dialog.settings.displaybuttonimageonly")};

	private java.util.List<Integer> allColumnIDs;

	private Table visible_columns;
	private boolean changedVisColumns = false;
	private TableItem[] visibleColumns;
	private TabItem columnsTabItem;
	private Combo confFilename, confLocation, localeCombo, buttonDisplay;
	private Text port, values, envValues;
	private Label heightValue, columnHeightValue;
	private Scale screenshotsHeight, screenshotsColumnHeight;
	private int previousSelection = -1;

	private Button[] setTitle, setDev, setPub, setYear, setGenre, setLink, setDescr, setRank, chooseCoverArt, allRegionsCoverArt, chooseScreenshot;
	private Text[] platformFilterValues;
	private Spinner[] maxCoverArt, maxScreenshots;

	private SectionsWrapper conf;
	private List options;
	protected Shell shell;
	private Settings settings;

	public SettingsDialog(final Shell parent, final int style) {
		super(parent, style);
	}

	public SettingsDialog(final Shell parent) {
		this(parent, SWT.NONE);
	}

	public Object open() {
		settings = Settings.getInstance();
		conf = new SectionsWrapper(settings.getSettings());
		createContents();
		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return changedVisColumns;
	}

	protected void createContents() {
		shell = new Shell(getParent(), SWT.TITLE | SWT.CLOSE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setLayout(new BorderLayout(0, 0));
		shell.addControlListener(new SizeControlAdapter(shell, "settingsdialog"));
		shell.setText(settings.msg("dialog.settings.title"));

		final TabFolder tabFolder = new TabFolder(shell, SWT.NONE);

		final TabItem generalTabItem = new TabItem(tabFolder, SWT.NONE);
		generalTabItem.setText(settings.msg("dialog.settings.tab.general"));

		final Composite composite = new Composite(tabFolder, SWT.NONE);
		composite.setLayout(new GridLayout());
		generalTabItem.setControl(composite);

		final Group dosboxGroup = new Group(composite, SWT.NONE);
		dosboxGroup.setText(settings.msg("dialog.settings.dosbox"));
		dosboxGroup.setLayout(new GridLayout(2, false));

		final Label showConsoleLabel = new Label(dosboxGroup, SWT.NONE);
		showConsoleLabel.setText(settings.msg("dialog.settings.hidestatuswindow"));

		final Button console = new Button(dosboxGroup, SWT.CHECK);
		console.setSelection(conf.getBooleanValue("dosbox", "hideconsole"));

		final Group sendToGroup = new Group(composite, SWT.NONE);
		sendToGroup.setText(settings.msg("dialog.settings.sendto"));
		sendToGroup.setLayout(new GridLayout(2, false));

		final Label enableCommLabel = new Label(sendToGroup, SWT.NONE);
		enableCommLabel.setText(settings.msg("dialog.settings.enableport"));

		final Button portEnabled = new Button(sendToGroup, SWT.CHECK);
		portEnabled.setSelection(conf.getBooleanValue("communication", "port_enabled"));
		portEnabled.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				port.setEnabled(portEnabled.getSelection());
			}
		});

		final Label portnumberLabel = new Label(sendToGroup, SWT.NONE);
		portnumberLabel.setText(settings.msg("dialog.settings.port"));

		port = new Text(sendToGroup, SWT.BORDER);
		port.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		port.setText(conf.getValue("communication", "port"));
		port.setEnabled(portEnabled.getSelection());

		final Group profileDefGroup = new Group(composite, SWT.NONE);
		profileDefGroup.setText(settings.msg("dialog.settings.profiledefaults"));
		profileDefGroup.setLayout(new GridLayout(3, false));

		final Label configFileLabel = new Label(profileDefGroup, SWT.NONE);
		configFileLabel.setText(settings.msg("dialog.settings.configfile"));

		confLocation = new Combo(profileDefGroup, SWT.READ_ONLY);
		confLocation.setItems(confLocations);
		confLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		confLocation.select(conf.getIntValue("profiledefaults", "confpath"));

		confFilename = new Combo(profileDefGroup, SWT.READ_ONLY);
		confFilename.setItems(confFilenames);
		confFilename.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		confFilename.select(conf.getIntValue("profiledefaults", "conffile"));

		final Group i18nGroup = new Group(composite, SWT.NONE);
		i18nGroup.setText(settings.msg("dialog.settings.i18n"));
		i18nGroup.setLayout(new GridLayout(2, false));

		final Label languageLabel = new Label(i18nGroup, SWT.NONE);
		languageLabel.setText(settings.msg("dialog.settings.languagecountry"));

		localeCombo = new Combo(i18nGroup, SWT.READ_ONLY);
		Locale locale = new Locale(conf.getValue("locale", "language"), conf.getValue("locale", "country"), conf.getValue("locale", "variant"));

		final SortedMap<String, Locale> locales = new TreeMap<String, Locale>();
		String locString = "";

		java.util.List<String> supportedLanguages = new ArrayList<String>(SUPPORTED_LANGUAGES);
		File[] files = new File("./plugins/i18n").listFiles();
		if (files != null) {
			for (File file: files) {
				String name = file.getName();
				if (name.startsWith("MessagesBundle_") && name.endsWith(".properties")) {
					String code = name.substring("MessagesBundle_".length(), name.indexOf(".properties"));
					if (code.length() > 0) {
						supportedLanguages.add(code);
					}
				}
			}
		}

		for (String lang: supportedLanguages) {
			Locale loc = allLocales.get(lang);
			String variant = null;
			if (loc == null && StringUtils.countMatches(lang, "_") == 2) {
				String langWithoutVariant = StringUtils.removeEnd(StringUtils.substringBeforeLast(lang, "_"), "_");
				variant = StringUtils.substringAfterLast(lang, "_");
				loc = allLocales.get(langWithoutVariant);
			}
			if (loc != null) {
				StringBuffer s = new StringBuffer(loc.getDisplayLanguage(Locale.getDefault()));
				if (loc.getCountry().length() > 0)
					s.append(" - ").append(loc.getDisplayCountry(Locale.getDefault()));
				if (variant != null) {
					s.append(" (").append(variant).append(')');
					loc = new Locale(loc.getLanguage(), loc.getCountry(), variant);
				}
				locales.put(s.toString(), loc);
				if (loc.equals(locale)) {
					locString = s.toString();
				}
			}
		}

		for (String sloc: locales.keySet()) {
			localeCombo.add(sloc);
		}
		localeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		localeCombo.setText(locString);
		localeCombo.setVisibleItemCount(20);

		columnsTabItem = new TabItem(tabFolder, SWT.NONE);
		columnsTabItem.setText(settings.msg("dialog.settings.tab.profiletable"));

		final Composite composite2 = new Composite(tabFolder, SWT.NONE);
		composite2.setLayout(new BorderLayout(0, 0));
		columnsTabItem.setControl(composite2);

		final Group visColumnsGroup = new Group(composite2, SWT.NONE);
		visColumnsGroup.setLayout(new FillLayout());
		visColumnsGroup.setText(settings.msg("dialog.settings.visiblecolunms"));

		visible_columns = new Table(visColumnsGroup, SWT.FULL_SELECTION | SWT.BORDER | SWT.CHECK);
		visible_columns.setLinesVisible(true);

		TableColumn column1 = new TableColumn(visible_columns, SWT.NONE);
		column1.setWidth(350);

		java.util.List<Integer> visibleColumnIDs = new ArrayList<Integer>();
		for (int i = 0; i < MainWindow.columnNames.length; i++)
			if (conf.getBooleanValue("gui", "column" + (i + 1) + "visible"))
				visibleColumnIDs.add(i);
		java.util.List<Integer> orderedVisibleColumnIDs = new ArrayList<Integer>();
		int[] columnOrder = conf.getIntValues("gui", "columnorder");
		for (int i = 0; i < columnOrder.length; i++)
			orderedVisibleColumnIDs.add(visibleColumnIDs.get(columnOrder[i]));
		java.util.List<Integer> remainingColumnIDs = new ArrayList<Integer>();
		for (int i = 0; i < MainWindow.columnNames.length; i++)
			if (!orderedVisibleColumnIDs.contains(i))
				remainingColumnIDs.add(i);
		allColumnIDs = new ArrayList<Integer>(orderedVisibleColumnIDs);
		allColumnIDs.addAll(remainingColumnIDs);

		visibleColumns = new TableItem[MainWindow.columnNames.length];

		for (int i = 0; i < MainWindow.columnNames.length; i++) {
			visibleColumns[i] = new TableItem(visible_columns, SWT.BORDER);
			visibleColumns[i].setText(MainWindow.columnNames[allColumnIDs.get(i)]);
			visibleColumns[i].setChecked(conf.getBooleanValue("gui", "column" + (allColumnIDs.get(i) + 1) + "visible"));
		}

		final TableEditor editor = new TableEditor(visible_columns);
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;
		editor.minimumWidth = 50;

		visible_columns.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				// Clean up any previous editor control
				Control oldEditor = editor.getEditor();
				if (oldEditor != null) {
					oldEditor.dispose();
				}

				// Identify the selected row
				TableItem item = (TableItem)event.item;
				if (item == null) {
					return;
				}
				int selIdx = item.getParent().getSelectionIndex();
				if (selIdx == -1)
					return;
				int idx = allColumnIDs.get(selIdx);
				if (idx < Constants.RO_COLUMN_NAMES || idx >= (Constants.RO_COLUMN_NAMES + Constants.EDIT_COLUMN_NAMES))
					return;

				// The control that will be the editor must be a child of the table
				Text newEditor = new Text(visible_columns, SWT.NONE);
				newEditor.setText(item.getText(EDITABLE_COLUMN));
				newEditor.addModifyListener(new ModifyListener() {
					public void modifyText(final ModifyEvent mEvent) {
						Text text = (Text)editor.getEditor();
						editor.getItem().setText(EDITABLE_COLUMN, text.getText());
					}
				});
				newEditor.selectAll();
				newEditor.setFocus();
				editor.setEditor(newEditor, item, EDITABLE_COLUMN);
			}
		});

		final Group addProfGroup = new Group(composite2, SWT.NONE);
		addProfGroup.setLayout(new GridLayout(2, false));
		addProfGroup.setText(settings.msg("dialog.settings.addeditduplicateprofile"));
		addProfGroup.setLayoutData(BorderLayout.SOUTH);

		final Label autoSortLabel = new Label(addProfGroup, SWT.NONE);
		autoSortLabel.setText(settings.msg("dialog.settings.autosort"));

		final Button autosort = new Button(addProfGroup, SWT.CHECK);
		autosort.setSelection(conf.getBooleanValue("gui", "autosortonupdate"));

		final TabItem dynTabItem = new TabItem(tabFolder, SWT.NONE);
		dynTabItem.setText(settings.msg("dialog.settings.tab.dynamicoptions"));

		final Composite composite_1 = new Composite(tabFolder, SWT.NONE);
		composite_1.setLayout(new FillLayout());
		dynTabItem.setControl(composite_1);

		final Group dynOptionsGroup = new Group(composite_1, SWT.NONE);
		dynOptionsGroup.setLayout(new GridLayout(2, false));
		dynOptionsGroup.setText(settings.msg("dialog.settings.dynamicoptions"));

		final Label optionsLabel = new Label(dynOptionsGroup, SWT.NONE);
		optionsLabel.setText(settings.msg("dialog.settings.options"));

		final Label valuesLabel = new Label(dynOptionsGroup, SWT.NONE);
		valuesLabel.setText(settings.msg("dialog.settings.values"));

		options = new List(dynOptionsGroup, SWT.V_SCROLL | SWT.BORDER);
		options.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				storeValues();
				previousSelection = options.getSelectionIndex();
				if (previousSelection != -1) {
					values.setText(conf.getMultilineValues("profile", options.getItem(previousSelection), values.getLineDelimiter()));
				}
			}
		});
		options.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		for (String s: conf.getAllItemNames("profile")) {
			options.add(s);
		}

		values = new Text(dynOptionsGroup, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.H_SCROLL);
		values.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		final TabItem guiTabItem = new TabItem(tabFolder, SWT.NONE);
		guiTabItem.setText(settings.msg("dialog.settings.tab.gui"));
		final Composite composite1 = new Composite(tabFolder, SWT.NONE);
		composite1.setLayout(new GridLayout(2, false));
		guiTabItem.setControl(composite1);
		Group screenshots = new Group(composite1, SWT.NONE);
		screenshots.setLayout(new GridLayout(3, false));
		GridData screenshotsLData = new GridData();
		screenshotsLData.grabExcessHorizontalSpace = true;
		screenshotsLData.horizontalAlignment = GridData.FILL;
		screenshotsLData.horizontalSpan = 2;
		screenshots.setLayoutData(screenshotsLData);
		screenshots.setText(settings.msg("dialog.settings.screenshots"));
		Label heightLabel = new Label(screenshots, SWT.NONE);
		heightLabel.setText(settings.msg("dialog.settings.height"));
		GridData sshotsHeightData = new GridData();
		sshotsHeightData.grabExcessHorizontalSpace = true;
		sshotsHeightData.horizontalAlignment = GridData.FILL;
		screenshotsHeight = new Scale(screenshots, SWT.NONE);
		screenshotsHeight.setMaximum(750);
		screenshotsHeight.setMinimum(50);
		screenshotsHeight.setLayoutData(sshotsHeightData);
		screenshotsHeight.setIncrement(25);
		screenshotsHeight.setPageIncrement(100);
		screenshotsHeight.setSelection(conf.getIntValue("gui", "screenshotsheight"));
		screenshotsHeight.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent evt) {
				heightValue.setText(screenshotsHeight.getSelection() + settings.msg("dialog.settings.px"));
				heightValue.pack();
			}
		});
		heightValue = new Label(screenshots, SWT.NONE);
		heightValue.setText(screenshotsHeight.getSelection() + settings.msg("dialog.settings.px"));

		final Label displayFilenameLabel = new Label(screenshots, SWT.NONE);
		displayFilenameLabel.setText(settings.msg("dialog.settings.screenshotsfilename"));
		final Button displayFilename = new Button(screenshots, SWT.CHECK);
		displayFilename.setSelection(conf.getBooleanValue("gui", "screenshotsfilename"));

		Group screenshotsColumn = new Group(composite1, SWT.NONE);
		screenshotsColumn.setLayout(new GridLayout(3, false));
		GridData screenshotsCData = new GridData();
		screenshotsCData.grabExcessHorizontalSpace = true;
		screenshotsCData.horizontalAlignment = GridData.FILL;
		screenshotsCData.horizontalSpan = 2;
		screenshotsColumn.setLayoutData(screenshotsCData);
		screenshotsColumn.setText(settings.msg("dialog.settings.screenshotscolumn"));
		Label columnHeightLabel = new Label(screenshotsColumn, SWT.NONE);
		columnHeightLabel.setText(settings.msg("dialog.settings.height"));
		screenshotsColumnHeight = new Scale(screenshotsColumn, SWT.NONE);
		screenshotsColumnHeight.setMaximum(200);
		screenshotsColumnHeight.setMinimum(16);
		GridData sshotsColumnHeightData = new GridData();
		sshotsColumnHeightData.grabExcessHorizontalSpace = true;
		sshotsColumnHeightData.horizontalAlignment = GridData.FILL;
		screenshotsColumnHeight.setLayoutData(sshotsColumnHeightData);
		screenshotsColumnHeight.setIncrement(4);
		screenshotsColumnHeight.setPageIncrement(16);
		screenshotsColumnHeight.setSelection(conf.getIntValue("gui", "screenshotscolumnheight"));
		screenshotsColumnHeight.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent evt) {
				columnHeightValue.setText(screenshotsColumnHeight.getSelection() + settings.msg("dialog.settings.px"));
				columnHeightValue.pack();
			}
		});
		columnHeightValue = new Label(screenshotsColumn, SWT.NONE);
		columnHeightValue.setText(screenshotsColumnHeight.getSelection() + settings.msg("dialog.settings.px"));

		final Label stretchLabel = new Label(screenshotsColumn, SWT.NONE);
		stretchLabel.setText(settings.msg("dialog.settings.screenshotscolumnstretch"));
		final Button stretch = new Button(screenshotsColumn, SWT.CHECK);
		stretch.setSelection(conf.getBooleanValue("gui", "screenshotscolumnstretch"));
		new Label(screenshotsColumn, SWT.NONE);
		final Label keepAspectRatioLabel = new Label(screenshotsColumn, SWT.NONE);
		keepAspectRatioLabel.setText(settings.msg("dialog.settings.screenshotscolumnkeepaspectratio"));
		final Button keepAspectRatio = new Button(screenshotsColumn, SWT.CHECK);
		keepAspectRatio.setSelection(conf.getBooleanValue("gui", "screenshotscolumnkeepaspectratio"));
		new Label(screenshotsColumn, SWT.NONE);
		keepAspectRatio.setEnabled(stretch.getSelection());
		stretch.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				keepAspectRatio.setEnabled(stretch.getSelection());
			}
		});

		Group buttonsGroup = new Group(composite1, SWT.NONE);
		buttonsGroup.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		buttonsGroup.setLayout(new GridLayout(2, false));
		buttonsGroup.setText(settings.msg("dialog.settings.buttons"));

		final Label buttonLabel = new Label(buttonsGroup, SWT.NONE);
		buttonLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		buttonLabel.setText(settings.msg("dialog.settings.display"));

		buttonDisplay = new Combo(buttonsGroup, SWT.READ_ONLY);
		buttonDisplay.setItems(buttonDisplayOptions);
		buttonDisplay.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		buttonDisplay.select(conf.getIntValue("gui", "buttondisplay"));

		final Group notesGroup = new Group(composite1, SWT.NONE);
		notesGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		notesGroup.setLayout(new GridLayout(2, false));
		notesGroup.setText(settings.msg("dialog.profile.notes"));

		final Label fontLabel = new Label(notesGroup, SWT.NONE);
		fontLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		fontLabel.setText(settings.msg("dialog.settings.font"));

		final Button fontButton = new Button(notesGroup, SWT.PUSH);
		fontButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Font f = GeneralPurposeGUI.stringToFont(shell.getDisplay(), port.getFont(), conf.getValues("gui", "notesfont"));
		fontButton.setText(f.getFontData()[0].getName());
		fontButton.setFont(f);
		fontButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FontDialog fd = new FontDialog(shell, SWT.NONE);
				fd.setFontList(fontButton.getFont().getFontData());
				FontData newFont = fd.open();
				if (newFont != null) {
					fontButton.setText(newFont.getName());
					fontButton.setFont(new Font(shell.getDisplay(), newFont));
					notesGroup.setSize(notesGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
					composite1.layout();
				}
			}
		});

		final TabItem enginesTabItem = new TabItem(tabFolder, SWT.NONE);
		enginesTabItem.setText(settings.msg("dialog.settings.tab.engines"));
		Composite compositeHoldingSubTabs = new Composite(tabFolder, SWT.NONE);
		compositeHoldingSubTabs.setLayout(new FillLayout());
		enginesTabItem.setControl(compositeHoldingSubTabs);
		final TabFolder enginesTabFolder = new TabFolder(compositeHoldingSubTabs, SWT.NONE);

		setTitle = new Button[NR_OF_ENGINES];
		setDev = new Button[NR_OF_ENGINES];
		setPub = new Button[NR_OF_ENGINES];
		setYear = new Button[NR_OF_ENGINES];
		setGenre = new Button[NR_OF_ENGINES];
		setLink = new Button[NR_OF_ENGINES];
		setRank = new Button[NR_OF_ENGINES];
		setDescr = new Button[NR_OF_ENGINES];
		allRegionsCoverArt = new Button[NR_OF_ENGINES];
		chooseCoverArt = new Button[NR_OF_ENGINES];
		chooseScreenshot = new Button[NR_OF_ENGINES];
		maxCoverArt = new Spinner[NR_OF_ENGINES];
		maxScreenshots = new Spinner[NR_OF_ENGINES];
		platformFilterValues = new Text[NR_OF_ENGINES];

		for (int i = 0; i < NR_OF_ENGINES; i++) {
			WebSearchEngine engine = EditProfileDialog.webSearchEngines.get(i);
			final TabItem engineTabItem = new TabItem(enginesTabFolder, SWT.NONE);
			engineTabItem.setText(settings.msg("dialog.settings.tab." + engine.getSimpleName()));
			Composite composite3 = new Composite(enginesTabFolder, SWT.NONE);
			composite3.setLayout(new GridLayout(1, true));
			engineTabItem.setControl(composite3);
			Group consult = new Group(composite3, SWT.NONE);
			consult.setLayout(new GridLayout(2, false));
			GridData consultLData = new GridData();
			consultLData.grabExcessHorizontalSpace = true;
			consultLData.horizontalAlignment = GridData.FILL;
			consultLData.grabExcessVerticalSpace = true;
			consultLData.verticalAlignment = GridData.FILL;
			consult.setLayoutData(consultLData);
			consult.setText(settings.msg("dialog.settings.consult", new String[] {engine.getName()}));
			Label titleLabel = new Label(consult, SWT.NONE);
			titleLabel.setText(settings.msg("dialog.settings.settitle"));
			setTitle[i] = new Button(consult, SWT.CHECK);
			setTitle[i].setSelection(conf.getBooleanValue(engine.getSimpleName(), "set_title"));
			Label devLabel = new Label(consult, SWT.NONE);
			devLabel.setText(settings.msg("dialog.settings.setdeveloper"));
			setDev[i] = new Button(consult, SWT.CHECK);
			setDev[i].setSelection(conf.getBooleanValue(engine.getSimpleName(), "set_developer"));
			if (engine.getSimpleName().equals("mobygames") || engine.getSimpleName().equals("hotud") || engine.getSimpleName().equals("thegamesdb")) {
				Label pubLabel = new Label(consult, SWT.NONE);
				pubLabel.setText(settings.msg("dialog.settings.setpublisher"));
				setPub[i] = new Button(consult, SWT.CHECK);
				setPub[i].setSelection(conf.getBooleanValue(engine.getSimpleName(), "set_publisher"));
			}
			Label yearLabel = new Label(consult, SWT.NONE);
			yearLabel.setText(settings.msg("dialog.settings.setyear"));
			setYear[i] = new Button(consult, SWT.CHECK);
			setYear[i].setSelection(conf.getBooleanValue(engine.getSimpleName(), "set_year"));
			Label genreLabel = new Label(consult, SWT.NONE);
			genreLabel.setText(settings.msg("dialog.settings.setgenre"));
			setGenre[i] = new Button(consult, SWT.CHECK);
			setGenre[i].setSelection(conf.getBooleanValue(engine.getSimpleName(), "set_genre"));
			Label linkLabel = new Label(consult, SWT.NONE);
			linkLabel.setText(settings.msg("dialog.settings.setlink", new String[] {engine.getName()}));
			setLink[i] = new Button(consult, SWT.CHECK);
			setLink[i].setSelection(conf.getBooleanValue(engine.getSimpleName(), "set_link"));
			Label rankLabel = new Label(consult, SWT.NONE);
			rankLabel.setText(settings.msg("dialog.settings.setrank", new Object[] {MainWindow.columnNames[Constants.RO_COLUMN_NAMES + 8]}));
			setRank[i] = new Button(consult, SWT.CHECK);
			setRank[i].setSelection(conf.getBooleanValue(engine.getSimpleName(), "set_rank"));
			if (engine.getSimpleName().equals("mobygames") || engine.getSimpleName().equals("hotud") || engine.getSimpleName().equals("thegamesdb")) {
				Label descrLabel = new Label(consult, SWT.NONE);
				descrLabel.setText(settings.msg("dialog.settings.setdescription"));
				setDescr[i] = new Button(consult, SWT.CHECK);
				setDescr[i].setSelection(conf.getBooleanValue(engine.getSimpleName(), "set_description"));
			}
			if (engine.getSimpleName().equals("mobygames") || engine.getSimpleName().equals("thegamesdb")) {
				Label chooseCoverArtLabel = new Label(consult, SWT.NONE);
				chooseCoverArtLabel.setText(settings.msg("dialog.settings.choosecoverart"));
				Composite comp = new Composite(consult, SWT.NONE);
				GridLayout layout = new GridLayout(3, false);
				layout.marginWidth = 0;
				comp.setLayout(layout);
				chooseCoverArt[i] = new Button(comp, SWT.CHECK);
				chooseCoverArt[i].setSelection(conf.getBooleanValue(engine.getSimpleName(), "choose_coverart"));

				if (engine.getSimpleName().equals("mobygames")) {
					Label allRegionsCoverArtLabel = new Label(comp, SWT.NONE);
					allRegionsCoverArtLabel.setText(settings.msg("dialog.settings.allregionscoverart"));
					GridData gd = new GridData();
					gd.horizontalIndent = 40;
					allRegionsCoverArtLabel.setLayoutData(gd);

					allRegionsCoverArt[i] = new Button(comp, SWT.CHECK);
					allRegionsCoverArt[i].setSelection(conf.getBooleanValue(engine.getSimpleName(), "force_all_regions_coverart"));
				}
			}
			if (engine.getSimpleName().equals("mobygames") || engine.getSimpleName().equals("pouet") || engine.getSimpleName().equals("thegamesdb")) {
				Label chooseScreenshotLabel = new Label(consult, SWT.NONE);
				chooseScreenshotLabel.setText(settings.msg("dialog.settings.choosescreenshot"));
				chooseScreenshot[i] = new Button(consult, SWT.CHECK);
				chooseScreenshot[i].setSelection(conf.getBooleanValue(engine.getSimpleName(), "choose_screenshot"));
			}
			if (engine.getSimpleName().equals("mobygames") || engine.getSimpleName().equals("thegamesdb")) {
				final Label maxCoverArtLabel = new Label(consult, SWT.NONE);
				maxCoverArtLabel.setText(settings.msg("dialog.settings.multieditmaxcoverart"));
				maxCoverArt[i] = new Spinner(consult, SWT.BORDER);
				maxCoverArt[i].setLayoutData(new GridData(100, SWT.DEFAULT));
				maxCoverArt[i].setMinimum(0);
				maxCoverArt[i].setMaximum(Integer.MAX_VALUE);
				maxCoverArt[i].setSelection(conf.getIntValue(engine.getSimpleName(), "multi_max_coverart"));
			}
			if (engine.getSimpleName().equals("mobygames") || engine.getSimpleName().equals("pouet") || engine.getSimpleName().equals("thegamesdb")) {
				final Label maxScreenshotsLabel = new Label(consult, SWT.NONE);
				maxScreenshotsLabel.setText(settings.msg("dialog.settings.multieditmaxscreenshot"));
				maxScreenshots[i] = new Spinner(consult, SWT.BORDER);
				maxScreenshots[i].setLayoutData(new GridData(100, SWT.DEFAULT));
				maxScreenshots[i].setMinimum(0);
				maxScreenshots[i].setMaximum(Integer.MAX_VALUE);
				maxScreenshots[i].setSelection(conf.getIntValue(engine.getSimpleName(), "multi_max_screenshot"));
			}
			Label filterLabel = new Label(consult, SWT.NONE);
			filterLabel.setText(settings.msg("dialog.settings.platformfilter"));
			if (engine.getSimpleName().equals("mobygames") || engine.getSimpleName().equals("pouet")) {
				platformFilterValues[i] = new Text(consult, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.H_SCROLL);
				platformFilterValues[i].setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				platformFilterValues[i].setText(conf.getMultilineValues(engine.getSimpleName(), "platform_filter", platformFilterValues[i].getLineDelimiter()));
			} else {
				platformFilterValues[i] = new Text(consult, SWT.BORDER);
				platformFilterValues[i].setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				platformFilterValues[i].setText(conf.getValue(engine.getSimpleName(), "platform_filter"));
			}
		}

		final TabItem envTabItem = new TabItem(tabFolder, SWT.NONE);
		envTabItem.setText(settings.msg("dialog.settings.tab.environment"));
		Composite composite4 = new Composite(tabFolder, SWT.NONE);
		composite4.setLayout(new GridLayout(1, true));
		envTabItem.setControl(composite4);
		Group envGroup = new Group(composite4, SWT.NONE);
		envGroup.setLayout(new GridLayout(2, false));
		GridData envLData = new GridData();
		envLData.grabExcessHorizontalSpace = true;
		envLData.horizontalAlignment = GridData.FILL;
		envLData.grabExcessVerticalSpace = true;
		envLData.verticalAlignment = GridData.FILL;
		envGroup.setLayoutData(envLData);
		envGroup.setText(settings.msg("dialog.settings.environment"));
		Label enableEnvLabel = new Label(envGroup, SWT.NONE);
		enableEnvLabel.setText(settings.msg("dialog.settings.enableenvironment"));
		final Button enableEnv = new Button(envGroup, SWT.CHECK);
		enableEnv.setSelection(conf.getBooleanValue("environment", "use"));
		enableEnv.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				envValues.setEnabled(enableEnv.getSelection());
			}
		});
		Label envLabel = new Label(envGroup, SWT.NONE);
		envLabel.setText(settings.msg("dialog.settings.environmentvariables"));
		envValues = new Text(envGroup, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.H_SCROLL);
		envValues.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		envValues.setText(conf.getMultilineValues("environment", "value", envValues.getLineDelimiter()));
		envValues.setEnabled(enableEnv.getSelection());

		final Composite composite_7 = new Composite(shell, SWT.NONE);
		composite_7.setLayout(new GridLayout(2, true));
		composite_7.setLayoutData(BorderLayout.SOUTH);

		final Button okButton = new Button(composite_7, SWT.NONE);
		okButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				if (!isValid()) {
					return;
				}

				changedVisColumns = haveColumnsBeenChanged();
				if (changedVisColumns)
					updateColumnSettings();

				conf.setBooleanValue("dosbox", "hideconsole", console.getSelection());
				conf.setBooleanValue("communication", "port_enabled", portEnabled.getSelection());
				conf.setValue("communication", "port", port.getText());
				conf.setIntValue("profiledefaults", "confpath", confLocation.getSelectionIndex());
				conf.setIntValue("profiledefaults", "conffile", confFilename.getSelectionIndex());
				conf.setValue("locale", "language", locales.get(localeCombo.getText()).getLanguage());
				conf.setValue("locale", "country", locales.get(localeCombo.getText()).getCountry());
				conf.setValue("locale", "variant", locales.get(localeCombo.getText()).getVariant());
				for (int i = 0; i < MainWindow.columnNames.length; i++) {
					conf.setBooleanValue("gui", "column" + (i + 1) + "visible", visibleColumns[allColumnIDs.indexOf(i)].getChecked());
				}
				conf.setBooleanValue("gui", "autosortonupdate", autosort.getSelection());
				for (int i = 0; i < Constants.EDIT_COLUMN_NAMES; i++) {
					conf.setValue("gui", "custom" + (i + 1), visibleColumns[allColumnIDs.indexOf(i + Constants.RO_COLUMN_NAMES)].getText());
				}
				conf.setIntValue("gui", "screenshotsheight", screenshotsHeight.getSelection());
				conf.setBooleanValue("gui", "screenshotsfilename", displayFilename.getSelection());
				conf.setIntValue("gui", "screenshotscolumnheight", screenshotsColumnHeight.getSelection());
				conf.setBooleanValue("gui", "screenshotscolumnstretch", stretch.getSelection());
				conf.setBooleanValue("gui", "screenshotscolumnkeepaspectratio", keepAspectRatio.getSelection());

				Rectangle rec = shell.getBounds();
				conf.setIntValue("gui", "settingsdialog_width", rec.width);
				conf.setIntValue("gui", "settingsdialog_height", rec.height);
				conf.setIntValue("gui", "buttondisplay", buttonDisplay.getSelectionIndex());
				conf.setMultilineValues("gui", "notesfont", GeneralPurposeGUI.fontToString(shell.getDisplay(), fontButton.getFont()), "|");

				for (int i = 0; i < NR_OF_ENGINES; i++) {
					WebSearchEngine engine = EditProfileDialog.webSearchEngines.get(i);
					conf.setBooleanValue(engine.getSimpleName(), "set_title", setTitle[i].getSelection());
					conf.setBooleanValue(engine.getSimpleName(), "set_developer", setDev[i].getSelection());
					if (engine.getSimpleName().equals("mobygames") || engine.getSimpleName().equals("hotud") || engine.getSimpleName().equals("thegamesdb")) {
						conf.setBooleanValue(engine.getSimpleName(), "set_publisher", setPub[i].getSelection());
						conf.setBooleanValue(engine.getSimpleName(), "set_description", setDescr[i].getSelection());
					}
					conf.setBooleanValue(engine.getSimpleName(), "set_year", setYear[i].getSelection());
					conf.setBooleanValue(engine.getSimpleName(), "set_genre", setGenre[i].getSelection());
					conf.setBooleanValue(engine.getSimpleName(), "set_link", setLink[i].getSelection());
					conf.setBooleanValue(engine.getSimpleName(), "set_rank", setRank[i].getSelection());
					if (engine.getSimpleName().equals("mobygames")) {
						conf.setBooleanValue(engine.getSimpleName(), "force_all_regions_coverart", allRegionsCoverArt[i].getSelection());
					}
					if (engine.getSimpleName().equals("mobygames") || engine.getSimpleName().equals("thegamesdb")) {
						conf.setBooleanValue(engine.getSimpleName(), "choose_coverart", chooseCoverArt[i].getSelection());
						conf.setIntValue(engine.getSimpleName(), "multi_max_coverart", maxCoverArt[i].getSelection());
					}
					if (engine.getSimpleName().equals("mobygames") || engine.getSimpleName().equals("pouet") || engine.getSimpleName().equals("thegamesdb")) {
						conf.setBooleanValue(engine.getSimpleName(), "choose_screenshot", chooseScreenshot[i].getSelection());
						conf.setIntValue(engine.getSimpleName(), "multi_max_screenshot", maxScreenshots[i].getSelection());
					}
					if (engine.getSimpleName().equals("mobygames") || engine.getSimpleName().equals("pouet")) {
						conf.setMultilineValues(engine.getSimpleName(), "platform_filter", platformFilterValues[i].getText(), platformFilterValues[i].getLineDelimiter());
					} else {
						conf.setValue(engine.getSimpleName(), "platform_filter", platformFilterValues[i].getText());
					}
				}

				conf.setBooleanValue("environment", "use", enableEnv.getSelection());
				conf.setMultilineValues("environment", "value", envValues.getText(), envValues.getLineDelimiter());

				storeValues();
				settings.getSettings().injectValuesFrom(conf);
				shell.close();
			}

			private boolean haveColumnsBeenChanged() {
				for (int i = 0; i < MainWindow.columnNames.length; i++)
					if ((conf.getBooleanValue("gui", "column" + (allColumnIDs.get(i) + 1) + "visible") != visibleColumns[i].getChecked())
							|| !MainWindow.columnNames[allColumnIDs.get(i)].equals(visibleColumns[i].getText()))
						return true;
				return false;
			}
		});
		shell.setDefaultButton(okButton);
		okButton.setText(settings.msg("button.ok"));

		final Button cancelButton = new Button(composite_7, SWT.NONE);
		cancelButton.setText(settings.msg("button.cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				shell.close();
			}
		});

		final GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.widthHint = GeneralPurposeGUI.getWidth(okButton, cancelButton);
		okButton.setLayoutData(gridData);
		cancelButton.setLayoutData(gridData);
	}

	private void storeValues() {
		if (previousSelection != -1) {
			conf.setMultilineValues("profile", options.getItem(previousSelection), values.getText(), values.getLineDelimiter());
		}
	}

	private int countVisibleColumns() {
		int result = 0;
		for (int i = 0; i < MainWindow.columnNames.length; i++) {
			if (visibleColumns[i].getChecked()) {
				result++;
			}
		}
		return result;
	}

	private int getFirstVisibleColumn() {
		for (int i = 0; i < MainWindow.columnNames.length; i++) {
			if (visibleColumns[i].getChecked()) {
				return i;
			}
		}
		return -1;
	}

	private void updateColumnSettings() {
		int[] sort = conf.getIntValues("gui", "sortcolumn");
		boolean[] ascs = conf.getBooleanValues("gui", "sortascending");
		java.util.List<Integer> sortColumnIDs = new ArrayList<Integer>(sort.length);
		java.util.List<Boolean> sortColumnAscs = new ArrayList<Boolean>(sort.length);

		for (int i = 0; i < sort.length; i++) {
			if (visibleColumns[allColumnIDs.indexOf(sort[i])].getChecked()) {
				sortColumnIDs.add(sort[i]);
				sortColumnAscs.add(ascs[i]);
			}
		}
		if (sortColumnIDs.isEmpty()) {
			sortColumnIDs.add(allColumnIDs.get(getFirstVisibleColumn()));
			sortColumnAscs.add(true);
		}

		conf.setIntValues("gui", "sortcolumn", ArrayUtils.toPrimitive(sortColumnIDs.toArray(new Integer[0])));
		conf.setBooleanValues("gui", "sortascending", ArrayUtils.toPrimitive(sortColumnAscs.toArray(new Boolean[0])));

		java.util.List<Integer> visColumns = new ArrayList<Integer>();
		for (int i = 0; i < MainWindow.columnNames.length; i++)
			if (visibleColumns[i].getChecked())
				visColumns.add(allColumnIDs.get(i));

		java.util.List<Integer> orderedVisColumns = new ArrayList<Integer>(visColumns);
		Collections.sort(orderedVisColumns);

		java.util.List<Integer> colOrder = new ArrayList<Integer>();
		for (int id: visColumns)
			colOrder.add(orderedVisColumns.indexOf(id));

		conf.setValue("gui", "columnorder", StringUtils.join(colOrder, ' '));
	}

	private boolean isValid() {
		GeneralPurposeDialogs.initErrorDialog();
		if (countVisibleColumns() == 0) {
			GeneralPurposeDialogs.addError(settings.msg("dialog.settings.required.onevisiblecolumn"), visible_columns, columnsTabItem);
		}
		return !GeneralPurposeDialogs.displayErrorDialog(shell);
	}
}
