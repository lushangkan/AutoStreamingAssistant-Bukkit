package cn.cutemc.autostreamingassistant.bukkit.listeners

import cn.cutemc.autostreamingassistant.bukkit.AutoStreamingAssistant
import cn.cutemc.autostreamingassistant.bukkit.events.CameraLeaveEvent
import cn.cutemc.autostreamingassistant.bukkit.events.PlayerLeaveEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

object PlayerQuitListener : Listener {

    private val plugin by lazy { AutoStreamingAssistant.INSTANCE }
    private val logger by lazy { plugin.logger }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        CoroutineScope(Dispatchers.Default).launch {
            plugin.mutexCameras.withLock {
                val playerName = event.player.name
                val isCamera = plugin.cameras.map { it.name }.contains(playerName)

                if (isCamera) {
                    CameraLeaveEvent.EVENT.post(CameraLeaveEvent(event.player))
                    return@launch
                }

                PlayerLeaveEvent.EVENT.post(PlayerLeaveEvent(event.player))
            }
        }
    }
}
