package org.riekr.jloga.utils;

import static org.riekr.jloga.utils.TextUtils.humanReadableByteCount;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

public class Info {

	public static void writeTo(@NotNull PrintStream out) {
		Runtime rt = Runtime.getRuntime();
		out.println("Virtual machine:");
		out.println(" Java Version = " + Runtime.version());
		out.println(" Java Vendor = " + System.getProperty("java.vendor") + ' ' + Optional.ofNullable(System.getProperty("java.vendor.url")).orElse(""));
		out.println(" Java Home = " + System.getProperty("java.home"));
		out.println();
		out.println("Runtime memory:");
		out.println(" Available Processors = " + rt.availableProcessors());
		out.println(" Heap Memory = " + humanReadableByteCount(rt.totalMemory(), false));
		out.println(" Free Memory = " + humanReadableByteCount(rt.freeMemory(), false));
		out.println(" Max Memory = " + humanReadableByteCount(rt.maxMemory(), false));
		// System.getProperties().forEach((k, v) -> System.out.println(k + " = " + v));
	}

	public static String get() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(384);
		PrintStream out = new PrintStream(baos, true);
		writeTo(out);
		out.flush();
		return baos.toString();
	}

	private Info() {}

}
