package cn.cutemc.autostreamingassistant.bukkit.commands

import cn.cutemc.autostreamingassistant.bukkit.AutoStreamingAssistant
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

object PluginCommands : CommandExecutor {

    private val lang by lazy { AutoStreamingAssistant.INSTANCE.lang }

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
                            2 -> {
                                cameraStatusCommand(sender)
                            }

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
                    else -> {
                        // 运行帮助
                        return wrongCommand(sender)
                    }
                }
            }
        }

        return wrongCommand(sender)
    }

    private fun wrongCommand(sender: CommandSender): Boolean {
        sender.sendMessage("${ChatColor.RED}${lang.getTranslation("command.wrong")}")
        return true
    }

    private fun mainCommand(sender: CommandSender): Boolean {

        return helpCommand(sender)
    }

    private fun reloadCommand(sender: CommandSender): Boolean {
        return true

    }

    private fun helpCommand(sender: CommandSender): Boolean {
        sender.sendMessage("\n")
        sender.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}================================================")
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
        sender.sendMessage("${ChatColor.GOLD} ● /autostreamingassistantserver camera bind <${lang.getTranslation("command.help.command.args.player")}> <${lang.getTranslation("command.help.command.args.camera")}>${ChatColor.GRAY} - ${ChatColor.DARK_AQUA}${lang.getTranslation("command.help.command.camera.bind")}")
        sender.sendMessage("${ChatColor.GOLD} ● /autostreamingassistantserver camera unbind <${lang.getTranslation("command.help.command.args.player")}>${ChatColor.GRAY} - ${ChatColor.DARK_AQUA}${lang.getTranslation("command.help.command.camera.unbind")}")
        sender.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}================================================")
        sender.sendMessage("\n")

        return true
    }

    private fun versionCommand(sender: CommandSender): Boolean {
        return true

    }

    private fun cameraListCommand(sender: CommandSender): Boolean {
        return true

    }

    private fun cameraStatusCommand(sender: CommandSender, cameraName: String): Boolean {
        return true

    }

    private fun cameraStatusCommand(sender: CommandSender): Boolean {
        return true

    }

    private fun cameraBindCommand(sender: CommandSender, playerName: String, cameraName: String): Boolean {
        return true

    }

    private fun cameraUnbindCommand(sender: CommandSender, playerName: String): Boolean {
        return true
    }

}