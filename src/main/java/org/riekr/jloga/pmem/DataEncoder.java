package org.riekr.jloga.pmem;

import java.io.DataOutputStream;
import java.io.IOException;

public interface DataEncoder<T> {

	DataEncoder<String> STRING = DataOutputStream::writeUTF;

	void accept(DataOutputStream dataOutputStream, T t) throws IOException;
}
