package cn.cutemc.autostreamingassistant.bukkit.logger

import cn.cutemc.autostreamingassistant.bukkit.AutoStreamingAssistant
import java.util.function.Supplier
import java.util.logging.LogRecord
import java.util.logging.Logger

class PluginLogger(private val defLogger: Logger) : Logger(defLogger.name, defLogger.resourceBundleName) {

    private val consoleSender by lazy { AutoStreamingAssistant.INSTANCE.server.consoleSender }

    override fun info(msg: String?) {
        consoleSender.sendMessage("[${defLogger.name}] ${msg}")
    }

    override fun info(msgSupplier: Supplier<String>?) {
        consoleSender.sendMessage("[${defLogger.name}] ${msgSupplier?.get()}")
    }

    override fun warning(msg: String?) {
        consoleSender.sendMessage("[${defLogger.name}] ${msg}")
    }

    override fun warning(msgSupplier: Supplier<String>?) {
        consoleSender.sendMessage("[${defLogger.name}] ${msgSupplier?.get()}")
    }

    override fun log(record: LogRecord?) {
        consoleSender.sendMessage("[${defLogger.name}] ${record?.message}")
    }

}