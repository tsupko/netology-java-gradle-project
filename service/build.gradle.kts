plugins {
    id("java")
}

group = "edu.tsupko"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":db"))
}

tasks.test {
    useJUnitPlatform()
}