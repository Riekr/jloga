package org.riekr.jloga.prefs;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.react.Observable;
import org.riekr.jloga.react.Observer;
import org.riekr.jloga.react.Unsubscribable;

public interface Preference<T> extends Observable<T> {

	T get();

	boolean set(T t);

	T reset();

	static <T extends Serializable> KeyedPreference<T> of(String key, Supplier<T> deflt) {
		return new KeyedPreference<>(key, deflt);
	}

	static <T extends Enum<T>> Preference<T> of(String key, Supplier<T> deflt, Class<T> cl) {
		return Preference.of(key, () -> {
			T d = deflt.get();
			return d == null ? null : d.toString();
		}).withConversion(
				(s) -> Enum.valueOf(cl, s),
				Enum::toString
		);
	}

	static Preference<Charset> of(String key, Charset deflt) {
		return Preference.of(key,
				() -> deflt == null ? Charset.defaultCharset().name() : deflt.name()).withConversion(
				Charset::forName,
				Charset::name
		);
	}

	default <R> Preference<R> withConversion(Function<T, R> encoder, Function<R, T> decoder) {
		Preference<T> self = this;
		Observable<R> o = self.map((t) -> {
			try {
				return t == null ? null : encoder.apply(t);
			} catch (Throwable e) {
				t = self.reset();
				return t == null ? null : encoder.apply(t);
			}
		});
		return new Preference<>() {
			@Override
			public @NotNull Unsubscribable subscribe(Observer<? super R> observer) {
				return o.subscribe(observer);
			}

			@Override
			public R get() {
				T t = self.get();
				try {
					return t == null ? null : encoder.apply(t);
				} catch (Throwable e) {
					t = self.reset();
					return t == null ? null : encoder.apply(t);
				}
			}

			@Override
			public boolean set(R r) {
				return self.set(r == null ? null : decoder.apply(r));
			}

			@Override
			public R reset() {
				T t = self.reset();
				return t == null ? null : encoder.apply(t);
			}
		};
	}

	static <T extends Serializable & Comparable<T>> Preference<T> of(String key, T deflt, T min, T max) {
		Preference<T> orig = of(key, deflt == null ? null : () -> deflt);
		return new Preference<>() {
			@Override
			public @NotNull Unsubscribable subscribe(Observer<? super T> observer) {
				return orig.subscribe(observer);
			}

			private T limit(T val) {
				if (val != null) {
					if (min != null && min.compareTo(val) > 0)
						return min;
					if (max != null && max.compareTo(val) < 0)
						return max;
				}
				return val;
			}

			@Override
			public T get() {return limit(orig.get());}

			@Override
			public boolean set(T val) {return orig.set(limit(val));}

			@Override
			public T reset() {return orig.reset();}
		};
	}

	default GUIPreference<T> describe(GUIPreference.Type type, String title) {
		return new GUIPreference<>(this, type, title);
	}

}
