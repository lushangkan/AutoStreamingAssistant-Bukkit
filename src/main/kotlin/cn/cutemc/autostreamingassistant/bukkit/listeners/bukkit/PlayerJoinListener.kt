package cn.cutemc.autostreamingassistant.bukkit.listeners.bukkit

import cn.cutemc.autostreamingassistant.bukkit.AutoStreamingAssistant
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

object PlayerJoinListener : Listener {

    val plugin by lazy { AutoStreamingAssistant.INSTANCE }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val playerName = event.player.name;

        plugin.cameras.forEach {
            if (it.name == playerName) {
                it.joinServer(event.player)
            }
        }
    }

}