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
package org.dbgl.connect;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import org.dbgl.gui.MainWindow;
import org.dbgl.model.conf.Settings;


public final class Messaging extends Thread {

	private transient final int port;
	private transient final Object obj;
	private transient ServerSocket server = null;
	private transient boolean cont = true;
	private Settings settings;

	public Messaging(final int port, final Object obj) {
		this.port = port;
		this.obj = obj;
		settings = Settings.getInstance();
	}

	public void close() {
		cont = false;
		try {
			if (server != null) {
				server.close();
			}
		} catch (IOException e) {
			System.err.println(settings.msg("communication.error.closesocket"));
		}
	}

	public void run() {
		try {
			server = new ServerSocket(port);
			System.out.println(settings.msg("communication.notice.listening", new Object[] {port}));
		} catch (IOException e) {
			cont = false;
			System.err.println(settings.msg("communication.error.createsocket", new Object[] {port}));
		}

		while (cont) {
			try {

				Socket client = server.accept();
				BufferedReader bufferedIStream = new BufferedReader(new InputStreamReader(client.getInputStream()));
				System.out.print(settings.msg("communication.notice.receivingmessage"));
				String line = bufferedIStream.readLine();
				System.out.println(' ' + line);
				client.close();
				if (line != null && line.startsWith("sendtoprofile ")) {
					((MainWindow)obj).addProfile(line.substring(14));
				}
			} catch (IOException e) {
				if (cont) {
					System.err.println(settings.msg("communication.error.io"));
				}
			}
		}
	}
}
