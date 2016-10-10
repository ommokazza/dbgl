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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.lang3.ArrayUtils;
import org.dbgl.util.PlatformUtils;
import org.dbgl.util.StringRelatedUtils;


public class SectionsWrapper {

	private Map<String, Section> sections;

	public SectionsWrapper() {
		sections = new LinkedHashMap<String, Section>();
	}

	public SectionsWrapper(SectionsWrapper sec) {
		this();
		for (String key: sec.sections.keySet()) {
			sections.put(key, new Section(sec.sections.get(key)));
		}
	}

	public SectionsWrapper(SectionsWrapper base, SectionsWrapper ext) {
		this();
		for (String sectionName: ext.sections.keySet()) {
			for (String itemName: ext.sections.get(sectionName).items.keySet()) {
				if (!((base != null) && base.hasValue(sectionName, itemName) && base.getValue(sectionName, itemName).equals(ext.getValue(sectionName, itemName))))
					setValue(sectionName, itemName, ext.getValue(sectionName, itemName));
			}
		}
	}

	public static SectionsWrapper createCombination(final SectionsWrapper s1, final SectionsWrapper s2) {
		SectionsWrapper result = new SectionsWrapper();
		for (String sectionName: s1.sections.keySet()) {
			for (String sectionItem: s1.sections.get(sectionName).items.keySet()) {
				String val1 = s1.getValue(sectionName, sectionItem);
				if (s2.hasValue(sectionName, sectionItem)) {
					String val2 = s2.getValue(sectionName, sectionItem);
					if (val1.equals(val2)) {
						result.setValue(sectionName, sectionItem, val1);
					} else {
						result.setValue(sectionName, sectionItem, Conf.CONFLICTING_STRING_SETTING);
					}
				}
			}
		}
		return result;
	}

	public Set<String> getAllSectionNames() {
		return sections.keySet();
	}

	public String[] getAllItemNames(final String sectionTitle) {
		return new TreeMap<String, String>(sections.get(sectionTitle).items).keySet().toArray(new String[0]);
	}

	public boolean hasValue(final String sectionTitle, final String sectionItem) {
		return sections.containsKey(sectionTitle) && sections.get(sectionTitle).items.containsKey(sectionItem);
	}

	public String getValue(final String sectionTitle, final String sectionItem) {
		String result = null;
		if (sections.containsKey(sectionTitle)) {
			result = sections.get(sectionTitle).items.get(sectionItem);
		}
		if (result == null) {
			result = ""; // in case the item was not found
		}
		return result;
	}

	public boolean isConflictingValue(final String sectionTitle, final String sectionItem) {
		return hasValue(sectionTitle, sectionItem) && getValue(sectionTitle, sectionItem).equals(Conf.CONFLICTING_STRING_SETTING);
	}

	public String[] getValues(final String sectionTitle, final String sectionItem) {
		String val = getValue(sectionTitle, sectionItem);
		if (val.length() <= 0) {
			return new String[0];
		}
		String[] res = val.split(" ");
		for (int i = 0; i < res.length; i++) {
			res[i] = res[i].replaceAll("<space>", " ");
		}
		return res;
	}

	public String[] getCommonValues(final String sectionTitle, final String item1, final String item2) {
		java.util.List<String> resultList = new ArrayList<String>();
		String[] a1 = getValues(sectionTitle, item1);
		String[] a2 = getValues(sectionTitle, item2);
		for (String s: a1) {
			if (ArrayUtils.contains(a2, s)) {
				resultList.add(s);
			}
		}
		return resultList.toArray(new String[0]);
	}

	public String getMultilineValues(final String sectionTitle, final String sectionItem, final String delimiter) {
		return StringRelatedUtils.stringArrayToString(getValues(sectionTitle, sectionItem), delimiter);
	}

	public int getIntValue(final String sectionTitle, final String sectionItem) {
		try {
			return Integer.parseInt(getValue(sectionTitle, sectionItem));
		} catch (NumberFormatException e) {
			return -1; // value is not a number
		}
	}

	public int[] getIntValues(final String sectionTitle, final String sectionItem) {
		return StringRelatedUtils.stringToIntArray(getValue(sectionTitle, sectionItem));
	}

	public boolean getBooleanValue(final String sectionTitle, final String sectionItem) {
		return Boolean.valueOf(getValue(sectionTitle, sectionItem));
	}

	public boolean[] getBooleanValues(final String sectionTitle, final String sectionItem) {
		return StringRelatedUtils.stringToBooleanArray(getValue(sectionTitle, sectionItem));
	}

	public void setValue(final String sectionTitle, final String sectionItem, final String value) {
		Section sec = createSection(sectionTitle);
		sec.items.put(sectionItem, value);
	}

	public void setMultilineValues(final String sectionTitle, final String sectionItem, final String values, final String delimiter) {
		setValue(sectionTitle, sectionItem, values.replaceAll(" ", "<space>").replace(delimiter, " ").trim());
	}

	public void setIntValue(final String sectionTitle, final String sectionItem, final int value) {
		setValue(sectionTitle, sectionItem, String.valueOf(value));
	}

	public void setIntValues(final String sectionTitle, final String sectionItem, final int[] values) {
		setValue(sectionTitle, sectionItem, StringRelatedUtils.join(values));
	}

	public void setBooleanValue(final String sectionTitle, final String sectionItem, final boolean value) {
		setValue(sectionTitle, sectionItem, String.valueOf(value));
	}

	public void setBooleanValues(final String sectionTitle, final String sectionItem, final boolean[] values) {
		setValue(sectionTitle, sectionItem, StringRelatedUtils.join(values));
	}

	public void updateValue(final String sectionTitle, final String sectionItem, final String value) {
		if (hasValue(sectionTitle, sectionItem))
			sections.get(sectionTitle).items.put(sectionItem, value);
	}

	public void setMissingValue(final String sectionTitle, final String sectionItem, final String value) {
		if (!hasValue(sectionTitle, sectionItem))
			setValue(sectionTitle, sectionItem, value);
	}

	public void removeSection(final String sectionTitle) {
		sections.remove(sectionTitle);
	}

	public void removeValue(final String sectionTitle, final String sectionItem) {
		if (sections.containsKey(sectionTitle)) {
			Section sec = sections.get(sectionTitle);
			sec.items.remove(sectionItem);
			if (sec.items.isEmpty()) {
				sections.remove(sectionTitle);
			}
		}
	}

	public void injectValuesFrom(final SectionsWrapper sec) {
		for (String sectionTitle: sec.sections.keySet()) {
			for (String sectionItem: sec.sections.get(sectionTitle).items.keySet()) {
				setValue(sectionTitle, sectionItem, sec.getValue(sectionTitle, sectionItem));
			}
		}
	}

	public void addMissingValuesFrom(final SectionsWrapper sec) {
		for (String sectionTitle: sec.sections.keySet()) {
			for (String sectionItem: sec.sections.get(sectionTitle).items.keySet()) {
				setMissingValue(sectionTitle, sectionItem, sec.getValue(sectionTitle, sectionItem));
			}
		}
	}

	public void removeValuesThatAreNotIn(final SectionsWrapper sec) {
		SectionsWrapper copy = new SectionsWrapper(this);
		for (String sectionTitle: copy.sections.keySet()) {
			for (String sectionItem: copy.sections.get(sectionTitle).items.keySet()) {
				if (!sec.hasValue(sectionTitle, sectionItem))
					removeValue(sectionTitle, sectionItem);
			}
		}
	}

	public int detectDosboxVersionGeneration() {
		if (hasValue("cpu", "cputype") && hasValue("midi", "mididevice") && hasValue("midi", "midiconfig") && hasValue("sblaster", "sbmixer") && hasValue("sblaster", "oplemu")
				&& hasValue("gus", "gusirq") && hasValue("gus", "gusdma"))
			return 3; // 73
		if (hasValue("joystick", "joysticktype") && hasValue("joystick", "timed") && hasValue("joystick", "autofire") && hasValue("joystick", "swap34") && hasValue("joystick", "buttonwrap")
				&& hasValue("dos", "keyboardlayout"))
			return 2; // 70
		if (hasValue("sdl", "windowresolution") && hasValue("sdl", "usescancodes") && hasValue("sblaster", "sbtype") && hasValue("sblaster", "sbbase") && hasValue("gus", "gusrate")
				&& hasValue("gus", "gusbase") && hasValue("speaker", "tandy") && hasValue("bios", "joysticktype") && hasValue("serial", "serial1") && hasValue("dos", "umb"))
			return 1; // 65
		return 0; // 63
	}

	public void alterToDosboxVersionGeneration(final SectionsWrapper dosboxSections) {
		int srcGeneration = detectDosboxVersionGeneration();
		int dstGeneration = dosboxSections.detectDosboxVersionGeneration();

		alterToDosboxVersionGeneration(dosboxSections, srcGeneration, dstGeneration);
	}

	public void switchToDosboxVersionGeneration(final SectionsWrapper orgDosboxSections, final SectionsWrapper targetDosboxSections) {
		int srcGeneration = detectDosboxVersionGeneration();
		int dstGeneration = targetDosboxSections.detectDosboxVersionGeneration();

		SectionsWrapper substracted = new SectionsWrapper(orgDosboxSections, this);
		this.sections = substracted.sections;

		alterToDosboxVersionGeneration(targetDosboxSections, srcGeneration, dstGeneration);
	}

	public void alterToDosboxVersionGeneration(final SectionsWrapper dosboxSections, final int srcGeneration, final int dstGeneration) {
		if (srcGeneration > dstGeneration) {
			for (int i = 0; i < (srcGeneration - dstGeneration); i++)
				downgradeOneGeneration(srcGeneration - i);
		}
		if (srcGeneration < dstGeneration) {
			for (int i = 0; i < (dstGeneration - srcGeneration); i++)
				upgradeOneGeneration(srcGeneration + i);
		}

		addMissingValuesFrom(dosboxSections);
		removeValuesThatAreNotIn(dosboxSections);
	}

	public String toString(final boolean ordered) {
		StringBuffer result = new StringBuffer();
		for (String key: sections.keySet()) {
			result.append("[" + key + "]" + PlatformUtils.EOLN);
			result.append((sections.get(key)).toString(ordered)).append(PlatformUtils.EOLN);
		}
		return result.toString();
	}

	public String toString() {
		return toString(false);
	}

	private Section createSection(final String sectionTitle) {
		if (sections.containsKey(sectionTitle)) {
			return sections.get(sectionTitle);
		}
		Section newSection = new Section();
		sections.put(sectionTitle, newSection);
		return newSection;
	}

	private void upgradeOneGeneration(final int baseGeneration) {
		switch (baseGeneration) {
			case 0:
				boolean mpu = !hasValue("midi", "mpu401") || getBooleanValue("midi", "mpu401");
				boolean intelli = !hasValue("midi", "intelligent") || getBooleanValue("midi", "intelligent");
				setValue("midi", "mpu401", mpu ? (intelli ? "intelligent": "uart"): "none");
				removeValue("midi", "intelligent");
				switchSetting("sblaster", "type", "sblaster", "sbtype");
				switchSetting("sblaster", "base", "sblaster", "sbbase");
				switchSetting("gus", "rate", "gus", "gusrate");
				switchSetting("gus", "base", "gus", "gusbase");
				break;
			case 1:
				switchSetting("bios", "joysticktype", "joystick", "joysticktype");
				break;
			case 2:
				switchSetting("gus", "irq1", "gus", "gusirq");
				removeValue("gus", "irq2");
				switchSetting("gus", "dma1", "gus", "gusdma");
				removeValue("gus", "dma2");
				if (getValue("dosbox", "machine").equalsIgnoreCase("vga")) {
					removeValue("dosbox", "machine"); // if machine was set to vga, remove the value to have it reset
				}
				if (getValue("dos", "keyboardlayout").equalsIgnoreCase("none")) {
					removeValue("dos", "keyboardlayout"); // if keyboard layout was set to none, remove the value to have it reset
				}
				switchSetting("midi", "device", "midi", "mididevice");
				switchSetting("midi", "config", "midi", "midiconfig");
				switchSetting("sblaster", "mixer", "sblaster", "sbmixer");
				break;
			default: // cannot upgrade further
		}
	}

	private void downgradeOneGeneration(final int baseGeneration) {
		switch (baseGeneration) {
			case 3:
				switchSetting("gus", "gusirq", "gus", "irq1");
				if (hasValue("gus", "irq1")) {
					setValue("gus", "irq2", getValue("gus", "irq1"));
				}
				switchSetting("gus", "gusdma", "gus", "dma1");
				if (hasValue("gus", "dma1")) {
					setValue("gus", "dma2", getValue("gus", "dma1"));
				}
				String mach = getValue("dosbox", "machine");
				if (!(mach.equalsIgnoreCase("cga") || mach.equalsIgnoreCase("hercules") || mach.equalsIgnoreCase("pcjr") || mach.equalsIgnoreCase("tandy"))) {
					removeValue("dosbox", "machine"); // if machine was NOT set to cga/hercules/pcjr/tandy, remove the value to have it reset
				}
				if (getValue("dos", "keyboardlayout").equalsIgnoreCase("auto")) {
					removeValue("dos", "keyboardlayout"); // if keyboard layout was set to auto, remove the value to have it reset
				}
				switchSetting("midi", "mididevice", "midi", "device");
				switchSetting("midi", "midiconfig", "midi", "config");
				switchSetting("sblaster", "sbmixer", "sblaster", "mixer");
				break;
			case 2:
				switchSetting("joystick", "joysticktype", "bios", "joysticktype");
				break;
			case 1:
				if (hasValue("midi", "mpu401")) {
					String mpu = getValue("midi", "mpu401");
					setBooleanValue("midi", "mpu401", !mpu.equalsIgnoreCase("none"));
					setBooleanValue("midi", "intelligent", mpu.equalsIgnoreCase("intelligent"));
				}
				switchSetting("sblaster", "sbtype", "sblaster", "type");
				switchSetting("sblaster", "sbbase", "sblaster", "base");
				switchSetting("gus", "gusrate", "gus", "rate");
				switchSetting("gus", "gusbase", "gus", "base");
				break;
			default: // cannot downgrade further
		}
	}

	private void switchSetting(final String oldSection, final String oldItem, final String newSection, final String newItem) {
		if (hasValue(oldSection, oldItem)) {
			setValue(newSection, newItem, getValue(oldSection, oldItem));
		}
		removeValue(oldSection, oldItem);
	}

	public boolean hasFullResolutionDesktopSupport() {
		return hasValue("dosbox", "vmemsize"); // valid for mostly all ykhwong builds
	}

	public boolean hasOutputDirect3DSupport() {
		return hasValue("sdl", "pixelshader"); // valid for mostly all ykhwong+gulikoza builds
	}

	public boolean hasAmstradSupport() {
		return hasValue("speaker", "ps1audio"); // valid for more recent ykhwong builds
	}

	public boolean hasHardwareScalerSupport() {
		return hasOutputDirect3DSupport(); // valid for mostly all ykhwong+gulikoza builds
	}

	public boolean hasMT32Support() {
		return hasValue("midi", "mt32.dac"); // valid for more recent ykhwong builds
	}

	public boolean hasSoundBlaster16VibraSupport() {
		return hasValue("sblaster", "goldplay"); // valid for more recent ykhwong builds
	}

	public boolean hasHardwareOPLSupport() {
		return hasValue("sblaster", "hardwarebase"); // valid for mostly all ykhwong+gulikoza builds
	}

	public boolean hasAdditionalCPUTypesSupport() {
		return hasValue("parallel", "dongle"); // valid for more recent ykhwong builds
	}
}
