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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.dbgl.gui.EditProfileDialog;
import org.dbgl.interfaces.Configurable;
import org.dbgl.model.conf.Conf;
import org.dbgl.util.FileUtils;
import org.dbgl.util.PlatformUtils;
import org.dbgl.util.XmlUtils;


public class ExpProfile extends Profile implements Configurable {

	private Conf conf;
	private File baseDir, gameDir;
	private String importedFullConfig, importedIncrConfig;
	private String mapperfile;
	private List<NativeCommand> nativeCommandsList;
	private int importedId;

	public ExpProfile(final Conf conf, final List<NativeCommand> nativeCommands) {
		super(null, false);
		this.conf = conf;
		this.nativeCommandsList = nativeCommands;
		this.baseDir = null;
		this.gameDir = null;
		this.dbversionId = conf.getDbversion().getId();
	}

	public ExpProfile(final int id, final Conf conf, final File gDir, final List<NativeCommand> nativeCommands, final Profile prof) {
		super(id, prof.getConfPathAndFile(), prof.getCaptures(), prof);
		this.conf = conf;
		this.baseDir = FileUtils.makeRelativeToDosroot(new File("."));
		this.gameDir = gDir;
		this.nativeCommandsList = nativeCommands;
	}

	public ExpProfile(final Element n, final int dbversionIndex, boolean nativecommandsAvailable, final String packageVersion) {
		super(XmlUtils.getTextValue(n, "title"), Boolean.valueOf(XmlUtils.getTextValue(XmlUtils.getNode(n, "meta-info"), "favorite")));
		this.importedId = Integer.valueOf(XmlUtils.getTextValue(n, "id"));
		this.confPathAndFile = PlatformUtils.pathToNativePath(XmlUtils.getTextValue(XmlUtils.getNode(n, "config-file"), "raw"));
		this.captures = PlatformUtils.pathToNativePath(XmlUtils.getTextValue(XmlUtils.getNode(n, "captures"), "raw"));
		this.baseDir = FileUtils.makeRelativeToDosroot(new File("."));
		this.gameDir = new File(PlatformUtils.pathToNativePath(XmlUtils.getTextValue(XmlUtils.getNode(n, "game-dir"), "raw")));
		if (packageVersion.equals("1.0")) {
			this.setup = new String[] {PlatformUtils.pathToNativePath(XmlUtils.getTextValue(n, "setup")), "", ""};
			this.setupParams = new String[] {XmlUtils.getTextValue(n, "setup-parameters"), "", ""};
		} else {
			this.setup = new String[] {PlatformUtils.pathToNativePath(XmlUtils.getTextValue(n, "setup")), PlatformUtils.pathToNativePath(XmlUtils.getTextValue(n, "altexe1")),
					PlatformUtils.pathToNativePath(XmlUtils.getTextValue(n, "altexe2"))};
			this.setupParams = new String[] {XmlUtils.getTextValue(n, "setup-parameters"), XmlUtils.getTextValue(n, "altexe1-parameters"), XmlUtils.getTextValue(n, "altexe2-parameters")};
			Element map = XmlUtils.getNode(n, "keymapper-file");
			if (map != null) {
				this.mapperfile = PlatformUtils.pathToNativePath(XmlUtils.getTextValue(map, "raw"));
			}
		}
		Element metainfo = XmlUtils.getNode(n, "meta-info");
		this.developerName = XmlUtils.getTextValue(metainfo, "developer");
		this.publisherName = XmlUtils.getTextValue(metainfo, "publisher");
		this.year = XmlUtils.getTextValue(metainfo, "year");
		this.genre = XmlUtils.getTextValue(metainfo, "genre");
		this.status = XmlUtils.getTextValue(metainfo, "status");
		this.notes = XmlUtils.getTextValue(metainfo, "notes");
		this.customString = new String[] {XmlUtils.getTextValue(metainfo, "custom1"), XmlUtils.getTextValue(metainfo, "custom2"), XmlUtils.getTextValue(metainfo, "custom3"),
				XmlUtils.getTextValue(metainfo, "custom4"), XmlUtils.getTextValue(metainfo, "custom5"), XmlUtils.getTextValue(metainfo, "custom6"), XmlUtils.getTextValue(metainfo, "custom7"),
				XmlUtils.getTextValue(metainfo, "custom8")};
		this.customInt = new int[] {Integer.valueOf(XmlUtils.getTextValue(metainfo, "custom9")), Integer.valueOf(XmlUtils.getTextValue(metainfo, "custom10"))};
		this.link = new String[EditProfileDialog.AMOUNT_OF_LINKS];
		this.linkTitle = new String[EditProfileDialog.AMOUNT_OF_LINKS];
		for (int i = 0; i < EditProfileDialog.AMOUNT_OF_LINKS; i++) {
			if (packageVersion.equals("1.0") && i >= 4) {
				this.link[i] = "";
				this.linkTitle[i] = "";
			} else {
				Element link = XmlUtils.getNode(metainfo, "link" + (i + 1));
				this.link[i] = PlatformUtils.pathToNativePath(XmlUtils.getTextValue(link, "raw"));
				this.linkTitle[i] = XmlUtils.getTextValue(link, "title");
			}
		}
		this.importedFullConfig = XmlUtils.getTextValue(n, "full-configuration");
		this.importedIncrConfig = XmlUtils.getTextValue(n, "incremental-configuration");
		this.dbversionId = dbversionIndex;

		if (nativecommandsAvailable) {
			this.nativeCommandsList = new ArrayList<NativeCommand>();
			Element nativecommands = XmlUtils.getNode(n, "native-commands");

			if (nativecommands != null) {
				NodeList cmds = nativecommands.getChildNodes();

				for (int i = 0; i < cmds.getLength(); i++) {
					Node node = cmds.item(i);
					if (node instanceof Element) {
						Element cmd = (Element)cmds.item(i);
						String cwd = XmlUtils.getTextValue(cmd, "cwd");
						String command = XmlUtils.getTextValue(cmd, "command");
						nativeCommandsList.add(new NativeCommand(new File(command), XmlUtils.getTextValue(cmd, "parameters"), cwd == null ? null: new File(cwd),
								Boolean.valueOf(XmlUtils.getTextValue(cmd, "waitfor")), Integer.valueOf(XmlUtils.getTextValue(cmd, "ordernr"))));
					}
				}
			}

			NativeCommand.insertDosboxCommand(nativeCommandsList);
		}
	}

	public ExpProfile(final ExpProfile p1, final ExpProfile p2, final Conf conf) {
		super(p1, p2);
		this.conf = conf;
	}

	public ExpProfile(final ExpProfile p1, final Profile p2) {
		super(p1, p2, false);
		this.conf = p1.conf;
	}

	public void setDbversionId(final int id) {
		this.dbversionId = id;
	}

	public int getImportedId() {
		return importedId;
	}

	public void setImportedId(int importedId) {
		this.importedId = importedId;
	}

	public String getCapturesExport() {
		return FileUtils.constructCapturesDir(getId());
	}

	public String getMapperExport() {
		return FileUtils.constructMapperFile(getId());
	}

	public String getMapperfile() {
		return mapperfile;
	}

	public File getBaseDir() {
		return baseDir;
	}

	public File getGameDir() {
		return gameDir;
	}

	public void setBaseDir(File baseDir) {
		this.baseDir = baseDir;
	}

	public void setGameDir(File gameDir) {
		this.gameDir = gameDir;
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
		boolean listExport = (conf == null);
		Element profEmt = xmldoc.createElement("profile");
		XmlUtils.addCDataElement(xmldoc, profEmt, "title", getTitle());
		XmlUtils.addElement(xmldoc, profEmt, "id", String.valueOf(getId()));
		Element captures = xmldoc.createElement("captures");
		XmlUtils.addElement(xmldoc, captures, "raw", getCapturesExport());
		if (listExport) {
			XmlUtils.addElement(xmldoc, captures, "url", getCapturesAsUrl());
		}
		profEmt.appendChild(captures);
		Element config = xmldoc.createElement("config-file");
		XmlUtils.addElement(xmldoc, config, "raw", FileUtils.makeRelativeToData(new File(getConfPathAndFile())).getPath());
		if (listExport) {
			XmlUtils.addElement(xmldoc, config, "url", getConfFileAsUrl());
		}
		profEmt.appendChild(config);
		if ((conf != null) && (conf.getCustomMapperFile() != null)) {
			Element map = xmldoc.createElement("keymapper-file");
			XmlUtils.addElement(xmldoc, map, "raw", getMapperExport());
			profEmt.appendChild(map);
		}
		if (gameDir != null) {
			Element gamedir = xmldoc.createElement("game-dir");
			XmlUtils.addElement(xmldoc, gamedir, "raw", PlatformUtils.toDosboxPath(gameDir.getPath()));
			profEmt.appendChild(gamedir);
		}
		XmlUtils.addElement(xmldoc, profEmt, "setup", getSetup(0));
		XmlUtils.addElement(xmldoc, profEmt, "altexe1", getSetup(1));
		XmlUtils.addElement(xmldoc, profEmt, "altexe2", getSetup(2));
		XmlUtils.addElement(xmldoc, profEmt, "setup-parameters", getSetupParameters(0));
		XmlUtils.addElement(xmldoc, profEmt, "altexe1-parameters", getSetupParameters(1));
		XmlUtils.addElement(xmldoc, profEmt, "altexe2-parameters", getSetupParameters(2));
		Element meta = xmldoc.createElement("meta-info");
		XmlUtils.addCDataElement(xmldoc, meta, "developer", getDeveloperName());
		XmlUtils.addCDataElement(xmldoc, meta, "publisher", getPublisherName());
		XmlUtils.addCDataElement(xmldoc, meta, "year", getYear());
		XmlUtils.addCDataElement(xmldoc, meta, "genre", getGenre());
		XmlUtils.addCDataElement(xmldoc, meta, "status", getStatus());
		XmlUtils.addElement(xmldoc, meta, "favorite", String.valueOf(isDefault()));
		XmlUtils.addCDataElement(xmldoc, meta, "notes", XmlUtils.cleanEolnForXml(getNotes()));
		for (int i = 0; i < 8; i++) {
			XmlUtils.addCDataElement(xmldoc, meta, "custom" + (i + 1), getCustomString(i));
		}
		for (int i = 0; i < 2; i++) {
			XmlUtils.addElement(xmldoc, meta, "custom" + (i + 9), String.valueOf(getCustomInt(i)));
		}
		for (int i = 0; i < EditProfileDialog.AMOUNT_OF_LINKS; i++) {
			Element link = xmldoc.createElement("link" + (i + 1));
			XmlUtils.addElement(xmldoc, link, "raw", getLink(i));
			if (listExport) {
				XmlUtils.addElement(xmldoc, link, "url", getLinkAsUrl(i));
			}
			XmlUtils.addCDataElement(xmldoc, link, "title", getLinkTitle(i));
			meta.appendChild(link);
		}
		profEmt.appendChild(meta);
		if (conf != null) {
			XmlUtils.addCDataElement(xmldoc, profEmt, "full-configuration", XmlUtils.cleanEolnForXml(conf.toFullConfString(false)));
			XmlUtils.addCDataElement(xmldoc, profEmt, "incremental-configuration", XmlUtils.cleanEolnForXml(conf.toIncrConfString(false)));
		}
		DosboxVersion dbv = DosboxVersion.findById(dbversionsList, getDbversionId());
		Element dosbox = xmldoc.createElement("dosbox");
		XmlUtils.addCDataElement(xmldoc, dosbox, "title", dbv.getValue());
		XmlUtils.addElement(xmldoc, dosbox, "version", dbv.getVersion());
		profEmt.appendChild(dosbox);

		if ((nativeCommandsList != null) && (nativeCommandsList.size() > 1)) {
			Element nativecommands = xmldoc.createElement("native-commands");
			for (NativeCommand cmd: nativeCommandsList) {
				if (cmd.getCommand() != null) {
					Element nativecommand = xmldoc.createElement("native-command");
					XmlUtils.addElement(xmldoc, nativecommand, "command", cmd.getCommand().getPath());
					XmlUtils.addElement(xmldoc, nativecommand, "parameters", cmd.getParameters());
					XmlUtils.addElement(xmldoc, nativecommand, "cwd", cmd.getCwd().getPath());
					XmlUtils.addElement(xmldoc, nativecommand, "waitfor", String.valueOf(cmd.isWaitFor()));
					XmlUtils.addElement(xmldoc, nativecommand, "ordernr", String.valueOf(cmd.getOrderNr()));
					nativecommands.appendChild(nativecommand);
				}
			}
			profEmt.appendChild(nativecommands);
		}

		return profEmt;
	}

	public List<NativeCommand> getNativeCommandsList() {
		return nativeCommandsList;
	}

	public void setNativeCommandsList(List<NativeCommand> nativeCommandsList) {
		this.nativeCommandsList = nativeCommandsList;
	}
}
