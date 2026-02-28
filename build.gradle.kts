@file:Suppress("ktPropBy")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktorVersion: String by project
val kotlinVersion: String by project
val logbackVersion: String by project
val skrapeVersion: String by project
val kotlinCsvVersion: String by project
val kMongoVersion: String by project

plugins {
    application
    kotlin("jvm") version "1.9.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.20-RC"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "biz.lungo"
version = "0.6.1"
application {
    mainClass.set("biz.lungo.currencybot.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("io.ktor:ktor-server-host-common:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-gson:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-gson:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("it.skrape:skrapeit:$skrapeVersion")
    implementation("org.litote.kmongo:kmongo:$kMongoVersion")
    implementation("org.litote.kmongo:kmongo-coroutine:$kMongoVersion")
    implementation(kotlin("stdlib-jdk8"))
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.RequiresOptIn")
}

tasks.register<Copy>("copyRelease") {
    from(layout.buildDirectory.dir("libs"))
    include("*.jar")
    rename("(.+)currency-bot(.+)", "currency-bot.jar")
    into(layout.projectDirectory.dir("release"))
}

tasks.register<Delete>("removePrevious") {
    delete(fileTree("build/libs").matching {
        include("*.jar")
    })
}

tasks {
    shadowJar {
        dependsOn(named<Delete>("removePrevious"))
        manifest {
            attributes(Pair("Main-Class", "biz.lungo.currencybot.ApplicationKt"))
        }
    }
    named<Copy>("copyRelease") {
        dependsOn(shadowJar)
    }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "21"
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "21"
}

kotlin {
    jvmToolchain(21)
}