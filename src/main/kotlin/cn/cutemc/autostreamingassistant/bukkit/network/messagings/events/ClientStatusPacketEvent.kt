package cn.cutemc.autostreamingassistant.bukkit.network.messagings.events

import cn.cutemc.autostreamingassistant.bukkit.network.ClientStatusPacket
import com.google.common.eventbus.EventBus
import org.bukkit.entity.Player

data class ClientStatusPacketEvent(val player: Player, val packet: ClientStatusPacket) {
    companion object {
        val EVENT = EventBus("CLIENT_STATUS")
    }
}
