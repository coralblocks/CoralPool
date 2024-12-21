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

import org.junit.Assert;
import org.junit.Test;

import com.coralblocks.coralpool.util.Builder;

public class MultiArrayObjectPoolTest {

    // A simple test builder that creates string objects with incrementing IDs
    static class TestBuilder implements Builder<Object> {
        private int counter = 0;
        @Override
        public Object newInstance() {
            return "Obj-" + (counter++);
        }
    }

    @Test
    public void testBasicGetPreloaded() {
        int initialCapacity = 5;
        int preloadCount = 5;
        MultiArrayObjectPool<Object> pool = new MultiArrayObjectPool<>(initialCapacity, preloadCount, new TestBuilder());

        // All objects should be pre-constructed
        for (int i = 0; i < initialCapacity; i++) {
            Object obj = pool.get();
            Assert.assertNotNull("Preloaded object should not be null", obj);
            Assert.assertTrue("Preloaded object should have correct format", obj.toString().startsWith("Obj-"));
        }

        // Next get should create a new object since no preloaded ones remain
        Object newObj = pool.get();
        Assert.assertNotNull(newObj);
        Assert.assertTrue(newObj.toString().startsWith("Obj-"));
    }

    @Test
    public void testRelease() {
        int initialCapacity = 4;
        int preloadCount = 4;
        ObjectPool<Object> pool = new MultiArrayObjectPool<>(initialCapacity, preloadCount, new TestBuilder());

        Object[] objs = new Object[initialCapacity];
        for (int i = 0; i < initialCapacity; i++) {
            objs[i] = pool.get();
        }

        // Release all objects
        for (Object obj : objs) {
            pool.release(obj);
        }

        // Now get them back and ensure we get the same objects (since they're reused)
        for (int i = 0; i < initialCapacity; i++) {
            Object obj = pool.get();
            Assert.assertTrue("Should get a previously released object", contains(objs, obj));
        }
    }
    
    private boolean contains(Object[] array, Object obj) {
        for (Object o : array) {
            if (o == obj) return true;
        }
        return false;
    }

    @SuppressWarnings("unused")
	@Test
    public void testGrowToRight() {
        int initialCapacity = 2;
        int preloadCount = 2;
        MultiArrayObjectPool<Object> pool = new MultiArrayObjectPool<>(initialCapacity, preloadCount, new TestBuilder());

        // Initially: 2 preloaded objects
        Object o1 = pool.get();
        Object o2 = pool.get();

        // Now we're out of preloaded objects and at the boundary.
        // The next get should trigger growth to the right if we need more space.
        // pointer == (arrays.index + 1)*arrayLength means pointer=2, arrays.index=0, arrayLength=2
        // next get should cause grow(true) since arrays.next is null.
        Object o3 = pool.get();
        Assert.assertNotNull(o3);

        Object o4 = pool.get();
        Assert.assertNotNull("Should be able to get another object from the new array", o4);
    }

    @Test
    public void testGrowToLeft() {
        int initialCapacity = 2;
        int preloadCount = 0; // start with empty arrays for simplicity
        MultiArrayObjectPool<Object> pool = new MultiArrayObjectPool<>(initialCapacity, preloadCount, new TestBuilder());

        // Initially empty, pointer starts at 0.
        // If we release an object now, pointer will decrement and we’ll need space to the left.
        Object testObj = "TestObj";
        pool.release(testObj);

        // Now get the object back
        Object retrieved = pool.get();
        Assert.assertSame("We should get back the same object we just released", testObj, retrieved);
    }

    @Test
    public void testMultipleExpansions() {
        int initialCapacity = 2;
        MultiArrayObjectPool<Object> pool = new MultiArrayObjectPool<>(initialCapacity, new TestBuilder());

        // Force multiple right expansions
        for (int i = 0; i < 10; i++) {
            Object o = pool.get();
            Assert.assertNotNull(o);
        }

        // Now release more objects than fit in a single array, forcing left expansion
        for (int i = 0; i < 10; i++) {
            pool.release("ReleaseObj-" + i);
        }

        // Get them all back
        for (int i = 0; i < 10; i++) {
            Object o = pool.get();
            Assert.assertNotNull(o);
        }
    }

    @Test
    public void testNoPreload() {
        int initialCapacity = 2;
        int preloadCount = 0;
        MultiArrayObjectPool<Object> pool = new MultiArrayObjectPool<>(initialCapacity, preloadCount, new TestBuilder());

        // No objects preloaded, first get should create a new object
        Object obj1 = pool.get();
        Assert.assertNotNull(obj1);

        // Release and get again to ensure reuse
        pool.release(obj1);
        Object obj2 = pool.get();
        Assert.assertSame("Should reuse the same object after release", obj1, obj2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidArgs() {
        // preloadCount > initialCapacity should throw
        new MultiArrayObjectPool<>(5, 6, new TestBuilder());
    }
}
