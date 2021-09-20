package org.riekr.jloga.search.simple;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.io.Preferences;
import org.riekr.jloga.search.SearchPredicate;

public class SimpleSearchPredicate {

	private static final String _PREF_MODEL = "Multithreading.model";

	public interface Factory {
		SearchPredicate from(@NotNull Predicate<String> predicate);

		SearchPredicate from(@NotNull Supplier<Predicate<String>> predicateSupplier);

		String description();
	}

	/** * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
	 * Should we use multithreading searches?
	 * 1. this only works for simple search, anaysis tasks are still not supported
	 * 2. for very fast search operations multithreading overhead is an overkill
	 * From my tests I see multithreading gains are up to 20% but even of this
	 * improvement a regex search in a ~400 MB log files needs about 5 seconds.
	 ** * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	private enum Impl implements Factory {

		SYNC {
			@Override
			public String description() {
				return "Mono thread, low memory use; slowest but best if no multiple processors.";
			}

			@Override
			public SearchPredicate from(@NotNull Predicate<String> predicate) {
				return new SyncSimpleSearchPredicate(predicate);
			}

			@Override
			public SearchPredicate from(@NotNull Supplier<Predicate<String>> predicateSupplier) {
				return new SyncSimpleSearchPredicate(predicateSupplier.get());
			}
		},


		STREAM {
			@Override
			public String description() {
				return "Parallel streaming, faster than mono, more memory use; should be a compromise.";
			}

			@Override
			public SearchPredicate from(@NotNull Predicate<String> predicate) {
				return new StreamSearchPredicate(predicate);
			}

			@Override
			public SearchPredicate from(@NotNull Supplier<Predicate<String>> predicateSupplier) {
				return new StreamSearchPredicate(predicateSupplier);
			}
		},


		EXECUTORS {
			@Override
			public String description() {
				return "Executor thread-pool, fastest, highest memory use.";
			}

			@Override
			public SearchPredicate from(@NotNull Predicate<String> predicate) {
				return new MultiThreadSearchPredicate(predicate);
			}

			@Override
			public SearchPredicate from(@NotNull Supplier<Predicate<String>> predicateSupplier) {
				return new MultiThreadSearchPredicate(predicateSupplier);
			}
		},
	}

	public static Factory FACTORY;

	static {

		String forcedValue = System.getProperty("jloga.search.multithreading");
		if (forcedValue == null || (forcedValue = forcedValue.trim()).isEmpty())
			FACTORY = Impl.valueOf(Preferences.load(_PREF_MODEL, () -> (Runtime.getRuntime().availableProcessors() > 1 ? Impl.EXECUTORS : Impl.SYNC).toString()));
		else
			FACTORY = Impl.valueOf(forcedValue.toUpperCase(Locale.ROOT));
		System.out.println("SimpleSearchPredicate factory is " + FACTORY);
	}

	@NotNull
	public static Map<String, String> getModels() {
		LinkedHashMap<String, String> res = new LinkedHashMap<>();
		for (Impl impl : Impl.values())
			res.put(impl.toString(), impl.description());
		return res;
	}

	public static void setModel(String name) {
		FACTORY = Impl.valueOf(name);
		Preferences.save(_PREF_MODEL, name);
	}

	public static String getModel() {
		return FACTORY.toString();
	}

	private SimpleSearchPredicate() {}
}
