package cn.cutemc.autostreamingassistant.bukkit.listeners.network.messagings

import cn.cutemc.autostreamingassistant.bukkit.network.UnbindResult
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener
import java.util.*

object UnbindCameraResponsePacketListener : PluginMessageListener {

    val listeners = mutableMapOf<UUID, (UnbindResult) -> Unit>()
    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        val jackson = jacksonObjectMapper()
        val status: UnbindResult = jackson.readValue(message, UnbindResult::class.java)

        listeners[player.uniqueId]?.invoke(status)
        listeners.remove(player.uniqueId)
    }
}

