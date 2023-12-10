package cn.cutemc.autostreamingassistant.bukkit.network.messagings.listeners

import cn.cutemc.autostreamingassistant.bukkit.network.ClientStatusPacket
import cn.cutemc.autostreamingassistant.bukkit.network.messagings.events.ClientStatusPacketEvent
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener

object ClientStatusPacketListener : PluginMessageListener {


    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        val jackson = jacksonObjectMapper()
        val packet: ClientStatusPacket = jackson.readValue(message, ClientStatusPacket::class.java)

        ClientStatusPacketEvent.EVENT.post(ClientStatusPacketEvent(player, packet))
    }

}


