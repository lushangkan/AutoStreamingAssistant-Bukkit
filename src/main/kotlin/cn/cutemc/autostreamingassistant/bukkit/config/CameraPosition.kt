package cn.cutemc.autostreamingassistant.bukkit.config

data class CameraPosition(
    val name: String,
    val world: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CameraPosition) return false

        if (name != other.name) return false
        if (world != other.world) return false
        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false
        if (yaw != other.yaw) return false
        if (pitch != other.pitch) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + world.hashCode()
        result = 31 * result + x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + z.hashCode()
        result = 31 * result + yaw.hashCode()
        result = 31 * result + pitch.hashCode()
        return result
    }
}