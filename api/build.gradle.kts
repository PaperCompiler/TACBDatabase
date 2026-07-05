plugins {
    `java-library`
}

group = "de.papercompiler"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // ORMLite core + JDBC
    api("com.j256.ormlite:ormlite-jdbc:6.1")
    api("com.j256.ormlite:ormlite-core:6.1")

    // PostgreSQL driver
    api("org.postgresql:postgresql:42.7.3")

    // HikariCP connection pooling
    api("com.zaxxer:HikariCP:5.1.0")

    // Lettuce Redis client
    api("io.lettuce:lettuce-core:7.1.0.RELEASE")

    // Jackson for JSON serialization
    api("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")

    // SLF4J logging
    api("org.slf4j:slf4j-api:2.0.16")

    // Test dependencies
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
