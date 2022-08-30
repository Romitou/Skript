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

public abstract class Variable<V> {

	private final String name;

	Variable(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public Object getAll() {
		if (this instanceof SingleVariable<?>) {
			return ((SingleVariable<?>) this).getValue();
		} else {
			return ((ListVariable<?>) this).getValues();
		}
	}

	abstract void setChild(Variable<? extends V> child);

	abstract String getFullName();

	abstract public String toSchema();
}
