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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.dbgl.model.WebProfile;
import org.dbgl.model.conf.Settings;
import org.dbgl.util.searchengine.WebSearchEngine;


public final class BrowseSearchEngineDialog extends Dialog {

	private String title;
	private java.util.List<WebProfile> profs;
	private WebSearchEngine engine;
	private Object result;
	private Shell shell;
	private Settings settings;

	public BrowseSearchEngineDialog(final Shell parent, final int style) {
		super(parent, style);
		profs = null;
		settings = Settings.getInstance();
	}

	public BrowseSearchEngineDialog(final Shell parent) {
		this(parent, SWT.NONE);
	}

	public void setProfilesToBrowse(final String title, final java.util.List<WebProfile> profs) {
		this.title = title;
		this.profs = profs;
	}

	public void setEngine(WebSearchEngine engine) {
		this.engine = engine;
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
		shell.addControlListener(new SizeControlAdapter(shell, "mobygamesbrowser"));
		shell.setText(settings.msg("dialog.searchenginebrowser.title", new String[] {engine.getName()}));

		final Table table = new Table(shell, SWT.BORDER | SWT.FULL_SELECTION);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		final Listener sortListener = new Listener() {
			public void handleEvent(Event e) {
				int selIndex = table.getSelectionIndex();
				WebProfile selWebProfile = profs.get(selIndex);
				TableColumn column = (TableColumn)e.widget;
				int index = (Integer)column.getData();
				switch (index) {
					case 0:
						Collections.sort(profs, new WebProfile.byTitle());
						break;
					case 1:
						Collections.sort(profs, new WebProfile.byYear());
						break;
					case 2:
						Collections.sort(profs, new WebProfile.byPlatform());
						break;
					default: // do nothing
				}
				table.removeAll();
				populate(table);
				table.setSortColumn(column);
				table.setSortDirection(SWT.UP);
				for (int i = 0; i < profs.size(); i++) {
					if (selWebProfile == profs.get(i)) {
						table.setSelection(i);
						break;
					}
				}
			}
		};

		String[] titles = {settings.msg("dialog.profile.title"), settings.msg("dialog.profile.year"), settings.msg("dialog.searchenginebrowser.column.platform")};
		for (int i = 0; i < titles.length; i++) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(titles[i]);
			column.setData(i);
			column.addListener(SWT.Selection, sortListener);
			if (i == (titles.length - 1)) {
				table.setSortColumn(column);
				table.setSortDirection(SWT.UP);
			}
		}

		populate(table);
		for (int i = 0; i < titles.length; i++) {
			table.getColumn(i).pack();
		}
		table.setSelection(WebSearchEngine.getEntryBestMatchIndex(title, profs));
		table.showSelection();
		table.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(final MouseEvent event) {
				result = table.getSelectionIndex();
				shell.close();
			}
		});

		final Button okButton = new Button(shell, SWT.NONE);
		okButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				result = table.getSelectionIndex();
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
	}

	private void populate(final Table table) {
		for (WebProfile p: profs) {
			TableItem item = new TableItem(table, SWT.NONE);
			item.setText(0, p.getTitle());
			item.setText(1, p.getYear());
			item.setText(2, p.getPlatform());
		}
	}
}
