package cn.cutemc.autostreamingassistant.bukkit.network.messagings.listeners

import cn.cutemc.autostreamingassistant.bukkit.network.BindCameraResponsePacket
import cn.cutemc.autostreamingassistant.bukkit.network.messagings.events.BindCameraResponsePacketEvent
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener

object BindCameraResponsePacketListener: PluginMessageListener {


    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        val jackson = jacksonObjectMapper()
        val packet: BindCameraResponsePacket = jackson.readValue(message, BindCameraResponsePacket::class.java)

        BindCameraResponsePacketEvent.EVENT.post(BindCameraResponsePacketEvent(player, packet))
    }

}


