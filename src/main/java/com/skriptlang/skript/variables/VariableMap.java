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

import java.util.concurrent.ConcurrentHashMap;

public class VariableMap {
	private final ConcurrentHashMap<String, Object> variables = new ConcurrentHashMap<>();

	public Object getVariable(String name) {
		String[] split = Variables.splitVariable(name);
		if (split == null)
			return null;
		if (split.length > 1) {
			ConcurrentHashMap<String, Object> parentMap = variables;
			for (String splitName : split) {
				if (splitName.equals("*"))
					return parentMap;
				Object existing = parentMap.get(splitName);
				if (existing == null)
					return null;
				if (existing instanceof ConcurrentHashMap)
					parentMap = (ConcurrentHashMap<String, Object>) existing;
				else
					return existing;
			}
			return null;
		} else {
			return variables.get(name);
		}
	}

	public void deleteVariable(String name) {
		String[] split = Variables.splitVariable(name);
		if (split == null)
			return;
		if (split.length > 1) {
			ConcurrentHashMap<String, Object> parentMap = variables;
			for (int i = 0; i < split.length; i++) {
				Object existing = parentMap.get(split[i]);
				if (existing instanceof ConcurrentHashMap)
					parentMap = (ConcurrentHashMap<String, Object>) existing;
				if (i == split.length - 2) {
					if ("*".equals(split[i + 1]))
						parentMap.clear();
					else if (!(parentMap.get(split[i + 1]) instanceof ConcurrentHashMap))
						parentMap.remove(split[i + 1]);
					break;
				}
			}
		} else {
			variables.remove(split[0]);
		}
	}

	public void setVariable(String name, Object value) {
		String[] split = Variables.splitVariable(name);
		if (split == null)
			return;
		if (split.length > 1) {
			ConcurrentHashMap<String, Object> parentMap = variables;
			for (int i = 0; i < split.length; i++) {
				String splitName = split[i];
				Object existing = parentMap.get(splitName);
				if (existing instanceof ConcurrentHashMap) {
					if (i == split.length - 1) {
						parentMap.put(splitName, value);
						break;
					}
				} else {
					if (i == split.length - 1) {
						parentMap.put(splitName, value);
						break;
					}
					existing = new ConcurrentHashMap<>();
					parentMap.put(splitName, existing);
				}
				parentMap = (ConcurrentHashMap<String, Object>) existing;
			}
		} else {
			variables.put(name, value);
		}
	}

	public int size() {
		return variables.size();
	}

	/**
	 * Clears all variables
	 */
	public void clearVariables() {
		variables.clear();
	}
}
