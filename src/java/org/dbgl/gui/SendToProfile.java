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

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import org.dbgl.model.conf.Settings;


public final class SendToProfile {

	private static final String LOCALHOST = "localhost";

	public static void main(final String args[]) {
		if (args.length < 1) {
			System.out.println(Settings.getInstance().msg("external.notice.addcmdusage"));
			System.exit(1);
		}

		try {

			Settings settings = Settings.getInstance();
			Socket socket = new Socket(LOCALHOST, settings.getSettings().getIntValue("communication", "port"));
			new PrintWriter(socket.getOutputStream(), true).println("sendtoprofile " + args[0]);
			socket.close();

		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			Launcher.loadSWT();
			MainWindow.openSendToProfileDialog(args[0]);
		}
	}
}
