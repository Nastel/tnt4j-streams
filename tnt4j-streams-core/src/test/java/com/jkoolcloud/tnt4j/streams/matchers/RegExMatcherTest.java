/*
 * Copyright 2014-2023 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.matchers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author akausinis
 * @version 1.0
 */
public class RegExMatcherTest {
	@Test
	public void evaluateTrue() throws Exception {
		assertTrue(Matchers.evaluate("regex:ee", "rree"));
	}

	@Test
	public void evaluateFalse() throws Exception {
		assertFalse(Matchers.evaluate("regex:ee", "ggrr"));
	}

	@Test
	public void evaluateAdvanced() throws Exception {
		assertTrue(Matchers.evaluate("regex:.*", "eegg"));
	}

}