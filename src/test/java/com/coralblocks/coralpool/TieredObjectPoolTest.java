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

 public class TieredObjectPoolTest { 

 	// Test Object for Pooling 
 	public static class MyObject { 
 		public int id; 
 		 
 		public MyObject() { 
 			 
 		} 
 	} 

 	@Test 
 	public void testConstructorWithClass_PreloadEqualsInitialCapacity() { 
 		TieredObjectPool<MyObject> pool = new TieredObjectPool<>(5, MyObject.class); 
 		assertEquals(5, pool.getArrayLength()); 
 		for (int i = 0; i < 5; i++) { 
 			assertNotNull(pool.getArrayElement(i)); 
 		} 
 		assertEquals(0, pool.getLinkedList().size()); 
 	} 

 	@Test 
 	public void testConstructorWithBuilder_PreloadEqualsInitialCapacity() { 
 		ObjectBuilder<MyObject> builder = ObjectBuilder.createBuilder(MyObject.class); 
 		TieredObjectPool<MyObject> pool = new TieredObjectPool<>(5, builder); 
 		assertEquals(5, pool.getArrayLength()); 
 		for (int i = 0; i < 5; i++) { 
 			assertNotNull(pool.getArrayElement(i)); 
 		} 
 		assertEquals(0, pool.getLinkedList().size()); 
 	} 

 	@Test 
 	public void testConstructorWithClass_PartialPreload() { 
 		TieredObjectPool<MyObject> pool = new TieredObjectPool<>(5, 3, MyObject.class); 
 		assertEquals(5, pool.getArrayLength()); 
 		assertNotNull(pool.getArrayElement(0)); 
 		assertNotNull(pool.getArrayElement(1)); 
 		assertNotNull(pool.getArrayElement(2)); 
 		assertNull(pool.getArrayElement(3)); 
 		assertNull(pool.getArrayElement(4)); 
 		assertEquals(0, pool.getLinkedList().size()); 
 	} 

 	@Test 
 	public void testConstructorWithBuilder_PartialPreload() { 
 		ObjectBuilder<MyObject> builder = ObjectBuilder.createBuilder(MyObject.class); 
 		TieredObjectPool<MyObject> pool = new TieredObjectPool<>(5, 3, builder); 
 		assertEquals(5, pool.getArrayLength()); 
 		assertNotNull(pool.getArrayElement(0)); 
 		assertNotNull(pool.getArrayElement(1)); 
 		assertNotNull(pool.getArrayElement(2)); 
 		assertNull(pool.getArrayElement(3)); 
 		assertNull(pool.getArrayElement(4)); 
 		assertEquals(0, pool.getLinkedList().size()); 
 	} 

 	@Test(expected = IllegalArgumentException.class) 
 	public void testConstructor_PreloadGreaterThanInitialCapacity() { 
 		new TieredObjectPool<>(5, 6, MyObject.class); 
 	} 

 	@Test 
 	public void testGet_WithinInitialCapacity() { 
 		TieredObjectPool<MyObject> pool = new TieredObjectPool<>(5, MyObject.class); 
 		MyObject obj1 = pool.get(); 
 		MyObject obj2 = pool.get(); 
 		assertNotNull(obj1); 
 		assertNotNull(obj2); 
 		assertNotSame(obj1, obj2); 
 		assertEquals(3, pool.getLinkedList().size() + getNotNullArrayElementsCount(pool)); 
 	} 
 	 
 	@Test 
 	public void testGet_ExceedingInitialCapacity() { 
 		TieredObjectPool<MyObject> pool = new TieredObjectPool<>(2, MyObject.class); 
 		MyObject obj1 = pool.get(); 
 		MyObject obj2 = pool.get(); 
 		MyObject obj3 = pool.get(); 
 		MyObject obj4 = pool.get(); 
 		assertNotNull(obj1); 
 		assertNotNull(obj2); 
 		assertNotNull(obj3); 
 		assertNotNull(obj4); 
 		assertNotSame(obj1, obj3); 
 		assertNotSame(obj2, obj4); 
 		assertNotSame(obj3, obj4); 
 		assertEquals(0, getNotNullArrayElementsCount(pool)); 
 		assertEquals(0, pool.getLinkedList().size()); 
 	} 

 	@Test 
 	public void testRelease_WithinInitialCapacity() { 
 		TieredObjectPool<MyObject> pool = new TieredObjectPool<>(5, MyObject.class); 
 		MyObject obj1 = pool.get(); 
 		MyObject obj2 = pool.get(); 
 		pool.release(obj1); 
 		pool.release(obj2); 
 		assertEquals(5, getNotNullArrayElementsCount(pool)); 
 		assertEquals(0, pool.getLinkedList().size()); 
 	} 

 	@Test 
 	public void testRelease_ExceedingInitialCapacity() { 
 		TieredObjectPool<MyObject> pool = new TieredObjectPool<>(2, MyObject.class); 
 		MyObject obj1 = pool.get(); 
 		MyObject obj2 = pool.get(); 
 		MyObject obj3 = new MyObject(); 
 		MyObject obj4 = new MyObject(); 
 		pool.release(obj1); 
 		pool.release(obj2); 
 		pool.release(obj3); 
 		pool.release(obj4); 
 		assertEquals(2, getNotNullArrayElementsCount(pool)); 
 		assertEquals(2, pool.getLinkedList().size()); 
 	} 
 	 
 	@Test 
     public void testReleaseAndGet_Interleaved() { 
         TieredObjectPool<MyObject> pool = new TieredObjectPool<>(2, MyObject.class); 
         MyObject obj1 = pool.get(); 
         MyObject obj2 = pool.get(); 
         pool.release(obj1); 
         MyObject obj3 = pool.get(); 
         assertSame(obj1, obj3); 
         pool.release(obj2); 
         pool.release(obj3); 
         MyObject obj4 = pool.get(); 
         MyObject obj5 = pool.get(); 
         assertSame(obj3, obj4); 
         assertSame(obj2, obj5); 
         assertEquals(0, pool.getLinkedList().size()); 
         MyObject obj6 = pool.get(); 
         assertNotSame(obj1, obj6); 
     } 
 	 
 	@Test 
 	public void testGetAndRelease_InitialNotPreloaded_Interleaved() { 
 		TieredObjectPool<MyObject> pool = new TieredObjectPool<>(2, 0, MyObject.class); 
         MyObject obj1 = pool.get(); 
         MyObject obj2 = pool.get(); 
         pool.release(obj1); 
         MyObject obj3 = pool.get(); 
         assertSame(obj1, obj3); 
         pool.release(obj2); 
         pool.release(obj3); 
         MyObject obj4 = pool.get(); 
         MyObject obj5 = pool.get(); 
         assertSame(obj3, obj4); 
         assertSame(obj2, obj5); 
         assertEquals(0, pool.getLinkedList().size()); 
         MyObject obj6 = pool.get(); 
         assertNotSame(obj1, obj6); 
 	} 

 	// Helper Method to Count Non-Null Elements in Array 
 	private int getNotNullArrayElementsCount(TieredObjectPool<MyObject> pool) { 
 		int count = 0; 
 		for (int i = 0; i < pool.getArrayLength(); i++) { 
 			if (pool.getArrayElement(i) != null) { 
 				count++; 
 			} 
 		} 
 		return count; 
 	} 
 }