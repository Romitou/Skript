/**
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright Peter Güttinger, SkriptLang team and contributors
 */
package com.skriptlang.skript.variables;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.config.Config;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import com.skriptlang.skript.variables.storages.CsvStorage;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Variables {
	public final static String LOCAL_TOKEN = "_";
	public final static String SEPARATOR = "::";

	private static final VariableMap VARIABLES = new VariableMap();
	private static final HashMap<Event, VariableMap> LOCALS = new HashMap<>();

	private final static VariableStorage[] DEFAULT_STORAGES = new VariableStorage[]{new CsvStorage()};
	public final static HashMap<StoragePattern, VariableStorage> STORAGES = new HashMap<>();

	public static VariableStorage getStorage(final String name) {
		for (final VariableStorage storage : DEFAULT_STORAGES) {
			if (storage.getStorageName().equals(name))
				return storage;
		}
		return null;
	}

	public static boolean load() {
		final Config c = SkriptConfig.getConfig();
		if (c == null)
			throw new SkriptAPIException("Cannot load variables before the config");

		final Node databases = c.getMainNode().get("databases");
		if (!(databases instanceof SectionNode)) {
			Skript.error("The config is missing the required 'databases' section that defines where the variables are saved");
			return false;
		}

		boolean successful = true;
		for (final Node node : (SectionNode) databases) {
			if (node instanceof SectionNode) {
				SectionNode n = (SectionNode) node;
				final String type = n.getValue("type");
				if (type == null) {
					Skript.error("Missing entry 'type' in database definition");
					successful = false;
					continue;
				}

				final String name = n.getKey();
				if (name == null) {
					Skript.error("Empty or invalid database definition");
					successful = false;
					continue;
				}


				final String startsWith = n.getValue("startsWith");
				StoragePattern storagePattern = StoragePattern.startsWith(startsWith != null ? startsWith : "");

				final String pattern = n.getValue("pattern");
				if (pattern != null) {
					try {
						storagePattern = StoragePattern.regex(Pattern.compile(pattern));
					} catch (PatternSyntaxException e) {
						Skript.error("Invalid regex pattern in database definition: " + e.getMessage());
						successful = false;
						continue;
					}
				}

				VariableStorage variableStorage = getStorage(type);
				if (variableStorage == null) {
					Skript.error("Unknown database type '" + type + "'");
					Skript.error("Available database types: " + String.join(", ", Arrays.stream(DEFAULT_STORAGES).map(VariableStorage::getStorageName).toArray(String[]::new)));
					successful = false;
					continue;
				}

				successful = variableStorage.configure(n);
				if (successful) {
					variableStorage.getPreloadedVariables().forEach(VARIABLES::setVariable);
					STORAGES.put(storagePattern, variableStorage);
				}
			} else {
				Skript.error("Invalid line in databases: databases must be defined as sections");
				successful = false;
			}

		}

		Skript.closeOnDisable(() -> STORAGES.values().forEach(VariableStorage::close));

		return successful;
	}

	public static Object getVariable(final String name) {
		return VARIABLES.getVariable(name);
//			if (CACHED_VARIABLES.containsKey(name))
//				return CACHED_VARIABLES.get(name);
//			VariableStorage variableStorage = getStorageForVariable(name);
//			if (variableStorage == null)
//				return null;
			// return variableStorage.getVariable(name);

	}

	public static Object getLocalVariable(final String name, final Event e) {
		VariableMap variableMap = LOCALS.get(e);
		if (variableMap == null)
			return null;
		return variableMap.getVariable(name);
	}

	public static void setVariable(final String name, @Nullable Object value) {
		// System.out.println("Variables.setVariable("+name+", "+value+");");
		VariableStorage variableStorage = getStorageForVariable(name);

		if (value == null) {
			VARIABLES.deleteVariable(name);
			if (variableStorage != null)
				variableStorage.deleteVariable(name);
			return;
		}

		// DEBUG System.out.println("VARIABLES.setVariable("+name+", "+value+");");
		VARIABLES.setVariable(name, value);
		if (variableStorage != null) {
			// System.out.println("Set storage variable: " + name + ", " + value);
			variableStorage.setVariable(name, value);
		}
	}

	public static void setLocalVariable(final String name, Object value, Event e) {
		// DEBUG System.out.println("Variables.setLocalVariable("+name+", "+value+", "+e+");");
		VariableMap variableMap = LOCALS.get(e);
		if (variableMap == null) {
			variableMap = new VariableMap();
			LOCALS.put(e, variableMap);
		}

		if (value == null) {
			variableMap.deleteVariable(name);
			return;
		}

		variableMap.setVariable(name, value);
	}

	public static boolean isValidVariableName(String name, boolean b, boolean b1) {
		return splitVariable(name) != null; // TODO
	}

	public static void setLocalVariables(Event e, VariableMap variableMap) {
		LOCALS.put(e, variableMap);
	}

	public static int size() {
		return VARIABLES.size();
	}

	public static VariableMap getLocalVariables(Event e) {
		return LOCALS.get(e);
	}
	public static void removeLocals(Event e) {
		LOCALS.remove(e);
	}

	public static VariableStorage getStorageForVariable(String name) {
		for (StoragePattern storagePattern : STORAGES.keySet()) {
			if (storagePattern.matches(name))
				return STORAGES.get(storagePattern);
		}
		return null;
	}

	public static String[] splitVariable(String name) {
		List<String> split = new ArrayList<>();

		int lastSplit = 0;
		for (int i = 0; i < name.length(); i++) {
			char character = name.charAt(i);
			if (character == ':') {
				if (i + 1 >= name.length()
					|| name.charAt(i + 1) != ':'
					|| name.charAt(i + 2) == ':') {
					Skript.error("Invalid variable name '" + name + "'");
					return null;
				}
				split.add(name.substring(lastSplit, i));
				i = i + 2;
				lastSplit = i;
			}
		}
		split.add(name.substring(lastSplit));

		return split.toArray(new String[0]);
	}
}
