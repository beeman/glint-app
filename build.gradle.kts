// Top-level build file where you can add configuration options common to all sub-projects/modules.
import java.util.Properties

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.lefthook)
    alias(libs.plugins.kotlin.compose) apply false
}

tasks.register("bumpVersion") {
    group = "versioning"
    description = "Bumps VERSION_CODE and VERSION_NAME in gradle.properties. Use -Pbump=patch|minor|major."

    doLast {
        val bump = providers.gradleProperty("bump").orNull
            ?: throw GradleException("Missing bump type. Use -Pbump=patch, -Pbump=minor, or -Pbump=major.")
        val bumpTypes = listOf("major", "minor", "patch")
        if (bump !in bumpTypes) {
            throw GradleException("Invalid bump type '$bump'. Use one of: ${bumpTypes.joinToString(", ")}.")
        }

        val versionPropertiesFile = rootProject.file("gradle.properties")
        val versionProperties = Properties().apply {
            versionPropertiesFile.inputStream().use(::load)
        }
        val currentVersionCode = versionProperties.getProperty("VERSION_CODE")?.toIntOrNull()
            ?: throw GradleException("VERSION_CODE must be an integer in gradle.properties.")
        val currentVersionName = versionProperties.getProperty("VERSION_NAME")
            ?: throw GradleException("VERSION_NAME must be set in gradle.properties.")
        val versionParts = currentVersionName.split(".").map { part ->
            part.toIntOrNull() ?: throw GradleException("VERSION_NAME must use numeric parts, for example 1.0.0.")
        }
        if (versionParts.size !in 2..3) {
            throw GradleException("VERSION_NAME must use major.minor or major.minor.patch format.")
        }

        val currentMajor = versionParts[0]
        val currentMinor = versionParts[1]
        val currentPatch = versionParts.getOrElse(2) { 0 }
        val nextVersionName = when (bump) {
            "major" -> "${currentMajor + 1}.0.0"
            "minor" -> "$currentMajor.${currentMinor + 1}.0"
            else -> "$currentMajor.$currentMinor.${currentPatch + 1}"
        }
        val nextVersionCode = currentVersionCode + 1

        val updatedLines = versionPropertiesFile.readLines().map { line ->
            when {
                line.startsWith("VERSION_CODE=") -> "VERSION_CODE=$nextVersionCode"
                line.startsWith("VERSION_NAME=") -> "VERSION_NAME=$nextVersionName"
                else -> line
            }
        }
        versionPropertiesFile.writeText(updatedLines.joinToString(System.lineSeparator()) + System.lineSeparator())

        println("Bumped Android version from $currentVersionName ($currentVersionCode) to $nextVersionName ($nextVersionCode).")
    }
}

lefthook {
    config.set(
        mapOf(
            "pre-commit" to mapOf(
                "commands" to mapOf(
                    "ktlint" to mapOf(
                        "run" to "./gradlew :app:ktlintCheck"
                    )
                )
            ),
            "pre-push" to mapOf(
                "commands" to mapOf(
                    "check" to mapOf(
                        "run" to "just check"
                    )
                )
            )
        )
    )
    version.set("v2.1.6")
}
