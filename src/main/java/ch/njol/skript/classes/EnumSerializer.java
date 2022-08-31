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
package ch.njol.skript.classes;

import com.skriptlang.skript.yggdrasil.YggdrasilReader;
import com.skriptlang.skript.yggdrasil.YggdrasilSerializer;
import com.skriptlang.skript.yggdrasil.YggdrasilWriter;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Mainly kept for backwards compatibility, but also serves as {@link ClassResolver} for enums.
 *
 * @author Peter Güttinger
 */
public class EnumSerializer<T extends Enum<T>> extends YggdrasilSerializer<T> {

	private final Class<T> c;

	public EnumSerializer(final Class<T> c) {
		this.c = c;
	}

	@Override
	public void serialize(YggdrasilWriter writer, @Nullable T object) {
		throw new IllegalStateException(); // not used
	}

	@Override
	public @Nullable T deserialize(YggdrasilReader reader) {
		return Enum.valueOf(c, reader.readString("enum"));
	}
}
