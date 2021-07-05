package org.riekr.jloga.react;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public class BoolBehaviourSubject extends BehaviourSubject<Boolean> {

	public BoolBehaviourSubject() {
		super(Boolean.FALSE);
	}

	@NotNull
	public Unsubscribable subscribe(BoolConsumer onNext) {
		return super.subscribe((Consumer<? super Boolean>) onNext::accept);
	}

	@Override
	public void next(Boolean item) {
		super.next(requireNonNull(item));
	}

	@Override
	public void last(Boolean item) {
		super.last(requireNonNull(item));
	}

	@Override
	public void updateUI(Boolean item) {
		super.updateUI(requireNonNull(item));
	}

	public void toggle() {
		next(!get());
	}

}
