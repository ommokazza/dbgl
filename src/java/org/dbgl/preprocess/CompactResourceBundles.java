package org.dbgl.preprocess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.dbgl.gui.SettingsDialog;


public class CompactResourceBundles {

	public static void main(String[] args) throws IOException {
		if (args.length != 1)
			throw new RuntimeException("parameter for i18n directory is missing");

		File dir = new File(args[0]);

		Map<String, String> baseResourceBundle = new TreeMap<String, String>();
		int totalNrOfEntries = readBundle(getResourceBundleFile(dir, ""), baseResourceBundle);

		for (String lang: SettingsDialog.SUPPORTED_LANGUAGES) {
			if (lang.equals("en"))
				continue;

			Map<String, String> srcBundle = new LinkedHashMap<String, String>();
			Map<String, String> dstBundle = new TreeMap<String, String>();

			int entries = readBundle(getResourceBundleFile(dir, "_" + lang), srcBundle);
			if (entries != totalNrOfEntries)
				throw new RuntimeException("Invalid number of entries in language resource bundle [" + lang + "]");

			for (Entry<String, String> entry: srcBundle.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();
				if (!baseResourceBundle.containsKey(key))
					throw new RuntimeException("Invalid entry [" + key + "] in [" + lang + "]");
				else if (!baseResourceBundle.get(key).equals(value))
					dstBundle.put(key, value);
			}

			writeBundle(getResourceBundleFile(dir, "_" + lang), dstBundle);
		}

		writeBundle(getResourceBundleFile(dir, ""), baseResourceBundle);
	}

	private static void writeBundle(File file, Map<String, String> bundle) throws FileNotFoundException, UnsupportedEncodingException {
		PrintWriter pw = new PrintWriter(file, "ISO_8859_1");
		for (Entry<String, String> entry: bundle.entrySet()) {
			pw.print(entry.getKey());
			pw.print('=');
			pw.println(entry.getValue());
		}
		pw.close();
	}

	private static int readBundle(File file, Map<String, String> bundle) throws FileNotFoundException, IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "ISO_8859_1"));
		try {
			String line = br.readLine();
			while (line != null) {
				line = line.trim();
				if (!line.isEmpty() && !line.startsWith("#")) {
					int idx = line.indexOf('=');
					bundle.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
				}
				line = br.readLine();
			}
		} finally {
			br.close();
		}
		return bundle.size();
	}

	private static File getResourceBundleFile(File dir, String locale) {
		return new File(dir, "MessagesBundle" + locale + ".properties");
	}
}
