plugins {
    id("java")
    id("java-library")
    id("maven-publish")
}

group = "de.papercompiler.tacbdatabase"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// Collect all JARs from subprojects into the root build/libs directory
val collectJars by tasks.registering(Copy::class) {
    group = "build"
    description = "Collects all JARs from subprojects into the root build/libs directory"
    
    // Only depend on and collect from projects that have successfully built
    subprojects.forEach { subproject ->
        if (subproject.plugins.hasPlugin("java")) {
            dependsOn(subproject.tasks.withType<Jar>())
        }
    }
    
    from(subprojects.filter { it.plugins.hasPlugin("java") }.map { 
        it.tasks.withType<Jar>().map { it.archiveFile.get().asFile } 
    })
    into(layout.buildDirectory.dir("libs"))
}

// Make build depend on collectJars
tasks.named("build") {
    dependsOn(collectJars)
}
