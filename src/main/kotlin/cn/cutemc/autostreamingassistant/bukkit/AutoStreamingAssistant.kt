package cn.cutemc.autostreamingassistant.bukkit

import cn.cutemc.autostreamingassistant.bukkit.commands.PluginCommands
import cn.cutemc.autostreamingassistant.bukkit.config.PluginConfig
import cn.cutemc.autostreamingassistant.bukkit.lang.PluginLang
import cn.cutemc.autostreamingassistant.bukkit.logger.PluginLogger
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

        logger.info("${ChatColor.GOLD}${lang.getTranslation("loading.done")}")
    }

    override fun onDisable() {
        logger.info("${ChatColor.GOLD}${lang.getTranslation("disabling.done")}")
    }

    override fun reloadConfig() {
        super.reloadConfig()

        config.loadConfig()
    }

}
