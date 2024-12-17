package com.coralblocks.coralpool;

import static org.junit.Assert.*;

import org.junit.Test;

import com.coralblocks.coralpool.util.Builder;

public class ObjectPoolTest {

    @Test
    public void testLinkedObjectPoolBasicGetRelease() {
    	
        ObjectPool<StringBuilder> pool = new LinkedObjectPool<StringBuilder>(5, Builder.createBuilder(StringBuilder.class));

        // We preloaded 5 instances
        StringBuilder sb1 = pool.get();
        assertNotNull(sb1);
        
        // Release and get again should give the same instance (since it's a pool)
        pool.release(sb1);
        StringBuilder sb2 = pool.get();
        assertSame(sb1, sb2);
    }

    @Test
    public void testLinkedObjectPoolNewInstancesWhenEmpty() {
    	
        ObjectPool<StringBuilder> pool = new LinkedObjectPool<StringBuilder>(2, Builder.createBuilder(StringBuilder.class));
        
        StringBuilder s1 = pool.get();
        StringBuilder s2 = pool.get();
        
        // We had 2 preloaded, we took them all
        StringBuilder s3 = pool.get();
        // s3 should be a new instance (different from s1 and s2)
        assertNotNull(s3);
        assertNotSame(s1, s3);
        assertNotSame(s2, s3);
    }

    @Test
    public void testLinkedObjectPoolReleaseOrder() {
    	
        ObjectPool<StringBuilder> pool = new LinkedObjectPool<StringBuilder>(2, Builder.createBuilder(StringBuilder.class));
        StringBuilder s1 = pool.get();
        StringBuilder s2 = pool.get();

        // The pool is now empty internally
        pool.release(s2);
        pool.release(s1);

        // The pool is LIFO (given LinkedObjectList.addLast/removeLast)
        // So we should get s1 last if it was re-added last, depending on the structure.
        // LinkedObjectList as implemented suggests last in is last out,
        // which means we should get s1 first:
        StringBuilder s3 = pool.get();
        assertSame(s1, s3);
        StringBuilder s4 = pool.get();
        assertSame(s2, s4);
    }

    @Test
    public void testArrayObjectPoolPreload() {
    	
        // Preload 5 objects
        ObjectPool<StringBuilder> pool = new ArrayObjectPool<StringBuilder>(10, 5, Builder.createBuilder(StringBuilder.class));
        
        // First 5 gets should give us the 5 preloaded objects
        StringBuilder[] preloaded = new StringBuilder[5];
        for (int i = 0; i < 5; i++) {
            preloaded[i] = pool.get();
            assertNotNull(preloaded[i]);
        }
        
        // The next get should create a new one since first 5 are taken
        StringBuilder s6 = pool.get();
        assertNotNull(s6);
        boolean isNew = true;
        for (StringBuilder sb : preloaded) {
            if (sb == s6) {
                isNew = false;
                break;
            }
        }
        assertTrue("Expected a newly created instance", isNew);
    }

    @Test
    public void testArrayObjectPoolReleaseAndReuse() {
    	
        ObjectPool<StringBuilder> pool = new ArrayObjectPool<StringBuilder>(10, 5, Builder.createBuilder(StringBuilder.class));
        
        StringBuilder s1 = pool.get();
        StringBuilder s2 = pool.get();
        
        pool.release(s1);
        // Next get should return s1 again, not a new object
        StringBuilder s3 = pool.get();
        assertSame(s1, s3);
        
        pool.release(s2);
        StringBuilder s4 = pool.get();
        assertSame(s2, s4);
    }

    @Test
    public void testArrayObjectPoolGrowth() {
    	
        // Start with capacity 4, preload 4
        ObjectPool<StringBuilder> pool = new ArrayObjectPool<StringBuilder>(4, 4, Builder.createBuilder(StringBuilder.class));
        
        // Take all 4
        StringBuilder[] initial = new StringBuilder[4];
        for (int i = 0; i < 4; i++) {
            initial[i] = pool.get();
        }
        
        // Now the array is full and pointer == array.length
        // The next get should trigger growth
        StringBuilder s5 = pool.get();
        assertNotNull(s5);
        boolean isNew = true;
        for (StringBuilder sb : initial) {
            if (sb == s5) {
                isNew = false;
                break;
            }
        }
        assertTrue("Expected a newly created instance after grow", isNew);
        
        // Release all, now we can get them back to ensure growth was successful
        for (StringBuilder sb : initial) {
            pool.release(sb);
        }
        pool.release(s5);
        
        // Now let's get them all back; if growth worked, we have more capacity now
        for (int i = 0; i < 5; i++) {
            assertNotNull(pool.get());
        }
    }
    
    @Test
    public void testArrayObjectPoolInvertingRelease() {
    	
        ObjectPool<StringBuilder> pool = new ArrayObjectPool<StringBuilder>(5, 2, Builder.createBuilder(StringBuilder.class));
        
        // Initially 2 preloaded
        StringBuilder s1 = pool.get();
        StringBuilder s2 = pool.get();

        // Both taken, pool empty internally
        // Release them:
        pool.release(s2);
        pool.release(s1);

        // Now we should be able to get 2 again
        StringBuilder s3 = pool.get();
        StringBuilder s4 = pool.get();
        
        assertSame(s1, s3);
        assertSame(s2, s4);
        
        // get again
        s1 = pool.get();
        s2 = pool.get();

        // now invert release order
        pool.release(s1);
        pool.release(s2);

        // Now we should be able to get 2 again
        s3 = pool.get();
        s4 = pool.get();
        
        assertSame(s1, s4);
        assertSame(s2, s3);
    }
        
        
    @Test
    public void testArrayObjectPoolReleasingWhenEmpty() {
        
        // If we release when empty (pointer=0), it should not fail:
    	ObjectPool<StringBuilder> pool = new ArrayObjectPool<StringBuilder>(5, 0, Builder.createBuilder(StringBuilder.class));
        // pointer=0, release something not from pool:
        pool.release(new StringBuilder()); // should not fail, just ignore
    }

    @Test
    public void testManyGetsAndReleases() {
    	
        ObjectPool<StringBuilder> pool = new ArrayObjectPool<StringBuilder>(10, 5, Builder.createBuilder(StringBuilder.class));
        // Take all 5 preloaded
        StringBuilder[] arr = new StringBuilder[5];
        for (int i = 0; i < 5; i++) {
            arr[i] = pool.get();
        }

        // Release them all
        for (int i = 0; i < 5; i++) {
            pool.release(arr[i]);
        }

        // Take them all again
        for (int i = 0; i < 5; i++) {
            StringBuilder sb = pool.get();
            assertNotNull(sb);
        }

        // Stress test: get many times over capacity
        for (int i = 0; i < 100; i++) {
            pool.get(); // Will cause multiple growth operations
        }

        // Release a few and get again
        StringBuilder x = new StringBuilder();
        pool.release(x);
        assertSame(x, pool.get());
    }
    
    @Test
    public void testLinkedObjectPoolLinkedListSize() {
    	
        // Preload 2 objects
        LinkedObjectPool<StringBuilder> pool = new LinkedObjectPool<StringBuilder>(2, 2, Builder.createBuilder(StringBuilder.class));
        
        // Initially linked list should have size=2 (from preload)
        assertEquals(2, pool.getLinkedListSize());
        
        // Take both objects out
        StringBuilder s1 = pool.get();
        StringBuilder s2 = pool.get();
        
        // Now the linked list is empty
        assertEquals(0, pool.getLinkedListSize());
        
        // Release them back
        pool.release(s1);
        pool.release(s2);

        // Should have size=2 again
        assertEquals(2, pool.getLinkedListSize());
        
        // Release even more objects (completely new ones)
        // This shows we can keep adding to the linked list
        pool.release(new StringBuilder());
        pool.release(new StringBuilder());
        pool.release(new StringBuilder());
        
        // Now size should be 5 (original 2 + 3 newly released)
        assertEquals(5, pool.getLinkedListSize());
    }

    @Test
    public void testArrayObjectPoolGrowArrayLength() {
    	
        // Create an array pool with a small initial capacity (4) and preload all of them
        ArrayObjectPool<StringBuilder> pool = new ArrayObjectPool<StringBuilder>(4, 4, Builder.createBuilder(StringBuilder.class));
        
        // Initially array length should be 4
        assertEquals(4, pool.getArrayLength());
        
        // Take all 4 objects
        for (int i = 0; i < 4; i++) {
            pool.get();
        }
        
        assertEquals(4, pool.getArrayLength());
        
        // Now the pool is empty, next get should cause a grow
        pool.get(); // triggers growth by 50%, oldLength=4, newLength=6
        assertEquals(6, pool.getArrayLength());
        
        pool.get();
        assertEquals(6, pool.getArrayLength());
        
        pool.get();
        // Now oldLength was 6, so new length after grow should be 6 + 3 = 9
        assertEquals(9, pool.getArrayLength());
    }
}
