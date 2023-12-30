package cn.cutemc.autostreamingassistant.bukkit.events

import org.bukkit.Location
import org.bukkit.entity.Player

data class PlayerMoveEvent(val player: Player, val from: Location, val to: Location?): Event {
    companion object {
        val EVENT = EventBus<PlayerMoveEvent>()
    }
}
