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

package com.jkoolcloud.tnt4j.streams.matchers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author akausinis
 * @version 1.0
 */
public class StringMatcherTest {
	@Test
	public void evaluateTrue() throws Exception {
		assertTrue(Matchers.evaluate("isEmpty()", ""));
		assertTrue(Matchers.evaluate("string:compare(blah)", "blah"));
		assertTrue(Matchers.evaluate("string:startsWith(blah)", "blah12121"));
		assertTrue(Matchers.evaluate("string:startsWith(0xa9059cbb)",
				"0xa9059cbb00000000000000000000000003ee867205c02eeda0b2650f7ee02ccdb52055cc00000000000000000000000000000000000000000000000000000000000007d0"));
		assertTrue(Matchers.evaluate("contains(:)", "blah1:2121"));
	}

	@Test
	public void evaluateFalse() throws Exception {
		assertFalse(Matchers.evaluate("!isEmpty()", ""));
		assertFalse(Matchers.evaluate("string:!compare(blah)", "blah"));
		assertFalse(Matchers.evaluate("contains(:)", "blah1;2121"));
	}

	@Test
	public void startsWithIgnoreCase() throws Exception {
		assertTrue(Matchers.evaluate("string:startsWithIgnoreCase(blah)", "BLah12121"));
		assertFalse(Matchers.evaluate("string:compare(blah)", (Object) null));
		assertFalse(Matchers.evaluate("string:contains(r)", "fooboo"));
		assertTrue(Matchers.evaluate("string:contains(b)", "fooboo"));
	}

}