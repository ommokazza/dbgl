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
package org.dbgl.gui;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import swing2swt.layout.BorderLayout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.dbgl.gui.BrowseButton.BrowseType;
import org.dbgl.gui.BrowseButton.CanonicalType;
import org.dbgl.model.DosboxVersion;
import org.dbgl.model.Profile;
import org.dbgl.model.conf.Settings;
import org.dbgl.util.FileUtils;
import org.dbgl.util.PlatformUtils;
import org.dbgl.util.XmlUtils;


public class ExportListDialog extends Dialog {

	private Text filename;
	private File[] files;
	private List<Profile> profs;
	private List<DosboxVersion> dbversionsList;
	private Shell shell;
	private Settings settings;

	public ExportListDialog(final Shell parent, final int style) {
		super(parent, style);
	}

	public ExportListDialog(final Shell parent, final List<DosboxVersion> dbversionsList, final List<Profile> profs) {
		this(parent, SWT.NONE);
		this.dbversionsList = dbversionsList;
		this.profs = profs;
	}

	public Object open() {
		settings = Settings.getInstance();
		if (init()) {
			createContents();
			shell.open();
			shell.layout();
			Display display = getParent().getDisplay();
			while (!shell.isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
		}
		return null;
	}

	protected boolean init() {
		File path = FileUtils.canonicalToData(FileUtils.XSL_DIR);
		FilenameFilter xslExtension = new FilenameFilter() {
			public boolean accept(final File dir, final String name) {
				return name.toLowerCase().endsWith(FileUtils.XSL_EXT);
			}
		};
		files = path.listFiles(xslExtension);
		if (files == null) {
			GeneralPurposeDialogs.fatalMessage(getParent(), settings.msg("dialog.exportlist.error.noxsldir"));
			return false;
		}
		if (files.length == 0) {
			GeneralPurposeDialogs.fatalMessage(getParent(), settings.msg("dialog.exportlist.error.noxslfiles"));
			return false;
		}
		return true;
	}

	protected void createContents() {
		shell = new Shell(getParent(), SWT.TITLE | SWT.CLOSE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setLayout(new BorderLayout(0, 0));
		shell.addControlListener(new SizeControlAdapter(shell, "exportlistdialog"));
		shell.setText(settings.msg("dialog.exportlist.title"));

		final Group settingsGroup = new Group(shell, SWT.NONE);
		settingsGroup.setLayout(new GridLayout(3, false));
		settingsGroup.setText(settings.msg("dialog.exportlist.options"));
		settingsGroup.setLayoutData(BorderLayout.NORTH);

		final Label fileTypeLabel = new Label(settingsGroup, SWT.NONE);
		fileTypeLabel.setText(settings.msg("dialog.exportlist.exportfiletype"));

		final Combo fileTypes = new Combo(settingsGroup, SWT.READ_ONLY);
		fileTypes.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		for (File file: files) {
			fileTypes.add(file.getName().substring(0, file.getName().length() - 4));
		}

		fileTypes.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				String file = filename.getText();
				String type = fileTypes.getItem(fileTypes.getSelectionIndex());
				int usi = type.lastIndexOf('_');
				if (usi != -1) {
					type = type.substring(usi + 1);
				}
				int index = file.lastIndexOf('.');
				if (index == -1) {
					filename.setText(file + '.' + type);
				} else {
					filename.setText(file.substring(0, index + 1) + type);
				}
			}
		});

		final Label fileLabel = new Label(settingsGroup, SWT.NONE);
		fileLabel.setText(settings.msg("dialog.exportlist.filename"));

		filename = new Text(settingsGroup, SWT.BORDER);
		filename.setText(FileUtils.EXPORT_DIR + "dbgllist");
		filename.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		fileTypes.select(0);
		fileTypes.notifyListeners(SWT.Selection, new Event());

		final BrowseButton browseButton = new BrowseButton(settingsGroup, SWT.NONE);
		browseButton.connect(shell, filename, null, BrowseType.FILE, CanonicalType.DOC, true, null);

		final Label saveXmlLabel = new Label(settingsGroup, SWT.NONE);
		saveXmlLabel.setText(settings.msg("dialog.exportlist.exportintermediatexml"));
		final Button saveXml = new Button(settingsGroup, SWT.CHECK);
		new Label(settingsGroup, SWT.NONE);

		final Button startButton = new Button(settingsGroup, SWT.NONE);
		shell.setDefaultButton(startButton);
		startButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		startButton.setText(settings.msg("dialog.exportlist.startexport"));

		final Button cancelButton = new Button(settingsGroup, SWT.NONE);
		cancelButton.setLayoutData(new GridData(120, SWT.DEFAULT));
		cancelButton.setText(settings.msg("button.cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				shell.close();
			}
		});
		new Label(settingsGroup, SWT.NONE);

		startButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				if (!isValid()) {
					return;
				}
				try {
					DOMSource xmlSource = new DOMSource(XmlUtils.getProfilesXML(profs, dbversionsList));
					if (saveXml.getSelection()) {
						XmlUtils.saveDomSource(xmlSource, FileUtils.canonicalToData(filename.getText() + FileUtils.XML_EXT), null);
					}
					XmlUtils.saveDomSource(xmlSource, FileUtils.canonicalToData(filename.getText()),
						FileUtils.canonicalToData(FileUtils.XSL_DIR + fileTypes.getItem(fileTypes.getSelectionIndex()) + FileUtils.XSL_EXT));
					if (GeneralPurposeDialogs.confirmMessage(shell, settings.msg("dialog.exportlist.confirm.viewexport"))) {
						PlatformUtils.openForBrowsing(FileUtils.canonicalToData(filename.getText()).getPath());
					}
				} catch (IOException | TransformerException | ParserConfigurationException e) {
					GeneralPurposeDialogs.warningMessage(shell, e);
				}
			}
		});
	}

	private boolean isValid() {
		GeneralPurposeDialogs.initErrorDialog();
		String file = filename.getText();
		if ("".equals(file)) {
			GeneralPurposeDialogs.addError(settings.msg("dialog.exportlist.required.filename"), filename);
		} else if (FileUtils.isExistingFile(FileUtils.canonicalToData(file))) {
			if (!GeneralPurposeDialogs.confirmMessage(shell, settings.msg("dialog.exportlist.confirm.overwrite", new Object[] {FileUtils.canonicalToData(file)}))) {
				GeneralPurposeDialogs.addError(settings.msg("dialog.exportlist.notice.anotherfilename"), filename);
			}
		} else {
			File dir = FileUtils.canonicalToData(file).getParentFile();
			if (dir == null || !dir.exists()) {
				GeneralPurposeDialogs.addError(settings.msg("dialog.exportlist.error.dirmissing"), filename);
			}
		}
		return !GeneralPurposeDialogs.displayErrorDialog(shell);
	}
}
