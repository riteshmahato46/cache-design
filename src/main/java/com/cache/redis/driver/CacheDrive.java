package com.cache.redis.driver;

import com.cache.redis.constants.CacheType;
import com.cache.redis.interfaces.Cache;
import com.cache.redis.models.KeyValueStore;

import java.util.concurrent.TimeUnit;

/**
 * Simple demo entry point showing how to construct and interact with the
 * cache via the public {@link Cache} interface.
 */
public class CacheDrive {

	public static void main(String[] args) throws InterruptedException {
		try (Cache<String, String> cache = new KeyValueStore<>(CacheType.LRU, 4)) {
			cache.put("Key1", "Value1", 10L, TimeUnit.SECONDS);
			cache.put("Key2", "Value2", 3L, TimeUnit.SECONDS);
			cache.put("Key3", "Value3", 7L, TimeUnit.SECONDS);
			cache.put("Key4", "Value4", 1L, TimeUnit.SECONDS);
			cache.put("Key5", "Value5", 8L, TimeUnit.SECONDS);
			Thread.sleep(2000);
			cache.put("Key6", "Value6", 8L, TimeUnit.SECONDS);
			cache.put("Key7", "Value7", 8L, TimeUnit.SECONDS);

			System.out.println(cache.get("Key3"));
			System.out.println(cache.get("Key5"));
			System.out.println(cache.get("Key4"));
		}
	}
}
