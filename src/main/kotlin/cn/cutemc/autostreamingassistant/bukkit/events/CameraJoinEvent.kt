package cn.cutemc.autostreamingassistant.bukkit.events

import org.bukkit.entity.Player

data class CameraJoinEvent(val player: Player): Event {
    companion object {
        val EVENT = EventBus<CameraJoinEvent>()
    }
}