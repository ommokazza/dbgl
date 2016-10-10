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
package org.dbgl.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.dbgl.model.DosboxVersion;
import org.dbgl.model.ExpProfile;
import org.dbgl.model.ExpTemplate;
import org.dbgl.model.Profile;
import org.dbgl.model.Constants;
import org.dbgl.model.conf.Settings;


public class XmlUtils {

	private final static String PROFILES_XML_FORMAT_VERSION = "1.2";
	private final static String TEMPLATES_XML_FORMAT_VERSION = "1.0";
	public final static SimpleDateFormat datetimeFormatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

	public static Element addElement(final Document dom, final Element el, final String name, final String value) {
		Element newElement = dom.createElement(name);
		newElement.appendChild(dom.createTextNode(value));
		el.appendChild(newElement);
		return el;
	}

	public static Element addCDataElement(final Document dom, final Element el, final String name, final String value) {
		Element newElement = dom.createElement(name);
		newElement.appendChild(dom.createCDATASection(value));
		el.appendChild(newElement);
		return el;
	}

	public static void saveDomSource(final DOMSource source, final File target, final File xslt) throws TransformerException, IOException {
		TransformerFactory transFact = TransformerFactory.newInstance();
		transFact.setAttribute("indent-number", 2);
		Transformer trans;
		if (xslt == null) {
			trans = transFact.newTransformer();
		} else {
			trans = transFact.newTransformer(new StreamSource(xslt));
		}
		trans.setOutputProperty(OutputKeys.INDENT, "yes");
		FileOutputStream fos = new FileOutputStream(target);
		trans.transform(source, new StreamResult(new OutputStreamWriter(fos, "UTF-8")));
		fos.close();
	}

	public static void domToZipOutputStream(final Document doc, final File zipFileEntry, final ZipOutputStream zipOutputStream) throws IOException, TransformerException {
		ZipEntry anEntry = new ZipEntry(PlatformUtils.toArchivePath(zipFileEntry, false));
		zipOutputStream.putNextEntry(anEntry);
		TransformerFactory tfactory = TransformerFactory.newInstance();
		tfactory.setAttribute("indent-number", 2);
		Transformer transformer = tfactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.transform(new DOMSource(doc), new StreamResult(new OutputStreamWriter(zipOutputStream, "UTF-8")));
		zipOutputStream.closeEntry();
	}

	public static Document getProfilesXML(final List<Profile> profs, final List<DosboxVersion> dbversions) throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document xmldoc = builder.newDocument();
		Element root = xmldoc.createElement("document");
		root.appendChild(getProfileExportElement(xmldoc, Settings.getInstance().msg("exportlist.title"), "", "", false, false, false, false));
		for (Profile profile: profs) {
			root.appendChild(new ExpProfile(profile.getId(), null, null, null, profile).getXml(xmldoc, dbversions));
		}
		xmldoc.appendChild(root);
		return xmldoc;
	}

	public static Document getFullProfilesXML(final List<ExpProfile> profs, final List<DosboxVersion> dbversions, final String title, final String notes, final String author, final boolean captures,
			final boolean mapperfiles, final boolean nativecommands, final boolean gameData) throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document xmldoc = builder.newDocument();
		Element root = xmldoc.createElement("document");
		root.appendChild(getProfileExportElement(xmldoc, title, notes, author, captures, mapperfiles, nativecommands, gameData));
		for (ExpProfile profile: profs) {
			root.appendChild(profile.getXml(xmldoc, dbversions));
		}
		xmldoc.appendChild(root);
		return xmldoc;
	}

	private static Element getProfileExportElement(final Document xmldoc, final String title, final String notes, final String author, final boolean captures, final boolean mapperfiles,
			final boolean nativecommands, final boolean gameData) {
		Element export = xmldoc.createElement("export");
		addElement(xmldoc, export, "format-version", PROFILES_XML_FORMAT_VERSION);
		addCDataElement(xmldoc, export, "title", title);
		addCDataElement(xmldoc, export, "author", author);
		addCDataElement(xmldoc, export, "notes", cleanEolnForXml(notes));
		addCDataElement(xmldoc, export, "creationdatetime", datetimeFormatter.format(new Date()));
		addCDataElement(xmldoc, export, "generator-title", Constants.PROGRAM_NAME_FULL);
		addElement(xmldoc, export, "generator-version", Constants.PROGRAM_VERSION);
		addElement(xmldoc, export, "captures-available", String.valueOf(captures));
		addElement(xmldoc, export, "keymapperfiles-available", String.valueOf(mapperfiles));
		addElement(xmldoc, export, "nativecommands-available", String.valueOf(nativecommands));
		addElement(xmldoc, export, "gamedata-available", String.valueOf(gameData));
		Settings settings = Settings.getInstance();
		for (int i = 0; i < Constants.EDIT_COLUMN_NAMES; i++) {
			String s = settings.getSettings().getValue("gui", "custom" + (i + 1));
			addCDataElement(xmldoc, export, "custom" + (i + 1), s);
		}
		return export;
	}

	public static Document getFullTemplatesXML(final List<ExpTemplate> templates, final List<DosboxVersion> dbversions, final String title, final String notes, final String author)
			throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document xmldoc = builder.newDocument();
		Element root = xmldoc.createElement("document");
		Element export = xmldoc.createElement("export");
		addElement(xmldoc, export, "format-version", TEMPLATES_XML_FORMAT_VERSION);
		addCDataElement(xmldoc, export, "title", title);
		addCDataElement(xmldoc, export, "author", author);
		addCDataElement(xmldoc, export, "notes", cleanEolnForXml(notes));
		addCDataElement(xmldoc, export, "creationdatetime", datetimeFormatter.format(new Date()));
		addCDataElement(xmldoc, export, "generator-title", Constants.PROGRAM_NAME_FULL);
		addElement(xmldoc, export, "generator-version", Constants.PROGRAM_VERSION);
		root.appendChild(export);
		for (ExpTemplate template: templates) {
			root.appendChild(template.getXml(xmldoc, dbversions));
		}
		xmldoc.appendChild(root);
		return xmldoc;
	}

	public static String cleanEolnForXml(final String s) {
		return s.replaceAll("\\r", "");
	}

	public static String getTextValue(final Element element, final String tagName) {
		Node n = getNode(element, tagName);
		if (n != null) {
			Node child = n.getFirstChild();
			return (child == null) ? "": child.getNodeValue();
		}
		return null;
	}

	public static Element getNode(final Element element, final String tagName) {
		NodeList nodelist = element.getElementsByTagName(tagName);
		if (nodelist != null && nodelist.getLength() > 0) {
			return (Element)nodelist.item(0);
		}
		return null;
	}
}
