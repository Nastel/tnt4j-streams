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

package com.jkoolcloud.tnt4j.streams.configure;

/**
 * Lists predefined property names used by TNT4-Streams Elastic Beats input streams.
 *
 * @version $Revision: 1 $
 */
public interface ElasticBeatsStreamProperties extends StreamProperties {

	/**
	 * Constant for name of built-in stream {@value} property.
	 */
	String PROP_SSL_CERTIFICATE_FILE_PATH = "SSLCertificateFilePath"; // NON-NLS
	/**
	 * Constant for name of built-in stream {@value} property.
	 */
	String PROP_SSL_KEY_FILE_PATH = "SSLKeyFilePath"; // NON-NLS
	/**
	 * Constant for name of built-in stream {@value} property.
	 */
	String PROP_PASSPHRASE = "PassPhrase"; // NON-NLS
	/**
	 * Constant for name of built-in stream {@value} property.
	 */
	String PROP_TIMEOUT = "Timeout"; // NON-NLS
	/**
	 * Constant for name of built-in stream {@value} property.
	 */
	String PROP_THREAD_COUNT = "ThreadCount"; // NON-NLS
}
