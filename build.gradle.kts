plugins {
    id("com.android.application") version "8.4.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
}

// Keep build outputs outside OneDrive to avoid file locking during Kotlin/KSP tasks.
subprojects {
    layout.buildDirectory.set(
        file("${System.getProperty("user.home")}/.packapp-build/${rootProject.rootDir.name}/${project.name}")
    )
}
