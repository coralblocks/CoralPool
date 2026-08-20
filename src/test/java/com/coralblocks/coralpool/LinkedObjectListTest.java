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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.Test;

public class LinkedObjectListTest {

    @Test
    public void supportsDequeOperationsClearAndReuse() {
        LinkedObjectList<String> list = new LinkedObjectList<>(1);
        list.addLast("middle");
        list.addFirst("first");
        list.addLast("last");

        assertEquals("first", list.first());
        assertEquals("last", list.last());
        assertEquals("first", list.removeFirst());
        assertEquals(2, list.size());

        list.clear();
        assertTrue(list.isEmpty());
        assertNull(list.first());
        assertNull(list.last());

        // Reusing the list after clear exercises entries returned to its internal pool.
        list.addFirst("reused");
        assertEquals("reused", list.removeFirst());
        assertTrue(list.isEmpty());
    }

    @Test
    public void iteratorTraversesHeadToTail() {
        LinkedObjectList<String> list = new LinkedObjectList<>(3);
        list.addLast("first");
        list.addLast("second");
        list.addLast("third");
        Iterator<String> iterator = list.iterator();

        assertTrue(iterator.hasNext());
        assertEquals("first", iterator.next());
        assertEquals("second", iterator.next());
        assertEquals("third", iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void iteratorRemovesHeadMiddleAndTail() {
        LinkedObjectList<String> list = new LinkedObjectList<>(4);
        list.addLast("head");
        list.addLast("second");
        list.addLast("middle");
        list.addLast("tail");
        Iterator<String> iterator = list.iterator();

        assertEquals("head", iterator.next());
        iterator.remove();
        assertEquals("second", list.first());
        assertEquals(3, list.size());

        assertEquals("second", iterator.next());
        assertEquals("middle", iterator.next());
        iterator.remove();
        assertEquals(2, list.size());

        assertEquals("tail", iterator.next());
        iterator.remove();
        assertEquals(1, list.size());
        assertEquals("second", list.first());
        assertEquals("second", list.last());
        assertFalse(iterator.hasNext());
    }

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
