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


public class Template extends KeyTitleDefault {

	protected int dbversionId;
	protected GenericStats stats;

	public Template(final int id, final String title, final int dbversionId, final boolean isDefault, final Date created, final Date modified, final Date lastrun, final int runs) {
		super(id, title, isDefault);
		this.dbversionId = dbversionId;
		this.stats = new GenericStats(created, modified, lastrun, runs);
	}

	public Template(final int id, final Template t) {
		this(id, t.getTitle(), t.getDbversionId(), false, t.stats.getCreated(), t.stats.getModified(), t.stats.getLastrun(), t.stats.getRuns());
	}

	public int getDbversionId() {
		return dbversionId;
	}

	public GenericStats getStats() {
		return stats;
	}
}
