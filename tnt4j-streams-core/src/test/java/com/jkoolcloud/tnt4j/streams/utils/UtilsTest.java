/*
 * Copyright 2014-2018 JKOOL, LLC.
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

import static org.junit.Assert.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.*;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;

import com.jkoolcloud.tnt4j.core.OpType;

/**
 * @author akausinis
 * @version 1.0
 */
public class UtilsTest {

	private static final String TEST = "TEST"; // NON-NLS

	@Test
	public void testBase64Encode() {
		byte[] resultDecode = Utils.base64Decode(TEST.getBytes());
		byte[] resultEncode = Utils.base64Encode(resultDecode);
		assertArrayEquals(resultEncode, TEST.getBytes());
	}

	@Test
	public void testBase64Decode() {
		byte[] resultEncode = Utils.base64Encode(TEST.getBytes());
		byte[] resultDecode = Utils.base64Decode(resultEncode);
		assertArrayEquals(resultDecode, TEST.getBytes());
	}

	// @Test
	// public void testEncodeHex()
	// {
	// final char[] resultEncode = Utils.encodeHex(TEST.getBytes());
	// final byte[] resultDecode = Utils.decodeHex(resultEncode.toString());
	// assertArrayEquals(resultDecode, TEST.getBytes());
	// }

	@Test
	public void testMapOpType() {
		int opTypeCount = 20;
		Map<String, OpType> opTypes = new HashMap<>(opTypeCount);

		for (int i = 0; i <= opTypeCount; i++) {
			OpType opType = Utils.mapOpType(i);
			if (opType == OpType.STOP) {
				////////////////////////////
				opTypes.put("END", opType); // NON-NLS
				///////////////////////////
			} else {
				opTypes.put(opType.name(), opType);
			}
		}

		Set<Map.Entry<String, OpType>> entrySet = opTypes.entrySet();
		for (Map.Entry<String, OpType> entry : entrySet) {
			assertEquals(entry.getValue(), Utils.mapOpType(entry.getKey()));
		}
	}

	@Test
	public void testIsWildcardFileName() {
		String N_WILDC = "c:/Users/Default.migrated/AppData/Local/Microsoft/Windows/INetCache/"; // NON-NLS
		String WILDC = "c:/Windows/schemas/TSWorkSpace/*.*"; // NON-NLS
		String WILDC2 = "c:/Windows/schemas/TSWorkSpace/*.*"; // NON-NLS
		String WILDC3 = "c:/Windows/schemas/TSWorkSpa?e/*.*"; // NON-NLS
		String EMPTY = "";

		assertFalse(Utils.isWildcardString(N_WILDC));
		assertTrue(Utils.isWildcardString(WILDC));
		assertTrue(Utils.isWildcardString(WILDC2));
		assertTrue(Utils.isWildcardString(WILDC3));
		assertFalse(Utils.isWildcardString(EMPTY));
	}

	@Test
	public void testGetFirstNewer() throws Exception {
		int count = 5;
		List<Path> files = new ArrayList<>();
		for (int i = 0; i <= count; i++) {
			File tempFile = File.createTempFile("TEST", ".TST");
			if (count / 2 >= i) {
				(new Date()).getTime();
			}
			files.add(tempFile.toPath());
			Thread.sleep(300);
		}
		Path[] fArray = files.toArray(new Path[files.size()]);
		Path result = Utils.getFirstNewer(fArray, null);
		assertEquals(files.get(files.size() - 1), result);

		result = Utils.getFirstNewer(fArray,
				Files.getLastModifiedTime(files.get(0), LinkOption.NOFOLLOW_LINKS).toMillis());
		assertEquals(files.get(1), result);

		result = Utils.getFirstNewer(fArray,
				Files.getLastModifiedTime(files.get(3), LinkOption.NOFOLLOW_LINKS).toMillis());
		assertEquals(files.get(4), result);

		ArrayUtils.reverse(fArray);
		Path result2 = Utils.getFirstNewer(fArray,
				Files.getLastModifiedTime(files.get(3), LinkOption.NOFOLLOW_LINKS).toMillis());
		assertEquals(result, result2);

		for (Path fileToRemove : files) {
			Files.delete(fileToRemove);
		}
	}

	@Test
	public void testFromJsonToMap() {
		Map<String, String> testMap = new HashMap<String, String>() {
			private static final long serialVersionUID = 1L;
			{
				put("TEST", "TESTVAL"); // NON-NLS
				put("TEST2", "TESTVAL2"); // NON-NLS
				put("TEST3", "TESTVAL3"); // NON-NLS
			}
		};
		String testString = "{\"TEST2\"=\"TESTVAL2\", \"TEST3\"=\"TESTVAL3\", \"TEST\"=\"TESTVAL\"}"; // NON-NLS
		// Gson gson = new Gson();
		// final String json = gson.toJson(testMap);
		Map<String, ?> result = Utils.fromJsonToMap(testString, false);
		assertEquals(testMap, result);
		result = Utils.fromJsonToMap(testString.getBytes(), false);
		assertEquals(testMap, result);
		result = Utils.fromJsonToMap(toReader(testString), false);
		assertEquals(testMap, result);
		result = Utils.fromJsonToMap(toInputStream(testString), false);
		assertEquals(testMap, result);

	}

	public static StringReader toReader(String testString) {
		return new StringReader(testString);
	}

	public static ByteArrayInputStream toInputStream(String testString) {
		return new ByteArrayInputStream(testString.getBytes());
	}

	@Test
	public void testGetStringLine() throws IOException {
		String testString = "TEST \n TEST1 \n TEST2 \n TEST3 \n TEST4 \n TEST5 \n"; // NON-NLS
		String testStringLine = "TEST "; // NON-NLS
		String result = Utils.getStringLine(testString);
		assertEquals(testStringLine, result);

		result = Utils.getStringLine(testString.getBytes());
		assertEquals(testStringLine, result);

		Reader rdr = toReader(testString);
		result = Utils.getStringLine(rdr);
		assertEquals(testStringLine, result);
		Utils.close(rdr);

		rdr = new BufferedReader(toReader(testString));
		result = Utils.getStringLine(rdr);
		assertEquals(testStringLine, result);
		Utils.close(rdr);

		InputStream is = toInputStream(testString);
		result = Utils.getStringLine(is);
		assertEquals(testStringLine, result);
		Utils.close(is);
	}

	@Test
	public void testGetTags() {
		String testStrig = "TAG1,TAG2,TAG3"; // NON-NLS
		String[] expected = { "TAG1", "TAG2", "TAG3" }; // NON-NLS
		String[] result = Utils.getTags(testStrig);
		assertArrayEquals(expected, result);

		result = Utils.getTags(expected);
		assertArrayEquals(expected, result);

		List<String> list = Arrays.asList(expected);
		result = Utils.getTags(list);
		assertArrayEquals(expected, result);

		result = Utils.getTags(this);
		assertTrue(result != null && result.length == 1);
	}

	@Test
	public void testCleanActivityData() {
		// {\"sinkName\":\"TNT4JStreams\",\"chanelName\":\"memoryChannel\",\"headers\":{},\"body\":\"127.0.0.1
		// - - [26/Nov/2015:16:26:21 +0200] \\\"POST
		// /gvm_java/gvm/services/OperatorWebService HTTP/1.1\\\" 200 380\\r\"}
		// String testStrig = "line\\r"; // NON-NLS
		// String testStrig2 = "line\\n"; // NON-NLS
		// String expected = "line"; // NON-NLS
		// assertEquals(expected, Utils.cleanActivityData(testStrig));
		// assertEquals(expected, Utils.cleanActivityData(testStrig2));
	}

	@Test
	public void testRemoveMapEntryByPath() {
		Map<String, Object> root = new HashMap<>();

		root.put("RootKey", "RootValue");

		Map<String, Object> level1 = new HashMap<>();
		level1.put("Level1Key", "Level1Value");
		root.put("RootBranch", level1);

		Map<String, Object> level2 = new HashMap<>();
		level2.put("Level2Key", "Level2Value");
		level1.put("Level1Branch", level2);

		Set<String[]> accessedPaths = new HashSet<>();
		System.out.println(Utils.getMapValueByPath("RootBranch.Level1Key", root, accessedPaths));
		System.out.println(Utils.getMapValueByPath("RootBranch.Level1Branch", root, accessedPaths));
		System.out.println(Utils.getMapValueByPath("RootBranch.Level1Branch.Level2Key", root, accessedPaths));
		Object finalMapObj = Utils.getMapValueByPath("#", root, accessedPaths);

		assertNotNull(finalMapObj);

		@SuppressWarnings("unchecked")
		Map<String, Object> finalMap = (Map<String, Object>) finalMapObj;

		assertEquals(1, finalMap.size());
		assertEquals("RootValue", finalMap.get("RootKey"));
	}

	@Test
	public void searchFilesTest() throws IOException {
		String exampleFilesPath = ".." + "/config/*.properties"; // NON-NLS
		Utils.searchFiles(exampleFilesPath);
	}

	@Test
	public void testMakeArray() {
		String str = "Some string";
		Object[] array = new Object[] { str };

		// single element arrays
		Object[] cArray = Utils.makeArray(str);
		assertTrue(cArray[0] instanceof String);
		assertTrue(cArray instanceof String[]);

		cArray = Utils.makeArray(array);
		assertTrue(cArray[0] instanceof String);
		assertTrue(cArray instanceof String[]);

		cArray = Utils.makeArray(array, Object.class);
		assertTrue(cArray[0] instanceof String);
		assertFalse(cArray instanceof String[]);
		assertTrue(cArray instanceof Object[]);

		// multi element arrays
		array = new Collection[] { new ArrayList(), new Vector(), new Stack() };
		cArray = Utils.makeArray(array);
		assertTrue(cArray[0] instanceof ArrayList);
		assertFalse(cArray instanceof ArrayList[]);
		assertTrue(cArray instanceof List[]);
		assertTrue(cArray instanceof AbstractList[]);

		cArray = Utils.makeArray(array, AbstractCollection.class);
		assertTrue(cArray[0] instanceof ArrayList);
		assertFalse(cArray instanceof ArrayList[]);
		assertFalse(cArray instanceof AbstractList[]);
		assertFalse(cArray instanceof List[]);
		assertTrue(cArray instanceof AbstractCollection[]);
		assertTrue(cArray instanceof Collection[]);

		cArray = Utils.makeArray(array, Object.class);
		assertTrue(cArray[0] instanceof ArrayList);
		assertFalse(cArray instanceof ArrayList[]);
		assertFalse(cArray instanceof AbstractList[]);
		assertFalse(cArray instanceof List[]);
		assertFalse(cArray instanceof AbstractCollection[]);
		assertFalse(cArray instanceof Collection[]);
		assertTrue(cArray instanceof Object[]);
	}

	@Test
	public void testMapValueByPath() {
		Map<String, Object> map = new HashMap<>();
		map.put("address", "0x0d8775f648430679a709e98d2b0cb6250d2887ef");
		map.put("blockHash", "0xb99d79fb25389f56ae10adedf74215628b1d39762f1150d3e7772540d42bbb05");
		map.put("blockNumber", "0x6acfe7");
		map.put("data", "0x00000000000000000000000000000000000000000000000e076e3e7507c77c00");
		map.put("logIndex", "0x5d");
		map.put("topics",
				new String[] { "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
						"0x00000000000000000000000051b6548d47bf263cfca5603a0c5f28f1063ef072",
						"0x0000000000000000000000006fea7f12b33b41087fe3e4038d4002bff6e83bbb" });
		map.put("transactionHash", "0x760e9517678120de00c3d19374990259cfe510bbec10eaae2edf5606506e69f6");
		map.put("transactionIndex", "0x90");

		Map<String, Object> innerMap = new HashMap<>();
		innerMap.put("InnerMapKey1", "MapValue1");
		innerMap.put("InnerMapKey2", "MapValue2");
		innerMap.put("InnerMapKey3", "MapValue3");
		innerMap.put("InnerMapKey4",
				new String[] { "inner_inner_array_item1", "inner_inner_array_item2", "inner_inner_array_item3" });

		map.put("additionalItem1",
				new String[] { "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef" });
		map.put("additionalItem2", new Object[] { "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
				new Object[] { "inner_array_item1", "inner_array_item2", innerMap } });

		Set<String[]> accessedPaths = new HashSet<>();

		Object mValue = Utils.getMapValueByPath(Utils.getNodePath("address", "."), map, 0, accessedPaths);
		assertEquals(mValue, "0x0d8775f648430679a709e98d2b0cb6250d2887ef");

		mValue = Utils.getMapValueByPath(Utils.getNodePath("topics.2", "."), map, 0, accessedPaths);
		assertEquals(mValue, "0x0000000000000000000000006fea7f12b33b41087fe3e4038d4002bff6e83bbb");

		mValue = Utils.getMapValueByPath(Utils.getNodePath("additionalItem1.2", "."), map, 0, accessedPaths);
		assertEquals(mValue, null);

		assertEquals(accessedPaths.size(), 3);

		mValue = Utils.getMapValueByPath(Utils.getNodePath("additionalItem2.1.1", "."), map, 0, accessedPaths);
		assertEquals(mValue, "inner_array_item2");

		mValue = Utils.getMapValueByPath(Utils.getNodePath("additionalItem2.1.2.InnerMapKey1", "."), map, 0,
				accessedPaths);
		assertEquals(mValue, "MapValue1");

		mValue = Utils.getMapValueByPath(Utils.getNodePath("additionalItem2.1.2.InnerMapKey4.0", "."), map, 0,
				accessedPaths);
		assertEquals(mValue, "inner_inner_array_item1");

		mValue = Utils.getMapValueByPath(Utils.getNodePath("topics", "."), map, 0, accessedPaths);
		assertEquals(ArrayUtils.getLength(mValue), 3);

		mValue = Utils.getMapValueByPath(Utils.getNodePath("topics.*", "."), map, 0, accessedPaths);
		assertEquals(ArrayUtils.getLength(mValue), 3);

		mValue = Utils.getMapValueByPath(Utils.getNodePath("address.*", "."), map, 0, accessedPaths);
		assertEquals(mValue, "0x0d8775f648430679a709e98d2b0cb6250d2887ef");

		mValue = Utils.getMapValueByPath(Utils.getNodePath("additionalItem2.1.0.InnerMapKey1", "."), map, 0,
				accessedPaths);
		assertEquals(mValue, "inner_array_item1");

		assertEquals(accessedPaths.size(), 10);

		mValue = Utils.getMapValueByPath(Utils.getNodePath("#", "."), map, 0, accessedPaths);
		assertEquals(((Map<?, ?>) mValue).size(), 8);

		mValue = Utils.getMapValueByPath(Utils.getNodePath("additionalItem2.1.2.#", "."), map, 0, accessedPaths);
		assertEquals(((Map<?, ?>) mValue).size(), 3);
	}

}
