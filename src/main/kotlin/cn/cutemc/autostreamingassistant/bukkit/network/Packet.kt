package cn.cutemc.autostreamingassistant.bukkit.network

import java.util.*

data class BindStatusPacket(val playerUuid: UUID)

data class ClientStatusPacket(val status: ClientStatus, val version: String)

data class ManualBindCameraPacket(val playerUuid: UUID)

data class UnbindCameraResponsePacket(val success: UnbindResult, val result: UnbindResult)

data class BindCameraPacket(val playerUuid: UUID)
data class BindCameraResponse(val success: Boolean, val result: BindResult)