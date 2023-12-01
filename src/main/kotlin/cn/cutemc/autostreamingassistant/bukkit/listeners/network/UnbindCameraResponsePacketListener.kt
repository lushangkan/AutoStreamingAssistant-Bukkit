package cn.cutemc.autostreamingassistant.bukkit.listeners.network

import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener

object UnbindCameraResponsePacketListener : PluginMessageListener {

    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {

    }
}