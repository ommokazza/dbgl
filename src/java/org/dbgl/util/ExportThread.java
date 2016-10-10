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
package org.dbgl.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.zip.ZipOutputStream;
import javax.xml.transform.TransformerException;
import org.dbgl.model.ExpProfile;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;
import org.w3c.dom.Document;


public final class ExportThread extends UIThread {

	private Document xmlDoc;
	private boolean exportCaptures;
	private boolean exportMapperfiles;
	private boolean exportGameData;
	private File target;
	private ZipOutputStream zos;

	public ExportThread(Text log, ProgressBar progressBar, Label status, List<ExpProfile> p, Document doc, boolean captures, boolean mapperfiles, boolean gamedata, File zipfile)
			throws IOException, SQLException {
		super(log, progressBar, status);

		this.xmlDoc = doc;
		this.exportCaptures = captures;
		this.exportMapperfiles = mapperfiles;
		this.exportGameData = gamedata;
		this.target = zipfile;

		this.objects = p;
		this.progressBar.setMaximum(objects.size());

		zos = new ZipOutputStream(new FileOutputStream(target));
		try {
			XmlUtils.domToZipOutputStream(xmlDoc, new File(FileUtils.PROFILES_XML), zos);
		} catch (TransformerException e) {
			throw new IOException(e);
		}
	}

	public void doFancyStuff(Object obj, StringBuffer messageLog) throws IOException {
		ExpProfile prof = (ExpProfile)obj;
		displayTitle(settings.msg("dialog.export.exporting", new Object[] {prof.getTitle()}));

		File relativeGameDir = prof.getGameDir();
		File relativeGameDirInZip = new File(FileUtils.DOSROOT_DIR, new File(String.valueOf(prof.getId()), relativeGameDir.getPath()).getPath());

		if (exportCaptures) {
			File captures = new File(FileUtils.constructCapturesDir(prof.getId()));
			try {
				FileUtils.zipDir(prof.getCanonicalCaptures(), zos, captures);
			} catch (IOException e) {
				throw new IOException(settings.msg("dialog.export.error.exportcaptures", new Object[] {prof.getTitle(), StringRelatedUtils.toString(e)}), e);
			}
		}

		if (exportMapperfiles) {
			File orgMapperfile = prof.getConf().getCustomMapperFile();
			if (orgMapperfile != null) {
				File dstMapperfile = new File(FileUtils.constructMapperFile(prof.getId()));
				FileUtils.zipEntry(orgMapperfile, dstMapperfile, zos);
			}
		}

		if (exportGameData) {
			boolean uniqueGameFolder = true;
			for (Object obj2: objects) {
				ExpProfile prof2 = (ExpProfile)obj2;
				if (prof != prof2 && prof.getGameDir().equals(prof2.getGameDir()) && (prof.getId() > prof2.getId())) {
					uniqueGameFolder = false;
				}
			}
			if (uniqueGameFolder) {
				try {
					FileUtils.zipDir(FileUtils.canonicalToDosroot(relativeGameDir.getPath()), zos, relativeGameDirInZip);
				} catch (IOException e) {
					throw new IOException(settings.msg("dialog.export.error.exportgamedata", new Object[] {prof.getTitle(), StringRelatedUtils.toString(e)}), e);
				}
			}
		}
	}

	public String getTitle(Object o) {
		return ((ExpProfile)o).getTitle();
	}

	public void preFinish() throws IOException {
		zos.close();
	}
}
