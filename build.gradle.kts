import org.jetbrains.intellij.tasks.PublishTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// https://kotlinlang.org/docs/reference/using-gradle.html

plugins {
    idea
    id("org.jetbrains.intellij") version "0.4.18" // https://github.com/JetBrains/gradle-intellij-plugin
    kotlin("jvm") version "1.3.31"
    id("org.sonarqube") version "2.8"
}

sonarqube {
    properties {
        property("sonar.projectKey", "masooh_groovyfier")
        property("sonar.organization", "masooh")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.login", "a33bf2e7694238965372c69a490d0b5c7ce56b58")
    }
}

intellij {
    pluginName = "Groovyfier"
//    version = "191.6183.87" // overrides plugin.xml since-build in case of conflict, https://www.jetbrains.com/intellij-repository/releases
    version = "2019.3.3" // overrides plugin.xml since-build in case of conflict, https://www.jetbrains.com/intellij-repository/releases
    setPlugins("Groovy", "java") // Bundled plugin dependencies
    updateSinceUntilBuild = false
}

tasks.withType<PublishTask> {
    if (project.hasProperty("intellijPublishToken")) {
        token(project.property("intellijPublishToken"))
    }
}

group = "com.github.masooh.intellij.plugin.junitspock"
version = "0.1.1" // overrides plugin.xml version in case of conflict

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("stdlib-jdk7"))
    implementation(kotlin("stdlib-jdk8"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}