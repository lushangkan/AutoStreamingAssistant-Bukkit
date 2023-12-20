package cn.cutemc.autostreamingassistant.bukkit.camera;

import cn.cutemc.autostreamingassistant.bukkit.AutoStreamingAssistant
import com.Zrips.CMI.CMI
import java.util.*

class CMICameraProfile(uuid: UUID): CameraProfile {

    private val plugin by lazy { AutoStreamingAssistant.INSTANCE }
    private val cmi: CMI by lazy { plugin.server.pluginManager.getPlugin("CMI") as CMI }
    private val user by lazy { cmi.playerManager.getUser(uuid) }
    override fun isGodModOn(): Boolean {
        return user.isGod
    }

    override fun setGodMod(on: Boolean) {
        // CMI API 无法设置上帝模式
        plugin.server.dispatchCommand(plugin.server.consoleSender, "cmi god ${user.name} $on")
    }


}
