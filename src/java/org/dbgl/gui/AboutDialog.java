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

import org.dbgl.model.Constants;
import org.dbgl.model.conf.Settings;
import org.dbgl.swtdesigner.SWTImageManager;
import org.dbgl.util.PlatformUtils;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Link;
import org.hsqldb.persist.HsqlDatabaseProperties;
import org.eclipse.swt.widgets.Group;


public class AboutDialog extends Dialog {

	protected Shell shell;
	private Label lblCreatedBy, lblStats, lblThanks;
	private Group group;

	public AboutDialog(Shell parent, int style) {
		super(parent, style);
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
		return null;
	}

	private void createContents() {
		Settings settings = Settings.getInstance();

		shell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
		shell.setSize(652, 380);
		shell.setText(settings.msg("dialog.about.title"));
		GridLayout gl_shell = new GridLayout(2, false);
		gl_shell.marginTop = 2;
		gl_shell.marginBottom = 5;
		shell.setLayout(gl_shell);

		Canvas canvas = new Canvas(shell, SWT.NONE);
		GridData gd_canvas = new GridData(SWT.LEFT, SWT.CENTER, true, true);
		gd_canvas.heightHint = 256;
		gd_canvas.widthHint = 256;
		canvas.setLayoutData(gd_canvas);
		canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				e.gc.drawImage(SWTImageManager.getResourceImage(getParent().getDisplay(), "ico/256.png"), 0, 0);
			}
		});

		group = new Group(shell, SWT.NONE);
		GridLayout gl_group = new GridLayout(1, false);
		group.setLayout(gl_group);
		group.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, true));

		lblCreatedBy = new Label(group, SWT.WRAP);
		GridData gd_lblCreatedBy = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		gd_lblCreatedBy.widthHint = 360;
		lblCreatedBy.setLayoutData(gd_lblCreatedBy);
		lblCreatedBy.setText(settings.msg("dialog.about.createdby", new Object[] {Constants.PROGRAM_NAME_FULL}));

		lblStats = new Label(group, SWT.WRAP);
		GridData gd_lblStats = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		gd_lblStats.widthHint = 360;
		lblStats.setLayoutData(gd_lblStats);
		lblStats.setText(settings.msg("dialog.about.info", new Object[] {PlatformUtils.JVM_ARCH, PlatformUtils.JVM_VERSION, PlatformUtils.OS_NAME, PlatformUtils.OS_VERSION, PlatformUtils.OS_ARCH,
				HsqlDatabaseProperties.PRODUCT_NAME, HsqlDatabaseProperties.THIS_FULL_VERSION, String.valueOf(SWT.getVersion()), SWT.getPlatform()}));

		lblThanks = new Label(group, SWT.WRAP);
		GridData gd_lblThanks = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		gd_lblThanks.widthHint = 360;
		lblThanks.setLayoutData(gd_lblThanks);
		lblThanks.setText(settings.msg("dialog.about.thanks"));

		Link link = new Link(group, SWT.NONE);
		link.setText("<a href=\"http://home.quicknet.nl/qn/prive/blankendaalr/dbgl/\">" + settings.msg("dialog.about.website") + "</a>");
		link.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				PlatformUtils.openForBrowsing(event.text);
			}
		});

		Button btnOk = new Button(group, SWT.NONE);
		btnOk.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, true));
		btnOk.setText(settings.msg("button.ok"));
		btnOk.setFocus();
		shell.setDefaultButton(btnOk);
		btnOk.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				shell.close();
			}
		});
	}
}
