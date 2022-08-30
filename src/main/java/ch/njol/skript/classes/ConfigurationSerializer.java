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
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Uses strings for serialisation because the whole ConfigurationSerializable interface is badly documented, and especially DelegateDeserialization doesn't work well with
 * Yggdrasil.
 *
 * @author Peter Güttinger
 */
public class ConfigurationSerializer<T extends ConfigurationSerializable> extends YggdrasilSerializer<T> {
	public static String serializeCS(final ConfigurationSerializable o) {
		final YamlConfiguration y = new YamlConfiguration();
		y.set("value", o);
		return "" + y.saveToString();
	}

	@SuppressWarnings("unchecked")
	@Nullable
	public static <T extends ConfigurationSerializable> T deserializeCS(final String s, final Class<T> c) {
		final YamlConfiguration y = new YamlConfiguration();
		try {
			y.loadFromString(s);
		} catch (final InvalidConfigurationException e) {
			return null;
		}
		final Object o = y.get("value");
		if (!c.isInstance(o))
			return null;
		return (T) o;
	}

	@Nullable
	public <E extends T> E newInstance(final Class<E> c) {
		assert false;
		return null;
	}

	@SuppressWarnings("unchecked")
	@Deprecated
	@Nullable
	public static <T extends ConfigurationSerializable> T deserializeCSOld(final String s, final Class<T> c) {
		final YamlConfiguration y = new YamlConfiguration();
		try {
			y.loadFromString(s.replace("\uFEFF", "\n"));
		} catch (final InvalidConfigurationException e) {
			return null;
		}
		final Object o = y.get("value");
		if (!c.isInstance(o))
			return null;
		return (T) o;
	}

	@Override
	public void serialize(YggdrasilWriter writer, @Nullable T value) {
		writer.writeString("value", serializeCS(value));
	}

	@Override
	public @Nullable T deserialize(YggdrasilReader reader) {
		final String value = reader.readString("value");
		if (value == null)
			return null;
		final ClassInfo<? extends T> info = this.getClassInfo();
		return deserializeCS(value, info.getC());
	}
}
