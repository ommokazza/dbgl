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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.dbgl.model.DosboxVersion;
import org.dbgl.model.ExpProfile;
import org.dbgl.model.Constants;
import org.dbgl.model.conf.Settings;
import org.dbgl.util.FileUtils;
import org.dbgl.util.ImportThread;
import org.dbgl.util.ProgressNotifyable;
import org.dbgl.util.SevenzipExtractSingleFileCallback;
import org.dbgl.util.XmlUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import SevenZip.MyRandomAccessFile;
import SevenZip.Archive.IInArchive;
import SevenZip.Archive.SevenZipEntry;
import SevenZip.Archive.SevenZip.Handler;
import swing2swt.layout.BorderLayout;


public class ImportDialog extends Wizard {

	private Text confLogText, logText, title, author, notes;
	private Button settingsOnly, fullGames, fullSettingsButton, incrementalSettingsButton;
	private Button importCapturesButton, importMapperfilesButton, importNativeCommandsButton, useOrgConf, createNewConf;
	private Button customValues, customFields;
	private Table profilesTable, impDbVersionsList;
	private List myDbVersionsList;
	private ProgressBar progressBar;
	private Label profileLabel;

	private File archive;
	private java.util.List<DosboxVersion> dbversionsList;

	private StringBuffer messageLog;
	private String packageVersion, packageTitle, packageAuthor, packageNotes, creationApp, creationAppVersion;
	private String[] customFieldTitles;
	private Date creationDate;
	private boolean capturesAvailable = false, mapperfilesAvailable = false, nativecommandsAvailable = false, gamedataAvailable = false;
	private java.util.List<ExpProfile> profs;
	private SortedSet<DosboxVersion> dbSet;
	private java.util.List<Integer> dbmapping;

	public ImportDialog(Shell parent, java.util.List<DosboxVersion> dbList, File archive) {
		super(parent, SWT.NONE, Settings.getInstance().msg("dialog.import.title"), "import", true);
		this.dbversionsList = dbList;
		this.archive = archive;
	}

	protected boolean actionAfterNext() {
		if (stepNr == 2) {
			refillImportedDBVersionsList();
		} else if (stepNr == 4) {
			try {
				// check for equal gamedirs, if there are, set importedid to the first
				for (int i = 0; i < profs.size(); i++) {
					for (int j = 0; j < i; j++) {
						ExpProfile pI = profs.get(i);
						ExpProfile pJ = profs.get(j);
						if (pI.getGameDir().equals(pJ.getGameDir())) {
							pI.setImportedId(pJ.getImportedId());
						}
					}
				}

				for (int i = profs.size() - 1; i >= 0; i--) {
					TableItem it = profilesTable.getItem(i);
					if (it.getChecked()) {
						ExpProfile p = profs.get(i);
						p.setDbversionId(getMappedDosboxVersionId(dbSet, dbmapping, p.getDbversionId()));
					} else {
						profs.remove(i);
					}
				}

				extensiveJobThread = new ImportThread(logText, progressBar, profileLabel, profs, archive, importCapturesButton.getSelection(), importMapperfilesButton.getSelection(),
						importNativeCommandsButton.getSelection(), useOrgConf.getSelection(), fullGames.getSelection(), fullSettingsButton.getSelection(), customValues.getSelection(),
						customFields.getSelection() ? customFieldTitles: null);

			} catch (IOException | SQLException e) {
				GeneralPurposeDialogs.warningMessage(shell, e);
				extensiveJobThread = null;
			}
		} else if (stepNr == 5) {
			if (extensiveJobThread.isEverythingOk()) {
				GeneralPurposeDialogs.infoMessage(shell, settings.msg("dialog.import.notice.importok"));
			} else {
				GeneralPurposeDialogs.warningMessage(shell, settings.msg("dialog.import.error.problem"));
			}
			profileLabel.setText(settings.msg("dialog.export.reviewlog"));
			profileLabel.pack();
		}
		return true;
	}

	protected boolean init() {
		Settings settings = Settings.getInstance();

		messageLog = new StringBuffer();
		try {
			profs = new ArrayList<ExpProfile>();
			dbSet = new TreeSet<DosboxVersion>();

			Document doc = getProfilesXmlDocFromZip();
			if (doc == null)
				throw new ZipException(settings.msg("dialog.import.error.gamepackarchivemissingprofilesxml"));

			XPathFactory xfactory = XPathFactory.newInstance();
			XPath xPath = xfactory.newXPath();
			packageVersion = xPath.evaluate("/document/export/format-version", doc);
			packageTitle = xPath.evaluate("/document/export/title", doc);
			packageAuthor = xPath.evaluate("/document/export/author", doc);
			packageNotes = xPath.evaluate("/document/export/notes", doc);
			customFieldTitles = new String[Constants.EDIT_COLUMN_NAMES];
			for (int i = 0; i < Constants.EDIT_COLUMN_NAMES; i++) {
				customFieldTitles[i] = xPath.evaluate("/document/export/custom" + (i + 1), doc);
			}
			creationApp = xPath.evaluate("/document/export/generator-title", doc);
			creationAppVersion = xPath.evaluate("/document/export/generator-version", doc);
			creationDate = XmlUtils.datetimeFormatter.parse(xPath.evaluate("/document/export/creationdatetime", doc));
			capturesAvailable = Boolean.valueOf(xPath.evaluate("/document/export/captures-available", doc));
			mapperfilesAvailable = (packageVersion.equals("1.0")) ? false: Boolean.valueOf(xPath.evaluate("/document/export/keymapperfiles-available", doc));
			nativecommandsAvailable = (packageVersion.equals("1.0") || packageVersion.equals("1.1")) ? false: Boolean.valueOf(xPath.evaluate("/document/export/nativecommands-available", doc));
			gamedataAvailable = Boolean.valueOf(xPath.evaluate("/document/export/gamedata-available", doc));

			NodeList profNodes = (NodeList)xPath.evaluate("/document/profile", doc, XPathConstants.NODESET);

			for (int i = 0; i < profNodes.getLength(); i++) {
				Element profileNode = (Element)profNodes.item(i);
				Element dosbox = XmlUtils.getNode(profileNode, "dosbox");
				DosboxVersion d = new DosboxVersion(i, XmlUtils.getTextValue(dosbox, "title"), "", "", true, false, false, "", XmlUtils.getTextValue(dosbox, "version"), null, null, null, 0);
				dbSet.add(d);
				profs.add(new ExpProfile(profileNode, getDosboxVersionId(d, dbSet), nativecommandsAvailable, packageVersion));
			}

		} catch (ZipException | ParserConfigurationException | ParseException e) {
			messageLog.append(e.toString()).append('\n');
			e.printStackTrace();
		} catch (XPathExpressionException | SAXException e) {
			messageLog.append(settings.msg("dialog.import.error.profilesxmlinvalidformat", new Object[] {e.toString()})).append('\n');
			e.printStackTrace();
		} catch (IOException e) {
			messageLog.append(settings.msg("general.error.openfile", new Object[] {archive})).append('\n').append(e.toString()).append('\n');
			e.printStackTrace();
		}
		if (messageLog.length() == 0) {
			messageLog.append(settings.msg("dialog.import.notice.importinformation", new Object[] {packageVersion, creationDate, creationApp, creationAppVersion})).append('\n');
		}
		return true;
	}

	public Document getProfilesXmlDocFromZip() throws IOException, ParserConfigurationException, SAXException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		String archive = this.archive.getPath();

		Document doc = null;

		if (archive.toLowerCase().endsWith(FileUtils.ARCHIVES[0])) { // zip
			ZipFile zf = new ZipFile(archive);
			for (Enumeration<? extends ZipEntry> entries = zf.entries(); entries.hasMoreElements();) {
				ZipEntry entry = entries.nextElement();
				if (!entry.isDirectory() && entry.getName().equalsIgnoreCase(FileUtils.PROFILES_XML)) {
					doc = builder.parse(zf.getInputStream(entry));
					break;
				}
			}
			zf.close();
			return doc;
		} else if (archive.toLowerCase().endsWith(FileUtils.ARCHIVES[1])) { // 7-zip
			MyRandomAccessFile istream = new MyRandomAccessFile(archive, "r");
			final IInArchive zArchive = new Handler();
			if (zArchive.Open(istream) != 0) {
				throw new IOException(Settings.getInstance().msg("general.error.opensevenzip", new Object[] {archive}));
			}

			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			for (int i = 0; i < zArchive.size(); i++) {
				SevenZipEntry entry = zArchive.getEntry(i);
				if (!entry.isDirectory() && entry.getName().equalsIgnoreCase(FileUtils.PROFILES_XML)) {

					class Extract extends Thread {
						private int entryId;
						private SevenzipExtractSingleFileCallback extractCallback;
						private IOException exception;

						Extract(int entryId, ProgressNotifyable prog) {
							this.entryId = entryId;
							this.extractCallback = new SevenzipExtractSingleFileCallback(prog, out);
							this.exception = null;
						}

						public void run() {
							try {
								zArchive.Extract(new int[] {entryId}, 1, IInArchive.NExtract_NAskMode_kExtract, extractCallback);
							} catch (IOException e) {
								exception = e;
							}
						}

						public IOException getIOException() {
							return exception;
						}
					}

					ProgressDialog prog = new ProgressDialog(getParent(), settings.msg("dialog.import.notice.processing7zip"));
					Extract extract = new Extract(i, prog);
					prog.setThread(extract);
					prog.open();

					if (extract.getIOException() != null) {
						zArchive.close();
						out.close();
						throw extract.getIOException();
					}

					doc = builder.parse(new ByteArrayInputStream(out.toByteArray()));
					break;
				}
			}
			zArchive.close();
			out.close();
			return doc;
		}

		return null;
	}

	protected void onExit() {
		result = Boolean.valueOf(extensiveJobThread != null && ((ImportThread)extensiveJobThread).getCustomFields() != null);
	}

	protected boolean isValidInput() {
		if (stepNr == 2) {
			return conditionsForStep3Ok();
		}
		return true;
	}

	private boolean conditionsForStep3Ok() {
		GeneralPurposeDialogs.initErrorDialog();
		atLeastOneProfileSelected();
		if (fullGames.getSelection()) {
			gameDirExists();
		}
		return !GeneralPurposeDialogs.displayErrorDialog(shell);
	}

	private void atLeastOneProfileSelected() {
		for (TableItem item: profilesTable.getItems()) {
			if (item.getChecked()) {
				return;
			}
		}
		GeneralPurposeDialogs.addError(settings.msg("dialog.import.required.oneprofiletoimport"), profilesTable);
	}

	private void gameDirExists() {
		for (TableItem item: profilesTable.getItems()) {
			if (item.getChecked()) {
				File f = FileUtils.canonicalToDosroot(new File(item.getText(1), item.getText(2)).getPath());
				if (f.exists()) {
					GeneralPurposeDialogs.addError(settings.msg("dialog.import.error.gamedatadirexists", new Object[] {f}), profilesTable);
				}
			}
		}
	}

	private void refillImportedDBVersionsList() {
		int idx = 0;
		for (DosboxVersion dbversion: dbSet) {
			int dbid = dbversion.getId();
			TableItem ti = impDbVersionsList.getItem(idx);
			Display d = shell.getDisplay();
			Color c = isUsed(dbid) ? null: d.getSystemColor(SWT.COLOR_GRAY);
			ti.setForeground(c);
			idx++;
		}
	}

	private boolean isUsed(int dbVersionId) {
		for (int i = 0; i < profs.size(); i++) {
			TableItem item = profilesTable.getItem(i);
			int dbid = profs.get(i).getDbversionId();
			if (item.getChecked() && dbVersionId == dbid)
				return true;
		}
		return false;
	}

	public static int getMappedDosboxVersionId(final Set<DosboxVersion> dbSet, final java.util.List<Integer> dbmapping, final int id) {
		int index = 0;
		for (DosboxVersion ver: dbSet) {
			if (ver.getId() == id)
				return dbmapping.get(index);
			index++;
		}
		return -1;
	}

	public static int getDosboxVersionId(final DosboxVersion dbversion, final Set<DosboxVersion> dbSet) {
		for (DosboxVersion d: dbSet)
			if (d.compareTo(dbversion) == 0)
				return d.getId();
		return -1;
	}

	protected void fillPages() {
		addStep(page1());
		addStep(page2());
		addStep(page3());
		addStep(page4());
		addStep(page5());
	}

	private Control page1() {
		final Group group1 = new Group(shell, SWT.NONE);
		group1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		group1.setText(settings.msg("dialog.import.step1"));
		group1.setLayout(new GridLayout(2, false));

		final Label titleLabel = new Label(group1, SWT.NONE);
		titleLabel.setText(settings.msg("dialog.export.exporttitle"));
		title = new Text(group1, SWT.BORDER);
		title.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		title.setEditable(false);

		final Label authorLabel = new Label(group1, SWT.NONE);
		authorLabel.setText(settings.msg("dialog.export.author"));
		author = new Text(group1, SWT.BORDER);
		author.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		author.setEditable(false);

		final Label notesLabel = new Label(group1, SWT.NONE);
		notesLabel.setText(settings.msg("dialog.export.notes"));
		notes = new Text(group1, SWT.WRAP | SWT.V_SCROLL | SWT.MULTI | SWT.H_SCROLL | SWT.BORDER | SWT.READ_ONLY);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.minimumHeight = 40;
		notes.setLayoutData(gridData);

		final Label logLabel = new Label(group1, SWT.NONE);
		logLabel.setText(settings.msg("dialog.import.log"));
		confLogText = new Text(group1, SWT.WRAP | SWT.V_SCROLL | SWT.MULTI | SWT.H_SCROLL | SWT.BORDER | SWT.READ_ONLY);
		GridData gridData2 = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData2.minimumHeight = 40;
		confLogText.setLayoutData(gridData2);

		if (packageTitle != null)
			title.setText(packageTitle);
		if (packageAuthor != null)
			author.setText(packageAuthor);
		if (packageNotes != null)
			notes.setText(packageNotes);
		confLogText.setText(messageLog.toString());

		return group1;
	}

	private Control page2() {
		final Group page2 = new Group(shell, SWT.NONE);
		page2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		page2.setText(settings.msg("dialog.import.step2"));
		page2.setLayout(new GridLayout(2, false));

		final Label importLabel = new Label(page2, SWT.NONE);
		importLabel.setText(settings.msg("dialog.import.import"));

		final Group group1 = new Group(page2, SWT.NONE);
		group1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		group1.setLayout(new GridLayout());

		settingsOnly = new Button(group1, SWT.RADIO);
		settingsOnly.setText(settings.msg("dialog.export.export.profiles"));
		fullGames = new Button(group1, SWT.RADIO);
		fullGames.setText(settings.msg("dialog.export.export.games"));

		new Label(page2, SWT.NONE);
		final Group group2 = new Group(page2, SWT.NONE);
		group2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		group2.setLayout(new GridLayout());
		incrementalSettingsButton = new Button(group2, SWT.RADIO);
		incrementalSettingsButton.setText(settings.msg("dialog.import.import.incrconf"));
		fullSettingsButton = new Button(group2, SWT.RADIO);
		fullSettingsButton.setText(settings.msg("dialog.import.import.fullconf"));
		incrementalSettingsButton.setSelection(true);

		new Label(page2, SWT.NONE);
		final Group group3 = new Group(page2, SWT.NONE);
		group3.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		group3.setLayout(new GridLayout());
		importCapturesButton = new Button(group3, SWT.CHECK);
		importCapturesButton.setText(settings.msg("dialog.template.captures"));
		importMapperfilesButton = new Button(group3, SWT.CHECK);
		importMapperfilesButton.setText(settings.msg("dialog.template.mapperfile"));
		importNativeCommandsButton = new Button(group3, SWT.CHECK);
		importNativeCommandsButton.setText(settings.msg("dialog.export.nativecommands"));

		new Label(page2, SWT.NONE);
		final Group group5 = new Group(page2, SWT.NONE);
		group5.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		group5.setLayout(new GridLayout());
		customValues = new Button(group5, SWT.CHECK);
		customValues.setText(settings.msg("dialog.import.import.customvalues"));
		customValues.setSelection(true);
		customFields = new Button(group5, SWT.CHECK);
		customFields.setText(settings.msg("dialog.import.import.customfields"));

		final Label confFileLabel = new Label(page2, SWT.NONE);
		confFileLabel.setText(settings.msg("dialog.main.profile.view.conf"));

		final Group group4 = new Group(page2, SWT.NONE);
		group4.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		group4.setLayout(new GridLayout());

		useOrgConf = new Button(group4, SWT.RADIO);
		useOrgConf.setText(settings.msg("dialog.import.useorgconf"));
		createNewConf = new Button(group4, SWT.RADIO);
		createNewConf.setText(settings.msg("dialog.import.createnewconf"));

		if (gamedataAvailable) {
			fullGames.setSelection(true);
		} else {
			fullGames.setEnabled(false);
			settingsOnly.setSelection(true);
		}

		if (settings.getSettings().getIntValue("profiledefaults", "confpath") == 1) {
			useOrgConf.setSelection(true);
		} else {
			useOrgConf.setEnabled(false);
			createNewConf.setEnabled(false);
		}

		if (capturesAvailable) {
			importCapturesButton.setSelection(true);
		} else {
			importCapturesButton.setEnabled(false);
		}

		if (mapperfilesAvailable) {
			importMapperfilesButton.setSelection(true);
		} else {
			importMapperfilesButton.setEnabled(false);
		}

		importNativeCommandsButton.setEnabled(nativecommandsAvailable);

		return page2;
	}

	private Control page3() {
		final Group profilesGroup = new Group(shell, SWT.NONE);
		profilesGroup.setText(settings.msg("dialog.import.step3"));
		profilesGroup.setLayout(new GridLayout(2, false));

		profilesTable = new Table(profilesGroup, SWT.FULL_SELECTION | SWT.CHECK | SWT.BORDER);
		profilesTable.setHeaderVisible(true);
		final GridData gridData_1 = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2);
		gridData_1.heightHint = 80;
		profilesTable.setLayoutData(gridData_1);
		profilesTable.setLinesVisible(true);

		final TableColumn title = new TableColumn(profilesTable, SWT.NONE);
		title.setWidth(260);
		title.setText(settings.msg("dialog.main.profiles.column.title"));
		final TableColumn basedir = new TableColumn(profilesTable, SWT.NONE);
		basedir.setWidth(100);
		basedir.setText(settings.msg("dialog.import.column.basedir"));
		final TableColumn subdir = new TableColumn(profilesTable, SWT.NONE);
		subdir.setWidth(120);
		subdir.setText(settings.msg("dialog.export.column.gamedir"));

		final Button allButton = new Button(profilesGroup, SWT.NONE);
		final GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
		allButton.setLayoutData(gridData);
		allButton.setText(settings.msg("button.all"));
		allButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				for (TableItem item: profilesTable.getItems())
					item.setChecked(true);
			}
		});

		final Button noneButton = new Button(profilesGroup, SWT.NONE);
		final GridData gridDataNone = new GridData(SWT.FILL, SWT.BOTTOM, true, false);
		noneButton.setLayoutData(gridDataNone);
		noneButton.setText(settings.msg("button.none"));
		noneButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				for (TableItem item: profilesTable.getItems())
					item.setChecked(false);
			}
		});

		final Composite buttonsGroup = new Composite(profilesGroup, SWT.NONE);
		buttonsGroup.setLayout(new RowLayout());

		final Button setBaseDirButton = new Button(buttonsGroup, SWT.NONE);
		setBaseDirButton.setText(settings.msg("button.setbasedir"));
		setBaseDirButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(shell);
				dialog.setFilterPath(FileUtils.canonicalToDosroot(".").getPath());
				String result = dialog.open();
				if (result != null) {
					File newBaseDir = FileUtils.makeRelativeToDosroot(new File(result));
					for (int i = 0; i < profilesTable.getItems().length; i++) {
						TableItem item = profilesTable.getItem(i);
						if (item.getChecked()) {
							profs.get(i).setBaseDir(newBaseDir);
							item.setText(1, newBaseDir.getPath());
						}
					}
				}
			}
		});

		final Button addGameTitleToBaseDirButton = new Button(buttonsGroup, SWT.NONE);
		addGameTitleToBaseDirButton.setText(settings.msg("button.addgametitletobasedir"));
		addGameTitleToBaseDirButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				for (int i = 0; i < profilesTable.getItems().length; i++) {
					TableItem item = profilesTable.getItem(i);
					if (item.getChecked()) {
						String gameTitle = item.getText(0).replaceAll(FileUtils.INVALID_FILENAME_CHARS_REGEXP, "");
						if (!item.getText(1).endsWith(gameTitle)) {
							File newFile = new File(item.getText(1), gameTitle);
							profs.get(i).setBaseDir(newFile);
							item.setText(1, newFile.getPath());
						}
					}
				}
			}
		});

		final Button removeGameTitleFromBaseDirButton = new Button(buttonsGroup, SWT.NONE);
		removeGameTitleFromBaseDirButton.setText(settings.msg("button.removegametitlefrombasedir"));
		removeGameTitleFromBaseDirButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				for (int i = 0; i < profilesTable.getItems().length; i++) {
					TableItem item = profilesTable.getItem(i);
					if (item.getChecked()) {
						String gameTitle = item.getText(0).replaceAll(FileUtils.INVALID_FILENAME_CHARS_REGEXP, "");
						if (item.getText(1).endsWith(gameTitle)) {
							File newFile = new File(item.getText(1)).getParentFile();
							profs.get(i).setBaseDir(newFile);
							item.setText(1, newFile.getPath());
						}
					}
				}
			}
		});

		profilesTable.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(final MouseEvent event) {
				int idx = profilesTable.getSelectionIndex();
				ExpProfile prof = profs.get(idx);
				DirectoryDialog dialog = new DirectoryDialog(shell);
				dialog.setFilterPath(FileUtils.canonicalToDosroot(prof.getBaseDir().getPath()).getPath());
				String result = dialog.open();
				if (result != null) {
					File newBaseDir = FileUtils.makeRelativeToDosroot(new File(result));
					prof.setBaseDir(newBaseDir);
					profilesTable.getSelection()[0].setText(1, newBaseDir.getPath());
				}
			}
		});

		for (ExpProfile prof: profs) {
			TableItem item = new TableItem(profilesTable, SWT.NONE);
			item.setText(prof.getTitle());
			item.setText(1, prof.getBaseDir().getPath());
			item.setText(2, prof.getGameDir().getPath());
			item.setChecked(true);
		}

		return profilesGroup;
	}

	private Control page4() {
		final Group dosboxVersionsGroup = new Group(shell, SWT.NONE);
		dosboxVersionsGroup.setLayout(new GridLayout(3, false));
		dosboxVersionsGroup.setText(settings.msg("dialog.import.step4"));
		dosboxVersionsGroup.setLayoutData(BorderLayout.NORTH);

		final Label importedVersionsLabel = new Label(dosboxVersionsGroup, SWT.NONE);
		importedVersionsLabel.setText(settings.msg("dialog.import.dosboxversioninimport"));

		final Label label_2 = new Label(dosboxVersionsGroup, SWT.SEPARATOR);
		label_2.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true, 1, 2));

		final Label configuredVersionsLabel = new Label(dosboxVersionsGroup, SWT.NONE);
		configuredVersionsLabel.setText(settings.msg("dialog.import.dosboxversioninstalled"));

		impDbVersionsList = new Table(dosboxVersionsGroup, SWT.FULL_SELECTION | SWT.BORDER);
		impDbVersionsList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		impDbVersionsList.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				int impIdx = impDbVersionsList.getSelectionIndex();
				int mappedId = dbmapping.get(impIdx);
				int myIdx = DosboxVersion.findIndexById(dbversionsList, mappedId);
				myDbVersionsList.select(myIdx);
			}
		});

		myDbVersionsList = new List(dosboxVersionsGroup, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		myDbVersionsList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		myDbVersionsList.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				int idx = impDbVersionsList.getSelectionIndex();
				if (idx != -1) {
					int myIdx = myDbVersionsList.getSelectionIndex();
					dbmapping.set(idx, dbversionsList.get(myIdx).getId());
				}
			}
		});

		for (DosboxVersion ver: dbversionsList)
			myDbVersionsList.add(ver.getTitle() + " (" + ver.getVersion() + ")");

		dbmapping = new ArrayList<Integer>();
		for (DosboxVersion dbversion: dbSet) {
			TableItem item = new TableItem(impDbVersionsList, SWT.NONE);
			item.setText(dbversion.getTitle() + " (" + dbversion.getVersion() + ")");
			dbmapping.add(dbversion.findBestMatchId(dbversionsList));
		}

		return dosboxVersionsGroup;
	}

	private Control page5() {
		final Group progressGroup = new Group(shell, SWT.NONE);
		progressGroup.setText(settings.msg("dialog.import.step5"));
		progressGroup.setLayout(new GridLayout());

		progressBar = new ProgressBar(progressGroup, SWT.NONE);
		progressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		profileLabel = new Label(progressGroup, SWT.NONE);
		profileLabel.setText(settings.msg("dialog.import.start"));

		logText = new Text(progressGroup, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.READ_ONLY | SWT.BORDER);
		logText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		return progressGroup;
	}
}
