package cn.cutemc.autostreamingassistant.bukkit.network.messagings.events

import cn.cutemc.autostreamingassistant.bukkit.network.UnbindCameraResponsePacket
import com.google.common.eventbus.EventBus
import org.bukkit.entity.Player

data class UnbindCameraResponsePacketEvent(val player: Player, val packet: UnbindCameraResponsePacket) {
    companion object {
        val EVENT = EventBus("UNBIND_CAMERA_RESPONSE")
    }
}
