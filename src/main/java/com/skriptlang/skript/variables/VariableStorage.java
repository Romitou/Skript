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

import ch.njol.skript.config.SectionNode;
import com.skriptlang.skript.old.Variable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public interface VariableStorage {

	@NotNull String getStorageName();

	boolean configure(SectionNode node);

	HashMap<String, Object> getPreloadedVariables();

	Variable<?> getVariable(String name);

	void setVariable(String name, Object value);

	void deleteVariable(String name);

	void close();

}
