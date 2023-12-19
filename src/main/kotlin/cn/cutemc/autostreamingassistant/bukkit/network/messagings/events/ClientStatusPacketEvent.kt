package cn.cutemc.autostreamingassistant.bukkit.network.messagings.events

import cn.cutemc.autostreamingassistant.bukkit.events.Event
import cn.cutemc.autostreamingassistant.bukkit.events.EventBus
import cn.cutemc.autostreamingassistant.bukkit.network.ClientStatusPacket
import org.bukkit.entity.Player

data class ClientStatusPacketEvent(val player: Player, val packet: ClientStatusPacket): Event {
    companion object {
        val EVENT = EventBus<ClientStatusPacketEvent>()
    }
}
