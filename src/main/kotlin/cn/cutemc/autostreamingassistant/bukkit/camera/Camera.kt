package cn.cutemc.autostreamingassistant.bukkit.camera

import cn.cutemc.autostreamingassistant.bukkit.AutoStreamingAssistant
import cn.cutemc.autostreamingassistant.bukkit.network.PacketID
import com.google.gson.Gson
import net.minecraft.resources.MinecraftKey
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.StandardMessenger
import java.nio.charset.StandardCharsets

class Camera(val name: String) {

    private val plugin by lazy { AutoStreamingAssistant.INSTANCE }
    private var online = false
    private var player: Player? = null

    fun joinServer(player: Player) {
        online = true
        this.player = player

        val message = Gson().toJson(mapOf("version" to plugin.description.version))

        if (player !is CraftPlayer) throw IllegalStateException("Player is not CraftPlayer!")

        // 由于PlayerJoinEvent被触发时信道并未注册，无法使用sendPluginMessage发送，这个方法可能会产生兼容性问题，但在1.20.2测试通过
        // 这Bug修了3天，绝望了😭😭😭😭
        val id = MinecraftKey(StandardMessenger.validateAndCorrectChannel(PacketID.REQUEST_STATUS))
        val method = CraftPlayer::class.java.declaredMethods.filter { it.name == "sendCustomPayload" && it.parameterCount == 2 }.firstOrNull() ?: throw IllegalStateException("Cannot find method sendPluginMessage")
        method.isAccessible = true
        method.invoke(player, id, message.toByteArray(StandardCharsets.UTF_8))
    }

    fun leaveServer() {
        online = false
        player = null
    }

    fun isOnline() = online

}