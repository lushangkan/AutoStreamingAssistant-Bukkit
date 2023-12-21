package cn.cutemc.autostreamingassistant.bukkit.listeners

import cn.cutemc.autostreamingassistant.bukkit.AutoStreamingAssistant
import cn.cutemc.autostreamingassistant.bukkit.events.CameraGameModeChangeEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerGameModeChangeEvent

object PlayerGameModeChangeListener: Listener {

    private val plugin by lazy { AutoStreamingAssistant.INSTANCE }
    private val logger by lazy { plugin.logger }

    @EventHandler
    fun onPlayerGameModeChange(event: PlayerGameModeChangeEvent) {
        val playerName = event.player.name

        plugin.cameras.forEach {
            if (it.name == playerName) {
                CameraGameModeChangeEvent.EVENT.post(CameraGameModeChangeEvent(it, event.newGameMode))
                return
            }
        }

        cn.cutemc.autostreamingassistant.bukkit.events.PlayerGameModeChangeEvent.EVENT.post(
            cn.cutemc.autostreamingassistant.bukkit.events.PlayerGameModeChangeEvent(event.player, event.newGameMode)
        )
    }

}