//
// This is a Kotlin DSL script which gets compiled into the binary
// Gradle plugin `se.zensum.gradle.project`.
//

package se.zensum.gradle

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

// Use built-in plugins.  External plugins are specified in the
// resource file `plugins.properties`.
plugins {
    java
    kotlin("jvm")
    maven
    idea
}

// Configure all the POM/JAR repositories we like to use.
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

// Make the plugin configurable via a ``zensum { ... }'' block.
val zensum = extensions.create<ZensumProject>("zensum")

// Apply the plugins listed in `plugins.properties'.
for ((k, _) in pluginProperties) {
    apply(plugin = k as String)
}

// Add our standard dependencies that don't have configurable
// versions; the configurable ones come later.
dependencies {
    "compile"(
        "io.github.microutils:kotlin-logging:1.6.20")

    "testCompile"(
        "org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    "testRuntime"(
        "org.junit.jupiter:junit-jupiter-engine:${junitVersion}")

    "implementation"(
        "io.grpc:grpc-netty:${grpcVersion}")
    "implementation"(
        "io.grpc:grpc-protobuf:${grpcVersion}")
    "implementation"(
        "io.grpc:grpc-stub:${grpcVersion}")
}

val mainSourceSet = the<JavaPluginConvention>().sourceSets["main"]
val mainClasspath = mainSourceSet.runtimeClasspath

defaultTasks("run")

task<JavaExec>("run") {
    main = zensum.main_class
    classpath = mainClasspath
}

task<JavaExec>("debug") {
    main = zensum.main_class
    classpath = mainClasspath
    debug = true
    environment["DEBUG"] = true
}

tasks {
    // This task sets up dependencies with versions that can be
    // configured by the plugin consumer.
    val configureDependencies by registering() {
        dependencies {
            doLast {
                "compile"("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${zensum.kotlin_version}")
                "compile"("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${zensum.kotlin_coroutines_version}")
                "compile"("org.jetbrains.kotlinx:kotlinx-coroutines-core:${zensum.kotlin_coroutines_version}")
            }
        }
    }

    withType<KotlinCompile> {
        dependsOn(configureDependencies)
        kotlinOptions.languageVersion = zensum.kotlin_api_version
        kotlinOptions.apiVersion = zensum.kotlin_api_version
        kotlinOptions.jvmTarget = zensum.jvm_version
        kotlinOptions.javaParameters = true
    }

    withType<JavaCompile> {
        dependsOn(configureDependencies)
        sourceCompatibility = zensum.jvm_version
        targetCompatibility = zensum.jvm_version
        options.setIncremental(true)
        options.encoding = "UTF-8"
    }

    withType<Wrapper> {
        gradleVersion = version("gradle")
    }

    withType<Test> {
        useJUnitPlatform()
    }

    withType<Jar> {
        manifest {
            attributes("Main-Class" to zensum.main_class)
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

