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
public class ObjectPoolBuilderValidationTest {

    private interface PoolFactory {
        ObjectPool<Object> create(ObjectBuilder<Object> builder);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> poolFactories() {
        return Arrays.asList(new Object[][] {
            { "ArrayObjectPool", (PoolFactory) builder -> new ArrayObjectPool<>(1, 0, builder) },
            { "StackObjectPool", (PoolFactory) builder -> new StackObjectPool<>(1, 0, builder) },
            { "LinkedObjectPool", (PoolFactory) builder -> new LinkedObjectPool<>(1, 0, builder) },
            { "MultiArrayObjectPool", (PoolFactory) builder -> new MultiArrayObjectPool<>(1, 0, builder) },
            { "TieredObjectPool", (PoolFactory) builder -> new TieredObjectPool<>(1, 0, builder) }
        });
    }

    private final PoolFactory poolFactory;

    public ObjectPoolBuilderValidationTest(String poolName, PoolFactory poolFactory) {
        this.poolFactory = poolFactory;
    }

    @Test
    public void rejectsNullBuilderResults() {
        ObjectPool<Object> pool = poolFactory.create(() -> null);

        assertNullBuilderResultRejected(pool);
        assertNullBuilderResultRejected(pool);
    }

    private void assertNullBuilderResultRejected(ObjectPool<Object> pool) {
        try {
            pool.get();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("ObjectBuilder returned null", e.getMessage());
        }
    }
}
