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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.dbgl.db.Database;
import org.dbgl.model.LogEntry;
import org.dbgl.model.LogEntry.EntityType;
import org.dbgl.model.conf.Settings;
import org.dbgl.swtdesigner.SWTImageManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;


public final class LogDialog extends Dialog {

	private Shell shell;
	private Settings settings;
	private List<LogEntry> logEntries;
	private Map<String, String> filterClauses;
	private String orderByClause;
	private LogEntry clickedEntry;
	private Integer clickedColumn;

	public LogDialog(final Shell parent, final int style) {
		super(parent, style);
		settings = Settings.getInstance();
		filterClauses = new LinkedHashMap<String, String>();
		orderByClause = " ORDER BY ID";
	}

	public LogDialog(final Shell parent) {
		this(parent, SWT.NONE);
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
		shell = new Shell(getParent(), SWT.TITLE | SWT.CLOSE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setLayout(new GridLayout(3, false));
		shell.addControlListener(new SizeControlAdapter(shell, "log"));
		shell.setText(settings.msg("dialog.log.title"));

		final Table table = new Table(shell, SWT.BORDER | SWT.FULL_SELECTION);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));

		final String[] titles = {settings.msg("dialog.log.columns.time"), settings.msg("dialog.log.columns.event"), settings.msg("dialog.log.columns.entitytype"),
				settings.msg("dialog.log.columns.entitytitle"), settings.msg("dialog.log.columns.entityid")};
		for (int i = 0; i < titles.length; i++) {
			final int columnId = i;
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(titles[i]);
			column.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					switch (columnId) {
						case 0:
							orderByClause = " ORDER BY ID";
							break;
						case 1:
							orderByClause = " ORDER BY EVENT";
							break;
						case 2:
							orderByClause = " ORDER BY ENTITY_TYPE";
							break;
						case 3:
							orderByClause = " ORDER BY ENTITY_TITLE";
							break;
						case 4:
							orderByClause = " ORDER BY ENTITY_ID";
							break;
						default:
					}
					TableColumn sortColumn = (TableColumn)e.widget;
					int sortDirection = SWT.UP;
					if (sortColumn == table.getSortColumn() && table.getSortDirection() == SWT.UP)
						sortDirection = SWT.DOWN;
					if (sortDirection == SWT.DOWN)
						orderByClause += " DESC";
					repopulateEntries(table, sortColumn, sortDirection);
				}
			});
		}

		final Menu menu = new Menu(shell, SWT.POP_UP);
		menu.addListener(SWT.Show, new Listener() {
			public void handleEvent(Event event) {
				MenuItem[] menuItems = menu.getItems();
				for (int i = 0; i < menuItems.length; i++)
					menuItems[i].dispose();

				if ((clickedEntry != null) && (clickedColumn != null)) {
					MenuItem mi = new MenuItem(menu, SWT.PUSH);
					switch (clickedColumn) {
						case 0:
							mi.setText(titles[clickedColumn] + ": " + Settings.toString(clickedEntry.getTime()));
							break;
						case 1:
							mi.setText(titles[clickedColumn] + ": " + getEventName(clickedEntry.getEvent()));
							break;
						case 2:
							mi.setText(titles[clickedColumn] + ": " + getEntityTypeName(clickedEntry.getEntityType()));
							break;
						case 3:
							mi.setText(titles[clickedColumn] + ": " + clickedEntry.getEntityTitle());
							break;
						case 4:
							mi.setText(titles[clickedColumn] + ": " + clickedEntry.getEntityId());
							break;
						default:
							break;
					}
					mi.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(final SelectionEvent event) {
							MenuItem mi = (MenuItem)event.widget;
							switch (clickedColumn) {
								case 0:
									Calendar cal = Calendar.getInstance();
									cal.setTime(clickedEntry.getTime());
									filterClauses.put("YEAR(TIME)=" + cal.get(Calendar.YEAR) + " AND MONTH(TIME)=" + (cal.get(Calendar.MONTH) + 1) + " AND DAY(TIME)=" + cal.get(Calendar.DAY_OF_MONTH),
										mi.getText());
									break;
								case 1:
									filterClauses.put("EVENT=" + clickedEntry.getEvent().ordinal(), mi.getText());
									break;
								case 2:
									filterClauses.put("ENTITY_TYPE=" + clickedEntry.getEntityType().ordinal(), mi.getText());
									break;
								case 3:
									filterClauses.put("ENTITY_TITLE='" + clickedEntry.getEntityTitle() + "'", mi.getText());
									break;
								case 4:
									filterClauses.put("ENTITY_ID=" + clickedEntry.getEntityId(), mi.getText());
									break;
								default:
									break;
							}
							repopulateEntries(table, table.getSortColumn(), table.getSortDirection());
						}
					});

					if (!filterClauses.isEmpty())
						new MenuItem(menu, SWT.SEPARATOR);
				}

				for (final Map.Entry<String, String> filterClause: filterClauses.entrySet()) {
					MenuItem ci = new MenuItem(menu, SWT.PUSH);
					ci.setText(filterClause.getValue());
					ci.setImage(SWTImageManager.getResourceImage(shell.getDisplay(), SWTImageManager.IMG_DELETE));
					ci.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(final SelectionEvent event) {
							filterClauses.remove(filterClause.getKey());
							repopulateEntries(table, table.getSortColumn(), table.getSortDirection());
						}
					});
				}
			}
		});
		table.setMenu(menu);
		table.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {
				clickedEntry = null;
				clickedColumn = null;
				final Point pt = new Point(event.x, event.y);
				final TableItem item = table.getItem(pt);
				if (item == null)
					return;
				for (int i = 0; i < titles.length; i++) {
					if (item.getBounds(i).contains(pt)) {
						clickedEntry = logEntries.get(table.indexOf(item));
						clickedColumn = i;
						break;
					}
				}
			}
		});

		repopulateEntries(table, table.getColumn(0), SWT.UP);
		for (int i = 0; i < titles.length; i++)
			table.getColumn(i).pack();

		final Button okButton = new Button(shell, SWT.NONE);
		okButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				shell.close();
			}
		});
		shell.setDefaultButton(okButton);
		okButton.setText(settings.msg("button.ok"));

		final Button enableButton = new Button(shell, SWT.TOGGLE);
		enableButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				settings.getSettings().setBooleanValue("log", "enabled", enableButton.getSelection());
				enableButton.setText(settings.msg(enableButton.getSelection() ? "dialog.log.enabled": "dialog.log.disabled"));
			}
		});
		enableButton.setSelection(settings.getSettings().getBooleanValue("log", "enabled"));
		enableButton.setText(settings.msg(enableButton.getSelection() ? "dialog.log.enabled": "dialog.log.disabled"));

		final Button clearButton = new Button(shell, SWT.NONE);
		clearButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				if (GeneralPurposeDialogs.confirmMessage(shell, settings.msg("dialog.log.confirm.clear"))) {
					try {
						Database.getInstance().clearLog();
						table.removeAll();
					} catch (SQLException e) {
						GeneralPurposeDialogs.warningMessage(shell, e);
					}
				}
			}
		});
		clearButton.setText(settings.msg("button.clear"));

		final GridData gridData = new GridData(SWT.BEGINNING, SWT.FILL, true, false);
		gridData.widthHint = GeneralPurposeGUI.getWidth(okButton, clearButton);
		okButton.setLayoutData(gridData);
		GridData gridData2 = new GridData();
		gridData2.widthHint = GeneralPurposeGUI.getWidth(enableButton, clearButton);
		enableButton.setLayoutData(gridData2);
		final GridData gridData3 = new GridData();
		gridData3.widthHint = gridData2.widthHint;
		clearButton.setLayoutData(gridData3);
	}

	private void repopulateEntries(final Table table, final TableColumn sortColumn, final int sortDirection) {
		int selLogEntryId = table.getSelectionIndex() == -1 ? -1: logEntries.get(table.getSelectionIndex()).getId();
		try {
			String whereClause = filterClauses.isEmpty() ? StringUtils.EMPTY: " WHERE " + StringUtils.join(filterClauses.keySet(), " AND ");
			logEntries = Database.getInstance().readLogEntries(whereClause, orderByClause);
		} catch (SQLException e) {
			GeneralPurposeDialogs.warningMessage(shell, e);
			logEntries = new ArrayList<LogEntry>();
		}
		table.removeAll();
		table.setSortColumn(sortColumn);
		table.setSortDirection(sortDirection);
		table.setItemCount(logEntries.size());
		int idx = logEntries.size() - 1;
		for (int i = 0; i < logEntries.size(); i++) {
			LogEntry entry = logEntries.get(i);
			TableItem item = table.getItem(i);
			item.setText(0, Settings.toString(entry.getTime(), DateFormat.MEDIUM));
			item.setText(1, getEventName(entry.getEvent()));
			item.setText(2, getEntityTypeName(entry.getEntityType()));
			item.setText(3, entry.getEntityTitle());
			item.setText(4, String.valueOf(entry.getEntityId()));
			if (selLogEntryId == logEntries.get(i).getId())
				idx = i;
		}
		table.setSelection(idx);
		table.showSelection();
	}

	private String getEventName(final LogEntry.Event event) {
		switch (event) {
			case ADD:
				return settings.msg("dialog.log.columns.event.add");
			case EDIT:
				return settings.msg("dialog.log.columns.event.edit");
			case REMOVE:
				return settings.msg("dialog.log.columns.event.remove");
			case DUPLICATE:
				return settings.msg("dialog.log.columns.event.duplicate");
			case RUN:
				return settings.msg("dialog.log.columns.event.run");
			case SETUP:
				return settings.msg("dialog.log.columns.event.setup");
			default:
				return StringUtils.EMPTY;
		}
	}

	private String getEntityTypeName(final EntityType type) {
		switch (type) {
			case PROFILE:
				return settings.msg("dialog.log.columns.entitytype.profile");
			case DOSBOXVERSION:
				return settings.msg("dialog.template.dosboxversion");
			case TEMPLATE:
				return settings.msg("dialog.profile.template");
			case FILTER:
				return settings.msg("dialog.log.columns.entitytype.filter");
			default:
				return StringUtils.EMPTY;
		}
	}
}
