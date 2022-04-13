package org.riekr.jloga.pmem;

import java.io.DataOutputStream;
import java.io.IOException;

public interface DataEncoder<T> {

	DataEncoder<String> STRING = (str, dos) -> dos.writeUTF(str);

	void accept(T t, DataOutputStream dataOutputStream) throws IOException;
}
