package cn.cutemc.autostreamingassistant.bukkit.commands

import cn.cutemc.autostreamingassistant.bukkit.AutoStreamingAssistant
import cn.cutemc.autostreamingassistant.bukkit.network.BindResult
import cn.cutemc.autostreamingassistant.bukkit.network.UnbindResult
import cn.cutemc.autostreamingassistant.bukkit.utils.BukkitUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender


object PluginCommands : CommandExecutor {

    private val lang by lazy { AutoStreamingAssistant.INSTANCE.lang }
    private val plugin by lazy { AutoStreamingAssistant.INSTANCE }
    private val config by lazy { AutoStreamingAssistant.INSTANCE.config }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            // 运行主命令
            return mainCommand(sender)
        }

        when (args[0]) {
            "reload" -> {
                // 运行重载命令
                return reloadCommand(sender)
            }
            "help" -> {
                // 运行帮助命令
                return helpCommand(sender)
            }
            "version" -> {
                // 运行版本命令
                return versionCommand(sender)
            }
            "camera" -> {
                if (args.size == 1) {
                    // 运行帮助
                    return wrongCommand(sender)
                }

                when (args[1]) {
                    "status" -> {
                        // 运行状态命令

                        return when (args.size) {
                            3 -> {
                                cameraStatusCommand(sender, args[2])
                            }

                            else -> {
                                // 运行帮助
                                wrongCommand(sender)
                            }
                        }
                    }
                    "bind" -> {
                        if (args.size == 4) {
                            // 运行绑定命令
                            return cameraBindCommand(sender, args[2], args[3])
                        }

                        // 运行帮助
                        return wrongCommand(sender)
                    }
                    "unbind" -> {
                        if (args.size == 3) {
                            // 运行解绑命令
                            return cameraUnbindCommand(sender, args[2])
                        }

                        // 运行帮助
                        return wrongCommand(sender)
                    }
                    "list" -> {
                        // 运行列表命令
                        return cameraListCommand(sender)
                    }
                    "autoswitch" -> {
                        // 运行自动切换命令
                        return autoSwitchCommand(sender, args[2], args[3])
                    }
                    else -> {
                        // 运行帮助
                        return wrongCommand(sender)
                    }
                }
            }
            "position" -> {
                if (args.size == 1) {
                    // 运行帮助
                    return wrongCommand(sender)
                }

                return when (args[1]) {
                    "list" -> {
                        // 运行列表命令
                        positionListCommand(sender)
                    }

                    else -> {
                        // 运行帮助
                        wrongCommand(sender)
                    }
                }
            }
            else -> {
                // 运行帮助
                return wrongCommand(sender)
            }
        }
    }

    private fun wrongCommand(sender: CommandSender): Boolean {
        sender.sendMessage(lang.getTranslation("command.wrong"))
        return true
    }

    private fun mainCommand(sender: CommandSender): Boolean {

        return helpCommand(sender)
    }

    private fun reloadCommand(sender: CommandSender): Boolean {
        sender.sendMessage(lang.getTranslation("command.reload"))

        plugin.reloadConfig()
        plugin.reloadCameras()

        return true
    }

    private fun helpCommand(sender: CommandSender): Boolean {
        sender.sendMessage("\n")
        sender.sendMessage("${ChatColor.GOLD}===============================")
        sender.sendMessage(" ${lang.getTranslation("command.help.title")}")
        sender.sendMessage(" ${lang.getTranslation("command.help.author")}${AutoStreamingAssistant.INSTANCE.description.authors.joinToString()}")
        sender.sendMessage(" ${lang.getTranslation("command.help.version")}${AutoStreamingAssistant.INSTANCE.description.version}")
        sender.sendMessage(" ${lang.getTranslation("command.help.source")}https://github.com/lushangkan/AutoStreamingAssistant-Server")
        sender.sendMessage("\n")
        sender.sendMessage(" ${lang.getTranslation("command.help.command.title")}")
        sender.sendMessage("${ChatColor.GOLD} ● /autostreamingassistantserver help${ChatColor.GRAY} - ${lang.getTranslation("command.help.command.help")}")
        sender.sendMessage("${ChatColor.GOLD} ● /autostreamingassistantserver reload${ChatColor.GRAY} - ${lang.getTranslation("command.help.command.reload")}")
        sender.sendMessage("${ChatColor.GOLD} ● /autostreamingassistantserver version${ChatColor.GRAY} - ${lang.getTranslation("command.help.command.version")}")
        sender.sendMessage("${ChatColor.GOLD} ● /autostreamingassistantserver camera list${ChatColor.GRAY} - ${lang.getTranslation("command.help.command.camera.list")}")
        sender.sendMessage("${ChatColor.GOLD} ● /autostreamingassistantserver camera status${ChatColor.GRAY} - ${lang.getTranslation("command.help.command.camera.status")}")
        sender.sendMessage("${ChatColor.GOLD} ● /autostreamingassistantserver camera status <${lang.getTranslation("command.help.command.args.camera")}>${ChatColor.GRAY} - ${lang.getTranslation("command.help.command.camera.status.camera")}")
        sender.sendMessage("${ChatColor.GOLD} ● /autostreamingassistantserver camera bind <${lang.getTranslation("command.help.command.args.camera")}> <${lang.getTranslation("command.help.command.args.player")}/${lang.getTranslation("command.help.command.args.fixedlocation")}>${ChatColor.GRAY} - ${lang.getTranslation("command.help.command.camera.bind")}")
        sender.sendMessage("${ChatColor.GOLD} ● /autostreamingassistantserver camera unbind <${lang.getTranslation("command.help.command.args.camera")}>${ChatColor.GRAY} - ${lang.getTranslation("command.help.command.camera.unbind")}")
        sender.sendMessage("${ChatColor.GOLD} ● /autostreamingassistantserver camera autoswitch <${lang.getTranslation("command.help.command.args.camera")}> <on/off>${ChatColor.GRAY} - ${lang.getTranslation("command.help.command.camera.autoswitch")}")
        sender.sendMessage("${ChatColor.GOLD} ● /autostreamingassistantserver position list${ChatColor.GRAY} - ${lang.getTranslation("command.help.command.position.list")}")
        sender.sendMessage("${ChatColor.GOLD}===============================")
        sender.sendMessage("\n")

        return true
    }

    private fun versionCommand(sender: CommandSender): Boolean {
        sender.sendMessage("${lang.getTranslation("command.version")}${AutoStreamingAssistant.INSTANCE.description.version}")

        return true
    }

    private fun cameraListCommand(sender: CommandSender): Boolean {
        CoroutineScope(Dispatchers.Default).launch {
            plugin.mutexCameras.withLock {
                if (plugin.cameras.size == 0) {
                    sender.sendMessage(lang.getTranslation("command.camera.list.notfound"))
                    return@launch
                }

                sender.sendMessage("\n")
                sender.sendMessage("${ChatColor.GOLD}===============================")
                sender.sendMessage(" ${lang.getTranslation("command.camera.list.title")}")
                plugin.cameras.forEach {
                    sender.sendMessage("\n")
                    sender.sendMessage("${ChatColor.GOLD} ● ${lang.getTranslation("command.camera.list.camera.name")}${it.name}")
                    sender.sendMessage("   ${lang.getTranslation("command.camera.list.camera.status")}")
                    if (it.online) {
                        sender.sendMessage("   ${lang.getTranslation("command.camera.list.camera.status.online")}")
                    } else {
                        sender.sendMessage("   ${lang.getTranslation("command.camera.list.camera.status.offline")}")
                    }
                    sender.sendMessage(
                        "   ${lang.getTranslation("command.camera.list.camera.status.autoswitchplayer")}${
                            if (it.autoSwitch) lang.getTranslation(
                                "command.camera.list.camera.status.autoswitchplayer.true"
                            ) else lang.getTranslation("command.camera.list.camera.status.autoswitchplayer.false")
                        }"
                    )
                    if (it.boundPlayer != null) {
                        sender.sendMessage("   ${lang.getTranslation("command.camera.list.camera.status.boundplayer")}${it.boundPlayer!!.name}")
                    } else if (it.fixedPos != null) {
                        sender.sendMessage("   ${lang.getTranslation("command.camera.list.camera.status.fixedposition")}${it.fixedPos!!.name}")

                    }
                }
                sender.sendMessage("${ChatColor.GOLD}===============================")
                sender.sendMessage("\n")
            }
        }
        return true
    }

    private fun cameraStatusCommand(sender: CommandSender, cameraName: String): Boolean {
        CoroutineScope(Dispatchers.Default).launch {
            plugin.mutexCameras.withLock {
                val camera = plugin.cameras.find { it.name == cameraName }

                if (camera == null) {
                    sender.sendMessage(lang.getTranslation("command.camera.status.notfound"))
                    return@launch
                }

                sender.sendMessage(lang.getTranslation("command.camera.status.title"))
                sender.sendMessage("${lang.getTranslation("command.camera.status.camera.name")}${camera.name}")
                sender.sendMessage(lang.getTranslation("command.camera.status.camera.status"))
                if (camera.online) {
                    sender.sendMessage(lang.getTranslation("command.camera.status.camera.status.online"))
                } else {
                    sender.sendMessage(lang.getTranslation("command.camera.status.camera.status.offline"))
                }
                sender.sendMessage(
                    "${lang.getTranslation("command.camera.status.camera.status.autoswitchplayer")}${
                        if (camera.autoSwitch) lang.getTranslation(
                            "command.camera.status.camera.status.autoswitchplayer.true"
                        ) else lang.getTranslation("command.camera.status.camera.status.autoswitchplayer.false")
                    }"
                )
                if (camera.boundPlayer != null) {
                    sender.sendMessage("${lang.getTranslation("command.camera.status.camera.status.boundplayer")}${camera.boundPlayer!!.name}")
                } else if (camera.fixedPos != null) {
                    sender.sendMessage("${lang.getTranslation("command.camera.status.camera.status.fixedposition")}${camera.fixedPos!!.name}")
                }
            }
        }
        return true
    }

    private fun cameraBindCommand(sender: CommandSender, cameraName: String, name: String): Boolean {
        CoroutineScope(Dispatchers.Default).launch {
            plugin.mutexCameras.withLock {
                plugin.mutexConfig.withLock {
                    val camera = plugin.cameras.find { it.name == cameraName }
                    val player = sender.server.getPlayer(name)
                    val cameraPosition = config.mainConfig.fixedCameraPosition.firstOrNull { it.name == name }

                    if (camera == null) {
                        // 找不到摄像机
                        sender.sendMessage(lang.getTranslation("command.camera.bind.notfound.camera"))
                        return@launch
                    }

                    if (player == null && cameraPosition == null) {
                        // 找不到玩家和位置
                        sender.sendMessage(lang.getTranslation("command.camera.bind.notfound.name"))
                        return@launch
                    }

                    if (player == null && cameraPosition != null) {
                        // 找到位置
                        sender.sendMessage(lang.getTranslation("command.camera.binding.fixedposition", cameraPosition.name))

                        CoroutineScope(Dispatchers.Default).launch {
                            val result = camera.bindFixedPos(cameraPosition, false)
                            if (result is BindResult && result != BindResult.SUCCESS) {
                                sender.sendMessage("${getBindFailedMessage(result)}")
                            } else if (result is UnbindResult && result != BindResult.SUCCESS) {
                                sender.sendMessage("${getUnbindFailedMessage(result)}")
                            }
                        }
                        sender.sendMessage(lang.getTranslation("command.camera.bind.success"))
                        return@launch
                    }

                    // 找到玩家
                    if (BukkitUtils.isCraftPlayer(player!!)) sender.sendMessage(lang.getTranslation("command.camera.bind.notsupport"))

                    sender.sendMessage(
                        lang.getTranslation("command.camera.binding.player"),
                        player.name
                    )

                    val result = camera.bindCamera(player, false)
                    if (result != BindResult.SUCCESS) {
                        sender.sendMessage("${getBindFailedMessage(result)}")
                        return@launch
                    }

                    sender.sendMessage(lang.getTranslation("command.camera.bind.success"))
                }
            }
        }

        return true
    }

    private fun cameraUnbindCommand(sender: CommandSender, cameraName: String): Boolean {
        CoroutineScope(Dispatchers.Default).launch {
            plugin.mutexCameras.withLock {
                val camera = plugin.cameras.find { it.name == cameraName }

                if (camera == null) {
                    sender.sendMessage(lang.getTranslation("command.camera.bind.notfound.camera"))
                    return@launch
                }

                camera.autoSwitch = false
                val result = camera.unbindCamera()
                if (result != UnbindResult.SUCCESS) {
                    sender.sendMessage("${getUnbindFailedMessage(result)}")
                    return@launch
                }

                sender.sendMessage(lang.getTranslation("command.camera.unbind.success"))
            }
        }
        return true
    }

    private fun positionListCommand(sender: CommandSender): Boolean {
        CoroutineScope(Dispatchers.Default).launch {
            plugin.mutexConfig.withLock {
                if (config.mainConfig.fixedCameraPosition.isEmpty()) {
                    sender.sendMessage(lang.getTranslation("command.position.list.notfound"))
                    return@launch
                }

                sender.sendMessage("\n")
                sender.sendMessage("${ChatColor.GOLD}===============================")
                sender.sendMessage(" ${lang.getTranslation("command.position.list.title")}")
                config.mainConfig.fixedCameraPosition.forEach {
                    sender.sendMessage("\n")
                    sender.sendMessage("${ChatColor.GOLD} ● ${lang.getTranslation("command.position.list.position.name")}${it.name}")
                    sender.sendMessage("   ${lang.getTranslation("command.position.list.position.world")}${it.world}")
                    sender.sendMessage("   ${lang.getTranslation("command.position.list.position.x")}${it.x}")
                    sender.sendMessage("   ${lang.getTranslation("command.position.list.position.y")}${it.y}")
                    sender.sendMessage("   ${lang.getTranslation("command.position.list.position.z")}${it.z}")
                    sender.sendMessage("   ${lang.getTranslation("command.position.list.position.yaw")}${it.yaw}")
                    sender.sendMessage("   ${lang.getTranslation("command.position.list.position.pitch")}${it.pitch}")
                }
                sender.sendMessage("${ChatColor.GOLD}===============================")
                sender.sendMessage("\n")
            }
        }
        return true
    }

    private fun autoSwitchCommand(sender: CommandSender, cameraName: String, status: String): Boolean {
        CoroutineScope(Dispatchers.Default).launch {
            plugin.mutexCameras.withLock {
                val camera = plugin.cameras.find { it.name == cameraName }

                if (camera == null) {
                    sender.sendMessage(lang.getTranslation("command.camera.autoswitch.notfound"))
                    return@withLock
                }

                when (status) {
                    "on" -> {
                        camera.autoSwitch = true
                        sender.sendMessage(lang.getTranslation("command.camera.autoswitch.success"))
                    }
                    "off" -> {
                        camera.autoSwitch = false
                        sender.sendMessage(lang.getTranslation("command.camera.autoswitch.success"))
                    }
                    else -> {
                        sender.sendMessage(lang.getTranslation("command.camera.autoswitch.wrong"))
                    }
                }
            }
        }

        return true
    }

    private fun getBindFailedMessage(result: BindResult): String? {
        return when (result) {
            BindResult.CLIENT_NOT_RESPONDING -> lang.getTranslation("bind.failed.cause.clientnotrespond")
            BindResult.NO_OTHER_PLAYERS -> lang.getTranslation("bind.failed.cause.nootherplayers")
            BindResult.NOT_FOUND_PLAYER -> lang.getTranslation("bind.failed.cause.notfoundplayer")
            BindResult.NOT_AT_NEAR_BY -> lang.getTranslation("bind.failed.cause.notatnearby")
            BindResult.WORLD_IS_NULL -> lang.getTranslation("bind.failed.cause.worldisnull")
            BindResult.PLAYER_IS_NULL -> lang.getTranslation("bind.failed.cause.playerisnull")
            BindResult.CAMERA_PLAYER_NOT_ONLINE -> lang.getTranslation("bind.failed.cause.cameraplayernotonline")
            BindResult.SUCCESS -> null
        }
    }

    private fun getUnbindFailedMessage(result: UnbindResult): String? {
        return when (result) {
            UnbindResult.CLIENT_NOT_RESPONDING -> lang.getTranslation("unbind.failed.cause.clientnotrespond")
            UnbindResult.CAMERA_PLAYER_NOT_ONLINE -> lang.getTranslation("unbind.failed.cause.cameraplayernotonline")
            UnbindResult.NOT_BOUND_CAMERA -> lang.getTranslation("unbind.failed.cause.notboundcamera")
            UnbindResult.SUCCESS -> null
        }
    }

}