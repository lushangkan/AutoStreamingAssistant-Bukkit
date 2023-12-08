package cn.cutemc.autostreamingassistant.bukkit.network

enum class UnbindResult {
    NOT_BOUND_CAMERA,
    SUCCESS;

    override fun toString(): String {
        return this.name.lowercase()
    }

    fun fromString(string: String): UnbindResult {
        return UnbindResult.valueOf(string.uppercase())
    }
}

enum class BindResult {
    CLIENT_NOT_RESPONDING,
    NOT_FOUND_PLAYER,
    NOT_AT_NEAR_BY,
    WORLD_IS_NULL,
    PLAYER_IS_NULL,
    SUCCESS;

    override fun toString(): String {
        return this.name.lowercase()
    }

    fun fromString(string: String): BindResult {
        return BindResult.valueOf(string.uppercase())
    }
}

enum class ClientStatus {

    READY,
    BOUND;

    override fun toString(): String {
        return this.name.lowercase()
    }

    fun fromString(string: String): ClientStatus {
        return ClientStatus.valueOf(string.uppercase())
    }
}