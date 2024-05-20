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

package com.jkoolcloud.tnt4j.streams.configure.zookeeper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.ZKUtil;
import org.apache.zookeeper.data.Stat;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Main class for jKool LLC TNT4J-Streams configuration uploader to ZooKeeper.
 *
 * @version $Revision: 1 $
 */
public class ZKConfigInit {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(ZKConfigInit.class);

	private static final String PARAM_CFG_FILE = "-f:"; // NON-NLS
	private static final String PARAM_CLEAN = "-c"; // NON-NLS

	private static String cfgFileName = null;
	private static boolean clean = false;

	/**
	 * Main entry point for running as a standalone application.
	 *
	 * @param args
	 *            command-line arguments. Supported arguments:
	 *            <table>
	 *            <caption>TNT4J-Streams configuration uploader to ZooKeeper command line arguments</caption>
	 *            <tr>
	 *            <td>&nbsp;&nbsp;</td>
	 *            <td>&nbsp;-f:&lt;cfg_file_name&gt;</td>
	 *            <td>(optional) Load TNT4J-Streams configuration upload to ZooKeeper configuration from
	 *            &lt;cfg_file_name&gt;</td>
	 *            </tr>
	 *            <tr>
	 *            <td>&nbsp;&nbsp;</td>
	 *            <td>&nbsp;&nbsp;&nbsp;-c</td>
	 *            <td>(optional) Clean ZooKeeper contained TNT4J-Streams configuration</td>
	 *            </tr>
	 *            </table>
	 */
	public static void main(String... args) {
		LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"ZKConfigInit.starting.main");
		boolean argsValid = processArgs(args);
		if (argsValid) {
			if (StringUtils.isEmpty(cfgFileName)) {
				LOGGER.log(OpLevel.ERROR, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ZKConfigInit.upload.cfg.not.defined");
				System.exit(2);
			} else {
				loadConfigAndRun(cfgFileName);
			}
		}

		System.exit(argsValid ? 0 : 1);
	}

	/**
	 * Loads uploader configuration and runs uploading process.
	 * 
	 * @param cfgFileName
	 *            uploader configuration file path
	 */
	private static void loadConfigAndRun(String cfgFileName) {
		if (StringUtils.isEmpty(cfgFileName)) {
			LOGGER.log(OpLevel.ERROR, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ZKConfigInit.upload.cfg.not.defined");
			return;
		}

		Properties zkup = ZKConfigManager.readStreamsZKConfig(cfgFileName);

		if (MapUtils.isNotEmpty(zkup)) {
			try {
				ZKConfigManager.openConnection(zkup);

				String streamsPath = zkup.getProperty(ZKConfigManager.PROP_ZK_STREAMS_PATH);
				Stat nodeStat = ZKConfigManager.zk().exists(streamsPath, false);

				if (clean && nodeStat != null) {
					LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"ZKConfigInit.clearing.zk", streamsPath);
					ZKUtil.deleteRecursive(ZKConfigManager.zk(), streamsPath);
				}

				byte[] cfgData;
				String cfgPath;

				for (Map.Entry<?, ?> pe : zkup.entrySet()) {
					String pk = (String) pe.getKey();
					String pv = (String) pe.getValue();

					if (!pk.startsWith("zk.")) { // NON-NLS
						cfgData = loadDataFromFile(pv);
						cfgPath = streamsPath + ZKConfigManager.PATH_DELIM
								+ pk.replaceAll("\\.", ZKConfigManager.PATH_DELIM); // NON-NLS
						ZKConfigManager.setNodeData(ZKConfigManager.zk(), cfgPath, cfgData);
					}
				}
			} catch (Exception exc) {
				Utils.logThrowable(LOGGER, OpLevel.ERROR,
						StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME), "ZKConfigInit.upload.error",
						exc);
			} finally {
				ZKConfigManager.close();
			}
		}
	}

	/**
	 * Process and interprets command-line arguments.
	 *
	 * @param args
	 *            command-line arguments
	 * @return {@code true} if command-line arguments were valid to interpret, {@code false} - otherwise
	 */
	private static boolean processArgs(String... args) {
		for (String arg : args) {
			if (StringUtils.isEmpty(arg)) {
				continue;
			}
			if (arg.startsWith(PARAM_CFG_FILE)) {
				if (StringUtils.isNotEmpty(cfgFileName)) {
					System.out.println(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ZKConfigInit.invalid.args"));
					printUsage();
					return false;
				}

				cfgFileName = arg.substring(PARAM_CFG_FILE.length());
				if (StringUtils.isEmpty(cfgFileName)) {
					System.out.println(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"StreamsAgent.missing.cfg.file", arg.substring(0, PARAM_CFG_FILE.length())));
					printUsage();
					return false;
				}
			} else if (PARAM_CLEAN.equals(arg)) {
				clean = true;
			} else {
				System.out.println(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"StreamsAgent.invalid.argument", arg));
				printUsage();
				return false;
			}
		}

		return true;
	}

	/**
	 * Prints short standalone application usage manual.
	 */
	private static void printUsage() {
		System.out.println(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ZKConfigInit.help"));
	}

	/**
	 * Loads all bytes from provided file.
	 * 
	 * @param cfgFileName
	 *            path string of file to read data
	 * @return a byte array containing the bytes read from the file, or {@code null} if path is {@code null}/empty or
	 *         {@link java.io.IOException} occurs
	 */
	public static byte[] loadDataFromFile(String cfgFileName) {
		LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"ZKConfigInit.loading.cfg.data", cfgFileName);

		if (StringUtils.isEmpty(cfgFileName)) {
			return null;
		}

		try {
			return Files.readAllBytes(Paths.get(cfgFileName));
		} catch (IOException exc) {
			Utils.logThrowable(LOGGER, OpLevel.ERROR, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ZKConfigInit.loading.cfg.failed", exc);
			return null;
		}
	}
}
