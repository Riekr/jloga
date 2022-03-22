package org.riekr.jloga.ui;

import org.riekr.jloga.io.ProgressListener;
import org.riekr.jloga.react.BehaviourSubject;
import org.riekr.jloga.react.IntBehaviourSubject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.stream.Collectors.joining;

public class JobProgressBar extends JProgressBar {
	private static final long serialVersionUID = -6197337243003830409L;

	private static final class Job {
		public final String descr;
		public       long   pos, max;

		public Job(String descr) {this.descr = descr;}

		@Override public String toString() {return descr;}
	}

	private final Set<Job>                 _jobs  = Collections.synchronizedSet(new LinkedHashSet<>());
	private final IntBehaviourSubject      _pos   = new IntBehaviourSubject();
	private final BehaviourSubject<String> _descr = new BehaviourSubject<>("");

	private int _width;

	public JobProgressBar() {
		setVisible(false);
		setMinimum(0);
		setStringPainted(true);
		_pos.subscribe(this::setValue);
		_descr.subscribe(this::setString);
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				_width = getWidth();
				setMaximum(_width);
			}
		});
	}

	public ProgressListener addJob(String description) {
		Job job = new Job(description);
		_jobs.add(job);
		refreshDescr();
		return (pos, size) -> {
			// System.out.println(job.descr + " " + pos + " " + size);
			if (pos >= size) {
				if (_jobs.remove(job))
					refreshDescr();
			} else if (_jobs.contains(job)) {
				job.pos = pos;
				job.max = size;
				refreshValues();
			}
		};
	}

	private void refreshDescr() {
		int sz = _jobs.size();
		String text = "";
		if (sz > 0) {
			String suffix = sz > 1 ? " (" + sz + " jobs)" : "";
			synchronized (_jobs) {
				text = _jobs.stream()
						.map((job) -> job.descr)
						.distinct()
						.collect(joining(" / ", "", suffix));
			}
		}
		EventQueue.invokeLater(() -> setVisible(!_jobs.isEmpty()));
		_descr.next(text);
	}

	private void refreshValues() {
		if (_jobs.isEmpty())
			return;
		long pos = 0, max = 0;
		synchronized (_jobs) {
			for (Job job : _jobs) {
				pos += job.pos;
				max += job.max;
			}
		}
		int intPos = (int)(pos * (double)_width / max);
		if (intPos >= _width) {
			if (_pos.get() != intPos)
				_pos.next(intPos);
		} else {
			if (_pos.get() != intPos)
				_pos.updateUI(intPos);
		}
	}
}
