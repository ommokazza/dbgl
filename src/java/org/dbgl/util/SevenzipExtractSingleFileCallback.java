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
import java.io.IOException;
import java.io.OutputStream;


public class SevenzipExtractSingleFileCallback implements IArchiveExtractCallback {
	private ProgressNotifyable prog;
	private OutputStream outputStream;

	public SevenzipExtractSingleFileCallback(final ProgressNotifyable prog, final OutputStream out) {
		this.prog = prog;
		this.outputStream = out;
	}

	public int SetTotal(final long size) {
		prog.setTotal((int)(size / 1024));
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
		outStream[0] = (askExtractMode == IInArchive.NExtract_NAskMode_kExtract) ? outputStream: null;
		return HRESULT.S_OK;
	}
}
