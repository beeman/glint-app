// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.lefthook)
    alias(libs.plugins.kotlin.compose) apply false
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
