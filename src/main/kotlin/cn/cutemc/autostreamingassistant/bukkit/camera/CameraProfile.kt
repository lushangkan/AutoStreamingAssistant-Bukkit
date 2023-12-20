package cn.cutemc.autostreamingassistant.bukkit.camera

interface CameraProfile {

    fun isGodModOn(): Boolean

    fun setGodMod(on: Boolean)

    fun isVanished(): Boolean

    fun setVanish(on: Boolean)

}