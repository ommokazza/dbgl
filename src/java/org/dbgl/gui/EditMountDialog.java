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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.apache.commons.lang3.StringUtils;
import org.dbgl.exception.InvalidMountstringException;
import org.dbgl.gui.BrowseButton.BrowseType;
import org.dbgl.gui.BrowseButton.CanonicalType;
import org.dbgl.model.Mount;
import org.dbgl.model.conf.Settings;
import org.dbgl.util.StringRelatedUtils;
import swing2swt.layout.BorderLayout;


public class EditMountDialog extends Dialog {

	private Combo usecd;
	private Combo freesize;
	private Label mbLabel;
	private char defDriveletter = 'C';
	private Combo driveletter;
	private Button mountZipButton;
	private Button mountImageButton;
	private Button mountDirButton;
	private Text mount_dir;
	private Text mount_label;
	private Combo mount_type;
	private Combo lowlevelcd_type;
	private Text imgmount_image;
	private Combo imgmount_type;
	private Combo imgmount_fs;
	private Text imgmount_size;
	private Button zipmount_write_enable;
	private Text zipmount_write;
	private Text zipmount_zip;
	private Text zipmount_label;
	private Combo zipmount_type;
	private Settings settings;

	protected Object result;
	protected Shell shell;

	public EditMountDialog(final Shell parent) {
		super(parent, SWT.NONE);
	}

	public void setMount(final String mount) {
		this.result = mount;
	}

	public void setDefaultDriveletter(final char driveletter) {
		this.defDriveletter = driveletter;
	}

	public Object open() {
		settings = Settings.getInstance();
		createContents();
		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result;
	}

	protected void createContents() {
		shell = new Shell(getParent(), SWT.TITLE | SWT.CLOSE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setLayout(new BorderLayout(0, 0));
		shell.addControlListener(new SizeControlAdapter(shell, "mountdialog"));
		if (result == null) {
			shell.setText(settings.msg("dialog.mount.title.add"));
		} else {
			// meaning we are essentially editing an existing mount point
			shell.setText(settings.msg("dialog.mount.title.edit"));
		}

		final Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayout(new GridLayout(6, false));

		final Label driveLetterLabel = new Label(composite, SWT.NONE);
		driveLetterLabel.setText(settings.msg("dialog.mount.driveletter"));
		driveletter = new Combo(composite, SWT.READ_ONLY);
		driveletter.setItems(new String[] {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y"});
		driveletter.setLayoutData(new GridData());
		driveletter.select(defDriveletter - 'A');
		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);

		mountDirButton = new Button(composite, SWT.RADIO);
		mountDirButton.setText(settings.msg("dialog.mount.mountdir"));
		mount_dir = new Text(composite, SWT.BORDER);
		mount_dir.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		final BrowseButton browseButton = new BrowseButton(composite, SWT.NONE);
		browseButton.connect(shell, mount_dir, null, BrowseType.DIR, CanonicalType.DOSROOT, false, null);

		new Label(composite, SWT.NONE);
		final Label asLabel = new Label(composite, SWT.NONE);
		asLabel.setLayoutData(new GridData());
		asLabel.setText(settings.msg("dialog.mount.mountdiras"));
		mount_type = new Combo(composite, SWT.READ_ONLY);
		mount_type.setItems(settings.getSettings().getValues("profile", "mount_type"));
		mount_type.add("", 0);
		mount_type.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));

		mount_type.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				boolean enableLLItems = mount_type.getItem(mount_type.getSelectionIndex()).equalsIgnoreCase("cdrom");
				lowlevelcd_type.setEnabled(enableLLItems);
				usecd.setEnabled(enableLLItems);
				freesize.setEnabled(!enableLLItems);
				String sizeLabel = mount_type.getItem(mount_type.getSelectionIndex()).equalsIgnoreCase("floppy") ? settings.msg("dialog.mount.kb"): settings.msg("dialog.mount.mb");
				mbLabel.setText(sizeLabel);
				mbLabel.pack();
			}
		});

		new Label(composite, SWT.NONE);
		final Label label = new Label(composite, SWT.NONE);
		label.setLayoutData(new GridData());
		label.setText(settings.msg("dialog.mount.drivelabel"));
		mount_label = new Text(composite, SWT.BORDER);
		mount_label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		new Label(composite, SWT.NONE);

		new Label(composite, SWT.NONE);

		final Composite composite2 = new Composite(composite, SWT.NONE);
		composite2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 5, 1));
		final GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		gridLayout.numColumns = 4;
		composite2.setLayout(gridLayout);

		final Label lowlevelcdLabel = new Label(composite2, SWT.NONE);
		lowlevelcdLabel.setText(settings.msg("dialog.mount.lowlevelcdsupport"));
		lowlevelcd_type = new Combo(composite2, SWT.READ_ONLY);
		lowlevelcd_type.setVisibleItemCount(10);
		lowlevelcd_type.setItems(settings.getSettings().getValues("profile", "lowlevelcd_type"));
		lowlevelcd_type.add("", 0);
		final Label usecdLabel = new Label(composite2, SWT.NONE);
		usecdLabel.setText(settings.msg("dialog.mount.usecd"));
		usecd = new Combo(composite2, SWT.READ_ONLY);
		usecd.setItems(new String[] {"", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"});

		new Label(composite, SWT.NONE);
		final Label freesizeLabel = new Label(composite, SWT.NONE);
		freesizeLabel.setText(settings.msg("dialog.mount.freesize"));
		freesize = new Combo(composite, SWT.NONE);
		freesize.setItems(settings.getSettings().getValues("profile", "freesize"));
		freesize.setVisibleItemCount(10);
		freesize.add("", 0);
		freesize.setLayoutData(new GridData(70, SWT.DEFAULT));
		mbLabel = new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);

		final Label label_1 = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		label_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 6, 1));

		mountImageButton = new Button(composite, SWT.RADIO);
		mountImageButton.setText(settings.msg("dialog.mount.mountimages"));
		imgmount_image = new Text(composite, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.H_SCROLL);
		imgmount_image.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
		final BrowseButton imgBrowseButton = new BrowseButton(composite, SWT.NONE);

		new Label(composite, SWT.NONE);
		final Label imgmountAsLabel = new Label(composite, SWT.NONE);
		imgmountAsLabel.setLayoutData(new GridData());
		imgmountAsLabel.setText(settings.msg("dialog.mount.mountdiras"));

		final Composite composite3 = new Composite(composite, SWT.NONE);
		composite3.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		final GridLayout gridLayout3 = new GridLayout();
		gridLayout3.marginHeight = 0;
		gridLayout3.marginWidth = 0;
		gridLayout3.numColumns = 3;
		composite3.setLayout(gridLayout3);

		imgmount_type = new Combo(composite3, SWT.READ_ONLY);
		imgmount_type.setItems(settings.getSettings().getValues("profile", "imgmount_type"));
		imgmount_type.setText("iso");
		imgBrowseButton.connect(shell, imgmount_image, null, BrowseType.FILE, CanonicalType.CDIMAGE, false, imgmount_type);

		final Label imgmountFsLabel = new Label(composite3, SWT.NONE);
		imgmountFsLabel.setText(settings.msg("dialog.mount.imgmountfs"));
		imgmount_fs = new Combo(composite3, SWT.READ_ONLY);
		imgmount_fs.setItems(settings.getSettings().getValues("profile", "imgmount_fs"));
		imgmount_fs.add("", 0);

		new Label(composite, SWT.NONE);
		final Label imgmountSizeLabel = new Label(composite, SWT.NONE);
		imgmountSizeLabel.setLayoutData(new GridData());
		imgmountSizeLabel.setText(settings.msg("dialog.mount.imgmountsize"));

		final Composite imgSizeSettings = new Composite(composite, SWT.NONE);
		imgSizeSettings.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		GridLayout filler = new GridLayout();
		filler.numColumns = 2;
		filler.horizontalSpacing = 2;
		filler.marginWidth = 0;
		imgSizeSettings.setLayout(filler);

		imgmount_size = new Text(imgSizeSettings, SWT.BORDER);
		imgmount_size.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		final Button img_size_config = new Button(imgSizeSettings, SWT.NONE);
		img_size_config.setText("...");
		img_size_config.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				EditImgSizeDialog dialog = new EditImgSizeDialog(shell);
				dialog.setImgMountSizeCommand(imgmount_size.getText());
				String command = (String)dialog.open();
				if (command != null) {
					imgmount_size.setText(command);
				}
			}
		});

		new Label(composite, SWT.NONE);

		final SelectionAdapter driveLetterSelectionAdapter = new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				boolean imgFsNone = imgmount_fs.getSelectionIndex() == -1 ? false: imgmount_fs.getItem(imgmount_fs.getSelectionIndex()).equalsIgnoreCase("none");
				int sel = driveletter.getSelectionIndex();
				if (mountImageButton.getSelection() && imgFsNone) {
					if (!StringUtils.isNumeric(driveletter.getItem(sel))) {
						driveletter.setItems(new String[] {"0", "1", "2", "3"});
						driveletter.setText(driveletter.getItem(Math.min(sel, 3)));
					}
				} else {
					if (StringUtils.isNumeric(driveletter.getItem(sel))) {
						driveletter.setItems(new String[] {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y"});
						driveletter.setText(driveletter.getItem(Math.min(sel, 3)));
					}
				}
			}
		};
		imgmount_fs.addSelectionListener(driveLetterSelectionAdapter);
		imgmount_fs.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				boolean imgFsNone = imgmount_fs.getItem(imgmount_fs.getSelectionIndex()).equalsIgnoreCase("none");
				imgmount_size.setEnabled(imgFsNone);
				img_size_config.setEnabled(imgFsNone);
			}
		});

		final Label label_2 = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		label_2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 6, 1));

		mountZipButton = new Button(composite, SWT.RADIO);
		mountZipButton.setText(settings.msg("dialog.mount.mountzip"));
		final Label zipLabel = new Label(composite, SWT.NONE);
		zipLabel.setLayoutData(new GridData());
		zipLabel.setText(settings.msg("dialog.mount.zipfile"));
		zipmount_zip = new Text(composite, SWT.BORDER);
		zipmount_zip.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		final BrowseButton zipBrowseButton = new BrowseButton(composite, SWT.NONE);
		zipBrowseButton.connect(shell, zipmount_zip, null, BrowseType.FILE, CanonicalType.ZIP, false, null);

		zipmount_write_enable = new Button(composite, SWT.CHECK);
		zipmount_write_enable.setSelection(true);
		zipmount_write_enable.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));
		final Label writeLabel = new Label(composite, SWT.NONE);
		writeLabel.setLayoutData(new GridData());
		writeLabel.setText(settings.msg("dialog.mount.writedirectory"));
		zipmount_write = new Text(composite, SWT.BORDER);
		zipmount_write.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		final BrowseButton writeBrowseButton = new BrowseButton(composite, SWT.NONE);
		writeBrowseButton.connect(shell, zipmount_write, null, BrowseType.DIR, CanonicalType.DOSROOT, false, null);

		zipmount_write_enable.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				zipmount_write.setEnabled(zipmount_write_enable.getSelection());
			}
		});
		zipmount_write.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent event) {
				zipmount_write_enable.setSelection(true);
				zipmount_write.setEnabled(true);
			}
		});

		new Label(composite, SWT.NONE);
		final Label asLabel_1 = new Label(composite, SWT.NONE);
		asLabel_1.setLayoutData(new GridData());
		asLabel_1.setText(settings.msg("dialog.mount.mountzipas"));
		zipmount_type = new Combo(composite, SWT.READ_ONLY);
		zipmount_type.setItems(settings.getSettings().getValues("profile", "zipmount_type"));
		zipmount_type.add("", 0);
		zipmount_type.setLayoutData(new GridData());
		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);

		new Label(composite, SWT.NONE);
		final Label label_3 = new Label(composite, SWT.NONE);
		label_3.setLayoutData(new GridData());
		label_3.setText(settings.msg("dialog.mount.drivelabel"));
		zipmount_label = new Text(composite, SWT.BORDER);
		zipmount_label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		new Label(composite, SWT.NONE);

		mountDirButton.addSelectionListener(driveLetterSelectionAdapter);
		mountImageButton.addSelectionListener(driveLetterSelectionAdapter);
		mountZipButton.addSelectionListener(driveLetterSelectionAdapter);

		final Composite composite_1 = new Composite(shell, SWT.NONE);
		composite_1.setLayout(new RowLayout());
		composite_1.setLayoutData(BorderLayout.SOUTH);

		final Button okButton = new Button(composite_1, SWT.NONE);
		shell.setDefaultButton(okButton);
		okButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				if (!isValid()) {
					return;
				}
				Mount mount = null;
				if (mountDirButton.getSelection()) {
					mount = new Mount(Mount.MountingType.DIR, mount_type.getText(), driveletter.getText(), new String[] {mount_dir.getText()}, mount_label.getText(),
							lowlevelcd_type.isEnabled() ? lowlevelcd_type.getText(): "", usecd.isEnabled() ? usecd.getText(): "", "", freesize.isEnabled() ? freesize.getText(): "", "", "");
				} else if (mountImageButton.getSelection()) {
					mount = new Mount(Mount.MountingType.IMAGE, imgmount_type.getText(), driveletter.getText(),
							StringRelatedUtils.textAreaToStringArray(imgmount_image.getText(), imgmount_image.getLineDelimiter()), "", "", "", "", "", imgmount_fs.getText(),
							imgmount_size.isEnabled() ? imgmount_size.getText(): "");
				} else if (mountZipButton.getSelection()) {
					mount = new Mount(Mount.MountingType.PHYSFS, zipmount_type.getText(), driveletter.getText(), new String[] {zipmount_zip.getText()}, zipmount_label.getText(), "", "",
							zipmount_write_enable.getSelection() ? zipmount_write.getText(): null, "", "", "");
				}
				if (mount != null) {
					result = mount.toString();
				}
				shell.close();
			}
		});
		okButton.setText(settings.msg("button.ok"));

		final Button cancelButton = new Button(composite_1, SWT.NONE);
		cancelButton.setText(settings.msg("button.cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				result = null;
				shell.close();
			}
		});

		final RowData rowData = new RowData();
		rowData.width = GeneralPurposeGUI.getWidth(cancelButton, okButton);
		okButton.setLayoutData(rowData);
		cancelButton.setLayoutData(rowData);

		ModifyListener modListener = new ModifyListener() {
			public void modifyText(final ModifyEvent event) {
				mountDirButton.setSelection(event.widget == mount_dir);
				mountImageButton.setSelection(event.widget == imgmount_image);
				mountZipButton.setSelection(event.widget == zipmount_zip);
				mountDirButton.notifyListeners(SWT.Selection, null);
			}
		};

		mount_dir.addModifyListener(modListener);
		imgmount_image.addModifyListener(modListener);
		zipmount_zip.addModifyListener(modListener);

		Mount mount = null;
		if (result != null) {
			try {
				mount = new Mount((String)result);
			} catch (InvalidMountstringException e1) {
				// if the mount could not be instantiated, just take the default
				// values for a mount by pretending there was no string input
				result = null;
			}
		}

		if (result == null) {
			// new mount point, set default values
			mountDirButton.setSelection(true);
			mount_dir.setText(".");
			mount_dir.selectAll();
			mount_dir.setFocus();
		} else {
			// meaning we are essentially editing an existing mount point
			// so we need to set previous values
			if (mount.getFs().equalsIgnoreCase("none")) {
				driveletter.setItems(new String[] {"0", "1", "2", "3"});
			}
			driveletter.setText(mount.getDriveletterString());
			switch (mount.getMountingType()) {
				case DIR:
					mountDirButton.setSelection(true);
					mount_dir.setText(mount.getPathAsString());
					if (!mount.getMountAs().equals("")) {
						mount_type.setText(mount.getMountAs());
					}
					mount_label.setText(mount.getLabel());
					lowlevelcd_type.setText(mount.getLowlevelCD());
					usecd.setText(mount.getUseCD());
					if (!mount.getFreesize().equals("")) {
						freesize.setText(mount.getFreesize());
					}
					mount_dir.selectAll();
					mount_dir.setFocus();
					break;
				case PHYSFS:
					mountZipButton.setSelection(true);
					zipmount_zip.setText(mount.getPathAsString());
					boolean writeable = (mount.getWrite() != null);
					zipmount_write_enable.setSelection(writeable);
					zipmount_write.setEnabled(writeable);
					if (writeable)
						zipmount_write.setText(mount.getWrite().getPath());
					if (!mount.getMountAs().equals("")) {
						zipmount_type.setText(mount.getMountAs());
					}
					zipmount_label.setText(mount.getLabel());
					zipmount_zip.selectAll();
					zipmount_zip.setFocus();
					break;
				case IMAGE:
					mountImageButton.setSelection(true);
					if (!mount.getMountAs().equals("")) {
						imgmount_type.setText(mount.getMountAs());
					}
					if (!mount.getFs().equals("")) {
						imgmount_fs.setText(mount.getFs());
					}
					if (!mount.getSize().equals("")) {
						imgmount_size.setText(mount.getSize());
					}
					imgmount_image.setText(mount.getImgMountAsString(imgmount_image.getLineDelimiter()));
					imgmount_image.selectAll();
					imgmount_image.setFocus();
				default:
			}
		}
		int idx = mount_type.getSelectionIndex();
		boolean enableLLItems = idx == -1 ? false: mount_type.getItem(idx).equalsIgnoreCase("cdrom");
		lowlevelcd_type.setEnabled(enableLLItems);
		usecd.setEnabled(enableLLItems);
		freesize.setEnabled(!enableLLItems);
		String sizeLabel = idx == -1 ? settings.msg("dialog.mount.mb"): (mount_type.getItem(idx).equalsIgnoreCase("floppy") ? settings.msg("dialog.mount.kb"): settings.msg("dialog.mount.mb"));
		mbLabel.setText(sizeLabel);
		idx = imgmount_fs.getSelectionIndex();
		boolean imgFsNone = idx == -1 ? false: imgmount_fs.getItem(idx).equalsIgnoreCase("none");
		imgmount_size.setEnabled(imgFsNone);
		img_size_config.setEnabled(imgFsNone);
	}

	private boolean isValid() {
		GeneralPurposeDialogs.initErrorDialog();
		if (mountDirButton.getSelection() && mount_dir.getText().equals("")) {
			GeneralPurposeDialogs.addError(settings.msg("dialog.mount.required.path"), mount_dir);
		} else if (mountImageButton.getSelection()) {
			if (imgmount_image.getText().equals(""))
				GeneralPurposeDialogs.addError(settings.msg("dialog.mount.required.image"), imgmount_image);
			if (imgmount_fs.getText().equalsIgnoreCase("none") && imgmount_size.getText().equals(""))
				GeneralPurposeDialogs.addError(settings.msg("dialog.mount.required.imgsize"), imgmount_size);
		} else if (mountZipButton.getSelection() && zipmount_zip.getText().equals("")) {
			GeneralPurposeDialogs.addError(settings.msg("dialog.mount.required.zip"), zipmount_zip);
		}
		return !GeneralPurposeDialogs.displayErrorDialog(shell);
	}
}
