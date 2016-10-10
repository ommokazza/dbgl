package exodos.model;

import java.io.File;
import java.util.List;
import org.dbgl.model.ExpProfile;
import org.dbgl.util.FileUtils;


public class GameData implements Comparable<GameData> {

	private ExpProfile expProfile;
	private List<File> capturesList;
	private List<File> extrasList;
	private File zipFile;
	private boolean multipleRootEntries;

	public GameData(ExpProfile expProfile, List<File> capturesList, List<File> extrasList, File zipFile, boolean multipleRootEntries) {
		this.expProfile = expProfile;
		this.capturesList = capturesList;
		this.extrasList = extrasList;
		this.zipFile = zipFile;
		this.multipleRootEntries = multipleRootEntries;
	}

	public ExpProfile getExpProfile() {
		return expProfile;
	}

	public List<File> getCapturesList() {
		return capturesList;
	}

	public List<File> getExtrasList() {
		return extrasList;
	}

	public File getZipFile() {
		return zipFile;
	}

	public boolean isMultipleRootEntries() {
		return multipleRootEntries;
	}

	public int compareTo(final GameData comp) {
		return FileUtils.fileSystemSafe(this.expProfile.getTitle()).compareToIgnoreCase(FileUtils.fileSystemSafe(comp.expProfile.getTitle()));
	}
}
