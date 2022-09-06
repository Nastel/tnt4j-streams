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

package com.jkoolcloud.tnt4j.streams.parsers;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author akausinis
 * @version 1.0
 */
public class MessageTypeTest {

	@Test
	public void test() {
		for (MessageType type : MessageType.values()) {
			assertEquals(type, MessageType.valueOf(type.value()));
			assertEquals(type, MessageType.valueOf((Object) type.name()));
			assertEquals(type, MessageType.valueOf((Number) type.value()));
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFail() {
		MessageType.valueOf(3);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFailOnNull() {
		MessageType.valueOf((Object) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFailOnOtherClass() {
		MessageType.valueOf(this);
	}
}
