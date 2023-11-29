package cn.cutemc.autostreamingassistant.bukkit.lang

import cn.cutemc.autostreamingassistant.bukkit.AutoStreamingAssistant
import com.google.gson.Gson
import org.apache.commons.io.FileUtils
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipFile

class PluginLang {

    private lateinit var langFiles: Map<String, File>

    init {
        reload()
    }

    fun getTranslation(lang: String, key: String): String {
        val langFile = langFiles[lang] ?: return key
        val langJson = Files.readString(langFile.toPath())
        return (Gson().fromJson(langJson, Map::class.java)[key] ?: key).toString()
    }

    fun getTranslation(key: String): String {
        val lang = AutoStreamingAssistant.INSTANCE.config.getString("language") ?: "en_us"
        return getTranslation(lang, key)
    }

    private fun saveLangFiles() {
        val plugin: JavaPlugin = AutoStreamingAssistant.INSTANCE
        val jarFile: Any? = plugin::class.java.getMethod("getFile").invoke(plugin)

        if (jarFile !is File) throw RuntimeException("Cannot get jar file!")

        val jarZipFile = ZipFile(jarFile)
        val langFiles = jarZipFile.entries().asSequence().filter { it.name.startsWith("langs/") && it.name.endsWith(".json") }.toList()

        val langsFolder = File(plugin.dataFolder, "langs")

        if (!langsFolder.exists()) {
            FileUtils.createParentDirectories(langsFolder)
            langsFolder.mkdir()
        }

        langFiles.forEach {
            val langFile = File(langsFolder, it.name.substringAfterLast("/"))
            if (!langFile.exists()) {
                FileUtils.copyInputStreamToFile(jarZipFile.getInputStream(it), langFile)
            }
        }
    }

    private fun getAllLangFiles(): Map<String, File> {
        val plugin: JavaPlugin = AutoStreamingAssistant.INSTANCE
        val langsFolder = File(plugin.dataFolder, "langs")

        if (!langsFolder.exists()) {
            FileUtils.createParentDirectories(langsFolder)
            langsFolder.mkdir()
        }

        return langsFolder.listFiles()?.filter { it.isFile && it.name.endsWith(".json") }?.associateBy { it.nameWithoutExtension } ?: mapOf()
    }

    private fun reload() {
        saveLangFiles()
        langFiles = getAllLangFiles()
    }

}