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
import java.sql.SQLException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.dbgl.gui.BrowseButton.BrowseType;
import org.dbgl.gui.BrowseButton.CanonicalType;
import org.dbgl.model.conf.Settings;
import org.dbgl.util.FileUtils;
import org.dbgl.util.MigrateThread;


public final class MigrateDialog extends Dialog {

	private Text from;
	private Text logText;
	private MigrateThread migrateThread;
	private Button cancelButton;
	private Label profileLabel;
	private String result = null;
	private Shell shell;
	private Settings settings;

	public MigrateDialog(final Shell parent, final int style) {
		super(parent, style);
	}

	public MigrateDialog(final Shell parent) {
		this(parent, SWT.NONE);
	}

	public Object open() {
		settings = Settings.getInstance();
		createContents();
		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (migrateThread != null && !migrateThread.isAlive() && !cancelButton.getText().equals(settings.msg("button.finish"))) {
				if (migrateThread.isEverythingOk())
					GeneralPurposeDialogs.infoMessage(shell, settings.msg("dialog.migration.notice.migrationok"));
				else
					GeneralPurposeDialogs.warningMessage(shell, settings.msg("dialog.migration.error.problem"));
				profileLabel.setText(settings.msg("dialog.migration.reviewlog"));
				profileLabel.pack();
				cancelButton.setText(settings.msg("button.finish"));
				cancelButton.setEnabled(true);
				shell.setDefaultButton(cancelButton);
			}
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result;
	}

	protected void createContents() {
		shell = new Shell(getParent(), SWT.TITLE | SWT.CLOSE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setLayout(new GridLayout());
		shell.addControlListener(new SizeControlAdapter(shell, "migratedialog"));
		shell.setText(settings.msg("dialog.migration.title"));

		final Group optionsGroup = new Group(shell, SWT.NONE);
		optionsGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		optionsGroup.setText(settings.msg("dialog.migration.options"));
		optionsGroup.setLayout(new GridLayout(3, false));

		final Label fromLocationLabel = new Label(optionsGroup, SWT.NONE);
		fromLocationLabel.setText(settings.msg("dialog.migration.from"));

		from = new Text(optionsGroup, SWT.BORDER);
		from.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		final BrowseButton fromBrowseButton = new BrowseButton(optionsGroup, SWT.NONE);
		fromBrowseButton.connect(shell, from, null, BrowseType.DIR, CanonicalType.NONE, false, null);

		final Label toLabel = new Label(optionsGroup, SWT.NONE);
		toLabel.setText(settings.msg("dialog.migration.to"));

		Text toDosRoot = new Text(optionsGroup, SWT.READ_ONLY | SWT.BORDER);
		toDosRoot.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		toDosRoot.setText(FileUtils.getDosRoot());
		new Label(optionsGroup, SWT.NONE);

		final Button startButton = new Button(optionsGroup, SWT.NONE);
		shell.setDefaultButton(startButton);
		startButton.setLayoutData(new GridData(120, SWT.DEFAULT));
		startButton.setText(settings.msg("dialog.migration.startmigration"));

		cancelButton = new Button(optionsGroup, SWT.NONE);
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				shell.close();
			}
		});
		cancelButton.setLayoutData(new GridData(120, SWT.DEFAULT));
		cancelButton.setText(settings.msg("button.cancel"));
		new Label(optionsGroup, SWT.NONE);

		final Group progressGroup = new Group(shell, SWT.NONE);
		progressGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		progressGroup.setText(settings.msg("dialog.migration.progress"));
		progressGroup.setLayout(new GridLayout());

		final ProgressBar progressBar = new ProgressBar(progressGroup, SWT.NONE);
		progressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		profileLabel = new Label(progressGroup, SWT.NONE);

		logText = new Text(progressGroup, SWT.V_SCROLL | SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.H_SCROLL);
		logText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		startButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				if (!isValid()) {
					return;
				}
				try {
					migrateThread = new MigrateThread(logText, progressBar, profileLabel, new File(from.getText()));
					result = from.getText();
					from.setEnabled(false);
					fromBrowseButton.setEnabled(false);
					startButton.setEnabled(false);
					cancelButton.setEnabled(false);
					migrateThread.start();
				} catch (SQLException e) {
					GeneralPurposeDialogs.warningMessage(shell, e);
					migrateThread = null;
				}
			}
		});
	}

	private boolean isValid() {
		GeneralPurposeDialogs.initErrorDialog();
		if (from.getText().equals("")) {
			GeneralPurposeDialogs.addError(settings.msg("dialog.migration.required.from"), from);
		}
		return !GeneralPurposeDialogs.displayErrorDialog(shell);
	}
}
