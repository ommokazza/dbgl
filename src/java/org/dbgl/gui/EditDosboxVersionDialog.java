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
import java.io.IOException;
import java.sql.SQLException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.dbgl.db.Database;
import org.dbgl.gui.BrowseButton.BrowseType;
import org.dbgl.gui.BrowseButton.CanonicalType;
import org.dbgl.model.DosboxVersion;
import org.dbgl.model.conf.Settings;
import org.dbgl.util.FileUtils;
import org.dbgl.util.PlatformUtils;
import swing2swt.layout.BorderLayout;


public final class EditDosboxVersionDialog extends Dialog {

	private Text title, path, conf, parameters;
	private Button usingCurses;
	private Database dbase;
	private Object result;
	private Shell shell;
	private Settings settings;
	private boolean isDefault;

	public EditDosboxVersionDialog(final Shell parent, final boolean isDefault) {
		super(parent, SWT.NONE);
		this.isDefault = isDefault;
	}

	public void setDosboxVersion(final DosboxVersion dbversion) {
		this.result = dbversion;
	}

	public Object open() {
		dbase = Database.getInstance();
		settings = Settings.getInstance();
		createContents();
		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result;
	}

	private void createContents() {
		shell = new Shell(getParent(), SWT.TITLE | SWT.CLOSE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setLayout(new BorderLayout(0, 0));
		shell.addControlListener(new SizeControlAdapter(shell, "dosboxdialog"));

		if (result == null || (((DosboxVersion)result).getId() == -1)) {
			shell.setText(settings.msg("dialog.dosboxversion.title.add"));
		} else {
			// meaning we are essentially editing an existing dosbox version
			shell.setText(settings.msg("dialog.dosboxversion.title.edit", new Object[] {((DosboxVersion)result).getTitle(), ((DosboxVersion)result).getId()}));
		}

		final TabFolder tabFolder = new TabFolder(shell, SWT.NONE);

		TabItem infoTabItem = new TabItem(tabFolder, SWT.NONE);
		infoTabItem.setText(settings.msg("dialog.dosboxversion.tab.info"));

		final Composite composite = new Composite(tabFolder, SWT.NONE);
		composite.setLayout(new GridLayout(3, false));
		infoTabItem.setControl(composite);

		final Label titleLabel = new Label(composite, SWT.NONE);
		titleLabel.setText(settings.msg("dialog.dosboxversion.title"));

		title = new Text(composite, SWT.BORDER);
		title.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		new Label(composite, SWT.NONE);

		final Label pathLabel = new Label(composite, SWT.NONE);
		pathLabel.setText(settings.msg("dialog.dosboxversion.path"));
		path = new Text(composite, SWT.BORDER);
		path.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		final BrowseButton browsePathButton = new BrowseButton(composite, SWT.NONE);

		final Label confLabel = new Label(composite, SWT.NONE);
		confLabel.setText(settings.msg("dialog.profile.configfile"));
		conf = new Text(composite, SWT.BORDER);
		conf.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		final BrowseButton browseConfButton = new BrowseButton(composite, SWT.NONE);

		browsePathButton.connect(shell, path, conf, PlatformUtils.IS_OSX ? BrowseType.FILE: BrowseType.DIR, CanonicalType.DOSBOX, false, null);
		browseConfButton.connect(shell, conf, null, BrowseType.FILE, CanonicalType.DOSBOXCONF, false, null);

		final Label parametersLabel = new Label(composite, SWT.NONE);
		parametersLabel.setText(settings.msg("dialog.dosboxversion.parameters"));

		parameters = new Text(composite, SWT.BORDER);
		parameters.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		new Label(composite, SWT.NONE);

		final Label versionLabel = new Label(composite, SWT.NONE);
		versionLabel.setText(settings.msg("dialog.dosboxversion.version"));

		final Combo version = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
		version.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		version.setItems(DosboxVersion.SUPP_RELEASES);
		version.setVisibleItemCount(15);
		version.select(version.getItemCount() - 1);
		new Label(composite, SWT.NONE);

		final Label multiconfLabel = new Label(composite, SWT.NONE);
		multiconfLabel.setText(settings.msg("dialog.dosboxversion.multiconfsupport"));

		final Button multiconf = new Button(composite, SWT.CHECK);
		multiconf.setSelection(true);
		new Label(composite, SWT.NONE);

		final Label cursesLabel = new Label(composite, SWT.NONE);
		cursesLabel.setText(settings.msg("dialog.dosboxversion.altstartup"));

		usingCurses = new Button(composite, SWT.CHECK);
		usingCurses.setText(settings.msg("dialog.dosboxversion.altstartupexplanation"));
		usingCurses.setSelection(false);
		usingCurses.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false, 2, 1));

		final Label defaultLabel = new Label(composite, SWT.NONE);
		defaultLabel.setText(settings.msg("dialog.dosboxversion.default"));

		final Button defaultButton = new Button(composite, SWT.CHECK);
		defaultButton.setSelection(isDefault);
		new Label(composite, SWT.NONE);

		final Composite composite_7 = new Composite(shell, SWT.NONE);
		composite_7.setLayout(new GridLayout(2, true));
		composite_7.setLayoutData(BorderLayout.SOUTH);

		final Button okButton = new Button(composite_7, SWT.NONE);
		shell.setDefaultButton(okButton);
		okButton.setText(settings.msg("button.ok"));
		okButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				if (!isValid()) {
					return;
				}
				try {
					result = dbase.addOrEditDosboxVersion(title.getText(), path.getText(), conf.getText(), multiconf.getSelection(), usingCurses.getSelection(), defaultButton.getSelection(),
						parameters.getText(), version.getText(), result == null ? -1: ((DosboxVersion)result).getId());
				} catch (SQLException e) {
					GeneralPurposeDialogs.warningMessage(shell, e);
				}
				shell.close();
			}
		});

		final Button cancelButton = new Button(composite_7, SWT.NONE);
		cancelButton.setText(settings.msg("button.cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				result = null;
				shell.close();
			}
		});

		final GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.widthHint = GeneralPurposeGUI.getWidth(okButton, cancelButton);
		okButton.setLayoutData(gridData);
		cancelButton.setLayoutData(gridData);

		// init values
		if (result != null) {
			// meaning we are essentially editing an existing dosbox version
			// so we need to set previous values
			title.setText(((DosboxVersion)result).getTitle());
			path.setText(((DosboxVersion)result).getPath());
			conf.setText(((DosboxVersion)result).getConf());
			parameters.setText(((DosboxVersion)result).getParameters());
			version.setText(((DosboxVersion)result).getVersion());
			defaultButton.setSelection(((DosboxVersion)result).isDefault());
			multiconf.setSelection(((DosboxVersion)result).isMultiConfig());
			usingCurses.setSelection(((DosboxVersion)result).isUsingCurses());
		}
		title.setFocus();
	}

	private boolean isValid() {
		GeneralPurposeDialogs.initErrorDialog();
		if (title.getText().equals("")) {
			GeneralPurposeDialogs.addError(settings.msg("dialog.dosboxversion.required.title"), title);
		}
		if (path.getText().equals("")) {
			GeneralPurposeDialogs.addError(settings.msg("dialog.dosboxversion.required.path"), path);
		}
		if (conf.getText().equals("")) {
			GeneralPurposeDialogs.addError(settings.msg("dialog.dosboxversion.required.conf"), conf);
		}
		if (!GeneralPurposeDialogs.hasErrors()) {
			DosboxVersion dbversion = new DosboxVersion(-1, "", path.getText(), conf.getText(), false, usingCurses.getSelection(), false, parameters.getText(), "", null, null, null, 0);
			File executable = dbversion.getCanonicalExecutable();
			File configFile = dbversion.getCanonicalConfFile();
			boolean exeAvailable = FileUtils.isReadableFile(executable);
			if (!exeAvailable) {
				GeneralPurposeDialogs.addError(settings.msg("dialog.dosboxversion.error.dosboxexemissing", new Object[] {executable}), path);
			}
			if (!FileUtils.isReadableFile(configFile) && exeAvailable) {
				if (GeneralPurposeDialogs.confirmMessage(shell, settings.msg("dialog.dosboxversion.confirm.createmissingdosboxconf", new Object[] {configFile}))) {
					try {
						FileUtils.doCreateDosboxConf(dbversion);
					} catch (IOException e) {
						GeneralPurposeDialogs.warningMessage(shell, e);
					}
				}
			}
			if (!usingCurses.getSelection() && !FileUtils.isReadableFile(configFile)) {
				GeneralPurposeDialogs.addError(settings.msg("dialog.dosboxversion.error.dosboxconfmissing", new Object[] {configFile}), path);
			}
		}
		return !GeneralPurposeDialogs.displayErrorDialog(shell);
	}
}
