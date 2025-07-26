group = "io.github.dordor12"

plugins {
    alias(libs.plugins.release)
}

// Task delegation to subproject for workflow compatibility
tasks.named("build") {
    dependsOn(":logback-to-metrics:build")
}

tasks.register("test") {
    dependsOn(":logback-to-metrics:test")
    description = "Runs unit tests in the main subproject"
}

tasks.register("intTest") {
    dependsOn(":logback-to-metrics:intTest")
    description = "Runs integration tests in the main subproject"
}

tasks.register("javadoc") {
    dependsOn(":logback-to-metrics:javadoc")
    description = "Generates Javadoc for the main subproject"
}

tasks.register("publishToMavenCentral") {
    dependsOn(":logback-to-metrics:publishToMavenCentral")
    description = "Publishes artifacts to Maven Central"
}

tasks.register("publishAndReleaseToMavenCentral") {
    dependsOn(":logback-to-metrics:publishAndReleaseToMavenCentral")
    description = "Publishes and releases artifacts to Maven Central"
}

// Release plugin configuration
release {
    buildTasks = listOf("build")
}
