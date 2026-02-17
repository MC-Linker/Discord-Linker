plugins {
    id("dev.kikugie.stonecutter")
    id("dev.architectury.loom") version "1.13-SNAPSHOT" apply false
    id("architectury-plugin") version "3.4-SNAPSHOT" apply false
    id("me.modmuss50.mod-publish-plugin") version "0.8.4" apply false // Publishes builds to hosting websites
}

stonecutter active "1.20+1-fabric"

