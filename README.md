# TACBDatabase

A multi-platform Minecraft database library providing unified data access across Paper, Velocity, and Minestom servers using **ORMLite + PostgreSQL** for persistent storage and **Lettuce (Redis)** for caching, pub/sub, and real-time packet communication.

## Overview

TACBDatabase follows a **Proxy-as-Master** architecture:

- **Master node (Velocity)**: Has full PostgreSQL access. Periodically flushes Redis cache to PostgreSQL.
- **Slave nodes (Paper, Minestom)**: Redis-only. No direct PostgreSQL access. All data operations go through Redis.

This architecture provides:
- Low-latency reads/writes on game servers (Redis)
- Centralized data persistence (PostgreSQL on proxy)
- Real-time cross-server communication (Redis pub/sub)

## Features

- **Multi-platform support**: Paper, Velocity, and Minestom
- **Entity models**: Player, Guild, Economy, Home, Ban
- **Repository pattern**: Async database operations with CompletableFuture
- **Redis caching**: Automatic caching with configurable TTL
- **Cross-server packets**: Real-time communication via Redis pub/sub
- **Dirty tracking**: Automatic sync of modified entities to PostgreSQL

## Module Structure

```
TACBDatabase/
├── api/                    # Core platform-agnostic API (no platform dependencies)
├── paper/                  # Paper (Bukkit/Spigot) platform implementation
├── velocity/               # Velocity proxy platform implementation
├── minestom/               # Minestom server platform implementation
├── build.gradle.kts        # Root build config
└── settings.gradle.kts     # Module includes
```

## Requirements

- Java 21+
- PostgreSQL 12+ (for master node)
- Redis 6+ (for all nodes)

## Quick Start

### Maven

```xml
<dependencies>
    <!-- For Paper plugins -->
    <dependency>
        <groupId>de.papercompiler</groupId>
        <artifactId>paper</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
    
    <!-- For Velocity plugins -->
    <dependency>
        <groupId>de.papercompiler</groupId>
        <artifactId>velocity</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
    
    <!-- For Minestom applications -->
    <dependency>
        <groupId>de.papercompiler</groupId>
        <artifactId>minestom</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

### Gradle

```kotlin
dependencies {
    // For Paper plugins
    implementation("de.papercompiler:paper:1.0-SNAPSHOT")
    
    // For Velocity plugins
    implementation("de.papercompiler:velocity:1.0-SNAPSHOT")
    
    // For Minestom applications
    implementation("de.papercompiler:minestom:1.0-SNAPSHOT")
}
```

## Configuration

### config.properties

```properties
# Redis configuration (required for all nodes)
redis.host=localhost
redis.port=6379
redis.password=
redis.database=0

# Database configuration (only for master/Velocity node)
database.url=jdbc:postgresql://localhost:5432/tacbdatabase
database.user=tacb
database.password=secret
database.pool-size=10

# Sync interval (master node only, in seconds)
sync.interval-seconds=300
```

## Usage

See [API.md](API.md) for detailed usage instructions for each platform.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Velocity Proxy (Master)                   │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐   │
│  │ PostgreSQL  │◄──►│   Redis     │◄──►│   Redis     │   │
│  │  (Primary)  │    │  (Cache)    │    │  (Pub/Sub)  │   │
│  └─────────────┘    └─────────────┘    └─────────────┘   │
└─────────────────────────────────────────────────────────────┘
                           │
           ┌───────────────┼───────────────┐
           ▼               ▼               ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│   Paper Server  │ │  Minestom Srv   │ │   Paper Srv 2   │
│   (Slave Node)  │ │   (Slave Node)  │ │   (Slave Node)  │
│                 │ │                 │ │                 │
│  Redis (Cache)  │ │  Redis (Cache)  │ │  Redis (Cache)  │
└─────────────────┘ └─────────────────┘ └─────────────────┘
```

## License

This project is licensed under the MIT License.