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

package com.jkoolcloud.tnt4j.streams.utils;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.CacheProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;

/**
 * Utility class to support TNT4J-Streams streamed data values caching.
 * <p>
 * Cache entries are defined using static or dynamic (e.g., patterns having field name variable to fill in data from
 * activity entity) values.
 * <p>
 * Streams cache supports the following configuration properties:
 * <ul>
 * <li>MaxSize - max. capacity of stream resolved values cache. Default value - {@code 100}. (Optional)</li>
 * <li>ExpireDuration - stream resolved values cache entries expiration duration in minutes. Default value - {@code 10}.
 * (Optional)</li>
 * <li>Persisted - flag indicating cache contents has to be persisted to file on close and loaded on initialization.
 * Default value - {@code false}. (Optional)</li>
 * <li>FileName - defines file name to persist cache entries as XML. Default value - {@code "./persistedCache.xml"}.
 * (Optional)</li>
 * <li>PersistingPeriod - cache persisting period in seconds. Value {@code <= 0} disables periodic persisting. Default
 * value - {@code 0}. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 3 $
 */
public final class StreamsCache {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(StreamsCache.class);

	private static final long DEFAULT_CACHE_MAX_SIZE = 100;
	private static final long DEFAULT_CACHE_EXPIRE_IN_MINUTES = 10;
	private static final String DEFAULT_FILE_NAME = "./persistedCache.xml"; // NON-NLS

	private static final String PARSER_NAME_VAR = "${ParserName}"; // NON-NLS

	private static Cache<String, CacheValue> valuesCache;
	private static Map<String, CacheEntry> cacheEntries = new HashMap<>(5);
	private static AtomicInteger referencesCount = new AtomicInteger();

	private static long maxSize = DEFAULT_CACHE_MAX_SIZE;
	private static long expireDuration = DEFAULT_CACHE_EXPIRE_IN_MINUTES;
	private static boolean persistenceOn = false;
	private static String fileName = DEFAULT_FILE_NAME;

	private static ScheduledExecutorService periodicPersistingScheduler = null;
	private static long persistingPeriodInSeconds;

	private static Cache<String, CacheValue> buildCache(long cSize, long duration) {
		return CacheBuilder.newBuilder().maximumSize(cSize).expireAfterAccess(duration, TimeUnit.MINUTES).build();
	}

	/**
	 * Sets cache configuration properties collection.
	 *
	 * @param props
	 *            configuration properties to set
	 *
	 * @see #initialize()
	 */
	public static void setProperties(Collection<Map.Entry<String, String>> props) {
		if (CollectionUtils.isNotEmpty(props)) {
			for (Map.Entry<String, String> prop : props) {
				String name = prop.getKey();
				String value = prop.getValue();
				if (CacheProperties.PROP_MAX_SIZE.equalsIgnoreCase(name)) {
					maxSize = Long.parseLong(value);
				} else if (CacheProperties.PROP_EXPIRE_DURATION.equalsIgnoreCase(name)) {
					expireDuration = Long.parseLong(value);
				} else if (CacheProperties.PROP_PERSISTED.equalsIgnoreCase(name)) {
					persistenceOn = Utils.toBoolean(value);
				} else if (CacheProperties.PROP_PERSISTED_FILE_NAME.equalsIgnoreCase(name)) {
					fileName = value;
				} else if (CacheProperties.PROP_PERSISTING_PERIOD.equalsIgnoreCase(name)) {
					persistingPeriodInSeconds = Integer.parseInt(value);
				}
			}
		}

		initialize();
	}

	/**
	 * Initializes cache setting maximum cache size and cache entries expiration duration.
	 */
	public static void initialize() {
		valuesCache = buildCache(maxSize, expireDuration);

		if (persistenceOn) {
			loadPersisted();
		}

		if (persistingPeriodInSeconds > 0) {
			periodicPersistingScheduler = Executors.newScheduledThreadPool(1);
			periodicPersistingScheduler.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					if (valuesCache != null) {
						persist(valuesCache.asMap());
					}
				}
			}, persistingPeriodInSeconds, persistingPeriodInSeconds, TimeUnit.SECONDS);
		}
	}

	/**
	 * Checks if streams cache is initialized.
	 * 
	 * @return {@code true} if cache is initialized, {@code false} - otherwise
	 */
	public static boolean isInitialized() {
		return valuesCache != null;
	}

	/**
	 * Fills in cache entries patterns with activity entity fields values and puts filled in entries to cache.
	 *
	 * @param ai
	 *            activity entity to be used to fill in patterns data
	 * @param parserName
	 *            parser name
	 */
	public static void cacheValues(ActivityInfo ai, String parserName) {
		if (!isInitialized()) {
			// valuesCache = buildCache(maxSize, expireDuration);
			return;
		}

		for (CacheEntry cacheEntry : cacheEntries.values()) {
			String resolvedFieldKey = fillInKeyPattern(cacheEntry.getKey(), ai, parserName);
			Object resolvedFieldValue = fillInValuePattern(cacheEntry.getValue(), ai, parserName);

			if (resolvedFieldKey != null && resolvedFieldValue != null) {
				valuesCache.put(resolvedFieldKey, new CacheValue(resolvedFieldValue, cacheEntry.isTransient()));
			}
		}
	}

	/**
	 * Fills in some key pattern string with activity entity fields values.
	 * 
	 * @param pattern
	 *            pattern string to fill
	 * @param ai
	 *            activity entity data
	 * @param parserName
	 *            parser name
	 * @return pattern string filled in with data values
	 */
	public static String fillInKeyPattern(String pattern, ActivityInfo ai, String parserName) {
		List<String> vars = new ArrayList<>();
		Utils.resolveCfgVariables(vars, pattern);

		return fillInPattern(pattern, vars, ai, parserName);
	}

	private static Object fillInValuePattern(String pattern, ActivityInfo ai, String parserName) {
		List<String> vars = new ArrayList<>();
		Utils.resolveCfgVariables(vars, pattern);

		return vars.size() == 1 ? ai.getFieldValue(vars.get(0)) : fillInPattern(pattern, vars, ai, parserName);
	}

	private static String fillInPattern(String pattern, List<String> vars, ActivityInfo ai, String parserName) {
		String filledInValue = pattern;

		for (String var : vars) {
			Object fieldValue;
			if (var.equals(PARSER_NAME_VAR)) {
				fieldValue = parserName;
			} else {
				fieldValue = ai.getFieldValue(var);
			}

			if (fieldValue != null) {
				filledInValue = filledInValue.replace(var, Utils.toString(fieldValue));
			}
		}

		return filledInValue;
	}

	/**
	 * Resolves cache stored value identified by cache entry id.
	 *
	 * @param ai
	 *            activity entity to be used to fill in patterns data
	 * @param entryIdStr
	 *            cache entity pattern identifier string
	 * @param parserName
	 *            parser name
	 * @return resolved cached value, {@code default entry value} if there is no data in cache for entry, or
	 *         {@code null} if there is no such entry defined
	 */
	public static Object getValue(ActivityInfo ai, String entryIdStr, String parserName) {
		CacheEntry cacheEntry = cacheEntries.get(entryIdStr);
		if (cacheEntry != null) {
			String cacheKey = fillInKeyPattern(cacheEntry.getKey(), ai, parserName);
			if (cacheKey != null) {
				CacheValue value = valuesCache == null ? null : valuesCache.getIfPresent(cacheKey);
				return value == null ? cacheEntry.getDefaultValue() : value.value();
			} else {
				return cacheEntry.getDefaultValue();
			}
		}
		return null;
	}

	/**
	 * Resolves cache stored value identified by cache entry key.
	 *
	 * @param cacheKey
	 *            cache entry key
	 * @return resolved cached value, {@code default entry value} if there is no data in cache for entry, or
	 *         {@code null} if there is no such entry defined
	 */
	public static Object getValue(String cacheKey) {
		CacheValue value = valuesCache == null ? null : valuesCache.getIfPresent(cacheKey);
		if (value == null) {
			CacheEntry cacheEntry = cacheEntries.get(cacheKey);

			return cacheEntry == null ? null : cacheEntry.getDefaultValue();
		}
		return value.value();
	}

	/**
	 * Cleans cache contents.
	 *
	 * @see #unreferStream()
	 */
	public static void cleanup() {
		if (periodicPersistingScheduler != null) {
			periodicPersistingScheduler.shutdown();

			try {
				periodicPersistingScheduler.awaitTermination(20, TimeUnit.SECONDS);
			} catch (InterruptedException exc) {
			} finally {
				periodicPersistingScheduler.shutdownNow();
			}
		}

		if (valuesCache != null) {
			if (persistenceOn) {
				persist(valuesCache.asMap());
			}
			valuesCache.invalidateAll();
		}
		cacheEntries.clear();
	}

	/**
	 * Cleans all cache stored values.
	 */
	public static void clearValues() {
		if (valuesCache != null) {
			valuesCache.invalidateAll();
		}
	}

	/**
	 * Cleans cache stored values for entries having key fragment defined by {@code entryKey}.
	 * 
	 * @param entryKey
	 *            cache entry key fragment
	 */
	public static void clearValues(String entryKey) {
		if (valuesCache != null) {
			Map<String, CacheValue> cEntries = valuesCache.asMap();
			List<String> keyList = new ArrayList<>();
			for (Map.Entry<String, CacheValue> ce : cEntries.entrySet()) {
				if (ce.getKey().startsWith(entryKey)) {
					keyList.add(ce.getKey());
				}
			}

			valuesCache.invalidateAll(keyList);
		}
	}

	/**
	 * Adds stream-cache reference.
	 */
	public static void referStream() {
		referencesCount.getAndIncrement();
	}

	/**
	 * Removes stream-cache reference. When last stream is unreferenced, cache gets cleaned.
	 *
	 * @see #cleanup()
	 */
	public static void unreferStream() {
		int crc = referencesCount.decrementAndGet();

		if (crc <= 0) {
			cleanup();
			referencesCount.set(0);
		}
	}

	/**
	 * Adds cache entry pattern definition to cache entry patterns map.
	 *
	 * @param entryId
	 *            entry identifier
	 * @param key
	 *            entry key pattern
	 * @param value
	 *            entry value pattern
	 * @param defaultValue
	 *            default entry value
	 * @return previous cache entry instance stored
	 */
	public static CacheEntry addEntry(String entryId, String key, String value, String defaultValue) {
		return cacheEntries.put(entryId, new CacheEntry(entryId, key, value, defaultValue));
	}

	/**
	 * Adds cache entry pattern definition to cache entry patterns map.
	 *
	 * @param entryId
	 *            entry identifier
	 * @param key
	 *            entry key pattern
	 * @param value
	 *            entry value pattern
	 * @param defaultValue
	 *            default entry value
	 * @param transientEntry
	 *            indicating whether cache entry is transient and should not be persisted
	 * @return previous cache entry instance stored
	 */
	public static CacheEntry addEntry(String entryId, String key, String value, String defaultValue,
			boolean transientEntry) {
		return cacheEntries.put(entryId, new CacheEntry(entryId, key, value, defaultValue, transientEntry));
	}

	private static void loadPersisted() {
		try {
			JAXBContext jc = JAXBContext.newInstance(CacheRoot.class);
			Unmarshaller unmarshaller = jc.createUnmarshaller();
			File persistedFile = new File(fileName);
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"StreamsCache.loading.file", persistedFile.getAbsolutePath());
			if (!persistedFile.exists()) {
				LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"StreamsCache.loading.file.not.found");
				return;
			}

			CacheRoot root = (CacheRoot) unmarshaller.unmarshal(persistedFile);

			Map<String, CacheValue> mapProperty = root.getEntriesMap();
			if (MapUtils.isNotEmpty(mapProperty)) {
				for (Map.Entry<String, CacheValue> entry : mapProperty.entrySet()) {
					valuesCache.put(entry.getKey(), entry.getValue());
				}
			}
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"StreamsCache.loading.done", mapProperty == null ? 0 : mapProperty.size(),
					persistedFile.getAbsolutePath());
		} catch (JAXBException exc) {
			Utils.logThrowable(LOGGER, OpLevel.ERROR, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"StreamsCache.loading.failed", exc);
		}
	}

	private static void persist(Map<String, CacheValue> cacheEntries) {
		try {
			JAXBContext jc = JAXBContext.newInstance(CacheRoot.class);
			Marshaller marshaller = jc.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			CacheRoot root = new CacheRoot();
			root.setEntriesMap(cacheEntries);
			File persistedFile = new File(fileName);
			File parentPath = persistedFile.getParentFile();
			if (parentPath != null) {
				parentPath.mkdirs();
			}
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"StreamsCache.persisting.file", persistedFile.getAbsolutePath());
			marshaller.marshal(root, persistedFile);
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"StreamsCache.persisting.done", cacheEntries.size(), persistedFile.getAbsolutePath());
		} catch (JAXBException exc) {
			Utils.logThrowable(LOGGER, OpLevel.ERROR, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"StreamsCache.persisting.failed", exc);
		}
	}

	/**
	 * Adds non-transient cache entry for defined key and value.
	 * 
	 * @param key
	 *            cache entry key
	 * @param value
	 *            cache entry value
	 */
	public static void addValue(String key, Object value) {
		addValue(key, value, false);
	}

	/**
	 * Adds cache entry for defined key and value.
	 * 
	 * @param key
	 *            cache entry key
	 * @param value
	 *            cache entry value
	 * @param transientValue
	 *            flag indicating if value is transient
	 */
	public static void addValue(String key, Object value, boolean transientValue) {
		if (!isInitialized()) {
			initialize();
		}

		valuesCache.put(key, new CacheValue(value, transientValue));
	}

	/**
	 * Defines cache entry pattern.
	 */
	public static class CacheEntry {
		private String id;
		private String key;
		private String value;
		private String defaultValue;
		private boolean transientEntry = false;

		/**
		 * Constructs new CacheEntry.
		 *
		 * @param id
		 *            cache entry identifier
		 * @param key
		 *            cache entry key pattern
		 * @param value
		 *            cache entry value pattern
		 * @param defaultValue
		 *            cache entry default value
		 */
		private CacheEntry(String id, String key, String value, String defaultValue) {
			this(id, key, value, defaultValue, false);
		}

		/**
		 * Constructs new CacheEntry.
		 *
		 * @param id
		 *            cache entry identifier
		 * @param key
		 *            cache entry key pattern
		 * @param value
		 *            cache entry value pattern
		 * @param defaultValue
		 *            cache entry default value
		 * @param transientEntry
		 *            indicating whether cache entry is transient and should not be persisted
		 */
		private CacheEntry(String id, String key, String value, String defaultValue, boolean transientEntry) {
			this.id = id;
			this.key = key;
			this.value = value;
			this.defaultValue = defaultValue;
			this.transientEntry = transientEntry;
		}

		/**
		 * Returns cache entry identifier.
		 *
		 * @return entry identifier
		 */
		public String getId() {
			return id;
		}

		/**
		 * Returns cache entry key pattern.
		 *
		 * @return cache entry key pattern
		 */
		public String getKey() {
			return key;
		}

		/**
		 * Returns cache entry value pattern.
		 *
		 * @return cache entry value pattern
		 */
		public String getValue() {
			return value;
		}

		/**
		 * Returns default cache entry value.
		 *
		 * @return default cache entry value
		 */
		public String getDefaultValue() {
			return defaultValue;
		}

		/**
		 * Returns flag indicating that all cache entries build by this entry pattern are transient and should not be
		 * persisted.
		 * 
		 * @return {@code true} if cache entries build by this entry pattern are transient and should not be persisted,
		 *         {@code false} - otherwise
		 */
		public boolean isTransient() {
			return transientEntry;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("CacheEntry{"); // NON-NLS
			sb.append("id="); // NON-NLS
			Utils.quote(id, sb);
			sb.append(", key="); // NON-NLS
			Utils.quote(key, sb);
			sb.append(", value="); // NON-NLS
			Utils.quote(value, sb);
			sb.append(", defaultValue="); // NON-NLS
			Utils.quote(defaultValue, sb);
			sb.append(", transient="); // NON-NLS
			Utils.quote(transientEntry, sb);
			sb.append('}'); // NON-NLS
			return sb.toString();
		}
	}

	/**
	 * Defines cache entry value.
	 */
	public static class CacheValue {
		private Object value;
		private boolean transientValue = false;

		public CacheValue(Object value) {
			this(value, false);
		}

		public CacheValue(Object value, boolean transientValue) {
			this.value = value;
			this.transientValue = transientValue;
		}

		public Object value() {
			return value;
		}

		public boolean isTransient() {
			return transientValue;
		}
	}

	/**
	 * Cache entries map JAXB marshaling adapter.
	 */
	public static class MapAdapter extends XmlAdapter<MapEntry[], Map<String, CacheValue>> {
		@Override
		public MapEntry[] marshal(Map<String, CacheValue> cache) throws Exception {
			List<MapEntry> pEntries = new ArrayList<>(cache.size());
			for (Map.Entry<String, CacheValue> entry : cache.entrySet()) {
				if (!entry.getValue().isTransient()) {
					pEntries.add(new MapEntry(entry.getKey(), entry.getValue().value()));

					LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"StreamsCache.entry.marshal", entry.getKey(), Utils.toString(entry.getValue().value()));
				} else {
					LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"StreamsCache.entry.marshal.skip", entry.getKey(),
							Utils.toString(entry.getValue().value()));
				}
			}

			return pEntries.toArray(new MapEntry[pEntries.size()]);
		}

		@Override
		public Map<String, CacheValue> unmarshal(MapEntry[] mapElements) throws Exception {
			Map<String, CacheValue> r = new ConcurrentHashMap<>(mapElements.length);
			for (MapEntry mapElement : mapElements) {
				r.put(mapElement.key, new CacheValue(mapElement.getValue()));
				LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"StreamsCache.entry.unmarshal", mapElement.key, Utils.toString(mapElement.getValue()));
			}
			return r;
		}
	}

	/**
	 * Cache map entry marshaling entity.
	 */
	public static class MapEntry {
		@XmlElement
		public String key;
		private Object value;

		@XmlElement
		public Object getValue() {
			return value;
		}

		public void setValue(Object value) {
			if (value instanceof XMLGregorianCalendar) {
				this.value = ((XMLGregorianCalendar) value).toGregorianCalendar().getTime();
			} else {
				this.value = value;
			}
		}

		@SuppressWarnings("unused")
		private MapEntry() {
		} // Required by JAXB

		public MapEntry(String key, Object value) {
			this.key = key;
			setValue(value);
		}
	}

	/**
	 * Root element for JAXB cache entries persisting.
	 */
	@XmlRootElement
	public static class CacheRoot {

		private Map<String, CacheValue> entriesMap;

		@XmlJavaTypeAdapter(MapAdapter.class)
		public Map<String, CacheValue> getEntriesMap() {
			return entriesMap;
		}

		public void setEntriesMap(Map<String, CacheValue> map) {
			entriesMap = map;
		}
	}

	// private static class ByteArrayAdapter extends XmlAdapter<String, Byte[]> {
	//
	// @Override
	// public Byte[] unmarshal(String v) throws Exception {
	// return ArrayUtils.toObject(v.getBytes());
	// }
	//
	// @Override
	// public String marshal(Byte[] v) throws Exception {
	// return String.valueOf(v);
	// }
	// }

}
