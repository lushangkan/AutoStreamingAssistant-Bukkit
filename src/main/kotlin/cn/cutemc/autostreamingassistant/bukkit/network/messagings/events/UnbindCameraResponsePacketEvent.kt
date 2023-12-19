package cn.cutemc.autostreamingassistant.bukkit.network.messagings.events

import cn.cutemc.autostreamingassistant.bukkit.events.Event
import cn.cutemc.autostreamingassistant.bukkit.events.EventBus
import cn.cutemc.autostreamingassistant.bukkit.network.UnbindCameraResponsePacket
import org.bukkit.entity.Player

data class UnbindCameraResponsePacketEvent(val player: Player, val packet: UnbindCameraResponsePacket): Event {
    companion object {
        val EVENT = EventBus<UnbindCameraResponsePacketEvent>()
    }
}
