package cn.cutemc.autostreamingassistant.bukkit.utils

import cn.cutemc.autostreamingassistant.bukkit.AutoStreamingAssistant
import cn.cutemc.autostreamingassistant.bukkit.ManagePluginType
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

object BukkitUtils {

    private val plugin by lazy { AutoStreamingAssistant.INSTANCE }

    /**
     * 发送插件消息到用户的客户端
     * 由于PlayerJoinEvent被触发时信道并未注册，无法直接使用sendPluginMessage发送，这个方法可能会产生兼容性问题，但在1.20.2测试通过
     * 这Bug修了3天，绝望了😭😭😭😭
     *
     * @param plugin 插件实例
     * @param player 玩家实例
     * @param channel 信道ID
     * @param message 消息
     *
     * @see org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer.sendPluginMessage
     */
    fun sendPluginMessage(plugin: JavaPlugin, player: Player, channel: String, message: ByteArray) {
        if (!isCraftPlayer(player)) {
            throw IllegalArgumentException("Player must be CraftPlayer")
        }

        var done = false

        plugin.server.scheduler.runTask(plugin, Runnable {
            val channelsField = player::class.java.getDeclaredField("channels")
            channelsField.isAccessible = true
            val channels = channelsField.get(player) as HashSet<String>

            if (!channels.contains(channel)) {
                channels.add(channel)
            }

            player.sendPluginMessage(plugin, channel, message)
            done = true
        })

        while (!done) {
            Thread.sleep(1)
        }
    }

    /**
     * 判断玩家类是否为CraftPlayer
     */
    fun isCraftPlayer(player: Player): Boolean {
        return player::class.java.simpleName == "CraftPlayer"
    }

    /**
     * 获取管理插件类型
     *
     *
     */
    fun getManagePluginType(): ManagePluginType? {
        val plugin = AutoStreamingAssistant.INSTANCE

        for (type in ManagePluginType.entries) {
            if (plugin.server.pluginManager.getPlugin(type.pluginName) != null) {
                return type
            }
        }

        return null
    }
}