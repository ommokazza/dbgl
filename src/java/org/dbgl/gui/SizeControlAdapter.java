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

import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;
import org.dbgl.model.conf.Settings;


public class SizeControlAdapter extends ControlAdapter {

	private Shell shell;
	private String itemName;
	private boolean enabled = true;
	private static Settings settings = Settings.getInstance();

	public SizeControlAdapter(final Shell shell, final String itemName) {
		this.shell = shell;
		this.itemName = itemName;
		shell.setSize(settings.getSettings().getIntValue("gui", itemName + "_width"), settings.getSettings().getIntValue("gui", itemName + "_height"));
	}

	public void controlResized(final ControlEvent arg0) {
		super.controlResized(arg0);
		if (enabled) {
			Rectangle rec = shell.getBounds();
			settings.getSettings().setIntValue("gui", itemName + "_width", rec.width);
			settings.getSettings().setIntValue("gui", itemName + "_height", rec.height);
		}
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
