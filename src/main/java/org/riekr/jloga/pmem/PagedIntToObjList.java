package org.riekr.jloga.pmem;

import java.io.Serializable;

public class PagedIntToObjList<T extends Serializable> extends PagedList<IntToObject<T>> {
	public PagedIntToObjList(int pageSizeLimit) {
		super(pageSizeLimit);
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
