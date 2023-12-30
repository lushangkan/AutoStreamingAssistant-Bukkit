package cn.cutemc.autostreamingassistant.bukkit.camera

import cn.cutemc.autostreamingassistant.bukkit.AutoStreamingAssistant
import cn.cutemc.autostreamingassistant.bukkit.ManagePluginType.CMI
import cn.cutemc.autostreamingassistant.bukkit.ManagePluginType.ESSENTIALS
import cn.cutemc.autostreamingassistant.bukkit.config.CameraPosition
import cn.cutemc.autostreamingassistant.bukkit.events.*
import cn.cutemc.autostreamingassistant.bukkit.network.*
import cn.cutemc.autostreamingassistant.bukkit.network.BindResult.*
import cn.cutemc.autostreamingassistant.bukkit.network.messagings.events.*
import cn.cutemc.autostreamingassistant.bukkit.utils.BukkitUtils
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitScheduler
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.concurrent.schedule
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume

class Camera(val name: String) {

    private val plugin by lazy { AutoStreamingAssistant.INSTANCE }
    private val logger by lazy { plugin.logger }
    private val config by lazy { plugin.config.mainConfig }

    private var timer = Timer()

    private val timerMutex = Mutex()

    var online = false
        private set
    private val onlineMutex = Mutex()

    var player: Player? = null
        private set
    private val playerMutex = Mutex()

    val cameraProfile: CameraProfile? by lazy {
        val type = BukkitUtils.getManagePluginType()

        when(type) {
            ESSENTIALS -> EssentialsCameraProfile(player!!.uniqueId)
            CMI -> CMICameraProfile(player!!.uniqueId)
            else -> null
        }
    }

    var fixedPos: CameraPosition? = null
        private set
    private val fixedPosMutex = Mutex()

    var boundPlayer: Player? = null
        private set
    private val boundPlayerMutex = Mutex()

    var autoSwitch = true
        set (value) {
            onSetAutoSwitch(value)
            field = value
        }
    private val autoSwitchMutex = Mutex()

    private var status: ClientStatus? = null
    private val statusMutex = Mutex()

    private var godModeDaemonID: Int? = null
    private var godModeDaemonMutex = Mutex()
    private var vanishDaemonID: Int? = null
    private var vanishDaemonMutex = Mutex()

    init {
        CameraJoinEvent.EVENT.register(this::onCameraJoin)
        CameraLeaveEvent.EVENT.register(this::onCameraLeave)
        CameraGameModeChangeEvent.EVENT.register(this::onCameraGameModeChange)
        CameraMoveEvent.EVENT.register(this::onCameraMove)
        PlayerJoinEvent.EVENT.register(this::onPlayerJoin)
        PlayerLeaveEvent.EVENT.register(this::onPlayerLeave)
        PlayerGameModeChangeEvent.EVENT.register(this::onPlayerGameModeChange)
        PlayerMoveEvent.EVENT.register(this::onPlayerMove)

        ManualBindCameraPacketEvent.EVENT.register(this::onClientManualBindCamera)
    }

    /**
     * 当摄像头加入服务器时调用
     * 这个方法会先发送一个请求客户端状态的消息，然后等待客户端回应
     * 如果客户端回应了，会检查客户端绑定的玩家，如果没有绑定玩家，会随机绑定一个玩家
     */
    private fun onCameraJoin(event: CameraJoinEvent) {
        CoroutineScope(Dispatchers.Default).launch {
            if (event.camera != this@Camera) return@launch
            val player = plugin.server.getPlayer(name) ?: return@launch

            // 请求客户端状态
            val result = withTimeoutOrNull(config.networkTimeout.toLong() * 1000L) {
                suspendCancellableCoroutine { cont ->
                    val callback = { event: ClientStatusPacketEvent ->
                        if (event.player.uniqueId == player.uniqueId) {
                            if (cont.isActive) cont.resume(event.packet.status)
                        }
                    }
                    ClientStatusPacketEvent.EVENT.registerOnce(callback)
                    cont.invokeOnCancellation { ClientStatusPacketEvent.EVENT.unregister(callback) }
                    val message = Gson().toJson(mapOf("version" to plugin.description.version))
                    BukkitUtils.sendPluginMessage(
                        plugin,
                        player,
                        PacketID.REQUEST_STATUS,
                        message.toByteArray(StandardCharsets.UTF_8)
                    )
                }
            }

            // 超时
            if (result == null) {
                logger.warning("Cannot get client status from $name, it seems that the client does not load the dependent Mod, please check it!")
                return@launch
            }

            // 设置在线并绑定玩家
            playerMutex.safeWithLock {
                onlineMutex.safeWithLock {
                    statusMutex.safeWithLock {
                        this@Camera.player = player
                        online = true
                        status = result

                        // 设置为上帝模式和隐身并禁止碰撞
                        plugin.server.scheduler.runBlockTask(plugin, Runnable {
                            cameraProfile?.setGodMod(true)
                            cameraProfile?.setVanish(true)
                            player.isCollidable = false
                        })

                        // 运行守护
                        runGodModeDaemon()
                        runVanishDaemon()

                        // 延迟1秒以处理未知的Bug，即绑定数据包发送后，客户端收不到数据包
                        delay(1000)
                        bindRandom()
                    }

                }

            }
        }
    }

    /**
     * 获取摄像头绑定的玩家
     *
     * @return 绑定的玩家，如果没有绑定玩家，返回null
     */
    suspend fun getBoundPlayer(): Player? {
        return playerMutex.safeWithLock {
            if (player == null) return@safeWithLock null

            val uuid = withTimeoutOrNull(config.networkTimeout.toLong() * 1000L) {
                return@withTimeoutOrNull suspendCancellableCoroutine<UUID?> { cont ->
                    val callback = { event: BindStatusPacketEvent ->
                        if (event.player.uniqueId == player!!.uniqueId) {
                            if (cont.isActive && online) cont.resume(event.packet.playerUuid)
                        }
                    }
                    BindStatusPacketEvent.EVENT.registerOnce(callback)
                    cont.invokeOnCancellation { BindStatusPacketEvent.EVENT.unregister(callback) }
                    BukkitUtils.sendPluginMessage(plugin, player!!, PacketID.REQUEST_BIND_STATUS, ByteArray(0))
                }
            }

            if (uuid != null) {
                addTimer()
                return@safeWithLock plugin.server.getPlayer(uuid)
            }

            return@safeWithLock null
        }
    }


    /**
     * 添加切换玩家计时器
     */
    private fun addTimer() {
        CoroutineScope(Dispatchers.Default).launch {
            cleanTimer()
            autoSwitchMutex.safeWithLock {
                if (autoSwitch) {
                    timerMutex.safeWithLock {
                        timer.schedule(config.switchPlayerInterval.toLong() * 60L * 1000L) {
                            CoroutineScope(Dispatchers.Default).launch {
                                playerMutex.safeWithLock {
                                    if (player == null) return@safeWithLock
                                    logger.info("Interval time is up, try to bind camera $name to a random player")
                                    bindRandom()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 当设置自动切换玩家时调用
     * 如果设置为开启，但之前是关闭的，并且没有绑定玩家，会随机绑定一个玩家或固定位置，如果已经绑定了玩家，会添加计时器
     * 如果设置为关闭，但之前是开启的，会取消计时器
     *
     * @param value 自动切换玩家的值
     */
    private fun onSetAutoSwitch(value: Boolean) {
        CoroutineScope(Dispatchers.Default).launch {
            autoSwitchMutex.safeWithLock {
                if (value && !autoSwitch) {
                    boundPlayerMutex.safeWithLock {
                        if (boundPlayer == null) {
                            // 随机绑定一个玩家或位置
                            CoroutineScope(Dispatchers.Default).launch {
                                if (player == null) return@launch

                                bindRandom()
                            }
                        } else {
                            // 添加计时器
                            addTimer()
                        }
                    }
                } else if (!value && autoSwitch) {
                    // 取消计时器
                    cleanTimer()
                }

                return@safeWithLock
            }

        }
    }

    /**
     * 绑定一个随机玩家
     *
     * @return 绑定结果
     */
    suspend fun bindRandomPlayer(): BindResult {
        return plugin.mutexConfig.safeWithLock {
            return@safeWithLock plugin.mutexCameras.safeWithLock {
                val cameraPlayers = config.cameraNames.mapNotNull { plugin.server.getPlayer(it) }
                val boundPlayers = plugin.cameras.mapNotNull { it.boundPlayer }
                val canBoundPlayers = plugin.server.onlinePlayers.filter { it !in boundPlayers && it !in cameraPlayers && it.gameMode != GameMode.SPECTATOR }
                if (canBoundPlayers.isEmpty()) return@safeWithLock NO_OTHER_PLAYERS
                return@safeWithLock bindPlayer(canBoundPlayers.random())
            }
        }
    }

    /**
     * 绑定一个玩家
     *
     * @param bindPlayer 要绑定的玩家
     * @return 绑定结果
     */
    suspend fun bindPlayer(bindPlayer: Player, sendConsoleMsg: Boolean = true): BindResult {
        return playerMutex.safeWithLock {
            if (player == null) return@safeWithLock CAMERA_PLAYER_NOT_ONLINE

            if (bindPlayer.gameMode == GameMode.SPECTATOR) return@safeWithLock UNSUPPORTED_GAME_MODE

            fixedPosMutex.safeWithLock {
                // 设置pos为null
                fixedPos = null

                // 传送到玩家并设置为对应的游戏模式
                plugin.server.scheduler.runBlockTask(plugin, Runnable {
                    player!!.gameMode = bindPlayer.gameMode
                    player!!.teleport(bindPlayer.location)
                })

                // 请求客户端绑定到玩家
                val result = withTimeoutOrNull(config.networkTimeout.toLong() * 1000L) {

                    return@withTimeoutOrNull suspendCancellableCoroutine { cont ->
                        val callback = { event: BindCameraResponsePacketEvent ->
                            if (event.player.uniqueId == player!!.uniqueId) {
                                if (cont.isActive && online) cont.resume(event.packet.result)
                            }
                        }

                        BindCameraResponsePacketEvent.EVENT.registerOnce(callback)

                        cont.invokeOnCancellation { BindCameraResponsePacketEvent.EVENT.unregister(callback) }

                        val message = BindCameraPacket(bindPlayer.uniqueId)
                        val jackson = jacksonObjectMapper()
                        val messageJson = jackson.writeValueAsString(message)

                        BukkitUtils.sendPluginMessage(
                            plugin,
                            player!!,
                            PacketID.BIND_CAMERA,
                            messageJson.toByteArray(StandardCharsets.UTF_8)
                        )
                    }
                }

                var boundPlayer: Player? = null

                when (result) {
                    CLIENT_NOT_RESPONDING -> {
                        if (sendConsoleMsg) logger.warning("Cannot bind camera $name to a random player, please check network connection, or add more timeouts in config.yml")
                        if (autoSwitch && sendConsoleMsg) logger.warning("Will try to rebind the camera at the next interval.")
                    }

                    NO_OTHER_PLAYERS -> {
                    }

                    CAMERA_PLAYER_NOT_ONLINE -> {
                        if (sendConsoleMsg) logger.warning("Camera $name is not online!")
                    }

                    NOT_FOUND_PLAYER -> {
                        if (sendConsoleMsg) logger.warning("Can't find player, maybe the player quit when they were ready to bind, if this warning happens multiple times in a row, please feedback this issue!")
                        if (autoSwitch && sendConsoleMsg) logger.warning("Will try to rebind the camera at the next interval.")
                    }

                    NOT_AT_NEAR_BY -> {
                        if (sendConsoleMsg) logger.warning("The random player is not at near by, it seems that the wait time for the camera to load the entity is too short, please try increasing the wait time")
                        if (autoSwitch && sendConsoleMsg) logger.warning("Will try to rebind the camera at the next interval.")
                    }

                    WORLD_IS_NULL -> {
                        if (sendConsoleMsg) logger.warning("Camera $name error, please check the client log")
                        player!!.kickPlayer("Camera $name error, please check the client log")
                    }

                    PLAYER_IS_NULL -> {
                        if (sendConsoleMsg) logger.warning("Camera $name error, please check the client log")
                        player!!.kickPlayer("Camera $name error, please check the client log")
                    }

                    UNSUPPORTED_GAME_MODE -> {
                        if (sendConsoleMsg) logger.warning("Unsupported game mode ${bindPlayer.gameMode}, please check config.yml")
                        if (autoSwitch && sendConsoleMsg) logger.warning("Will try to rebind the camera at the next interval.")
                    }

                    SUCCESS -> {
                        if (sendConsoleMsg) logger.info("Camera $name bind to ${bindPlayer.name}")
                        boundPlayer = bindPlayer
                    }

                    null -> {
                        if (sendConsoleMsg) logger.warning("Cannot bind camera $name to a random player, please check network connection, or add more timeouts in config.yml")
                        if (autoSwitch && sendConsoleMsg) logger.warning("Will try to rebind the camera at the next interval.")
                    }
                }

                return@safeWithLock boundPlayerMutex.safeWithLock {
                    this.boundPlayer = boundPlayer

                    addTimer()

                    return@safeWithLock result ?: CLIENT_NOT_RESPONDING
                }
            }
        }
    }

    /**
     * 解绑摄像头
     *
     * @return 解绑结果
     */
    suspend fun unbindCamera(): UnbindResult {
        return playerMutex.safeWithLock {
            if (player == null) return@safeWithLock UnbindResult.CAMERA_PLAYER_NOT_ONLINE

            return@safeWithLock boundPlayerMutex.safeWithLock {
                return@safeWithLock fixedPosMutex.safeWithLock {
                    if (boundPlayer != null && fixedPos == null) {
                        val result = withTimeoutOrNull(config.networkTimeout.toLong() * 1000L) {
                            return@withTimeoutOrNull suspendCancellableCoroutine { cont ->
                                val callback = { event: UnbindCameraResponsePacketEvent ->
                                    if (event.player.uniqueId == player!!.uniqueId) {
                                        if (cont.isActive && online) cont.resume(event.packet.result)
                                    }
                                }

                                UnbindCameraResponsePacketEvent.EVENT.registerOnce(callback)

                                cont.invokeOnCancellation { UnbindCameraResponsePacketEvent.EVENT.unregister(callback) }

                                BukkitUtils.sendPluginMessage(plugin, player!!, PacketID.UNBIND_CAMERA, ByteArray(0))
                            }
                        }

                        if (result != UnbindResult.SUCCESS && result != UnbindResult.NOT_BOUND_CAMERA) {
                            logger.warning("Cannot unbind camera $name, please check the client log")
                        }

                        return@safeWithLock result ?: UnbindResult.CLIENT_NOT_RESPONDING
                    } else if (fixedPos != null) {
                        fixedPos = null
                        return@safeWithLock UnbindResult.SUCCESS
                    } else {
                        return@safeWithLock UnbindResult.NOT_BOUND_CAMERA
                    }
                }
            }

        }
    }

    /**
     * 当客户端请求绑定摄像头时调用
     *
     * @param event 客户端请求绑定摄像头事件
     */
    fun onClientManualBindCamera(event: ManualBindCameraPacketEvent) {
        CoroutineScope(Dispatchers.Default).launch {
            playerMutex.safeWithLock {
                if (player == null) return@safeWithLock

                val bindPlayer = plugin.server.getPlayer(event.packet.playerUuid)

                if (bindPlayer == null) {
                    logger.warning("Cannot find player $event.packet.playerUuid, please check the client log")
                    logger.warning("Force bind camera to a new player")
                    bindRandomPlayer()
                    return@safeWithLock
                }

                logger.info("Camera $name manual bind to ${bindPlayer.name}")

                bindPlayer(bindPlayer)
            }
        }
    }

    /**
     * 显示一个固定的位置
     *
     * @param cameraPosition 要显示的位置
     * @return 绑定结果, 类型UnbindResult或者BindResult
     */
    suspend fun bindFixedPos(cameraPosition: CameraPosition, sendConsoleMsg: Boolean = true): Any {
        if (sendConsoleMsg) logger.info("Camera $name show fixed pos")

        return playerMutex.safeWithLock {
            if (player == null) return@safeWithLock CAMERA_PLAYER_NOT_ONLINE

            val result = unbindCamera()

            if (result != UnbindResult.SUCCESS && result != UnbindResult.NOT_BOUND_CAMERA) return@safeWithLock result

            if (plugin.server.getWorld(cameraPosition.world) == null) {
                if (sendConsoleMsg) logger.warning("Cannot find world ${cameraPosition.world}, please check config.yml")
                return@safeWithLock WORLD_IS_NULL
            }

            val loc = cameraPosition.toLocation()

            plugin.server.scheduler.runTask(plugin, Runnable {
                player!!.gameMode = GameMode.SPECTATOR
                player!!.teleport(loc)
            })

            return@safeWithLock fixedPosMutex.safeWithLock {
                return@safeWithLock boundPlayerMutex.safeWithLock {
                    fixedPos = cameraPosition
                    boundPlayer = null

                    addTimer()

                    return@safeWithLock SUCCESS
                }
            }
        }
    }

    /**
     * 显示一个随机位置
     */
    suspend fun bindRandomPos() {
        playerMutex.safeWithLock {
            if (player == null) return@safeWithLock

            plugin.mutexCameras.safeWithLock {
                val onlineCameras = AutoStreamingAssistant.INSTANCE.cameras.filter { it.isOnline() && it != this }

                val allLocs = config.fixedCameraPosition.clone()

                if (allLocs.isEmpty()) {
                    logger.warning("Cannot find a random position, please check config.yml")
                    return@safeWithLock
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
        }
    }

    suspend fun bindRandom() {
        if (bindRandomPlayer() !== SUCCESS) bindRandomPos()
    }

    /**
     * 当摄像头玩家离开服务器时调用
     */
    suspend fun onBoundPlayerLeaveServer() {
        playerMutex.safeWithLock {
            if (player == null) return@safeWithLock

            if (bindRandomPlayer() != SUCCESS) {
                bindRandomPos()
            }
        }
    }

    private fun onPlayerLeave(event: PlayerLeaveEvent) {
        CoroutineScope(Dispatchers.Default).launch {
            boundPlayerMutex.safeWithLock {
                if (event.player == boundPlayer) onBoundPlayerLeaveServer()
            }
        }
    }

    /**
     * 当有新玩家加入服务器时调用
     */
    private fun onPlayerJoin(event: PlayerJoinEvent) {
        CoroutineScope(Dispatchers.Default).launch {
            playerMutex.safeWithLock {
                if (player == null) return@safeWithLock

                autoSwitchMutex.safeWithLock {
                    fixedPosMutex.safeWithLock {
                        if (autoSwitch && fixedPos != null) {
                            // 如果自动切换玩家开启，并且摄像头显示的是固定位置，那么就绑定到新加入的玩家
                            if (bindRandomPlayer() !== SUCCESS) {
                                bindRandomPos()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onCameraGameModeChange(event: CameraGameModeChangeEvent) {
        CoroutineScope(Dispatchers.Default).launch {
            playerMutex.safeWithLock {
                if (player == null) return@safeWithLock

                fixedPosMutex.safeWithLock {
                    boundPlayerMutex.safeWithLock {
                        if (fixedPos != null) {
                            plugin.server.scheduler.runBlockTask(plugin, Runnable {
                                player!!.gameMode = GameMode.SPECTATOR
                            })
                        } else if (boundPlayer != null && boundPlayer!!.gameMode == GameMode.SPECTATOR) {
                            bindRandom()
                        } else if (boundPlayer != null) {
                            plugin.server.scheduler.runBlockTask(plugin, Runnable {
                                player!!.gameMode = boundPlayer!!.gameMode
                            })
                        }
                    }
                }
            }
        }
    }

    private fun onPlayerGameModeChange(event: PlayerGameModeChangeEvent) {
        CoroutineScope(Dispatchers.Default).launch {
            playerMutex.safeWithLock {
                if (player == null) return@safeWithLock

                boundPlayerMutex.safeWithLock {
                    fixedPosMutex.safeWithLock {
                        autoSwitchMutex.safeWithLock {
                            if (event.player == boundPlayer) {
                                // 如果玩家是摄像头绑定的玩家
                                if (event.newGameMode == GameMode.SPECTATOR) {
                                    bindRandom()
                                    return@safeWithLock
                                }

                                plugin.server.scheduler.runTask(plugin, Runnable {
                                    player!!.gameMode = event.newGameMode
                                })
                            } else if (boundPlayer == null && fixedPos != null && autoSwitch && event.newGameMode != GameMode.SPECTATOR) {
                                // 如果没有绑定玩家而是显示固定位置，并且自动切换玩家开启，并且玩家不是旁观模式，那么就绑定到新的玩家
                                bindRandom()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onCameraLeave(event: CameraLeaveEvent) {
        CoroutineScope(Dispatchers.Default).launch {
            if (event.camera == this@Camera) {
                cleanTimer()
                stopGodModeDaemon()
                stopVanishDaemon()

                playerMutex.safeWithLock {
                    onlineMutex.withLock {
                        boundPlayerMutex.safeWithLock {
                            statusMutex.safeWithLock {
                                fixedPosMutex.safeWithLock {
                                    online = false
                                    player = null
                                    boundPlayer = null
                                    status = null
                                    fixedPos = null
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    private fun onCameraMove(event: CameraMoveEvent) {
        CoroutineScope(Dispatchers.Default).launch {
            if (event.camera == this@Camera) {
                fixedPosMutex.safeWithLock {
                    boundPlayerMutex.safeWithLock {
                        playerMutex.safeWithLock {
                            if (fixedPos != null && event.to != fixedPos!!.toLocation()) {
                                // 如果显示的是固定位置，并且位置不对，那么就传送到固定位置
                                plugin.server.scheduler.runBlockTask(plugin, Runnable {
                                    player!!.teleport(fixedPos!!.toLocation())
                                })
                            } else if (boundPlayer != null && event.to != boundPlayer!!.location) {
                                // 如果绑定了玩家，并且位置不对，那么就传送到玩家
                                plugin.server.scheduler.runBlockTask(plugin, Runnable {
                                    player!!.teleport(boundPlayer!!.location)
                                })
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onPlayerMove(event: PlayerMoveEvent) {
        CoroutineScope(Dispatchers.Default).launch {
            playerMutex.safeWithLock {
                boundPlayerMutex.safeWithLock {
                    if (event.player == boundPlayer) {
                        if (event.to != boundPlayer!!.location) {
                            // 如果绑定了玩家，并且位置不对，那么就传送到玩家
                            plugin.server.scheduler.runBlockTask(plugin, Runnable {
                                player!!.teleport(boundPlayer!!.location)
                            })
                        }
                    }
                }
            }
        }
    }

    /**
     * 运行上帝模式守护进程
     */
    private suspend fun runGodModeDaemon() {
        godModeDaemonID = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            CoroutineScope(Dispatchers.Default).launch {
                playerMutex.safeWithLock {
                    onlineMutex.safeWithLock {
                        if (player == null || cameraProfile == null || !online) return@safeWithLock

                        plugin.server.scheduler.runBlockTask(plugin, Runnable {
                            try {
                                if (!cameraProfile!!.isGodModOn()) {
                                    cameraProfile!!.setGodMod(true)
                                }
                            } catch (e: Exception) {
                                if (online) {
                                    e.printStackTrace()
                                }
                            }
                        })
                    }
                }
            }
        }, 0L, 20L).taskId
    }

    /**
     * 运行隐身守护进程
     */
    private suspend fun runVanishDaemon() {
        vanishDaemonMutex.safeWithLock {
            vanishDaemonID = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
                CoroutineScope(Dispatchers.Default).launch {
                    playerMutex.safeWithLock {
                        onlineMutex.safeWithLock {
                            if (player == null || cameraProfile == null || !online) return@safeWithLock

                            try {
                                if (!cameraProfile!!.isVanished()) {
                                    cameraProfile!!.setVanish(true)
                                }
                            } catch (e: Exception) {
                                if (online) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }
            }, 0L, 20L).taskId
        }
    }

    /**
     * 停止上帝模式守护进程
     */
    private suspend fun stopGodModeDaemon() {
        godModeDaemonMutex.safeWithLock {
            if (godModeDaemonID != null) {
                plugin.server.scheduler.cancelTask(godModeDaemonID!!)
                godModeDaemonID = null
            }
        }
    }

    private suspend fun stopVanishDaemon() {
        vanishDaemonMutex.safeWithLock {
            if (vanishDaemonID != null) {
                plugin.server.scheduler.cancelTask(vanishDaemonID!!)
                vanishDaemonID = null
            }
        }
    }
    

    /**
     * 清除所有计时器
     */
    private suspend fun cleanTimer() {
        timerMutex.safeWithLock {
            timer.cancel()
            timer = Timer()
        }
    }

    suspend fun isOnline(): Boolean = onlineMutex.safeWithLock { return@safeWithLock online }

}

suspend fun BukkitScheduler.runBlockTask(plugin: JavaPlugin, runnable: java.lang.Runnable) {
    var done = false
    this.runTask(plugin, Runnable {
        runnable.run()
        done = true
    })
    while (!done) {
        delay(1)
    }
}

suspend fun <T> Mutex.safeWithLock(action: suspend () -> T): T {
    if (this.holdsLock(coroutineContext)) {
        return action()
    } else {
        this.withLock(coroutineContext) {
            return action()
        }
    }
}