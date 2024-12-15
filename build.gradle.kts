fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

// https://kotlinlang.org/docs/reference/using-gradle.html
object Versions {
    const val spring = "5.3.3"
}

plugins {
    idea
    jacoco // https://docs.gradle.org/current/userguide/jacoco_plugin.html
    id("org.jetbrains.intellij") version "1.17.4" // https://github.com/JetBrains/gradle-intellij-plugin
    // id("org.jetbrains.intellij.platform") version "2.2.0"
    kotlin("jvm") version "1.9.25"
    id("org.sonarqube") version "3.4.0.2513"
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
    pluginName = properties("pluginName")
    version = properties("platformVersion")
    type = properties("platformType")

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    plugins = properties("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) }
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

repositories {
    mavenCentral()
}

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(17)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

dependencies {
    /*
       the following test dependencies break test execution
       but are helpful for code completion and syntax check in src/test/resources/testdata
       Test classpath is defined by Test#getProjectDescriptor()
     */
//    testImplementation("org.springframework:spring-context:${Versions.spring}")
//    testImplementation("org.springframework:spring-test:${Versions.spring}")
//    testImplementation("org.spockframework:spock-core:1.3-groovy-2.5")
//    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
//    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.0")
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    patchPluginXml {
        version = properties("pluginVersion")
        sinceBuild = properties("pluginSinceBuild")
        untilBuild = properties("pluginUntilBuild")
    }

    compileKotlin {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_17.majorVersion
    }

    compileTestKotlin {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_17.majorVersion
    }

    jacocoTestReport {
        reports {
            xml.required.set(true)
        }
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token = environment("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = properties("pluginVersion").map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
    }
}
