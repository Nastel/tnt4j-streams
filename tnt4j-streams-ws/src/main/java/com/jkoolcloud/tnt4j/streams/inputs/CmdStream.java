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

package com.jkoolcloud.tnt4j.streams.inputs;

import java.util.concurrent.Semaphore;

import org.apache.commons.lang3.StringUtils;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.scenario.WsRequest;
import com.jkoolcloud.tnt4j.streams.scenario.WsResponse;
import com.jkoolcloud.tnt4j.streams.scenario.WsScenarioStep;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.streams.utils.WsStreamConstants;

/**
 * Implements a scheduled system command call activity stream, where each call response is assumed to represent a single
 * activity or event which should be recorded.
 * <p>
 * System command call is performed by invoking {@link Runtime#exec(String)}.
 * <p>
 * This activity stream requires parsers that can support {@link String} data to parse
 * {@link com.jkoolcloud.tnt4j.streams.scenario.WsResponse#getData()} provided string.
 * <p>
 * This activity stream supports configuration properties from {@link AbstractWsStream} (and higher hierarchy streams).
 *
 * @version $Revision: 3 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 * @see java.lang.Runtime#exec(String)
 */
public class CmdStream extends AbstractWsStream<String, String> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(CmdStream.class);

	/**
	 * Constructs an empty CmdStream. Requires configuration settings to set input stream source.
	 */
	public CmdStream() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	protected long getActivityItemByteSize(WsResponse<String, String> item) {
		return item == null || item.getData() == null ? 0 : item.getData().getBytes().length;
	}

	@Override
	protected JobDetail buildJob(String group, String jobId, JobDataMap jobAttrs) {
		return JobBuilder.newJob(CmdCallJob.class).withIdentity(jobId, group).usingJobData(jobAttrs).build();
	}

	/**
	 * Performs system command call.
	 *
	 * @param cmdData
	 *            command data: name and parameters
	 * @return command response string
	 * 
	 * @throws Exception
	 *             if exception occurs while performing system command call
	 */
	protected String executeCommand(String cmdData) throws Exception {
		if (StringUtils.isEmpty(cmdData)) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"CmdStream.cant.execute.cmd", cmdData);
			return null;
		}

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"CmdStream.invoking.command", cmdData);

		Process p = Runtime.getRuntime().exec(cmdData);
		String respStr = Utils.readInput(p.getInputStream(), false);

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"CmdStream.received.response", respStr);

		return respStr;
	}

	/**
	 * Scheduler job to execute system command call.
	 */
	public static class CmdCallJob extends CallJob {

		/**
		 * Constructs a new CmdCallJob.
		 */
		public CmdCallJob() {
		}

		@Override
		public void executeCalls(JobDataMap dataMap) {
			CmdStream stream = (CmdStream) dataMap.get(JOB_PROP_STREAM_KEY);
			WsScenarioStep scenarioStep = (WsScenarioStep) dataMap.get(JOB_PROP_SCENARIO_STEP_KEY);

			if (!scenarioStep.isEmpty()) {
				String respStr;
				Semaphore acquiredSemaphore;
				WsRequest<String> processedRequest;
				for (WsRequest<String> request : scenarioStep.requestsArray()) {
					if (stream.isShotDown()) {
						return;
					}

					respStr = null;
					acquiredSemaphore = null;
					processedRequest = null;
					try {
						acquiredSemaphore = stream.acquireSemaphore(request);
						processedRequest = stream.fillInRequest(request);
						respStr = stream.executeCommand(processedRequest.getData());
					} catch (VoidRequestException exc) {
						stream.logger().log(OpLevel.INFO,
								StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
								"AbstractWsStream.void.request", request.getId(), exc.getMessage());
					} catch (Throwable exc) {
						Utils.logThrowable(stream.logger(), OpLevel.ERROR,
								StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
								"CmdStream.execute.exception", stream.getName(), processedRequest.getId(), exc);
					} finally {
						if (StringUtils.isNotEmpty(respStr)) {
							stream.addInputToBuffer(new WsResponse<>(respStr, processedRequest));
						} else {
							stream.requestFailed(processedRequest);
							stream.releaseSemaphore(acquiredSemaphore, scenarioStep.getName(), request);
						}
					}
				}
			}
		}
	}
}
