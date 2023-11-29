package cn.cutemc.autostreamingassistant.bukkit

import cn.cutemc.autostreamingassistant.bukkit.commands.PluginCommands
import cn.cutemc.autostreamingassistant.bukkit.config.PluginConfig
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Color
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.yaml.snakeyaml.Yaml

class AutoStreamingAssistant: JavaPlugin() {

    private val consoleSender by lazy { Bukkit.getConsoleSender() }
    private lateinit var config: PluginConfig

    override fun onEnable() {
        consoleSender.sendMessage(ChatColor.GOLD + "Loading AutoStreamingAssistant...")
        consoleSender.sendMessage(ChatColor.GOLD + "Loading Config...")

        saveDefaultConfig()
        reloadConfig()

        consoleSender.sendMessage(ChatColor.GOLD + "Registering Commands...")

        Bukkit.getPluginCommand("autostreamingassistantserver")?.setExecutor(PluginCommands)

        logger.info(ChatColor.GOLD + "AutoStreamingAssistant loaded!")
    }

    override fun onDisable() {
        logger.info(ChatColor.GOLD + "Disabling AutoStreamingAssistant...")
        logger.info(ChatColor.GOLD + "AutoStreamingAssistant disabled!")
    }

    override fun reloadConfig() {
        super.reloadConfig()

        val fileConfig = super.getConfig()

        val yaml = Yaml()

        config = yaml.loadAs(fileConfig.saveToString(), PluginConfig::class.java)
    }

}

private operator fun ChatColor.plus(s: String): String {
    return this.toString() + s
}

