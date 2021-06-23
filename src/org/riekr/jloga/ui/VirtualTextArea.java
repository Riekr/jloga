package org.riekr.jloga.ui;

import org.riekr.jloga.io.TextSource;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER;

public class VirtualTextArea extends JComponent {

	private int _lineHeight;

	private int _fromLine = 0;
	private int _lineCount = 0;
	private int _allLinesCount = 0;

	private final JTextArea _text;
	private final JTextArea _lineNumbers;
	private final JScrollBar _scrollBar;

	private TextSource _textSource;
	private Runnable _lineListenerUnsubscribe;
	private IntConsumer _lineListener;

	public VirtualTextArea() {
		_text = new JTextArea();
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
		JScrollPane scrollPane = new JScrollPane(_text, VERTICAL_SCROLLBAR_NEVER, HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		scrollPane.setWheelScrollingEnabled(false);
		add(scrollPane, BorderLayout.CENTER);
		_lineNumbers = new JTextArea();
		_lineNumbers.setEditable(false);
		_lineNumbers.setEnabled(false);
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
		_text.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				recalcLineCount();
			}
		});
		_text.addMouseWheelListener((e) -> {
			if (e.isShiftDown()) {
				JScrollBar scrollBar = scrollPane.getHorizontalScrollBar();
				int incr = (scrollBar.getMaximum() - scrollBar.getMinimum()) / 10;
				scrollBar.setValue(scrollBar.getValue() + (e.getWheelRotation() * incr));
			} else {
				if (e.getWheelRotation() < 0)
					pageUp();
				else
					pageDn();
			}
		});
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
		setFromLine(_fromLine - _lineCount);
	}

	public void pageDn() {
		setFromLine(_fromLine + _lineCount);
	}

	public void toBeginning() {
		setFromLine(0);
	}

	public void toEnding() {
		setFromLine(_allLinesCount - _lineCount);
	}

	public void centerOn(int line) {
		setFromLine(line - (_lineCount / 2));
		EventQueue.invokeLater(() -> {
			int _highlightedLine = line - _fromLine;
			try {
				Highlighter highlighter = _text.getHighlighter();
				highlighter.removeAllHighlights();
				highlighter.addHighlight(
						_text.getLineStartOffset(_highlightedLine),
						_text.getLineEndOffset(_highlightedLine),
						new DefaultHighlightPainter(_text.getForeground().darker())
				);
				if (_lineListener != null)
					_lineListener.accept(line);
			} catch (BadLocationException e) {
				e.printStackTrace(System.err);
			}
		});
	}

	@Override
	public void setFont(Font f) {
		_text.setFont(f);
		if (_text.getDocument() != null)
			recalcLineHeight();
		_lineNumbers.setFont(f);
	}

	public void setTextSource(TextSource textSource) {
		_textSource = textSource;
		_textSource.requestLineCount(this::setFileLineCount);
		requireText();
	}

	private void setFileLineCount(int lineCount) {
		_allLinesCount = lineCount;
		recalcScrollBarMaximum();
		_scrollBar.setEnabled(true);
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
			int from = _fromLine;
			int to = _fromLine + _lineCount;
			_textSource.requestText(_fromLine, _lineCount, (text) -> {
				_text.setText(text);
				reNumerate(from, to);
			});
		}
	}

	private void reNumerate(int from, int to) {
		if (to >= _allLinesCount)
			to = _allLinesCount - 1;
		int width = Integer.toString(_allLinesCount).length();
		String fmt = "%0" + width + "d ";
		StringBuilder buf = new StringBuilder(String.format(fmt, from++));
		for (int i = from; i <= to; i++)
			buf.append('\n').append(String.format(fmt, i));
		_lineNumbers.setText(buf.toString());
	}

	public TextSource getTextSource() {
		return _textSource;
	}

	public void setLineListener(IntConsumer listener) {
		if (_lineListenerUnsubscribe != null) {
			_lineListenerUnsubscribe.run();
			_lineListenerUnsubscribe = null;
			_lineListener = null;
		}
		if (listener != null) {
			_lineListener = listener;
			AtomicInteger caretLine = new AtomicInteger();
			CaretListener caretListener = e -> {
				try {
					int line = _text.getLineOfOffset(e.getDot());
					caretLine.set(line);
				} catch (BadLocationException badLocationException) {
					badLocationException.printStackTrace(System.err);
				}
			};
			MouseListener mouseListener = new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					listener.accept(_fromLine + caretLine.intValue());
				}
			};
			MouseListener mouseListener2 = new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					int line = e.getY() / _lineHeight;
					caretLine.set(line);
					mouseListener.mouseClicked(e);
				}
			};
			_text.addCaretListener(caretListener);
			_text.addMouseListener(mouseListener);
			_lineNumbers.addMouseListener(mouseListener2);
			_lineListenerUnsubscribe = () -> {
				_text.removeCaretListener(caretListener);
				_text.removeMouseListener(mouseListener);
				_lineNumbers.removeMouseListener(mouseListener2);
			};
		}
	}
}
