package se.zensum.gradle

import java.io.StringReader
import java.util.Properties
import groovy.text.SimpleTemplateEngine

val versionProperties =
    readProperties("versions.properties")
val pluginProperties =
    readProperties("plugins.properties", vars = versionProperties)

fun version(x: String) = versionProperties.getProperty(x)

// This is the configurable ``zensum { ... }'' block definition.
open class ZensumProject {
    var jvm_version: String = version("jvm")

    var kotlin_version: String = version("kotlin")
    var kotlin_api_version: String = version("kotlin_api")
    var kotlin_coroutines_version: String = version("kotlin_coroutines")

    var main_class: String = "se.zensum.MainKt"
}

val grpcVersion = version("grpc")
val junitVersion = version("junit")



// We only use this to get access to the resources via class loader.
private class Foo {}

private fun readProperties(
    path: String,
    vars: Properties = Properties()
): Properties {
    fun parse(text: String) =
        Properties().apply { load(StringReader(text)) }
    fun read(path: String) =
        Foo::class.java.classLoader.getResource(path).readText()
    fun expand(text: String, vars: Properties) =
        SimpleTemplateEngine().createTemplate(text).make(vars).toString()

    return parse(expand(read(path), vars))
}
