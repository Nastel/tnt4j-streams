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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.quartz.SchedulerException;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.scenario.WsRequest;
import com.jkoolcloud.tnt4j.streams.scenario.WsResponse;
import com.jkoolcloud.tnt4j.streams.scenario.WsScenario;
import com.jkoolcloud.tnt4j.streams.scenario.WsScenarioStep;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.WsStreamConstants;

/**
 * Extends REST Stream by adding failed requests recovery.
 * <p>
 * Failed request is considered when it times out, contains failure definition or contains no payload.
 * 
 * @version $Revision: 1 $
 */
public class RecoverableRestStream extends ExtRestStream {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(RecoverableRestStream.class);

	private static int MAX_RECOVERY_ATTEMPTS_COUNT = 5;

	private Map<String, Counter> stepCountersMap = new HashMap<>();

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	private WsScenarioStep makeRecoveryStep(WsScenarioStep wsScenarioStep) {
		WsScenarioStep rStep = new WsScenarioStep(
				wsScenarioStep.getName() + "_recovery-step_" + System.currentTimeMillis()); // NON-NLS

		rStep.setMethod(wsScenarioStep.getMethod());
		rStep.setUrlStr(wsScenarioStep.getUrlStr());
		rStep.setProperty(ExtRestStream.INTERIM_STEP_PROP, String.valueOf(true));

		List<WsRequest<String>> wsRequestsList = wsScenarioStep.getRequests();
		List<WsRequest<String>> rStepWsRequestsList = new ArrayList<>();

		// When a request is added WsRequest is returned for which we can pass parameters
		for (WsRequest<String> wsRequest : wsRequestsList) {
			String id = wsRequest.getId();
			String data = wsRequest.getData();
			rStepWsRequestsList.add(rStep.addRequest(id, data));
		}

		// Passing parameters to WsRequest
		for (WsRequest<String> req : rStepWsRequestsList) {
			for (WsRequest<String> wsRequest : wsRequestsList) {
				for (Map.Entry<String, WsRequest.Parameter> parameterEntry : wsRequest.getParameters().entrySet()) {
					req.addParameter(parameterEntry.getValue());
				}
			}
		}

		return rStep;
	}

	private void scheduleRecoveryJob(int interval, TimeUnit timeUnit, WsScenarioStep wsScenarioStep) {
		if (isShotDown()) {
			return;
		}

		WsScenarioStep rStep = makeRecoveryStep(wsScenarioStep);
		ExtRestStream.setStepToTriggerSingleTime(rStep, interval, timeUnit);

		WsScenario wsScenario = new WsScenario("recovery_scenario_" + rStep.getName()); // NON-NLS
		wsScenario.addStep(rStep);

		logger().log(OpLevel.INFO, StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME,
				"RecoverableRestStream.recovery.job.add"));
		try {
			scheduleScenarioStep(rStep);
		} catch (SchedulerException e) {
			logger().log(OpLevel.ERROR, StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME,
					"RecoverableRestStream.recovery.job.failed"), rStep.getName(), e);
		}
	}

	private String extractKeyName(WsResponse<String, String> wsResponse) {
		return wsResponse.getOriginalRequest().getData();
	}

	private void scheduleRecoveryJobWrapper(WsResponse<String, String> wsResponse) {
		if (isShotDown()) {
			return;
		}

		Counter counter = stepCountersMap.computeIfAbsent(extractKeyName(wsResponse),
				k -> new Counter(MAX_RECOVERY_ATTEMPTS_COUNT));

		if (!counter.isTargetReached()) {
			logger().log(OpLevel.INFO, StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME,
					"RecoverableRestStream.recovery.job.schedule"));
			counter.increment();
			scheduleRecoveryJob(10, TimeUnit.MINUTES, wsResponse.getScenarioStep());
		} else {
			resetCounter(wsResponse, StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME,
					"RecoverableRestStream.recovery.job.no.response"));
		}
	}

	private void resetCounter(WsResponse<String, String> wsResponse, String logMessage) {
		Counter counter = stepCountersMap.get(extractKeyName(wsResponse));

		if (counter != null) {
			logger().log(OpLevel.INFO, logMessage);
			counter.reset();
		}
	}

	@Override
	public WsResponse<String, String> getNextItem() throws Exception {
		WsResponse<String, String> wsResponse = super.getNextItem();

		if (wsResponse != null && wsResponse.getData().equals("[]")) { // NON-NLS //TODO: make configurable
			scheduleRecoveryJobWrapper(wsResponse);
		} else {
			resetCounter(wsResponse, StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME,
					"RecoverableRestStream.recovery.job.got.response"));
		}

		purgeInactiveSchedulerJobs();

		return wsResponse;
	}

	/**
	 * Recovery job counter defining target count to reach.
	 */
	protected static class Counter {
		private int target;
		private int start;

		/**
		 * Instantiates a new Counter.
		 *
		 * @param target
		 *            the target count
		 */
		public Counter(int target) {
			this.target = target;
			start = 0;
		}

		/**
		 * Increments current count.
		 */
		public void increment() {
			start++;
		}

		/**
		 * Resets current count.
		 */
		public void reset() {
			start = 0;
		}

		/**
		 * Checks if target count has been reached.
		 *
		 * @return {@code true} if current count is equal or greater than target count, {@code false} - otherwise
		 */
		public boolean isTargetReached() {
			return start >= target;
		}
	}
}
