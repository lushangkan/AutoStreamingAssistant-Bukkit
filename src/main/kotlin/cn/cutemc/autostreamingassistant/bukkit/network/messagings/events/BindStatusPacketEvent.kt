package cn.cutemc.autostreamingassistant.bukkit.network.messagings.events

import cn.cutemc.autostreamingassistant.bukkit.events.Event
import cn.cutemc.autostreamingassistant.bukkit.events.EventBus
import cn.cutemc.autostreamingassistant.bukkit.network.BindStatusPacket
import org.bukkit.entity.Player

data class BindStatusPacketEvent(val player: Player, val packet: BindStatusPacket): Event {
    companion object {
        val EVENT = EventBus<BindStatusPacketEvent>()
    }
}
