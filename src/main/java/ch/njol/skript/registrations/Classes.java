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
package ch.njol.skript.registrations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.SequenceInputStream;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Converter;
import ch.njol.skript.classes.Converter.ConverterInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.lang.DefaultExpression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.localization.Language;
import ch.njol.skript.log.ParseLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.util.StringMode;
import ch.njol.util.Kleenean;
import ch.njol.util.StringUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author Peter Güttinger
 */
public abstract class Classes {

	private Classes() {}

	@Nullable
	private static ClassInfo<?>[] classInfos = null;
	private final static List<ClassInfo<?>> tempClassInfos = new ArrayList<>();
	private final static HashMap<Class<?>, ClassInfo<?>> exactClassInfos = new HashMap<>();
	private final static HashMap<Class<?>, ClassInfo<?>> superClassInfos = new HashMap<>();
	private final static HashMap<String, ClassInfo<?>> classInfosByCodeName = new HashMap<>();

	/**
	 * @param info info about the class to register
	 */
	public static <T> void registerClass(final ClassInfo<T> info) {
		try {
			Skript.checkAcceptRegistrations();
			if (classInfosByCodeName.containsKey(info.getCodeName()))
				throw new IllegalArgumentException("Can't register " + info.getC().getName() + " with the code name " + info.getCodeName() + " because that name is already used by " + classInfosByCodeName.get(info.getCodeName()));
			if (exactClassInfos.containsKey(info.getC()))
				throw new IllegalArgumentException("Can't register the class info " + info.getCodeName() + " because the class " + info.getC().getName() + " is already registered");
			exactClassInfos.put(info.getC(), info);
			classInfosByCodeName.put(info.getCodeName(), info);
			tempClassInfos.add(info);
		} catch (RuntimeException e) {
			if (SkriptConfig.apiSoftExceptions.value())
				Skript.warning("Ignored an exception due to user configuration: " + e.getMessage());
			else
				throw e;
		}
	}

	public static void onRegistrationsStop() {

		sortClassInfos();

		// validate serializeAs
		for (final ClassInfo<?> ci : getClassInfos()) {
			if (ci.getSerializeAs() != null) {
				final ClassInfo<?> sa = getExactClassInfo(ci.getSerializeAs());
				if (sa == null) {
					Skript.error(ci.getCodeName() + "'s 'serializeAs' class is not registered");
				} else if (sa.getSerializer() == null) {
					Skript.error(ci.getCodeName() + "'s 'serializeAs' class is not serializable");
				}
			}
		}
	}

	/**
	 * Sorts the class infos according to sub/superclasses and relations set with {@link ClassInfo#before(String...)} and {@link ClassInfo#after(String...)}.
	 */
	@SuppressFBWarnings("LI_LAZY_INIT_STATIC")
	private static void sortClassInfos() {
		assert classInfos == null;

		if (!Skript.testing() && SkriptConfig.addonSafetyChecks.value())
			removeNullElements();

		// merge before, after & sub/supertypes in after
		for (final ClassInfo<?> ci : tempClassInfos) {
			final Set<String> before = ci.before();
			if (before != null && !before.isEmpty()) {
				for (final ClassInfo<?> ci2 : tempClassInfos) {
					if (before.contains(ci2.getCodeName())) {
						ci2.after().add(ci.getCodeName());
						before.remove(ci2.getCodeName());
						if (before.isEmpty())
							break;
					}
				}
			}
		}
		for (final ClassInfo<?> ci : tempClassInfos) {
			for (final ClassInfo<?> ci2 : tempClassInfos) {
				if (ci == ci2)
					continue;
				if (ci.getC().isAssignableFrom(ci2.getC()))
					ci.after().add(ci2.getCodeName());
			}
		}

		// remove unresolvable dependencies (and print a warning if testing)
		for (final ClassInfo<?> ci : tempClassInfos) {
			final Set<String> s = new HashSet<>();
			final Set<String> before = ci.before();
			if (before != null) {
				for (final String b : before) {
					if (getClassInfoNoError(b) == null) {
						s.add(b);
					}
				}
				before.removeAll(s);
			}
			for (final String a : ci.after()) {
				if (getClassInfoNoError(a) == null) {
					s.add(a);
				}
			}
			ci.after().removeAll(s);
			if (!s.isEmpty() && Skript.testing())
				Skript.warning(s.size() + " dependency/ies could not be resolved for " + ci + ": " + StringUtils.join(s, ", "));
		}

		final List<ClassInfo<?>> classInfos = new ArrayList<>(tempClassInfos.size());

		boolean changed = true;
		while (changed) {
			changed = false;
			for (int i = 0; i < tempClassInfos.size(); i++) {
				final ClassInfo<?> ci = tempClassInfos.get(i);
				if (ci.after().isEmpty()) {
					classInfos.add(ci);
					tempClassInfos.remove(i);
					i--;
					for (final ClassInfo<?> ci2 : tempClassInfos)
						ci2.after().remove(ci.getCodeName());
					changed = true;
				}
			}
		}

		Classes.classInfos = classInfos.toArray(new ClassInfo[classInfos.size()]);

		// check for circular dependencies
		if (!tempClassInfos.isEmpty()) {
			final StringBuilder b = new StringBuilder();
			for (final ClassInfo<?> c : tempClassInfos) {
				if (b.length() != 0)
					b.append(", ");
				b.append(c.getCodeName() + " (after: " + StringUtils.join(c.after(), ", ") + ")");
			}
			throw new IllegalStateException("ClassInfos with circular dependencies detected: " + b.toString());
		}

		// debug message
		if (Skript.debug()) {
			final StringBuilder b = new StringBuilder();
			for (final ClassInfo<?> ci : classInfos) {
				if (b.length() != 0)
					b.append(", ");
				b.append(ci.getCodeName());
			}
			Skript.info("All registered classes in order: " + b.toString());
		}

	}

	@SuppressWarnings({"null", "unused"})
	private static void removeNullElements() {
		Iterator<ClassInfo<?>> it = tempClassInfos.iterator();
		while (it.hasNext()) {
			ClassInfo<?> ci = it.next();
			if (ci.getC() == null)
				it.remove();
		}
	}

	private static void checkAllowClassInfoInteraction() {
		if (Skript.isAcceptRegistrations())
			throw new IllegalStateException("Cannot use classinfos until registration is over");
	}

	@SuppressWarnings("null")
	public static List<ClassInfo<?>> getClassInfos() {
		checkAllowClassInfoInteraction();
		final ClassInfo<?>[] ci = classInfos;
		if (ci == null)
			return Collections.emptyList();
		return Collections.unmodifiableList(Arrays.asList(ci));
	}

	/**
	 * This method can be called even while Skript is loading.
	 *
	 * @param codeName
	 * @return The ClassInfo with the given code name
	 * @throws SkriptAPIException If the given class was not registered
	 */
	public static ClassInfo<?> getClassInfo(final String codeName) {
		final ClassInfo<?> ci = classInfosByCodeName.get(codeName);
		if (ci == null)
			throw new SkriptAPIException("No class info found for " + codeName);
		return ci;
	}

	/**
	 * This method can be called even while Skript is loading.
	 *
	 * @param codeName
	 * @return The class info registered with the given code name or null if the code name is invalid or not yet registered
	 */
	@Nullable
	public static ClassInfo<?> getClassInfoNoError(final @Nullable String codeName) {
		return classInfosByCodeName.get(codeName);
	}

	/**
	 * Gets the class info for the given class.
	 * <p>
	 * This method can be called even while Skript is loading.
	 *
	 * @param c The exact class to get the class info for.
	 * @return The class info for the given class or null if no info was found.
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static <T> ClassInfo<T> getExactClassInfo(final @Nullable Class<T> c) {
		return (ClassInfo<T>) exactClassInfos.get(c);
	}

	/**
	 * Gets the class info of the given class or its closest registered superclass. This method will never return null unless <tt>c</tt> is null.
	 *
	 * @param c
	 * @return The closest superclass's info
	 */
	@SuppressWarnings({"unchecked", "null"})
	public static <T> ClassInfo<? super T> getSuperClassInfo(final Class<T> c) {
		assert c != null;
		checkAllowClassInfoInteraction();
		final ClassInfo<?> i = superClassInfos.get(c);
		if (i != null)
			return (ClassInfo<? super T>) i;
		for (final ClassInfo<?> ci : getClassInfos()) {
			if (ci.getC().isAssignableFrom(c)) {
				if (!Skript.isAcceptRegistrations())
					superClassInfos.put(c, ci);
				return (ClassInfo<? super T>) ci;
			}
		}
		assert false;
		return null;
	}

	/**
	 * Gets a class by its code name
	 *
	 * @param codeName
	 * @return the class with the given code name
	 * @throws SkriptAPIException If the given class was not registered
	 */
	public static Class<?> getClass(final String codeName) {
		checkAllowClassInfoInteraction();
		return getClassInfo(codeName).getC();
	}

	/**
	 * As the name implies
	 *
	 * @param name
	 * @return the class info or null if the name was not recognised
	 */
	@Nullable
	public static ClassInfo<?> getClassInfoFromUserInput(String name) {
		checkAllowClassInfoInteraction();
		name = "" + name.toLowerCase(Locale.ENGLISH);
		for (final ClassInfo<?> ci : getClassInfos()) {
			final Pattern[] uip = ci.getUserInputPatterns();
			if (uip == null)
				continue;
			for (final Pattern pattern : uip) {
				if (pattern.matcher(name).matches())
					return ci;
			}
		}
		return null;
	}

	/**
	 * As the name implies
	 *
	 * @param name
	 * @return the class or null if the name was not recognized
	 */
	@Nullable
	public static Class<?> getClassFromUserInput(final String name) {
		checkAllowClassInfoInteraction();
		final ClassInfo<?> ci = getClassInfoFromUserInput(name);
		return ci == null ? null : ci.getC();
	}

	/**
	 * Gets the default of a class
	 *
	 * @param codeName
	 * @return the expression holding the default value or null if this class doesn't have one
	 * @throws SkriptAPIException If the given class was not registered
	 */
	@Nullable
	public static DefaultExpression<?> getDefaultExpression(final String codeName) {
		checkAllowClassInfoInteraction();
		return getClassInfo(codeName).getDefaultExpression();
	}

	/**
	 * Gets the default expression of a class
	 *
	 * @param c The class
	 * @return The expression holding the default value or null if this class doesn't have one
	 */
	@Nullable
	public static <T> DefaultExpression<T> getDefaultExpression(final Class<T> c) {
		checkAllowClassInfoInteraction();
		final ClassInfo<T> ci = getExactClassInfo(c);
		return ci == null ? null : ci.getDefaultExpression();
	}

	/**
	 * Clones the given object by calling {@link ClassInfo#clone(Object)},
	 * getting the {@link ClassInfo} from the closest registered superclass
	 * (or the given object's class). Supports arrays too.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static Object clone(Object obj) {
		if (obj == null)
			return null;
		if (obj.getClass().isArray()) {
			int length = Array.getLength(obj);
			Object clone = Array.newInstance(obj.getClass().getComponentType(), length);
			for (int i = 0; i < length; i++) {
				Array.set(clone, i, clone(Array.get(obj, i)));
			}
			return clone;
		} else {
			ClassInfo classInfo = getSuperClassInfo(obj.getClass());
			return classInfo.clone(obj);
		}
	}

	/**
	 * Gets the name a class was registered with.
	 *
	 * @param c The exact class
	 * @return The name of the class or null if the given class wasn't registered.
	 */
	@Nullable
	public static String getExactClassName(final Class<?> c) {
		checkAllowClassInfoInteraction();
		final ClassInfo<?> ci = exactClassInfos.get(c);
		return ci == null ? null : ci.getCodeName();
	}

	/**
	 * Parses without trying to convert anything.
	 * <p>
	 * Can log an error xor other log messages.
	 *
	 * @param s
	 * @param c
	 * @return The parsed object
	 */
	@Nullable
	public static <T> T parseSimple(final String s, final Class<T> c, final ParseContext context) {
		final ParseLogHandler log = SkriptLogger.startParseLogHandler();
		try {
			for (final ClassInfo<?> info : getClassInfos()) {
				final Parser<?> parser = info.getParser();
				if (parser == null || !parser.canParse(context) || !c.isAssignableFrom(info.getC()))
					continue;
				log.clear();
				@SuppressWarnings("unchecked")
				final T t = (T) parser.parse(s, context);
				if (t != null) {
					log.printLog();
					return t;
				}
			}
			log.printError();
		} finally {
			log.stop();
		}
		return null;
	}

	/**
	 * Parses a string to get an object of the desired type.
	 * <p>
	 * Instead of repeatedly calling this with the same class argument, you should get a parser with {@link #getParser(Class)} and use it for parsing.
	 * <p>
	 * Can log an error if it returned null.
	 *
	 * @param s The string to parse
	 * @param c The desired type. The returned value will be of this type or a subclass if it.
	 * @return The parsed object
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	@Nullable
	public static <T> T parse(final String s, final Class<T> c, final ParseContext context) {
		final ParseLogHandler log = SkriptLogger.startParseLogHandler();
		try {
			T t = parseSimple(s, c, context);
			if (t != null) {
				log.printLog();
				return t;
			}
			for (final ConverterInfo<?, ?> conv : Converters.getConverters()) {
				if (context == ParseContext.COMMAND && (conv.options & Converter.NO_COMMAND_ARGUMENTS) != 0)
					continue;
				if (c.isAssignableFrom(conv.to)) {
					log.clear();
					final Object o = parseSimple(s, conv.from, context);
					if (o != null) {
						t = (T) ((Converter) conv.converter).convert(o);
						if (t != null) {
							log.printLog();
							return t;
						}
					}
				}
			}
			log.printError();
		} finally {
			log.stop();
		}
		return null;
	}

	/**
	 * Gets a parser for parsing instances of the desired type from strings. The returned parser may only be used for parsing, i.e. you must not use its toString methods.
	 *
	 * @param to
	 * @return A parser to parse object of the desired type
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static <T> Parser<? extends T> getParser(final Class<T> to) {
		checkAllowClassInfoInteraction();
		final ClassInfo<?>[] classInfos = Classes.classInfos;
		if (classInfos == null)
			return null;
		for (int i = classInfos.length - 1; i >= 0; i--) {
			final ClassInfo<?> ci = classInfos[i];
			if (to.isAssignableFrom(ci.getC()) && ci.getParser() != null)
				return (Parser<? extends T>) ci.getParser();
		}
		for (final ConverterInfo<?, ?> conv : Converters.getConverters()) {
			if (to.isAssignableFrom(conv.to)) {
				for (int i = classInfos.length - 1; i >= 0; i--) {
					final ClassInfo<?> ci = classInfos[i];
					final Parser<?> parser = ci.getParser();
					if (conv.from.isAssignableFrom(ci.getC()) && parser != null)
						return Classes.createConvertedParser(parser, (Converter<?, ? extends T>) conv.converter);
				}
			}
		}
		return null;
	}

	/**
	 * Gets a parser for an exactly known class. You should usually use {@link #getParser(Class)} instead of this method.
	 * <p>
	 * The main benefit of this method is that it's the only class info method of Skript that can be used while Skript is initializing and thus useful for parsing configs.
	 *
	 * @param c
	 * @return A parser to parse object of the desired type
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static <T> Parser<? extends T> getExactParser(final Class<T> c) {
		if (Skript.isAcceptRegistrations()) {
			for (final ClassInfo<?> ci : tempClassInfos) {
				if (ci.getC() == c)
					return (Parser<? extends T>) ci.getParser();
			}
			return null;
		} else {
			final ClassInfo<T> ci = getExactClassInfo(c);
			return ci == null ? null : ci.getParser();
		}
	}

	private static <F, T> Parser<T> createConvertedParser(final Parser<?> parser, final Converter<F, T> converter) {
		return new Parser<T>() {
			@SuppressWarnings("unchecked")
			@Override
			@Nullable
			public T parse(final String s, final ParseContext context) {
				final Object f = parser.parse(s, context);
				if (f == null)
					return null;
				return converter.convert((F) f);
			}

			@Override
			public String toString(final T o, final int flags) {
				throw new UnsupportedOperationException();
			}

			@Override
			public String toVariableNameString(final T o) {
				throw new UnsupportedOperationException();
			}
        };
	}

	/**
	 * @param o Any object, preferably not an array: use {@link Classes#toString(Object[], boolean)} instead.
	 * @return String representation of the object (using a parser if found or {@link String#valueOf(Object)} otherwise).
	 * @see #toString(Object, StringMode)
	 * @see #toString(Object[], boolean)
	 * @see #toString(Object[], boolean, StringMode)
	 * @see Parser
	 */
	public static String toString(final @Nullable Object o) {
		return toString(o, StringMode.MESSAGE, 0);
	}

	public static String getDebugMessage(final @Nullable Object o) {
		return toString(o, StringMode.DEBUG, 0);
	}

	public static <T> String toString(final @Nullable T o, final StringMode mode) {
		return toString(o, mode, 0);
	}

	private static <T> String toString(final @Nullable T o, final StringMode mode, final int flags) {
		assert flags == 0 || mode == StringMode.MESSAGE;
		if (o == null)
			return Language.get("none");
		if (o.getClass().isArray()) {
			if (((Object[]) o).length == 0)
				return Language.get("none");
			final StringBuilder b = new StringBuilder();
			boolean first = true;
			for (final Object i : (Object[]) o) {
				if (!first)
					b.append(", ");
				b.append(toString(i, mode, flags));
				first = false;
			}
			return "[" + b.toString() + "]";
		}
		for (final ClassInfo<?> ci : getClassInfos()) {
			final Parser<?> parser = ci.getParser();
			if (parser != null && ci.getC().isInstance(o)) {
				@SuppressWarnings("unchecked")
				final String s = mode == StringMode.MESSAGE ? ((Parser<T>) parser).toString(o, flags)
						: mode == StringMode.DEBUG ? "[" + ci.getCodeName() + ":" + ((Parser<T>) parser).toString(o, mode) + "]"
								: ((Parser<T>) parser).toString(o, mode);
				return s;
			}
		}
		return mode == StringMode.VARIABLE_NAME ? "object:" + o : "" + o;
	}

	public static String toString(final Object[] os, final int flags, final boolean and) {
		return toString(os, and, null, StringMode.MESSAGE, flags);
	}

	public static String toString(final Object[] os, final int flags, final @Nullable ChatColor c) {
		return toString(os, true, c, StringMode.MESSAGE, flags);
	}

	public static String toString(final Object[] os, final boolean and) {
		return toString(os, and, null, StringMode.MESSAGE, 0);
	}

	public static String toString(final Object[] os, final boolean and, final StringMode mode) {
		return toString(os, and, null, mode, 0);
	}

	private static String toString(final Object[] os, final boolean and, final @Nullable ChatColor c, final StringMode mode, final int flags) {
		if (os.length == 0)
			return toString(null);
		if (os.length == 1)
			return toString(os[0], mode, flags);
		final StringBuilder b = new StringBuilder();
		for (int i = 0; i < os.length; i++) {
			if (i != 0) {
				if (c != null)
					b.append(c.toString());
				if (i == os.length - 1)
					b.append(and ? " and " : " or ");
				else
					b.append(", ");
			}
			b.append(toString(os[i], mode, flags));
		}
		return "" + b.toString();
	}


	private static boolean equals(final @Nullable Object o, final @Nullable Object d) {
		if (o instanceof Chunk) { // CraftChunk does neither override equals nor is it a "coordinate-specific singleton" like Block
			if (!(d instanceof Chunk))
				return false;
			final Chunk c1 = (Chunk) o, c2 = (Chunk) d;
			return c1.getWorld().equals(c2.getWorld()) && c1.getX() == c2.getX() && c1.getZ() == c2.getZ();
		}
		return o == null ? d == null : o.equals(d);
	}
}
