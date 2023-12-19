package cn.cutemc.autostreamingassistant.bukkit.network.messagings.events

import cn.cutemc.autostreamingassistant.bukkit.events.Event
import cn.cutemc.autostreamingassistant.bukkit.events.EventBus
import cn.cutemc.autostreamingassistant.bukkit.network.ManualBindCameraPacket
import org.bukkit.entity.Player

data class ManualBindCameraPacketEvent(val player: Player, val packet: ManualBindCameraPacket): Event {
    companion object {
        val EVENT = EventBus<ManualBindCameraPacketEvent>()
    }
}
