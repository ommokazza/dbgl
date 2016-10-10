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
import java.util.List;
import org.dbgl.gui.BrowseButton.BrowseType;
import org.dbgl.gui.BrowseButton.CanonicalType;
import org.dbgl.model.DosboxVersion;
import org.dbgl.model.ExpProfile;
import org.dbgl.model.conf.Settings;
import org.dbgl.util.ExportThread;
import org.dbgl.util.FileUtils;
import org.dbgl.util.XmlUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.w3c.dom.Document;
import swing2swt.layout.BorderLayout;


public class ExportDialog extends Wizard {

	private Text logText;
	private Button settingsOnly, fullGames, exportCapturesButton, exportMapperfilesButton, exportNativeCommandsButton;
	private Table profilesTable;
	private Text title, notes, author, filename;
	private ProgressBar progressBar;
	private Label profileLabel;

	private List<DosboxVersion> dbversionsList;
	private List<ExpProfile> expProfileList;

	public ExportDialog(final Shell parent, final List<DosboxVersion> dbList, final java.util.List<ExpProfile> expProfiles) {
		super(parent, SWT.NONE, Settings.getInstance().msg("dialog.export.title", new Object[] {expProfiles.size()}), "export", true);
		this.dbversionsList = dbList;
		this.expProfileList = expProfiles;
	}

	protected int stepSize(boolean up) {
		if (((stepNr == 0 && up) || (stepNr == 2 && !up)) && settingsOnly.getSelection())
			return 2;
		return super.stepSize(up);
	}

	protected boolean actionAfterNext() {
		if (stepNr == 3) {
			try {
				Document doc = XmlUtils.getFullProfilesXML(expProfileList, dbversionsList, title.getText(), notes.getText(), author.getText(), exportCapturesButton.getSelection(),
					exportMapperfilesButton.getSelection(), exportNativeCommandsButton.getSelection(), fullGames.getSelection());
				extensiveJobThread = new ExportThread(logText, progressBar, profileLabel, expProfileList, doc, exportCapturesButton.getSelection(), exportMapperfilesButton.getSelection(),
						fullGames.getSelection(), FileUtils.canonicalToData(filename.getText()));
			} catch (Exception ex) {
				GeneralPurposeDialogs.warningMessage(shell, ex);
				extensiveJobThread = null;
				return false;
			}
		} else if (stepNr == 4) {
			if (extensiveJobThread.isEverythingOk()) {
				GeneralPurposeDialogs.infoMessage(shell, settings.msg("dialog.export.notice.exportok"));
			} else {
				GeneralPurposeDialogs.warningMessage(shell, settings.msg("dialog.export.error.problem"));
			}
			profileLabel.setText(settings.msg("dialog.export.reviewlog"));
			profileLabel.pack();
		}
		return true;
	}

	protected boolean isValidInput() {
		if (stepNr == 1) {
			return isValidGameDirs();
		} else if (stepNr == 2) {
			return isValidTargetZip();
		}
		return true;
	}

	private boolean isValidTargetZip() {
		GeneralPurposeDialogs.initErrorDialog();
		if (title.getText().equals("")) {
			GeneralPurposeDialogs.addError(settings.msg("dialog.export.required.title"), title);
		}
		String f = filename.getText();
		if (f.equals(""))
			GeneralPurposeDialogs.addError(settings.msg("dialog.export.required.filename"), filename);
		else if (FileUtils.isExistingFile(FileUtils.canonicalToData(f)))
			GeneralPurposeDialogs.addError(settings.msg("dialog.export.error.fileexists", new Object[] {FileUtils.canonicalToData(f)}), filename);
		else {
			File dir = FileUtils.canonicalToData(f).getParentFile();
			if (dir == null || !dir.exists())
				GeneralPurposeDialogs.addError(settings.msg("dialog.export.error.exportdirmissing", new Object[] {FileUtils.canonicalToData(f)}), filename);
		}
		return !GeneralPurposeDialogs.displayErrorDialog(shell);
	}

	private boolean isValidGameDirs() {
		GeneralPurposeDialogs.initErrorDialog();
		for (ExpProfile prof: expProfileList) {
			if (!FileUtils.canonicalToDosroot(prof.getGameDir().getPath()).exists()) {
				GeneralPurposeDialogs.addError(settings.msg("dialog.export.error.gamedirmissing", new Object[] {prof.getGameDir()}), profilesTable);
			}
			if (prof.getGameDir().isAbsolute()) {
				GeneralPurposeDialogs.addError(settings.msg("dialog.export.error.gamedirnotrelative", new Object[] {prof.getGameDir()}), profilesTable);
			}
			for (ExpProfile prof2: expProfileList) {
				if (prof != prof2 && FileUtils.areRelated(prof.getGameDir(), prof2.getGameDir())) {
					GeneralPurposeDialogs.addError(settings.msg("dialog.export.error.gamedirsconflict", new Object[] {prof.getGameDir(), prof.getTitle(), prof2.getGameDir(), prof2.getTitle()}),
						profilesTable);;
				}
			}
		}
		return !GeneralPurposeDialogs.displayErrorDialog(shell);
	}

	protected void fillPages() {
		addStep(page1());
		addStep(page2());
		addStep(page3());
		addStep(page4());
	}

	private Control page1() {
		final Group group1 = new Group(shell, SWT.NONE);
		group1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		group1.setText(settings.msg("dialog.export.step1"));
		group1.setLayout(new GridLayout(2, false));

		final Label settingsOnlyLabel = new Label(group1, SWT.NONE);
		settingsOnlyLabel.setText(settings.msg("dialog.export.export"));
		settingsOnly = new Button(group1, SWT.RADIO);
		settingsOnly.setText(settings.msg("dialog.export.export.profiles"));
		settingsOnly.setSelection(true);

		new Label(group1, SWT.NONE);
		fullGames = new Button(group1, SWT.RADIO);
		fullGames.setText(settings.msg("dialog.export.export.games"));

		new Label(group1, SWT.NONE);
		new Label(group1, SWT.NONE);

		new Label(group1, SWT.NONE);
		exportCapturesButton = new Button(group1, SWT.CHECK);
		exportCapturesButton.setText(settings.msg("dialog.template.captures"));
		exportCapturesButton.setSelection(true);

		new Label(group1, SWT.NONE);
		exportMapperfilesButton = new Button(group1, SWT.CHECK);
		exportMapperfilesButton.setText(settings.msg("dialog.template.mapperfile"));

		new Label(group1, SWT.NONE);
		exportNativeCommandsButton = new Button(group1, SWT.CHECK);
		exportNativeCommandsButton.setText(settings.msg("dialog.export.nativecommands"));
		return group1;
	}

	private Control page2() {
		final Group reviewDirsGroup = new Group(shell, SWT.NONE);
		reviewDirsGroup.setText(settings.msg("dialog.export.step2"));
		reviewDirsGroup.setLayout(new GridLayout());

		profilesTable = new Table(reviewDirsGroup, SWT.FULL_SELECTION | SWT.BORDER);
		profilesTable.setHeaderVisible(true);
		final GridData gridData_1 = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2);
		gridData_1.heightHint = 80;
		profilesTable.setLayoutData(gridData_1);
		profilesTable.setLinesVisible(true);

		final TableColumn titleColumn = new TableColumn(profilesTable, SWT.NONE);
		titleColumn.setWidth(260);
		titleColumn.setText(settings.msg("dialog.main.profiles.column.title"));

		final TableColumn subdirColumn = new TableColumn(profilesTable, SWT.NONE);
		subdirColumn.setWidth(120);
		subdirColumn.setText(settings.msg("dialog.export.column.gamedir"));

		for (ExpProfile expProfile: expProfileList) {
			TableItem item = new TableItem(profilesTable, SWT.NONE);
			item.setText(expProfile.getTitle());
			item.setText(1, expProfile.getGameDir().getPath());
		}

		profilesTable.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(final MouseEvent event) {
				int idx = profilesTable.getSelectionIndex();
				ExpProfile prof = expProfileList.get(idx);
				DirectoryDialog dialog = new DirectoryDialog(shell);
				dialog.setFilterPath(FileUtils.canonicalToDosroot(prof.getGameDir().getPath()).getPath());
				String result = dialog.open();
				if (result != null) {
					File newGameDir = FileUtils.makeRelativeToDosroot(new File(result));
					prof.setGameDir(newGameDir);
					profilesTable.getSelection()[0].setText(1, newGameDir.getPath());
				}
			}
		});
		return reviewDirsGroup;
	}

	private Control page3() {
		final Group settingsGroup = new Group(shell, SWT.NONE);
		settingsGroup.setLayout(new GridLayout(3, false));
		settingsGroup.setText(settings.msg("dialog.export.step3"));
		settingsGroup.setLayoutData(BorderLayout.NORTH);

		final Label titleLabel = new Label(settingsGroup, SWT.NONE);
		titleLabel.setText(settings.msg("dialog.export.exporttitle"));

		title = new Text(settingsGroup, SWT.BORDER);
		title.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		final Label authorLabel = new Label(settingsGroup, SWT.NONE);
		authorLabel.setText(settings.msg("dialog.export.author"));

		author = new Text(settingsGroup, SWT.BORDER);
		author.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		final Label notesLabel = new Label(settingsGroup, SWT.NONE);
		notesLabel.setText(settings.msg("dialog.export.notes"));

		notes = new Text(settingsGroup, SWT.WRAP | SWT.V_SCROLL | SWT.MULTI | SWT.H_SCROLL | SWT.BORDER);
		final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 2);
		gridData.heightHint = 40;
		notes.setLayoutData(gridData);
		StringBuffer sb = new StringBuffer();
		for (ExpProfile expProfile: expProfileList) {
			sb.append(expProfile.getTitle()).append(notes.getLineDelimiter());
		}
		notes.setText(sb.toString());
		new Label(settingsGroup, SWT.NONE);

		final Label fileLabel = new Label(settingsGroup, SWT.NONE);
		fileLabel.setText(settings.msg("dialog.export.file"));

		filename = new Text(settingsGroup, SWT.BORDER);
		filename.setText(FileUtils.EXPORT_DIR + "games.dbgl.zip");
		filename.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		final BrowseButton browseButton = new BrowseButton(settingsGroup, SWT.NONE);
		browseButton.connect(shell, filename, null, BrowseType.FILE, CanonicalType.DBGLZIP, true, null);
		return settingsGroup;
	}

	private Control page4() {
		final Group progressGroup = new Group(shell, SWT.NONE);
		progressGroup.setText(settings.msg("dialog.export.step4"));
		progressGroup.setLayout(new GridLayout());

		progressBar = new ProgressBar(progressGroup, SWT.NONE);
		progressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		profileLabel = new Label(progressGroup, SWT.NONE);
		profileLabel.setText(settings.msg("dialog.export.start"));

		logText = new Text(progressGroup, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.READ_ONLY | SWT.BORDER);
		logText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		return progressGroup;
	}
}
