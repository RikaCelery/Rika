import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    val kotlinVersion = "1.6.20"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.12.2"
}

group = "org.celery"
version = "0.1.0"

repositories {
    maven("https://maven.aliyun.com/repository/public")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    mavenCentral()
//    maven("https://repo1.maven.org/maven2/")/
}
dependencies {
    val brotliVersion = "1.8.0"
    val operatingSystem: OperatingSystem = DefaultNativePlatform.getCurrentOperatingSystem()

    implementation("org.ktorm:ktorm-core:3.5.0")
    implementation("org.ktorm:ktorm-support-sqlite:3.5.0")
    implementation("org.ktorm:ktorm-support-mysql:3.5.0")
    implementation("org.ktorm:ktorm-support-postgresql:3.5.0")
    implementation("com.aayushatharva.brotli4j:brotli4j:1.8.0")
    compileOnly("net.mamoe:mirai-core-utils:2.12.2")
    implementation("xyz.cssxsh.pixiv:pixiv-client:1.2.5")
    implementation("org.seleniumhq.selenium:selenium-java:4.4.0")
    implementation("org.seleniumhq.selenium:selenium-chrome-driver:4.4.0")
    implementation("commons-codec:commons-codec:1.15")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.apache.commons:commons-text:1.9")
//    implementation("com.aayushatharva.brotli4j:brotli4j:$brotliVersion")
//    implementation(
//        "com.aayushatharva.brotli4j:native-${
//            if (operatingSystem.isWindows) "windows-x86_64"
//            else if (operatingSystem.isMacOsX)
//                if (DefaultNativePlatform.getCurrentArchitecture().isArm) "osx-aarch64"
//                else "osx-x86_64"
//            else if (operatingSystem.isLinux)
//                if (DefaultNativePlatform.getCurrentArchitecture().isArm) "linux-aarch64"
//                else "linux-x86_64"
//            else ""
//        }:$brotliVersion")
    implementation("org.brotli:dec:0.1.2")

}
mirai {
    jvmTarget = JavaVersion.VERSION_11
}