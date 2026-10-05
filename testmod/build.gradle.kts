import net.fabricmc.loom.util.ModPlatform
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.architectury.loom)
}

val loader = loom.platform.get()
val mcVersion = property("vers.mcVersion").toString()

loom {
    if (stonecutter.current.isActive) {
        runConfigs.all {
            generateRunConfig = true
            runDirectory = project.file("../../run")
        }
    }

    runConfigs.create("smokeServer") {
        server()
        generateRunConfig = false
        runDirectory = layout.buildDirectory.dir("smoke-server").get().asFile
        vmArg("-Dklf.smoke.result=${layout.buildDirectory.file("smoke-server/result.txt").get().asFile.absolutePath}")
        programArgs("--port", "0")
    }

    silentMojangMappingsLicense()
}

repositories {
    mavenCentral()
    maven("https://maven.quiltmc.org/repository/release/")
    maven("https://maven.neoforged.net/releases/")
    maven("https://maven.minecraftforge.net/")
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings(loom.officialMojangMappings())

    if (loader == ModPlatform.FORGE) "forge"("net.minecraftforge:forge:$mcVersion-${property("vers.deps.fml")}")
    else "neoForge"("net.neoforged:neoforge:${property("vers.deps.fml")}")

    implementation(include(project(":klf:${project.name}"))!!)

    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")
}

val javaVersion =
    if (stonecutter.eval(mcVersion, ">=1.20.6")) 21 else if (stonecutter.eval(mcVersion, ">1.16.5")) 17 else 8

tasks {
    processResources {
        exclude(if (loader == ModPlatform.NEOFORGE) "META-INF/mods.toml" else "META-INF/neoforge.mods.toml")
    }

    withType<JavaCompile> {
        options.release = javaVersion
    }

    withType<KotlinCompile> {
        compilerOptions {
            jvmTarget = JvmTarget.fromTarget(javaVersion.toString().let { if (it == "8") "1.8" else it })
        }
    }
}

// Load the fixture in a real named module, as KotlinModContainer does during a launch.
// Keep it off the test worker's parent classpath so Class.forName(Module, ...) resolves it in that module.
val smokeService = tasks.register("generateSmokeService") {
    val directory = layout.buildDirectory.dir("generated/smoke-service")
    inputs.property("loader", loader.name)
    outputs.dir(directory)
    doLast {
        val namespace = if (loader == ModPlatform.FORGE) "net.minecraftforge" else "net.neoforged"
        directory.get().asFile.resolve("META-INF/services/$namespace.fml.IBindingsProvider").apply {
            parentFile.mkdirs()
            writeText("dev.nyon.klf.test.runtime.SmokeBindingsProvider\n")
        }
    }
}
val smokeFixtureJar = tasks.register<Jar>("smokeFixtureJar") {
    from(sourceSets.main.get().output)
    from(smokeService)
    archiveClassifier = "smoke-fixture"
    manifest.attributes("Automatic-Module-Name" to "klf.smoke.fixture")
}

fun Test.configureRuntimeChecks() {
    dependsOn(smokeFixtureJar)
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath - sourceSets.main.get().output
    maxParallelForks = 1
    workingDir = layout.buildDirectory.dir("runtime-jvm/$name").get().asFile
    doFirst { workingDir.mkdirs() }
    systemProperty("klf.fixtureJar", smokeFixtureJar.get().archiveFile.get().asFile.absolutePath)
    systemProperty("klf.mcVersion", mcVersion)
    systemProperty("klf.test.dist", "DEDICATED_SERVER")
    testLogging { events("passed", "skipped", "failed") }
}

tasks.test {
    configureRuntimeChecks()
    useJUnitPlatform {
        // These assert the desired behavior and fail on the baseline. See testing.md for fix dependencies.
        excludeTags("event-regression", "forge-regression", "diagnostics-regression")
    }
}

val testClientDistribution = tasks.register<Test>("testClientDistribution") {
    description = "Runs JVM behavior checks with a synthetic CLIENT FML distribution; does not launch Minecraft."
    group = "verification"
    configureRuntimeChecks()
    systemProperty("klf.test.dist", "CLIENT")
    useJUnitPlatform { excludeTags("event-regression", "forge-regression", "diagnostics-regression") }
}
tasks.check { dependsOn(testClientDistribution) }

tasks.register<Test>("runtimeRegressionTest") {
    description = "Runs all runtime checks, including regressions requiring the event/Forge/diagnostics fixes."
    group = "verification"
    configureRuntimeChecks()
    useJUnitPlatform()
}

val smokeResult = layout.buildDirectory.file("smoke-server/result.txt")
tasks.named("runSmokeServer") {
    doFirst { smokeResult.get().asFile.delete() }
    doLast {
        check(smokeResult.get().asFile.takeIf { it.isFile }?.readText() == "KLF_SERVER_SMOKE_PASSED\n") {
            "The server did not finish the KLF runtime assertions. See the smoke-server logs."
        }
    }
}
