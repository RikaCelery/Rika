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
}
dependencies {
    compileOnly("net.mamoe:mirai-core-utils:2.12.2")
    implementation("xyz.cssxsh.pixiv:pixiv-client:1.2.5")
    implementation("org.seleniumhq.selenium:selenium-java:4.4.0")
    implementation("org.seleniumhq.selenium:selenium-chrome-driver:4.4.0")
    implementation("commons-codec:commons-codec:1.15")
    implementation("org.apache.commons:commons-lang3:3.12.0")
}
mirai {
    jvmTarget = JavaVersion.VERSION_11

}