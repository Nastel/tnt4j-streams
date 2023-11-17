package com.jkoolcloud.tnt4j.streams.filters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.fields.*;
import com.jkoolcloud.tnt4j.streams.utils.StreamsConstants;

public class StreamEntityFilterTest {

	@Test
	public void testFiltersSimpleScenarioExclude() throws ParseException {
		ActivityInfo ai = new ActivityInfo();
		ActivityField af = new ActivityField("Correlator");
		// ActivityField af2 = new ActivityField("Correlator2");
		ActivityFieldLocator testLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "TestLocator");
		// testLocator.formatValue("1");

		ActivityFieldLocator locator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "TestLocator");
		locator.setDataType(ActivityFieldDataType.Binary);
		StreamFiltersGroup<Object> filterGroup = new StreamFiltersGroup<>("FilterGroup");
		DefaultValueFilter filter = new DefaultValueFilter("EXCLUDE", "IS", "hexBinary",
				"0x000000000000000000000000000000000000000000000000");
		DefaultValueFilter filter2 = new DefaultValueFilter("EXCLUDE", "IS", "hexBinary",
				"0x000000000000000000000000000000000000000000000001");

		filterGroup.addFilter(filter);
		filterGroup.addFilter(filter2);

		af.setFilter(filterGroup);
		af.addLocator(locator);
		af.setRequired("false");

		byte[] byteZero = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
		byte[] byteZero1 = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01 };
		byte[] byteZero2 = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02 };

		ai.applyField(af, "1111");
		ai.applyField(af, "2222");
		ai.applyField(af, byteZero);
		ai.applyField(af, byteZero1);
		ai.applyField(af, byteZero2);

		System.out.println(ai.getFieldValue("Correlator"));

		assertTrue(ai.getFieldValue("Correlator").toString().split(",").length == 3);
		// should not be [1111, 2222, 000000000000000000000000000000000000000000000000, null]
		// should be [1111, 2222, 000000000000000000000000000000000000000000000000]
	}

	@Test
	public void testFiltersSimpleScenarioInclude() throws ParseException {
		ActivityInfo ai = new ActivityInfo();
		ActivityField af = new ActivityField("Correlator");
		// ActivityField af2 = new ActivityField("Correlator2");
		ActivityFieldLocator testLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "TestLocator");
		// testLocator.formatValue("1");

		ActivityFieldLocator locator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "TestLocator");
		locator.setDataType(ActivityFieldDataType.Binary);
		StreamFiltersGroup<Object> filterGroup = new StreamFiltersGroup<>("FilterGroup");
		DefaultValueFilter filter = new DefaultValueFilter("INCLUDE", "IS", "hexBinary",
				"0x000000000000000000000000000000000000000000000000");
		DefaultValueFilter filter2 = new DefaultValueFilter("INCLUDE", "IS", "hexBinary",
				"0x000000000000000000000000000000000000000000000001");

		filterGroup.addFilter(filter);
		filterGroup.addFilter(filter2);

		af.setFilter(filterGroup);
		af.addLocator(locator);
		af.setRequired("false");

		byte[] byteZero = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
		byte[] byteZero1 = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01 };
		byte[] byteZero2 = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02 };

		ai.applyField(af, "1111");
		ai.applyField(af, "2222");
		ai.applyField(af, byteZero);
		ai.applyField(af, byteZero1);
		ai.applyField(af, byteZero2);

		System.out.println(ai.getFieldValue("Correlator"));

		assertTrue(ai.getFieldValue("Correlator").toString().split(",").length == 2);
		// should not be [1111, 2222, 000000000000000000000000000000000000000000000000,
		// 000000000000000000000000000000000000000000000001, 000000000000000000000000000000000000000000000002]
		// should be [1111, 2222, 000000000000000000000000000000000000000000000000]
	}

	@Test
	public void testFilterIncludePositive() throws FilterException {
		StreamFiltersGroup<Object> filterGroup = new StreamFiltersGroup<>("FilterGroup");
		DefaultValueFilter filter = new DefaultValueFilter("INCLUDE", "IS", "string", "123");

		filterGroup.addFilter(filter);

		Map<String, Object> context = new HashMap<>(1);
		context.put(StreamsConstants.CTX_ACTIVITY_DATA_KEY, mock(ActivityInfo.class));

		boolean filterResult = filterGroup.doFilter("123", context);
		assertFalse(filterResult);
	}

	@Test
	public void testFilterIncludeNegative() throws FilterException {
		StreamFiltersGroup<Object> filterGroup = new StreamFiltersGroup<>("FilterGroup");
		DefaultValueFilter filter = new DefaultValueFilter("INCLUDE", "IS", "string", "1243");

		filterGroup.addFilter(filter);

		Map<String, Object> context = new HashMap<>(1);
		context.put(StreamsConstants.CTX_ACTIVITY_DATA_KEY, mock(ActivityInfo.class));

		boolean filterResult = filterGroup.doFilter("123", context);
		assertTrue(filterResult);
	}

	@Test
	public void testFilterExcludePositive() throws FilterException {
		StreamFiltersGroup<Object> filterGroup = new StreamFiltersGroup<>("FilterGroup");
		DefaultValueFilter filter = new DefaultValueFilter("EXCLUDE", "IS", "string", "123");

		filterGroup.addFilter(filter);

		Map<String, Object> context = new HashMap<>(1);
		context.put(StreamsConstants.CTX_ACTIVITY_DATA_KEY, mock(ActivityInfo.class));

		boolean filterResult = filterGroup.doFilter("123", context);
		assertTrue(filterResult);
	}

	@Test
	public void testFilterExcludeNegative() throws FilterException {
		StreamFiltersGroup<Object> filterGroup = new StreamFiltersGroup<>("FilterGroup");
		DefaultValueFilter filter = new DefaultValueFilter("EXCLUDE", "IS", "string", "1243");

		filterGroup.addFilter(filter);

		Map<String, Object> context = new HashMap<>(1);
		context.put(StreamsConstants.CTX_ACTIVITY_DATA_KEY, mock(ActivityInfo.class));

		boolean filterResult = filterGroup.doFilter("123", context);
		assertFalse(filterResult);
	}

	@Test
	public void testFilterNull() throws FilterException {
		StreamFiltersGroup<Object> filterGroup = new StreamFiltersGroup<>("FilterGroup");
		DefaultValueFilter filter = new DefaultValueFilter("EXCLUDE", "IS", "string", "1243");

		filterGroup.addFilter(filter);

		Map<String, Object> context = new HashMap<>(1);
		context.put(StreamsConstants.CTX_ACTIVITY_DATA_KEY, mock(ActivityInfo.class));

		boolean filterResult = filterGroup.doFilter(null, context);
		assertFalse(filterResult);
	}

	@Test
	public void testFilterNullPositive() throws FilterException {
		StreamFiltersGroup<Object> filterGroup = new StreamFiltersGroup<>("FilterGroup");
		DefaultValueFilter filter = new DefaultValueFilter("EXCLUDE", "IS", "string", "null");

		filterGroup.addFilter(filter);

		Map<String, Object> context = new HashMap<>(1);
		context.put(StreamsConstants.CTX_ACTIVITY_DATA_KEY, mock(ActivityInfo.class));

		boolean filterResult = filterGroup.doFilter(null, context);
		assertTrue(filterResult);
	}

}