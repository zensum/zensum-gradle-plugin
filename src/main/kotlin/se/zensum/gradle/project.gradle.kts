//
// This is a Kotlin DSL script which gets compiled into the binary
// Gradle plugin `se.zensum.gradle.project`.
//

package se.zensum.gradle

import java.io.StringReader
import java.util.Properties
import groovy.text.SimpleTemplateEngine

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

// Use built-in plugins.  External plugins are specified in the
// resource file `plugins.properties`.
plugins {
    java
    maven
    idea
}

repositories {
    jcenter()
    mavenCentral()
    maven("https://dl.bintray.com/kotlin/kotlinx")
    maven("https://dl.bintray.com/kotlin/ktor")
    maven("https://jitpack.io") {
      System.getenv("JITPACK_TOKEN")?.let {
        token -> credentials { username = token }
      }
    }
}

val mainClassName = "se.zensum.MainKt"
val mainSourceSet = the<JavaPluginConvention>().sourceSets["main"]
val mainClasspath = mainSourceSet.runtimeClasspath

defaultTasks("run")

task<JavaExec>("run") {
    main = mainClassName
    classpath = mainClasspath
}

task<JavaExec>("debug") {
    main = mainClassName
    classpath = mainClasspath
    debug = true
    environment["DEBUG"] = true
}

val versionProperties =
    readProperties(this, "versions.properties")
val pluginProperties =
    readProperties(this, "plugins.properties", vars = versionProperties)

for ((k, _) in pluginProperties) {
    apply(plugin = k as String)
}

fun version(x: String) = versionProperties.getProperty(x)

val gradleVersion = version("gradle")
val jvmVersion = version("jvm")

val kotlinVersion = version("kotlin")
val kotlinApiVersion = version("kotlin_api")
val kotlinCoroutinesVersion = version("kotlin_coroutines")

val grpcVersion = version("grpc")
val junitVersion = version("junit")

tasks {
    withType<Wrapper> {
        gradleVersion = gradleVersion
    }

    withType<KotlinCompile> {
        kotlinOptions.languageVersion = kotlinApiVersion
        kotlinOptions.apiVersion = kotlinApiVersion
        kotlinOptions.jvmTarget = jvmVersion
        kotlinOptions.javaParameters = true
    }

    withType<JavaCompile> {
        sourceCompatibility = jvmVersion
        targetCompatibility = jvmVersion
        options.setIncremental(true)
        options.encoding = "UTF-8"
    }

    withType<Test> {
        useJUnitPlatform()
    }

    withType<Jar> {
        manifest {
            attributes("Main-Class" to mainClassName)
        }
    }

    withType<ShadowJar> {
        // These properties are deprecated for some reason?
        baseName = "shadow"
        classifier = null
        version = null
    }

    val sourcesJar by registering(Jar::class) {
        dependsOn("classes")
        classifier = "sources"
        from(mainSourceSet.allSource)
    }

    val javadoc by existing(Javadoc::class)
    val javadocJar by registering(Jar::class) {
        dependsOn(javadoc)
        from(javadoc.get().destinationDir)
    }

    artifacts {
        add("archives", sourcesJar)
        add("archives", javadocJar)
    }
}

dependencies {
    "compile"(
        "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    "compile"(
        "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinCoroutinesVersion")
    "compile"(
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    "compile"(
        "io.github.microutils:kotlin-logging:1.6.20")

    "testCompile"(
        "org.junit.jupiter:junit-jupiter-api:$junitVersion")
    "testRuntime"(
        "org.junit.jupiter:junit-jupiter-engine:$junitVersion")

    "implementation"(
        "io.grpc:grpc-netty:${grpcVersion}")
    "implementation"(
        "io.grpc:grpc-protobuf:${grpcVersion}")
    "implementation"(
        "io.grpc:grpc-stub:${grpcVersion}")

    "implementation"(
        "com.github.zensum:profile-proto:e421a410a0")
}

fun readProperties(
    who: Any,
    path: String,
    vars: Properties = Properties()
): Properties {
    fun parse(text: String) =
        Properties().apply { load(StringReader(text)) }
    fun read(who: Any, path: String) =
        who::class.java.classLoader.getResource(path).readText()
    fun expand(text: String, vars: Properties) =
        SimpleTemplateEngine().createTemplate(text).make(vars).toString()

    return parse(expand(read(who, path), vars))
}
