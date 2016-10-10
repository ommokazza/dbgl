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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.dbgl.model.conf.Settings;
import org.dbgl.util.StringRelatedUtils;


public final class GeneralPurposeDialogs {

	private static List<String> errorMessages;
	private static Control wgt;
	private static TabItem tab;

	public static void fatalMessage(final Shell shell, final String theMessage) {
		fatalMessage(shell, System.err, theMessage, null);
	}

	public static void fatalMessage(final Shell shell, final String theMessage, final Exception exception) {
		fatalMessage(shell, System.err, theMessage, exception);
	}

	private static void fatalMessage(final Shell shell, final PrintStream ps, final String theMessage, final Exception exception) {
		if (shell != null) {
			MessageBox messageBox = new MessageBox(shell, SWT.APPLICATION_MODAL | SWT.ICON_ERROR | SWT.OK);
			messageBox.setText(Settings.getInstance().msg("general.fatalerror"));
			messageBox.setMessage(theMessage);
			messageBox.open();
		}
		ps.println(Settings.getInstance().msg("general.fatalerror") + ": " + theMessage);
		if (exception != null)
			exception.printStackTrace(ps);
	}

	public static void warningMessage(final Shell shell, final String theMessage) {
		warningMessage(shell, System.err, theMessage, null);
	}

	public static void warningMessage(final Shell shell, final Exception exception) {
		warningMessage(shell, System.err, StringRelatedUtils.toString(exception), exception);
	}

	public static void warningMessage(final Shell shell, final String theMessage, final Exception exception) {
		warningMessage(shell, System.err, theMessage, exception);
	}

	private static void warningMessage(final Shell shell, final PrintStream ps, final String theMessage, final Exception exception) {
		if (shell != null) {
			MessageBox messageBox = new MessageBox(shell, SWT.APPLICATION_MODAL | SWT.ICON_WARNING | SWT.OK);
			messageBox.setText(Settings.getInstance().msg("general.warning"));
			messageBox.setMessage(theMessage);
			messageBox.open();
		}
		ps.println(Settings.getInstance().msg("general.warning") + ": " + theMessage);
		if (exception != null)
			exception.printStackTrace(ps);
	}

	public static void infoMessage(final Shell shell, final String theMessage) {
		infoMessage(shell, System.out, theMessage);
	}

	private static void infoMessage(final Shell shell, final PrintStream ps, final String theMessage) {
		if (shell != null) {
			MessageBox messageBox = new MessageBox(shell, SWT.APPLICATION_MODAL | SWT.ICON_INFORMATION | SWT.OK);
			messageBox.setText(Settings.getInstance().msg("general.information"));
			messageBox.setMessage(theMessage);
			messageBox.open();
		}
		ps.println(Settings.getInstance().msg("general.information") + ": " + theMessage);
	}

	public static boolean confirmMessage(final Shell shell, final String theMessage) {
		MessageBox messageBox = new MessageBox(shell, SWT.APPLICATION_MODAL | SWT.ICON_WARNING | SWT.YES | SWT.NO);
		messageBox.setText(Settings.getInstance().msg("general.confirmation"));
		messageBox.setMessage(theMessage);
		return messageBox.open() == SWT.YES;
	}

	public static void initErrorDialog() {
		errorMessages = new ArrayList<String>();
		wgt = null;
		tab = null;
	}

	public static void addError(final String msg, final Control widget) {
		addError(msg, widget, null);
	}

	public static void addError(final String msg, final Control widget, final TabItem tabItem) {
		if (errorMessages.isEmpty()) {
			wgt = widget;
			tab = tabItem;
		}
		errorMessages.add(msg);
	}

	public static boolean hasErrors() {
		return !errorMessages.isEmpty();
	}

	public static boolean displayErrorDialog(final Shell shell) {
		boolean errors = !errorMessages.isEmpty();
		if (errors) {
			if (tab != null) {
				TabFolder folder = tab.getParent();
				folder.setSelection(tab);
			}
			infoMessage(shell, StringRelatedUtils.stringArrayToString(errorMessages.toArray(new String[errorMessages.size()]), "\n"));
			wgt.setFocus();
		}
		return errors;
	}
}
