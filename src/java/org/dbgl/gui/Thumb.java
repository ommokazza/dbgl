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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.dbgl.model.conf.Settings;
import org.dbgl.swtdesigner.SWTImageManager;


public final class Thumb extends Dialog {

	private static final double THUMB_FACTOR = 2.0;
	private static final int DIALOG_WIDTH_EXT = 24;
	private static final int DIALOG_HEIGHT_EXT = 50;

	private Shell shell;
	private Display display;
	private Image thumbImage = null;
	private String filename;

	public Thumb(final Shell parent, final int style) {
		super(parent, style);
		display = parent.getDisplay();
	}

	public Thumb(final Shell parent) {
		this(parent, SWT.NONE);
	}

	public void setThumb(final String filename) {
		this.filename = filename;
		Image orgthumb = SWTImageManager.getImage(display, filename);
		ImageData bigThumb = orgthumb.getImageData();
		int width = bigThumb.width;
		int height = bigThumb.height;

		Rectangle screen = display.getClientArea();
		if ((width <= 400) && (height <= 350)) {
			bigThumb = bigThumb.scaledTo((int)(width * THUMB_FACTOR), (int)(height * THUMB_FACTOR));
		} else if (width + DIALOG_WIDTH_EXT > screen.width || height + DIALOG_HEIGHT_EXT > screen.height) {
			final double factor = Math.min((double)screen.width / (width + DIALOG_WIDTH_EXT * 2), (double)screen.height / (height + DIALOG_HEIGHT_EXT * 2));
			bigThumb = bigThumb.scaledTo((int)(width * factor), (int)(height * factor));
		}
		thumbImage = new Image(display, bigThumb);
		orgthumb.dispose();
	}

	public Object open() {
		createContents();
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		if (thumbImage != null && !thumbImage.isDisposed()) {
			thumbImage.dispose();
		}
		return null;
	}

	protected void createContents() {
		if (thumbImage != null) {
			shell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			shell.setLayout(new FillLayout());
			shell.setSize(thumbImage.getImageData().width + DIALOG_WIDTH_EXT, thumbImage.getImageData().height + DIALOG_HEIGHT_EXT);
			shell.setText(Settings.getInstance().msg("dialog.screenshot.title", new Object[] {filename}));
			final Button button = new Button(shell, SWT.FLAT);
			button.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(final SelectionEvent event) {
					shell.close();
				}
			});
			button.setImage(thumbImage);
			button.pack();
		}
	}
}
