## In-Memory Cache (Java)

[![JDK](https://img.shields.io/badge/JDK-21%2B-blue.svg)](https://adoptium.net/)
[![Status](https://img.shields.io/badge/status-alpha-orange.svg)](https://github.com/)
[![License](https://img.shields.io/badge/license-MIT-lightgrey.svg)](LICENSE)

A fast, embeddable, thread-safe in-memory key–value cache with TTL and pluggable eviction strategies.

### Why this library?
- **Simple API**: a tiny `Cache<K,V>` interface with predictable semantics.
- **Safe under load**: lock-disciplined linked list + concurrent map; background TTL cleanup.
- **Extensible**: add strategies, listeners, and policies without touching the core.

---

## Features
- **Generic API**: `Cache<K,V>` with `get/put/remove/containsKey/size/clear/close`.
- **Evictions**: `LRU` and `LFU` strategies (O(1)-style operations).
- **TTL per entry**: expires on read + periodic sweep.
- **Thread-safety**: `ConcurrentHashMap` + `ReentrantLock` for list ops; single-threaded mutation executor.
- **Exceptions**: `CacheException`, `KeyNotFoundException`, `CapacityExceededException`.
- **Builder**: `CacheBuilder` for ergonomic construction.

---

## Quick start
```java
import com.cache.redis.constants.CacheType;
import com.cache.redis.interfaces.Cache;
import com.cache.redis.models.CacheBuilder;
import java.util.concurrent.TimeUnit;

try (Cache<String, String> cache = CacheBuilder.<String, String>newBuilder()
	.withType(CacheType.LRU)
	.withCapacity(10_000)
	.build()) {

	cache.put("user:1", "Alice", 60, TimeUnit.SECONDS);
	String v = cache.get("user:1"); // "Alice"
}
```

Build and run the included demo:
```bash
mvn -q clean package
java -cp target/in-memory-cache-1.0-SNAPSHOT.jar com.cache.redis.driver.CacheDrive
```

---

## API overview
```java
public interface Cache<K, V> extends AutoCloseable {
	V get(K key);
	void put(K key, V value, long ttl, TimeUnit unit);
	V remove(K key);
	boolean containsKey(K key);
	int size();
	void clear();
	void close();
}
```

Strategy selection via the facade:
```java
new KeyValueStore<>(CacheType.LRU, 1000);
new KeyValueStore<>(CacheType.LFU, 1000);
```

---

## Architecture
- `KeyValueStore<K,V>`: Facade over an `IEvictStrategy<K,V>` implementation.
- `LRUStrategy<K,V>`: Doubly-linked list of `CacheNode<K,V>` + map for O(1) get/put/move-to-front.
- `LFUStrategy<K,V>`: Frequency buckets (`freq -> LinkedHashSet<K>`) + map.
- `CacheNode<K,V>`: Key, value, TTL metadata, list links.
- Cleanup: Scheduled executor runs `cleanUp()` periodically; reads also enforce TTL lazily.

### Architecture diagram
```mermaid
flowchart TD
    A[Application Code] --> B[Cache&lt;K,V&gt;]
    B --> C[KeyValueStore&lt;K,V&gt;]
    C -.selects via CacheType.-> D{IEvictStrategy&lt;K,V&gt;}
    D --> E[LRUStrategy]
    D --> F[LFUStrategy]

    subgraph LRU internals
      E --> G[ConcurrentHashMap&lt;K, CacheNode&lt;K,V&gt;&gt;]
      E --> H[Doubly Linked List of CacheNode&lt;K,V&gt;]
    end

    subgraph LFU internals
      F --> I[ConcurrentHashMap&lt;K, CacheNode&lt;K,V&gt;&gt;]
      F --> J[Frequency Buckets: freq → LinkedHashSet&lt;K&gt;]
    end

    subgraph Background
      K[Scheduled cleanUp()]
      K --> D
    end
```

Threading model
- Mutating calls (`put/remove`) are executed on a single-threaded executor and guarded by a lock for list integrity.
- Read path uses a small critical section to move nodes to the head and to prune expired entries.
- Data map uses `ConcurrentHashMap`.

---

## TTL semantics
- TTL is provided per `put` call.
- An entry is considered expired when `creationTime + ttl <= now`.
- Expired entries are removed lazily on `get` and proactively by scheduled cleanup.

---

## Configuration
Using the builder:
```java
Cache<String, byte[]> cache = CacheBuilder.<String, byte[]>newBuilder()
	.withType(CacheType.LFU)
	.withCapacity(50_000)
	.build();
```

Notes
- Capacity is entry-count based. For very large values use external sharding or add a byte-based weigher (see Roadmap).
- Provide TTL and unit per `put`.

---

## Exceptions
- `KeyNotFoundException`: thrown by `get` when key is missing or expired.
- `CapacityExceededException`: reserved for future use when enforcing byte-based capacity.
- `CacheException`: base runtime for other domain errors.

Use-cases typically catch `KeyNotFoundException` on read.

---

## Extending the cache
Add a new eviction strategy:
1. Implement `IEvictStrategy<K,V>`.
2. Wire it in `KeyValueStore` switch or expose via a new builder method.

Add metrics/listeners (suggested hook points):
- Wrap `get/put/remove` in your strategy and publish counters/timers.
- Expose JMX or Micrometer meters from strategy instances.

---

## Roadmap
- Byte-based capacity using a `Weigher<V>` and `maxBytes`.
- Metrics and event listeners (hit/miss/eviction/expire).
- Off-heap or disk tiers (e.g., Chronicle/RocksDB) for larger datasets.
- Distributed/tiered cache wrapper with consistent hashing and replication.

---

## Project structure
```
src/main/java/com/cache/redis/
  constants/CacheType.java
  interfaces/{Cache.java, IEvictStrategy.java}
  models/{CacheNode.java, KeyValueStore.java, CacheBuilder.java}
  strategy/{LRUStrategy.java, LFUStrategy.java}
  exceptions/{CacheException.java, KeyNotFoundException.java, CapacityExceededException.java}
driver/CacheDrive.java
```

An additional sample project lives at `online-chess-game/` to demonstrate a clean, concurrent architecture in a different domain.

---

## Getting started (dev setup)

Prerequisites
- Git
- JDK 21+ (`java -version`)
- Maven 3.8+ (`mvn -version`)

Clone and build
```bash
git clone https://github.com/<your-user>/in-memory-cache.git
cd in-memory-cache
mvn -q clean package
```

Run the demo
```bash
java -cp target/in-memory-cache-1.0-SNAPSHOT.jar com.cache.redis.driver.CacheDrive
```

Use in another project (local install)
```bash
mvn -q clean install
```
Then add to your project's `pom.xml`:
```xml
<dependency>
  <groupId>org.example</groupId>
  <artifactId>in-memory-cache</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

Open in an IDE
- Import as a Maven project.
- JDK language level 21 or higher.

Optional: chess demo project
```bash
mvn -q -f online-chess-game/pom.xml clean package
java -cp online-chess-game/target/online-chess-game-1.0-SNAPSHOT.jar org.example.chess.driver.ChessDrive
```

Common commands
```bash
# Compile without tests
mvn -q -DskipTests clean compile

# Run unit tests (if/when added)
mvn -q test

# Install to local maven repo
mvn -q clean install
```

---

## Compatibility
- Java 21+
- Built with Maven

---

## Contributing
Contributions are welcome! Please:
- Open an issue to discuss significant changes.
- Include tests where practical and keep public APIs generic and minimal.
- Match the existing code style and avoid unrelated refactors in the same edit.

### Development
```bash
mvn -q clean package
```

---

## Security
If you discover a security issue, please report it privately via issues marked as security or by contacting the maintainer. Avoid filing public exploits before a coordinated fix.

---

## License
This project is licensed under the MIT License. See `LICENSE` for details.

---

## FAQ
**Q: Does the cache enforce a max memory footprint?**
A: Currently capacity is by entry count. For very large values, consider sharding or extending with a byte-based weigher (planned).

**Q: What happens on expiration?**
A: Reads lazily remove expired entries; a scheduled cleanup also prunes stale nodes periodically.

**Q: Is `get` blocking?**
A: Operations complete quickly; mutations are sequenced on an executor to simplify consistency of the LRU/LFU structures.