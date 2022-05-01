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
package ch.njol.skript.lang.structure.util;

import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.structure.KeyValueStructureEntryData;
import ch.njol.skript.registrations.Classes;
import org.eclipse.jdt.annotation.Nullable;

public class LiteralStructureEntryData<T> extends KeyValueStructureEntryData<T> {

	private final Class<T> type;

	public LiteralStructureEntryData(String key, Class<T> type) {
		super(key);
		this.type = type;
	}

	public LiteralStructureEntryData(String key, T defaultValue, Class<T> type) {
		super(key, defaultValue);
		this.type = type;
	}

	public LiteralStructureEntryData(String key, boolean optional, Class<T> type) {
		super(key, optional);
		this.type = type;
	}

	@Override
	@Nullable
	public T getValue(String value) {
		return Classes.parse(value, type, ParseContext.DEFAULT);
	}

}
