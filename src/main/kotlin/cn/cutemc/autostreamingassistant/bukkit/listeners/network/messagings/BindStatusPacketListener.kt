package cn.cutemc.autostreamingassistant.bukkit.listeners.network.messagings

import cn.cutemc.autostreamingassistant.bukkit.network.BindStatusPacket
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener
import java.util.*

object BindStatusPacketListener : PluginMessageListener {

    val listeners = mutableMapOf<UUID, (BindStatusPacket) -> Unit>()

    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        val jackson = jacksonObjectMapper()
        val packet: BindStatusPacket = jackson.readValue(message, BindStatusPacket::class.java)

        listeners[player.uniqueId]?.invoke(packet)
        listeners.remove(player.uniqueId)
    }

}

