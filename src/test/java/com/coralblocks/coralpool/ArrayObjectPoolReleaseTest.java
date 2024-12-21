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

import static org.junit.Assert.*;
import org.junit.Test;

import com.coralblocks.coralpool.util.Builder;

public class ArrayObjectPoolReleaseTest {

    // A simple builder for testing: it creates incrementing integers starting at 0
    private static class IntegerBuilder implements Builder<Integer> {
        private int counter = 0;
        @Override
        public Integer newInstance() {
            return counter++;
        }
    }

    @Test
    public void testReleaseWithoutGrow() {
        // Initial capacity: 5, preload: 3 (so indices [0,1,2] have preloaded objects)
        ArrayObjectPool<Integer> pool = new ArrayObjectPool<>(5, 3, new IntegerBuilder());

        // Initial state: pointer=0 and we have objects at [0,1,2]
        // Get two objects:
        Integer obj1 = pool.get(); // pointer=1 after get
        Integer obj2 = pool.get(); // pointer=2 after get

        // Release obj2:
        pool.release(obj2); 
        // pointer should now be 1 (since release decrements pointer)
        assertEquals(5, pool.getArrayLength());
        assertEquals(obj2, pool.getArrayElement(1)); 

        // Release obj1:
        pool.release(obj1); 
        // pointer should now be 0
        assertEquals(obj1, pool.getArrayElement(0));
    }

    @Test
    public void testReleaseWithGrow() {
        // Small pool: capacity 2, preload 2, so it's initially full at [0,1]
        ArrayObjectPool<Integer> pool = new ArrayObjectPool<>(2, 2, new IntegerBuilder(), 2.0f);

        // Get both objects to move pointer to 2:
        Integer o1 = pool.get(); // pointer=1
        Integer o2 = pool.get(); // pointer=2 now (out of objects at top)

        // Release both objects back to make it full again
        pool.release(o2); // pointer=1
        pool.release(o1); // pointer=0 (full again at [0,1])

        // Now, the array is full at capacity 2 with pointer=0
        // Releasing a new object should force growth
        Integer o3 = 999;
        pool.release(o3); 
        // This should trigger grow(true).

        // After grow:
        // The old length was 2, new length should be 4
        assertEquals(4, pool.getArrayLength());

        // offset = newLen - oldLen = 4 - 2 = 2
        // old array elements originally at [0,1] are now moved to [2,3]
        // pointer was set to offset (2), then decremented to store o3 at index 1
        assertNull(pool.getArrayElement(0));
        assertNotNull(pool.getArrayElement(2)); // old preloaded object
        assertNotNull(pool.getArrayElement(3)); // old preloaded object
        assertEquals((Integer)999, pool.getArrayElement(1));
    }

    @Test
    public void testReleaseWithGetAfterGrow() {
        // Start small: capacity 2, preload 2
        ArrayObjectPool<Integer> pool = new ArrayObjectPool<>(2, 2, new IntegerBuilder(), 2.0f);

        // Drain both objects:
        Integer a = pool.get(); // pointer=1
        Integer b = pool.get(); // pointer=2 (empty at top)

        // Release both to fill again:
        pool.release(b); // pointer=1
        pool.release(a); // pointer=0 (full again)

        // Now release one more object to force growth
        Integer c = 500;
        pool.release(c); 
        assertEquals(4, pool.getArrayLength()); 

        // After growth:
        // old objects at [0,1] should be at [2,3], pointer set to 2, then decremented to 1 for c
        assertEquals((Integer)500, pool.getArrayElement(1));
        // [0] should be null:
        assertNull(pool.getArrayElement(0));
        // old preloaded objects now at the end:
        assertNotNull(pool.getArrayElement(2));
        assertNotNull(pool.getArrayElement(3));

        // Now get some objects after growth:
        // pointer currently = 1+1=2 (because we placed c at index 1)
        Integer retrievedC = pool.get(); // should return c from index 1, pointer=2
        assertEquals((Integer)500, retrievedC);

        // Next get should return what was at [2]:
        Integer oldObj = pool.get();
        assertNotNull(oldObj);

        // Ensure we can still release objects normally without another grow:
        pool.release(600);
        // pointer was 3 after two gets, now release sets pointer=2
        assertEquals((Integer)600, pool.getArrayElement(2));
    }

    @Test
    public void testMultipleReleaseGrowScenarios() {
        // Start with a slightly bigger pool: capacity 4, preload 4
        ArrayObjectPool<Integer> pool = new ArrayObjectPool<>(4, 4, new IntegerBuilder(), 2.0f);

        // Drain all 4 preloaded objects:
        Integer o1 = pool.get(); // pointer=1
        Integer o2 = pool.get(); // pointer=2
        Integer o3 = pool.get(); // pointer=3
        Integer o4 = pool.get(); // pointer=4 (empty now)

        // Release them all back:
        pool.release(o4); // pointer=3
        pool.release(o3); // pointer=2
        pool.release(o2); // pointer=1
        pool.release(o1); // pointer=0 (full again)

        // Now force a grow on release:
        pool.release(700); 
        // old length=4, new length=8
        assertEquals(8, pool.getArrayLength());

        // offset = 8 - 4 = 4
        // old elements moved to [4..7], pointer=4 after grow, then pointer--=3 for new object 700
        assertEquals((Integer)700, pool.getArrayElement(3));
        for (int i = 0; i < 3; i++) {
            assertNull(pool.getArrayElement(i));
        }

        // Check old objects now reside at [4..7]
        for (int i = 4; i < 8; i++) {
            assertNotNull(pool.getArrayElement(i));
        }

        // Retrieve the newly released object:
        Integer retrieved = pool.get(); // pointer=4 after get
        assertEquals((Integer)700, retrieved);
    }
}
