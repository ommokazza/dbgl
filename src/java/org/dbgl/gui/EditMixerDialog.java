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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.dbgl.model.MixerCommand;
import org.dbgl.model.conf.Settings;


public final class EditMixerDialog extends Dialog {

	private MixerCommand mixerCommand;
	private Object result;
	private Shell shell;
	private Settings settings;

	public EditMixerDialog(final Shell parent, final int style) {
		super(parent, style);
		settings = Settings.getInstance();
	}

	public EditMixerDialog(final Shell parent) {
		this(parent, SWT.NONE);
	}

	public void setMixerCommand(final String command) {
		mixerCommand = new MixerCommand(command);
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
		shell.setLayout(new GridLayout(MixerCommand.CHANNELS.length, false));
		shell.addControlListener(new SizeControlAdapter(shell, "mixerdialog"));
		shell.setText(settings.msg("dialog.mixer.title"));

		for (final String channelName: MixerCommand.CHANNELS) {
			Group channelGroup = new Group(shell, SWT.NONE);
			channelGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			channelGroup.setText(settings.msg("dialog.mixer." + channelName));
			channelGroup.setLayout(new GridLayout(2, true));

			final Text left = new Text(channelGroup, SWT.NONE);
			left.setEditable(false);
			left.setLayoutData(new GridData(SWT.BEGINNING, SWT.TOP, true, false));

			final Text right = new Text(channelGroup, SWT.NONE);
			right.setEditable(false);
			right.setLayoutData(new GridData(SWT.BEGINNING, SWT.TOP, true, false));

			final Scale scaleLeft = new Scale(channelGroup, SWT.VERTICAL);
			scaleLeft.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, true));
			scaleLeft.setMaximum(MixerCommand.MAX_VOLUME_LEVEL);
			scaleLeft.setPageIncrement(10);
			scaleLeft.setSelection(MixerCommand.MAX_VOLUME_LEVEL - mixerCommand.getVolumeFor(channelName).getLeft());

			final Scale scaleRight = new Scale(channelGroup, SWT.VERTICAL);
			scaleRight.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, true));
			scaleRight.setMaximum(MixerCommand.MAX_VOLUME_LEVEL);
			scaleRight.setPageIncrement(10);
			scaleRight.setSelection(MixerCommand.MAX_VOLUME_LEVEL - mixerCommand.getVolumeFor(channelName).getRight());

			final Button lock = new Button(channelGroup, SWT.CHECK);
			GridData lockData = new GridData(SWT.CENTER, SWT.FILL, true, false);
			lockData.horizontalSpan = 2;
			lock.setLayoutData(lockData);
			lock.setText(settings.msg("dialog.mixer.lockbalance"));
			lock.setSelection(scaleLeft.getSelection() == scaleRight.getSelection());

			setVolumeBar(left, right, scaleLeft, scaleRight);

			scaleLeft.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent arg0) {
					super.widgetSelected(arg0);
					if (lock.getSelection()) {
						scaleRight.setSelection(scaleLeft.getSelection());
					}
					mixerCommand.setVolumeFor(channelName, MixerCommand.MAX_VOLUME_LEVEL - scaleLeft.getSelection(), MixerCommand.MAX_VOLUME_LEVEL - scaleRight.getSelection());
					setVolumeBar(left, right, scaleLeft, scaleRight);
				}
			});

			scaleRight.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent arg0) {
					super.widgetSelected(arg0);
					if (lock.getSelection()) {
						scaleLeft.setSelection(scaleRight.getSelection());
					}
					mixerCommand.setVolumeFor(channelName, MixerCommand.MAX_VOLUME_LEVEL - scaleLeft.getSelection(), MixerCommand.MAX_VOLUME_LEVEL - scaleRight.getSelection());
					setVolumeBar(left, right, scaleLeft, scaleRight);
				}
			});

		}

		Composite bc = new Composite(shell, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = MixerCommand.CHANNELS.length;
		bc.setLayoutData(gd);
		bc.setLayout(new GridLayout(2, true));

		final Button okButton = new Button(bc, SWT.NONE);
		okButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				result = mixerCommand.toString();
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

	private void setVolumeBar(final Text left, final Text right, final Scale scaleLeft, final Scale scaleRight) {
		left.setText(settings.msg("dialog.mixer.leftchannelvolume", new Integer[] {MixerCommand.MAX_VOLUME_LEVEL - scaleLeft.getSelection()}));
		left.pack();
		right.setText(settings.msg("dialog.mixer.rightchannelvolume", new Integer[] {MixerCommand.MAX_VOLUME_LEVEL - scaleRight.getSelection()}));
		right.pack();
	}
}
