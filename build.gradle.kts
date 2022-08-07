fun properties(key: String) = project.findProperty(key).toString()

// https://kotlinlang.org/docs/reference/using-gradle.html
object Versions {
    const val spring = "5.3.3"
}

plugins {
    idea
    jacoco // https://docs.gradle.org/current/userguide/jacoco_plugin.html
    id("org.jetbrains.intellij") version "1.8.0" // https://github.com/JetBrains/gradle-intellij-plugin
    kotlin("jvm") version "1.7.10"
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
    pluginName.set(properties("pluginName"))
    version.set(properties("platformVersion"))
    type.set(properties("platformType"))

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    plugins.set(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
}

group = properties("pluginGroup")
version = properties("pluginVersion")

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
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
        gradleVersion = properties("gradleVersion")
    }

    patchPluginXml {
        version.set(properties("pluginVersion"))
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))
    }

    compileKotlin {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_11.majorVersion
    }

    compileTestKotlin {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_11.majorVersion
    }

    jacocoTestReport {
        reports {
            xml.required.set(true)
        }
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token.set(System.getenv("PUBLISH_TOKEN"))
        // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels.set(listOf(properties("pluginVersion").split('-').getOrElse(1) { "default" }.split('.').first()))
    }
}
