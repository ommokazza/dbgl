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
package org.dbgl.swtdesigner;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.dbgl.util.searchengine.WebSearchEngine;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;


public final class SWTImageManager {

	private static final Map<String, Image> imageMap = new HashMap<String, Image>();

	public static final String IMG_TB_NEW = "tb_new.png";
	public static final String IMG_TB_EDIT = "tb_edit.png";
	public static final String IMG_TB_DELETE = "tb_delete.png";
	public static final String IMG_TB_RUN = "tb_run.png";
	public static final String IMG_TB_SETUP = "tb_setup.png";
	public static final String IMG_TB_ADDGAMEWIZARD = "tb_wizard.png";
	public static final String IMG_RUN = "run.png";
	public static final String IMG_SETUP = "setup.png";
	public static final String IMG_FOLDER = "folder.png";
	public static final String IMG_ZOOM = "zoom.png";
	public static final String IMG_NEW = "new.png";
	public static final String IMG_EDIT = "edit.png";
	public static final String IMG_DUPLICATE = "duplicate.png";
	public static final String IMG_DELETE = "delete.png";
	public static final String IMG_FAVORITE = "favorite.png";
	public static final String IMG_SHORTCUT = "shortcut.png";
	public static final String IMG_REFRESH = "refresh.png";
	public static final String IMG_HOME = "home.png";
	public static final String IMG_DFEND = "dfend.png";
	public static final String IMG_MIGRATE = "case.png";
	public static final String IMG_TABLEEXPORT = "checkout.png";
	public static final String IMG_IMPORT = "import.png";
	public static final String IMG_CLEAN = "clean.png";
	public static final String IMG_SETTINGS = "settings.png";
	public static final String IMG_LOG = "log.png";
	public static final String IMG_EXIT = "stop.png";
	public static final String IMG_FILTER = "filter.png";
	public static final String IMG_EDITFILTER = "editfilter.png";
	public static final String IMG_ABOUT = "about.png";
	public static final String IMG_GRAB = "grab.png";
	public static final String IMG_MOBYGAMES = "moby.png";
	public static final String IMG_POUET = "pouet.png";
	public static final String IMG_HOTUD = "hotud.png";
	public static final String IMG_THEGAMESDB = "thegamesdb.png";
	public static final String IMG_SHARE = "share.png";
	public static final String IMG_UNDO = "undo.png";
	public static final String IMG_TOPDOG = "topdog.png";
	public static final String IMG_TILES_LARGE = "tiles_large.png";
	public static final String IMG_TILES_MEDIUM = "tiles_medium.png";
	public static final String IMG_TILES_SMALL = "tiles_small.png";
	public static final String IMG_BOXES_LARGE = "boxes_large.png";
	public static final String IMG_BOXES_MEDIUM = "boxes_medium.png";
	public static final String IMG_BOXES_SMALL = "boxes_small.png";
	public static final String IMG_TABLE = "table.png";
	public static final String IMG_SCREENSHOTS = "screenshots.png";
	public static final String IMG_NOTES = "notes.png";

	public static void dispose() {
		for (Image image: imageMap.values()) {
			image.dispose();
		}
		imageMap.clear();
	}

	public static void flush(final String path) {
		Iterator<String> iterator = imageMap.keySet().iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();
			if (key.startsWith(path)) {
				imageMap.get(key).dispose();
				iterator.remove();
			}
		}
	}

	public static Image getResourceImage(final Display display, final String path) {
		return new Image(display, imageMap.getClass().getResourceAsStream("/img/" + path));
	}

	public static Image[] getResourceImages(final Display display, final String[] path) {
		Image[] result = new Image[path.length];
		for (int i = 0; i < path.length; i++) {
			result[i] = getResourceImage(display, path[i]);
		}
		return result;
	}

	public static Image getImage(final Display display, final String path) {
		try {
			if (path.toLowerCase().endsWith(".ico")) {
				ImageLoader iLoader = new ImageLoader();
				iLoader.load(path);
				int bestWidth = 0;
				int bestDepth = 0;
				int bestIndex = 0;
				for (int i = 0; i < iLoader.data.length; i++) {
					if (iLoader.data[i].width >= bestWidth && iLoader.data[i].depth >= bestDepth) {
						bestWidth = iLoader.data[i].width;
						bestDepth = iLoader.data[i].depth;
						bestIndex = i;
					}
				}
				return new Image(display, iLoader.data[bestIndex]);
			} else {
				Image img = new Image(display, path);
				if (path.toLowerCase().endsWith(".gif") && img.getImageData().type == SWT.IMAGE_UNDEFINED)
					throw new SWTException();
				return img;
			}
		} catch (SWTException swte) {
			GifDecoder d = new GifDecoder();
			int stat = d.read(path);
			if (stat == GifDecoder.STATUS_OK || stat == GifDecoder.STATUS_FORMAT_ERROR) {
				BufferedImage bufferedImage = d.getFrame(0);
				int[] data = ((DataBufferInt)bufferedImage.getData().getDataBuffer()).getData();
				ImageData imageData = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(), 24, new PaletteData(0xFF0000, 0x00FF00, 0x0000FF));
				imageData.setPixels(0, 0, data.length, data, 0);
				return new Image(display, imageData);
			} else {
				return getEmptyImage(display, 10, 10);
			}
		} catch (Exception e) {
			return getEmptyImage(display, 10, 10);
		}
	}

	public static ImageData getImageData(final String url) throws IOException {
		try {
			return new ImageData(WebSearchEngine.getInputStream(url));
		} catch (SWTException e) {
			GifDecoder d = new GifDecoder();
			d.read(WebSearchEngine.getInputStream(url));
			BufferedImage bufferedImage = d.getFrame(0);
			int[] data = ((DataBufferInt)bufferedImage.getData().getDataBuffer()).getData();
			ImageData imageData = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(), 24, new PaletteData(0xFF0000, 0x00FF00, 0x0000FF));
			imageData.setPixels(0, 0, data.length, data, 0);
			return imageData;
		}
	}

	public static ImageData[] getAnimatedImageData(final String url) throws IOException {
		GifDecoder d = new GifDecoder();
		d.read(WebSearchEngine.getInputStream(url));
		int amount = d.getFrameCount();
		if (d.getDelay(0) < 500)
			amount = 1;
		ImageData[] result = new ImageData[amount];
		for (int i = 0; i < amount; i++) {
			BufferedImage bufferedImage = d.getFrame(i);
			int[] data = ((DataBufferInt)bufferedImage.getData().getDataBuffer()).getData();
			result[i] = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(), 24, new PaletteData(0xFF0000, 0x00FF00, 0x0000FF));
			result[i].setPixels(0, 0, data.length, data, 0);
		}
		return result;
	}

	public static Image getEmptyImage(final Display display, final int width, final int height) {
		Image image = new Image(display, width, height);
		GC graphc = new GC(image);
		graphc.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		graphc.fillRectangle(0, 0, width, height);
		graphc.dispose();
		return image;
	}

	public static void save(final ImageData imageData, final String filename) throws SWTException {
		ImageLoader loader = new ImageLoader();
		loader.data = new ImageData[] {imageData};
		if (filename.toUpperCase().endsWith(".JPG"))
			loader.save(filename, SWT.IMAGE_JPEG);
		else
			loader.save(filename, SWT.IMAGE_PNG);
	}

	public static Image getWidthLimitedImage(final Display display, final int width, final ImageData data) {
		Image orgImage = new Image(display, data);
		int orgW = orgImage.getBounds().width;
		int orgH = orgImage.getBounds().height;
		double factor = (double)width / (double)orgW;
		int height = (int)(orgH * factor);
		Image image = new Image(display, width, height);
		GC graphc = new GC(image);
		graphc.setAntialias(SWT.ON);
		graphc.drawImage(orgImage, 0, 0, orgW, orgH, 0, 0, width, height);
		graphc.dispose();
		orgImage.dispose();
		return image;
	}

	public static Image getResizedImage(final Display display, final int height, final String path, final String name) {
		String hash = path + height + name;
		Image image = imageMap.get(hash);
		if (image == null) {
			Image orgImage = getImage(display, path);
			int orgW = orgImage.getBounds().width;
			int orgH = orgImage.getBounds().height;
			double factor = (double)height / (double)orgH;
			int width = (int)(orgW * factor);
			image = new Image(display, width, height);
			GC graphc = new GC(image);
			graphc.setAntialias(SWT.ON);
			graphc.drawImage(orgImage, 0, 0, orgW, orgH, 0, 0, width, height);
			if (name != null) {
				Point size = graphc.textExtent(name);
				graphc.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
				graphc.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
				graphc.setAlpha(180);
				graphc.drawString(name, width - size.x - 2, height - size.y - 2, false);
			}
			graphc.dispose();
			orgImage.dispose();
			imageMap.put(hash, image);
		}
		return image;
	}

	public static Image getResizedImage(final Display display, final int width, final int height, final boolean keepAspectRatio, final String path) {
		String hash = path + width + height + keepAspectRatio;
		Image image = imageMap.get(hash);
		if (image == null) {
			Image orgImage = getImage(display, path);
			int orgW = orgImage.getBounds().width;
			int orgH = orgImage.getBounds().height;
			double factor = Math.max((double)width / (double)orgW, (double)height / (double)orgH);
			int cropX = keepAspectRatio ? (int)(((orgW * factor) - width) / factor): 0;
			int cropY = keepAspectRatio ? (int)(((orgH * factor) - height) / factor): 0;
			image = new Image(display, width, height);
			GC graphc = new GC(image);
			graphc.setAntialias(SWT.ON);
			graphc.drawImage(orgImage, cropX / 2, cropY / 2, orgW - cropX, orgH - cropY, 0, 0, width, height);
			graphc.dispose();
			orgImage.dispose();
			imageMap.put(hash, image);
		}
		return image;
	}

	public static Image createDisabledImage(final Image img) {
		return new Image(img.getDevice(), img, SWT.IMAGE_DISABLE);
	}
}