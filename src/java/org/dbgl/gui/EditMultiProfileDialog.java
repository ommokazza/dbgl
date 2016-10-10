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

import java.util.Collections;
import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.dbgl.db.Database;
import org.dbgl.interfaces.Configurable;
import org.dbgl.model.ExpProfile;
import org.dbgl.model.conf.Settings;
import org.dbgl.util.StringRelatedUtils;
import org.dbgl.util.searchengine.SearchEngineThread;
import org.dbgl.util.searchengine.WebSearchEngine;
import swing2swt.layout.BorderLayout;


public class EditMultiProfileDialog extends EditProfileDialog {

	private Label profileLabel;
	private ProgressBar progressBar;
	private Text logText;

	private static final int AMOUNT_OF_THREADS = 4;
	private Thread[] mbt = new Thread[AMOUNT_OF_THREADS];

	private java.util.List<Configurable> profs;
	private WebSearchEngine engine;

	public EditMultiProfileDialog(Shell parent) {
		super(parent);
	}

	public void setData(final java.util.List<Configurable> profs, final WebSearchEngine engine) {
		this.profs = profs;
		this.engine = engine;
	}

	@Override
	public Object open() {
		settings = Settings.getInstance();
		dbase = Database.getInstance();

		createContents();
		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();

		while (!shell.isDisposed()) {
			boolean alldone = true;
			if (mbt != null) {
				for (int i = 0; i < AMOUNT_OF_THREADS; i++) {
					alldone &= (mbt[i] != null) && (!mbt[i].isAlive());
				}
			}
			if (alldone) {
				if ((logText.getCharCount() > 0) && (!cancelButton.getText().equals(settings.msg("button.finish")))) {
					GeneralPurposeDialogs.infoMessage(shell, settings.msg("dialog.multiprofile.reviewlog"));
					profileLabel.setText(settings.msg("dialog.multiprofile.reviewlog"));
					profileLabel.pack();
					cancelButton.setText(settings.msg("button.finish"));
					shell.setDefaultButton(cancelButton);
					progressBar.setSelection(progressBar.getMaximum());
					mbt = new Thread[AMOUNT_OF_THREADS];
				} else {
					shell.close();
				}
			}

			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}

		return result;
	}

	@Override
	protected void createContents() {
		shell = new Shell(getParent(), SWT.TITLE | SWT.CLOSE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setLayout(new BorderLayout(0, 0));
		shell.addControlListener(new SizeControlAdapter(shell, "multiprofiledialog"));

		shell.setText(settings.msg("dialog.multiprofile.title.edit", new Object[] {profs.size()}));

		final TabFolder tabFolder = new TabFolder(shell, SWT.NONE);
		createGeneralTab(tabFolder);

		createOkCancelButtons();
		okButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {

				okButton.setEnabled(false);
				cancelButton.setEnabled(false);

				int tasks = profs.size();
				if (engine != null) {
					tasks += profs.size();
				}
				progressBar.setMaximum(tasks);

				int profileNumber = 0;
				for (Configurable cprof: profs) {
					ExpProfile prof = (ExpProfile)cprof;
					final StringBuffer messageLog = new StringBuffer();

					profileLabel.setText(Settings.getInstance().msg("dialog.multiprofile.updating", new Object[] {prof.getTitle()}));
					profileLabel.pack();

					try {
						prof.getConf().injectOrUpdateProfile(prof); // force captures folder to be set
						prof.getConf().save();
						dbase.addOrEditProfile(prof.getTitle(), prof.getDeveloperName(), prof.getPublisherName(), prof.getGenre(), prof.getYear(), prof.getStatus(), prof.getNotes(), prof.isDefault(),
							prof.getSetup(), prof.getSetupParameters(), prof.getDbversionId(), prof.getLinks(), prof.getLinkTitles(), prof.getCustomStrings(), prof.getCustomInts(), prof.getId());

					} catch (Exception e) {
						messageLog.append(StringRelatedUtils.toString(e));
					}

					if (messageLog.length() > 0) {
						logText.append(messageLog.append('\n').toString());
					}
					progressBar.setSelection(++profileNumber);
				}

				final List<Configurable> synchronizedList = Collections.synchronizedList(profs);
				cancelButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(final SelectionEvent event) {
						synchronizedList.clear();
						mbt = null;
					}
				});
				cancelButton.setEnabled(true);
				result = "";

				if (engine != null) {
					for (int i = 0; i < AMOUNT_OF_THREADS; i++) {
						mbt[i] = new SearchEngineThread(engine, synchronizedList, logText, progressBar, profileLabel);
						mbt[i].start();
					}
				} else {
					mbt = null;
				}
			}
		});
	}

	@Override
	protected void createGeneralTab(TabFolder tabFolder) {
		final TabItem generalTabItem = new TabItem(tabFolder, SWT.NONE);
		generalTabItem.setText(settings.msg("dialog.template.tab.general"));

		final Composite composite = new Composite(tabFolder, SWT.NONE);
		composite.setLayout(new GridLayout());
		generalTabItem.setControl(composite);

		final Group progressGroup = new Group(composite, SWT.NONE);
		progressGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		progressGroup.setText(settings.msg("dialog.dfendimport.progress"));
		progressGroup.setLayout(new GridLayout());

		progressBar = new ProgressBar(progressGroup, SWT.NONE);
		progressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		profileLabel = new Label(progressGroup, SWT.NONE);

		logText = new Text(progressGroup, SWT.V_SCROLL | SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.H_SCROLL);
		logText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}

	@Override
	protected void createOkCancelButtons() {
		final Composite composite_7 = new Composite(shell, SWT.NONE);
		composite_7.setLayout(new GridLayout(2, true));
		composite_7.setLayoutData(BorderLayout.SOUTH);

		okButton = new Button(composite_7, SWT.NONE);
		shell.setDefaultButton(okButton);
		okButton.setText(settings.msg("button.ok"));

		cancelButton = new Button(composite_7, SWT.NONE);
		cancelButton.setText(settings.msg("button.cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				if (result == null)
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
