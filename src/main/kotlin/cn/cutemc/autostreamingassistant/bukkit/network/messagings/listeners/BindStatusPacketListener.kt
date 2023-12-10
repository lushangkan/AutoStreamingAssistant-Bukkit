package cn.cutemc.autostreamingassistant.bukkit.network.messagings.listeners

import cn.cutemc.autostreamingassistant.bukkit.network.BindStatusPacket
import cn.cutemc.autostreamingassistant.bukkit.network.messagings.events.BindStatusPacketEvent
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener

object BindStatusPacketListener : PluginMessageListener {

    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        val jackson = jacksonObjectMapper()
        val packet: BindStatusPacket = jackson.readValue(message, BindStatusPacket::class.java)

        BindStatusPacketEvent.EVENT.post(BindStatusPacketEvent(player, packet))
    }

}