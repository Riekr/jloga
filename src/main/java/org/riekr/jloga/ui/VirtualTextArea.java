package org.riekr.jloga.ui;

import static java.lang.Integer.min;
import static java.lang.Math.max;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.io.Preferences;
import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.misc.FileDropListener;
import org.riekr.jloga.react.BehaviourSubject;
import org.riekr.jloga.react.Unsubscribable;

public class VirtualTextArea extends JComponent implements FileDropListener {

	private int _lineHeight;

	private       int                       _fromLine        = 0;
	private       int                       _lineCount       = 0;
	private       int                       _allLinesCount   = 0;
	private final BehaviourSubject<Integer> _highlightedLine = new BehaviourSubject<>(null);

	private final JScrollPane         _scrollPane;
	private final JTextArea           _text;
	private final LineNumbersTextArea _lineNumbers;
	private final JScrollBar          _scrollBar;

	private TextSource     _textSource;
	private Unsubscribable _textSourceUnsubscribable;

	private @Nullable IntConsumer _lineListener;
	private @Nullable Runnable    _lineListenerUnsubscribe;

	public VirtualTextArea(@Nullable TabNavigation tabNavigation) {
		_text = ContextMenu.addActionCopy(new JTextArea());
		_text.addKeyListener(new ROKeyListener() {
			@Override
			protected void onPageUp() {
				pageUp();
			}

			@Override
			protected void onPageDn() {
				pageDn();
			}

			@Override
			protected void onDocumentStart() {
				toBeginning();
			}

			@Override
			protected void onDocumentEnd() {
				toEnding();
			}

			@Override
			protected void onPreviousTab() {
				if (tabNavigation != null)
					tabNavigation.goToPreviousTab();
			}

			@Override
			protected void onNextTab() {
				if (tabNavigation != null)
					tabNavigation.goToNextTab();
			}
		});
		_text.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (_lineListener == null || e.getModifiersEx() != 0)
					return;
				Integer line = null;
				switch (e.getKeyCode()) {
					case 38: // up
						if ((line = _highlightedLine.get()) != null)
							line = max(0, line - 1);
						break;
					case 40: // down
						if ((line = _highlightedLine.get()) != null)
							line = min(_allLinesCount - 1, line + 1);
						break;
				}
				if (line != null) {
					setHighlightedLine(line);
					_lineListener.accept(line);
				}
			}
		});
		_text.setAutoscrolls(false);
		_text.setLineWrap(false);
		setLayout(new BorderLayout());
		_scrollPane = new JScrollPane(_text, VERTICAL_SCROLLBAR_NEVER, HORIZONTAL_SCROLLBAR_AS_NEEDED);
		_scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		_scrollPane.setWheelScrollingEnabled(false);
		BoundedRangeModel vscrollModel = _scrollPane.getVerticalScrollBar().getModel();
		vscrollModel.addChangeListener(e -> vscrollModel.setValue(0));
		add(_scrollPane, BorderLayout.CENTER);
		_lineNumbers = new LineNumbersTextArea();
		add(_lineNumbers, BorderLayout.LINE_START);
		_scrollBar = new JScrollBar(JScrollBar.VERTICAL);
		_scrollBar.setMinimum(0);
		_scrollBar.setEnabled(false);
		_scrollBar.addAdjustmentListener(new HalfeningAdjustmentListener(this::setFromLineNoScroll));
		add(_scrollBar, BorderLayout.EAST);

		recalcLineHeight();
		_scrollPane.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				recalcLineCount();
			}
		});
		_text.addMouseWheelListener((e) -> {
			if (e.isShiftDown()) {
				JScrollBar scrollBar = _scrollPane.getHorizontalScrollBar();
				int incr = (scrollBar.getMaximum() - scrollBar.getMinimum()) / 10;
				scrollBar.setValue(scrollBar.getValue() + (e.getWheelRotation() * incr));
			} else {
				if (e.getWheelRotation() < 0)
					pageUp();
				else
					pageDn();
			}
		});
		_highlightedLine.subscribe(this::highlightLine);
	}

	public void setFromLine(int fromLine) {
		if (setFromLineNoScroll(fromLine))
			_scrollBar.setValue(_fromLine);
	}

	private boolean setFromLineNoScroll(int fromLine) {
		if (fromLine < 0)
			fromLine = 0;
		else if (fromLine >= _allLinesCount - _lineCount)
			fromLine = max(0, _allLinesCount - _lineCount - 1);
		if (fromLine != _fromLine) {
			_fromLine = fromLine;
			requireText();
			return true;
		}
		return false;
	}

	public void pageUp() {
		setFromLine(_fromLine - (_lineCount / Preferences.getPageDivider()));
	}

	public void pageDn() {
		setFromLine(_fromLine + (_lineCount / Preferences.getPageDivider()));
	}

	public void toBeginning() {
		setHighlightedLine(0);
	}

	public void toEnding() {
		if (_textSource.isIndexing())
			setHighlightedLine(_allLinesCount);
		else {
			int limit = _allLinesCount - 100;
			TextSource.EXECUTOR.submit(() -> {
				for (int line = _allLinesCount; line > limit; line--) {
					try {
						String text = _textSource.getText(line);
						if (text != null && !text.isEmpty()) {
							setHighlightedLine(line);
							return;
						}
					} catch (ExecutionException | InterruptedException e) {
						e.printStackTrace(System.err);
						break;
					}
				}
				setHighlightedLine(_allLinesCount);
			});
		}
	}

	public void centerOn(int line) {
		_highlightedLine.next(line);
		setFromLine(line - (_lineCount / 2));
	}

	@Override
	public void setFont(Font f) {
		_text.setFont(f);
		if (_text.getDocument() != null)
			recalcLineHeight();
		_lineNumbers.setFont(f);
	}

	public void setTextSource(TextSource textSource) {
		if (_textSourceUnsubscribable != null)
			_textSourceUnsubscribable.unsubscribe();
		_textSource = textSource;
		if (textSource != null) {
			_textSourceUnsubscribable = textSource.requestLineCount(this::setFileLineCount);
			requireText();
		}
	}

	private void setFileLineCount(int lineCount) {
		if (_allLinesCount != lineCount) {
			// +1 to ensure last line is shown even if window borders fall across it
			_allLinesCount = lineCount + 1;
			recalcScrollBarMaximum();
			_scrollBar.setEnabled(true);
			requireText();
		}
	}

	private void recalcScrollBarMaximum() {
		_scrollBar.setMaximum(_allLinesCount - _lineCount);
	}

	private void recalcLineHeight() {
		final int newValue = _text.getFontMetrics(_text.getFont()).getHeight();
		if (newValue != _lineHeight) {
			_lineHeight = newValue;
			recalcLineCount();
		}
	}

	private void recalcLineCount() {
		final int newValue = max(1, (int)Math.floor(getHeight() / (double)_lineHeight) - 1);
		if (newValue != _lineCount) {
			_lineCount = newValue;
			_scrollBar.setBlockIncrement(newValue);
			recalcScrollBarMaximum();
			requireText();
		}
	}

	private void requireText() {
		if (_textSource == null)
			_text.setText("");
		else {
			_textSource.requestText(_fromLine, _lineCount, (reader) -> {
				try {
					// _text.setText(text); simply does not work every time
					_text.read(reader, _fromLine);
				} catch (IOException e) {
					e.printStackTrace(System.err);
				}
				reNumerate();
				EventQueue.invokeLater(() -> {
					_scrollPane.getHorizontalScrollBar().setValue(0);
					highlightLine(_highlightedLine.get());
				});
			});
		}
	}

	private void highlightLine(Integer highlightedLine) {
		if (highlightedLine != null) {
			int line = highlightedLine - _fromLine;
			try {
				Highlighter highlighter = _text.getHighlighter();
				highlighter.removeAllHighlights();
				if (line >= 0 && line <= _lineCount) {
					int start = _text.getLineStartOffset(line);
					highlighter.addHighlight(
							start,
							_text.getLineEndOffset(line),
							new DefaultHighlighter.DefaultHighlightPainter(_text.getSelectionColor())
					);
					_text.setCaretPosition(start);
				}
			} catch (BadLocationException e) {
				e.printStackTrace(System.err);
			}
		}
	}

	private void reNumerate() {
		_lineNumbers.reNumerate(
				_fromLine,
				_fromLine + _lineCount,
				_allLinesCount
		);
	}

	public TextSource getTextSource() {
		return _textSource;
	}

	public void setLineClickListener(IntConsumer listener) {
		if (_lineListenerUnsubscribe != null) {
			_lineListenerUnsubscribe.run();
			_lineListenerUnsubscribe = null;
		}
		_lineListener = listener;
		if (listener != null) {
			MouseListener mouseListener = new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getButton() == MouseEvent.BUTTON1)
						listener.accept(_fromLine + (e.getY() / _lineHeight));
					else
						_highlightedLine.next(null);
				}
			};
			_text.addMouseListener(mouseListener);
			_lineNumbers.addMouseListener(mouseListener);
			_lineListenerUnsubscribe = () -> {
				_text.removeMouseListener(mouseListener);
				_lineNumbers.removeMouseListener(mouseListener);
			};
		}
	}

	public void setHighlightedLine(Integer line) {
		_highlightedLine.next(line);
		if (line != null) {
			if (line < _fromLine)
				setFromLine(line);
			else if (line > (_fromLine + _lineCount))
				setFromLine(line - _lineCount);
		}
	}

	public void onClose() {
		_textSource.onClose();
		if (_lineListenerUnsubscribe != null)
			_lineListenerUnsubscribe.run();
		if (_textSourceUnsubscribable != null)
			_textSourceUnsubscribable.unsubscribe();
	}

	@Override
	public void setFileDropListener(@NotNull Consumer<List<File>> consumer) {
		UIUtils.setFileDropListener(_text, consumer);
		UIUtils.setFileDropListener(_lineNumbers, consumer);
	}
}
