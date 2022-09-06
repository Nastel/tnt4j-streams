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

package com.jkoolcloud.tnt4j.streams.fields;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author akausinis
 * @version 1.0
 */
public class StreamFieldTypeTest {

	@Test
	public void test() {
		for (StreamFieldType type : StreamFieldType.values()) {
			final Enum<?> enumRet = type.getEnum(0);
			if (type.getDataType() == Enum.class) {
				assertTrue("Enum " + type.name() + " is of " + enumRet.getClass(), enumRet instanceof Enum); // NON-NLS
				for (Enum<?> enumOn : enumRet.getClass().getEnumConstants()) {
					assertEquals(enumOn, type.getEnum(enumOn.name()));
					assertEquals(enumOn.name(), type.getEnumLabel(enumOn.ordinal()));
					assertEquals(enumOn, type.getEnum(enumOn.ordinal()));
					assertEquals(enumOn.ordinal(), type.getEnumValue(enumOn.name()));
				}
			} else {
				assertNull(enumRet);
			}
		}
	}
}
