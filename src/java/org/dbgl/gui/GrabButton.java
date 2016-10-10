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

import java.io.File;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.dbgl.exception.InvalidMountstringException;
import org.dbgl.model.Mount;
import org.dbgl.model.Mount.MountingType;
import org.dbgl.model.conf.Settings;
import org.dbgl.swtdesigner.SWTImageManager;


public final class GrabButton {

	private final Button button;

	public GrabButton(final Composite composite, final int style) {
		Settings settings = Settings.getInstance();
		button = GeneralPurposeGUI.createIconButton(composite, style, settings, settings.msg("button.grab"), SWTImageManager.IMG_GRAB);
	}

	public void connect(final Text text, final List source, final boolean isBooter) {
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				int index = source.getSelectionIndex();
				if (index == -1 && source.getItemCount() == 1) {
					source.select(0);
					index = 0;
				}
				if (index != -1) {
					try {
						Mount mnt = new Mount(source.getItem(index));
						if (isBooter && mnt.getMountingType() == MountingType.IMAGE)
							text.setText(mnt.getPathAsString());
						else
							text.setText(mnt.getPathAsString() + File.separatorChar);
						text.selectAll();
						text.setFocus();
					} catch (InvalidMountstringException e1) {
						// nothing we can do
					}
				}
			}
		});
	}

	public void setEnabled(boolean enabled) {
		button.setEnabled(enabled);
	}
}
