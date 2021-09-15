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

import static com.jkoolcloud.tnt4j.streams.TestUtils.testPropertyList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.text.ParseException;
import java.util.*;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.TestUtils;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityField;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.streams.parsers.data.ActivityData;
import com.jkoolcloud.tnt4j.streams.reference.ParserReference;
import com.jkoolcloud.tnt4j.streams.utils.CustomProperties;
import com.jkoolcloud.tnt4j.tracker.Tracker;

/**
 * @author akausinis
 * @version 1.0
 */
public class TNTInputStreamTest {
	EventSink LOGGER = mock(EventSink.class);
	private ActivityParser parser;
	private TestStream ts;
	private StreamThread streamThread;
	public ActivityInfo ai;

	@Before
	public void initTest() {
		parser = new TestActivityParser();
		ai = mock(ActivityInfo.class);
		ts = new TestStream();
		streamThread = new StreamThread(ts);
	}

	@Test
	@Ignore("Mockito failure")
	public void streamFlowTest() throws Exception {
		ts.addParser(parser);
		streamThread.start();
		when(ai.isFilteredOut()).thenReturn(false);

		Thread.sleep(500);
		verify(ai).buildTrackable(any(Tracker.class));
		ts.halt(true);
		ts.cleanup();
	}

	@Test
	public void setPropertiesTest() {
		Map<String, String> props = new HashMap<>(8);
		props.put(StreamProperties.PROP_HALT_ON_PARSER, String.valueOf(true));
		props.put(StreamProperties.PROP_EXECUTOR_THREADS_QTY, String.valueOf(5));
		props.put(StreamProperties.PROP_USE_EXECUTOR_SERVICE, String.valueOf(true));
		props.put(StreamProperties.PROP_DATETIME, new Date().toString());
		props.put(StreamProperties.PROP_EXECUTOR_REJECTED_TASK_OFFER_TIMEOUT, String.valueOf(500));
		props.put(StreamProperties.PROP_EXECUTORS_TERMINATION_TIMEOUT, String.valueOf(500));
		props.put(StreamProperties.PROP_EXECUTORS_BOUNDED, String.valueOf(true));
		ts.setProperties(props.entrySet());
		testPropertyList(ts, props.entrySet());
	}

	@Test
	public void setPropertiesIfNullTest() {
		TNTInputStream<?, ?> myStream = new TestUtils.SimpleTestStream();
		myStream.setProperties(null);
		assertNull(myStream.getProperty("PROP_EXECUTORS_BOUNDED")); // NON-NLS
	}

	@Test
	public void getBoundedExecutorServiceTest() throws Exception {
		Map<String, String> props = new HashMap<>(8);
		props.put(StreamProperties.PROP_HALT_ON_PARSER, String.valueOf(true));
		props.put(StreamProperties.PROP_EXECUTOR_THREADS_QTY, String.valueOf(5));
		props.put(StreamProperties.PROP_USE_EXECUTOR_SERVICE, String.valueOf(true));
		props.put(StreamProperties.PROP_DATETIME, new Date().toString());
		props.put(StreamProperties.PROP_EXECUTOR_REJECTED_TASK_OFFER_TIMEOUT, String.valueOf(500));
		props.put(StreamProperties.PROP_EXECUTORS_TERMINATION_TIMEOUT, String.valueOf(500));
		props.put(StreamProperties.PROP_EXECUTORS_BOUNDED, String.valueOf(true));
		ts.setProperties(props.entrySet());
		ts.startStream();
	}

	@Test
	public void getDefaultExecutorServiceTest() throws Exception {
		Map<String, String> props = new HashMap<>(8);
		props.put(StreamProperties.PROP_HALT_ON_PARSER, String.valueOf(true));
		props.put(StreamProperties.PROP_EXECUTOR_THREADS_QTY, String.valueOf(5));
		props.put(StreamProperties.PROP_USE_EXECUTOR_SERVICE, String.valueOf(true));
		props.put(StreamProperties.PROP_DATETIME, new Date().toString());
		props.put(StreamProperties.PROP_EXECUTOR_REJECTED_TASK_OFFER_TIMEOUT, String.valueOf(500));
		props.put(StreamProperties.PROP_EXECUTORS_TERMINATION_TIMEOUT, String.valueOf(500));
		props.put(StreamProperties.PROP_EXECUTORS_BOUNDED, String.valueOf(false));
		ts.setProperties(props.entrySet());
		ts.startStream();
		ts.halt(true);
	}

	@Test(expected = IllegalStateException.class)
	public void runTest() {
		ts.setOwnerThread(null);
		ts.run();
	}

	@Test
	public void testMakeActivityInfo() throws Exception {

		assertNull(ts.makeActivityInfo("TEST")); // NON-NLS
	}

	@Test(expected = ParseException.class)
	public void testMakeActivityInfoFails() throws Exception {
		ts.addParser(parser);
		assertNotNull(ts.makeActivityInfo("TESTPARSEEXCEPTION")); // NON-NLS
	}

	//
	@Test
	public void testApplyParsers() throws Exception {
		ActivityParser parser = mock(ActivityParser.class);
		String[] tags = { "TestTag" }; // NON-NLS
		String[] differentTags = { "TestTagNot" }; // NON-NLS
		when(parser.parse(any(TNTInputStream.class), any())).thenReturn(new ActivityInfo());

		assertNull(ts.applyParsers(null));

		ParserReference parserRef = new ParserReference(parser);
		parserRef.setTags(tags);
		ts.addParser(parserRef);

		// Data class nor supported
		assertNull(ts.applyParsers("TEST")); // NON-NLS

		when(parser.isDataClassSupported(any())).thenReturn(true);

		ActivityInfo ai = ts.applyParsers("TEST"); // NON-NLS
		assertNotNull(ai);

		ai = ts.applyParsers("TEST", tags); // NON-NLS
		assertNotNull(ai);

		ai = ts.applyParsers("TEST", differentTags); // NON-NLS
		assertNull(ai);

		verify(parser, times(2)).parse(any(TNTInputStream.class), any());
	}

	@Test
	public void testInputListeners() {
		InputStreamListener inputStreamListenerMock = mock(InputStreamListener.class);
		ts.addStreamListener(inputStreamListenerMock);
		ts.notifyProgressUpdate(1, 10);
		verify(inputStreamListenerMock).onProgressUpdate(ts, 1, 10);
		ts.notifyStreamSuccess();
		verify(inputStreamListenerMock).onSuccess(ts);
		ts.notifyFailed("", new Exception(), "");
		verify(inputStreamListenerMock).onFailure(eq(ts), eq(""), any(Exception.class), eq(""));
		verify(inputStreamListenerMock).onStatusChange(ts, StreamStatus.FAILURE);
		ts.notifyStatusChange(StreamStatus.STARTED);
		verify(inputStreamListenerMock).onStatusChange(ts, StreamStatus.STARTED);
		ts.notifyFinished();
		verify(inputStreamListenerMock).onFinish(eq(ts), any(TNTInputStreamStatistics.class));
		ts.notifyStreamEvent(OpLevel.NOTICE, "", "");
		verify(inputStreamListenerMock).onStreamEvent(ts, OpLevel.NOTICE, "", "");

	}

	@Test
	public void addInputTaskListenerNullTest() {
		StreamTasksListener streamTaskListenerMock = mock(StreamTasksListener.class);
		Runnable runnable = mock(Runnable.class);
		ts.addStreamTasksListener(null);
		ts.notifyStreamTaskRejected(runnable);
		verify(streamTaskListenerMock, never()).onReject(eq(ts), any(Runnable.class));
	}

	@Test
	public void addStreamListenerNullTest() {
		InputStreamListener inputStreamListenerMock = mock(InputStreamListener.class);
		ts.addStreamListener(null);
		ts.notifyProgressUpdate(1, 10);
		verify(inputStreamListenerMock, never()).onProgressUpdate(ts, 1, 10);
	}

	@Test
	public void removeStreamListenerTest() {
		InputStreamListener inputStreamListenerMock = mock(InputStreamListener.class);
		ts.addStreamListener(inputStreamListenerMock);
		ts.removeStreamListener(inputStreamListenerMock);
		ts.notifyProgressUpdate(1, 10);
		verify(inputStreamListenerMock, never()).onProgressUpdate(ts, 1, 10);
	}

	@Test
	public void removeStreamTasksListenerTest() {
		StreamTasksListener streamTaskListenerMock = mock(StreamTasksListener.class);
		Runnable runnable = mock(Runnable.class);
		ts.addStreamTasksListener(streamTaskListenerMock);
		ts.removeStreamTasksListener(streamTaskListenerMock);
		ts.notifyStreamTaskRejected(runnable);
		verify(streamTaskListenerMock, never()).onReject(eq(ts), any(Runnable.class));
	}

	// TODO
	/*
	 * @Test public void streamStatsTest() { assertEquals(ts.getTotalActivities() == -1 ? ts.getCurrentActivity() :
	 * ts.getTotalActivities(), ts.getStreamStatistics().getActivitiesTotal()); assertEquals(ts.getCurrentActivity(),
	 * ts.getStreamStatistics().getCurrActivity()); assertEquals(ts.getTotalBytes(),
	 * ts.getStreamStatistics().getTotalBytes()); assertEquals(ts.getStreamedBytesCount(),
	 * ts.getStreamStatistics().getBytesStreamed()); assertEquals(ts.getElapsedTime() == -1 ? 0 : ts.getElapsedTime(),
	 * ts.getStreamStatistics().getElapsedTime()); assertEquals(ts.getSkippedActivitiesCount(),
	 * ts.getStreamStatistics().getSkippedActivities()); assertEquals(ts.getFilteredActivitiesCount(),
	 * ts.getStreamStatistics().getFilteredActivities()); assertEquals(ts.getLostActivitiesCount(),
	 * ts.getStreamStatistics().getLostActivities()); assertEquals(ts.getAverageActivityRate(),
	 * ts.getStreamStatistics().getAverageActivitiesRate(), 0.01); }
	 */

	@Test
	@Ignore("Mixed")
	public void cleanUpTest() {
		InputStreamListener inputStreamListenerMock = mock(InputStreamListener.class);
		StreamTasksListener streamTaskListenerMock = mock(StreamTasksListener.class);
		Runnable runnable = mock(Runnable.class);
		List<Runnable> list = Collections.singletonList(runnable);
		ts.addStreamListener(inputStreamListenerMock);
		ts.addStreamTasksListener(streamTaskListenerMock);
		ts.cleanup();
		ts.notifyStreamTasksDropOff(list);
		verify(streamTaskListenerMock, never()).onDropOff(ts, list);
		ts.notifyProgressUpdate(1, 10);
		verify(inputStreamListenerMock, never()).onProgressUpdate(ts, 1, 10);

	}

	@Test
	public void getOwnerThreadTest() {
		ts.getOwnerThread().setName("TEST_STREAM"); // NON-NLS
		assertEquals("TEST_STREAM", ts.getOwnerThread().getName());
	}

	@Test
	public void testInputTaskListener() {
		StreamTasksListener streamTaskListenerMock = mock(StreamTasksListener.class);
		Runnable runnable = mock(Runnable.class);
		ts.addStreamTasksListener(streamTaskListenerMock);
		ts.notifyStreamTaskRejected(runnable);
		verify(streamTaskListenerMock).onReject(eq(ts), any(Runnable.class));

		List<Runnable> list = Collections.singletonList(runnable);
		ts.notifyStreamTasksDropOff(list);
		verify(streamTaskListenerMock).onDropOff(ts, list);

	}

	@Test
	public void customPropertiesTest() {
		TNTInputStream<?, ?> myStream = new TestUtils.SimpleTestStream();
		myStream.setProperty(StreamProperties.PROP_EXECUTOR_THREADS_QTY, "6");
		myStream.setProperty(CustomProperties.CONTEXT_PROPERTY_PREFIX + "property1", "value1");
		myStream.setProperty(CustomProperties.CONTEXT_PROPERTY_PREFIX.toUpperCase() + "PROPERTY2", "value2");
		myStream.setProperty(CustomProperties.CONTEXT_PROPERTY_PREFIX + StreamProperties.PROP_EXECUTOR_THREADS_QTY,
				"value3");

		Object pValue = myStream.getProperty(StreamProperties.PROP_EXECUTOR_THREADS_QTY);
		assertEquals(6, pValue);
		pValue = myStream.getProperty("property1");
		assertEquals("value1", pValue);
		pValue = myStream.getProperty("property2");
		assertEquals("value2", pValue);
		pValue = myStream.getProperty(CustomProperties.CONTEXT_PROPERTY_PREFIX + "property1");
		assertEquals("value1", pValue);
		pValue = myStream.getProperty(CustomProperties.CONTEXT_PROPERTY_PREFIX + "property2");
		assertEquals("value2", pValue);
		pValue = myStream
				.getProperty(CustomProperties.CONTEXT_PROPERTY_PREFIX + StreamProperties.PROP_EXECUTOR_THREADS_QTY);
		assertEquals("value3", pValue);

	}

	private class TestStream extends TNTParseableInputStream<String> {
		// BlockingQueue<String> buffer = new ArrayBlockingQueue<String>(5);
		boolean used = false;

		protected TestStream() {
			super();
			// buffer.offer("TEST");

		}

		@Override
		protected EventSink logger() {
			return LOGGER;
		}

		@Override
		public String getNextItem() throws Exception {
			if (used) {
				return null;
			}
			used = true;
			return "TEST"; // NON-NLS
		}

	}

	private class TestActivityParser extends ActivityParser {

		protected TestActivityParser() {
			super();
		}

		@Override
		protected EventSink logger() {
			return LOGGER;
		}

		@Override
		public void setProperty(String name, String value) {
		}

		@Override
		public Object getProperty(String name) {
			return null;
		}

		@Override
		public void addField(ActivityField field) {
		}

		@Override
		public void organizeFields() {
		}

		@Override
		protected ActivityInfo parse(TNTInputStream<?, ?> stream, ActivityData<Object> data,
				ActivityParserContext cData) throws IllegalStateException, ParseException {
			if (data.equals("TESTPARSEEXCEPTION")) { // NON-NLS
				throw new ParseException("TESTPARSEEXCEPTION", 0); // NON-NLS
			}
			return ai;
		}

		@Override
		public boolean isDataClassSupported(Object data) {
			return true;
		}

		@Override
		public void addReference(Object refObject) {

		}

		@Override
		protected String[] getActivityDataType() {
			return new String[] { "TEXT" }; // NON-NLS
		}

	}

}
