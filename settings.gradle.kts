pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "nibbliGO"

include(":app")
include(":core:model")
include(":core:designsystem")
include(":core:ui")
include(":core:domain")
include(":core:storage")
include(":core:runtime")
include(":core:agent")
include(":core:runtime-litert")
include(":core:litert-engine")
include(":core:hf-download")
include(":core:mcp")
include(":core:mobile-actions")
include(":core:pet-llm")
include(":feature:pet")
include(":feature:chat")
include(":feature:promptlab")
include(":feature:image")
include(":feature:audio")
include(":feature:actions")
include(":feature:models")
include(":feature:benchmark")
include(":feature:settings")
include(":feature:agent")
