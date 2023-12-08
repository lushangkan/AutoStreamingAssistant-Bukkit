package cn.cutemc.autostreamingassistant.bukkit.camera

import cn.cutemc.autostreamingassistant.bukkit.AutoStreamingAssistant
import cn.cutemc.autostreamingassistant.bukkit.listeners.network.messagings.BindCameraResponsePacketListener
import cn.cutemc.autostreamingassistant.bukkit.listeners.network.messagings.BindStatusPacketListener
import cn.cutemc.autostreamingassistant.bukkit.listeners.network.messagings.ClientStatusPacketListener
import cn.cutemc.autostreamingassistant.bukkit.network.BindCameraPacket
import cn.cutemc.autostreamingassistant.bukkit.network.BindResult
import cn.cutemc.autostreamingassistant.bukkit.network.ClientStatus
import cn.cutemc.autostreamingassistant.bukkit.network.PacketID
import cn.cutemc.autostreamingassistant.bukkit.utils.BukkitUtils
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.gson.Gson
import kotlinx.coroutines.*
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer
import org.bukkit.entity.Player
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.concurrent.schedule
import kotlin.coroutines.resume

class Camera(val name: String) {

    private var timer = Timer()

    private val plugin by lazy { AutoStreamingAssistant.INSTANCE }
    private val config by lazy { plugin.config.mainConfig }
    private var online = false
    private var player: CraftPlayer? = null
    private var boundPlayer: CraftPlayer? = null
        set (value) {
            field = value
            onSetBoundPlayer()
        }

    private var status: ClientStatus? = null

    suspend fun joinServer(player: Player) {
        if (player !is CraftPlayer) throw IllegalStateException("Player is not CraftPlayer!")

        val result = withTimeoutOrNull(100000) {
            suspendCancellableCoroutine { cont ->
                ClientStatusPacketListener.listeners[player.uniqueId] = { clientStatus ->
                    this@Camera.status = clientStatus.status
                    if (cont.isActive) cont.resume(Unit)
                }

                cont.invokeOnCancellation { ClientStatusPacketListener.listeners.remove(player.uniqueId) }

                val message = Gson().toJson(mapOf("version" to plugin.description.version))

                BukkitUtils.sendPluginMessage(plugin, player, PacketID.REQUEST_STATUS, message.toByteArray(StandardCharsets.UTF_8))
            }
        }

        if (result == null) {
            plugin.logger.warning("Cannot get client status from $name, it seems that the client does not load the dependent Mod, please check it!")
            return
        }

        online = true
        this.player = player

        getBoundPlayer() ?: bindRandomPlayer()
    }


    private suspend fun getBoundPlayer(): CraftPlayer? {
        if (player == null) throw IllegalStateException("Camera is not online!")

        val uuid = withTimeoutOrNull(100000) {
            val uuid = suspendCancellableCoroutine { cont ->
                BindStatusPacketListener.listeners[player!!.uniqueId] = { bindStatus ->
                    if (cont.isActive) cont.resume(bindStatus.playerUuid)
                }

                BukkitUtils.sendPluginMessage(plugin, player!!, PacketID.REQUEST_BIND_STATUS, ByteArray(0))
            }

            return@withTimeoutOrNull uuid
        }

        return if (uuid != null) plugin.server.getPlayer(uuid) as CraftPlayer else null
    }

    private fun onSetBoundPlayer() {
        cleanTimer()

        timer.schedule(config.switchPlayerInterval.toLong() * 60L * 1000L) {
            synchronized(this@Camera) {
                bindRandomPlayer()
            }
        }
    }

    private fun bindRandomPlayer() {
        val cameraPlayers = config.cameraNames.mapNotNull { plugin.server.getPlayer(it) }
        val boundPlayers = plugin.cameras.mapNotNull { it.boundPlayer }
        val canBoundPlayers = plugin.server.onlinePlayers.filter { it !in boundPlayers && it !in cameraPlayers }

        if (canBoundPlayers.isEmpty()) return

        CoroutineScope(Dispatchers.Default).launch {
            bindCamera(canBoundPlayers.random() as CraftPlayer)
        }
    }

    private suspend fun bindCamera(bindPlayer: CraftPlayer): BindResult {
        if (player == null) throw IllegalStateException("Camera is not online!")

        val result = withTimeoutOrNull(100000) {
            return@withTimeoutOrNull suspendCancellableCoroutine { cont ->
                BindCameraResponsePacketListener.listeners[player!!.uniqueId] = { bindCameraResponse ->
                    if (cont.isActive) cont.resume(bindCameraResponse.result)
                }

                val message = BindCameraPacket(bindPlayer.uniqueId)
                val jackson = jacksonObjectMapper()
                val messageJson = jackson.writeValueAsString(message)

                BukkitUtils.sendPluginMessage(plugin, player!!, PacketID.BIND_CAMERA, messageJson.toByteArray(StandardCharsets.UTF_8))
            }
        }

        boundPlayer = if (result == BindResult.SUCCESS) bindPlayer else null

        return result ?: BindResult.CLIENT_NOT_RESPONDING
    }

    private fun cleanTimer() {
        timer.cancel()
        timer = Timer()
    }


    fun leaveServer() {
        synchronized(this) {
            cleanTimer()

            online = false
            player = null
        }
    }

    fun isOnline() = online

}

