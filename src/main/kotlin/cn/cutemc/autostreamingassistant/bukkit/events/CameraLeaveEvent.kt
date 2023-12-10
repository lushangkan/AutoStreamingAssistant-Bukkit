package cn.cutemc.autostreamingassistant.bukkit.events

import com.google.common.eventbus.EventBus
import org.bukkit.entity.Player

data class CameraLeaveEvent(val player: Player) {
    companion object {
        val EVENT = EventBus("CAMERA_LEAVE_EVENT")
    }
}