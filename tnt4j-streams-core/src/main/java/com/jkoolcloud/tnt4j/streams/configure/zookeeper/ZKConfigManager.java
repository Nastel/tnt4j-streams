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
import java.util.Properties;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.StreamsAgent;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Helper class to manage ZooKeeper stored streams configuration.
 *
 * @version $Revision: 1 $
 */
public class ZKConfigManager implements ZKConfigConstants {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(StreamsAgent.class);

	// create static instance for ZKConnection class.
	private static ZKConnection conn;

	private static Properties zkConfigProperties;

	/**
	 * Creates node in ZK ensemble.
	 * 
	 * @param zk
	 *            ZooKeeper instance
	 * @param path
	 *            node path
	 * @param data
	 *            node data to set
	 * @param cMode
	 *            create mode
	 * @throws KeeperException
	 *             if the server returns a non-zero error code
	 * @throws InterruptedException
	 *             if the transaction is interrupted
	 */
	public static void create(ZooKeeper zk, String path, byte[] data, CreateMode cMode)
			throws KeeperException, InterruptedException {
		zk.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, cMode);
	}

	/**
	 * Creates node in ZK ensemble.
	 * 
	 * @param zk
	 *            ZooKeeper instance
	 * @param path
	 *            node path
	 * @param data
	 *            node data to set
	 * @throws KeeperException
	 *             if the server returns a non-zero error code
	 * @throws InterruptedException
	 *             if the transaction is interrupted
	 *
	 * @see #create(org.apache.zookeeper.ZooKeeper, String, byte[], org.apache.zookeeper.CreateMode)
	 */
	public static void create(ZooKeeper zk, String path, byte[] data) throws KeeperException, InterruptedException {
		create(zk, path, data, CreateMode.PERSISTENT);
	}

	/**
	 * Updates ZK ensemble node stored data.
	 * 
	 * @param zk
	 *            ZooKeeper instance
	 * @param path
	 *            node path
	 * @param data
	 *            node data to set
	 * @throws KeeperException
	 *             if the server signals an error with a non-zero error code
	 * @throws InterruptedException
	 *             if the server transaction is interrupted
	 */
	public static void update(ZooKeeper zk, String path, byte[] data) throws KeeperException, InterruptedException {
		zk.setData(path, data, nodeExists(zk, path).getVersion());
	}

	/**
	 * Checks existence of node and its status, if node is available.
	 * 
	 * @param zk
	 *            ZooKeeper instance
	 * @param path
	 *            node path
	 * @return the stat of the node of the given path, or {@code null} if such node does not exists
	 * @throws KeeperException
	 *             if the server signals an error
	 * @throws InterruptedException
	 *             if the server transaction is interrupted
	 */
	public static Stat nodeExists(ZooKeeper zk, String path) throws KeeperException, InterruptedException {
		return zk.exists(path, true);
	}

	/**
	 * Deletes node from ZK ensemble.
	 * 
	 * @param zk
	 *            ZooKeeper instance
	 * @param path
	 *            node path
	 * @throws KeeperException
	 *             if the server signals an error with a non-zero return code
	 * @throws InterruptedException
	 *             if the server transaction is interrupted
	 */
	public static void delete(ZooKeeper zk, String path) throws KeeperException, InterruptedException {
		zk.delete(path, nodeExists(zk, path).getVersion());
	}

	/**
	 * Checks whether node exists in ZK ensemble by given path and creates one if it does not. Does same as
	 * {@link #ensureNodeExists(org.apache.zookeeper.ZooKeeper, String, byte[])} setting node data to {@code null}.
	 *
	 * @param zk
	 *            ZooKeeper instance
	 * @param path
	 *            node path
	 * @throws KeeperException
	 *             if the server signals an error
	 * @throws InterruptedException
	 *             if the server transaction is interrupted
	 *
	 * @see #ensureNodeExists(org.apache.zookeeper.ZooKeeper, String, byte[])
	 */
	public static void ensureNodeExists(ZooKeeper zk, String path) throws KeeperException, InterruptedException {
		ensureNodeExists(zk, path, null);
	}

	/**
	 * Checks whether node exists in ZK ensemble by given path and creates one if it does not.
	 *
	 * @param zk
	 *            ZooKeeper instance
	 * @param path
	 *            node path
	 * @param data
	 *            node data to set
	 * @throws KeeperException
	 *             if the server signals an error
	 * @throws InterruptedException
	 *             if the server transaction is interrupted
	 *
	 * @see #nodeExists(org.apache.zookeeper.ZooKeeper, String)
	 * @see #createAllNodes(org.apache.zookeeper.ZooKeeper, String, byte[])
	 */
	public static void ensureNodeExists(ZooKeeper zk, String path, byte[] data)
			throws KeeperException, InterruptedException {
		Stat nStat = nodeExists(zk, path);

		if (nStat == null) {
			createAllNodes(zk, path, data);
		}
	}

	/**
	 * Sets ZK ensemble node data. Checks whether node exists in ZK ensemble by given path. If node exists then updates
	 * node data, if does not exist - creates new node with defined data.
	 * 
	 * @param zk
	 *            ZooKeeper instance
	 * @param path
	 *            node path
	 * @param data
	 *            node data to set
	 * @throws KeeperException
	 *             if the server signals an error
	 * @throws InterruptedException
	 *             if the server transaction is interrupted
	 *
	 * @see #nodeExists(org.apache.zookeeper.ZooKeeper, String)
	 * @see #createAllNodes(org.apache.zookeeper.ZooKeeper, String, byte[])
	 * @see #update(org.apache.zookeeper.ZooKeeper, String, byte[])
	 */
	public static void setNodeData(ZooKeeper zk, String path, byte[] data)
			throws KeeperException, InterruptedException {
		Stat nStat = nodeExists(zk, path);

		if (nStat == null) {
			createAllNodes(zk, path, data);
		} else {
			update(zk, path, data);
		}
	}

	/**
	 * Creates all missing ZK ensemble nodes for provided path and sets/updates provided node data.
	 * 
	 * @param zk
	 *            ZooKeeper instance
	 * @param path
	 *            node path
	 * @param data
	 *            node data to set
	 * @throws KeeperException
	 *             if the server signals an error
	 * @throws InterruptedException
	 *             if the server transaction is interrupted
	 *
	 * @see #nodeExists(org.apache.zookeeper.ZooKeeper, String)
	 * @see #create(org.apache.zookeeper.ZooKeeper, String, byte[])
	 * @see #update(org.apache.zookeeper.ZooKeeper, String, byte[])
	 */
	public static void createAllNodes(ZooKeeper zk, String path, byte[] data)
			throws KeeperException, InterruptedException {
		String[] pNodes = path.split(PATH_DELIM);
		Stat nStat;
		String cPath = "";

		for (int i = 0; i < pNodes.length; i++) {
			String node = pNodes[i];

			if (StringUtils.isEmpty(node)) {
				continue;
			}

			cPath += PATH_DELIM + node;

			nStat = nodeExists(zk, cPath);

			if (i == pNodes.length - 1) {
				if (nStat == null) {
					create(zk, cPath, data);
				} else {
					update(zk, cPath, data);
				}
			} else {
				if (nStat == null) {
					create(zk, cPath, null);
				}
			}
		}
	}

	/**
	 * Initializes ZK ensemble node data monitoring (over {@link org.apache.zookeeper.Watcher}) and initial data
	 * loading.
	 * 
	 * @param path
	 *            node path
	 * @param zkCfgChangeListener
	 *            zookeeper node data change listener instance
	 *
	 * @throws java.io.IOException
	 *             if I/O exception occurs while initializing ZooKeeper connection
	 * @throws java.lang.InterruptedException
	 *             if the current thread is interrupted while waiting
	 * 
	 * @see #zk()
	 * @see #handleZKStoredConfiguration(org.apache.zookeeper.ZooKeeper, String, ZKConfigChangeListener)
	 */
	public static void handleZKStoredConfiguration(String path, ZKConfigChangeListener zkCfgChangeListener)
			throws IOException, InterruptedException {
		handleZKStoredConfiguration(zk(), path, zkCfgChangeListener);
	}

	/**
	 * Initializes ZK ensemble node data monitoring (over {@link org.apache.zookeeper.Watcher}) and initial data
	 * loading.
	 * 
	 * @param zk
	 *            ZooKeeper instance
	 * @param path
	 *            node path
	 * @param zkCfgChangeListener
	 *            zookeeper node data change listener instance
	 */
	public static void handleZKStoredConfiguration(ZooKeeper zk, String path,
			ZKConfigChangeListener zkCfgChangeListener) {
		Watcher watch = new Watcher() {
			@Override
			public void process(WatchedEvent watchedEvent) {
				if (path.equals(watchedEvent.getPath())) {
					if (watchedEvent.getType() == Event.EventType.NodeDataChanged) {
						LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
								"ZKConfigManager.node.changed", path);
						zkCfgChangeListener.reconfigure(zk, path, this);
					} else if (watchedEvent.getType() == Event.EventType.NodeDeleted) {
						LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
								"ZKConfigManager.node.deleted", path);
						zk.exists(path, this, null, null);
					} else if (watchedEvent.getType() == Event.EventType.NodeCreated) {
						LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
								"ZKConfigManager.node.created", path);
						zkCfgChangeListener.reconfigure(zk, path, this);
					}
				}
			}
		};

		Stat nStat = null;

		try {
			nStat = zk.exists(path, false);
		} catch (Exception exc) {
			Utils.logThrowable(LOGGER, OpLevel.WARNING,
					StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ZKConfigManager.node.exists.failed", path, exc);
		}

		if (nStat == null) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ZKConfigManager.node.create.wait", path);
			zk.exists(path, watch, null, null);
		} else {
			zkCfgChangeListener.reconfigure(zk, path, watch);
		}
	}

	/**
	 * Reads streams ZooKeeper configuration file and loads contents into {@link java.util.Properties} object.
	 *
	 * @param zookeeperCfgFile
	 *            streams zookeeper configuration file path string
	 * @return properties loaded from configuration file
	 */
	public static Properties readStreamsZKConfig(String zookeeperCfgFile) {
		if (StringUtils.isNotEmpty(zookeeperCfgFile)) {
			LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ZKConfigManager.loading.cfg.file", zookeeperCfgFile);

			try {
				zkConfigProperties = Utils.loadProperties(zookeeperCfgFile);
				LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ZKConfigManager.loaded.props", zkConfigProperties.size());
			} catch (Exception exc) {
				LOGGER.log(OpLevel.ERROR, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ZKConfigManager.props.load.error", zookeeperCfgFile);
				zkConfigProperties = null;
			}
		} else {
			if (zkConfigProperties != null) {
				LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ZKConfigManager.resetting.cfg");
				zkConfigProperties.clear();
			}
		}

		return zkConfigProperties;
	}

	/**
	 * Gets streams ZooKeeper configuration property value.
	 *
	 * @param propName
	 *            streams ZooKeeper configuration property name
	 * @return streams ZooKeeper configuration property contained value
	 */
	public static String getZKCfgProperty(String propName) {
		return zkConfigProperties == null ? null : zkConfigProperties.getProperty(propName);
	}

	/**
	 * Gets streams ZooKeeper configuration property value.
	 *
	 * @param propName
	 *            streams ZooKeeper configuration property name
	 * @param defValue
	 *            a default value
	 * @return streams ZooKeeper configuration property contained value, or {@code defValue} if no property found by
	 *         name
	 */
	public static String getZKCfgProperty(String propName, String defValue) {
		return zkConfigProperties == null ? defValue : zkConfigProperties.getProperty(propName, defValue);
	}

	/**
	 * Opens ZK ensemble connection using configuration properties defined values.
	 *
	 * @param zkConfProps
	 *            streams ZooKeeper configuration properties
	 * @return instance of opened ZK connection
	 * @throws IOException
	 *             if I/O exception occurs while initializing ZooKeeper connection
	 * @throws InterruptedException
	 *             if the current thread is interrupted while waiting
	 */
	public static ZKConnection openConnection(Properties zkConfProps) throws IOException, InterruptedException {
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"ZKConfigManager.connecting");

		if (MapUtils.isEmpty(zkConfProps)) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ZKConfigManager.empty.cfg.props");
		}

		conn = new ZKConnection();

		String zkConnStr = zkConfProps.getProperty(PROP_ZK_CONN, DEFAULT_CONN_HOST);
		int timeout = Utils.getInt(PROP_ZK_CONN_TIMEOUT, zkConfProps, DEFAULT_CONN_TIMEOUT);

		conn.connect(zkConnStr, timeout); // NON-NLS

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"ZKConfigManager.connected", zkConnStr);

		return conn;
	}

	/**
	 * Returns {@link org.apache.zookeeper.ZooKeeper} instance connection was established to. If no connection is
	 * established, then opens new connection using last read streams ZK configuration properties.
	 *
	 * @return zookeeper instance
	 * @throws IOException
	 *             if I/O exception occurs while initializing ZooKeeper connection
	 * @throws InterruptedException
	 *             if the current thread is interrupted while waiting
	 */
	public static ZooKeeper zk() throws IOException, InterruptedException {
		if (conn == null || !conn.isConnected()) {
			openConnection(zkConfigProperties);
		}

		return conn.zk();
	}

	/**
	 * Sets up ZooKeeper node data for defined configuration entity. Node path is referenced by property named
	 * {@code cfgId} + {@value com.jkoolcloud.tnt4j.streams.configure.zookeeper.ZKConfigConstants#CFG_SUFFIX_ZK_PATH}.
	 * <p>
	 * If node exists and node data is not {@code null} - does nothing.
	 * <p>
	 * If node does not exist - creates all missing ZK path nodes and sets node data from configuration file referenced
	 * by property named {@code cfgId} +
	 * {@value com.jkoolcloud.tnt4j.streams.configure.zookeeper.ZKConfigConstants#CFG_SUFFIX_CFG_FILE}.
	 * <p>
	 * If node exists, but node data is null or empty - updates node data from configuration file referenced by property
	 * named {@code cfgId} +
	 * {@value com.jkoolcloud.tnt4j.streams.configure.zookeeper.ZKConfigConstants#CFG_SUFFIX_CFG_FILE}.
	 *
	 * @param zkConfProps
	 *            streams ZooKeeper configuration properties
	 * @param cfgId
	 *            configuration entity identifier
	 * @throws IOException
	 *             if I/O exception occurs while initializing ZooKeeper connection
	 * @throws InterruptedException
	 *             if the current thread is interrupted while waiting
	 * @throws KeeperException
	 *             if the server signals an error
	 *
	 * @see #nodeExists(org.apache.zookeeper.ZooKeeper, String)
	 * @see #createAllNodes(org.apache.zookeeper.ZooKeeper, String, byte[])
	 * @see #update(org.apache.zookeeper.ZooKeeper, String, byte[])
	 * @see #makeCfgFilePathProperty(String)
	 * @see #makeZKNodePathProperty(String)
	 */
	public static void setupZKNodeData(Properties zkConfProps, String cfgId)
			throws IOException, InterruptedException, KeeperException {
		String zkPath = getZKNodePath(zkConfProps, cfgId);
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"ZKConfigManager.cfg.ent.setup.zk.path", cfgId, zkPath);
		if (StringUtils.isEmpty(zkPath)) {
			LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ZKConfigManager.cfg.ent.setup.zk.path.empty", cfgId);

			return;
		}

		Stat nStat = nodeExists(zk(), zkPath);

		if (nStat == null || nStat.getDataLength() <= 0) {
			String cfgFile = getCfgFilePath(zkConfProps, cfgId);
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ZKConfigManager.cfg.ent.setup.file.path", cfgId, cfgFile);
			byte[] cfgData = ZKConfigInit.loadDataFromFile(cfgFile);

			if (nStat == null) {
				LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ZKConfigManager.cfg.ent.setup.setting", cfgId, cfgFile);
				createAllNodes(zk(), zkPath, cfgData);
			} else {
				LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ZKConfigManager.cfg.ent.setup.updating", cfgId, cfgFile);
				if (cfgData != null) {
					update(zk(), zkPath, cfgData);
				}
			}
		} else {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ZKConfigManager.cfg.ent.setup.ok", cfgId);
		}
	}

	/**
	 * Makes configuration entity ZK node path property name: {@code cfgId} +
	 * {@value com.jkoolcloud.tnt4j.streams.configure.zookeeper.ZKConfigConstants#CFG_SUFFIX_ZK_PATH}.
	 *
	 * @param cfgId
	 *            configuration entity identifier
	 * @return configuration entity ZK node path property name
	 */
	public static String makeZKNodePathProperty(String cfgId) {
		return cfgId + CFG_SUFFIX_ZK_PATH;
	}

	/**
	 * Makes configuration entity configuration file path property name: {@code cfgId} +
	 * {@value com.jkoolcloud.tnt4j.streams.configure.zookeeper.ZKConfigConstants#CFG_SUFFIX_CFG_FILE}.
	 *
	 * @param cfgId
	 *            configuration entity identifier
	 * @return configuration entity configuration file path property name
	 */
	public static String makeCfgFilePathProperty(String cfgId) {
		return cfgId + CFG_SUFFIX_CFG_FILE;
	}

	/**
	 * Finds and returns configuration entity ZK node path defined in streams ZooKeeper configuration properties.
	 *
	 * @param zkConfProps
	 *            streams ZooKeeper configuration properties
	 * @param cfgId
	 *            configuration entity identifier
	 * @return configuration entity ZK node path
	 */
	public static String getZKNodePath(Properties zkConfProps, String cfgId) {
		return zkConfProps == null ? null : zkConfProps.getProperty(makeZKNodePathProperty(cfgId));
	}

	/**
	 * Finds and returns configuration entity configuration file path defined in streams ZooKeeper configuration
	 * properties.
	 *
	 * @param zkConfProps
	 *            streams ZooKeeper configuration properties
	 * @param cfgId
	 *            configuration entity identifier
	 * @return configuration entity configuration file path
	 */
	public static String getCfgFilePath(Properties zkConfProps, String cfgId) {
		return zkConfProps == null ? null : zkConfProps.getProperty(makeCfgFilePathProperty(cfgId));
	}

	/**
	 * Closes ZK ensemble connection.
	 */
	public static void close() {
		Utils.close(conn);
	}

	/**
	 * Abstract ZooKeeper stored configuration change listener.
	 */
	public abstract static class ZKConfigChangeListener {
		/**
		 * Invoked when ZK node kept configuration changes or node under defined path gets created.
		 *
		 * @param zk
		 *            ZooKeeper instance
		 * @param path
		 *            node path
		 * @param watcher
		 *            node watcher instance
		 */
		public void reconfigure(ZooKeeper zk, String path, Watcher watcher) {
			LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ZKConfigManager.loading.node.data", path);

			try {
				byte[] data = zk.getData(path, watcher, null);

				applyConfigurationData(data);
			} catch (Exception exc) {
				Utils.logThrowable(LOGGER, OpLevel.WARNING,
						StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ZKConfigManager.cfg.loading.failed", exc);
			}
		}

		/**
		 * Defines ZK node retrieved configuration data processing.
		 *
		 * @param data
		 *            data retrieved from ZK ensemble node
		 */
		public abstract void applyConfigurationData(byte[] data);
	}
}