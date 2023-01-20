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

package com.jkoolcloud.tnt4j.streams.utils;

import static org.junit.Assert.assertEquals;

import java.security.GeneralSecurityException;

import org.junit.Ignore;
import org.junit.Test;

/**
 * @author akausinis
 * @version 1.0
 */
public class SecurityUtilsTest {

	@Test
	public void testPassEncrypt() throws Exception {
		String ePass = SecurityUtils.encryptPass("someDummyPass");
		assertEquals("v5OmKh7o3bHWRibcb4bnLg==", ePass);
	}

	@Test
	public void testPassDecrypt() throws Exception {
		String dPass = SecurityUtils.decryptPass("v5OmKh7o3bHWRibcb4bnLg==");
		assertEquals("someDummyPass", dPass);
	}

	@Test(expected = GeneralSecurityException.class)
	public void testPassDecryptFail() throws Exception {
		String dPass = SecurityUtils.decryptPass("v5OmKh7o3bHWRibcb4bnLa==");
		assertEquals("someDummyPass", dPass);
	}

	@Test
	public void testGetPass2() {
		String pStr = SecurityUtils.getPass2("v5OmKh7o3bHWRibcb4bnLa==");
		assertEquals("v5OmKh7o3bHWRibcb4bnLa==", pStr);

		pStr = SecurityUtils.getPass2("v5OmKh7o3bHWRibcb4bnLg==");
		assertEquals("someDummyPass", pStr);
	}

	@Test
	public void testGetPass() {
		String pStr = SecurityUtils.getPass("v5OmKh7o3bHWRibcb4bnLa==");
		assertEquals("v5OmKh7o3bHWRibcb4bnLa==", pStr);

		pStr = SecurityUtils.getPass("v5OmKh7o3bHWRibcb4bnLg==");
		assertEquals("someDummyPass", pStr);
	}

	@Test
	@Ignore
	public void testMainPassEncrypt() {
		SecurityUtils.main("-e", "", "  ", "", "        ", "someDummyPass");
	}

	@Test
	@Ignore
	public void testMainPassDecrypt() {
		SecurityUtils.main("-d", " v5OmKh7o3bHWRibcb4bnLg==");
	}

	@Test
	@Ignore
	public void testMainEmpty() {
		SecurityUtils.main();
	}

	@Test
	@Ignore
	public void testMainInvalid() {
		SecurityUtils.main("-p", "extra");
	}

	@Test
	@Ignore
	public void testMainInvalid2() {
		SecurityUtils.main("-e", "", "  ", "", "        ", "someDummyPass", "-decrypt", "", "extra");
	}
}
