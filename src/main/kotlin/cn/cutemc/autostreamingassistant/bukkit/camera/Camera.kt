package cn.cutemc.autostreamingassistant.bukkit.camera

import cn.cutemc.autostreamingassistant.bukkit.AutoStreamingAssistant
import cn.cutemc.autostreamingassistant.bukkit.config.CameraPosition
import cn.cutemc.autostreamingassistant.bukkit.events.CameraJoinEvent
import cn.cutemc.autostreamingassistant.bukkit.events.CameraLeaveEvent
import cn.cutemc.autostreamingassistant.bukkit.events.PlayerJoinEvent
import cn.cutemc.autostreamingassistant.bukkit.events.PlayerLeaveEvent
import cn.cutemc.autostreamingassistant.bukkit.network.*
import cn.cutemc.autostreamingassistant.bukkit.network.BindResult.*
import cn.cutemc.autostreamingassistant.bukkit.network.messagings.events.*
import cn.cutemc.autostreamingassistant.bukkit.network.messagings.listeners.*
import cn.cutemc.autostreamingassistant.bukkit.utils.BukkitUtils
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.gson.Gson
import kotlinx.coroutines.*
import org.bukkit.Location
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.concurrent.schedule
import kotlin.coroutines.resume

class Camera(val name: String) {

    private var timer = Timer()

    private val plugin by lazy { AutoStreamingAssistant.INSTANCE }
    private val config by lazy { plugin.config.mainConfig }
    var online = false
        private set
    var player: CraftPlayer? = null
        private set
    var fixedPos: CameraPosition? = null
        private set

    var autoSwitch = true
        set (value) {
            onSetAutoSwitch(value)
            field = value
        }

    var boundPlayer: CraftPlayer? = null
        private set (value) {
            onSetBoundPlayer()
            field = value
        }

    private var status: ClientStatus? = null

    init {
        CameraJoinEvent.EVENT.register(this::onCameraJoin)
        CameraLeaveEvent.EVENT.register(this::onCameraLeave)
        PlayerJoinEvent.EVENT.register(this::onPlayerJoin)
        PlayerLeaveEvent.EVENT.register(this::onPlayerLeave)

        ManualBindCameraPacketEvent.EVENT.register(this::onClientManualBindCamera)
    }

    /**
     * 当摄像头加入服务器时调用
     * 这个方法会先发送一个请求客户端状态的消息，然后等待客户端回应
     * 如果客户端回应了，会检查客户端绑定的玩家，如果没有绑定玩家，会随机绑定一个玩家
     *
     * @param player 摄像头所属的玩家
     */
    private fun onCameraJoin(event: CameraJoinEvent) {
        CoroutineScope(Dispatchers.Default).launch {
            val player = event.player

            if (player !is CraftPlayer) throw IllegalStateException("Player is not CraftPlayer!")

            val result = withTimeoutOrNull(config.networkTimeout.toLong() * 1000L) {
                suspendCancellableCoroutine<ClientStatus> { cont ->
                    val callback = { event: ClientStatusPacketEvent ->
                        if (event.player.uniqueId == player.uniqueId) {
                            ClientStatusPacketEvent.EVENT.unregister(this)
                            if (cont.isActive) cont.resume(event.packet.status)
                        }
                    }

                    ClientStatusPacketEvent.EVENT.register(callback)

                    cont.invokeOnCancellation { ClientStatusPacketEvent.EVENT.unregister(callback) }

                    val message = Gson().toJson(mapOf("version" to plugin.description.version))

                    BukkitUtils.sendPluginMessage(plugin, player, PacketID.REQUEST_STATUS, message.toByteArray(StandardCharsets.UTF_8))
                }
            }

            if (result == null) {
                plugin.logger.warning("Cannot get client status from $name, it seems that the client does not load the dependent Mod, please check it!")
                return@launch
            }

            online = true
            this@Camera.player = player

            getBoundPlayer() ?: bindRandomPlayer()
        }
    }

    /**
     * 获取摄像头绑定的玩家
     *
     * @return 绑定的玩家，如果没有绑定玩家，返回null
     */
    suspend fun getBoundPlayer(): CraftPlayer? {
        if (player == null) throw IllegalStateException("Camera is not online!")

        val uuid = withTimeoutOrNull(config.networkTimeout.toLong() * 1000L) {
            return@withTimeoutOrNull suspendCancellableCoroutine<UUID> { cont ->
                val callback = { event: BindStatusPacketEvent ->
                    if (event.player.uniqueId == player!!.uniqueId) {
                        BindStatusPacketEvent.EVENT.unregister(this)
                        if (cont.isActive) cont.resume(event.packet.playerUuid)
                    }
                }

                BindStatusPacketEvent.EVENT.register(callback)

                cont.invokeOnCancellation { BindStatusPacketEvent.EVENT.unregister(callback) }

                BukkitUtils.sendPluginMessage(plugin, player!!, PacketID.REQUEST_BIND_STATUS, ByteArray(0))
            }
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
        if (autoSwitch) {
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
    private fun onSetAutoSwitch(value: Boolean) {
        if (value && !autoSwitch) {
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
        } else if (!value && autoSwitch) {
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

        unbindCamera();

        player!!.teleport(bindPlayer.location)

        delay(5000) // TODO 将延迟添加到配置文件 添加数据包以获取客户端加载玩家列表

        val result = withTimeoutOrNull(config.networkTimeout.toLong() * 1000L) {

            return@withTimeoutOrNull suspendCancellableCoroutine { cont ->
                val callback = { event: BindCameraResponsePacketEvent ->
                    if (event.player.uniqueId == player!!.uniqueId) {
                        BindCameraResponsePacketEvent.EVENT.unregister(this)
                        if (cont.isActive) cont.resume(event.packet.result)
                    }
                }

                BindCameraResponsePacketEvent.EVENT.register(callback)

                cont.invokeOnCancellation { BindCameraResponsePacketEvent.EVENT.unregister(callback) }

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
                if (autoSwitch) plugin.logger.warning("Will try to rebind the camera at the next interval.")
            }
            NO_OTHER_PLAYERS -> {
            }
            NOT_FOUND_PLAYER -> {
                plugin.logger.warning("Can't find player, maybe the player quit when they were ready to bind, if this warning happens multiple times in a row, please feedback this issue!")
                if (autoSwitch) plugin.logger.warning("Will try to rebind the camera at the next interval.")
            }
            NOT_AT_NEAR_BY -> {
                plugin.logger.warning("The random player is not at near by, it seems that the wait time for the camera to load the entity is too short, please try increasing the wait time")
                if (autoSwitch) plugin.logger.warning("Will try to rebind the camera at the next interval.")
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
                if (autoSwitch) plugin.logger.warning("Will try to rebind the camera at the next interval.")
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

        val result = withTimeoutOrNull(config.networkTimeout.toLong() * 1000L) {
            return@withTimeoutOrNull suspendCancellableCoroutine { cont ->
                val callback = { event: UnbindCameraResponsePacketEvent ->
                    if (event.player.uniqueId == player!!.uniqueId) {
                        UnbindCameraResponsePacketEvent.EVENT.unregister(this)
                        if (cont.isActive) cont.resume(event.packet.result)
                    }
                }

                UnbindCameraResponsePacketEvent.EVENT.register(callback)

                cont.invokeOnCancellation { UnbindCameraResponsePacketEvent.EVENT.unregister(callback) }

                BukkitUtils.sendPluginMessage(plugin, player!!, PacketID.UNBIND_CAMERA, ByteArray(0))
            }
        }

        if (result != UnbindResult.SUCCESS) {
            plugin.logger.warning("Cannot unbind camera $name, please check the client log")
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
     * 显示一个固定的位置
     *
     * @param cameraPosition 要显示的位置
     * @return 绑定结果, 类型UnbindResult或者BindResult
     */
    suspend fun bindFixedPos(cameraPosition: CameraPosition): Any {
        plugin.logger.info("Camera $name show fixed pos")

        if (player == null) throw IllegalStateException("Camera is not online!")

        val result = unbindCamera()

        if (result != UnbindResult.SUCCESS) return result

        val world = plugin.server.getWorld(cameraPosition.world)

        if (world == null) {
            plugin.logger.warning("Cannot find world ${cameraPosition.world}, please check config.yml")
            return WORLD_IS_NULL
        }

        val loc = Location(world, cameraPosition.x, cameraPosition.y, cameraPosition.z, cameraPosition.yaw, cameraPosition.pitch)

        player!!.teleport(loc)

        fixedPos = cameraPosition

        boundPlayer = null

        return SUCCESS
    }

    /**
     * 显示一个随机位置
     */
    suspend fun bindRandomPos() {
        if (player == null) throw IllegalStateException("Camera is not online!")

        val result = unbindCamera()

        if (result != UnbindResult.SUCCESS) return

        val onlineCameras = AutoStreamingAssistant.INSTANCE.cameras.filter { it.isOnline() && it != this }

        val allLocs = config.fixedCameraPosition

        if (allLocs.isEmpty()) {
            plugin.logger.warning("Cannot find a random position, please check config.yml")
            return
        }

        val showingLocs = onlineCameras.mapNotNull { it.fixedPos }

        // 计数，以找出最少的那个位置
        val countMap = mutableMapOf<CameraPosition, Int>()
        allLocs.forEach {
            countMap[it] = 0
        }
        showingLocs.forEach {
            countMap[it] = countMap[it]!! + 1
        }

        val minLoc = countMap.minByOrNull { it.value }?.key

        bindFixedPos(minLoc!!)
    }

    /**
     * 当摄像头玩家离开服务器时调用
     */
    suspend fun onBoundPlayerLeaveServer() {
        if (player == null) return

        if (bindRandomPlayer() != SUCCESS) {
            bindRandomPos()
        }
    }

    private fun onPlayerLeave(event: PlayerLeaveEvent) {
        CoroutineScope(Dispatchers.Default).launch {
            if (event.player == boundPlayer) onBoundPlayerLeaveServer()
        }
    }

    /**
     * 当有新玩家加入服务器时调用
     */
    private fun onPlayerJoin(event: PlayerJoinEvent) {
        CoroutineScope(Dispatchers.Default).launch {
            if (player == null) return@launch

            if (autoSwitch && fixedPos != null) {
                // 如果自动切换玩家开启，并且摄像头显示的是固定位置，那么就绑定到新加入的玩家

                if (bindRandomPlayer() !== SUCCESS) {
                    bindRandomPos()
                }
            }
        }
    }

    /**
     * 清除所有计时器
     */
    private fun cleanTimer() {
        timer.cancel()
        timer = Timer()
    }

    private fun onCameraLeave(event: CameraLeaveEvent) {
        synchronized(this) {
            if (event.player == boundPlayer) {
                cleanTimer()

                online = false
                player = null
                boundPlayer = null
                status = null
                fixedPos = null
            }
        }
    }

    fun isOnline() = online

}

