plugins {
    alias(libs.plugins.kotlin)
}

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases/")
}

dependencies {
    implementation(kotlin("reflect"))
    implementation("net.neoforged:bus:8.0.5")
    implementation("org.apache.logging.log4j:log4j-api:2.24.1")
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(21)
    sourceSets.main {
        kotlin.srcDir("../../klf/src/main/kotlin")
        // Compile the production implementation directly, with small loader fixtures instead of Minecraft.
        kotlin.include(
            "dev/nyon/klf/AutomaticEventSubscriber.kt",
            "dev/nyon/klf/KotlinModContainer.kt",
            "dev/nyon/klf/LoadingContextFixture.kt",
            "dev/nyon/klf/mv/LoaderFixtures.kt"
        )
    }
}

val fixtureJar = tasks.register<Jar>("fixtureJar") {
    dependsOn(tasks.testClasses)
    from(sourceSets.test.get().output) {
        include("dev/nyon/klf/regression/fixtures/**")
    }
    archiveBaseName = "subscriber-fixtures"
    manifest.attributes("Automatic-Module-Name" to "klf.regression.fixtures")
}

tasks.test {
    useJUnitPlatform()
    dependsOn(fixtureJar)
    systemProperty("regression.fixtureJar", fixtureJar.get().archiveFile.get().asFile.absolutePath)
}