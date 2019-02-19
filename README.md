# zensum-gradle-plugin

This plugin gathers common configuration for our Kotlin projects, so
we don't have to repeat them, and so we can more easily apply updates
to the other projects.

We use Gradle 5 and the Kotlin DSL for both the plugin's build
configuration and the plugin's source code.

The plugin

  - applies several other plugins;
  - configures Maven repositories;
  - configures compiler options;
  - configures `run`/`test`/`debug` tasks;
  - adds boilerplate dependencies, like
    - Kotlin's standard library,
    - Kotlin coroutines,
    - Kotlin logging,
    - GRPC,
    - etc.

See `project.gradle.kts` for the actual plugin code.

See also `versions.properties` and `plugins.properties`.
