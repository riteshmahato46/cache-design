package com.cache.redis.exceptions;

/**
 * Thrown when a key lookup fails because the key is absent or the entry
 * has expired per its TTL.
 */
public class KeyNotFoundException extends CacheException {

	public KeyNotFoundException(String message) {
		super(message);
	}
}


