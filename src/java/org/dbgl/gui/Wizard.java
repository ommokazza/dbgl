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

import java.util.Vector;
import org.dbgl.model.conf.Settings;
import org.dbgl.util.UIThread;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;


public abstract class Wizard extends Dialog {

	protected String dialogTitle, dialog;
	protected int stepNr;
	protected Vector<Control> steps;
	protected UIThread extensiveJobThread;
	protected boolean hasExtensiveJob;
	protected Composite wizardPanel;
	protected StackLayout wizardLayout;
	protected Button back, next, cancel;
	protected Shell shell;
	protected Settings settings;
	protected Object result;

	public Wizard(Shell arg0, int arg1, String dialogTitle, String dialog, boolean hasExtensiveJob) {
		super(arg0, arg1);
		settings = Settings.getInstance();
		stepNr = 0;
		steps = new Vector<Control>();
		this.hasExtensiveJob = hasExtensiveJob;
		this.dialogTitle = dialogTitle;
		this.dialog = dialog;
	}

	final protected void createContents() {
		shell = new Shell(getParent(), SWT.TITLE | SWT.CLOSE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setText(dialogTitle);
		shell.setLayout(new GridLayout(4, false));
		shell.addControlListener(new SizeControlAdapter(shell, dialog));

		wizardPanel = new Composite(shell, SWT.NONE);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 4;
		wizardPanel.setLayoutData(gd);

		wizardLayout = new StackLayout();
		wizardPanel.setLayout(wizardLayout);

		fillPages();

		back = new Button(shell, SWT.PUSH);
		next = new Button(shell, SWT.PUSH);
		back.setText(settings.msg("button.back"));
		next.setText(settings.msg("button.next"));

		final GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.widthHint = GeneralPurposeGUI.getWidth(back, next);

		back.setLayoutData(gridData);
		next.setLayoutData(gridData);
		back.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				previousStep();
			}
		});
		next.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				nextStep();
			}
		});

		cancel = new Button(shell, SWT.PUSH);
		cancel.setText(settings.msg("button.cancel"));
		final GridData gridData2 = new GridData();
		gridData2.horizontalAlignment = SWT.FILL;
		gridData2.widthHint = GeneralPurposeGUI.getWidth(cancel);
		gridData2.horizontalIndent = 30;
		cancel.setLayoutData(gridData2);
		cancel.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				shell.close();
			}
		});

		shell.setDefaultButton(next);
		showCurrentStep();
	}

	protected abstract void fillPages();

	public Object open() {
		if (init()) {
			createContents();
			shell.open();
			shell.layout();
			Display display = getParent().getDisplay();
			while (!shell.isDisposed()) {
				if (extensiveJobThread != null && !extensiveJobThread.isAlive() && !next.getEnabled())
					nextStep();
				if (!display.readAndDispatch())
					display.sleep();
			}
		}
		onExit();
		return result;
	}

	protected boolean init() {
		return true;
	}

	protected void onExit() {}

	protected boolean isValidInput() {
		return true;
	}

	protected boolean actionAfterNext() {
		return true;
	}

	protected int stepSize(boolean up) {
		return 1;
	}

	final protected boolean isFinished() {
		if (hasExtensiveJob) {
			return (stepNr >= steps.size() + 1) && (extensiveJobThread != null && !extensiveJobThread.isAlive());
		} else {
			return (stepNr >= steps.size() - 1);
		}
	}

	final protected void nextStep() {
		if (isValidInput() && actionAfterNext()) {
			if (isFinished()) {
				shell.close();
			} else {
				stepNr += stepSize(true);
				showCurrentStep();
			}
		}
	}

	final protected void previousStep() {
		stepNr -= stepSize(false);
		showCurrentStep();
	}

	final protected void showCurrentStep() {
		if (stepNr < steps.size()) {
			wizardLayout.topControl = steps.get(stepNr);
			wizardPanel.layout();
		}

		boolean jobReady = (extensiveJobThread != null);
		boolean jobFinished = isFinished();

		back.setEnabled((stepNr > 0) && !jobReady);
		next.setEnabled(!jobReady || jobFinished);
		cancel.setEnabled(!jobReady);

		next.setText(jobFinished ? settings.msg("button.finish"): settings.msg("button.next"));

		if (stepNr > 0)
			wizardLayout.topControl.setFocus();

		if (jobReady && !jobFinished)
			extensiveJobThread.start();
	}

	final protected void addStep(Control c) {
		c.setParent(wizardPanel);
		steps.addElement(c);
	}
}
