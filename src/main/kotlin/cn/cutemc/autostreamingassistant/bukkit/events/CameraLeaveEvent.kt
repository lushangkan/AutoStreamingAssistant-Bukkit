package cn.cutemc.autostreamingassistant.bukkit.events

import org.bukkit.entity.Player

data class CameraLeaveEvent(val player: Player): Event {
    companion object {
        val EVENT = EventBus<CameraLeaveEvent>()
    }
}