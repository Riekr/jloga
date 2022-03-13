package org.riekr.jloga.transform;


import org.junit.Assert;
import org.junit.Test;

import java.time.format.DateTimeParseException;
import java.util.ArrayList;

public class ArrowConversionTest {

	private static class TestArrowConversion extends ArrowConversion {
		private final ArrayList<Throwable> _errors = new ArrayList<>();

		public TestArrowConversion() {
			super(new String[0]);
		}

		@Override
		protected void onDateTimeParseException(DateTimeParseException e) {
			_errors.add(e);
			super.onDateTimeParseException(e);
		}

		@Override
		protected String fixDate(String dateString) {
			String res = super.fixDate(dateString);
			if (res == null) {
				_errors.forEach((e) -> {
					e.printStackTrace(System.err);
					System.err.println();
				});
			}
			return res;
		}
	}

	@Test
	public void fixDate() {
		// 2022-03-06T00:13:29
		String val = "2022-03-06T00:13:29.231";
		String res = new TestArrowConversion().fixDate(val);
		System.err.println(val + '\n' + res);
		Assert.assertNotNull(res);
		Assert.assertFalse(res.endsWith("[UTC]"));
	}

	@Test
	public void fixTime() {
		// 2022-03-06T00:13:29
		String val = "00:13:29.232";
		String res = new TestArrowConversion().fixDate(val);
		System.err.println(val + '\n' + res);
		Assert.assertNotNull(res);
		Assert.assertFalse(res.endsWith("[UTC]"));
	}

}