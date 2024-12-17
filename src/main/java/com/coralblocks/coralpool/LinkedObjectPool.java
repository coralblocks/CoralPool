package com.coralblocks.coralpool;

import com.coralblocks.coralpool.util.Builder;
import com.coralblocks.coralpool.util.LinkedObjectList;

public class LinkedObjectPool<E> implements ObjectPool<E> {
	
	private final LinkedObjectList<E> linkedList;
	private final Builder<E> builder;
	
	public LinkedObjectPool(int preloadCount, Builder<E> builder) {
		this.builder = builder;
		this.linkedList = new LinkedObjectList<E>(preloadCount * 2);
		for(int i = 0; i < preloadCount; i++) {
			linkedList.addLast(builder.newInstance());
		}
	}

	@Override
	public E get() {
		E toReturn = linkedList.removeLast();
		if (toReturn == null) {
			return builder.newInstance();
		}
		return toReturn;
	}

	@Override
	public void release(E e) {
		linkedList.addLast(e);
	}

	@Override
	public Builder<E> getBuilder() {
		return builder;
	}
}