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
package org.dbgl.model.conf;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
public class SharedConf implements Serializable, Comparable<SharedConf> {

	private static final long serialVersionUID = 5570022733154073208L;

	public static short CONF_STATE_NEW = 0;
	public static short CONF_STATE_ONLINE = 10;

	private int id;
	private String author, notes;
	private String gameTitle, gameVersion, gameYear;
	private String incrConf, fullConf;
	private String explanation;
	private String dosboxTitle, dosboxVersion;
	private short state;
	private Date insertDate;
	private int submissionId;

	public SharedConf() {}

	public SharedConf(int id, String author, String notes, String gameTitle, String gameVersion, String gameYear, String incrConf, String fullConf, String explanation, String dosboxTitle,
			String dosboxVersion, short state, Date insertDate, int submissionId) {
		super();
		this.id = id;
		this.author = author;
		this.notes = notes;
		this.gameTitle = gameTitle;
		this.gameVersion = gameVersion;
		this.gameYear = gameYear;
		this.incrConf = incrConf;
		this.fullConf = fullConf;
		this.explanation = explanation;
		this.dosboxTitle = dosboxTitle;
		this.dosboxVersion = dosboxVersion;
		this.state = state;
		this.insertDate = insertDate;
		this.submissionId = submissionId;
	}

	public SharedConf(String author, String notes, String gameTitle, String gameVersion, String gameYear, String incrConf, String fullConf, String explanation, String dosboxTitle,
			String dosboxVersion) {
		this(-1, author, notes, gameTitle, gameVersion, gameYear, incrConf, fullConf, explanation, dosboxTitle, dosboxVersion, CONF_STATE_NEW, new Date(), -1);
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public String getGameTitle() {
		return gameTitle;
	}

	public void setGameTitle(String gameTitle) {
		this.gameTitle = gameTitle;
	}

	public String getGameVersion() {
		return gameVersion;
	}

	public void setGameVersion(String gameVersion) {
		this.gameVersion = gameVersion;
	}

	public String getGameYear() {
		return gameYear;
	}

	public void setGameYear(String gameYear) {
		this.gameYear = gameYear;
	}

	public String getIncrConf() {
		return incrConf;
	}

	public void setIncrConf(String incrConf) {
		this.incrConf = incrConf;
	}

	public String getFullConf() {
		return fullConf;
	}

	public void setFullConf(String fullConf) {
		this.fullConf = fullConf;
	}

	public String getExplanation() {
		return explanation;
	}

	public void setExplanation(String explanation) {
		this.explanation = explanation;
	}

	public String getDosboxTitle() {
		return dosboxTitle;
	}

	public void setDosboxTitle(String dosboxTitle) {
		this.dosboxTitle = dosboxTitle;
	}

	public String getDosboxVersion() {
		return dosboxVersion;
	}

	public void setDosboxVersion(String dosboxVersion) {
		this.dosboxVersion = dosboxVersion;
	}

	public short getState() {
		return state;
	}

	public void setState(short state) {
		this.state = state;
	}

	public Date getInsertDate() {
		return insertDate;
	}

	public void setInsertDate(Date insertDate) {
		this.insertDate = insertDate;
	}

	public int getSubmissionId() {
		return submissionId;
	}

	public void setSubmissionId(int submissionId) {
		this.submissionId = submissionId;
	}

	public int compareTo(SharedConf otherConf) {
		return gameTitle.compareToIgnoreCase(otherConf.gameTitle);
	}

	public static final class byTitle implements Comparator<SharedConf> {
		public int compare(final SharedConf conf1, final SharedConf conf2) {
			return conf1.gameTitle.compareToIgnoreCase(conf2.gameTitle);
		}
	}

	public static final class byYear implements Comparator<SharedConf> {
		public int compare(final SharedConf conf1, final SharedConf conf2) {
			return conf1.gameYear.compareToIgnoreCase(conf2.gameYear);
		}
	}

	public static final class byVersion implements Comparator<SharedConf> {
		public int compare(final SharedConf conf1, final SharedConf conf2) {
			return conf1.gameVersion.compareToIgnoreCase(conf2.gameVersion);
		}
	}
}
