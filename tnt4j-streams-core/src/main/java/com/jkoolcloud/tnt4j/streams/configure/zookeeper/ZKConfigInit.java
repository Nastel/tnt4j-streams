/*
 * Copyright 2014-2017 JKOOL, LLC.
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

import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.ZKUtil;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Main class for jKool LLC TNT4J-Streams configuration uploader to ZooKeeper.
 *
 * @version $Revision: 1 $
 */
public class ZKConfigInit {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ZKConfigInit.class);

	private static final String PARAM_CFG_FILE = "-f:"; // NON-NLS
	private static final String PARAM_CLEAN = "-c"; // NON-NLS

	private static String cfgFileName = null;
	private static boolean clean = false;

	/**
	 * Main entry point for running as a standalone application.
	 *
	 * @param args
	 *            command-line arguments. Supported arguments:
	 *            <table summary="TNT4J-Streams configuration uploader to ZooKeeper command line arguments">
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
		LOGGER.log(OpLevel.INFO,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ZKConfigInit.starting.main"));
		boolean argsValid = processArgs(args);
		if (argsValid) {
			loadConfigAndRun(cfgFileName);
		}
	}

	/**
	 * Loads uploader configuration and runs uploading process.
	 * 
	 * @param cfgFileName
	 *            uploader configuration file path
	 */
	private static void loadConfigAndRun(String cfgFileName) {
		if (StringUtils.isEmpty(cfgFileName)) {
			LOGGER.log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ZKConfigInit.upload.cfg.not.defined"));
			System.exit(0);
		}
		ZooKeeperConnection zkConn = null;

		try {
			Properties zkup = Utils.loadPropertiesFile(cfgFileName);

			zkConn = new ZooKeeperConnection();
			zkConn.connect(zkup.getProperty("zk.conn", "localhost")); // NON-NLS

			String streamsPath = zkup.getProperty("zk.streams.path");
			// ZKConfigManager.ensureNodeExists(zkConn.zk(), streamsPath);

			if (clean) {
				LOGGER.log(OpLevel.INFO,
						StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ZKConfigInit.clearing.zk"),
						streamsPath);
				ZKUtil.deleteRecursive(zkConn.zk(), streamsPath);
			}

			byte[] cfgData;
			String cfgPath;

			// String streamsCfgPath = streamsPath + "/config"; // NON-NLS
			// ZKConfigManager.ensureNodeExists(zkConn.zk(), streamsCfgPath);
			//
			// cfgData = loadDataFromFile(zkup.getProperty("config.logger"));
			// cfgPath = streamsCfgPath + "/logger"; // NON-NLS
			// ZKConfigManager.setNodeData(zkConn.zk(), cfgPath, cfgData);
			//
			// cfgData = loadDataFromFile(zkup.getProperty("config.tnt4j"));
			// cfgPath = streamsCfgPath + "/tnt4j"; // NON-NLS
			// ZKConfigManager.setNodeData(zkConn.zk(), cfgPath, cfgData);
			//
			// String samplesPath = streamsPath + "/samples"; // NON-NLS
			// ZKConfigManager.ensureNodeExists(zkConn.zk(), samplesPath);

			for (Map.Entry<?, ?> pe : zkup.entrySet()) {
				String pk = (String) pe.getKey();
				String pv = (String) pe.getValue();

				if (pk.startsWith("samples.") || pk.startsWith("config.")) { // NON-NLS
					cfgData = loadDataFromFile(pv);
					cfgPath = streamsPath + ZKConfigManager.PATH_DELIM + pk.replaceAll("\\.", "/"); // NON-NLS
					ZKConfigManager.setNodeData(zkConn.zk(), cfgPath, cfgData);
				}
			}
		} catch (Exception exc) {
			LOGGER.log(OpLevel.ERROR,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ZKConfigInit.upload.error"),
					exc.getLocalizedMessage(), exc);
		} finally {
			zkConn.close();
		}
	}

	/**
	 * Process and interprets command-line arguments.
	 *
	 * @param args
	 *            command-line arguments
	 * @return {@code true} if command-line arguments where valid to interpret, {@code false} - otherwise
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

				cfgFileName = arg.substring(3);
				if (StringUtils.isEmpty(cfgFileName)) {
					System.out.println(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"StreamsAgent.missing.cfg.file", arg.substring(0, 3)));
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
	 * @return a byte array containing the bytes read from the file
	 */
	private static byte[] loadDataFromFile(String cfgFileName) {
		LOGGER.log(OpLevel.INFO,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ZKConfigInit.loading.cfg.data"),
				cfgFileName);
		try {
			return Files.readAllBytes(Paths.get(cfgFileName));
		} catch (IOException exc) {
			LOGGER.log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ZKConfigInit.loading.cfg.failed"), exc.getLocalizedMessage(), exc);
			return null;
		}
	}
}
