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

import java.util.Date;


public class ProfileStats extends GenericStats {
	protected Date lastsetup;
	protected int setups;

	public ProfileStats(Date created, Date modified, Date lastrun, Date lastsetup, int runs, int setups) {
		super(created, modified, lastrun, runs);
		this.lastsetup = lastsetup;
		this.setups = setups;
	}

	public Date getLastsetup() {
		return lastsetup;
	}

	public int getSetups() {
		return setups;
	}
}
