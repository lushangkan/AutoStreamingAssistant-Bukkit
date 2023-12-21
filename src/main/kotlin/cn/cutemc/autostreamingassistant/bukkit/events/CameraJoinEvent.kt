package cn.cutemc.autostreamingassistant.bukkit.events

import cn.cutemc.autostreamingassistant.bukkit.camera.Camera

data class CameraJoinEvent(val camera: Camera): Event {
    companion object {
        val EVENT = EventBus<CameraJoinEvent>()
    }
}