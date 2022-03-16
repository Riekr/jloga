package org.riekr.jloga.search;

import javax.swing.*;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.ext.ExtSearchRegistry;

public final class SearchRegistry {

	private static final LinkedHashMap<String, Entry<?>> _ENTRIES = new LinkedHashMap<>();

	public static class Entry<T extends JComponent & SearchComponent> {
		public final @NotNull String         id;
		public final          IntFunction<T> factory;
		public final          String         description;

		public Entry(@NotNull String id, IntFunction<T> factory, String description) {
			this.id = id;
			this.factory = factory;
			this.description = description;
		}

		public T newInstance(int level) {
			return factory.apply(level);
		}

		@Override
		public final String toString() {
			return description;
		}

		@Override public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			Entry<?> entry = (Entry<?>)o;
			if (!id.equals(entry.id))
				return false;
			return Objects.equals(description, entry.description);
		}

		@Override
		public int hashCode() {
			int result = id.hashCode();
			result = 31 * result + (description != null ? description.hashCode() : 0);
			return result;
		}
	}

	public static void remove(Entry<?> entry) {
		_ENTRIES.remove(entry.id);
	}

	public static void register(Entry<?> entry) {
		_ENTRIES.put(entry.id, entry);
	}

	static {
		register(new Entry<>(RegExComponent.ID, RegExComponent::new, "Regular Expressions"));
		register(new Entry<>(PlainTextComponent.ID, PlainTextComponent::new, "Plain Text"));
		register(new Entry<>(DurationAnalysisComponent.ID, DurationAnalysisComponent::new, "Duration Analysis"));
		register(new Entry<>(FrequencyAnalysisComponent.ID, FrequencyAnalysisComponent::new, "Frequency Analysis"));
		register(new Entry<>(UniqueSearchComponent.ID, UniqueSearchComponent::new, "Unique pattern search"));
		ExtSearchRegistry.init();
	}

	public static Entry<?>[] getChoices() {
		return _ENTRIES.values().toArray(new Entry[0]);
	}

	@SuppressWarnings("unchecked")
	public static <T extends JComponent & SearchComponent> void get(String id, int level, Consumer<? super T> consumer) {
		Entry<?> res = _ENTRIES.get(id);
		if (res == null) {
			res = _ENTRIES.values().iterator().next();
			System.err.println("ID " + id + " not registered");
		}
		consumer.accept((T)res.newInstance(level));
	}

	private SearchRegistry() {}
}
