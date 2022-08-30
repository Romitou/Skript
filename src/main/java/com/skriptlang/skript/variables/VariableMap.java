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

class VariableMap {
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
//
//	public void aaa(String name, @Nullable Object value) {
//		if (!name.endsWith("*")) {
//			if (value == null) {
//				variables.remove(name);
//			} else {
//				variables.put(name, value);
//			}
//		}
//		String[] split = Variables.splitVariable(name);
//		if (split == null)
//			return;
//		ConcurrentHashMap<String, Object> parent = variables;
//		for (int i = 0; i < split.length; i++) {
//			String splitName = split[i];
//			Object current = parent.get(splitName);
//			if (current == null) {
//				if (i == split.length - 1) {
//					if (value != null)
//						parent.put(splitName, value);
//					break;
//				} else if (value != null) {
//					parent.put(splitName, current = new ConcurrentHashMap<>());
//					parent = (ConcurrentHashMap<String, Object>) current;
//				} else {
//					break;
//				}
//			} else if (current instanceof ConcurrentHashMap) {
//				if (i == split.length - 1) {
//					if (value == null) {
//						((ConcurrentHashMap<String, Object>) current).remove(null);
//					} else {
//						((ConcurrentHashMap<String, Object>) current).put(null, value);
//					}
//					break;
//				} else if (i == split.length - 2 && split[i + 1].equals("*")) {
//					deleteFromConcurrentHashMap(String
//						.join(Variables.SEPARATOR, Arrays.copyOfRange(split, 0, i + 1)), (ConcurrentHashMap<String, Object>) current);
//					value = ((ConcurrentHashMap<String, Object>) current).get(null);
//					if (value == null) {
//						parent.remove(splitName);
//					} else {
//						parent.put(splitName, value);
//					}
//					break;
//				} else {
//					parent = (ConcurrentHashMap<String, Object>) current;
//				}
//			} else {
//				if (i == split.length - 1) {
//					if (value == null) {
//						parent.remove(splitName);
//					} else {
//						parent.put(splitName, value);
//					}
//					break;
//				} else if (value != null) {
//					ConcurrentHashMap<String, Object> c = new ConcurrentHashMap<>();
//					c.put(null, current);
//					parent.put(splitName, c);
//					parent = c;
//				} else {
//					break;
//				}
//			}
//		}
//	}

	/**
	 * Clears all variables
	 */
	public void clearVariables() {
		variables.clear();
	}

//	private void deleteFromConcurrentHashMap(String parent, Map<String, Object> current) {
//		for (Map.Entry<String, Object> entry : current.entrySet()) {
//			if (entry.getKey() == null)
//				continue;
//			variables.remove(parent + Variables.SEPARATOR + entry.getKey());
//			Object value = entry.getValue();
//			if (value instanceof Map) {
//				deleteFromConcurrentHashMap(parent + Variables.SEPARATOR + entry.getKey(), (Map<String, Object>) value);
//			}
//		}
//	}
}
