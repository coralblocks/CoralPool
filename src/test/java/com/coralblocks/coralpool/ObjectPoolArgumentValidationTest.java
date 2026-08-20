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

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ObjectPoolArgumentValidationTest {

    private static final ObjectBuilder<Object> BUILDER = Object::new;

    private interface PoolFactory {
        ObjectPool<Object> create(int initialCapacity, int preloadCount);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> poolFactories() {
        return Arrays.asList(new Object[][] {
            { "ArrayObjectPool", (PoolFactory) (capacity, preload) -> new ArrayObjectPool<>(capacity, preload, BUILDER) },
            { "StackObjectPool", (PoolFactory) (capacity, preload) -> new StackObjectPool<>(capacity, preload, BUILDER) },
            { "LinkedObjectPool", (PoolFactory) (capacity, preload) -> new LinkedObjectPool<>(capacity, preload, BUILDER) },
            { "MultiArrayObjectPool", (PoolFactory) (capacity, preload) -> new MultiArrayObjectPool<>(capacity, preload, BUILDER) },
            { "TieredObjectPool", (PoolFactory) (capacity, preload) -> new TieredObjectPool<>(capacity, preload, BUILDER) }
        });
    }

    private final PoolFactory poolFactory;

    public ObjectPoolArgumentValidationTest(String poolName, PoolFactory poolFactory) {
        this.poolFactory = poolFactory;
    }

    @Test
    public void rejectsZeroInitialCapacity() {
        assertInvalidArguments(0, 0, "initialCapacity (0) must be greater than zero");
    }

    @Test
    public void rejectsNegativeInitialCapacity() {
        assertInvalidArguments(-1, 0, "initialCapacity (-1) must be greater than zero");
    }

    @Test
    public void rejectsNegativePreloadCount() {
        assertInvalidArguments(1, -1, "preloadCount (-1) cannot be negative");
    }

    private void assertInvalidArguments(int initialCapacity, int preloadCount, String expectedMessage) {
        try {
            poolFactory.create(initialCapacity, preloadCount);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals(expectedMessage, e.getMessage());
        }
    }
}
