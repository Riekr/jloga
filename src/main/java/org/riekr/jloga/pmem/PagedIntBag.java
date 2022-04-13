package org.riekr.jloga.pmem;

public class PagedIntBag extends PagedList<int[]> {

	private static final int _MAX_PAGE_SIZE = 2048 * 1024; // 2MB

	private static int calcLimit(int depth) {
		int max = _MAX_PAGE_SIZE / Integer.BYTES;
		if (depth <= 0 || depth > max)
			throw new IllegalArgumentException("Depth must be between 1 and " + max + " inclusive");
		return _MAX_PAGE_SIZE / (Integer.BYTES * depth);
	}

	public PagedIntBag(int depth) {
		super(calcLimit(depth),
				(arr, dos) -> {
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
