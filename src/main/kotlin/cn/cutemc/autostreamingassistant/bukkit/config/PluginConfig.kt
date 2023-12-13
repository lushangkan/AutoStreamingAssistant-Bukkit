package cn.cutemc.autostreamingassistant.bukkit.config

import cn.cutemc.autostreamingassistant.bukkit.AutoStreamingAssistant
import cn.cutemc.autostreamingassistant.bukkit.logger.PluginLogger
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.common.io.Files
import java.io.File

class PluginConfig {

    lateinit var mainConfig: MainConfig

    private val plugin by lazy { AutoStreamingAssistant.INSTANCE }
    private val logger: PluginLogger by lazy { AutoStreamingAssistant.INSTANCE.logger }

    fun loadConfig() {
        val configStr = plugin.getConfig().saveToString()

        val yaml = YAMLMapper().registerKotlinModule()

        try {
            mainConfig = yaml.readValue(configStr, MainConfig::class.java)
        } catch (e: Exception) {
            updateConfig()
            plugin.reloadConfig()
        }
    }

    private fun updateConfig() {
        logger.warning("Config file is not valid, save default config file to config.yml, old config file will be renamed to config.yml.old")
        val configFile = File(plugin.dataFolder, "config.yml")
        val oldConfigFile = File(plugin.dataFolder, "config.yml.old")
        if (oldConfigFile.exists()) {
            oldConfigFile.delete()
        }
        Files.move(configFile, oldConfigFile)
        plugin.saveDefaultConfig()
        return
    }

}