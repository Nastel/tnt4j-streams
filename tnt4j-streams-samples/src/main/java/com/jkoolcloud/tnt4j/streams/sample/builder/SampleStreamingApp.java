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

package com.jkoolcloud.tnt4j.streams.sample.builder;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

import com.jkoolcloud.tnt4j.streams.StreamsAgent;
import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.configure.build.POJOStreamsBuilder;
import com.jkoolcloud.tnt4j.streams.fields.ActivityField;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkoolcloud.tnt4j.streams.fields.StreamFieldType;
import com.jkoolcloud.tnt4j.streams.inputs.FileLineStream;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser;

/**
 * Sample integration of TNT4J-Streams into an application using plain Java API without configuration XML file.
 *
 * This sample applies same configuration as in 'single-log' sample.
 *
 * @version $Revision: 1 $
 */
public class SampleStreamingApp {

	private SampleStreamingApp() {
	}

	/**
	 * Main entry point for running as a standalone application.
	 *
	 * @param args
	 *            program command-line arguments. Supported arguments:
	 *            <table summary="TNT4J-Streams agent command line arguments">
	 *            <tr>
	 *            <td>&nbsp;&nbsp;</td>
	 *            <td>&nbsp;&lt;orders_log_file_path&gt;</td>
	 *            <td>(optional) path of "orders.log" file. Default value is working dir.</td>
	 *            </tr>
	 *            </table>
	 * @throws Exception
	 *             if any exception occurs while running application
	 */
	public static void main(String... args) throws Exception {
		Map<String, String> props = new HashMap<>();
		props.put(ParserProperties.PROP_FLD_DELIM, "|"); // NON-NLS

		ActivityTokenParser atp = new ActivityTokenParser();
		atp.setName("TokenParser"); // NON-NLS
		atp.setProperties(props.entrySet());

		ActivityField f = new ActivityField(StreamFieldType.StartTime.name());
		ActivityFieldLocator afl = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "1");
		afl.setFormat("dd MMM yyyy HH:mm:ss", "en-US"); // NON-NLS
		f.addLocator(afl);
		atp.addField(f);

		f = new ActivityField(StreamFieldType.ServerIp.name());
		afl = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "2");
		f.addLocator(afl);
		atp.addField(f);

		f = new ActivityField(StreamFieldType.ApplName.name());
		afl = new ActivityFieldLocator("orders"); // NON-NLS
		f.addLocator(afl);
		atp.addField(f);

		f = new ActivityField(StreamFieldType.Correlator.name());
		afl = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "3");
		f.addLocator(afl);
		atp.addField(f);

		f = new ActivityField(StreamFieldType.UserName.name());
		afl = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "4");
		f.addLocator(afl);
		atp.addField(f);

		f = new ActivityField(StreamFieldType.EventName.name());
		afl = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "5");
		f.addLocator(afl);
		atp.addField(f);

		f = new ActivityField(StreamFieldType.EventType.name());
		afl = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "5");
		afl.addValueMap("Order Placed", "START").addValueMap("Order Received", "RECEIVE") // NON-NLS
				.addValueMap("Order Processing", "OPEN").addValueMap("Order Processed", "SEND") // NON-NLS
				.addValueMap("Order Shipped", "END"); // NON-NLS
		f.addLocator(afl);
		atp.addField(f);

		f = new ActivityField("MsgValue"); // NON-NLS
		afl = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "8");
		f.addLocator(afl);
		atp.addField(f);

		props = new HashMap<>();
		props.put(StreamProperties.PROP_FILENAME, ArrayUtils.isEmpty(args) ? "orders.log" : args[0]); // NON-NLS

		FileLineStream fls = new FileLineStream();
		fls.setName("FileStream"); // NON-NLS
		fls.setProperties(props.entrySet());
		fls.addParser(atp);
		// if (fls.getOutput() == null) {
		// fls.setDefaultStreamOutput();
		// }

		StreamsAgent.runFromAPI(new POJOStreamsBuilder().addStreams(fls));
	}
}
