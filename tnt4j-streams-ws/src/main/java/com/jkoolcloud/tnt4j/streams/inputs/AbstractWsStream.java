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

package com.jkoolcloud.tnt4j.streams.inputs;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.script.CompiledScript;
import javax.script.ScriptException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.triggers.AbstractTrigger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.core.UsecTimestamp;
import com.jkoolcloud.tnt4j.streams.configure.WsStreamProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.matchers.Matchers;
import com.jkoolcloud.tnt4j.streams.parsers.data.CommonActivityData;
import com.jkoolcloud.tnt4j.streams.scenario.*;
import com.jkoolcloud.tnt4j.streams.utils.*;

/**
 * Base class for scheduled service or system command request/call/query produced activity stream, where each
 * call/request//query response is assumed to represent a single activity or event which should be recorded.
 * <p>
 * This activity stream requires parsers that can support {@code RS} defined type data to parse
 * {@link com.jkoolcloud.tnt4j.streams.scenario.WsResponse#getData()} provided data.
 * <p>
 * This activity stream provides activity data request parameters accessible over {@code $METADATA$} locator.
 * <p>
 * This activity stream supports the following configuration properties (in addition to those supported by
 * {@link com.jkoolcloud.tnt4j.streams.inputs.AbstractBufferedStream}):
 * <ul>
 * <li>SynchronizeRequests - flag indicating that stream issued WebService requests shall be synchronized and handled in
 * configuration defined sequence - waiting for prior request to complete before issuing next. Default value -
 * {@code false}. (Optional)</li>
 * <li>DropRecurrentRequests - flag indicating whether to drop streaming stream input buffer contained recurring
 * requests, when stream input scheduler invokes requests faster than they can be processed (parsed and sent to sink,
 * e.g. because of sink/JKool limiter throttling). Default value - {@code true}. (Optional)</li>
 * <li>List of Quartz configuration properties. See
 * <a href= "http://www.quartz-scheduler.org/documentation/quartz-2.3.0/configuration/">Quartz Configuration
 * Reference</a> for details.</li>
 * </ul>
 *
 * @param <RQ>
 *            type of handled request data
 * @param <RS>
 *            type of handled response data
 *
 * @version $Revision: 4 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public abstract class AbstractWsStream<RQ, RS> extends AbstractBufferedStream<WsResponse<RQ, RS>> {

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
	 * Constant for name of built-in request parameter {@value}.
	 */
	protected static final String REQ_URL_PARAM = "WS_REQ_URL"; // NON-NLS

	private static final String OBJECT_TYPE = "OBJECT";// NON-NLS

	// private static final Pattern GROOVY_EXP_PATTERN = Pattern.compile("\\s*use\\s*\\(.+\\)\\s*\\{(?s).+\\}");

	private final Cache<String, CompiledScript> scriptsCache = CacheBuilder.newBuilder().maximumSize(100)
			.expireAfterAccess(30, TimeUnit.MINUTES).build();

	/**
	 * Semaphore for synchronizing stream requests.
	 */
	private Semaphore semaphore;

	private List<WsScenario> scenarioList;

	private Scheduler scheduler;
	private boolean synchronizeRequests = false;
	private boolean dropRecurrentRequests = true;
	private final Properties quartzProperties = new Properties();

	private final Set<String> parsedRequests = new HashSet<>();

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (WsStreamProperties.PROP_SYNCHRONIZE_REQUESTS.equalsIgnoreCase(name)) {
			synchronizeRequests = Utils.toBoolean(value);
		} else if (WsStreamProperties.PROP_DROP_RECURRENT_REQUESTS.equalsIgnoreCase(name)) {
			dropRecurrentRequests = Utils.toBoolean(value);
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
		if (WsStreamProperties.PROP_DROP_RECURRENT_REQUESTS.equalsIgnoreCase(name)) {
			return dropRecurrentRequests;
		}
		if (name.startsWith("org.quartz.")) { // NON-NLS
			quartzProperties.getProperty(name);
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
	 *
	 * @throws SchedulerException
	 *             if scheduler fails to schedule job for defined step
	 */
	protected void scheduleScenarioStep(WsScenarioStep step) throws SchedulerException {
		if (scheduler == null) {
			throw new SchedulerException(StreamsResources.getStringFormatted(WsStreamConstants.RESOURCE_BUNDLE_NAME,
					"AbstractWsStream.null.scheduler", getName()));
		}

		if (isShotDown()) {
			return;
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
				repCount = -1;
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

	@Override
	public void addReference(Object refObject) throws IllegalStateException {
		if (refObject instanceof WsScenario) {
			addScenario((WsScenario) refObject);
		}
		super.addReference(refObject);
	}

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

		synchronized (parsedRequests) {
			parsedRequests.clear();
		}

		scriptsCache.invalidateAll();

		super.cleanup();
	}

	@Override
	protected boolean isItemConsumed(WsResponse<RQ, RS> item) {
		if (item == null || item.getData() == null) {
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"AbstractWsStream.response.consumption.null");
			return true;
		}

		if (dropRecurrentRequests) {
			WsResponse<RQ, RS> recurringItem = getRecurrentResponse(item, inputBuffer);

			if (recurringItem != null) {
				logger().log(OpLevel.WARNING, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
						"AbstractWsStream.response.consumption.drop", item.getOriginalRequest().fqn());
				inputBuffer.remove(recurringItem);
				cleanupItem(recurringItem);
			}
		}

		boolean consumed = isResponseConsumed(item);
		if (consumed) {
			cleanupItem(item);
			postParse(item);
		}

		return consumed;
	}

	/**
	 * Checks whether provided response item is consumed, and stream should take next response from buffer.
	 * 
	 * @param item
	 *            response item to check
	 * @return {@code true} if response is consumed, {@code false} - otherwise
	 */
	protected boolean isResponseConsumed(WsResponse<RQ, RS> item) {
		return true;
	}

	@Override
	protected void setCurrentItem(WsResponse<RQ, RS> item) {
		super.setCurrentItem(item);

		if (item != null) {
			requestParsingStarted(item);
		}
	}

	@Override
	protected void cleanupItem(WsResponse<RQ, RS> item) {
		if (item != null) {
			if (item.getData() != null) {
				closeResponse(item.getData());
			}
			responseConsumed(item);
		}
	}

	/**
	 * Removes all inactive jobs from stream scheduler.
	 */
	protected void purgeInactiveSchedulerJobs() {
		if (scheduler != null && !isShotDown()) {
			try {
				int rCount = 0;
				int lCount = 0;
				Set<TriggerKey> triggerKeys = scheduler.getTriggerKeys(GroupMatcher.anyGroup());
				if (CollectionUtils.isNotEmpty(triggerKeys)) {
					for (TriggerKey tKey : triggerKeys) {
						try {
							Trigger t = scheduler.getTrigger(tKey);
							if (t == null) {
								continue;
							}

							if (isInactiveTrigger(t)) {
								scheduler.deleteJob(t.getJobKey());
								rCount++;
								logger().log(OpLevel.TRACE,
										StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
										"AbstractWsStream.scheduler.removed.inactive.job", getName(), tKey);
							} else {
								lCount++;
							}
						} catch (SchedulerException exc) {
						}
					}
				}

				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
						"AbstractWsStream.scheduler.removed.inactive.jobs", getName(), rCount, lCount);
			} catch (SchedulerException exc) {
			}
		}
	}

	@Override
	protected boolean isInputEnded() {
		if (scheduler != null && !isShotDown()) {
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
							if (t == null) {
								continue;
							}

							logger().log(OpLevel.TRACE,
									StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
									"AbstractWsStream.scheduler.trigger", t instanceof AbstractTrigger
											? ((AbstractTrigger<?>) t).getFullName() : t.getClass().getName(),
									t.getPreviousFireTime(), t.getNextFireTime());
							if (isActiveTrigger(t)) {
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
		return data instanceof WsResponse<?, ?> ? ((WsResponse<?, ?>) data).getTags() : super.getDataTags(data);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected ActivityInfo applyParsers(Object data, String... tags) throws IllegalStateException, ParseException {
		if (data instanceof WsResponse) {
			WsResponse<RQ, RS> response = (WsResponse<RQ, RS>) data;
			RS respData = response.getData();
			Map<String, ?> reqMetadata = response.getOriginalRequest().getParametersMap();
			CommonActivityData<?> dataPack = new CommonActivityData<>(respData, reqMetadata);

			return super.applyParsers(dataPack, tags);
		}

		return super.applyParsers(data, tags);
	}

	/**
	 * Performs post parsing actions for provided activity data item.
	 * <p>
	 * Generic post parsing case is to release all acquired requests synchronization semaphores.
	 *
	 * @param item
	 *            processed activity data item
	 */
	protected void postParse(WsResponse<RQ, RS> item) {
		if (item != null) {
			if (semaphore != null) {
				releaseSemaphore(semaphore, getName(), item.getOriginalRequest());
			}

			Semaphore reqSemaphore = item.getSemaphore();

			if (reqSemaphore != null) {
				releaseSemaphore(reqSemaphore, item.getScenarioStep().getName(), item.getOriginalRequest());
			}
		} else {
			if (semaphore != null) {
				releaseSemaphore(semaphore, getName(), null);
			}
		}
	}

	/**
	 * Fills-in request/query/command string having variable expressions with values stored in stream configuration
	 * properties maps and streams cache {@link com.jkoolcloud.tnt4j.streams.utils.StreamsCache}.
	 *
	 * @param reqDataStr
	 *            request/query/command string
	 * @return variable values filled-in request/query/command string
	 *
	 * @see #fillInRequestData(String, String, String)
	 */
	public String fillInRequestData(String reqDataStr) {
		return fillInRequestData(reqDataStr, null, null);
	}

	/**
	 * Fills-in request/query/command string having variable expressions with values stored in stream configuration
	 * properties maps and streams cache {@link com.jkoolcloud.tnt4j.streams.utils.StreamsCache}.
	 *
	 * @param reqDataStr
	 *            request/query/command string
	 * @param format
	 *            format of value to fill
	 * @return variable values filled-in request/query/command string
	 *
	 * @see #fillInRequestData(DataFillContext)
	 */
	public String fillInRequestData(String reqDataStr, String format) {
		return fillInRequestData(reqDataStr, format, null);
	}

	/**
	 * Fills-in request/query/command string having variable expressions with values stored in stream configuration
	 * properties maps and streams cache {@link com.jkoolcloud.tnt4j.streams.utils.StreamsCache}.
	 *
	 * @param reqDataStr
	 *            request/query/command string
	 * @param format
	 *            format of value to fill
	 * @return variable values filled-in request/query/command string
	 *
	 * @see #fillInRequestData(DataFillContext)
	 */
	public String fillInRequestData(String reqDataStr, String format, String tz) {
		if (StringUtils.isEmpty(reqDataStr)) {
			return reqDataStr;
		}

		DataFillContext ctx = makeDataContext(reqDataStr, format, tz, null);

		return (String) fillInRequestData(ctx);
	}

	/**
	 * Fills-in request/query/command string having variable expressions with values configured over provided
	 * {@code context}.
	 *
	 * @param context
	 *            request data fill-in context to use
	 * @return variable values filled-in request/query/command data entity
	 * 
	 * @see #getVariableValue(String, DataFillContext)
	 * @see #formattedValue(Object, String, String)
	 */
	protected Object fillInRequestData(DataFillContext context) {
		if (context == null || StringUtils.isEmpty(context.getData())) {
			return context.getData();
		}

		String reqData = context.getData();
		Set<String> vars = new HashSet<>();
		Utils.resolveExpressionVariables(vars, reqData);
		// Utils.resolveCfgVariables(vars, reqData);

		if (CollectionUtils.isNotEmpty(vars)) {
			for (String rdVar : vars) {
				Object cValue = getVariableValue(Utils.getVarName(rdVar), context);
				if (OBJECT_TYPE.equals(context.getType())) {
					logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
							"AbstractWsStream.filling.req.data.variable", rdVar, Utils.toString(cValue));
					return cValue;
				} else {
					String varVal = formattedValue(cValue, context.getFormat(), context.getTimeZone());
					reqData = reqData.replace(rdVar, varVal);
					logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
							"AbstractWsStream.filling.req.data.variable", rdVar, varVal);
				}
			}

			if (!reqData.equals(context.getData()) && Utils.isVariableExpression(reqData)) {
				context.setData(reqData);
				return fillInRequestData(context);
			}
		}

		return reqData;
	}

	/**
	 * Resolves variable defined value from:
	 * <ul>
	 * <li>{@code context} parameter passed entries: request, step and scenario. Value resolution from context provided
	 * entries is stream dependent.</li>
	 * <li>streams cache - {@link com.jkoolcloud.tnt4j.streams.utils.StreamsCache#getValue(String)}</li>
	 * <li>stream configuration properties - {@link #getProperty(String)}</li>
	 * <li>groovy expression evaluated value - see
	 * <a href="https://docs.groovy-lang.org/latest/html/api/groovy/time/TimeCategory.html">Groovy API TimeCategory</a>
	 * for details. See {@link #getScript(String)} for supported groovy script additions. NOTE: expressions shall not
	 * contain any whitespace symbols!</li>
	 * </ul>
	 * 
	 * @param varName
	 *            variable name to resolve
	 * @param context
	 *            variable value resolution context to use
	 * @return variable resolved value
	 *
	 * @see #getReqContextProperty(String, com.jkoolcloud.tnt4j.streams.inputs.AbstractWsStream.DataFillContext)
	 * @see com.jkoolcloud.tnt4j.streams.utils.StreamsCache#getValue(String)
	 * @see #getProperty(String)
	 * @see #evaluateExpr(String)
	 */
	protected Object getVariableValue(String varName, DataFillContext context) {
		Object rValue = getReqContextProperty(varName, context);
		if (rValue == null) {
			rValue = StreamsCache.getValue(varName);
		}
		if (rValue == null) {
			rValue = getProperty(varName);
		}
		if (rValue == null) {
			try {
				rValue = evaluateExpr(varName);
			} catch (ScriptException e) {
				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
						"AbstractWsStream.expr.evaluation.failed", varName, e.getMessage());
			}
		}

		return rValue;
	}

	/**
	 * Resolves request context provided property value. Property value resolution is made from context provided
	 * entities: request, step and scenario.
	 *
	 * @param varName
	 *            property name to resolve
	 * @param context
	 *            request context context to use
	 * @return resolved property value
	 * 
	 * @see com.jkoolcloud.tnt4j.streams.inputs.AbstractWsStream.DataFillContext#getReqParameter(String)
	 * @see com.jkoolcloud.tnt4j.streams.scenario.WsScenarioStep#getProperty(String)
	 * @see com.jkoolcloud.tnt4j.streams.scenario.WsScenario#getProperty(String)
	 */
	protected Object getReqContextProperty(String varName, DataFillContext context) {
		Object rValue = context.getReqParameter(varName);
		if (rValue == null) {
			WsRequest<?> req = context.getRequest();
			WsScenarioStep reqStep = req == null ? null : req.getScenarioStep();
			if (reqStep != null) {
				rValue = reqStep.getProperty(varName);

				if (rValue == null) {
					WsScenario reqScenario = reqStep.getScenario();
					if (reqScenario != null) {
						rValue = reqScenario.getProperty(varName);
					}
				}
			}
		}

		return rValue;
	}

	/**
	 * Evaluates groovy script defined variable expression.
	 * 
	 * @param varExpr
	 *            variable expression to evaluate
	 * @return groovy script evaluated expression value
	 * 
	 * @throws ScriptException
	 *             if can't compose valid or compile groovy script
	 * 
	 * @see #getScript(String)
	 */
	protected Object evaluateExpr(String varExpr) throws ScriptException {
		String[] vet = varExpr.split(":"); // NON-NLS
		if (vet.length == 1 || !vet[0].equalsIgnoreCase("groovy") || StringUtils.isEmpty(vet[1])) { // NON-NLS
			return null;
		}

		String scriptStr = vet[1];
		CompiledScript compiledScript = scriptsCache.getIfPresent(scriptStr);
		try {
			if (compiledScript == null) {
				String script = getScript(scriptStr);
				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
						"AbstractWsStream.expr.script", scriptStr, script);
				if (script == null) {
					throw new ScriptException(StreamsResources.getStringFormatted(
							WsStreamConstants.RESOURCE_BUNDLE_NAME, "AbstractWsStream.expr.compose.failed", scriptStr));
				}

				compiledScript = StreamsScriptingUtils.compileGroovyScript(null, script);
				scriptsCache.put(scriptStr, compiledScript);
			}

			return compiledScript.eval();
		} catch (ScriptException se) {
			throw se;
		}
	}

	/**
	 * Prepares groovy script for provided variable expression.
	 * <p>
	 * Supports these expression keywords:
	 * <ul>
	 * <li>HOUR_BEGIN - to pick 00:00 minute and second of the evaluated hour value</li>
	 * <li>HOUR_END - to pick 59:59 minute and second of the evaluated hour value</li>
	 * </ul>
	 * 
	 * @param varExpr
	 *            variable expression to use for groovy script
	 * @return expression groovy script
	 */
	protected String getScript(String varExpr) {
		String scriptStr = StringUtils.trim(varExpr);
		if (StringUtils.isEmpty(scriptStr)) {
			return null;
		}

		// if (GROOVY_EXP_PATTERN.matcher(scriptStr).matches()) {
		// return scriptStr;
		// }

		String gScript = translateKeyword(scriptStr);
		if (scriptStr.length() >= 8) {
			switch (scriptStr.substring(0, 8)) {
			case "HOUR_BEG": // NOTE: full value is HOUR_BEGIN
				String dateVariable = translateKeyword(scriptStr.substring(11));
				gScript = " def vDate = " + dateVariable + ";";
				gScript += "    vDate.putAt('minutes',0);";
				gScript += "    vDate.putAt('seconds',0);";
				gScript += "return vDate;";
				break;
			case "HOUR_END":
				dateVariable = translateKeyword(scriptStr.substring(9));
				gScript = " def vDate = " + dateVariable + ";";
				gScript += "    vDate.putAt('minutes',59);";
				gScript += "    vDate.putAt('seconds',59);";
				gScript += "return vDate;";
				break;
			default:
			}
		}

		return "use( groovy.time.TimeCategory ) { " + gScript + " }"; // NON-NLS
	}

	private String translateKeyword(String script) {
		switch (script) {
		case "now":
			return "new Date()";
		default:
			return script;
		}
	}

	/**
	 * Fills-in request data and parameter values having variable expressions.
	 * 
	 * @param req
	 *            request instance to fill-in data
	 * @return request instance having filled-in values
	 * 
	 * @throws VoidRequestException
	 *             if request can't be build from request context data or is meaningless
	 * 
	 * @see #fillInRequest(WsRequest, RequestFillContext)
	 */
	protected WsRequest<String> fillInRequest(WsRequest<String> req) throws VoidRequestException {
		RequestFillContext context = new RequestFillContext(isDirectRequestUse());

		return fillInRequest(req, context);
	}

	/**
	 * Returns flag indicating if stream will use filled-in request directly. Indirect use is when some additional
	 * object (e.g. SQL statement) is created from provided request data.
	 * 
	 * @return {@code true} if stream uses request directly, {@code false} - if stream will make additional request
	 *         object from request data
	 */
	protected boolean isDirectRequestUse() { // TODO: clear naming
		return true;
	}

	/**
	 * Fills-in request data and parameter values having variable expressions.
	 * <p>
	 * Request entities filled-in:
	 * <ul>
	 * <li>parameters (identifiers and values)</li>
	 * <li>identifier</li>
	 * <li>data</li>
	 * </ul>
	 * 
	 * @param req
	 *            request instance to fill-in data
	 * @param context
	 *            request values fill-in context to use
	 * @return request instance having filled-in values
	 * 
	 * @throws VoidRequestException
	 *             if request can't be build from request context data or is meaningless
	 * 
	 * @see #fillInRequestData(DataFillContext)
	 */
	protected WsRequest<String> fillInRequest(WsRequest<String> req, RequestFillContext context)
			throws VoidRequestException {
		DataFillContext ctx = makeDataContext(null, null, null, context);
		ctx.setRequest(req);
		checkConditions(req, ctx);

		WsRequest<String> fReq = req.clone();
		if (req.isDynamic()) {
			ctx.setRequest(fReq);

			if (context.isFillingParams()) {
				for (Map.Entry<String, WsRequest.Parameter> reqParam : fReq.getParameters().entrySet()) {
					reqParam.getValue()
							.setValue(fillInRequestData(ctx.setData(reqParam.getValue().getStringValue())
									.setFormat(reqParam.getValue().getAttribute(WsRequest.Parameter.ATTR_FORMAT))
									.setType(reqParam.getValue().getAttribute(WsRequest.Parameter.ATTR_TYPE))
									.setTimeZone(reqParam.getValue().getAttribute(WsRequest.Parameter.ATTR_TIMEZONE))));
				}
			}

			fReq.setId((String) fillInRequestData(ctx.setData(fReq.getId()).reset()));
			fReq.setData((String) fillInRequestData(ctx.setData(fReq.getData())));
		}

		return fReq;
	}

	/**
	 * Makes request data fill-in context instance for provided request entity data string, value format and request
	 * fill-in context.
	 * 
	 * @param reqDataStr
	 *            request entity data string
	 * @param format
	 *            format of value to fill
	 * @param tz
	 *            date-time value format timezone
	 * @param reqCtx
	 *            request fill-in context to use
	 * @return data fill-in context instance
	 */
	protected DataFillContext makeDataContext(String reqDataStr, String format, String tz, RequestFillContext reqCtx) {
		DataFillContext dataCtx = new DataFillContext(reqDataStr);
		dataCtx.setFormat(format);
		dataCtx.setTimeZone(tz);

		if (reqCtx != null) {
			for (Map.Entry<String, Object> rcme : reqCtx.entrySet()) {
				if (rcme.getKey().startsWith(DataFillContext.KEY_PREFIX)) {
					dataCtx.put(rcme.getKey(), rcme.getValue());
				}
			}
		}

		return dataCtx;
	}

	/**
	 * Formats provided value as a string using defined format pattern.
	 *
	 * @param cValue
	 *            value to format
	 * @param format
	 *            format pattern of the value
	 * @param tz
	 *            timezone to format date-time values
	 * @return formatted value string
	 */
	protected static String formattedValue(Object cValue, String format, String tz) {
		if (StringUtils.isNotEmpty(format)) {
			if (cValue instanceof UsecTimestamp) {
				return ((UsecTimestamp) cValue).toString(format, tz);
			} else if (cValue instanceof Date) {
				FastDateFormat df = FastDateFormat.getInstance(format,
						StringUtils.isEmpty(tz) ? null : TimeZone.getTimeZone(tz));
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
	 * Checks if request matches any of defined conditions.
	 * 
	 * @param req
	 *            request instance to check conditions against
	 * @param context
	 *            variable values resolution context to use
	 * 
	 * @throws VoidRequestException
	 *             if request matched any of conditions and resolution is to skip or to stop
	 */
	protected void checkConditions(WsRequest<String> req, DataFillContext context) throws VoidRequestException {
		List<Condition> reqConditions = req.getConditions();

		if (CollectionUtils.isNotEmpty(reqConditions)) {
			for (Condition condition : reqConditions) {
				if (!condition.isEmpty()) {
					for (String matchExp : condition.getMatchExpressions()) {
						boolean match = false;

						try {
							match = matchExpression(matchExp, context);
						} catch (Exception exc) {
							Utils.logThrowable(logger(), OpLevel.WARNING,
									StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
									"AbstractWsStream.condition.match.failed", req.getId(), condition.getId(), exc);
						}

						if (match) {
							if (condition.getResolution() == Condition.Resolution.STOP) {
								offerDieMarker();
							}

							throw new VoidRequestException(StreamsResources.getStringFormatted(
									WsStreamConstants.RESOURCE_BUNDLE_NAME, "AbstractWsStream.condition.match",
									condition.getId(), condition.getResolution()));
						}
					}
				}
			}
		}
	}

	/**
	 * Evaluates provided match expressions {@code matchExp} against {@code context} provided data.
	 * 
	 * @param matchExp
	 *            match expression to evaluate
	 * @param context
	 *            variable values resolution context to use
	 * @return {@code true} if expressions matches context provided data, {@code false} - otherwise
	 * 
	 * @throws Exception
	 *             if evaluation expression is empty or evaluation of match expression fails
	 */
	protected boolean matchExpression(String matchExp, DataFillContext context) throws Exception {
		Set<String> vars = new HashSet<>();
		Utils.resolveExpressionVariables(vars, matchExp);
		// Utils.resolveCfgVariables(vars, reqData);

		Map<String, Object> valBindings = new HashMap<>(vars.size());
		if (CollectionUtils.isNotEmpty(vars)) {
			for (String rdVar : vars) {
				Object vValue = getVariableValue(Utils.getVarName(rdVar), context);
				valBindings.put(rdVar, vValue);
			}
		}
		return Matchers.evaluateBindings(matchExp, valBindings);
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
	 * 
	 * @throws InterruptedException
	 *             if current thread got interrupted when acquiring semaphore
	 */
	protected Semaphore acquireSemaphore(WsRequest<RQ> request) throws InterruptedException {
		if (semaphore != null) {
			while (!semaphore.tryAcquire()) {
				Thread.sleep(50);
			}
			logger().log(OpLevel.DEBUG, StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME,
					"AbstractWsStream.semaphore.acquired.stream"), getName(), request.getId());
			return semaphore;
		}

		WsScenarioStep scenarioStep = request.getScenarioStep();
		Semaphore stepSemaphore = scenarioStep.getSemaphore();

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
	protected void releaseSemaphore(Semaphore acquiredSemaphore, String lockName, WsRequest<RQ> request) {
		if (acquiredSemaphore != null && acquiredSemaphore.availablePermits() < 1) {
			logger().log(OpLevel.DEBUG,
					StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME,
							"AbstractWsStream.semaphore.release"),
					lockName, request == null ? "UNKNOWN" : request.getId()); // NON-NLS
			acquiredSemaphore.release();
		}
	}

	/**
	 * Invokes actions to be done when request fails or response has no meaningful payload.
	 * 
	 * @param request
	 *            failed request
	 */
	protected void requestFailed(WsRequest<RQ> request) {
	}

	/**
	 * Performs actions on response item before parsing.
	 * 
	 * @param rItem
	 *            parsed response item
	 */
	protected void requestParsingStarted(WsResponse<RQ, RS> rItem) {
		String reqName = rItem.getOriginalRequest().fqn();
		if (reqName != null) {
			synchronized (parsedRequests) {
				parsedRequests.add(reqName);
			}
		}
	}

	/**
	 * Performs actions on consumed response item.
	 * 
	 * @param rItem
	 *            consumed response item
	 */
	protected void responseConsumed(WsResponse<RQ, RS> rItem) {
		String qName = rItem.getOriginalRequest().fqn();
		if (qName != null) {
			synchronized (parsedRequests) {
				parsedRequests.remove(qName);
			}
		}
	}

	/**
	 * Checks if request has any response currently processed or pending on input buffer.
	 * 
	 * @param req
	 *            request instance to check
	 * @return {@code true} if request has any response currently processed or pending on input buffer, {@code false} -
	 *         otherwise
	 */
	@SuppressWarnings("unchecked")
	protected boolean isRequestOngoing(WsRequest<RQ> req) {
		String reqName = req.fqn();

		synchronized (parsedRequests) {
			for (String pqName : parsedRequests) {
				if (reqName.equals(pqName)) {
					logger().log(OpLevel.WARNING, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
							"AbstractWsStream.response.in.progress", reqName);
					return true;
				}
			}
		}

		for (Object item : inputBuffer) {
			if (item instanceof WsResponse) {
				WsResponse<RQ, RS> respItem = (WsResponse<RQ, RS>) item;

				if (reqName.equals(respItem.getOriginalRequest().fqn())) {
					logger().log(OpLevel.WARNING, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
							"AbstractWsStream.response.pending", reqName);
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Returns recurrent response for currently parsed response.
	 * 
	 * @param cItem
	 *            currently parsed response
	 * @param buffer
	 *            input buffer instance
	 * @return recurrent response for currently processed response, or {@code null} if no recurring responses available
	 *         in input buffer
	 */
	@SuppressWarnings("unchecked")
	protected WsResponse<RQ, RS> getRecurrentResponse(WsResponse<RQ, RS> cItem, Queue<?> buffer) {
		for (Object item : buffer) {
			if (item instanceof WsResponse) {
				WsResponse<RQ, RS> respItem = (WsResponse<RQ, RS>) item;

				if (respItem.getOriginalRequest().fqn().equals(cItem.getOriginalRequest().fqn())) {
					return respItem;
				}
			}
		}

		return null;
	}

	/**
	 * Closes response instance.
	 * 
	 * @param resp
	 *            response to close
	 */
	protected void closeResponse(RS resp) {
	}

	/**
	 * Checks if stream is configured to drop recurring requests and if request is recurring: currently processed by
	 * parser or pending in input buffer.
	 * 
	 * @param req
	 *            request to check
	 * @return {@code true} is stream shall drop recurring requests and request is processed or pending in input buffer,
	 *         {@code false} - otherwise
	 * 
	 * @see #isRequestOngoing(com.jkoolcloud.tnt4j.streams.scenario.WsRequest)
	 */
	protected boolean isDropRecurring(WsRequest<RQ> req) {
		return dropRecurrentRequests && isRequestOngoing(req);
	}

	/**
	 * Base scheduler job class to be executing implementing stream calls.
	 */
	protected static abstract class CallJob implements Job {
		@Override
		public void execute(JobExecutionContext context) throws JobExecutionException {
			JobDataMap dataMap = context.getJobDetail().getJobDataMap();
			AbstractWsStream<?, ?> stream = (AbstractWsStream<?, ?>) dataMap.get(JOB_PROP_STREAM_KEY);

			if (stream.isShotDown()) {
				return;
			}

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

	/**
	 * Request entity data fill-in context.
	 */
	protected static class DataFillContext extends HashMap<String, Object> {
		private static final long serialVersionUID = 925353711026880141L;

		/**
		 * Constant for context key prefix {@value}.
		 */
		public static final String KEY_PREFIX = "REQ_DATA_CTX_"; // NON-NLS

		private static final String DATA = KEY_PREFIX + "DATA"; // NON-NLS
		private static final String FORMAT = KEY_PREFIX + "FORMAT"; // NON-NLS
		private static final String TYPE = KEY_PREFIX + "TYPE"; // NON-NLS
		private static final String REQUEST = KEY_PREFIX + "REQUEST"; // NON-NLS
		private static final String TIMEZONE = KEY_PREFIX + "TIMEZONE"; // NON-NLS

		/**
		 * Constructs a new instance of DataFillContext.
		 * 
		 * @param reqData
		 *            request entity data string
		 */
		public DataFillContext(String reqData) {
			super();

			put(DATA, reqData);
		}

		/**
		 * Sets request entity data string.
		 * 
		 * @param data
		 *            request entity data string
		 * @return instance of this context
		 */
		public DataFillContext setData(String data) {
			put(DATA, data);

			return this;
		}

		/**
		 * Returns request entity data string.
		 * 
		 * @return request entity data string
		 */
		public String getData() {
			return (String) get(DATA);
		}

		/**
		 * Sets value format.
		 * 
		 * @param format
		 *            value format
		 * @return instance of this context
		 */
		public DataFillContext setFormat(String format) {
			put(FORMAT, format);

			return this;
		}

		/**
		 * Returns value format.
		 * 
		 * @return value format
		 */
		public String getFormat() {
			return (String) get(FORMAT);
		}

		/**
		 * Sets date-time value format timezone.
		 * 
		 * @param tz
		 *            timezone identifier string
		 * @return instance of this context
		 */
		public DataFillContext setTimeZone(String tz) {
			put(TIMEZONE, tz);

			return this;
		}

		/**
		 * Returns date-time value format timezone.
		 * 
		 * @return date-time value format timezone
		 */
		public String getTimeZone() {
			return (String) get(TIMEZONE);
		}

		/**
		 * Sets value type.
		 * 
		 * @param type
		 *            value type
		 * @return instance of this context
		 */
		public DataFillContext setType(String type) {
			put(TYPE, type);

			return this;
		}

		/**
		 * Returns value type.
		 * 
		 * @return value type
		 */
		public String getType() {
			return (String) get(TYPE);
		}

		/**
		 * Removes context additional properties: format, timezone and type.
		 * 
		 * @return instance of this context
		 */
		public DataFillContext reset() {
			remove(FORMAT);
			remove(TIMEZONE);
			remove(TYPE);

			return this;
		}

		/**
		 * Sets request instance bound to this context.
		 * 
		 * @param request
		 *            request instance to bind
		 * @return instance of this context
		 */
		public DataFillContext setRequest(WsRequest<String> request) {
			put(REQUEST, request);

			return this;
		}

		/**
		 * Returns this context bound request instance.
		 * 
		 * @return bound request instance
		 */
		@SuppressWarnings("unchecked")
		public WsRequest<String> getRequest() {
			return (WsRequest<String>) get(REQUEST);
		}

		/**
		 * Returns request parameter value.
		 * 
		 * @param pKey
		 *            request parameter key
		 * @return request parameter value, or {@code null} if request has no such property
		 */
		public Object getReqParameter(String pKey) {
			WsRequest<?> request = getRequest();

			return request == null ? null : request.getParameterValue(pKey);
		}

		/**
		 * Returns boolean context property value.
		 * 
		 * @param key
		 *            context property key
		 * @param defValue
		 *            default property value
		 * @return resolved context property value or default value if context has no defined property
		 */
		public boolean getBoolean(String key, boolean defValue) {
			Boolean val = (Boolean) get(key);

			return val == null ? defValue : val;
		}
	}

	/**
	 * Request fill-in context.
	 */
	protected static class RequestFillContext extends HashMap<String, Object> {
		private static final long serialVersionUID = 181407282124290094L;

		/**
		 * Constant for context key prefix {@value}.
		 */
		public static final String KEY_PREFIX = "REQ_CTX_"; // NON-NLS

		private static final String FILL_PARAMS = KEY_PREFIX + "FILL_PARAMS"; // NON-NLS

		/**
		 * Constructs a new instance of RequestFillContext.
		 * 
		 * @param fillParams
		 *            flag indicating whether to fill-in request parameters
		 */
		public RequestFillContext(boolean fillParams) {
			super();

			put(FILL_PARAMS, fillParams);
		}

		/**
		 * Returns flag indicating whether to fill-in request parameters.
		 * 
		 * @return {@code true} if request parameters values shall be filled-in, {@code false} - otherwise
		 */
		public boolean isFillingParams() {
			Boolean fp = (Boolean) get(FILL_PARAMS);

			return fp == null ? true : fp;
		}
	}
}
