package net.auoeke.wheel.util

import java.util.function.BiConsumer

class ObservableMap<K, V> : HashMap<K, V?>() {
    private val observers: MutableList<BiConsumer<in K, in V?>> = ArrayList()

    override fun put(key: K, value: V?): V? {
        val old = super.put(key, value)
        observers.forEach {it.accept(key, value)}

        return old
    }

    fun observe(observer: BiConsumer<in K, in V?>) {
        observers.add(observer)
    }

    fun all(action: BiConsumer<in K, in V?>) {
        this.observe(action)
        this.forEach(action)
    }
}
