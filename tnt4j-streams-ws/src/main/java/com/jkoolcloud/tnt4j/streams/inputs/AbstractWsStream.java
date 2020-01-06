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

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.triggers.AbstractTrigger;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.core.UsecTimestamp;
import com.jkoolcloud.tnt4j.streams.configure.WsStreamProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.scenario.*;
import com.jkoolcloud.tnt4j.streams.utils.StreamsCache;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.streams.utils.WsStreamConstants;

/**
 * Base class for scheduled service or system command request/call produced activity stream, where each call/request
 * response is assumed to represent a single activity or event which should be recorded.
 * <p>
 * This activity stream requires parsers that can support {@link String} data to parse
 * {@link com.jkoolcloud.tnt4j.streams.scenario.WsResponse#getData()} provided string.
 * <p>
 * This activity stream supports the following configuration properties (in addition to those supported by
 * {@link com.jkoolcloud.tnt4j.streams.inputs.AbstractBufferedStream}):
 * <ul>
 * <li>SynchronizeRequests - flag indicating that stream issued WebService requests shall be synchronized and handled in
 * configuration defined sequence - waiting for prior request to complete before issuing next. Default value -
 * {@code false}. (Optional)</li>
 * <li>List of Quartz configuration properties. See
 * <a href= "http://www.quartz-scheduler.org/documentation/quartz-2.3.0/configuration/">Quartz Configuration
 * Reference</a> for details.</li>
 * </ul>
 *
 * @param <T>
 *            type of handled response data
 *
 * @version $Revision: 2 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public abstract class AbstractWsStream<T> extends AbstractBufferedStream<WsResponse<T>> {

	/**
	 * Constant for name of built-in scheduler job property {@value}.
	 */
	protected static final String JOB_PROP_STREAM_KEY = "streamObj"; // NON-NLS
	/**
	 * Constant for name of built-in scheduler job property {@value}.
	 */
	protected static final String JOB_PROP_SCENARIO_STEP_KEY = "scenarioStepObj"; // NON-NLS
	/**
	 * Constant for name of built-in scheduler job property {@value}.
	 */
	protected static final String JOB_PROP_SEMAPHORE = "semaphoreObj"; // NON-NLS

	/**
	 * Semaphore for synchronizing stream requests.
	 */
	private Semaphore semaphore;

	private List<WsScenario> scenarioList;

	private Scheduler scheduler;
	private boolean synchronizeRequests = false;
	private Properties quartzProperties = new Properties();

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (WsStreamProperties.PROP_SYNCHRONIZE_REQUESTS.equalsIgnoreCase(name)) {
			synchronizeRequests = Utils.toBoolean(value);
		} else if (name.startsWith("org.quartz.")) { // NON-NLS
			quartzProperties.setProperty(name, value);
		}
	}

	@Override
	protected void applyProperties() throws Exception {
		super.applyProperties();

		if (synchronizeRequests) {
			semaphore = new Semaphore(1);
		}
	}

	@Override
	public Object getProperty(String name) {
		if (WsStreamProperties.PROP_SYNCHRONIZE_REQUESTS.equalsIgnoreCase(name)) {
			return synchronizeRequests;
		}

		return super.getProperty(name);
	}

	/**
	 * Initiates required Quartz configuration properties, if not set over streams configuration:
	 * 
	 * <ul>
	 * <li>{@code org.quartz.scheduler.instanceName} - name of stream appended by {@code "Scheduler"} suffix.</li>
	 * <li>{@code org.quartz.threadPool.threadCount=2}</li>
	 * </ul>
	 */
	protected void initQuartzProperties() {
		Utils.setPropertyIfAbsent(quartzProperties, "org.quartz.scheduler.instanceName", getName() + "Scheduler"); // NON-NLS
		Utils.setPropertyIfAbsent(quartzProperties, "org.quartz.threadPool.threadCount", "2"); // NON-NLS
	}

	@Override
	protected void initialize() throws Exception {
		super.initialize();

		if (scheduler == null) {
			initQuartzProperties();

			StdSchedulerFactory schf = new StdSchedulerFactory(quartzProperties);
			scheduler = schf.getScheduler();
			scheduler.start();
		}

		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"AbstractWsStream.scheduler.started", getName());

		loadScenarios();
	}

	/**
	 * Loads scenario steps into scheduler.
	 *
	 * @throws Exception
	 *             If any exception occurs while loading scenario steps to scheduler
	 */
	private void loadScenarios() throws Exception {
		int scenariosCount = 0;
		if (CollectionUtils.isNotEmpty(scenarioList)) {
			for (WsScenario scenario : scenarioList) {
				if (!scenario.isEmpty()) {
					for (WsScenarioStep step : scenario.getStepsList()) {
						scheduleScenarioStep(step);
					}
					scenariosCount++;
				}
			}
		}

		if (scenariosCount == 0) {
			throw new IllegalStateException(StreamsResources.getStringFormatted(WsStreamConstants.RESOURCE_BUNDLE_NAME,
					"AbstractWsStream.no.scenarios.defined", getName()));
		} else {
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"AbstractWsStream.stream.scenarios.loaded", getName(), scenariosCount);
		}
	}

	/**
	 * Schedules scenario step to be executed by step defined scheduler configuration data.
	 *
	 * @param step
	 *            scenario step instance to schedule
	 * @throws SchedulerException
	 *             if scheduler fails to schedule job for defined step
	 */
	protected void scheduleScenarioStep(WsScenarioStep step) throws SchedulerException {
		if (scheduler == null) {
			throw new SchedulerException(StreamsResources.getStringFormatted(WsStreamConstants.RESOURCE_BUNDLE_NAME,
					"AbstractWsStream.null.scheduler", getName()));
		}

		String enabledProp = step.getProperty("Enabled"); // NON-NLS
		if (StringUtils.equalsIgnoreCase("false", enabledProp)) { // NON-NLS
			logger().log(OpLevel.WARNING, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"AbstractWsStream.stream.step.disabled", getName(), step.getScenario().getName(), step.getName());
			return;
		}

		JobDataMap jobAttrs = new JobDataMap();
		jobAttrs.put(JOB_PROP_STREAM_KEY, this);
		jobAttrs.put(JOB_PROP_SCENARIO_STEP_KEY, step);
		jobAttrs.put(JOB_PROP_SEMAPHORE, semaphore);

		String jobId = step.getScenario().getName() + ':' + step.getName();

		JobDetail job = buildJob(step.getScenario().getName(), step.getName(), jobAttrs);

		ScheduleBuilder<?> scheduleBuilder;
		AbstractSchedulerData schedulerData = (AbstractSchedulerData) step.getSchedulerData();

		if (schedulerData instanceof CronSchedulerData) {
			CronSchedulerData csd = (CronSchedulerData) schedulerData;
			scheduleBuilder = CronScheduleBuilder.cronSchedule(csd.getExpression());
		} else {
			SimpleSchedulerData ssd = (SimpleSchedulerData) schedulerData;
			Integer repCount = ssd == null ? null : ssd.getRepeatCount();

			if (repCount != null && repCount == 0) {
				return;
			}

			if (repCount == null) {
				repCount = 1;
			}

			TimeUnit timeUnit = ssd == null ? TimeUnit.SECONDS : ssd.getUnits();
			long interval = ssd == null ? 1 : ssd.getInterval();

			scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
					.withIntervalInMilliseconds(timeUnit.toMillis(interval))
					.withRepeatCount(repCount > 0 ? repCount - 1 : repCount);
		}

		Date startAt = schedulerData == null ? new Date() : schedulerData.getStartAt();
		Trigger trigger = TriggerBuilder.newTrigger().withIdentity(String.valueOf(job.getKey()), getName())
				.startAt(startAt).withSchedule(scheduleBuilder).build();

		if (schedulerData != null && schedulerData.getStartDelay() != null && schedulerData.getStartDelay() > 0) {
			logger().log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"AbstractWsStream.stream.step.start.delayed", getName(), jobId, schedulerData.getStartDelay(),
					schedulerData.getStartDelayUnits());
		}

		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"AbstractWsStream.stream.scheduling.job", getName(), jobId);
		scheduler.scheduleJob(job, trigger);
	}

	/**
	 * Builds scheduler job for call scenario step.
	 *
	 * @param group
	 *            jobs group name
	 * @param jobId
	 *            job identifier
	 * @param jobAttrs
	 *            additional job attributes
	 * @return scheduler job detail object.
	 */
	protected abstract JobDetail buildJob(String group, String jobId, JobDataMap jobAttrs);

	/**
	 * Adds scenario to scenarios list.
	 *
	 * @param scenario
	 *            scenario to be added to list
	 */
	public void addScenario(WsScenario scenario) {
		if (scenarioList == null) {
			scenarioList = new ArrayList<>();
		}

		scenarioList.add(scenario);
	}

	/**
	 * Returns list of defined streaming scenarios.
	 *
	 * @return list of streaming scenarios
	 */
	public List<WsScenario> getScenarios() {
		return scenarioList;
	}

	@Override
	protected void cleanup() {
		if (scenarioList != null) {
			scenarioList.clear();
		}

		if (scheduler != null) {
			try {
				scheduler.shutdown(true);
			} catch (SchedulerException exc) {
				Utils.logThrowable(logger(), OpLevel.WARNING,
						StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
						"AbstractWsStream.error.closing.scheduler", exc);
			}
			scheduler = null;
		}

		super.cleanup();
	}

	/**
	 * Removes all inactive jobs from stream scheduler.
	 */
	protected void purgeInactiveSchedulerJobs() {
		if (scheduler != null) {
			try {
				int rCount = 0;
				Set<TriggerKey> triggerKeys = scheduler.getTriggerKeys(GroupMatcher.anyGroup());
				if (CollectionUtils.isNotEmpty(triggerKeys)) {
					for (TriggerKey tKey : triggerKeys) {
						try {
							Trigger t = scheduler.getTrigger(tKey);
							if (t != null && isInactiveTrigger(t)) {
								scheduler.deleteJob(t.getJobKey());
								rCount++;
								logger().log(OpLevel.TRACE,
										StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
										"AbstractWsStream.scheduler.removed.inactive.job", getName(), tKey);
							}
						} catch (SchedulerException exc) {
						}
					}
				}

				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
						"AbstractWsStream.scheduler.removed.inactive.jobs", getName(), rCount);
			} catch (SchedulerException exc) {
			}
		}
	}

	@Override
	protected boolean isInputEnded() {
		if (scheduler != null) {
			// Check if scheduler has any jobs executed - WS streams does not allow void schedulers.
			try {
				if (scheduler.getMetaData().getNumberOfJobsExecuted() == 0) {
					return false;
				}
			} catch (SchedulerException exc) {
			}

			// Check if scheduler is executing some jobs
			try {
				List<JobExecutionContext> runningJobs = scheduler.getCurrentlyExecutingJobs();
				logger().log(OpLevel.TRACE, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
						"AbstractWsStream.currently.executing", runningJobs.size());
				if (CollectionUtils.isNotEmpty(runningJobs)) {
					return false;
				}
			} catch (SchedulerException exc) {
			}

			// Check scheduler triggers - have they been fired and may fire again
			try {
				Set<TriggerKey> triggerKeys = scheduler.getTriggerKeys(GroupMatcher.anyGroup());
				logger().log(OpLevel.TRACE, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
						"AbstractWsStream.scheduler.triggers", triggerKeys.size());
				if (CollectionUtils.isNotEmpty(triggerKeys)) {
					for (TriggerKey tKey : triggerKeys) {
						try {
							Trigger t = scheduler.getTrigger(tKey);
							logger().log(OpLevel.TRACE,
									StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
									"AbstractWsStream.scheduler.trigger", t instanceof AbstractTrigger
											? ((AbstractTrigger<?>) t).getFullName() : t.getClass().getName(),
									t.getPreviousFireTime(), t.getNextFireTime());
							if (t != null && isActiveTrigger(t)) {
								return false;
							}
						} catch (SchedulerException exc) {
						}
					}
				}
			} catch (SchedulerException exc) {
			}

			// Looks like this scheduler already has done it's job...
			try {
				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
						"AbstractWsStream.inactive.scheduler", scheduler.getMetaData());
			} catch (SchedulerException exc) {
			}
		}

		return true;
	}

	private static boolean isActiveTrigger(Trigger t) {
		return t.getPreviousFireTime() == null || t.mayFireAgain();
	}

	private static boolean isInactiveTrigger(Trigger t) {
		return t.getPreviousFireTime() != null && !t.mayFireAgain();
	}

	@Override
	public String[] getDataTags(Object data) {
		return data instanceof WsResponse<?> ? ((WsResponse<?>) data).getTags() : super.getDataTags(data);
	}

	@Override
	protected ActivityInfo applyParsers(Object data, String... tags) throws IllegalStateException, ParseException {
		return super.applyParsers(data instanceof WsResponse<?> ? ((WsResponse<?>) data).getData() : data, tags);
	}

	/**
	 * Marks stream state for picked item from buffer and performs post parsing actions for provided activity data item.
	 *
	 * @param item
	 *            processed activity data item
	 *
	 * @see #postParse(com.jkoolcloud.tnt4j.streams.scenario.WsResponse)
	 */
	protected void pickAndPostParseItem(WsResponse<T> item) {
		postParse(item);
	}

	@Override
	protected void setCurrentItem(WsResponse<T> item) {
		pickAndPostParseItem(getCurrentItem());

		super.setCurrentItem(item);
	}

	/**
	 * Performs post parsing actions for provided activity data item.
	 * <p>
	 * Generic post parsing case is to release all acquired requests synchronization semaphores.
	 *
	 * @param item
	 *            processed activity data item
	 */
	protected void postParse(WsResponse<T> item) {
		if (item instanceof WsReqResponse) {
			WsReqResponse<?, ?> resp = (WsReqResponse<?, ?>) item;

			if (semaphore != null) {
				releaseSemaphore(semaphore, getName(), resp.getOriginalRequest());
			}

			Semaphore reqSemaphore = resp.getSemaphore();

			if (reqSemaphore != null) {
				releaseSemaphore(reqSemaphore, resp.getScenarioStep().getName(), resp.getOriginalRequest());
			}
		} else {
			if (semaphore != null) {
				releaseSemaphore(semaphore, getName(), null);
			}
		}
	}

	/**
	 * Performs pre-processing of request/command/query body data: it can be expressions evaluation, filling in variable
	 * values and so on.
	 *
	 * @param requestData
	 *            request/command/query body data
	 * @return preprocessed request/command/query body data string
	 */
	protected String preProcess(String requestData) {
		return requestData;
	}

	/**
	 * Fills in request/query/command string having variable expressions with parameters stored in stream configuration
	 * properties map and streams cache {@link com.jkoolcloud.tnt4j.streams.utils.StreamsCache}.
	 *
	 * @param reqDataStr
	 *            JDBC query string
	 * @return variable values filled in JDBC query string
	 *
	 * @see #fillInRequestData(String, String)
	 */
	protected String fillInRequestData(String reqDataStr) {
		return fillInRequestData(reqDataStr, (String) null);
	}

	/**
	 * Fills in request/query/command string having variable expressions with parameters stored in stream configuration
	 * properties map map and streams cache {@link com.jkoolcloud.tnt4j.streams.utils.StreamsCache}.
	 *
	 * @param reqDataStr
	 *            JDBC query string
	 * @param format
	 *            format of value to fill
	 * @return variable values filled in JDBC query string
	 *
	 * @see #fillInRequestData(String, java.util.Map)
	 * @see #fillInRequestCacheData(String, String)
	 */
	protected String fillInRequestData(String reqDataStr, String format) {
		String frd = fillInRequestData(reqDataStr, getConfigProperties());
		frd = fillInRequestCacheData(frd, format);

		return frd;
	}

	/**
	 * Returns streams specific configuration properties map.
	 *
	 * @return streams specific configuration properties map
	 */
	protected Map<String, String> getConfigProperties() {
		return null;
	}

	/**
	 * Fills in request/query/command string having variable expressions with parameters stored in
	 * {@code streamProperties} map.
	 *
	 * @param reqDataStr
	 *            request/query/command string
	 * @param streamProperties
	 *            stream properties map
	 * @return variable values filled in request/query/command string
	 */
	protected String fillInRequestData(String reqDataStr, Map<String, String> streamProperties) {
		if (StringUtils.isEmpty(reqDataStr) || MapUtils.isEmpty(streamProperties)) {
			return reqDataStr;
		}

		List<String> vars = new ArrayList<>();
		Utils.resolveExpressionVariables(vars, reqDataStr);
		// Utils.resolveCfgVariables(vars, reqDataStr);

		String reqData = reqDataStr;
		if (CollectionUtils.isNotEmpty(vars)) {
			String varVal;
			for (String rdVar : vars) {
				varVal = streamProperties.get(Utils.getVarName(rdVar));
				if (varVal != null) {
					reqData = reqData.replace(rdVar, varVal);
					logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
							"AbstractWsStream.filling.req.data.variable", rdVar, Utils.toString(varVal));
				}
			}
		}

		return reqData;
	}

	/**
	 * Fills in request/query/command string having variable expressions with parameters stored in streams cache
	 * {@link com.jkoolcloud.tnt4j.streams.utils.StreamsCache}.
	 *
	 * @param reqDataStr
	 *            request/query/command string
	 * @param format
	 *            format of value to fill
	 * @return variable values filled in request/query/command string
	 */
	protected String fillInRequestCacheData(String reqDataStr, String format) {
		if (StringUtils.isEmpty(reqDataStr)) {
			return reqDataStr;
		}

		List<String> vars = new ArrayList<>();
		Utils.resolveExpressionVariables(vars, reqDataStr);
		// Utils.resolveCfgVariables(vars, reqDataStr);

		String reqData = reqDataStr;
		if (CollectionUtils.isNotEmpty(vars)) {
			String varVal;
			for (String rdVar : vars) {
				Object cValue = StreamsCache.getValue(Utils.getVarName(rdVar));
				varVal = formattedValue(cValue, format);
				if (varVal != null) {
					reqData = reqData.replace(rdVar, varVal);
					logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
							"AbstractWsStream.filling.req.data.variable", rdVar, Utils.toString(varVal));
				}
			}
		}

		return reqData;
	}

	/**
	 * Formats provided value as a string using defined format pattern.
	 *
	 * @param cValue
	 *            value to format
	 * @param format
	 *            format pattern of the value
	 * @return formatted value string
	 */
	protected static String formattedValue(Object cValue, String format) {
		if (StringUtils.isNotEmpty(format)) {
			if (cValue instanceof UsecTimestamp) {
				return ((UsecTimestamp) cValue).toString(format);
			} else if (cValue instanceof Date) {
				SimpleDateFormat df = new SimpleDateFormat(format);
				return df.format(cValue);
			} else if (cValue instanceof Number) {
				DecimalFormat df = new DecimalFormat(format);
				return df.format(cValue);
			} else {
				return Utils.toString(cValue);
			}
		}

		return Utils.toString(cValue);
	}

	/**
	 * Acquires semaphore to be used for requests synchronization. Semaphore can be bound to stream or scenario step.
	 * Stream semaphore has preference against scenario step semaphore.
	 * <p>
	 * To synchronize stream or scenario step requests use flag ({@code true}/{@code false}) type stream/scenario step
	 * configuration property
	 * {@value com.jkoolcloud.tnt4j.streams.configure.WsStreamProperties#PROP_SYNCHRONIZE_REQUESTS}.
	 *
	 * @param request
	 *            request semaphore is obtained for
	 * @return acquired semaphore instance or {@code null} if no semaphore was acquired
	 * @throws InterruptedException
	 *             if current thread got interrupted when acquiring semaphore
	 */
	protected Semaphore acquireSemaphore(WsRequest<?> request) throws InterruptedException {
		WsScenarioStep scenarioStep = request.getScenarioStep();
		Semaphore stepSemaphore = scenarioStep.getSemaphore();

		if (semaphore != null) {
			while (!semaphore.tryAcquire()) {
				Thread.sleep(50);
			}
			logger().log(OpLevel.DEBUG, StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME,
					"AbstractWsStream.semaphore.acquired.stream"), getName(), request.getId());
			return semaphore;
		}

		if (stepSemaphore != null) {
			while (!stepSemaphore.tryAcquire()) {
				Thread.sleep(50);
			}
			logger().log(OpLevel.DEBUG, StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME,
					"AbstractWsStream.semaphore.acquired.step"), scenarioStep.getName(), request.getId());
			return stepSemaphore;
		}

		return null;
	}

	/**
	 * Releases provided semaphore to continue next request execution.
	 *
	 * @param acquiredSemaphore
	 *            requests semaphore to release
	 * @param lockName
	 *            locker object name
	 * @param request
	 *            request semaphore was obtained for
	 */
	protected void releaseSemaphore(Semaphore acquiredSemaphore, String lockName, WsRequest<?> request) {
		if (acquiredSemaphore != null && acquiredSemaphore.availablePermits() < 1) {
			logger().log(OpLevel.DEBUG,
					StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME,
							"AbstractWsStream.semaphore.release"),
					lockName, request == null ? "UNKNOWN" : request.getId()); // NON-NLS
			acquiredSemaphore.release();
		}
	}

	/**
	 * Base scheduler job class to be executing implementing stream calls.
	 */
	protected static abstract class CallJob implements Job {
		@Override
		public void execute(JobExecutionContext context) throws JobExecutionException {
			JobDataMap dataMap = context.getJobDetail().getJobDataMap();
			AbstractWsStream<?> stream = (AbstractWsStream<?>) dataMap.get(JOB_PROP_STREAM_KEY);

			stream.startProcessingTask();
			try {
				executeCalls(dataMap);
			} finally {
				stream.endProcessingTask();
			}
		}

		/**
		 * Executes dedicated endpoint (e.g. JAX-RS, JAX-WS, JDBC, etc.) calls.
		 * 
		 * @param dataMap
		 *            job data map instance
		 */
		protected abstract void executeCalls(JobDataMap dataMap);
	}
}
