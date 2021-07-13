package org.riekr.jloga.react;

public class IntBehaviourSubject extends BehaviourSubject<Integer> {

	public IntBehaviourSubject() {
		super(0);
	}

	@Override
	public void next(Integer item) {
		assert item != null;
		super.next(item);
	}

	@Override
	public void last(Integer item) {
		assert item != null;
		super.last(item);
	}

	@Override
	public void updateUI(Integer item) {
		assert item != null;
		super.updateUI(item);
	}

}
