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
        google()          // 🔹 Firebase bağımlılıkları buradan çözülür
        mavenCentral()
    }
}

rootProject.name = "Dozi"
include(":app")
