# TACBDatabase API Documentation

This document provides detailed usage instructions for integrating TACBDatabase into your Minecraft projects.

## Table of Contents

- [Core API](#core-api)
- [Paper Integration](#paper-integration)
- [Velocity Integration](#velocity-integration)
- [Minestom Integration](#minestom-integration)
- [Entity Models](#entity-models)
- [Repository Usage](#repository-usage)
- [Packet System](#packet-system)
- [Cross-Plugin Access](#cross-plugin-access)

## Core API

### Bootstrap

The main entry point is [`TACBDatabase`](api/src/main/java/de/papercompiler/tacbdatabase/TACBDatabase.java):

```java
TACBDatabase database = TACBDatabase.bootstrap(platform, config);
```

### Configuration

```java
TACBConfig config = TACBConfig.builder()
    .redis(RedisConfig.of("localhost", 6379, "password", 0, 10, 5))
    .database(DatabaseConfig.of("localhost", 5432, "tacb", "user", "pass", 10))
    .syncInterval(Duration.ofMinutes(5))
    .build();
```

### Platform Detection

The platform type is automatically detected based on the [`Platform`](api/src/main/java/de/papercompiler/tacbdatabase/platform/Platform.java) implementation:

- `PlatformType.PAPER` - Paper/Spigot/Bukkit server
- `PlatformType.VELOCITY` - Velocity proxy
- `PlatformType.MINESTOM` - Minestom server

## Paper Integration

### Setup

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("de.papercompiler:paper:1.0-SNAPSHOT")
}
```

### Plugin Bootstrap

```java
public class MyPlugin extends JavaPlugin {
    private TACBDatabase database;
    
    @Override
    public void onEnable() {
        // Method 1: Using PaperBootstrap helper
        PaperBootstrap bootstrap = new PaperBootstrap(this);
        database = bootstrap.bootstrap("config.properties");
        
        // Method 2: Manual bootstrap
        TACBConfig config = TACBConfig.builder()
            .redis(RedisConfig.of("localhost", 6379))
            .build();
        database = TACBDatabase.bootstrap(new PaperPlatform(this), config);
    }
    
    @Override
    public void onDisable() {
        if (database != null) {
            database.shutdown();
        }
    }
    
    public TACBDatabase getDatabase() {
        return database;
    }
}
```

### Using Repositories

```java
// Get a player by UUID
UUID playerId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
database.getPlayerRepository().findByUuid(playerId)
    .thenAccept(optional -> {
        if (optional.isPresent()) {
            Player player = optional.get();
            getLogger().info("Found player: " + player.getName());
        }
    });

// Create a new player
Player newPlayer = new Player(playerId, "PlayerName");
database.getPlayerRepository().save(newPlayer)
    .thenAccept(savedPlayer -> {
        getLogger().info("Saved player with ID: " + savedPlayer.getId());
    });

// Update player data
player.setLastServer("lobby-1");
database.getPlayerRepository().update(player);
```

### Using Packets

```java
// Send a player sync packet
PlayerSyncPacket packet = new PlayerSyncPacket(
    playerId, 
    "PlayerName", 
    "lobby-1", 
    12345L
);
database.getPacketManager().send(packet);

// Register a packet handler
database.getPacketManager().registerHandler(PlayerSyncPacket.class, packet -> {
    getLogger().info("Received player sync: " + packet.getName() + " on " + packet.getServer());
});
```

## Velocity Integration

### Setup

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("de.papercompiler:velocity:1.0-SNAPSHOT")
}
```

### Plugin Bootstrap

```java
@Plugin(id = "myplugin", name = "MyPlugin", version = "1.0")
public class MyVelocityPlugin {
    private TACBDatabase database;
    
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        TACBConfig config = TACBConfig.builder()
            .database(DatabaseConfig.of("localhost", 5432, "tacb", "user", "pass", 10))
            .redis(RedisConfig.of("localhost", 6379))
            .build();
        database = TACBDatabase.bootstrap(new VelocityPlatform(getServer()), config);
    }
    
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (database != null) {
            database.shutdown();
        }
    }
}
```

### Master Node Operations

On Velocity (master node), you have access to PostgreSQL:

```java
// Manual flush to database
database.flushToDatabase().thenRun(() -> {
    getLogger().info("Database sync completed");
});

// Check if this is the master node
if (database.isMaster()) {
    // Perform operations that require database access
}
```

## Minestom Integration

### Setup

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("de.papercompiler:minestom:1.0-SNAPSHOT")
}
```

### Application Bootstrap

```java
public class MyMinestomApp {
    private TACBDatabase database;
    
    public void init() {
        MinecraftServer server = MinecraftServer.init();
        
        TACBConfig config = TACBConfig.builder()
            .redis(RedisConfig.of("localhost", 6379))
            .build();
        database = TACBDatabase.bootstrap(new MinestomPlatform(server), config);
        
        server.start();
    }
    
    public void stop() {
        if (database != null) {
            database.shutdown();
        }
    }
}
```

## Entity Models

### Player

```java
public class Player implements Entity {
    private Long id;
    private UUID uuid;
    private String name;
    private String lastServer;
    private Instant firstJoin;
    private Instant lastJoin;
    private long playtimeTicks;
    private Instant createdAt;
    private Instant updatedAt;
}
```

### Guild

```java
public class Guild implements Entity {
    private Long id;
    private String name;
    private String tag;
    private UUID ownerUuid;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}
```

### Economy

```java
public class Economy implements Entity {
    private Long id;
    private UUID uuid;
    private BigDecimal balance;
    private String currency;
    private Instant createdAt;
    private Instant updatedAt;
}
```

### Home

```java
public class Home implements Entity {
    private Long id;
    private UUID uuid;
    private String name;
    private String world;
    private double x, y, z;
    private float yaw, pitch;
    private Instant createdAt;
    private Instant updatedAt;
}
```

### Ban

```java
public class Ban implements Entity {
    private Long id;
    private UUID targetUuid;
    private String targetIp;
    private UUID sourceUuid;
    private String reason;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;
}
```

## Repository Usage

### Base Repository Methods

All repositories extend [`Repository<T, ID>`](api/src/main/java/de/papercompiler/tacbdatabase/repository/Repository.java) and provide:

```java
// Find by ID
CompletableFuture<Optional<T>> findById(ID id);

// Find all entities
CompletableFuture<List<T>> findAll();

// Save a new entity
CompletableFuture<T> save(T entity);

// Update an existing entity
CompletableFuture<T> update(T entity);

// Delete an entity
CompletableFuture<Void> delete(T entity);

// Delete by ID
CompletableFuture<Void> deleteById(ID id);

// Check existence
CompletableFuture<Boolean> exists(ID id);

// Count entities
CompletableFuture<Long> count();
```

### PlayerRepository

```java
// Find by UUID
Optional<Player> findByUuid(UUID uuid);

// Find by name
Optional<Player> findByName(String name);

// Find all online players
List<Player> findOnline();
```

### GuildRepository

```java
// Find by owner UUID
Optional<Guild> findByOwner(UUID ownerUuid);

// Find by tag
Optional<Guild> findByTag(String tag);
```

### EconomyRepository

```java
// Find by UUID
Optional<Economy> findByUuid(UUID uuid);

// Adjust balance atomically
Economy adjustBalance(UUID uuid, BigDecimal amount);
```

### HomeRepository

```java
// Find all homes for a player
List<Home> findByPlayer(UUID uuid);

// Find specific home by player and name
Optional<Home> findByName(UUID uuid, String name);
```

### BanRepository

```java
// Find active ban by UUID
Optional<Ban> findActiveByUuid(UUID uuid);

// Find active ban by IP
Optional<Ban> findActiveByIp(String ip);
```

## Packet System

### Built-in Packets

#### PlayerSyncPacket

Syncs player data across servers:

```java
PlayerSyncPacket packet = new PlayerSyncPacket(uuid, name, server, playtimeTicks);
database.getPacketManager().send(packet);
```

#### ServerStatusPacket

Broadcasts server status for monitoring:

```java
ServerStatusPacket packet = new ServerStatusPacket(
    "lobby-1",
    50,
    100,
    19.5,
    512,
    1024,
    Map.of("custom", "data")
);
database.getPacketManager().send(packet);
```

#### CustomEventPacket

Generic event bus for custom events:

```java
CustomEventPacket packet = new CustomEventPacket(
    "player:join",
    Map.of("uuid", uuid.toString(), "server", "lobby-1")
);
database.getPacketManager().send(packet);
```

### Custom Packets

Create your own packet by implementing [`Packet`](api/src/main/java/de/papercompiler/tacbdatabase/packet/Packet.java):

```java
public class MyCustomPacket implements Packet {
    public static final String CHANNEL = "tacb:custom:myevent";
    
    private String data;
    
    public MyCustomPacket() {}
    
    public MyCustomPacket(String data) {
        this.data = data;
    }
    
    @Override
    public String getChannel() {
        return CHANNEL;
    }
    
    @Override
    public byte[] serialize() throws IOException {
        return MAPPER.writeValueAsBytes(this);
    }
}
```

Register the handler:

```java
database.getPacketManager().registerHandler(MyCustomPacket.class, packet -> {
    // Handle the packet
    System.out.println("Received: " + packet.getData());
});
```

## Cross-Plugin Access

### Plugin Instance Container

TACBDatabase provides a [`PluginContainer`](api/src/main/java/de/papercompiler/tacbdatabase/PluginContainer.java) for cross-plugin access to database instances:

```java
// Register your database instance
PluginContainer.register("myplugin", database);

// Other plugins can access it
TACBDatabase otherDb = PluginContainer.get("myplugin");
if (otherDb != null) {
    // Use the database
}
```

### Using PluginContainer

```java
// In your plugin's onEnable
@Override
public void onEnable() {
    TACBConfig config = TACBConfig.builder()
        .redis(RedisConfig.of("localhost", 6379))
        .build();
    database = TACBDatabase.bootstrap(new PaperPlatform(this), config);
    
    // Register for cross-plugin access
    PluginContainer.register("myplugin", database);
}

// In another plugin
TACBDatabase db = PluginContainer.get("myplugin");
if (db != null) {
    db.getPlayerRepository().findByUuid(someUuid)
        .thenAccept(optional -> {
            // Use the player data
        });
}
```

## Cache Manager

Direct cache access is available for advanced use cases:

```java
// Get from cache
database.getCacheManager().get("player:123", Player.class);

// Put to cache with TTL
database.getCacheManager().put("player:123", player, Duration.ofMinutes(5));

// Evict from cache
database.getCacheManager().evict("player:123");

// Flush all cache
database.getCacheManager().flushAll();
```

## Pub/Sub Manager

Direct pub/sub access for custom messaging:

```java
// Subscribe to a channel
database.getPubSubManager().subscribe("my:channel", (channel, message) -> {
    System.out.println("Received: " + message);
});

// Publish to a channel
database.getPubSubManager().publish("my:channel", "Hello, world!");
```

## Error Handling

All repository operations return `CompletableFuture` and should be handled appropriately:

```java
database.getPlayerRepository().findByUuid(uuid)
    .thenAccept(optional -> {
        optional.ifPresentOrElse(
            player -> {
                // Success
                getLogger().info("Player found: " + player.getName());
            },
            () -> {
                // Not found
                getLogger().info("Player not found");
            }
        );
    })
    .exceptionally(throwable -> {
        // Error handling
        getLogger().severe("Database error: " + throwable.getMessage());
        return null;
    });
```

## Thread Safety

- All repository methods are thread-safe
- Cache operations are thread-safe
- Packet sending/receiving is thread-safe
- Use the provided [`Scheduler`](api/src/main/java/de/papercompiler/tacbdatabase/util/Scheduler.java) for platform-appropriate task scheduling