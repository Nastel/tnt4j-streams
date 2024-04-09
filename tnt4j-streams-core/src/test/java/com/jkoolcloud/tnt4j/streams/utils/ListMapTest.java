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

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * @author akausinis
 * @version 1.0
 */
public class ListMapTest {

	protected Map<String, String> initTest() {
		Map<String, String> hMap1 = new HashMap<>();
		hMap1.put("key11", "value11");
		hMap1.put("key12", "value12");
		hMap1.put("key13", "value13");

		Map<String, String> hMap2 = new HashMap<>();
		hMap2.put("key21", "value21");
		hMap2.put("key22", "value22");
		hMap2.put("key12", "value23");
		hMap2.put("key23", "value24");

		Map<String, String> hMap3 = new HashMap<>();
		hMap3.put("key31", "value31");
		hMap3.put("key32", "value32");
		hMap3.put("key33", "value33");
		hMap3.put("key34", "value34");
		hMap3.put("key35", "value35");

		Map<String, String> lMap = new ListMap<>();
		lMap.putAll(hMap1);
		lMap.putAll(hMap2);
		lMap.putAll(hMap3);

		return lMap;
	}

	@Test
	public void testSize() {
		Map<String, String> lMap = initTest();

		assertFalse(lMap.isEmpty(), "Expecting non-empty map");

		assertEquals(12, lMap.size(), "Map size mismatch");
		assertEquals(11, lMap.keySet().size(), "Map key set size mismatch");
		assertEquals(12, lMap.values().size(), "Map values list size mismatch");
		assertEquals(12, lMap.entrySet().size(), "Map entry set size mismatch");
	}

	@Test
	public void testContainsKey() {
		Map<String, String> lMap = initTest();

		assertTrue(lMap.containsKey("key12"), "Missing expected key");
		assertTrue(lMap.containsKey("key23"), "Missing expected key");
		assertTrue(lMap.containsKey("key35"), "Missing expected key");
		assertFalse(lMap.containsKey("key14"), "Got unexpected key");
	}

	@Test
	public void testContainsValue() {
		Map<String, String> lMap = initTest();

		assertTrue(lMap.containsValue("value12"), "Missing expected value");
		assertTrue(lMap.containsValue("value23"), "Missing expected value");
		assertTrue(lMap.containsValue("value34"), "Missing expected value");
		assertFalse(lMap.containsValue("value25"), "Got unexpected value");
	}

	@Test
	public void testGet() {
		Map<String, String> lMap = initTest();

		assertEquals(lMap.get("key34"), "value34", "Unexpected key-value mapping");
		assertEquals(lMap.get("key12"), "value12", "Unexpected key-value mapping");
		assertEquals(lMap.get("key22"), "value22", "Unexpected key-value mapping");
	}

	@Test
	public void testRemove() {
		Map<String, String> lMap = initTest();

		String str = lMap.remove("key12");
		assertEquals("value23", str, "Unexpected key-value mapping");
		assertEquals(10, lMap.size(), "Map size mismatch");
		assertEquals(10, lMap.keySet().size(), "Map key set size mismatch");
		assertEquals(10, lMap.values().size(), "Map values list size mismatch");
		assertEquals(10, lMap.entrySet().size(), "Map entry set size mismatch");
	}

	@Test
	public void testPut() {
		Map<String, String> lMap = initTest();

		String str = lMap.put("key01", "value01");
		assertEquals(null, str, "Unexpected key-value mapping");
		assertEquals(13, lMap.size(), "Map size mismatch");
		assertEquals(12, lMap.keySet().size(), "Map key set size mismatch");
		assertEquals(13, lMap.values().size(), "Map values list size mismatch");
		assertEquals(13, lMap.entrySet().size(), "Map entry set size mismatch");

		str = lMap.put("key22", "value02");
		assertEquals(null, str, "Unexpected key-value mapping");
		assertEquals(14, lMap.size(), "Map size mismatch");
		assertEquals(12, lMap.keySet().size(), "Map key set size mismatch");
		assertEquals(14, lMap.values().size(), "Map values list size mismatch");
		assertEquals(14, lMap.entrySet().size(), "Map entry set size mismatch");
	}

	@Test
	public void testClear() {
		Map<String, String> lMap = initTest();

		lMap.clear();
		assertTrue(lMap.isEmpty(), "Expecting empty map");
		assertFalse(lMap.containsKey("key35"), "Unexpected key found");
		assertFalse(lMap.containsValue("value23"), "Unexpected value found");
		assertFalse(lMap.containsValue("value25"), "Unexpected value found");
	}
}
