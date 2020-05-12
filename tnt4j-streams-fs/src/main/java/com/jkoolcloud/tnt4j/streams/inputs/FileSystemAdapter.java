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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.github.robtimus.filesystems.sftp.SFTPEnvironment;
import com.jkoolcloud.tnt4j.streams.configure.FsStreamProperties;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.StreamsConstants;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.pastdev.jsch.DefaultSessionFactory;
import com.pastdev.jsch.nio.file.UnixSshFileSystemProvider;

/**
 * Utility class helping to configure JSR-203 compliant {@link java.nio.file.FileSystems} and returning reference of
 * {@link java.nio.file.FileSystem} to be used by streams accessing file system provided files.
 * <p>
 * Currently adapter supports these file systems:
 * <ul>
 * <li>SCP - schemes {@code "ssh.unix:"}, {@code "scp:"} and {@code "ssh:"}</li>
 * <li>SFTP - scheme {@code "sftp:"}</li>
 * <li>ZIP - schemes {@code "zip:"} and {@code "jar:"}</li>
 * <li>FILE - scheme {@code "file:"}</li>
 * <li>any other JSR-203 compliant (requires manually add file system implementing libs to classpath), but those are not
 * tested and may not work "out of the box"</li>
 * </ul>
 * <p>
 * File system adapter supports the following configuration properties:
 * <ul>
 * <li>Host - remote machine host name/IP address. Default value - {@code "localhost"}. (Optional - can be defined over
 * stream property 'FileName', e.g. {@code 'ftp://username:password@hostname:port/[FILE_PATH]'})</li>
 * <li>Port - remote machine port number to accept connection. (Optional - can be defined over stream property
 * 'FileName', e.g. {@code 'ftp://username:password@hostname:port/[FILE_PATH]'})</li>
 * <li>UserName - remote machine authorized user name. (Optional - can be defined over stream property 'FileName', e.g.
 * {@code 'ftp://username:password@hostname:port/[FILE_PATH]'})</li>
 * <li>Password - remote machine authorized user password. (Optional - can be defined over stream property 'FileName',
 * e.g. {@code 'ftp://username:password@hostname:port/[FILE_PATH]'})</li>
 * <li>Scheme - file access protocol scheme name. Default value - {@code "scp"}. (Optional - can be defined over stream
 * property 'FileName', e.g. {@code 'ssh.unix:///[FILE_PATH]'})</li>
 * <li>StrictHostKeyChecking - flag indicating whether strictly check add remote host keys changes against ssh know
 * hosts.
 * <ul>
 * <li>If flag value is set to 'yes', ssh will never automatically add host keys to the '~/.ssh/known_hosts' file and
 * will refuse to connect to a host whose host key has changed. This provides maximum protection against trojan horse
 * attacks, but can be troublesome when the '/etc/ssh/ssh_known_hosts' file is poorly maintained or connections to new
 * hosts are frequently made. This option forces the user to manually add all new hosts.</li>
 * <li>If flag value is set to 'no', ssh will automatically add new host keys to the user known hosts files.</li>
 * </ul>
 * The host keys of known hosts will be verified automatically in all cases. The flag value must be set to 'yes' or
 * 'no'. Default value - {@code "no"}. (Optional)</li>
 * <li>IdentityFromPrivateKey - private key file to be used for remote machine secure connection initialization.
 * (Optional)</li>
 * <li>KnownHosts - known hosts file path. (Optional)</li>
 * <li>ResolveAbsolutePath - flag indicating whether to resolve absolute file path when relative one is provided.
 * Default value - {@code false}. (Optional)</li>
 * <li>set of target {@link java.nio.file.spi.FileSystemProvider} supported properties</li>
 * </ul>
 *
 * @version $Revision: 1 $
 *
 * @see java.nio.file.FileSystem
 * @see java.nio.file.FileSystems
 * @see com.jkoolcloud.tnt4j.streams.inputs.FileSystemBytesInputStream
 * @see com.jkoolcloud.tnt4j.streams.inputs.FileSystemCharacterStream
 * @see com.jkoolcloud.tnt4j.streams.inputs.FileSystemLineStream
 */
class FileSystemAdapter {

	/**
	 * Constant defining JAR scheme prefix.
	 */
	protected static final String JAR_SCHEME = "jar"; // NON-NLS

	private static final String ZIP_PATH_SEPARATOR = "!"; // NON-NLS
	private static final String PATH_DELIM = "/"; // NON-NLS
	private static final String RELATIVE_PATH_SECTION = "./"; // NON-NLS
	private static final Pattern LEADING_PATH_SLASHES_PATTERN = Pattern.compile("^/+(?=./)"); // NON-NLS

	private static final String STRICT_HOST_KEY_CHECKING = "StrictHostKeyChecking"; // NON-NLS

	private String host = "localhost"; // NON-NLS
	private int port = -1;
	private String user;
	private String password;
	private String scheme = "scp"; // NON-NLS
	private String strictHostChecking = "no"; // NON-NLS
	private String knownHosts;
	private String privateKey;
	private boolean resolveAbsolutePath = false;

	private URI fileURI;
	private Map<String, Object> envConfiguration = new HashMap<>();

	FileSystemAdapter() {
	}

	/**
	 * Sets given configuration properties.
	 *
	 * @param props
	 *            configuration properties to set
	 *
	 * @see #setProperty(String, String)
	 */
	public void setProperties(Collection<Map.Entry<String, String>> props) {
		if (CollectionUtils.isNotEmpty(props)) {
			for (Map.Entry<String, String> prop : props) {
				setProperty(prop.getKey(), prop.getValue());
			}
		}
	}

	/**
	 * Sets given configuration property.
	 * 
	 * @param name
	 *            property name
	 * @param value
	 *            property value
	 *
	 * @throws java.lang.IllegalArgumentException
	 *             if configuration property can't be applied
	 *
	 * @see #setProperties(java.util.Collection)
	 */
	protected void setProperty(String name, String value) {
		if (StreamProperties.PROP_PASSWORD.equalsIgnoreCase(name)) {
			password = TNTInputStream.decPassword(value);
		} else if (StreamProperties.PROP_HOST.equalsIgnoreCase(name)) {
			host = value;
		} else if (StreamProperties.PROP_USERNAME.equalsIgnoreCase(name)) {
			user = value;
		} else if (StreamProperties.PROP_PORT.equalsIgnoreCase(name)) {
			port = Integer.parseInt(value);
		} else if (FsStreamProperties.PROP_SCHEME.equalsIgnoreCase(name)) {
			scheme = value;
		} else if (FsStreamProperties.PROP_STRICT_HOST_KEY_CHECKING.equalsIgnoreCase(name)) {
			strictHostChecking = value;
		} else if (FsStreamProperties.PROP_PRIVATE_KEY.equalsIgnoreCase(name)) {
			privateKey = value;
		} else if (FsStreamProperties.PROP_KNOWN_HOSTS.equalsIgnoreCase(name)) {
			knownHosts = value;
		} else if (FsStreamProperties.PROP_RESOLVE_ABSOLUTE_PATH.equalsIgnoreCase(name)) {
			resolveAbsolutePath = Utils.toBoolean(value);
		} else if (!StreamsConstants.isStreamCfgProperty(name, FsStreamProperties.class)) {
			envConfiguration.put(name, TNTInputStream.decPassword(value));
		}
	}

	/**
	 * Get value of specified configuration property.
	 *
	 * @param name
	 *            name of property whose value is to be retrieved
	 * @return value for property, or {@code null} if property does not exist
	 *
	 * @see #setProperty(String, String)
	 */
	public Object getProperty(String name) {
		if (StreamProperties.PROP_PASSWORD.equalsIgnoreCase(name)) {
			return TNTInputStream.encPassword(password);
		}
		if (StreamProperties.PROP_HOST.equalsIgnoreCase(name)) {
			return host;
		}
		if (StreamProperties.PROP_USERNAME.equalsIgnoreCase(name)) {
			return user;
		}
		if (StreamProperties.PROP_PORT.equalsIgnoreCase(name)) {
			return port;
		}
		if (FsStreamProperties.PROP_SCHEME.equalsIgnoreCase(name)) {
			return scheme;
		}
		if (FsStreamProperties.PROP_STRICT_HOST_KEY_CHECKING.equalsIgnoreCase(name)) {
			return strictHostChecking;
		}
		if (FsStreamProperties.PROP_PRIVATE_KEY.equalsIgnoreCase(name)) {
			return privateKey;
		}
		if (FsStreamProperties.PROP_KNOWN_HOSTS.equalsIgnoreCase(name)) {
			return knownHosts;
		}
		if (FsStreamProperties.PROP_RESOLVE_ABSOLUTE_PATH.equalsIgnoreCase(name)) {
			return resolveAbsolutePath;
		}

		Object envPropValue = envConfiguration.get(name);

		if (envPropValue != null) {
			return envPropValue;
		}

		return null;
	}

	/**
	 * Builds file {@link URI} from provided file name and configuration properties.
	 *
	 * @param fileName
	 *            file descriptor (path or URI) string used to initialize file system and build file {@link URI}
	 * @return file system adapted file name
	 * @throws Exception
	 *             if an error occurs initializing file system or building file {@link URI}
	 */
	public String initialize(String fileName) throws Exception {
		URI tmpURI;

		try {
			tmpURI = URI.create(fileName);
			if (StringUtils.isNotEmpty(tmpURI.getScheme())) {
				scheme = tmpURI.getScheme();
			}
			if (StringUtils.isEmpty(user)) {
				user = tmpURI.getUserInfo();
			}
			if (StringUtils.isNotEmpty(tmpURI.getHost())) {
				host = tmpURI.getHost();
			}
			if (port == -1) {
				port = tmpURI.getPort();
			}
			fileName = StringUtils.isEmpty(tmpURI.getPath()) ? tmpURI.getSchemeSpecificPart() : tmpURI.getPath();
		} catch (IllegalArgumentException e) {
			if (StringUtils.isEmpty(user) && StringUtils.isEmpty(host)) {
				throw e;
			}
		}

		switch (scheme) {
		case "ssh.unix": // NON-NLS
		case "scp": // NON-NLS
		case "ssh": // NON-NLS
			fileURI = new URI(UnixSshFileSystemProvider.SCHEME_SSH_UNIX, user, host, port == -1 ? 22 : port, fileName,
					null, null); // NON-NLS

			DefaultSessionFactory defaultSessionFactory = new DefaultSessionFactory(user, host, port);
			defaultSessionFactory.setPassword(password);
			defaultSessionFactory.setConfig(STRICT_HOST_KEY_CHECKING, strictHostChecking); // NON-NLS
			if (StringUtils.isNotEmpty(knownHosts)) {
				defaultSessionFactory.setKnownHosts(knownHosts);
			}
			if (StringUtils.isNotEmpty(privateKey)) {
				defaultSessionFactory.setIdentityFromPrivateKey(privateKey);
			}

			envConfiguration.put("defaultSessionFactory", defaultSessionFactory); // NON-NLS
			break;
		case "sftp": // NON-NLS
			// SFTPEnvironment throws exception if user or path exist in URI
			fileURI = new URI(scheme, null, host, port == -1 ? 22 : port, null, null, null);
			envConfiguration.putAll(new SFTPEnvironment().withConfig(STRICT_HOST_KEY_CHECKING, strictHostChecking)
					.withUsername(user).withPassword(password.toCharArray()));
			break;
		case "zip": // NON-NLS
		case "jar": // NON-NLS
			fileURI = new URI(JAR_SCHEME, fileName, null);
			String[] zipPathTokens = fileURI.getSchemeSpecificPart().split(ZIP_PATH_SEPARATOR);
			if (zipPathTokens.length == 2) {
				fileName = zipPathTokens[1];
			} else {
				fileName = PATH_DELIM;
			}
			if (zipPathTokens[0].contains(RELATIVE_PATH_SECTION)) {
				// now convert relative file system path to absolute
				fileURI = makeAbsoluteURI(JAR_SCHEME, zipPathTokens[0],
						zipPathTokens.length > 1 ? ZIP_PATH_SEPARATOR + zipPathTokens[1] : null);
			}
			break;
		case "file": // NON-NLS
			break;
		default:
			fileURI = URI.create(fileName);
			if (resolveAbsolutePath) {
				fileURI = makeAbsoluteURI(fileURI, null);
			}
			break;
		}

		return fileName;
	}

	/**
	 * Returns reference to a {@link FileSystem}:
	 * <ul>
	 * <li>JVM installed and accessible using built file {@link URI} over
	 * {@link java.nio.file.FileSystems#getFileSystem(java.net.URI)}</li>
	 * <li>new one, created using built file {@link URI} and file system configuration properties map using
	 * {@link java.nio.file.FileSystems#newFileSystem(java.net.URI, java.util.Map)}</li>
	 * <li>default JVM file system referenced over {@link java.nio.file.FileSystems#getDefault()}, if file {@link URI}
	 * is {@code null}</li>
	 * </ul>
	 *
	 * @return file system accessible using configuration provided parameters and built file {@link URI}
	 * @throws Exception
	 *             if an error occurs referencing or creating the file system
	 *
	 * @see java.nio.file.FileSystems#getDefault()
	 * @see java.nio.file.FileSystems#getFileSystem(java.net.URI)
	 * @see java.nio.file.FileSystems#newFileSystem(java.net.URI, java.util.Map)
	 */
	public FileSystem getFileSystem() throws Exception {
		if (fileURI == null) {
			return FileSystems.getDefault();
		}

		try {
			return FileSystems.getFileSystem(fileURI);
		} catch (Exception exc) {
			return FileSystems.newFileSystem(fileURI, envConfiguration);
		}
	}

	/**
	 * Returns file URI built by {@link #initialize(String)} call using configuration parameters and provided file name.
	 *
	 * @return file URI
	 *
	 * @see #initialize(String)
	 */
	public URI getFileURI() {
		return fileURI;
	}

	private static URI makeAbsoluteURI(String scheme, String relFilePath, String ext) throws URISyntaxException {
		URI tmpURI = URI.create(relFilePath);
		String absolutePath = getAbsoluteFilePath(tmpURI);
		return new URI(scheme == null ? tmpURI.getScheme() : scheme, absolutePath + (ext == null ? "" : ext), null);
	}

	private static URI makeAbsoluteURI(URI relFileURI, String ext) throws URISyntaxException {
		String absolutePath = getAbsoluteFilePath(relFileURI);
		return new URI(relFileURI.getScheme(), absolutePath + (ext == null ? "" : ext), null);
	}

	private static String getAbsoluteFilePath(URI relFileURI) throws URISyntaxException {
		String filePath = relFileURI.getPath();
		if (filePath == null) {
			filePath = relFileURI.getSchemeSpecificPart();
		}
		// cleanup leading slashes from relative file path
		filePath = LEADING_PATH_SLASHES_PATTERN.matcher(filePath).replaceAll("");
		File file = new File(filePath);
		return file.toURI().normalize().toString();
	}
}
