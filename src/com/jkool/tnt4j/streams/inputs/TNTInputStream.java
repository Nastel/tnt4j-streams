/*
 * Copyright (c) 2015 jKool, LLC. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * jKool, LLC. ("Confidential Information").  You shall not disclose
 * such Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with jKool, LLC.
 *
 * JKOOL MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. JKOOL SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 *
 * CopyrightVersion 1.0
 *
 */

package com.jkool.tnt4j.streams.inputs;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.ArrayUtils;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.fields.ActivityInfo;
import com.jkool.tnt4j.streams.parsers.ActivityParser;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.jkool.tnt4j.streams.utils.StreamsThread;
import com.jkool.tnt4j.streams.utils.Utils;
import com.nastel.jkool.tnt4j.TrackingLogger;
import com.nastel.jkool.tnt4j.config.DefaultConfigFactory;
import com.nastel.jkool.tnt4j.config.TrackerConfig;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.EventSink;
import com.nastel.jkool.tnt4j.source.Source;
import com.nastel.jkool.tnt4j.tracker.Tracker;

/**
 * <p>
 * Base class that all activity streams must extend. It provides some base
 * functionality useful for all activity streams.
 * </p>
 * <p>
 * All activity streams should support the following properties:
 * <ul>
 * <li>DateTime - default date/time to associate with activities</li>
 * <li>HaltIfNoParser - if set to true, stream will halt if none of the parsers
 * can parse activity event RAW data. If set to false - puts log entry and
 * continues.</li>
 * </ul>
 *
 * @version $Revision: 8 $
 */
public abstract class TNTInputStream implements Runnable {
	/**
	 * Stream logger.
	 */
	protected final EventSink logger;

	/**
	 * StreamThread running this stream.
	 */
	protected StreamThread ownerThread = null;

	/**
	 * Map of parsers being used by stream.
	 */
	protected final Map<String, List<ActivityParser>> parsersMap = new LinkedHashMap<String, List<ActivityParser>>();

	/**
	 * Used to deliver processed activity data to destination.
	 */
	protected Map<String, Tracker> trackersMap = new HashMap<String, Tracker>();

	private TrackerConfig streamConfig;
	private Source defaultSource;

	private boolean haltIfNoParser = true;

	/**
	 * Delay between retries to submit data package to jKool Cloud Service if
	 * some transmission failure occurs, in milliseconds.
	 */
	protected static final long CONN_RETRY_INTERVAL = TimeUnit.SECONDS.toMillis(15);

	/**
	 * Initializes TNTInputStream.
	 *
	 * @param logger
	 *            debug logger used by activity stream
	 */
	protected TNTInputStream(EventSink logger) {
		this.logger = logger;
	}

	/**
	 * Get the thread owning this stream.
	 *
	 * @return owner thread
	 */
	public StreamThread getOwnerThread() {
		return ownerThread;
	}

	/**
	 * Set the thread owning this stream.
	 *
	 * @param ownerThread
	 *            thread owning this stream
	 */
	public void setOwnerThread(StreamThread ownerThread) {
		this.ownerThread = ownerThread;
	}

	/**
	 * Set properties for activity stream. This method is invoked by the
	 * configuration loader in response to the {@code property} configuration
	 * elements. It is invoked once per stream definition, with all property
	 * names and values specified for this stream. Subclasses should generally
	 * override this method to process custom properties, and invoke the base
	 * class method to handle any built-in properties.
	 *
	 * @param props
	 *            properties to set
	 *
	 * @throws Throwable
	 *             indicates error with properties
	 */
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Throwable {
		if (props == null) {
			return;
		}

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();
			if (StreamsConfig.PROP_HALT_ON_PARSER.equalsIgnoreCase(name)) {
				haltIfNoParser = Boolean.parseBoolean(value);
			}
		}
	}

	/**
	 * Get value of specified property. If subclasses override
	 * {@link #setProperties(Collection)}, they should generally override this
	 * method as well to return the value of custom properties, and invoke the
	 * base class method to handle any built-in properties.
	 * <p>
	 * The default implementation handles the
	 * {@value com.jkool.tnt4j.streams.configure.StreamsConfig#PROP_DATETIME}
	 * property and returns the value of the {@link #getDate()} method.
	 *
	 * @param name
	 *            name of property whose value is to be retrieved
	 *
	 * @return value for property, or {@code null} if property does not exist
	 */
	public Object getProperty(String name) {
		if (StreamsConfig.PROP_DATETIME.equals(name)) {
			return getDate();
		}
		if (StreamsConfig.PROP_HALT_ON_PARSER.equals(name)) {
			return haltIfNoParser;
		}

		return null;
	}

	/**
	 * Initialize the stream.
	 * <p>
	 * This method is called by default {@code run} method to perform any
	 * necessary initializations before the stream starts processing, including
	 * verifying that all required properties are set. If subclasses override
	 * this method to perform any custom initializations, they must call the
	 * base class method. If subclass also overrides the {@code run} method, it
	 * must call this at start of {@code run} method before entering into
	 * processing loop.
	 *
	 * @throws Throwable
	 *             indicates that stream is not configured properly and cannot
	 *             continue.
	 */
	protected void initialize() throws Throwable {
		streamConfig = DefaultConfigFactory.getInstance().getConfig("com.jkool.tnt4j.streams"); // NON-NLS
		Tracker tracker = TrackingLogger.getInstance(streamConfig.build());
		defaultSource = streamConfig.getSource();
		trackersMap.put(defaultSource.getFQName(), tracker);
		logger.log(OpLevel.DEBUG,
				StreamsResources.getStringFormatted("TNTInputStream.default.tracker", defaultSource.getFQName()));
	}

	/**
	 * Adds the specified parser to the list of parsers being used by this
	 * stream.
	 *
	 * @param parser
	 *            parser to add
	 */
	public void addParser(ActivityParser parser) {
		String[] tags = parser.getTags();

		if (ArrayUtils.isNotEmpty(tags)) {
			for (String tag : tags) {
				addTaggedParser(tag, parser);
			}
		} else {
			addTaggedParser(parser.getName(), parser);
		}
	}

	private void addTaggedParser(String tag, ActivityParser parser) {
		List<ActivityParser> tpl = parsersMap.get(tag);

		if (tpl == null) {
			tpl = new ArrayList<ActivityParser>();
			parsersMap.put(tag, tpl);
		}

		tpl.add(parser);
	}

	/**
	 * Get the position in the source activity data currently being processed.
	 * For line-based data sources, this is generally the line number.
	 * Subclasses should override this to provide meaningful information, if
	 * relevant. The default implementation just returns 0.
	 *
	 * @return current position in activity data source being processed
	 */
	public int getActivityPosition() {
		return 0;
	}

	/**
	 * Get the next activity data item to be processed. All subclasses must
	 * implement this.
	 *
	 * @return next activity data item, or {@code null} if there is no next item
	 *
	 * @throws Throwable
	 *             if any errors occurred getting next item
	 */
	public abstract Object getNextItem() throws Throwable;

	/**
	 * Gets the next processed activity.
	 * <p>
	 * Default implementation simply calls {@link #getNextItem()} to get next
	 * activity data item and calls {@link #applyParsers(Object)} to process it.
	 *
	 * @return next activity item
	 *
	 * @throws Throwable
	 *             if error getting next activity data item or processing it
	 */
	protected ActivityInfo getNextActivity() throws Throwable {
		ActivityInfo ai = null;
		Object data = getNextItem();
		try {
			if (data == null) {
				halt(); // no more data items to process
			} else {
				ai = applyParsers(data);
			}
		} catch (ParseException e) {
			int position = getActivityPosition();
			ParseException pe = new ParseException(
					StreamsResources.getStringFormatted("TNTInputStream.failed.to.process", position), position);
			pe.initCause(e);
			throw pe;
		}
		return ai;
	}

	/**
	 * Applies all defined parsers for this stream that support the format that
	 * the raw activity data is in the order added until one successfully
	 * matches the specified activity data item.
	 *
	 * @param data
	 *            activity data item to process
	 *
	 * @return processed activity data item, or {@code null} if activity data
	 *         item does not match rules for any parsers
	 *
	 * @throws IllegalStateException
	 *             if parser fails to run
	 * @throws ParseException
	 *             if any parser encounters an error parsing the activity data
	 */
	protected ActivityInfo applyParsers(Object data) throws IllegalStateException, ParseException {
		return applyParsers(null, data);
	}

	/**
	 * Applies all defined parsers for this stream that support the format that
	 * the raw activity data is in the order added until one successfully
	 * matches the specified activity data item.
	 *
	 * @param tags
	 *            array of tag strings to map activity data with parsers. Can be
	 *            {@code null}.
	 * @param data
	 *            activity data item to process
	 *
	 * @return processed activity data item, or {@code null} if activity data
	 *         item does not match rules for any parsers
	 *
	 * @throws IllegalStateException
	 *             if parser fails to run
	 * @throws ParseException
	 *             if any parser encounters an error parsing the activity data
	 */
	protected ActivityInfo applyParsers(String[] tags, Object data) throws IllegalStateException, ParseException {
		if (data == null) {
			return null;
		}

		Set<ActivityParser> parsers = getParsersFor(tags);
		for (ActivityParser parser : parsers) {
			if (parser.isDataClassSupported(data)) {
				ActivityInfo ai = parser.parse(this, data);
				if (ai != null) {
					return ai;
				}
			}
		}
		return null;
	}

	private Set<ActivityParser> getParsersFor(String[] tags) {
		Set<ActivityParser> parsersSet = new LinkedHashSet<ActivityParser>();

		if (ArrayUtils.isNotEmpty(tags)) {
			for (String tag : tags) {
				List<ActivityParser> tpl = parsersMap.get(tag);

				if (tpl != null) {
					parsersSet.addAll(tpl);
				}
			}
		}

		if (parsersSet.isEmpty()) {
			Collection<List<ActivityParser>> allParsers = parsersMap.values();

			for (List<ActivityParser> tpl : allParsers) {
				if (tpl != null) {
					parsersSet.addAll(tpl);
				}
			}
		}

		return parsersSet;
	}

	/**
	 * Gets the default date/time to use for activity entries that do not
	 * contain a date. Default implementation returns the current date.
	 *
	 * @return default date/time to use for activity entries
	 */
	public Date getDate() {
		return new Date();
	}

	/**
	 * Signals that this stream should stop processing so that controlling
	 * thread will terminate.
	 */
	public void halt() {
		ownerThread.halt();
	}

	/**
	 * Indicates whether this stream has stopped.
	 *
	 * @return {@code true} if stream has stopped processing, {@code false}
	 *         otherwise
	 */
	public boolean isHalted() {
		return ownerThread.isStopRunning();
	}

	/**
	 * Cleanup the stream.
	 * <p>
	 * This method is called by default {@code run} method to perform any
	 * necessary cleanup before the stream stops processing, releasing any
	 * resources created by {@link #initialize()} method. If subclasses override
	 * this method to perform any custom cleanup, they must call the base class
	 * method. If subclass also overrides the {@code run} method, it must call
	 * this at end of {@code run} method before returning.
	 */
	protected void cleanup() {
		if (!trackersMap.isEmpty()) {
			for (Map.Entry<String, Tracker> te : trackersMap.entrySet()) {
				Tracker tracker = te.getValue();
				Utils.close(tracker);
			}

			trackersMap.clear();
		}
	}

	/**
	 * Starts input stream processing. Implementing {@code Runnable} interface
	 * makes it possible to process each stream in separate thread.
	 *
	 * @see Runnable#run()
	 */
	@Override
	public void run() {
		logger.log(OpLevel.INFO, StreamsResources.getString("TNTInputStream.starting"));
		if (ownerThread == null) {
			throw new IllegalStateException(StreamsResources.getString("TNTInputStream.no.owner.thread"));
		}
		try {
			initialize();
			while (!isHalted()) {
				try {
					ActivityInfo ai = getNextActivity();
					if (ai == null) { // TODO: better distinguishing between
										// data stream end and unparseable
										// activity data.
						if (isHalted()) {
							logger.log(OpLevel.INFO, StreamsResources.getString("TNTInputStream.data.stream.ended"));
						} else {
							logger.log(OpLevel.INFO, StreamsResources.getString("TNTInputStream.no.parser"));
							if (haltIfNoParser) {
								halt();
							}
						}
					} else {
						if (!ai.isFiltered()) {
							Source aiSource = ai.getSource();

							Tracker tracker = trackersMap
									.get(aiSource == null ? defaultSource.getFQName() : aiSource.getFQName());
							if (tracker == null) {
								aiSource.setSSN(defaultSource.getSSN());
								streamConfig.setSource(aiSource);
								tracker = TrackingLogger.getInstance(streamConfig.build());
								trackersMap.put(aiSource.getFQName(), tracker);
								logger.log(OpLevel.DEBUG, StreamsResources
										.getStringFormatted("TNTInputStream.build.new.tracker", aiSource.getFQName()));
							}

							while (!isHalted() && !tracker.isOpen()) {
								try {
									tracker.open();
								} catch (IOException ioe) {
									logger.log(OpLevel.ERROR, StreamsResources
											.getStringFormatted("TNTInputStream.failed.to.connect", tracker), ioe);
									Utils.close(tracker);
									logger.log(OpLevel.INFO,
											StreamsResources.getStringFormatted("TNTInputStream.will.retry",
													TimeUnit.MILLISECONDS.toSeconds(CONN_RETRY_INTERVAL)));
									if (!isHalted()) {
										StreamsThread.sleep(CONN_RETRY_INTERVAL);
									}
								}
							}

							ai.recordActivity(tracker, CONN_RETRY_INTERVAL);
						}
					}
				} catch (IllegalStateException ise) {
					logger.log(OpLevel.ERROR, StreamsResources.getStringFormatted(
							"TNTInputStream.failed.record.activity.at", getActivityPosition(), ise.getMessage()), ise);
					halt();
				} catch (Exception e) {
					logger.log(OpLevel.ERROR, StreamsResources.getStringFormatted(
							"TNTInputStream.failed.record.activity.at", getActivityPosition(), e.getMessage()), e);
				}
			}
		} catch (Throwable t) {
			logger.log(OpLevel.ERROR,
					StreamsResources.getStringFormatted("TNTInputStream.failed.record.activity", t.getMessage()), t);
		} finally {
			cleanup();
			logger.log(OpLevel.INFO, StreamsResources.getStringFormatted("TNTInputStream.thread.ended",
					Thread.currentThread().getName()));
		}
	}
}
