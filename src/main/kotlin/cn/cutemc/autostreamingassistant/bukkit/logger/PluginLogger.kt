package cn.cutemc.autostreamingassistant.bukkit.logger

import cn.cutemc.autostreamingassistant.bukkit.AutoStreamingAssistant
import java.util.*
import java.util.function.Supplier
import java.util.logging.Level
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
        defLogger.warning(msg)
    }

    override fun warning(msgSupplier: Supplier<String>?) {
        defLogger.warning(msgSupplier?.get())
    }

    override fun severe(msg: String?) {
        defLogger.severe(msg)
    }

    override fun severe(msgSupplier: Supplier<String>?) {
        defLogger.severe(msgSupplier?.get())
    }

    override fun log(record: LogRecord?) {
        defLogger.log(record)
    }

    override fun log(level: Level?, msg: String?) {
        defLogger.log(level, msg)
    }

    override fun log(level: Level?, msgSupplier: Supplier<String>?) {
        defLogger.log(level, msgSupplier)
    }

    override fun log(level: Level?, msg: String?, param1: Any?) {
        defLogger.log(level, msg, param1)
    }


    override fun log(level: Level?, msg: String?, params: Array<Any>?) {
        defLogger.log(level, msg, params)
    }

    override fun logp(level: Level?, sourceClass: String?, sourceMethod: String?, msg: String?) {
        defLogger.logp(level, sourceClass, sourceMethod, msg)
    }

    override fun logp(level: Level?, sourceClass: String?, sourceMethod: String?, msgSupplier: Supplier<String>?) {
        defLogger.logp(level, sourceClass, sourceMethod, msgSupplier)
    }

    override fun logp(level: Level?, sourceClass: String?, sourceMethod: String?, msg: String?, param1: Any?) {
        defLogger.logp(level, sourceClass, sourceMethod, msg, param1)
    }

    override fun logp(level: Level?, sourceClass: String?, sourceMethod: String?, msg: String?, params: Array<Any>?) {
        defLogger.logp(level, sourceClass, sourceMethod, msg, params)
    }

    override fun logp(level: Level?, sourceClass: String?, sourceMethod: String?, msg: String?, thrown: Throwable?) {
        defLogger.logp(level, sourceClass, sourceMethod, msg, thrown)
    }

    override fun entering(sourceClass: String?, sourceMethod: String?, params: Array<Any>?) {
        defLogger.entering(sourceClass, sourceMethod, params)
    }

    override fun exiting(sourceClass: String?, sourceMethod: String?, result: Any?) {
        defLogger.exiting(sourceClass, sourceMethod, result)
    }

    override fun throwing(sourceClass: String?, sourceMethod: String?, thrown: Throwable?) {
        defLogger.throwing(sourceClass, sourceMethod, thrown)
    }

    override fun config(msgSupplier: Supplier<String>?) {
        defLogger.config(msgSupplier)
    }

    override fun fine(msgSupplier: Supplier<String>?) {
        defLogger.fine(msgSupplier)
    }

    override fun finer(msgSupplier: Supplier<String>?) {
        defLogger.finer(msgSupplier)
    }

    override fun finest(msgSupplier: Supplier<String>?) {
        defLogger.finest(msgSupplier)
    }

    override fun logrb(
        level: Level?,
        sourceClass: String?,
        sourceMethod: String?,
        bundle: ResourceBundle?,
        msg: String?,
        thrown: Throwable?
    ) {
        super.logrb(level, sourceClass, sourceMethod, bundle, msg, thrown)
    }

    override fun log(level: Level?, thrown: Throwable?, msgSupplier: Supplier<String>?) {
        super.log(level, thrown, msgSupplier)
    }

    override fun log(level: Level?, msg: String?, thrown: Throwable?) {
        super.log(level, msg, thrown)
    }

    override fun logrb(
        level: Level?,
        sourceClass: String?,
        sourceMethod: String?,
        bundle: ResourceBundle?,
        msg: String?,
        vararg params: Any?
    ) {
        super.logrb(level, sourceClass, sourceMethod, bundle, msg, *params)
    }

    override fun logp(
        level: Level?,
        sourceClass: String?,
        sourceMethod: String?,
        thrown: Throwable?,
        msgSupplier: Supplier<String>?
    ) {
        super.logp(level, sourceClass, sourceMethod, thrown, msgSupplier)
    }

    override fun logrb(level: Level?, bundle: ResourceBundle?, msg: String?, thrown: Throwable?) {
        super.logrb(level, bundle, msg, thrown)
    }

    override fun logrb(level: Level?, bundle: ResourceBundle?, msg: String?, vararg params: Any?) {
        super.logrb(level, bundle, msg, *params)
    }

    override fun isLoggable(level: Level?): Boolean {
        return super.isLoggable(level)
    }

    override fun fine(msg: String?) {
        super.fine(msg)
    }

    override fun finer(msg: String?) {
        super.finer(msg)
    }

    override fun finest(msg: String?) {
        super.finest(msg)
    }
}