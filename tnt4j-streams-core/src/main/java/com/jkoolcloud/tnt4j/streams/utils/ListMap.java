/*
 * Copyright 2014-2024 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jkoolcloud.tnt4j.streams.utils;

import java.util.*;

/**
 * It is custom map implementation based on maps list. First list element represents map which is used for entries added
 * by {@link #put(Object, Object)} and over constructor.
 * <p>
 * Other list entries are for maps refereed by {@link #putAll(java.util.Map)}.
 * <p>
 * All other methods like {@link #size()}, {@link #values()} and etc. takes all the list elements into account.
 * <p>
 * Benefit of such map implementation is that {@link #putAll(java.util.Map)} does not make in memory copying of another
 * map entries, but makes a reference trough the list entry. So dealing with large set of map entries it makes this
 * implementation memory efficient, yet provides all the base map operations available.
 * 
 * @param <K>
 *            the type of keys maintained by this map
 * @param <V>
 *            the type of mapped values
 *
 * @version $Revision: 1 $
 */
public class ListMap<K, V> implements Map<K, V> {
	private List<Map<K, V>> mapList = new ArrayList<>(5);

	/**
	 * Constructs an empty {@code ListMap}.
	 */
	public ListMap() {
		mapList.add(new HashMap<>());
	}

	/**
	 * Constructs a new {@code ListMap} by adding specified {@code Map} reference into the list.
	 * 
	 * @param map
	 *            the map instance to add to the references list
	 * @throws NullPointerException
	 *             if the specified map is {@code null}
	 */
	public ListMap(Map<K, V> map) {
		this();

		mapList.add(map);
	}

	@Override
	public int size() {
		int size = 0;

		for (Map<K, V> map : mapList) {
			size += map.size();
		}

		return size;
	}

	@Override
	public boolean isEmpty() {
		return mapList.size() == 1 && mapList.get(0).isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		for (Map<K, V> map : mapList) {
			if (map.containsKey(key)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean containsValue(Object value) {
		for (Map<K, V> map : mapList) {
			if (map.containsValue(value)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public V get(Object key) {
		for (Map<K, V> map : mapList) {
			V value = map.get(key);
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	@Override
	public V put(K key, V value) {
		return mapList.get(0).put(key, value);
	}

	@Override
	public V remove(Object key) {
		V value = null;
		for (Map<K, V> map : mapList) {
			V v = map.remove(key);
			value = v == null ? value : v;
		}
		return value;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void putAll(Map<? extends K, ? extends V> m) {
		if (mapList.contains(m)) {
			return;
		}
		mapList.add((Map<K, V>) m);
	}

	@Override
	public void clear() {
		mapList.get(0).clear();

		for (int i = mapList.size() - 1; i > 0; i--) {
			mapList.remove(i);
		}
	}

	@Override
	public Set<K> keySet() {
		Set<K> keySet = new HashSet<>();

		for (Map<K, V> map : mapList) {
			keySet.addAll(map.keySet());
		}
		return keySet;
	}

	@Override
	public Collection<V> values() {
		Collection<V> values = new ArrayList<>();

		for (Map<K, V> map : mapList) {
			values.addAll(map.values());
		}
		return values;
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		Set<Entry<K, V>> entrySet = new HashSet<>();

		for (Map<K, V> map : mapList) {
			entrySet.addAll(map.entrySet());
		}
		return entrySet;
	}
}
