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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.coralblocks.coralpool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Modifier;

import org.junit.Test;

public class TieredObjectPoolCapacityTest {

	private static final ObjectBuilder<Object> BUILDER = Object::new;

	@Test
	public void linkedListCapacityFactorIsFinal() throws Exception {
		int modifiers = TieredObjectPool.class.getField("LINKED_LIST_CAPACITY_FACTOR").getModifiers();
		assertTrue(Modifier.isFinal(modifiers));
	}

	@Test
	public void acceptsExplicitZeroLinkedListCapacityWithClass() {
		TieredObjectPool<Object> pool = new TieredObjectPool<Object>(1, 1, Object.class, 0);
		Object pooled = pool.get();
		pool.release(pooled);
		Object external = new Object();
		pool.release(external);

		assertSame(external, pool.get());
		assertSame(pooled, pool.get());
	}

	@Test
	public void acceptsExplicitLinkedListCapacityWithBuilder() {
		TieredObjectPool<Object> pool = new TieredObjectPool<Object>(1, 0, BUILDER, 2);
		assertEquals(1, pool.getArrayLength());
	}

	@Test
	public void rejectsNegativeExplicitLinkedListCapacity() {
		try {
			new TieredObjectPool<Object>(1, 0, BUILDER, -1);
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException expected) {
			assertEquals("linkedListInitialCapacity (-1) cannot be negative", expected.getMessage());
		}
	}
}
