package org.riekr.jloga.utils;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.text.CharacterIterator;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.prefs.Preferences;

public class FontMetricsWrapper extends FontMetrics {
	private static final long serialVersionUID = 3329608676061154280L;


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
		return _tie.getLineMetrics(str, context);
	}

	@Override
	public LineMetrics getLineMetrics(String str, int beginIndex, int limit, Graphics context) {
		return _tie.getLineMetrics(str, beginIndex, limit, context);
	}

	@Override
	public LineMetrics getLineMetrics(char[] chars, int beginIndex, int limit, Graphics context) {
		return _tie.getLineMetrics(chars, beginIndex, limit, context);
	}

	@Override
	public LineMetrics getLineMetrics(CharacterIterator ci, int beginIndex, int limit, Graphics context) {
		return _tie.getLineMetrics(ci, beginIndex, limit, context);
	}

	@Override
	public Rectangle2D getStringBounds(String str, Graphics context) {
		return _tie.getStringBounds(str, context);
	}

	@Override
	public Rectangle2D getStringBounds(String str, int beginIndex, int limit, Graphics context) {
		return _tie.getStringBounds(str, beginIndex, limit, context);
	}

	@Override
	public Rectangle2D getStringBounds(char[] chars, int beginIndex, int limit, Graphics context) {
		return _tie.getStringBounds(chars, beginIndex, limit, context);
	}

	@Override
	public Rectangle2D getStringBounds(CharacterIterator ci, int beginIndex, int limit, Graphics context) {
		return _tie.getStringBounds(ci, beginIndex, limit, context);
	}

	@Override
	public Rectangle2D getMaxCharBounds(Graphics context) {
		return _tie.getMaxCharBounds(context);
	}
}
