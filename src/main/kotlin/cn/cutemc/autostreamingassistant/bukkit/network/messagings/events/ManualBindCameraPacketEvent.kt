package cn.cutemc.autostreamingassistant.bukkit.network.messagings.events

import cn.cutemc.autostreamingassistant.bukkit.network.ManualBindCameraPacket
import com.google.common.eventbus.EventBus
import org.bukkit.entity.Player

data class ManualBindCameraPacketEvent(val player: Player, val packet: ManualBindCameraPacket) {
    companion object {
        val EVENT = EventBus("MANUAL_BIND_CAMERA")
    }
}
