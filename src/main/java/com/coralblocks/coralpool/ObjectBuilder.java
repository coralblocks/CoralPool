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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * An interface that knows how to create instances of a class
 * 
 * @param <E> the class of the object that will be built
 */
public interface ObjectBuilder<E> {

	/**
	 * Return a new instance of the class
	 * 
	 * @return a new instance
	 */
	public E newInstance();
	
	/**
	 * Convenient method to create a {@code ObjectBuilder} from a class through its default (no-arguments) constructor.
	 * 
	 * @param <E> the class of the object that will be built
	 * @param klass the class of the object that will be built
	 * @return a new {@code ObjectBuilder} for the given class
	 * @throws RuntimeException if a no-arguments constructor cannot be prepared for invocation
	 */
	public static <E> ObjectBuilder<E> createBuilder(final Class<E> klass) {
		final Constructor<E> constructor;
		try {
			constructor = klass.getDeclaredConstructor();
			if (!constructor.canAccess(null)) constructor.setAccessible(true);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}

		final Object[] noArguments = new Object[0];

		return new ObjectBuilder<E>() {
			@Override
			public E newInstance() {
				try {
					return constructor.newInstance(noArguments);
				} catch(InvocationTargetException e) {
					Throwable cause = e.getCause();
					if (cause instanceof RuntimeException) throw (RuntimeException) cause;
					if (cause instanceof Error) throw (Error) cause;
					throw new RuntimeException(cause);
				} catch(Exception e) {
					throw new RuntimeException(e);
				}
			}
		};
	}
}
