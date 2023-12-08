package cn.cutemc.autostreamingassistant.bukkit.listeners.network.messagings

import cn.cutemc.autostreamingassistant.bukkit.network.BindCameraResponse
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener
import java.util.*

object BindCameraResponsePacketListener: PluginMessageListener {

    val listeners = mutableMapOf<UUID, (BindCameraResponse) -> Unit>()

    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        val jackson = jacksonObjectMapper()
        val status: BindCameraResponse = jackson.readValue(message, BindCameraResponse::class.java)

        listeners[player.uniqueId]?.invoke(status)
        listeners.remove(player.uniqueId)
    }

}

