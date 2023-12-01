package cn.cutemc.autostreamingassistant.bukkit.listeners.network;

import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener

object ManualBindCameraPacketListener : PluginMessageListener {

    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {

    }
}
