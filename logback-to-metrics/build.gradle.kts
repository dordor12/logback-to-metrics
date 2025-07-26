group = "io.github.dordor12"

object Meta {
    const val release = "https://s01.oss.sonatype.org/service/local/"
    const val snapshot = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
    const val desc = "seamlessly integrates logging with metrics collection"
    const val license = "Apache-2.0"
    const val licenseUrl = "https://opensource.org/licenses/Apache-2.0"
    const val githubRepo = "dordor12/logback-to-metrics"
    const val developerId = "dordor12"
    const val developerName = "Dor Amid"
    const val developerOrganization = "dordor inc"
    const val developerOrganizationUrl = "https://yourdomain.com"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

plugins {
    // Apply the java-library plugin for API and implementation separation.
    `java-library`
    id("com.vanniktech.maven.publish") version "0.34.0"
}

repositories {
    mavenCentral()
}

sourceSets {
    create("intTest") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

val intTestImplementation by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

configurations["intTestRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())

dependencies {
     intTestImplementation(libs.junit)
     intTestImplementation(libs.mockito)
     intTestImplementation(libs.bundles.testcontainers.junit)
     intTestImplementation(libs.assertj)

     api(libs.logback)
     api(libs.micrometer)
     api(libs.commonsCodec)
     api(libs.logstashEncoder)
     annotationProcessor(libs.lombok)
     compileOnly(libs.lombok)

    testImplementation(libs.mockito)
    testImplementation(libs.junit)
    annotationProcessor(libs.junit)
    testAnnotationProcessor(libs.junit)
}

val intTest = task<Test>("intTest") {
    description = "Runs integration tests."
    group = "verification"

    testClassesDirs = sourceSets["intTest"].output.classesDirs
    classpath = sourceSets["intTest"].runtimeClasspath
    shouldRunAfter("test")

    useJUnitPlatform()

    testLogging {
        events("passed")
    }
}

tasks.check { dependsOn(intTest) }

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}

mavenPublishing {
    coordinates("io.github.dordor12", "logback-to-metrics", project.version.toString())
    
    // Configure Maven Central publishing
    publishToMavenCentral()
    
    // Enable signing for Maven Central (required)
    signAllPublications()
    
    pom {
        name.set("Logback to Metrics")
        description.set(Meta.desc)
        inceptionYear.set("2024")
        url.set("https://github.com/${Meta.githubRepo}")
        
        licenses {
            license {
                name.set(Meta.license)
                url.set(Meta.licenseUrl)
                distribution.set(Meta.licenseUrl)
            }
        }
        
        developers {
            developer {
                id.set(Meta.developerId)
                name.set(Meta.developerName)
                url.set("https://github.com/${Meta.developerId}")
            }
        }
        
        scm {
            url.set("https://github.com/${Meta.githubRepo}")
            connection.set("scm:git:git://github.com/${Meta.githubRepo}.git")
            developerConnection.set("scm:git:ssh://git@github.com/${Meta.githubRepo}.git")
        }
    }
}

tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
            ),
        )
    }
}

// Sources and Javadoc jars are automatically configured by the Vanniktech Maven Publish plugin

// gradle locking of dependency versions
//   *required+used for trivy scan
dependencyLocking {
    lockAllConfigurations()
}
// always run subproject task with parent
rootProject.tasks.dependencies { dependsOn(tasks.dependencies) }
