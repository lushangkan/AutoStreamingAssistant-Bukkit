package cn.cutemc.autostreamingassistant.bukkit.listeners

import cn.cutemc.autostreamingassistant.bukkit.AutoStreamingAssistant
import cn.cutemc.autostreamingassistant.bukkit.events.CameraJoinEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.scoreboard.Team


object PlayerJoinListener : Listener {

    private val plugin by lazy { AutoStreamingAssistant.INSTANCE }
    private val logger by lazy { plugin.logger }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        CoroutineScope(Dispatchers.Default).launch {
            plugin.mutexCameras.withLock {
                val playerName = event.player.name

                plugin.cameras.forEach {
                    if (it.name == playerName) {
                        CameraJoinEvent.EVENT.post(CameraJoinEvent(it))
                        return@launch
                    }
                }

                cn.cutemc.autostreamingassistant.bukkit.events.PlayerJoinEvent.EVENT.post(
                    cn.cutemc.autostreamingassistant.bukkit.events.PlayerJoinEvent(
                        event.player
                    )
                )
            }
        }

        val player = event.player

        player.scoreboard = plugin.server.scoreboardManager!!.newScoreboard.apply {
            registerNewTeam("camera").apply {
                plugin.server.onlinePlayers.forEach {
                    addEntry(it.name)
                }
                setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.FOR_OWN_TEAM)
            }
        }
    }
}

