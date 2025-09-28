package com.cache.redis.generics;

import java.util.Objects;

/**
 * Immutable wrapper for a cache key. Provided primarily for prior API
 * compatibility; the current public API generally accepts plain keys of type
 * {@code K}. Still useful when callers want stronger typing than raw strings
 * or when keys need custom equality semantics.
 */
public final class Key<T> {

	private final T key;

	public Key(T key) {
		this.key = key;
	}

	public T getKey() {
		return this.key;
	}

	@Override public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Key<?> key1 = (Key<?>) o;
		return Objects.equals(key, key1.key);
	}

	@Override public int hashCode() {
		return Objects.hash(key);
	}

	@Override public String toString() {
		return String.valueOf(key);
	}
}
