package cn.cutemc.autostreamingassistant.bukkit.listeners.bukkit

import cn.cutemc.autostreamingassistant.bukkit.AutoStreamingAssistant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent


object PlayerJoinListener : Listener {

    private val plugin by lazy { AutoStreamingAssistant.INSTANCE }
    private val logger by lazy { plugin.logger }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val playerName = event.player.name

        plugin.cameras.forEach {
            if (it.name == playerName) {
                CoroutineScope(Dispatchers.IO).launch {
                    it.joinServer(event.player)
                }
            }
        }
    }

}