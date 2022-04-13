package org.riekr.jloga.pmem;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;

public interface DataDecoder<T> {

	DataDecoder<String> STRING = DataInput::readUTF;

	T apply(DataInputStream dataInputStream) throws IOException;
}
