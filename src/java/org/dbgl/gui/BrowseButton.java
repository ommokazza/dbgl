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
import java.util.ArrayList;
import java.util.Collections;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.dbgl.model.conf.Settings;
import org.dbgl.swtdesigner.SWTImageManager;
import org.dbgl.util.FileUtils;
import org.dbgl.util.PlatformUtils;
import org.dbgl.util.StringRelatedUtils;


public final class BrowseButton {

	public enum BrowseType {
		DIR, FILE
	};
	public enum CanonicalType {
		DOSROOT, DFEND, CDIMAGE, ZIP, DBGLZIP, DOSBOX, DOSBOXCONF, DOC, BOOTER, EXE, INSTALLER, NATIVE_EXE, NONE
	};

	private final Button button;
	private Settings settings;

	private class Sa extends SelectionAdapter {

		private final Text field;
		private final Text alt;
		private final BrowseType browse;
		private final CanonicalType canon;
		private final boolean save;
		private final Combo combo;
		private final Shell shell;

		public Sa(final Shell shell, final Text control, final Text altControl, final BrowseType browse, final CanonicalType canon, final boolean save, final Combo combo) {
			this.shell = shell;
			this.field = control;
			this.alt = altControl;
			this.browse = browse;
			this.canon = canon;
			this.save = save;
			this.combo = combo;
		}

		private String filterPath() {
			String path = field.getText();
			CanonicalType fileType = canon;
			if (path.equals("") && (alt != null)) {
				path = alt.getText();
				if (fileType == CanonicalType.DOC)
					fileType = CanonicalType.DOSROOT;
			} else if (browse == BrowseType.DIR && fileType == CanonicalType.NONE) {
				fileType = CanonicalType.DOSROOT;
			}
			switch (fileType) {
				case DOC:
				case DBGLZIP:
				case NATIVE_EXE:
					return FileUtils.canonicalToData(path).getPath();
				case EXE:
				case INSTALLER:
				case ZIP:
				case BOOTER:
				case DOSROOT:
					return FileUtils.canonicalToDosroot(path).getPath();
				case DOSBOX:
				case DOSBOXCONF:
					return FileUtils.canonicalToDosbox(path).getPath();
				case DFEND:
					return PlatformUtils.DFEND_PATH;
				case CDIMAGE:
					String[] fPaths = StringRelatedUtils.textAreaToStringArray(field.getText(), field.getLineDelimiter());
					if (fPaths.length > 0) {
						return FileUtils.canonicalToDosroot(fPaths[0]).getPath();
					} else {
						return FileUtils.getDosRoot();
					}
				default:
					return "";
			}
		}

		private String getResult(final String result, final File filterPath, final String[] filenames) {
			switch (canon) {
				case DOC:
				case DBGLZIP:
				case NATIVE_EXE:
					return FileUtils.makeRelativeToData(new File(result)).getPath();
				case EXE:
				case INSTALLER:
					if (FileUtils.isArchive(result) || FileUtils.isIsoFile(result) || FileUtils.isFatImage(result)) {
						BrowseArchiveDialog dialog = new BrowseArchiveDialog(shell, SWT.OPEN);
						dialog.setFileToBrowse(result);
						String choice = (String)dialog.open();
						return (choice == null) ? null: FileUtils.makeRelativeToDosroot(new File(choice)).getPath();
					}
				case ZIP:
				case BOOTER:
				case DOSROOT:
					return FileUtils.makeRelativeToDosroot(new File(result)).getPath();
				case DOSBOX:
					if (PlatformUtils.IS_OSX) {
						File f = FileUtils.makeRelativeToDosbox(new File(result));
						if (f.getName().endsWith(PlatformUtils.DB_APP_EXT)) {
							File exe = new File(f, PlatformUtils.DB_APP_EXE);
							if (FileUtils.isExistingFile(exe)) {
								return exe.getParent();
							}
						} else if (f.getName().equals(PlatformUtils.DB_EXECUTABLE)) {
							return f.getParent();
						}
					}
				case DOSBOXCONF:
					return FileUtils.makeRelativeToDosbox(new File(result)).getPath();
				case CDIMAGE:
					File path = FileUtils.makeRelativeToDosroot(filterPath);
					StringBuffer images = new StringBuffer();
					for (String file: filenames) {
						images.append(new File(path, file)).append(field.getLineDelimiter());
					}
					return images.toString();
				default:
					return result;
			}
		}

		public void widgetSelected(final SelectionEvent event) {
			String result = null;
			File filterPath = null;
			String[] filenames = null;

			shell.setEnabled(false);

			String rawFilterPath = filterPath();
			File fpath = new File(rawFilterPath);
			if (fpath != null && !fpath.isDirectory())
				fpath = fpath.getParentFile();

			if (browse == BrowseType.DIR) {
				DirectoryDialog dialog = new DirectoryDialog(shell);
				if (fpath != null)
					dialog.setFilterPath(fpath.getPath());
				result = dialog.open();
			} else if (browse == BrowseType.FILE && ((canon == CanonicalType.EXE) || (canon == CanonicalType.INSTALLER))
					&& (FileUtils.isArchive(rawFilterPath) || FileUtils.isIsoFile(rawFilterPath) || FileUtils.isFatImage(rawFilterPath))) {
				result = rawFilterPath;
			} else if (browse == BrowseType.FILE) {
				int style = (canon == CanonicalType.CDIMAGE) ? SWT.OPEN | SWT.MULTI: SWT.OPEN;
				if (save)
					style = SWT.SAVE;
				FileDialog dialog = new FileDialog(shell, style);
				if (fpath != null)
					dialog.setFilterPath(fpath.getPath());

				String[] filterNames = null;
				String[] filterExts = null;
				switch (canon) {
					case DOC:
						filterNames = new String[] {FileUtils.ALL_FILTER};
						filterExts = new String[] {FileUtils.ALL_FILTER};
						break;
					case EXE:
					case INSTALLER:
						boolean supportsPhysFS = ((combo != null) && (combo.isEnabled()));
						java.util.List<String> fNames = new ArrayList<String>();
						java.util.List<String> fExts = new ArrayList<String>();
						fNames.add(settings.msg("filetype.applicable"));
						fExts.add(FileUtils.EXE_FILTER + ";" + FileUtils.CDI_FILTER + ';' + FileUtils.FATI_FILTER + (supportsPhysFS ? ';' + FileUtils.ARC_FILTER: ""));
						Collections.addAll(fNames, settings.msg("filetype.exe"), settings.msg("filetype.cdimage"), settings.msg("filetype.floppyimage"), settings.msg("filetype.archive"),
							FileUtils.ALL_FILTER);
						Collections.addAll(fExts, FileUtils.EXE_FILTER, FileUtils.CDI_FILTER, FileUtils.FATI_FILTER, FileUtils.ARC_FILTER, FileUtils.ALL_FILTER);
						filterNames = fNames.toArray(new String[0]);
						filterExts = fExts.toArray(new String[0]);
						break;
					case DOSBOX: // only applicable on OSX
					case NATIVE_EXE:
						String defFilterTitleNative = settings.msg("filetype.native_exe");
						String defaultFilterNative = PlatformUtils.NATIVE_EXE_FILTER;
						filterNames = new String[] {defFilterTitleNative, FileUtils.ALL_FILTER};
						filterExts = new String[] {defaultFilterNative, FileUtils.ALL_FILTER};
						break;
					case ZIP:
						filterNames = new String[] {settings.msg("filetype.archive"), FileUtils.ALL_FILTER};
						filterExts = new String[] {FileUtils.ARC_FILTER, FileUtils.ALL_FILTER};
						break;
					case DBGLZIP:
						filterNames = new String[] {settings.msg("filetype.gamepack"), FileUtils.ALL_FILTER};
						filterExts = new String[] {FileUtils.DBGLZIP_FILTER, FileUtils.ALL_FILTER};
						break;
					case BOOTER:
						filterNames = new String[] {settings.msg("filetype.booterimage"), FileUtils.ALL_FILTER};
						filterExts = new String[] {FileUtils.BTR_FILTER, FileUtils.ALL_FILTER};
						break;
					case DFEND:
						filterNames = new String[] {settings.msg("filetype.dfendprofile")};
						filterExts = new String[] {PlatformUtils.DFEND_PROFILES};
						break;
					case CDIMAGE:
						filterNames = new String[] {settings.msg("filetype.applicable"), settings.msg("filetype.cdimage"), settings.msg("filetype.floppyimage"), FileUtils.ALL_FILTER};
						filterExts = new String[] {FileUtils.CDI_FILTER + ';' + FileUtils.FATI_FILTER, FileUtils.CDI_FILTER, FileUtils.FATI_FILTER, FileUtils.ALL_FILTER};
						break;
					case DOSBOXCONF:
						filterNames = new String[] {settings.msg("filetype.conf"), FileUtils.ALL_FILTER};
						filterExts = new String[] {FileUtils.CNF_FILTER, FileUtils.ALL_FILTER};
						break;
					default:
				}
				if (filterNames != null) {
					dialog.setFilterNames(filterNames);
				}
				if (filterExts != null) {
					dialog.setFilterExtensions(filterExts);
				}
				if (canon == CanonicalType.DFEND) {
					dialog.setFileName(PlatformUtils.DFEND_PROFILES);
				}
				result = dialog.open();
				if (canon == CanonicalType.CDIMAGE) {
					filterPath = new File(dialog.getFilterPath());
					filenames = dialog.getFileNames();
				}
			}
			if (result != null) {
				result = getResult(result, filterPath, filenames);
				if (result != null) {
					field.setText(result);
					if ((canon == CanonicalType.DOSBOX) && (alt != null)) {
						alt.setText(FileUtils.constructRelativeDBConfLocation(result).getPath());
					} else if ((canon == CanonicalType.CDIMAGE) && (combo != null)) {
						if (FileUtils.isIsoFile(filenames[0])) {
							combo.setText("iso");
						} else if (FileUtils.isFatImage(filenames[0])) {
							combo.setText("floppy");
						}
					} else if ((canon == CanonicalType.NATIVE_EXE) && (alt != null)) {
						String dir = FileUtils.makeRelativeToData((FileUtils.canonicalToData(result)).getParentFile()).getPath();
						if (dir != null)
							alt.setText(dir);
					}
				}
			}

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {}
			while (shell.getDisplay().readAndDispatch());
			shell.setEnabled(true);
		}
	}

	public BrowseButton(final Composite composite, final int style) {
		settings = Settings.getInstance();
		button = GeneralPurposeGUI.createIconButton(composite, style, settings, settings.msg("button.browse"), SWTImageManager.IMG_FOLDER);
	}

	public void connect(final Shell shell, final Text control, final Text altControl, final BrowseType browse, final CanonicalType canon, final boolean save, final Combo combo) {
		button.addSelectionListener(new Sa(shell, control, altControl, browse, canon, save, combo));
	}

	public void setLayoutData(final Object arg0) {
		button.setLayoutData(arg0);
	}

	public void setEnabled(final boolean enabled) {
		button.setEnabled(enabled);
	}
}
