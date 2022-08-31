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
package com.skriptlang.skript.lang;

import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.VariableString;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.registrations.Converters;
import ch.njol.skript.util.Utils;
import ch.njol.util.Checker;
import ch.njol.util.Kleenean;
import ch.njol.util.Pair;
import ch.njol.util.StringUtils;
import ch.njol.util.coll.CollectionUtils;
import ch.njol.util.coll.iterator.EmptyIterator;
import ch.njol.util.coll.iterator.SingleItemIterator;
import com.google.errorprone.annotations.Var;
import com.skriptlang.skript.variables.Variables;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.lang.reflect.Array;
import java.util.*;

public class ExprVariable<V> implements Expression<V> {

	private final VariableString name;
	private final boolean isSingle;
	private final boolean isLocal;
	private final Class<? extends V>[] types;
	private final Class<V> superType;

	@SafeVarargs
	public ExprVariable(VariableString name, boolean isLocal, boolean isSingle, Class<? extends V> ...types) {
		// System.out.println("name:" + name);
		this.name = name;
		this.isLocal = isLocal;
		this.isSingle = isSingle;
		this.types = types;
		this.superType = (Class<V>) Utils.getSuperType(this.types);
	}

	public static <T> ExprVariable<T> newInstance(String name, Class<? extends T>[] types) {
		name = name.trim();
		boolean isLocal = name.startsWith(Variables.LOCAL_TOKEN);
		boolean isSingle = !name.endsWith(Variables.SEPARATOR + "*");
		name = name.substring(isLocal ? 1 : 0, isSingle ? name.length() : name.length() - 3);
		VariableString variableString = VariableString.newInstance(name);
		if (variableString == null)
			return null;
		return new ExprVariable<>(variableString, isLocal, isSingle, types);
	}

	public VariableString getName() {
		return name;
	}

	public boolean isIndexLoop(String s) {
		return s.equalsIgnoreCase("index");
	}

	@Override
	public @Nullable V getSingle(Event e) {
		if (!isSingle())
			throw new SkriptAPIException("Called Variable#getSingle while the variable is a list!");
		Object variable = isLocal
			? Variables.getLocalVariable(this.name.getSingle(e), e)
			: Variables.getVariable(this.name.getSingle(e));
		// System.out.println("ExprVariable.getSingle(): Variables.getVariable("+this.name.getSingle(e)+"): "+variable);
		// System.out.println("|-> isLocal: " + isLocal);
		if (variable == null)
			return null;
		return (V) variable;
	}

	@Override
	public V[] getArray(Event e) {
//		if (isSingle())
//			throw new SkriptAPIException("Called Variable#getArray while the variable is single!");
//		return ((ListVariable<V>) getVariable(e)).getValues();
		return getAll(e);
	}

	@Override
	public V[] getAll(Event e) {
		if (isSingle()) {
			V value = getSingle(e);
			if (value == null)
				return (V[]) Array.newInstance(superType, 0);

			// System.out.println("superType:" + superType);
			// System.out.println("value: " + value.getClass());
			V[] array = (V[]) Array.newInstance(superType, 1);
			array[0] = value;
			return array;
		}

		Object variable = isLocal
			? Variables.getLocalVariable(this.name.getSingle(e), e)
			: Variables.getVariable(this.name.getSingle(e));

		if (variable instanceof Map)
			return (V[]) ((Map) variable).values().toArray();
		return (V[]) Array.newInstance(superType, 0);
	}

	@Override
	public boolean isSingle() {
		return isSingle;
	}

	@Override
	public boolean check(Event e, Checker<? super V> c, boolean negated) {
		return SimpleExpression.check(getAll(e), c, negated, getAnd());
	}

	@Override
	public boolean check(Event e, Checker<? super V> c) {
		return SimpleExpression.check(getAll(e), c, false, getAnd());
	}

	@Override
	public @Nullable <R> Expression<? extends R> getConvertedExpression(Class<R>... to) {
		return new ExprVariable<>(name, isLocal, isSingle, to);
	}

	@Override
	public Class<? extends V> getReturnType() {
		return this.superType;
	}

	@Override
	public boolean getAnd() {
		return true;
	}

	@Override
	public boolean setTime(int time) {
		return false;
	}

	@Override
	public int getTime() {
		return 0;
	}

	@Override
	public boolean isDefault() {
		return false;
	}

	@Override
	public boolean isLoopOf(String s) {
		return s.equalsIgnoreCase("var") || s.equalsIgnoreCase("variable") || s.equalsIgnoreCase("value") || s.equalsIgnoreCase("index");
	}

	@Override
	public Expression<?> getSource() {
		return null;
	}

	@Override
	public Expression<? extends V> simplify() {
		return this;
	}

	@Override
	public @Nullable Class<?>[] acceptChange(Changer.ChangeMode mode) {
		if (isSingle() && mode == Changer.ChangeMode.SET)
			return CollectionUtils.array(Object.class);
		return CollectionUtils.array(Object[].class);
	}

	@Override
	public void change(Event e, Object[] delta, Changer.ChangeMode mode) {
		Object omega;
		if (delta == null) {
			omega = null;
		} else {
			omega = isSingle ? delta[0] : delta;
		}
		String variableName = this.name.getSingle(e);
		if (!isSingle) {
			switch (mode) {
				case SET:
					for (int i = 0; i < delta.length; i++) {
						// System.out.println("expr setVar: ("+name + Variables.SEPARATOR + i+", "+delta[i]+");");
						if (isLocal)
							Variables.setLocalVariable(variableName + Variables.SEPARATOR + i, delta[i], e);
						else
							Variables.setVariable(variableName + Variables.SEPARATOR + i, delta[i]);
					}
					break;
			}
		} else {
			switch (mode) {
				case SET:
					if (isLocal)
						Variables.setLocalVariable(variableName, omega, e);
					else {
						// System.out.println("expr setVar a: ("+name+", "+omega+");");
						Variables.setVariable(variableName, omega);
					}
					break;
				case DELETE:
					if (isLocal)
						Variables.setLocalVariable(variableName, omega, e);
					else
						Variables.setVariable(variableName, omega);
			}
		}
	}

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		return false;
	}

	@Override
	public String toString(Event e, boolean debug) {
		StringBuilder stringBuilder = new StringBuilder()
			.append("{");
		if (isLocal)
			stringBuilder.append(Variables.LOCAL_TOKEN);
		stringBuilder.append(this.name.toString(e, debug))
			.append("}");

		if (debug) {
			stringBuilder.append(" (");
			if (e != null) {
				stringBuilder.append(Classes.toString(getAll(e)))
					.append(", ");
			}
			stringBuilder.append("as ")
				.append(superType.getName())
				.append(")");
		}
		return stringBuilder.toString();
	}

	@Override
	public String toString() {
		return toString(null, false);
	}

	public boolean isLocal() {
		return isLocal;
	}

	public Iterator<Pair<String, Object>> variablesIterator(Event e) {
		if (isSingle())
			throw new SkriptAPIException("Looping a non-list variable");
		String name = StringUtils.substring(this.name.toString(e), 0, -1);
		Object val = Variables.getVariable(name + "*");
		if (val == null)
			return new EmptyIterator<>();
		// temporary list to prevent CMEs
		@SuppressWarnings("unchecked")
		Iterator<String> keys = new ArrayList<>(((Map<String, Object>) val).keySet()).iterator();
		return new Iterator<Pair<String, Object>>() {
			@Nullable
			private String key;
			@Nullable
			private Object next = null;

			@Override
			public boolean hasNext() {
				if (next != null)
					return true;
				while (keys.hasNext()) {
					key = keys.next();
					if (key != null) {
						if (next != null && !(next instanceof TreeMap))
							return true;
					}
				}
				next = null;
				return false;
			}

			@Override
			public Pair<String, Object> next() {
				if (!hasNext())
					throw new NoSuchElementException();
				Pair<String, Object> n = new Pair<>(key, next);
				next = null;
				return n;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public @Nullable Iterator<? extends V> iterator(Event e) {
		if (isSingle) {
			V item = getSingle(e);
			return item != null ? new SingleItemIterator<>(item) : null;
		}
		String name = StringUtils.substring(this.name.getSingle(e), 0, -1);
		Object val = Variables.getVariable(name + "*");
		if (val == null)
			return new EmptyIterator<>();
		@SuppressWarnings("unchecked")
		Iterator<String> keys = new ArrayList<>(((Map<String, Object>) val).keySet()).iterator();
		return new Iterator<V>() {
			@Nullable
			private String key;
			@Nullable
			private V next = null;

			@SuppressWarnings({"unchecked"})
			@Override
			public boolean hasNext() {
				if (next != null)
					return true;
				while (keys.hasNext()) {
					key = keys.next();
					if (key != null) {
						next = (V) Converters.convert(Variables.getVariable(name + key), types);
						if (next != null && !(next instanceof TreeMap))
							return true;
					}
				}
				next = null;
				return false;
			}

			@Override
			public V next() {
				if (!hasNext())
					throw new NoSuchElementException();
				V n = next;
				assert n != null;
				next = null;
				return n;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}


}
