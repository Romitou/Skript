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

import org.eclipse.jdt.annotation.Nullable;

import java.util.HashMap;
import java.util.Optional;

public class ListVariable<V> extends Variable<V> {

	private final HashMap<String, Variable<? extends V>> children = new HashMap<>();

	@SafeVarargs
	public ListVariable(String name, Variable<? extends V>... children) {
		super(name);
		for (Variable<? extends V> child : children)
			this.children.put(child.getName(), child);
	}

	public Variable<? extends V> getChild(String name) {
		return children.get(name);
	}

	public HashMap<String, Variable<? extends V>> getChildren() {
		return children;
	}

	@Override
	public String getFullName() {
		if (children.size() != 1)
			return getName() + "::*";
		Optional<Variable<? extends V>> var = children.values().stream().findFirst();
		return getName() + "::" + var.get().getFullName();
	}

	public @Nullable V[] getValues()  {
		return (V[]) children.values()
			.stream()
			.filter(v -> v instanceof SingleVariable<?>)
			.map(v -> ((SingleVariable<?>) v).getValue())
			.toArray();
	}

	@Override
	public String toSchema() {
		StringBuilder builder = new StringBuilder();
		builder.append("{ ").append(this.getName()).append(": ").append("[");
		this.children.forEach((k, v) -> builder.append(k).append(": ").append(v.toSchema()).append(", "));
		builder.append("] }");
		return builder.toString();
	}

	public void setChild(Variable<? extends V> child) {
		children.put(child.getName(), child);
	}
}
