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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.dbgl.model.conf.Settings;
import org.dbgl.util.FileUtils;
import org.dbgl.util.StringRelatedUtils;


public final class BrowseArchiveDialog extends Dialog {

	private String[] fileNames;
	private String file;
	private Object result;
	private Shell shell;
	private Settings settings;

	public BrowseArchiveDialog(final Shell parent, final int style) {
		super(parent, style);
		file = null;
		settings = Settings.getInstance();
	}

	public BrowseArchiveDialog(final Shell parent) {
		this(parent, SWT.NONE);
	}

	public void setFileToBrowse(final String fileToBrowse) {
		this.file = fileToBrowse;
	}

	public Object open() {
		if (init()) {
			createContents();
			shell.open();
			shell.layout();
			Display display = getParent().getDisplay();
			while (!shell.isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
		}
		return result;
	}

	private boolean init() {
		try {
			fileNames = FileUtils.getExecutablesInZipOrIsoOrFat(file);
			if (fileNames.length <= 0) {
				GeneralPurposeDialogs.warningMessage(getParent(), settings.msg("dialog.archivebrowser.notice.noexe"));
				return false;
			}
		} catch (IOException e) {
			GeneralPurposeDialogs.warningMessage(getParent(), settings.msg("dialog.archivebrowser.error.readarchive", new Object[] {file, StringRelatedUtils.toString(e)}), e);
			return false;
		}
		return true;
	}

	private void createContents() {
		shell = new Shell(getParent(), SWT.TITLE | SWT.CLOSE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setLayout(new GridLayout(2, false));
		shell.addControlListener(new SizeControlAdapter(shell, "archivebrowser"));
		shell.setText(settings.msg("dialog.archivebrowser.title"));

		final List files = new List(shell, SWT.V_SCROLL | SWT.BORDER);
		files.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		files.setItems(fileNames);
		files.setSelection(0);
		files.showSelection();
		files.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(final MouseEvent event) {
				doChooseFile(files);
			}
		});

		final Button okButton = new Button(shell, SWT.NONE);
		okButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				doChooseFile(files);
			}
		});
		shell.setDefaultButton(okButton);
		okButton.setText(settings.msg("button.ok"));

		final Button cancelButton = new Button(shell, SWT.NONE);
		cancelButton.setText(settings.msg("button.cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				result = null;
				shell.close();
			}
		});

		final GridData gridData = new GridData();
		gridData.widthHint = GeneralPurposeGUI.getWidth(okButton, cancelButton);
		okButton.setLayoutData(gridData);
		cancelButton.setLayoutData(gridData);
	}

	private void doChooseFile(final List files) {
		if (FileUtils.isArchive(file)) {
			result = file + ':' + File.separatorChar + files.getItem(files.getSelectionIndex());
		} else if (FileUtils.isIsoFile(file) || FileUtils.isFatImage(file)) {
			result = file + File.separatorChar + files.getItem(files.getSelectionIndex());
		}
		shell.close();
	}
}
