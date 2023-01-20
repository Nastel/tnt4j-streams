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

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author akausinis
 * @version 1.0
 */
public class XPathMatcherTest {
	private static String xml;

	@BeforeClass
	public static void initXMLData() throws Exception {
		try {
			xml = new String(Files.readAllBytes(Paths.get("./tnt-streams-core/samples/swift/msg.xml")));
		} catch (Exception exc) {
			xml = new String(Files.readAllBytes(Paths.get("./samples/swift/msg.xml")));
		}
	}

	@Test
	public void evaluateTrue() throws Exception {
		assertTrue(Matchers.evaluate("xpath:/Request/header[currency='EUR']/messageId", xml));
		assertTrue(Matchers.evaluate("xpath:boolean(/Request)", xml));
	}

	@Test
	public void evaluateFalse() throws Exception {
		assertFalse(Matchers.evaluate("xpath:/Request/header[currency='USD']/messageId", xml));
		assertFalse(Matchers.evaluate("xpath:boolean(/Request1)", xml));
	}

	@Test
	public void evaluateAdvanced() throws Exception {
		assertFalse(Matchers.evaluate("xpath:/Request/genericTransFields/receiver", xml));
		assertTrue(Matchers.evaluate("xpath:contains(/Request/messageDetails/tag21, 'VOLUME')", xml));
		assertFalse(Matchers.evaluate("xpath:contains(/Request/messageDetails/tag21, 'V0LUME')", xml));
	}

	@Test
	public void evaluateAdvanced2() throws Exception {
		assertFalse(Matchers.evaluate("xpath:/source",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><destination exist=\"overwrite\" type=\"file\">                <file last-modified=\"2018-07-10T16:04:49.944Z\" size=\"4360\">                    \\\\hwsenas1\\ECE\\Copypri\\CSEC\\WorkForceNow\\G3AKSDWE6SA97N23_2018-07-10-12-00-06-620_WORKER_WFNTOHC_2018.JSON                </file>                <checksum method=\"MD5\">8debe5e7a05106ca1087d021e2561e7a</checksum>            </destination>\n"));
		assertTrue(Matchers.evaluate("xpath:/destination",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><destination exist=\"overwrite\" type=\"file\">                <file last-modified=\"2018-07-10T16:04:49.944Z\" size=\"4360\">                    \\\\hwsenas1\\ECE\\Copypri\\CSEC\\WorkForceNow\\G3AKSDWE6SA97N23_2018-07-10-12-00-06-620_WORKER_WFNTOHC_2018.JSON                </file>                <checksum method=\"MD5\">8debe5e7a05106ca1087d021e2561e7a</checksum>            </destination>\n"));
		assertFalse(Matchers.evaluate("xpath://source",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><destination exist=\"overwrite\" type=\"file\">                <file last-modified=\"2018-07-10T16:04:49.944Z\" size=\"4360\">                    \\\\hwsenas1\\ECE\\Copypri\\CSEC\\WorkForceNow\\G3AKSDWE6SA97N23_2018-07-10-12-00-06-620_WORKER_WFNTOHC_2018.JSON                </file>                <checksum method=\"MD5\">8debe5e7a05106ca1087d021e2561e7a</checksum>            </destination>\n"));
		assertTrue(Matchers.evaluate("xpath://destination",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><destination exist=\"overwrite\" type=\"file\">                <file last-modified=\"2018-07-10T16:04:49.944Z\" size=\"4360\">                    \\\\hwsenas1\\ECE\\Copypri\\CSEC\\WorkForceNow\\G3AKSDWE6SA97N23_2018-07-10-12-00-06-620_WORKER_WFNTOHC_2018.JSON                </file>                <checksum method=\"MD5\">8debe5e7a05106ca1087d021e2561e7a</checksum>            </destination>\n"));

	}

}
