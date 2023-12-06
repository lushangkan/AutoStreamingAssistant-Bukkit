package cn.cutemc.autostreamingassistant.bukkit

import cn.cutemc.autostreamingassistant.bukkit.camera.Camera
import cn.cutemc.autostreamingassistant.bukkit.commands.PluginCommands
import cn.cutemc.autostreamingassistant.bukkit.config.PluginConfig
import cn.cutemc.autostreamingassistant.bukkit.lang.PluginLang
import cn.cutemc.autostreamingassistant.bukkit.listeners.bukkit.PlayerJoinListener
import cn.cutemc.autostreamingassistant.bukkit.listeners.network.messagings.*
import cn.cutemc.autostreamingassistant.bukkit.logger.PluginLogger
import cn.cutemc.autostreamingassistant.bukkit.network.PacketID
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.plugin.java.JavaPlugin

class AutoStreamingAssistant: JavaPlugin() {

    companion object {
        lateinit var INSTANCE: AutoStreamingAssistant
    }

    private val logger by lazy { PluginLogger(super.getLogger()) }

    val config: PluginConfig = PluginConfig()

    lateinit var lang: PluginLang
        private set

    val cameras: MutableList<Camera> = mutableListOf()

    override fun onLoad() {
        INSTANCE = this
        lang = PluginLang()
        saveDefaultConfig()
        reloadConfig()
    }

    override fun onEnable() {
        logger.info("${ChatColor.GOLD}${lang.getTranslation("loading.main")}")

        logger.info("${ChatColor.GOLD}${lang.getTranslation("loading.reg.command")}")
        Bukkit.getPluginCommand("autostreamingassistantserver")?.setExecutor(PluginCommands)

        logger.info("${ChatColor.GOLD}${lang.getTranslation("loading.reg.listeners")}")
        Bukkit.getPluginManager().registerEvents(PlayerJoinListener, this)

        logger.info("${ChatColor.GOLD}${lang.getTranslation("loading.create.cameras")}")
        createCameras()

        logger.info("${ChatColor.GOLD}${lang.getTranslation("loading.reg.packet")}")
        registerPacket()

        logger.info("${ChatColor.GOLD}${lang.getTranslation("loading.done")}")
    }

    override fun onDisable() {
        logger.info("${ChatColor.GOLD}${lang.getTranslation("disabling.done")}")
    }

    override fun reloadConfig() {
        super.reloadConfig()

        config.loadConfig()
    }

    private fun createCameras() {
        config.mainConfig.cameraNames.forEach {
            cameras.add(Camera(it))
        }
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


}
