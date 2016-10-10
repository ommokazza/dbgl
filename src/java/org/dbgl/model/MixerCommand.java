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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;


public class MixerCommand {

	public final static String[] CHANNELS = {"master", "spkr", "sb", "gus", "fm", "disney", "cdaudio"};
	public final static int DEFAULT_VOLUME_LEVEL = 100; // percent
	public final static int MAX_VOLUME_LEVEL = 200; // percent

	public static class VolumeSetting {

		private String name;
		private int left, right;

		public VolumeSetting(String name) {
			this.name = name;
			this.left = DEFAULT_VOLUME_LEVEL;
			this.right = DEFAULT_VOLUME_LEVEL;
		}

		public void setVolume(String command) {
			try {
				String[] vols = StringUtils.split(command, ':');
				if (vols.length == 2)
					setVolume(Integer.parseInt(vols[0]), Integer.parseInt(vols[1]));
			} catch (NumberFormatException e) {}
		}

		private void setVolume(int left, int right) {
			this.left = left;
			this.right = right;
		}

		public int getLeft() {
			return left;
		}

		public int getRight() {
			return right;
		}

		public String toString() {
			return left != DEFAULT_VOLUME_LEVEL || right != DEFAULT_VOLUME_LEVEL ? new StringBuffer(name).append(' ').append(left).append(':').append(right).toString(): "";
		}
	}

	private List<VolumeSetting> volumes;

	public MixerCommand(String command) {
		init();
		String[] elements = StringUtils.split(command, ' ');
		for (int i = 0; i < elements.length; i += 2) {
			for (VolumeSetting vol: volumes) {
				if (elements[i].equalsIgnoreCase(vol.name)) {
					if ((i + 1) < elements.length)
						vol.setVolume(elements[i + 1]);
				}
			}
		}
	}

	private void init() {
		volumes = new ArrayList<VolumeSetting>();
		for (String channel: CHANNELS) {
			volumes.add(new VolumeSetting(channel));
		}
	}

	public VolumeSetting getVolumeFor(String channel) {
		for (VolumeSetting vol: volumes) {
			if (channel.equalsIgnoreCase(vol.name)) {
				return vol;
			}
		}
		return null;
	}

	public void setVolumeFor(String channel, int left, int right) {
		for (VolumeSetting vol: volumes) {
			if (channel.equalsIgnoreCase(vol.name)) {
				vol.setVolume(left, right);
			}
		}
	}

	public String toString() {
		return StringUtils.join(volumes, ' ').trim().replaceAll("\\s+", " ");
	}
}
