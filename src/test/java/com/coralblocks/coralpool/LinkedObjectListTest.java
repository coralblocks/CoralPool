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

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.Test;

public class LinkedObjectListTest {

    @Test
    public void nextDoesNotRequireHasNext() {
        LinkedObjectList<String> list = new LinkedObjectList<>(1);
        list.addLast("value");

        assertEquals("value", list.iterator().next());
    }

    @Test(expected = NoSuchElementException.class)
    public void nextRejectsEmptyIterator() {
        new LinkedObjectList<>(0).iterator().next();
    }

    @Test(expected = NoSuchElementException.class)
    public void nextRejectsExhaustedIterator() {
        LinkedObjectList<String> list = new LinkedObjectList<>(1);
        list.addLast("value");
        Iterator<String> iterator = list.iterator();
        iterator.next();

        iterator.next();
    }

    @Test(expected = IllegalStateException.class)
    public void removeRejectsCallBeforeNext() {
        LinkedObjectList<String> list = new LinkedObjectList<>(1);
        list.addLast("value");

        list.iterator().remove();
    }

    @Test(expected = IllegalStateException.class)
    public void removeRejectsRepeatedCall() {
        LinkedObjectList<String> list = new LinkedObjectList<>(1);
        list.addLast("value");
        Iterator<String> iterator = list.iterator();
        iterator.next();
        iterator.remove();

        iterator.remove();
    }
}
