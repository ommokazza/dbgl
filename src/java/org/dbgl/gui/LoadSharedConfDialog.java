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
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.dbgl.model.conf.Settings;
import org.dbgl.model.conf.SharedConf;
import org.dbgl.util.StringRelatedUtils;


public final class LoadSharedConfDialog extends Dialog {

	public final class SharedConfLoading {
		public SharedConf conf;
		public boolean reloadDosboxDefaults;
	}

	private String title;
	private java.util.List<SharedConf> confs;
	private SharedConfLoading result;
	private Shell shell;
	private Settings settings;

	public LoadSharedConfDialog(final Shell parent, final String title, final List<SharedConf> confs) {
		super(parent, SWT.NONE);
		this.title = title;
		this.confs = confs;
		settings = Settings.getInstance();
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
		shell.setLayout(new GridLayout(2, false));
		shell.addControlListener(new SizeControlAdapter(shell, "sharedconfbrowser"));
		shell.setText(settings.msg("dialog.loadsharedconf.title"));

		final SashForm sashForm = new SashForm(shell, SWT.HORIZONTAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		sashForm.setLayout(new FillLayout());

		final Table table = new Table(sashForm, SWT.BORDER | SWT.FULL_SELECTION);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);

		final Listener sortListener = new Listener() {
			public void handleEvent(Event e) {
				int selIndex = table.getSelectionIndex();
				SharedConf selWebProfile = confs.get(selIndex);
				TableColumn column = (TableColumn)e.widget;
				int index = (Integer)column.getData();
				switch (index) {
					case 0:
						Collections.sort(confs, new SharedConf.byTitle());
						break;
					case 1:
						Collections.sort(confs, new SharedConf.byYear());
						break;
					case 2:
						Collections.sort(confs, new SharedConf.byVersion());
						break;
					default: // do nothing
				}
				table.removeAll();
				populate(table);
				table.setSortColumn(column);
				table.setSortDirection(SWT.UP);
				for (int i = 0; i < confs.size(); i++) {
					if (selWebProfile == confs.get(i)) {
						table.setSelection(i);
						break;
					}
				}
			}
		};

		String[] titles = {settings.msg("dialog.profile.title"), settings.msg("dialog.profile.year"), settings.msg("dialog.confsharing.gameversion")};
		for (int i = 0; i < titles.length; i++) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(titles[i]);
			column.setData(i);
			column.addListener(SWT.Selection, sortListener);
			if (i == 0) {
				table.setSortColumn(column);
				table.setSortDirection(SWT.UP);
			}
			Collections.sort(confs, new SharedConf.byTitle());
		}

		final TabFolder tabFolder = new TabFolder(sashForm, SWT.NONE);
		sashForm.setWeights(new int[] {40, 60});

		TabItem infoTabItem = new TabItem(tabFolder, SWT.NONE);
		infoTabItem.setText(settings.msg("dialog.confsharing.tab.info"));

		final Composite composite = new Composite(tabFolder, SWT.NONE);
		composite.setLayout(new GridLayout(3, false));
		infoTabItem.setControl(composite);

		final Label authorLabel = new Label(composite, SWT.NONE);
		authorLabel.setText(settings.msg("dialog.confsharing.author"));
		final Text author = new Text(composite, SWT.BORDER);
		author.setEditable(false);
		author.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		final Label dosboxLabel = new Label(composite, SWT.NONE);
		dosboxLabel.setText(settings.msg("dialog.loadsharedconf.dosboxversion"));
		final Text dosbox = new Text(composite, SWT.BORDER);
		dosbox.setEditable(false);
		dosbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		final Label explanationLabel = new Label(composite, SWT.NONE);
		explanationLabel.setText(settings.msg("dialog.confsharing.explanation"));
		final Text incrConf = new Text(composite, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.H_SCROLL);
		incrConf.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		incrConf.setEditable(false);
		final Text explanation = new Text(composite, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.WRAP);
		explanation.setEditable(false);
		explanation.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		final Label notesLabel = new Label(composite, SWT.NONE);
		notesLabel.setText(settings.msg("dialog.confsharing.notes"));
		final Text notes = new Text(composite, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.WRAP);
		notes.setEditable(false);
		notes.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		final Button reloadDosboxDefaults = new Button(composite, SWT.CHECK);
		reloadDosboxDefaults.setText(settings.msg("dialog.loadsharedconf.reloaddefaults"));
		reloadDosboxDefaults.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 3, 1));

		table.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				displaySharedConfData(table, author, dosbox, incrConf, explanation, notes);
			}
		});

		table.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(final MouseEvent event) {
				result = new SharedConfLoading();
				result.conf = confs.get(table.getSelectionIndex());
				result.reloadDosboxDefaults = reloadDosboxDefaults.getSelection();
				shell.close();
			}
		});

		final Button okButton = new Button(shell, SWT.NONE);
		okButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				result = new SharedConfLoading();
				result.conf = confs.get(table.getSelectionIndex());
				result.reloadDosboxDefaults = reloadDosboxDefaults.getSelection();
				shell.close();
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

		// pre-fill data
		populate(table);
		for (int i = 0; i < titles.length; i++)
			table.getColumn(i).pack();
		table.setSelection(getEntryBestMatchIndex(title, confs));
		table.showSelection();
		displaySharedConfData(table, author, dosbox, incrConf, explanation, notes);
	}

	private void populate(final Table table) {
		for (SharedConf p: confs) {
			TableItem item = new TableItem(table, SWT.NONE);
			item.setText(0, p.getGameTitle());
			item.setText(1, p.getGameYear());
			item.setText(2, p.getGameVersion());
		}
	}

	public static int getEntryBestMatchIndex(final String search, final List<SharedConf> confs) {
		String[] titles = new String[confs.size()];
		for (int i = 0; i < confs.size(); i++)
			titles[i] = confs.get(i).getGameTitle();
		return StringRelatedUtils.findBestMatchIndex(search, titles);
	}

	private void displaySharedConfData(final Table table, final Text author, final Text dosbox, final Text incrConf, final Text explanation, final Text notes) {
		int selection = table.getSelectionIndex();
		if (selection != -1) {
			SharedConf conf = confs.get(selection);
			author.setText(conf.getAuthor());
			dosbox.setText(conf.getDosboxTitle() + " (" + conf.getDosboxVersion() + ")");
			incrConf.setText(conf.getIncrConf());
			explanation.setText(conf.getExplanation());
			notes.setText(conf.getNotes());
		}
	}
}
