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


public class LogEntry {

	public static enum Event {
		ADD(1), EDIT(2), REMOVE(3), DUPLICATE(5), RUN(10), SETUP(11);

		@SuppressWarnings("unused")
		private int value;

		private Event(int value) {
			this.value = value;
		}
	};

	public static enum EntityType {
		PROFILE(1), DOSBOXVERSION(2), TEMPLATE(3), FILTER(4);

		@SuppressWarnings("unused")
		private int value;

		private EntityType(int value) {
			this.value = value;
		}
	};

	private int id;
	private Date time;
	private Event event;
	private EntityType entityType;
	private int entityId;
	private String entityTitle;

	public LogEntry(int id, Date time, byte event, byte entityType, int entityId, String entityTitle) {
		this.id = id;
		this.time = time;
		this.event = Event.values()[event];
		this.entityType = EntityType.values()[entityType];
		this.entityId = entityId;
		this.entityTitle = entityTitle;
	}

	public int getId() {
		return id;
	}

	public Date getTime() {
		return time;
	}

	public Event getEvent() {
		return event;
	}

	public EntityType getEntityType() {
		return entityType;
	}

	public int getEntityId() {
		return entityId;
	}

	public String getEntityTitle() {
		return entityTitle;
	}
}