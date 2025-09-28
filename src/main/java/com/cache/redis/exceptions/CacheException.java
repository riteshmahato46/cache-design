package com.cache.redis.exceptions;

/**
 * Base unchecked exception for cache-related errors.
 * <p>
 * Specific error categories should extend this class, e.g.
 * {@link KeyNotFoundException} and {@link CapacityExceededException}.
 */
public class CacheException extends RuntimeException {

	public CacheException(String message) {
		super(message);
	}

	public CacheException(String message, Throwable cause) {
		super(message, cause);
	}
}


