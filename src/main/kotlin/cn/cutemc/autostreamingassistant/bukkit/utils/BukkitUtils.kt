package cn.cutemc.autostreamingassistant.bukkit.utils

import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer
import org.bukkit.plugin.java.JavaPlugin

object BukkitUtils {

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
    fun sendPluginMessage(plugin: JavaPlugin, player: CraftPlayer, channel: String, message: ByteArray) {
        val channelsField = CraftPlayer::class.java.getDeclaredField("channels")
        channelsField.isAccessible = true
        val channels = channelsField.get(player) as HashSet<String>

        if (!channels.contains(channel)) {
            player.addChannel(channel)
        }

        player.sendPluginMessage(plugin, channel, message)
    }
}