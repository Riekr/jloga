package org.riekr.jloga.utils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

public class Info {

	public static void writeTo(@NotNull PrintStream out) {
		Runtime rt = Runtime.getRuntime();
		out.println("Java Version = " + Runtime.version());
		out.println("Java Vendor = " + System.getProperty("java.vendor") + ' ' + Optional.ofNullable(System.getProperty("java.vendor.url")).orElse(""));
		out.println("Java Home = " + System.getProperty("java.home"));
		out.println("RT Available Processors = " + rt.availableProcessors());
		out.println("RT Heap Memory = " + rt.totalMemory());
		out.println("RT Free Memory = " + rt.freeMemory());
		out.println("RT Max Memory = " + rt.maxMemory());
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
