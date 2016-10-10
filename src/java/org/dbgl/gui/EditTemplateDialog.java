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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.dbgl.db.Database;
import org.dbgl.interfaces.Configurable;
import org.dbgl.model.DosboxVersion;
import org.dbgl.model.ExpTemplate;
import org.dbgl.model.Mount;
import org.dbgl.model.NativeCommand;
import org.dbgl.model.Template;
import org.dbgl.model.conf.Autoexec;
import org.dbgl.model.conf.Conf;
import org.dbgl.model.conf.SectionsWrapper;
import org.dbgl.model.conf.Settings;
import org.dbgl.util.FileUtils;
import org.dbgl.util.PlatformUtils;
import org.dbgl.util.StringRelatedUtils;
import swing2swt.layout.BorderLayout;


public class EditTemplateDialog extends Dialog {

	protected Text title;
	protected Button defaultButton;
	protected Combo dbversion;
	protected Button setButton, switchButton, reloadButton;
	protected Combo priority_active, priority_inactive;
	protected Button waitonerror, exit;
	protected Text language, captures, config_file;
	protected Combo output, frameskip;
	protected Combo scaler, pixelshader;
	protected Button fulldouble, fullscreen;
	protected Combo fullresolution, windowresolution;
	protected Button aspect;
	protected Combo overscan;
	protected Combo videoram;
	protected Combo vsyncmode;
	protected Text vsyncrate, forcerate;
	protected Button scaler_forced, linewise, char9, multiscan, autofit, cgasnow, splash3dfx;
	protected Text glideport, memsizeKB;
	protected Combo glide, lfbGlide;
	protected Combo dacMT32, reverbmodeMT32, reverbtimeMT32, reverblevelMT32;
	protected Combo machine, cpu_type, core;
	protected Combo cycles, cycles_up, cycles_down;
	protected Combo memsize;
	protected Button xms;
	protected Combo ems, umb, memalias;
	protected Button nosound;
	protected Combo rate, blocksize, prebuffer, mpu401, midi_device;
	protected Text midi_config;
	protected Text mixer_config;
	protected Combo sbtype, oplrate, oplmode, oplemu, sbbase, irq, dma, hdma, hardwareaddresssbbase;
	protected Button sbmixer, goldplay;
	protected Button gus, ps1, innova;
	protected Combo gusrate, gusbase, gusdma1, gusdma2, gusirq1, gusirq2;
	protected Combo ps1rate, innovarate, innovabase, innovaquality;
	protected Text ultradir;
	protected Button pcspeaker;
	protected Combo pcrate;
	protected Combo tandy, tandyrate;
	protected Button disney, swapstereo, swapstereoMT32, loggingMT32, multithreadMT32;
	protected Button int33, biosps2, aux, isapnpbios, ide1, ide2, ide3, ide4, automount;
	protected Spinner partialsMT32, files, printerdpi, printerwidth, printerheight, printertimeout;
	protected Button usescancodes, dongle, ne2000;
	protected Text mapperfile;
	protected Combo keyboard_layout, auxdevice, printeroutput;
	protected Text keyb, printerdocpath, ne2000base, ne2000irq, ne2000macaddress, ne2000realnic;
	protected Button autolock;
	protected Combo sensitivity;
	protected Combo joysticktype;
	protected Button timed, autofire, swap34, buttonwrap;
	protected Text serial1, serial2, serial3, serial4, parallel1, parallel2, parallel3;
	protected Button ipx, printer, printermultipage;
	protected Text ipxnet;
	protected Text[] customCommands;
	protected List mountingpoints;
	protected ExpandItem booterExpandItem, dosExpandItem;
	protected Group memoryGroup, associationGroup, miscGroup, executeGroup;
	protected TabItem infoTabItem, generalTabItem, mountingTabItem;
	protected Button okButton, cancelButton;

	protected int dbversionIndex = -1;

	protected Shell shell;
	protected java.util.List<DosboxVersion> dbversionsList;
	protected Database dbase;
	protected java.util.List<Configurable> multiProfileList = new ArrayList<Configurable>();
	protected List nativeCommands;
	protected Settings settings;
	protected Object result;

	protected enum DosboxConfAction {
		SET, SWITCH, RELOAD, RELOAD_TEMPLATE
	};

	public EditTemplateDialog(final Shell parent) {
		super(parent, SWT.NONE);
	}

	public void setTemplate(final Template template) {
		this.result = template;
	}

	public Object open() {
		settings = Settings.getInstance();
		dbase = Database.getInstance();
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
		return result;
	}

	protected boolean init() {
		try {
			dbversionsList = dbase.readDosboxVersionsList();
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(bos);
			if (result == null) {
				dbversionIndex = DosboxVersion.indexOfDefault(dbversionsList);
				java.util.List<NativeCommand> nativeCommands = dbase.readNativeCommandsList(-1, -1);
				multiProfileList.add(new ExpTemplate(new Conf((Template)null, dbversionsList.get(dbversionIndex), ps), nativeCommands));
			} else {
				dbversionIndex = DosboxVersion.findIndexById(dbversionsList, ((Template)result).getDbversionId());
				java.util.List<NativeCommand> nativeCommands = dbase.readNativeCommandsList(-1, ((Template)result).getId());
				multiProfileList.add(new ExpTemplate(new Conf((Template)result, dbversionsList.get(dbversionIndex), ps), nativeCommands));
			}
			if (bos.size() > 0) {
				GeneralPurposeDialogs.warningMessage(getParent(), bos.toString());
				bos.reset();
			}
			return true;
		} catch (Exception e) {
			GeneralPurposeDialogs.warningMessage(getParent(), e);
			return false;
		}
	}

	protected void createContents() {
		shell = new Shell(getParent(), SWT.TITLE | SWT.CLOSE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setLayout(new BorderLayout(0, 0));
		shell.addControlListener(new SizeControlAdapter(shell, "templatedialog"));
		if (result == null) {
			shell.setText(settings.msg("dialog.template.title.add"));
		} else {
			// meaning we are essentially editing an existing template
			shell.setText(settings.msg("dialog.template.title.edit", new Object[] {((Template)result).getTitle(), ((Template)result).getId()}));
		}

		final TabFolder tabFolder = new TabFolder(shell, SWT.NONE);
		createInfoTab(tabFolder);
		createGeneralTab(tabFolder);
		createDisplayTab(tabFolder);
		createMachineTab(tabFolder);
		createAudioTab(tabFolder);
		createIOTab(tabFolder);
		createCustomCommandsTab(tabFolder);
		createMountingTab(tabFolder);

		createOkCancelButtons();
		okButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				if (!isValid()) {
					return;
				}
				try {
					Conf conf = multiProfileList.get(0).getConf();
					result = dbase.addOrEditTemplate(title.getText(), conf.getDbversion().getId(), defaultButton.getSelection(), result == null ? -1: ((Template)result).getId());
					dbase.saveNativeCommands(multiProfileList.get(0).getNativeCommandsList(), -1, ((Template)result).getId());
					updateConfigurationBySettings(conf);
					conf.injectOrUpdateTemplate((Template)result);
					conf.save();
				} catch (Exception e) {
					GeneralPurposeDialogs.warningMessage(shell, e);
				}
				shell.close();
			}
		});

		// init values
		for (DosboxVersion dbv: dbversionsList) {
			dbversion.add(dbv.getTitle());
		}
		dbversion.select(dbversionIndex);

		if (result == null) {
			title.setFocus();
		} else {
			// meaning we are essentially editing an existing template
			// so we need to set previous values
			title.setText(((Template)result).getTitle());
			defaultButton.setSelection(((Template)result).isDefault());
		}

		enableSettingsByConfiguration(multiProfileList.get(0).getConf().getDosboxSettings());
		selectSettingsByConfiguration(multiProfileList.get(0).getConf());
	}

	protected void createOkCancelButtons() {
		final Composite composite_7 = new Composite(shell, SWT.NONE);
		composite_7.setLayout(new GridLayout(2, true));
		composite_7.setLayoutData(BorderLayout.SOUTH);

		okButton = new Button(composite_7, SWT.NONE);
		shell.setDefaultButton(okButton);
		okButton.setText(settings.msg("button.ok"));

		cancelButton = new Button(composite_7, SWT.NONE);
		cancelButton.setText(settings.msg("button.cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				if (cancelButton.getText().equals(settings.msg("button.cancel"))) {
					result = null;
				}
				shell.close();
			}
		});

		final GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.widthHint = GeneralPurposeGUI.getWidth(okButton, cancelButton);
		okButton.setLayoutData(gridData);
		cancelButton.setLayoutData(gridData);
	}

	protected void createMountingTab(final TabFolder tabFolder) {
		mountingTabItem = new TabItem(tabFolder, SWT.NONE);
		mountingTabItem.setText(settings.msg("dialog.template.tab.mounting"));

		final Composite composite = new Composite(tabFolder, SWT.NONE);
		composite.setLayout(new GridLayout());
		mountingTabItem.setControl(composite);

		createMountingGroup(composite);
		createExecuteGroup(composite);
	}

	protected void createExecuteGroup(final Composite composite) {
		executeGroup = new Group(composite, SWT.NONE);
		executeGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		executeGroup.setText(settings.msg("dialog.template.execute"));
		executeGroup.setLayout(new FillLayout());

		final ExpandBar bar = new ExpandBar(executeGroup, SWT.V_SCROLL);

		Composite booterComposite = new Composite(bar, SWT.NONE);
		Composite dosComposite = new Composite(bar, SWT.NONE);

		createBooterComposite(booterComposite);
		createDosComposite(dosComposite);

		booterExpandItem = new ExpandItem(bar, SWT.NONE, 0);
		booterExpandItem.setText(settings.msg("dialog.template.booter"));
		booterExpandItem.setHeight(booterComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
		booterExpandItem.setControl(booterComposite);

		dosExpandItem = new ExpandItem(bar, SWT.NONE, 1);
		dosExpandItem.setText(settings.msg("dialog.template.dos"));
		dosExpandItem.setHeight(dosComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
		dosExpandItem.setControl(dosComposite);

		bar.addExpandListener(new ExpandAdapter() {
			public void itemCollapsed(final ExpandEvent e) {
				bar.getItem((((ExpandItem)e.item).getText().equals(settings.msg("dialog.template.dos"))) ? 0: 1).setExpanded(true);
				Display.getCurrent().asyncExec(new Runnable() {
					public void run() {
						composite.layout();
					}
				});
			}

			public void itemExpanded(final ExpandEvent e) {
				bar.getItem((((ExpandItem)e.item).getText().equals(settings.msg("dialog.template.dos"))) ? 0: 1).setExpanded(false);
				Display.getCurrent().asyncExec(new Runnable() {
					public void run() {
						composite.layout();
					}
				});
			}
		});

		if (isMultiEdit()) {
			bar.setEnabled(false);
		}
	}

	protected void createBooterComposite(Composite booterComposite) {
		booterComposite.setLayout(new GridLayout());
		final Label image1Label = new Label(booterComposite, SWT.NONE);
		image1Label.setText(settings.msg("dialog.profile.booterimage1"));
	}

	protected void createDosComposite(Composite dosComposite) {
		dosComposite.setLayout(new GridLayout());
		final Label mainExeLabel = new Label(dosComposite, SWT.NONE);
		mainExeLabel.setText(settings.msg("dialog.profile.mainexe"));
	}

	protected void createMountingGroup(final Composite composite) {
		final Group mountGroup = new Group(composite, SWT.NONE);
		mountGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		mountGroup.setText(settings.msg("dialog.template.mountingoverview"));
		mountGroup.setLayout(new GridLayout(2, false));

		mountingpoints = new List(mountGroup, SWT.V_SCROLL | SWT.BORDER);
		mountingpoints.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 3));
		mountingpoints.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(final MouseEvent event) {
				if (mountingpoints.getSelectionIndex() == -1) {
					doAddMount();
				} else {
					doEditMount();
				}
			}
		});

		final Button addButton = new Button(mountGroup, SWT.NONE);
		addButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		addButton.setText(settings.msg("dialog.template.mount.add"));
		addButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				doAddMount();
			}
		});

		final Button editButton = new Button(mountGroup, SWT.NONE);
		editButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		editButton.setText(settings.msg("dialog.template.mount.edit"));
		editButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				doEditMount();
			}
		});

		final Button removeButton = new Button(mountGroup, SWT.NONE);
		removeButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		removeButton.setText(settings.msg("dialog.template.mount.remove"));
		removeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				doRemoveMount();
			}
		});

		if (isMultiEdit()) {
			mountingpoints.setEnabled(false);
			addButton.setEnabled(false);
			editButton.setEnabled(false);
			removeButton.setEnabled(false);
		}
	}

	protected void createCustomCommandsTab(final TabFolder tabFolder) {
		final TabItem customCommandsTabItem = new TabItem(tabFolder, SWT.NONE);
		customCommandsTabItem.setText(settings.msg("dialog.template.tab.customcommands"));

		Composite compositeHoldingSubTabs = new Composite(tabFolder, SWT.NONE);
		compositeHoldingSubTabs.setLayout(new FillLayout());
		customCommandsTabItem.setControl(compositeHoldingSubTabs);
		final TabFolder subTabFolder = new TabFolder(compositeHoldingSubTabs, SWT.NONE);

		final TabItem dosboxTabItem = new TabItem(subTabFolder, SWT.NONE);
		dosboxTabItem.setText(settings.msg("dialog.template.tab.dosboxautoexec"));

		final Composite dosboxComposite = new Composite(subTabFolder, SWT.NONE);
		dosboxComposite.setLayout(new GridLayout(2, false));
		dosboxTabItem.setControl(dosboxComposite);

		customCommands = new Text[Autoexec.SECTIONS];
		for (int i = 0; i < Autoexec.SECTIONS; i++) {
			Label filterLabel = new Label(dosboxComposite, SWT.NONE);
			filterLabel.setText(settings.msg("dialog.template.customcommand" + (i + 1)));
			customCommands[i] = new Text(dosboxComposite, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.H_SCROLL);
			customCommands[i].setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		}

		final TabItem nativeTabItem = new TabItem(subTabFolder, SWT.NONE);
		nativeTabItem.setText(settings.msg("dialog.template.tab.native"));

		final Composite nativeComposite = new Composite(subTabFolder, SWT.NONE);
		nativeComposite.setLayout(new GridLayout(2, false));
		nativeTabItem.setControl(nativeComposite);

		nativeCommands = new List(nativeComposite, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		nativeCommands.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 6));
		nativeCommands.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(final MouseEvent event) {
				if (nativeCommands.getSelectionIndex() == -1) {
					doAddNativeCommand();
				} else {
					doEditNativeCommand();
				}
			}
		});

		final Button addButton = new Button(nativeComposite, SWT.NONE);
		addButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		addButton.setText(settings.msg("dialog.template.mount.add"));
		addButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				doAddNativeCommand();
			}
		});

		final Button editButton = new Button(nativeComposite, SWT.NONE);
		editButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		editButton.setText(settings.msg("dialog.template.mount.edit"));
		editButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				doEditNativeCommand();
			}
		});

		final Button removeButton = new Button(nativeComposite, SWT.NONE);
		removeButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		removeButton.setText(settings.msg("dialog.template.mount.remove"));
		removeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				doRemoveNativeCommand();
			}
		});

		Button arrowUpButton = new Button(nativeComposite, SWT.ARROW | SWT.UP);
		arrowUpButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		arrowUpButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				int sel = nativeCommands.getSelectionIndex();
				if (sel > 0) {
					Collections.swap(multiProfileList.get(0).getNativeCommandsList(), sel, sel - 1);
					updateNativeCommands(sel - 1);
				}
			}
		});

		Button arrowDownButton = new Button(nativeComposite, SWT.ARROW | SWT.DOWN);
		arrowDownButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		arrowDownButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				int sel = nativeCommands.getSelectionIndex();
				if (sel >= 0 && sel < nativeCommands.getItemCount() - 1) {
					Collections.swap(multiProfileList.get(0).getNativeCommandsList(), sel, sel + 1);
					updateNativeCommands(sel + 1);
				}
			}
		});

		new Label(nativeComposite, SWT.NONE);

		if (isMultiEdit()) {
			nativeCommands.setEnabled(false);
			addButton.setEnabled(false);
			editButton.setEnabled(false);
			removeButton.setEnabled(false);
			arrowUpButton.setEnabled(false);
			arrowDownButton.setEnabled(false);
		} else {
			updateNativeCommands(-1);
		}
	}

	protected void updateNativeCommands(int sel) {
		nativeCommands.removeAll();
		for (NativeCommand cmd: multiProfileList.get(0).getNativeCommandsList())
			nativeCommands.add(cmd.toString());
		nativeCommands.select(sel);
	}

	protected void createIOTab(final TabFolder tabFolder) {
		final TabFolder subTabFolder = createRelExpTabs(tabFolder, "dialog.template.tab.io", 3, 3);
		final Composite composite_ro = (Composite)subTabFolder.getChildren()[0];
		final Composite composite_xo = (Composite)subTabFolder.getChildren()[1];

		final Group mouseGroup = new Group(composite_ro, SWT.NONE);
		mouseGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		mouseGroup.setText(settings.msg("dialog.template.mouse"));
		mouseGroup.setLayout(new GridLayout(2, false));

		final Label autolockLabel = new Label(mouseGroup, SWT.NONE);
		autolockLabel.setText(settings.msg("dialog.template.autolock"));
		autolock = new Button(mouseGroup, SWT.CHECK);
		autolock.setToolTipText(settings.msg("dialog.template.autolock.tooltip"));

		final Label sensitivityLabel = new Label(mouseGroup, SWT.NONE);
		sensitivityLabel.setText(settings.msg("dialog.template.sensitivity"));
		sensitivity = new Combo(mouseGroup, SWT.READ_ONLY);
		sensitivity.setItems(settings.getSettings().getValues("profile", "sensitivity"));
		sensitivity.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		sensitivity.setVisibleItemCount(20);
		sensitivity.setToolTipText(settings.msg("dialog.template.sensitivity.tooltip"));

		final Group keyboardGroup = new Group(composite_ro, SWT.NONE);
		keyboardGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		keyboardGroup.setText(settings.msg("dialog.template.keyboard"));
		keyboardGroup.setLayout(new GridLayout(2, false));

		final Label useScancodesLabel = new Label(keyboardGroup, SWT.NONE);
		useScancodesLabel.setText(settings.msg("dialog.template.usescancodes"));
		usescancodes = new Button(keyboardGroup, SWT.CHECK);
		usescancodes.setToolTipText(settings.msg("dialog.template.usescancodes.tooltip"));

		final Label mapperFileLabel = new Label(keyboardGroup, SWT.NONE);
		mapperFileLabel.setText(settings.msg("dialog.template.mapperfile"));
		mapperfile = new Text(keyboardGroup, SWT.BORDER);
		mapperfile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		mapperfile.setToolTipText(settings.msg("dialog.template.mapperfile.tooltip"));

		final Label layoutLabel = new Label(keyboardGroup, SWT.NONE);
		layoutLabel.setText(settings.msg("dialog.template.keyboardlayout"));
		keyboard_layout = new Combo(keyboardGroup, SWT.NONE);
		keyboard_layout.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		keyboard_layout.setVisibleItemCount(15);
		keyboard_layout.setItems(settings.getSettings().getValues("profile", "keyboardlayout"));
		keyboard_layout.setToolTipText(settings.msg("dialog.template.keyboardlayout.tooltip"));

		final Label keybLabel = new Label(keyboardGroup, SWT.NONE);
		keybLabel.setText(settings.msg("dialog.template.keybcommand"));
		keyb = new Text(keyboardGroup, SWT.BORDER);
		keyb.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		final Group joystickGroup = new Group(composite_ro, SWT.NONE);
		joystickGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		joystickGroup.setText(settings.msg("dialog.template.joystick"));
		joystickGroup.setLayout(new GridLayout(2, false));

		final Label typeLabel_1 = new Label(joystickGroup, SWT.NONE);
		typeLabel_1.setText(settings.msg("dialog.template.joysticktype"));
		joysticktype = new Combo(joystickGroup, SWT.READ_ONLY);
		joysticktype.setItems(settings.getSettings().getValues("profile", "joysticktype"));
		joysticktype.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		joysticktype.setToolTipText(settings.msg("dialog.template.joysticktype.tooltip"));

		final Label timedLabel = new Label(joystickGroup, SWT.NONE);
		timedLabel.setText(settings.msg("dialog.template.timedemulation"));
		timed = new Button(joystickGroup, SWT.CHECK);
		timed.setToolTipText(settings.msg("dialog.template.timedemulation.tooltip"));

		final Label autofireLabel = new Label(joystickGroup, SWT.NONE);
		autofireLabel.setText(settings.msg("dialog.template.autofire"));
		autofire = new Button(joystickGroup, SWT.CHECK);
		autofire.setToolTipText(settings.msg("dialog.template.autofire.tooltip"));

		final Label swapAxes3Label = new Label(joystickGroup, SWT.NONE);
		swapAxes3Label.setText(settings.msg("dialog.template.swap34"));
		swap34 = new Button(joystickGroup, SWT.CHECK);
		swap34.setToolTipText(settings.msg("dialog.template.swap34.tooltip"));

		final Label buttonWrapLabel = new Label(joystickGroup, SWT.NONE);
		buttonWrapLabel.setText(settings.msg("dialog.template.buttonwrapping"));
		buttonwrap = new Button(joystickGroup, SWT.CHECK);
		buttonwrap.setToolTipText(settings.msg("dialog.template.buttonwrapping.tooltip"));

		final Group modemGroup = new Group(composite_ro, SWT.NONE);
		modemGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		modemGroup.setText(settings.msg("dialog.template.modem"));
		modemGroup.setLayout(new GridLayout(2, false));

		final Label serial1Label = new Label(modemGroup, SWT.NONE);
		serial1Label.setText(settings.msg("dialog.template.serial1"));
		serial1 = new Text(modemGroup, SWT.BORDER);
		serial1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		serial1.setToolTipText(settings.msg("dialog.template.serial.tooltip"));

		final Label serial2Label = new Label(modemGroup, SWT.NONE);
		serial2Label.setText(settings.msg("dialog.template.serial2"));
		serial2 = new Text(modemGroup, SWT.BORDER);
		serial2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		serial2.setToolTipText(settings.msg("dialog.template.serial.tooltip"));

		final Label serial3Label = new Label(modemGroup, SWT.NONE);
		serial3Label.setText(settings.msg("dialog.template.serial3"));
		serial3 = new Text(modemGroup, SWT.BORDER);
		serial3.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		serial3.setToolTipText(settings.msg("dialog.template.serial.tooltip"));

		final Label serial4Label = new Label(modemGroup, SWT.NONE);
		serial4Label.setText(settings.msg("dialog.template.serial4"));
		serial4 = new Text(modemGroup, SWT.BORDER);
		serial4.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		serial4.setToolTipText(settings.msg("dialog.template.serial.tooltip"));

		final Group networkGroup = new Group(composite_ro, SWT.NONE);
		networkGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		networkGroup.setText(settings.msg("dialog.template.network"));
		networkGroup.setLayout(new GridLayout(2, false));

		final Label ipxLabel = new Label(networkGroup, SWT.NONE);
		ipxLabel.setText(settings.msg("dialog.template.enableipx"));
		ipx = new Button(networkGroup, SWT.CHECK);
		ipx.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				ipxnet.setEnabled(ipx.getSelection());
			}
		});
		ipx.setToolTipText(settings.msg("dialog.template.enableipx.tooltip"));

		final Label ipxnetCommandLabel = new Label(networkGroup, SWT.NONE);
		ipxnetCommandLabel.setText(settings.msg("dialog.template.ipxnetcommand"));
		ipxnetCommandLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));

		ipxnet = new Text(networkGroup, SWT.BORDER);
		ipxnet.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		final Group mouseExpGroup = new Group(composite_xo, SWT.NONE);
		mouseExpGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		mouseExpGroup.setText(settings.msg("dialog.template.mouse"));
		mouseExpGroup.setLayout(new GridLayout(2, false));

		final Label int33Label = new Label(mouseExpGroup, SWT.NONE);
		int33Label.setText(settings.msg("dialog.template.int33"));
		int33 = new Button(mouseExpGroup, SWT.CHECK);

		final Label biosps2Label = new Label(mouseExpGroup, SWT.NONE);
		biosps2Label.setText(settings.msg("dialog.template.biosps2"));
		biosps2 = new Button(mouseExpGroup, SWT.CHECK);

		final Label auxLabel = new Label(mouseExpGroup, SWT.NONE);
		auxLabel.setText(settings.msg("dialog.template.aux"));
		aux = new Button(mouseExpGroup, SWT.CHECK);

		final Label auxdeviceLabel = new Label(mouseExpGroup, SWT.NONE);
		auxdeviceLabel.setText(settings.msg("dialog.template.auxdevice"));
		auxdevice = new Combo(mouseExpGroup, SWT.READ_ONLY);
		auxdevice.setItems(settings.getSettings().getValues("profile", "auxdevice"));

		final Group miscExpGroup = new Group(composite_xo, SWT.NONE);
		miscExpGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		miscExpGroup.setText(settings.msg("dialog.template.miscellaneous"));
		miscExpGroup.setLayout(new GridLayout(2, false));

		final Label filesLabel = new Label(miscExpGroup, SWT.NONE);
		filesLabel.setText(settings.msg("dialog.template.files"));
		files = new Spinner(miscExpGroup, SWT.BORDER);
		files.setMinimum(8);
		files.setMaximum(255);

		final Label isapnpbiosLabel = new Label(miscExpGroup, SWT.NONE);
		isapnpbiosLabel.setText(settings.msg("dialog.template.isapnpbios"));
		isapnpbios = new Button(miscExpGroup, SWT.CHECK);

		final Label ide1Label = new Label(miscExpGroup, SWT.NONE);
		ide1Label.setText(settings.msg("dialog.template.ide1"));
		ide1 = new Button(miscExpGroup, SWT.CHECK);

		final Label ide2Label = new Label(miscExpGroup, SWT.NONE);
		ide2Label.setText(settings.msg("dialog.template.ide2"));
		ide2 = new Button(miscExpGroup, SWT.CHECK);

		final Label ide3Label = new Label(miscExpGroup, SWT.NONE);
		ide3Label.setText(settings.msg("dialog.template.ide3"));
		ide3 = new Button(miscExpGroup, SWT.CHECK);

		final Label ide4Label = new Label(miscExpGroup, SWT.NONE);
		ide4Label.setText(settings.msg("dialog.template.ide4"));
		ide4 = new Button(miscExpGroup, SWT.CHECK);

		final Label automountLabel = new Label(miscExpGroup, SWT.NONE);
		automountLabel.setText(settings.msg("dialog.template.automount"));
		automount = new Button(miscExpGroup, SWT.CHECK);

		final Group printerExpGroup = new Group(composite_xo, SWT.NONE);
		printerExpGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		printerExpGroup.setText(settings.msg("dialog.template.printer"));
		printerExpGroup.setLayout(new GridLayout(2, false));

		final Label printerLabel = new Label(printerExpGroup, SWT.NONE);
		printerLabel.setText(settings.msg("dialog.template.printerenable"));
		printer = new Button(printerExpGroup, SWT.CHECK);

		final Label printerdpiLabel = new Label(printerExpGroup, SWT.NONE);
		printerdpiLabel.setText(settings.msg("dialog.template.printerdpi"));
		printerdpi = new Spinner(printerExpGroup, SWT.BORDER);
		printerdpi.setMinimum(0);
		printerdpi.setMaximum(Short.MAX_VALUE);

		final Label printerwidthLabel = new Label(printerExpGroup, SWT.NONE);
		printerwidthLabel.setText(settings.msg("dialog.template.printerwidth"));
		printerwidth = new Spinner(printerExpGroup, SWT.BORDER);
		printerwidth.setMinimum(0);
		printerwidth.setMaximum(Short.MAX_VALUE);

		final Label printerheightLabel = new Label(printerExpGroup, SWT.NONE);
		printerheightLabel.setText(settings.msg("dialog.template.printerheight"));
		printerheight = new Spinner(printerExpGroup, SWT.BORDER);
		printerheight.setMinimum(0);
		printerheight.setMaximum(Short.MAX_VALUE);

		final Label printeroutputLabel = new Label(printerExpGroup, SWT.NONE);
		printeroutputLabel.setText(settings.msg("dialog.template.printeroutput"));
		printeroutput = new Combo(printerExpGroup, SWT.READ_ONLY);
		printeroutput.setItems(settings.getSettings().getValues("profile", "printeroutput"));

		final Label printermultipageLabel = new Label(printerExpGroup, SWT.NONE);
		printermultipageLabel.setText(settings.msg("dialog.template.printermultipage"));
		printermultipage = new Button(printerExpGroup, SWT.CHECK);

		final Label printerdocpathLabel = new Label(printerExpGroup, SWT.NONE);
		printerdocpathLabel.setText(settings.msg("dialog.template.printerdocpath"));
		printerdocpath = new Text(printerExpGroup, SWT.BORDER);
		printerdocpath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		final Label printertimeoutLabel = new Label(printerExpGroup, SWT.NONE);
		printertimeoutLabel.setText(settings.msg("dialog.template.printertimeout"));
		printertimeout = new Spinner(printerExpGroup, SWT.BORDER);
		printertimeout.setMinimum(0);
		printertimeout.setMaximum(Short.MAX_VALUE);

		final Group parallelExpGroup = new Group(composite_xo, SWT.NONE);
		parallelExpGroup.setText(settings.msg("dialog.template.parallel"));
		parallelExpGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		parallelExpGroup.setLayout(new GridLayout(2, false));

		final Label parallel1Label = new Label(parallelExpGroup, SWT.NONE);
		parallel1Label.setText(settings.msg("dialog.template.parallel1"));
		parallel1 = new Text(parallelExpGroup, SWT.BORDER);
		parallel1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		final Label parallel2Label = new Label(parallelExpGroup, SWT.NONE);
		parallel2Label.setText(settings.msg("dialog.template.parallel2"));
		parallel2 = new Text(parallelExpGroup, SWT.BORDER);
		parallel2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		final Label parallel3Label = new Label(parallelExpGroup, SWT.NONE);
		parallel3Label.setText(settings.msg("dialog.template.parallel3"));
		parallel3 = new Text(parallelExpGroup, SWT.BORDER);
		parallel3.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		final Label dongleLabel = new Label(parallelExpGroup, SWT.NONE);
		dongleLabel.setText(settings.msg("dialog.template.dongle"));
		dongle = new Button(parallelExpGroup, SWT.CHECK);

		final Group ne2000ExpGroup = new Group(composite_xo, SWT.NONE);
		ne2000ExpGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		ne2000ExpGroup.setText(settings.msg("dialog.template.ne2000"));
		ne2000ExpGroup.setLayout(new GridLayout(2, false));

		final Label ne2000Label = new Label(ne2000ExpGroup, SWT.NONE);
		ne2000Label.setText(settings.msg("dialog.template.ne2000enable"));
		ne2000 = new Button(ne2000ExpGroup, SWT.CHECK);

		final Label ne2000baseLabel = new Label(ne2000ExpGroup, SWT.NONE);
		ne2000baseLabel.setText(settings.msg("dialog.template.ne2000base"));
		ne2000base = new Text(ne2000ExpGroup, SWT.BORDER);
		ne2000base.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		final Label ne2000irqLabel = new Label(ne2000ExpGroup, SWT.NONE);
		ne2000irqLabel.setText(settings.msg("dialog.template.ne2000irq"));
		ne2000irq = new Text(ne2000ExpGroup, SWT.BORDER);
		ne2000irq.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		final Label ne2000macaddressLabel = new Label(ne2000ExpGroup, SWT.NONE);
		ne2000macaddressLabel.setText(settings.msg("dialog.template.ne2000macaddress"));
		ne2000macaddress = new Text(ne2000ExpGroup, SWT.BORDER);
		ne2000macaddress.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		final Label ne2000realnicLabel = new Label(ne2000ExpGroup, SWT.NONE);
		ne2000realnicLabel.setText(settings.msg("dialog.template.ne2000realnic"));
		ne2000realnic = new Text(ne2000ExpGroup, SWT.BORDER);
		ne2000realnic.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	}

	protected void createAudioTab(final TabFolder tabFolder) {
		final TabFolder subTabFolder = createRelExpTabs(tabFolder, "dialog.template.tab.audio", 3, 3);
		final Composite composite_ro = (Composite)subTabFolder.getChildren()[0];
		final Composite composite_xo = (Composite)subTabFolder.getChildren()[1];

		final Group generalGroup = new Group(composite_ro, SWT.NONE);
		generalGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		generalGroup.setLayout(new GridLayout(2, false));
		generalGroup.setText(settings.msg("dialog.template.general"));

		final Label silentModeLabel = new Label(generalGroup, SWT.NONE);
		silentModeLabel.setText(settings.msg("dialog.template.silentmode"));
		nosound = new Button(generalGroup, SWT.CHECK);
		nosound.setToolTipText(settings.msg("dialog.template.silentmode.tooltip"));

		final Label sampleRateLabel = new Label(generalGroup, SWT.NONE);
		sampleRateLabel.setText(settings.msg("dialog.template.samplerate"));
		rate = new Combo(generalGroup, SWT.READ_ONLY);
		rate.setVisibleItemCount(10);
		rate.setItems(settings.getSettings().getValues("profile", "rate"));
		rate.setLayoutData(new GridData());
		rate.setToolTipText(settings.msg("dialog.template.samplerate.tooltip"));

		final Label blockSizeLabel = new Label(generalGroup, SWT.NONE);
		blockSizeLabel.setText(settings.msg("dialog.template.blocksize"));
		blocksize = new Combo(generalGroup, SWT.READ_ONLY);
		blocksize.setItems(settings.getSettings().getValues("profile", "blocksize"));
		blocksize.setLayoutData(new GridData());
		blocksize.setToolTipText(settings.msg("dialog.template.blocksize.tooltip"));

		final Label prebufferLabel = new Label(generalGroup, SWT.NONE);
		prebufferLabel.setText(settings.msg("dialog.template.prebuffer"));
		prebuffer = new Combo(generalGroup, SWT.NONE);
		prebuffer.setItems(settings.getSettings().getValues("profile", "prebuffer"));
		prebuffer.setLayoutData(new GridData(70, SWT.DEFAULT));
		prebuffer.setToolTipText(settings.msg("dialog.template.prebuffer.tooltip"));

		final Label mpu401Label = new Label(generalGroup, SWT.NONE);
		mpu401Label.setText(settings.msg("dialog.template.mpu401"));
		mpu401 = new Combo(generalGroup, SWT.READ_ONLY);
		mpu401.setItems(settings.getSettings().getValues("profile", "mpu401"));
		mpu401.setLayoutData(new GridData());
		mpu401.setToolTipText(settings.msg("dialog.template.mpu401.tooltip"));

		final Label midiDeviceLabel = new Label(generalGroup, SWT.NONE);
		midiDeviceLabel.setText(settings.msg("dialog.template.mididevice"));
		midi_device = new Combo(generalGroup, SWT.READ_ONLY);
		midi_device.setVisibleItemCount(10);
		midi_device.setItems(settings.getSettings().getValues("profile", "device"));
		midi_device.setLayoutData(new GridData());
		midi_device.setToolTipText(settings.msg("dialog.template.mididevice.tooltip"));

		final Label configLabel = new Label(generalGroup, SWT.NONE);
		configLabel.setText(settings.msg("dialog.template.midiconfig"));
		midi_config = new Text(generalGroup, SWT.BORDER);
		midi_config.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		midi_config.setToolTipText(settings.msg("dialog.template.midiconfig.tooltip"));

		final Label mixerConfigLabel = new Label(generalGroup, SWT.NONE);
		mixerConfigLabel.setText(settings.msg("dialog.template.mixercommand"));

		final Composite mixerSettings = new Composite(generalGroup, SWT.NONE);
		mixerSettings.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		GridLayout filler = new GridLayout();
		filler.numColumns = 2;
		filler.horizontalSpacing = 2;
		filler.marginWidth = 0;
		mixerSettings.setLayout(filler);

		mixer_config = new Text(mixerSettings, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		GridData mixerConfigGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		mixerConfigGridData.widthHint = 66;
		mixer_config.setLayoutData(mixerConfigGridData);
		Button mixerConfig = new Button(mixerSettings, SWT.NONE);
		mixerConfig.setText("...");
		mixerConfig.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				EditMixerDialog dialog = new EditMixerDialog(shell);
				dialog.setMixerCommand(mixer_config.getText());
				String command = (String)dialog.open();
				if (command != null) {
					mixer_config.setText(command);
				}
			}
		});

		final Group soundblasterGroup = new Group(composite_ro, SWT.NONE);
		soundblasterGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		soundblasterGroup.setText(settings.msg("dialog.template.soundblaster"));
		soundblasterGroup.setLayout(new GridLayout(2, false));

		final Label typeLabel = new Label(soundblasterGroup, SWT.NONE);
		typeLabel.setText(settings.msg("dialog.template.sbtype"));
		sbtype = new Combo(soundblasterGroup, SWT.READ_ONLY);
		sbtype.setVisibleItemCount(10);
		sbtype.setItems(settings.getSettings().getValues("profile", "sbtype"));
		sbtype.setLayoutData(new GridData());
		sbtype.setToolTipText(settings.msg("dialog.template.sbtype.tooltip"));

		final Label oplRateLabel = new Label(soundblasterGroup, SWT.NONE);
		oplRateLabel.setText(settings.msg("dialog.template.sboplrate"));
		oplrate = new Combo(soundblasterGroup, SWT.READ_ONLY);
		oplrate.setVisibleItemCount(10);
		oplrate.setItems(settings.getSettings().getValues("profile", "oplrate"));
		oplrate.setLayoutData(new GridData());
		oplrate.setToolTipText(settings.msg("dialog.template.sboplrate.tooltip"));

		final Label oplModeLabel = new Label(soundblasterGroup, SWT.NONE);
		oplModeLabel.setLayoutData(new GridData());
		oplModeLabel.setText(settings.msg("dialog.template.sboplmode"));
		oplmode = new Combo(soundblasterGroup, SWT.READ_ONLY);
		oplmode.setItems(settings.getSettings().getValues("profile", "oplmode"));
		oplmode.setLayoutData(new GridData());
		oplmode.setToolTipText(settings.msg("dialog.template.sboplmode.tooltip"));

		final Label oplEmuLabel = new Label(soundblasterGroup, SWT.NONE);
		oplEmuLabel.setLayoutData(new GridData());
		oplEmuLabel.setText(settings.msg("dialog.template.sboplemu"));
		oplemu = new Combo(soundblasterGroup, SWT.READ_ONLY);
		oplemu.setItems(settings.getSettings().getValues("profile", "oplemu"));
		oplemu.setLayoutData(new GridData());
		oplemu.setToolTipText(settings.msg("dialog.template.sboplemu.tooltip"));

		final Label addressLabel = new Label(soundblasterGroup, SWT.NONE);
		addressLabel.setText(settings.msg("dialog.template.sbaddress"));
		sbbase = new Combo(soundblasterGroup, SWT.READ_ONLY);
		sbbase.setItems(settings.getSettings().getValues("profile", "sbbase"));
		sbbase.setLayoutData(new GridData());
		sbbase.setToolTipText(settings.msg("dialog.template.sbaddress.tooltip"));

		final Label irqLabel = new Label(soundblasterGroup, SWT.NONE);
		irqLabel.setText(settings.msg("dialog.template.sbirq"));
		irq = new Combo(soundblasterGroup, SWT.READ_ONLY);
		irq.setItems(settings.getSettings().getValues("profile", "irq"));
		irq.setLayoutData(new GridData());
		irq.setToolTipText(settings.msg("dialog.template.sbirq.tooltip"));

		final Label dmaLabel = new Label(soundblasterGroup, SWT.NONE);
		dmaLabel.setText(settings.msg("dialog.template.sbdma"));
		dma = new Combo(soundblasterGroup, SWT.READ_ONLY);
		dma.setItems(settings.getSettings().getValues("profile", "dma"));
		dma.setLayoutData(new GridData());
		dma.setToolTipText(settings.msg("dialog.template.sbdma.tooltip"));

		final Label hdmaLabel = new Label(soundblasterGroup, SWT.NONE);
		hdmaLabel.setLayoutData(new GridData());
		hdmaLabel.setText(settings.msg("dialog.template.sbhdma"));
		hdma = new Combo(soundblasterGroup, SWT.READ_ONLY);
		hdma.setItems(settings.getSettings().getValues("profile", "hdma"));
		hdma.setLayoutData(new GridData());
		hdma.setToolTipText(settings.msg("dialog.template.sbhdma.tooltip"));

		final Label mixerLabel = new Label(soundblasterGroup, SWT.NONE);
		mixerLabel.setText(settings.msg("dialog.template.mixer"));
		sbmixer = new Button(soundblasterGroup, SWT.CHECK);
		sbmixer.setToolTipText(settings.msg("dialog.template.mixer.tooltip"));

		final Group gusGroup = new Group(composite_ro, SWT.NONE);
		gusGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		gusGroup.setText(settings.msg("dialog.template.gravisultrasound"));
		gusGroup.setLayout(new GridLayout(2, false));

		final Label enableLabel_1 = new Label(gusGroup, SWT.NONE);
		enableLabel_1.setLayoutData(new GridData());
		enableLabel_1.setText(settings.msg("dialog.template.enablegus"));
		gus = new Button(gusGroup, SWT.CHECK);
		gus.setLayoutData(new GridData());
		gus.setToolTipText(settings.msg("dialog.template.enablegus.tooltip"));

		final Label sampleRateLabel_2 = new Label(gusGroup, SWT.NONE);
		sampleRateLabel_2.setText(settings.msg("dialog.template.gusrate"));
		gusrate = new Combo(gusGroup, SWT.READ_ONLY);
		gusrate.setVisibleItemCount(10);
		gusrate.setItems(settings.getSettings().getValues("profile", "gusrate"));
		gusrate.setLayoutData(new GridData());
		gusrate.setToolTipText(settings.msg("dialog.template.gusrate.tooltip"));

		final Label addressLabel_1 = new Label(gusGroup, SWT.NONE);
		addressLabel_1.setText(settings.msg("dialog.template.gusaddress"));
		gusbase = new Combo(gusGroup, SWT.READ_ONLY);
		gusbase.setItems(settings.getSettings().getValues("profile", "gusbase"));
		gusbase.setLayoutData(new GridData());
		gusbase.setToolTipText(settings.msg("dialog.template.gusaddress.tooltip"));

		final Label irqLabel_1 = new Label(gusGroup, SWT.NONE);
		irqLabel_1.setText(settings.msg("dialog.template.gusirq1"));
		gusirq1 = new Combo(gusGroup, SWT.READ_ONLY);
		gusirq1.setItems(settings.getSettings().getValues("profile", "irq1"));
		gusirq1.setLayoutData(new GridData());
		gusirq1.setToolTipText(settings.msg("dialog.template.gusirq1.tooltip"));

		final Label irq2Label = new Label(gusGroup, SWT.NONE);
		irq2Label.setText(settings.msg("dialog.template.gusirq2"));
		gusirq2 = new Combo(gusGroup, SWT.READ_ONLY);
		gusirq2.setItems(settings.getSettings().getValues("profile", "irq2"));
		gusirq2.setLayoutData(new GridData());

		final Label dmaLabel_1 = new Label(gusGroup, SWT.NONE);
		dmaLabel_1.setText(settings.msg("dialog.template.gusdma1"));
		gusdma1 = new Combo(gusGroup, SWT.READ_ONLY);
		gusdma1.setItems(settings.getSettings().getValues("profile", "dma1"));
		gusdma1.setLayoutData(new GridData());
		gusdma1.setToolTipText(settings.msg("dialog.template.gusdma1.tooltip"));

		final Label hdmaLabel_1 = new Label(gusGroup, SWT.NONE);
		hdmaLabel_1.setLayoutData(new GridData());
		hdmaLabel_1.setText(settings.msg("dialog.template.gusdma2"));
		gusdma2 = new Combo(gusGroup, SWT.READ_ONLY);
		gusdma2.setItems(settings.getSettings().getValues("profile", "dma2"));
		gusdma2.setLayoutData(new GridData());

		final Label ultradirLabel = new Label(gusGroup, SWT.NONE);
		ultradirLabel.setText(settings.msg("dialog.template.ultradir"));
		ultradir = new Text(gusGroup, SWT.BORDER);
		ultradir.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		ultradir.setToolTipText(settings.msg("dialog.template.ultradir.tooltip"));

		final Group speakerGroup = new Group(composite_ro, SWT.NONE);
		speakerGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		speakerGroup.setText(settings.msg("dialog.template.pcspeaker"));
		speakerGroup.setLayout(new GridLayout(2, false));

		final Label enableLabel = new Label(speakerGroup, SWT.NONE);
		enableLabel.setText(settings.msg("dialog.template.enablepcspeaker"));
		pcspeaker = new Button(speakerGroup, SWT.CHECK);
		pcspeaker.setToolTipText(settings.msg("dialog.template.enablepcspeaker.tooltip"));

		final Label sampleRateLabel_1 = new Label(speakerGroup, SWT.NONE);
		sampleRateLabel_1.setText(settings.msg("dialog.template.pcrate"));
		pcrate = new Combo(speakerGroup, SWT.READ_ONLY);
		pcrate.setVisibleItemCount(10);
		pcrate.setItems(settings.getSettings().getValues("profile", "pcrate"));
		pcrate.setLayoutData(new GridData());
		pcrate.setToolTipText(settings.msg("dialog.template.pcrate.tooltip"));

		final Group tandyGroup = new Group(composite_ro, SWT.NONE);
		tandyGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		tandyGroup.setText(settings.msg("dialog.template.tandy"));
		tandyGroup.setLayout(new GridLayout(2, false));

		final Label enableLabel_2 = new Label(tandyGroup, SWT.NONE);
		enableLabel_2.setText(settings.msg("dialog.template.enabletandy"));
		tandy = new Combo(tandyGroup, SWT.READ_ONLY);
		tandy.setItems(settings.getSettings().getValues("profile", "tandy"));
		tandy.setLayoutData(new GridData());
		tandy.setToolTipText(settings.msg("dialog.template.enabletandy.tooltip"));

		final Label sampleLabel2 = new Label(tandyGroup, SWT.NONE);
		sampleLabel2.setText(settings.msg("dialog.template.tandyrate"));
		tandyrate = new Combo(tandyGroup, SWT.READ_ONLY);
		tandyrate.setVisibleItemCount(10);
		tandyrate.setItems(settings.getSettings().getValues("profile", "tandyrate"));
		tandyrate.setLayoutData(new GridData());
		tandyrate.setToolTipText(settings.msg("dialog.template.tandyrate.tooltip"));

		final Group disneyGroup = new Group(composite_ro, SWT.NONE);
		disneyGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		disneyGroup.setText(settings.msg("dialog.template.miscellaneous"));
		disneyGroup.setLayout(new GridLayout(2, false));

		final Label enableLabel_3 = new Label(disneyGroup, SWT.NONE);
		enableLabel_3.setText(settings.msg("dialog.template.enablesoundsource"));
		disney = new Button(disneyGroup, SWT.CHECK);
		disney.setToolTipText(settings.msg("dialog.template.enablesoundsource.tooltip"));

		final Group generalExpGroup = new Group(composite_xo, SWT.NONE);
		generalExpGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		generalExpGroup.setLayout(new GridLayout(2, false));
		generalExpGroup.setText(settings.msg("dialog.template.general"));

		final Label swapstereoLabel = new Label(generalExpGroup, SWT.NONE);
		swapstereoLabel.setText(settings.msg("dialog.template.swapstereo"));
		swapstereo = new Button(generalExpGroup, SWT.CHECK);

		final Group soundblasterExpGroup = new Group(composite_xo, SWT.NONE);
		soundblasterExpGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		soundblasterExpGroup.setText(settings.msg("dialog.template.soundblaster"));
		soundblasterExpGroup.setLayout(new GridLayout(2, false));

		final Label hardwareaddressLabel = new Label(soundblasterExpGroup, SWT.NONE);
		hardwareaddressLabel.setText(settings.msg("dialog.template.hardwaresbaddress"));
		hardwareaddresssbbase = new Combo(soundblasterExpGroup, SWT.READ_ONLY);
		hardwareaddresssbbase.setItems(settings.getSettings().getValues("profile", "hardwaresbbase"));

		final Label goldplayLabel = new Label(soundblasterExpGroup, SWT.NONE);
		goldplayLabel.setText(settings.msg("dialog.template.goldplay"));
		goldplay = new Button(soundblasterExpGroup, SWT.CHECK);

		final Group mt32ExpGroup = new Group(composite_xo, SWT.NONE);
		mt32ExpGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		mt32ExpGroup.setText(settings.msg("dialog.template.mt32"));
		mt32ExpGroup.setLayout(new GridLayout(2, false));

		final Label swapstereoMT32Label = new Label(mt32ExpGroup, SWT.NONE);
		swapstereoMT32Label.setText(settings.msg("dialog.template.swapstereo"));
		swapstereoMT32 = new Button(mt32ExpGroup, SWT.CHECK);

		final Label loggingMT32Label = new Label(mt32ExpGroup, SWT.NONE);
		loggingMT32Label.setText(settings.msg("dialog.template.mt32.verboselogging"));
		loggingMT32 = new Button(mt32ExpGroup, SWT.CHECK);

		final Label multithreadMT32Label = new Label(mt32ExpGroup, SWT.NONE);
		multithreadMT32Label.setText(settings.msg("dialog.template.mt32.multithread"));
		multithreadMT32 = new Button(mt32ExpGroup, SWT.CHECK);

		final Label dacLabel = new Label(mt32ExpGroup, SWT.NONE);
		dacLabel.setText(settings.msg("dialog.template.mt32.dac"));
		dacMT32 = new Combo(mt32ExpGroup, SWT.READ_ONLY);
		dacMT32.setItems(settings.getSettings().getValues("profile", "mt32dac"));

		final Label reverbmodeLabel = new Label(mt32ExpGroup, SWT.NONE);
		reverbmodeLabel.setText(settings.msg("dialog.template.mt32.reverbmode"));
		reverbmodeMT32 = new Combo(mt32ExpGroup, SWT.READ_ONLY);
		reverbmodeMT32.setItems(settings.getSettings().getValues("profile", "mt32reverbmode"));

		final Label reverbtimeLabel = new Label(mt32ExpGroup, SWT.NONE);
		reverbtimeLabel.setText(settings.msg("dialog.template.mt32.reverbtime"));
		reverbtimeMT32 = new Combo(mt32ExpGroup, SWT.READ_ONLY);
		reverbtimeMT32.setItems(settings.getSettings().getValues("profile", "mt32reverbtime"));

		final Label reverblevelLabel = new Label(mt32ExpGroup, SWT.NONE);
		reverblevelLabel.setText(settings.msg("dialog.template.mt32.reverblevel"));
		reverblevelMT32 = new Combo(mt32ExpGroup, SWT.READ_ONLY);
		reverblevelMT32.setItems(settings.getSettings().getValues("profile", "mt32reverblevel"));

		final Label partialsLabel = new Label(mt32ExpGroup, SWT.NONE);
		partialsLabel.setText(settings.msg("dialog.template.mt32.partials"));
		partialsMT32 = new Spinner(mt32ExpGroup, SWT.BORDER);
		partialsMT32.setMinimum(0);
		partialsMT32.setMaximum(256);

		new Label(composite_xo, SWT.NONE);

		final Group ps1ExpGroup = new Group(composite_xo, SWT.NONE);
		ps1ExpGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		ps1ExpGroup.setText(settings.msg("dialog.template.ps1"));
		ps1ExpGroup.setLayout(new GridLayout(2, false));

		final Label enablePS1Label = new Label(ps1ExpGroup, SWT.NONE);
		enablePS1Label.setText(settings.msg("dialog.template.ps1enable"));
		ps1 = new Button(ps1ExpGroup, SWT.CHECK);

		final Label sampleRatePS1Label = new Label(ps1ExpGroup, SWT.NONE);
		sampleRatePS1Label.setText(settings.msg("dialog.template.ps1rate"));
		ps1rate = new Combo(ps1ExpGroup, SWT.READ_ONLY);
		ps1rate.setItems(settings.getSettings().getValues("profile", "ps1rate"));

		final Group innovaExpGroup = new Group(composite_xo, SWT.NONE);
		innovaExpGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		innovaExpGroup.setText(settings.msg("dialog.template.innova"));
		innovaExpGroup.setLayout(new GridLayout(2, false));

		final Label enableInnovaLabel = new Label(innovaExpGroup, SWT.NONE);
		enableInnovaLabel.setText(settings.msg("dialog.template.innovaenable"));
		innova = new Button(innovaExpGroup, SWT.CHECK);

		final Label sampleRateInnovaLabel = new Label(innovaExpGroup, SWT.NONE);
		sampleRateInnovaLabel.setText(settings.msg("dialog.template.innovarate"));
		innovarate = new Combo(innovaExpGroup, SWT.READ_ONLY);
		innovarate.setItems(settings.getSettings().getValues("profile", "innovarate"));

		final Label sidbaseLabel = new Label(innovaExpGroup, SWT.NONE);
		sidbaseLabel.setText(settings.msg("dialog.template.innovaaddress"));
		innovabase = new Combo(innovaExpGroup, SWT.READ_ONLY);
		innovabase.setItems(settings.getSettings().getValues("profile", "innovabase"));

		final Label sidqualityLabel = new Label(innovaExpGroup, SWT.NONE);
		sidqualityLabel.setText(settings.msg("dialog.template.innovaquality"));
		innovaquality = new Combo(innovaExpGroup, SWT.READ_ONLY);
		innovaquality.setItems(settings.getSettings().getValues("profile", "innovaquality"));
	}

	protected void createMachineTab(final TabFolder tabFolder) {
		final TabFolder subTabFolder = createRelExpTabs(tabFolder, "dialog.template.tab.machine", 1, 1);
		final Composite composite_ro = (Composite)subTabFolder.getChildren()[0];
		final Composite composite_xo = (Composite)subTabFolder.getChildren()[1];

		final Group cpuGroup = new Group(composite_ro, SWT.NONE);
		cpuGroup.setText(settings.msg("dialog.template.cpu"));
		cpuGroup.setLayout(new GridLayout(6, false));

		final Label machineLabel = new Label(cpuGroup, SWT.NONE);
		machineLabel.setText(settings.msg("dialog.template.machine"));
		machine = new Combo(cpuGroup, SWT.READ_ONLY);
		machine.setLayoutData(new GridData());
		machine.setVisibleItemCount(20);
		machine.setToolTipText(settings.msg("dialog.template.machine.tooltip"));

		final Label cputypeLabel = new Label(cpuGroup, SWT.NONE);
		cputypeLabel.setText(settings.msg("dialog.template.cputype"));
		cpu_type = new Combo(cpuGroup, SWT.READ_ONLY);
		cpu_type.setItems(settings.getSettings().getValues("profile", "cputype"));
		cpu_type.setLayoutData(new GridData());
		cpu_type.setVisibleItemCount(10);
		cpu_type.setToolTipText(settings.msg("dialog.template.cputype.tooltip"));
		new Label(cpuGroup, SWT.NONE);
		new Label(cpuGroup, SWT.NONE);

		final Label coreLabel = new Label(cpuGroup, SWT.NONE);
		coreLabel.setText(settings.msg("dialog.template.core"));
		core = new Combo(cpuGroup, SWT.READ_ONLY);
		core.setItems(settings.getSettings().getValues("profile", "core"));
		core.setLayoutData(new GridData());
		core.setToolTipText(settings.msg("dialog.template.core.tooltip"));
		new Label(cpuGroup, SWT.NONE);
		new Label(cpuGroup, SWT.NONE);
		new Label(cpuGroup, SWT.NONE);
		new Label(cpuGroup, SWT.NONE);

		final Label cyclesLabel = new Label(cpuGroup, SWT.NONE);
		cyclesLabel.setText(settings.msg("dialog.template.cycles"));
		cycles = new Combo(cpuGroup, SWT.NONE);
		cycles.setItems(settings.getSettings().getValues("profile", "cycles"));
		cycles.setLayoutData(new GridData(100, SWT.DEFAULT));
		cycles.setVisibleItemCount(15);
		cycles.setToolTipText(settings.msg("dialog.template.cycles.tooltip"));

		final Label upLabel = new Label(cpuGroup, SWT.NONE);
		upLabel.setLayoutData(new GridData());
		upLabel.setText(settings.msg("dialog.template.up"));
		cycles_up = new Combo(cpuGroup, SWT.NONE);
		cycles_up.setItems(settings.getSettings().getValues("profile", "cycles_up"));
		cycles_up.setLayoutData(new GridData(75, SWT.DEFAULT));
		cycles_up.setToolTipText(settings.msg("dialog.template.up.tooltip"));

		final Label downLabel = new Label(cpuGroup, SWT.NONE);
		downLabel.setLayoutData(new GridData());
		downLabel.setText(settings.msg("dialog.template.down"));
		cycles_down = new Combo(cpuGroup, SWT.NONE);
		cycles_down.setItems(settings.getSettings().getValues("profile", "cycles_down"));
		cycles_down.setLayoutData(new GridData(75, SWT.DEFAULT));
		cycles_down.setToolTipText(settings.msg("dialog.template.down.tooltip"));

		memoryGroup = new Group(composite_ro, SWT.NONE);
		memoryGroup.setText(settings.msg("dialog.template.memory"));
		memoryGroup.setLayout(new GridLayout(4, false));

		final Label sizeLabel = new Label(memoryGroup, SWT.NONE);
		sizeLabel.setText(settings.msg("dialog.template.memorysize"));
		memsize = new Combo(memoryGroup, SWT.READ_ONLY);
		memsize.setItems(settings.getSettings().getValues("profile", "memsize"));
		memsize.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		memsize.setToolTipText(settings.msg("dialog.template.memorysize.tooltip"));
		new Label(memoryGroup, SWT.NONE);
		new Label(memoryGroup, SWT.NONE);

		final Label xmsLabel = new Label(memoryGroup, SWT.NONE);
		xmsLabel.setText(settings.msg("dialog.template.xms"));
		xms = new Button(memoryGroup, SWT.CHECK);
		xms.setToolTipText(settings.msg("dialog.template.xms.tooltip"));
		new Label(memoryGroup, SWT.NONE);
		new Label(memoryGroup, SWT.NONE);

		final Label emsLabel = new Label(memoryGroup, SWT.NONE);
		emsLabel.setText(settings.msg("dialog.template.ems"));
		ems = new Combo(memoryGroup, SWT.READ_ONLY);
		ems.setItems(settings.getSettings().getValues("profile", "ems"));
		ems.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		ems.setToolTipText(settings.msg("dialog.template.ems.tooltip"));
		new Label(memoryGroup, SWT.NONE);
		new Label(memoryGroup, SWT.NONE);

		final Label umbLabel = new Label(memoryGroup, SWT.NONE);
		umbLabel.setText(settings.msg("dialog.template.umb"));
		umb = new Combo(memoryGroup, SWT.READ_ONLY);
		umb.setItems(settings.getSettings().getValues("profile", "umb"));
		umb.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		umb.setToolTipText(settings.msg("dialog.template.umb.tooltip"));
		new Label(memoryGroup, SWT.NONE);
		new Label(memoryGroup, SWT.NONE);

		final Group expMemoryGroup = new Group(composite_xo, SWT.NONE);
		expMemoryGroup.setText(settings.msg("dialog.template.memory"));
		expMemoryGroup.setLayout(new GridLayout(2, false));

		final Label sizeKBLabel = new Label(expMemoryGroup, SWT.NONE);
		sizeKBLabel.setText(settings.msg("dialog.template.memorysizekb"));
		memsizeKB = new Text(expMemoryGroup, SWT.BORDER);
		memsizeKB.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		final Label aliasLabel = new Label(expMemoryGroup, SWT.NONE);
		aliasLabel.setText(settings.msg("dialog.template.memalias"));
		memalias = new Combo(expMemoryGroup, SWT.NONE);
		memalias.setItems(settings.getSettings().getValues("profile", "memalias"));
		memalias.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	}

	protected TabFolder createRelExpTabs(final TabFolder tabFolder, final String titleKey, int col1, int col2) {
		final TabItem displayTabItem = new TabItem(tabFolder, SWT.NONE);
		displayTabItem.setText(settings.msg(titleKey));

		final Composite compositeHoldingSubTabs = new Composite(tabFolder, SWT.NONE);
		compositeHoldingSubTabs.setLayout(new FillLayout());
		displayTabItem.setControl(compositeHoldingSubTabs);
		final TabFolder subTabFolder = new TabFolder(compositeHoldingSubTabs, SWT.NONE);

		final TabItem releaseOptionsTabItem = new TabItem(subTabFolder, SWT.NONE);
		releaseOptionsTabItem.setText(settings.msg("dialog.template.tab.releaseoptions"));
		final TabItem experimentalOptionsTabItem = new TabItem(subTabFolder, SWT.NONE);
		experimentalOptionsTabItem.setText(settings.msg("dialog.template.tab.experimentaloptions"));

		final Composite relComp = new Composite(subTabFolder, SWT.NONE);
		relComp.setLayout(new GridLayout(col1, false));
		releaseOptionsTabItem.setControl(relComp);
		final Composite expComp = new Composite(subTabFolder, SWT.NONE);
		expComp.setLayout(new GridLayout(col2, false));
		experimentalOptionsTabItem.setControl(expComp);

		return subTabFolder;
	}

	protected void createDisplayTab(final TabFolder tabFolder) {
		final TabFolder subTabFolder = createRelExpTabs(tabFolder, "dialog.template.tab.display", 1, 2);
		final Composite composite_ro = (Composite)subTabFolder.getChildren()[0];
		final Composite composite_xo = (Composite)subTabFolder.getChildren()[1];

		final Group groupRelease = new Group(composite_ro, SWT.NONE);
		groupRelease.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
		groupRelease.setLayout(new GridLayout(4, false));
		groupRelease.setText(settings.msg("dialog.template.general"));

		final Label outputLabel = new Label(groupRelease, SWT.NONE);
		outputLabel.setText(settings.msg("dialog.template.output"));
		output = new Combo(groupRelease, SWT.READ_ONLY);
		output.setItems(settings.getSettings().getValues("profile", "output"));
		output.setVisibleItemCount(10);
		output.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		output.setToolTipText(settings.msg("dialog.template.output.tooltip"));
		new Label(groupRelease, SWT.NONE);
		new Label(groupRelease, SWT.NONE);

		final Label frameskipLabel = new Label(groupRelease, SWT.NONE);
		frameskipLabel.setText(settings.msg("dialog.template.frameskip"));
		frameskip = new Combo(groupRelease, SWT.READ_ONLY);
		frameskip.setItems(settings.getSettings().getValues("profile", "frameskip"));
		frameskip.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		frameskip.setToolTipText(settings.msg("dialog.template.frameskip.tooltip"));
		new Label(groupRelease, SWT.NONE);
		new Label(groupRelease, SWT.NONE);

		final Label scalerLabel = new Label(groupRelease, SWT.NONE);
		scalerLabel.setText(settings.msg("dialog.template.scaler"));
		scaler = new Combo(groupRelease, SWT.READ_ONLY);
		scaler.setItems(settings.getSettings().getValues("profile", "scaler"));
		scaler.setVisibleItemCount(15);
		scaler.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		scaler.setToolTipText(settings.msg("dialog.template.scaler.tooltip"));

		final Label forcedLabel = new Label(groupRelease, SWT.NONE);
		forcedLabel.setText(settings.msg("dialog.template.scalerforced"));
		scaler_forced = new Button(groupRelease, SWT.CHECK);
		scaler_forced.setToolTipText(settings.msg("dialog.template.scalerforced.tooltip"));

		final Label resolutionLabel = new Label(groupRelease, SWT.NONE);
		resolutionLabel.setText(settings.msg("dialog.template.fullscreenresolution"));
		fullresolution = new Combo(groupRelease, SWT.READ_ONLY);
		fullresolution.setItems(settings.getSettings().getValues("profile", "fullresolution"));
		fullresolution.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		fullresolution.setVisibleItemCount(10);
		fullresolution.setToolTipText(settings.msg("dialog.template.fullscreenresolution.tooltip"));
		new Label(groupRelease, SWT.NONE);
		new Label(groupRelease, SWT.NONE);

		final Label windowResLabel = new Label(groupRelease, SWT.NONE);
		windowResLabel.setText(settings.msg("dialog.template.windowresolution"));
		windowresolution = new Combo(groupRelease, SWT.READ_ONLY);
		windowresolution.setItems(settings.getSettings().getValues("profile", "windowresolution"));
		windowresolution.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		windowresolution.setVisibleItemCount(10);
		windowresolution.setToolTipText(settings.msg("dialog.template.windowresolution.tooltip"));
		new Label(groupRelease, SWT.NONE);
		new Label(groupRelease, SWT.NONE);

		final Label fullscreenLabel = new Label(groupRelease, SWT.NONE);
		fullscreenLabel.setText(settings.msg("dialog.template.fullscreen"));
		fullscreen = new Button(groupRelease, SWT.CHECK);
		fullscreen.setToolTipText(settings.msg("dialog.template.fullscreen.tooltip"));
		new Label(groupRelease, SWT.NONE);
		new Label(groupRelease, SWT.NONE);

		final Label doubleBuffLabel = new Label(groupRelease, SWT.NONE);
		doubleBuffLabel.setText(settings.msg("dialog.template.doublebuffering"));
		fulldouble = new Button(groupRelease, SWT.CHECK);
		fulldouble.setToolTipText(settings.msg("dialog.template.doublebuffering.tooltip"));
		new Label(groupRelease, SWT.NONE);
		new Label(groupRelease, SWT.NONE);

		final Label aspectLabel = new Label(groupRelease, SWT.NONE);
		aspectLabel.setText(settings.msg("dialog.template.aspectcorrection"));
		aspect = new Button(groupRelease, SWT.CHECK);
		aspect.setToolTipText(settings.msg("dialog.template.aspectcorrection.tooltip"));
		new Label(groupRelease, SWT.NONE);
		new Label(groupRelease, SWT.NONE);

		final Group groupExpGeneral = new Group(composite_xo, SWT.NONE);
		groupExpGeneral.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
		groupExpGeneral.setLayout(new GridLayout(2, false));
		groupExpGeneral.setText(settings.msg("dialog.template.general"));

		final Label autofitLabel = new Label(groupExpGeneral, SWT.NONE);
		autofitLabel.setText(settings.msg("dialog.template.autofit"));
		autofit = new Button(groupExpGeneral, SWT.CHECK);

		final Label pixelshaderLabel = new Label(groupExpGeneral, SWT.NONE);
		pixelshaderLabel.setLayoutData(new GridData());
		pixelshaderLabel.setText(settings.msg("dialog.template.pixelshader"));
		pixelshader = new Combo(groupExpGeneral, SWT.NONE);
		pixelshader.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		pixelshader.setVisibleItemCount(20);

		String[] shaders = FileUtils.getShaders();
		if (shaders != null && shaders.length > 0) {
			pixelshader.setItems(shaders);
			pixelshader.add("none", 0);
		} else {
			pixelshader.setItems(settings.getSettings().getValues("profile", "pixelshader"));
		}

		final Label linewiseLabel = new Label(groupExpGeneral, SWT.NONE);
		linewiseLabel.setText(settings.msg("dialog.template.linewise"));
		linewise = new Button(groupExpGeneral, SWT.CHECK);

		final Label char9Label = new Label(groupExpGeneral, SWT.NONE);
		char9Label.setText(settings.msg("dialog.template.char9"));
		char9 = new Button(groupExpGeneral, SWT.CHECK);

		final Label multiscanLabel = new Label(groupExpGeneral, SWT.NONE);
		multiscanLabel.setText(settings.msg("dialog.template.multiscan"));
		multiscan = new Button(groupExpGeneral, SWT.CHECK);

		final Label cgasnowLabel = new Label(groupExpGeneral, SWT.NONE);
		cgasnowLabel.setText(settings.msg("dialog.template.cgasnow"));
		cgasnow = new Button(groupExpGeneral, SWT.CHECK);

		final Label overscanLabel = new Label(groupExpGeneral, SWT.NONE);
		overscanLabel.setText(settings.msg("dialog.template.overscan"));
		overscan = new Combo(groupExpGeneral, SWT.NONE);
		overscan.setItems(settings.getSettings().getValues("profile", "overscan"));
		overscan.setLayoutData(new GridData(25, SWT.DEFAULT));

		final Label vsyncModeLabel = new Label(groupExpGeneral, SWT.NONE);
		vsyncModeLabel.setText(settings.msg("dialog.template.vsyncmode"));
		vsyncmode = new Combo(groupExpGeneral, SWT.READ_ONLY);
		vsyncmode.setItems(settings.getSettings().getValues("profile", "vsyncmode"));

		final Label vsyncRateLabel = new Label(groupExpGeneral, SWT.NONE);
		vsyncRateLabel.setLayoutData(new GridData());
		vsyncRateLabel.setText(settings.msg("dialog.template.vsyncrate"));
		vsyncrate = new Text(groupExpGeneral, SWT.BORDER);
		vsyncrate.setLayoutData(new GridData(25, SWT.DEFAULT));

		final Label forceRateLabel = new Label(groupExpGeneral, SWT.NONE);
		forceRateLabel.setLayoutData(new GridData());
		forceRateLabel.setText(settings.msg("dialog.template.forcerate"));
		forcerate = new Text(groupExpGeneral, SWT.BORDER);
		forcerate.setLayoutData(new GridData(25, SWT.DEFAULT));

		final Label videoRamLabel = new Label(groupExpGeneral, SWT.NONE);
		videoRamLabel.setText(settings.msg("dialog.template.videoram"));
		videoram = new Combo(groupExpGeneral, SWT.READ_ONLY);
		videoram.setItems(settings.getSettings().getValues("profile", "vmemsize"));
		videoram.setLayoutData(new GridData(25, SWT.DEFAULT));

		final Group groupExpGlide = new Group(composite_xo, SWT.NONE);
		groupExpGlide.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
		groupExpGlide.setLayout(new GridLayout(2, false));
		groupExpGlide.setText(settings.msg("dialog.template.glide"));

		final Label glideLabel = new Label(groupExpGlide, SWT.NONE);
		glideLabel.setText(settings.msg("dialog.template.glide"));
		glide = new Combo(groupExpGlide, SWT.READ_ONLY);
		glide.setItems(settings.getSettings().getValues("profile", "glide"));
		glide.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false));

		final Label glidePortLabel = new Label(groupExpGlide, SWT.NONE);
		glidePortLabel.setText(settings.msg("dialog.template.glideport"));
		glideport = new Text(groupExpGlide, SWT.BORDER);
		glideport.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		final Label lfbGlideLabel = new Label(groupExpGlide, SWT.NONE);
		lfbGlideLabel.setText(settings.msg("dialog.template.lfbglide"));
		lfbGlide = new Combo(groupExpGlide, SWT.READ_ONLY);
		lfbGlide.setItems(settings.getSettings().getValues("profile", "lfbglide"));
		lfbGlide.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		final Label splashLabel = new Label(groupExpGlide, SWT.NONE);
		splashLabel.setText(settings.msg("dialog.template.splash3dfx"));
		splash3dfx = new Button(groupExpGlide, SWT.CHECK);
	}

	protected void createGeneralTab(final TabFolder tabFolder) {
		generalTabItem = new TabItem(tabFolder, SWT.NONE);
		generalTabItem.setText(settings.msg("dialog.template.tab.general"));

		final Composite composite_1 = new Composite(tabFolder, SWT.NONE);
		composite_1.setLayout(new GridLayout());
		generalTabItem.setControl(composite_1);

		associationGroup = new Group(composite_1, SWT.NONE);
		associationGroup.setText(settings.msg("dialog.template.association"));
		associationGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		associationGroup.setLayout(new GridLayout(5, false));

		final Label dbversionLabel = new Label(associationGroup, SWT.NONE);
		dbversionLabel.setText(settings.msg("dialog.template.dosboxversion"));
		dbversion = new Combo(associationGroup, SWT.READ_ONLY);
		dbversion.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		dbversion.setVisibleItemCount(20);

		setButton = new Button(associationGroup, SWT.NONE);
		setButton.setText(settings.msg("dialog.template.set"));
		setButton.setToolTipText(settings.msg("dialog.template.set.tooltip"));
		setButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				doPerformDosboxConfAction(DosboxConfAction.SET);
			}
		});

		switchButton = new Button(associationGroup, SWT.NONE);
		switchButton.setText(settings.msg("dialog.template.switch"));
		switchButton.setToolTipText(settings.msg("dialog.template.switch.tooltip"));
		switchButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				doPerformDosboxConfAction(DosboxConfAction.SWITCH);
			}
		});

		dbversion.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				setButton.setEnabled(true);
				switchButton.setEnabled(true);
			}
		});

		reloadButton = new Button(associationGroup, SWT.NONE);
		reloadButton.setText(settings.msg("dialog.template.reloadsettings"));
		reloadButton.setToolTipText(settings.msg("dialog.template.reloadsettings.tooltip"));
		reloadButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				doPerformDosboxConfAction(DosboxConfAction.RELOAD);
			}
		});

		miscGroup = new Group(composite_1, SWT.NONE);
		miscGroup.setText(settings.msg("dialog.template.miscellaneous"));
		miscGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		miscGroup.setLayout(new GridLayout(3, false));

		new Label(miscGroup, SWT.NONE);
		final Label activeLabel = new Label(miscGroup, SWT.NONE);
		activeLabel.setText(settings.msg("dialog.template.active"));
		final Label inactiveLabel = new Label(miscGroup, SWT.NONE);
		inactiveLabel.setText(settings.msg("dialog.template.inactive"));

		final Label priorityLabel = new Label(miscGroup, SWT.NONE);
		priorityLabel.setText(settings.msg("dialog.template.priority"));
		priority_active = new Combo(miscGroup, SWT.READ_ONLY);
		priority_active.setItems(settings.getSettings().getValues("profile", "priority_active"));
		priority_active.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		priority_active.setToolTipText(settings.msg("dialog.template.priority.tooltip"));
		priority_inactive = new Combo(miscGroup, SWT.READ_ONLY);
		priority_inactive.setItems(settings.getSettings().getValues("profile", "priority_inactive"));
		priority_inactive.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		priority_inactive.setToolTipText(settings.msg("dialog.template.priority.tooltip"));

		final Label waitOnErrorLabel = new Label(miscGroup, SWT.NONE);
		waitOnErrorLabel.setText(settings.msg("dialog.template.waitonerror"));
		waitonerror = new Button(miscGroup, SWT.CHECK);
		waitonerror.setToolTipText(settings.msg("dialog.template.waitonerror.tooltip"));
		new Label(miscGroup, SWT.NONE);

		final Label exitLabel = new Label(miscGroup, SWT.NONE);
		exitLabel.setText(settings.msg("dialog.template.exitafterwards"));
		exit = new Button(miscGroup, SWT.CHECK);
		new Label(miscGroup, SWT.NONE);

		final Label languageFileLabel = new Label(miscGroup, SWT.NONE);
		languageFileLabel.setLayoutData(new GridData());
		languageFileLabel.setText(settings.msg("dialog.template.languagefile"));
		language = new Text(miscGroup, SWT.BORDER);
		language.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		language.setToolTipText(settings.msg("dialog.template.languagefile.tooltip"));

		final Label capturesLabel = new Label(miscGroup, SWT.NONE);
		capturesLabel.setLayoutData(new GridData());
		capturesLabel.setText(settings.msg("dialog.template.captures"));
		captures = new Text(miscGroup, SWT.BORDER);
		captures.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		captures.setEditable(false);
		captures.setToolTipText(settings.msg("dialog.template.captures.tooltip"));

		final Label configFileLabel = new Label(miscGroup, SWT.NONE);
		configFileLabel.setText(settings.msg("dialog.profile.configfile"));
		config_file = new Text(miscGroup, SWT.BORDER);
		config_file.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		config_file.setEditable(false);
		config_file.setText(result instanceof Template ? FileUtils.constructCanonicalTemplateFileLocation(((Template)result).getId()).getPath(): SettingsDialog.confFilenames[0]);
	}

	protected void createInfoTab(final TabFolder tabFolder) {
		infoTabItem = new TabItem(tabFolder, SWT.NONE);
		infoTabItem.setText(settings.msg("dialog.template.tab.info"));

		final Composite composite = new Composite(tabFolder, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		infoTabItem.setControl(composite);

		final Label titleLabel = new Label(composite, SWT.NONE);
		titleLabel.setText(settings.msg("dialog.template.title"));
		title = new Text(composite, SWT.BORDER);
		title.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		final Label defaultLabel = new Label(composite, SWT.NONE);
		defaultLabel.setText(settings.msg("dialog.template.default"));
		defaultButton = new Button(composite, SWT.CHECK);
	}

	protected void selectSettingsByConfiguration(Conf conf) {
		SectionsWrapper sections = conf.getSettings();
		setFieldIfEnabled(sections, "sdl", "priority", priority_active, priority_inactive);
		setFieldIfEnabled(sections, "sdl", "waitonerror", waitonerror);
		setFieldIfEnabled(sections, "dosbox", "language", language);
		setFieldIfEnabled(settings.msg("dialog.profile.automatic"), false, captures);
		setFieldIfEnabled(sections, "sdl", "output", output);
		setFieldIfEnabled(sections, "render", "frameskip", frameskip);
		if (sections.isConflictingValue("render", "scaler")) {
			setFieldIfEnabled(sections, "render", "scaler", scaler);
			setFieldIfEnabled(null, true, scaler_forced);
		} else {
			String value = sections.getValue("render", "scaler");
			if (value.endsWith(" forced")) {
				setFieldIfEnabled(value.substring(0, value.length() - 7), false, scaler);
				setFieldIfEnabled(String.valueOf(true), false, scaler_forced);
			} else {
				setFieldIfEnabled(value, false, scaler);
				setFieldIfEnabled(String.valueOf(false), false, scaler_forced);
			}
		}
		setFieldIfEnabled(sections, "sdl", "fullscreen", fullscreen);
		setFieldIfEnabled(sections, "sdl", "fulldouble", fulldouble);
		setFieldIfEnabled(sections, "sdl", "fullresolution", fullresolution);
		setFieldIfEnabled(sections, "sdl", "windowresolution", windowresolution);
		setFieldIfEnabled(sections, "render", "aspect", aspect);
		setFieldIfEnabled(sections, "dosbox", "machine", machine);
		setFieldIfEnabled(sections, "cpu", "cputype", cpu_type);
		setFieldIfEnabled(sections, "cpu", "core", core);
		setFieldIfEnabled(sections, "cpu", "cycles", cycles);
		setFieldIfEnabled(sections, "cpu", "cycleup", cycles_up);
		setFieldIfEnabled(sections, "cpu", "cycledown", cycles_down);
		setFieldIfEnabled(sections, "dosbox", "memsize", memsize);
		setFieldIfEnabled(sections, "dos", "xms", xms);
		setFieldIfEnabled(sections, "dos", "ems", ems);
		setFieldIfEnabled(sections, "dos", "umb", umb);
		setFieldIfEnabled(sections, "mixer", "nosound", nosound);
		setFieldIfEnabled(sections, "mixer", "rate", rate);
		setFieldIfEnabled(sections, "mixer", "blocksize", blocksize);
		setFieldIfEnabled(sections, "mixer", "prebuffer", prebuffer);
		if (sections.hasValue("midi", "intelligent")) {
			if (sections.isConflictingValue("midi", "mpu401") || sections.isConflictingValue("midi", "intelligent")) {
				setFieldIfEnabled(null, true, mpu401);
			} else {
				setFieldIfEnabled(sections.getBooleanValue("midi", "mpu401") ? (sections.getBooleanValue("midi", "intelligent") ? "intelligent": "uart"): "none", false, mpu401);
			}
		} else {
			setFieldIfEnabled(sections, "midi", "mpu401", mpu401);
		}
		setFieldIfEnabled(sections, "midi", "device", "mididevice", midi_device);
		setFieldIfEnabled(sections, "midi", "config", "midiconfig", midi_config);
		setFieldIfEnabled(sections, "sblaster", "type", "sbtype", sbtype);
		setFieldIfEnabled(sections, "sblaster", "oplmode", oplmode);
		setFieldIfEnabled(sections, "sblaster", "oplrate", oplrate);
		setFieldIfEnabled(sections, "sblaster", "oplemu", oplemu);
		setFieldIfEnabled(sections, "sblaster", "base", "sbbase", sbbase);
		setFieldIfEnabled(sections, "sblaster", "irq", irq);
		setFieldIfEnabled(sections, "sblaster", "dma", dma);
		setFieldIfEnabled(sections, "sblaster", "hdma", hdma);
		setFieldIfEnabled(sections, "sblaster", "mixer", "sbmixer", sbmixer);
		setFieldIfEnabled(sections, "gus", "gus", gus);
		setFieldIfEnabled(sections, "gus", "rate", "gusrate", gusrate);
		setFieldIfEnabled(sections, "gus", "base", "gusbase", gusbase);
		setFieldIfEnabled(sections, "gus", "irq1", "gusirq", gusirq1);
		setFieldIfEnabled(sections, "gus", "irq2", "gusirq", gusirq2);
		setFieldIfEnabled(sections, "gus", "dma1", "gusdma", gusdma1);
		setFieldIfEnabled(sections, "gus", "dma2", "gusdma", gusdma2);
		setFieldIfEnabled(sections, "gus", "ultradir", ultradir);
		setFieldIfEnabled(sections, "speaker", "pcspeaker", pcspeaker);
		setFieldIfEnabled(sections, "speaker", "pcrate", pcrate);
		setFieldIfEnabled(sections, "speaker", "tandy", tandy);
		setFieldIfEnabled(sections, "speaker", "tandyrate", tandyrate);
		setFieldIfEnabled(sections, "speaker", "disney", disney);
		setFieldIfEnabled(sections, "sdl", "autolock", autolock);
		setFieldIfEnabled(sections, "sdl", "sensitivity", sensitivity);
		setFieldIfEnabled(sections, "sdl", "usescancodes", usescancodes);
		setFieldIfEnabled(sections, "sdl", "mapperfile", mapperfile);
		setFieldIfEnabled(sections, "dos", "keyboardlayout", keyboard_layout);
		if (sections.hasValue("joystick", "joysticktype")) {
			setFieldIfEnabled(sections, "joystick", "joysticktype", joysticktype);
			setFieldIfEnabled(sections, "joystick", "timed", timed);
			setFieldIfEnabled(sections, "joystick", "autofire", autofire);
			setFieldIfEnabled(sections, "joystick", "swap34", swap34);
			setFieldIfEnabled(sections, "joystick", "buttonwrap", buttonwrap);
		} else {
			setFieldIfEnabled(sections, "bios", "joysticktype", joysticktype);
		}
		setFieldIfEnabled(sections, "serial", "serial1", serial1);
		setFieldIfEnabled(sections, "serial", "serial2", serial2);
		setFieldIfEnabled(sections, "serial", "serial3", serial3);
		setFieldIfEnabled(sections, "serial", "serial4", serial4);
		setFieldIfEnabled(sections, "ipx", "ipx", ipx);
		ipxnet.setEnabled(ipx.getSelection());

		setFieldIfEnabled(sections, "render", "autofit", autofit);
		setFieldIfEnabled(sections, "sdl", "pixelshader", pixelshader);
		setFieldIfEnabled(sections, "render", "linewise", linewise);
		setFieldIfEnabled(sections, "render", "char9", char9);
		setFieldIfEnabled(sections, "render", "multiscan", multiscan);
		setFieldIfEnabled(sections, "cpu", "cgasnow", cgasnow);
		setFieldIfEnabled(sections, "sdl", "overscan", overscan);
		setFieldIfEnabled(sections, "vsync", "vsyncmode", vsyncmode);
		setFieldIfEnabled(sections, "vsync", "vsyncrate", vsyncrate);
		setFieldIfEnabled(sections, "cpu", "forcerate", forcerate);
		setFieldIfEnabled(sections, "dosbox", "vmemsize", videoram);
		setFieldIfEnabled(sections, "glide", "glide", glide);
		setFieldIfEnabled(sections, "glide", "port", "grport", glideport);
		setFieldIfEnabled(sections, "glide", "lfb", lfbGlide);
		setFieldIfEnabled(sections, "glide", "splash", splash3dfx);
		setFieldIfEnabled(sections, "dosbox", "memsizekb", memsizeKB);
		setFieldIfEnabled(sections, "dosbox", "memalias", memalias);
		setFieldIfEnabled(sections, "mixer", "swapstereo", swapstereo);
		setFieldIfEnabled(sections, "sblaster", "hardwarebase", hardwareaddresssbbase);
		setFieldIfEnabled(sections, "sblaster", "goldplay", goldplay);
		setOnOffFieldIfEnabled(sections, "midi", "mt32.reverse.stereo", swapstereoMT32);
		setOnOffFieldIfEnabled(sections, "midi", "mt32.verbose", loggingMT32);
		setOnOffFieldIfEnabled(sections, "midi", "mt32.thread", multithreadMT32);
		setFieldIfEnabled(sections, "midi", "mt32.dac", dacMT32);
		setFieldIfEnabled(sections, "midi", "mt32.reverb.mode", reverbmodeMT32);
		setFieldIfEnabled(sections, "midi", "mt32.reverb.time", reverbtimeMT32);
		setFieldIfEnabled(sections, "midi", "mt32.reverb.level", reverblevelMT32);
		setFieldIfEnabled(sections, "midi", "mt32.partials", partialsMT32);
		setOnOffFieldIfEnabled(sections, "speaker", "ps1audio", ps1);
		setFieldIfEnabled(sections, "speaker", "ps1audiorate", ps1rate);
		setFieldIfEnabled(sections, "innova", "innova", innova);
		setFieldIfEnabled(sections, "innova", "samplerate", innovarate);
		setFieldIfEnabled(sections, "innova", "sidbase", innovabase);
		setFieldIfEnabled(sections, "innova", "quality", innovaquality);
		setFieldIfEnabled(sections, "dos", "int33", int33);
		setFieldIfEnabled(sections, "dos", "biosps2", biosps2);
		setFieldIfEnabled(sections, "keyboard", "aux", aux);
		setFieldIfEnabled(sections, "keyboard", "auxdevice", auxdevice);
		setFieldIfEnabled(sections, "dos", "files", files);
		setFieldIfEnabled(sections, "cpu", "isapnpbios", isapnpbios);
		setFieldIfEnabled(sections, "ide, primary", "enable", ide1);
		setFieldIfEnabled(sections, "ide, secondary", "enable", ide2);
		setFieldIfEnabled(sections, "ide, tertiary", "enable", ide3);
		setFieldIfEnabled(sections, "ide, quaternary", "enable", ide4);
		setFieldIfEnabled(sections, "dos", "automount", automount);
		setFieldIfEnabled(sections, "printer", "printer", printer);
		setFieldIfEnabled(sections, "printer", "dpi", printerdpi);
		setFieldIfEnabled(sections, "printer", "width", printerwidth);
		setFieldIfEnabled(sections, "printer", "height", printerheight);
		setFieldIfEnabled(sections, "printer", "printoutput", printeroutput);
		setFieldIfEnabled(sections, "printer", "multipage", printermultipage);
		setFieldIfEnabled(sections, "printer", "docpath", printerdocpath);
		setFieldIfEnabled(sections, "printer", "timeout", printertimeout);
		setFieldIfEnabled(sections, "parallel", "parallel1", parallel1);
		setFieldIfEnabled(sections, "parallel", "parallel2", parallel2);
		setFieldIfEnabled(sections, "parallel", "parallel3", parallel3);
		setFieldIfEnabled(sections, "parallel", "dongle", dongle);
		setFieldIfEnabled(sections, "ne2000", "ne2000", ne2000);
		setFieldIfEnabled(sections, "ne2000", "nicbase", ne2000base);
		setFieldIfEnabled(sections, "ne2000", "nicirq", ne2000irq);
		setFieldIfEnabled(sections, "ne2000", "macaddr", ne2000macaddress);
		setFieldIfEnabled(sections, "ne2000", "realnic", ne2000realnic);

		// autoexec settings
		setFieldIfEnabled(String.valueOf(conf.getAutoexec().isExit()), Conf.isConflictingValue(conf.getAutoexec().isExit()), exit);
		setFieldIfEnabled(conf.getAutoexec().getMixer(), Conf.isConflictingValue(conf.getAutoexec().getMixer()), mixer_config);
		setFieldIfEnabled(conf.getAutoexec().getKeyb(), Conf.isConflictingValue(conf.getAutoexec().getKeyb()), keyb);
		setFieldIfEnabled(conf.getAutoexec().getIpxnet(), Conf.isConflictingValue(conf.getAutoexec().getIpxnet()), ipxnet);
		for (int i = 0; i < Autoexec.SECTIONS; i++)
			setFieldIfEnabled(StringRelatedUtils.stringToTextArea(conf.getAutoexec().getCustomSection(i), customCommands[i].getLineDelimiter(), PlatformUtils.EOLN),
				Conf.isConflictingValue(conf.getAutoexec().getCustomSection(i)), customCommands[i]);

		if (!isMultiEdit()) {
			mountingpoints.setItems(conf.getAutoexec().getMountingpoints());
			dosExpandItem.setExpanded(!conf.getAutoexec().isBooter());
			booterExpandItem.setExpanded(conf.getAutoexec().isBooter());
		}
	}

	protected void updateConfigurationBySettings(Conf conf) {
		boolean singleEdit = !isMultiEdit();
		conf.updateValue("sdl", "priority", priority_active.getText() + "," + priority_inactive.getText(), singleEdit || ((Boolean)priority_active.getData() || (Boolean)priority_inactive.getData()));
		conf.updateValue("sdl", "waitonerror", String.valueOf(waitonerror.getSelection()), singleEdit || (Boolean)waitonerror.getData());
		conf.updateValue("dosbox", "language", language.getText(), singleEdit || (Boolean)language.getData());
		conf.updateValue("sdl", "output", output.getText(), singleEdit || (Boolean)output.getData());
		conf.updateValue("render", "frameskip", frameskip.getText(), singleEdit || (Boolean)frameskip.getData());
		conf.updateScalerValue(scaler.getText(), scaler_forced.getSelection(), singleEdit || (Boolean)scaler.getData() || (Boolean)scaler_forced.getData());
		conf.updateValue("sdl", "fullscreen", String.valueOf(fullscreen.getSelection()), singleEdit || (Boolean)fullscreen.getData());
		conf.updateValue("sdl", "fulldouble", String.valueOf(fulldouble.getSelection()), singleEdit || (Boolean)fulldouble.getData());
		conf.updateValue("sdl", "fullresolution", fullresolution.getText(), singleEdit || (Boolean)fullresolution.getData());
		conf.updateValue("sdl", "windowresolution", windowresolution.getText(), singleEdit || (Boolean)windowresolution.getData());
		conf.updateValue("render", "aspect", String.valueOf(aspect.getSelection()), singleEdit || (Boolean)aspect.getData());
		conf.updateValue("dosbox", "machine", machine.getText(), singleEdit || (Boolean)machine.getData());
		conf.updateValue("cpu", "cputype", cpu_type.getText(), singleEdit || (Boolean)cpu_type.getData());
		conf.updateValue("cpu", "core", core.getText(), singleEdit || (Boolean)core.getData());
		conf.updateValue("cpu", "cycles", cycles.getText(), singleEdit || (Boolean)cycles.getData());
		conf.updateValue("cpu", "cycleup", cycles_up.getText(), singleEdit || (Boolean)cycles_up.getData());
		conf.updateValue("cpu", "cycledown", cycles_down.getText(), singleEdit || (Boolean)cycles_down.getData());
		conf.updateValue("dosbox", "memsize", memsize.getText(), singleEdit || (Boolean)memsize.getData());
		conf.updateValue("dos", "xms", String.valueOf(xms.getSelection()), singleEdit || (Boolean)xms.getData());
		conf.updateValue("dos", "ems", ems.getText(), singleEdit || (Boolean)ems.getData());
		conf.updateValue("dos", "umb", umb.getText(), singleEdit || (Boolean)umb.getData());
		conf.updateValue("mixer", "nosound", String.valueOf(nosound.getSelection()), singleEdit || (Boolean)nosound.getData());
		conf.updateValue("mixer", "rate", rate.getText(), singleEdit || (Boolean)rate.getData());
		conf.updateValue("mixer", "blocksize", blocksize.getText(), singleEdit || (Boolean)blocksize.getData());
		conf.updateValue("mixer", "prebuffer", prebuffer.getText(), singleEdit || (Boolean)prebuffer.getData());
		conf.updateMidiValue(mpu401.getText(), singleEdit || (Boolean)mpu401.getData());
		conf.updateValue("midi", "device", "mididevice", midi_device.getText(), singleEdit || (Boolean)midi_device.getData());
		conf.updateValue("midi", "config", "midiconfig", midi_config.getText(), singleEdit || (Boolean)midi_config.getData());
		conf.updateValue("sblaster", "type", "sbtype", sbtype.getText(), singleEdit || (Boolean)sbtype.getData());
		conf.updateValue("sblaster", "oplmode", oplmode.getText(), singleEdit || (Boolean)oplmode.getData());
		conf.updateValue("sblaster", "oplrate", oplrate.getText(), singleEdit || (Boolean)oplrate.getData());
		conf.updateValue("sblaster", "oplemu", oplemu.getText(), singleEdit || (Boolean)oplemu.getData());
		conf.updateValue("sblaster", "base", "sbbase", sbbase.getText(), singleEdit || (Boolean)sbbase.getData());
		conf.updateValue("sblaster", "irq", irq.getText(), singleEdit || (Boolean)irq.getData());
		conf.updateValue("sblaster", "dma", dma.getText(), singleEdit || (Boolean)dma.getData());
		conf.updateValue("sblaster", "hdma", hdma.getText(), singleEdit || (Boolean)hdma.getData());
		conf.updateValue("sblaster", "mixer", "sbmixer", String.valueOf(sbmixer.getSelection()), singleEdit || (Boolean)sbmixer.getData());
		conf.updateValue("gus", "gus", String.valueOf(gus.getSelection()), singleEdit || (Boolean)gus.getData());
		conf.updateValue("gus", "rate", "gusrate", gusrate.getText(), singleEdit || (Boolean)gusrate.getData());
		conf.updateValue("gus", "base", "gusbase", gusbase.getText(), singleEdit || (Boolean)gusbase.getData());
		conf.updateValue("gus", "irq1", "gusirq", gusirq1.getText(), singleEdit || (Boolean)gusirq1.getData());
		conf.updateValue("gus", "irq2", gusirq2.getText(), singleEdit || (Boolean)gusirq2.getData());
		conf.updateValue("gus", "dma1", "gusdma", gusdma1.getText(), singleEdit || (Boolean)gusdma1.getData());
		conf.updateValue("gus", "dma2", gusdma2.getText(), singleEdit || (Boolean)gusdma2.getData());
		conf.updateValue("gus", "ultradir", ultradir.getText(), singleEdit || (Boolean)ultradir.getData());
		conf.updateValue("speaker", "pcspeaker", String.valueOf(pcspeaker.getSelection()), singleEdit || (Boolean)pcspeaker.getData());
		conf.updateValue("speaker", "pcrate", pcrate.getText(), singleEdit || (Boolean)pcrate.getData());
		conf.updateValue("speaker", "tandy", tandy.getText(), singleEdit || (Boolean)tandy.getData());
		conf.updateValue("speaker", "tandyrate", tandyrate.getText(), singleEdit || (Boolean)tandyrate.getData());
		conf.updateValue("speaker", "disney", String.valueOf(disney.getSelection()), singleEdit || (Boolean)disney.getData());
		conf.updateValue("sdl", "autolock", String.valueOf(autolock.getSelection()), singleEdit || (Boolean)autolock.getData());
		conf.updateValue("sdl", "sensitivity", sensitivity.getText(), singleEdit || (Boolean)sensitivity.getData());
		conf.updateValue("sdl", "usescancodes", String.valueOf(usescancodes.getSelection()), singleEdit || (Boolean)usescancodes.getData());
		conf.updateValue("sdl", "mapperfile", mapperfile.getText(), singleEdit || (Boolean)mapperfile.getData());
		conf.updateValue("dos", "keyboardlayout", keyboard_layout.getText(), singleEdit || (Boolean)keyboard_layout.getData());
		conf.updateJoystickValue(joysticktype.getText(), timed.getSelection(), autofire.getSelection(), swap34.getSelection(), buttonwrap.getSelection(), singleEdit || (Boolean)joysticktype.getData(),
			singleEdit || (Boolean)timed.getData(), singleEdit || (Boolean)autofire.getData(), singleEdit || (Boolean)swap34.getData(), singleEdit || (Boolean)buttonwrap.getData());
		conf.updateValue("serial", "serial1", serial1.getText(), singleEdit || (Boolean)serial1.getData());
		conf.updateValue("serial", "serial2", serial2.getText(), singleEdit || (Boolean)serial2.getData());
		conf.updateValue("serial", "serial3", serial3.getText(), singleEdit || (Boolean)serial3.getData());
		conf.updateValue("serial", "serial4", serial4.getText(), singleEdit || (Boolean)serial4.getData());
		conf.updateValue("ipx", "ipx", String.valueOf(ipx.getSelection()), singleEdit || (Boolean)ipx.getData());

		conf.updateValue("render", "autofit", String.valueOf(autofit.getSelection()), singleEdit || (Boolean)autofit.getData());
		conf.updateValue("sdl", "pixelshader", pixelshader.getText(), singleEdit || (Boolean)pixelshader.getData());
		conf.updateValue("render", "linewise", String.valueOf(linewise.getSelection()), singleEdit || (Boolean)linewise.getData());
		conf.updateValue("render", "char9", String.valueOf(char9.getSelection()), singleEdit || (Boolean)char9.getData());
		conf.updateValue("render", "multiscan", String.valueOf(multiscan.getSelection()), singleEdit || (Boolean)multiscan.getData());
		conf.updateValue("cpu", "cgasnow", String.valueOf(cgasnow.getSelection()), singleEdit || (Boolean)cgasnow.getData());
		conf.updateValue("sdl", "overscan", overscan.getText(), singleEdit || (Boolean)overscan.getData());
		conf.updateValue("vsync", "vsyncmode", vsyncmode.getText(), singleEdit || (Boolean)vsyncmode.getData());
		conf.updateValue("vsync", "vsyncrate", vsyncrate.getText(), singleEdit || (Boolean)vsyncrate.getData());
		conf.updateValue("cpu", "forcerate", forcerate.getText(), singleEdit || (Boolean)forcerate.getData());
		conf.updateValue("dosbox", "vmemsize", videoram.getText(), singleEdit || (Boolean)videoram.getData());
		conf.updateValue("glide", "glide", glide.getText(), singleEdit || (Boolean)glide.getData());
		conf.updateValue("glide", "port", "grport", glideport.getText(), singleEdit || (Boolean)glideport.getData());
		conf.updateValue("glide", "lfb", lfbGlide.getText(), singleEdit || (Boolean)lfbGlide.getData());
		conf.updateValue("glide", "splash", String.valueOf(splash3dfx.getSelection()), singleEdit || (Boolean)splash3dfx.getData());
		conf.updateValue("dosbox", "memsizekb", memsizeKB.getText(), singleEdit || (Boolean)memsizeKB.getData());
		conf.updateValue("dosbox", "memalias", memalias.getText(), singleEdit || (Boolean)memalias.getData());
		conf.updateValue("mixer", "swapstereo", String.valueOf(swapstereo.getSelection()), singleEdit || (Boolean)swapstereo.getData());
		conf.updateValue("sblaster", "hardwarebase", hardwareaddresssbbase.getText(), singleEdit || (Boolean)hardwareaddresssbbase.getData());
		conf.updateValue("sblaster", "goldplay", String.valueOf(goldplay.getSelection()), singleEdit || (Boolean)goldplay.getData());
		conf.updateValue("midi", "mt32.reverse.stereo", StringRelatedUtils.onOffValue(swapstereoMT32.getSelection()), singleEdit || (Boolean)swapstereoMT32.getData());
		conf.updateValue("midi", "mt32.verbose", StringRelatedUtils.onOffValue(loggingMT32.getSelection()), singleEdit || (Boolean)loggingMT32.getData());
		conf.updateValue("midi", "mt32.thread", StringRelatedUtils.onOffValue(multithreadMT32.getSelection()), singleEdit || (Boolean)multithreadMT32.getData());
		conf.updateValue("midi", "mt32.dac", dacMT32.getText(), singleEdit || (Boolean)dacMT32.getData());
		conf.updateValue("midi", "mt32.reverb.mode", reverbmodeMT32.getText(), singleEdit || (Boolean)reverbmodeMT32.getData());
		conf.updateValue("midi", "mt32.reverb.time", reverbtimeMT32.getText(), singleEdit || (Boolean)reverbtimeMT32.getData());
		conf.updateValue("midi", "mt32.reverb.level", reverblevelMT32.getText(), singleEdit || (Boolean)reverblevelMT32.getData());
		conf.updateValue("midi", "mt32.partials", partialsMT32.getText(), singleEdit || (Boolean)partialsMT32.getData());
		conf.updateValue("speaker", "ps1audio", StringRelatedUtils.onOffValue(ps1.getSelection()), singleEdit || (Boolean)ps1.getData());
		conf.updateValue("speaker", "ps1audiorate", ps1rate.getText(), singleEdit || (Boolean)ps1rate.getData());
		conf.updateValue("innova", "innova", String.valueOf(innova.getSelection()), singleEdit || (Boolean)innova.getData());
		conf.updateValue("innova", "samplerate", innovarate.getText(), singleEdit || (Boolean)innovarate.getData());
		conf.updateValue("innova", "sidbase", innovabase.getText(), singleEdit || (Boolean)innovabase.getData());
		conf.updateValue("innova", "quality", innovaquality.getText(), singleEdit || (Boolean)innovaquality.getData());
		conf.updateValue("dos", "int33", String.valueOf(int33.getSelection()), singleEdit || (Boolean)int33.getData());
		conf.updateValue("dos", "biosps2", String.valueOf(biosps2.getSelection()), singleEdit || (Boolean)biosps2.getData());
		conf.updateValue("keyboard", "aux", String.valueOf(aux.getSelection()), singleEdit || (Boolean)aux.getData());
		conf.updateValue("keyboard", "auxdevice", auxdevice.getText(), singleEdit || (Boolean)auxdevice.getData());
		conf.updateValue("dos", "files", files.getText(), singleEdit || (Boolean)files.getData());
		conf.updateValue("cpu", "isapnpbios", String.valueOf(isapnpbios.getSelection()), singleEdit || (Boolean)isapnpbios.getData());
		conf.updateValue("ide, primary", "enable", String.valueOf(ide1.getSelection()), singleEdit || (Boolean)ide1.getData());
		conf.updateValue("ide, secondary", "enable", String.valueOf(ide2.getSelection()), singleEdit || (Boolean)ide2.getData());
		conf.updateValue("ide, tertiary", "enable", String.valueOf(ide3.getSelection()), singleEdit || (Boolean)ide3.getData());
		conf.updateValue("ide, quaternary", "enable", String.valueOf(ide4.getSelection()), singleEdit || (Boolean)ide4.getData());
		conf.updateValue("dos", "automount", String.valueOf(automount.getSelection()), singleEdit || (Boolean)automount.getData());
		conf.updateValue("printer", "printer", String.valueOf(printer.getSelection()), singleEdit || (Boolean)printer.getData());
		conf.updateValue("printer", "dpi", printerdpi.getText(), singleEdit || (Boolean)printerdpi.getData());
		conf.updateValue("printer", "width", printerwidth.getText(), singleEdit || (Boolean)printerwidth.getData());
		conf.updateValue("printer", "height", printerheight.getText(), singleEdit || (Boolean)printerheight.getData());
		conf.updateValue("printer", "printoutput", printeroutput.getText(), singleEdit || (Boolean)printeroutput.getData());
		conf.updateValue("printer", "multipage", String.valueOf(printermultipage.getSelection()), singleEdit || (Boolean)printermultipage.getData());
		conf.updateValue("printer", "docpath", printerdocpath.getText(), singleEdit || (Boolean)printerdocpath.getData());
		conf.updateValue("printer", "timeout", printertimeout.getText(), singleEdit || (Boolean)printertimeout.getData());
		conf.updateValue("parallel", "parallel1", parallel1.getText(), singleEdit || (Boolean)parallel1.getData());
		conf.updateValue("parallel", "parallel2", parallel2.getText(), singleEdit || (Boolean)parallel2.getData());
		conf.updateValue("parallel", "parallel3", parallel3.getText(), singleEdit || (Boolean)parallel3.getData());
		conf.updateValue("parallel", "dongle", String.valueOf(dongle.getSelection()), singleEdit || (Boolean)dongle.getData());
		conf.updateValue("ne2000", "ne2000", String.valueOf(ne2000.getSelection()), singleEdit || (Boolean)ne2000.getData());
		conf.updateValue("ne2000", "nicbase", ne2000base.getText(), singleEdit || (Boolean)ne2000base.getData());
		conf.updateValue("ne2000", "nicirq", ne2000irq.getText(), singleEdit || (Boolean)ne2000irq.getData());
		conf.updateValue("ne2000", "macaddr", ne2000macaddress.getText(), singleEdit || (Boolean)ne2000macaddress.getData());
		conf.updateValue("ne2000", "realnic", ne2000realnic.getText(), singleEdit || (Boolean)ne2000realnic.getData());

		// autoexec settings
		if (singleEdit) {
			String[] customSections = new String[customCommands.length];
			for (int i = 0; i < customCommands.length; i++)
				customSections[i] = StringRelatedUtils.textAreaToString(customCommands[i].getText(), customCommands[i].getLineDelimiter(), PlatformUtils.EOLN);
			conf.setAutoexecSettingsForTemplate(exit.getSelection(), mixer_config.getText(), keyb.getText(), ipx.getSelection() ? ipxnet.getText(): "", booterExpandItem.getExpanded(), customSections);
		}
	}

	protected void doAddNativeCommand() {
		final EditNativeCommandDialog cmdDialog = new EditNativeCommandDialog(shell);
		NativeCommand cmd = (NativeCommand)cmdDialog.open();
		if (cmd != null) {
			int nr = nativeCommands.getSelectionIndex() + 1;
			multiProfileList.get(0).getNativeCommandsList().add(nr, cmd);
			updateNativeCommands(nr);
		}
	}

	protected void doEditNativeCommand() {
		int sel = nativeCommands.getSelectionIndex();
		if (sel != -1) {
			NativeCommand cmd = multiProfileList.get(0).getNativeCommandsList().get(sel);
			if (cmd.getCommand() != null) {
				final EditNativeCommandDialog cmdDialog = new EditNativeCommandDialog(shell);
				cmdDialog.setCommand(cmd);
				cmd = (NativeCommand)cmdDialog.open();
				if (cmd != null) {
					multiProfileList.get(0).getNativeCommandsList().set(sel, cmd);
					updateNativeCommands(sel);
				}
			}
		}
	}

	protected void doRemoveNativeCommand() {
		int sel = nativeCommands.getSelectionIndex();
		if (sel != -1) {
			NativeCommand cmd = multiProfileList.get(0).getNativeCommandsList().get(sel);
			if (cmd.getCommand() != null) {
				multiProfileList.get(0).getNativeCommandsList().remove(sel);
				updateNativeCommands(Math.min(sel, nativeCommands.getItemCount() - 1));
			}
		}
	}

	public static Template duplicateTemplate(final Template template, final java.util.List<DosboxVersion> dbversionsList, final Database dbase, final Shell shell) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(bos);

		try {
			dbase.startTransaction();
			DosboxVersion dbversion = DosboxVersion.findById(dbversionsList, template.getDbversionId());
			Conf newCompositeConf = new Conf(template, dbversion, ps);
			Template newTemplate = dbase.duplicateTemplate(template);
			dbase.saveNativeCommands(dbase.readNativeCommandsList(-1, template.getId()), -1, newTemplate.getId());

			newTemplate = new Template(newTemplate.getId(), newTemplate);

			newCompositeConf.injectOrUpdateTemplate(newTemplate);
			newCompositeConf.save();

			dbase.commitTransaction();
			if (bos.size() > 0) {
				GeneralPurposeDialogs.warningMessage(shell, bos.toString());
				bos.reset();
			}
			return newTemplate;
		} catch (Exception e) {
			GeneralPurposeDialogs.warningMessage(shell, e);
			try {
				dbase.rollbackTransaction();
			} catch (SQLException se) {
				GeneralPurposeDialogs.warningMessage(shell, se);
			}
			return null;
		} finally {
			dbase.finishTransaction();
		}
	}

	protected void doAddMount() {
		final EditMountDialog addMountDialog = new EditMountDialog(shell);
		try {
			addMountDialog.setDefaultDriveletter(Mount.getFreeDriveletter(booterExpandItem.getExpanded(), multiProfileList.get(0).getConf().nettoMountedDriveLetters()));
		} catch (Exception e) {
			// nothing we can do, just take default 'C'
		}
		String mount = (String)addMountDialog.open();
		if (mount != null) {
			mountingpoints.setItems(multiProfileList.get(0).getConf().addMount(mount));
			mountingpoints.select(mountingpoints.getItemCount() - 1);
		}
	}

	protected void doEditMount() {
		int mounts = mountingpoints.getItemCount();
		int sel = mountingpoints.getSelectionIndex();
		if (sel != -1) {
			final EditMountDialog editMountDialog = new EditMountDialog(shell);
			editMountDialog.setMount(mountingpoints.getItem(sel));
			String mount = (String)editMountDialog.open();
			if (mount != null) {
				mountingpoints.setItems(multiProfileList.get(0).getConf().editMount(sel, mount));
				if (mountingpoints.getItemCount() == mounts) {
					mountingpoints.select(sel);
				} else {
					mountingpoints.select(mountingpoints.getItemCount() - 1);
				}
			}
		}
	}

	protected void doRemoveMount() {
		int mounts = mountingpoints.getItemCount();
		int sel = mountingpoints.getSelectionIndex();
		if (sel == -1 && mounts == 1) {
			sel = 0;
			mountingpoints.select(sel);
		}
		if (sel != -1) {
			mountingpoints.setItems(multiProfileList.get(0).getConf().removeMount(sel));
			if (mountingpoints.getItemCount() == mounts) {
				mountingpoints.select(sel);
			} else {
				if (mountingpoints.getItemCount() > 0) {
					mountingpoints.select(mountingpoints.getItemCount() - 1);
				}
			}
		}
	}

	protected void updateItems(String[] items, boolean available, Combo combo) {
		java.util.List<String> l = new ArrayList<String>(Arrays.asList(combo.getItems()));
		boolean changes = false;
		if (available) {
			for (String s: items) {
				if (!l.contains(s)) {
					l.add(s);
					changes = true;
				}
			}
		} else {
			for (String s: items) {
				if (l.contains(s)) {
					l.remove(s);
					changes = true;
				}
			}
		}
		if (changes) {
			combo.setItems(l.toArray(new String[0]));
		}
	}

	protected void enableSettingsByConfiguration(SectionsWrapper conf) {
		setButton.setEnabled(false);
		switchButton.setEnabled(false);

		String[] otherValues = null;
		if (conf != null) {
			otherValues = conf.hasValue("cpu", "cputype") ? settings.getSettings().getValues("profile", "machine073"): settings.getSettings().getValues("profile", "machine");

			updateItems(new String[] {"desktop"}, conf.hasFullResolutionDesktopSupport(), fullresolution);
			updateItems(new String[] {"openglhq", "direct3d"}, conf.hasOutputDirect3DSupport(), output);
			updateItems(new String[] {"cga_mono", "svga_s3_full", "amstrad"}, conf.hasAmstradSupport(), machine);
			updateItems(new String[] {"hardware2x", "hardware3x"}, conf.hasHardwareScalerSupport(), scaler);
			updateItems(new String[] {"mt32", "synth", "timidity"}, conf.hasMT32Support(), midi_device);
			updateItems(new String[] {"sb16vibra"}, conf.hasSoundBlaster16VibraSupport(), sbtype);
			updateItems(new String[] {"hardware", "hardwaregb"}, conf.hasHardwareOPLSupport(), oplmode);
			updateItems(new String[] {"486", "pentium", "pentium_mmx"}, conf.hasAdditionalCPUTypesSupport(), cpu_type);
		} else {
			otherValues = settings.getSettings().getCommonValues("profile", "machine073", "machine");
		}
		if (!Arrays.equals(machine.getItems(), otherValues)) {
			Object obj = machine.getData();
			machine.setItems(otherValues);
			machine.setData(obj);
		}

		enableDisableControl(conf, "cpu", "cputype", cpu_type);
		enableDisableControl(conf, "dos", "umb", umb);
		enableDisableControl(conf, "sdl", "windowresolution", windowresolution);
		enableDisableControl(conf, "sdl", "usescancodes", usescancodes);
		enableDisableControl(conf, "sblaster", "oplemu", oplemu);
		enableDisableControl(conf, "gus", "irq2", gusirq2);
		enableDisableControl(conf, "gus", "dma2", gusdma2);
		enableDisableControl(conf, "speaker", "tandy", tandy);
		enableDisableControl(conf, "bios", "joysticktype", "joystick", "joysticktype", joysticktype);
		enableDisableControl(conf, "joystick", "timed", timed);
		enableDisableControl(conf, "joystick", "autofire", autofire);
		enableDisableControl(conf, "joystick", "swap34", swap34);
		enableDisableControl(conf, "joystick", "buttonwrap", buttonwrap);
		enableDisableControl(conf, "dos", "keyboardlayout", keyboard_layout);
		enableDisableControl(conf, "serial", "serial1", serial1);
		enableDisableControl(conf, "serial", "serial2", serial2);
		enableDisableControl(conf, "serial", "serial3", serial3);
		enableDisableControl(conf, "serial", "serial4", serial4);

		enableDisableControl(conf, "render", "autofit", autofit);
		enableDisableControl(conf, "sdl", "pixelshader", pixelshader);
		enableDisableControl(conf, "render", "linewise", linewise);
		enableDisableControl(conf, "render", "char9", char9);
		enableDisableControl(conf, "render", "multiscan", multiscan);
		enableDisableControl(conf, "cpu", "cgasnow", cgasnow);
		enableDisableControl(conf, "sdl", "overscan", overscan);
		enableDisableControl(conf, "vsync", "vsyncmode", vsyncmode);
		enableDisableControl(conf, "vsync", "vsyncrate", vsyncrate);
		enableDisableControl(conf, "cpu", "forcerate", forcerate);
		enableDisableControl(conf, "dosbox", "vmemsize", videoram);
		enableDisableControl(conf, "glide", "glide", glide);
		enableDisableControl(conf, "glide", "port", "grport", glideport);
		enableDisableControl(conf, "glide", "lfb", lfbGlide);
		enableDisableControl(conf, "glide", "splash", splash3dfx);
		enableDisableControl(conf, "dosbox", "memsizekb", memsizeKB);
		enableDisableControl(conf, "dosbox", "memalias", memalias);
		enableDisableControl(conf, "mixer", "swapstereo", swapstereo);
		enableDisableControl(conf, "sblaster", "hardwarebase", hardwareaddresssbbase);
		enableDisableControl(conf, "sblaster", "goldplay", goldplay);
		enableDisableControl(conf, "midi", "mt32.reverse.stereo", swapstereoMT32);
		enableDisableControl(conf, "midi", "mt32.verbose", loggingMT32);
		enableDisableControl(conf, "midi", "mt32.thread", multithreadMT32);
		enableDisableControl(conf, "midi", "mt32.dac", dacMT32);
		enableDisableControl(conf, "midi", "mt32.reverb.mode", reverbmodeMT32);
		enableDisableControl(conf, "midi", "mt32.reverb.time", reverbtimeMT32);
		enableDisableControl(conf, "midi", "mt32.reverb.level", reverblevelMT32);
		enableDisableControl(conf, "midi", "mt32.partials", partialsMT32);
		enableDisableControl(conf, "speaker", "ps1audio", ps1);
		enableDisableControl(conf, "speaker", "ps1audiorate", ps1rate);
		enableDisableControl(conf, "innova", "innova", innova);
		enableDisableControl(conf, "innova", "samplerate", innovarate);
		enableDisableControl(conf, "innova", "sidbase", innovabase);
		enableDisableControl(conf, "innova", "quality", innovaquality);
		enableDisableControl(conf, "dos", "int33", int33);
		enableDisableControl(conf, "dos", "biosps2", biosps2);
		enableDisableControl(conf, "keyboard", "aux", aux);
		enableDisableControl(conf, "keyboard", "auxdevice", auxdevice);
		enableDisableControl(conf, "dos", "files", files);
		enableDisableControl(conf, "cpu", "isapnpbios", isapnpbios);
		enableDisableControl(conf, "ide, primary", "enable", ide1);
		enableDisableControl(conf, "ide, secondary", "enable", ide2);
		enableDisableControl(conf, "ide, tertiary", "enable", ide3);
		enableDisableControl(conf, "ide, quaternary", "enable", ide4);
		enableDisableControl(conf, "dos", "automount", automount);
		enableDisableControl(conf, "printer", "printer", printer);
		enableDisableControl(conf, "printer", "dpi", printerdpi);
		enableDisableControl(conf, "printer", "width", printerwidth);
		enableDisableControl(conf, "printer", "height", printerheight);
		enableDisableControl(conf, "printer", "printoutput", printeroutput);
		enableDisableControl(conf, "printer", "multipage", printermultipage);
		enableDisableControl(conf, "printer", "docpath", printerdocpath);
		enableDisableControl(conf, "printer", "timeout", printertimeout);
		enableDisableControl(conf, "parallel", "parallel1", parallel1);
		enableDisableControl(conf, "parallel", "parallel2", parallel2);
		enableDisableControl(conf, "parallel", "parallel3", parallel3);
		enableDisableControl(conf, "parallel", "dongle", dongle);
		enableDisableControl(conf, "ne2000", "ne2000", ne2000);
		enableDisableControl(conf, "ne2000", "nicbase", ne2000base);
		enableDisableControl(conf, "ne2000", "nicirq", ne2000irq);
		enableDisableControl(conf, "ne2000", "macaddr", ne2000macaddress);
		enableDisableControl(conf, "ne2000", "realnic", ne2000realnic);
	}

	protected void doPerformDosboxConfAction(DosboxConfAction action) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(bos);
			updateConfigurationBySettings(multiProfileList.get(0).getConf());
			Conf newDosboxVersion = new Conf(dbversionsList.get(dbversion.getSelectionIndex()), ps);

			if (action == DosboxConfAction.SET) {
				multiProfileList.get(0).getConf().setToDosboxVersion(newDosboxVersion);
			} else if (action == DosboxConfAction.SWITCH) {
				multiProfileList.get(0).getConf().switchToDosboxVersion(newDosboxVersion);
			} else if (action == DosboxConfAction.RELOAD) {
				multiProfileList.get(0).getConf().reloadDosboxVersion(newDosboxVersion);
			}

			enableSettingsByConfiguration(multiProfileList.get(0).getConf().getDosboxSettings());
			selectSettingsByConfiguration(multiProfileList.get(0).getConf());

			if (bos.size() > 0) {
				GeneralPurposeDialogs.warningMessage(getParent(), bos.toString());
				bos.reset();
			}
		} catch (IOException e) {
			GeneralPurposeDialogs.warningMessage(getParent(), e);
		}
	}

	protected static void enableDisableControl(final SectionsWrapper dbConf, final String oldSection, final String oldItem, final String newSection, final String newItem, final Control control) {
		control.setEnabled((dbConf != null) && (dbConf.hasValue(newSection, newItem) || dbConf.hasValue(oldSection, oldItem)));
	}

	protected static void enableDisableControl(final SectionsWrapper dbConf, final String section, final String oldItem, final String newItem, final Control control) {
		control.setEnabled((dbConf != null) && (dbConf.hasValue(section, newItem) || dbConf.hasValue(section, oldItem)));
	}

	protected static void enableDisableControl(final SectionsWrapper dbConf, final String section, final String item, final Control control) {
		control.setEnabled((dbConf != null) && dbConf.hasValue(section, item));
	}

	protected static void highlight(final Control control, final int foreGroundColor) {
		Control[] children = control.getParent().getChildren();
		for (int i = 0; i < children.length; i++) {
			if (children[i] == control) {
				if (i > 0) {
					Control target = null;
					if (children[i - 1] instanceof Label) {
						target = children[i - 1];
					} else {
						if ((i + 1) == children.length - 1) {
							target = children[i + 1];
						} else if ((i - 1) > 0 && children[i - 2] instanceof Label) {
							target = children[i - 2];
						}
					}
					if (target != null) {
						target.setForeground(control.getDisplay().getSystemColor(foreGroundColor));
					}
				} else {
					highlight(control.getParent(), foreGroundColor);
				}
			}
		}
	}

	protected static void setFieldIfEnabled(final String value, final boolean isConflictingValue, final AutoSelectCombo combo) {
		if (isConflictingValue) {
			highlight(combo.getControl(), SWT.COLOR_DARK_RED);
		} else {
			if (!combo.getText().equals(value))
				combo.setText(value);
		}
	}

	protected static void setFieldIfEnabled(final String value, final boolean isConflictingValue, final Control control) {
		setFieldIfEnabled(value, isConflictingValue, control, false);
	}

	protected static void setFieldIfEnabled(final String value, final boolean isConflictingValue, final Control control, final boolean isOnOff) {
		if (control.isEnabled()) {
			if (control instanceof Text) {
				if (isConflictingValue) {
					highlight(control, SWT.COLOR_DARK_RED);
				} else {
					if (!((Text)control).getText().equals(value))
						((Text)control).setText(value);
				}
			} else if (control instanceof Combo) {
				if (isConflictingValue) {
					highlight(control, SWT.COLOR_DARK_RED);
				} else {
					if (!((Combo)control).getText().equals(value))
						((Combo)control).setText(value);
				}
			} else if (control instanceof Button) {
				if (isConflictingValue) {
					((Button)control).setSelection(true);
					((Button)control).setGrayed(true);
					highlight(control, SWT.COLOR_DARK_RED);
				} else {
					boolean newValue = isOnOff ? "on".equalsIgnoreCase(value): Boolean.valueOf(value);
					if ((((Button)control).getSelection() != newValue) || ((Button)control).getGrayed()) {
						((Button)control).setSelection(newValue);
						((Button)control).notifyListeners(SWT.Selection, new Event());
					}
				}
			} else if (control instanceof Scale) {
				if (isConflictingValue) {
					highlight(control, SWT.COLOR_DARK_RED);
				} else {
					Integer newValue = Integer.valueOf(value);
					if (((Scale)control).getSelection() != newValue) {
						((Scale)control).setSelection(newValue);
					}
				}
			} else if (control instanceof Spinner) {
				if (isConflictingValue) {
					highlight(control, SWT.COLOR_DARK_RED);
				} else {
					Integer newValue = Integer.valueOf(value);
					if (((Spinner)control).getSelection() != newValue) {
						((Spinner)control).setSelection(newValue);
					}
				}
			}
		}
	}

	protected static void setFieldIfEnabled(final SectionsWrapper conf, final String section, final String item, final Control control) {
		setFieldIfEnabled(conf.getValue(section, item), conf.isConflictingValue(section, item), control);
	}

	protected static void setOnOffFieldIfEnabled(final SectionsWrapper conf, final String section, final String item, final Control control) {
		setFieldIfEnabled(conf.getValue(section, item), conf.isConflictingValue(section, item), control, true);
	}

	protected static void setFieldIfEnabled(final SectionsWrapper conf, final String section, final String item, final Control control1, final Control control2) {
		if (conf.isConflictingValue(section, item)) {
			setFieldIfEnabled(conf.getValue(section, item), conf.isConflictingValue(section, item), control1);
			setFieldIfEnabled(conf.getValue(section, item), conf.isConflictingValue(section, item), control2);
		} else {
			String value = conf.getValue(section, item);
			String[] seperatedValues = value.split(",");
			if ((seperatedValues != null) && (seperatedValues.length >= 2)) {
				setFieldIfEnabled(seperatedValues[0], false, control1);
				setFieldIfEnabled(seperatedValues[1], false, control2);
			}
		}
	}

	protected static void setFieldIfEnabled(final SectionsWrapper conf, final String section, final String oldItem, final String newItem, final Control control) {
		if (conf.hasValue(section, newItem)) {
			setFieldIfEnabled(conf, section, newItem, control);
		} else {
			setFieldIfEnabled(conf, section, oldItem, control);
		}
	}

	protected boolean isMultiEdit() {
		return multiProfileList.size() > 1;
	}

	protected String fetch(Text t) {
		return ((Boolean)t.getData()) ? t.getText(): null;
	}

	protected String fetch(Combo t) {
		return ((Boolean)t.getData()) ? t.getText(): null;
	}

	protected String fetch(AutoSelectCombo t) {
		return ((Boolean)t.getData()) ? t.getText(): null;
	}

	protected Boolean fetch(Button t) {
		return ((Boolean)t.getData()) ? t.getSelection(): null;
	}

	protected int fetch(Scale t) {
		return ((Boolean)t.getData()) ? t.getSelection(): -1;
	}

	protected int fetch(Spinner t) {
		return ((Boolean)t.getData()) ? t.getSelection(): -1;
	}

	private boolean isValid() {
		GeneralPurposeDialogs.initErrorDialog();
		if (title.getText().equals("")) {
			GeneralPurposeDialogs.addError(settings.msg("dialog.template.required.title"), title, infoTabItem);
		}
		if (setButton.isEnabled()) {
			GeneralPurposeDialogs.addError(settings.msg("dialog.template.required.dosboxassociation"), setButton, generalTabItem);
		}
		return !GeneralPurposeDialogs.displayErrorDialog(shell);
	}
}
