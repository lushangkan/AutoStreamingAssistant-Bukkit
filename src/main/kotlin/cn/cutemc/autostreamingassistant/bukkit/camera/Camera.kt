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
import kotlinx.coroutines.sync.withLock
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.coroutines.resume

class Camera(val name: String) {

    private val plugin by lazy { AutoStreamingAssistant.INSTANCE }
    private val logger by lazy { plugin.logger }
    private val config by lazy { plugin.config.mainConfig }

    private var timer = Timer()

    var online = false
        private set
    var player: Player? = null
        private set
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

    var boundPlayer: Player? = null
        private set

    var autoSwitch = true
        set (value) {
            onSetAutoSwitch(value)
            field = value
        }

    private var status: ClientStatus? = null

    private var godModeDaemonID: Int? = null
    private var vanishDaemonID: Int? = null

    init {
        CameraJoinEvent.EVENT.register(this::onCameraJoin)
        CameraLeaveEvent.EVENT.register(this::onCameraLeave)
        CameraGameModeChangeEvent.EVENT.register(this::onCameraGameModeChange)
        PlayerJoinEvent.EVENT.register(this::onPlayerJoin)
        PlayerLeaveEvent.EVENT.register(this::onPlayerLeave)
        PlayerGameModeChangeEvent.EVENT.register(this::onPlayerGameModeChange)

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

                    BukkitUtils.sendPluginMessage(plugin, player, PacketID.REQUEST_STATUS, message.toByteArray(StandardCharsets.UTF_8))
                }
            }

            // 超时
            if (result == null) {
                logger.warning("Cannot get client status from $name, it seems that the client does not load the dependent Mod, please check it!")
                return@launch
            }

            // 设置在线并绑定玩家
            online = true
            this@Camera.player = player

            // 设置为上帝模式和隐身
            plugin.server.scheduler.runTask(plugin, Runnable {
                cameraProfile?.setGodMod(true)
                cameraProfile?.setVanish(true)
            })

            // 运行守护
            runGodModeDaemon()
            runVanishDaemon()

            // 延迟1秒以处理未知的Bug，即绑定数据包发送后，客户端收不到数据包
            delay(1000)

            bindRandom()
        }
    }

    /**
     * 获取摄像头绑定的玩家
     *
     * @return 绑定的玩家，如果没有绑定玩家，返回null
     */
    suspend fun getBoundPlayer(): Player? {
        if (player == null) return null

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
            return plugin.server.getPlayer(uuid)
        }

        return null
    }


    /**
     * 添加切换玩家计时器
     */
    private fun addTimer() {
        cleanTimer()
        if (autoSwitch) {
            timer.schedule(object : TimerTask() {
                override fun run() {
                    synchronized(this@Camera) {
                        CoroutineScope(Dispatchers.IO).launch {
                            if (player == null) return@launch

                            logger.info("Interval time is up, try to bind camera $name to a random player")

                            bindRandom()
                        }
                    }
                }
            }, config.switchPlayerInterval.toLong() * 60L * 1000L)
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
        if (value && !autoSwitch) {
            //
            if (boundPlayer == null) {
                // 随机绑定一个玩家或位置
                CoroutineScope(Dispatchers.IO).launch {
                    if (player == null) return@launch

                    bindRandom()
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
        plugin.mutexCameras.withLock {
            plugin.mutexConfig.withLock {
                val cameraPlayers = config.cameraNames.mapNotNull { plugin.server.getPlayer(it) }
                val boundPlayers = plugin.cameras.mapNotNull { it.boundPlayer }
                val canBoundPlayers = plugin.server.onlinePlayers.filter { it !in boundPlayers && it !in cameraPlayers && it.gameMode != GameMode.SPECTATOR }

                if (canBoundPlayers.isEmpty()) return NO_OTHER_PLAYERS

                return bindPlayer(canBoundPlayers.random())
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
        if (player == null) return CAMERA_PLAYER_NOT_ONLINE

        if (bindPlayer.gameMode == GameMode.SPECTATOR) return UNSUPPORTED_GAME_MODE

        // 设置pos为null
        fixedPos = null

        var taskDone = false

        // 传送到玩家并设置为对应的游戏模式
        plugin.server.scheduler.runTask(plugin, Runnable {
            player!!.gameMode = bindPlayer.gameMode
            player!!.teleport(bindPlayer.location)
            taskDone = true
        })

        // 等待任务完成
        while (!taskDone) {
            delay(1)
        }

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

                BukkitUtils.sendPluginMessage(plugin, player!!, PacketID.BIND_CAMERA, messageJson.toByteArray(StandardCharsets.UTF_8))
            }
        }

        var boundPlayer: Player? = null

        when(result) {
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


        this.boundPlayer = boundPlayer

        addTimer()

        return result ?: CLIENT_NOT_RESPONDING
    }

    /**
     * 解绑摄像头
     *
     * @return 解绑结果
     */
    suspend fun unbindCamera(): UnbindResult {
        if (player == null) return UnbindResult.CAMERA_PLAYER_NOT_ONLINE

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

        return result ?: UnbindResult.CLIENT_NOT_RESPONDING
    }

    /**
     * 当客户端请求绑定摄像头时调用
     *
     * @param event 客户端请求绑定摄像头事件
     */
    fun onClientManualBindCamera(event: ManualBindCameraPacketEvent) {
        CoroutineScope(Dispatchers.IO).launch {
            if (player == null) return@launch

            val bindPlayer = plugin.server.getPlayer(event.packet.playerUuid)

            if (bindPlayer == null) {
                logger.warning("Cannot find player $event.packet.playerUuid, please check the client log")
                logger.warning("Force bind camera to a new player")
                bindRandomPlayer()
                return@launch
            }

            logger.info("Camera $name manual bind to ${bindPlayer.name}")

            bindPlayer(bindPlayer)
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

        if (player == null) return CAMERA_PLAYER_NOT_ONLINE

        val result = unbindCamera()

        if (result != UnbindResult.SUCCESS && result != UnbindResult.NOT_BOUND_CAMERA) return result

        val world = plugin.server.getWorld(cameraPosition.world)

        if (world == null) {
            if (sendConsoleMsg) logger.warning("Cannot find world ${cameraPosition.world}, please check config.yml")
            return WORLD_IS_NULL
        }

        val loc = Location(world, cameraPosition.x, cameraPosition.y, cameraPosition.z, cameraPosition.yaw, cameraPosition.pitch)

        plugin.server.scheduler.runTask(plugin, Runnable {
            player!!.gameMode = GameMode.SPECTATOR
            player!!.teleport(loc)
        })

        fixedPos = cameraPosition

        boundPlayer = null

        addTimer()

        return SUCCESS
    }

    /**
     * 显示一个随机位置
     */
    suspend fun bindRandomPos() {
        plugin.mutexCameras.withLock {
            if (player == null) return

            val onlineCameras = AutoStreamingAssistant.INSTANCE.cameras.filter { it.isOnline() && it != this }

            val allLocs = config.fixedCameraPosition.clone()

            if (allLocs.isEmpty()) {
                logger.warning("Cannot find a random position, please check config.yml")
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
    }

    suspend fun bindRandom() {
        if (bindRandomPlayer() !== SUCCESS) bindRandomPos()
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

    private fun onCameraGameModeChange(event: CameraGameModeChangeEvent) {
        if (player == null) return

        if (fixedPos != null) {
            player!!.gameMode = GameMode.SPECTATOR
        } else if (boundPlayer != null && boundPlayer!!.gameMode == GameMode.SPECTATOR) {
            CoroutineScope(Dispatchers.Default).launch {
                bindRandom()
            }
        } else if (boundPlayer != null) {
            player!!.gameMode = boundPlayer!!.gameMode
        }
    }

    private fun onPlayerGameModeChange(event: PlayerGameModeChangeEvent) {
        CoroutineScope(Dispatchers.Default).launch {
            if (player == null) return@launch

            if (event.player == boundPlayer) {
                // 如果玩家是摄像头绑定的玩家
                if (event.newGameMode == GameMode.SPECTATOR) {
                    bindRandom()
                    return@launch
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

    /**
     * 运行上帝模式守护进程
     */
    private fun runGodModeDaemon() {
        godModeDaemonID = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            if (player == null || cameraProfile == null || !online) return@Runnable

            try {
                if (!cameraProfile!!.isGodModOn()) {
                    cameraProfile!!.setGodMod(true)
                }
            } catch (e: Exception) {
                if (online) {
                    e.printStackTrace()
                }
            }
        }, 0L, 20L).taskId
    }

    /**
     * 运行隐身守护进程
     */
    private fun runVanishDaemon() {
        vanishDaemonID = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            if (player == null || cameraProfile == null || !online) return@Runnable

            try {
                if (!cameraProfile!!.isVanished()) {
                    cameraProfile!!.setVanish(true)
                }
            } catch (e: Exception) {
                if (online) {
                    e.printStackTrace()
                }
            }
        }, 0L, 20L).taskId
    }

    /**
     * 停止上帝模式守护进程
     */
    private fun stopGodModeDaemon() {
        if (godModeDaemonID != null) {
            plugin.server.scheduler.cancelTask(godModeDaemonID!!)
            godModeDaemonID = null
        }
    }

    private fun stopVanishDaemon() {
        if (vanishDaemonID != null) {
            plugin.server.scheduler.cancelTask(vanishDaemonID!!)
            vanishDaemonID = null
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
            if (event.camera == this) {
                cleanTimer()

                stopGodModeDaemon()
                stopVanishDaemon()

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

