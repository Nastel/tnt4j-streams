/*
 * Copyright 2014-2022 JKOOL, LLC.
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

import java.security.MessageDigest;
import java.util.Date;

import org.junit.Test;

import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.streams.parsers.MessageType;

/**
 * @author akausinis
 * @version 1.0
 */
public class WmqUtilsTest {

	@Test
	public void testComputeSignature() throws Exception {
		String expectedSignature = "ecFrCSkZqXWsnKJGGUIliA==";

		String sigMD5 = WmqUtils.computeSignature(MessageType.REQUEST, "MSG_FORMAT", "MSG_ID".getBytes(),
				"USER_ID".toLowerCase(), // NON-NLS
				"APPL_TYPE", "APPL_NAME", "2016-04-18", "13:17:25", "xxxyyyzzz".getBytes()); // NON-NLS

		assertEquals("MD5 signature does not match expected initial value", expectedSignature, sigMD5);

		MessageDigest msgDig = MessageDigest.getInstance("SHA1"); // NON-NLS
		String sigOther = WmqUtils.computeSignature(msgDig, MessageType.REQUEST, "MSG_FORMAT", "MSG_ID".getBytes(), // NON-NLS
				"USER_ID".toLowerCase(), "APPL_TYPE", "APPL_NAME", "2016-04-18", "13:17:25", "xxxyyyzzz".getBytes()); // NON-NLS

		assertNotEquals("Messages signatures should not match", sigMD5, sigOther);

		msgDig = MessageDigest.getInstance("MD5"); // NON-NLS
		sigOther = WmqUtils.computeSignature(msgDig, MessageType.REQUEST, "MSG_FORMAT", "MSG_ID".getBytes(),
				"USER_ID".toLowerCase(), // NON-NLS
				"APPL_TYPE", "APPL_NAME", "2016-04-18", "13:17:25", "xxxyyyzzz".getBytes()); // NON-NLS

		assertEquals("Messages signatures should match", sigMD5, sigOther);
	}

	@Test
	public void testComputeSignatureValueNull() throws Exception {
		assertNull(WmqUtils.computeSignature(null, ",", DefaultEventSinkFactory.defaultEventSink(WmqUtilsTest.class))); // NON-NLS

	}

	@Test
	public void testComputeSignatureValueBArray() throws Exception {
		Object sig = WmqUtils.computeSignature(new byte[] { 0x15, 0x22, 0x34 }, ",",
				DefaultEventSinkFactory.defaultEventSink(WmqUtilsTest.class)); // NON-NLS

		assertEquals(sig, "BlVu5i3AhAbaIdNXGjx2wg==");
	}

	@Test
	public void testComputeSignatureValueOArray() throws Exception {
		Object sig = WmqUtils.computeSignature(new Object[] { "Something", 0x22, new Date(123456789) }, ",",
				DefaultEventSinkFactory.defaultEventSink(WmqUtilsTest.class)); // NON-NLS

		assertEquals(sig, "21ZQKAr6Gq2B2T4Qu7IQqg==");
	}

	@Test
	public void testGetPayload() throws Exception {
		byte[] dlhData = new byte[] { 0x44, 0x4C, 0x48, 0x20, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x08, 0x25, 0x4D,
				0x4D, 0x53, 0x2E, 0x57, 0x4D, 0x51, 0x2E, 0x52, 0x45, 0x4D, 0x4F, 0x54, 0x45, 0x2E, 0x4E, 0x4F, 0x54,
				0x49, 0x43, 0x45, 0x2E, 0x52, 0x45, 0x53, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20,
				0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x57, 0x4D, 0x51, 0x2E,
				0x51, 0x53, 0x31, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20,
				0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20,
				0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x00, 0x00, 0x03, 0x11, 0x00, 0x00, 0x01,
				(byte) 0xF4, 0x4D, 0x51, 0x53, 0x54, 0x52, 0x20, 0x20, 0x20, 0x00, 0x00, 0x00, 0x06, 0x61, 0x6D, 0x71,
				0x72, 0x6D, 0x70, 0x70, 0x61, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20,
				0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x32, 0x30, 0x31, 0x31, 0x30, 0x32, 0x31, 0x30, 0x31,
				0x32, 0x33, 0x39, 0x31, 0x34, 0x38, 0x30, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B,
				0x5B, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B,
				0x5B, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B,
				0x5B, 0x5B, 0x5B, 0x5B, 0x5B, 0x5B };

		int[] oc = WmqUtils.getPayloadOffsetAndCCSID(dlhData);
		assertTrue(oc[0] == 172);
		assertTrue(oc[1] == 500);

		byte[] payload = WmqUtils.getMsgPayload(dlhData);
		assertTrue(payload.length == 50);

		String payloadStr = WmqUtils.getString(dlhData, 0, true);
		assertTrue(payloadStr.length() == 50);
		assertFalse(payloadStr.equals("[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[["));
		assertTrue(payloadStr.equals("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$"));

		dlhData[0] = (byte) (dlhData[0] + 1);
		payload = WmqUtils.getMsgPayload(dlhData);
		assertTrue(payload.length == 222);

	}
}
