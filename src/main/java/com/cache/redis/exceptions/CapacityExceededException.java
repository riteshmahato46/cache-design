package com.cache.redis.exceptions;

/**
 * Thrown when a cache write would exceed a configured capacity policy.
 * <p>
 * Currently reserved for byte-based capacity enforcement in future versions.
 */
public class CapacityExceededException extends CacheException {

	public CapacityExceededException(String message) {
		super(message);
	}
}


