package cn.cutemc.autostreamingassistant.bukkit.events

import cn.cutemc.autostreamingassistant.bukkit.camera.Camera
import org.bukkit.GameMode

data class CameraGameModeChangeEvent(val camera: Camera, val newGameMode: GameMode): Event {
    companion object {
        val EVENT = EventBus<CameraGameModeChangeEvent>()
    }
}
