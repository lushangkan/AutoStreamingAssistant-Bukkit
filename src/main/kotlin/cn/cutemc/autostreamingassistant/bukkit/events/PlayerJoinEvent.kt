package cn.cutemc.autostreamingassistant.bukkit.events

import com.google.common.eventbus.EventBus
import org.bukkit.entity.Player

data class PlayerJoinEvent(val player: Player) {
    companion object {
        val EVENT = EventBus("PLAYER_JOIN_EVENT")
    }
}