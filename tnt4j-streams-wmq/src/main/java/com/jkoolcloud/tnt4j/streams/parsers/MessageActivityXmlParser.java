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

package com.jkoolcloud.tnt4j.streams.parsers;

import java.text.ParseException;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.fields.ActivityField;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.WmqStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.WmqUtils;

/**
 * This class extends the basic activity XML parser for handling data specific to messaging operations. It provides
 * additional transformations of the raw activity data collected for specific fields.
 * <p>
 * In particular, this class is capable to calculate message signature from tokenized list of field values. Field
 * {@code value-type} attribute value shall be
 * {@value com.jkoolcloud.tnt4j.streams.utils.WmqStreamConstants#VT_SIGNATURE} to initiate signature calculation.
 * <p>
 * This activity parser supports configuration properties from
 * {@link com.jkoolcloud.tnt4j.streams.parsers.ActivityXmlParser} (and higher hierarchy parsers).
 *
 * @version $Revision: 2 $
 */
public class MessageActivityXmlParser extends ActivityXmlParser {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(MessageActivityXmlParser.class);

	/**
	 * Constructs a new MessageActivityXmlParser.
	 */
	public MessageActivityXmlParser() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method applies custom handling for setting field values. This method will construct the signature to use for
	 * the message from the specified value, which is assumed to be a string containing the inputs required for the
	 * message signature calculation, with each input delimited by field property
	 * {@link com.jkoolcloud.tnt4j.streams.fields.ActivityField#getSeparator()} defined delimiter.
	 * <p>
	 * To initiate signature calculation as a field value, {@code field} tag {@code value-type} attribute value has be
	 * set to {@value com.jkoolcloud.tnt4j.streams.utils.WmqStreamConstants#VT_SIGNATURE}.
	 *
	 * @see WmqUtils#computeSignature(Object, String, EventSink)
	 */
	@Override
	protected void applyFieldValue(ActivityInfo ai, ActivityField field, Object value) throws ParseException {
		if (WmqStreamConstants.VT_SIGNATURE.equalsIgnoreCase(field.getValueType())) {
			logger().log(OpLevel.DEBUG, StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
					"ActivityPCFParser.calculating.signature"), field);
			value = WmqUtils.computeSignature(value, field.getSeparator(), logger());
		}

		super.applyFieldValue(ai, field, value);
	}
}
