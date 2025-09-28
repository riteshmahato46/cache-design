package com.cache.redis.models;

import com.cache.redis.constants.CacheType;
import com.cache.redis.interfaces.Cache;
import com.cache.redis.interfaces.IEvictStrategy;
import com.cache.redis.strategy.LFUStrategy;
import com.cache.redis.strategy.LRUStrategy;

import java.util.concurrent.TimeUnit;

/**
 * Public cache implementation that composes an internal eviction strategy.
 * <p>
 * Acts as a facade that selects a concrete {@link IEvictStrategy} based on
 * {@link CacheType} and exposes the stable {@link Cache} API.
 */
public class KeyValueStore<K, V> implements Cache<K, V> {

	private final CacheType cacheType;
	private final IEvictStrategy<K, V> evictStrategy;
	private final int cacheLimit;

	public KeyValueStore(CacheType cacheType, int cacheLimit) {
		this.cacheType = cacheType;
		this.cacheLimit = cacheLimit;
		switch (this.cacheType) {
		case LRU:
			this.evictStrategy = new LRUStrategy<>(this.cacheLimit);
			break;

		case LFU:
			this.evictStrategy = new LFUStrategy<>(this.cacheLimit);
			break;
		default:
			throw new IllegalStateException("Unsupported cache type: " + cacheType);
		}
	}

	@Override public V get(K key) {
		return this.evictStrategy.get(key);
	}

	@Override public void put(K key, V value, long ttl, TimeUnit timeUnit) {
		this.evictStrategy.put(key, value, ttl, timeUnit);
	}

	@Override public V remove(K key) {
		return this.evictStrategy.remove(key);
	}

	@Override public boolean containsKey(K key) {
		return this.evictStrategy.containsKey(key);
	}

	@Override public int size() {
		return this.evictStrategy.size();
	}

	@Override public void clear() {
		this.evictStrategy.clear();
	}

	@Override public void close() {
		this.evictStrategy.close();
	}
}
