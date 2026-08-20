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

/** Shared, overflow-safe sizing for array-backed pool growth. */
final class ArraySizing {

	/* Leave conservative headroom for VM-specific array headers and alignment. */
	static final int MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

	private ArraySizing() {
	}

	static int calculateNewLength(int currentLength, double growthFactor) {
		if (currentLength >= MAX_ARRAY_LENGTH) {
			throw new OutOfMemoryError("Cannot grow pool array beyond " + MAX_ARRAY_LENGTH + " elements");
		}

		// Double arithmetic avoids losing integer precision above float's 24-bit mantissa.
		double targetLength = growthFactor * currentLength;
		int newLength = (int) Math.min(targetLength, MAX_ARRAY_LENGTH);
		newLength = Math.max(newLength, currentLength + 1);
		return newLength;
	}
}
