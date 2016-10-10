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

import org.dbgl.model.conf.Settings;
import org.dbgl.util.ProgressNotifyable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;


public final class ProgressDialog extends Dialog implements ProgressNotifyable {

	private Shell shell;
	private String title;
	private Thread thread;
	private ProgressBar progressBar;
	private Display display;

	public ProgressDialog(final Shell parent, final String title) {
		super(parent, SWT.NONE);
		this.title = title;
	}

	public void setThread(final Thread thread) {
		this.thread = thread;
	}

	public void open() {
		createContents();
		shell.open();
		shell.layout();
		display = getParent().getDisplay();
		thread.start();
		while (!shell.isDisposed() && thread.isAlive()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		shell.close();
	}

	protected void createContents() {
		shell = new Shell(getParent(), SWT.TITLE | SWT.CLOSE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setSize(400, 100);
		shell.setLayout(new GridLayout());
		shell.setText(title);

		final Group progressGroup = new Group(shell, SWT.NONE);
		progressGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		progressGroup.setText(Settings.getInstance().msg("dialog.migration.progress"));
		progressGroup.setLayout(new GridLayout());

		progressBar = new ProgressBar(progressGroup, SWT.NONE);
		progressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	}

	public void setTotal(final int total) {
		if (!display.isDisposed() && !progressBar.isDisposed()) {
			display.syncExec(new Runnable() {
				public void run() {
					progressBar.setMaximum(total);
				}
			});
		}
	}

	public void incrProgress(final int progress) {
		if (!display.isDisposed() && !progressBar.isDisposed()) {
			display.syncExec(new Runnable() {
				public void run() {
					progressBar.setSelection(progressBar.getSelection() + progress);
				}
			});
		}
	}

	public void setProgress(final int progress) {
		if (!display.isDisposed() && !progressBar.isDisposed()) {
			display.syncExec(new Runnable() {
				public void run() {
					progressBar.setSelection(progress);
				}
			});
		}
	}
}
