package cn.cutemc.autostreamingassistant.bukkit.network.messagings.events

import cn.cutemc.autostreamingassistant.bukkit.network.BindCameraResponsePacket
import com.google.common.eventbus.EventBus
import org.bukkit.entity.Player

data class BindCameraResponsePacketEvent(val player: Player, val packet: BindCameraResponsePacket) {
    companion object {
        val EVENT = EventBus("BIND_CAMERA_RESPONSE")
    }
}
