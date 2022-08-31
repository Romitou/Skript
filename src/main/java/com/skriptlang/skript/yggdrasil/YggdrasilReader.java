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
import java.util.UUID;

public class YggdrasilReader extends YggdrasilComplex {

	public YggdrasilReader(ClassInfo<?> type) {
		super(type);
	}

	protected YggdrasilReader(ClassInfo<?> type, HashMap<String, Object> values) {
		super(type, values);
	}

	public UUID readUUID(String key) {
		this.checkType(key, String.class);
		return (UUID) this.read(key);
	}

	public String readString(String key) {
		this.checkType(key, String.class);
		return (String) this.read(key);
	}

	public int readInt(String key) {
		this.checkType(key, Integer.class);
		return (int) this.read(key);
	}

	public boolean readBoolean(String key) {
		this.checkType(key, Boolean.class);
		return (boolean) this.read(key);
	}

	public double readDouble(String key) {
		this.checkType(key, Double.class);
		return (double) this.read(key);
	}

	public float readFloat(String key) {
		this.checkType(key, Float.class);
		return (float) this.read(key);
	}

	public long readLong(String key) {
		this.checkType(key, Long.class);
		return (long) this.read(key);
	}

	public <T> T readOrDefault(String key, T value) {
		this.checkType(key, value.getClass());
		Object read = this.read(key);
		return (read == null ? value : (T) read);
	}

	public void checkType(String key, Class<?> type) {
		if (!type.isInstance(this.read(key))) {
			throw new IllegalArgumentException("The value of the key '" + key + "' is not of type " + type.getSimpleName());
		}
	}

	public YggdrasilWriter toWriter() {
		return new YggdrasilWriter(getType(), readAll());
	}
}
