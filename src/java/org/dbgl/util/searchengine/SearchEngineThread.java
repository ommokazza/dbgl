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
package org.dbgl.util.searchengine;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;
import org.dbgl.db.Database;
import org.dbgl.interfaces.Configurable;
import org.dbgl.model.ExpProfile;
import org.dbgl.model.SearchEngineImageInformation;
import org.dbgl.model.WebProfile;
import org.dbgl.model.Profile;
import org.dbgl.model.SearchEngineImageInformation.SearchEngineImageType;
import org.dbgl.model.conf.Settings;
import org.dbgl.swtdesigner.SWTImageManager;
import org.dbgl.util.FileUtils;
import org.dbgl.util.StringRelatedUtils;


public final class SearchEngineThread extends Thread {

	private WebSearchEngine engine;
	private List<Configurable> profiles;
	private String title;

	private final Text log;
	private final ProgressBar progressBar;
	private final Label profileLabel;
	private final Display display;
	private final String lineDelimiter;

	public SearchEngineThread(final WebSearchEngine engine, final List<Configurable> profs, final Text log, final ProgressBar progressBar, final Label profileLabel) {
		this.engine = engine;
		this.profiles = profs;
		this.log = log;
		this.lineDelimiter = log.getLineDelimiter();
		this.progressBar = progressBar;
		this.profileLabel = profileLabel;
		this.display = log.getShell().getDisplay();
	}

	public void run() {
		Database dbase = Database.getInstance();
		Settings settings = Settings.getInstance();
		final StringBuffer displayedLog = new StringBuffer();
		final StringBuffer messageLog = new StringBuffer();

		while (true) {
			try {
				Profile prof;
				synchronized (profiles) {
					prof = (ExpProfile)profiles.remove(0);
				}
				title = prof.getTitle();
				try {
					java.util.List<WebProfile> webGamesList = engine.getEntries(title, settings.getSettings().getValues(engine.getSimpleName(), "platform_filter"));

					if (webGamesList.size() > 1) {
						int firstMatch = WebSearchEngine.getEntryFirstExactMatchIndex(title, webGamesList);
						if (firstMatch != -1) {
							webGamesList = webGamesList.subList(firstMatch, firstMatch + 1);
						}
					}

					if (webGamesList.size() == 1) {
						WebProfile thisGame = engine.getEntryDetailedInformation(webGamesList.get(0));
						synchronized (this) {
							String title = settings.getSettings().getBooleanValue(engine.getSimpleName(), "set_title") ? thisGame.getTitle(): prof.getTitle();
							String dev = settings.getSettings().getBooleanValue(engine.getSimpleName(), "set_developer") ? thisGame.getDeveloperName(): prof.getDeveloperName();
							String publ = settings.getSettings().getBooleanValue(engine.getSimpleName(), "set_publisher") ? thisGame.getPublisherName(): prof.getPublisherName();
							String year = settings.getSettings().getBooleanValue(engine.getSimpleName(), "set_year") ? thisGame.getYear(): prof.getYear();
							String genre = settings.getSettings().getBooleanValue(engine.getSimpleName(), "set_genre") ? thisGame.getGenre(): prof.getGenre();
							String link1 = settings.getSettings().getBooleanValue(engine.getSimpleName(), "set_link") ? thisGame.getUrl(): prof.getLink(0);
							String[] links = new String[] {link1, prof.getLink(1), prof.getLink(2), prof.getLink(3), prof.getLink(4), prof.getLink(5), prof.getLink(6), prof.getLink(7)};
							String linkTitle1 = settings.getSettings().getBooleanValue(engine.getSimpleName(), "set_link")
									? settings.msg("dialog.profile.searchengine.link.maininfo", new String[] {engine.getName()}): prof.getLinkTitle(0);
							String[] linkTitles = new String[] {linkTitle1, prof.getLinkTitle(1), prof.getLinkTitle(2), prof.getLinkTitle(3), prof.getLinkTitle(4), prof.getLinkTitle(5),
									prof.getLinkTitle(6), prof.getLinkTitle(7)};
							StringBuffer notes = new StringBuffer(prof.getNotes());
							String p = thisGame.getNotes().replaceAll("\n", lineDelimiter);
							if ((settings.getSettings().getBooleanValue(engine.getSimpleName(), "set_description")) && (!prof.getNotes().endsWith(p))) {
								if (notes.length() > 0) {
									notes.append(lineDelimiter + lineDelimiter);
								}
								notes.append(p);
							}
							int rank = settings.getSettings().getBooleanValue(engine.getSimpleName(), "set_rank") ? thisGame.getRank(): prof.getCustomInt(0);
							int[] customInts = new int[] {rank, prof.getCustomInt(1)};
							dbase.addOrEditProfile(title, dev, publ, genre, year, prof.getStatus(), notes.toString(), prof.isDefault(), prof.getSetup(), prof.getSetupParameters(),
								prof.getDbversionId(), links, linkTitles, prof.getCustomStrings(), customInts, prof.getId());
						}

						boolean forceAllRegionsCoverArt = settings.getSettings().getBooleanValue(engine.getSimpleName(), "force_all_regions_coverart");
						SearchEngineImageInformation[] imageInformation = engine.getEntryImages(thisGame, settings.getSettings().getIntValue(engine.getSimpleName(), "multi_max_coverart"),
							settings.getSettings().getIntValue(engine.getSimpleName(), "multi_max_screenshot"), forceAllRegionsCoverArt);
						for (int i = 0; i < imageInformation.length; i++) {
							String description = FileUtils.fileSystemSafeWebImages(imageInformation[i].description);
							File file;
							if (imageInformation[i].type == SearchEngineImageType.CoverArt) {
								String filename = settings.msg("dialog.profile.mobygames.coverartfilename", new Object[] {i, description});
								file = new File(prof.getCanonicalCaptures(), filename + ".jpg");
							} else {
								String filename = settings.msg("dialog.profile.mobygames.screenshotfilename", new Object[] {i, description});
								file = new File(prof.getCanonicalCaptures(), filename + ".png");
							}
							if (!FileUtils.isExistingFile(file)) {
								SWTImageManager.save(thisGame.getWebImage(i), file.getPath());
							} else {
								messageLog.append(title).append(": ").append(settings.msg("dialog.profile.error.imagealreadyexists", new String[] {file.getPath(), engine.getName()})).append('\n');
							}
						}
					} else if (webGamesList.size() == 0) {
						messageLog.append(title).append(": ").append(settings.msg("general.notice.searchenginenoresults", new String[] {engine.getName(), title})).append('\n');
					} else {
						messageLog.append(title).append(": ").append(settings.msg("dialog.multiprofile.notice.titlenotunique", new String[] {engine.getName(), title})).append('\n');
					}
				} catch (SQLException e) {
					messageLog.append(StringRelatedUtils.toString(e)).append('\n');
				}
			} catch (IOException e) {
				messageLog.append(title).append(": ").append(settings.msg("general.error.retrieveinfosearchengine", new String[] {engine.getName(), title, e.toString()})).append('\n');
			} catch (IndexOutOfBoundsException iobe) {
				return; // queue empty, we're done
			}

			display.asyncExec(new Runnable() {
				public void run() {
					if (profileLabel.isDisposed() || log.isDisposed() || progressBar.isDisposed())
						return;
					profileLabel.setText(Settings.getInstance().msg("dialog.multiprofile.updating", new Object[] {title}));
					profileLabel.pack();
					if (messageLog.length() > displayedLog.length()) {
						String newOutput = messageLog.substring(displayedLog.length());
						log.append(newOutput);
						displayedLog.append(newOutput);
					}
					progressBar.setSelection(progressBar.getSelection() + 1);
				}
			});
		}

	}
}
