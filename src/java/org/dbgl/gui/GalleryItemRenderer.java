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

import org.eclipse.nebula.widgets.gallery.AbstractGalleryItemRenderer;
import org.eclipse.nebula.widgets.gallery.GalleryItem;
import org.eclipse.nebula.widgets.gallery.RendererHelper;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;


public class GalleryItemRenderer extends AbstractGalleryItemRenderer {

	private static final int lineWidth = 2;
	private static final int selectionRadius = 8;

	private static final String ELLIPSIS = "...";

	// Renderer parameters
	private final Color foregroundColor, backgroundColor;
	private final Color selectionForegroundColor, selectionBackgroundColor;
	private final String truncPosition;

	public GalleryItemRenderer(String truncPosition) {
		foregroundColor = Display.getDefault().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
		backgroundColor = Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
		selectionForegroundColor = Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);
		selectionBackgroundColor = Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION);
		this.truncPosition = truncPosition;
	}

	public void draw(GC gc, GalleryItem item, int index, int x, int y, int width, int height) {
		// Set up the GC
		gc.setFont(getFont(item));

		// Create some room for the label.
		int fontHeight = gc.getFontMetrics().getHeight();

		// Checks if background has to be drawn
		boolean drawBackground = selected;
		Color drawBackgroundColor = null;
		if (!drawBackground && item.getBackground(true) != null) {
			drawBackgroundColor = getBackground(item);
			if (!RendererHelper.isColorsEquals(drawBackgroundColor, backgroundColor))
				drawBackground = true;
		}

		// Draw background (rounded rectangles)
		if (drawBackground) {
			if (selected) {
				gc.setBackground(selectionBackgroundColor);
				gc.setForeground(selectionBackgroundColor);
			} else if (drawBackgroundColor != null) {
				gc.setBackground(drawBackgroundColor);
			}

			gc.setLineStyle(SWT.LINE_DOT);
			gc.setLineWidth(lineWidth);
			gc.drawRoundRectangle(x + (lineWidth / 2), y + (lineWidth / 2), width - lineWidth, height - lineWidth, selectionRadius, selectionRadius);
			gc.fillRoundRectangle(x + (lineWidth / 2), y + height - fontHeight - (lineWidth / 2) - 1, width - lineWidth, fontHeight + 1, selectionRadius, selectionRadius);
		}

		// Draw image
		Image _drawImage = item.getImage();
		if (_drawImage != null) {
			Rectangle itemImageBounds = _drawImage.getBounds();
			int xShift = RendererHelper.getShift(width - (lineWidth * 2), itemImageBounds.width);
			gc.drawImage(_drawImage, x + lineWidth + xShift, y + lineWidth);
			drawAllOverlays(gc, item, x, y, new Point(itemImageBounds.width, itemImageBounds.height), xShift, lineWidth);
		}

		// Draw label
		// Set colors
		if (selected) {
			// Selected : use selection colors.
			gc.setForeground(selectionForegroundColor);
			gc.setBackground(selectionBackgroundColor);
		} else {
			// Not selected, use item values or defaults.
			gc.setBackground(drawBackgroundColor != null ? drawBackgroundColor: backgroundColor);
			Color _drawForegroundColor = getForeground(item);
			gc.setForeground(_drawForegroundColor != null ? _drawForegroundColor: foregroundColor);
		}

		// Create label
		String text;
		if (truncPosition.equalsIgnoreCase("end"))
			text = createLabelTruncAtEnd(item.getText(), gc, width - 8);
		else // middle
			text = RendererHelper.createLabel(item.getText(), gc, width - 8);

		// Center text
		int textWidth = gc.textExtent(text).x;
		int textxShift = RendererHelper.getShift(width, textWidth);

		// Draw
		gc.drawText(text, x + textxShift, y + height - fontHeight - lineWidth, true);
	}

	public void dispose() {}

	public static String createLabelTruncAtEnd(String text, GC gc, int width) {
		if (text != null) {
			int extent = gc.textExtent(text).x;
			if (extent > width) {
				final int w = gc.textExtent(ELLIPSIS).x;
				if (width > w) {
					int l = text.length();
					while (extent > width)
						extent = gc.textExtent(text.substring(0, --l)).x + w;
					return text.substring(0, l) + ELLIPSIS;
				}
			}
		}
		return text;
	}
}
