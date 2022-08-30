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

public class YggdrasilSingle extends Yggdrasil {
	private Object value;

	public YggdrasilSingle(ClassInfo<?> type) {
		super(type);
	}

	public YggdrasilSingle(ClassInfo<?> type, Object value) {
		super(type);
		this.value = value;
	}

	public void write(Object value) {
		this.value = value;
	}

	public Object read() {
		return this.value;
	}

	public void checkType(Class<?> type) {
		if (!type.isInstance(this.value)) {
			throw new IllegalArgumentException("The value is not of type " + type.getSimpleName());
		}
	}

}
