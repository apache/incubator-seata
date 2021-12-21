package io.seata.common.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;

public class LowerCaseLinkHashMap<V> implements Map<String, V> {

    private final LinkedHashMap<String, V> targetMap;

    public LowerCaseLinkHashMap() {
        targetMap = new LinkedHashMap<>(16, 1.001f);
    }

    public LowerCaseLinkHashMap(Integer initialCapacity, float loadFactor) {
        targetMap = new LinkedHashMap<>(initialCapacity, loadFactor);

    }

    public LowerCaseLinkHashMap(Map<String, V> map) {
        targetMap = new LinkedHashMap<>(16, 1.001f);

        putAll(map);
    }

    @Override
    public int size() {
        return targetMap.size();
    }

    @Override
    public boolean isEmpty() {
        return targetMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object o) {
        if (o instanceof String) {
            return targetMap.containsKey(((String) o).toLowerCase());
        }

        return false;
    }

    @Override
    public boolean containsValue(Object o) {
        return targetMap.containsKey(o);
    }

    @Override
    public V get(Object o) {
        if (o instanceof String) {
            return targetMap.get(((String) o).toLowerCase());
        }

        return null;
    }

    @Override
    public V put(String s, V v) {
        return targetMap.put(s.toLowerCase(), v);
    }

    @Override
    public V remove(Object o) {
        if (o instanceof String) {
            return targetMap.remove(((String) o).toLowerCase());
        }

        return null;
    }

    @Override
    public void putAll(Map<? extends String, ? extends V> map) {
        map.forEach((k, v) -> targetMap.put(k.toLowerCase(), v));
    }

    @Override
    public void clear() {
        targetMap.clear();
    }

    @Override
    public Set<String> keySet() {
        return targetMap.keySet();
    }

    @Override
    public Collection<V> values() {
        return targetMap.values();
    }

    @Override
    public Set<Entry<String, V>> entrySet() {
        return targetMap.entrySet();
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        if (key instanceof String) {
            return Map.super.getOrDefault(((String) key).toLowerCase(), defaultValue);
        }

        return defaultValue;
    }

    @Override
    public void replaceAll(BiFunction<? super String, ? super V, ? extends V> function) {
        Map.super.replaceAll(function);
    }

    @Override
    public V putIfAbsent(String key, V value) {
        return Map.super.putIfAbsent(key.toLowerCase(), value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        if (key instanceof String) {
            return Map.super.remove(((String) key).toLowerCase(), value);
        }

        return false;
    }

    @Override
    public boolean replace(String key, V oldValue, V newValue) {
        return Map.super.replace(key.toLowerCase(), oldValue, newValue);
    }

    @Override
    public V replace(String key, V value) {
        return Map.super.replace(key.toLowerCase(), value);
    }

    @Override
    public V computeIfAbsent(String key, Function<? super String, ? extends V> mappingFunction) {
        return Map.super.computeIfAbsent(key.toLowerCase(), mappingFunction);
    }

    @Override
    public V computeIfPresent(String key, BiFunction<? super String, ? super V, ? extends V> remappingFunction) {
        return Map.super.computeIfPresent(key.toLowerCase(), remappingFunction);
    }

    @Override
    public V compute(String key, BiFunction<? super String, ? super V, ? extends V> remappingFunction) {
        return Map.super.compute(key.toLowerCase(), remappingFunction);
    }

    @Override
    public V merge(String key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return Map.super.merge(key.toLowerCase(), value, remappingFunction);
    }

    @Override
    protected LowerCaseLinkHashMap<V> clone() throws CloneNotSupportedException {
        return new LowerCaseLinkHashMap<>(this);
    }
}
