/* 
 * Copyright 2015-2024 (c) CoralBlocks LLC - http://www.coralblocks.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.coralblocks.coralpool;

import com.coralblocks.coralpool.util.Builder;

/**
 * <p>The contract of an object pool that can dispense instances through {@link get()} and reclaim instances through {@link release(E)}.</p>
 * 
 * <p>The object pool can grow internally by creating more instances through its {@link Builder}.</p>
 * 
 * <p>The object pool must be garbage-free, in other words, it must not release references to the garbage collector.</p>
 * 
 * <p><b>NOTE:</b> This data structure is designed on purpose to be used by <b>single-threaded systems</b>, in other words, 
 *   it will break if used concurrently by multiple threads.</p>
 *
 * @param <E> the object being served by this object pool
 */
public interface ObjectPool<E> {

	/**
	 * Dispense an instance from the pool
	 * 
	 * @return an instance from the pool
	 */
	public E get();

	/**
	 * Return an instance to the pool
	 * 
	 * @param e the instance to be returned to the pool
	 */
	public void release(E e);
}
