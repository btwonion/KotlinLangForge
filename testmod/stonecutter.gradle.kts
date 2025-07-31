plugins {
    id("dev.kikugie.stonecutter")
}
stonecutter active "3.0-neoforge"

stonecutter parameters {
    val split = node.metadata.project.split("-")
    val lPVersion = split[0]
    dependencies["lp"] = lPVersion

    val loader = split[1]
    listOf("forge", "neoforge").forEach {
        constants[it] = loader == it
    }
}