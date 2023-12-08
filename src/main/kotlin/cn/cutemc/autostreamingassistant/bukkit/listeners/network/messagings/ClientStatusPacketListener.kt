package cn.cutemc.autostreamingassistant.bukkit.listeners.network.messagings

import cn.cutemc.autostreamingassistant.bukkit.network.ClientStatusPacket
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener
import java.util.*

object ClientStatusPacketListener : PluginMessageListener {

    val listeners = mutableMapOf<UUID, (ClientStatusPacket) -> Unit>()

    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        val jackson = jacksonObjectMapper()
        val packet: ClientStatusPacket = jackson.readValue(message, ClientStatusPacket::class.java)

        listeners[player.uniqueId]?.invoke(packet)
        listeners.remove(player.uniqueId)
    }

}

