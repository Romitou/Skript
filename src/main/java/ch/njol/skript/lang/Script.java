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
package ch.njol.skript.lang;

import ch.njol.skript.config.Config;
import ch.njol.skript.lang.parser.ParserInstance;
import org.eclipse.jdt.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

public class Script {

	private final Config config;

	/**
	 * Creates a new Script to be used across the API.
	 * Only one script should be created per Config. A loaded script may be obtained through {@link ch.njol.skript.ScriptLoader}.
	 * @param config The Config containing the contents of this script.
	 */
	public Script(Config config) {
		this.config = config;
	}

	/**
	 * @return The Config representing the structure of this script.
	 */
	public Config getConfig() {
		return config;
	}

	// Warning Suppressions

	private final Set<ScriptWarning> suppressedWarnings = new HashSet<>();

	/**
	 * @param warning The warning to check.
	 * @return Whether this script suppresses the provided warning.
	 */
	public boolean suppressesWarning(ScriptWarning warning) {
		return suppressedWarnings.contains(warning);
	}

	/**
	 * @param warning Suppresses the provided warning for this script.
	 */
	public void suppressWarning(ScriptWarning warning) {
		suppressedWarnings.add(warning);
	}

	/**
	 * @param warning Allows the provided warning for this script.
	 */
	public void allowWarning(ScriptWarning warning) {
		suppressedWarnings.remove(warning);
	}

	public enum ScriptWarning {
		VARIABLE_SAVE, // Variable cannot be saved (the ClassInfo is not serializable)
		MISSING_CONJUNCTION, // Missing "and" or "or"
		VARIABLE_STARTS_WITH_EXPRESSION // Variable starts with an Expression
	}

	// Script Events

	/**
	 * Called when this Script is loaded.
	 * @param oldScript The script that was unloaded before this script started loading.
	 *                  Null if there was no current script in the {@link ParserInstance}.
	 */
	public void onLoad(@Nullable Script oldScript, ParserInstance parser) {

	}

	/**
	 * Called when this Script is unloaded.
	 * @param newScript The script being loaded after this script is unloaded.
	 *                  Null if there is no script being loaded after this in the {@link ParserInstance}.
	 */
	public void onUnload(@Nullable Script newScript, ParserInstance parser) {

	}

}
