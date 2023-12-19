package cn.cutemc.autostreamingassistant.bukkit.events

import org.bukkit.entity.Player

data class PlayerLeaveEvent(val player: Player): Event {
    companion object {
        val EVENT = EventBus<PlayerLeaveEvent>()
    }
}
