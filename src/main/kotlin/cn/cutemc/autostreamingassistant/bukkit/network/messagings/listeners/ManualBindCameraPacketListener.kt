package cn.cutemc.autostreamingassistant.bukkit.network.messagings.listeners

import cn.cutemc.autostreamingassistant.bukkit.network.ManualBindCameraPacket
import cn.cutemc.autostreamingassistant.bukkit.network.messagings.events.ManualBindCameraPacketEvent
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener

object ManualBindCameraPacketListener : PluginMessageListener {

    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        val jackson = jacksonObjectMapper()
        val packet: ManualBindCameraPacket = jackson.readValue(message, ManualBindCameraPacket::class.java)

        ManualBindCameraPacketEvent.EVENT.post(ManualBindCameraPacketEvent(player, packet))
    }
}

