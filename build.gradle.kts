plugins {
    `java-library`
    `maven-publish`
    signing
    id("com.diffplug.spotless") version "6.25.0"
}

group = "io.alphainfo"
version = "1.5.23"
description = "Java client for the alphainfo.io Structural Intelligence API"

java {
    // Target Java 11 bytecode so consumers on 11+ can use the JAR, but
    // don't require a JDK 11 toolchain for the build — modern toolchains
    // (17/21) compile perfectly with --release 11.
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.javadoc {
    // Don't fail CI on missing JavaDoc tags.
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

spotless {
    java {
        // Keep this in lockstep with the `google-java-format` CLI used locally
        // to apply fixes (brew install google-java-format). Version skew
        // will cause CI to reject files that look fine locally.
        googleJavaFormat("1.25.2")
        target("src/**/*.java")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set("alphainfo")
                description.set(project.description)
                url.set("https://alphainfo.io")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                organization {
                    name.set("QGI Quantum Systems LTDA")
                    url.set("https://qgi.com.br")
                }
                developers {
                    developer {
                        id.set("qgi")
                        name.set("QGI Quantum Systems")
                        email.set("contato@alphainfo.io")
                        organization.set("QGI Quantum Systems LTDA")
                        organizationUrl.set("https://qgi.com.br")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/qgidev/alphainfo-java.git")
                    developerConnection.set("scm:git:ssh://github.com:qgidev/alphainfo-java.git")
                    url.set("https://github.com/qgidev/alphainfo-java")
                }
            }
        }
    }
}
