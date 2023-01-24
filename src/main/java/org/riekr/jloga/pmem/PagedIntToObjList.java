package org.riekr.jloga.pmem;

import org.jetbrains.annotations.NotNull;

public class PagedIntToObjList<T> extends PagedList<IntToObject<T>> {

	public static PagedIntToObjList<String> newPagedIntToStringList() {
		return new PagedIntToObjList<>(DataEncoder.STRING, DataDecoder.STRING);
	}

	public PagedIntToObjList(@NotNull DataEncoder<T> encoder, @NotNull DataDecoder<T> decoder) {
		super(
				(dos, ito) -> {
					dos.writeInt(ito.tag);
					encoder.accept(dos, ito.value);
				},
				(dis) -> new IntToObject<>(dis.readInt(), decoder.apply(dis))
		);
	}

	public final void add(int tag, T value) {
		super.add(new IntToObject<>(tag, value));
	}

	public final Integer getTag(int idx) {
		IntToObject<T> rec = get(idx);
		return rec == null ? null : rec.tag;
	}

	public final T getValue(int idx) {
		return get(idx).value;
	}
}
