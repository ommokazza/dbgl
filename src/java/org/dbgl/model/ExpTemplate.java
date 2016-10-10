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

import java.util.List;
import org.dbgl.interfaces.Configurable;
import org.dbgl.model.conf.Conf;
import org.dbgl.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class ExpTemplate extends Template implements Configurable {

	private Conf conf;
	private String importedFullConfig, importedIncrConfig;
	private List<NativeCommand> nativeCommandsList;

	public ExpTemplate(final Conf conf, List<NativeCommand> nativeCommands) {
		super(-1, null, -1, false, null, null, null, 0);
		this.conf = conf;
		this.nativeCommandsList = nativeCommands;
	}

	public ExpTemplate(final int id, final Conf conf, final Template template) {
		super(id, template);
		this.conf = conf;
	}

	public ExpTemplate(final Element n, final int dbversionIndex) {
		super(-1, XmlUtils.getTextValue(n, "title"), dbversionIndex, false, null, null, null, 0);
		this.importedFullConfig = XmlUtils.getTextValue(n, "full-configuration");
		this.importedIncrConfig = XmlUtils.getTextValue(n, "incremental-configuration");
	}

	public void setDbversionId(final int id) {
		this.dbversionId = id;
	}

	public String getImportedFullConfig() {
		return importedFullConfig;
	}

	public String getImportedIncrConfig() {
		return importedIncrConfig;
	}

	public Conf getConf() {
		return conf;
	}

	public void setConf(Conf conf) {
		this.conf = conf;
	}

	public Element getXml(final Document xmldoc, final List<DosboxVersion> dbversionsList) {
		Element profEmt = xmldoc.createElement("template");
		XmlUtils.addCDataElement(xmldoc, profEmt, "title", getTitle());
		if (conf != null) {
			XmlUtils.addCDataElement(xmldoc, profEmt, "full-configuration", XmlUtils.cleanEolnForXml(conf.toFullConfString(false)));
			XmlUtils.addCDataElement(xmldoc, profEmt, "incremental-configuration", XmlUtils.cleanEolnForXml(conf.toIncrConfString(false)));
		}
		DosboxVersion dbv = DosboxVersion.findById(dbversionsList, getDbversionId());
		Element dosbox = xmldoc.createElement("dosbox");
		XmlUtils.addCDataElement(xmldoc, dosbox, "title", dbv.getValue());
		XmlUtils.addElement(xmldoc, dosbox, "version", dbv.getVersion());
		profEmt.appendChild(dosbox);

		return profEmt;
	}

	public List<NativeCommand> getNativeCommandsList() {
		return nativeCommandsList;
	}

	public void setNativeCommandsList(List<NativeCommand> nativeCommandsList) {
		this.nativeCommandsList = nativeCommandsList;
	}
}
