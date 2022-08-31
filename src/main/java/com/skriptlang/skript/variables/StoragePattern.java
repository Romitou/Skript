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
package com.skriptlang.skript.variables;

import java.util.regex.Pattern;

public class StoragePattern {

	private final Pattern regex;
	private final String startsWith;

	private StoragePattern(Pattern regex, String startsWith) {
		this.regex = regex;
		this.startsWith = startsWith;
	}

	public static StoragePattern startsWith(String startsWith) {
		return new StoragePattern(null, startsWith);
	}

	public static StoragePattern regex(Pattern regex) {
		return new StoragePattern(regex, null);
	}

	public boolean matches(String name) {
		if (startsWith != null) {
			return name.startsWith(startsWith);
		} else {
			return regex.matcher(name).matches();
		}
	}

	@Override
	public String toString() {
		return "StoragePattern{" +
			"regex=" + regex +
			", startsWith='" + startsWith + '\'' +
			'}';
	}
}
