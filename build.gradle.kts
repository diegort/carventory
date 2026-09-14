// Top-level build file
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:9.0.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.10")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.9.6")
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}