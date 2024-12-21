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

public class ArrayObjectPoolGetTest {

    // Simple builder that returns incrementing integers
    private static class IntegerBuilder implements Builder<Integer> {
        private int counter = 0;
        @Override
        public Integer newInstance() {
            return counter++;
        }
    }
    
    @SuppressWarnings("unused")
	@Test
    public void testGetTriggersGrowthWhenEmpty() {
        // Start small: capacity=2, preload=2
        ArrayObjectPool<Integer> pool = new ArrayObjectPool<>(2, 2, new IntegerBuilder(), 2.0f);

        // Initially: pointer=0, but we have 2 preloaded objects at [0,1].
        // get two objects:
        Integer o1 = pool.get(); // pointer=1
        Integer o2 = pool.get(); // pointer=2 (now pointer == length, no space left)

        // Next get should trigger growth because pointer == array.length
        // Growth should double the size to 4
        Integer o3 = pool.get();
        assertEquals(4, pool.getArrayLength());
        // pointer should now be 3 after returning o3
        // o3 should be newly created since we exhausted preloaded objects
        assertNotNull(o3);
        // Ensure that no objects remain at indices 0,1 after we've taken them
        // They should be null since we got them out and set them to null
        assertNull(pool.getArrayElement(0));
        assertNull(pool.getArrayElement(1));
    }

    @Test
    public void testMultipleGrowthSteps() {
        // Start with capacity=2 and preload=2 again
        ArrayObjectPool<Integer> pool = new ArrayObjectPool<>(2, 2, new IntegerBuilder(), 2.0f);

        // Drain the 2 preloaded objects
        pool.get(); // pointer=1
        pool.get(); // pointer=2 (empty at top now)

        // Trigger first growth on get()
        pool.get(); 
        assertEquals(4, pool.getArrayLength()); // first growth

        // Drain until pointer=4
        // We had pointer=3 after the previous get, so we need one more get:
        pool.get(); // pointer=4 now, completely empty at top

        // Next get should trigger another growth (since pointer==4 and length=4)
        pool.get(); 
        assertEquals(8, pool.getArrayLength()); // second growth

        // After multiple growths, the pool should still function correctly
        // Keep getting more objects, ensure no exceptions and correct increments
        Integer nextObj = pool.get(); 
        assertNotNull(nextObj);

        // Just ensure pointer and array state are consistent
        // pointer should now be 6 after two gets post growth (detailed reasoning):
        // Before second growth get:
        // - pointer=4
        // After second growth get:
        // - pointer set to array.length (8) and then we return a new object, pointer=5
        // Another get:
        // - pointer=6
        // Verify the increments were stable.
        assertTrue(pool.getArrayLength() >= 8);
    }

    @SuppressWarnings("unused")
	@Test
    public void testGetNewInstancesAfterGrowth() {
        ArrayObjectPool<Integer> pool = new ArrayObjectPool<>(3, 3, new IntegerBuilder(), 2.0f);

        // Initially: capacity=3, preload=3 means indices [0,1,2] have objects 0,1,2
        Integer obj0 = pool.get(); // pointer=1
        Integer obj1 = pool.get(); // pointer=2
        Integer obj2 = pool.get(); // pointer=3 (now at capacity)

        // Next get triggers growth, doubling to 6
        Integer obj3 = pool.get();
        assertEquals(6, pool.getArrayLength());
        // pointer should now be 4

        // obj3 should be newly created by the builder since we used all preloaded ones
        // builder increments from 0, so obj0=0, obj1=1, obj2=2, obj3=3
        assertEquals((Integer)3, obj3);

        // Get more objects until we reach the new capacity limit
        Integer obj4 = pool.get(); // pointer=5
        Integer obj5 = pool.get(); // pointer=6 (now equal to length again)

        // obj5 should be the next builder object: 4 and 5 respectively
        assertEquals((Integer)4, obj4);
        assertEquals((Integer)5, obj5);

        // Another get now triggers a second growth
        Integer obj6 = pool.get();
        assertEquals(12, pool.getArrayLength()); // 2nd growth from 6 to 12
        // obj6 should be the next builder integer: 6
        assertEquals((Integer)6, obj6);
    }

    @SuppressWarnings("unused")
	@Test
    public void testGetAfterSomeReleasesDoesNotAffectGrowthLogic() {
        // capacity=2, preload=2
        ArrayObjectPool<Integer> pool = new ArrayObjectPool<>(2, 2, new IntegerBuilder(), 2.0f);

        Integer a = pool.get(); // pointer=1
        Integer b = pool.get(); // pointer=2
        // all preloaded used, pointer=2 means next get triggers growth

        // release b back
        pool.release(b); // pointer=1
        // now the top has space at index=1

        // get again should not trigger growth now because pointer=1 < array length=2
        Integer c = pool.get(); // pointer=2 again
        assertEquals(b, c); // got the released object back
        assertEquals(2, pool.getArrayLength()); // no growth

        // next get triggers growth:
        Integer d = pool.get();
        assertEquals(4, pool.getArrayLength());
        assertNotNull(d);
    }

    @SuppressWarnings("unused")
	@Test
    public void testNoSideEffectsAfterGrowth() {
        // Start with capacity=2, preload=2
        ArrayObjectPool<Integer> pool = new ArrayObjectPool<>(2, 2, new IntegerBuilder(), 2.0f);

        // Drain it:
        pool.get(); // pointer=1
        pool.get(); // pointer=2

        // Trigger growth:
        Integer newObj = pool.get();
        assertEquals(4, pool.getArrayLength());
        // pointer=3 after returning newObj

        // Just a sanity check: 
        // - Array length doubled
        // - We should be able to retrieve many new objects without issue:
        for (int i = 0; i < 10; i++) {
            // Eventually this will cause another growth or create new instances
            Integer temp = pool.get();
            assertNotNull(temp);
        }
        // If we reach here without error, the growth and subsequent gets worked fine.
    }
}
