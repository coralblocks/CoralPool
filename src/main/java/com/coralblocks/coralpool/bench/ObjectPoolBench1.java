/* 
 * Copyright 2024 (c) CoralBlocks - http://www.coralblocks.com
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
import com.coralblocks.coralpool.ObjectPool;
import com.coralblocks.coralpool.util.Builder;

public class ObjectPoolBench1 {

	private static enum Type { LINKED, ARRAY }
	
	public static void main(String[] args) {
		
		final Type type = getType(args);
		final int warmup = args.length > 1 ? Integer.parseInt(args[1]) : 1_000_000;
		final int measurements = args.length > 2 ? Integer.parseInt(args[2]) : 2_000_000;
		final int totalIterations = warmup + measurements;
		final int initialCapacity = args.length > 3 ? Integer.parseInt(args[3]) : 1_000;
		final int preloadCount = args.length > 4 ? Integer.parseInt(args[4]) : initialCapacity;
		final int randomSeed = args.length > 5 ? Integer.parseInt(args[5]) : 44444;
		
		Random rand = new Random(randomSeed);
		Bench benchGet = new Bench(warmup);
		Bench benchRel = new Bench(warmup);
		
		final Object object = new Object();
		Builder<Object> builder = new Builder<Object>() {
			@Override
			public Object newInstance() {
				return object;
			}
		};
		
		ObjectPool<Object> pool = null;
		if (type == Type.LINKED) {
			pool = new LinkedObjectPool<Object>(initialCapacity, preloadCount, builder);
		} else if (type == Type.ARRAY) {
			pool = new ArrayObjectPool<Object>(initialCapacity, preloadCount, builder);
		} else {
			throw new IllegalArgumentException("Bad type: " + type);
		}
		
		System.out.println("\ntype=" + pool.getClass().getSimpleName() + 
						   " warmup=" + warmup + " measurements=" + measurements + "\n");
		
		int remaining = totalIterations;
		
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

			x = rand.nextInt(initialCapacity) + 1;
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
	
	private static Type getType(String[] args) {
		if (args.length == 0) {
			throw new IllegalArgumentException("Pool type must be provided! (LinkedObjectPool or ArrayObjectPool)");
		}
		String type = args[0];
		if (type.equalsIgnoreCase(ArrayObjectPool.class.getSimpleName())) return Type.ARRAY;
		if (type.equalsIgnoreCase(LinkedObjectPool.class.getSimpleName())) return Type.LINKED;
		throw new IllegalArgumentException("Unrecognizable pool implementation: " + type);
	}
	
}