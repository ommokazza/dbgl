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

import org.eclipse.nebula.widgets.gallery.Gallery;
import org.eclipse.nebula.widgets.gallery.GalleryItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;


public class ToolTipListener implements Listener {

	private Shell tip = null;

	public void handleEvent(Event event) {
		switch (event.type) {
			case SWT.MouseHover:
				Gallery gallery = (Gallery)event.widget;
				GalleryItem item = gallery.getItem(new Point(event.x, event.y));
				if (item != null) {
					final Display display = gallery.getShell().getDisplay();
					if (tip != null && !tip.isDisposed())
						tip.dispose();
					tip = new Shell(gallery.getShell(), SWT.ON_TOP | SWT.NO_FOCUS | SWT.TOOL);
					tip.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
					FillLayout layout = new FillLayout();
					layout.marginWidth = 2;
					tip.setLayout(layout);
					Label label = new Label(tip, SWT.NONE);
					label.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
					label.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
					label.setText(item.getText(1));
					label.addListener(SWT.MouseExit, this);
					Point size = tip.computeSize(SWT.DEFAULT, SWT.DEFAULT);
					Point pt = gallery.toDisplay(event.x, event.y + 16);
					tip.setBounds(pt.x, pt.y, size.x, size.y);
					tip.setVisible(true);
				}
				break;
			default:
				if (tip != null)
					tip.dispose();
				tip = null;
		}
	}
}
