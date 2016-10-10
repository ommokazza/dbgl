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
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;
import org.dbgl.db.Database;
import org.dbgl.model.DosboxVersion;
import org.dbgl.model.Filter;
import org.dbgl.model.Profile;
import org.dbgl.model.Constants;
import org.dbgl.model.conf.Settings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import swing2swt.layout.BorderLayout;


public final class EditFilterDialog extends Dialog {

	private static final String EMPTY_FIELD = "[ ]";

	private Text filter;
	private Text title;
	private String prevTitle;
	private Set<Integer> selectedProfileIds;
	private java.util.List<Profile> profilesList;
	private java.util.List<DosboxVersion> dbversionsList;
	private Database dbase;
	private Object result;
	private Shell shell;
	private Settings settings;

	public EditFilterDialog(final Shell parent) {
		super(parent, SWT.NONE);
	}

	public void setFilter(final Filter filter) {
		this.result = filter;
	}

	public void setIds(Set<Integer> selectedProfileIds) {
		this.selectedProfileIds = selectedProfileIds;
	}

	public Object open() {
		dbase = Database.getInstance();
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
		return result;
	}

	private void createContents() {
		shell = new Shell(getParent(), SWT.TITLE | SWT.CLOSE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setLayout(new BorderLayout(0, 0));
		shell.addControlListener(new SizeControlAdapter(shell, "filterdialog"));

		if (result == null) {
			shell.setText(settings.msg("dialog.filter.title.add"));
		} else {
			// meaning we are essentially editing an existing filter
			shell.setText(settings.msg("dialog.filter.title.edit", new Object[] {((Filter)result).getTitle(), ((Filter)result).getId()}));
		}

		final TabFolder tabFolder = new TabFolder(shell, SWT.NONE);

		TabItem infoTabItem = new TabItem(tabFolder, SWT.NONE);
		infoTabItem.setText(settings.msg("dialog.filter.tab.info"));

		final Composite composite = new Composite(tabFolder, SWT.NONE);
		composite.setLayout(new GridLayout(3, false));
		infoTabItem.setControl(composite);

		final Label titleLabel = new Label(composite, SWT.NONE);
		titleLabel.setText(settings.msg("dialog.filter.title"));

		final SashForm sashForm = new SashForm(composite, SWT.HORIZONTAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 2));
		sashForm.setLayout(new FillLayout());

		final Composite leftComposite = new Composite(sashForm, SWT.NONE);
		GridLayout gl1 = new GridLayout(1, false);
		gl1.marginWidth = gl1.marginHeight = 0;
		leftComposite.setLayout(gl1);

		title = new Text(leftComposite, SWT.BORDER);
		title.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		final Tree tree = new Tree(sashForm, SWT.BORDER | SWT.CHECK);
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 1, 2));

		sashForm.setWeights(new int[] {60, 40});
		try {
			profilesList = dbase.readProfilesList(StringUtils.EMPTY, StringUtils.EMPTY);
			dbversionsList = dbase.readDosboxVersionsList();
		} catch (SQLException e) {
			GeneralPurposeDialogs.warningMessage(shell, e);
		}

		for (int i = 0; i < MainWindow.columnNames.length; i++) {
			if (i == 20)
				continue; // skip screenshot category

			TreeItem item = new TreeItem(tree, SWT.NONE);
			item.setText(MainWindow.columnNames[i]);

			class TreeNodeItem implements Comparable<TreeNodeItem> {
				String value;
				String subQuery;
				String likeQuery;

				public TreeNodeItem(String v, String q, String l) {
					value = StringUtils.isEmpty(v) ? EMPTY_FIELD: v;
					subQuery = q;
					likeQuery = l;
				}

				public int compareTo(TreeNodeItem o) {
					int eq1 = value.equals(EMPTY_FIELD) ? 1: 0;
					int eq2 = o.value.equals(EMPTY_FIELD) ? 1: 0;
					if (eq1 + eq2 > 0)
						return eq2 - eq1;
					return value.compareToIgnoreCase(o.value);
				}
			}

			SortedSet<TreeNodeItem> values = new TreeSet<TreeNodeItem>();
			switch (i) {
				case 0:
					for (Profile p: profilesList)
						values.add(new TreeNodeItem(p.getTitle(), "GAM.TITLE='" + p.getTitle() + "'", "GAM.TITLE"));
					break;
				case 1:
					for (Profile p: profilesList)
						values.add(new TreeNodeItem(p.hasSetupString(), p.hasSetup(0) ? "GAM.SETUP<>''": "(GAM.SETUP IS NULL OR GAM.SETUP='')", null));
					break;
				case 2:
					for (Profile p: profilesList)
						values.add(new TreeNodeItem(p.getDeveloperName(), "DEV.NAME='" + p.getDeveloperName() + "'", "DEV.NAME"));
					break;
				case 3:
					for (Profile p: profilesList)
						values.add(new TreeNodeItem(p.getPublisherName(), "PUBL.NAME='" + p.getPublisherName() + "'", "PUBL.NAME"));
					break;
				case 4:
					for (Profile p: profilesList)
						values.add(new TreeNodeItem(p.getGenre(), "GEN.NAME='" + p.getGenre() + "'", "GEN.NAME"));
					break;
				case 5:
					for (Profile p: profilesList)
						values.add(new TreeNodeItem(p.getYear(), "YR.YEAR='" + p.getYear() + "'", null));
					break;
				case 6:
					for (Profile p: profilesList)
						values.add(new TreeNodeItem(p.getStatus(), "STAT.STAT='" + p.getStatus() + "'", "STAT.STAT"));
					break;
				case 7:
					for (Profile p: profilesList)
						values.add(new TreeNodeItem(p.isDefaultString(), "GAM.FAVORITE=" + p.isDefault(), null));
					break;
				case 8:
					for (Profile p: profilesList)
						values.add(new TreeNodeItem(String.valueOf(p.getId()), "GAM.ID=" + p.getId(), null));
					break;
				case 9:
					for (Profile p: profilesList)
						values.add(new TreeNodeItem(String.valueOf(p.getDbversionId()), "GAM.DBVERSION_ID=" + p.getDbversionId(), null));
					break;
				case 10:
				case 11:
				case 12:
				case 13:
					for (Profile p: profilesList) {
						int idx = i - Constants.RO_COLUMN_NAMES;
						values.add(new TreeNodeItem(String.valueOf(p.getCustomString(idx)), "CUST" + (idx + 1) + ".VALUE='" + p.getCustomString(idx) + "'", "CUST" + (idx + 1) + ".VALUE"));
					}
					break;
				case 14:
				case 15:
				case 16:
				case 17:
					for (Profile p: profilesList) {
						int idx = i - Constants.RO_COLUMN_NAMES;
						values.add(new TreeNodeItem(String.valueOf(p.getCustomString(idx)), "GAM.CUSTOM" + (idx + 1) + "='" + p.getCustomString(idx) + "'", "GAM.CUSTOM" + (idx + 1)));
					}
					break;
				case 18:
				case 19:
					for (Profile p: profilesList) {
						int idx = i - Constants.RO_COLUMN_NAMES - 8;
						values.add(new TreeNodeItem(String.valueOf(p.getCustomInt(idx)), "GAM.CUSTOM" + (idx + 9) + "=" + p.getCustomInt(idx), null));
					}
					break;
				case 21:
					for (Profile p: profilesList) {
						String dbversionTitle = DosboxVersion.findById(dbversionsList, p.getDbversionId()).getTitle();
						values.add(new TreeNodeItem(dbversionTitle, "GAM.DBVERSION_ID=" + p.getDbversionId(), null));
					}
					break;
				case 22:
					for (Profile p: profilesList) {
						Date date = p.getStats().getCreated();
						values.add(new TreeNodeItem(Settings.toString(date), Settings.toDatabaseString("GAM.STATS_CREATED", date), null));
					}
					break;
				case 23:
					for (Profile p: profilesList) {
						Date date = p.getStats().getModified();
						values.add(new TreeNodeItem(Settings.toString(date), Settings.toDatabaseString("GAM.STATS_LASTMODIFY", date), null));
					}
					break;
				case 24:
					for (Profile p: profilesList) {
						Date date = p.getStats().getLastrun();
						values.add(new TreeNodeItem(Settings.toString(date), Settings.toDatabaseString("GAM.STATS_LASTRUN", date), null));
					}
					break;
				case 25:
					for (Profile p: profilesList) {
						Date date = p.getStats().getLastsetup();
						values.add(new TreeNodeItem(Settings.toString(date), Settings.toDatabaseString("GAM.STATS_LASTSETUP", date), null));
					}
					break;
				case 26:
					for (Profile p: profilesList)
						values.add(new TreeNodeItem(String.valueOf(p.getStats().getRuns()), "GAM.STATS_RUNS=" + p.getStats().getRuns(), null));
					break;
				case 27:
					for (Profile p: profilesList)
						values.add(new TreeNodeItem(String.valueOf(p.getStats().getSetups()), "GAM.STATS_SETUPS=" + p.getStats().getSetups(), null));
					break;
				default:
			}
			for (TreeNodeItem v: values) {
				TreeItem valueItem = new TreeItem(item, SWT.NONE);
				valueItem.setText(v.value);
				valueItem.setData(v.subQuery);
				valueItem.setGrayed(true);

				if (v.likeQuery != null) {
					String sentence = v.value.replaceAll("\\p{Punct}", " ");
					String[] words = sentence.split("\\s+");
					if (words.length > 1) {
						for (String w: words) {
							TreeItem likeItem = new TreeItem(valueItem, SWT.NONE);
							likeItem.setText(w);
							likeItem.setData("UPPER(" + v.likeQuery + ") LIKE '%" + w.toUpperCase() + "%'");
						}
					}
				}
			}
		}

		tree.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (event.detail == SWT.CHECK) {
					TreeItem tItem = (TreeItem)event.item;
					int depth = depth(tItem);
					if (depth == 0) {
						if (tItem.getChecked()) {
							if (tItem.getGrayed() || getAllCheckedItems(tItem).isEmpty()) {
								tItem.setChecked(false);
							}
						} else {
							if (tItem.getGrayed()) {
								tItem.setGrayed(false);
							} else {
								tItem.setGrayed(true);
								tItem.setChecked(true);
							}
						}
					} else if (depth == 1) {
						TreeItem parent = tItem.getParentItem();
						parent.setChecked(!getAllCheckedItems(parent).isEmpty());
					} else {
						if (tItem.getChecked()) {
							if (tItem.getGrayed()) {
								tItem.setChecked(false);
							}
						} else {
							if (tItem.getGrayed()) {
								tItem.setGrayed(false);
							} else {
								tItem.setGrayed(true);
								tItem.setChecked(true);
							}
						}
						TreeItem parent = tItem.getParentItem().getParentItem();
						parent.setChecked(!getAllCheckedItems(parent).isEmpty());
					}
				}
			}
		});

		tree.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				List<String> rootQueriesAnd = new ArrayList<String>();
				List<String> rootQueriesOr = new ArrayList<String>();
				String generatedTitle = null;

				for (TreeItem rootItem: tree.getItems()) {
					if (rootItem.getChecked()) {
						List<String> subQueriesAnd = new ArrayList<String>();
						List<String> subQueriesOr = new ArrayList<String>();

						for (TreeItem item: getAllCheckedItems(rootItem)) {
							TreeItem parent = item.getParentItem();
							TreeItem parentOfParent = null;
							if (parent != null) {
								if (generatedTitle == null) {
									parentOfParent = parent.getParentItem();
									if (parentOfParent != null)
										generatedTitle = parentOfParent.getText() + ": " + item.getText();
									else
										generatedTitle = parent.getText() + ": " + item.getText();
								} else if (!generatedTitle.endsWith("...")) {
									generatedTitle += "...";
								}
								if (item.getGrayed())
									subQueriesOr.add((String)item.getData());
								else
									subQueriesAnd.add((String)item.getData());
							}
						}

						String resultAnd = StringUtils.join(subQueriesAnd, "\n\tAND ");
						String resultOr = StringUtils.join(subQueriesOr, "\n\tOR ");
						boolean and = StringUtils.isNotBlank(resultAnd);
						boolean or = StringUtils.isNotBlank(resultOr);
						String result = null;
						if (and && or) {
							result = "(" + resultAnd + ")\nAND\n(" + resultOr + ")";
						} else if (and) {
							result = resultAnd;
						} else if (or) {
							result = resultOr;
						}

						if (rootItem.getGrayed())
							rootQueriesOr.add("(" + result + ")");
						else
							rootQueriesAnd.add("(" + result + ")");
					}
				}

				String resultAnd = StringUtils.join(rootQueriesAnd, "\nAND\n");
				String resultOr = StringUtils.join(rootQueriesOr, "\nOR\n");
				boolean and = StringUtils.isNotBlank(resultAnd);
				boolean or = StringUtils.isNotBlank(resultOr);
				String result = null;
				if (and && or) {
					result = "(" + resultAnd + ")\nAND\n(" + resultOr + ")";
				} else if (and) {
					result = resultAnd;
				} else if (or) {
					result = resultOr;
				}

				if (StringUtils.isNotBlank(generatedTitle))
					title.setText(generatedTitle);
				if (StringUtils.isNotBlank(result))
					filter.setText(result);
			}
		});

		final Label filterLabel = new Label(composite, SWT.NONE);
		filterLabel.setText(settings.msg("dialog.filter.filter"));

		filter = new Text(leftComposite, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.H_SCROLL);
		filter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		final Label resultLabel = new Label(composite, SWT.NONE);
		resultLabel.setText(settings.msg("dialog.filter.result"));

		final Text results = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
		results.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

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
					int filterId = dbase.addOrEditFilter(title.getText(), filter.getText(), result == null ? -1: ((Filter)result).getId());
					result = new Filter(filterId, title.getText(), filter.getText());
				} catch (SQLException e) {
					GeneralPurposeDialogs.warningMessage(shell, e);
				}
				shell.close();
			}
		});

		final Button cancelButton = new Button(composite_7, SWT.NONE);
		cancelButton.setText(settings.msg("button.cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				result = null;
				shell.close();
			}
		});

		final GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.widthHint = GeneralPurposeGUI.getWidth(okButton, cancelButton);
		okButton.setLayoutData(gridData);
		cancelButton.setLayoutData(gridData);

		title.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent arg0) {
				if (filter.getText().equals("") || filter.getText().equals("UPPER(GAM.TITLE) LIKE '%" + prevTitle.toUpperCase() + "%'")) {
					if (title.getText().length() == 0) {
						filter.setText("");
					} else {
						filter.setText("UPPER(GAM.TITLE) LIKE '%" + title.getText().toUpperCase() + "%'");
					}
				}
				prevTitle = title.getText();
			}
		});
		filter.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent arg0) {
				try {
					List<Profile> tmpList = dbase.readProfilesList("", filter.getText());
					results.setText(settings.msg("dialog.filter.notice.results", new Object[] {tmpList.size()}));
					okButton.setEnabled(true);
				} catch (SQLException e) {
					results.setText(settings.msg("dialog.filter.error.invalidcondition"));
					okButton.setEnabled(false);
				}
			}
		});

		// init values
		if (result != null) {
			// meaning we are essentially editing an existing filter
			// so we need to set previous values
			title.setText(((Filter)result).getTitle());
			filter.setText(((Filter)result).getFilter());
		} else {
			prevTitle = "";
			if (selectedProfileIds != null) {
				filter.setText("GAM.ID IN (" + StringUtils.join(selectedProfileIds, ',') + ")");
			}
		}
		title.setFocus();
	}

	private int depth(TreeItem item) {
		int result = 0;
		while ((item = item.getParentItem()) != null)
			result++;
		return result;
	}

	private java.util.List<TreeItem> getAllCheckedItems(final TreeItem treeItem) {
		java.util.List<TreeItem> result = new ArrayList<TreeItem>();
		for (TreeItem item: treeItem.getItems()) {
			if (item.getChecked())
				result.add(item);
			result.addAll(getAllCheckedItems(item));
		}
		return result;
	}

	private boolean isValid() {
		GeneralPurposeDialogs.initErrorDialog();
		if (title.getText().equals("")) {
			GeneralPurposeDialogs.addError(settings.msg("dialog.filter.required.title"), title);
		}
		return !GeneralPurposeDialogs.displayErrorDialog(shell);
	}
}
