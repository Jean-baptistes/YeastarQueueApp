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
        // Dépôt Maven officiel de Linphone (SDK SIP)
        maven {
            name = "linphone.org maven repository"
            url = uri("https://download.linphone.org/maven_repository")
        }
    }
}

rootProject.name = "YeastarQueueCaller"
include(":app")
