package cn.cutemc.autostreamingassistant.bukkit.config

data class PluginConfig(val language: String, val cameraNames: Array<String>, val autoSwitchPlayer: Boolean, val switchPlayerInterval: Int, val fixedCameraPosition: Array<CameraPosition>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PluginConfig

        if (autoSwitchPlayer != other.autoSwitchPlayer) return false
        if (switchPlayerInterval != other.switchPlayerInterval) return false
        return fixedCameraPosition.contentEquals(other.fixedCameraPosition)
    }

    override fun hashCode(): Int {
        var result = autoSwitchPlayer.hashCode()
        result = 31 * result + switchPlayerInterval
        result = 31 * result + fixedCameraPosition.contentHashCode()
        return result
    }
}