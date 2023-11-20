package org.riekr.jloga.transform;


import org.junit.Assert;
import org.junit.Test;

import java.time.format.DateTimeParseException;
import java.util.ArrayList;

public class ArrowConversionTest {

	private static class TestArrowConversion extends ArrowConversion {
		private final ArrayList<Throwable> _errors = new ArrayList<>();

		public TestArrowConversion() {
			super(new String[]{""});
		}

		@Override
		protected void onDateTimeParseException(DateTimeParseException e) {
			_errors.add(e);
			super.onDateTimeParseException(e);
		}

		@Override
		protected String fixDate(int col, String dateString) {
			String res = super.fixDate(col, dateString);
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
		// String val = "2022-03-06T00:13:29.231";
		String val = "3/9/2022 1:00:00 PM";
		String res = new TestArrowConversion().fixDate(0, val);
		System.err.println(val + '\n' + res);
		Assert.assertNotNull(res);
		Assert.assertFalse(res.endsWith("[UTC]"));
	}

	@Test
	public void fixTime() {
		// 2022-03-06T00:13:29
		String val = "00:13:29.232";
		String res = new TestArrowConversion().fixDate(0, val);
		System.err.println(val + '\n' + res);
		Assert.assertNotNull(res);
		Assert.assertFalse(res.endsWith("[UTC]"));
	}

	@Test
	public void escapeQuotes() {
		String val = "\"1\",2,\"3\",4,\"5\",ciao";
		TestArrowConversion c = new TestArrowConversion();
		c.escapeQuotes(val);
		System.err.println(val + '\n' + c);
		Assert.assertEquals("\\\"1\\\",2,\\\"3\\\",4,\\\"5\\\",ciao", c.toString());
	}

}