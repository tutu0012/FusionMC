plugins {
    kotlin("jvm") version "1.9.25"
    id("fabric-loom") version "1.8.12"
}

group = "com.fusionmc"
version = "0.1.1d-beta+mc1.20.1"

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
    maven("https://repo.spongepowered.org/maven") // Required for mixin
}

dependencies {
    minecraft("com.mojang:minecraft:1.20.1")
    mappings("net.fabricmc:yarn:1.20.1+build.10")
    modImplementation("net.fabricmc:fabric-loader:0.14.21")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.83.0+1.20.1")
    implementation("org.ow2.asm:asm-tree:9.5")
    implementation(kotlin("stdlib"))


}

loom {
    accessWidenerPath.set(file("src/main/resources/fusionmc.accesswidener"))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

kotlin {
    jvmToolchain(17)
}