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
package org.dbgl.gui.listeners;

import org.dbgl.model.ThumbInfo;
import org.dbgl.model.conf.SectionsWrapper;
import org.dbgl.model.conf.Settings;
import org.dbgl.swtdesigner.SWTImageManager;
import org.eclipse.nebula.widgets.gallery.Gallery;
import org.eclipse.nebula.widgets.gallery.GalleryItem;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Widget;


public class PaintListener implements Listener {

	private static final SectionsWrapper set = Settings.getInstance().getSettings();

	public void handleEvent(Event event) {
		if (event.widget instanceof Gallery) {
			GalleryItem item = (GalleryItem)event.item;
			ThumbInfo thumbInfo = (ThumbInfo)item.getData();
			String filename = thumbInfo.getMainThumb();
			if (thumbInfo.isUpdated()) {
				if (filename == null) {
					item.setImage(null);
				} else {
					Rectangle rect = item.getBounds();
					item.setImage(SWTImageManager.getResizedImage(event.display, rect.width - 4, rect.height - 22, set.getBooleanValue("gui", "screenshotscolumnkeepaspectratio"), filename));
				}
				item.setData(thumbInfo);
			}
		} else if (event.widget instanceof Table) {
			Table table = (Table)event.widget;
			Widget item = (Widget)event.item;
			Integer sc = (Integer)table.getData();
			if (event.index == sc) {
				ThumbInfo thumbInfo = (ThumbInfo)item.getData();
				String filename = thumbInfo.getMainThumb();
				if (filename != null) {
					int columnWidth = table.getColumn(sc).getWidth();
					int columnHeight = set.getIntValue("gui", "screenshotscolumnheight");
					Image image = set.getBooleanValue("gui", "screenshotscolumnstretch")
							? SWTImageManager.getResizedImage(event.display, columnWidth, columnHeight, set.getBooleanValue("gui", "screenshotscolumnkeepaspectratio"), filename)
							: SWTImageManager.getResizedImage(event.display, columnHeight, filename, null);
					int offsetX = Math.max(0, (columnWidth - image.getBounds().width - 2) / 2);
					int offsetY = Math.max(0, (columnHeight - image.getBounds().height - 2) / 2);
					event.gc.drawImage(image, event.x + offsetX, event.y + offsetY);
				}
				if (thumbInfo.isUpdated())
					item.setData(thumbInfo);
			}
		}
	}
}
