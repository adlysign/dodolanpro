System.setProperty("java.awt.headless", "true")

val previousUncaughtHandler = Thread.getDefaultUncaughtExceptionHandler()
val kspAwtExceptionFilter = Thread.UncaughtExceptionHandler { thread, throwable ->
    val msg = throwable.message ?: ""
    val isKspAwtTeardownNpe = thread.name.startsWith("AWT-EventQueue") &&
        throwable is NullPointerException &&
        (msg.contains("ksp.com.intellij.openapi.application.Application") ||
            throwable.stackTrace.any { it.className.startsWith("ksp.com.intellij.") })
    if (!isKspAwtTeardownNpe) {
        if (previousUncaughtHandler != null) {
            previousUncaughtHandler.uncaughtException(thread, throwable)
        } else {
            System.err.print("Exception in thread \"${thread.name}\" ")
            throwable.printStackTrace(System.err)
        }
    }
}
Thread.setDefaultUncaughtExceptionHandler(kspAwtExceptionFilter)
Thread.getAllStackTraces().keys
    .filter { it.name.startsWith("AWT-EventQueue") }
    .forEach { it.uncaughtExceptionHandler = kspAwtExceptionFilter }

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "DodolanPro"
include(":app")
