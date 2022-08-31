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

public class YggdrasilWriter extends YggdrasilComplex {

	public YggdrasilWriter(ClassInfo<?> type) {
		super(type);
	}

	protected YggdrasilWriter(ClassInfo<?> type, HashMap<String, Object> values) {
		super(type, values);
	}

	public YggdrasilWriter writeUUID(String key, UUID value) {
		this.write(key, value);
		return this;
	}

	public YggdrasilWriter writeString(String key, String value) {
		this.write(key, value);
		return this;
	}

	public YggdrasilWriter writeDouble(String key, double value) {
		this.write(key, value);
		return this;
	}

	public YggdrasilWriter writeFloat(String key, float value) {
		this.write(key, value);
		return this;
	}

	public YggdrasilWriter writeLong(String key, long value) {
		this.write(key, value);
		return this;
	}

	public YggdrasilWriter writeInt(String key, int value) {
		this.write(key, value);
		return this;
	}

	public YggdrasilWriter writeBoolean(String key, boolean value) {
		this.write(key, value);
		return this;
	}

	public YggdrasilReader toReader() {
		return new YggdrasilReader(getType(), readAll());
	}
}
