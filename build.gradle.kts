plugins {
    kotlin("jvm") version "2.0.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("net.dv8tion:JDA:5.2.1")
    implementation("ch.qos.logback:logback-classic:1.4.14")
}

application {
    mainClass.set("MainKt")
}

tasks.shadowJar {
    manifest {
        attributes(
            "Main-Class" to "MainKt"
        )
    }
    archiveBaseName.set("discord-bot")
    archiveClassifier.set("")
    archiveVersion.set("")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}