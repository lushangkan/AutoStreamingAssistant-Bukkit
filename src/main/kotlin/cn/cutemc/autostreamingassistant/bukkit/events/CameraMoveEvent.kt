package cn.cutemc.autostreamingassistant.bukkit.events

import cn.cutemc.autostreamingassistant.bukkit.camera.Camera
import org.bukkit.Location

data class CameraMoveEvent(val camera: Camera, val from: Location, val to: Location?): Event {
    companion object {
        val EVENT = EventBus<CameraMoveEvent>()
    }
}
