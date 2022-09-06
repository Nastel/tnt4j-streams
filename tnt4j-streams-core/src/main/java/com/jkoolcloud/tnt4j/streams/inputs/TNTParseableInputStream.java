/*
 * Copyright 2014-2022 JKOOL, LLC.
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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.core.OpType;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityField;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.fields.StreamFieldType;
import com.jkoolcloud.tnt4j.streams.outputs.JKCloudActivityOutput;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.streams.parsers.data.ActivityData;
import com.jkoolcloud.tnt4j.streams.parsers.data.CommonActivityData;
import com.jkoolcloud.tnt4j.streams.reference.MatchingParserReference;
import com.jkoolcloud.tnt4j.streams.reference.ParserReference;
import com.jkoolcloud.tnt4j.streams.utils.StreamsCache;
import com.jkoolcloud.tnt4j.streams.utils.StreamsConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Base class that all activity streams performing RAW activity data parsing must extend. It maps RAW activities data to
 * related parsers and controls generic parsing process.
 * <p>
 * This activity stream supports the following configuration properties (in addition to those supported by
 * {@link TNTInputStream}):
 * <ul>
 * <li>HaltIfNoParser - if set to {@code true}, stream will halt if none of the parsers can parse activity object RAW
 * data. If set to {@code false} - puts log entry and continues. Default value - {@code false}. (Optional)</li>
 * <li>GroupingActivityName - name of ACTIVITY entity used to group excel workbook streamed events. (Optional)</li>
 * </ul>
 *
 * @param <T>
 *            the type of handled RAW activity data
 *
 * @version $Revision: 1 $
 */
public abstract class TNTParseableInputStream<T> extends TNTInputStream<T, ActivityInfo> {

	/**
	 * Set of parsers being used by stream.
	 */
	protected final Set<ParserReference> parsersSet = new LinkedHashSet<>();

	private boolean haltIfNoParser = false;
	private String groupingActivityName = null;

	@Override
	protected void setDefaultStreamOutput() {
		setOutput(new JKCloudActivityOutput("DefaultParseableInputStreamOutput")); // NON-NLS
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (StreamProperties.PROP_HALT_ON_PARSER.equalsIgnoreCase(name)) {
			haltIfNoParser = Utils.toBoolean(value);
		} else if (StreamProperties.PROP_GROUPING_ACTIVITY_NAME.equalsIgnoreCase(name)) {
			groupingActivityName = value;
		}
	}

	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_HALT_ON_PARSER.equalsIgnoreCase(name)) {
			return haltIfNoParser;
		}
		if (StreamProperties.PROP_GROUPING_ACTIVITY_NAME.equalsIgnoreCase(name)) {
			return groupingActivityName;
		}

		return super.getProperty(name);
	}

	@Override
	protected void initialize() throws Exception {
		super.initialize();

		if (parsersSet.isEmpty()) {
			throw new IllegalStateException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"TNTInputStream.has.no.parsers.bound", getName()));
		}

		if (StringUtils.isNotEmpty(groupingActivityName)) {
			ActivityInfo gai = new ActivityInfo(true);
			gai.setFieldValue(new ActivityField(StreamFieldType.EventType.name()), OpType.ACTIVITY.name());
			gai.setFieldValue(new ActivityField(StreamFieldType.EventName.name()), groupingActivityName);
			output().logItem(gai);

			StreamsCache.addValue(getName() + StreamsConstants.STREAM_GROUPING_ACTIVITY_ID_CACHE_KEY,
					gai.getTrackingId(), true);
		}
	}

	@Override
	public void addReference(Object refObject) throws IllegalStateException {
		if (refObject instanceof ActivityParser) {
			ActivityParser ap = (ActivityParser) refObject;
			addParser(ap);
		} else if (refObject instanceof ParserReference) {
			ParserReference apr = (ParserReference) refObject;
			addParser(apr);
		}

		super.addReference(refObject);
	}

	/**
	 * Adds the specified parser to the list of parsers being used by this stream.
	 *
	 * @param parser
	 *            parser to add
	 * @throws IllegalStateException
	 *             if parser can't be added to stream
	 *
	 * @see #addParser(com.jkoolcloud.tnt4j.streams.reference.ParserReference)
	 */
	public void addParser(ActivityParser parser) throws IllegalStateException {
		if (parser == null) {
			return;
		}

		ParserReference parserRef = new ParserReference(parser);
		addParser(parserRef);
	}

	/**
	 * Adds the specified parser reference to the list of parsers being used by this stream.
	 *
	 * @param parserRef
	 *            parser reference to add
	 * @throws IllegalStateException
	 *             if parser can't be added to stream
	 */
	protected void addParser(ParserReference parserRef) throws IllegalStateException {
		if (parserRef == null) {
			return;
		}

		parsersSet.add(parserRef);
	}

	/**
	 * Adds specified parsers array to the list of parsers being used by this stream.
	 *
	 * @param parsers
	 *            array of parsers to add
	 * @throws IllegalStateException
	 *             if parser can't be added to stream
	 */
	public void addParser(ActivityParser... parsers) throws IllegalStateException {
		if (parsers == null) {
			return;
		}

		addParsers(Arrays.asList(parsers));
	}

	/**
	 * Adds specified parsers collection to the list of parsers being used by this stream.
	 *
	 * @param parsers
	 *            collection of parsers to add
	 * @throws IllegalArgumentException
	 *             if parser can't be added to stream
	 */
	public void addParsers(Iterable<ActivityParser> parsers) throws IllegalArgumentException {
		if (parsers == null) {
			return;
		}

		for (ActivityParser parser : parsers) {
			addParser(parser);
		}
	}

	/**
	 * Applies all defined parsers for this stream that support the format that the raw activity data is in the order
	 * added until one successfully matches the specified activity data item.
	 *
	 * @param data
	 *            activity data item to process
	 * @return processed activity data item, or {@code null} if activity data item does not match rules for any parsers
	 * @throws IllegalStateException
	 *             if parser fails to run
	 * @throws ParseException
	 *             if any parser encounters an error parsing the activity data
	 *
	 * @see #getDataTags(Object)
	 * @see #applyParsers(Object, String...)
	 */
	protected ActivityInfo applyParsers(Object data) throws IllegalStateException, ParseException {
		return applyParsers(data, getDataTags(data));
	}

	/**
	 * Applies all defined parsers for this stream that support the format that the raw activity data is in the order
	 * added until one successfully matches the specified activity data item.
	 *
	 * @param data
	 *            activity data item to process
	 * @param tags
	 *            array of tag strings to map activity data with parsers. Can be {@code null}.
	 * @return processed activity data item, or {@code null} if activity data item does not match rules for any parsers
	 * @throws IllegalStateException
	 *             if parser fails to run
	 * @throws ParseException
	 *             if any parser encounters an error parsing the activity data
	 */
	protected ActivityInfo applyParsers(Object data, String... tags) throws IllegalStateException, ParseException {
		if (data == null) {
			return null;
		}

		ActivityData<?> pData;
		if (data instanceof ActivityData) {
			pData = (ActivityData<?>) data;
		} else {
			pData = new CommonActivityData<>(data);
		}

		for (ParserReference pRef : parsersSet) {
			boolean dataMatch = pRef.getParser().isDataClassSupported(pData.getData());
			Boolean tagsMatch = null;
			Boolean expMatch = null;

			boolean parserMatch = dataMatch;

			if (parserMatch) {
				tagsMatch = pRef.matchTags(tags);
				parserMatch = BooleanUtils.toBooleanDefaultIfNull(tagsMatch, true);

				if (parserMatch && pRef instanceof MatchingParserReference) {
					expMatch = ((MatchingParserReference) pRef).matchExp(this, pData.getData());
					parserMatch = BooleanUtils.toBooleanDefaultIfNull(expMatch, true);
				}
			}

			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"TNTInputStream.parser.match", getName(), pRef, dataMatch, tagsMatch == null ? "----" : tagsMatch, // NON-NLS
					expMatch == null ? "----" : expMatch); // NON-NLS
			if (parserMatch) {
				ActivityInfo ai = pRef.getParser().parse(this, data);
				if (ai != null) {
					return ai;
				}
			}
		}

		return null;
	}

	/**
	 * Resolves RAW activity data tag strings array to be used for activity data and parsers mapping.
	 *
	 * @param data
	 *            activity data item to get tags
	 * @return array of activity data found tag strings
	 *
	 * @see #applyParsers(Object, String...)
	 */
	public String[] getDataTags(Object data) {
		return null;
	}

	/**
	 * Makes activity information {@link ActivityInfo} object from raw activity data item.
	 * <p>
	 * Default implementation simply calls {@link #applyParsers(Object)} to process raw activity data item.
	 *
	 * @param data
	 *            raw activity data item.
	 * @return activity information object
	 * @throws Exception
	 *             if exception occurs while parsing raw activity data item
	 */
	protected ActivityInfo makeActivityInfo(T data) throws Exception {
		ActivityInfo ai = null;
		if (data != null) {
			try {
				ai = applyParsers(data);
			} catch (ParseException exc) {
				int position = getActivityPosition();
				ParseException pe = new ParseException(StreamsResources.getStringFormatted(
						StreamsResources.RESOURCE_BUNDLE_NAME, "TNTInputStream.failed.to.process", position), position);
				pe.initCause(exc);
				throw pe;
			}
		}
		return ai;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Performs parsing of raw activity data to {@link ActivityInfo} data package, which can be transformed to
	 * {@link com.jkoolcloud.tnt4j.core.Trackable} object and sent to jKoolCloud using TNT4J and JESL APIs.
	 */
	@Override
	protected void processActivityItem(T item, AtomicBoolean failureFlag) throws Exception {
		notifyProgressUpdate(incrementCurrentActivitiesCount(), getTotalActivities());

		ActivityInfo ai = makeActivityInfo(item);
		if (ai == null) {
			logger().log(OpLevel.WARNING, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"TNTInputStream.no.parser", item);
			incrementSkippedActivitiesCount();
			if (haltIfNoParser) {
				failureFlag.set(true);
				notifyFailed(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"TNTInputStream.no.parser", item), null, null);
				halt(false);
			} else {
				notifyStreamEvent(OpLevel.WARNING, StreamsResources.getStringFormatted(
						StreamsResources.RESOURCE_BUNDLE_NAME, "TNTInputStream.could.not.parse.activity", item), item);
			}
		} else {
			if (ai.isDeliverable()) {
				getOutput().logItem(ai);
			} else {
				incrementFilteredActivitiesCount();
				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"TNTInputStream.activity.filtered.out", ai);
			}
		}
	}
}
