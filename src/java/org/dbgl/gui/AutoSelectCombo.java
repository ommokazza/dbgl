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

import java.util.List;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.dbgl.model.KeyValuePair;


public final class AutoSelectCombo {

	private final Combo combo;
	private int currentLength;
	private boolean mutex;

	public AutoSelectCombo(final Composite composite, final int style, final List<KeyValuePair> possibleValues, final Object layoutData) {
		combo = new Combo(composite, style);
		combo.setLayoutData(layoutData);
		combo.setVisibleItemCount(15);
		for (KeyValuePair val: possibleValues) {
			combo.add(val.getValue());
		}
		combo.pack();

		currentLength = 0;
		mutex = false;

		combo.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent event) {
				if (!mutex) {
					int index = indexOfClosestMatch(combo.getText());
					int newLength = combo.getText().length();
					if ((newLength > currentLength) && (index != -1)) {
						mutex = true;
						combo.setText(combo.getText() + combo.getItem(index).substring(newLength));
						combo.setSelection(new Point(newLength, combo.getText().length()));
						mutex = false;
					}
					currentLength = newLength;
				}
			}
		});
	}

	public String getText() {
		return combo.getText();
	}

	public void setText(final String arg0) {
		combo.setText(arg0);
		currentLength = combo.getText().length();
	}

	private int indexOfClosestMatch(final String text) {
		for (int i = 0; i < combo.getItemCount(); i++) {
			if (combo.getItem(i).toLowerCase().startsWith(text.toLowerCase())) {
				return i;
			}
		}
		return -1;
	}

	public Object getData() {
		return combo.getData();
	}

	public void setData(Object o) {
		combo.setData(o);
	}

	public Display getDisplay() {
		return combo.getDisplay();
	}

	public void setBackground(Color systemColor) {
		combo.setBackground(systemColor);
	}

	public Control getControl() {
		return combo;
	}
}