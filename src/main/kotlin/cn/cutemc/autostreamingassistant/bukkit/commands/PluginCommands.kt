package cn.cutemc.autostreamingassistant.bukkit.commands

import cn.cutemc.autostreamingassistant.bukkit.AutoStreamingAssistant
import cn.cutemc.autostreamingassistant.bukkit.network.BindResult
import cn.cutemc.autostreamingassistant.bukkit.network.UnbindResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer


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
        sender.sendMessage("${ChatColor.RED}${lang.getTranslation("command.wrong")}")
        return true
    }

    private fun mainCommand(sender: CommandSender): Boolean {

        return helpCommand(sender)
    }

    private fun reloadCommand(sender: CommandSender): Boolean {
        sender.sendMessage("${ChatColor.GOLD}${lang.getTranslation("command.reload")}")

        plugin.reloadConfig()

        return true
    }

    private fun helpCommand(sender: CommandSender): Boolean {
        sender.sendMessage("\n")
        sender.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}===============================")
        sender.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD} ${lang.getTranslation("command.help.title")}")
        sender.sendMessage("${ChatColor.GOLD} ${lang.getTranslation("command.help.author")}${ChatColor.DARK_AQUA}${AutoStreamingAssistant.INSTANCE.description.authors.joinToString()}")
        sender.sendMessage("${ChatColor.GOLD} ${lang.getTranslation("command.help.version")}${ChatColor.DARK_AQUA}${AutoStreamingAssistant.INSTANCE.description.version}")
        sender.sendMessage("${ChatColor.GOLD} ${lang.getTranslation("command.help.source")}${ChatColor.DARK_AQUA}https://github.com/lushangkan/AutoStreamingAssistant-Server")
        sender.sendMessage("\n")
        sender.sendMessage("${ChatColor.GOLD} ${lang.getTranslation("command.help.command.title")}")
        sender.sendMessage("${ChatColor.GOLD} ● /autostreamingassistantserver help${ChatColor.GRAY} - ${ChatColor.DARK_AQUA}${lang.getTranslation("command.help.command.help")}")
        sender.sendMessage("${ChatColor.GOLD} ● /autostreamingassistantserver reload${ChatColor.GRAY} - ${ChatColor.DARK_AQUA}${lang.getTranslation("command.help.command.reload")}")
        sender.sendMessage("${ChatColor.GOLD} ● /autostreamingassistantserver version${ChatColor.GRAY} - ${ChatColor.DARK_AQUA}${lang.getTranslation("command.help.command.version")}")
        sender.sendMessage("${ChatColor.GOLD} ● /autostreamingassistantserver camera list${ChatColor.GRAY} - ${ChatColor.DARK_AQUA}${lang.getTranslation("command.help.command.camera.list")}")
        sender.sendMessage("${ChatColor.GOLD} ● /autostreamingassistantserver camera status${ChatColor.GRAY} - ${ChatColor.DARK_AQUA}${lang.getTranslation("command.help.command.camera.status")}")
        sender.sendMessage("${ChatColor.GOLD} ● /autostreamingassistantserver camera status <${lang.getTranslation("command.help.command.args.camera")}>${ChatColor.GRAY} - ${ChatColor.DARK_AQUA}${lang.getTranslation("command.help.command.camera.status.camera")}")
        sender.sendMessage("${ChatColor.GOLD} ● /autostreamingassistantserver camera bind <${lang.getTranslation("command.help.command.args.camera")}> <${lang.getTranslation("command.help.command.args.player")}/${lang.getTranslation("command.help.command.args.fixedlocation")}>${ChatColor.GRAY} - ${ChatColor.DARK_AQUA}${lang.getTranslation("command.help.command.camera.bind")}")
        sender.sendMessage("${ChatColor.GOLD} ● /autostreamingassistantserver camera unbind <${lang.getTranslation("command.help.command.args.camera")}>${ChatColor.GRAY} - ${ChatColor.DARK_AQUA}${lang.getTranslation("command.help.command.camera.unbind")}")
        sender.sendMessage("${ChatColor.GOLD} ● /autostreamingassistantserver camera autoswitch <${lang.getTranslation("command.help.command.args.camera")}> <on/off>${ChatColor.GRAY} - ${ChatColor.DARK_AQUA}${lang.getTranslation("command.help.command.camera.autoswitch")}")
        sender.sendMessage("${ChatColor.GOLD} ● /autostreamingassistantserver position list${ChatColor.GRAY} - ${ChatColor.DARK_AQUA}${lang.getTranslation("command.help.command.position.list")}")
        sender.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}===============================")
        sender.sendMessage("\n")

        return true
    }

    private fun versionCommand(sender: CommandSender): Boolean {
        sender.sendMessage("${ChatColor.GOLD}${lang.getTranslation("command.version")}${ChatColor.DARK_AQUA}${AutoStreamingAssistant.INSTANCE.description.version}")

        return true
    }

    private fun cameraListCommand(sender: CommandSender): Boolean {
        if (plugin.cameras.size == 0) {
            sender.sendMessage("${ChatColor.RED}${lang.getTranslation("command.camera.list.notfound")}")
            return true
        }

        sender.sendMessage("\n")
        sender.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}===============================")
        sender.sendMessage("${ChatColor.GOLD} ${lang.getTranslation("command.camera.list.title")}")
        plugin.cameras.forEach {
            sender.sendMessage("\n")
            sender.sendMessage("${ChatColor.GOLD} ● ${lang.getTranslation("command.camera.list.camera.name")}${ChatColor.DARK_AQUA}${it.name}")
            sender.sendMessage("${ChatColor.GOLD}   ${lang.getTranslation("command.camera.list.camera.status")}")
            if (it.online) {
                sender.sendMessage("${ChatColor.GREEN}   ${lang.getTranslation("command.camera.list.camera.status.online")}")
            } else {
                sender.sendMessage("${ChatColor.RED}   ${lang.getTranslation("command.camera.list.camera.status.offline")}")
            }
            sender.sendMessage("${ChatColor.GOLD}   ${lang.getTranslation("command.camera.list.camera.status.autoswitchplayer")}${if (it.autoSwitch) ChatColor.GREEN else ChatColor.RED}${if (it.autoSwitch) lang.getTranslation("command.camera.list.camera.status.autoswitchplayer.true") else lang.getTranslation("command.camera.list.camera.status.autoswitchplayer.false")}")
            if (it.boundPlayer != null) {
                sender.sendMessage("${ChatColor.GOLD}   ${lang.getTranslation("command.camera.list.camera.status.boundplayer")}${ChatColor.DARK_AQUA}${it.boundPlayer!!.name}")
            } else if (it.fixedPos != null){
                sender.sendMessage("${ChatColor.GOLD}   ${lang.getTranslation("command.camera.list.camera.status.fixedposition")}${ChatColor.DARK_AQUA}${it.fixedPos!!.name}")

            }
        }
        sender.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}===============================")
        sender.sendMessage("\n")
        return true
    }

    private fun cameraStatusCommand(sender: CommandSender, cameraName: String): Boolean {
        val camera = plugin.cameras.find { it.name == cameraName }

        if (camera == null) {
            sender.sendMessage("${ChatColor.RED}${lang.getTranslation("command.camera.status.notfound")}")
            return true
        }

        sender.sendMessage("\n")
        sender.sendMessage("${ChatColor.GOLD} ${lang.getTranslation("command.camera.status.title")}")
        sender.sendMessage("${ChatColor.GOLD} ${lang.getTranslation("command.camera.status.camera.name")}${ChatColor.DARK_AQUA}${camera.name}")
        sender.sendMessage("${ChatColor.GOLD} ${lang.getTranslation("command.camera.status.camera.status")}")
        if (camera.online) {
            sender.sendMessage("${ChatColor.GREEN} ${lang.getTranslation("command.camera.status.camera.status.online")}")
        } else {
            sender.sendMessage("${ChatColor.RED} ${lang.getTranslation("command.camera.status.camera.status.offline")}")
        }
        sender.sendMessage("${ChatColor.GOLD} ${lang.getTranslation("command.camera.status.camera.status.autoswitchplayer")}${if (camera.autoSwitch) ChatColor.GREEN else ChatColor.RED}${if (camera.autoSwitch) lang.getTranslation("command.camera.status.camera.status.autoswitchplayer.true") else lang.getTranslation("command.camera.status.camera.status.autoswitchplayer.false")}")
        if (camera.boundPlayer != null) {
            sender.sendMessage("${ChatColor.GOLD} ${lang.getTranslation("command.camera.status.camera.status.boundplayer")}${ChatColor.DARK_AQUA}${camera.boundPlayer!!.name}")
        } else if (camera.fixedPos != null){
            sender.sendMessage("${ChatColor.GOLD} ${lang.getTranslation("command.camera.status.camera.status.fixedposition")}${ChatColor.DARK_AQUA}${camera.fixedPos!!.name}")
        }
        sender.sendMessage("\n")

        return true
    }

    private fun cameraBindCommand(sender: CommandSender, cameraName: String, name: String): Boolean {
        CoroutineScope(Dispatchers.Default).launch {
            val camera = plugin.cameras.find { it.name == cameraName }
            val player = sender.server.getPlayer(name)
            val cameraPosition = config.mainConfig.fixedCameraPosition.firstOrNull { it.name == name }

            if (camera == null) {
                // 找不到摄像机
                sender.sendMessage("${ChatColor.RED}${lang.getTranslation("command.camera.bind.notfound.camera")}")
                return@launch
            }

            if (player == null && cameraPosition == null) {
                // 找不到玩家和位置
                sender.sendMessage("${ChatColor.RED}${lang.getTranslation("command.camera.bind.notfound.name")}")
                return@launch
            }

            if (player == null && cameraPosition != null) {
                // 找到位置
                sender.sendMessage("${ChatColor.GOLD}${lang.getTranslation("command.camera.binding.fixedposition")}", cameraPosition.name)
                CoroutineScope(Dispatchers.Default).launch {
                    val result = camera.bindFixedPos(cameraPosition)
                    if (result is BindResult && result != BindResult.SUCCESS) {
                        sender.sendMessage("${ChatColor.RED}${getBindFailedMessage(result)}")
                    } else if (result is UnbindResult && result != BindResult.SUCCESS) {
                        sender.sendMessage("${ChatColor.RED}${getUnbindFailedMessage(result)}")
                    }
                }
                sender.sendMessage("${ChatColor.GOLD}${lang.getTranslation("command.camera.bind.success")}")
                return@launch
            }

            // 找到玩家
            if (player !is CraftPlayer) sender.sendMessage("${ChatColor.RED}${lang.getTranslation("command.camera.bind.notsupport")}")

            sender.sendMessage("${ChatColor.GOLD}${lang.getTranslation("command.camera.binding.player")}", player!!.name)

            val result = camera.bindCamera(player as CraftPlayer)
            if (result != BindResult.SUCCESS) {
                sender.sendMessage("${ChatColor.RED}${getBindFailedMessage(result)}")
                return@launch
            }

            sender.sendMessage("${ChatColor.GOLD}${lang.getTranslation("command.camera.bind.success")}")
        }

        return true
    }

    private fun cameraUnbindCommand(sender: CommandSender, cameraName: String): Boolean {
        CoroutineScope(Dispatchers.Default).launch {
            val camera = plugin.cameras.find { it.name == cameraName }

            if (camera == null) {
                sender.sendMessage("${ChatColor.RED}${lang.getTranslation("command.camera.bind.notfound.camera")}")
                return@launch
            }

            camera.autoSwitch = false
            val result = camera.unbindCamera()
            if (result != UnbindResult.SUCCESS) {
                sender.sendMessage("${ChatColor.RED}${getUnbindFailedMessage(result)}")
                return@launch
            }

            sender.sendMessage("${ChatColor.GOLD}${lang.getTranslation("command.camera.unbind.success")}")
        }
        return true
    }

    private fun positionListCommand(sender: CommandSender): Boolean {
        if (config.mainConfig.fixedCameraPosition.isEmpty()) {
            sender.sendMessage("${ChatColor.RED}${lang.getTranslation("command.position.list.notfound")}")
            return true
        }

        sender.sendMessage("\n")
        sender.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}===============================")
        sender.sendMessage("${ChatColor.GOLD} ${lang.getTranslation("command.position.list.title")}")
        config.mainConfig.fixedCameraPosition.forEach {
            sender.sendMessage("\n")
            sender.sendMessage("${ChatColor.GOLD} ● ${lang.getTranslation("command.position.list.position.name")}${ChatColor.DARK_AQUA}${it.name}")
            sender.sendMessage("${ChatColor.GOLD}   ${lang.getTranslation("command.position.list.position.world")}${ChatColor.DARK_AQUA}${it.world}")
            sender.sendMessage("${ChatColor.GOLD}   ${lang.getTranslation("command.position.list.position.x")}${ChatColor.DARK_AQUA}${it.x}")
            sender.sendMessage("${ChatColor.GOLD}   ${lang.getTranslation("command.position.list.position.y")}${ChatColor.DARK_AQUA}${it.y}")
            sender.sendMessage("${ChatColor.GOLD}   ${lang.getTranslation("command.position.list.position.z")}${ChatColor.DARK_AQUA}${it.z}")
            sender.sendMessage("${ChatColor.GOLD}   ${lang.getTranslation("command.position.list.position.yaw")}${ChatColor.DARK_AQUA}${it.yaw}")
            sender.sendMessage("${ChatColor.GOLD}   ${lang.getTranslation("command.position.list.position.pitch")}${ChatColor.DARK_AQUA}${it.pitch}")
        }
        sender.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}===============================")
        sender.sendMessage("\n")
        return true
    }

    private fun autoSwitchCommand(sender: CommandSender, cameraName: String, status: String): Boolean {
        val camera = plugin.cameras.find { it.name == cameraName }

        if (camera == null) {
            sender.sendMessage("${ChatColor.RED}${lang.getTranslation("command.camera.autoswitch.notfound")}")
            return true
        }

        when (status) {
            "on" -> {
                camera.autoSwitch = true
                sender.sendMessage("${ChatColor.GOLD}${lang.getTranslation("command.camera.autoswitch.success")}")
            }
            "off" -> {
                camera.autoSwitch = false
                sender.sendMessage("${ChatColor.GOLD}${lang.getTranslation("command.camera.autoswitch.success")}")
            }
            else -> {
                sender.sendMessage("${ChatColor.RED}${lang.getTranslation("command.camera.autoswitch.wrong")}")
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
            BindResult.SUCCESS -> null
        }
    }

    private fun getUnbindFailedMessage(result: UnbindResult): String? {
        return when (result) {
            UnbindResult.CLIENT_NOT_RESPONDING -> lang.getTranslation("unbind.failed.cause.clientnotrespond")
            UnbindResult.NOT_BOUND_CAMERA -> lang.getTranslation("unbind.failed.cause.notboundcamera")
            UnbindResult.SUCCESS -> null
        }
    }

}