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

import java.io.IOException;
import java.util.Comparator;
import org.apache.commons.lang3.StringUtils;
import org.dbgl.swtdesigner.SWTImageManager;
import org.eclipse.swt.graphics.ImageData;
import org.w3c.dom.Element;


public class WebProfile implements Comparable<WebProfile> {

	private String title;
	private String platform;
	private String year;
	private String url;
	private String developerName;
	private String publisherName;
	private String genre;
	private String notes;
	private int rank;
	private String coreGameCoverUrl;
	private SearchEngineImageInformation[] webImages;
	private Element xmlElementWithAllImages;

	public WebProfile() {}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getPlatform() {
		return platform;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	public String getYear() {
		return year;
	}

	public void setYear(String year) {
		this.year = year;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getScreenshotsUrl() {
		return url + "/screenshots";
	}

	public String getCoverArtUrl() {
		return url + "/cover-art";
	}

	public String getCoreGameCoverUrl() {
		return coreGameCoverUrl;
	}

	public String getCoreGameCoverUrlWithoutPathPrefix() {
		int index = StringUtils.ordinalIndexOf(coreGameCoverUrl, "/", 3);
		if (index > 0)
			return coreGameCoverUrl.substring(index + 1);
		return coreGameCoverUrl;
	}

	public void setCoreGameCoverUrl(String coreGameCoverUrl) {
		this.coreGameCoverUrl = coreGameCoverUrl;
	}

	public ImageData getWebImage(int i) throws IOException {
		if (webImages[i].data == null)
			webImages[i].data = SWTImageManager.getImageData(webImages[i].url);
		return webImages[i].data;
	}

	public SearchEngineImageInformation[] getWebImages() {
		return webImages.clone();
	}

	public void setWebImages(SearchEngineImageInformation[] webImages) {
		this.webImages = webImages.clone();
	}

	public String getDeveloperName() {
		return developerName;
	}

	public void setDeveloperName(String developerName) {
		this.developerName = developerName;
	}

	public String getPublisherName() {
		return publisherName;
	}

	public void setPublisherName(String publisherName) {
		this.publisherName = publisherName;
	}

	public String getGenre() {
		return genre;
	}

	public void setGenre(String genre) {
		this.genre = genre;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}

	public Element getXmlElementWithAllImages() {
		return xmlElementWithAllImages;
	}

	public void setXmlElementWithAllImages(Element xmlElementWithAllImages) {
		this.xmlElementWithAllImages = xmlElementWithAllImages;
	}

	public int hashCode() {
		return platform.hashCode() ^ title.hashCode();
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		WebProfile otherProfile = (WebProfile)obj;
		return platform.equalsIgnoreCase(otherProfile.platform) && title.equalsIgnoreCase(otherProfile.title);
	}

	public int compareTo(WebProfile otherProfile) {
		if (otherProfile.platform.equalsIgnoreCase(platform)) {
			return title.compareToIgnoreCase(otherProfile.title);
		}
		return platform.compareToIgnoreCase(otherProfile.platform);
	}

	public static final class byTitle implements Comparator<WebProfile> {
		public int compare(final WebProfile prof1, final WebProfile prof2) {
			return prof1.title.compareToIgnoreCase(prof2.title);
		}
	}

	public static final class byYear implements Comparator<WebProfile> {
		public int compare(final WebProfile prof1, final WebProfile prof2) {
			return prof1.year.compareToIgnoreCase(prof2.year);
		}
	}

	public static final class byPlatform implements Comparator<WebProfile> {
		public int compare(final WebProfile prof1, final WebProfile prof2) {
			return prof1.platform.compareToIgnoreCase(prof2.platform);
		}
	}

	public String toString() {
		return title + '@' + platform;
	}
}
