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
package com.skriptlang.skript.yggdrasil;

import ch.njol.skript.classes.ClassInfo;

import java.util.HashMap;

public abstract class YggdrasilComplex extends Yggdrasil {
	private final HashMap<String, Object> values = new HashMap<>();

	protected YggdrasilComplex(ClassInfo<?> type) {
		super(type);
	}

	protected YggdrasilComplex(ClassInfo<?> type, HashMap<String, Object> values) {
		super(type);
		this.values.putAll(values);
	}

	public void write(String key, Object value) {
		values.put(key, value);
	}

	public Object read(String key) {
		return values.get(key);
	}

	public HashMap<String, Object> readAll() {
		return values;
	}

	public boolean contains(String key) {
		return this.values.containsKey(key);
	}

	public int size() {
		return values.size();
	}

	public Object deserialize() {
		YggdrasilSerializer<?> serializer = getType().getSerializer();
		if (serializer == null)
			return null;
		return serializer.deserialize((YggdrasilReader) this);
	}

}
