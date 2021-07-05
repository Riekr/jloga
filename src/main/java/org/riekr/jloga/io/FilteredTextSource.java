package org.riekr.jloga.io;

import org.riekr.jloga.react.Unsubscribable;

import java.util.function.IntConsumer;

public interface FilteredTextSource extends TextSource {

	Unsubscribable requestLineCount(IntConsumer consumer);

	void dispatchLineCount();

	void complete();

}
