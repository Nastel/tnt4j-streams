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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;

/**
 * General logger utility methods used by TNT4J-Streams.
 *
 * @version $Revision: 1 $
 */
public class LoggerUtils {
	private static final int LOG4J = 1 << 0;
	private static final int LOGBACK = 1 << 1;
	private static final int JUL = 1 << 2;
	private static final int LOG4J2 = 1 << 3;
	private static final int SLF4J = 1 << 10; // NOTE: SLF4J may be as a logger (SLF4JSimple) or as wrapper.

	/**
	 * Detects used logging framework. Now recognizes these logger frameworks:
	 * <ul>
	 * <li>LOG4J</li>
	 * <li>JUL - Java util logging</li>
	 * <li>LOGBACK</li>
	 * <li>SLF4J - may be in combination with any from above</li>
	 * </ul>
	 *
	 * @param logger
	 *            logger event sink instance
	 *
	 * @return number containing used logging frameworks describing bits
	 */
	public static int detectLogger(EventSink logger) {
		int loggerMask = 0;

		if (!logger.isOpen()) {
			try {
				logger.open();
			} catch (Exception exc) {
				return loggerMask;
			}
		}

		Class<?> shc = logger.getSinkHandle().getClass();
		if (shc.getName().startsWith("org.apache.log4j.")) { // NON-NLS
			loggerMask |= LOG4J;
		} else if (shc.getName().startsWith("ch.qos.logback.")) { // NON-NLS
			loggerMask |= LOGBACK;
		} else if (shc.getName().startsWith("java.util.logging.")) { // NON-NLS
			loggerMask |= JUL;
		} else if (shc.getName().startsWith("org.apache.logging.log4j.")
				|| shc.getName().startsWith("org.apache.logging.slf4j.")) { // NON-NLS
			loggerMask |= LOG4J2;
		} else if (shc.getName().startsWith("org.slf4j.")) { // NON-NLS
			loggerMask |= SLF4J;

			if (shc.getName().equals("org.slf4j.impl.Log4jLoggerAdapter")) { // NON-NLS
				loggerMask |= LOG4J;
			} else if (shc.getName().equals("org.slf4j.impl.JDK14LoggerAdapter")) { // NON-NLS
				loggerMask |= JUL;
			} else {
				// TODO: anything else?
			}
		}

		return loggerMask;
	}

	/**
	 * Applies provided logger configuration data.
	 *
	 * @param cfgData
	 *            binary logger configuration data
	 * @param logger
	 *            logger instance
	 */
	public static void setLoggerConfig(byte[] cfgData, EventSink logger) {
		int lUsed = detectLogger(logger);

		if (Utils.matchMask(lUsed, LOG4J)) {
			setLog4jConfig(cfgData, logger);
		} else if (Utils.matchMask(lUsed, LOGBACK)) {
			setLogbackConfig(cfgData, logger);
		} else if (Utils.matchMask(lUsed, JUL)) {
			setJULConfig(cfgData, logger);
		} else if (Utils.matchMask(lUsed, LOG4J2)) {
			setLog4j2Config(cfgData, logger);
		} else {
			logger.log(OpLevel.WARNING, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"LoggerUtils.unknown.logger");
		}
	}

	private static void setLog4jConfig(byte[] data, EventSink logger) {
		Properties loggerProps = new Properties();
		try (InputStream is = new ByteArrayInputStream(data)) {
			loggerProps.load(is);
		} catch (Exception exc) {
			Utils.logThrowable(logger, OpLevel.ERROR, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"LoggerUtils.log4j.load.error", exc);
		}

		if (MapUtils.isEmpty(loggerProps)) {
			logger.log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"LoggerUtils.log4j.empty.configuration");
		} else {
			logger.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"LoggerUtils.log4j.reconfiguring", loggerProps.size());
			try {
				// org.apache.log4j.PropertyConfigurator.configure(loggerProps);

				invoke("org.apache.log4j.PropertyConfigurator", "configure", new Class[] { Properties.class }, // NON-NLS
						loggerProps);
			} catch (Exception exc) {
				Utils.logThrowable(logger, OpLevel.ERROR,
						StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"LoggerUtils.log4j.reconfiguring.fail", exc);
			}

			logger.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"LoggerUtils.log4j.reconfiguring.end");
		}
	}

	private static void setLog4j2Config(byte[] data, EventSink logger) {
		try (InputStream is = new ByteArrayInputStream(data)) {
			logger.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"LoggerUtils.log4j2.reconfiguring");
			// org.apache.logging.log4j.core.config.ConfigurationSource source =
			// new org.apache.logging.log4j.core.config.ConfigurationSource (is);
			Object source = Utils.createInstance("org.apache.logging.log4j.core.config.ConfigurationSource",
					new Object[] { is }, InputStream.class); // NON-NLS
			// org.apache.logging.log4j.core.LoggerContext ctx = (org.apache.logging.log4j.core.LoggerContext)
			// LogManager.getContext(false);
			Object ctx = invoke("org.apache.logging.log4j.LogManager", "getContext", new Class[] { boolean.class },
					false); // NON-NLS
			// Configuration cfg = new XmlConfiguration((LoggerContext) ctx, (ConfigurationSource) source);
			Object cfg = Utils.createInstance("org.apache.logging.log4j.core.config.xml.XmlConfiguration",
					new Object[] { ctx, source }, ctx.getClass(), source.getClass()); // NON-NLS
			// ctx.reconfigure(cfg);
			invoke(ctx, "reconfigure",
					new Class[] { Class.forName("org.apache.logging.log4j.core.config.Configuration") }, cfg); // NON-NLS
		} catch (Exception exc) {
			Utils.logThrowable(logger, OpLevel.ERROR, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"LoggerUtils.log4j2.reconfiguring.fail", exc);
		}

		logger.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"LoggerUtils.log4j2.reconfiguring.end");
	}

	private static void setJULConfig(byte[] data, EventSink logger) {
		try (InputStream is = new ByteArrayInputStream(data)) {
			logger.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"LoggerUtils.jul.reconfiguring");
			java.util.logging.LogManager.getLogManager().readConfiguration(is);
		} catch (Exception exc) {
			Utils.logThrowable(logger, OpLevel.ERROR, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"LoggerUtils.jul.reconfiguring.fail", exc);
		}

		logger.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"LoggerUtils.jul.reconfiguring.end");
	}

	private static void setLogbackConfig(byte[] data, EventSink logger) {
		try (InputStream is = new ByteArrayInputStream(data)) {
			logger.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"LoggerUtils.logback.reconfiguring");
			// ch.qos.logback.classic.LoggerContext context = (ch.qos.logback.classic.LoggerContext)
			// org.slf4j.LoggerFactory.getILoggerFactory();
			Object context = invoke("org.slf4j.LoggerFactory", "getILoggerFactory", null); // NON-NLS
			// ch.qos.logback.classic.joran.JoranConfigurator jc = new ch.qos.logback.classic.joran.JoranConfigurator();
			Object jc = Utils.createInstance("ch.qos.logback.classic.joran.JoranConfigurator"); // NON-NLS
			// jc.setContext(context);
			Class<?> ctxClass = Class.forName("ch.qos.logback.classic.LoggerContext");
			invoke(jc, "setContext", new Class[] { ctxClass }, context); // NON-NLS
			// context.reset();
			invoke(context, "reset", null); // NON-NLS
			// jc.doConfigure(is);
			invoke(jc, "doConfigure", new Class[] { InputStream.class }, is); // NON-NLS
		} catch (Exception exc) {
			Utils.logThrowable(logger, OpLevel.ERROR, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"LoggerUtils.logback.reconfiguring.fail", exc);
		}

		logger.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"LoggerUtils.logback.reconfiguring.end");
	}

	private static Object invoke(String className, String methodName, Class<?>[] paramTypes, Object... params)
			throws Exception {
		return invoke(null, className, methodName, paramTypes, params);
	}

	private static Object invoke(Object obj, String methodName, Class<?>[] paramTypes, Object... params)
			throws Exception {
		return invoke(obj, null, methodName, paramTypes, params);
	}

	private static Object invoke(Object obj, String className, String methodName, Class<?>[] paramTypes,
			Object... params) throws Exception {
		Class<?> cls = obj == null ? Class.forName(className) : obj.getClass();
		Method m = cls.getDeclaredMethod(methodName, paramTypes);
		return m.invoke(obj, params);
	}

	/**
	 * Obtains default logger event sink.
	 * <p>
	 * Performs fallback values initialization for required system properties.
	 *
	 * @param clazz
	 *            class for which to get the event sink
	 * @return new event sink instance associated with given class
	 *
	 * @see com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory#defaultEventSink(Class)
	 */
	public static EventSink getLoggerSink(Class<?> clazz) {
		initLoggerProperties();
		return DefaultEventSinkFactory.defaultEventSink(clazz);
	}

	/**
	 * Obtains default logger event sink.
	 * <p>
	 * Performs fallback values initialization for required system properties.
	 *
	 * @param name
	 *            name of the application/event sink to get
	 * @return new event sink instance associated with given class
	 *
	 * @see com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory#defaultEventSink(Class)
	 */
	public static EventSink getLoggerSink(String name) {
		initLoggerProperties();
		return DefaultEventSinkFactory.defaultEventSink(name);
	}

	private static void initLoggerProperties() {
		String lProp = System.getProperty(DefaultEventSinkFactory.DEFAULT_EVENT_FACTORY_KEY);
		if (StringUtils.isEmpty(lProp)) {
			System.setProperty(DefaultEventSinkFactory.DEFAULT_EVENT_FACTORY_KEY,
					"com.jkoolcloud.tnt4j.sink.impl.slf4j.SLF4JEventSinkFactory");
		}
	}

	private static void processLogArgs(Object... args) {
		if (ArrayUtils.isNotEmpty(args)) {
			for (int i = 0; i < args.length; i++) {
				if (!(args[i] instanceof Throwable)) {
					args[i] = Utils.toStringDump(args[i]);
				}
			}
		}
	}

	/**
	 * Log a given string message with a specified severity and {@link java.lang.Throwable} details.
	 * 
	 * @param logger
	 *            logger instance to use for logging
	 * @param sev
	 *            log entry severity
	 * @param msg
	 *            log message pattern
	 * @param args
	 *            log message formatting arguments
	 * 
	 * @see com.jkoolcloud.tnt4j.streams.utils.Utils#logThrowable(com.jkoolcloud.tnt4j.sink.EventSink,
	 *      com.jkoolcloud.tnt4j.core.OpLevel, String, Object...)
	 */
	public static void logThrowable(EventSink logger, OpLevel sev, String msg, Object... args) {
		if (logger.isSet(sev)) {
			processLogArgs(args);
			Utils.logThrowable(logger, sev, msg, args);
		}
	}

	/**
	 * Log a given resource bundle message with a specified severity and {@link java.lang.Throwable} details.
	 * 
	 * @param logger
	 *            logger instance to use for logging
	 * @param sev
	 *            log entry severity
	 * @param rb
	 *            resource bundle to use
	 * @param key
	 *            resource bundle entry key
	 * @param args
	 *            log message formatting arguments
	 * 
	 * @see Utils#logThrowable(com.jkoolcloud.tnt4j.sink.EventSink, com.jkoolcloud.tnt4j.core.OpLevel,
	 *      java.util.ResourceBundle, String, Object...)
	 */
	public static void logThrowable(EventSink logger, OpLevel sev, ResourceBundle rb, String key, Object... args) {
		if (logger.isSet(sev)) {
			processLogArgs(args);
			Utils.logThrowable(logger, sev, rb, key, args);
		}
	}

	/**
	 * Log a given string message with a specified severity.
	 * 
	 * @param logger
	 *            logger instance to use for logging
	 * @param sev
	 *            log entry severity
	 * @param msg
	 *            log message pattern
	 * @param args
	 *            log message formatting arguments
	 * 
	 * @see com.jkoolcloud.tnt4j.sink.EventSink#log(com.jkoolcloud.tnt4j.core.OpLevel, String, Object...)
	 */
	public static void log(EventSink logger, OpLevel sev, String msg, Object... args) {
		if (logger.isSet(sev)) {
			processLogArgs(args);
			logger.log(sev, msg, args);
		}
	}

	/**
	 * Log a given resource bundle entry with a specified severity.
	 * 
	 * @param logger
	 *            logger instance to use for logging
	 * @param sev
	 *            log entry severity
	 * @param rb
	 *            resource bundle to use
	 * @param key
	 *            resource bundle entry key
	 * @param args
	 *            log message formatting arguments
	 * 
	 * @see com.jkoolcloud.tnt4j.sink.EventSink#log(com.jkoolcloud.tnt4j.core.OpLevel, java.util.ResourceBundle, String,
	 *      Object...)
	 */
	public static void log(EventSink logger, OpLevel sev, ResourceBundle rb, String key, Object... args) {
		if (logger.isSet(sev)) {
			processLogArgs(args);
			logger.log(sev, rb, key, args);
		}
	}
}
