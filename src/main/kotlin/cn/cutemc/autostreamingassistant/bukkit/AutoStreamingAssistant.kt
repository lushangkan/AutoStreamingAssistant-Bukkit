package cn.cutemc.autostreamingassistant.bukkit

import cn.cutemc.autostreamingassistant.bukkit.camera.Camera
import cn.cutemc.autostreamingassistant.bukkit.commands.PluginCommands
import cn.cutemc.autostreamingassistant.bukkit.commands.PluginTabCompleter
import cn.cutemc.autostreamingassistant.bukkit.config.PluginConfig
import cn.cutemc.autostreamingassistant.bukkit.lang.PluginLang
import cn.cutemc.autostreamingassistant.bukkit.listeners.PlayerJoinListener
import cn.cutemc.autostreamingassistant.bukkit.listeners.PlayerQuitListener
import cn.cutemc.autostreamingassistant.bukkit.logger.PluginLogger
import cn.cutemc.autostreamingassistant.bukkit.network.PacketID
import cn.cutemc.autostreamingassistant.bukkit.network.messagings.listeners.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.bstats.bukkit.Metrics
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class AutoStreamingAssistant: JavaPlugin() {

    companion object {
        lateinit var INSTANCE: AutoStreamingAssistant
    }

    val logger by lazy { PluginLogger(super.getLogger()) }

    val config: PluginConfig = PluginConfig()
    val mutexConfig: Mutex = Mutex()

    lateinit var lang: PluginLang
    private set

    val cameras: MutableList<Camera> = mutableListOf()
    val mutexCameras: Mutex = Mutex()

    override fun onLoad() {
        INSTANCE = this
        lang = PluginLang()
        saveDefaultConfig()
        reloadConfig()
    }

    override fun onEnable() {
        logger.info(lang.getTranslation("loading.main"))
        registerBStats()

        logger.info(lang.getTranslation("loading.reg.command"))
        registerCommands()

        logger.info(lang.getTranslation("loading.reg.listeners"))
        registerListeners()

        logger.info(lang.getTranslation("loading.create.cameras"))
        createCameras()

        logger.info(lang.getTranslation("loading.reg.packet"))
        registerPacket()

        logger.info(lang.getTranslation("loading.done"))
    }

    override fun onDisable() {
        logger.info(lang.getTranslation("disabling.done"))
    }

    override fun reloadConfig() {
        super.reloadConfig()
        runBlocking {
            mutexConfig.withLock {
                config.loadConfig()
            }
        }
    }

    private fun createCameras() {
        reloadCameras()
    }

    fun reloadCameras() {
        CoroutineScope(Dispatchers.Default).launch {
            mutexCameras.withLock {
                mutexConfig.withLock {
                    val temps = mutableListOf<Camera>()

                    config.mainConfig.cameraNames.forEach {
                        if (cameras.find { camera -> camera.name == it } == null) {
                            temps.add(Camera(it))
                            return@forEach
                        }
                        temps.add(cameras.find { camera -> camera.name == it }!!)
                    }

                    cameras.clear()
                    cameras.addAll(temps)
                }
            }
        }
    }

    private fun registerCommands() {
        getCommand("autostreamingassistantserver")?.setExecutor(PluginCommands)
        getCommand("autostreamingassistantserver")?.tabCompleter = PluginTabCompleter
    }

    private fun registerListeners() {
        Bukkit.getPluginManager().registerEvents(PlayerJoinListener, this)
        Bukkit.getPluginManager().registerEvents(PlayerQuitListener, this)
    }

    private fun registerPacket() {
        val messenger = Bukkit.getServer().messenger

        messenger.registerIncomingPluginChannel(this, PacketID.BIND_CAMERA_RESULT, BindCameraResponsePacketListener)
        messenger.registerIncomingPluginChannel(this, PacketID.BIND_STATUS, BindStatusPacketListener)
        messenger.registerIncomingPluginChannel(this, PacketID.CLIENT_STATUS, ClientStatusPacketListener)
        messenger.registerIncomingPluginChannel(this, PacketID.MANUAL_BIND_CAMERA, ManualBindCameraPacketListener)
        messenger.registerIncomingPluginChannel(this, PacketID.UNBIND_CAMERA_RESULT, UnbindCameraResponsePacketListener)

        messenger.registerOutgoingPluginChannel(this, PacketID.BIND_CAMERA)
        messenger.registerOutgoingPluginChannel(this, PacketID.UNBIND_CAMERA)
        messenger.registerOutgoingPluginChannel(this, PacketID.REQUEST_STATUS)
        messenger.registerOutgoingPluginChannel(this, PacketID.REQUEST_BIND_STATUS)
    }

    private fun registerBStats() {
        Metrics(this, 20517)
    }


}
