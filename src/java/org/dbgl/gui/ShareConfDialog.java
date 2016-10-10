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

import org.apache.commons.lang3.StringUtils;
import org.dbgl.model.DosboxVersion;
import org.dbgl.model.conf.SectionsWrapper;
import org.dbgl.model.conf.Settings;
import org.dbgl.model.conf.SharedConf;
import org.dbgl.util.PlatformUtils;
import org.dbgl.util.StringRelatedUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import swing2swt.layout.BorderLayout;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;


public final class ShareConfDialog extends Dialog {

	private Text author, gameTitle, gameVersion, gameYear, explanation, notes;;
	private Tree incrConf;
	private SharedConf sharedConf;
	private Shell shell;
	private Settings settings;

	public ShareConfDialog(final Shell parent, String gameTitle, String gameYear, String incrConf, String fullConf, DosboxVersion dosboxVersion) {
		super(parent, SWT.NONE);
		sharedConf = new SharedConf("", "", gameTitle, "", gameYear, incrConf, fullConf, "", dosboxVersion.getTitle(), dosboxVersion.getVersion());
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
		return sharedConf;
	}

	private void createContents() {
		shell = new Shell(getParent(), SWT.TITLE | SWT.CLOSE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setLayout(new BorderLayout(0, 0));
		shell.addControlListener(new SizeControlAdapter(shell, "shareconfdialog"));
		shell.setText(settings.msg("dialog.confsharing.title"));

		final TabFolder tabFolder = new TabFolder(shell, SWT.NONE);

		TabItem infoTabItem = new TabItem(tabFolder, SWT.NONE);
		infoTabItem.setText(settings.msg("dialog.confsharing.tab.info"));

		final Composite composite = new Composite(tabFolder, SWT.NONE);
		composite.setLayout(new GridLayout(3, false));
		infoTabItem.setControl(composite);

		final Label authorLabel = new Label(composite, SWT.NONE);
		authorLabel.setText(settings.msg("dialog.confsharing.author"));
		author = new Text(composite, SWT.BORDER);
		author.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		final Label gameTitleLabel = new Label(composite, SWT.NONE);
		gameTitleLabel.setText(settings.msg("dialog.confsharing.gametitle"));
		gameTitle = new Text(composite, SWT.BORDER);
		gameTitle.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		final Label gameVersionLabel = new Label(composite, SWT.NONE);
		gameVersionLabel.setText(settings.msg("dialog.confsharing.gameversion"));
		gameVersion = new Text(composite, SWT.BORDER);
		gameVersion.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		final Label gameYearLabel = new Label(composite, SWT.NONE);
		gameYearLabel.setText(settings.msg("dialog.confsharing.gameyear"));
		gameYear = new Text(composite, SWT.BORDER);
		gameYear.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		final Label explanationLabel = new Label(composite, SWT.NONE);
		explanationLabel.setText(settings.msg("dialog.confsharing.explanation"));
		incrConf = new Tree(composite, SWT.BORDER | SWT.CHECK);
		incrConf.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		explanation = new Text(composite, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.WRAP);
		explanation.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		final Label notesLabel = new Label(composite, SWT.NONE);
		notesLabel.setText(settings.msg("dialog.confsharing.notes"));
		notes = new Text(composite, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.WRAP);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gd.minimumHeight = 3 * notes.getLineHeight();
		notes.setLayoutData(gd);

		final Composite composite_7 = new Composite(shell, SWT.NONE);
		composite_7.setLayout(new GridLayout(2, true));
		composite_7.setLayoutData(BorderLayout.SOUTH);

		final Button okButton = new Button(composite_7, SWT.NONE);
		shell.setDefaultButton(okButton);
		okButton.setText(settings.msg("button.ok"));
		okButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				if (!isValid()) {
					return;
				}
				try {
					sharedConf.setAuthor(author.getText());
					sharedConf.setGameTitle(gameTitle.getText());
					sharedConf.setGameVersion(gameVersion.getText());
					sharedConf.setGameYear(gameYear.getText());
					sharedConf.setIncrConf(extractConfFromTree(incrConf));
					sharedConf.setExplanation(explanation.getText());
					sharedConf.setNotes(notes.getText());

					Client client = ClientBuilder.newClient();
					SharedConf result = client.target(settings.getSettings().getValue("confsharing", "endpoint")).path("/submissions").request().post(
						Entity.entity(sharedConf, MediaType.APPLICATION_XML), SharedConf.class);
					GeneralPurposeDialogs.infoMessage(shell, settings.msg("dialog.confsharing.confirmation", new Object[] {result.getGameTitle()}));
					client.close();
				} catch (Exception e) {
					GeneralPurposeDialogs.fatalMessage(shell, settings.msg("dialog.confsharing.error.submit", new Object[] {StringRelatedUtils.toString(e)}), e);
				}
				settings.getSettings().setValue("confsharing", "author", author.getText());
				shell.close();
			}
		});

		final Button cancelButton = new Button(composite_7, SWT.NONE);
		cancelButton.setText(settings.msg("button.cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				sharedConf = null;
				shell.close();
			}
		});

		final GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.widthHint = GeneralPurposeGUI.getWidth(okButton, cancelButton);
		okButton.setLayoutData(gridData);
		cancelButton.setLayoutData(gridData);

		// init values
		author.setText(settings.getSettings().getValue("confsharing", "author"));
		gameTitle.setText(sharedConf.getGameTitle());
		gameYear.setText(sharedConf.getGameYear());
		fillStringIntoTree(this.sharedConf.getIncrConf(), incrConf);

		if (StringUtils.isBlank(sharedConf.getIncrConf()))
			explanation.setText("N/A");

		if (StringUtils.isBlank(gameTitle.getText()))
			gameTitle.setFocus();
		else if (StringUtils.isBlank(gameYear.getText()))
			gameYear.setFocus();
		else if (StringUtils.isBlank(explanation.getText()))
			explanation.setFocus();
		else
			notes.setFocus();
	}

	private boolean isValid() {
		GeneralPurposeDialogs.initErrorDialog();
		if (StringUtils.isBlank(author.getText()))
			GeneralPurposeDialogs.addError(settings.msg("dialog.confsharing.required.author"), author);
		if (StringUtils.isBlank(gameTitle.getText()))
			GeneralPurposeDialogs.addError(settings.msg("dialog.confsharing.required.gametitle"), gameTitle);
		if (StringUtils.isBlank(gameYear.getText()))
			GeneralPurposeDialogs.addError(settings.msg("dialog.confsharing.required.gameyear"), gameYear);
		if (StringUtils.isBlank(explanation.getText()))
			GeneralPurposeDialogs.addError(settings.msg("dialog.confsharing.required.explanation"), explanation);
		return !GeneralPurposeDialogs.displayErrorDialog(shell);
	}

	private void fillStringIntoTree(String conf, Tree tree) {
		String[] lines = StringUtils.split(conf, PlatformUtils.EOLN);

		TreeItem sectionItem = null;
		for (String s: lines) {
			if (s.startsWith("[")) {
				sectionItem = new TreeItem(tree, SWT.NONE);
				sectionItem.setText(s);
				sectionItem.setChecked(true);
			} else {
				TreeItem node = new TreeItem(sectionItem, SWT.NONE);
				node.setText(s);
				node.setChecked(true);
			}
		}

		for (TreeItem item: tree.getItems())
			item.setExpanded(true);

		tree.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (event.detail == SWT.CHECK) {
					TreeItem tItem = (TreeItem)event.item;
					if (tItem.getParentItem() == null) {
						tItem.setGrayed(false);
						for (TreeItem item: tItem.getItems())
							item.setChecked(tItem.getChecked());
					} else {
						TreeItem parent = tItem.getParentItem();
						int checkedCount = getCheckedItemsCount(parent);
						parent.setChecked(checkedCount > 0);
						parent.setGrayed(checkedCount > 0 && checkedCount < parent.getItemCount());
					}
				}
			}
		});
	}

	private int getCheckedItemsCount(final TreeItem treeItem) {
		int result = 0;
		for (TreeItem item: treeItem.getItems())
			if (item.getChecked())
				result++;
		return result;
	}

	private String extractConfFromTree(Tree tree) {
		SectionsWrapper sections = new SectionsWrapper();
		for (TreeItem sectionItem: tree.getItems()) {
			if (sectionItem.getChecked()) {
				for (TreeItem node: sectionItem.getItems()) {
					if (node.getChecked()) {
						String[] v = StringUtils.split(node.getText(), '=');
						sections.setValue(StringUtils.substring(sectionItem.getText(), 1, -1), v[0], v[1]);
					}
				}
			}
		}
		return sections.toString();
	}
}
