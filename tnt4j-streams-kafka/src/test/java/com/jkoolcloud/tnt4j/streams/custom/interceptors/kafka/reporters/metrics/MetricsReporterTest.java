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

package com.jkoolcloud.tnt4j.streams.custom.interceptors.kafka.reporters.metrics;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import javax.management.*;

import org.junit.Test;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jkoolcloud.tnt4j.TrackingLogger;
import com.jkoolcloud.tnt4j.config.DefaultConfigFactory;
import com.jkoolcloud.tnt4j.config.TrackerConfig;
import com.jkoolcloud.tnt4j.core.Activity;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.core.Snapshot;
import com.jkoolcloud.tnt4j.format.LevelingJSONFormatter;
import com.jkoolcloud.tnt4j.source.SourceType;
import com.jkoolcloud.tnt4j.streams.fields.*;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityJsonParser;
import com.jkoolcloud.tnt4j.tracker.Tracker;
import com.jkoolcloud.tnt4j.tracker.TrackingActivity;

/**
 * @author akausinis
 * @version 1.0
 */
public class MetricsReporterTest {

	@Test
	public void collectJMXMetricsAndReport() throws Exception {

		// Prepare part : create and register Mbean
		MBeanServer mBeanServer = createTestMBean();
		Activity activity = new Activity("TestActivity");
		MetricsReporter reporter = new MetricsReporter();
		reporter.collectMetricsJMX("my.name:*", mBeanServer, activity);

		Snapshot snap = activity.getSnapshots().iterator().next();

		// Assure created right
		assertThat(snap.get("Long").getValue(), instanceOf(Long.class));
		assertThat(snap.get("Float").getValue(), instanceOf(Float.class));

		// Serialize to string
		LevelingJSONFormatter formatter = new LevelingJSONFormatter(false);
		String valueToWriteToTopic = formatter.format(snap);

		// Try parsing
		DocumentContext context = JsonPath.parse(valueToWriteToTopic);
		Object longV = context.read("$.properties.Long");
		Object floatV = context.read("$.properties.Float");
		// Assure read correct values
		assertThat(longV, instanceOf(Integer.class));
		assertThat(floatV, instanceOf(Double.class));

		// Try the way Streams actually does this
		ActivityJsonParser parser = new ActivityJsonParser();
		parser.setDefaultDataType(ActivityFieldDataType.AsInput);
		ActivityField af = new ActivityField("Long");
		ActivityField af2 = new ActivityField("Float");
		af.addLocator(new ActivityFieldLocator(ActivityFieldLocatorType.Label, "$.properties.Long",
				parser.getDefaultDataType()));
		af2.addLocator(new ActivityFieldLocator(ActivityFieldLocatorType.Label, "$.properties.Float",
				parser.getDefaultDataType()));
		parser.addField(af);
		parser.addField(af2);
		ActivityInfo ai = parser.parse(mock(TNTInputStream.class), valueToWriteToTopic);
		assertThat(ai.getFieldValue("Long"), instanceOf(Integer.class));
		assertThat(ai.getFieldValue("Float"), instanceOf(Double.class));
	}

	private MBeanServer createTestMBean() throws MalformedObjectNameException, InstanceAlreadyExistsException,
			MBeanRegistrationException, NotCompliantMBeanException {
		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		TestMbean mbean = new TestMbean("my.name:type=Custom");
		mbean.setAttribute("Long", 15L);
		mbean.setAttribute("Float", 15f);
		mBeanServer.registerMBean(mbean, mbean.name());
		return mBeanServer;
	}

	@Test
	public void testTracker() throws Exception {
		TrackerConfig config = DefaultConfigFactory.getInstance().getConfig(MetricsReporter.class, SourceType.APPL,
				(String) null);

		config.setProperty("event.sink.factory", "com.jkoolcloud.tnt4j.sink.impl.kafka.SLF4JEventSinkFactory");
		config.setProperty("source", "com.jkoolcloud.tnt4j.streams.custom.interceptors.kafka.reporters.metrics");
		config.setProperty("event.formatter", "com.jkoolcloud.tnt4j.format.LevelingJSONFormatter");
		config.setProperty("event.formatter.Level", "1");

		Tracker tracker = TrackingLogger.getInstance(config.build());

		TrackingActivity activity = tracker.newActivity(OpLevel.INFO, "ActivityName");
		tracker.log(OpLevel.ERROR, "ASdfsadf");
		tracker.tnt(activity);
		//
		// MBeanServer mBeanServer = createTestMBean();
		// MetricsReporter mr = new MetricsReporter(Integer.MAX_VALUE);
		//
		// mr.reportMetrics(new HashMap<String, MetricsReporter.TopicMetrics>());
	}

	private static class TestMbean implements DynamicMBean {
		private final ObjectName objectName;
		private final Map<String, Object> attrs;

		public TestMbean(String mbeanName) throws MalformedObjectNameException {
			attrs = new HashMap<>();
			objectName = new ObjectName(mbeanName);
		}

		public ObjectName name() {
			return objectName;
		}

		public void setAttribute(String name, Object attr) {
			attrs.put(name, attr);
		}

		@Override
		public Object getAttribute(String name) throws AttributeNotFoundException, MBeanException, ReflectionException {
			if (attrs.containsKey(name)) {
				return attrs.get(name);
			} else {
				throw new AttributeNotFoundException("Could not find attribute " + name);
			}
		}

		@Override
		public AttributeList getAttributes(String[] names) {
			AttributeList list = new AttributeList();
			for (String name : names) {
				try {
					list.add(new Attribute(name, getAttribute(name)));
				} catch (Exception e) {
				}
			}
			return list;
		}

		public Object removeAttribute(String name) {
			return attrs.remove(name);
		}

		@Override
		public MBeanInfo getMBeanInfo() {
			MBeanAttributeInfo[] attrs = new MBeanAttributeInfo[this.attrs.size()];
			int i = 0;
			for (Map.Entry<String, Object> entry : this.attrs.entrySet()) {
				String attribute = entry.getKey();
				Object metric = entry;
				attrs[i] = new MBeanAttributeInfo(attribute, double.class.getName(), "TEST", true, false, false);
				i += 1;
			}
			return new MBeanInfo(getClass().getName(), "", attrs, null, null, null);
		}

		@Override
		public Object invoke(String name, Object[] params, String[] sig) throws MBeanException, ReflectionException {
			throw new UnsupportedOperationException("Set not allowed.");
		}

		@Override
		public void setAttribute(Attribute attribute)
				throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
			throw new UnsupportedOperationException("Set not allowed.");
		}

		@Override
		public AttributeList setAttributes(AttributeList list) {
			throw new UnsupportedOperationException("Set not allowed.");
		}

	}

}
