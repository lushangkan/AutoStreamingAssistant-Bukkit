package cn.cutemc.autostreamingassistant.bukkit.listeners

import cn.cutemc.autostreamingassistant.bukkit.AutoStreamingAssistant
import cn.cutemc.autostreamingassistant.bukkit.events.CameraJoinEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent


object PlayerJoinListener : Listener {

    private val plugin by lazy { AutoStreamingAssistant.INSTANCE }
    private val logger by lazy { plugin.logger }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val playerName = event.player.name
        val isCamera = plugin.cameras.map { it.name }.contains(playerName)

        if (isCamera) {
            CameraJoinEvent.EVENT.post(CameraJoinEvent(event.player))
            return
        }

        cn.cutemc.autostreamingassistant.bukkit.events.PlayerJoinEvent.EVENT.post(cn.cutemc.autostreamingassistant.bukkit.events.PlayerJoinEvent(event.player))
    }

}