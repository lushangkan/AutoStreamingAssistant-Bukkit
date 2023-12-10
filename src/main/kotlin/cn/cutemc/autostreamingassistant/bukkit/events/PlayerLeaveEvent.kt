package cn.cutemc.autostreamingassistant.bukkit.events

import com.google.common.eventbus.EventBus
import org.bukkit.entity.Player

data class PlayerLeaveEvent(val player: Player) {
    companion object {
        val EVENT = EventBus("PLAYER_LEAVE_EVENT")
    }
}
