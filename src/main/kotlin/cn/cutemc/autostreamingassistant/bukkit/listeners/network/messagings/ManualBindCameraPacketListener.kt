package cn.cutemc.autostreamingassistant.bukkit.listeners.network.messagings

import cn.cutemc.autostreamingassistant.bukkit.network.ManualBindCameraPacket
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener
import java.util.*

object ManualBindCameraPacketListener : PluginMessageListener {

    val listeners = mutableMapOf<UUID, (ManualBindCameraPacket) -> Unit>()

    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        val jackson = jacksonObjectMapper()
        val status: ManualBindCameraPacket = jackson.readValue(message, ManualBindCameraPacket::class.java)

        listeners[player.uniqueId]?.invoke(status)
        listeners.remove(player.uniqueId)
    }
}

