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

import java.io.File;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.dbgl.gui.ProfilesList.ProfilesListType;
import org.dbgl.util.PlatformUtils;
import org.dbgl.util.StringRelatedUtils;


public final class Settings extends Conf {

	private static final String SAMPLE_RATES = "8000 11025 16000 22050 32000 44100 48000 49716";
	private static final String BASE_ADDRS = "220 240 260 280 2a0 2c0 2e0 300";
	private static final String IRQS = "3 5 7 9 10 11 12";
	private static final String DMAS = "0 1 3 5 6 7";

	private static Locale locale;
	private static ResourceBundle mes;
	private static MessageFormat formatter;

	private Settings() {
		super(defaultSettings(),
				PlatformUtils.USE_USER_HOME_DIR || !PlatformUtils.isDirectoryWritable(new File(".")) ? new File(PlatformUtils.USER_DATA_DIR_FILE, "settings.conf"): new File("settings.conf"),
				System.err);

		locale = new Locale(getSettings().getValue("locale", "language"), getSettings().getValue("locale", "country"), getSettings().getValue("locale", "variant"));
		try {
			mes = ResourceBundle.getBundle("plugins/i18n/MessagesBundle", locale);
		} catch (MissingResourceException me) {
			mes = ResourceBundle.getBundle("i18n/MessagesBundle", locale);
		}
		formatter = new MessageFormat("");
		formatter.setLocale(locale);
	}

	private static class SettingsHolder {
		private static Settings instance = new Settings();
	}

	public static Settings getInstance() {
		return SettingsHolder.instance;
	}

	public String msg(String key) {
		try {
			return mes.getString(key);
		} catch (MissingResourceException me) {
			return "[" + key + "]";
		}
	}

	public String msg(String key, Object[] objs) {
		try {
			formatter.applyPattern(msg(key));
			return formatter.format(objs);
		} catch (IllegalArgumentException e) {
			return StringRelatedUtils.toString(e) + "[" + msg(key) + "]";
		}
	}

	public static String toString(final Date date) {
		if (date == null)
			return "-";
		return DateFormat.getDateInstance(DateFormat.SHORT, locale).format(date);
	}

	public static String toString(final Date date, int timeStyle) {
		if (date == null)
			return "-";
		return DateFormat.getDateTimeInstance(DateFormat.SHORT, timeStyle, locale).format(date);
	}

	public static String toDatabaseString(final String dbField, final Date date) {
		StringBuffer sb = new StringBuffer();
		if (date == null) {
			sb.append(dbField).append(" IS NULL");
		} else {
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			sb.append("YEAR(").append(dbField).append(")=").append(cal.get(Calendar.YEAR));
			sb.append(" AND MONTH(").append(dbField).append(")=").append(cal.get(Calendar.MONTH) + 1);
			sb.append(" AND DAYOFMONTH(").append(dbField).append(")=").append(cal.get(Calendar.DAY_OF_MONTH));
		}
		return sb.toString();
	}

	private static SectionsWrapper defaultSettings() {
		SectionsWrapper sec = new SectionsWrapper();

		sec.setValue("gui", "width", "904");
		sec.setValue("gui", "height", "475");
		sec.setValue("gui", "x", "10");
		sec.setValue("gui", "y", "10");
		sec.setValue("gui", "profiledialog_width", "768");
		sec.setValue("gui", "profiledialog_height", "588");
		sec.setValue("gui", "profileloaderdialog_width", "375");
		sec.setValue("gui", "profileloaderdialog_height", "300");
		sec.setValue("gui", "profiledeletedialog_width", "550");
		sec.setValue("gui", "profiledeletedialog_height", "224");
		sec.setValue("gui", "multiprofiledialog_width", "600");
		sec.setValue("gui", "multiprofiledialog_height", "375");
		sec.setValue("gui", "addgamewizard_width", "500");
		sec.setValue("gui", "addgamewizard_height", "375");
		sec.setValue("gui", "dosboxdialog_width", "600");
		sec.setValue("gui", "dosboxdialog_height", "400");
		sec.setValue("gui", "templatedialog_width", "768");
		sec.setValue("gui", "templatedialog_height", "588");
		sec.setValue("gui", "mountdialog_width", "640");
		sec.setValue("gui", "mountdialog_height", "500");
		sec.setValue("gui", "imgsizedialog_width", "330");
		sec.setValue("gui", "imgsizedialog_height", "260");
		sec.setValue("gui", "archivebrowser_width", "400");
		sec.setValue("gui", "mobygamesbrowser_width", "650");
		sec.setValue("gui", "mobygamesbrowser_height", "375");
		sec.setValue("gui", "mixerdialog_width", "950");
		sec.setValue("gui", "mixerdialog_height", "450");
		sec.setValue("gui", "archivebrowser_height", "375");
		sec.setValue("gui", "dfendimportdialog_width", "600");
		sec.setValue("gui", "dfendimportdialog_height", "375");
		sec.setValue("gui", "exportlistdialog_width", "550");
		sec.setValue("gui", "exportlistdialog_height", "190");
		sec.setValue("gui", "nativecommanddialog_width", "520");
		sec.setValue("gui", "nativecommanddialog_height", "225");
		sec.setValue("gui", "export_width", "550");
		sec.setValue("gui", "export_height", "500");
		sec.setValue("gui", "import_width", "654");
		sec.setValue("gui", "import_height", "500");
		sec.setValue("gui", "migratedialog_width", "600");
		sec.setValue("gui", "migratedialog_height", "375");
		sec.setValue("gui", "settingsdialog_width", "715");
		sec.setValue("gui", "settingsdialog_height", "560");
		sec.setValue("gui", "shareconfdialog_width", "560");
		sec.setValue("gui", "shareconfdialog_height", "540");
		sec.setValue("gui", "sharedconfbrowser_width", "860");
		sec.setValue("gui", "sharedconfbrowser_height", "540");
		sec.setValue("gui", "log_width", "860");
		sec.setValue("gui", "log_height", "540");
		sec.setValue("gui", "filterdialog_width", "725");
		sec.setValue("gui", "filterdialog_height", "540");
		sec.setValue("gui", "filtertab", "0");
		sec.setBooleanValue("gui", "maximized", false);
		sec.setValue("gui", "column1width", "150");
		sec.setValue("gui", "column2width", "48");
		sec.setValue("gui", "column3width", "100");
		sec.setValue("gui", "column4width", "100");
		sec.setValue("gui", "column5width", "70");
		sec.setValue("gui", "column6width", "40");
		sec.setValue("gui", "column7width", "60");
		sec.setValue("gui", "column8width", "60");
		sec.setValue("gui", "column9width", "38");
		sec.setValue("gui", "column10width", "40");
		sec.setValue("gui", "column11width", "70");
		sec.setValue("gui", "column12width", "70");
		sec.setValue("gui", "column13width", "70");
		sec.setValue("gui", "column14width", "70");
		sec.setValue("gui", "column15width", "70");
		sec.setValue("gui", "column16width", "70");
		sec.setValue("gui", "column17width", "70");
		sec.setValue("gui", "column18width", "70");
		sec.setValue("gui", "column19width", "44");
		sec.setValue("gui", "column20width", "44");
		sec.setValue("gui", "column21width", "82");
		sec.setValue("gui", "column22width", "70");
		sec.setValue("gui", "column23width", "150");
		sec.setValue("gui", "column24width", "150");
		sec.setValue("gui", "column25width", "150");
		sec.setValue("gui", "column26width", "150");
		sec.setValue("gui", "column27width", "70");
		sec.setValue("gui", "column28width", "70");
		sec.setBooleanValue("gui", "column1visible", true);
		sec.setBooleanValue("gui", "column2visible", true);
		sec.setBooleanValue("gui", "column3visible", true);
		sec.setBooleanValue("gui", "column4visible", true);
		sec.setBooleanValue("gui", "column5visible", true);
		sec.setBooleanValue("gui", "column6visible", true);
		sec.setBooleanValue("gui", "column7visible", true);
		sec.setBooleanValue("gui", "column8visible", true);
		sec.setBooleanValue("gui", "column9visible", true);
		sec.setBooleanValue("gui", "column10visible", false);
		sec.setBooleanValue("gui", "column11visible", false);
		sec.setBooleanValue("gui", "column12visible", false);
		sec.setBooleanValue("gui", "column13visible", false);
		sec.setBooleanValue("gui", "column14visible", false);
		sec.setBooleanValue("gui", "column15visible", false);
		sec.setBooleanValue("gui", "column16visible", false);
		sec.setBooleanValue("gui", "column17visible", false);
		sec.setBooleanValue("gui", "column18visible", false);
		sec.setBooleanValue("gui", "column19visible", false);
		sec.setBooleanValue("gui", "column20visible", false);
		sec.setBooleanValue("gui", "column21visible", false);
		sec.setBooleanValue("gui", "column22visible", false);
		sec.setBooleanValue("gui", "column23visible", false);
		sec.setBooleanValue("gui", "column24visible", false);
		sec.setBooleanValue("gui", "column25visible", false);
		sec.setBooleanValue("gui", "column26visible", false);
		sec.setBooleanValue("gui", "column27visible", false);
		sec.setBooleanValue("gui", "column28visible", false);
		sec.setValue("gui", "column2_1width", "300");
		sec.setValue("gui", "column2_2width", "250");
		sec.setValue("gui", "column2_3width", "150");
		sec.setValue("gui", "column2_4width", "68");
		sec.setValue("gui", "column2_5width", "46");
		sec.setValue("gui", "column2_6width", "150");
		sec.setValue("gui", "column2_7width", "150");
		sec.setValue("gui", "column2_8width", "150");
		sec.setValue("gui", "column2_9width", "112");
		sec.setValue("gui", "column3_1width", "500");
		sec.setValue("gui", "column3_2width", "50");
		sec.setValue("gui", "column3_3width", "68");
		sec.setValue("gui", "column3_4width", "150");
		sec.setValue("gui", "column3_5width", "150");
		sec.setValue("gui", "column3_6width", "150");
		sec.setValue("gui", "column3_7width", "112");
		sec.setValue("gui", "sortcolumn", "0 8");
		sec.setValue("gui", "sortascending", "true true");
		sec.setValue("gui", "columnorder", "0 1 2 3 4 5 6 7 8");
		sec.setValue("gui", "sashweights", "777 222");
		sec.setValue("gui", "screenshotsheight", "100");
		sec.setValue("gui", "screenshotscolumnheight", "50");
		sec.setBooleanValue("gui", "screenshotscolumnstretch", false);
		sec.setBooleanValue("gui", "screenshotscolumnkeepaspectratio", false);
		sec.setBooleanValue("gui", "screenshotsvisible", true);
		sec.setBooleanValue("gui", "autosortonupdate", false);
		sec.setBooleanValue("gui", "screenshotsfilename", true);
		sec.setIntValue("gui", "buttondisplay", 0);
		sec.setValue("gui", "custom1", "Custom1");
		sec.setValue("gui", "custom2", "Custom2");
		sec.setValue("gui", "custom3", "Custom3");
		sec.setValue("gui", "custom4", "Custom4");
		sec.setValue("gui", "custom5", "Custom5");
		sec.setValue("gui", "custom6", "Custom6");
		sec.setValue("gui", "custom7", "Custom7");
		sec.setValue("gui", "custom8", "Custom8");
		sec.setValue("gui", "custom9", "Custom9");
		sec.setValue("gui", "custom10", "Custom10");
		sec.setValue("gui", "searchengine", "mobygames");

		sec.setValue("gui", "notesfont", "Courier 10 0"); // 0 = SWT.NORMAL
		sec.setBooleanValue("gui", "notesvisible", true);

		sec.setValue("gui", "viewstyle", ProfilesListType.TABLE.toString().toLowerCase());
		sec.setIntValue("gui", "small_tile_width", 100);
		sec.setIntValue("gui", "small_tile_height", 82);
		sec.setIntValue("gui", "medium_tile_width", 132);
		sec.setIntValue("gui", "medium_tile_height", 102);
		sec.setIntValue("gui", "large_tile_width", 164);
		sec.setIntValue("gui", "large_tile_height", 122);
		sec.setIntValue("gui", "small_box_width", 75);
		sec.setIntValue("gui", "small_box_height", 100);
		sec.setIntValue("gui", "medium_box_width", 120);
		sec.setIntValue("gui", "medium_box_height", 150);
		sec.setIntValue("gui", "large_box_width", 150);
		sec.setIntValue("gui", "large_box_height", 200);
		sec.setValue("gui", "tile_title_trunc_pos", "end");

		sec.setValue("gui", "gallerybackgroundcolor", "-1");

		sec.setIntValue("profiledefaults", "confpath", 0);
		sec.setIntValue("profiledefaults", "conffile", 0);

		sec.setBooleanValue("dosbox", "hideconsole", false);

		sec.setBooleanValue("communication", "port_enabled", PlatformUtils.IS_WINDOWS);
		sec.setValue("communication", "port", "4740");

		sec.setValue("database", "connectionstring", "jdbc:hsqldb:file:./db/database");
		sec.setValue("database", "username", "sa");
		sec.setValue("database", "pasword", "");

		sec.setValue("directory", "data", ".");
		sec.setValue("directory", "dosbox", ".");
		sec.setValue("directory", "tmpinstall", "TMP_INST");
		sec.setValue("directory", "orgimages", "ORGIMAGE");

		sec.setValue("locale", "language", "en");
		sec.setValue("locale", "country", "");
		sec.setValue("locale", "variant", "");

		sec.setBooleanValue("log", "enabled", true);

		sec.setValue("mobygames", "platform_filter", "dos pc<space>booter");
		sec.setBooleanValue("mobygames", "set_title", true);
		sec.setBooleanValue("mobygames", "set_developer", true);
		sec.setBooleanValue("mobygames", "set_publisher", true);
		sec.setBooleanValue("mobygames", "set_year", true);
		sec.setBooleanValue("mobygames", "set_genre", true);
		sec.setBooleanValue("mobygames", "set_link", true);
		sec.setBooleanValue("mobygames", "set_description", true);
		sec.setBooleanValue("mobygames", "set_rank", true);
		sec.setBooleanValue("mobygames", "choose_coverart", false);
		sec.setBooleanValue("mobygames", "choose_screenshot", false);
		sec.setBooleanValue("mobygames", "force_all_regions_coverart", false);
		sec.setIntValue("mobygames", "multi_max_coverart", 0);
		sec.setIntValue("mobygames", "multi_max_screenshot", 0);
		sec.setIntValue("mobygames", "image_width", 128);
		sec.setIntValue("mobygames", "image_height", 80);
		sec.setIntValue("mobygames", "image_columns", 2);

		sec.setValue("pouet", "platform_filter", "ms-dos ms-dos/gus");
		sec.setBooleanValue("pouet", "set_title", true);
		sec.setBooleanValue("pouet", "set_developer", true);
		sec.setBooleanValue("pouet", "set_year", true);
		sec.setBooleanValue("pouet", "set_genre", true);
		sec.setBooleanValue("pouet", "set_link", true);
		sec.setBooleanValue("pouet", "set_rank", true);
		sec.setBooleanValue("pouet", "choose_coverart", false);
		sec.setBooleanValue("pouet", "choose_screenshot", false);
		sec.setIntValue("pouet", "multi_max_coverart", 0);
		sec.setIntValue("pouet", "multi_max_screenshot", 0);

		sec.setValue("hotud", "platform_filter", "dos");
		sec.setBooleanValue("hotud", "set_title", true);
		sec.setBooleanValue("hotud", "set_developer", true);
		sec.setBooleanValue("hotud", "set_publisher", true);
		sec.setBooleanValue("hotud", "set_year", true);
		sec.setBooleanValue("hotud", "set_genre", true);
		sec.setBooleanValue("hotud", "set_link", true);
		sec.setBooleanValue("hotud", "set_description", true);
		sec.setBooleanValue("hotud", "set_rank", true);

		sec.setValue("thegamesdb", "platform_filter", "pc");
		sec.setBooleanValue("thegamesdb", "set_title", true);
		sec.setBooleanValue("thegamesdb", "set_developer", true);
		sec.setBooleanValue("thegamesdb", "set_publisher", true);
		sec.setBooleanValue("thegamesdb", "set_year", true);
		sec.setBooleanValue("thegamesdb", "set_genre", true);
		sec.setBooleanValue("thegamesdb", "set_link", true);
		sec.setBooleanValue("thegamesdb", "set_description", true);
		sec.setBooleanValue("thegamesdb", "set_rank", true);
		sec.setBooleanValue("thegamesdb", "choose_coverart", false);
		sec.setBooleanValue("thegamesdb", "choose_screenshot", false);
		sec.setIntValue("thegamesdb", "multi_max_coverart", 0);
		sec.setIntValue("thegamesdb", "multi_max_screenshot", 0);

		sec.setBooleanValue("environment", "use", false);
		sec.setValue("environment", "value", "");

		sec.setValue("profile", "priority_active", "lowest lower normal higher highest");
		sec.setValue("profile", "priority_inactive", "lowest lower normal higher highest pause");
		sec.setValue("profile", "output", "ddraw overlay opengl openglnb surface");
		sec.setValue("profile", "frameskip", "0 1 2 3 4 5 6 7 8 9 10");
		sec.setValue("profile", "scaler", "none normal2x normal3x advmame2x advmame3x advinterp2x advinterp3x hq2x hq3x " + "2xsai super2xsai supereagle tv2x tv3x rgb2x rgb3x scan2x scan3x");
		sec.setValue("profile", "fullresolution", "original 0x0 320x200 640x480 800x600 1024x768 1280x768 1280x960 1280x1024");
		sec.setValue("profile", "windowresolution", "original 320x200 640x480 800x600 1024x768 1280x768 1280x960 1280x1024");
		sec.setValue("profile", "machine", "cga hercules pcjr tandy vga");
		sec.setValue("profile", "machine073", "cga hercules pcjr tandy ega vgaonly svga_s3 svga_et3000 svga_et4000 svga_paradise vesa_nolfb vesa_oldvbe");
		sec.setValue("profile", "cputype", "auto 386 386_slow 486_slow pentium_slow 386_prefetch");
		sec.setValue("profile", "core", "dynamic full normal simple auto");
		sec.setValue("profile", "cycles",
			"350 500 750 1000 2000 3000 4000 5000 7500 10000 12500 15000 17500 20000 " + "25000 30000 32500 35000 40000 45000 50000 55000 60000 auto max<space>50% max<space>80% max<space>90% max");
		sec.setValue("profile", "cycles_up", "10 20 50 100 500 1000 2000 5000 10000");
		sec.setValue("profile", "cycles_down", "10 20 50 100 500 1000 2000 5000 10000");
		sec.setValue("profile", "memsize", "0 1 2 4 8 16 32 63");
		sec.setValue("profile", "ems", "false true");
		sec.setValue("profile", "umb", "false true max");
		sec.setValue("profile", "loadfix_value", "1 63 64 127");
		sec.setValue("profile", "rate", SAMPLE_RATES);
		sec.setValue("profile", "blocksize", "256 512 1024 2048 4096 8192");
		sec.setValue("profile", "prebuffer", "10");
		sec.setValue("profile", "mpu401", "none intelligent uart");
		sec.setValue("profile", "device", "alsa default coreaudio coremidi none oss win32");
		sec.setValue("profile", "sbtype", "none gb sb1 sb2 sbpro1 sbpro2 sb16");
		sec.setValue("profile", "oplrate", SAMPLE_RATES);
		sec.setValue("profile", "oplmode", "auto cms opl2 dualopl2 opl3 opl3gold");
		sec.setValue("profile", "oplemu", "default compat fast");
		sec.setValue("profile", "sbbase", BASE_ADDRS);
		sec.setValue("profile", "irq", IRQS);
		sec.setValue("profile", "dma", DMAS);
		sec.setValue("profile", "hdma", DMAS);
		sec.setValue("profile", "gusrate", SAMPLE_RATES);
		sec.setValue("profile", "gusbase", BASE_ADDRS);
		sec.setValue("profile", "irq1", IRQS);
		sec.setValue("profile", "irq2", IRQS);
		sec.setValue("profile", "dma1", DMAS);
		sec.setValue("profile", "dma2", DMAS);
		sec.setValue("profile", "pcrate", SAMPLE_RATES);
		sec.setValue("profile", "tandy", "auto off on");
		sec.setValue("profile", "tandyrate", SAMPLE_RATES);
		sec.setValue("profile", "sensitivity", "10 20 30 40 50 60 70 80 90 100 125 150 175 200 250 300 350 400 " + "450 500 550 600 700 800 900 1000");
		sec.setValue("profile", "joysticktype", "auto none 2axis 4axis 4axis_2 ch fcs");
		sec.setValue("profile", "mount_type", "cdrom dir floppy");
		sec.setValue("profile", "imgmount_type", "iso floppy hdd");
		sec.setValue("profile", "imgmount_fs", "iso fat none");
		sec.setValue("profile", "zipmount_type", "cdrom dir floppy");
		sec.setValue("profile", "freesize", "1 10 100 200 500 1000");
		sec.setValue("profile", "lowlevelcd_type", "aspi ioctl ioctl_dx ioctl_dio ioctl_mci noioctl");
		sec.setValue("profile", "keyboardlayout",
			"auto none " + "ba234 be120 bg241 bg442 bl463 br274 br275 by463 ca58 ca445 cf58 cf445 cf501 cz243 de129 de453 dk159 dv103 ee454 el220 el319 el459 es172 es173 et454 "
					+ "fi153 fo fr120 fr189 gk220 gk319 gk459 gr129 gr453 hr234 hu208 is161 is197 is458 it141 it142 la171 lt210 lt211 lt212 lt221 lt456 lh103 mk449 ml47 mt47 "
					+ "nl143 no155 ph pl214 pl457 po163 rh103 ro333 ro446 ru441 ru443 sd150 sf150 sg150 si234 sk245 sp172 sp173 sq448 sq452 sr118 sr450 su153 sv153 "
					+ "tm tr179 tr440 ua465 uk166 uk168 ur465 us103 ux103 yc118 yc450 yu234");

		sec.setValue("profile", "pixelshader",
			"none 2xSaI.fx 2xSaI_sRGB.fx 2xSaL.fx 2xSaL_Ls.fx 2xSaL2xAA.fx " + "2xSaLAA.fx 4xSaL.fx 4xSoft.fx 4xSoft_PS3.0.fx AdvancedAA.fx bilinear.fx Cartoon.fx ColorSketch.fx "
					+ "CRT.D3D.fx CRT.D3D.br.fx CRT-simple.D3D.fx CRT-simple.D3D.br.fx DotnBloom.D3D.fx GS2x.fx GS2xFilter.fx "
					+ "Gs2xLS.fx Gs2xSmartFilter.fx GS2xSuper.fx GS2xTwo.fx GS4x.fx GS4xColorScale.fx GS4xFilter.fx "
					+ "GS4xHqFilter.fx GS4xScale.fx GS4xSoft.fx HQ2x.fx Lanczos.fx Lanczos12.fx Lanczos16.fx Matrix.fx "
					+ "MCAmber.fx MCGreen.fx MCHerc.fx MCOrange.fx none.fx point.fx scale2x.fx scale2x_ps14.fx Scale2xPlus.fx " + "Scale4x.fx SimpleAA.fx Sketch.fx Super2xSaI.fx SuperEagle.fx Tv.fx");
		sec.setValue("profile", "overscan", "1 2 3 4 5 6 7 8 9 10");
		sec.setValue("profile", "vsyncmode", "off on force host");
		sec.setValue("profile", "lfbglide", "full read write none");
		sec.setValue("profile", "vmemsize", "0 1 2 4 8");
		sec.setValue("profile", "glide", "false true emu");
		sec.setValue("profile", "memalias", "0 24 26");
		sec.setValue("profile", "hardwaresbbase", "210 220 230 240 250 260 280");
		sec.setValue("profile", "mt32dac", "0 1 2 3 auto");
		sec.setValue("profile", "mt32reverbmode", "0 1 2 3 auto");
		sec.setValue("profile", "mt32reverbtime", "0 1 2 3 4 5 6 7");
		sec.setValue("profile", "mt32reverblevel", "0 1 2 3 4 5 6 7");
		sec.setValue("profile", "ps1rate", SAMPLE_RATES);
		sec.setValue("profile", "innovarate", SAMPLE_RATES);
		sec.setValue("profile", "innovabase", BASE_ADDRS);
		sec.setValue("profile", "innovaquality", "0 1 2 3");
		sec.setValue("profile", "auxdevice", "none 2button 3button intellimouse intellimouse45");
		sec.setValue("profile", "printeroutput", "png ps bmp printer");

		sec.setBooleanValue("addgamewizard", "requiresinstallation", false);
		sec.setBooleanValue("addgamewizard", "consultsearchengine", true);
		sec.setBooleanValue("addgamewizard", "consultdbconfws", true);

		sec.setValue("confsharing", "endpoint", "http://www.squadrablu.nl:8080/DBConfWS/apiv1/");

		return sec;
	}
}
