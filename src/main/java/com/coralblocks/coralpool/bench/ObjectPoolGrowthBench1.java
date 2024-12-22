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
package com.coralblocks.coralpool.bench;

import java.util.Random;

import com.coralblocks.coralbench.Bench;
import com.coralblocks.coralpool.ArrayObjectPool;
import com.coralblocks.coralpool.LinkedObjectPool;
import com.coralblocks.coralpool.MultiArrayObjectPool;
import com.coralblocks.coralpool.ObjectPool;
import com.coralblocks.coralpool.StackObjectPool;
import com.coralblocks.coralpool.TieredObjectPool;
import com.coralblocks.coralpool.util.Builder;

public class ObjectPoolGrowthBench1 {

	private static enum Type { LINKED, ARRAY, MULTI, STACK, TIERED }
	
	public static void main(String[] args) {
		
		final int warmup = args.length > 0 ? Integer.parseInt(args[0]) : 1_000_000;
		final int measurements = args.length > 1 ? Integer.parseInt(args[1]) : 5_000_000;
		final int totalIterations = warmup + measurements;
		final int initialCapacity = args.length > 2 ? Integer.parseInt(args[2]) : 100;
		final int preloadCount = args.length > 3 ? Integer.parseInt(args[3]) : initialCapacity;
		final int randomSeed = args.length > 4 ? Integer.parseInt(args[4]) : 44444;
		
		Bench benchGet = new Bench(warmup);
		Bench benchRel = new Bench(warmup);
		
		final Object object = new Object();
		
		System.out.println();
		
		for(Type type : Type.values()) {
			
			Random rand = new Random(randomSeed);
		
			ObjectPool<Object> pool = createObjectPool(type, initialCapacity, preloadCount, object);
			
			System.out.println("type=" + pool.getClass().getSimpleName() + 
							   " warmup=" + warmup + " measurements=" + measurements + "\n");
			
			int remaining = totalIterations;
			
			benchGet.reset(true);
			benchRel.reset(true);
			
			while(remaining != 0) {
				
				int x = rand.nextInt(2) + 1;
				x = Math.min(remaining, x);
				
				for(int i = 0; i < x; i++) {
					benchGet.mark();
					pool.get();
					benchGet.measure();
				}
				for(int i = 0; i < x; i++) {
					benchRel.mark();
					pool.release(object);
					benchRel.measure();
				}
	
				remaining -= x;
				if (remaining == 0) break;
	
				x = rand.nextInt(initialCapacity * 100) + 1;
				x = Math.min(remaining, x);
				
				for(int i = 0; i < x; i++) {
					benchGet.mark();
					pool.get();
					benchGet.measure();
				}
				for(int i = 0; i < x; i++) {
					benchRel.mark();
					pool.release(object);
					benchRel.measure();
				}
				
				remaining -= x;
			}
			
			System.out.println("GET:");
			benchGet.printResults();
			
			System.out.println("RELEASE:");
			benchRel.printResults();
		}
	}
	
	private static ObjectPool<Object> createObjectPool(Type type, int initialCapacity, int preloadCount, final Object object) {
		
		Builder<Object> builder = new Builder<Object>() {
			@Override
			public Object newInstance() {
				return object;
			}
		};
		
		if (type == Type.LINKED) {
			return new LinkedObjectPool<Object>(initialCapacity, preloadCount, builder);
		} else if (type == Type.ARRAY) {
			return new ArrayObjectPool<Object>(initialCapacity, preloadCount, builder);
		} else if (type == Type.MULTI) {
			return new MultiArrayObjectPool<Object>(initialCapacity, preloadCount, builder);
		} else if (type == Type.STACK) {
			return new StackObjectPool<Object>(initialCapacity, preloadCount, builder);
		} else if (type == Type.TIERED) {
			return new TieredObjectPool<Object>(initialCapacity, preloadCount, builder);
		} else {
			throw new IllegalArgumentException("Bad type: " + type);
		}
	}
}