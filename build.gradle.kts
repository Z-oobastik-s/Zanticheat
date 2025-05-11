plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.zoobastiks.zanticheat"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://jitpack.io")
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.22")
    implementation("net.kyori:adventure-api:4.15.0")
    implementation("net.kyori:adventure-text-minimessage:4.15.0")
    implementation("net.kyori:adventure-text-serializer-legacy:4.15.0")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.9")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.18")
}

// Создаем задачу для сборки jar с зависимостями без использования shadow
tasks.register<Jar>("fatJar") {
    archiveBaseName.set("Zanticheat")
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    from(sourceSets.main.get().output)
    
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
    
    manifest {
        attributes(mapOf(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        ))
    }
}

tasks {
    // Оставляем shadowJar для совместимости
    shadowJar {
        archiveClassifier.set("shadow")
        archiveBaseName.set("Zanticheat")
        mergeServiceFiles()
        
        // Пытаемся обойти проблему с версией Java 21
        exclude("META-INF/versions/9/**")
        
        relocate("kotlin", "org.zoobastiks.zanticheat.libs.kotlin")
        relocate("net.kyori", "org.zoobastiks.zanticheat.libs.kyori")
    }

    build {
        dependsOn("fatJar")
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "21"
    }

    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}