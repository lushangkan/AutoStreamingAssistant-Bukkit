package cn.cutemc.autostreamingassistant.bukkit.commands

import cn.cutemc.autostreamingassistant.bukkit.AutoStreamingAssistant
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

object PluginTabCompleter : TabCompleter {

    private val plugin by lazy { AutoStreamingAssistant.INSTANCE }
    private val config by lazy { AutoStreamingAssistant.INSTANCE.config }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?
    ): MutableList<String> {
        if (args == null) return mutableListOf()

        return when {
            args.isEmpty() -> mutableListOf("reload", "help", "version", "camera", "position")
            args.size == 1 -> handleFirstArgument(args[0])
            args.size >= 2 -> handleSecondArgument(args)
            else -> mutableListOf()
        }
    }

    private fun handleFirstArgument(arg: String): MutableList<String> {
        return when (arg) {
            "camera" -> mutableListOf("bind", "unbind", "list", "status", "autoswitch")
            "position" -> mutableListOf("list")
            else -> mutableListOf()
        }
    }

    private fun handleSecondArgument(args: Array<out String>): MutableList<String> {
        return when (args[0]) {
            "camera" -> handleCameraArguments(args)
            "position" -> handlePositionArguments(args)
            else -> mutableListOf()
        }
    }

    private fun handlePositionArguments(args: Array<out String>): MutableList<String> {
        return when (args[1]) {
            "list" -> mutableListOf()
            else -> mutableListOf()
        }
    }

    private fun handleCameraArguments(args: Array<out String>): MutableList<String> {
        return when (args[1]) {
            "bind" -> handleBindArguments(args)
            "unbind" -> handleUnbindArguments(args)
            "list" -> mutableListOf()
            "status" -> handleStatusArguments(args)
            "autoswitch" -> mutableListOf("on", "off")
            else -> mutableListOf()
        }
    }

    private fun handleBindArguments(args: Array<out String>): MutableList<String> {
        return when (args.size) {
            3 -> getCameraNames()
            4 -> getPlayerNames().plus(getFixedPositionNames()).toMutableList()
            else -> mutableListOf()
        }
    }

    private fun handleUnbindArguments(args: Array<out String>): MutableList<String> {
        return when (args.size) {
            3 -> getCameraNames()
            else -> mutableListOf()
        }
    }

    private fun handleStatusArguments(args: Array<out String>): MutableList<String> {
        return when (args.size) {
            3 -> getCameraNames()
            else -> mutableListOf()
        }
    }

    private fun getCameraNames(): MutableList<String> {
        return plugin.cameras.map { it.name }.toMutableList()
    }

    private fun getPlayerNames(): MutableList<String> {
        return plugin.server.onlinePlayers.map { it.name }.toMutableList()
    }

    private fun getFixedPositionNames(): MutableList<String> {
        return config.mainConfig.fixedCameraPosition.map { it.name }.toMutableList()
    }
}