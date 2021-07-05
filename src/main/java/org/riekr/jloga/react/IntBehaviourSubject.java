package org.riekr.jloga.react;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static java.util.Objects.requireNonNull;

public class IntBehaviourSubject extends BehaviourSubject<Integer> {

	public IntBehaviourSubject() {
		super(0);
	}

	@NotNull
	public Unsubscribable subscribe(IntConsumer onNext) {
		return super.subscribe((Consumer<? super Integer>) onNext::accept);
	}

	@Override
	public void next(Integer item) {
		super.next(requireNonNull(item));
	}

	@Override
	public void last(Integer item) {
		super.last(requireNonNull(item));
	}

	@Override
	public void updateUI(Integer item) {
		super.updateUI(requireNonNull(item));
	}

}
