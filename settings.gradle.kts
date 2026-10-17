plugins {
    // Temurin 25가 로컬에 없으면 Gradle이 내려받게 한다
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "lab"

include(
    "core",
    "lab-redis",
)
