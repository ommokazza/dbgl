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
package org.dbgl.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.dbgl.model.Filter;
import org.dbgl.model.KeyValuePair;
import org.dbgl.model.LogEntry;
import org.dbgl.model.DosboxVersion;
import org.dbgl.model.NativeCommand;
import org.dbgl.model.Profile;
import org.dbgl.model.Template;
import org.dbgl.model.conf.Settings;
import org.dbgl.util.FileUtils;
import org.dbgl.util.StringRelatedUtils;
import org.apache.commons.lang3.StringUtils;
import org.dbgl.gui.GeneralPurposeDialogs;
import org.eclipse.swt.widgets.Shell;


public class Database {

	private static Connection con = null;
	private static boolean initializedNewDatabase = false;

	private static final String GET_IDENTITY_QRY = "CALL IDENTITY()";
	private static final String GAME_LIST_QRY = "SELECT GAM.ID, GAM.TITLE, DEV.NAME, PUBL.NAME, GEN.NAME, YR.YEAR, " + "STAT.STAT, GAM.NOTES, GAM.FAVORITE, "
			+ "GAM.SETUP, GAM.SETUP_PARAMS, GAM.ALT1, GAM.ALT1_PARAMS, GAM.ALT2, GAM.ALT2_PARAMS, " + "GAM.CONFFILE, GAM.CAPTURES, GAM.DBVERSION_ID, "
			+ "GAM.LINK1, GAM.LINK2, GAM.LINK3, GAM.LINK4, GAM.LINK5, GAM.LINK6, GAM.LINK7, GAM.LINK8, "
			+ "GAM.LINK1_TITLE, GAM.LINK2_TITLE, GAM.LINK3_TITLE, GAM.LINK4_TITLE, GAM.LINK5_TITLE, GAM.LINK6_TITLE, GAM.LINK7_TITLE, GAM.LINK8_TITLE, "
			+ "CUST1.VALUE, CUST2.VALUE, CUST3.VALUE, CUST4.VALUE, GAM.CUSTOM5, GAM.CUSTOM6, GAM.CUSTOM7, GAM.CUSTOM8, GAM.CUSTOM9, GAM.CUSTOM10, "
			+ "GAM.STATS_CREATED, GAM.STATS_LASTMODIFY, GAM.STATS_LASTRUN, GAM.STATS_LASTSETUP, GAM.STATS_RUNS, GAM.STATS_SETUPS "
			+ "FROM GAMES GAM, DEVELOPERS DEV, PUBLISHERS PUBL, GENRES GEN, PUBLYEARS YR, STATUS STAT, " + "CUSTOM1 CUST1, CUSTOM2 CUST2, CUSTOM3 CUST3, CUSTOM4 CUST4 "
			+ "WHERE GAM.DEV_ID=DEV.ID AND GAM.PUBL_ID=PUBL.ID AND GAM.GENRE_ID=GEN.ID AND GAM.YEAR_ID=YR.ID AND GAM.STAT_ID=STAT.ID "
			+ "AND   GAM.CUST1_ID=CUST1.ID AND GAM.CUST2_ID=CUST2.ID AND GAM.CUST3_ID=CUST3.ID AND GAM.CUST4_ID=CUST4.ID";
	private static final String DUPL_GAME_QRY = "INSERT INTO GAMES(" + "TITLE, DEV_ID, PUBL_ID, GENRE_ID, YEAR_ID, STAT_ID, NOTES, FAVORITE, "
			+ "SETUP, SETUP_PARAMS, ALT1, ALT1_PARAMS, ALT2, ALT2_PARAMS," + "CONFFILE, CAPTURES, DBVERSION_ID, LINK1, LINK2, LINK3, LINK4, LINK5, LINK6, LINK7, LINK8,"
			+ "LINK1_TITLE, LINK2_TITLE, LINK3_TITLE, LINK4_TITLE, LINK5_TITLE, LINK6_TITLE, LINK7_TITLE, LINK8_TITLE,"
			+ "CUST1_ID, CUST2_ID, CUST3_ID, CUST4_ID, CUSTOM5, CUSTOM6, CUSTOM7, CUSTOM8, CUSTOM9, CUSTOM10) " + "(SELECT TITLE, DEV_ID, PUBL_ID, GENRE_ID, YEAR_ID, STAT_ID, NOTES, FAVORITE,"
			+ "SETUP, SETUP_PARAMS, ALT1, ALT1_PARAMS, ALT2, ALT2_PARAMS, NULL, NULL, DBVERSION_ID," + "LINK1, LINK2, LINK3, LINK4, LINK5, LINK6, LINK7, LINK8, "
			+ "LINK1_TITLE, LINK2_TITLE, LINK3_TITLE, LINK4_TITLE, LINK5_TITLE, LINK6_TITLE, LINK7_TITLE, LINK8_TITLE,"
			+ "CUST1_ID, CUST2_ID, CUST3_ID, CUST4_ID, CUSTOM5, CUSTOM6, CUSTOM7, CUSTOM8, CUSTOM9, CUSTOM10 " + "FROM GAMES WHERE ID = ?)";
	private static final String DUPL_TEMPLATE_QRY = "INSERT INTO TEMPLATES(TITLE, DBVERSION_ID, ISDEFAULT) " + "(SELECT TITLE, DBVERSION_ID, FALSE FROM TEMPLATES WHERE ID = ?)";
	private static final String DEV_LIST_QRY = "SELECT ID, NAME FROM DEVELOPERS ORDER BY NAME";
	private static final String PUBL_LIST_QRY = "SELECT ID, NAME FROM PUBLISHERS ORDER BY NAME";
	private static final String GENRE_LIST_QRY = "SELECT ID, NAME FROM GENRES ORDER BY NAME";
	private static final String PUBLYEAR_LIST_QRY = "SELECT ID, YEAR FROM PUBLYEARS ORDER BY YEAR";
	private static final String DBVERS_LIST_QRY = "SELECT ID, TITLE, PATH, CONFFILE, MULTICONF, USINGCURSES, ISDEFAULT, PARAMETERS, VERSION, STATS_CREATED, STATS_LASTMODIFY, STATS_LASTRUN, STATS_RUNS FROM DOSBOXVERSIONS ORDER BY TITLE";
	private static final String FLTRS_LIST_QRY = "SELECT ID, TITLE, FILTER FROM FILTERS";
	private static final String LOG_LIST_QRY = "SELECT ID, TIME, EVENT, ENTITY_TYPE, ENTITY_ID, ENTITY_TITLE FROM LOG";
	private static final String TEMPL_LIST_QRY = "SELECT ID, TITLE, DBVERSION_ID, ISDEFAULT, STATS_CREATED, STATS_LASTMODIFY, STATS_LASTRUN, STATS_RUNS FROM TEMPLATES ORDER BY ID";
	private static final String STATUS_LIST_QRY = "SELECT ID, STAT FROM STATUS ORDER BY STAT";
	private static final String[] CUSTOM_LIST_QRY = {"SELECT ID, VALUE FROM CUSTOM1 ORDER BY VALUE", "SELECT ID, VALUE FROM CUSTOM2 ORDER BY VALUE", "SELECT ID, VALUE FROM CUSTOM3 ORDER BY VALUE",
			"SELECT ID, VALUE FROM CUSTOM4 ORDER BY VALUE"};
	private static final String DBV_USE_QRY = "SELECT TITLE FROM GAMES WHERE DBVERSION_ID = ? UNION ALL SELECT TITLE FROM TEMPLATES WHERE DBVERSION_ID = ?";
	private static final String NTVCMD_LIST_QRY_TEMPLATE = "SELECT COMMAND, PARAMETERS, CWD, WAITFOR, ORDERNR FROM NATIVECOMMANDS WHERE GAME_ID IS NULL AND TEMPLATE_ID = ? ORDER BY ORDERNR";
	private static final String NTVCMD_LIST_QRY_GAME = "SELECT COMMAND, PARAMETERS, CWD, WAITFOR, ORDERNR FROM NATIVECOMMANDS WHERE GAME_ID = ? AND TEMPLATE_ID IS NULL ORDER BY ORDERNR";

	private static final String ADD_GAME_QRY = "INSERT INTO GAMES(TITLE, DEV_ID, PUBL_ID, GENRE_ID, YEAR_ID, STAT_ID, NOTES, FAVORITE,"
			+ "SETUP, SETUP_PARAMS, ALT1, ALT1_PARAMS, ALT2, ALT2_PARAMS, CONFFILE, CAPTURES, DBVERSION_ID," + "LINK1, LINK2, LINK3, LINK4, LINK5, LINK6, LINK7, LINK8,"
			+ "LINK1_TITLE, LINK2_TITLE, LINK3_TITLE, LINK4_TITLE, LINK5_TITLE, LINK6_TITLE, LINK7_TITLE, LINK8_TITLE,"
			+ "CUST1_ID, CUST2_ID, CUST3_ID, CUST4_ID, CUSTOM5, CUSTOM6, CUSTOM7, CUSTOM8, CUSTOM9, CUSTOM10) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	private static final String ADD_DEVELOPER_QRY = "INSERT INTO DEVELOPERS(NAME) VALUES (?)";
	private static final String ADD_PUBLISHER_QRY = "INSERT INTO PUBLISHERS(NAME) VALUES (?)";
	private static final String ADD_GENRE_QRY = "INSERT INTO GENRES(NAME) VALUES (?)";
	private static final String ADD_YEAR_QRY = "INSERT INTO PUBLYEARS(YEAR) VALUES (?)";
	private static final String ADD_DBVERSION_QRY = "INSERT INTO DOSBOXVERSIONS(TITLE, PATH, CONFFILE, MULTICONF, USINGCURSES, ISDEFAULT, PARAMETERS, VERSION) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
	private static final String ADD_TEMPLATE_QRY = "INSERT INTO TEMPLATES(TITLE, DBVERSION_ID, ISDEFAULT) VALUES (?, ?, ?)";
	private static final String ADD_FILTER_QRY = "INSERT INTO FILTERS(TITLE, FILTER, CONF_FILTER) VALUES (?, ?, '')";
	private static final String ADD_STATUS_QRY = "INSERT INTO STATUS(STAT) VALUES (?)";
	private static final String[] ADD_CUSTOM_QRY = {"INSERT INTO CUSTOM1(VALUE) VALUES (?)", "INSERT INTO CUSTOM2(VALUE) VALUES (?)", "INSERT INTO CUSTOM3(VALUE) VALUES (?)",
			"INSERT INTO CUSTOM4(VALUE) VALUES (?)"};
	private static final String ADD_NATIVECOMMAND_QRY = "INSERT INTO NATIVECOMMANDS(COMMAND, PARAMETERS, CWD, WAITFOR, ORDERNR, GAME_ID, TEMPLATE_ID) VALUES (?, ?, ?, ?, ?, ?, ?)";
	private static final String ADD_LOG_QRY = "INSERT INTO LOG(EVENT, ENTITY_TYPE, ENTITY_ID, ENTITY_TITLE) VALUES(?, ?, ?, ?)";

	private static final String UPD_GAME_QRY = "UPDATE GAMES SET TITLE = ?, DEV_ID = ?, PUBL_ID = ?, GENRE_ID = ?, YEAR_ID = ?, "
			+ "STAT_ID = ?, NOTES = ?, FAVORITE = ?, SETUP = ?, SETUP_PARAMS = ?, ALT1 = ?, ALT1_PARAMS = ?, ALT2 = ?, ALT2_PARAMS = ?,"
			+ "DBVERSION_ID = ?, LINK1 = ?, LINK2 = ?, LINK3 = ?, LINK4 = ?, LINK5 = ?, LINK6 = ?, LINK7 = ?, LINK8 = ?, "
			+ "LINK1_TITLE = ?, LINK2_TITLE = ?, LINK3_TITLE = ?, LINK4_TITLE = ?, LINK5_TITLE = ?, LINK6_TITLE = ?, LINK7_TITLE = ?, LINK8_TITLE = ?,"
			+ "CUST1_ID = ?, CUST2_ID = ?, CUST3_ID = ?, CUST4_ID = ?, CUSTOM5 = ?, CUSTOM6 = ?, CUSTOM7 = ?, CUSTOM8 = ?, CUSTOM9 = ?, CUSTOM10 = ?, "
			+ "STATS_LASTMODIFY = CURRENT_TIMESTAMP WHERE ID = ?";
	private static final String RUN_GAME_QRY = "UPDATE GAMES SET STATS_LASTRUN = CURRENT_TIMESTAMP, STATS_RUNS = (STATS_RUNS + 1) WHERE ID = ?";
	private static final String SETUP_GAME_QRY = "UPDATE GAMES SET STATS_LASTSETUP = CURRENT_TIMESTAMP, STATS_SETUPS = (STATS_SETUPS + 1) WHERE ID = ?";
	private static final String UPD_GAME_CONF_QRY = "UPDATE GAMES SET CONFFILE = ?, CAPTURES = ? WHERE ID = ?";
	private static final String UPD_STPLNKS_QRY = "UPDATE GAMES SET SETUP = ?, ALT1 = ?, ALT2 = ?, "
			+ "LINK1 = ?, LINK2 = ?, LINK3 = ?, LINK4 = ?, LINK5 = ?, LINK6 = ?, LINK7 = ?, LINK8 = ? WHERE ID = ?";
	private static final String UPD_DBV_NODEFAULT = "UPDATE DOSBOXVERSIONS SET ISDEFAULT = FALSE";
	private static final String UPD_DBVERSION_QRY = "UPDATE DOSBOXVERSIONS SET TITLE = ?, PATH = ?, CONFFILE = ?, MULTICONF = ?, USINGCURSES = ?, ISDEFAULT = ?, PARAMETERS = ?, VERSION = ?, STATS_LASTMODIFY = CURRENT_TIMESTAMP WHERE ID = ?";
	private static final String RUN_DBVERSION_QRY = "UPDATE DOSBOXVERSIONS SET STATS_LASTRUN = CURRENT_TIMESTAMP, STATS_RUNS = (STATS_RUNS + 1) WHERE ID = ?";
	private static final String UPD_TEMPLATE_QRY = "UPDATE TEMPLATES SET TITLE = ?, DBVERSION_ID = ?, ISDEFAULT = ?, STATS_LASTMODIFY = CURRENT_TIMESTAMP WHERE ID = ?";
	private static final String RUN_TEMPLATE_QRY = "UPDATE TEMPLATES SET STATS_LASTRUN = CURRENT_TIMESTAMP, STATS_RUNS = (STATS_RUNS + 1) WHERE ID = ?";
	private static final String UPD_FILTER_QRY = "UPDATE FILTERS SET TITLE = ?, FILTER = ? WHERE ID = ?";
	private static final String UPD_TEMPL_NODFLT = "UPDATE TEMPLATES SET ISDEFAULT = FALSE";

	private static final String REMOVE_GAME_QRY = "DELETE FROM GAMES WHERE ID = ?";
	private static final String REMOVE_DBV_QRY = "DELETE FROM DOSBOXVERSIONS WHERE ID = ?";
	private static final String REMOVE_TEMPL_QRY = "DELETE FROM TEMPLATES WHERE ID = ?";
	private static final String REMOVE_FLTR_QRY = "DELETE FROM FILTERS WHERE ID = ?";
	private static final String REMOVE_NTVCMD_QRY_TEMPLATE = "DELETE FROM NATIVECOMMANDS WHERE GAME_ID IS NULL AND TEMPLATE_ID = ?";
	private static final String REMOVE_NTVCMD_QRY_GAME = "DELETE FROM NATIVECOMMANDS WHERE GAME_ID = ? AND TEMPLATE_ID IS NULL";
	private static final String CLEAR_LOG_QRY = "DELETE FROM LOG";
	private static final String TOGGLE_FAV_QRY = "UPDATE GAMES SET FAVORITE = NOT FAVORITE WHERE ID = ?";

	private static final String GET_VERSION = "SELECT MAJORVERSION, MINORVERSION FROM VERSION";
	private static final String UP_TO_V050_QRY = "ALTER TABLE GAMES ADD COLUMN CONFFILE VARCHAR(256);" + "ALTER TABLE GAMES ADD COLUMN CAPTURES VARCHAR(256);"
			+ "CREATE TABLE VERSION(MAJORVERSION INTEGER NOT NULL, MINORVERSION INTEGER NOT NULL);" + "INSERT INTO VERSION VALUES(0, 50);" + "UPDATE GAMES SET" + " CAPTURES = '"
			+ FileUtils.CAPTURES_DIR + "' || GAMES.ID," + " CONFFILE = '" + FileUtils.PROFILES_DIR + "' || GAMES.ID || '" + FileUtils.CONF_EXT + "';";
	private static final String UP_TO_V051_QRY = "ALTER TABLE DOSBOXVERSIONS ADD COLUMN PARAMETERS VARCHAR(256) DEFAULT '';" + "UPDATE VERSION SET MINORVERSION = 51;";
	private static final String UP_TO_V056_QRY = "ALTER TABLE GAMES ADD COLUMN LINK3 VARCHAR(256) DEFAULT '';" + "ALTER TABLE GAMES ADD COLUMN LINK4 VARCHAR(256) DEFAULT '';"
			+ "ALTER TABLE GAMES ADD COLUMN CUST1_ID INTEGER DEFAULT 0;" + "ALTER TABLE GAMES ADD COLUMN CUST2_ID INTEGER DEFAULT 0;" + "ALTER TABLE GAMES ADD COLUMN CUST3_ID INTEGER DEFAULT 0;"
			+ "ALTER TABLE GAMES ADD COLUMN CUST4_ID INTEGER DEFAULT 0;" + "ALTER TABLE GAMES ADD COLUMN CUSTOM5 VARCHAR(256) DEFAULT '';"
			+ "ALTER TABLE GAMES ADD COLUMN CUSTOM6 VARCHAR(256) DEFAULT '';" + "ALTER TABLE GAMES ADD COLUMN CUSTOM7 VARCHAR(256) DEFAULT '';"
			+ "ALTER TABLE GAMES ADD COLUMN CUSTOM8 VARCHAR(256) DEFAULT '';" + "ALTER TABLE GAMES ADD COLUMN CUSTOM9  INTEGER DEFAULT 0;" + "ALTER TABLE GAMES ADD COLUMN CUSTOM10 INTEGER DEFAULT 0;"
			+ "CREATE TABLE CUSTOM1(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY, VALUE VARCHAR(256) NOT NULL);"
			+ "CREATE TABLE CUSTOM2(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY, VALUE VARCHAR(256) NOT NULL);"
			+ "CREATE TABLE CUSTOM3(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY, VALUE VARCHAR(256) NOT NULL);"
			+ "CREATE TABLE CUSTOM4(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY, VALUE VARCHAR(256) NOT NULL);" + "INSERT INTO CUSTOM1(VALUE) VALUES('');"
			+ "INSERT INTO CUSTOM2(VALUE) VALUES('');" + "INSERT INTO CUSTOM3(VALUE) VALUES('');" + "INSERT INTO CUSTOM4(VALUE) VALUES('');" + "UPDATE VERSION SET MINORVERSION = 56;";
	private static final String UP_TO_V062_QRY = "ALTER TABLE DOSBOXVERSIONS ADD COLUMN VERSION VARCHAR(256) DEFAULT '0.72' NOT NULL;" + "UPDATE VERSION SET MINORVERSION = 62;";
	private static final String UP_TO_V065_QRY = "ALTER TABLE GAMES ADD COLUMN LINK1_TITLE VARCHAR(256) DEFAULT '';" + "ALTER TABLE GAMES ADD COLUMN LINK2_TITLE VARCHAR(256) DEFAULT '';"
			+ "ALTER TABLE GAMES ADD COLUMN LINK3_TITLE VARCHAR(256) DEFAULT '';" + "ALTER TABLE GAMES ADD COLUMN LINK4_TITLE VARCHAR(256) DEFAULT '';" + "UPDATE VERSION SET MINORVERSION = 65;";
	private static final String UP_TO_V067_QRY = "ALTER TABLE DOSBOXVERSIONS ADD COLUMN USINGCURSES BOOLEAN;" + "UPDATE VERSION SET MINORVERSION = 67;";
	private static final String UP_TO_V068_QRY = "CREATE MEMORY TABLE FILTERS(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,TITLE VARCHAR(256) NOT NULL,FILTER VARCHAR(256) NOT NULL,CONF_FILTER VARCHAR(256) NOT NULL);"
			+ "UPDATE VERSION SET MINORVERSION = 68;";
	private static final String UP_TO_V072_QRY = "ALTER TABLE DOSBOXVERSIONS ADD COLUMN CONFFILE VARCHAR(256) DEFAULT '' NOT NULL;" + "UPDATE DOSBOXVERSIONS SET CONFFILE = CONCAT(PATH, '"
			+ File.separatorChar + FileUtils.DOSBOX_CONF + "');" + "UPDATE VERSION SET MINORVERSION = 72;";
	private static final String UP_TO_V073_QRY = "ALTER TABLE GAMES ADD COLUMN ALT1 VARCHAR(256) DEFAULT '';" + "ALTER TABLE GAMES ADD COLUMN ALT1_PARAMS VARCHAR(256) DEFAULT '';"
			+ "ALTER TABLE GAMES ADD COLUMN ALT2 VARCHAR(256) DEFAULT '';" + "ALTER TABLE GAMES ADD COLUMN ALT2_PARAMS VARCHAR(256) DEFAULT '';"
			+ "ALTER TABLE GAMES ADD COLUMN LINK5 VARCHAR(256) DEFAULT '';" + "ALTER TABLE GAMES ADD COLUMN LINK6 VARCHAR(256) DEFAULT '';"
			+ "ALTER TABLE GAMES ADD COLUMN LINK7 VARCHAR(256) DEFAULT '';" + "ALTER TABLE GAMES ADD COLUMN LINK8 VARCHAR(256) DEFAULT '';"
			+ "ALTER TABLE GAMES ADD COLUMN LINK5_TITLE VARCHAR(256) DEFAULT '';" + "ALTER TABLE GAMES ADD COLUMN LINK6_TITLE VARCHAR(256) DEFAULT '';"
			+ "ALTER TABLE GAMES ADD COLUMN LINK7_TITLE VARCHAR(256) DEFAULT '';" + "ALTER TABLE GAMES ADD COLUMN LINK8_TITLE VARCHAR(256) DEFAULT '';" + "UPDATE VERSION SET MINORVERSION = 73;";
	private static final String UP_TO_V074_QRY = "ALTER TABLE DOSBOXVERSIONS ALTER COLUMN DEFAULT RENAME TO ISDEFAULT;" + "ALTER TABLE TEMPLATES ALTER COLUMN DEFAULT RENAME TO ISDEFAULT;"
			+ "UPDATE VERSION SET MINORVERSION = 74;";
	private static final String UP_TO_V075_QRY = "CREATE MEMORY TABLE NATIVECOMMANDS(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,COMMAND VARCHAR(256) NOT NULL,PARAMETERS VARCHAR(256) NOT NULL,CWD VARCHAR(256) NOT NULL,WAITFOR BOOLEAN,ORDERNR INTEGER,GAME_ID INTEGER,TEMPLATE_ID INTEGER,"
			+ "CONSTRAINT SYS_FK_180 FOREIGN KEY(GAME_ID) REFERENCES GAMES(ID),CONSTRAINT SYS_FK_181 FOREIGN KEY(TEMPLATE_ID) REFERENCES TEMPLATES(ID));" + "UPDATE VERSION SET MINORVERSION = 75;";
	private static final String UP_TO_V076_QRY = "ALTER TABLE GAMES ADD COLUMN STATS_CREATED TIMESTAMP(0) DEFAULT CURRENT_TIMESTAMP NOT NULL;"
			+ "ALTER TABLE GAMES ADD COLUMN STATS_LASTMODIFY TIMESTAMP(0);" + "ALTER TABLE GAMES ADD COLUMN STATS_LASTRUN TIMESTAMP(0);"
			+ "ALTER TABLE GAMES ADD COLUMN STATS_RUNS INTEGER DEFAULT 0 NOT NULL;" + "ALTER TABLE GAMES ADD COLUMN STATS_LASTSETUP TIMESTAMP(0);"
			+ "ALTER TABLE GAMES ADD COLUMN STATS_SETUPS INTEGER DEFAULT 0 NOT NULL;" + "ALTER TABLE DOSBOXVERSIONS ADD COLUMN STATS_CREATED TIMESTAMP(0) DEFAULT CURRENT_TIMESTAMP NOT NULL;"
			+ "ALTER TABLE DOSBOXVERSIONS ADD COLUMN STATS_LASTMODIFY TIMESTAMP(0);" + "ALTER TABLE DOSBOXVERSIONS ADD COLUMN STATS_LASTRUN TIMESTAMP(0);"
			+ "ALTER TABLE DOSBOXVERSIONS ADD COLUMN STATS_RUNS INTEGER DEFAULT 0 NOT NULL;" + "ALTER TABLE TEMPLATES ADD COLUMN STATS_CREATED TIMESTAMP(0) DEFAULT CURRENT_TIMESTAMP NOT NULL;"
			+ "ALTER TABLE TEMPLATES ADD COLUMN STATS_LASTMODIFY TIMESTAMP(0);" + "ALTER TABLE TEMPLATES ADD COLUMN STATS_LASTRUN TIMESTAMP(0);"
			+ "ALTER TABLE TEMPLATES ADD COLUMN STATS_RUNS INTEGER DEFAULT 0 NOT NULL;" + "UPDATE VERSION SET MINORVERSION = 76;";
	private static final String UP_TO_V077_QRY = "CREATE MEMORY TABLE LOG (" + "ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,"
			+ "TIME TIMESTAMP(0) DEFAULT CURRENT_TIMESTAMP NOT NULL," + "EVENT TINYINT NOT NULL, ENTITY_TYPE TINYINT NOT NULL," + "ENTITY_ID INT NOT NULL, ENTITY_TITLE VARCHAR(256) NOT NULL);"
			+ "UPDATE VERSION SET MINORVERSION = 77;";

	private static final String CREATE_INITIAL_DB = "SET WRITE_DELAY 1;" + "CREATE MEMORY TABLE VERSION(MAJORVERSION INTEGER NOT NULL,MINORVERSION INTEGER NOT NULL);"
			+ "CREATE MEMORY TABLE DEVELOPERS(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,NAME VARCHAR(256) NOT NULL);"
			+ "CREATE MEMORY TABLE PUBLISHERS(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,NAME VARCHAR(256) NOT NULL);"
			+ "CREATE MEMORY TABLE GENRES(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,NAME VARCHAR(256) NOT NULL);"
			+ "CREATE MEMORY TABLE PUBLYEARS(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,YEAR CHAR(4) NOT NULL);"
			+ "CREATE MEMORY TABLE STATUS(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,STAT VARCHAR(256) NOT NULL);"
			+ "CREATE MEMORY TABLE CUSTOM1(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,VALUE VARCHAR(256) NOT NULL);"
			+ "CREATE MEMORY TABLE CUSTOM2(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,VALUE VARCHAR(256) NOT NULL);"
			+ "CREATE MEMORY TABLE CUSTOM3(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,VALUE VARCHAR(256) NOT NULL);"
			+ "CREATE MEMORY TABLE CUSTOM4(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,VALUE VARCHAR(256) NOT NULL);"
			+ "CREATE MEMORY TABLE DOSBOXVERSIONS(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,TITLE VARCHAR(256) NOT NULL,PATH VARCHAR(256) NOT NULL,CONFFILE VARCHAR(256) NOT NULL,MULTICONF BOOLEAN,ISDEFAULT BOOLEAN,PARAMETERS VARCHAR(256) DEFAULT '',VERSION VARCHAR(256) NOT NULL,USINGCURSES BOOLEAN,STATS_CREATED TIMESTAMP(0) DEFAULT CURRENT_TIMESTAMP NOT NULL,STATS_LASTMODIFY TIMESTAMP(0),STATS_LASTRUN TIMESTAMP(0),STATS_RUNS INTEGER DEFAULT 0 NOT NULL);"
			+ "CREATE MEMORY TABLE TEMPLATES(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,TITLE VARCHAR(256) NOT NULL,DBVERSION_ID INTEGER,ISDEFAULT BOOLEAN,STATS_CREATED TIMESTAMP(0) DEFAULT CURRENT_TIMESTAMP NOT NULL,STATS_LASTMODIFY TIMESTAMP(0),STATS_LASTRUN TIMESTAMP(0),STATS_RUNS INTEGER DEFAULT 0 NOT NULL,CONSTRAINT SYS_FK_185 FOREIGN KEY(DBVERSION_ID) REFERENCES DOSBOXVERSIONS(ID));"
			+ "CREATE MEMORY TABLE GAMES(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,"
			+ "TITLE VARCHAR(256) NOT NULL,DEV_ID INTEGER,PUBL_ID INTEGER,GENRE_ID INTEGER,YEAR_ID INTEGER,STAT_ID INTEGER,NOTES LONGVARCHAR,FAVORITE BOOLEAN,"
			+ "SETUP VARCHAR(256),SETUP_PARAMS VARCHAR(256),ALT1 VARCHAR(256) DEFAULT '',ALT1_PARAMS VARCHAR(256) DEFAULT '',ALT2 VARCHAR(256) DEFAULT '',ALT2_PARAMS VARCHAR(256) DEFAULT '',"
			+ "CONFFILE VARCHAR(256),CAPTURES VARCHAR(256),DBVERSION_ID INTEGER," + "LINK1 VARCHAR(256),LINK2 VARCHAR(256),LINK3 VARCHAR(256) DEFAULT '',LINK4 VARCHAR(256) DEFAULT '',"
			+ "LINK5 VARCHAR(256) DEFAULT '',LINK6 VARCHAR(256) DEFAULT '',LINK7 VARCHAR(256) DEFAULT '',LINK8 VARCHAR(256) DEFAULT '',"
			+ "LINK1_TITLE VARCHAR(256) DEFAULT '',LINK2_TITLE VARCHAR(256) DEFAULT '',LINK3_TITLE VARCHAR(256) DEFAULT '',LINK4_TITLE VARCHAR(256) DEFAULT '',"
			+ "LINK5_TITLE VARCHAR(256) DEFAULT '',LINK6_TITLE VARCHAR(256) DEFAULT '',LINK7_TITLE VARCHAR(256) DEFAULT '',LINK8_TITLE VARCHAR(256) DEFAULT '',"
			+ "CUST1_ID INTEGER DEFAULT 0,CUST2_ID INTEGER DEFAULT 0,CUST3_ID INTEGER DEFAULT 0,CUST4_ID INTEGER DEFAULT 0,"
			+ "CUSTOM5 VARCHAR(256) DEFAULT '',CUSTOM6 VARCHAR(256) DEFAULT '',CUSTOM7 VARCHAR(256) DEFAULT '',CUSTOM8 VARCHAR(256) DEFAULT '',"
			+ "CUSTOM9 INTEGER DEFAULT 0,CUSTOM10 INTEGER DEFAULT 0," + "STATS_CREATED TIMESTAMP(0) DEFAULT CURRENT_TIMESTAMP NOT NULL,STATS_LASTMODIFY TIMESTAMP(0),"
			+ "STATS_LASTRUN TIMESTAMP(0),STATS_RUNS INTEGER DEFAULT 0 NOT NULL,STATS_LASTSETUP TIMESTAMP(0),STATS_SETUPS INTEGER DEFAULT 0 NOT NULL,"
			+ "CONSTRAINT SYS_FK_165 FOREIGN KEY(DEV_ID) REFERENCES DEVELOPERS(ID),CONSTRAINT SYS_FK_166 FOREIGN KEY(PUBL_ID) REFERENCES PUBLISHERS(ID),"
			+ "CONSTRAINT SYS_FK_167 FOREIGN KEY(GENRE_ID) REFERENCES GENRES(ID),CONSTRAINT SYS_FK_168 FOREIGN KEY(YEAR_ID) REFERENCES PUBLYEARS(ID),"
			+ "CONSTRAINT SYS_FK_169 FOREIGN KEY(DBVERSION_ID) REFERENCES DOSBOXVERSIONS(ID),CONSTRAINT SYS_FK_170 FOREIGN KEY(STAT_ID) REFERENCES STATUS(ID));"
			+ "CREATE MEMORY TABLE FILTERS(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,TITLE VARCHAR(256) NOT NULL,FILTER VARCHAR(256) NOT NULL,CONF_FILTER VARCHAR(256) NOT NULL);"
			+ "CREATE MEMORY TABLE NATIVECOMMANDS(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,COMMAND VARCHAR(256) NOT NULL,PARAMETERS VARCHAR(256) NOT NULL,CWD VARCHAR(256) NOT NULL,WAITFOR BOOLEAN,ORDERNR INTEGER,GAME_ID INTEGER,TEMPLATE_ID INTEGER,"
			+ "CONSTRAINT SYS_FK_180 FOREIGN KEY(GAME_ID) REFERENCES GAMES(ID),CONSTRAINT SYS_FK_181 FOREIGN KEY(TEMPLATE_ID) REFERENCES TEMPLATES(ID));" + "CREATE MEMORY TABLE LOG ("
			+ "ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,TIME TIMESTAMP(0) DEFAULT CURRENT_TIMESTAMP NOT NULL,"
			+ "EVENT TINYINT NOT NULL, ENTITY_TYPE TINYINT NOT NULL," + "ENTITY_ID INT NOT NULL, ENTITY_TITLE VARCHAR(256) NOT NULL);"
			+ "INSERT INTO CUSTOM1(VALUE) VALUES(''); INSERT INTO CUSTOM2(VALUE) VALUES('');" + "INSERT INTO CUSTOM3(VALUE) VALUES(''); INSERT INTO CUSTOM4(VALUE) VALUES('');"
			+ "INSERT INTO VERSION VALUES(0,77);";

	private static final String TEST_QRY = "SELECT TOP 1 ID FROM GAMES";

	private static final String CLEANUP_QRY = "DELETE FROM DEVELOPERS WHERE ID NOT IN (SELECT DISTINCT DEV_ID FROM GAMES);"
			+ "DELETE FROM PUBLISHERS WHERE ID NOT IN (SELECT DISTINCT PUBL_ID FROM GAMES);" + "DELETE FROM GENRES WHERE ID NOT IN (SELECT DISTINCT GENRE_ID FROM GAMES);"
			+ "DELETE FROM PUBLYEARS WHERE ID NOT IN (SELECT DISTINCT YEAR_ID FROM GAMES);" + "DELETE FROM STATUS WHERE ID NOT IN (SELECT DISTINCT STAT_ID FROM GAMES);"
			+ "DELETE FROM CUSTOM1 WHERE ID NOT IN (SELECT DISTINCT CUST1_ID FROM GAMES);" + "DELETE FROM CUSTOM2 WHERE ID NOT IN (SELECT DISTINCT CUST2_ID FROM GAMES);"
			+ "DELETE FROM CUSTOM3 WHERE ID NOT IN (SELECT DISTINCT CUST3_ID FROM GAMES);" + "DELETE FROM CUSTOM4 WHERE ID NOT IN (SELECT DISTINCT CUST4_ID FROM GAMES)";
	private static final String FIND_INVALID_PROFILES = GAME_LIST_QRY + " AND (GAM.CONFFILE IS NULL OR GAM.CAPTURES IS NULL)";

	private static final int COMPLEX_STRINGS = CUSTOM_LIST_QRY.length;
	private static final int SIMPLE_STRINGS = 4;

	private Database() {
		init();
	}

	private static class DatabaseHolder {
		private static Database instance = new Database();
	}

	public static Database getInstance() {
		return DatabaseHolder.instance;
	}

	public static boolean isInitializedNewDatabase() {
		return initializedNewDatabase;
	}

	private int[] getVersion() {
		try (Statement stmt = con.createStatement(); ResultSet resultset = stmt.executeQuery(GET_VERSION)) {
			resultset.next();
			return new int[] {resultset.getInt(1), resultset.getInt(2)}; // major, minor
		} catch (SQLException e) {
			return new int[] {0, 0}; // assume version < 0.50 (0.0)
		}
	}

	private void upgradeToVersion(final String query, final int minorVersion) throws SQLException {
		System.out.println(Settings.getInstance().msg("database.notice.upgrade", new Object[] {0, minorVersion}));
		try (Statement stmt = con.createStatement()) {
			for (String s: query.split(";"))
				stmt.addBatch(s);
			stmt.executeBatch();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(Settings.getInstance().msg("database.error.upgrade", new Object[] {0, minorVersion}));
		}
	}

	private void initializeIfNecessary() throws SQLException {
		try (Statement stmt = con.createStatement()) {
			stmt.executeQuery(TEST_QRY);
		} catch (SQLException emptydatabase) {
			// Probably empty database, fill it
			try (Statement stmt2 = con.createStatement()) {
				for (String s: CREATE_INITIAL_DB.split(";"))
					stmt2.addBatch(s);
				stmt2.executeBatch();
				initializedNewDatabase = true;
			} catch (SQLException e) {
				e.printStackTrace();
				throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"create initial tables"}));
			}
		}
	}

	private void upgradeIfNecessary() throws SQLException {
		int[] version = getVersion();
		if (version[0] <= 0 && version[1] < 50) {
			upgradeToVersion(UP_TO_V050_QRY, 50);
		}
		if (version[0] <= 0 && version[1] < 51) {
			upgradeToVersion(UP_TO_V051_QRY, 51);
		}
		if (version[0] <= 0 && version[1] < 56) {
			upgradeToVersion(UP_TO_V056_QRY, 56);
		}
		if (version[0] <= 0 && version[1] < 62) {
			upgradeToVersion(UP_TO_V062_QRY, 62);
		}
		if (version[0] <= 0 && version[1] < 65) {
			upgradeToVersion(UP_TO_V065_QRY, 65);
		}
		if (version[0] <= 0 && version[1] < 67) {
			upgradeToVersion(UP_TO_V067_QRY, 67);
		}
		if (version[0] <= 0 && version[1] < 68) {
			upgradeToVersion(UP_TO_V068_QRY, 68);
		}
		if (version[0] <= 0 && version[1] < 72) {
			upgradeToVersion(UP_TO_V072_QRY, 72);
		}
		if (version[0] <= 0 && version[1] < 73) {
			upgradeToVersion(UP_TO_V073_QRY, 73);
		}
		if (version[0] <= 0 && version[1] < 74) {
			upgradeToVersion(UP_TO_V074_QRY, 74);
		}
		if (version[0] <= 0 && version[1] < 75) {
			upgradeToVersion(UP_TO_V075_QRY, 75);
		}
		if (version[0] <= 0 && version[1] < 76) {
			upgradeToVersion(UP_TO_V076_QRY, 76);
		}
		if (version[0] <= 0 && version[1] < 77) {
			upgradeToVersion(UP_TO_V077_QRY, 77);
		}
	}

	private void init() {
		Settings settings = Settings.getInstance();
		System.out.println(settings.msg("database.notice.startup"));
		String connString = settings.getSettings().getValue("database", "connectionstring");
		if (connString.contains("file:")) {
			// Some magic on the connection string
			int start = connString.indexOf("file:") + 5; // skip 'file:'
			int end = connString.indexOf(';', start);
			if (end == -1) {
				end = connString.length();
			}
			String databasefile = FileUtils.getDatabaseFile(connString).getPath();
			connString = connString.substring(0, start) + databasefile + connString.substring(end);
		}
		try {
			// Register the JDBC driver for dBase
			Class.forName("org.hsqldb.jdbcDriver");
			con = DriverManager.getConnection(connString, settings.getSettings().getValue("database", "username"), settings.getSettings().getValue("database", "password"));
			initializeIfNecessary();
			upgradeIfNecessary();
		} catch (SQLException e) {
			Shell shell = new Shell();
			GeneralPurposeDialogs.fatalMessage(shell, settings.msg("database.error.initconnection", new Object[] {StringRelatedUtils.toString(e)}), e);
			try {
				if (con != null)
					con.close();
			} catch (SQLException exc) {
				GeneralPurposeDialogs.warningMessage(shell, settings.msg("database.error.connectionclose", new Object[] {StringRelatedUtils.toString(exc)}), exc);
			}
			throw new RuntimeException();
		} catch (ClassNotFoundException e) {
			Shell shell = new Shell();
			GeneralPurposeDialogs.fatalMessage(shell, settings.msg("database.error.registerdriver", new Object[] {StringRelatedUtils.toString(e)}), e);
			throw new RuntimeException();
		}
	}

	private int identity() throws SQLException {
		try (Statement stmt = con.createStatement(); ResultSet resultset = stmt.executeQuery(GET_IDENTITY_QRY)) {
			resultset.next();
			return resultset.getInt(1);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"get identity"}));
		}
	}

	private int addSomething(final String query, final String objectToAdd) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement(query)) {
			stmt.setString(1, objectToAdd);
			stmt.executeUpdate();
			return identity();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"add " + objectToAdd}));
		}
	}

	private void removeSomething(final String query, final String objectToRemove, final int objectId) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement(query)) {
			stmt.setInt(1, objectId);
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"remove " + objectToRemove}));
		}
	}

	private List<KeyValuePair> readKeyValuePairList(final String query) throws SQLException {
		try (Statement stmt = con.createStatement(); ResultSet resultset = stmt.executeQuery(query)) {
			List<KeyValuePair> customList = new ArrayList<KeyValuePair>();
			while (resultset.next())
				customList.add(new KeyValuePair(resultset.getInt(1), resultset.getString(2)));
			return customList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"read KeyValuePairs"}));
		}
	}

	public void shutdown() throws SQLException {
		System.out.println(Settings.getInstance().msg("database.notice.shutdown"));
		try (Statement stmt = con.createStatement()) {
			stmt.execute("SHUTDOWN");
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(Settings.getInstance().msg("database.error.shutdown"));
		} finally {
			con.close();
		}
	}

	public List<Profile> readProfilesList(final String orderingClause, final String filterClause) throws SQLException {
		StringBuffer qry = new StringBuffer(GAME_LIST_QRY);
		if (filterClause != null && filterClause.length() > 0)
			qry.append(" AND (").append(filterClause).append(")");
		qry.append(orderingClause);

		try (Statement stmt = con.createStatement(); ResultSet rset = stmt.executeQuery(qry.toString())) {
			List<Profile> profilesList = new ArrayList<Profile>();
			while (rset.next())
				profilesList.add(new Profile(rset.getInt(1), rset.getString(2), rset.getString(3), rset.getString(4), rset.getString(5), rset.getString(6), rset.getString(7), rset.getString(8),
						rset.getBoolean(9), new String[] {rset.getString(10), rset.getString(12), rset.getString(14)}, new String[] {rset.getString(11), rset.getString(13), rset.getString(15)},
						rset.getString(16), rset.getString(17), rset.getInt(18),
						new String[] {rset.getString(19), rset.getString(20), rset.getString(21), rset.getString(22), rset.getString(23), rset.getString(24), rset.getString(25), rset.getString(26)},
						new String[] {rset.getString(27), rset.getString(28), rset.getString(29), rset.getString(30), rset.getString(31), rset.getString(32), rset.getString(33), rset.getString(34)},
						new String[] {rset.getString(35), rset.getString(36), rset.getString(37), rset.getString(38), rset.getString(39), rset.getString(40), rset.getString(41), rset.getString(42)},
						new int[] {rset.getInt(43), rset.getInt(44)}, rset.getTimestamp(45), rset.getTimestamp(46), rset.getTimestamp(47), rset.getTimestamp(48), rset.getInt(49), rset.getInt(50)));
			return profilesList;
		} catch (SQLException e) {
			if (orderingClause.length() > 0)
				e.printStackTrace();
			throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"read profiles"}));
		}
	}

	public List<KeyValuePair> readDevelopersList() throws SQLException {
		return readKeyValuePairList(DEV_LIST_QRY);
	}

	public List<KeyValuePair> readPublishersList() throws SQLException {
		return readKeyValuePairList(PUBL_LIST_QRY);
	}

	public List<KeyValuePair> readGenresList() throws SQLException {
		return readKeyValuePairList(GENRE_LIST_QRY);
	}

	public List<KeyValuePair> readYearsList() throws SQLException {
		return readKeyValuePairList(PUBLYEAR_LIST_QRY);
	}

	public List<KeyValuePair> readStatusList() throws SQLException {
		return readKeyValuePairList(STATUS_LIST_QRY);
	}

	public List<KeyValuePair> readCustomList(final int index) throws SQLException {
		return readKeyValuePairList(CUSTOM_LIST_QRY[index]);
	}

	public List<DosboxVersion> readDosboxVersionsList() throws SQLException {
		try (Statement stmt = con.createStatement(); ResultSet rs = stmt.executeQuery(DBVERS_LIST_QRY);) {
			List<DosboxVersion> dbversionsList = new ArrayList<DosboxVersion>();
			while (rs.next())
				dbversionsList.add(new DosboxVersion(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getBoolean(5), rs.getBoolean(6), rs.getBoolean(7), rs.getString(8),
						rs.getString(9), rs.getTimestamp(10), rs.getTimestamp(11), rs.getTimestamp(12), rs.getInt(13)));
			return dbversionsList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"read dosboxversions"}));
		}
	}

	public List<Template> readTemplatesList() throws SQLException {
		try (Statement stmt = con.createStatement(); ResultSet rs = stmt.executeQuery(TEMPL_LIST_QRY);) {
			List<Template> templatesList = new ArrayList<Template>();
			while (rs.next())
				templatesList.add(new Template(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getBoolean(4), rs.getTimestamp(5), rs.getTimestamp(6), rs.getTimestamp(7), rs.getInt(8)));
			return templatesList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"read templates"}));
		}
	}

	public List<Filter> readFiltersList() throws SQLException {
		try (Statement stmt = con.createStatement(); ResultSet resultset = stmt.executeQuery(FLTRS_LIST_QRY);) {
			List<Filter> filtersList = new ArrayList<Filter>();
			while (resultset.next())
				filtersList.add(new Filter(resultset.getInt(1), resultset.getString(2), resultset.getString(3)));
			return filtersList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"read filters"}));
		}
	}

	public List<LogEntry> readLogEntries(final String whereClause, final String orderByClause) throws SQLException {
		try (Statement stmt = con.createStatement(); ResultSet resultset = stmt.executeQuery(LOG_LIST_QRY + whereClause + orderByClause);) {
			List<LogEntry> logEntriesList = new ArrayList<LogEntry>();
			while (resultset.next())
				logEntriesList.add(new LogEntry(resultset.getInt(1), resultset.getTimestamp(2), resultset.getByte(3), resultset.getByte(4), resultset.getInt(5), resultset.getString(6)));
			return logEntriesList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"read log entries"}));
		}
	}

	public List<NativeCommand> readNativeCommandsList(final int gameId, final int templateId) throws SQLException {
		List<NativeCommand> nativeCommandsList = new ArrayList<NativeCommand>();
		if ((gameId != -1) || (templateId != -1)) {
			try (PreparedStatement stmt = con.prepareStatement(gameId != -1 ? NTVCMD_LIST_QRY_GAME: NTVCMD_LIST_QRY_TEMPLATE)) {
				stmt.setInt(1, gameId != -1 ? gameId: templateId);
				try (ResultSet resultset = stmt.executeQuery()) {
					while (resultset.next())
						nativeCommandsList.add(
							new NativeCommand(new File(resultset.getString(1)), resultset.getString(2), new File(resultset.getString(3)), resultset.getBoolean(4), resultset.getInt(5)));
				}
			} catch (SQLException e) {
				e.printStackTrace();
				throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"read native commands"}));
			}
		}
		NativeCommand.insertDosboxCommand(nativeCommandsList);
		return nativeCommandsList;
	}

	public void saveNativeCommands(final List<NativeCommand> commands, final int gameId, final int templateId) throws SQLException {
		removeNativeCommands(gameId, templateId);

		for (int i = 0; i < commands.size(); i++) {
			NativeCommand cmd = commands.get(i);
			if (cmd.getCommand() != null) {
				try (PreparedStatement stmt = con.prepareStatement(ADD_NATIVECOMMAND_QRY)) {
					stmt.setString(1, cmd.getCommand().getPath());
					stmt.setString(2, cmd.getParameters());
					stmt.setString(3, cmd.getCwd().getPath());
					stmt.setBoolean(4, cmd.isWaitFor());
					stmt.setInt(5, i);
					if (gameId != -1)
						stmt.setInt(6, gameId);
					else
						stmt.setNull(6, java.sql.Types.INTEGER);
					if (templateId != -1)
						stmt.setInt(7, templateId);
					else
						stmt.setNull(7, java.sql.Types.INTEGER);
					stmt.executeUpdate();
				} catch (SQLException e) {
					e.printStackTrace();
					throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"save native commands"}));
				}
			}
		}
	}

	public void removeNativeCommands(final int gameId, final int templateId) throws SQLException {
		if (gameId != -1) {
			try (PreparedStatement stmt = con.prepareStatement(REMOVE_NTVCMD_QRY_GAME)) {
				stmt.setInt(1, gameId);
				stmt.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
				throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"remove native commands"}));
			}
		} else if (templateId != -1) {
			try (PreparedStatement stmt = con.prepareStatement(REMOVE_NTVCMD_QRY_TEMPLATE)) {
				stmt.setInt(1, templateId);
				stmt.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
				throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"remove native commands"}));
			}
		}
	}

	public void removeProfile(final Profile profile) throws SQLException {
		removeSomething(REMOVE_GAME_QRY, "profile", profile.getId());
		addLogEntry(LogEntry.Event.REMOVE, LogEntry.EntityType.PROFILE, profile.getId(), profile.getTitle());
	}

	public void removeDosboxVersion(final DosboxVersion dbversion) throws SQLException {
		try {
			removeSomething(REMOVE_DBV_QRY, "dosboxversion", dbversion.getId());
			addLogEntry(LogEntry.Event.REMOVE, LogEntry.EntityType.DOSBOXVERSION, dbversion.getId(), dbversion.getTitle());
		} catch (SQLException e) {
			int amount = 0;
			StringBuffer usageList = new StringBuffer();
			try (PreparedStatement stmt = con.prepareStatement(DBV_USE_QRY)) {
				stmt.setInt(1, dbversion.getId());
				stmt.setInt(2, dbversion.getId());
				try (ResultSet resultset = stmt.executeQuery()) {
					while (resultset.next()) {
						if (amount++ < 10) {
							if (amount > 1)
								usageList.append(", ");
							usageList.append(resultset.getString(1));
						}
					}
				}
			}
			throw new SQLException(Settings.getInstance().msg("general.error.profilesandtemplatesusingdbversion", new Object[] {StringRelatedUtils.toString(e), amount, usageList}));
		}
	}

	public void removeTemplate(final Template template) throws SQLException {
		removeSomething(REMOVE_TEMPL_QRY, "template", template.getId());
		addLogEntry(LogEntry.Event.REMOVE, LogEntry.EntityType.TEMPLATE, template.getId(), template.getTitle());
	}

	public void removeFilter(final Filter filter) throws SQLException {
		removeSomething(REMOVE_FLTR_QRY, "filter", filter.getId());
		addLogEntry(LogEntry.Event.REMOVE, LogEntry.EntityType.FILTER, filter.getId(), filter.getTitle());
	}

	public Profile addOrEditProfile(final String title, final String developer, final String publisher, final String genre, final String year, final String status, final String notes,
			final boolean favorite, final String[] setup, final String[] setupParams, final int devId, final int publId, final int genId, final int yrId, final int statId, final int dbversionId,
			final String[] link, final String[] linkTitle, final String[] sCust, final int[] iCust, final int[] custId, final int profileId) throws SQLException {
		try (PreparedStatement pstmt = con.prepareStatement(profileId != -1 ? UPD_GAME_QRY: ADD_GAME_QRY)) {
			pstmt.setString(1, title);
			pstmt.setInt(2, devId == -1 ? addSomething(ADD_DEVELOPER_QRY, developer): devId);
			pstmt.setInt(3, publId == -1 ? addSomething(ADD_PUBLISHER_QRY, publisher): publId);
			pstmt.setInt(4, genId == -1 ? addSomething(ADD_GENRE_QRY, genre): genId);
			pstmt.setInt(5, yrId == -1 ? addSomething(ADD_YEAR_QRY, year): yrId);
			pstmt.setInt(6, statId == -1 ? addSomething(ADD_STATUS_QRY, status): statId);
			pstmt.setString(7, notes);
			pstmt.setBoolean(8, favorite);
			for (int i = 0; i < setup.length; i++) {
				pstmt.setString(i * 2 + 9, setup[i]);
				pstmt.setString(i * 2 + 10, setupParams[i]);
			}
			pstmt.setInt(15, dbversionId);
			for (int i = 0; i < link.length; i++)
				pstmt.setString(i + 16, link[i]);
			for (int i = 0; i < linkTitle.length; i++)
				pstmt.setString(i + 16 + link.length, linkTitle[i]);
			for (int i = 0; i < custId.length; i++)
				pstmt.setInt(i + 16 + link.length + linkTitle.length, custId[i] == -1 ? addSomething(ADD_CUSTOM_QRY[i], sCust[i]): custId[i]);
			for (int i = 0; i < SIMPLE_STRINGS; i++)
				pstmt.setString(i + 16 + link.length + linkTitle.length + custId.length, sCust[i + custId.length]);
			for (int i = 0; i < iCust.length; i++)
				pstmt.setInt(i + 16 + link.length + linkTitle.length + custId.length + SIMPLE_STRINGS, iCust[i]);
			if (profileId != -1)
				pstmt.setInt(16 + link.length + linkTitle.length + custId.length + SIMPLE_STRINGS + iCust.length, profileId);
			pstmt.executeUpdate();
			int newProfileId = profileId != -1 ? profileId: identity();
			addLogEntry(profileId != -1 ? LogEntry.Event.EDIT: LogEntry.Event.ADD, LogEntry.EntityType.PROFILE, newProfileId, title);
			return Profile.findById(readProfilesList(StringUtils.EMPTY, StringUtils.EMPTY), newProfileId);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"add/edit profile"}));
		}
	}

	public synchronized Profile addOrEditProfile(final String title, final String developer, final String publisher, final String genre, final String year, final String status, final String notes,
			final boolean favorite, final String[] setup, final String[] setupParams, final int dbversionId, final String[] link, final String[] linkTitle, final String[] sCust, final int[] iCust,
			final int profileId) throws SQLException {
		int devId = KeyValuePair.findIdByValue(readDevelopersList(), developer);
		int publId = KeyValuePair.findIdByValue(readPublishersList(), publisher);
		int genId = KeyValuePair.findIdByValue(readGenresList(), genre);
		int yrId = KeyValuePair.findIdByValue(readYearsList(), year);
		int statId = KeyValuePair.findIdByValue(readStatusList(), status);
		int[] customIds = new int[COMPLEX_STRINGS];
		for (int i = 0; i < COMPLEX_STRINGS; i++)
			customIds[i] = KeyValuePair.findIdByValue(readCustomList(i), sCust[i]);
		return addOrEditProfile(title, developer, publisher, genre, year, status, notes, favorite, setup, setupParams, devId, publId, genId, yrId, statId, dbversionId, link, linkTitle, sCust, iCust,
			customIds, profileId);
	}

	public Profile updateProfileConf(final String confFile, final String captures, final int profileId) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement(UPD_GAME_CONF_QRY)) {
			stmt.setString(1, confFile);
			stmt.setString(2, captures);
			stmt.setInt(3, profileId);
			stmt.executeUpdate();
			return Profile.findById(readProfilesList(StringUtils.EMPTY, StringUtils.EMPTY), profileId);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"update profile conf"}));
		}
	}

	public Profile updateProfileSetupAndLinks(final String[] setup, final String[] links, final int profileId) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement(UPD_STPLNKS_QRY)) {
			for (int i = 0; i < setup.length; i++)
				stmt.setString(i + 1, setup[i]);
			for (int i = 0; i < links.length; i++)
				stmt.setString(i + 4, links[i]);
			stmt.setInt(4 + links.length, profileId);
			stmt.executeUpdate();
			return Profile.findById(readProfilesList(StringUtils.EMPTY, StringUtils.EMPTY), profileId);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"update profile setup and links"}));
		}
	}

	public void toggleFavorite(final int profileId) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement(TOGGLE_FAV_QRY)) {
			stmt.setInt(1, profileId);
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"toggle favorite"}));
		}
	}

	public Profile runProfile(final Profile profile) throws SQLException {
		try (PreparedStatement pstmt = con.prepareStatement(RUN_GAME_QRY)) {
			pstmt.setInt(1, profile.getId());
			pstmt.executeUpdate();
			addLogEntry(LogEntry.Event.RUN, LogEntry.EntityType.PROFILE, profile.getId(), profile.getTitle());
			return Profile.findById(readProfilesList(StringUtils.EMPTY, StringUtils.EMPTY), profile.getId());
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"run profile"}));
		}
	}

	public Profile setupProfile(final Profile profile) throws SQLException {
		try (PreparedStatement pstmt = con.prepareStatement(SETUP_GAME_QRY)) {
			pstmt.setInt(1, profile.getId());
			pstmt.executeUpdate();
			addLogEntry(LogEntry.Event.SETUP, LogEntry.EntityType.PROFILE, profile.getId(), profile.getTitle());
			return Profile.findById(readProfilesList(StringUtils.EMPTY, StringUtils.EMPTY), profile.getId());
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"setup profile"}));
		}
	}

	public Profile duplicateProfile(final Profile profile) throws SQLException {
		try (PreparedStatement pstmt = con.prepareStatement(DUPL_GAME_QRY)) {
			pstmt.setInt(1, profile.getId());
			pstmt.executeUpdate();
			int newProfileId = identity();
			addLogEntry(LogEntry.Event.DUPLICATE, LogEntry.EntityType.PROFILE, profile.getId(), profile.getTitle());
			addLogEntry(LogEntry.Event.ADD, LogEntry.EntityType.PROFILE, newProfileId, profile.getTitle());
			return Profile.findById(readProfilesList(StringUtils.EMPTY, StringUtils.EMPTY), newProfileId);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"duplicate profile"}));
		}
	}

	public Template duplicateTemplate(final Template template) throws SQLException {
		try (PreparedStatement pstmt = con.prepareStatement(DUPL_TEMPLATE_QRY)) {
			pstmt.setInt(1, template.getId());
			pstmt.executeUpdate();
			int newTemplateId = identity();
			addLogEntry(LogEntry.Event.DUPLICATE, LogEntry.EntityType.TEMPLATE, template.getId(), template.getTitle());
			addLogEntry(LogEntry.Event.ADD, LogEntry.EntityType.TEMPLATE, newTemplateId, template.getTitle());
			return Template.findById(readTemplatesList(), newTemplateId);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"duplicate template"}));
		}
	}

	public DosboxVersion addOrEditDosboxVersion(final String title, final String path, final String conf, final boolean multiConfig, final boolean usingCurses, final boolean defaultVersion,
			final String parameters, final String version, final int dbversionId) throws SQLException {
		try (Statement stmt = con.createStatement(); PreparedStatement pstmt = con.prepareStatement(dbversionId != -1 ? UPD_DBVERSION_QRY: ADD_DBVERSION_QRY)) {
			if (defaultVersion)
				stmt.executeUpdate(UPD_DBV_NODEFAULT);
			pstmt.setString(1, title);
			pstmt.setString(2, path);
			pstmt.setString(3, conf);
			pstmt.setBoolean(4, multiConfig);
			pstmt.setBoolean(5, usingCurses);
			pstmt.setBoolean(6, defaultVersion);
			pstmt.setString(7, parameters);
			pstmt.setString(8, version);
			if (dbversionId != -1)
				pstmt.setInt(9, dbversionId);
			pstmt.executeUpdate();
			int newDbversionId = dbversionId != -1 ? dbversionId: identity();
			addLogEntry(dbversionId != -1 ? LogEntry.Event.EDIT: LogEntry.Event.ADD, LogEntry.EntityType.DOSBOXVERSION, newDbversionId, title);
			return DosboxVersion.findById(readDosboxVersionsList(), newDbversionId);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"add/edit dosboxversion"}));
		}
	}

	public void runDosboxVersion(final DosboxVersion dbversion) throws SQLException {
		try (PreparedStatement pstmt = con.prepareStatement(RUN_DBVERSION_QRY)) {
			pstmt.setInt(1, dbversion.getId());
			pstmt.executeUpdate();
			addLogEntry(LogEntry.Event.RUN, LogEntry.EntityType.DOSBOXVERSION, dbversion.getId(), dbversion.getTitle());
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"run dosboxversion"}));
		}
	}

	public Template addOrEditTemplate(final String title, final int dbversionId, final boolean defaultVersion, final int templateId) throws SQLException {
		try (Statement stmt = con.createStatement(); PreparedStatement pstmt = con.prepareStatement(templateId != -1 ? UPD_TEMPLATE_QRY: ADD_TEMPLATE_QRY)) {
			if (defaultVersion)
				stmt.executeUpdate(UPD_TEMPL_NODFLT);
			pstmt.setString(1, title);
			pstmt.setInt(2, dbversionId);
			pstmt.setBoolean(3, defaultVersion);
			if (templateId != -1)
				pstmt.setInt(4, templateId);
			pstmt.executeUpdate();
			int newTemplateId = templateId != -1 ? templateId: identity();
			addLogEntry(templateId != -1 ? LogEntry.Event.EDIT: LogEntry.Event.ADD, LogEntry.EntityType.TEMPLATE, newTemplateId, title);
			return Template.findById(readTemplatesList(), newTemplateId);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"add/edit template"}));
		}
	}

	public void runTemplate(final Template template) throws SQLException {
		try (PreparedStatement pstmt = con.prepareStatement(RUN_TEMPLATE_QRY)) {
			pstmt.setInt(1, template.getId());
			pstmt.executeUpdate();
			addLogEntry(LogEntry.Event.RUN, LogEntry.EntityType.TEMPLATE, template.getId(), template.getTitle());
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"run template"}));
		}
	}

	public int addOrEditFilter(final String title, final String filter, final int filterId) throws SQLException {
		try (PreparedStatement pstmt = con.prepareStatement(filterId != -1 ? UPD_FILTER_QRY: ADD_FILTER_QRY)) {
			pstmt.setString(1, title);
			pstmt.setString(2, filter);
			if (filterId != -1)
				pstmt.setInt(3, filterId);
			pstmt.executeUpdate();
			int newFilterId = filterId != -1 ? filterId: identity();
			addLogEntry(filterId != -1 ? LogEntry.Event.EDIT: LogEntry.Event.ADD, LogEntry.EntityType.FILTER, newFilterId, title);
			return newFilterId;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"add/edit filter"}));
		}
	}

	public void addLogEntry(final LogEntry.Event event, final LogEntry.EntityType entityType, final int entityId, final String entityTitle) throws SQLException {
		if (Settings.getInstance().getSettings().getBooleanValue("log", "enabled")) {
			try (PreparedStatement pstmt = con.prepareStatement(ADD_LOG_QRY)) {
				pstmt.setByte(1, (byte)event.ordinal());
				pstmt.setByte(2, (byte)entityType.ordinal());
				pstmt.setInt(3, entityId);
				pstmt.setString(4, entityTitle);
				pstmt.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
				throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"add log entry"}));
			}
		}
	}
	
	public void clearLog() throws SQLException {
		try (Statement stmt = con.createStatement(); ResultSet resultset = stmt.executeQuery(CLEAR_LOG_QRY)) {
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"clear log"}));
		}
	}

	public void startTransaction() {
		try {
			con.setAutoCommit(false);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void rollbackTransaction() throws SQLException {
		con.rollback();
	}

	public void commitTransaction() throws SQLException {
		con.commit();
	}

	public void finishTransaction() {
		try {
			con.setAutoCommit(true);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public int cleanup() throws SQLException {
		try (Statement stmt = con.createStatement()) {
			for (String s: CLEANUP_QRY.split(";"))
				stmt.addBatch(s);
			int[] results = stmt.executeBatch();
			int result = 0;
			for (int uc: results)
				result += uc;
			return result;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"cleanup"}));
		}
	}

	public List<Profile> findInvalidProfiles() throws SQLException {
		try (Statement stmt = con.createStatement(); ResultSet rset = stmt.executeQuery(FIND_INVALID_PROFILES)) {
			List<Profile> profilesList = new ArrayList<Profile>();
			while (rset.next())
				profilesList.add(new Profile(rset.getInt(1), rset.getString(2), rset.getString(3), rset.getString(4), rset.getString(5), rset.getString(6), rset.getString(7), rset.getString(8),
						rset.getBoolean(9), new String[] {rset.getString(10), rset.getString(12), rset.getString(14)}, new String[] {rset.getString(11), rset.getString(13), rset.getString(15)},
						rset.getString(16), rset.getString(17), rset.getInt(18),
						new String[] {rset.getString(19), rset.getString(20), rset.getString(21), rset.getString(22), rset.getString(23), rset.getString(24), rset.getString(25), rset.getString(26)},
						new String[] {rset.getString(27), rset.getString(28), rset.getString(29), rset.getString(30), rset.getString(31), rset.getString(32), rset.getString(33), rset.getString(34)},
						new String[] {rset.getString(35), rset.getString(36), rset.getString(37), rset.getString(38), rset.getString(39), rset.getString(40), rset.getString(41), rset.getString(42)},
						new int[] {rset.getInt(43), rset.getInt(44)}, rset.getTimestamp(45), rset.getTimestamp(46), rset.getTimestamp(47), rset.getTimestamp(48), rset.getInt(49), rset.getInt(50)));
			return profilesList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(Settings.getInstance().msg("database.error.query", new Object[] {"remove invalid profiles"}));
		}
	}
}
