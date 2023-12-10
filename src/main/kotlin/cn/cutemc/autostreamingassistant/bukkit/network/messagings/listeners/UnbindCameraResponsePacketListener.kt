package cn.cutemc.autostreamingassistant.bukkit.network.messagings.listeners

import cn.cutemc.autostreamingassistant.bukkit.network.UnbindCameraResponsePacket
import cn.cutemc.autostreamingassistant.bukkit.network.messagings.events.UnbindCameraResponsePacketEvent
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener

object UnbindCameraResponsePacketListener : PluginMessageListener {

    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        val jackson = jacksonObjectMapper()
        val packet: UnbindCameraResponsePacket = jackson.readValue(message, UnbindCameraResponsePacket::class.java)

        UnbindCameraResponsePacketEvent.EVENT.post(UnbindCameraResponsePacketEvent(player, packet))
    }

}