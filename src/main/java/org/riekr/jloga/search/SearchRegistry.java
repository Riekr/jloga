package org.riekr.jloga.search;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.IntFunction;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.Main;
import org.riekr.jloga.ext.ExtSearchRegistry;

public final class SearchRegistry {

	private static final LinkedHashMap<String, Entry> _ENTRIES = new LinkedHashMap<>();

	public static class Entry {
		public final          Object                       src;
		public final @NotNull String                       id;
		public final          IntFunction<SearchComponent> factory;
		public final          String                       description;

		public Entry(Object src, @NotNull String id, IntFunction<SearchComponent> factory, String description) {
			this.src = src;
			this.id = id;
			this.factory = factory;
			this.description = description;
		}

		public SearchComponent newInstance(int level) {
			return factory.apply(level);
		}

		@Override
		public String toString() {
			return description;
		}

		@Override public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			Entry entry = (Entry)o;
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

	public static void remove(Entry entry) {
		_ENTRIES.computeIfPresent(entry.id, (id, prev) -> {
			if (prev.src == Main.class) {
				System.err.println("Can't remove built-in search");
				return prev;
			}
			if (Objects.equals(entry.src, prev.src))
				return null;
			return prev;
		});
	}

	public static void register(Entry entry) {
		_ENTRIES.putIfAbsent(entry.id, entry);
	}

	static {
		register(new Entry(Main.class, RegExComponent.ID, RegExComponent::new, "Regular Expressions"));
		register(new Entry(Main.class, PlainTextComponent.ID, PlainTextComponent::new, "Plain Text"));
		register(new Entry(Main.class, DurationAnalysisComponent.ID, DurationAnalysisComponent::new, "Duration Analysis"));
		register(new Entry(Main.class, FrequencyAnalysisComponent.ID, FrequencyAnalysisComponent::new, "Frequency Analysis"));
		register(new Entry(Main.class, UniqueSearchComponent.ID, UniqueSearchComponent::new, "Unique pattern search"));
		ExtSearchRegistry.init();
	}

	public static Entry[] getChoices() {
		return _ENTRIES.values().toArray(Entry[]::new);
	}

	public static SearchComponent get(String id, int level) {
		Entry res = _ENTRIES.get(id);
		if (res == null) {
			res = _ENTRIES.values().iterator().next();
			System.err.println("ID " + id + " not registered");
		}
		return res.newInstance(level);
	}

	private SearchRegistry() {}
}
