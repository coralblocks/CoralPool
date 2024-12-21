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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

public class RandomObjectPoolTest {
	
	private static enum Type { MULTI_ARRAY, ARRAY, LINKED };

    /**
     * Implement or provide this method. It must return a fresh object pool each time.
     */
    private ObjectPool<Object> createObjectPool(Type type) {
        if (type == Type.MULTI_ARRAY) {
        	return new MultiArrayObjectPool<Object>(4, 2, Object.class);
        } else if (type == Type.ARRAY) {
        	return new ArrayObjectPool<Object>(4, 2, Object.class);
        } else if (type == Type.LINKED) {
        	return new LinkedObjectPool<Object>(4, 2, Object.class);
        } else {
        	throw new IllegalStateException();
        }
    }

    @Test
    public void testRandomOperationsSmall() {
        testRandomOperations(100, 12345L);
    }

    @Test
    public void testRandomOperationsMedium() {
        testRandomOperations(1000, 98765L);
    }

    @Test
    public void testRandomOperationsLarge() {
        testRandomOperations(10000, System.currentTimeMillis());
    }

    /**
     * Performs random get/release operations on the pool.
     * 
     * @param operationsCount the number of random operations to perform
     * @param seed a seed for the random generator, to ensure reproducible results
     */
    private void testRandomOperations(int operationsCount, long seed) {
    	
    	for(Type type : Type.values()) {
    	
	        ObjectPool<Object> pool = createObjectPool(type);
	        Random rand = new Random(seed);
	
	        List<Object> checkedOut = new ArrayList<>();
	
	        for (int i = 0; i < operationsCount; i++) {
	            boolean doGet;
	            // If we have no objects checked out, we must get one.
	            if (checkedOut.isEmpty()) {
	                doGet = true;
	            } else {
	                // Otherwise randomly decide: 50% chance to get or release
	                doGet = rand.nextBoolean();
	            }
	
	            if (doGet) {
	                Object obj = pool.get();
	                Assert.assertNotNull("Object returned by get() should never be null", obj);
	                checkedOut.add(obj);
	            } else {
	                // Release a random object from the checkedOut list
	                int idx = rand.nextInt(checkedOut.size());
	                Object toRelease = checkedOut.remove(idx);
	                pool.release(toRelease);
	            }
	        }
	
	        // After all operations, release all objects still checked out
	        for (Object obj : checkedOut) {
	            pool.release(obj);
	        }
	        checkedOut.clear();
	
	        // After returning everything, let's do a few sanity checks:
	        // We should be able to get and immediately release objects without issues.
	        for (int i = 0; i < 10; i++) {
	            Object o = pool.get();
	            Assert.assertNotNull("Object returned by get() should never be null", o);
	            pool.release(o);
	        }
    	}
    }

    @Test
    public void testRandomOnlyGetsThenReleasesAll() {
        // This test first only gets a bunch of objects, then releases them all in random order
    	for(Type type : Type.values()) {
	        ObjectPool<Object> pool = createObjectPool(type);
	        int count = 100;
	        List<Object> checkedOut = new ArrayList<>(count);
	
	        for (int i = 0; i < count; i++) {
	            Object o = pool.get();
	            Assert.assertNotNull(o);
	            checkedOut.add(o);
	        }
	
	        // Shuffle and release
	        Random rand = new Random(1234);
	        for (int i = count - 1; i >= 0; i--) {
	            int idx = rand.nextInt(i + 1);
	            Object toRelease = checkedOut.remove(idx);
	            pool.release(toRelease);
	        }
	
	        // Sanity check: now that all are released, we should be able to get again
	        for (int i = 0; i < 10; i++) {
	            Object o = pool.get();
	            Assert.assertNotNull("Object returned by get() should never be null", o);
	            pool.release(o);
	        }
    	}
    }

    @Test
    public void testRandomInterleavedOperations() {
    	
    	for(Type type : Type.values()) {
    		ObjectPool<Object> pool = createObjectPool(type);
	        List<Object> checkedOut = new ArrayList<>();
	        Random rand = new Random(42);
	
	        // Perform a mix of gets and releases
	        for (int i = 0; i < 500; i++) {
	            if (checkedOut.isEmpty() || rand.nextBoolean()) {
	                // get an object
	                Object o = pool.get();
	                Assert.assertNotNull(o);
	                checkedOut.add(o);
	            } else {
	                // release an object
	                Object o = checkedOut.remove(rand.nextInt(checkedOut.size()));
	                pool.release(o);
	            }
	        }
	
	        // Release all remaining objects
	        for (Object o : checkedOut) {
	            pool.release(o);
	        }
    	}
    }

    @Test
    public void testReleaseInChunks() {
    	for(Type type : Type.values()) {
	        ObjectPool<Object> pool = createObjectPool(type);
	        List<Object> checkedOut = new ArrayList<>();
	
	        // Get a bunch of objects
	        for (int i = 0; i < 200; i++) {
	            checkedOut.add(pool.get());
	        }
	
	        // Release them in random sized chunks
	        Random rand = new Random(99);
	        while (!checkedOut.isEmpty()) {
	            int chunkSize = Math.min(checkedOut.size(), rand.nextInt(20) + 1);
	            for (int i = 0; i < chunkSize; i++) {
	                Object o = checkedOut.remove(checkedOut.size() - 1);
	                pool.release(o);
	            }
	        }
	
	        // After releasing all, do a few gets/releases to ensure the pool is still functional
	        for (int i = 0; i < 10; i++) {
	            Object o = pool.get();
	            Assert.assertNotNull(o);
	            pool.release(o);
	        }
    	}
    }
}
