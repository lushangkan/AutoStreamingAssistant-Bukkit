package cn.cutemc.autostreamingassistant.bukkit.listeners.network.messagings

import cn.cutemc.autostreamingassistant.bukkit.AutoStreamingAssistant
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener

object ClientStatusPacketListener : PluginMessageListener {

    val plugin by lazy { AutoStreamingAssistant.INSTANCE }

    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        plugin.logger.info("Received client status packet from ${player.name}: ${String(message)}")
    }

}