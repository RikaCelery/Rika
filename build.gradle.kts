plugins {
    val kotlinVersion = "1.6.20"
    java
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.12.3"
}

group = "org.celery"
version = "0.1.0"

repositories {
    maven("https://repo.mirai.mamoe.net/snapshots")
    maven("https://maven.aliyun.com/repository/public")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    mavenCentral()
//    maven("https://repo1.maven.org/maven2/")/
}
dependencies {
    implementation(fileTree("lib"))
    implementation("org.xerial:sqlite-jdbc:3.39.3.0")
    implementation("org.postgresql:postgresql:42.5.0")
    implementation("org.jsoup:jsoup:1.15.3")
    "shadowLink"(zipTree("lib/opencv-453.jar")) // 告知 mirai-console 在打包插件时包含此依赖；无需包含版本号
    implementation("org.ktorm:ktorm-core:3.5.0")
    implementation("org.ktorm:ktorm-support-sqlite:3.5.0")
    implementation("org.ktorm:ktorm-support-mysql:3.5.0")
    implementation("org.ktorm:ktorm-support-postgresql:3.5.0")
    implementation("com.aayushatharva.brotli4j:brotli4j:1.8.0")
    compileOnly("net.mamoe:mirai-core-utils:2.12.2")
    implementation("xyz.cssxsh.pixiv:pixiv-client:1.2.6")
    implementation("org.seleniumhq.selenium:selenium-java:4.5.0")
    implementation("org.seleniumhq.selenium:selenium-chrome-driver:4.5.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.4")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.13.4")
    implementation("commons-codec:commons-codec:1.15")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.apache.commons:commons-text:1.10.0")
    implementation("org.brotli:dec:0.1.2")

}

mirai {

    jvmTarget = JavaVersion.VERSION_11

//    configureShadow{
//        include("lib")
//        include("resources/wechatqrcode")
//    }
}