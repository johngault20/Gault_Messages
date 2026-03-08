// Official Gault Network - Root Manifold
val compileSdk: Int by extra(36)
val minSdk: Int by extra(24)
val targetSdk: Int by extra(36)
val desugarVersion: String by extra("2.1.4")

plugins {
    id("com.android.application") version "9.1.0" apply false
    id("com.android.library") version "9.1.0" apply false
    id("org.jetbrains.kotlin.android") version "2.3.10" apply false
}

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
}
