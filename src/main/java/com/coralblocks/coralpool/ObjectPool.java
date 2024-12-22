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
 * Defines the contract for an object pool that provides reusable instances via {@link #get()} 
 * and reclaims them using {@link #release(E)}.
 * 
 * <p>This object pool can dynamically expand by creating additional instances through its 
 * {@link Builder}, ensuring sufficient capacity to meet demand.</p>
 * 
 * <p>The object pool must be garbage-free, in other words, it must not release 
 * references to the garbage collector.</p>
 * 
 * <p><b>IMPORTANT:</b> This data structure is explicitly designed for <b>single-threaded systems</b>. 
 * Concurrent usage by multiple threads is not supported and will result in undefined behavior.</p>
 * 
 * @param <E> the type of objects managed by this object pool
 */
public interface ObjectPool<E> {

	/**
	 * Retrieves an instance from this object pool. If no instances are currently available,
	 * a new instance will be created, and the pool will grow in size if necessary to 
	 * accommodate more instances. This method can never return <code>null</code>.
	 * 
	 * @return an instance from the pool
	 */
	public E get();

	/**
	 * Returns an instance to this object pool. If the pool has no available space 
	 * to accommodate the instance, it will expand as needed. The pool can accept 
	 * external instances that were not necessarily created by it. 
	 * Passing <code>null</code> as the instance will result in an exception being thrown.
	 * 
	 * @param instance the instance to return to the pool
	 * @throws IllegalArgumentException if the provided instance is <code>null</code>
	 */
	public void release(E instance);
}
