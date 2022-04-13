package org.riekr.jloga.pmem;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

public class ByteArrayDataOutputStream extends DataOutputStream {

	private final ByteArrayOutputStream _buf;

	public ByteArrayDataOutputStream(int size) {
		this(new ByteArrayOutputStream(size));
	}

	private ByteArrayDataOutputStream(ByteArrayOutputStream buf) {
		super(buf);
		_buf = buf;
	}

	public byte[] toByteArray() {
		return _buf.toByteArray();
	}

}
