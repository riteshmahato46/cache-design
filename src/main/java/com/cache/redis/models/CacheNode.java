package com.cache.redis.models;

import java.util.concurrent.TimeUnit;

/**
 * Doubly-linked list node used by eviction strategies like LRU/LFU.
 * <p>
 * Holds the key, value, and TTL metadata and participates in a linked list for
 * O(1) reordering. TTL is expressed as a creation time + (ttl, unit) pair.
 */
public class CacheNode<K, V> {

	private final K key;
	private V value;
	private final long ttl;
	private final TimeUnit timeUnit;
	private final long creationTime;
	private CacheNode<K, V> nextNode;
	private CacheNode<K, V> prevNode;

	public CacheNode(K key, V value, long ttl, TimeUnit tunit) {
		this.key = key;
		this.value = value;
		this.ttl = ttl;
		this.timeUnit = tunit;
		this.creationTime = System.currentTimeMillis();
		this.prevNode = null;
		this.nextNode = null;
	}

	public K getKey() { return key; }

	public V getValue() { return value; }

	public void setValue(V value) { this.value = value; }

	public CacheNode<K, V> getNextNode() { return nextNode; }

	public CacheNode<K, V> getPrevNode() { return prevNode; }

	public void setPrevNode(CacheNode<K, V> node) { this.prevNode = node; }

	public void setNextNode(CacheNode<K, V> node) { this.nextNode = node; }

	public long getExpiryTime() { return (this.creationTime + this.timeUnit.toMillis(ttl)); }
}
