package org.riekr.jloga.react;

public class BoolBehaviourSubject extends BehaviourSubject<Boolean> {

	public BoolBehaviourSubject() {
		super(Boolean.FALSE);
	}

	@Override
	public void next(Boolean item) {
		assert item != null;
		super.next(item);
	}

	@Override
	public void last(Boolean item) {
		assert item != null;
		super.last(item);
	}

	@Override
	public void updateUI(Boolean item) {
		assert item != null;
		super.updateUI(item);
	}

	public void toggle() {
		next(!get());
	}

}
