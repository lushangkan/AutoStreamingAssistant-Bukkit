package cn.cutemc.autostreamingassistant.bukkit.events

data class EventData<T: Event>(val callback: (T) -> Unit, val once: Boolean)
