package com.cache.redis.models;

import com.cache.redis.constants.CacheType;
import com.cache.redis.interfaces.Cache;

/**
 * Fluent builder for constructing {@link Cache} instances.
 * <p>
 * Encapsulates strategy selection and capacity; returns a {@link KeyValueStore}
 * configured with the chosen {@link CacheType} and capacity.
 */
public class CacheBuilder<K, V> {

	private CacheType cacheType = CacheType.LRU;
	private int capacity = 1000;

	public static <K, V> CacheBuilder<K, V> newBuilder() {
		return new CacheBuilder<>();
	}

	public CacheBuilder<K, V> withType(CacheType type) {
		this.cacheType = type;
		return this;
	}

	public CacheBuilder<K, V> withCapacity(int capacity) {
		this.capacity = capacity;
		return this;
	}

	public Cache<K, V> build() {
		return new KeyValueStore<>(cacheType, capacity);
	}
}


