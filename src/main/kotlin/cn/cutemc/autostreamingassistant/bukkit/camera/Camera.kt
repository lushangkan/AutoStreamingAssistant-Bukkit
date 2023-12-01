package cn.cutemc.autostreamingassistant.bukkit.camera

import cn.cutemc.autostreamingassistant.bukkit.AutoStreamingAssistant
import cn.cutemc.autostreamingassistant.bukkit.network.PacketID
import com.google.gson.Gson
import org.bukkit.entity.Player

class Camera(val name: String) {

    private val plugin by lazy { AutoStreamingAssistant.INSTANCE }
    private var online = false
    private var player: Player? = null

    fun joinServer(player: Player) {
        online = true
        this.player = player

        val message = Gson().toJson(mapOf("version" to plugin.description.version))

        player.sendPluginMessage(plugin, PacketID.REQUEST_STATUS, message.toByteArray())
    }

    fun leaveServer() {
        online = false
        player = null
    }

    fun isOnline() = online

}