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
package org.dbgl.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.dbgl.model.conf.Settings;
import org.dbgl.util.FileUtils;


public class NativeCommand {

	private File command;
	private String parameters;
	private File cwd;
	private boolean waitFor;
	private int orderNr;

	public NativeCommand(File command, String parameters, File cwd, boolean waitFor, int orderNr) {
		this.command = command;
		this.parameters = parameters;
		this.cwd = cwd;
		this.waitFor = waitFor;
		this.orderNr = orderNr;
	}

	public File getCommand() {
		return command;
	}

	public String getParameters() {
		return parameters;
	}

	public File getCwd() {
		return cwd;
	}

	public File getCwdCanToData() {
		return FileUtils.canonicalToData(cwd.getPath());
	}

	public boolean isWaitFor() {
		return waitFor;
	}

	public int getOrderNr() {
		return orderNr;
	}

	public List<String> getExecCommandsCanToData() {
		List<String> execCommands = new ArrayList<String>();
		execCommands.add(FileUtils.canonicalToData(command.getPath()).getPath());
		if (parameters.length() > 0) {
			for (String p: parameters.split(" ")) {
				execCommands.add(p);
			}
		}
		return execCommands;
	}

	public String toString() {
		if (command == null)
			return "-- DOSBox --";
		StringBuilder s = new StringBuilder(command.getPath());
		if (StringUtils.isNotEmpty(parameters))
			s.append(' ').append(parameters);
		if (!cwd.getPath().equals(command.getParent()))
			s.append(", ").append(cwd.getPath());
		if (waitFor)
			s.append(", ").append(Settings.getInstance().msg("dialog.nativecommand.waitfor"));
		return s.toString();
	}

	public static void insertDosboxCommand(final List<NativeCommand> nativeCommandsList) {
		int dosboxNr = 0;
		for (int i = 0; i < nativeCommandsList.size(); i++) {
			if (nativeCommandsList.get(i).getOrderNr() == i)
				dosboxNr++;
		}
		nativeCommandsList.add(dosboxNr, new NativeCommand(null, null, null, true, dosboxNr));
	}
}
