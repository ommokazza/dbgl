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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import swing2swt.layout.BorderLayout;
import org.eclipse.swt.widgets.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.apache.commons.lang3.StringUtils;
import org.dbgl.connect.Messaging;
import org.dbgl.db.Database;
import org.dbgl.gui.ProfilesList.ProfilesListItem;
import org.dbgl.gui.ProfilesList.ProfilesListType;
import org.dbgl.interfaces.ReOrderable;
import org.dbgl.model.DosboxVersion;
import org.dbgl.model.ExpTemplate;
import org.dbgl.model.Filter;
import org.dbgl.model.NativeCommand;
import org.dbgl.model.OrderingVector;
import org.dbgl.model.Profile;
import org.dbgl.model.SearchResult;
import org.dbgl.model.SearchResult.ResultType;
import org.dbgl.model.Template;
import org.dbgl.model.ThumbInfo;
import org.dbgl.model.Constants;
import org.dbgl.model.ViewType;
import org.dbgl.model.conf.Conf;
import org.dbgl.model.conf.SectionsWrapper;
import org.dbgl.model.conf.Settings;
import org.dbgl.swtdesigner.SWTImageManager;
import org.dbgl.util.FileUtils;
import org.dbgl.util.PlatformUtils;
import org.dbgl.util.StringRelatedUtils;
import org.dbgl.util.XmlUtils;


public final class MainWindow implements ReOrderable {

	private static final String[] ICOS_DBGL = {"ico/016.png", "ico/024.png", "ico/032.png", "ico/048.png", "ico/064.png", "ico/128.png", "ico/256.png"};

	static String[] columnNames;

	private Display display;
	private Shell shell;
	private OrderingVector orderingVector = null;
	private String filterClause = null;
	private java.util.List<Profile> profilesList;
	private java.util.List<DosboxVersion> dbversionsList;
	private java.util.List<Template> templatesList;
	private java.util.List<Filter> filtersList;
	private Database dbase = null;
	private Settings settings = null;
	private SectionsWrapper ssettings = null;
	private File currentThumbFile = null;
	private int thumbHeight;
	private ToolItem displayNotes, displayScreenshots;

	private int[] columnIds;
	private ProfilesList profile_table;
	private Table dbversion_table, template_table;
	private CTabFolder filterFolder;
	private ToolItem setupToolItem, viewSelector;
	private Text notesField;
	private Composite thumbsToolBar;
	private Menu menu_thumb;
	private Link[] link;
	private Menu viewProfileSubMenu, viewDosboxSubMenu, viewTemplateSubMenu;

	public void open() {
		dbase = Database.getInstance();
		settings = Settings.getInstance();
		ssettings = settings.getSettings();

		if (PlatformUtils.IS_OSX) {
			Display.setAppName("DBGL");
			Display.setAppVersion(Constants.PROGRAM_VERSION);
		}
		display = Display.getDefault();
		createContents();

		if (Database.isInitializedNewDatabase()) {
			doLocateDosbox(false);
			if (dbversionsList.size() > 0)
				doImportDefaultTemplates(false);
		}

		Messaging mess = null;
		if (ssettings.getBooleanValue("communication", "port_enabled")) {
			mess = new Messaging(ssettings.getIntValue("communication", "port"), this);
			mess.start();
		}

		shell.open();
		shell.layout();

		profile_table.setSelection(ssettings.getIntValue("gui", "selectedprofile"));
		displayProfileInformation(false);

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		SWTImageManager.dispose();
		display.dispose();

		if (mess != null) {
			mess.close();
		}

		try {
			settings.save();
		} catch (IOException e) {
			GeneralPurposeDialogs.warningMessage(shell, e);
		}
		try {
			dbase.shutdown();
		} catch (SQLException e) {
			// nothing we can do
		}
	}

	private void createContents() {
		initColumnIds();
		orderingVector = new OrderingVector(ssettings.getIntValues("gui", "sortcolumn"), ssettings.getBooleanValues("gui", "sortascending"));

		shell = new Shell();
		shell.setImages(SWTImageManager.getResourceImages(display, ICOS_DBGL));

		try {
			java.util.List<Profile> invalidProfiles = dbase.findInvalidProfiles();
			if (invalidProfiles.size() > 0) {
				java.util.List<String> titles = new ArrayList<String>();
				for (Profile p: invalidProfiles)
					titles.add(p.getTitle());
				if (GeneralPurposeDialogs.confirmMessage(shell, settings.msg("dialog.main.confirm.removeinvalidprofiles", new Object[] {invalidProfiles.size(), StringUtils.join(titles, ", ")}))) {
					for (Profile prof: invalidProfiles)
						dbase.removeProfile(prof);
				}
			}

			dbversionsList = dbase.readDosboxVersionsList();
			templatesList = dbase.readTemplatesList();
			filtersList = dbase.readFiltersList();
			filtersList.add(0, new Filter(-1, settings.msg("dialog.main.allprofiles"), null));
			filterClause = filtersList.get(ssettings.getIntValue("gui", "filtertab")).getFilter();
			profilesList = dbase.readProfilesList(orderingVector.toClause(), filterClause);
		} catch (SQLException e) {
			GeneralPurposeDialogs.warningMessage(shell, e);
		}
		shell.addControlListener(new ControlAdapter() {
			public void controlResized(final ControlEvent event) {
				boolean isMaximized = shell.getMaximized();
				if (!isMaximized) {
					Rectangle rec = shell.getBounds();
					ssettings.setIntValue("gui", "width", rec.width);
					ssettings.setIntValue("gui", "height", rec.height);
					ssettings.setIntValue("gui", "x", rec.x);
					ssettings.setIntValue("gui", "y", rec.y);
				}
				ssettings.setBooleanValue("gui", "maximized", isMaximized);
			}
		});
		shell.addControlListener(new ControlAdapter() {
			public void controlMoved(final ControlEvent event) {
				if (!shell.getMaximized()) {
					Rectangle rec = shell.getBounds();
					ssettings.setIntValue("gui", "x", rec.x);
					ssettings.setIntValue("gui", "y", rec.y);
				}
			}
		});
		shell.setLayout(new BorderLayout(0, 0));
		shell.setLocation(ssettings.getIntValue("gui", "x"), ssettings.getIntValue("gui", "y"));
		if (ssettings.getBooleanValue("gui", "maximized")) {
			shell.setMaximized(true);
		} else {
			shell.setSize(ssettings.getIntValue("gui", "width"), ssettings.getIntValue("gui", "height"));
		}
		shell.setText(settings.msg("main.title", new Object[] {Constants.PROGRAM_VERSION}));

		createFileMenu();
		final TabFolder tabFolder = new TabFolder(shell, SWT.NONE);
		createProfilesTab(tabFolder);
		createDosboxVersionsTab(tabFolder);
		createTemplatesTab(tabFolder);

		shell.addListener(SWT.Activate, new Listener() {
			public void handleEvent(Event arg0) {
				if (tabFolder.getSelectionIndex() == 0) {
					profile_table.setFocus();
					displayProfileInformation(true);
				}
			}
		});

		// init values
		for (Profile prof: profilesList) {
			addProfileToTable(prof);
		}
		for (DosboxVersion dbversion: dbversionsList) {
			addDosboxVersionToTable(dbversion);
		}
		for (Template template: templatesList) {
			addTemplateToTable(template);
		}

		profile_table.setFocus();
	}

	public void doReorder(final int columnId, final int dir) {
		Set<Integer> selectedProfiles = getSelectedProfileIds();
		try {
			orderingVector.addOrdering(columnIds[columnId], dir == SWT.UP);
			profilesList = dbase.readProfilesList(orderingVector.toClause(), filterClause);
		} catch (SQLException e) {
			GeneralPurposeDialogs.warningMessage(shell, e);
		}
		for (int i = 0; i < profilesList.size(); i++) {
			setTableItem(profile_table.getItem(i), profilesList.get(i));
		}
		profile_table.setSelection(getIndicesByIds(selectedProfiles));
		ssettings.setIntValues("gui", "sortcolumn", orderingVector.getColumns());
		ssettings.setBooleanValues("gui", "sortascending", orderingVector.getAscendings());
	}

	private void displayLinks(final String[] p_link, final String[] p_linkTitle) {
		for (int i = 0; i < link.length; i++) {
			if (p_link[i] == null || "".equals(p_link[i])) {
				link[i].setText("");
				link[i].setToolTipText(null);
				((GridData)link[i].getLayoutData()).exclude = true;
				link[i].pack();

			} else {
				String url = p_link[i];
				String tag = p_link[i];
				if (url.indexOf("://") == -1) {
					url = "file://" + FileUtils.canonicalToData(url).getPath();
					tag = FileUtils.makeRelativeToDosroot(FileUtils.canonicalToData(tag)).getPath();
				}
				if (p_linkTitle[i] != null && !"".equals(p_linkTitle[i]))
					tag = p_linkTitle[i];
				StringBuffer text = new StringBuffer("<a href=\"").append(url).append("\">").append(tag).append("</a>");
				link[i].setText(text.toString());
				link[i].setToolTipText(url);
				((GridData)link[i].getLayoutData()).exclude = false;
			}
		}
		link[0].getParent().layout();
		link[0].getParent().getParent().layout();
	}

	private void displayScreenshots(final Profile prof, ProfilesListItem profilesListItem, boolean forceRefresh) {
		for (Control c: thumbsToolBar.getChildren()) {
			c.setMenu(null);
			c.dispose();
		}
		thumbsToolBar.pack();
		if (prof != null) {
			ThumbInfo thumbInfo = (ThumbInfo)profilesListItem.getData();
			if (forceRefresh)
				thumbInfo.resetCachedInfo();
			File[] files = thumbInfo.getAllThumbs();
			for (File file: files) {
				String label = file.getName().toLowerCase();
				if (ssettings.getBooleanValue("gui", "screenshotsfilename")) {
					label = ' ' + label.substring(0, label.lastIndexOf('.')) + ' ';
				} else {
					label = null;
				}
				final Button buttonItem = new Button(thumbsToolBar, SWT.FLAT);
				buttonItem.setToolTipText(file.getPath());
				buttonItem.setImage(SWTImageManager.getResizedImage(display, thumbHeight, file.getPath(), label));
				buttonItem.pack();
				buttonItem.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(final SelectionEvent event) {
						final Thumb thumbDialog = new Thumb(shell);
						thumbDialog.setThumb(buttonItem.getToolTipText());
						thumbDialog.open();
					}
				});
				buttonItem.addMouseListener(new MouseAdapter() {
					public void mouseDown(MouseEvent arg0) {
						currentThumbFile = new File(buttonItem.getToolTipText());
					}
				});
				buttonItem.setMenu(menu_thumb);
			}
			if (!forceRefresh && thumbInfo.isUpdated())
				profilesListItem.setData(thumbInfo);
		}
		thumbsToolBar.setVisible(thumbsToolBar.getChildren().length != 0);
		thumbsToolBar.pack();
	}

	private void doAddProfile() {
		doAddProfile(null);
	}

	private void doAddProfile(final String filename) {
		if (checkDefaultDBVersion() == null) {
			return;
		}
		if (filename == null || FileUtils.isBooterImage(filename) || FileUtils.isExecutable(filename) || FileUtils.isConfFile(filename)) {
			EditProfileDialog addProfileDialog = new EditProfileDialog(shell);
			if (filename != null)
				addProfileDialog.sendToProfile(filename);
			Object obj = addProfileDialog.open();
			if (obj instanceof Profile)
				updateWithAddedProfile((Profile)obj);
		} else if (FileUtils.isArchive(filename)) {
			ImportDialog importDialog = new ImportDialog(shell, dbversionsList, new File(filename));
			Boolean updateCustomFields = (Boolean)importDialog.open();
			if (updateCustomFields != null) {
				if (updateCustomFields) {
					rebuildProfilesTable();
				} else {
					updateProfilesList(getSelectedProfileIds());
				}
				displayProfileInformation(true);
			}
		} else {
			GeneralPurposeDialogs.warningMessage(shell, settings.msg("general.error.cannotimportunknownfile"));
		}
	}

	public void addProfile(final String file) {
		display.syncExec(new Runnable() {
			public void run() {
				doAddProfile(file);
			}
		});
	}

	private void doAddDosboxVersion() {
		final EditDosboxVersionDialog addVersionDialog = new EditDosboxVersionDialog(shell, DosboxVersion.findDefault(dbversionsList) == null);
		DosboxVersion dbversion = (DosboxVersion)addVersionDialog.open();
		if (dbversion != null) {
			updateDosboxVersionList(dbversion);
		}
	}

	private void doAddTemplate() {
		if (checkDefaultDBVersion() == null) {
			return;
		}
		final EditTemplateDialog addTemplateDialog = new EditTemplateDialog(shell);
		Template template = (Template)addTemplateDialog.open();
		if (template != null) {
			updateTemplateList(template);
		}
	}

	private void doAddFilter() {
		final EditFilterDialog addFilterDialog = new EditFilterDialog(shell);
		if (profile_table.getSelectionCount() > 1)
			addFilterDialog.setIds(getSelectedProfileIds());
		Filter filter = (Filter)addFilterDialog.open();
		if (filter != null) {
			filtersList.add(filter);
			addFilterTab(filter).setControl(profile_table.getControl());
			filterFolder.setSelection(filterFolder.getItemCount() - 1);
			updateProfilesAfterTabAction();
		}
	}

	private void doToggleFavoriteProfile() {
		int index = profile_table.getSelectionIndex();
		if (index != -1) {
			Profile profile = profilesList.get(index);
			try {
				dbase.toggleFavorite(profile.getId());
			} catch (SQLException e) {
				GeneralPurposeDialogs.warningMessage(shell, e);
			}
			profile.toggleDefault();
			profilesList.set(index, profile);
			setTableItem(profile_table.getItem(index), profile);
		}
	}

	private void doToggleDefaultVersion() {
		int index = dbversion_table.getSelectionIndex();
		if (index != -1) {
			DosboxVersion ver = dbversionsList.get(index);
			ver.toggleDefault();
			try {
				dbase.addOrEditDosboxVersion(ver.getTitle(), ver.getPath(), ver.getConf(), ver.isMultiConfig(), ver.isUsingCurses(), ver.isDefault(), ver.getParameters(), ver.getVersion(),
					ver.getId());
			} catch (SQLException e) {
				GeneralPurposeDialogs.warningMessage(shell, e);
			}
			updateDosboxVersionList(ver);
		}
	}

	private void doToggleDefaultTemplate() {
		int index = template_table.getSelectionIndex();
		if (index != -1) {
			Template temp = templatesList.get(index);
			temp.toggleDefault();
			try {
				dbase.addOrEditTemplate(temp.getTitle(), temp.getDbversionId(), temp.isDefault(), temp.getId());
			} catch (SQLException e) {
				GeneralPurposeDialogs.warningMessage(shell, e);
			}
			updateTemplateList(temp);
		}
	}

	private void doEditProfile() {
		doEditProfile(false);
	}

	private void doEditProfile(final boolean focusTitle) {
		int index = profile_table.getSelectionIndex();
		if (index != -1) {
			if (profile_table.getSelectionCount() > 1) {
				ProfileLoader pLoader = new ProfileLoader(shell, getSelectedProfiles(), true);
				if (pLoader.open() != null) {
					final EditProfileDialog editMultiProfileDialog = new EditProfileDialog(shell);
					editMultiProfileDialog.setConfigurables(pLoader.getResultAsConfigurables());
					editMultiProfileDialog.setMultiProfileCombined(pLoader.getMultiProfileCombined());
					if (editMultiProfileDialog.open() != null) {
						updateProfilesList(getSelectedProfileIds());
						displayProfileInformation(false);
					}
				}
			} else {
				final EditProfileDialog editProfileDialog = new EditProfileDialog(shell);
				editProfileDialog.setProfile(profilesList.get(index));
				if (focusTitle) {
					editProfileDialog.focusTitle();
				}
				Profile profile = (Profile)editProfileDialog.open();
				if (profile != null) {
					updateProfileListAfterEdit(index, profile);
					displayProfileInformation(false);
				}
			}
		}
	}

	private void updateProfileListAfterEdit(int index, Profile profile) {
		boolean quickUpdate = true;
		if (ssettings.getBooleanValue("gui", "autosortonupdate") || (filterFolder.getSelectionIndex() > 0)) {
			try {
				profilesList = dbase.readProfilesList(orderingVector.toClause(), filterClause);
				if (index != Profile.findIndexById(profilesList, profile.getId())) {
					quickUpdate = false;
				}
			} catch (SQLException e) {
				GeneralPurposeDialogs.warningMessage(shell, e);
			}
		}
		if (quickUpdate) {
			profilesList.set(index, profile);
			setTableItem(profile_table.getItem(index), profile);
		} else {
			updateProfilesList(new HashSet<Integer>(Arrays.asList(profile.getId())));
		}
	}

	private void doDuplicateProfile() {
		int index = profile_table.getSelectionIndex();
		if (index != -1) {
			Profile orgProf = profilesList.get(index);
			updateWithAddedProfile(EditProfileDialog.duplicateProfile(orgProf, dbversionsList, dbase, shell));
		}
	}

	private void doDuplicateTemplate() {
		int index = template_table.getSelectionIndex();
		if (index != -1) {
			Template orgTemplate = templatesList.get(index);
			Template newTemplate = EditProfileDialog.duplicateTemplate(orgTemplate, dbversionsList, dbase, shell);
			if (newTemplate != null) {
				updateTemplateList(newTemplate);
			}
		}
	}

	private void updateWithAddedProfile(final Profile profile) {
		if (profile != null) {
			if (ssettings.getBooleanValue("gui", "autosortonupdate") || (filterFolder.getSelectionIndex() > 0)) {
				updateProfilesList(new HashSet<Integer>(Arrays.asList(profile.getId())));
			} else {
				profilesList.add(profile);
				addProfileToTable(profile);
				profile_table.setSelection(profile_table.getItemCount() - 1);
				profile_table.setFocus();
			}
			displayProfileInformation(false);
		}
	}

	private void doEditDosboxVersion() {
		int index = dbversion_table.getSelectionIndex();
		if (index != -1) {
			final EditDosboxVersionDialog editVersionDialog = new EditDosboxVersionDialog(shell, false);
			editVersionDialog.setDosboxVersion(dbversionsList.get(index));
			DosboxVersion dbversion = (DosboxVersion)editVersionDialog.open();
			if (dbversion != null) {
				updateDosboxVersionList(dbversion);
			}
		}
	}

	private void doEditFilter() {
		int index = filterFolder.getSelectionIndex();
		if (index > 0) {
			final EditFilterDialog editFilterDialog = new EditFilterDialog(shell);
			editFilterDialog.setFilter(filtersList.get(index));
			Filter filter = (Filter)editFilterDialog.open();
			if (filter != null) {
				filtersList.set(index, filter);
				filterFolder.getSelection().setText("    " + filter.getTitle() + "    ");
				updateProfilesAfterTabAction();
			}
		}
	}

	private void updateProfilesList(final Set<Integer> profileIds) {
		try {
			profilesList = dbase.readProfilesList(orderingVector.toClause(), filterClause);
		} catch (SQLException e) {
			GeneralPurposeDialogs.warningMessage(shell, e);
		}
		profile_table.setRedraw(false);
		profile_table.removeAll();
		for (Profile prof: profilesList) {
			addProfileToTable(prof);
		}
		profile_table.setSelection(getIndicesByIds(profileIds));
		profile_table.setRedraw(true);
		profile_table.setFocus();
	}

	private void updateDosboxVersionList(final DosboxVersion dbversion) {
		try {
			dbversionsList = dbase.readDosboxVersionsList();
		} catch (SQLException e) {
			GeneralPurposeDialogs.warningMessage(shell, e);
		}
		dbversion_table.removeAll();
		for (DosboxVersion version: dbversionsList) {
			addDosboxVersionToTable(version);
		}
		dbversion_table.setSelection(DosboxVersion.findIndexById(dbversionsList, dbversion.getId()));
		dbversion_table.setFocus();
	}

	private void updateTemplateList(final Template template) {
		try {
			templatesList = dbase.readTemplatesList();
		} catch (SQLException e) {
			GeneralPurposeDialogs.warningMessage(shell, e);
		}
		template_table.removeAll();
		for (Template temp: templatesList) {
			addTemplateToTable(temp);
		}
		template_table.setSelection(Template.findIndexById(templatesList, template.getId()));
		template_table.setFocus();
	}

	private void doEditTemplate() {
		int index = template_table.getSelectionIndex();
		if (index != -1) {
			final EditTemplateDialog editTemplDialog = new EditTemplateDialog(shell);
			editTemplDialog.setTemplate(templatesList.get(index));
			Template template = (Template)editTemplDialog.open();
			if (template != null) {
				updateTemplateList(template);
			}
		}
	}

	private void doRemoveProfile() {
		int index = profile_table.getSelectionIndex();
		if ((index != -1)) {
			DeleteProfilesDialog dpDialog = new DeleteProfilesDialog(shell);
			dpDialog.setProfilesToBeDeleted(getSelectedProfiles(), dbversionsList);
			if (dpDialog.open() != null) {
				int[] idxs = profile_table.getSelectionIndices();
				Arrays.sort(idxs);
				for (int i = idxs.length - 1; i >= 0; i--) {
					profile_table.remove(idxs[i]);
					profilesList.remove(idxs[i]);
				}
				if (idxs[0] > 0)
					profile_table.setSelection(idxs[0] - 1);
				displayProfileInformation(false);
			}
		}
	}

	private void doRemoveDosboxVersion() {
		int index = dbversion_table.getSelectionIndex();
		if ((index != -1) && GeneralPurposeDialogs.confirmMessage(shell, settings.msg("dialog.main.confirm.removedosboxversion"))) {
			try {
				dbase.removeDosboxVersion((dbversionsList.get(index)));
				dbversion_table.remove(index);
				dbversionsList.remove(index);
			} catch (SQLException e) {
				GeneralPurposeDialogs.warningMessage(shell, e);
			}
		}
	}

	private void doRemoveTemplate() {
		int index = template_table.getSelectionIndex();
		if ((index != -1) && GeneralPurposeDialogs.confirmMessage(shell, settings.msg("dialog.main.confirm.removetemplate"))) {
			Template template = templatesList.get(index);
			try {
				dbase.startTransaction();
				dbase.removeNativeCommands(-1, template.getId());
				dbase.removeTemplate(template);
				dbase.commitTransaction();
				template_table.remove(index);
				templatesList.remove(index);
				File conffile = FileUtils.constructCanonicalTemplateFileLocation(template.getId());
				if (GeneralPurposeDialogs.confirmMessage(shell, settings.msg("dialog.main.confirm.removetemplateconf", new Object[] {conffile}))) {
					FileUtils.removeFile(conffile);
				}
			} catch (SQLException e) {
				try {
					dbase.rollbackTransaction();
				} catch (SQLException se) {
					GeneralPurposeDialogs.warningMessage(shell, se);
				}
				GeneralPurposeDialogs.warningMessage(shell, e);
			} finally {
				dbase.finishTransaction();
			}
		}
	}

	private void addProfileToTable(final Profile prof) {
		final ProfilesListItem newItemTableItem = profile_table.new ProfilesListItem(profile_table);
		setTableItem(newItemTableItem, prof);
	}

	private void addDosboxVersionToTable(final DosboxVersion dbversion) {
		final TableItem newItemTableItem = new TableItem(dbversion_table, SWT.BORDER);
		setTableItem(newItemTableItem, dbversion);
	}

	private void addTemplateToTable(final Template template) {
		final TableItem newItemTableItem = new TableItem(template_table, SWT.BORDER);
		setTableItem(newItemTableItem, template);
	}

	private void setTableItem(final ProfilesListItem newItemTableItem, final Profile prof) {
		for (int i = 0; i < columnIds.length; i++) {
			String value;
			switch (columnIds[i]) {
				case 0:
					value = prof.getTitle();
					break;
				case 1:
					value = prof.hasSetupString();
					break;
				case 2:
					value = prof.getDeveloperName();
					break;
				case 3:
					value = prof.getPublisherName();
					break;
				case 4:
					value = prof.getGenre();
					break;
				case 5:
					value = prof.getYear();
					break;
				case 6:
					value = prof.getStatus();
					break;
				case 7:
					value = prof.isDefaultString();
					break;
				case 8:
					value = String.valueOf(prof.getId());
					break;
				case 9:
					value = String.valueOf(prof.getDbversionId());
					break;
				case 10:
				case 11:
				case 12:
				case 13:
				case 14:
				case 15:
				case 16:
				case 17:
					value = prof.getCustomString(columnIds[i] - Constants.RO_COLUMN_NAMES);
					break;
				case 18:
					value = prof.getCustomInt(0) + " %";
					break;
				case 19:
					value = String.valueOf(prof.getCustomInt(1));
					break;
				case 21:
					value = DosboxVersion.findById(dbversionsList, prof.getDbversionId()).getTitle();
					break;
				case 22:
					value = Settings.toString(prof.getStats().getCreated(), DateFormat.SHORT);
					break;
				case 23:
					value = Settings.toString(prof.getStats().getModified(), DateFormat.SHORT);
					break;
				case 24:
					value = Settings.toString(prof.getStats().getLastrun(), DateFormat.SHORT);
					break;
				case 25:
					value = Settings.toString(prof.getStats().getLastsetup(), DateFormat.SHORT);
					break;
				case 26:
					value = String.valueOf(prof.getStats().getRuns());
					break;
				case 27:
					value = String.valueOf(prof.getStats().getSetups());
					break;
				default:
					value = "";
			}
			if (columnIds[i] != 20) {
				newItemTableItem.setText(i, columnIds[i], value);
			}
		}
		newItemTableItem.setData(new ThumbInfo(prof.getCaptures()));
	}

	private void setTableItem(final TableItem newItemTableItem, final DosboxVersion dbversion) {
		newItemTableItem.setText(0, dbversion.getTitle());
		newItemTableItem.setText(1, dbversion.getVersion());
		newItemTableItem.setText(2, dbversion.getPath());
		newItemTableItem.setText(3, dbversion.isDefaultString());
		newItemTableItem.setText(4, String.valueOf(dbversion.getId()));
		newItemTableItem.setText(5, Settings.toString(dbversion.getStats().getCreated(), DateFormat.SHORT));
		newItemTableItem.setText(6, Settings.toString(dbversion.getStats().getModified(), DateFormat.SHORT));
		newItemTableItem.setText(7, Settings.toString(dbversion.getStats().getLastrun(), DateFormat.SHORT));
		newItemTableItem.setText(8, String.valueOf(dbversion.getStats().getRuns()));
	}

	private void setTableItem(final TableItem newItemTableItem, final Template template) {
		newItemTableItem.setText(0, template.getTitle());
		newItemTableItem.setText(1, template.isDefaultString());
		newItemTableItem.setText(2, String.valueOf(template.getId()));
		newItemTableItem.setText(3, Settings.toString(template.getStats().getCreated(), DateFormat.SHORT));
		newItemTableItem.setText(4, Settings.toString(template.getStats().getModified(), DateFormat.SHORT));
		newItemTableItem.setText(5, Settings.toString(template.getStats().getLastrun(), DateFormat.SHORT));
		newItemTableItem.setText(6, String.valueOf(template.getStats().getRuns()));
	}

	private void doRunProfile(final int setup, final boolean prepareOnly) {
		int index = profile_table.getSelectionIndex();
		if (index != -1) {
			Profile prof = profilesList.get(index);
			if (setup == -1 || prof.hasSetup(setup)) {
				try {
					java.util.List<NativeCommand> cmds = dbase.readNativeCommandsList(prof.getId(), -1);
					FileUtils.doRunProfile(prof, dbversionsList, getEnv(ssettings), setup, prepareOnly, cmds, display);
					if (setup == -1)
						prof = dbase.runProfile(prof);
					else
						prof = dbase.setupProfile(prof);
					updateProfileListAfterEdit(index, prof);
				} catch (IOException | SQLException e) {
					GeneralPurposeDialogs.warningMessage(shell, e);
				}
			}
		}
	}

	private void doRunDosbox() {
		int index = dbversion_table.getSelectionIndex();
		if (index != -1) {
			DosboxVersion dbversion = dbversionsList.get(index);
			try {
				FileUtils.doRunDosbox(dbversion, getEnv(ssettings));
				dbase.runDosboxVersion(dbversion);
				updateDosboxVersionList(dbversion);
			} catch (IOException | SQLException e) {
				GeneralPurposeDialogs.warningMessage(shell, e);
			}
		}
	}

	private void doRunTemplate() {
		int index = template_table.getSelectionIndex();
		if (index != -1) {
			Template template = templatesList.get(index);
			try {
				java.util.List<NativeCommand> cmds = dbase.readNativeCommandsList(-1, template.getId());
				FileUtils.doRunTemplate(template, dbversionsList, getEnv(ssettings), cmds, display);
				dbase.runTemplate(template);
				updateTemplateList(template);
			} catch (IOException | SQLException e) {
				GeneralPurposeDialogs.warningMessage(shell, e);
			}
		}
	}

	private DosboxVersion checkDefaultDBVersion() {
		DosboxVersion dbv = DosboxVersion.findDefault(dbversionsList);
		if (dbv == null) {
			GeneralPurposeDialogs.infoMessage(shell, settings.msg("dialog.main.required.defaultdosboxversion"));
		}
		return dbv;
	}

	private void doDFendImport() {
		DosboxVersion defaultDbversion = checkDefaultDBVersion();
		if (defaultDbversion == null) {
			return;
		}

		if (ssettings.getIntValue("profiledefaults", "confpath") == 1) {
			GeneralPurposeDialogs.infoMessage(shell, settings.msg("dialog.main.notice.dfendimportconflocation", new Object[] {SettingsDialog.confLocations[0]}));
		}

		DFendImportDialog importDialog = new DFendImportDialog(shell);
		importDialog.setDefaultDosboxVersion(defaultDbversion);
		if (importDialog.open() != null) {
			updateProfilesList(getSelectedProfileIds());
			displayProfileInformation(true);
		}
	}

	private void doMigrate() {
		GeneralPurposeDialogs.infoMessage(shell, settings.msg("dialog.main.notice.premigration"));
		String from = (String)new MigrateDialog(shell).open();
		if (from != null) {
			updateProfilesList(getSelectedProfileIds());
			displayProfileInformation(true);
			GeneralPurposeDialogs.infoMessage(shell, settings.msg("dialog.main.notice.postmigration", new Object[] {from, FileUtils.getDosRoot()}));
		}
	}

	private void doLocateDosbox(final boolean interactive) {
		SearchResult result = PlatformUtils.findDosbox();
		if (result.result == ResultType.NOTFOUND) {
			GeneralPurposeDialogs.warningMessage(shell, settings.msg("dialog.locatedosbox.notice.notfound"));
			return;
		}

		if (result.result == ResultType.COMPLETE && !interactive) {
			try {
				DosboxVersion newDbversion = dbase.addOrEditDosboxVersion(result.dosbox.getTitle(), result.dosbox.getPath(), result.dosbox.getConf(), result.dosbox.isMultiConfig(),
					result.dosbox.isUsingCurses(), result.dosbox.isDefault(), result.dosbox.getParameters(), result.dosbox.getVersion(), result.dosbox.getId());
				updateDosboxVersionList(newDbversion);
			} catch (SQLException e) {
				GeneralPurposeDialogs.warningMessage(shell, e);
			}
		} else {
			final EditDosboxVersionDialog addVersionDialog = new EditDosboxVersionDialog(shell, DosboxVersion.findDefault(dbversionsList) == null);
			addVersionDialog.setDosboxVersion(result.dosbox);
			DosboxVersion dbversion = (DosboxVersion)addVersionDialog.open();
			if (dbversion != null) {
				updateDosboxVersionList(dbversion);
			}
		}
	}

	private void displayProfileInformation(boolean forceRefresh) {
		int index = profile_table.getSelectionIndex();
		if (index == -1) {
			if (displayScreenshots.getSelection())
				displayScreenshots(null, null, forceRefresh);
			if (displayNotes.getSelection()) {
				notesField.setText("");
				displayLinks(new String[] {null, null, null, null, null, null, null, null}, new String[] {null, null, null, null, null, null, null, null});
			}
			updateViewProfileSubmenu(null);
			setupToolItem.setEnabled(false);
		} else {
			ssettings.setIntValue("gui", "selectedprofile", index);
			Profile prof = profilesList.get(index);
			if (displayScreenshots.getSelection()) {
				displayScreenshots(prof, profile_table.getItem(index), forceRefresh);
			} else {
				if (forceRefresh) {
					ThumbInfo thumbInfo = (ThumbInfo)profile_table.getItem(index).getData();
					thumbInfo.resetCachedInfo();
				}
			}

			if (forceRefresh)
				profile_table.redraw();
			if (displayNotes.getSelection()) {
				notesField.setText(prof.getNotes());
				displayLinks(prof.getLinks(), prof.getLinkTitles());
			}
			updateViewProfileSubmenu(prof);
			setupToolItem.setEnabled(prof.hasSetup(0));
		}
	}

	private void doRemoveThumb() {
		if (GeneralPurposeDialogs.confirmMessage(shell, settings.msg("dialog.main.confirm.removethumb", new Object[] {currentThumbFile}))) {
			FileUtils.removeFile(currentThumbFile);
			SWTImageManager.flush(currentThumbFile.getPath());
			displayProfileInformation(true);
		}
		currentThumbFile = null;
	}

	private void doCreateShortcut() {
		int[] selectedProfiles = profile_table.getSelectionIndices();
		for (int i = 0; i < selectedProfiles.length; i++) {
			try {
				PlatformUtils.createShortcut(profilesList.get(selectedProfiles[i]), dbversionsList);
			} catch (IOException e) {
				GeneralPurposeDialogs.warningMessage(shell, e);
			}
		}
	}

	private void addDBColumn(final String title, final int colIndex) {
		final String width = "column2_" + (colIndex + 1) + "width";
		final TableColumn column = new TableColumn(dbversion_table, SWT.NONE);
		column.setWidth(ssettings.getIntValue("gui", width));
		column.setText(title);
		column.addControlListener(new ControlAdapter() {
			public void controlResized(final ControlEvent event) {
				ssettings.setIntValue("gui", width, column.getWidth());
			}
		});
	}

	private void addTemplateColumn(final String title, final int colIndex) {
		final String width = "column3_" + (colIndex + 1) + "width";
		final TableColumn column = new TableColumn(template_table, SWT.NONE);
		column.setWidth(ssettings.getIntValue("gui", width));
		column.setText(title);
		column.addControlListener(new ControlAdapter() {
			public void controlResized(final ControlEvent event) {
				ssettings.setIntValue("gui", width, column.getWidth());
			}
		});
	}

	private void doOpenSettingsDialog() {
		SettingsDialog sDialog = new SettingsDialog(shell);
		if (((Boolean)sDialog.open())) {
			rebuildProfilesTable();
		}
		notesField.setFont(GeneralPurposeGUI.stringToFont(display, notesField.getFont(), ssettings.getValues("gui", "notesfont")));
	}
	
	private void doOpenLogDialog() {
		new LogDialog(shell).open();
	}

	private void rebuildProfilesTable() {
		Set<Integer> selectedProfiles = getSelectedProfileIds();
		initColumnIds();
		disposeProfilesList();
		constructProfilesList();
		updateProfilesList(selectedProfiles);
	}

	private void doImportConfigfile() {
		FileDialog dialog = new FileDialog(shell, SWT.OPEN);
		dialog.setFilterNames(new String[] {settings.msg("filetype.conf"), settings.msg("filetype.exe") + ", " + settings.msg("filetype.booterimage"), FileUtils.ALL_FILTER});
		dialog.setFilterExtensions(new String[] {FileUtils.CNF_FILTER, FileUtils.EXE_FILTER + ";" + FileUtils.BTR_FILTER, FileUtils.ALL_FILTER});
		String result = dialog.open();
		if (result != null) {
			doAddProfile(result);
		}
	}

	private void doExportTemplates() {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(bos);

			java.util.List<ExpTemplate> expTemplateList = new ArrayList<ExpTemplate>();
			for (Template template: templatesList) {
				Conf conf = new Conf(template, DosboxVersion.findById(dbversionsList, template.getDbversionId()), ps);
				expTemplateList.add(new ExpTemplate(-1, conf, template));
			}
			DOMSource doc = new DOMSource(XmlUtils.getFullTemplatesXML(expTemplateList, dbversionsList, "DBGL default templates", StringUtils.EMPTY, "rcblanke"));
			XmlUtils.saveDomSource(doc, FileUtils.getDefaultTemplatesXmlFile(), null);

			if (bos.size() > 0) {
				GeneralPurposeDialogs.warningMessage(shell, bos.toString());
				bos.reset();
			}
		} catch (Exception e) {
			GeneralPurposeDialogs.fatalMessage(shell, e.toString(), e);
		}
	}

	private void doImportDefaultTemplates(final boolean interactive) {
		if (!interactive || GeneralPurposeDialogs.confirmMessage(shell, settings.msg("dialog.importdefaulttemplates.confirm.start"))) {
			try {
				if (checkDefaultDBVersion() == null) {
					return;
				}

				File defaultXml = FileUtils.getDefaultTemplatesXmlFile();
				if (!FileUtils.isExistingFile(defaultXml))
					throw new IOException(settings.msg("general.error.openfile", new Object[] {defaultXml}));

				DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				Document doc = builder.parse(defaultXml);

				XPath xPath = XPathFactory.newInstance().newXPath();
				String packageVersion = xPath.evaluate("/document/export/format-version", doc);
				String packageTitle = xPath.evaluate("/document/export/title", doc);
				String packageAuthor = xPath.evaluate("/document/export/author", doc);
				String packageNotes = xPath.evaluate("/document/export/notes", doc);
				String creationApp = xPath.evaluate("/document/export/generator-title", doc);
				String creationAppVersion = xPath.evaluate("/document/export/generator-version", doc);
				Date creationDate = XmlUtils.datetimeFormatter.parse(xPath.evaluate("/document/export/creationdatetime", doc));

				System.out.println(settings.msg("dialog.import.importing",
					new Object[] {StringUtils.join(new String[] {packageTitle, packageVersion, packageAuthor, packageNotes, creationApp, creationAppVersion, creationDate.toString()}, ' ')}));

				NodeList templateNodes = (NodeList)xPath.evaluate("/document/template", doc, XPathConstants.NODESET);

				java.util.List<ExpTemplate> templates = new ArrayList<ExpTemplate>();
				SortedSet<DosboxVersion> dbSet = new TreeSet<DosboxVersion>();
				for (int i = 0; i < templateNodes.getLength(); i++) {
					Element templateNode = (Element)templateNodes.item(i);
					Element dosbox = XmlUtils.getNode(templateNode, "dosbox");
					DosboxVersion d = new DosboxVersion(i, XmlUtils.getTextValue(dosbox, "title"), "", "", true, false, false, "", XmlUtils.getTextValue(dosbox, "version"), null, null, null, 0);
					dbSet.add(d);
					templates.add(new ExpTemplate(templateNode, ImportDialog.getDosboxVersionId(d, dbSet)));
				}

				java.util.List<Integer> dbmapping = new ArrayList<Integer>();
				for (DosboxVersion dbversion: dbSet) {
					dbmapping.add(dbversion.findBestMatchId(dbversionsList));
				}

				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				PrintStream ps = new PrintStream(bos);

				Template addedTemplate = null;
				for (ExpTemplate template: templates) {
					template.setDbversionId(ImportDialog.getMappedDosboxVersionId(dbSet, dbmapping, template.getDbversionId()));

					DosboxVersion assocDBVersion = DosboxVersion.findById(dbversionsList, template.getDbversionId());

					addedTemplate = dbase.addOrEditTemplate(template.getTitle(), template.getDbversionId(), template.isDefault(), -1);
					Conf gameConf = new Conf(template.getImportedFullConfig(), template.getImportedIncrConfig(), false, FileUtils.getDefaultTemplatesXmlFile().getPath(), addedTemplate.getId(),
							assocDBVersion, ps);
					gameConf.save();
				}
				updateTemplateList(addedTemplate);

				if (bos.size() > 0) {
					GeneralPurposeDialogs.warningMessage(shell, bos.toString());
					bos.reset();
				} else {
					if (interactive)
						GeneralPurposeDialogs.infoMessage(shell, settings.msg("dialog.import.notice.importok"));
				}

			} catch (XPathExpressionException | SAXException e) {
				GeneralPurposeDialogs.fatalMessage(shell, settings.msg("dialog.importdefaulttemplates.error.defaultxmlinvalidformat", new Object[] {e.toString()}), e);
			} catch (Exception e) {
				GeneralPurposeDialogs.fatalMessage(shell, e.toString(), e);
			}
		}
	}

	private void initColumnIds() {
		columnNames = new String[Constants.RO_COLUMN_NAMES + Constants.EDIT_COLUMN_NAMES + 8];
		columnNames[0] = settings.msg("dialog.main.profiles.column.title");
		columnNames[1] = settings.msg("dialog.main.profiles.column.setup");
		columnNames[2] = settings.msg("dialog.main.profiles.column.developer");
		columnNames[3] = settings.msg("dialog.main.profiles.column.publisher");
		columnNames[4] = settings.msg("dialog.main.profiles.column.genre");
		columnNames[5] = settings.msg("dialog.main.profiles.column.year");
		columnNames[6] = settings.msg("dialog.main.profiles.column.status");
		columnNames[7] = settings.msg("dialog.main.profiles.column.favorite");
		columnNames[8] = settings.msg("dialog.main.profiles.column.id");
		columnNames[9] = settings.msg("dialog.main.profiles.column.dosboxversionid");
		for (int i = 0; i < Constants.EDIT_COLUMN_NAMES; i++) {
			columnNames[i + Constants.RO_COLUMN_NAMES] = ssettings.getValue("gui", "custom" + (i + 1));
		}
		columnNames[Constants.RO_COLUMN_NAMES + Constants.EDIT_COLUMN_NAMES] = settings.msg("dialog.main.profiles.column.screenshot");
		columnNames[Constants.RO_COLUMN_NAMES + Constants.EDIT_COLUMN_NAMES + 1] = settings.msg("dialog.main.profiles.column.dosboxversiontitle");

		columnNames[Constants.RO_COLUMN_NAMES + Constants.EDIT_COLUMN_NAMES + 2] = settings.msg("dialog.main.generic.column.created");
		columnNames[Constants.RO_COLUMN_NAMES + Constants.EDIT_COLUMN_NAMES + 3] = settings.msg("dialog.main.generic.column.lastmodify");
		columnNames[Constants.RO_COLUMN_NAMES + Constants.EDIT_COLUMN_NAMES + 4] = settings.msg("dialog.main.generic.column.lastrun");
		columnNames[Constants.RO_COLUMN_NAMES + Constants.EDIT_COLUMN_NAMES + 5] = settings.msg("dialog.main.generic.column.lastsetup");
		columnNames[Constants.RO_COLUMN_NAMES + Constants.EDIT_COLUMN_NAMES + 6] = settings.msg("dialog.main.generic.column.runs");
		columnNames[Constants.RO_COLUMN_NAMES + Constants.EDIT_COLUMN_NAMES + 7] = settings.msg("dialog.main.generic.column.setups");

		int amount = 0;
		for (int i = 0; i < columnNames.length; i++) {
			if (ssettings.getBooleanValue("gui", "column" + (i + 1) + "visible")) {
				amount++;
			}
		}

		int cNr = 0;
		columnIds = new int[amount];
		for (int i = 0; i < columnNames.length; i++) {
			if (ssettings.getBooleanValue("gui", "column" + (i + 1) + "visible")) {
				columnIds[cNr++] = i;
			}
		}
	}

	private Menu createDosboxVersionsSubmenu(final Menu parent, final int setup, final boolean prepareOnly) {
		final Menu dosboxVersionsSubMenu = new Menu(parent);
		for (int i = 0; i < dbversionsList.size(); i++) {
			final MenuItem menuItem = new MenuItem(dosboxVersionsSubMenu, SWT.NONE);
			menuItem.setText(dbversionsList.get(i).getTitle());
			menuItem.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(final SelectionEvent event) {
					int index = profile_table.getSelectionIndex();
					if (index != -1) {
						Profile prof = profilesList.get(index);
						DosboxVersion dbversion = dbversionsList.get(dosboxVersionsSubMenu.indexOf(menuItem));
						try {
							java.util.List<NativeCommand> cmds = dbase.readNativeCommandsList(prof.getId(), -1);
							FileUtils.doRunProfile(prof, dbversion, getEnv(ssettings), setup, prepareOnly, cmds, display);
						} catch (IOException | SQLException e) {
							GeneralPurposeDialogs.warningMessage(shell, e);
						}
					}
				}
			});
		}
		return dosboxVersionsSubMenu;
	}

	private void updateViewProfileSubmenu(final Profile prof) {
		MenuItem parent = viewProfileSubMenu.getParentItem();
		viewProfileSubMenu.dispose();
		viewProfileSubMenu = new Menu(parent);
		parent.setMenu(viewProfileSubMenu);

		if (prof != null) {
			for (int i = 0; i < prof.getLinks().length; i++) {
				String link = prof.getLink(i);
				if (link.length() > 0) {
					final MenuItem linkMenuItem = new MenuItem(viewProfileSubMenu, SWT.NONE);
					String url = link;
					String tag = link;
					if (url.indexOf("://") == -1) {
						url = "file://" + FileUtils.canonicalToData(url).getPath();
						tag = FileUtils.makeRelativeToDosroot(FileUtils.canonicalToData(tag)).getPath();
					}
					String title = prof.getLinkTitle(i);
					if (title != null && !"".equals(title))
						tag = title;
					linkMenuItem.setData(url);
					linkMenuItem.setText(StringUtils.abbreviateMiddle(tag, "....", 80));
					linkMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(final SelectionEvent event) {
							PlatformUtils.openForBrowsing((String)linkMenuItem.getData());
						}
					});
				}
			}
			final MenuItem linkMenuItem = new MenuItem(viewProfileSubMenu, SWT.NONE);
			linkMenuItem.setText(settings.msg("dialog.main.profile.view.conf"));
			linkMenuItem.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(final SelectionEvent event) {
					PlatformUtils.openForEditing(prof.getCanonicalConfFile());
				}
			});
		}
	}

	private void updateViewDosboxSubmenu(final DosboxVersion dbversion) {
		MenuItem parent = viewDosboxSubMenu.getParentItem();
		viewDosboxSubMenu.dispose();
		viewDosboxSubMenu = new Menu(parent);
		parent.setMenu(viewDosboxSubMenu);

		if (dbversion != null) {
			final MenuItem linkMenuItem = new MenuItem(viewDosboxSubMenu, SWT.NONE);
			linkMenuItem.setText(settings.msg("dialog.main.profile.view.conf"));
			linkMenuItem.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(final SelectionEvent event) {
					PlatformUtils.openForEditing(dbversion.getCanonicalConfFile());
				}
			});
		}
	}

	private void updateViewTemplateSubmenu(final Template template) {
		MenuItem parent = viewTemplateSubMenu.getParentItem();
		viewTemplateSubMenu.dispose();
		viewTemplateSubMenu = new Menu(parent);
		parent.setMenu(viewTemplateSubMenu);

		if (template != null) {
			final MenuItem linkMenuItem = new MenuItem(viewTemplateSubMenu, SWT.NONE);
			linkMenuItem.setText(settings.msg("dialog.main.profile.view.conf"));
			linkMenuItem.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(final SelectionEvent event) {
					PlatformUtils.openForEditing(FileUtils.constructCanonicalTemplateFileLocation(template.getId()));
				}
			});
		}
	}

	private void doExportProfilesList() {
		new ExportListDialog(shell, dbversionsList, profilesList).open();
	}

	private void createProfilesTab(TabFolder tabFolder) {
		final TabItem profilesTabItem = new TabItem(tabFolder, SWT.NONE);
		profilesTabItem.setText(settings.msg("dialog.main.profiles"));

		final Composite composite = new Composite(tabFolder, SWT.NONE);
		GridLayout compGL = new GridLayout(1, false);
		compGL.horizontalSpacing = compGL.verticalSpacing = compGL.marginHeight = compGL.marginWidth = 0;
		composite.setLayout(compGL);
		profilesTabItem.setControl(composite);

		final Composite toolbarComposite = new Composite(composite, SWT.NONE);
		toolbarComposite.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		RowLayout rowLayout = new RowLayout();
		rowLayout.spacing = 20;
		rowLayout.marginTop = rowLayout.marginBottom = rowLayout.marginLeft = 0;
		toolbarComposite.setLayout(rowLayout);

		final ToolBar toolBar = new ToolBar(toolbarComposite, SWT.HORIZONTAL);
		final ToolBar toolBarRight = new ToolBar(toolbarComposite, SWT.HORIZONTAL);

		GeneralPurposeGUI.createIconToolItem(toolBar, settings, settings.msg("dialog.main.addprofile"), SWTImageManager.IMG_TB_NEW, addProfAdapter);
		GeneralPurposeGUI.createIconToolItem(toolBar, settings, settings.msg("dialog.main.editprofile"), SWTImageManager.IMG_TB_EDIT, editProfAdapter);
		GeneralPurposeGUI.createIconToolItem(toolBar, settings, settings.msg("dialog.main.removeprofile"), SWTImageManager.IMG_TB_DELETE, removeProfAdapter);
		GeneralPurposeGUI.createIconToolItem(toolBar, settings, settings.msg("dialog.main.runprofile"), SWTImageManager.IMG_TB_RUN, runProfAdapter);
		setupToolItem = GeneralPurposeGUI.createIconToolItem(toolBar, settings, settings.msg("dialog.main.runprofilesetup"), SWTImageManager.IMG_TB_SETUP, setupProfAdapter);
		GeneralPurposeGUI.createSeparatorToolItem(toolBar, 40);
		GeneralPurposeGUI.createIconToolItem(toolBar, settings, settings.msg("dialog.main.addwizard"), SWTImageManager.IMG_TB_ADDGAMEWIZARD, addWizardAdapter);

		final ViewType[] views = new ViewType[] {new ViewType(ProfilesListType.TABLE.toString(), SWTImageManager.IMG_TABLE, settings.msg("dialog.main.profiles.viewtype.table")),
				new ViewType(ProfilesListType.SMALL_TILES.toString(), SWTImageManager.IMG_TILES_SMALL, settings.msg("dialog.main.profiles.viewtype.smalltiles")),
				new ViewType(ProfilesListType.MEDIUM_TILES.toString(), SWTImageManager.IMG_TILES_MEDIUM, settings.msg("dialog.main.profiles.viewtype.mediumtiles")),
				new ViewType(ProfilesListType.LARGE_TILES.toString(), SWTImageManager.IMG_TILES_LARGE, settings.msg("dialog.main.profiles.viewtype.largetiles")),
				new ViewType(ProfilesListType.SMALL_BOXES.toString(), SWTImageManager.IMG_BOXES_SMALL, settings.msg("dialog.main.profiles.viewtype.smallboxes")),
				new ViewType(ProfilesListType.MEDIUM_BOXES.toString(), SWTImageManager.IMG_BOXES_MEDIUM, settings.msg("dialog.main.profiles.viewtype.mediumboxes")),
				new ViewType(ProfilesListType.LARGE_BOXES.toString(), SWTImageManager.IMG_BOXES_LARGE, settings.msg("dialog.main.profiles.viewtype.largeboxes"))};
		ViewType currentViewType = settings.getSettings().getValue("gui", "viewstyle").equalsIgnoreCase(views[0].getName()) ? views[0]: views[1];

		viewSelector = new ToolItem(toolBarRight, SWT.DROP_DOWN);
		viewSelector.setImage(SWTImageManager.getResourceImage(shell.getDisplay(), currentViewType.getImage()));
		viewSelector.setToolTipText(currentViewType.getDisplayName());

		final Menu viewMenu = new Menu(shell, SWT.POP_UP);
		for (final ViewType view: views) {
			MenuItem item = new MenuItem(viewMenu, SWT.PUSH);
			item.setImage(SWTImageManager.getResourceImage(shell.getDisplay(), view.getImage()));
			item.setText(view.getDisplayName());
			item.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(final SelectionEvent event) {
					MenuItem menuItem = (MenuItem)event.widget;
					ViewType newViewType = views[menuItem.getParent().indexOf(menuItem)];
					if (!settings.getSettings().getValue("gui", "viewstyle").equalsIgnoreCase(newViewType.getName())) {
						toggleProfileViewType(newViewType, selectProfAdapter, mouseAdapter, keyAdapter, travListener, addProfAdapter, editProfAdapter, removeProfAdapter, setupProfAdapter,
							runProfAdapter, prepareProfAdapter, duplicateProfAdapter, toggleProfAdapter, shortcutProfAdapter, openProfAdapter);
					}
				}
			});
		}

		viewSelector.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (event.detail == SWT.ARROW) {
					Rectangle rect = viewSelector.getBounds();
					Point pt = new Point(rect.x, rect.y + rect.height);
					pt = toolBarRight.toDisplay(pt);
					viewMenu.setLocation(pt.x, pt.y);
					viewMenu.setVisible(true);
				} else {
					for (int i = 0; i < views.length; i++) {
						if (settings.getSettings().getValue("gui", "viewstyle").equalsIgnoreCase(views[i].getName())) {
							toggleProfileViewType(views[(i + 1) % views.length], selectProfAdapter, mouseAdapter, keyAdapter, travListener, addProfAdapter, editProfAdapter, removeProfAdapter,
								setupProfAdapter, runProfAdapter, prepareProfAdapter, duplicateProfAdapter, toggleProfAdapter, shortcutProfAdapter, openProfAdapter);
							break;
						}
					}
				}
			}
		});

		ToolItem itemSeparator = new ToolItem(toolBarRight, SWT.SEPARATOR);
		itemSeparator.setWidth(4);

		displayScreenshots = new ToolItem(toolBarRight, SWT.CHECK);
		displayScreenshots.setImage(SWTImageManager.getResourceImage(shell.getDisplay(), SWTImageManager.IMG_SCREENSHOTS));
		displayScreenshots.setToolTipText(settings.msg("dialog.main.profiles.togglebutton.screenshots"));

		ToolItem itemSeparator2 = new ToolItem(toolBarRight, SWT.SEPARATOR);
		itemSeparator2.setWidth(4);

		displayNotes = new ToolItem(toolBarRight, SWT.CHECK);
		displayNotes.setImage(SWTImageManager.getResourceImage(shell.getDisplay(), SWTImageManager.IMG_NOTES));
		displayNotes.setToolTipText(settings.msg("dialog.main.profiles.togglebutton.notes"));

		final SashForm sashForm = new SashForm(composite, SWT.NONE);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		ControlAdapter sashResizeAdapter = new ControlAdapter() {
			public void controlResized(final ControlEvent event) {
				ssettings.setIntValues("gui", "sashweights", sashForm.getWeights());
			}
		};
		SelectionAdapter linkAdapter = new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				PlatformUtils.openForBrowsing(event.text);
			}
		};

		filterFolder = new CTabFolder(sashForm, SWT.BORDER);
		filterFolder.setUnselectedCloseVisible(true);
		filterFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				updateProfilesAfterTabAction();
			}
		});
		filterFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
			public void close(final CTabFolderEvent event) {
				if (GeneralPurposeDialogs.confirmMessage(shell, settings.msg("dialog.main.confirm.removefilter", new Object[] {((CTabItem)event.item).getText().trim()}))) {
					boolean currentTabToBeClosed = (event.item == filterFolder.getSelection());
					try {
						int filterId = (Integer)event.item.getData();
						dbase.removeFilter(Filter.findById(filtersList, filterId));
						filtersList.remove(Filter.findIndexById(filtersList, filterId));
					} catch (SQLException e) {
						GeneralPurposeDialogs.warningMessage(shell, e);
					}
					if (currentTabToBeClosed) {
						filterFolder.setSelection(0);
						updateProfilesAfterTabAction();
					}
				} else {
					event.doit = false;
				}
			}
		});
		filterFolder.addMouseListener(filterMouseAdapter);
		for (Filter filter: filtersList) {
			addFilterTab(filter);
		}
		filterFolder.setSelection(ssettings.getIntValue("gui", "filtertab"));
		filterFolder.getSelection().setToolTipText(settings.msg("dialog.filter.notice.results", new Object[] {profilesList.size()}));

		constructProfilesList();

		final Composite informationGroup = new Composite(sashForm, SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		informationGroup.setLayout(gl);
		informationGroup.addControlListener(sashResizeAdapter);
		notesField = new Text(informationGroup, SWT.V_SCROLL | SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.WRAP);
		notesField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		notesField.setFont(GeneralPurposeGUI.stringToFont(display, notesField.getFont(), ssettings.getValues("gui", "notesfont")));

		sashForm.setWeights(ssettings.getIntValues("gui", "sashweights"));

		final Composite linksComposite = new Composite(informationGroup, SWT.NONE);
		final GridLayout gridLayout = new GridLayout();
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 1;
		gridLayout.verticalSpacing = 2;
		linksComposite.setLayout(gridLayout);
		linksComposite.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
		link = new Link[EditProfileDialog.AMOUNT_OF_LINKS];
		for (int i = 0; i < link.length; i++) {
			link[i] = new Link(linksComposite, SWT.NONE);
			link[i].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			link[i].addSelectionListener(linkAdapter);
		}

		thumbHeight = ssettings.getIntValue("gui", "screenshotsheight");
		final ScrolledComposite scrolledComposite = new ScrolledComposite(composite, SWT.BORDER | SWT.H_SCROLL);
		scrolledComposite.setMinHeight(thumbHeight + 20);
		final GridData scrolledCompositeGD = new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1);
		scrolledComposite.setLayoutData(scrolledCompositeGD);

		thumbsToolBar = new Composite(scrolledComposite, SWT.NONE);
		thumbsToolBar.setLayout(new RowLayout(SWT.HORIZONTAL));
		scrolledComposite.setContent(thumbsToolBar);
		scrolledComposite.getHorizontalBar().setPageIncrement(300);
		scrolledComposite.getHorizontalBar().setIncrement(50);

		displayScreenshots.setSelection(settings.getSettings().getBooleanValue("gui", "screenshotsvisible"));
		scrolledCompositeGD.exclude = !displayScreenshots.getSelection();

		displayScreenshots.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent arg0) {
				ToolItem item = (ToolItem)arg0.widget;
				scrolledCompositeGD.exclude = !item.getSelection();
				scrolledComposite.getParent().layout();
				settings.getSettings().setBooleanValue("gui", "screenshotsvisible", item.getSelection());
				displayProfileInformation(false);
			}

			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});

		displayNotes.setSelection(settings.getSettings().getBooleanValue("gui", "notesvisible"));
		sashForm.setMaximizedControl(displayNotes.getSelection() ? null: filterFolder);

		displayNotes.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent arg0) {
				ToolItem item = (ToolItem)arg0.widget;
				sashForm.setMaximizedControl(item.getSelection() ? null: filterFolder);
				settings.getSettings().setBooleanValue("gui", "notesvisible", item.getSelection());
				displayProfileInformation(false);
			}

			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});

		menu_thumb = new Menu(thumbsToolBar);

		SelectionAdapter removeThumbAdapter = new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				doRemoveThumb();
			}
		};
		SelectionAdapter openThumbsAdapter = new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				PlatformUtils.openDirForViewing(currentThumbFile.getParentFile());
			}
		};
		SelectionAdapter refreshThumbsAdapter = new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				for (ProfilesListItem item: profile_table.getItems()) {
					item.resetCachedInfo();
				}
				SWTImageManager.dispose();
				displayProfileInformation(true);
			}
		};

		GeneralPurposeGUI.createIconMenuItem(menu_thumb, SWT.NONE, settings, settings.msg("dialog.main.thumb.remove"), SWTImageManager.IMG_DELETE, removeThumbAdapter);
		new MenuItem(menu_thumb, SWT.SEPARATOR);
		GeneralPurposeGUI.createIconMenuItem(menu_thumb, SWT.NONE, settings, settings.msg("dialog.main.thumb.openfolder"), SWTImageManager.IMG_FOLDER, openThumbsAdapter);
		GeneralPurposeGUI.createIconMenuItem(menu_thumb, SWT.NONE, settings, settings.msg("dialog.main.thumb.refresh"), SWTImageManager.IMG_REFRESH, refreshThumbsAdapter);
	}

	private void toggleProfileViewType(ViewType newViewType, final SelectionAdapter selectProfAdapter, final MouseAdapter mouseAdapter, final KeyAdapter keyAdapter,
			final TraverseListener travListener, final SelectionAdapter addProfAdapter, final SelectionAdapter editProfAdapter, final SelectionAdapter removeProfAdapter,
			final SelectionAdapter setupProfAdapter, final SelectionAdapter runProfAdapter, final SelectionAdapter prepareProfAdapter, final SelectionAdapter duplicateProfAdapter,
			final SelectionAdapter toggleProfAdapter, final SelectionAdapter shortcutProfAdapter, final SelectionAdapter openProfAdapter) {
		viewSelector.setImage(SWTImageManager.getResourceImage(shell.getDisplay(), newViewType.getImage()));
		viewSelector.setToolTipText(newViewType.getDisplayName());
		settings.getSettings().setValue("gui", "viewstyle", newViewType.getName().toLowerCase());

		Set<Integer> selectedProfiles = getSelectedProfileIds();

		/* Workaround for bug in Gallery, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=416476 */
		viewSelector.setEnabled(false);
		Display.getCurrent().timerExec(250, new Runnable() {
			@Override
			public void run() {
				viewSelector.setEnabled(true);
			}
		});

		disposeProfilesList();
		constructProfilesList();
		updateProfilesList(selectedProfiles);
		filterFolder.layout(true);
	}

	private void disposeProfilesList() {
		for (CTabItem tab: filterFolder.getItems())
			tab.setControl(null);
		profile_table.dispose();
	}

	private void constructProfilesList() {
		if (settings.getSettings().getValue("gui", "viewstyle").equalsIgnoreCase(ProfilesListType.SMALL_TILES.toString())) {
			profile_table = new ProfilesList(filterFolder, ProfilesListType.SMALL_TILES);
		} else if (settings.getSettings().getValue("gui", "viewstyle").equalsIgnoreCase(ProfilesListType.MEDIUM_TILES.toString())) {
			profile_table = new ProfilesList(filterFolder, ProfilesListType.MEDIUM_TILES);
		} else if (settings.getSettings().getValue("gui", "viewstyle").equalsIgnoreCase(ProfilesListType.LARGE_TILES.toString())) {
			profile_table = new ProfilesList(filterFolder, ProfilesListType.LARGE_TILES);
		} else if (settings.getSettings().getValue("gui", "viewstyle").equalsIgnoreCase(ProfilesListType.SMALL_BOXES.toString())) {
			profile_table = new ProfilesList(filterFolder, ProfilesListType.SMALL_BOXES);
		} else if (settings.getSettings().getValue("gui", "viewstyle").equalsIgnoreCase(ProfilesListType.MEDIUM_BOXES.toString())) {
			profile_table = new ProfilesList(filterFolder, ProfilesListType.MEDIUM_BOXES);
		} else if (settings.getSettings().getValue("gui", "viewstyle").equalsIgnoreCase(ProfilesListType.LARGE_BOXES.toString())) {
			profile_table = new ProfilesList(filterFolder, ProfilesListType.LARGE_BOXES);
		} else {
			profile_table = new ProfilesList(filterFolder, ProfilesListType.TABLE, this, columnIds, columnNames);
		}

		for (CTabItem tab: filterFolder.getItems())
			tab.setControl(profile_table.getControl());

		profile_table.addMouseListener(mouseAdapter);
		profile_table.addKeyListener(keyAdapter);
		profile_table.addTraverseListener(travListener);
		profile_table.addSelectionListener(selectProfAdapter);

		int operations = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_DEFAULT;
		DropTarget target = new DropTarget(profile_table.getControl(), operations);
		Transfer[] types = new Transfer[] {FileTransfer.getInstance()};
		target.setTransfer(types);
		target.addDropListener(new DropTargetAdapter() {
			public void drop(DropTargetEvent event) {
				String[] filenames = (String[])event.data;
				if (filenames != null && filenames.length == 1)
					doAddProfile(filenames[0]);
			}
		});

		final Menu menu = new Menu(profile_table.getControl());
		profile_table.setMenu(menu);

		new MenuItem(menu, SWT.SEPARATOR);
		GeneralPurposeGUI.createIconMenuItem(menu, SWT.NONE, settings, settings.msg("dialog.main.profile.openfolder"), SWTImageManager.IMG_FOLDER, openProfAdapter);
		GeneralPurposeGUI.createIconMenuItem(menu, SWT.NONE, settings, settings.msg("dialog.main.profile.opencapturesfolder"), SWTImageManager.IMG_FOLDER, openCapturesAdapter);

		final MenuItem viewLinkMenuItem = GeneralPurposeGUI.createIconMenuItem(menu, SWT.CASCADE, settings, settings.msg("dialog.main.profile.view"), SWTImageManager.IMG_ZOOM, null);
		viewProfileSubMenu = new Menu(viewLinkMenuItem);
		viewLinkMenuItem.setMenu(viewProfileSubMenu);

		new MenuItem(menu, SWT.SEPARATOR);
		GeneralPurposeGUI.createIconMenuItem(menu, SWT.NONE, settings, settings.msg("dialog.main.profile.add"), SWTImageManager.IMG_NEW, addProfAdapter);
		GeneralPurposeGUI.createIconMenuItem(menu, SWT.NONE, settings, settings.msg("dialog.main.profile.edit"), SWTImageManager.IMG_EDIT, editProfAdapter);
		GeneralPurposeGUI.createIconMenuItem(menu, SWT.NONE, settings, settings.msg("dialog.main.profile.duplicate"), SWTImageManager.IMG_DUPLICATE, duplicateProfAdapter);
		GeneralPurposeGUI.createIconMenuItem(menu, SWT.NONE, settings, settings.msg("dialog.main.profile.remove"), SWTImageManager.IMG_DELETE, removeProfAdapter);
		new MenuItem(menu, SWT.SEPARATOR);
		GeneralPurposeGUI.createIconMenuItem(menu, SWT.NONE, settings, settings.msg("dialog.main.profile.togglefavorite"), SWTImageManager.IMG_FAVORITE, toggleProfAdapter);
		if (PlatformUtils.IS_WINDOWS || PlatformUtils.IS_LINUX) {
			new MenuItem(menu, SWT.SEPARATOR);
			GeneralPurposeGUI.createIconMenuItem(menu, SWT.NONE, settings, settings.msg("dialog.main.profile.createshortcut"), SWTImageManager.IMG_SHORTCUT, shortcutProfAdapter);
		}

		menu.addMenuListener(new MenuAdapter() {
			public void menuShown(final MenuEvent event) {
				if (profile_table.getSelectionIndex() != -1) {
					Profile prof = profilesList.get(profile_table.getSelectionIndex());

					for (MenuItem it: menu.getItems()) {
						if (it.getStyle() == SWT.SEPARATOR)
							break;
						it.dispose();
					}

					if (dbversionsList.size() > 1) {
						final MenuItem prepareWithMenuItem = GeneralPurposeGUI.createIconTopMenuItem(menu, SWT.CASCADE, settings, settings.msg("dialog.main.profile.startmanuallywith"), null, null);
						prepareWithMenuItem.setMenu(createDosboxVersionsSubmenu(menu, -1, true));
					}
					GeneralPurposeGUI.createIconTopMenuItem(menu, SWT.NONE, settings, settings.msg("dialog.main.profile.startmanually"), null, prepareProfAdapter);

					if (prof.hasSetup(2)) {
						if (dbversionsList.size() > 1) {
							final MenuItem alt2WithMenuItem = GeneralPurposeGUI.createIconTopMenuItem(menu, SWT.CASCADE, settings, new File(prof.getSetup(2)).getName(), null, null);
							alt2WithMenuItem.setMenu(createDosboxVersionsSubmenu(menu, 2, false));
						}
						GeneralPurposeGUI.createIconTopMenuItem(menu, SWT.NONE, settings, new File(prof.getSetup(2)).getName(), null, new SelectionAdapter() {
							public void widgetSelected(final SelectionEvent event) {
								doRunProfile(2, false);
							}
						});
					}

					if (prof.hasSetup(1)) {
						if (dbversionsList.size() > 1) {
							final MenuItem alt1WithMenuItem = GeneralPurposeGUI.createIconTopMenuItem(menu, SWT.CASCADE, settings, new File(prof.getSetup(1)).getName(), null, null);
							alt1WithMenuItem.setMenu(createDosboxVersionsSubmenu(menu, 1, false));
						}
						GeneralPurposeGUI.createIconTopMenuItem(menu, SWT.NONE, settings, new File(prof.getSetup(1)).getName(), null, new SelectionAdapter() {
							public void widgetSelected(final SelectionEvent event) {
								doRunProfile(1, false);
							}
						});
					}

					boolean hasSetup = prof.hasSetup(0);
					setupToolItem.setEnabled(hasSetup);
					if (hasSetup) {
						if (dbversionsList.size() > 1) {
							final MenuItem setupWithMenuItem = GeneralPurposeGUI.createIconTopMenuItem(menu, SWT.CASCADE, settings, settings.msg("dialog.main.profile.setupwith"), null, null);
							setupWithMenuItem.setMenu(createDosboxVersionsSubmenu(menu, 0, false));
						}
						GeneralPurposeGUI.createIconTopMenuItem(menu, SWT.NONE, settings, settings.msg("dialog.main.profile.setup"), SWTImageManager.IMG_SETUP, setupProfAdapter);
					}

					if (dbversionsList.size() > 1) {
						final MenuItem runWithMenuItem = GeneralPurposeGUI.createIconTopMenuItem(menu, SWT.CASCADE, settings, settings.msg("dialog.main.profile.runwith"), null, null);
						runWithMenuItem.setMenu(createDosboxVersionsSubmenu(menu, -1, false));
					}
					GeneralPurposeGUI.createIconTopMenuItem(menu, SWT.NONE, settings, settings.msg("dialog.main.profile.run"), SWTImageManager.IMG_RUN, runProfAdapter);
				}
			}
		});
	}

	protected void doStartGameWizard() {
		updateWithAddedProfile((Profile)new AddGameWizardDialog(shell, SWT.NONE).open());
	}

	private CTabItem addFilterTab(Filter filter) {
		CTabItem item = new CTabItem(filterFolder, filter.getFilter() == null ? SWT.NONE: SWT.CLOSE);
		item.setText("    " + filter.getTitle() + "    ");
		item.setData(filter.getId());
		return item;
	}

	private void createDosboxVersionsTab(TabFolder tabFolder) {
		final TabItem dosboxTabItem = new TabItem(tabFolder, SWT.NONE);
		dosboxTabItem.setText(settings.msg("dialog.main.dosboxversions"));

		final Composite composite = new Composite(tabFolder, SWT.NONE);
		composite.setLayout(new BorderLayout(0, 0));
		dosboxTabItem.setControl(composite);

		final ToolBar toolBar = new ToolBar(composite, SWT.NONE);
		toolBar.setLayoutData(BorderLayout.NORTH);

		SelectionAdapter addDosboxAdapter = new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				doAddDosboxVersion();
			}
		};
		SelectionAdapter editDosboxAdapter = new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				doEditDosboxVersion();
			}
		};
		SelectionAdapter removeDosboxAdapter = new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				doRemoveDosboxVersion();
			}
		};
		SelectionAdapter runDosboxAdapter = new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				doRunDosbox();
			}
		};
		SelectionAdapter toggleDosboxAdapter = new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				doToggleDefaultVersion();
			}
		};
		SelectionAdapter openDosboxAdapter = new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				int index = dbversion_table.getSelectionIndex();
				if (index != -1) {
					PlatformUtils.openDirForViewing(dbversionsList.get(index).getCanonicalConfFile().getParentFile());
				}
			}
		};
		MouseAdapter mouseAdapter = new MouseAdapter() {
			public void mouseDoubleClick(final MouseEvent event) {
				doRunDosbox();
			}
		};
		KeyAdapter keyAdapter = new KeyAdapter() {
			public void keyPressed(final KeyEvent event) {
				if (event.keyCode == SWT.DEL || (event.stateMask == SWT.MOD1 && (Character.toLowerCase(event.keyCode) == 'r'))) {
					doRemoveDosboxVersion();
				} else if (event.keyCode == SWT.INSERT || (event.stateMask == SWT.MOD1 && (Character.toLowerCase(event.keyCode) == 'n'))) {
					doAddDosboxVersion();
				} else if (event.stateMask == SWT.MOD1 && (Character.toLowerCase(event.keyCode) == 'm')) {
					doToggleDefaultVersion();
				}
			}
		};
		TraverseListener travListener = new TraverseListener() {
			public void keyTraversed(final TraverseEvent event) {
				if ((event.stateMask == SWT.MOD1) && (event.detail == SWT.TRAVERSE_RETURN)) {
					doEditDosboxVersion();
				} else if (event.detail == SWT.TRAVERSE_RETURN) {
					doRunDosbox();
				}
			}
		};
		final SelectionAdapter selectDosboxAdapter = new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				int index = dbversion_table.getSelectionIndex();
				if (index != -1) {
					updateViewDosboxSubmenu(dbversionsList.get(index));
				}
			}
		};

		GeneralPurposeGUI.createIconToolItem(toolBar, settings, settings.msg("dialog.main.addversion"), SWTImageManager.IMG_TB_NEW, addDosboxAdapter);
		GeneralPurposeGUI.createIconToolItem(toolBar, settings, settings.msg("dialog.main.editversion"), SWTImageManager.IMG_TB_EDIT, editDosboxAdapter);
		GeneralPurposeGUI.createIconToolItem(toolBar, settings, settings.msg("dialog.main.removeversion"), SWTImageManager.IMG_TB_DELETE, removeDosboxAdapter);
		GeneralPurposeGUI.createIconToolItem(toolBar, settings, settings.msg("dialog.main.runversion"), SWTImageManager.IMG_TB_RUN, runDosboxAdapter);

		dbversion_table = new Table(composite, SWT.FULL_SELECTION | SWT.BORDER);
		dbversion_table.setLinesVisible(true);
		dbversion_table.setHeaderVisible(true);
		addDBColumn(settings.msg("dialog.main.dosboxversions.column.title"), 0);
		addDBColumn(settings.msg("dialog.main.dosboxversions.column.version"), 1);
		addDBColumn(settings.msg("dialog.main.dosboxversions.column.path"), 2);
		addDBColumn(settings.msg("dialog.main.dosboxversions.column.default"), 3);
		addDBColumn(settings.msg("dialog.main.dosboxversions.column.id"), 4);
		addDBColumn(settings.msg("dialog.main.generic.column.created"), 5);
		addDBColumn(settings.msg("dialog.main.generic.column.lastmodify"), 6);
		addDBColumn(settings.msg("dialog.main.generic.column.lastrun"), 7);
		addDBColumn(settings.msg("dialog.main.generic.column.runs"), 8);

		final Menu menu = new Menu(dbversion_table);
		dbversion_table.setMenu(menu);
		GeneralPurposeGUI.createIconMenuItem(menu, SWT.NONE, settings, settings.msg("dialog.main.dosboxversion.run"), SWTImageManager.IMG_RUN, runDosboxAdapter);
		new MenuItem(menu, SWT.SEPARATOR);
		GeneralPurposeGUI.createIconMenuItem(menu, SWT.NONE, settings, settings.msg("dialog.main.dosboxversion.openfolder"), SWTImageManager.IMG_FOLDER, openDosboxAdapter);
		final MenuItem viewLinkMenuItem = GeneralPurposeGUI.createIconMenuItem(menu, SWT.CASCADE, settings, settings.msg("dialog.main.profile.view"), SWTImageManager.IMG_ZOOM, null);
		viewDosboxSubMenu = new Menu(viewLinkMenuItem);
		viewLinkMenuItem.setMenu(viewDosboxSubMenu);
		new MenuItem(menu, SWT.SEPARATOR);
		GeneralPurposeGUI.createIconMenuItem(menu, SWT.NONE, settings, settings.msg("dialog.main.dosboxversion.add"), SWTImageManager.IMG_NEW, addDosboxAdapter);
		GeneralPurposeGUI.createIconMenuItem(menu, SWT.NONE, settings, settings.msg("dialog.main.dosboxversion.edit"), SWTImageManager.IMG_EDIT, editDosboxAdapter);
		GeneralPurposeGUI.createIconMenuItem(menu, SWT.NONE, settings, settings.msg("dialog.main.dosboxversion.remove"), SWTImageManager.IMG_DELETE, removeDosboxAdapter);
		new MenuItem(menu, SWT.SEPARATOR);
		GeneralPurposeGUI.createIconMenuItem(menu, SWT.NONE, settings, settings.msg("dialog.main.dosboxversion.toggledefault"), SWTImageManager.IMG_HOME, toggleDosboxAdapter);

		dbversion_table.addKeyListener(keyAdapter);
		dbversion_table.addTraverseListener(travListener);
		dbversion_table.addMouseListener(mouseAdapter);
		dbversion_table.addSelectionListener(selectDosboxAdapter);
	}

	private void createTemplatesTab(TabFolder tabFolder) {
		final TabItem templatesTabItem = new TabItem(tabFolder, SWT.NONE);
		templatesTabItem.setText(settings.msg("dialog.main.templates"));

		final Composite composite = new Composite(tabFolder, SWT.NONE);
		composite.setLayout(new BorderLayout(0, 0));
		templatesTabItem.setControl(composite);

		final ToolBar toolBar = new ToolBar(composite, SWT.NONE);
		toolBar.setLayoutData(BorderLayout.NORTH);

		SelectionAdapter addTemplAdapter = new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				doAddTemplate();
			}
		};
		SelectionAdapter editTemplAdapter = new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				doEditTemplate();
			}
		};
		SelectionAdapter duplicateTemplateAdapter = new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				doDuplicateTemplate();
			}
		};
		SelectionAdapter removeTemplAdapter = new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				doRemoveTemplate();
			}
		};
		SelectionAdapter runTemplAdapter = new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				doRunTemplate();
			}
		};
		SelectionAdapter toggleTemplAdapter = new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				doToggleDefaultTemplate();
			}
		};
		MouseAdapter mouseAdapter = new MouseAdapter() {
			public void mouseDoubleClick(final MouseEvent event) {
				doRunTemplate();
			}
		};
		KeyAdapter keyAdapter = new KeyAdapter() {
			public void keyPressed(final KeyEvent event) {
				if (event.keyCode == SWT.DEL || (event.stateMask == SWT.MOD1 && (Character.toLowerCase(event.keyCode) == 'r'))) {
					doRemoveTemplate();
				} else if (event.keyCode == SWT.INSERT || (event.stateMask == SWT.MOD1 && (Character.toLowerCase(event.keyCode) == 'n'))) {
					doAddTemplate();
				} else if (event.stateMask == SWT.MOD1 && (Character.toLowerCase(event.keyCode) == 'm')) {
					doToggleDefaultTemplate();
				}
			}
		};
		TraverseListener travListener = new TraverseListener() {
			public void keyTraversed(final TraverseEvent event) {
				if ((event.stateMask == SWT.MOD1) && (event.detail == SWT.TRAVERSE_RETURN)) {
					doEditTemplate();
				} else if (event.detail == SWT.TRAVERSE_RETURN) {
					doRunTemplate();
				}
			}
		};
		final SelectionAdapter selectTemplateAdapter = new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				int index = template_table.getSelectionIndex();
				if (index != -1) {
					updateViewTemplateSubmenu(templatesList.get(index));
				}
			}
		};

		GeneralPurposeGUI.createIconToolItem(toolBar, settings, settings.msg("dialog.main.addtemplate"), SWTImageManager.IMG_TB_NEW, addTemplAdapter);
		GeneralPurposeGUI.createIconToolItem(toolBar, settings, settings.msg("dialog.main.edittemplate"), SWTImageManager.IMG_TB_EDIT, editTemplAdapter);
		GeneralPurposeGUI.createIconToolItem(toolBar, settings, settings.msg("dialog.main.removetemplate"), SWTImageManager.IMG_TB_DELETE, removeTemplAdapter);
		GeneralPurposeGUI.createIconToolItem(toolBar, settings, settings.msg("dialog.main.runtemplate"), SWTImageManager.IMG_TB_RUN, runTemplAdapter);

		template_table = new Table(composite, SWT.FULL_SELECTION | SWT.BORDER);
		template_table.setLinesVisible(true);
		template_table.setHeaderVisible(true);
		addTemplateColumn(settings.msg("dialog.main.templates.column.title"), 0);
		addTemplateColumn(settings.msg("dialog.main.templates.column.default"), 1);
		addTemplateColumn(settings.msg("dialog.main.templates.column.id"), 2);
		addTemplateColumn(settings.msg("dialog.main.generic.column.created"), 3);
		addTemplateColumn(settings.msg("dialog.main.generic.column.lastmodify"), 4);
		addTemplateColumn(settings.msg("dialog.main.generic.column.lastrun"), 5);
		addTemplateColumn(settings.msg("dialog.main.generic.column.runs"), 6);

		final Menu menu = new Menu(template_table);
		template_table.setMenu(menu);
		GeneralPurposeGUI.createIconMenuItem(menu, SWT.NONE, settings, settings.msg("dialog.main.template.run"), SWTImageManager.IMG_RUN, runTemplAdapter);
		new MenuItem(menu, SWT.SEPARATOR);
		final MenuItem viewLinkMenuItem = GeneralPurposeGUI.createIconMenuItem(menu, SWT.CASCADE, settings, settings.msg("dialog.main.profile.view"), SWTImageManager.IMG_ZOOM, null);
		viewTemplateSubMenu = new Menu(viewLinkMenuItem);
		viewLinkMenuItem.setMenu(viewTemplateSubMenu);
		new MenuItem(menu, SWT.SEPARATOR);
		GeneralPurposeGUI.createIconMenuItem(menu, SWT.NONE, settings, settings.msg("dialog.main.template.add"), SWTImageManager.IMG_NEW, addTemplAdapter);
		GeneralPurposeGUI.createIconMenuItem(menu, SWT.NONE, settings, settings.msg("dialog.main.template.edit"), SWTImageManager.IMG_EDIT, editTemplAdapter);
		GeneralPurposeGUI.createIconMenuItem(menu, SWT.NONE, settings, settings.msg("dialog.main.template.duplicate"), SWTImageManager.IMG_DUPLICATE, duplicateTemplateAdapter);
		GeneralPurposeGUI.createIconMenuItem(menu, SWT.NONE, settings, settings.msg("dialog.main.template.remove"), SWTImageManager.IMG_DELETE, removeTemplAdapter);
		new MenuItem(menu, SWT.SEPARATOR);
		GeneralPurposeGUI.createIconMenuItem(menu, SWT.NONE, settings, settings.msg("dialog.main.template.toggledefault"), SWTImageManager.IMG_HOME, toggleTemplAdapter);

		template_table.addKeyListener(keyAdapter);
		template_table.addTraverseListener(travListener);
		template_table.addMouseListener(mouseAdapter);
		template_table.addSelectionListener(selectTemplateAdapter);
	}

	private void doOpenAbout() {
		new AboutDialog(shell, SWT.NONE).open();
	}

	private void createFileMenu() {
		Menu appMenuBar = display.getMenuBar();
		if (appMenuBar == null) {
			appMenuBar = new Menu(shell, SWT.BAR);
			shell.setMenuBar(appMenuBar);
		}

		Menu systemMenu = display.getSystemMenu();
		if (systemMenu != null) {
			MenuItem prefs = getMenuItemById(systemMenu, SWT.ID_PREFERENCES);
			prefs.addSelectionListener(settingsAdapter);

			MenuItem about = getMenuItemById(systemMenu, SWT.ID_ABOUT);
			about.addSelectionListener(openAboutAdapter);

			int prefsIndex = systemMenu.indexOf(getMenuItemById(systemMenu, SWT.ID_PREFERENCES));
			MenuItem logMenuItem = new MenuItem(systemMenu, SWT.CASCADE, prefsIndex + 1);
			logMenuItem.setText(settings.msg("dialog.main.menu.log"));
			logMenuItem.addSelectionListener(logAdapter);
			MenuItem dbCleanupMenuItem = new MenuItem(systemMenu, SWT.CASCADE, prefsIndex + 2);
			dbCleanupMenuItem.setText(settings.msg("dialog.main.menu.databasecleanup"));
			dbCleanupMenuItem.addSelectionListener(cleanupAdapter);
		} else {
			final MenuItem fileMenuItem = new MenuItem(appMenuBar, SWT.CASCADE);
			fileMenuItem.setText(settings.msg("dialog.main.menu.file"));
			final Menu fileMenu = new Menu(fileMenuItem);
			fileMenuItem.setMenu(fileMenu);

			GeneralPurposeGUI.createIconMenuItem(fileMenu, SWT.NONE, settings, settings.msg("dialog.main.menu.adjustsettings"), SWTImageManager.IMG_SETTINGS, settingsAdapter);
			GeneralPurposeGUI.createIconMenuItem(fileMenu, SWT.NONE, settings, settings.msg("dialog.main.menu.log"), SWTImageManager.IMG_LOG, logAdapter);
			GeneralPurposeGUI.createIconMenuItem(fileMenu, SWT.NONE, settings, settings.msg("dialog.main.menu.databasecleanup"), SWTImageManager.IMG_CLEAN, cleanupAdapter);
			GeneralPurposeGUI.createIconMenuItem(fileMenu, SWT.NONE, settings, settings.msg("dialog.main.menu.exit"), SWTImageManager.IMG_EXIT, exitAdapter);
		}

		final MenuItem profilesMenuItem = new MenuItem(appMenuBar, SWT.CASCADE);
		profilesMenuItem.setText(settings.msg("dialog.main.menu.profiles"));
		final Menu profilesMenu = new Menu(profilesMenuItem);
		profilesMenuItem.setMenu(profilesMenu);

		GeneralPurposeGUI.createIconMenuItem(profilesMenu, SWT.NONE, settings, settings.msg("dialog.main.menu.import"), SWTImageManager.IMG_IMPORT, SWT.MOD1 | SWT.MOD3 | 'I', importAdapter);
		GeneralPurposeGUI.createIconMenuItem(profilesMenu, SWT.NONE, settings, settings.msg("dialog.main.menu.importprofile"), SWTImageManager.IMG_IMPORT, importConfAdapter);
		GeneralPurposeGUI.createIconMenuItem(profilesMenu, SWT.NONE, settings, settings.msg("dialog.main.menu.importdfendprofiles"), SWTImageManager.IMG_DFEND, dfendAdapter);

		new MenuItem(profilesMenu, SWT.SEPARATOR);
		GeneralPurposeGUI.createIconMenuItem(profilesMenu, SWT.NONE, settings, settings.msg("dialog.main.menu.export"), SWTImageManager.IMG_TABLEEXPORT, SWT.MOD1 | SWT.MOD3 | 'E', exportAdapter);
		GeneralPurposeGUI.createIconMenuItem(profilesMenu, SWT.NONE, settings, settings.msg("dialog.main.menu.exportprofileslist"), SWTImageManager.IMG_TABLEEXPORT, exportListAdapter);
		new MenuItem(profilesMenu, SWT.SEPARATOR);
		GeneralPurposeGUI.createIconMenuItem(profilesMenu, SWT.NONE, settings, settings.msg("dialog.main.menu.migrateprofiles"), SWTImageManager.IMG_MIGRATE, migrateAdapter);

		final MenuItem dbversionsMenuItem = new MenuItem(appMenuBar, SWT.CASCADE);
		dbversionsMenuItem.setText(settings.msg("dialog.main.menu.dosboxversions"));
		final Menu dbversionsMenu = new Menu(dbversionsMenuItem);
		dbversionsMenuItem.setMenu(dbversionsMenu);

		GeneralPurposeGUI.createIconMenuItem(dbversionsMenu, SWT.NONE, settings, settings.msg("dialog.main.menu.locatedosbox"), SWTImageManager.IMG_ZOOM, locateDosboxAdapter);

		final MenuItem templatesMenuItem = new MenuItem(appMenuBar, SWT.CASCADE);
		templatesMenuItem.setText(settings.msg("dialog.main.menu.templates"));
		final Menu templatesMenu = new Menu(templatesMenuItem);
		templatesMenuItem.setMenu(templatesMenu);

		if (PlatformUtils.IS_WINDOWS && PlatformUtils.IS_LINUX) // comment this line out to be able to export templates
			GeneralPurposeGUI.createIconMenuItem(templatesMenu, SWT.NONE, settings, settings.msg("dialog.main.menu.exporttemplates"), SWTImageManager.IMG_TABLEEXPORT, exportTemplatesAdapter);
		GeneralPurposeGUI.createIconMenuItem(templatesMenu, SWT.NONE, settings, settings.msg("dialog.main.menu.importdefaulttemplates"), SWTImageManager.IMG_IMPORT, importDefaultTemplatesAdapter);

		final MenuItem filterMenuItem = new MenuItem(appMenuBar, SWT.CASCADE);
		filterMenuItem.setText(settings.msg("dialog.main.menu.filter"));
		final Menu filterMenu = new Menu(filterMenuItem);
		filterMenuItem.setMenu(filterMenu);

		GeneralPurposeGUI.createIconMenuItem(filterMenu, SWT.NONE, settings, settings.msg("dialog.main.menu.addfilter"), SWTImageManager.IMG_FILTER, addFilterAdapter);
		GeneralPurposeGUI.createIconMenuItem(filterMenu, SWT.NONE, settings, settings.msg("dialog.main.menu.editfilter"), SWTImageManager.IMG_EDITFILTER, editFilterAdapter);

		if (systemMenu == null) {
			final MenuItem helpMenuItem = new MenuItem(appMenuBar, SWT.CASCADE);
			helpMenuItem.setText(settings.msg("dialog.main.menu.help"));
			final Menu helpMenu = new Menu(helpMenuItem);
			helpMenuItem.setMenu(helpMenu);

			GeneralPurposeGUI.createIconMenuItem(helpMenu, SWT.NONE, settings, settings.msg("dialog.main.menu.about"), SWTImageManager.IMG_ABOUT, openAboutAdapter);
		}
	}

	private static MenuItem getMenuItemById(final Menu menu, final int id) {
		for (MenuItem item: menu.getItems())
			if (item.getID() == id)
				return item;
		return null;
	}

	private void doExportProfiles() {
		if (profile_table.getSelectionIndex() != -1) {
			ProfileLoader pLoader = new ProfileLoader(shell, getSelectedProfiles(), false);
			if (pLoader.open() != null) {
				new ExportDialog(shell, dbversionsList, pLoader.getResult()).open();
			}
		}
	}

	private void doImportProfiles() {
		FileDialog dialog = new FileDialog(shell, SWT.OPEN);
		dialog.setFilterNames(new String[] {"GamePack Archives", "DOSBox configuration files", "Executables, Booter Disk Images", FileUtils.ALL_FILTER});
		dialog.setFilterExtensions(new String[] {FileUtils.ARC_FILTER, FileUtils.CNF_FILTER, FileUtils.EXE_FILTER + ";" + FileUtils.BTR_FILTER, FileUtils.ALL_FILTER});
		String result = dialog.open();
		if (result != null)
			doAddProfile(result);
	}

	private static Map<String, String> getEnv(SectionsWrapper settings) {
		if (settings.getBooleanValue("environment", "use")) {
			return StringRelatedUtils.stringArrayToMap(settings.getValues("environment", "value"));
		}
		return null;
	}

	private void updateProfilesAfterTabAction() {
		int tabIndex = filterFolder.getSelectionIndex();
		ssettings.setValue("gui", "filtertab", String.valueOf(tabIndex));
		filterClause = filtersList.get(tabIndex).getFilter();
		updateProfilesList(getSelectedProfileIds());
		for (CTabItem tab: filterFolder.getItems())
			tab.setToolTipText(null);
		filterFolder.getSelection().setToolTipText(settings.msg("dialog.filter.notice.results", new Object[] {profilesList.size()}));
		displayProfileInformation(false);
	}

	private void doViewProfileConf() {
		int index = profile_table.getSelectionIndex();
		if (index != -1) {
			PlatformUtils.openForEditing(profilesList.get(index).getCanonicalConfFile());
		}
	}

	private Set<Integer> getSelectedProfileIds() {
		int[] selection = profile_table.getSelectionIndices();
		Set<Integer> profileIds = new HashSet<Integer>();
		for (int i: selection) {
			profileIds.add(profilesList.get(i).getId());
		}
		return profileIds;
	}

	private java.util.List<Profile> getSelectedProfiles() {
		int[] selection = profile_table.getSelectionIndices();
		java.util.List<Profile> profiles = new ArrayList<Profile>();
		for (int i: selection) {
			profiles.add(profilesList.get(i));
		}
		return profiles;
	}

	private int[] getIndicesByIds(final Set<Integer> profileIds) {
		java.util.List<Integer> tableIdxsList = new ArrayList<Integer>();
		for (int i = 0; i < profilesList.size(); i++) {
			if (profileIds.contains(profilesList.get(i).getId())) {
				tableIdxsList.add(i);
			}
		}
		int[] tableIdxsArray = new int[tableIdxsList.size()];
		for (int i = 0; i < tableIdxsList.size(); i++) {
			tableIdxsArray[i] = tableIdxsList.get(i);
		}
		return tableIdxsArray;
	}

	public static void openSendToProfileDialog(final String file) {
		Database dbase = Database.getInstance();

		Shell shell = new Shell();
		shell.setMinimized(true);
		shell.open();

		try {
			java.util.List<DosboxVersion> dbversionsList = dbase.readDosboxVersionsList();
			if (DosboxVersion.findDefault(dbversionsList) == null) {
				GeneralPurposeDialogs.infoMessage(shell, Settings.getInstance().msg("dialog.main.required.defaultdosboxversion"));
				try {
					dbase.shutdown();
				} catch (SQLException e) {
					// nothing we can do
				}
				return;
			}

			if (FileUtils.isGamePackArchiveFile(file)) {
				ImportDialog importDialog = new ImportDialog(shell, dbversionsList, new File(file));
				importDialog.open();
			} else {
				EditProfileDialog editProfDialog = new EditProfileDialog(shell);
				editProfDialog.sendToProfile(file);
				editProfDialog.open();
			}
		} catch (SQLException e) {
			GeneralPurposeDialogs.warningMessage(shell, e);
		}

		try {
			dbase.shutdown();
		} catch (SQLException e) {
			// nothing we can do
		}
	}

	private final SelectionAdapter addProfAdapter = new SelectionAdapter() {
		public void widgetSelected(final SelectionEvent event) {
			doAddProfile();
		}
	};
	private final SelectionAdapter editProfAdapter = new SelectionAdapter() {
		public void widgetSelected(final SelectionEvent event) {
			doEditProfile();
		}
	};
	private final SelectionAdapter removeProfAdapter = new SelectionAdapter() {
		public void widgetSelected(final SelectionEvent event) {
			doRemoveProfile();
		}
	};
	private final SelectionAdapter setupProfAdapter = new SelectionAdapter() {
		public void widgetSelected(final SelectionEvent event) {
			doRunProfile(0, false);
		}
	};
	private final SelectionAdapter addWizardAdapter = new SelectionAdapter() {
		public void widgetSelected(final SelectionEvent event) {
			doStartGameWizard();
		}
	};
	private final SelectionAdapter runProfAdapter = new SelectionAdapter() {
		public void widgetSelected(final SelectionEvent event) {
			doRunProfile(-1, false);
		}
	};
	private final SelectionAdapter prepareProfAdapter = new SelectionAdapter() {
		public void widgetSelected(final SelectionEvent event) {
			doRunProfile(-1, true);
		}
	};
	private final SelectionAdapter selectProfAdapter = new SelectionAdapter() {
		public void widgetSelected(final SelectionEvent event) {
			displayProfileInformation(false);
		}
	};
	private final SelectionAdapter duplicateProfAdapter = new SelectionAdapter() {
		public void widgetSelected(final SelectionEvent event) {
			doDuplicateProfile();
		}
	};
	private final SelectionAdapter toggleProfAdapter = new SelectionAdapter() {
		public void widgetSelected(final SelectionEvent event) {
			doToggleFavoriteProfile();
		}
	};
	private final SelectionAdapter shortcutProfAdapter = new SelectionAdapter() {
		public void widgetSelected(final SelectionEvent event) {
			doCreateShortcut();
		}
	};
	private final SelectionAdapter openProfAdapter = new SelectionAdapter() {
		public void widgetSelected(final SelectionEvent event) {
			int index = profile_table.getSelectionIndex();
			if (index != -1) {
				Profile prof = profilesList.get(index);
				try {
					Conf c = new Conf(prof, DosboxVersion.findById(dbversionsList, prof.getDbversionId()), System.err);
					PlatformUtils.openDirForViewing(c.getAutoexec().getCanonicalMainDir());
				} catch (IOException e) {
					GeneralPurposeDialogs.warningMessage(shell, e);
				}
			}
		}
	};
	private final SelectionAdapter openCapturesAdapter = new SelectionAdapter() {
		public void widgetSelected(final SelectionEvent event) {
			int index = profile_table.getSelectionIndex();
			if (index != -1) {
				Profile prof = profilesList.get(index);
				PlatformUtils.openDirForViewing(prof.getCanonicalCaptures());
			}
		}
	};
	private final MouseAdapter mouseAdapter = new MouseAdapter() {
		public void mouseDoubleClick(final MouseEvent event) {
			doRunProfile(-1, false);
		}
	};
	private final MouseAdapter filterMouseAdapter = new MouseAdapter() {
		public void mouseDoubleClick(final MouseEvent event) {
			doEditFilter();
		}
	};
	private final KeyAdapter keyAdapter = new KeyAdapter() {
		public void keyPressed(final KeyEvent event) {
			if (event.keyCode == SWT.DEL || (event.stateMask == SWT.MOD1 && (Character.toLowerCase(event.keyCode) == 'r'))) {
				doRemoveProfile();
			} else if (event.keyCode == SWT.INSERT || (event.stateMask == SWT.MOD1 && (Character.toLowerCase(event.keyCode) == 'n'))) {
				doAddProfile();
			} else if (event.keyCode == SWT.F2) {
				doEditProfile(true);
			} else if (event.stateMask == SWT.MOD1 && (Character.toLowerCase(event.keyCode) == 'm')) {
				doToggleFavoriteProfile();
			} else if (event.stateMask == SWT.MOD1 && (Character.toLowerCase(event.keyCode) == 'd')) {
				doDuplicateProfile();
			} else if (event.stateMask == SWT.MOD1 && (Character.toLowerCase(event.keyCode) == 'a')) {
				profile_table.selectAll();
			} else if (event.stateMask == SWT.MOD1 && (Character.toLowerCase(event.keyCode) == 'f')) {
				doAddFilter();
			} else if (event.stateMask == SWT.MOD1 && (Character.toLowerCase(event.keyCode) == 'c')) {
				doViewProfileConf();
			} else if (event.stateMask == SWT.MOD1 && (Character.toLowerCase(event.keyCode) == 'w')) {
				doStartGameWizard();
			}
		}
	};
	private final TraverseListener travListener = new TraverseListener() {
		public void keyTraversed(final TraverseEvent event) {
			if ((event.stateMask == SWT.MOD1) && (event.detail == SWT.TRAVERSE_RETURN)) {
				doEditProfile();
			} else if ((event.stateMask == SWT.SHIFT) && (event.detail == SWT.TRAVERSE_RETURN)) {
				doRunProfile(0, false);
			} else if (event.detail == SWT.TRAVERSE_RETURN) {
				doRunProfile(-1, false);
			}
		}
	};
	private final SelectionAdapter dfendAdapter = new SelectionAdapter() {
		public void widgetSelected(final SelectionEvent event) {
			doDFendImport();
		}
	};
	private final SelectionAdapter migrateAdapter = new SelectionAdapter() {
		public void widgetSelected(final SelectionEvent event) {
			doMigrate();
		}
	};
	private final SelectionAdapter locateDosboxAdapter = new SelectionAdapter() {
		public void widgetSelected(final SelectionEvent event) {
			doLocateDosbox(true);
		}
	};
	private final SelectionAdapter exportListAdapter = new SelectionAdapter() {
		public void widgetSelected(final SelectionEvent event) {
			doExportProfilesList();
		}
	};
	private final SelectionAdapter importConfAdapter = new SelectionAdapter() {
		public void widgetSelected(final SelectionEvent event) {
			doImportConfigfile();
		}
	};
	private final SelectionAdapter exportTemplatesAdapter = new SelectionAdapter() {
		public void widgetSelected(final SelectionEvent event) {
			doExportTemplates();
		}
	};
	private final SelectionAdapter importDefaultTemplatesAdapter = new SelectionAdapter() {
		public void widgetSelected(final SelectionEvent event) {
			doImportDefaultTemplates(true);
		}
	};
	private final SelectionAdapter exportAdapter = new SelectionAdapter() {
		public void widgetSelected(final SelectionEvent event) {
			doExportProfiles();
		}
	};
	private final SelectionAdapter importAdapter = new SelectionAdapter() {
		public void widgetSelected(final SelectionEvent event) {
			doImportProfiles();
		}
	};
	private final SelectionAdapter cleanupAdapter = new SelectionAdapter() {
		public void widgetSelected(final SelectionEvent event) {
			try {
				if (GeneralPurposeDialogs.confirmMessage(shell, settings.msg("dialog.main.confirm.databasecleanup"))) {
					int itemsRemoved = dbase.cleanup();
					GeneralPurposeDialogs.infoMessage(shell, settings.msg("dialog.main.notice.databasecleanupok", new Object[] {itemsRemoved}));
				}
			} catch (SQLException e) {
				GeneralPurposeDialogs.warningMessage(shell, e);
			}
		}
	};
	private final SelectionAdapter settingsAdapter = new SelectionAdapter() {
		public void widgetSelected(final SelectionEvent event) {
			doOpenSettingsDialog();
		}
	};
	private final SelectionAdapter logAdapter = new SelectionAdapter() {
		public void widgetSelected(final SelectionEvent event) {
			doOpenLogDialog();
		}
	};
	private final SelectionAdapter exitAdapter = new SelectionAdapter() {
		public void widgetSelected(final SelectionEvent event) {
			shell.close();
		}
	};
	private final SelectionAdapter addFilterAdapter = new SelectionAdapter() {
		public void widgetSelected(final SelectionEvent event) {
			doAddFilter();
		}
	};
	private final SelectionAdapter editFilterAdapter = new SelectionAdapter() {
		public void widgetSelected(final SelectionEvent event) {
			doEditFilter();
		}
	};
	private final SelectionAdapter openAboutAdapter = new SelectionAdapter() {
		public void widgetSelected(final SelectionEvent event) {
			doOpenAbout();
		}
	};
}
