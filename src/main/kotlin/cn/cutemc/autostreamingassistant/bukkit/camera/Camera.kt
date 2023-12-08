package cn.cutemc.autostreamingassistant.bukkit.camera

import cn.cutemc.autostreamingassistant.bukkit.AutoStreamingAssistant
import cn.cutemc.autostreamingassistant.bukkit.listeners.network.messagings.ClientStatusPacketListener
import cn.cutemc.autostreamingassistant.bukkit.network.ClientStatus
import cn.cutemc.autostreamingassistant.bukkit.network.PacketID
import cn.cutemc.autostreamingassistant.bukkit.utils.BukkitUtils
import com.google.gson.Gson
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer
import org.bukkit.entity.Player
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.coroutines.resume

class Camera(val name: String) {

    private val timer by lazy { Timer() }

    private val plugin by lazy { AutoStreamingAssistant.INSTANCE }
    private val config by lazy { plugin.config.mainConfig }
    private var online = false
    private var player: CraftPlayer? = null
    private var boundPlayer: CraftPlayer? = null
        set (value) {
            field = value
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
    }


    fun leaveServer() {
        online = false
        player = null
    }

    fun isOnline() = online

}

