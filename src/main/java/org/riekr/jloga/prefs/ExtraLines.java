package org.riekr.jloga.prefs;

public enum ExtraLines {

	NONE {
		@Override
		public int apply(int lineCount) {return lineCount;}
	},
	HALF {
		@Override
		public int apply(int lineCount) {return lineCount >> 1;}
	};

	public abstract int apply(int lineCount);

}
