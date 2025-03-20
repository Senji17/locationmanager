pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

// ✅ ตรวจสอบว่าโมดูล `:app` ถูกเพิ่มไว้
rootProject.name = "locationmanager"
include(":app")
