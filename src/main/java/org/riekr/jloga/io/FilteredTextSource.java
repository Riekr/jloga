package org.riekr.jloga.io;

public interface FilteredTextSource extends TextSource {

	void dispatchLineCount();

	void complete();

}
