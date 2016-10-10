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

import SevenZip.HRESULT;
import SevenZip.Archive.IArchiveExtractCallback;
import SevenZip.Archive.IInArchive;
import SevenZip.Archive.SevenZipEntry;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.TreeMap;
import org.dbgl.model.conf.Settings;


public class SevenzipExtractFilesCallback implements IArchiveExtractCallback {
	private ProgressNotifyable prog;
	private IInArchive archiveHandler;
	private Map<Integer, File> dstFileMap = new TreeMap<Integer, File>();

	public SevenzipExtractFilesCallback(final ProgressNotifyable prog, final IInArchive archiveHandler, final Map<Integer, File> dstFileMap) {
		this.prog = prog;
		this.archiveHandler = archiveHandler;
		this.dstFileMap = dstFileMap;
	}

	public int SetTotal(final long size) {
		return HRESULT.S_OK;
	}

	public int SetCompleted(final long completeValue) {
		prog.setProgress((int)(completeValue / 1024));
		return HRESULT.S_OK;
	}

	public int PrepareOperation(final int askExtractMode) {
		return HRESULT.S_OK;
	}

	public int SetOperationResult(final int operationResult) throws java.io.IOException {
		switch (operationResult) {
			case IInArchive.NExtract_NOperationResult_kOK:
				return HRESULT.S_OK;
			case IInArchive.NExtract_NOperationResult_kUnSupportedMethod:
				throw new IOException("Unsupported Method");
			case IInArchive.NExtract_NOperationResult_kCRCError:
				throw new IOException("CRC Failed");
			case IInArchive.NExtract_NOperationResult_kDataError:
				throw new IOException("Data Error");
			default:
				throw new IOException("Unknown Error");
		}
	}

	public int GetStream(final int index, final OutputStream[] outStream, final int askExtractMode) throws java.io.IOException {
		outStream[0] = null;

		if (askExtractMode == IInArchive.NExtract_NAskMode_kExtract) {
			SevenZipEntry entry = archiveHandler.getEntry(index);
			File dstFile = dstFileMap.get(index);

			if (entry.isDirectory()) {
				if (dstFile.isDirectory())
					return HRESULT.S_OK;
				if (dstFile.mkdirs())
					return HRESULT.S_OK;
				else
					return HRESULT.S_FALSE;
			} else {
				File dirAbove = dstFile.getParentFile();
				if (dirAbove != null) {
					if (!dirAbove.isDirectory())
						if (!dirAbove.mkdirs())
							return HRESULT.S_FALSE;
				}
				if (dstFile.exists()) {
					throw new IOException(Settings.getInstance().msg("general.error.filetobeextractedexists", new Object[] {dstFile}));
				}
				outStream[0] = new FileOutputStream(dstFile);
			}
		}

		return HRESULT.S_OK;
	}
}
