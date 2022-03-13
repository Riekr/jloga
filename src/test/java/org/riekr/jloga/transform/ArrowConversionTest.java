package org.riekr.jloga.transform;


import org.junit.Assert;
import org.junit.Test;

public class ArrowConversionTest {

	@Test
	public void fixDate() {
		// 2022-03-06T00:13:29
		String val = "2022-03-06T00:13:29.231";
		String res = new ArrowConversion(new String[0]).fixDate(val);
		System.err.println(val + '\n' + res);
		Assert.assertFalse(res.endsWith("[UTC]"));
	}

}