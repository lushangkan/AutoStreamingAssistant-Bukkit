package cn.cutemc.autostreamingassistant.bukkit.listeners

import cn.cutemc.autostreamingassistant.bukkit.AutoStreamingAssistant
import cn.cutemc.autostreamingassistant.bukkit.events.CameraMoveEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

object PlayerMoveListener: Listener {

    private val plugin by lazy { AutoStreamingAssistant.INSTANCE }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        CoroutineScope(Dispatchers.Default).launch {
            plugin.mutexCameras.withLock {
                val playerName = event.player.name

                plugin.cameras.forEach {
                    if (it.name == playerName) {
                        CameraMoveEvent.EVENT.post(CameraMoveEvent(it, event.from, event.to))
                        return@launch
                    }
                }

                cn.cutemc.autostreamingassistant.bukkit.events.PlayerMoveEvent.EVENT.post(
                    cn.cutemc.autostreamingassistant.bukkit.events.PlayerMoveEvent(
                        event.player,
                        event.from,
                        event.to
                    )
                )
            }
        }
    }

}