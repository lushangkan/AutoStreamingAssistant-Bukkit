package cn.cutemc.autostreamingassistant.bukkit.events

import org.bukkit.entity.Player

data class PlayerJoinEvent(val player: Player): Event {
    companion object {
        val EVENT = EventBus<PlayerJoinEvent>()
    }
}