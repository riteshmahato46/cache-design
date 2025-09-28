package com.cache.redis.interfaces;

import java.util.concurrent.TimeUnit;

/**
 * Internal strategy contract that encapsulates a specific eviction policy and
 * storage details (e.g., LRU, LFU). The {@code KeyValueStore} composes an
 * implementation of this interface to provide the public {@code Cache} API.
 * <p>
 * Implementations should be thread-safe and may manage background cleanup
 * tasks (see {@link #cleanUp()}).
 */
public interface IEvictStrategy<K, V> extends AutoCloseable {

	/** Returns the value for the key or throws if missing/expired. */
	V get(K key);

	/** Inserts or updates a value with TTL. */
	void put(K key, V value, long ttl, TimeUnit timeUnit);

	/** Removes the entry associated with the key, returning the previous value. */
	V remove(K key);

	/** Returns whether the key is present in the internal map. */
	boolean containsKey(K key);

	/** Current number of entries tracked by this strategy. */
	int size();

	/** Removes all entries from the strategy. */
	void clear();

	/** A periodic cleanup task that can be scheduled to prune expired entries. */
	Runnable cleanUp();

	/** Releases any resources (executors, timers). */
	void close();
}
