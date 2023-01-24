package org.riekr.jloga.pmem;

public class PagedIntBag extends PagedList<int[]> {

	public PagedIntBag() {
		super(
				(dos, arr) -> {
					dos.writeInt(arr.length);
					for (int e : arr)
						dos.writeInt(e);
				},
				(dis) -> {
					int[] res = new int[dis.readInt()];
					for (int i = 0; i < res.length; i++)
						res[i] = dis.readInt();
					return res;
				}
		);
	}

	public final void add(int val1, int val2) {
		super.add(new int[]{val1, val2});
	}

}
