package org.riekr.jloga.ui;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

public class ComboEntryWrapper<T> {

	public static int indexOf(Object value, ComboEntryWrapper<?>[] arr) {
		for (int i = 0, arrLength = arr.length; i < arrLength; i++) {
			final ComboEntryWrapper<?> wrapper = arr[i];
			if (Objects.equals(value, wrapper.value))
				return i;
		}
		return -1;
	}

	public final @NotNull String name;
	public final          T      value;

	public ComboEntryWrapper(@NotNull Map.Entry<String, T> entry) {
		this(entry.getKey(), entry.getValue());
	}

	public ComboEntryWrapper(@NotNull String name, T value) {
		this.name = name;
		this.value = value;
	}

	@Override public String toString() {
		return name;
	}

}
