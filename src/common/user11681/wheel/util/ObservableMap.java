package user11681.wheel.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;

public class ObservableMap<K, V> extends HashMap<K, V> {
    protected final List<BiConsumer<K, V>> observers = new ArrayList<>();

    @Override
    public V put(K key, V value) {
        V old = super.put(key, value);
        this.observers.forEach(observer -> observer.accept(key, value));

        return old;
    }

    public void observe(BiConsumer<K, V> observer) {
        this.observers.add(observer);
    }

    public void all(BiConsumer<K, V> action) {
        this.observe(action);
        this.forEach(action);
    }
}
