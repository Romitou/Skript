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
package com.skriptlang.skript.old;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.registrations.Classes;
import com.skriptlang.skript.yggdrasil.Yggdrasil;
import com.skriptlang.skript.yggdrasil.YggdrasilSerializer;
import com.skriptlang.skript.yggdrasil.YggdrasilSingle;
import com.skriptlang.skript.yggdrasil.YggdrasilWriter;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

public class SingleVariable<V> extends Variable<V> {

	private final V value;
	private Variable<? extends V> child;

	public SingleVariable(String name, V value) {
		super(name);
		this.value = value;
	}

	public V getValue() {
		return value;
	}

	@Override
	public void setChild(Variable<? extends V> child) {
		this.child = child;
	}

	@Override
	public String getFullName() {
		return getName() + (child == null ? "" : "::" + child.getFullName());
	}

	public @Nullable V getValue(Event e)  {
		return value;
	}

	@Override
	public String toSchema() {
		return "{" + this.getName() + ": " + this.value + "}";
	}

	public Variable<? extends V> getChild() {
		return child;
	}

	public Yggdrasil serialize() {
		ClassInfo<?> classInfo = Classes.getSuperClassInfo(value.getClass());
		if (classInfo.getC() == Object.class)
			return null;

		Yggdrasil yggdrasil;
		if (classInfo.safeSerialization()) {
			yggdrasil = new YggdrasilSingle(classInfo, value);
		} else {
			YggdrasilSerializer<V> serializer = (YggdrasilSerializer<V>) classInfo.getSerializer();
			if (serializer == null) {
				Skript.error("serializer null : " + classInfo.getCodeName());
				return null;
			}
			YggdrasilWriter writer = new YggdrasilWriter(classInfo);
			serializer.serialize(writer, value);
			yggdrasil = writer.toReader();
		}

		return yggdrasil;
	}
}
