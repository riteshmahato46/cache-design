package com.cache.redis.strategy;

import com.cache.redis.interfaces.IEvictStrategy;
import com.cache.redis.models.CacheNode;
import com.cache.redis.exceptions.KeyNotFoundException;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * LRU (Least Recently Used) eviction strategy.
 * <p>
 * Maintains a doubly-linked list of entries ordered by recency and a
 * concurrent map for O(1) lookups. Reads move the accessed node to the head;
 * when capacity is reached, the tail node is evicted.
 */
public class LRUStrategy<K, V> implements IEvictStrategy<K, V> {

	private final static TimeUnit defaultTimeUnit = TimeUnit.SECONDS;
	private final int cacheLimit;
	private final AtomicInteger currSize;
	private final ConcurrentHashMap<K, CacheNode<K, V>> nodeMap;
	private final ReentrantLock lock;
	private final ExecutorService executorService;
	private final ScheduledExecutorService cleanUpService;
	private final CacheNode<K, V> headNode;
	private final CacheNode<K, V> tailNode;

	public LRUStrategy(int cacheLimit) {
		this.cacheLimit = cacheLimit;
		this.currSize = new AtomicInteger(0);
		this.nodeMap = new ConcurrentHashMap<>();
		this.lock = new ReentrantLock();
		this.executorService = Executors.newSingleThreadExecutor();
		this.cleanUpService = Executors.newSingleThreadScheduledExecutor();
		this.headNode = new CacheNode<>(null, null, Long.MAX_VALUE, defaultTimeUnit);
		this.tailNode = new CacheNode<>(null, null, Long.MAX_VALUE, defaultTimeUnit);
		this.headNode.setNextNode(this.tailNode);
		this.tailNode.setPrevNode(this.headNode);
		cleanUpService.scheduleAtFixedRate(cleanUp(), 1, defaultTimeUnit.toSeconds(1L), defaultTimeUnit);
	}

	private void moveToHead(CacheNode<K, V> node) {
		CacheNode<K, V> prev = node.getPrevNode();
		CacheNode<K, V> next = node.getNextNode();
		if (prev != null) prev.setNextNode(next);
		if (next != null) next.setPrevNode(prev);
		CacheNode<K, V> first = headNode.getNextNode();
		node.setPrevNode(headNode);
		node.setNextNode(first);
		first.setPrevNode(node);
		headNode.setNextNode(node);
	}

	private void evictTailIfNeeded() {
		CacheNode<K, V> last = tailNode.getPrevNode();
		if (last != null && last != headNode) {
			CacheNode<K, V> newLast = last.getPrevNode();
			newLast.setNextNode(tailNode);
			tailNode.setPrevNode(newLast);
			nodeMap.remove(last.getKey());
			currSize.getAndDecrement();
		}
	}

	@Override public V get(K key) {
		Future<V> future = executorService.submit(() -> {
			lock.lock();
			try {
				CacheNode<K, V> node = nodeMap.get(key);
				if (node == null) throw new KeyNotFoundException("Key not found: " + key);
				if (node.getExpiryTime() <= System.currentTimeMillis()) {
					// remove expired
					nodeMap.remove(key);
					CacheNode<K, V> prev = node.getPrevNode();
					CacheNode<K, V> next = node.getNextNode();
					if (prev != null) prev.setNextNode(next);
					if (next != null) next.setPrevNode(prev);
					currSize.decrementAndGet();
					throw new KeyNotFoundException("Key expired: " + key);
				}
				moveToHead(node);
				return node.getValue();
			} finally {
				lock.unlock();
			}
		});
		try { return future.get(); } catch (RuntimeException e) { throw e; } catch (Exception e) { throw new KeyNotFoundException("Lookup failed: " + key); }
	}

	@Override public void put(K key, V value, long ttl, TimeUnit timeUnit) {
		executorService.execute(() -> {
			lock.lock();
			try {
				CacheNode<K, V> node = nodeMap.get(key);
				if (node != null) {
					node.setValue(value);
					moveToHead(node);
					return;
				}
				if (currSize.get() >= cacheLimit) evictTailIfNeeded();
				CacheNode<K, V> newNode = new CacheNode<>(key, value, ttl, timeUnit == null ? defaultTimeUnit : timeUnit);
				nodeMap.put(key, newNode);
				CacheNode<K, V> first = headNode.getNextNode();
				newNode.setPrevNode(headNode);
				newNode.setNextNode(first);
				first.setPrevNode(newNode);
				headNode.setNextNode(newNode);
				currSize.incrementAndGet();
			} finally {
				lock.unlock();
			}
		});
	}

	@Override public V remove(K key) {
		Future<V> future = executorService.submit(() -> {
			lock.lock();
			try {
				CacheNode<K, V> node = nodeMap.remove(key);
				if (node == null) return null;
				CacheNode<K, V> prev = node.getPrevNode();
				CacheNode<K, V> next = node.getNextNode();
				if (prev != null) prev.setNextNode(next);
				if (next != null) next.setPrevNode(prev);
				currSize.decrementAndGet();
				return node.getValue();
			} finally { lock.unlock(); }
		});
		try { return future.get(); } catch (Exception e) { return null; }
	}

	@Override public boolean containsKey(K key) { return nodeMap.containsKey(key); }

	@Override public int size() { return currSize.get(); }

	@Override public void clear() {
		lock.lock();
		try {
			nodeMap.clear();
			currSize.set(0);
			headNode.setNextNode(tailNode);
			tailNode.setPrevNode(headNode);
		} finally { lock.unlock(); }
	}

	@Override public Runnable cleanUp() {
		return () -> {
			lock.lock();
			try {
				CacheNode<K, V> itr = headNode.getNextNode();
				while (itr != tailNode) {
					if (itr.getExpiryTime() <= System.currentTimeMillis()) {
						CacheNode<K, V> next = itr.getNextNode();
						CacheNode<K, V> prev = itr.getPrevNode();
						next.setPrevNode(prev);
						prev.setNextNode(next);
						nodeMap.remove(itr.getKey());
						currSize.getAndDecrement();
						itr = next;
						continue;
					}
					itr = itr.getNextNode();
				}
			} finally { lock.unlock(); }
		};
	}

	@Override public void close() {
		cleanUpService.shutdownNow();
		executorService.shutdownNow();
	}
}


