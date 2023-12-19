package cn.cutemc.autostreamingassistant.bukkit.events

class EventBus <T: Event> {

    val listeners: MutableMap<(T) -> Unit, Boolean> = mutableMapOf()

    /**
     * 注册监听器
     * @param listener 监听器
     */
    fun register(listener: (T) -> Unit) {
        register(listener, false)
    }

    /**
     * 注册监听器
     *
     * @param listener 监听器
     * @param once 是否只监听一次
     */
    fun register(listener: (T) -> Unit, once: Boolean) {
        listeners[listener] = once
    }

    /**
     * 取消注册监听器
     *
     * @param listener 监听器
     */
    fun unregister(listener: (T) -> Unit) {
        listeners.remove(listener)
    }

    /**
     * 注册一次性监听器
     *
     * @param listener 监听器
     */
    fun registerOnce(listener: (T) -> Unit) {
        register(listener, true)
    }

    /**
     * 发布事件
     *
     * @param event 事件
     */
    fun post(event: T) {
        listeners.forEach { (listener, once) ->
            listener(event)
            if (once) {
                listeners.remove(listener)
            }
        }
    }

}