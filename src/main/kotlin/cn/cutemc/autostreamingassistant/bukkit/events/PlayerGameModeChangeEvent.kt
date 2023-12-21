package cn.cutemc.autostreamingassistant.bukkit.events

import org.bukkit.GameMode
import org.bukkit.entity.Player

data class PlayerGameModeChangeEvent(val player: Player, val newGameMode: GameMode): Event {
    companion object {
        val EVENT = EventBus<PlayerGameModeChangeEvent>()
    }
}
