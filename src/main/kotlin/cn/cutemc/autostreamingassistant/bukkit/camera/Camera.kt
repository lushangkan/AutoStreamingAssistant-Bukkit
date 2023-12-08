package cn.cutemc.autostreamingassistant.bukkit.camera

import cn.cutemc.autostreamingassistant.bukkit.AutoStreamingAssistant
import cn.cutemc.autostreamingassistant.bukkit.listeners.network.messagings.BindCameraResponsePacketListener
import cn.cutemc.autostreamingassistant.bukkit.listeners.network.messagings.BindStatusPacketListener
import cn.cutemc.autostreamingassistant.bukkit.listeners.network.messagings.ClientStatusPacketListener
import cn.cutemc.autostreamingassistant.bukkit.listeners.network.messagings.UnbindCameraResponsePacketListener
import cn.cutemc.autostreamingassistant.bukkit.network.*
import cn.cutemc.autostreamingassistant.bukkit.network.BindResult.*
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

    var autoSwitchPlayer = true
        set (value) {
            onSetAutoSwitchPlayer(value)
            field = value
        }

    private var boundPlayer: CraftPlayer? = null
        set (value) {
            onSetBoundPlayer()
            field = value
        }

    private var status: ClientStatus? = null

    /**
     * 当摄像头加入服务器时调用
     * 这个方法会先发送一个请求客户端状态的消息，然后等待客户端回应
     * 如果客户端回应了，会检查客户端绑定的玩家，如果没有绑定玩家，会随机绑定一个玩家
     *
     * @param player 摄像头所属的玩家
     */
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

    /**
     * 获取摄像头绑定的玩家
     *
     * @return 绑定的玩家，如果没有绑定玩家，返回null
     */
    suspend fun getBoundPlayer(): CraftPlayer? {
        if (player == null) throw IllegalStateException("Camera is not online!")

        val uuid = withTimeoutOrNull(100000) {
            val uuid = suspendCancellableCoroutine { cont ->
                BindStatusPacketListener.listeners[player!!.uniqueId] = { bindStatus ->
                    if (cont.isActive) cont.resume(bindStatus.playerUuid)
                }

                cont.invokeOnCancellation { BindStatusPacketListener.listeners.remove(player!!.uniqueId) }

                BukkitUtils.sendPluginMessage(plugin, player!!, PacketID.REQUEST_BIND_STATUS, ByteArray(0))
            }

            return@withTimeoutOrNull uuid
        }

        return if (uuid != null) plugin.server.getPlayer(uuid) as CraftPlayer else null
    }

    /**
     * 当绑定玩家时调用
     */
    private fun onSetBoundPlayer() {
        addTimer()
    }

    /**
     * 添加切换玩家计时器
     */
    private fun addTimer() {
        cleanTimer()
        if (autoSwitchPlayer) {
            timer.schedule(config.switchPlayerInterval.toLong() * 60L * 1000L) {
                synchronized(this@Camera) {
                    CoroutineScope(Dispatchers.IO).launch {
                        if (player == null) return@launch

                        plugin.logger.info("Interval time is up, try to bind camera $name to a random player")

                        bindRandomPlayer()
                    }
                }
            }
        }
    }

    /**
     * 当设置自动切换玩家时调用
     * 如果设置为开启，但之前是关闭的，并且没有绑定玩家，会随机绑定一个玩家，如果已经绑定了玩家，会添加计时器
     * 如果设置为关闭，但之前是开启的，会取消计时器
     *
     * @param value 自动切换玩家的值
     */
    private fun onSetAutoSwitchPlayer(value: Boolean) {
        if (value && !autoSwitchPlayer) {
            //
            if (boundPlayer == null) {
                // 随机绑定一个玩家
                CoroutineScope(Dispatchers.IO).launch {
                    if (player == null) return@launch

                    bindRandomPlayer()
                }
            } else {
                // 添加计时器
                addTimer()
            }
        } else if (!value && autoSwitchPlayer) {
            // 取消计时器
            cleanTimer()
        }
    }

    /**
     * 绑定一个随机玩家
     *
     * @return 绑定结果
     */
    suspend fun bindRandomPlayer(): BindResult {
        val cameraPlayers = config.cameraNames.mapNotNull { plugin.server.getPlayer(it) }
        val boundPlayers = plugin.cameras.mapNotNull { it.boundPlayer }
        val canBoundPlayers = plugin.server.onlinePlayers.filter { it !in boundPlayers && it !in cameraPlayers }

        if (canBoundPlayers.isEmpty()) return NO_OTHER_PLAYERS

        return bindCamera(canBoundPlayers.random() as CraftPlayer)
    }

    /**
     * 绑定一个玩家
     *
     * @param bindPlayer 要绑定的玩家
     * @return 绑定结果
     */
    suspend fun bindCamera(bindPlayer: CraftPlayer): BindResult {
        if (player == null) throw IllegalStateException("Camera is not online!")

        player!!.teleport(bindPlayer.location)

        delay(5000) // TODO 将延迟添加到配置文件

        val result = withTimeoutOrNull(100000) {

            return@withTimeoutOrNull suspendCancellableCoroutine { cont ->
                BindCameraResponsePacketListener.listeners[player!!.uniqueId] = { bindCameraResponse ->
                    if (cont.isActive) cont.resume(bindCameraResponse.result)
                }

                cont.invokeOnCancellation { BindCameraResponsePacketListener.listeners.remove(player!!.uniqueId) }

                val message = BindCameraPacket(bindPlayer.uniqueId)
                val jackson = jacksonObjectMapper()
                val messageJson = jackson.writeValueAsString(message)

                BukkitUtils.sendPluginMessage(plugin, player!!, PacketID.BIND_CAMERA, messageJson.toByteArray(StandardCharsets.UTF_8))
            }
        }

        var boundPlayer: CraftPlayer? = null

        when(result) {
            CLIENT_NOT_RESPONDING -> {
                plugin.logger.warning("Cannot bind camera $name to a random player, please check network connection, or add more timeouts in config.yml")
                if (autoSwitchPlayer) plugin.logger.warning("Will try to rebind the camera at the next interval.")
            }
            NO_OTHER_PLAYERS -> {
                // TODO 将摄像头放置到一个固定的位置
            }
            NOT_FOUND_PLAYER -> {
                plugin.logger.warning("Can't find player, maybe the player quit when they were ready to bind, if this warning happens multiple times in a row, please feedback this issue!")
                if (autoSwitchPlayer) plugin.logger.warning("Will try to rebind the camera at the next interval.")
            }
            NOT_AT_NEAR_BY -> {
                plugin.logger.warning("The random player is not at near by, it seems that the wait time for the camera to load the entity is too short, please try increasing the wait time")
                if (autoSwitchPlayer) plugin.logger.warning("Will try to rebind the camera at the next interval.")
            }
            WORLD_IS_NULL -> {
                plugin.logger.warning("Camera $name error, please check the client log")
                player!!.kickPlayer("Camera $name error, please check the client log")
            }
            PLAYER_IS_NULL -> {
                plugin.logger.warning("Camera $name error, please check the client log")
                player!!.kickPlayer("Camera $name error, please check the client log")
            }
            SUCCESS -> {
                plugin.logger.info("Camera $name bind to ${bindPlayer.name}")
                boundPlayer = bindPlayer
            }
            null -> {
                plugin.logger.warning("Cannot bind camera $name to a random player, please check network connection, or add more timeouts in config.yml")
                if (autoSwitchPlayer) plugin.logger.warning("Will try to rebind the camera at the next interval.")
            }
        }

        this.boundPlayer = boundPlayer

        return result ?: CLIENT_NOT_RESPONDING
    }

    /**
     * 解绑摄像头
     *
     * @return 解绑结果
     */
    suspend fun unbindCamera(): UnbindResult {
        if (player == null) throw IllegalStateException("Camera is not online!")

        val result = withTimeoutOrNull(100000) {
            return@withTimeoutOrNull suspendCancellableCoroutine { cont ->
                UnbindCameraResponsePacketListener.listeners[player!!.uniqueId] = { result ->
                    if (cont.isActive) cont.resume(result.result)
                }

                cont.invokeOnCancellation { UnbindCameraResponsePacketListener.listeners.remove(player!!.uniqueId) }

                BukkitUtils.sendPluginMessage(plugin, player!!, PacketID.UNBIND_CAMERA, ByteArray(0))
            }
        }

        return result ?: UnbindResult.CLIENT_NOT_RESPONDING
    }

    /**
     * 当客户端请求绑定摄像头时调用
     *
     * @param playerUuid 客户端请求绑定的玩家的UUID
     */
    fun onClientManualBindCamera(playerUuid: UUID) {
        CoroutineScope(Dispatchers.IO).launch {
            if (player == null) return@launch

            val bindPlayer = plugin.server.getPlayer(playerUuid)

            if (bindPlayer == null) {
                plugin.logger.warning("Cannot find player $playerUuid, please check the client log")
                plugin.logger.warning("Force bind camera to a new player")
                bindRandomPlayer()
                return@launch
            }

            plugin.logger.info("Camera $name manual bind to ${bindPlayer.name}")

            bindCamera(bindPlayer as CraftPlayer)
        }
    }

    /**
     * 清除所有计时器
     */
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

