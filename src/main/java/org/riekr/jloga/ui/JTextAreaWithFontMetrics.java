package org.riekr.jloga.ui;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.io.Serial;
import java.text.CharacterIterator;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.prefs.Preferences;

public class JTextAreaWithFontMetrics extends javax.swing.JTextArea {
	@Serial private static final long serialVersionUID = 5071286309976476695L;

	static class FontMetricsWrapper extends FontMetrics {
		@Serial private static final long serialVersionUID = 3329608676061154280L;

		static class LineMetricsWrapper extends LineMetrics {

			private final @NotNull LineMetrics _tie;

			public LineMetricsWrapper(@NotNull LineMetrics tie) {
				_tie = tie;
			}

			@Override
			public int getNumChars() {
				return _tie.getNumChars();
			}

			@Override
			public float getAscent() {
				return _tie.getAscent();
			}

			@Override
			public float getDescent() {
				return _tie.getDescent();
			}

			@Override
			public float getLeading() {
				return _tie.getLeading();
			}

			@Override
			public float getHeight() {
				return _tie.getHeight() * Preferences.LINEHEIGHT.get();
			}

			@Override
			public int getBaselineIndex() {
				return _tie.getBaselineIndex();
			}

			@Override
			public float[] getBaselineOffsets() {
				return _tie.getBaselineOffsets();
			}

			@Override
			public float getStrikethroughOffset() {
				return _tie.getStrikethroughOffset();
			}

			@Override
			public float getStrikethroughThickness() {
				return _tie.getStrikethroughThickness();
			}

			@Override
			public float getUnderlineOffset() {
				return _tie.getUnderlineOffset();
			}

			@Override
			public float getUnderlineThickness() {
				return _tie.getUnderlineThickness();
			}
		}

		static class Rectangle2DWrapper extends Rectangle2D {

			private final @NotNull Rectangle2D _tie;

			public Rectangle2DWrapper(@NotNull Rectangle2D tie) {
				_tie = tie;
			}

			@Override
			public void setRect(double x, double y, double w, double h) {
				_tie.setRect(x, y, w, h);
			}

			@Override
			public int outcode(double x, double y) {
				return _tie.outcode(x, y);
			}

			@Override
			public Rectangle2D createIntersection(Rectangle2D r) {
				return _tie.createIntersection(r);
			}

			@Override
			public Rectangle2D createUnion(Rectangle2D r) {
				return _tie.createUnion(r);
			}

			@Override
			public double getX() {
				return _tie.getX();
			}

			@Override
			public double getY() {
				return _tie.getY();
			}

			@Override
			public double getWidth() {
				return _tie.getWidth();
			}

			@Override
			public double getHeight() {
				return _tie.getHeight() * (double)Preferences.LINEHEIGHT.get();
			}

			@Override
			public boolean isEmpty() {
				return _tie.isEmpty();
			}
		}

		private final @NotNull FontMetrics _tie;

		public FontMetricsWrapper(@NotNull FontMetrics tie) {
			super(tie.getFont());
			_tie = tie;
		}

		@Override
		public int getHeight() {
			return Math.round(_tie.getHeight() * Preferences.LINEHEIGHT.get());
		}

		@Override
		public Font getFont() {
			return _tie.getFont();
		}

		@Override
		public FontRenderContext getFontRenderContext() {
			return _tie.getFontRenderContext();
		}

		@Override
		public int getLeading() {
			return _tie.getLeading();
		}

		@Override
		public int getAscent() {
			return _tie.getAscent();
		}

		@Override
		public int getDescent() {
			return _tie.getDescent();
		}

		@Override
		public int getMaxAscent() {
			return _tie.getMaxAscent();
		}

		@Override
		public int getMaxDescent() {
			return _tie.getMaxDescent();
		}

		@Override
		public int getMaxAdvance() {
			return _tie.getMaxAdvance();
		}

		@Override
		public int charWidth(int codePoint) {
			return _tie.charWidth(codePoint);
		}

		@Override
		public int charWidth(char ch) {
			return _tie.charWidth(ch);
		}

		@Override
		public int stringWidth(@NotNull String str) {
			return _tie.stringWidth(str);
		}

		@Override
		public int charsWidth(char[] data, int off, int len) {
			return _tie.charsWidth(data, off, len);
		}

		@Override
		public int bytesWidth(byte[] data, int off, int len) {
			return _tie.bytesWidth(data, off, len);
		}

		@Override
		public int[] getWidths() {
			return _tie.getWidths();
		}

		@Override
		public boolean hasUniformLineMetrics() {
			return _tie.hasUniformLineMetrics();
		}

		@Override
		public LineMetrics getLineMetrics(String str, Graphics context) {
			return new LineMetricsWrapper(_tie.getLineMetrics(str, context));
		}

		@Override
		public LineMetrics getLineMetrics(String str, int beginIndex, int limit, Graphics context) {
			return new LineMetricsWrapper(_tie.getLineMetrics(str, beginIndex, limit, context));
		}

		@Override
		public LineMetrics getLineMetrics(char[] chars, int beginIndex, int limit, Graphics context) {
			return new LineMetricsWrapper(_tie.getLineMetrics(chars, beginIndex, limit, context));
		}

		@Override
		public LineMetrics getLineMetrics(CharacterIterator ci, int beginIndex, int limit, Graphics context) {
			return new LineMetricsWrapper(_tie.getLineMetrics(ci, beginIndex, limit, context));
		}

		@Override
		public Rectangle2D getStringBounds(String str, Graphics context) {
			return new Rectangle2DWrapper(_tie.getStringBounds(str, context));
		}

		@Override
		public Rectangle2D getStringBounds(String str, int beginIndex, int limit, Graphics context) {
			return new Rectangle2DWrapper(_tie.getStringBounds(str, beginIndex, limit, context));
		}

		@Override
		public Rectangle2D getStringBounds(char[] chars, int beginIndex, int limit, Graphics context) {
			return new Rectangle2DWrapper(_tie.getStringBounds(chars, beginIndex, limit, context));
		}

		@Override
		public Rectangle2D getStringBounds(CharacterIterator ci, int beginIndex, int limit, Graphics context) {
			return new Rectangle2DWrapper(_tie.getStringBounds(ci, beginIndex, limit, context));
		}

		@Override
		public Rectangle2D getMaxCharBounds(Graphics context) {
			return new Rectangle2DWrapper(_tie.getMaxCharBounds(context));
		}
	}

	public JTextAreaWithFontMetrics() {
		Preferences.LINEHEIGHT.subscribe(this, (val) -> this.repaint());
	}

	@Override
	public final FontMetrics getFontMetrics(Font font) {
		return new FontMetricsWrapper(super.getFontMetrics(font));
	}
}
