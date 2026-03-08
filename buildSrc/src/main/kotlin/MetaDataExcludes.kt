class MetadataExcludes(env: Env) {
    val files: Set<String> = buildSet {
        if (!env.isForge && (!env.isNeo || !env.atLeast("1.20.6")))
            add("META-INF/mods.toml")
        if (!env.isFabric)
            add("fabric.mod.json")
        if (!env.isNeo)
            add("META-INF/neoforge.mods.toml")
    }
}