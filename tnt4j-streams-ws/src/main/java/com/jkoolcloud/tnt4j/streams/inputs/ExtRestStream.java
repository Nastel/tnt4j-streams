/*
 * Copyright 2014-2024 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.inputs;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.util.concurrent.RateLimiter;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.scenario.*;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.streams.utils.WsStreamConstants;

/**
 * Extends REST Stream by adding additional scenario steps configuration to trigger execution of intermediate scenarios
 * to enrich activity entity data depending on initial activity data values.
 * 
 * @version $Revision: 1 $
 */
public class ExtRestStream extends RestStream {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(ExtRestStream.class);

	private static final int DEFAULT_CLEANUP_PERIOD = 65; // in seconds

	/**
	 * The constant for name of built-in scenario step configuration property {@value}.
	 */
	protected static final String TRIGGERS_ON_PROP = "triggersOn"; // NON-NLS
	/**
	 * The constant for name of built-in scenario step configuration property {@value}.
	 */
	protected static final String TRANSFORM_PROP = "transform"; // NON-NLS
	/**
	 * The constant for name of built-in scenario step configuration property {@value}.
	 */
	protected static final String ITERABLE_VAR_PROP = "iterable"; // NON-NLS
	/**
	 * The constant for name of built-in scenario step configuration property {@value}.
	 */
	protected static final String REQ_LIMIT_PROP = "requestsLimit"; // NON-NLS
	/**
	 * The constant for name of internal intermediate scenario step configuration property {@value}.
	 */
	protected static final String INTERIM_STEP_PROP = "InterimStep"; // NON-NLS
	/**
	 * The constant for name of internal intermediate scenario step configuration property {@value}.
	 */
	protected static final String CONSUMED_REQ_PROP = "Consumed"; // NON-NLS
	/**
	 * The constant for name of internal intermediate scenario step configuration property {@value}.
	 */
	protected static final String ORIGINAL_STEP_NAME_PROP = "OriginalStepName"; // NON-NLS

	protected static final String TRANS_TO_HEX = "toHex"; // NON-NLS

	private long lastCleanTime;
	private long cleanPeriod = TimeUnit.SECONDS.toMillis(DEFAULT_CLEANUP_PERIOD);
	private final Map<String, RateLimiter> reqRateLimiters = new HashMap<>(5);

	/**
	 * Constructs an empty ExtRestStream. Requires configuration settings to set input stream source.
	 */
	public ExtRestStream() {
		super();

		lastCleanTime = System.currentTimeMillis();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	// @Override
	// public void setProperty(String name, String value) {
	// super.setProperty(name, value);
	// }
	//
	// @Override
	// public Object getProperty(String name) {
	// return super.getProperty(name);
	// }

	@Override
	public void addScenario(WsScenario scenario) {
		synchronized (triggersOnStepsMap) {
			List<WsScenarioStep> steps = scenario.getStepsList();
			for (int i = steps.size() - 1; i >= 0; i--) {
				WsScenarioStep step = steps.get(i);
				String triggersOn = step.getProperty(TRIGGERS_ON_PROP);
				if (StringUtils.isNotEmpty(triggersOn)) {
					steps.remove(i);
					if (ReqMethod.GET.name().equalsIgnoreCase(step.getMethod()) && step.getUrlStr() != null) {
						logger().log(OpLevel.FATAL, StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME,
								"ExtRestStream.triggers.on.missing.uri"));
						halt(false);
					}
					triggersOnStepsMap.put(step.getName(), step);
				}
			}
		}

		super.addScenario(scenario);
	}

	@Override
	protected ActivityInfo applyParsers(Object data, String... tags) throws IllegalStateException, ParseException {
		ActivityInfo ai = null;
		try {
			ai = super.applyParsers(data, tags);
		} finally {
			try {
				postParse(data, ai);
			} catch (Throwable exc) {
				Utils.logThrowable(logger(), OpLevel.ERROR, StreamsResources
						.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME, "ExtRestStream.post.parse.failed"), exc);
			}
		}

		return ai;
	}

	@Override
	protected Semaphore acquireSemaphore(WsRequest<String> request) throws InterruptedException {
		String osName = request.getScenarioStep().getProperty(ORIGINAL_STEP_NAME_PROP);
		if (StringUtils.isNotEmpty(osName)) {
			synchronized (reqRateLimiters) {
				RateLimiter rl = reqRateLimiters.get(osName);
				if (rl != null) {
					rl.acquire();
				}
			}
		}

		return super.acquireSemaphore(request);
	}

	@Override
	protected void requestFailed(WsRequest<String> request) {
		markRequestConsumed(request);
	}

	private static void markRequestConsumed(WsRequest<?> request) {
		if (request != null && request.getScenarioStep() != null) {
			if (Boolean.parseBoolean(request.getScenarioStep().getProperty(INTERIM_STEP_PROP))) {
				request.addParameter(CONSUMED_REQ_PROP, String.valueOf(true), true);
			}
		}
	}

	/**
	 * Does post-parsing of provided activity data and activity entity instance containing parsed values.
	 *
	 * @param data
	 *            the activity data item to post-process
	 * @param activityInfo
	 *            the activity entity instance containing parsed values
	 */
	@SuppressWarnings("unchecked")
	protected void postParse(Object data, ActivityInfo activityInfo) {
		if (activityInfo == null) {
			return;
		}

		WsResponse<String, String> resp = null;
		if (data instanceof WsResponse) {
			resp = (WsResponse<String, String>) data;
			markRequestConsumed(resp.getOriginalRequest());
		}

		Object error = activityInfo.getFieldValue("Error"); // NON-NLS
		if (error != null) {
			logger().log(OpLevel.WARNING, StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME,
					"ExtRestStream.post.parse.error.response"), error);

			if (resp != null) {
				WsScenarioStep step = resp.getScenarioStep();
				WsScenario scenario = step.getScenario();
				try {
					handleFault(scenario);
				} catch (Throwable e) {
					Utils.logThrowable(logger(), OpLevel.ERROR, StreamsResources.getString(
							WsStreamConstants.RESOURCE_BUNDLE_NAME, "ExtRestStream.post.parse.fault.handle.failed"), e);
				}
			}
		}

		cleanupIntermediateSteps();

		synchronized (triggersOnStepsMap) {
			logger().log(OpLevel.DEBUG,
					StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME,
							"ExtRestStream.post.parse.scheduling.triggers.on"),
					getName(), activityInfo.determineTrackingId());
			for (Map.Entry<String, WsScenarioStep> entry : triggersOnStepsMap.entrySet()) {
				WsScenarioStep step = entry.getValue();
				String triggersOn = step.getProperty(TRIGGERS_ON_PROP);
				Object fieldValue = getTriggersOnValue(triggersOn, activityInfo, resp);
				if (fieldValue == null) {
					logger().log(OpLevel.DEBUG,
							StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME,
									"ExtRestStream.post.parse.no.value"),
							getName(), step.getScenario().getName(), step.getName());
				} else {
					handleCommon(fieldValue, step, activityInfo);
				}
			}
		}
	}

	private static Object getTriggersOnValue(String field, ActivityInfo ai, WsResponse<String, String> resp) {
		Object fValue = ai.getFieldValue(field);

		if (fValue == null && resp != null) {
			WsRequest<String> req = resp.getOriginalRequest();
			if (req != null) {
				fValue = req.getParameterValue(field);
			}
		}

		return fValue;
	}

	private void handleCommon(Object value, WsScenarioStep genStep, ActivityInfo activityInfo) {
		if (isShotDown()) {
			return;
		}

		logger().log(OpLevel.DEBUG, StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME,
				"ExtRestStream.handle.common.running.triggers.on"), genStep.getName(), Utils.toString(value));
		Object[] valueA = value instanceof String ? Utils.toArray((String) value) : Utils.makeArray(value);
		if (ArrayUtils.isEmpty(valueA)) {
			logger().log(OpLevel.DEBUG, StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME,
					"ExtRestStream.handle.common.no.value"));
			return;
		}

		String transform = genStep.getProperty(TRANSFORM_PROP);
		String iterableVar = genStep.getProperty(ITERABLE_VAR_PROP);
		List<String> iterables = new ArrayList<>(valueA.length);
		for (Object entry : valueA) { // NON-NLS
			String eStr = String.valueOf(entry);
			if (TRANS_TO_HEX.equalsIgnoreCase(transform)) {
				if (entry instanceof Integer) {
					eStr = Integer.toHexString((Integer) entry);
				} else {
					eStr = Integer.toHexString(Integer.parseInt(eStr.trim()));
				}
			}
			iterables.add(eStr);
		}

		List<WsScenarioStep> interimSteps = buildIntermediateSteps(genStep, iterableVar, iterables, activityInfo); // NON-NLS
		for (WsScenarioStep interimStep : interimSteps) {
			if (isShotDown()) {
				return;
			}

			try {
				scheduleScenarioStep(interimStep);
			} catch (Throwable e) {
				logger().log(OpLevel.ERROR,
						StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME,
								"ExtRestStream.handle.common.scheduling.failed"),
						iterables, iterableVar, interimStep, Utils.toString(interimStep.getRequests()));
				Utils.logThrowable(logger(), OpLevel.ERROR, StreamsResources.getString(
						WsStreamConstants.RESOURCE_BUNDLE_NAME, "ExtRestStream.handle.common.can.not.schedule"), e);
			}
		}
	}

	/**
	 * The map of intermediate scenario steps build for {@value #TRIGGERS_ON_PROP} defined reference.
	 */
	final Map<String, WsScenarioStep> triggersOnStepsMap = new HashMap<>();

	private void handleFault(WsScenario wsScenario) throws Exception {
		WsScenarioStep loginStep = wsScenario.getLoginStep();
		if (loginStep != null && !loginStep.isEmpty()) {
			logger().log(OpLevel.INFO, StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME,
					"ExtRestStream.handle.fault.login"));
			WsRequest<String> wsRequest = loginStep.getRequests().get(0);
			WsResponse<String, String> wsResponse = new WsResponse<>(RestStream.executePOST(client, wsRequest),
					wsRequest);
			addInputToBuffer(wsResponse);
		}
	}

	@Override
	protected void cleanup() {
		reqRateLimiters.clear();

		super.cleanup();
	}

	/**
	 * Builds intermediate scenario steps list based on provided generic (base) scenario step, iterable variable,
	 * properties list and activity entity instance containing parsed values.
	 * 
	 * @param genericStep
	 *            the generic scenario step to be used for building intermediate steps
	 * @param iterableVar
	 *            the iterable variable name
	 * @param parameters
	 *            the parameters list
	 * @param ai
	 *            the activity entity instance containing parsed values
	 *
	 * @return the list of intermediate scenario steps
	 * 
	 * @see #setStepToTriggerSingleTime(com.jkoolcloud.tnt4j.streams.scenario.WsScenarioStep, int,
	 *      java.util.concurrent.TimeUnit)
	 */
	protected List<WsScenarioStep> buildIntermediateSteps(WsScenarioStep genericStep, String iterableVar,
			List<String> parameters, ActivityInfo ai) {
		List<WsScenarioStep> steps = new ArrayList<>(5);
		if (CollectionUtils.isEmpty(parameters)) {
			return steps;
		}

		String genericStepName = genericStep.getName();
		String requestsLimit = genericStep.getProperty(REQ_LIMIT_PROP);

		int rlReqCount = parameters.size();
		TimeUnit rlTimeUnit = TimeUnit.SECONDS;

		if (StringUtils.isNotEmpty(requestsLimit)) {
			String[] rlTokes = requestsLimit.split("/");

			try {
				rlReqCount = Integer.parseInt(rlTokes[0]);
			} catch (NumberFormatException exc) {
			}
			if (rlTokes.length > 1) {
				try {
					rlTimeUnit = TimeUnit.valueOf(rlTokes[1].toUpperCase() + "S"); // NON-NLS
				} catch (IllegalArgumentException exc) {
				}
			}

			synchronized (reqRateLimiters) {
				if (reqRateLimiters.get(genericStepName) == null) {
					double rate = (double) rlReqCount / rlTimeUnit.toSeconds(1);
					RateLimiter reqRateLimiter = RateLimiter.create(rate);
					reqRateLimiters.put(genericStepName, reqRateLimiter);
				}
			}
		}

		int startIndex = 0;
		int endIndex = Math.min(rlReqCount, parameters.size()) - 1;
		int delay = 0;

		while (startIndex <= parameters.size() - 1) {
			WsScenarioStep step = new WsScenarioStep(genericStepName + ":" + System.currentTimeMillis() + ":" // NON-NLS
					+ parameters.get(startIndex) + "-" + parameters.get(endIndex)); // NON-NLS
			step.setProperties(genericStep.getProperties());
			step.setProperty(INTERIM_STEP_PROP, String.valueOf(true));
			step.setProperty(ORIGINAL_STEP_NAME_PROP, genericStepName);
			step.setMethod(genericStep.getMethod());
			step.setUrlStr(genericStep.getUrlStr());

			for (int i = startIndex; i <= endIndex; i++) {
				WsRequest<String> tmpRequest;
				for (WsRequest<String> genericRequest : genericStep.getRequests()) {
					try {
						String hash = parameters.get(i);
						tmpRequest = fillInIntermediateRequest(genericRequest, makeMap(iterableVar, hash), ai);
						tmpRequest.setId(hash);
						step.addRequest(tmpRequest);
					} catch (VoidRequestException exc) {
						logger().log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
								"AbstractWsStream.void.request", genericRequest.getId(), exc.getMessage());
					}
				}
			}

			if (genericStep.getUsername() != null && genericStep.getPassword() != null) {
				step.setCredentials(genericStep.getUsername(), genericStep.getPassword());
			}

			setStepToTriggerSingleTime(step, delay++, rlTimeUnit);
			genericStep.getScenario().addStep(step);

			steps.add(step);

			startIndex = endIndex + 1;
			endIndex = Math.min(endIndex + rlReqCount, parameters.size() - 1);
		}

		return steps;
	}

	/**
	 * Schedules scenario step to be triggered one single time after defined interval expires.
	 *
	 * @param wsScenarioStep
	 *            the scenario step
	 * @param interval
	 *            the interval
	 * @param timeUnit
	 *            the time units
	 */
	protected static void setStepToTriggerSingleTime(WsScenarioStep wsScenarioStep, int interval, TimeUnit timeUnit) {
		SimpleSchedulerData schedulerData = new SimpleSchedulerData(0, TimeUnit.SECONDS);
		schedulerData.setRepeatCount(1);
		schedulerData.setStartDelay(interval, timeUnit);
		wsScenarioStep.setSchedulerData(schedulerData);
	}

	private static Map<String, String> makeMap(String var, String val) {
		return Collections.singletonMap(var, val);
	}

	private static final String ITR_MAP_KEY = DataFillContext.KEY_PREFIX + "ITR_MAP"; // NON-NLS
	private static final String AI_MAP_KEY = DataFillContext.KEY_PREFIX + "ACTIVITY"; // NON-NLS

	private WsRequest<String> fillInIntermediateRequest(WsRequest<String> gRequest, Map<String, String> itrMap,
			ActivityInfo ai) throws VoidRequestException {
		RequestFillContext reqCtx = new RequestFillContext(isDirectRequestUse());
		reqCtx.put(ITR_MAP_KEY, itrMap);
		reqCtx.put(AI_MAP_KEY, ai);

		return fillInRequest(gRequest, reqCtx);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Object getVariableValue(String varName, DataFillContext context) {
		Object rValue;
		Map<String, String> itrMap = (Map<String, String>) context.get(ITR_MAP_KEY);
		if (itrMap != null) {
			rValue = itrMap.get(varName);

			if (rValue != null) {
				return rValue;
			}
		}
		ActivityInfo ai = (ActivityInfo) context.get(AI_MAP_KEY);
		if (ai != null) {
			rValue = ai.getFieldValue(varName);

			if (rValue != null) {
				return rValue;
			}
		}

		return super.getVariableValue(varName, context);
	}

	/**
	 * Performs cleanup of intermediate scenario steps by removing steps containing no unconsumed requests left.
	 * 
	 * @see #isStepConsumed(com.jkoolcloud.tnt4j.streams.scenario.WsScenarioStep)
	 */
	protected void cleanupIntermediateSteps() {
		if ((System.currentTimeMillis() - lastCleanTime) < cleanPeriod) {
			return;
		}

		synchronized (triggersOnStepsMap) {
			lastCleanTime = System.currentTimeMillis();

			int rCount = 0;
			int lCount = 0;
			Set<String> processedScenarios = new HashSet<>(triggersOnStepsMap.size());
			for (Map.Entry<String, WsScenarioStep> entry : triggersOnStepsMap.entrySet()) {
				WsScenario scenario = entry.getValue().getScenario();
				if (processedScenarios.contains(scenario.getName())) {
					continue;
				}
				processedScenarios.add(scenario.getName());
				List<WsScenarioStep> steps = scenario.getStepsList();

				for (int i = steps.size() - 1; i >= 0; i--) {
					WsScenarioStep step = steps.get(i);
					if (Boolean.parseBoolean(step.getProperty(INTERIM_STEP_PROP))) {
						if (isStepConsumed(step)) {
							scenario.removeStep(step);
							rCount++;
							logger().log(OpLevel.DEBUG,
									StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME,
											"ExtRestStream.cleanup.interim.steps.removed.step"),
									getName(), step.getName());
						} else {
							lCount++;
						}
					}
				}
			}

			logger().log(OpLevel.DEBUG, StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME,
					"ExtRestStream.cleanup.interim.steps.stats"), getName(), rCount, lCount);
		}

		purgeInactiveSchedulerJobs();
	}

	/**
	 * Iterates through all requests of provided scenario step and removes requests marked as consumed. After such
	 * scenario step cleanup, it checks if scenario step has any unconsumed requests left.
	 *
	 * @param step
	 *            the scenario step instance to check
	 *
	 * @return {@code true} if all step requests were marked as consumed, {@code false} - otherwise
	 */
	protected static boolean isStepConsumed(WsScenarioStep step) {
		if (!step.isEmpty()) {
			List<WsRequest<String>> requests = step.getRequests();
			for (int i = requests.size() - 1; i >= 0; i--) {
				WsRequest<String> req = requests.get(i);
				if (Boolean.parseBoolean(req.getParameterStringValue(CONSUMED_REQ_PROP))) {
					step.removeRequest(req);
				}
			}
		}

		return step.isEmpty();
	}
}
