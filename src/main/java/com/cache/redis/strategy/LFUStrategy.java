package com.cache.redis.strategy;

import com.cache.redis.interfaces.IEvictStrategy;
import com.cache.redis.models.CacheNode;
import com.cache.redis.exceptions.KeyNotFoundException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LFU (Least Frequently Used) eviction strategy.
 * <p>
 * Tracks access frequency per key using frequency buckets (freq -> keys).
 * On eviction, removes a key from the lowest-frequency bucket (ties broken by
 * insertion order within the bucket).
 */
public class LFUStrategy<K, V> implements IEvictStrategy<K, V> {

	private final int cacheLimit;
	private final ConcurrentHashMap<K, CacheNode<K, V>> nodeMap;
	private final Map<K, Integer> frequencyMap;
	private final Map<Integer, LinkedHashSet<K>> freqToKeys;
	private final AtomicInteger size;
	private volatile int minFreq;

	public LFUStrategy(int cacheLimit) {
		this.cacheLimit = cacheLimit;
		this.nodeMap = new ConcurrentHashMap<>();
		this.frequencyMap = new HashMap<>();
		this.freqToKeys = new HashMap<>();
		this.size = new AtomicInteger(0);
		this.minFreq = 0;
	}


	@Override public V get(K key) {
		CacheNode<K, V> node = nodeMap.get(key);
		if (node == null) throw new KeyNotFoundException("Key not found: " + key);
		if (node.getExpiryTime() <= System.currentTimeMillis()) {
			nodeMap.remove(key);
			int f = frequencyMap.getOrDefault(key, 0);
			LinkedHashSet<K> set = freqToKeys.getOrDefault(f, new LinkedHashSet<>());
			set.remove(key);
			size.decrementAndGet();
			throw new KeyNotFoundException("Key expired: " + key);
		}
		int freq = frequencyMap.getOrDefault(key, 0);
		frequencyMap.put(key, freq + 1);
		freqToKeys.get(freq).remove(key);
		freqToKeys.computeIfAbsent(freq + 1, f -> new LinkedHashSet<>()).add(key);
		if (freqToKeys.getOrDefault(freq, new LinkedHashSet<>()).isEmpty() && minFreq == freq) minFreq++;
		return node.getValue();
	}

	@Override public void put(K key, V value, long ttl, TimeUnit timeUnit) {
		if (cacheLimit <= 0) return;
		CacheNode<K, V> node = nodeMap.get(key);
		if (node != null) {
			node.setValue(value);
			get(key);
			return;
		}
		if (size.get() >= cacheLimit) {
			LinkedHashSet<K> keys = freqToKeys.get(minFreq);
			Iterator<K> it = keys.iterator();
			if (it.hasNext()) {
				K evictKey = it.next();
				it.remove();
				nodeMap.remove(evictKey);
				frequencyMap.remove(evictKey);
				size.decrementAndGet();
			}
		}
		CacheNode<K, V> newNode = new CacheNode<>(key, value, ttl, timeUnit == null ? TimeUnit.SECONDS : timeUnit);
		nodeMap.put(key, newNode);
		frequencyMap.put(key, 1);
		freqToKeys.computeIfAbsent(1, f -> new LinkedHashSet<>()).add(key);
		minFreq = 1;
		size.incrementAndGet();
	}

	@Override public V remove(K key) {
		CacheNode<K, V> node = nodeMap.remove(key);
		if (node == null) return null;
		int freq = frequencyMap.getOrDefault(key, 0);
		frequencyMap.remove(key);
		LinkedHashSet<K> set = freqToKeys.getOrDefault(freq, new LinkedHashSet<>());
		set.remove(key);
		size.decrementAndGet();
		return node.getValue();
	}

	@Override public boolean containsKey(K key) { return nodeMap.containsKey(key); }

	@Override public int size() { return size.get(); }

	@Override public void clear() {
		nodeMap.clear();
		frequencyMap.clear();
		freqToKeys.clear();
		size.set(0);
		minFreq = 0;
	}

	@Override public Runnable cleanUp() { return () -> {}; }

	@Override public void close() { }
}
