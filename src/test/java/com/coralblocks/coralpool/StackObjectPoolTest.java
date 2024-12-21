package com.coralblocks.coralpool;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;

public class StackObjectPoolTest {
    
    private StackObjectPool<String> pool;
    private static final int INITIAL_CAPACITY = 4;
    private static final int PRELOAD_COUNT = 2;
    private static final float GROWTH_FACTOR = 2.0f;
    
    @Before
    public void setUp() {
        pool = new StackObjectPool<>(INITIAL_CAPACITY, PRELOAD_COUNT, String.class, GROWTH_FACTOR);
    }
    
    @Test
    public void testInitialState() {
        assertEquals(INITIAL_CAPACITY, pool.getArrayLength());
        // Check preloaded elements
        for (int i = 0; i < PRELOAD_COUNT; i++) {
            assertNotNull(pool.getArrayElement(i));
        }
        // Check remaining slots are null
        for (int i = PRELOAD_COUNT; i < INITIAL_CAPACITY; i++) {
            assertNull(pool.getArrayElement(i));
        }
    }
    
    @Test
    public void testGetAndRelease() {
        // First get the preloaded objects
        String obj1 = pool.get();
        String obj2 = pool.get();
        assertNotNull(obj1);
        assertNotNull(obj2);
        
        // Now pool is empty, next get will create new instance
        String obj3 = pool.get();
        assertNotNull(obj3);
        
        // Release objects back
        pool.release(obj3);
        pool.release(obj2);
        pool.release(obj1);
        
        // Verify objects are back in pool in correct order
        assertEquals(obj1, pool.get());
        assertEquals(obj2, pool.get());
        assertEquals(obj3, pool.get());
    }
    
    @Test
    public void testUnderflow() {
        // Empty the pool of preloaded objects
        for (int i = 0; i < PRELOAD_COUNT; i++) {
            pool.get();
        }
        
        // Getting from empty pool should create new instance
        String newObj = pool.get();
        assertNotNull(newObj);
    }
    
    @Test
    public void testGrowth() {
        // Fill the pool beyond capacity
        String[] objects = new String[INITIAL_CAPACITY + 2];
        for (int i = 0; i < objects.length; i++) {
            objects[i] = "Test" + i;
            pool.release(objects[i]);
        }
        
        // Verify pool grew
        int expectedNewLength = (int)(INITIAL_CAPACITY * GROWTH_FACTOR);
        if (expectedNewLength == INITIAL_CAPACITY) expectedNewLength++;
        assertEquals(expectedNewLength, pool.getArrayLength());
        
        // Verify all objects are stored correctly (LIFO order)
        for (int i = 0; i < objects.length; i++) {
            assertEquals(objects[objects.length - 1 - i], pool.get());
        }
    }
    
    @Test
    public void testMultipleGrowth() {
        // Test multiple growth cycles
        int totalObjects = INITIAL_CAPACITY * 3;
        String[] objects = new String[totalObjects];
        
        // Fill pool beyond capacity multiple times
        for (int i = 0; i < totalObjects; i++) {
            objects[i] = "Test" + i;
            pool.release(objects[i]);
        }
        
        // Verify all objects can be retrieved in LIFO order
        for (int i = 0; i < totalObjects; i++) {
            assertEquals(objects[totalObjects - 1 - i], pool.get());
        }
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidPreloadCount() {
        new StackObjectPool<>(2, 4, String.class); // preloadCount > initialCapacity
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidGrowthFactor() {
        new StackObjectPool<>(4, 2, String.class, 0f); // growthFactor <= 0
    }
    
    @Test
    public void testCustomPreloadCount() {
        int initialCapacity = 8;
        int preloadCount = 4;
        StackObjectPool<String> customPool = new StackObjectPool<>(initialCapacity, preloadCount, String.class);
        
        // Verify preloaded elements
        for (int i = 0; i < preloadCount; i++) {
            assertNotNull(customPool.getArrayElement(i));
        }
        
        // Verify remaining slots are null
        for (int i = preloadCount; i < initialCapacity; i++) {
            assertNull(customPool.getArrayElement(i));
        }
        
        // Get all preloaded objects
        for (int i = 0; i < preloadCount; i++) {
            assertNotNull(customPool.get());
        }
        
        // Next get should create new instance
        assertNotNull(customPool.get());
    }
    
    @Test
    public void testReleaseSoftReferences() {
        // First get all preloaded objects
        for (int i = 0; i < PRELOAD_COUNT; i++) {
            pool.get();
        }
        
        // Fill and grow the pool multiple times
        for (int i = 0; i < INITIAL_CAPACITY * 5; i++) {
            pool.release("Test" + i);
        }
        
        int releaseCount = pool.releaseSoftReferences();
        System.out.println(releaseCount);
        assertEquals(3, releaseCount);
        
        // Verify pool still functions correctly
        String testObj = "TestObject";
        pool.release(testObj);
        assertEquals(testObj, pool.get());
    }

    @Test
    public void testGrowthFactorJustAboveOne() {
        float growthFactor = 1.001f;
        StackObjectPool<String> smallGrowthPool = new StackObjectPool<>(INITIAL_CAPACITY, PRELOAD_COUNT, String.class, growthFactor);
        
        // First get preloaded objects
        for (int i = 0; i < PRELOAD_COUNT; i++) {
            smallGrowthPool.get();
        }
        
        // Fill beyond initial capacity
        for (int i = 0; i < INITIAL_CAPACITY + 1; i++) {
            smallGrowthPool.release("Test" + i);
        }
        
        // Should grow by at least 1
        assertTrue(smallGrowthPool.getArrayLength() > INITIAL_CAPACITY);
        assertEquals(INITIAL_CAPACITY + 1, smallGrowthPool.getArrayLength());
    }
    
    @Test
    public void testVeryLargeGrowthFactor() {
        float growthFactor = 1000f;
        StackObjectPool<String> largeGrowthPool = new StackObjectPool<>(INITIAL_CAPACITY, PRELOAD_COUNT, String.class, growthFactor);
        
        // First get preloaded objects
        for (int i = 0; i < PRELOAD_COUNT; i++) {
            largeGrowthPool.get();
        }
        
        // Fill beyond initial capacity
        for (int i = 0; i < INITIAL_CAPACITY + 1; i++) {
            largeGrowthPool.release("Test" + i);
        }
        
        // Verify large growth
        assertEquals(INITIAL_CAPACITY * 1000, largeGrowthPool.getArrayLength());
    }
    
    @Test
    public void testFractionalGrowthFactor() {
        float growthFactor = 1.5f;
        StackObjectPool<String> fractionalGrowthPool = new StackObjectPool<>(4, 2, String.class, growthFactor);
        
        // First get preloaded objects
        for (int i = 0; i < 2; i++) {
            fractionalGrowthPool.get();
        }
        
        // Fill beyond initial capacity
        for (int i = 0; i < 5; i++) {
            fractionalGrowthPool.release("Test" + i);
        }
        
        // Verify growth rounds down (4 * 1.5 = 6)
        assertEquals(6, fractionalGrowthPool.getArrayLength());
    }
    
    @Test
    public void testRepeatedGrowthWithSmallFactor() {
        float growthFactor = 1.1f;
        StackObjectPool<String> smallGrowthPool = new StackObjectPool<>(4, 2, String.class, growthFactor);
        
        // First get preloaded objects
        for (int i = 0; i < 2; i++) {
            smallGrowthPool.get();
        }
        
        // Force multiple small growths
        String[] objects = new String[20];
        for (int i = 0; i < 20; i++) {
            objects[i] = "Test" + i;
            smallGrowthPool.release(objects[i]);
        }
        
        // Get all objects back and verify order
        for (int i = 0; i < 20; i++) {
            assertEquals(objects[19 - i], smallGrowthPool.get());
        }
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNegativeGrowthFactor() {
        new StackObjectPool<>(4, 2, String.class, -1.5f);
    }
    
    @Test
    public void testGrowthWithMinimalIncrease() {
        float growthFactor = 1.0001f;
        StackObjectPool<String> minimalGrowthPool = new StackObjectPool<>(100, 50, String.class, growthFactor);
        
        // First get preloaded objects
        for (int i = 0; i < 50; i++) {
            minimalGrowthPool.get();
        }
        
        // Fill beyond capacity
        for (int i = 0; i < 101; i++) {
            minimalGrowthPool.release("Test" + i);
        }
        
        // Should grow by at least 1
        assertEquals(101, minimalGrowthPool.getArrayLength());
    }
}