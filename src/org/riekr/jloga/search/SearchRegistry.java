package org.riekr.jloga.search;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashSet;
import java.util.function.Consumer;

public final class SearchRegistry {

	public static class Entry<T extends JComponent & SearchComponent> {
		public final Class<T> comp;
		public final String description;

		public Entry(Class<T> comp, String description) {
			this.comp = comp;
			this.description = description;
		}

		public T newInstance(int level) {
			try {
				return comp.getConstructor(Integer.TYPE).newInstance(level);
			} catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException ignored) {
			}
			try {
				return comp.getConstructor().newInstance();
			} catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException ignored) {
			}
			throw new UnsupportedOperationException("No constructor found");
		}

		@Override
		public final String toString() {
			return description;
		}

		@Override
		public final boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof Entry)) return false;
			Entry<?> entry = (Entry<?>) o;
			return comp.equals(entry.comp);
		}

		@Override
		public final int hashCode() {
			return comp.hashCode();
		}
	}

	private static final LinkedHashSet<Entry<?>> _ENTRIES = new LinkedHashSet<>();

	static {
		_ENTRIES.add(new Entry<>(RegExComponent.class, "Regular Expressions"));
		_ENTRIES.add(new Entry<>(PlainTextComponent.class, "Plain Text"));
	}

	@SuppressWarnings("unused")
	public static boolean add(Entry<?> entry) {
		return _ENTRIES.add(entry);
	}

	public static Entry<?>[] getChoices() {
		return _ENTRIES.toArray(new Entry[0]);
	}

	public static <T extends JComponent & SearchComponent> void get(Class<?> cl, int level, Consumer<? super T> consumer) {
		for (Entry<?> e : _ENTRIES) {
			if (e.comp == cl) {
				consumer.accept((T) e.newInstance(level));
				return;
			}
		}
		throw new IllegalArgumentException("Class " + cl.getCanonicalName() + " not registered");
	}

	private SearchRegistry() {
	}
}
