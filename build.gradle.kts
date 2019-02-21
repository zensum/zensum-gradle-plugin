// This is the build configuration for the plugin.  For the Gradle
// stuff that gets applied to the plugin's consumers, see
// `project.gradle.kts`.

import java.io.StringReader
import java.util.Properties
import groovy.text.SimpleTemplateEngine

import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven`
    `maven-publish`
}

group = "se.zensum.gradle"
version = "1.0"
description = "Common Gradle configuration for Zensum projects"

repositories {
    jcenter()
    mavenCentral()
    gradlePluginPortal()
}

publishing {
    repositories {
        mavenLocal()
    }
}

val versions = readProperties("versions.properties")
val plugins = readProperties("plugins.properties", vars = versions)

val gradleVersion = versions.getProperty("gradle")
val kotlinVersion = versions.getProperty("kotlin")
val kotlinApiVersion = versions.getProperty("kotlin_api")
val jvmVersion = versions.getProperty("jvm")

tasks {
    withType<Wrapper> {
        gradleVersion = gradleVersion
    }

    withType<KotlinCompile> {
        kotlinOptions.languageVersion = kotlinVersion
        kotlinOptions.apiVersion = kotlinApiVersion
        kotlinOptions.jvmTarget = jvmVersion
        kotlinOptions.javaParameters = true
    }
}

dependencies {
    for ((_, v) in plugins) {
        // Give our plugin access to external plugin classes, so we
        // can configure them.
        add("implementation", v)
    }

    add("implementation", "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    add("implementation", "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
}

fun readProperties(
    path: String,
    vars: Properties = Properties()
): Properties {
    fun read(path: String) =
        File(path).readText(Charsets.UTF_8)
    fun expand(text: String, vars: Properties) =
        SimpleTemplateEngine().createTemplate(text).make(vars).toString()
    fun parse(text: String): Properties =
        Properties().apply { load(StringReader(text)) }

    return parse(expand(read(path), vars))
}
