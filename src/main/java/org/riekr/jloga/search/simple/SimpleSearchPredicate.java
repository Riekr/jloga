package org.riekr.jloga.search.simple;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.prefs.Preferences;
import org.riekr.jloga.search.SearchPredicate;

public class SimpleSearchPredicate {

	/** * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
	 * Should we use multithreading searches?
	 * 1. this only works for simple search, anaysis tasks are still not supported
	 * 2. for very fast search operations multithreading overhead is an overkill
	 * From my tests I see multithreading gains are up to 20% but even of this
	 * improvement a regex search in a ~400 MB log files needs about 5 seconds.
	 ** * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	public enum ThreadModel {

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
				return "Parallel streaming, faster than mono, more memory use.";
			}

			@Override
			public SearchPredicate from(@NotNull Predicate<String> predicate) {
				return new StreamSearchPredicate(predicate);
			}

			@Override
			public SearchPredicate from(@NotNull Supplier<Predicate<String>> predicateSupplier) {
				return new StreamSearchPredicate(predicateSupplier);
			}
		};


		// EXECUTORS {
		// 	@Override
		// 	public String description() {
		// 		return "Executor thread-pool, fastest, highest memory use.";
		// 	}
		//
		// 	@Override
		// 	public SearchPredicate from(@NotNull Predicate<String> predicate) {
		// 		return new MultiThreadSearchPredicate(predicate);
		// 	}
		//
		// 	@Override
		// 	public SearchPredicate from(@NotNull Supplier<Predicate<String>> predicateSupplier) {
		// 		return new MultiThreadSearchPredicate(predicateSupplier);
		// 	}
		// }

		public abstract SearchPredicate from(@NotNull Predicate<String> predicate);

		public abstract SearchPredicate from(@NotNull Supplier<Predicate<String>> predicateSupplier);

		public abstract String description();

	}

	public static ThreadModel FACTORY;

	static {
		String forcedValue = System.getProperty("jloga.search.multithreading");
		if (forcedValue == null || (forcedValue = forcedValue.trim()).isEmpty())
			Preferences.MT_MODEL.subscribe(SimpleSearchPredicate::setModel);
		else
			setModel(ThreadModel.valueOf(forcedValue.toUpperCase(Locale.ROOT)));
	}

	private static void setModel(ThreadModel model) {
		FACTORY = model;
		System.out.println("SimpleSearchPredicate factory is " + FACTORY);
	}

	@NotNull
	public static Map<String, ThreadModel> getThreadModels() {
		LinkedHashMap<String, ThreadModel> res = new LinkedHashMap<>();
		for (ThreadModel threadModel : ThreadModel.values())
			res.put(threadModel + " - " + threadModel.description(), threadModel);
		return res;
	}

	public static String getModel() {
		return FACTORY.toString();
	}

	private SimpleSearchPredicate() {}
}
