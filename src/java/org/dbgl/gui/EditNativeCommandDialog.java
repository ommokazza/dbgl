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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.apache.commons.lang3.StringUtils;
import org.dbgl.gui.BrowseButton.BrowseType;
import org.dbgl.gui.BrowseButton.CanonicalType;
import org.dbgl.model.NativeCommand;
import org.dbgl.model.conf.Settings;
import swing2swt.layout.BorderLayout;


public class EditNativeCommandDialog extends Dialog {

	private Text command;
	private Text parameters;
	private Text cwd;
	private Button waitFor;
	private int orderNr = -1;

	private Settings settings;

	protected Object result;
	protected Shell shell;

	public EditNativeCommandDialog(final Shell parent) {
		super(parent, SWT.NONE);
	}

	public void setCommand(final NativeCommand cmd) {
		this.result = cmd;
		this.orderNr = cmd.getOrderNr();
	}

	public Object open() {
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

	protected void createContents() {
		shell = new Shell(getParent(), SWT.TITLE | SWT.CLOSE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setLayout(new BorderLayout(0, 0));
		shell.addControlListener(new SizeControlAdapter(shell, "nativecommanddialog"));
		if (result == null) {
			shell.setText(settings.msg("dialog.nativecommand.title.add"));
		} else {
			// meaning we are essentially editing an existing native command
			shell.setText(settings.msg("dialog.nativecommand.title.edit"));
		}

		final Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayout(new GridLayout(3, false));

		final Label commandLabel = new Label(composite, SWT.NONE);
		commandLabel.setText(settings.msg("dialog.nativecommand.command"));
		command = new Text(composite, SWT.BORDER);
		command.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		final BrowseButton cmdBrowseButton = new BrowseButton(composite, SWT.NONE);

		final Label parametersLabel = new Label(composite, SWT.NONE);
		parametersLabel.setText(settings.msg("dialog.nativecommand.parameters"));
		parameters = new Text(composite, SWT.BORDER);
		parameters.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		final Label cwdLabel = new Label(composite, SWT.NONE);
		cwdLabel.setText(settings.msg("dialog.nativecommand.cwd"));
		cwd = new Text(composite, SWT.BORDER);
		cwd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		final BrowseButton cwdBrowseButton = new BrowseButton(composite, SWT.NONE);

		cmdBrowseButton.connect(shell, command, cwd, BrowseType.FILE, CanonicalType.NATIVE_EXE, false, null);
		cwdBrowseButton.connect(shell, cwd, null, BrowseType.DIR, CanonicalType.NATIVE_EXE, false, null);

		final Label waitLabel = new Label(composite, SWT.NONE);
		waitLabel.setText(settings.msg("dialog.nativecommand.waitfor"));
		waitFor = new Button(composite, SWT.CHECK);
		waitFor.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));

		final Composite composite_1 = new Composite(shell, SWT.NONE);
		composite_1.setLayout(new RowLayout());
		composite_1.setLayoutData(BorderLayout.SOUTH);

		final Button okButton = new Button(composite_1, SWT.NONE);
		shell.setDefaultButton(okButton);
		okButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				if (!isValid()) {
					return;
				}
				result = new NativeCommand(new File(command.getText()), parameters.getText(), new File(cwd.getText()), waitFor.getSelection(), orderNr);
				shell.close();
			}
		});
		okButton.setText(settings.msg("button.ok"));

		final Button cancelButton = new Button(composite_1, SWT.NONE);
		cancelButton.setText(settings.msg("button.cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				result = null;
				shell.close();
			}
		});

		final RowData rowData = new RowData();
		rowData.width = GeneralPurposeGUI.getWidth(okButton, cancelButton);
		okButton.setLayoutData(rowData);
		cancelButton.setLayoutData(rowData);

		if (result == null) {
			// new native command, set default values
		} else {
			// meaning we are essentially editing an existing native command
			// so we need to set previous values
			NativeCommand cmd = (NativeCommand)result;
			command.setText(cmd.getCommand().getPath());
			parameters.setText(cmd.getParameters());
			cwd.setText(cmd.getCwd().getPath());
			waitFor.setSelection(cmd.isWaitFor());
		}
		command.setFocus();
	}

	private boolean isValid() {
		GeneralPurposeDialogs.initErrorDialog();
		if (StringUtils.isBlank(command.getText())) {
			GeneralPurposeDialogs.addError(settings.msg("dialog.nativecommand.required.command"), command);
		} else if (StringUtils.isBlank(cwd.getText())) {
			GeneralPurposeDialogs.addError(settings.msg("dialog.nativecommand.required.cwd"), cwd);
		}
		return !GeneralPurposeDialogs.displayErrorDialog(shell);
	}
}
