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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.List;
import org.dbgl.db.Database;
import org.dbgl.model.DosboxVersion;
import org.dbgl.model.conf.Settings;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;


public abstract class UIThread extends Thread implements ProgressNotifyable {

	protected static final String PREFIX_OK = "  + ";
	protected static final String PREFIX_ERR = "  - ";

	protected final Settings settings = Settings.getInstance();

	protected List<?> objects;
	protected List<DosboxVersion> dbversions;
	protected ProgressBar progressBar;
	protected ByteArrayOutputStream bos;
	protected PrintStream ps;
	protected Database dbase;
	protected boolean extensiveLogging = false;

	private boolean everythingOk = true;
	private Text log;
	private Label status;
	private Display display;

	public UIThread(Text log, ProgressBar progressBar, Label status) throws SQLException {
		this.log = log;
		this.progressBar = progressBar;
		this.status = status;
		this.display = log.getShell().getDisplay();

		bos = new ByteArrayOutputStream();
		ps = new PrintStream(bos);

		dbase = Database.getInstance();
		this.dbversions = dbase.readDosboxVersionsList();
	}

	public void run() {
		for (final Object o: objects) {
			final StringBuffer messageLog = new StringBuffer();
			try {

				if (extensiveLogging)
					messageLog.append(getTitle(o)).append(":\n");

				dbase.startTransaction();
				doFancyStuff(o, messageLog);
				if (bos.size() > 0) {
					if (!extensiveLogging)
						messageLog.append(getTitle(o));
					messageLog.append(PREFIX_ERR).append(bos.toString());
					bos.reset();
				}
				dbase.commitTransaction();

			} catch (Exception e) {

				e.printStackTrace();
				if (!extensiveLogging)
					messageLog.append(getTitle(o));
				messageLog.append(PREFIX_ERR).append(StringRelatedUtils.toString(e)).append('\n');
				try {
					dbase.rollbackTransaction();
				} catch (SQLException se) {
					se.printStackTrace();
					if (!extensiveLogging)
						messageLog.append(getTitle(o));
					messageLog.append(PREFIX_ERR).append(StringRelatedUtils.toString(se)).append('\n');
				}
				everythingOk = false;

			} finally {
				dbase.finishTransaction();
			}

			if (display.isDisposed() || log.isDisposed() || progressBar.isDisposed() || status.isDisposed())
				break;
			display.syncExec(new Runnable() {
				public void run() {
					if (messageLog.length() > 0) {
						String newOutput = messageLog.toString();
						log.append(newOutput);
						System.out.print(newOutput);
					}
					incrProgress(1);
				}
			});
		}

		try {
			preFinish();
		} catch (IOException e) {
			e.printStackTrace();
		}

		completeProgress();
	}

	public void displayTitle(final String title) {
		if (!display.isDisposed() && !status.isDisposed()) {
			display.syncExec(new Runnable() {
				public void run() {
					status.setText(title);
					status.pack();
				}
			});
		}
	}

	public void setTotal(final int total) {
		if (!display.isDisposed() && !progressBar.isDisposed()) {
			display.syncExec(new Runnable() {
				public void run() {
					progressBar.setMaximum(total + objects.size());
				}
			});
		}
	}

	public void incrProgress(final int progress) {
		if (!display.isDisposed() && !progressBar.isDisposed()) {
			display.syncExec(new Runnable() {
				public void run() {
					progressBar.setSelection(progressBar.getSelection() + progress);
				}
			});
		}
	}

	public void setProgress(final int progress) {
		if (!display.isDisposed() && !progressBar.isDisposed()) {
			display.syncExec(new Runnable() {
				public void run() {
					progressBar.setSelection(progress);
				}
			});
		}
	}

	private void completeProgress() {
		if (!display.isDisposed() && !progressBar.isDisposed()) {
			display.syncExec(new Runnable() {
				public void run() {
					progressBar.setSelection(progressBar.getMaximum());
				}
			});
		}
	}

	public boolean isEverythingOk() {
		return everythingOk;
	}

	public abstract String getTitle(Object o);

	public abstract void doFancyStuff(Object obj, StringBuffer messageLog) throws IOException, SQLException;

	public abstract void preFinish() throws IOException;
}
