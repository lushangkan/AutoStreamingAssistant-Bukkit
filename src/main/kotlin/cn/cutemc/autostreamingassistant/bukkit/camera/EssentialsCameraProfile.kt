package cn.cutemc.autostreamingassistant.bukkit.camera

import cn.cutemc.autostreamingassistant.bukkit.AutoStreamingAssistant
import com.earth2me.essentials.Essentials
import com.earth2me.essentials.User
import java.util.UUID

class EssentialsCameraProfile(uuid: UUID) : CameraProfile {

    private val plugin by lazy { AutoStreamingAssistant.INSTANCE }
    private val essentials: Essentials by lazy { plugin.server.pluginManager.getPlugin("Essentials") as Essentials }
    private val user by lazy { essentials.getUser(uuid) }

    override fun isGodModOn(): Boolean {
        return user.isGodModeEnabled
    }

    override fun setGodMod(on: Boolean) {
        user.isGodModeEnabled = on
    }
}