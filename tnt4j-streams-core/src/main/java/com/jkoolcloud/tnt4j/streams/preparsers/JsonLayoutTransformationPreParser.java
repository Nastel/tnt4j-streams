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

package com.jkoolcloud.tnt4j.streams.preparsers;

import java.text.ParseException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jkoolcloud.tnt4j.config.ConfigException;
import com.jkoolcloud.tnt4j.config.Configurable;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Parser;

/**
 * Pre-parser to convert JSON input data layout using JSLT compatible expressions.
 * 
 * @version $Revision: 1 $
 */
public class JsonLayoutTransformationPreParser extends AbstractPreParser<String, String> implements Configurable {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(JsonLayoutTransformationPreParser.class);

	/**
	 * Constant for name of pre-parser configuration property {@value} defining transformation expression code.
	 */
	protected static final String PROP_JSLT = "jslt"; // NON-NLS

	private Map<String, ?> configuration;

	private static ObjectMapper objectMapper = new ObjectMapper();
	private Expression jsltExpression;

	/**
	 * Constructs a new JSON Layout Transformation pre parser.
	 */
	public JsonLayoutTransformationPreParser() {
	}

	@Override
	public Map<String, ?> getConfiguration() {
		return configuration;
	}

	@Override
	public void setConfiguration(Map<String, ?> settings) throws ConfigException {
		this.configuration = settings;

		String jsltScript = Utils.getString(PROP_JSLT, settings, null);

		if (StringUtils.isEmpty(jsltScript)) {
			throw new ConfigException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"JsonLayoutTransformationPreParser.missing.transformation.definition", getName(), PROP_JSLT),
					settings);
		}

		try {
			jsltExpression = Parser.compileString(jsltScript);
		} catch (Exception e) {
			throw new ConfigException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"JsonLayoutTransformationPreParser.invalid.transformation.definition", getName(),
					e.getLocalizedMessage()), settings);
		}
	}

	@Override
	public String preParse(String data) throws Exception {
		if (jsltExpression == null) {
			throw new ParseException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"JsonLayoutTransformationPreParser.jslt.expression.not.initialized"), 0);
		}

		JsonNode input = objectMapper.readTree(data);
		JsonNode output = jsltExpression.apply(input);
		return output.toString();
	}

	@Override
	public boolean isUsingParserForInput() {
		return true;
	}
}
