package com.cache.redis.constants;

/**
 * Enumerates the supported eviction strategies for the in-memory cache.
 * Used by {@code CacheBuilder} and {@code KeyValueStore} to select a concrete
 * {@code IEvictStrategy} implementation.
 */
public enum CacheType {
	LRU, LFU;
}
