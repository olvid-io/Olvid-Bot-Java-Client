plugins {
    id("java")
    id("application")
}

dependencies {
    implementation("com.github.olvid-io:Olvid-Bot-Java-Client:2.0.1")
}

// configure application plugin (to run module as main)
apply { plugin("java") }

application {
    mainClass = "io.olvid.daemon.example.Main"
}
