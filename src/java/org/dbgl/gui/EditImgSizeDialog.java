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

import org.dbgl.model.ImgSizeCommand;
import org.dbgl.model.conf.Settings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;


public final class EditImgSizeDialog extends Dialog {

	private ImgSizeCommand sizeCommand;
	private Object result;
	private Shell shell;
	private Settings settings;

	public EditImgSizeDialog(final Shell parent, final int style) {
		super(parent, style);
		settings = Settings.getInstance();
	}

	public EditImgSizeDialog(final Shell parent) {
		this(parent, SWT.NONE);
	}

	public void setImgMountSizeCommand(final String command) {
		sizeCommand = new ImgSizeCommand(command);
	}

	public Object open() {
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
		shell.setLayout(new GridLayout());
		shell.addControlListener(new SizeControlAdapter(shell, "imgsizedialog"));
		shell.setText(settings.msg("dialog.imgsize.title"));

		final Group paramGroup = new Group(shell, SWT.NONE);
		paramGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		paramGroup.setText(settings.msg("dialog.imgsize.params"));
		paramGroup.setLayout(new GridLayout(2, false));

		final Label bpsLabel = new Label(paramGroup, SWT.NONE);
		bpsLabel.setText(settings.msg("dialog.imgsize.bytespersector"));
		final Spinner bytesPerSector = new Spinner(paramGroup, SWT.BORDER);
		bytesPerSector.setValues(sizeCommand.getBytesPerSector(), 1, 4096, 0, 1, 512);

		final Label sptLabel = new Label(paramGroup, SWT.NONE);
		sptLabel.setText(settings.msg("dialog.imgsize.sectorspertrack"));
		final Spinner sectorsPerTrack = new Spinner(paramGroup, SWT.BORDER);
		sectorsPerTrack.setValues(sizeCommand.getSectorsPerTrack(), 1, 255, 0, 1, 64);

		final Label headsLabel = new Label(paramGroup, SWT.NONE);
		headsLabel.setText(settings.msg("dialog.imgsize.heads"));
		final Spinner heads = new Spinner(paramGroup, SWT.BORDER);
		heads.setValues(sizeCommand.getHeads(), 1, 64, 0, 1, 16);

		final Label cylindersLabel = new Label(paramGroup, SWT.NONE);
		cylindersLabel.setText(settings.msg("dialog.imgsize.cylinders"));
		final Spinner cylinders = new Spinner(paramGroup, SWT.BORDER);
		cylinders.setValues(sizeCommand.getCylinders(), 1, 8192, 0, 1, 20);

		final Label totalSizeLabel = new Label(paramGroup, SWT.NONE);
		totalSizeLabel.setText(settings.msg("dialog.imgsize.totalsize"));
		final Text totalSize = new Text(paramGroup, SWT.BORDER);
		totalSize.setEditable(false);
		totalSize.setText(settings.msg("dialog.imgsize.totalsize.value", new Long[] {sizeCommand.getTotalSize(), sizeCommand.getTotalSizeInMB()}));

		ModifyListener listener = new ModifyListener() {
			public void modifyText(ModifyEvent arg0) {
				sizeCommand = new ImgSizeCommand(bytesPerSector.getSelection(), sectorsPerTrack.getSelection(), heads.getSelection(), cylinders.getSelection());
				totalSize.setText(settings.msg("dialog.imgsize.totalsize.value", new Long[] {sizeCommand.getTotalSize(), sizeCommand.getTotalSizeInMB()}));
				totalSize.pack();
			}
		};
		bytesPerSector.addModifyListener(listener);
		sectorsPerTrack.addModifyListener(listener);
		heads.addModifyListener(listener);
		cylinders.addModifyListener(listener);

		Composite bc = new Composite(shell, SWT.NONE);
		bc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		bc.setLayout(new GridLayout(2, true));

		final Button okButton = new Button(bc, SWT.NONE);
		okButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				result = sizeCommand.toString();
				shell.close();
			}
		});
		shell.setDefaultButton(okButton);
		okButton.setText(settings.msg("button.ok"));

		final Button cancelButton = new Button(bc, SWT.NONE);
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
}
