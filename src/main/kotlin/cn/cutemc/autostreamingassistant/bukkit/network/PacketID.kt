package cn.cutemc.autostreamingassistant.bukkit.network

object PacketID {

    const val REQUEST_STATUS = "autostreamingassistant:request_status"
    const val REQUEST_BIND_STATUS = "autostreamingassistant:request_bind_status"
    const val CLIENT_STATUS = "autostreamingassistant:client_status"
    const val BIND_STATUS = "autostreamingassistant:bind_status"

    const val BIND_CAMERA = "autostreamingassistant:bind_camera"
    const val UNBIND_CAMERA = "autostreamingassistant:unbind_camera"

    const val BIND_CAMERA_RESULT = "autostreamingassistant:bind_camera_result"
    const val UNBIND_CAMERA_RESULT = "autostreamingassistant:unbind_camera_result"

    const val MANUAL_BIND_CAMERA = "autostreamingassistant:manual_bind_camera"

}