plugins {
    id("dev.kikugie.stonecutter")
    id("dev.architectury.loom") version "1.11-SNAPSHOT" apply false
    id("architectury-plugin") version "3.4-SNAPSHOT" apply false
    id("dev.kikugie.j52j") version "2.0" apply false // Enables asset processing by writing json5 files
    id("me.modmuss50.mod-publish-plugin") version "0.8.4" apply false // Publishes builds to hosting websites
}

stonecutter active "1.20+1-fabric"

