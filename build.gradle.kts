import net.fabricmc.loom.task.RemapJarTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.3.71"
    idea
    id("fabric-loom") version "0.4-SNAPSHOT"
    `maven-publish`
    id("org.jmailen.kotlinter") version "2.4.0"
}

group = "io.github.juuxel"

base {
    archivesBaseName = "Adorn"
}

val minecraft: String by ext
val modVersion = ext["mod-version"] ?: error("Version was null")
val localBuild = ext["local-build"].toString().toBoolean()
version = "$modVersion+$minecraft"
val heavyweight = ext["heavyweight-run"].toString().toBoolean()

if (localBuild) {
    println("Note: local build mode enabled in gradle.properties; all dependencies might not work!")
}

repositories {
    mavenCentral()
    if (localBuild) {
        mavenLocal()
    }

    maven(url = "http://server.bbkr.space:8081/artifactory/libs-release") { name = "Cotton" }
    maven(url = "http://server.bbkr.space:8081/artifactory/libs-snapshot") { name = "Cotton (snapshots)" }
    maven(url = "https://minecraft.curseforge.com/api/maven") { name = "CurseForge" }
    maven(url = "https://jitpack.io") {
        content {
            includeGroup("com.github.Virtuoel")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.getByName<ProcessResources>("processResources") {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand(
            mutableMapOf(
                "version" to project.version
            )
        )
    }
}

minecraft {
}

fun DependencyHandler.includedMod(str: String, block: ExternalModuleDependency.() -> Unit = {}) {
    modImplementation(str, block)
    include(str, block)
}

fun DependencyHandler.includedMod(group: String, name: String, version: String, block: ExternalModuleDependency.() -> Unit = {}) {
    modImplementation(group, name, version, dependencyConfiguration = block)
    include(group, name, version, dependencyConfiguration = block)
}

dependencies {
    /**
     * Gets a version string with the [key].
     */
    fun v(key: String) = project.properties[key].toString()

    fun ExternalModuleDependency.excludes() {
        exclude(group = "net.fabricmc.fabric-api")
        exclude(group = "curse.maven")
    }

    minecraft("com.mojang:minecraft:$minecraft")
    mappings("net.fabricmc:yarn:" + v("minecraft") + '+' + v("mappings") + ":v2")

    // Fabric
    modImplementation("net.fabricmc:fabric-loader:" + v("fabric-loader"))
    modImplementation("net.fabricmc.fabric-api:fabric-api:" + v("fabric-api"))
    modApi("net.fabricmc:fabric-language-kotlin:" + v("fabric-kotlin"))
    compileOnly("net.fabricmc:fabric-language-kotlin:" + v("fabric-kotlin"))

    // Other mods
    includedMod("io.github.cottonmc:LibGui:" + v("libgui")) { isTransitive = false }
    includedMod("io.github.cottonmc:Jankson-Fabric:" + v("jankson")) { excludes() }
    includedMod("io.github.cottonmc", "LibCD", v("libcd")) { excludes(); exclude(module = "Jankson"); exclude(module = "nbt-crafting") }
    modCompileOnly("com.github.Virtuoel:Towelette:" + v("towelette")) { excludes() }
    modCompileOnly("io.github.prospector:modmenu:" + v("modmenu")) { excludes() }
    modRuntime("io.github.prospector:modmenu:" + v("modmenu")) { excludes() }
//    modCompileOnly("extra-pieces:extrapieces:" + v("extra-pieces"))
//    modCompileOnly("com.github.artificemc:artifice:" + v("artifice"))

    if (heavyweight) {
        // FIXME: Biome mods
//        modRuntime("com.terraformersmc", "traverse", v("traverse")) { excludes() }
//        modRuntime("com.terraformersmc", "terrestria", v("terrestria")) { excludes() }
        modRuntime("me.shedaniel", "RoughlyEnoughItems", v("rei")) { exclude(module = "jankson"); excludes() }
        modRuntime("com.github.Virtuoel:Towelette:" + v("towelette")) { excludes() }
//        modRuntime("extra-pieces:extrapieces:" + v("extra-pieces"))
//        modRuntime("com.github.artificemc:artifice:" + v("artifice"))
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.Experimental"
}

val remapJar = tasks.getByName<RemapJarTask>("remapJar")

kotlinter {
    disabledRules = arrayOf("parameter-list-wrapping")
}

publishing {
    publications.create<MavenPublication>("maven") {
        artifactId = "adorn"

        artifact(remapJar) {
            classifier = null
            builtBy(remapJar)
        }
    }
}
