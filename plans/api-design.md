# TACBDatabase API Design Plan

## Overview

TACBDatabase is a multi-platform Minecraft database library providing unified data access across Paper, Velocity, and Minestom. It uses **ORMLite + PostgreSQL** for persistent storage and **Lettuce (Redis)** for caching, pub/sub, and real-time packet communication.

## Architecture: Proxy-as-Master

The plugin is installed on:
- **Velocity proxy** — acts as the **master** node. It has full PostgreSQL access and periodically flushes Redis cache to PostgreSQL.
- **Paper / Minestom servers** — act as **slave** nodes. They only use Redis for caching and real-time sync. They do NOT have direct PostgreSQL access.

This means:
- Under servers read/write to Redis only (fast, low-latency).
- The proxy syncs Redis → PostgreSQL on a configurable interval.
- All cross-server communication goes through Redis pub/sub.

---

## 1. Module Structure

```
TACBDatabase/
├── api/                    # Core platform-agnostic API (no platform dependencies)
├── paper/                  # Paper (Bukkit/Spigot) platform implementation
├── velocity/               # Velocity proxy platform implementation
├── minestom/               # Minestom server platform implementation
├── build.gradle.kts        # Root build config
└── settings.gradle.kts     # Module includes
```

### Dependency Hierarchy

```
paper     → depends on api + Paper API
velocity  → depends on api + Velocity API
minestom  → depends on api + Minestom API
api       → depends on ORMLite, Lettuce, HikariCP, SLF4J, Jackson
```

---

## 2. Core Package: `de.papercompiler.tacbdatabase`

### 2.1 Bootstrap & Platform

| Interface/Class | Purpose |
|-----------------|---------|
| [`TACBDatabase`](api/src/main/java/de/papercompiler/tacbdatabase/TACBDatabase.java) | Main entry point. `bootstrap(Platform, TACBConfig)` initializes everything. |
| [`Platform`](api/src/main/java/de/papercompiler/tacbdatabase/platform/Platform.java) | Abstraction over Minecraft platform. Provides server info, scheduler, logger. |
| [`PlatformType`](api/src/main/java/de/papercompiler/tacbdatabase/platform/PlatformType.java) | Enum: `PAPER`, `VELOCITY`, `MINESTOM`. Used for auto-detection. |
| [`Server`](api/src/main/java/de/papercompiler/tacbdatabase/platform/Server.java) | Wraps platform-specific server instance (e.g., `Server`, `ProxyServer`, `MinecraftServer`). |

### 2.2 Configuration

| Interface/Class | Purpose |
|-----------------|---------|
| [`TACBConfig`](api/src/main/java/de/papercompiler/tacbdatabase/config/TACBConfig.java) | Top-level config holder. |
| [`DatabaseConfig`](api/src/main/java/de/papercompiler/tacbdatabase/config/DatabaseConfig.java) | PostgreSQL connection settings (url, user, password, pool size). |
| [`RedisConfig`](api/src/main/java/de/papercompiler/tacbdatabase/config/RedisConfig.java) | Redis connection settings (host, port, password, database, pool size). |

---

## 3. Entity Models: `de.papercompiler.tacbdatabase.entity`

All entities are ORMLite-annotated POJOs. They implement [`Entity`](api/src/main/java/de/papercompiler/tacbdatabase/entity/Entity.java) which provides `getId()`, `getCreatedAt()`, `getUpdatedAt()`.

### 3.1 Base Entity

```java
public interface Entity extends Serializable {
    Long getId();
    Instant getCreatedAt();
    Instant getUpdatedAt();
}
```

### 3.2 Player

```java
@DatabaseTable(tableName = "players")
public class Player implements Entity {
    @DatabaseField(id = true, generated = true)
    private Long id;

    @DatabaseField(unique = true, canBeNull = false)
    private UUID uuid;

    @DatabaseField(canBeNull = false)
    private String name;

    @DatabaseField
    private String lastServer;

    @DatabaseField
    private Instant firstJoin;

    @DatabaseField
    private Instant lastJoin;

    @DatabaseField
    private long playtimeTicks;

    // getters/setters
}
```

### 3.3 Guild

```java
@DatabaseTable(tableName = "guilds")
public class Guild implements Entity {
    @DatabaseField(id = true, generated = true)
    private Long id;

    @DatabaseField(unique = true, canBeNull = false)
    private String name;

    @DatabaseField(unique = true)
    private String tag;

    @DatabaseField
    private UUID ownerUuid;

    @DatabaseField
    private String description;

    @DatabaseField
    private Instant createdAt;

    // getters/setters
}
```

### 3.4 Economy

```java
@DatabaseTable(tableName = "economy")
public class Economy implements Entity {
    @DatabaseField(id = true, generated = true)
    private Long id;

    @DatabaseField(unique = true, canBeNull = false)
    private UUID uuid;

    @DatabaseField
    private BigDecimal balance;

    @DatabaseField
    private Currency currency;

    // getters/setters
}
```

### 3.5 Home

```java
@DatabaseTable(tableName = "homes")
public class Home implements Entity {
    @DatabaseField(id = true, generated = true)
    private Long id;

    @DatabaseField(uniqueCombo = true, canBeNull = false)
    private UUID uuid;

    @DatabaseField(uniqueCombo = true, canBeNull = false)
    private String name;

    @DatabaseField(canBeNull = false)
    private String world;

    @DatabaseField(canBeNull = false)
    private double x;

    @DatabaseField(canBeNull = false)
    private double y;

    @DatabaseField(canBeNull = false)
    private double z;

    @DatabaseField
    private float yaw;

    @DatabaseField
    private float pitch;

    // getters/setters
}
```

### 3.6 Ban

```java
@DatabaseTable(tableName = "bans")
public class Ban implements Entity {
    @DatabaseField(id = true, generated = true)
    private Long id;

    @DatabaseField(uniqueCombo = true, canBeNull = false)
    private UUID targetUuid;

    @DatabaseField(uniqueCombo = true)
    private String targetIp;

    @DatabaseField(canBeNull = false)
    private UUID sourceUuid;

    @DatabaseField(canBeNull = false)
    private String reason;

    @DatabaseField(canBeNull = false)
    private Instant expiresAt; // null = permanent

    @DatabaseField(canBeNull = false)
    private Instant createdAt;

    // getters/setters
}
```

---

## 4. Repository Layer: `de.papercompiler.tacbdatabase.repository`

Generic repository pattern with ORMLite implementation and Redis caching.

### 4.1 Base Repository

```java
public interface Repository<T, ID> {
    CompletableFuture<Optional<T>> findById(ID id);
    CompletableFuture<List<T>> findAll();
    CompletableFuture<List<T>> find(WhereCondition... conditions);
    CompletableFuture<T> save(T entity);
    CompletableFuture<T> update(T entity);
    CompletableFuture<Void> delete(T entity);
    CompletableFuture<Void> deleteById(ID id);
    CompletableFuture<Boolean> exists(ID id);
    CompletableFuture<Long> count();
}
```

### 4.2 Specialized Repositories

| Repository | Additional Methods |
|------------|-------------------|
| [`PlayerRepository`](api/src/main/java/de/papercompiler/tacbdatabase/repository/PlayerRepository.java) | `findByUuid(UUID)`, `findByName(String)`, `findOnline()` |
| [`GuildRepository`](api/src/main/java/de/papercompiler/tacbdatabase/repository/GuildRepository.java) | `findByOwner(UUID)`, `findByTag(String)` |
| [`EconomyRepository`](api/src/main/java/de/papercompiler/tacbdatabase/repository/EconomyRepository.java) | `findByUuid(UUID)`, `adjustBalance(UUID, BigDecimal)` |
| [`HomeRepository`](api/src/main/java/de/papercompiler/tacbdatabase/repository/HomeRepository.java) | `findByPlayer(UUID)`, `findByName(UUID, String)` |
| [`BanRepository`](api/src/main/java/de/papercompiler/tacbdatabase/repository/BanRepository.java) | `findActiveByUuid(UUID)`, `findActiveByIp(String)` |

### 4.3 Caching Strategy

- **Slave nodes (Paper/Minestom)**: All reads/writes go to Redis only. No PostgreSQL access.
- **Master node (Velocity)**: Reads from Redis cache first, falls back to PostgreSQL, then populates cache. Writes go to Redis AND are queued for PostgreSQL flush.
- **Write-through on master**: `save`/`update` writes to Redis immediately, and queues for PostgreSQL sync.
- **Periodic flush**: Velocity flushes dirty Redis data to PostgreSQL on a configurable interval (e.g., every 5 minutes).
- **TTL-based expiration**: Configurable per entity type (e.g., Player: 5 min, Guild: 1 hour).
- **Cache invalidation**: Explicit eviction on mutations; pub/sub invalidation across cluster nodes.

---

## 5. Redis Layer: `de.papercompiler.tacbdatabase.redis`

### 5.1 Cache Manager

```java
public interface CacheManager {
    <T> CompletableFuture<Optional<T>> get(String key, Class<T> type);
    <T> CompletableFuture<Void> put(String key, T value, Duration ttl);
    <T> CompletableFuture<Void> put(String key, T value);
    CompletableFuture<Void> evict(String key);
    CompletableFuture<Void> evictPattern(String pattern);
    CompletableFuture<Void> flushAll();
}
```

### 5.2 Pub/Sub Manager

```java
public interface PubSubManager {
    CompletableFuture<Void> subscribe(String channel, MessageHandler handler);
    CompletableFuture<Void> unsubscribe(String channel);
    CompletableFuture<Void> publish(String channel, Object message);
    CompletableFuture<Void> publish(String channel, byte[] data);
}
```

### 5.3 Redis Connection

- Uses **Lettuce** `RedisClient` with async/reactive API.
- Connection pooling via Lettuce's built-in `GenericObjectPool`.
- Separate connections for commands and pub/sub (Lettuce requirement).

---

## 6. Packet System: `de.papercompiler.tacbdatabase.packet`

Cross-platform real-time communication via Redis pub/sub.

### 6.1 Packet Interface

```java
public interface Packet {
    String getChannel();
    byte[] serialize();
    static <T extends Packet> T deserialize(byte[] data, Class<T> type);
}
```

### 6.2 Packet Manager

```java
public interface PacketManager {
    <T extends Packet> CompletableFuture<Void> send(T packet);
    <T extends Packet> void registerHandler(Class<T> type, PacketHandler<T> handler);
    <T extends Packet> void unregisterHandler(Class<T> type);
}
```

### 6.3 Built-in Packets

| Packet | Channel | Purpose |
|--------|---------|---------|
| [`PlayerSyncPacket`](api/src/main/java/de/papercompiler/tacbdatabase/packet/PlayerSyncPacket.java) | `tacb:player:sync` | Sync player data (name, server, playtime) across servers. |
| [`ServerStatusPacket`](api/src/main/java/de/papercompiler/tacbdatabase/packet/ServerStatusPacket.java) | `tacb:server:status` | Broadcast server status (online players, tps, memory) for external services. |
| [`CustomEventPacket`](api/src/main/java/de/papercompiler/tacbdatabase/packet/CustomEventPacket.java) | `tacb:event:*` | Generic custom event bus. Channel is configurable per event type. |

### 6.4 Packet Flow

```
Sender → PacketManager.send() → Redis Pub/Sub → Receiver PacketManager → Handler
```

- Packets are serialized with **Jackson** (JSON or MessagePack).
- Handlers are registered per packet type.
- Packet channels follow pattern: `tacb:<category>:<action>`.

---

## 7. Unified Bootstrap: `TACBDatabase`

### 7.1 Bootstrap Flow

```java
public final class TACBDatabase {
    public static TACBDatabase bootstrap(Platform platform, TACBConfig config) {
        // 1. Auto-detect platform type
        PlatformType type = PlatformType.fromPlatform(platform);
        
        // 2. Initialize Redis (Lettuce) — required on ALL nodes
        RedisClient redisClient = RedisClient.create(config.getRedis().toURI());
        CacheManager cacheManager = new LettuceCacheManager(redisClient, config.getRedis());
        PubSubManager pubSubManager = new LettucePubSubManager(redisClient, config.getRedis());
        
        // 3. Initialize repositories
        //    - Master (Velocity): PostgreSQL-backed with Redis cache
        //    - Slave (Paper/Minestom): Redis-only (no PostgreSQL)
        Map<Class<?>, Repository<?, ?>> repositories = RepositoryFactory.create(
            type, config, cacheManager, pubSubManager
        );
        
        // 4. Initialize packet manager
        PacketManager packets = new LettucePacketManager(pubSubManager);
        
        // 5. Initialize sync scheduler (master only)
        SyncScheduler syncScheduler = null;
        if (type == PlatformType.VELOCITY) {
            syncScheduler = new SyncScheduler(repositories, config.getSyncInterval());
        }
        
        // 6. Return TACBDatabase instance
        return new TACBDatabase(platform, type, cacheManager, pubSubManager, packets, repositories, syncScheduler);
    }
}
```

### 7.2 Public API

```java
public final class TACBDatabase {
    private final Platform platform;
    private final PlatformType platformType;
    private final CacheManager cacheManager;
    private final PubSubManager pubSubManager;
    private final PacketManager packetManager;
    private final Map<Class<?>, Repository<?, ?>> repositories;
    private final SyncScheduler syncScheduler; // null on slave nodes
    
    // Getters for all components
    public Platform getPlatform() { ... }
    public PlatformType getPlatformType() { ... }
    public boolean isMaster() { return platformType == PlatformType.VELOCITY; }
    public CacheManager getCacheManager() { ... }
    public PubSubManager getPubSubManager() { ... }
    public PacketManager getPacketManager() { ... }
    
    // Repository access
    public <T, ID> Repository<T, ID> getRepository(Class<T> entityClass) { ... }
    public PlayerRepository getPlayerRepository() { ... }
    public GuildRepository getGuildRepository() { ... }
    public EconomyRepository getEconomyRepository() { ... }
    public HomeRepository getHomeRepository() { ... }
    public BanRepository getBanRepository() { ... }
    
    // Sync control (master only)
    public CompletableFuture<Void> flushToDatabase() { ... }
    
    // Lifecycle
    public void shutdown() { ... }
}
```

---

## 8. Platform Implementations

### 8.1 Paper (`paper` module)

| Class | Purpose |
|-------|---------|
| [`PaperPlatform`](paper/src/main/java/de/papercompiler/tacbdatabase/paper/PaperPlatform.java) | Implements `Platform`. Wraps `Server`. |
| [`PaperBootstrap`](paper/src/main/java/de/papercompiler/tacbdatabase/paper/PaperBootstrap.java) | JavaPlugin `onEnable()` calls `TACBDatabase.bootstrap(this, config)`. |

### 8.2 Velocity (`velocity` module)

| Class | Purpose |
|-------|---------|
| [`VelocityPlatform`](velocity/src/main/java/de/papercompiler/tacbdatabase/velocity/VelocityPlatform.java) | Implements `Platform`. Wraps `ProxyServer`. |
| [`VelocityBootstrap`](velocity/src/main/java/de/papercompiler/tacbdatabase/velocity/VelocityBootstrap.java) | Velocity `ProxyPlugin` `onInit()`/`onShutdown()` integration. |

### 8.3 Minestom (`minestom` module)

| Class | Purpose |
|-------|---------|
| [`MinestomPlatform`](minestom/src/main/java/de/papercompiler/tacbdatabase/minestom/MinestomPlatform.java) | Implements `Platform`. Wraps `MinecraftServer`. |
| [`MinestomBootstrap`](minestom/src/main/java/de/papercompiler/tacbdatabase/minestom/MinestomBootstrap.java) | Minestom `EventNode` or `Server` listener integration. |

---

## 9. Build Configuration

### 9.1 `api/build.gradle.kts` Dependencies

```kotlin
dependencies {
    // ORMLite
    implementation("com.j256.ormlite:ormlite-jdbc:6.1")
    implementation("com.j256.ormlite:ormlite-core:6.1")
    
    // PostgreSQL
    implementation("org.postgresql:postgresql:42.7.3")
    
    // HikariCP (connection pooling)
    implementation("com.zaxxer:HikariCP:5.1.0")
    
    // Lettuce (Redis)
    implementation("io.lettuce:lettuce-core:6.3.2")
    
    // Jackson (serialization)
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")
    
    // SLF4J (logging)
    implementation("org.slf4j:slf4j-api:2.0.16")
    
    // Test
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
```

### 9.2 Platform Module Dependencies

```kotlin
// paper/build.gradle.kts
dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    implementation(project(":api"))
}

// velocity/build.gradle.kts
dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    implementation(project(":api"))
}

// minestom/build.gradle.kts
dependencies {
    compileOnly("net.minestom:minestom-snapshots:1.21.1-SNAPSHOT")
    implementation(project(":api"))
}
```

---

## 10. Usage Example

```java
// Paper plugin onEnable()
@Override
public void onEnable() {
    TACBConfig config = TACBConfig.load(this.getDataFolder().toPath().resolve("config.yml"));
    TACBDatabase database = TACBDatabase.bootstrap(new PaperPlatform(this), config);
    
    // Repository usage
    Player player = database.getPlayerRepository()
        .findByUuid(player.getUniqueId())
        .join()
        .orElseGet(() -> new Player(player.getUniqueId(), player.getName()));
    
    // Packet usage
    database.getPacketManager().registerHandler(PlayerSyncPacket.class, packet -> {
        getLogger().info("Player synced: " + packet.getPlayerName());
    });
    
    // Send packet
    PlayerSyncPacket packet = new PlayerSyncPacket(player.getUuid(), player.getName(), "lobby");
    database.getPacketManager().send(packet);
}
```

---

## 11. Key Design Principles

1. **Platform-agnostic core**: `api` module has zero platform dependencies.
2. **Async-first**: All I/O operations return `CompletableFuture` to avoid blocking platform threads.
3. **Proxy-as-master architecture**: Velocity proxy is the only node with PostgreSQL access. Paper/Minestom servers use Redis only.
4. **Periodic sync**: Master node flushes dirty Redis data to PostgreSQL on a configurable interval.
5. **Pub/sub for real-time**: Packet system uses Redis pub/sub for cross-server communication.
6. **Extensible**: Users can register custom packets, custom repositories, and custom cache strategies.
7. **Connection management**: API manages Lettuce pools internally; PostgreSQL is managed only on the master node.

---

## 12. SyncScheduler (Master Node)

The `SyncScheduler` runs on the Velocity proxy and periodically flushes dirty Redis data to PostgreSQL:

```java
public class SyncScheduler {
    private final Map<Class<?>, Repository<?, ?>> repositories;
    private final Duration interval;
    private final Platform platform;
    
    public SyncScheduler(Map<Class<?>, Repository<?, ?>> repositories, Duration interval, Platform platform) {
        this.repositories = repositories;
        this.interval = interval;
        this.platform = platform;
    }
    
    public void start() {
        platform.getScheduler().asyncRepeating(this::flush, interval);
    }
    
    private CompletableFuture<Void> flush() {
        // For each repository, scan Redis for dirty keys and persist to PostgreSQL
        // Uses a "dirty" flag or timestamp to identify changed records
    }
}
```

### Dirty Tracking Strategy

- Each cached entity in Redis has a `dirty` flag.
- On slave nodes, `dirty` is set to `true` on any write.
- On master flush, only dirty entities are written to PostgreSQL, then `dirty` is cleared.
- This minimizes PostgreSQL write load.

---

## 13. Open Questions for Implementation

1. Should we support **MessagePack** as an alternative to JSON for packet serialization (smaller payloads)?
2. Should repositories support **transactions** (batch operations)?
3. Should we provide a **migration system** (like Flyway) for schema changes?
4. Should `Entity` use `@DatabaseField(generatedId = true)` or UUID-based IDs?
5. Should the packet system support **request-response** patterns (correlation IDs)?
