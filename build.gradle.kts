plugins {
    java
    id("com.gradleup.shadow") version "8.3.6"
}

group = "io.github.toolwarn"
version = "1.0.0"

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.66-stable")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }
    shadowJar {
        archiveClassifier.set("")
    }
    build {
        dependsOn(shadowJar)
    }
}
