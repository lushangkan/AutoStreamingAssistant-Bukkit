package cn.cutemc.autostreamingassistant.bukkit.config

import cn.cutemc.autostreamingassistant.bukkit.AutoStreamingAssistant
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

class PluginConfig {

    lateinit var mainConfig: MainConfig

    private val plugin by lazy { AutoStreamingAssistant.INSTANCE }

    fun loadConfig() {
        val configStr = plugin.getConfig().saveToString()

        val yaml = YAMLMapper().registerKotlinModule()

        mainConfig = yaml.readValue(configStr, MainConfig::class.java)
    }
}