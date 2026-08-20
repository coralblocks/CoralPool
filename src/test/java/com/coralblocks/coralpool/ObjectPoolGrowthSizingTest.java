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
import static org.junit.Assert.fail;

import org.junit.Test;

public class ObjectPoolGrowthSizingTest {

	private static final ObjectBuilder<Object> BUILDER = Object::new;

	@Test
	public void calculatesLargeLengthsWithDoublePrecision() {
		assertNewLength(25_165_825, 16_777_217, 1.5f);
	}

	@Test
	public void alwaysGrowsByAtLeastOneElement() {
		assertNewLength(101, 100, 1.0000001f);
	}

	@Test
	public void clampsLengthToConservativeVmLimit() {
		assertNewLength(ArraySizing.MAX_ARRAY_LENGTH, ArraySizing.MAX_ARRAY_LENGTH - 100, 2.0f);
	}

	@Test
	public void rejectsGrowthAtConservativeVmLimit() {
		assertCannotGrow(() -> ArraySizing.calculateNewLength(ArraySizing.MAX_ARRAY_LENGTH, 2.0f));
	}

	@Test
	public void rejectsNonFiniteGrowthFactors() {
		for (float growthFactor : new float[] { Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY }) {
			assertInvalidGrowthFactor(() -> new ArrayObjectPool<Object>(1, 0, BUILDER, growthFactor));
			assertInvalidGrowthFactor(() -> new StackObjectPool<Object>(1, 0, BUILDER, growthFactor));
		}
	}

	private static void assertNewLength(int expected, int currentLength, double growthFactor) {
		assertEquals(expected, ArraySizing.calculateNewLength(currentLength, growthFactor));
	}

	private static void assertCannotGrow(Runnable operation) {
		try {
			operation.run();
			fail("Expected OutOfMemoryError");
		} catch (OutOfMemoryError expected) {
			assertEquals("Cannot grow pool array beyond " + ArraySizing.MAX_ARRAY_LENGTH + " elements", expected.getMessage());
		}
	}

	private static void assertInvalidGrowthFactor(Runnable operation) {
		try {
			operation.run();
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException expected) {
			// Expected.
		}
	}
}
