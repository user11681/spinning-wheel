package net.auoeke.wheel.util

class ObservableMap<K, V> : HashMap<K, V>() {
    private val observers: MutableList<(K, V) -> Unit> = ArrayList()

    override fun put(key: K, value: V): V? {
        val old = super.put(key, value)
        this.observers.forEach {it(key, value)}

        return old
    }

    fun observe(observer: (K, V) -> Unit) {
        this.observers.add(observer)
    }

    fun all(action: (K, V) -> Unit) {
        this.observe(action)
        this.forEach(action)
    }
}
