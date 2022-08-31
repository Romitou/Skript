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
import org.eclipse.jdt.annotation.Nullable;

public abstract class YggdrasilSerializer<T> {

	private ClassInfo<? extends T> classInfo = null;

	public void setClassInfo(ClassInfo<? extends T> classInfo) {
		this.classInfo = classInfo;
	}

	public ClassInfo<? extends T> getClassInfo() {
		return this.classInfo;
	}

	public abstract void serialize(YggdrasilWriter writer, T object);

	public abstract @Nullable T deserialize(YggdrasilReader reader);

}
