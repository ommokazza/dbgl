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
import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.dbgl.db.Database;
import org.dbgl.model.DosboxVersion;
import org.dbgl.model.Profile;
import org.dbgl.model.conf.Conf;
import org.dbgl.model.conf.Settings;
import org.dbgl.util.FileUtils;
import swing2swt.layout.BorderLayout;


public final class DeleteProfilesDialog extends Dialog {

	private List<Profile> profs;
	private List<DosboxVersion> dbversions;
	private Database dbase;
	private Object result;
	private Shell shell;
	private Settings settings;

	public DeleteProfilesDialog(final Shell parent) {
		super(parent, SWT.NONE);
	}

	public void setProfilesToBeDeleted(final List<Profile> profs, final List<DosboxVersion> dbversions) {
		this.profs = profs;
		this.dbversions = dbversions;
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
		shell.addControlListener(new SizeControlAdapter(shell, "profiledeletedialog"));

		shell.setText(settings.msg("dialog.deleteprofiles.title", new Object[] {profs.size()}));

		final TabFolder tabFolder = new TabFolder(shell, SWT.NONE);

		TabItem infoTabItem = new TabItem(tabFolder, SWT.NONE);
		infoTabItem.setText(settings.msg("dialog.deleteprofiles.options"));

		final Composite composite = new Composite(tabFolder, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		infoTabItem.setControl(composite);

		final Label deleteRecordLabel = new Label(composite, SWT.NONE);
		deleteRecordLabel.setText(settings.msg("dialog.deleteprofiles.confirm.removedatabaseentry"));
		final Button deleteRecord = new Button(composite, SWT.CHECK);
		deleteRecord.setSelection(true);
		deleteRecord.setEnabled(false);

		final Label deleteConfsLabel = new Label(composite, SWT.NONE);
		deleteConfsLabel.setText(settings.msg("dialog.deleteprofiles.confirm.removeprofileconf"));
		final Button deleteConfs = new Button(composite, SWT.CHECK);
		deleteConfs.setSelection(true);

		final Label deleteMapperfilesLabel = new Label(composite, SWT.NONE);
		deleteMapperfilesLabel.setText(settings.msg("dialog.deleteprofiles.confirm.removemapperfile"));
		final Button deleteMapperfiles = new Button(composite, SWT.CHECK);
		deleteMapperfiles.setSelection(true);

		final Label deleteCapturesLabel = new Label(composite, SWT.NONE);
		deleteCapturesLabel.setText(settings.msg("dialog.deleteprofiles.confirm.removeprofilecaptures"));
		final Button deleteCaptures = new Button(composite, SWT.CHECK);
		deleteCaptures.setSelection(true);

		final Composite composite_7 = new Composite(shell, SWT.NONE);
		composite_7.setLayout(new GridLayout(2, true));
		composite_7.setLayoutData(BorderLayout.SOUTH);

		final Button okButton = new Button(composite_7, SWT.NONE);
		shell.setDefaultButton(okButton);
		okButton.setText(settings.msg("button.ok"));
		okButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				if (GeneralPurposeDialogs.confirmMessage(shell, settings.msg("dialog.deleteprofiles.confirm.removal", new Object[] {profs.size()}))) {
					for (Profile prof: profs) {
						try {
							dbase.startTransaction();
							dbase.removeNativeCommands(prof.getId(), -1);
							dbase.removeProfile(prof);
							dbase.commitTransaction();
							if (deleteMapperfiles.getSelection()) {
								Conf conf = new Conf(prof, DosboxVersion.findById(dbversions, prof.getDbversionId()), System.err);
								File customMapperfile = conf.getCustomMapperFile();
								if (customMapperfile != null) {
									FileUtils.removeFile(customMapperfile);
								}
							}
							if (deleteConfs.getSelection()) {
								FileUtils.removeFile(prof.getCanonicalConfFile());
							}
							if (deleteCaptures.getSelection()) {
								FileUtils.removeFilesInDirAndDir(prof.getCanonicalCaptures());
							}
						} catch (IOException e) {
							GeneralPurposeDialogs.warningMessage(shell, e);
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
					result = profs;
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
	}
}
