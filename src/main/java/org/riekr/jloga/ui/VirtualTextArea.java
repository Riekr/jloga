package org.riekr.jloga.ui;

import org.riekr.jloga.io.Preferences;
import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.react.BehaviourSubject;
import org.riekr.jloga.react.Unsubscribable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.function.IntConsumer;

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER;

public class VirtualTextArea extends JComponent {

	private int _lineHeight;

	private int _fromLine = 0;
	private int _lineCount = 0;
	private int _allLinesCount = 0;
	private final BehaviourSubject<Integer> _highlightedLine = new BehaviourSubject<>(null);

	private final JScrollPane _scrollPane;
	private final JTextArea _text;
	private final LineNumbersTextArea _lineNumbers;
	private final JScrollBar _scrollBar;

	private TextSource _textSource;
	private Unsubscribable _textSourceUnsubscribable;
	private Runnable _lineListenerUnsubscribe;

	public VirtualTextArea() {
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
		_scrollBar.addAdjustmentListener(e -> {
			if (!e.getValueIsAdjusting())
				setFromLine(e.getValue());
		});
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
		if (fromLine < 0)
			fromLine = 0;
		else if (fromLine >= _allLinesCount - _lineCount)
			fromLine = _allLinesCount - _lineCount - 1;
		if (fromLine != _fromLine) {
			_fromLine = fromLine;
			_scrollBar.setValue(_fromLine);
			requireText();
		}
	}

	public void pageUp() {
		setFromLine(_fromLine - (_lineCount / Preferences.getPageDivider()));
	}

	public void pageDn() {
		setFromLine(_fromLine + (_lineCount / Preferences.getPageDivider()));
	}

	public void toBeginning() {
		setFromLine(0);
	}

	public void toEnding() {
		setFromLine(_allLinesCount - _lineCount);
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
		final int newValue = Math.max(1, (int) Math.floor(getHeight() / (double) _lineHeight) - 1);
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
				if (line >= 0 && line < _lineCount) {
					highlighter.addHighlight(
							_text.getLineStartOffset(line),
							_text.getLineEndOffset(line),
							new DefaultHighlightPainter(_text.getForeground().darker())
					);
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
	}

	public void onClose() {
		_textSource.onClose();
		if (_lineListenerUnsubscribe != null)
			_lineListenerUnsubscribe.run();
		if (_textSourceUnsubscribable != null)
			_textSourceUnsubscribable.unsubscribe();
	}
}
