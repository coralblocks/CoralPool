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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class ObjectBuilderTest {

    private static class PrivateConstructor {
        private PrivateConstructor() {
        }
    }

    private static class ArgumentsOnlyConstructor {
        private ArgumentsOnlyConstructor(String value) {
        }
    }

    private static class ThrowingConstructor {
        private ThrowingConstructor() {
            throw new IllegalStateException("constructor failed");
        }
    }

    @Test
    public void createsInstancesWithPrivateConstructor() {
        ObjectBuilder<PrivateConstructor> builder = ObjectBuilder.createBuilder(PrivateConstructor.class);

        assertNotNull(builder.newInstance());
    }

    @Test
    public void rejectsMissingNoArgumentsConstructorWhenBuilderIsCreated() {
        try {
            ObjectBuilder.createBuilder(ArgumentsOnlyConstructor.class);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof NoSuchMethodException);
        }
    }

    @Test
    public void unwrapsRuntimeExceptionThrownByConstructor() {
        ObjectBuilder<ThrowingConstructor> builder = ObjectBuilder.createBuilder(ThrowingConstructor.class);

        try {
            builder.newInstance();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("constructor failed", e.getMessage());
        }
    }
}
