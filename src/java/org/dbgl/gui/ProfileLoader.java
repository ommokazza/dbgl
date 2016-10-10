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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.dbgl.interfaces.Configurable;
import org.dbgl.model.ExpProfile;
import org.dbgl.model.Profile;
import org.dbgl.model.conf.Settings;
import org.dbgl.util.LoaderThread;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


public final class ProfileLoader extends Dialog {

	private Shell shell;
	private ProgressBar progressBar;

	private List<Profile> profs;
	private LoaderThread job = null;
	private List<ExpProfile> result = null;
	private boolean combine;

	private Settings settings;
	private Label status;
	private Text log;
	private Button okButton, cancelButton;

	public ProfileLoader(final Shell parent, final int style) {
		super(parent, style);
	}

	public ProfileLoader(Shell parent, List<Profile> profs, boolean combine) {
		this(parent, SWT.NONE);
		this.profs = profs;
		this.combine = combine;
		this.settings = Settings.getInstance();
	}

	public ProfileLoader(final Shell parent) {
		this(parent, SWT.NONE);
	}

	public Object open() {
		createContents();
		shell.open();
		shell.layout();

		final Display display = getParent().getDisplay();

		try {
			job = new LoaderThread(log, progressBar, status, profs, combine);
			job.start();
		} catch (SQLException e) {
			GeneralPurposeDialogs.warningMessage(shell, e);
			shell.close();
		}
		boolean finishedOnce = false;
		while (!shell.isDisposed()) {
			if (job != null && !job.isAlive() && !finishedOnce) {
				if (!job.isEverythingOk()) {
					String msg = settings.msg("dialog.profileloader.error.reading");
					if (!job.getResult().isEmpty()) {
						okButton.setEnabled(true);
						msg += "\n\n" + settings.msg("dialog.profileloader.confirm.continue", new Object[] {job.getResult().size()});
					}
					GeneralPurposeDialogs.warningMessage(shell, msg);
					status.setText(settings.msg("dialog.migration.reviewlog"));
					status.pack();
				} else {
					result = job.getResult();
					shell.close();
				}
				finishedOnce = true;
			}
			if (!display.readAndDispatch())
				display.sleep();
		}
		return result;
	}

	protected void createContents() {
		shell = new Shell(getParent(), SWT.TITLE | SWT.CLOSE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setLayout(new GridLayout());
		shell.addControlListener(new SizeControlAdapter(shell, "profileloaderdialog"));
		shell.setText(settings.msg("dialog.profileloader.title", new Object[] {profs.size()}));

		final Group progressGroup = new Group(shell, SWT.NONE);
		progressGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		progressGroup.setText(settings.msg("dialog.migration.progress"));
		progressGroup.setLayout(new GridLayout());

		progressBar = new ProgressBar(progressGroup, SWT.NONE);
		progressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		progressBar.setMaximum(profs.size());

		status = new Label(progressGroup, SWT.NONE);
		status.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		log = new Text(progressGroup, SWT.V_SCROLL | SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.H_SCROLL);
		log.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		final Composite buttonComposite = new Composite(shell, SWT.NONE);
		buttonComposite.setLayout(new GridLayout(2, true));

		okButton = new Button(buttonComposite, SWT.NONE);
		shell.setDefaultButton(okButton);
		final GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.widthHint = 80;
		okButton.setLayoutData(gridData);
		okButton.setText(settings.msg("button.ok"));
		okButton.setEnabled(false);
		okButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				result = job.getResult();
				shell.close();
			}
		});

		cancelButton = new Button(buttonComposite, SWT.NONE);
		final GridData gridData_1 = new GridData();
		gridData_1.horizontalAlignment = SWT.FILL;
		cancelButton.setLayoutData(gridData_1);
		cancelButton.setText(settings.msg("button.cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				shell.close();
			}
		});
	}

	public List<ExpProfile> getResult() {
		return result;
	}

	public List<Configurable> getResultAsConfigurables() {
		return new ArrayList<Configurable>(result);
	}

	public ExpProfile getMultiProfileCombined() {
		return job.getMultiProfileCombined();
	}
}
