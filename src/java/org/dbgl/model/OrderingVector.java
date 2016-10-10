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


public class OrderingVector {

	private static class OrderingElement {

		private final int column;
		private final boolean ascending;

		OrderingElement(final int col, final boolean asc) {
			this.column = col;
			this.ascending = asc;
		}

		public int hashCode() {
			return toString().hashCode();
		}

		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}

			return this.column == ((OrderingElement)obj).column;
		}

		public String toString() {
			return GAME_LIST_ORDER[(column == 21) ? 9: column] + (ascending ? " ASC": " DESC"); // order by db id
		}
	}

	private static final int MAX_ORDERING_COLS = 8;
	private static final String[] GAME_LIST_ORDER = {"LOWER(GAM.TITLE)", "GAM.SETUP", "LOWER(DEV.NAME)", "LOWER(PUBL.NAME)", "LOWER(GEN.NAME)", "YR.YEAR", "LOWER(STAT.STAT)", "GAM.FAVORITE", "GAM.ID",
			"GAM.DBVERSION_ID", "LOWER(CUST1.VALUE)", "LOWER(CUST2.VALUE)", "LOWER(CUST3.VALUE)", "LOWER(CUST4.VALUE)", "LOWER(GAM.CUSTOM5)", "LOWER(GAM.CUSTOM6)", "LOWER(GAM.CUSTOM7)",
			"LOWER(GAM.CUSTOM8)", "GAM.CUSTOM9", "GAM.CUSTOM10", "LOWER(GAM.CAPTURES)", "", "GAM.STATS_CREATED", "GAM.STATS_LASTMODIFY", "GAM.STATS_LASTRUN", "GAM.STATS_LASTSETUP", "GAM.STATS_RUNS",
			"GAM.STATS_SETUPS"};

	private final List<OrderingElement> vector;

	public OrderingVector(final int[] columnArray, final boolean[] ascendingArray) {
		vector = new ArrayList<OrderingElement>();
		for (int i = 0; i < columnArray.length; i++) {
			vector.add(new OrderingElement(columnArray[i], ascendingArray[i]));
		}
	}

	public void addOrdering(final int column, final boolean ascending) {
		OrderingElement newOrdering = new OrderingElement(column, ascending);
		int existingIndex = vector.indexOf(newOrdering);
		if (existingIndex != -1) {
			vector.remove(existingIndex);
		}
		vector.add(0, newOrdering);
		if (vector.size() > MAX_ORDERING_COLS) {
			vector.remove(MAX_ORDERING_COLS);
		}
	}

	public int[] getColumns() {
		int[] columnArray = new int[vector.size()];
		for (int i = 0; i < columnArray.length; i++) {
			columnArray[i] = vector.get(i).column;
		}
		return columnArray;
	}

	public boolean[] getAscendings() {
		boolean[] ascendingArray = new boolean[vector.size()];
		for (int i = 0; i < ascendingArray.length; i++) {
			ascendingArray[i] = vector.get(i).ascending;
		}
		return ascendingArray;
	}

	public String toClause() {
		StringBuffer orderingClause = new StringBuffer();
		if (!vector.isEmpty()) {
			orderingClause.append(" ORDER BY ");
		}
		for (int index = 0; index < vector.size(); index++) {
			OrderingElement element = vector.get(index);
			orderingClause.append(element.toString());
			if (index + 1 < vector.size()) {
				orderingClause.append(',');
			}
		}
		return orderingClause.toString();
	}
}
