package org.riekr.jloga.misc;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class TaggedHolder<T> {

	public final @NotNull String tag;
	public final T value;

	public TaggedHolder(@NotNull String tag, T value) {
		this.tag = tag;
		this.value = value;
	}

	@Override
	public String toString() {
		return tag;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof TaggedHolder))
			return false;
		TaggedHolder<?> that = (TaggedHolder<?>) o;
		return Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {
		return value != null ? value.hashCode() : 0;
	}
}
