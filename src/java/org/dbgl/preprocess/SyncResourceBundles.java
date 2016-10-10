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
import org.dbgl.gui.SettingsDialog;


public class SyncResourceBundles {

	public static void main(String[] args) throws IOException {
		if (args.length != 1)
			throw new RuntimeException("parameter for i18n directory is missing");

		File dir = new File(args[0]);

		Map<String, String> baseResourceBundle = new LinkedHashMap<String, String>();
		readBundle(getResourceBundleFile(dir, ""), baseResourceBundle);

		for (String lang: SettingsDialog.SUPPORTED_LANGUAGES) {
			if (lang.equals("en"))
				continue;

			Map<String, String> srcBundle = new LinkedHashMap<String, String>();
			Map<String, String> dstBundle = new LinkedHashMap<String, String>();

			readBundle(getResourceBundleFile(dir, "_" + lang), srcBundle);

			for (Entry<String, String> entry: baseResourceBundle.entrySet()) {
				String key = entry.getKey();
				if (key.startsWith("_")) {
					dstBundle.put(key, entry.getValue());
				} else {
					if (!srcBundle.containsKey(key))
						dstBundle.put(key, entry.getValue());
					else
						dstBundle.put(key, srcBundle.get(key));
				}
			}

			writeBundle(getResourceBundleFile(dir, "_" + lang), dstBundle);
		}
	}

	private static void writeBundle(File file, Map<String, String> bundle) throws FileNotFoundException, UnsupportedEncodingException {
		PrintWriter pw = new PrintWriter(file, "ISO_8859_1");
		for (Entry<String, String> entry: bundle.entrySet()) {
			if (entry.getKey().startsWith("_")) {
				pw.println(entry.getValue());
			} else {
				pw.print(entry.getKey());
				pw.print(" = ");
				pw.println(entry.getValue());
			}
		}
		pw.close();
	}

	private static void readBundle(File file, Map<String, String> bundle) throws FileNotFoundException, IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "ISO_8859_1"));
		try {
			Integer nr = 0;
			String line = br.readLine();
			while (line != null) {
				line = line.trim();
				if (!line.isEmpty() && !line.startsWith("#")) {
					int idx = line.indexOf('=');
					if (idx != -1)
						bundle.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
				} else {
					bundle.put('_' + nr.toString(), line);
				}
				line = br.readLine();
				nr++;
			}
		} finally {
			br.close();
		}
	}

	private static File getResourceBundleFile(File dir, String locale) {
		return new File(dir, "MessagesBundle" + locale + ".properties");
	}
}
