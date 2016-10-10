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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ExpandAdapter;
import org.eclipse.swt.events.ExpandEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.apache.commons.lang3.StringUtils;
import org.dbgl.gui.BrowseButton.BrowseType;
import org.dbgl.gui.BrowseButton.CanonicalType;
import org.dbgl.model.DosboxVersion;
import org.dbgl.model.conf.Settings;
import org.dbgl.util.DFendImportThread;
import org.dbgl.util.DFendReloadedImportThread;
import org.dbgl.util.PlatformUtils;
import org.dbgl.util.UIThread;


public final class DFendImportDialog extends Dialog {

	private ExpandItem orginalExpandItem, reloadedExpandItem;
	private Text location, dfrLocation, dfrConfsLocation;
	private Text logText;
	private DosboxVersion defaultDbversion;
	private UIThread importThread;
	private Button cancelButton;
	private Label profileLabel;
	private Shell shell;
	private Settings settings;

	public DFendImportDialog(final Shell parent, final int style) {
		super(parent, style);
	}

	public DFendImportDialog(final Shell parent) {
		this(parent, SWT.NONE);
	}

	public void setDefaultDosboxVersion(final DosboxVersion dbversion) {
		this.defaultDbversion = dbversion;
	}

	public Object open() {
		settings = Settings.getInstance();
		createContents();
		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (importThread != null && !importThread.isAlive() && !cancelButton.getText().equals(settings.msg("button.finish"))) {
				if (importThread.isEverythingOk())
					GeneralPurposeDialogs.infoMessage(shell, settings.msg("dialog.dfendimport.notice.importok"));
				else
					GeneralPurposeDialogs.warningMessage(shell, settings.msg("dialog.dfendimport.error.problem"));
				profileLabel.setText(settings.msg("dialog.dfendimport.reviewlog"));
				profileLabel.pack();
				cancelButton.setText(settings.msg("button.finish"));
				cancelButton.setEnabled(true);
				shell.setDefaultButton(cancelButton);
			}
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return importThread;
	}

	private void createContents() {
		shell = new Shell(getParent(), SWT.TITLE | SWT.CLOSE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setLayout(new GridLayout());
		shell.addControlListener(new SizeControlAdapter(shell, "dfendimportdialog"));
		shell.setText(settings.msg("dialog.dfendimport.title"));

		final Group optionsGroup = new Group(shell, SWT.NONE);
		optionsGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		optionsGroup.setText(settings.msg("dialog.dfendimport.options"));
		optionsGroup.setLayout(new GridLayout(3, false));

		final ExpandBar bar = new ExpandBar(optionsGroup, SWT.V_SCROLL);
		bar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));

		Composite originalComposite = new Composite(bar, SWT.NONE);
		originalComposite.setLayout(new GridLayout(3, false));

		Composite reloadedComposite = new Composite(bar, SWT.NONE);
		reloadedComposite.setLayout(new GridLayout(3, false));

		final Label dfendLocLabel = new Label(originalComposite, SWT.NONE);
		dfendLocLabel.setText(settings.msg("dialog.dfendimport.dfendpath"));

		location = new Text(originalComposite, SWT.BORDER);
		location.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		location.setText(PlatformUtils.DFEND_PATH + PlatformUtils.DFEND_PROFILES);

		final BrowseButton browseButton = new BrowseButton(originalComposite, SWT.NONE);
		browseButton.connect(shell, location, null, BrowseType.FILE, CanonicalType.DFEND, false, null);

		final Label cleanUpLabel = new Label(originalComposite, SWT.NONE);
		cleanUpLabel.setText(settings.msg("dialog.dfendimport.cleanup"));

		final Button cleanup = new Button(originalComposite, SWT.CHECK);
		cleanup.setText(settings.msg("dialog.dfendimport.removesections"));
		cleanup.setSelection(true);
		new Label(originalComposite, SWT.NONE);

		final Label dfendreloadedLocLabel = new Label(reloadedComposite, SWT.NONE);
		dfendreloadedLocLabel.setText(settings.msg("dialog.dfendimport.reloaded.path"));

		dfrLocation = new Text(reloadedComposite, SWT.BORDER);
		dfrLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		dfrLocation.setText(PlatformUtils.DFEND_RELOADED_PATH.getPath());

		final BrowseButton dfrBrowseButton = new BrowseButton(reloadedComposite, SWT.NONE);
		dfrBrowseButton.connect(shell, dfrLocation, null, BrowseType.DIR, CanonicalType.NONE, false, null);

		final Label dfendreloadedConfsLocLabel = new Label(reloadedComposite, SWT.NONE);
		dfendreloadedConfsLocLabel.setText(settings.msg("dialog.dfendimport.reloaded.exportedconfspath"));

		dfrConfsLocation = new Text(reloadedComposite, SWT.BORDER);
		dfrConfsLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		final BrowseButton dfrConfsBrowseButton = new BrowseButton(reloadedComposite, SWT.NONE);
		dfrConfsBrowseButton.connect(shell, dfrConfsLocation, null, BrowseType.DIR, CanonicalType.NONE, false, null);

		final Label dfrCleanUpLabel = new Label(reloadedComposite, SWT.NONE);
		dfrCleanUpLabel.setText(settings.msg("dialog.dfendimport.cleanup"));

		final Button dfrCleanup = new Button(reloadedComposite, SWT.CHECK);
		dfrCleanup.setText(settings.msg("dialog.dfendimport.reloaded.removesections"));
		new Label(reloadedComposite, SWT.NONE);

		orginalExpandItem = new ExpandItem(bar, SWT.NONE, 0);
		orginalExpandItem.setText(settings.msg("dialog.dfendimport.original.title"));
		orginalExpandItem.setHeight(originalComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
		orginalExpandItem.setControl(originalComposite);

		reloadedExpandItem = new ExpandItem(bar, SWT.NONE, 1);
		reloadedExpandItem.setText(settings.msg("dialog.dfendimport.reloaded.title"));
		reloadedExpandItem.setHeight(reloadedComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
		reloadedExpandItem.setControl(reloadedComposite);
		reloadedExpandItem.setExpanded(true);

		bar.addExpandListener(new ExpandAdapter() {
			public void itemCollapsed(final ExpandEvent e) {
				bar.getItem((((ExpandItem)e.item).getText().equals(settings.msg("dialog.dfendimport.reloaded.title"))) ? 0: 1).setExpanded(true);
				Display.getCurrent().asyncExec(new Runnable() {
					public void run() {
						shell.layout();
					}
				});
			}

			public void itemExpanded(final ExpandEvent e) {
				bar.getItem((((ExpandItem)e.item).getText().equals(settings.msg("dialog.dfendimport.reloaded.title"))) ? 0: 1).setExpanded(false);
				Display.getCurrent().asyncExec(new Runnable() {
					public void run() {
						shell.layout();
					}
				});
			}
		});

		final Button startImportButton = new Button(optionsGroup, SWT.NONE);
		shell.setDefaultButton(startImportButton);
		startImportButton.setText(settings.msg("dialog.dfendimport.startimport"));

		cancelButton = new Button(optionsGroup, SWT.NONE);
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				shell.close();
			}
		});
		cancelButton.setText(settings.msg("button.cancel"));

		GridData gridData = new GridData(GeneralPurposeGUI.getWidth(startImportButton, cancelButton), SWT.DEFAULT);
		startImportButton.setLayoutData(gridData);
		cancelButton.setLayoutData(gridData);
		new Label(optionsGroup, SWT.NONE);

		final Group progressGroup = new Group(shell, SWT.NONE);
		progressGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		progressGroup.setText(settings.msg("dialog.dfendimport.progress"));
		progressGroup.setLayout(new GridLayout());

		final ProgressBar progressBar = new ProgressBar(progressGroup, SWT.NONE);
		progressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		profileLabel = new Label(progressGroup, SWT.NONE);

		logText = new Text(progressGroup, SWT.V_SCROLL | SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.H_SCROLL);
		logText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		startImportButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				try {
					if (!isValid()) {
						return;
					}
					if (orginalExpandItem.getExpanded())
						importThread = new DFendImportThread(logText, progressBar, profileLabel, new File(location.getText()), cleanup.getSelection(), defaultDbversion);
					else
						// TODO: import data files
						importThread = new DFendReloadedImportThread(logText, progressBar, profileLabel, new File(dfrLocation.getText()), new File(dfrConfsLocation.getText()),
								dfrCleanup.getSelection(), defaultDbversion);

					location.setEnabled(false);
					dfrLocation.setEnabled(false);
					dfrConfsLocation.setEnabled(false);
					browseButton.setEnabled(false);
					cleanup.setEnabled(false);
					startImportButton.setEnabled(false);
					cancelButton.setEnabled(false);
					importThread.start();
				} catch (Exception e) {
					GeneralPurposeDialogs.warningMessage(shell, e);
					importThread = null;
				}
			}
		});
	}

	private boolean isValid() {
		GeneralPurposeDialogs.initErrorDialog();
		if (orginalExpandItem.getExpanded()) {
			if (StringUtils.isBlank(location.getText())) {
				GeneralPurposeDialogs.addError(settings.msg("dialog.dfendimport.required.location"), location);
			}
		} else {
			if (StringUtils.isBlank(dfrLocation.getText())) {
				GeneralPurposeDialogs.addError(settings.msg("dialog.dfendimport.reloaded.required.location"), dfrLocation);
			}
			if (StringUtils.isBlank(dfrConfsLocation.getText())) {
				GeneralPurposeDialogs.addError(settings.msg("dialog.dfendimport.reloaded.required.confslocation"), dfrConfsLocation);
			}
		}
		return !GeneralPurposeDialogs.displayErrorDialog(shell);
	}
}
