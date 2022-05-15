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
package ch.njol.skript.structures;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.bukkitutil.CommandReloader;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.command.Argument;
import ch.njol.skript.command.CommandEvent;
import ch.njol.skript.command.Commands;
import ch.njol.skript.command.ScriptCommand;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.Script;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.VariableString;
import ch.njol.skript.lang.structure.EntryContainer;
import ch.njol.skript.lang.structure.KeyValueStructureEntryData;
import ch.njol.skript.lang.structure.Structure;
import ch.njol.skript.lang.structure.StructureEntryValidator;
import ch.njol.skript.lang.structure.util.LiteralStructureEntryData;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.StringMode;
import ch.njol.skript.util.Timespan;
import ch.njol.skript.util.Utils;
import ch.njol.util.NonNullPair;
import ch.njol.util.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StructCommand extends Structure {

	public static final Priority PRIORITY = new Priority(500);

	private static final Pattern
		COMMAND_PATTERN = Pattern.compile("(?i)^command /?(\\S+)\\s*(\\s+(.+))?$"),
		ARGUMENT_PATTERN = Pattern.compile("<\\s*(?:(.+?)\\s*:\\s*)?(.+?)\\s*(?:=\\s*(" + SkriptParser.wildcard + "))?\\s*>"),
		DESCRIPTION_PATTERN = Pattern.compile("(?<!\\\\)%-?(.+?)%");

	private static final AtomicBoolean syncCommands = new AtomicBoolean();

	static {
		Skript.registerStructure(
			StructCommand.class,
			StructureEntryValidator.builder()
				.addEntry("usage", true)
				.addEntry("description", "")
				.addEntry("permission", "")
				.addEntryData(new KeyValueStructureEntryData<VariableString>("permission message", true) {
					@Override
					@Nullable
					public VariableString getValue(String value) {
						return VariableString.newInstance(value.replace("\"", "\"\""));
					}
				})
				.addEntryData(new KeyValueStructureEntryData<List<String>>("aliases", new ArrayList<>()) {
					private final Pattern pattern = Pattern.compile("\\s*,\\s*/?");

					@Override
					public List<String> getValue(String value) {
						List<String> aliases = new ArrayList<>(Arrays.asList(pattern.split(value)));
						if (aliases.get(0).startsWith("/")) {
							aliases.set(0, aliases.get(0).substring(1));
						} else if (aliases.get(0).isEmpty()) {
							aliases = new ArrayList<>(0);
						}
						return aliases;
					}
				})
				.addEntryData(new KeyValueStructureEntryData<Integer>("executable by", ScriptCommand.CONSOLE | ScriptCommand.PLAYERS) {
					private final Pattern pattern = Pattern.compile("\\s*,\\s*|\\s+(and|or)\\s+");

					@Override
					@Nullable
					public Integer getValue(String value) {
						int executableBy = 0;
						for (String b : pattern.split(value)) {
							if (b.equalsIgnoreCase("console") || b.equalsIgnoreCase("the console")) {
								executableBy |= ScriptCommand.CONSOLE;
							} else if (b.equalsIgnoreCase("players") || b.equalsIgnoreCase("player")) {
								executableBy |= ScriptCommand.PLAYERS;
							} else {
								return null;
							}
						}
						return executableBy;
					}
				})
				.addEntryData(new LiteralStructureEntryData<>("cooldown", true, Timespan.class))
				.addEntryData(new KeyValueStructureEntryData<VariableString>("cooldown message", true) {
					@Override
					@Nullable
					public VariableString getValue(String value) {
						return VariableString.newInstance(value.replace("\"", "\"\""));
					}
				})
				.addEntry("cooldown bypass", true)
				.addEntryData(new KeyValueStructureEntryData<VariableString>("cooldown storage", true) {
					@Override
					@Nullable
					public VariableString getValue(String value) {
						return VariableString.newInstance(value, StringMode.VARIABLE_NAME);
					}
				})
				.addSection("trigger", false)
				.build(),
			"command <.+>"
		);
	}

	@Nullable
	private ScriptCommand scriptCommand;
	@SuppressWarnings("NotNullFieldNotInitialized")
	private EntryContainer entryContainer;

	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult, EntryContainer entryContainer) {
		this.entryContainer = entryContainer;
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void load() {
		getParser().setCurrentEvent("command", CommandEvent.class);

		String fullCommand = entryContainer.getSource().getKey();
		assert fullCommand != null;
		fullCommand = ScriptLoader.replaceOptions(fullCommand);

		int level = 0;
		for (int i = 0; i < fullCommand.length(); i++) {
			if (fullCommand.charAt(i) == '[') {
				level++;
			} else if (fullCommand.charAt(i) == ']') {
				if (level == 0) {
					Skript.error("Invalid placement of [optional brackets]");
					return;
				}
				level--;
			}
		}
		if (level > 0) {
			Skript.error("Invalid amount of [optional brackets]");
			return;
		}

		Matcher matcher = COMMAND_PATTERN.matcher(fullCommand);
		boolean matches = matcher.matches();
		assert matches;

		String command = matcher.group(1).toLowerCase();
		ScriptCommand existingCommand = Commands.getScriptCommand(command);
		if (existingCommand != null && existingCommand.getLabel().equals(command)) {
			String fileName = "";
			Script script = existingCommand.getScript();
			if (script != null) {
				File scriptFile = script.getConfig().getFile();
				if (scriptFile != null)
					fileName = " in " + scriptFile.getName();
			}
			Skript.error("A command with the name /" + existingCommand.getName() + " is already defined" + fileName);
			return;
		}

		String arguments = matcher.group(3) == null ? "" : matcher.group(3);
		StringBuilder pattern = new StringBuilder();

		List<Argument<?>> currentArguments = Commands.currentArguments = new ArrayList<>(); //Mirre
		matcher = ARGUMENT_PATTERN.matcher(arguments);
		int lastEnd = 0;
		int optionals = 0;
		for (int i = 0; matcher.find(); i++) {
			pattern.append(Commands.escape(arguments.substring(lastEnd, matcher.start())));
			optionals += StringUtils.count(arguments, '[', lastEnd, matcher.start());
			optionals -= StringUtils.count(arguments, ']', lastEnd, matcher.start());

			lastEnd = matcher.end();

			ClassInfo<?> c;
			c = Classes.getClassInfoFromUserInput(matcher.group(2));
			NonNullPair<String, Boolean> p = Utils.getEnglishPlural(matcher.group(2));
			if (c == null)
				c = Classes.getClassInfoFromUserInput(p.getFirst());
			if (c == null) {
				Skript.error("Unknown type '" + matcher.group(2) + "'");
				return;
			}
			Parser<?> parser = c.getParser();
			if (parser == null || !parser.canParse(ParseContext.COMMAND)) {
				Skript.error("Can't use " + c + " as argument of a command");
				return;
			}

			Argument<?> arg = Argument.newInstance(matcher.group(1), c, matcher.group(3), i, !p.getSecond(), optionals > 0);
			if (arg == null)
				return;
			currentArguments.add(arg);

			if (arg.isOptional() && optionals == 0) {
				pattern.append('[');
				optionals++;
			}
			pattern.append("%").append(arg.isOptional() ? "-" : "").append(Utils.toEnglishPlural(c.getCodeName(), p.getSecond())).append("%");
		}

		pattern.append(Commands.escape("" + arguments.substring(lastEnd)));
		optionals += StringUtils.count(arguments, '[', lastEnd);
		optionals -= StringUtils.count(arguments, ']', lastEnd);
		for (int i = 0; i < optionals; i++)
			pattern.append(']');

		// TODO this is only needed sometimes for non-specified usage message OR if Skript is running with high verbosity/debug - consider only doing this if needed
		String desc = "/" + command + " ";
		desc += StringUtils.replaceAll(pattern, DESCRIPTION_PATTERN, m1 -> {
			assert m1 != null;
			NonNullPair<String, Boolean> p = Utils.getEnglishPlural("" + m1.group(1));
			String s1 = p.getFirst();
			return "<" + Classes.getClassInfo(s1).getName().toString(p.getSecond()) + ">";
		});
		desc = Commands.unescape(desc).trim();

		String usage = (String) entryContainer.getParsed("usage");
		if (usage == null) {
			usage = Commands.m_correct_usage + " " + desc;
		}

		String description = (String) entryContainer.getNonNullParsed("description");

		String permission = (String) entryContainer.getNonNullParsed("permission");
		VariableString permissionMessage = (VariableString) entryContainer.getParsed("permission message");
		if (permissionMessage != null && permission.isEmpty())
			Skript.warning("command /" + command + " has a permission message set, but not a permission");

		List<String> aliases = (List<String>) entryContainer.getNonNullParsed("aliases");
		int executableBy = (Integer) entryContainer.getNonNullParsed("executable by");

		Timespan cooldown = (Timespan) entryContainer.getParsed("cooldown");
		VariableString cooldownMessage = (VariableString) entryContainer.getParsed("cooldown message");
		if (cooldownMessage != null && cooldown == null)
			Skript.warning("command /" + command + " has a cooldown message set, but not a cooldown");
		String cooldownBypass = (String) entryContainer.getParsed("cooldown bypass");
		if (cooldownBypass == null) {
			cooldownBypass = "";
		} else if (cooldownBypass.isEmpty() && cooldown == null) {
			Skript.warning("command /" + command + " has a cooldown bypass set, but not a cooldown");
		}
		VariableString cooldownStorage = (VariableString) entryContainer.getParsed("cooldown storage");
		if (cooldownStorage != null && cooldown == null)
			Skript.warning("command /" + command + " has a cooldown storage set, but not a cooldown");

		SectionNode node = entryContainer.getSource();

		if (Skript.debug() || node.debug())
			Skript.debug("command " + desc + ":");

		Commands.currentArguments = currentArguments;
		try {
			//noinspection ConstantConditions
			scriptCommand = new ScriptCommand(getParser().getCurrentScript(), command, pattern.toString(), currentArguments, description, usage,
				aliases, permission, permissionMessage, cooldown, cooldownMessage, cooldownBypass, cooldownStorage,
				executableBy, ScriptLoader.loadItems((SectionNode) entryContainer.getNonNullParsed("trigger")));
			scriptCommand.trigger.setLineNumber(node.getLine());
		} finally {
			Commands.currentArguments = null;
		}

		if (Skript.logVeryHigh() && !Skript.debug())
			Skript.info("Registered command " + desc);

		getParser().deleteCurrentEvent();

		Commands.registerCommand(scriptCommand);
		syncCommands.set(true);
	}

	@Override
	public void postLoad() {
		// TODO consider a message about number of scripts loaded for debug (it was removed when commands were internalized)
		if (syncCommands.get()) {
			syncCommands.set(false);
			if (CommandReloader.syncCommands(Bukkit.getServer())) {
				Skript.debug("Commands synced to clients");
			} else {
				Skript.debug("Commands changed but not synced to clients (normal on 1.12 and older)");
			}
		}
	}

	@Override
	public void unload() {
		if (scriptCommand != null) {
			syncCommands.set(true);
			Commands.unregisterCommand(scriptCommand);
		}
	}

	@Override
	public Priority getPriority() {
		return PRIORITY;
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return "command";
	}

}
