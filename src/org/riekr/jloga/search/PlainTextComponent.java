package org.riekr.jloga.search;

import org.riekr.jloga.ui.MRUTextCombo;

import java.util.function.Consumer;

public class PlainTextComponent extends MRUTextCombo implements SearchComponent {

	private static class PlainTextSearch extends SearchPredicate.Simple {

		private final String _pattern;

		public PlainTextSearch(String pattern) {
			_pattern = pattern;
		}

		@Override
		public boolean accept(int line, String text) {
			return text.contains(_pattern);
		}
	}

	public PlainTextComponent(int level) {
		super("plainTextSearch." + level);
	}

	@Override
	public void onSearch(Consumer<SearchPredicate> consumer) {
		if (consumer == null)
			setListener(null);
		else
			setListener((text) -> consumer.accept(new PlainTextSearch(text)));
	}

	@Override
	public String getLabel() {
		return "Tt";
//		return "\uD83C\uDD43";
	}
}
