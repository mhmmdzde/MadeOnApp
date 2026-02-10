pluginManagement {
    repositories {
        // ⭐️ اول mirrorها (برای پلاگین‌ها)
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }

        // fallback
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // 🔹 این خط باعث می‌شود فقط ریپوهای زیر مجاز باشند
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        // ⭐️ mirrorها برای dependencyها
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }

        // fallback
        google()
        mavenCentral()
    }
}

rootProject.name = "MadeOn"
include(":app")