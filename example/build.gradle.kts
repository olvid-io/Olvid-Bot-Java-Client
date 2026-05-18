plugins {
    id("java")
    id("application")
}

dependencies {
    implementation("com.github.olvid-io:Olvid-Bot-Java-Client:307c7bb")
}

// configure application plugin (to run module as main)
apply { plugin("java") }

application {
    mainClass = "io.olvid.daemon.example.Main"
}
