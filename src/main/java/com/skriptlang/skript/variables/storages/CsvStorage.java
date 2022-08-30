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
package com.skriptlang.skript.variables.storages;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import com.skriptlang.skript.old.Variable;
import com.skriptlang.skript.variables.VariableStorage;
import com.skriptlang.skript.yggdrasil.*;
import org.eclipse.jdt.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CsvStorage implements VariableStorage {

	private final static Pattern CSV_PATTERN = Pattern.compile("(?<=^|,)\\s*([^\",]*|\"([^\"]|\"\")*\")\\s*(,|$)");
	private final BlockingQueue<String> WRITING_QUEUE = new LinkedBlockingQueue<>();
	private File file;

	@Override
	public @NotNull String getStorageName() {
		return "csv";
	}

	@Override
	public boolean configure(SectionNode node) {
		String filePath = node.getValue("file");
		if (filePath == null) {
			Skript.error("Cannot find 'file' option which is required for CSV storage");
			return false;
		}

		file = new File(filePath);
		try {
			if (file.createNewFile()) {
				Skript.info("Created new CSV file: " + file.getPath());
			} else {
				Skript.info("Opened existing CSV file: " + file.getPath());
			}
		} catch (Exception e) {
			Skript.error("Could not create file '" + file.getPath() + "' for CSV storage");
			return false;
		}

		startWritingThread();
		return true;
	}

	@Override
	public Variable<?> getVariable(String name) {
		return null; //TODO
	}

	@Override
	public HashMap<String, Object> getPreloadedVariables() {
		HashMap<String, Object> preloadedVariables = new HashMap<>();

		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;
			int lineNumber = 0;
			int success = 0;
			while ((line = reader.readLine()) != null) {
				// Schema of values : (0) variable's name, (1) classInfo's name, (2) field 1 name, (3) field 1 class,
				// (4) field 1 value, (5) field 2 name...
				String[] values = splitLine(line);

				lineNumber++;

				if (values == null || values.length < 3) {
					Skript.error("Invalid CSV line " + lineNumber + " in CSV file '" + file.getPath() + "'");
					continue;
				}

				try {
					Object deserialized;

					ClassInfo<?> classInfo = Classes.getClassInfo(values[1]);
					if (classInfo.safeSerialization()) {
						deserialized = classInfo.getParser().parse(values[2], ParseContext.CONFIG);
					} else {
						YggdrasilWriter data = new YggdrasilWriter(classInfo);
						for (int i = 2; i < values.length; i = i + 3) {
							ClassInfo<?> fieldClassInfo = Classes.getClassInfo(values[i + 1]);
							data.write(values[i], fieldClassInfo.getParser().parse(values[i + 2], ParseContext.CONFIG));
						}
						deserialized = classInfo.getSerializer().deserialize(data.toReader());
					}

					if (deserialized == null) {
						Skript.error("Could not deserialize data from CSV line " + lineNumber + " in CSV file '" + file.getPath() + "'");
						continue;
					}

					preloadedVariables.put(values[0], deserialized);
					success++;

				} catch (SkriptAPIException e) {
					Skript.error("Invalid class name '" + values[1] + "' at line " + lineNumber + " in CSV file '" + file.getPath() + "'");
				}
			}

			Skript.info("Preloaded " + success + " variables from CSV file '" + file.getPath() + "'");
			int errored = lineNumber - success;
			if (errored > 0)
				Skript.error("Could not preload " + errored + " variables from CSV file '" + file.getPath() + "'");

		} catch (IOException e) {
			e.printStackTrace();
		}

		return preloadedVariables;
	}

	@Override
	public void setVariable(String name, Object value) {
		setSingleVariable(name, value);
	}

//	public void setNamedVariable(String name, Object value) {
//		Variable<?> lastVariable = variable;
//		while (lastVariable != null) {
//			if (lastVariable instanceof SingleVariable) {
//				lastVariable = ((SingleVariable<?>) lastVariable).getChild();
//			} else if (lastVariable instanceof ListVariable) {
//				((ListVariable<?>) lastVariable).getChildren().values().forEach(v -> setNamedVariable(name + "::" + v.getName(), v));
//				lastVariable = null;
//			}
//		}
//	}

	public <V> void setSingleVariable(String name, V value) {
		StringBuilder builder = new StringBuilder();

		ClassInfo<V> baseClassInfo = (ClassInfo<V>) Classes.getExactClassInfo(value.getClass());
		if (baseClassInfo == null)
			return;

		builder.append(name).append(",")
			.append(baseClassInfo.getCodeName());

		if (baseClassInfo.safeSerialization()) {
			builder.append(",").append(value);
			// System.out.println("Add to queue: " + builder.toString());
			WRITING_QUEUE.add(builder.toString());
			return;
		}

		YggdrasilSerializer<V> serializer = baseClassInfo.getSerializer();
		if (serializer == null)
			return;

		YggdrasilWriter writer = new YggdrasilWriter(baseClassInfo);
		serializer.serialize(writer, value);
		writer.toReader().readAll().forEach((k, v) -> {
			ClassInfo<?> classInfo = Classes.getExactClassInfo(v.getClass());
			if (classInfo == null)
				return;
			builder.append(",")
				.append(k).append(",")
				.append(classInfo.getCodeName()).append(",")
				.append(v);
		});

		WRITING_QUEUE.add(builder.toString());
	}

	@Override
	public void deleteVariable(String name) {
	}

	@Override
	public void close() {
		System.out.println("called close");
	}

	@Nullable
	private String[] splitLine(final String line) {
		final Matcher matcher = CSV_PATTERN.matcher(line);
		int lastEnd = 0;
		final ArrayList<String> r = new ArrayList<>();
		while (matcher.find()) {
			if (lastEnd != matcher.start())
				return null;
			final String v = matcher.group(1);
			if (v.startsWith("\""))
				r.add(v.substring(1, v.length() - 1).replace("\"\"", "\""));
			else
				r.add(v.trim());
			lastEnd = matcher.end();
		}
		if (lastEnd != line.length())
			return null;
		return r.toArray(new String[0]);
	}

	private void startWritingThread() {
		Thread writingThread = new Thread(() -> {
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
				while (!Thread.interrupted()) {
					String line = WRITING_QUEUE.take();
					// System.out.println("Writing: " + line);
					writer.write(line);
					writer.newLine();
					writer.flush();
				}
				writer.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		writingThread.start();
		Skript.info("Started writing thread for CSV storage");
	}
}
