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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.dbgl.gui.listeners.MeasureListener;
import org.dbgl.gui.listeners.PaintListener;
import org.dbgl.gui.listeners.ToolTipListener;
import org.dbgl.interfaces.ReOrderable;
import org.dbgl.model.ThumbInfo;
import org.dbgl.model.conf.SectionsWrapper;
import org.dbgl.model.conf.Settings;
import org.dbgl.swtdesigner.SWTImageManager;
import org.eclipse.nebula.widgets.gallery.AbstractGalleryItemRenderer;
import org.eclipse.nebula.widgets.gallery.Gallery;
import org.eclipse.nebula.widgets.gallery.GalleryItem;
import org.eclipse.nebula.widgets.gallery.NoGroupRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;


public class ProfilesList {

	private final Image topdog;

	public class ProfilesListItem {
		GalleryItem gItem;
		TableItem tItem;

		public ProfilesListItem(final ProfilesList list) {
			if (list.type == ProfilesListType.TABLE)
				tItem = new TableItem(list.table, SWT.BORDER);
			else
				gItem = new GalleryItem(list.group, SWT.NONE);
		}

		public ProfilesListItem(final TableItem item) {
			tItem = item;
		}

		public ProfilesListItem(final GalleryItem item) {
			gItem = item;
		}

		public Object getData() {
			if (tItem != null)
				return tItem.getData();
			else
				return gItem.getData();
		}

		public void setData(final Object obj) {
			if (tItem != null)
				tItem.setData(obj);
			else
				gItem.setData(obj);
		}

		public void resetCachedInfo() {
			ThumbInfo thumbInfo = (ThumbInfo)getData();
			thumbInfo.resetCachedInfo();
			setData(thumbInfo);

			if (gItem != null)
				gItem.setImage(null);
		}

		public void setText(final int i, int columnId, final String value) {
			if (tItem != null)
				tItem.setText(i, value);
			else {
				if (i == 0) {
					gItem.setText(1, StringUtils.EMPTY);
				}
				if (columnId == 0) {
					gItem.setText(value);
				}
				if (columnId == 7)
					gItem.setData(AbstractGalleryItemRenderer.OVERLAY_BOTTOM_LEFT, value.equals(Settings.getInstance().msg("general.yes")) ? topdog: null);
				if (StringUtils.isEmpty(value))
					return;
				StringBuffer s = new StringBuffer(gItem.getText(1));
				if (!StringUtils.isEmpty(s))
					s.append('\n');
				s.append(MainWindow.columnNames[columnId]).append(": ").append(value);
				gItem.setText(1, s.toString());
			}
		}
	}

	private ProfilesListType type;
	private Gallery gallery;
	private GalleryItem group;
	private Color bgColor;
	private Table table;

	private static final Listener paintListener = new PaintListener();
	private static final Listener measureListener = new MeasureListener();
	private static final Listener toolTipOpenListener = new ToolTipListener();

	private static final SectionsWrapper ssettings = Settings.getInstance().getSettings();

	public enum ProfilesListType {
		TABLE, SMALL_TILES, MEDIUM_TILES, LARGE_TILES, SMALL_BOXES, MEDIUM_BOXES, LARGE_BOXES
	};

	public ProfilesList(final Composite composite, final ProfilesListType type) {
		this(composite, type, null, null, null);
	}

	public ProfilesList(final Composite composite, final ProfilesListType type, final ReOrderable reOrderable, final int[] columnIds, final String[] columnNames) {
		this.type = type;
		topdog = SWTImageManager.getResourceImage(composite.getShell().getDisplay(), SWTImageManager.IMG_TOPDOG);

		if (type == ProfilesListType.TABLE) {
			table = new Table(composite, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
			table.setLinesVisible(true);
			table.setHeaderVisible(true);
			for (int i = 0; i < columnIds.length; i++) {
				addProfileColumn(reOrderable, columnIds, columnNames[columnIds[i]], i);
			}
			table.setSortColumn(table.getColumn(findColumnById(columnIds, ssettings.getIntValues("gui", "sortcolumn")[0])));
			table.setSortDirection(ssettings.getBooleanValues("gui", "sortascending")[0] ? SWT.UP: SWT.DOWN);
			table.setColumnOrder(ssettings.getIntValues("gui", "columnorder"));

			for (int i = 0; i < columnIds.length; i++) {
				if (columnIds[i] == 20) { // screenshot column
					table.setData(i);
					table.addListener(SWT.PaintItem, paintListener);
					table.addListener(SWT.MeasureItem, measureListener);
				}
			}

		} else {

			gallery = new Gallery(composite, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER);
			gallery.setAntialias(SWT.OFF);
			gallery.setLowQualityOnUserAction(true);
			gallery.setHigherQualityDelay(100);
			int[] rgb = ssettings.getIntValues("gui", "gallerybackgroundcolor");
			if (rgb.length == 3) {
				bgColor = new Color(composite.getShell().getDisplay(), rgb[0], rgb[1], rgb[2]);
				gallery.setBackground(bgColor);
			}
			NoGroupRenderer gr = new NoGroupRenderer();

			switch (type) {
				case LARGE_TILES:
					gr.setItemSize(ssettings.getIntValue("gui", "large_tile_width"), ssettings.getIntValue("gui", "large_tile_height"));
					break;
				case MEDIUM_TILES:
					gr.setItemSize(ssettings.getIntValue("gui", "medium_tile_width"), ssettings.getIntValue("gui", "medium_tile_height"));
					break;
				case SMALL_TILES:
					gr.setItemSize(ssettings.getIntValue("gui", "small_tile_width"), ssettings.getIntValue("gui", "small_tile_height"));
					break;
				case LARGE_BOXES:
					gr.setItemSize(ssettings.getIntValue("gui", "large_box_width"), ssettings.getIntValue("gui", "large_box_height"));
					break;
				case MEDIUM_BOXES:
					gr.setItemSize(ssettings.getIntValue("gui", "medium_box_width"), ssettings.getIntValue("gui", "medium_box_height"));
					break;
				case SMALL_BOXES:
					gr.setItemSize(ssettings.getIntValue("gui", "small_box_width"), ssettings.getIntValue("gui", "small_box_height"));
					break;
				default:
					break;
			}

			gr.setAutoMargin(true);
			gr.setMinMargin(1);
			gallery.setGroupRenderer(gr);
			GalleryItemRenderer ir = new GalleryItemRenderer(ssettings.getValue("gui", "tile_title_trunc_pos"));
			gallery.setItemRenderer(ir);
			group = new GalleryItem(gallery, SWT.NONE);

			gallery.getShell().addListener(SWT.Deactivate, toolTipOpenListener);
			gallery.addListener(SWT.Dispose, toolTipOpenListener);
			gallery.addListener(SWT.KeyDown, toolTipOpenListener);
			gallery.addListener(SWT.MouseMove, toolTipOpenListener);
			gallery.addListener(SWT.MouseWheel, toolTipOpenListener);
			gallery.addListener(SWT.MouseUp, toolTipOpenListener);
			gallery.addListener(SWT.MouseHover, toolTipOpenListener);
			gallery.addListener(SWT.PaintItem, paintListener);
		}
	}

	private int findColumnById(final int[] columnIds, final int id) {
		for (int i = 0; i < columnIds.length; i++) {
			if (columnIds[i] == id) {
				return i;
			}
		}
		return -1;
	}

	private void addProfileColumn(final ReOrderable reOrderable, final int[] columnIds, final String title, final int colIndex) {
		final String width = "column" + (columnIds[colIndex] + 1) + "width";
		final TableColumn column = new TableColumn(table, SWT.NONE);
		column.setWidth(ssettings.getIntValue("gui", width));
		column.setMoveable(true);
		column.setText(title);
		if ((columnIds[colIndex] == 8) || (columnIds[colIndex] == 9) || (columnIds[colIndex] == 18) || (columnIds[colIndex] == 19)) { // numeric values
			column.setAlignment(SWT.RIGHT);
		}
		if ((columnIds[colIndex] == 20)) { // screenshot
			column.setAlignment(SWT.CENTER);
		}
		column.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				TableColumn sortColumn = table.getSortColumn();
				TableColumn currentColumn = (TableColumn)event.widget;
				int dir = table.getSortDirection();
				if (sortColumn.equals(currentColumn)) {
					dir = dir == SWT.UP ? SWT.DOWN: SWT.UP;
				} else {
					table.setSortColumn(currentColumn);
					dir = SWT.UP;
				}
				table.setSortDirection(dir);

				reOrderable.doReorder(colIndex, dir);
			}
		});
		column.addControlListener(new ControlAdapter() {
			public void controlResized(final ControlEvent event) {
				ssettings.setIntValue("gui", width, column.getWidth());
			}

			public void controlMoved(final ControlEvent event) {
				if (event.time != 0) // workaround for buggy SWT behavior in GTK
					ssettings.setIntValues("gui", "columnorder", table.getColumnOrder());
			}
		});
	}

	public void addMouseListener(final MouseAdapter mouseAdapter) {
		if (type == ProfilesListType.TABLE)
			table.addMouseListener(mouseAdapter);
		else
			gallery.addMouseListener(mouseAdapter);
	}

	public void addKeyListener(final KeyAdapter keyAdapter) {
		if (type == ProfilesListType.TABLE)
			table.addKeyListener(keyAdapter);
		else
			gallery.addKeyListener(keyAdapter);
	}

	public void addTraverseListener(final TraverseListener travListener) {
		if (type == ProfilesListType.TABLE)
			table.addTraverseListener(travListener);
		else
			gallery.addTraverseListener(travListener);
	}

	public void addSelectionListener(final SelectionAdapter selectProfAdapter) {
		if (type == ProfilesListType.TABLE)
			table.addSelectionListener(selectProfAdapter);
		else
			gallery.addSelectionListener(selectProfAdapter);
	}

	public void setFocus() {
		if (type == ProfilesListType.TABLE)
			table.setFocus();
		else
			gallery.setFocus();
	}

	public void setRedraw(final boolean b) {
		if (type == ProfilesListType.TABLE)
			table.setRedraw(b);
		else
			gallery.setRedraw(b);
	}

	public void redraw() {
		if (type == ProfilesListType.TABLE)
			table.redraw();
		else
			gallery.redraw();
	}

	public void setMenu(final Menu menu) {
		if (type == ProfilesListType.TABLE)
			table.setMenu(menu);
		else
			gallery.setMenu(menu);
	}

	public int getSelectionCount() {
		if (type == ProfilesListType.TABLE)
			return table.getSelectionCount();
		else
			return gallery.getSelectionCount();
	}

	public int getSelectionIndex() {
		if (type == ProfilesListType.TABLE)
			return table.getSelectionIndex();
		else {
			GalleryItem[] items = gallery.getSelection();
			if (items.length == 0)
				return -1;
			return (group.indexOf(items[0]));
		}
	}

	public int[] getSelectionIndices() {
		if (type == ProfilesListType.TABLE)
			return table.getSelectionIndices();
		else {
			GalleryItem[] items = gallery.getSelection();
			if (items.length == 0)
				return new int[0];
			List<Integer> indices = new ArrayList<Integer>();
			for (GalleryItem i: items) {
				int index = group.indexOf(i);
				if (index != -1)
					indices.add(index);
			}
			return ArrayUtils.toPrimitive(indices.toArray(new Integer[0]));
		}
	}

	public void setSelection(final int index) {
		if (type == ProfilesListType.TABLE)
			table.setSelection(index);
		else {
			GalleryItem item = group.getItem(index);
			if (item != null)
				gallery.setSelection(new GalleryItem[] {item});
		}
	}

	public void setSelection(final int[] indices) {
		if (type == ProfilesListType.TABLE)
			table.setSelection(indices);
		else {
			List<GalleryItem> items = new ArrayList<GalleryItem>();
			for (int index: indices) {
				GalleryItem item = group.getItem(index);
				if (item != null)
					items.add(item);
			}
			gallery.setSelection(items.toArray(new GalleryItem[0]));
		}
	}

	public int getItemCount() {
		if (type == ProfilesListType.TABLE)
			return table.getItemCount();
		else
			return group.getItemCount();
	}

	public ProfilesListItem getItem(final int index) {
		if (type == ProfilesListType.TABLE)
			return new ProfilesListItem(table.getItem(index));
		else
			return new ProfilesListItem(group.getItem(index));
	}

	public void removeAll() {
		if (type == ProfilesListType.TABLE) {
			table.removeAll();
		} else {
			gallery.removeAll();
			group = new GalleryItem(gallery, SWT.NONE);
		}
	}

	public void remove(final int index) {
		if (type == ProfilesListType.TABLE)
			table.remove(index);
		else
			group.remove(index);
	}

	public void selectAll() {
		if (type == ProfilesListType.TABLE)
			table.selectAll();
		else
			group.selectAll();
	}

	public ProfilesListItem[] getItems() {
		if (type == ProfilesListType.TABLE) {
			List<ProfilesListItem> items = new ArrayList<ProfilesListItem>();
			for (TableItem item: table.getItems())
				items.add(new ProfilesListItem(item));
			return items.toArray(new ProfilesListItem[0]);
		} else {
			List<ProfilesListItem> items = new ArrayList<ProfilesListItem>();
			for (GalleryItem item: group.getItems())
				items.add(new ProfilesListItem(item));
			return items.toArray(new ProfilesListItem[0]);
		}
	}

	public Control getControl() {
		if (type == ProfilesListType.TABLE)
			return table;
		else
			return gallery;
	}

	public void dispose() {
		if (type == ProfilesListType.TABLE) {
			table.removeListener(SWT.PaintItem, paintListener);
			table.removeListener(SWT.MeasureItem, measureListener);
			table.dispose();
		} else {
			gallery.getShell().removeListener(SWT.Deactivate, toolTipOpenListener);
			gallery.removeListener(SWT.KeyDown, toolTipOpenListener);
			gallery.removeListener(SWT.MouseMove, toolTipOpenListener);
			gallery.removeListener(SWT.MouseWheel, toolTipOpenListener);
			gallery.removeListener(SWT.MouseUp, toolTipOpenListener);
			gallery.removeListener(SWT.MouseHover, toolTipOpenListener);
			gallery.removeListener(SWT.PaintItem, paintListener);
			gallery.dispose();
			if (bgColor != null)
				bgColor.dispose();
		}
	}
}
