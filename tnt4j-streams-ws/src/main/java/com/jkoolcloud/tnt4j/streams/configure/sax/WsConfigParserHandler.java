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

package com.jkoolcloud.tnt4j.streams.configure.sax;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.inputs.AbstractWsStream;
import com.jkoolcloud.tnt4j.streams.scenario.*;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.streams.utils.WsStreamConstants;

/**
 * Extends default TNT4J-Streams configuration SAX parser handler and adds additional TNT4J-Streams-WS configuration
 * handling.
 *
 * @version $Revision: 1 $
 * 
 * @see com.jkoolcloud.tnt4j.streams.configure.StreamsConfigLoader
 * @see com.jkoolcloud.tnt4j.streams.configure.sax.StreamsConfigSAXParser
 */
public class WsConfigParserHandler extends ConfigParserHandler {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(WsConfigParserHandler.class);

	private static final String SCENARIO_ELMT = "scenario"; // NON-NLS
	private static final String STEP_ELMT = "step"; // NON-NLS
	private static final String SCHED_CRON_ELMT = "schedule-cron"; // NON-NLS
	private static final String SCHED_SIMPLE_ELMT = "schedule-simple"; // NON-NLS
	private static final String REQ_ELMT = "request"; // NON-NLS
	private static final String REQ_PARAM_ELMT = "req-param"; // NON-NLS
	private static final String CONDITION_ELMT = "condition"; // NON-NLS

	private static final String URL_ATTR = "url"; // NON-NLS
	private static final String METHOD_ATTR = "method"; // NON-NLS
	private static final String USERMAME_ATTR = "username"; // NON-NLS
	private static final String PASSWORD_ATTR = "password"; // NON-NLS

	private static final String EXPRESSION_ATTR = "expression"; // NON-NLS
	private static final String RESOLUTION_ATTR = "resolution"; // NON-NLS

	private static final String INTERVAL_ATTR = "interval"; // NON-NLS
	private static final String REPEATS_ATTR = "repeatCount"; // NON-NLS
	private static final String START_DELAY_ATTR = "startDelay"; // NON-NLS
	private static final String START_DELAY_UNITS_ATTR = "startDelayUnits"; // NON-NLS

	private WsScenario currScenario;
	private WsScenarioStep currStep;
	private RequestData currRequest;
	private ConditionData currCondition;

	@Override
	public void startDocument() throws SAXException {
		super.startDocument();

		currScenario = null;
		currStep = null;
	}

	@Override
	protected void startCfgElement(String qName, Attributes attributes) throws SAXException {
		if (SCENARIO_ELMT.equals(qName)) {
			processScenario(attributes);
		} else if (STEP_ELMT.equals(qName)) {
			processScenarioStep(attributes);
		} else if (SCHED_CRON_ELMT.equals(qName)) {
			processCronScheduler(attributes);
		} else if (SCHED_SIMPLE_ELMT.equals(qName)) {
			processSimpleScheduler(attributes);
		} else if (REQ_ELMT.equals(qName)) {
			processRequest(attributes);
		} else if (REQ_PARAM_ELMT.equals(qName)) {
			processReqParam(attributes);
		} else if (CONDITION_ELMT.equals(qName)) {
			processCondition(attributes);
		} else {
			super.startCfgElement(qName, attributes);
		}
	}

	private void processScenario(Attributes attrs) throws SAXParseException {
		if (currScenario != null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.malformed.configuration", SCENARIO_ELMT), currParseLocation);
		}

		String name = null;
		String url = null;
		String method = null;
		String username = null;
		String password = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (NAME_ATTR.equals(attName)) {
				name = attValue;
			} else if (URL_ATTR.equals(attName)) {
				url = attValue;
			} else if (METHOD_ATTR.equals(attName)) {
				method = attValue;
			} else if (USERMAME_ATTR.equals(attName)) {
				username = attValue;
			} else if (PASSWORD_ATTR.equals(attName)) {
				password = attValue;
			} else {
				unknownAttribute(STEP_ELMT, attName);
			}
		}

		notEmpty(name, SCENARIO_ELMT, NAME_ATTR);

		currScenario = new WsScenario(name);
		currScenario.setUrlStr(url);
		currScenario.setMethod(method);
		currScenario.setCredentials(username, password);
	}

	private void processScenarioStep(Attributes attrs) throws SAXException {
		if (currScenario == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration2", STEP_ELMT, SCENARIO_ELMT),
					currParseLocation);
		}
		String name = null;
		String url = null;
		String method = null;
		String username = null;
		String password = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (NAME_ATTR.equals(attName)) {
				name = attValue;
			} else if (URL_ATTR.equals(attName)) {
				url = attValue;
			} else if (METHOD_ATTR.equals(attName)) {
				method = attValue;
			} else if (USERMAME_ATTR.equals(attName)) {
				username = attValue;
			} else if (PASSWORD_ATTR.equals(attName)) {
				password = attValue;
			} else {
				unknownAttribute(STEP_ELMT, attName);
			}
		}
		notEmpty(name, STEP_ELMT, NAME_ATTR);

		currStep = new WsScenarioStep(name);
		currStep.setUrlStr(url);
		currStep.setMethod(method);
		currStep.setCredentials(username, password);
	}

	private void processCronScheduler(Attributes attrs) throws SAXException {
		if (currStep == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration2", SCHED_CRON_ELMT, STEP_ELMT),
					currParseLocation);
		}

		if (currStep.getSchedulerData() != null) {
			throw new SAXException(StreamsResources.getStringFormatted(WsStreamConstants.RESOURCE_BUNDLE_NAME,
					"WsConfigParserHandler.element.has.multiple", STEP_ELMT, SCHED_CRON_ELMT, SCHED_SIMPLE_ELMT,
					getLocationInfo()));
		}

		String expression = null;
		Integer startDelay = null;
		String startDelayUnits = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (EXPRESSION_ATTR.equals(attName)) {
				expression = attValue;
			} else if (START_DELAY_ATTR.equals(attName)) {
				startDelay = Integer.parseInt(attValue);
			} else if (START_DELAY_UNITS_ATTR.equals(attName)) {
				startDelayUnits = attValue;
			} else {
				unknownAttribute(SCHED_CRON_ELMT, attName);
			}
		}
		notEmpty(expression, SCHED_CRON_ELMT, EXPRESSION_ATTR);

		CronSchedulerData schedData = new CronSchedulerData(expression);
		schedData.setStartDelay(startDelay, startDelayUnits);

		currStep.setSchedulerData(schedData);
	}

	private void processSimpleScheduler(Attributes attrs) throws SAXException {
		if (currStep == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration2", SCHED_SIMPLE_ELMT, STEP_ELMT),
					currParseLocation);
		}

		if (currStep.getSchedulerData() != null) {
			throw new SAXException(StreamsResources.getStringFormatted(WsStreamConstants.RESOURCE_BUNDLE_NAME,
					"WsConfigParserHandler.element.has.multiple", STEP_ELMT, SCHED_CRON_ELMT, SCHED_SIMPLE_ELMT,
					getLocationInfo()));
		}

		Long interval = null;
		String timeUnits = null;
		Integer repeatCount = null;
		Integer startDelay = null;
		String startDelayUnits = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (INTERVAL_ATTR.equals(attName)) {
				interval = Long.parseLong(attValue);
			} else if (UNITS_ATTR.equals(attName)) {
				timeUnits = attValue;
			} else if (REPEATS_ATTR.equals(attName)) {
				repeatCount = Integer.parseInt(attValue);
			} else if (START_DELAY_ATTR.equals(attName)) {
				startDelay = Integer.parseInt(attValue);
			} else if (START_DELAY_UNITS_ATTR.equals(attName)) {
				startDelayUnits = attValue;
			} else {
				unknownAttribute(SCHED_SIMPLE_ELMT, attName);
			}
		}
		if (interval == null || interval < 0) {
			throw new SAXException(StreamsResources.getStringFormatted(WsStreamConstants.RESOURCE_BUNDLE_NAME,
					"WsConfigParserHandler.attr.missing.or.illegal", SCHED_SIMPLE_ELMT, INTERVAL_ATTR, interval,
					getLocationInfo()));
		}

		SimpleSchedulerData schedData = new SimpleSchedulerData(interval, timeUnits);
		schedData.setRepeatCount(repeatCount);
		schedData.setStartDelay(startDelay, startDelayUnits);

		currStep.setSchedulerData(schedData);
	}

	private void processRequest(Attributes attrs) throws SAXException {
		if (currStep == null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.malformed.configuration2", REQ_ELMT, STEP_ELMT), currParseLocation);
		}

		if (currRequest != null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.malformed.configuration", REQ_ELMT), currParseLocation);
		}

		currRequest = new RequestData();

		String id = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (ID_ATTR.equals(attName)) {
				id = attValue;
			} else {
				unknownAttribute(REQ_PARAM_ELMT, attName);
			}
		}

		// notEmpty(id, REQ_ELMT, ID_ATTR);

		currRequest.setId(id);

		elementDataStack.push(new StringBuilder());
	}

	private void processReqParam(Attributes attrs) throws SAXException {
		if (currRequest == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration2", REQ_PARAM_ELMT, REQ_ELMT),
					currParseLocation);
		}

		String id = null;
		String value = null;
		String type = null;
		String format = null;
		String timeZone = null;
		boolean transient_ = false;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (ID_ATTR.equals(attName)) {
				id = attValue;
			} else if (VALUE_ATTR.equals(attName)) {
				value = attValue;
			} else if (TYPE_ATTR.equals(attName)) {
				type = attValue;
			} else if (FORMAT_ATTR.equals(attName)) {
				format = attValue;
			} else if (TIMEZONE_ATTR.equals(attName)) {
				timeZone = attValue;
			} else if (TRANSIENT_ATTR.equals(attName)) {
				transient_ = Utils.toBoolean(attValue);
			} else {
				unknownAttribute(REQ_PARAM_ELMT, attName);
			}
		}

		// notEmpty(id, REQ_PARAM_ELMT, ID_ATTR);
		notEmpty(value, REQ_PARAM_ELMT, VALUE_ATTR);

		currRequest.addParameter(id, value, type, format, timeZone, transient_);
	}

	private void processCondition(Attributes attrs) throws SAXException {
		if (currRequest == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration2", CONDITION_ELMT, REQ_ELMT),
					currParseLocation);
		}

		currCondition = new ConditionData();

		String id = null;
		String resolution = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (ID_ATTR.equals(attName)) {
				id = attValue;
			} else if (RESOLUTION_ATTR.equals(attName)) {
				resolution = attValue;
			} else {
				unknownAttribute(REQ_PARAM_ELMT, attName);
			}
		}

		// notEmpty(id, REQ_PARAM_ELMT, ID_ATTR);
		notEmpty(resolution, CONDITION_ELMT, RESOLUTION_ATTR);

		currCondition.setId(id);
		currCondition.setResolution(resolution);
	}

	@Override
	protected void processMatchExpression(Attributes attrs) throws SAXException {
		if (!StringUtils.equalsAnyIgnoreCase(getParentElmt(MATCH_EXP_ELMT), PARSER_REF_ELMT, CONDITION_ELMT)) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.malformed.configuration2", MATCH_EXP_ELMT,
					Utils.arrayToString(PARSER_REF_ELMT, CONDITION_ELMT)), currParseLocation);
		}

		elementDataStack.push(new StringBuilder());
	}

	private void handleMatchExp(ConditionData currCondition) throws SAXException {
		String eDataVal = getElementData();
		checkScriptExpression(eDataVal);

		if (StringUtils.isNotEmpty(eDataVal)) {
			currCondition.addMatcherExp(eDataVal);
		}
	}

	private void handleCondition(ConditionData currCondition) throws SAXException {
		if (CollectionUtils.isEmpty(currCondition.matchExps)) {
			throw new SAXException(StreamsResources.getStringFormatted(WsStreamConstants.RESOURCE_BUNDLE_NAME,
					"WsConfigParserHandler.element.must.have.one", CONDITION_ELMT, MATCH_EXP_ELMT, getLocationInfo()));
		}

		currRequest.addCondition(currCondition);
	}

	@Override
	protected void checkPropertyState() throws SAXException {
		try {
			super.checkPropertyState();
		} catch (SAXException exc) {
			if (!StringUtils.equalsAnyIgnoreCase(getParentElmt(PROPERTY_ELMT), SCENARIO_ELMT, STEP_ELMT)) {
				throw new SAXParseException(
						StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
								"ConfigParserHandler.malformed.configuration2", PROPERTY_ELMT,
								Utils.arrayToString(CONFIG_ROOT_ELMT, STREAM_ELMT, PARSER_ELMT, JAVA_OBJ_ELMT,
										CACHE_ELMT, TNT4J_PROPERTIES_ELMT, SCENARIO_ELMT, STEP_ELMT)),
						currParseLocation);
			}
		}
	}

	@Override
	public void endCfgElement(String qName) throws SAXException {
		super.endCfgElement(qName);

		try {
			if (SCENARIO_ELMT.equals(qName)) {
				if (currScenario.isEmpty()) {
					throw new SAXException(StreamsResources.getStringFormatted(WsStreamConstants.RESOURCE_BUNDLE_NAME,
							"WsConfigParserHandler.element.must.have.one", SCENARIO_ELMT, STEP_ELMT,
							getLocationInfo()));
				}

				currScenario.setProperties(applyVariableProperties(currProperties.remove(qName)));

				currStream.addReference(currScenario);
				currScenario = null;
			} else if (STREAM_ELMT.equals(qName)) {
				if (currStream instanceof AbstractWsStream) {
					if (CollectionUtils.isEmpty(((AbstractWsStream<?, ?>) currStream).getScenarios())) {
						throw new SAXException(StreamsResources.getStringFormatted(
								WsStreamConstants.RESOURCE_BUNDLE_NAME, "WsConfigParserHandler.element.must.have.one",
								STREAM_ELMT, SCENARIO_ELMT, getLocationInfo()));
					}
				}
			} else if (STEP_ELMT.equals(qName)) {
				if (currStep.isEmpty() && StringUtils.isEmpty(currStep.getUrlStr())) {
					throw new SAXParseException(
							StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
									"ConfigParserHandler.must.contain", STEP_ELMT, URL_ATTR, REQ_ELMT),
							currParseLocation);
				}

				currStep.setProperties(applyVariableProperties(currProperties.remove(qName)));

				currScenario.addStep(currStep);
				currStep = null;
			} else if (REQ_ELMT.equals(qName)) {
				String eDataVal = getElementData();
				if (StringUtils.isNotEmpty(eDataVal)) {
					WsRequest<?> currReq = currStep.addRequest(currRequest.id, eDataVal, currRequest.getTags());

					for (WsRequest.Parameter param : currRequest.params) {
						currReq.addParameter(param);
					}

					for (ConditionData condition : currRequest.conditions) {
						currReq.addCondition(condition.id, condition.resolution, condition.matchExps);
					}
				}

				currRequest = null;
			} else if (MATCH_EXP_ELMT.equals(qName)) {
				if (currCondition != null) {
					handleMatchExp(currCondition);
				}
			} else if (CONDITION_ELMT.equals(qName)) {
				if (currCondition != null) {
					handleCondition(currCondition);

					currCondition = null;
				}
			}
		} catch (SAXException exc) {
			throw exc;
		} catch (Exception e) {
			throw new SAXException(e.getLocalizedMessage() + getLocationInfo(), e);
		}
	}

	@Override
	protected void handleParserRef(ParserRefData parserRefData) throws SAXException {
		if (currRequest != null) {
			if (StringUtils.isEmpty(parserRefData.tags)) {
				parserRefData.tags = parserRefData.parser.getName();
			} else {
				parserRefData.tags += "," + parserRefData.parser.getName(); // NON-NLS
			}

			currRequest.addParserRef(parserRefData);
		}

		super.handleParserRef(parserRefData);
	}

	private static class RequestData {
		private String id;
		private List<WsRequest.Parameter> params = new ArrayList<>();
		private List<ParserRefData> parserRefs = new ArrayList<>();
		private List<ConditionData> conditions = new ArrayList<>();

		void setId(String id) {
			this.id = id;
		}

		void addParameter(String id, String value, String type, String format, String tz, boolean transient_) {
			params.add(new WsRequest.Parameter(id, value, type, format, tz, transient_));
		}

		void addParserRef(ParserRefData pRef) {
			parserRefs.add(pRef);
		}

		void addCondition(ConditionData cond) {
			conditions.add(cond);
		}

		String[] getTags() {
			String[] tags = new String[parserRefs.size()];

			for (int i = 0; i < parserRefs.size(); i++) {
				tags[i] = parserRefs.get(i).parser.getName();
			}

			return tags;
		}
	}

	private static class ConditionData {
		private String id;
		private String resolution;
		private List<String> matchExps = new ArrayList<>();

		void setId(String id) {
			this.id = id;
		}

		void setResolution(String resolution) {
			this.resolution = resolution;
		}

		void addMatcherExp(String matchExp) {
			matchExps.add(matchExp);
		}
	}

}
