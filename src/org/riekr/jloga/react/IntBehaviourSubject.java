package org.riekr.jloga.react;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static java.util.Objects.requireNonNull;

public class IntBehaviourSubject extends BehaviourSubject<Integer> {


	public IntBehaviourSubject() {
		super(0);
	}

	public IntBehaviourSubject(int initialValue) {
		super(initialValue);
	}

	@NotNull
	public Unsubscribable subscribe(IntConsumer onNext) {
		return super.subscribe((Consumer<? super Integer>) onNext::accept);
	}

	public void next(int item) {
		super.next(item);
	}

	@Override
	public void next(Integer item) {
		super.next(requireNonNull(item));
	}

	public void increment() {
		next(get() + 1);
	}

	public void decrement() {
		next(get() - 1);
	}
}