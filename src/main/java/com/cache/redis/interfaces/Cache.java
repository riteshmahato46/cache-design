package com.cache.redis.interfaces;

import java.util.concurrent.TimeUnit;

/**
 * Core cache facade exposed to consumers of the library.
 * <p>
 * This interface defines a minimal, generic API for interacting with a cache
 * instance regardless of the underlying eviction strategy or storage details.
 * Implementations are expected to be thread-safe.
 * <p>
 * The default in-memory implementation is provided by {@code KeyValueStore},
 * which composes an {@code IEvictStrategy} such as LRU or LFU.
 */
public interface Cache<K, V> extends AutoCloseable {

	/** Returns the value for the given key or throws if missing/expired. */
	V get(K key);

	/** Inserts or updates a value with a per-entry TTL and unit. */
	void put(K key, V value, long ttl, TimeUnit timeUnit);

	/** Removes a value, returning the previous value if present. */
	V remove(K key);

	/** Returns whether the key is present (not checking TTL). */
	boolean containsKey(K key);

	/** Returns the number of entries currently tracked by the cache. */
	int size();

	/** Clears all entries from the cache. */
	void clear();

	/** Releases any underlying resources (executors, timers, etc.). */
	void close();
}


