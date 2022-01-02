val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val skrape_version: String by project
val kotlin_csv_version: String by project

plugins {
    application
    kotlin("jvm") version "1.6.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.6.10"
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "lungo.biz"
version = "0.3.0"
application {
    mainClass.set("biz.lungo.currencybot.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-gson:$ktor_version")
    implementation("io.ktor:ktor-serialization:$ktor_version")
    implementation("io.ktor:ktor-server-host-common:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("it.skrape:skrapeit:$skrape_version")
    implementation("io.ktor:ktor-client-apache:$ktor_version")
    implementation("io.ktor:ktor-client-serialization:$ktor_version")
    implementation("io.ktor:ktor-client-gson:$ktor_version")
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:$kotlin_csv_version")
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