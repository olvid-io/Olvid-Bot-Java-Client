import build.buf.gradle.GENERATED_DIR
import build.buf.gradle.BUF_BUILD_DIR

repositories {
    mavenCentral()
}

plugins {
    id("java")
    id("application")
    id("build.buf") version "0.10.2"
}

group = "io.olvid.daemon"
version = "2.0.0-alpha"

var protobufVersion = "4.33.0"
var grpcVersion = "1.78.0"

dependencies {
    implementation("com.google.protobuf:protobuf-java:${protobufVersion}")
    implementation("io.grpc:grpc-core:${grpcVersion}")
    implementation("io.grpc:grpc-protobuf:${grpcVersion}")
    implementation("io.grpc:grpc-stub:${grpcVersion}")
    implementation("io.grpc:grpc-okhttp:${grpcVersion}")

    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// configure application plugin (to run module as main)
apply { plugin("java") }

application {
    mainClass = "io.olvid.daemon.java.main.Main"
}

// configure buf plugin
buf {
    // disable format review
    enforceFormat = false

    generate {
        includeImports = true
        templateFileLocation = rootProject.file("buf.gen.yaml")
    }
}

// manually removed bufLint task that is failing for path reasons (and we run it regularly)
tasks.named("check") {
    // This removes the dependency if it was added via dependsOn
    setDependsOn(dependsOn.filterNot {
        (it as? Task)?.name == "bufLint" || it.toString().contains("bufLint")
    })
}

// Add a task dependency for compilation
tasks.named("compileJava").configure { dependsOn("bufGenerate") }

// Add the generated code to the main source set
sourceSets["main"].java {
    srcDir("${layout.buildDirectory.get()}/${BUF_BUILD_DIR}/${GENERATED_DIR}/java")
    srcDir("${layout.buildDirectory.get()}/${BUF_BUILD_DIR}/${GENERATED_DIR}/grpc")
}

// setup tests
tasks.withType<Test> {
    useJUnitPlatform()
}
