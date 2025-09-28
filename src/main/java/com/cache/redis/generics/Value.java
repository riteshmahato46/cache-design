package com.cache.redis.generics;

import java.util.Objects;

/**
 * Immutable wrapper for a cache value. Provided for prior API compatibility;
 * the current public API generally accepts plain values of type {@code V}.
 * Can be used by callers who prefer wrapping to make mutability explicit.
 */
public final class Value<T> {

	private final T value;

	public Value(T value) {
		this.value = value;
	}

	public T getValue() {
		return this.value;
	}

	@Override public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Value<?> value1 = (Value<?>) o;
		return Objects.equals(value, value1.value);
	}

	@Override public int hashCode() {
		return Objects.hash(value);
	}

	@Override public String toString() {
		return String.valueOf(value);
	}
}
