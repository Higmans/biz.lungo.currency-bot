val ktorVersion: String by project
val kotlinVersion: String by project
val logbackVersion: String by project
val skrapeVersion: String by project
val kotlinCsvVersion: String by project

plugins {
    application
    kotlin("jvm") version "1.6.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.6.10"
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "lungo.biz"
version = "0.5.3"
application {
    mainClass.set("biz.lungo.currencybot.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-gson:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("io.ktor:ktor-server-host-common:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("it.skrape:skrapeit:$skrapeVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-serialization:$ktorVersion")
    implementation("io.ktor:ktor-client-gson:$ktorVersion")
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:$kotlinCsvVersion")
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