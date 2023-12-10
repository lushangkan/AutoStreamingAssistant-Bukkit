package cn.cutemc.autostreamingassistant.bukkit.network.messagings.events

import cn.cutemc.autostreamingassistant.bukkit.network.BindStatusPacket
import com.google.common.eventbus.EventBus
import org.bukkit.entity.Player

data class BindStatusPacketEvent(val player: Player, val packet: BindStatusPacket) {
    companion object {
        val EVENT = EventBus("BIND_STATUS")
    }
}
