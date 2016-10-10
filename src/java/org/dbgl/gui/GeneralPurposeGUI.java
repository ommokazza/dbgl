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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.dbgl.model.conf.Settings;
import org.dbgl.swtdesigner.SWTImageManager;


public final class GeneralPurposeGUI {

	public static Button createIconButton(final Composite composite, final int style, final Settings settings, final String title, final String img) {
		Button button = new Button(composite, style);
		updateIcon(button, settings, title, img);
		return button;
	}

	public static void updateIcon(final Button button, final Settings settings, final String title, final String img) {
		int displaySelection = settings.getSettings().getIntValue("gui", "buttondisplay");
		if (displaySelection != 1) {
			button.setImage(SWTImageManager.getResourceImage(button.getDisplay(), img));
		}
		if (displaySelection == 2) {
			button.setToolTipText(title);
		} else {
			button.setText(title);
		}
	}

	public static ToolItem createIconToolItem(final ToolBar toolBar, final Settings settings, final String title, final String img, final SelectionListener listener) {
		final ToolItem button = new ToolItem(toolBar, SWT.PUSH | SWT.BORDER);
		int displaySelection = settings.getSettings().getIntValue("gui", "buttondisplay");
		if (displaySelection != 1) {
			button.setImage(SWTImageManager.getResourceImage(button.getDisplay(), img));
		}
		if (displaySelection == 2) {
			button.setToolTipText(title);
		} else {
			button.setText(title);
		}
		button.addSelectionListener(listener);
		return button;
	}

	public static ToolItem createSeparatorToolItem(final ToolBar toolBar, final int width) {
		final ToolItem button = new ToolItem(toolBar, SWT.SEPARATOR | SWT.BORDER);
		button.setWidth(width);
		return button;
	}

	public static MenuItem createIconMenuItem(final Menu menu, final int style, final Settings settings, final String title, final String img, final int accelerator,
			final SelectionListener listener) {
		return createIconMenuItem(menu, style, -1, settings, title, img, accelerator, listener);
	}

	public static MenuItem createIconMenuItem(final Menu menu, final int style, final Settings settings, final String title, final String img, final SelectionListener listener) {
		return createIconMenuItem(menu, style, -1, settings, title, img, SWT.NONE, listener);
	}

	public static MenuItem createIconTopMenuItem(final Menu menu, final int style, final Settings settings, final String title, final String img, final SelectionListener listener) {
		return createIconMenuItem(menu, style, 0, settings, title, img, SWT.NONE, listener);
	}

	public static MenuItem createIconMenuItem(final Menu menu, final int style, final int pos, final Settings settings, final String title, final String img, final int accelerator,
			final SelectionListener listener) {
		final MenuItem menuItem = pos != -1 ? new MenuItem(menu, style, pos): new MenuItem(menu, style);
		menuItem.setText(title);
		if (accelerator != SWT.NONE)
			menuItem.setAccelerator(accelerator);
		if ((img != null) && (settings.getSettings().getIntValue("gui", "buttondisplay") != 1)) {
			menuItem.setImage(SWTImageManager.getResourceImage(menu.getDisplay(), img));
		}
		if (listener != null) {
			menuItem.addSelectionListener(listener);
		}
		return menuItem;
	}

	public static Font stringToFont(final Device device, final Font defaultFont, final String[] font) {
		try {
			return new Font(device, font[0], Integer.parseInt(font[1]), Integer.parseInt(font[2]));
		} catch (Exception e) {
			e.printStackTrace();
			return defaultFont;
		}
	}

	public static String fontToString(final Device device, final Font font) {
		FontData data = font.getFontData()[0];
		return data.getName() + '|' + data.getHeight() + '|' + data.getStyle();
	}

	public static int getWidth(Button button) {
		GC gc = new GC(button);
		FontMetrics fm = gc.getFontMetrics();
		int width = (4 * fm.getAverageCharWidth()) + button.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		gc.dispose();
		return width;
	}

	public static int getWidth(Button button1, Button button2) {
		GC gc = new GC(button1);
		FontMetrics fm = gc.getFontMetrics();
		int width = (4 * fm.getAverageCharWidth()) + Math.max(button1.computeSize(SWT.DEFAULT, SWT.DEFAULT).x, button2.computeSize(SWT.DEFAULT, SWT.DEFAULT).x);
		gc.dispose();
		return width;
	}
}
