package cn.cutemc.autostreamingassistant.bukkit.network.messagings.events

import cn.cutemc.autostreamingassistant.bukkit.events.Event
import cn.cutemc.autostreamingassistant.bukkit.events.EventBus
import cn.cutemc.autostreamingassistant.bukkit.network.BindCameraResponsePacket
import org.bukkit.entity.Player

data class BindCameraResponsePacketEvent(val player: Player, val packet: BindCameraResponsePacket): Event {
    companion object {
        val EVENT = EventBus<BindCameraResponsePacketEvent>()
    }
}
