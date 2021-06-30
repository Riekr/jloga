package org.riekr.jloga.react;

import java.util.Objects;

@FunctionalInterface
public interface BoolConsumer {

	void accept(boolean value);

	default BoolConsumer andThen(BoolConsumer after) {
		Objects.requireNonNull(after);
		return (boolean t) -> {
			accept(t);
			after.accept(t);
		};
	}
}
