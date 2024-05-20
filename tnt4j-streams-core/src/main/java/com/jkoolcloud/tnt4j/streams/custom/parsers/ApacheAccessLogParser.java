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

package com.jkoolcloud.tnt4j.streams.custom.parsers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityRegExParser;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * Implements a custom Apache access log parser based on RegEx parsing. User can define RegEx string to parse log as for
 * ordinary activity RegEx parser.
 * <p>
 * But it is also possible to use Apache access log configuration pattern over LogPattern parameter. Then RegEx is
 * generated from it. Additional config pattern tokens may be mapped to RegEx'es using ConfRegexMapping parameters.
 * <p>
 * This parser supports the following configuration properties (in addition to those supported by
 * {@link ActivityRegExParser}):
 * <ul>
 * <li>LogPattern - access log pattern. (Optional, if RegEx {@link Pattern} property is defined)</li>
 * <li>ConfRegexMapping - custom log pattern token (must be wildcard or RegEx compliant string) and RegEx mapping.
 * (Optional, actual only if {@code LogPattern} property is used)</li>
 * </ul>
 * <p>
 * RegEx group names and log pattern tokens mapping:
 * <table>
 * <caption>RegEx group names and log pattern tokens mapping</caption>
 * <tr><th>RegEx group name</th><th>Format String</th><th>Description</th></tr>
 * <tr><td>address</td><td>%a</td><td>Client IP address of the request (see the mod_remoteip module).</td></tr>
 * <tr><td>localAddress</td><td>%A</td><td>Local IP-address.</td></tr>
 * <tr><td>size</td><td>%B</td><td>Size of response in bytes, excluding HTTP headers.</td></tr>
 * <tr><td>sizeClf</td><td>%b</td><td>Size of response in bytes, excluding HTTP headers. In CLF format, e.g., a '-' rather than a 0 when no bytes are
 * sent.</td></tr>
 * <tr><td>cookie</td><td>%{VARNAME}C</td><td>The contents of cookie VARNAME in the request sent to the server. Only version 0 cookies are fully
 * supported.</td></tr>
 * <tr><td>reqTime</td><td>%D</td><td>The time taken to serve the request, in microseconds.</td></tr>
 * <tr><td>envVariable</td><td>%{VARNAME}e</td><td>The contents of the environment variable VARNAME.</td></tr>
 * <tr><td>filename</td><td>%f</td><td>Filename.</td></tr>
 * <tr><td>hostname</td><td>%h</td><td>Remote hostname. Will log the IP address if HostnameLookups is set to Off, which is the default. If it logs the
 * hostname for only a few hosts, you probably have access control directives mentioning them by name. See the Require
 * host documentation.</td></tr>
 * <tr><td>protocol</td><td>%H</td><td>The request protocol.</td></tr>
 * <tr><td>variable</td><td>%{VARNAME}i</td><td>The contents of VARNAME: header line(s) in the request sent to the server. Changes made by other modules
 * (e.g. mod_headers) affect this. If you're interested in what the request header was prior to when most modules would
 * have modified it, use mod_setenv if to copy the header into an internal environment variable and log that value with
 * the %{VARNAME}edescribed above.</td></tr>
 * <tr><td>keepAlive</td><td>%k</td><td>Number of keepalive requests handled on this connection. Interesting if KeepAlive is being used, so that, for
 * example, a '1' means the first keepalive request after the initial one, '2' the second, etc...; otherwise this is
 * always 0 (indicating the initial request).</td></tr>
 * <tr><td>logname</td><td>%l</td><td>Remote logname (from identd, if supplied). This will return a dash unless mod_ident is present
 * and IdentityCheck is set On.</td></tr>
 * <tr><td>method</td><td>%m</td><td>The request method.</td></tr>
 * <tr><td>note</td><td>%{VARNAME}n</td><td>The contents of note VARNAME from another module.</td></tr>
 * <tr><td>headerVar</td><td>%{VARNAME}o</td><td>The contents of VARNAME: header line(s) in the reply.</td></tr>
 * <tr><td>port</td><td>%{format}p</td><td>The canonical port of the server serving the request, or the server's actual port, or the client's actual port.
 * Valid formats are canonical, local, or remote.</td></tr>
 * <tr><td>process</td><td>%{format}P</td><td>The process ID or thread ID of the child that serviced the request. Valid formats are pid, tid,
 * and hextid. hextid requires APR 1.2.0 or higher.</td></tr>
 * <tr><td>query</td><td>%q</td><td>The query string (prepended with a ? if a query string exists, otherwise an empty string).</td></tr>
 * <tr><td>line</td><td>%r</td><td>First line of request.</td></tr>
 * <tr><td>respHandler</td><td>%R</td><td>The handler generating the response (if any).</td></tr>
 * <tr><td>status</td><td>%s</td><td>Status. For requests that have been internally redirected, this is the status of the original request.
 * Use %&gt;s for the final status.</td></tr>
 * <tr><td>time</td><td>%{format}t</td><td>The time, in the form given by format, which should be in an extended strftime(3) format (potentially localized).
 * If the format starts with begin: (default) the time is taken at the beginning of the request processing. If it starts
 * with end: it is the time when the log entry gets written, close to the end of the request processing. In addition to
 * the formats supported by strftime(3), the following format tokens are supported:</td></tr>
 * <tr><td>reqTimeExt</td><td>%{UNIT}T</td><td>The time taken to serve the request, in a time unit given by UNIT. Valid units are ms for milliseconds, us for
 * microseconds, and s for seconds. Using s gives the same result as %Twithout any format; using us gives the same
 * result as %D. Combining %T with a unit is available in 2.4.13 and later.</td></tr>
 * <tr><td>user</td><td>%u</td><td>Remote user if the request was authenticated. May be bogus if return status (%s) is 401 (unauthorized).</td></tr>
 * <tr><td>uri</td><td>%U</td><td>The URL path requested, not including any query string.</td></tr>
 * <tr><td>serverName</td><td>%v</td><td>The canonical ServerName of the server serving the request.</td></tr>
 * <tr><td>serverNameExt</td><td>%V</td><td>The server name according to the UseCanonicalName setting.</td></tr>
 * <tr><td>conStatus</td><td>%X</td><td>Connection status when response is completed:</td></tr>
 * <tr><td>received</td><td>%I</td><td>Bytes received, including request and headers. Cannot be zero. You need to enable mod_logio to use this.</td></tr>
 * <tr><td>sent</td><td>%O</td><td>Bytes sent, including headers. May be zero in rare cases such as when a request is aborted before a response is
 * sent. You need to enable mod_logio to use this.</td></tr>
 * </table>
 * <p>
 * NOTE: See {@link Pattern} documentation section "Group name" regarding groups naming conventions!
 *
 * @version $Revision: 2 $
 */
public class ApacheAccessLogParser extends ActivityRegExParser {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(ApacheAccessLogParser.class);

	/**
	 * Constant for name of built-in {@value} property.
	 */
	protected static final String PROP_APACHE_LOG_PATTERN = "LogPattern"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	protected static final String PROP_CONF_REGEX_MAPPING = "ConfRegexMapping"; // NON-NLS

	private static final String APACHE_LOG_CONFIG_TOKEN_REPLACEMENT_REGEX = "%\\S*(%|\\w)"; // NON-NLS

	private static final String DEFAULT_LOG_TOKEN_REGEX = "(\\S+)"; // NON-NLS
	private static final String STATUS_LOG_TOKEN_REGEX = "(?<status>\\d{3})"; // NON-NLS
	private static final String REQUEST_LOG_TOKEN_REGEX = "(?<request>((?<method>\\S+) (?<uri>.*?)( (?<version>\\S+))?)|(-))"; // NON-NLS

	// static final String REGEX_TOKENS = "(?<address>\\S+)
	// (?<user>.*?)\\[(?<when>.*?)\\] \"(?<request>.*?)\"
	// (?<status>[\\d\\-]+)(?<length>[\\d\\-]+) \"(?<referer>.*?)\"
	// \"(?<agent>.*?)\".*";
	//
	// static final String REGEX_CLIENT_REQUEST =
	// "(?<method>\\S+)\\s+?(?<uri>.*?)\\s+?(?<version>HTTP.*)";

	/**
	 * Apache access log configuration pattern string.
	 */
	protected String apacheLogPattern = null;

	/**
	 * Pre configured mappings between Apache access log configuration pattern token strings and RegEx strings used to
	 * parse log entries.
	 */
	protected final Map<String, String> configRegexMappings = new HashMap<>();
	/**
	 * User defined mappings between Apache access log configuration pattern token strings and RegEx strings used to
	 * parse log entries.
	 */
	protected final Map<String, String> userRegexMappings = new HashMap<>();

	/**
	 * Constructs a new ApacheAccessLogParser.
	 */
	public ApacheAccessLogParser() {
		fillDefaultConfigRegexMappings();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	/**
	 * Fills default Apache access log configuration to RegEx mappings.
	 */
	private void fillDefaultConfigRegexMappings() {
		configRegexMappings.put("%%", "%"); // NON-NLS %% The percent sign.
		configRegexMappings.put("%a", makeNamedGroup("address")); // NON-NLS
		configRegexMappings.put("%A", makeNamedGroup("localAddress")); // NON-NLS
		configRegexMappings.put("%B", makeNamedGroup("size")); // NON-NLS
		configRegexMappings.put("%b", "(?<sizeClf>\\d+|-)"); // NON-NLS
		configRegexMappings.put("%*C", makeNamedGroup("cookie")); // NON-NLS
		configRegexMappings.put("%*D", makeNamedGroup("reqTime")); // NON-NLS
		configRegexMappings.put("%*e", makeNamedGroup("envVariable")); // NON-NLS
		configRegexMappings.put("%f", makeNamedGroup("filename")); // NON-NLS
		configRegexMappings.put("%h", makeNamedGroup("hostname")); // NON-NLS
		configRegexMappings.put("%H", makeNamedGroup("protocol")); // NON-NLS
		configRegexMappings.put("%*i", "(?<variable>.*?)"); // NON-NLS
		configRegexMappings.put("%k", makeNamedGroup("keepAlive")); // NON-NLS
		configRegexMappings.put("%l", makeNamedGroup("logname")); // NON-NLS
		configRegexMappings.put("%m", makeNamedGroup("method")); // NON-NLS
		configRegexMappings.put("%*n", makeNamedGroup("note")); // NON-NLS
		configRegexMappings.put("%*o", makeNamedGroup("headerVar")); // NON-NLS
		configRegexMappings.put("%*p", makeNamedGroup("port")); // NON-NLS
		configRegexMappings.put("%*P", makeNamedGroup("process")); // NON-NLS
		configRegexMappings.put("%q", makeNamedGroup("query")); // NON-NLS
		configRegexMappings.put("%*r", REQUEST_LOG_TOKEN_REGEX); // NON-NLS
		configRegexMappings.put("%R", makeNamedGroup("respHandler")); // NON-NLS
		configRegexMappings.put("%*s", STATUS_LOG_TOKEN_REGEX); // NON-NLS
		configRegexMappings.put("%*t", "\\[(?<time>[\\w:/]+\\s[+\\-]\\d{4})\\]"); // NON-NLS
		configRegexMappings.put("%*T", makeNamedGroup("reqTimeExt")); // NON-NLS
		configRegexMappings.put("%*u", makeNamedGroup("user")); // NON-NLS
		configRegexMappings.put("%*U", makeNamedGroup("uri")); // NON-NLS
		configRegexMappings.put("%v", makeNamedGroup("serverName")); // NON-NLS
		configRegexMappings.put("%V", makeNamedGroup("serverNameExt")); // NON-NLS
		configRegexMappings.put("%X", makeNamedGroup("conStatus")); // NON-NLS
		configRegexMappings.put("%I", makeNamedGroup("received")); // NON-NLS
		configRegexMappings.put("%O", makeNamedGroup("sent")); // NON-NLS
	}

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) {
		super.setProperties(props);

		if (pattern == null && StringUtils.isNotEmpty(apacheLogPattern)) {
			String regex = makeRegexPattern(apacheLogPattern);
			if (regex != null) {
				pattern = Pattern.compile(regex);
				logger().log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ApacheAccessLogParser.regex.made", getName(), regex);
			} else {
				logger().log(OpLevel.WARNING, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ApacheAccessLogParser.could.not.make.regex", getName(), apacheLogPattern);
			}
		}
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (PROP_APACHE_LOG_PATTERN.equalsIgnoreCase(name)) {
			if (StringUtils.isNotEmpty(value)) {
				apacheLogPattern = value;
				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityParser.setting", name, value);
			}
		} else if (PROP_CONF_REGEX_MAPPING.equalsIgnoreCase(name)) {
			if (StringUtils.isNotEmpty(value)) {
				String[] uMappings = StreamsConstants.getMultiProperties(value);
				for (String uMapping : uMappings) {
					int idx = uMapping.indexOf('=');
					if (idx > 0) {
						String confKey = uMapping.substring(0, idx);
						String regex = uMapping.substring(idx + 1);

						String oldRegex = userRegexMappings.put(confKey, regex);
						logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
								"ActivityParser.setting", name, uMapping);
						logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
								"ApacheAccessLogParser.setting.regex.mapping", confKey, oldRegex, regex);
					}
				}
			}
		}
	}

	@Override
	public Object getProperty(String name) {
		if (PROP_APACHE_LOG_PATTERN.equalsIgnoreCase(name)) {
			return apacheLogPattern;
		}
		if (PROP_CONF_REGEX_MAPPING.equalsIgnoreCase(name)) {
			return userRegexMappings;
		}

		return super.getProperty(name);
	}

	/**
	 * Makes log entry parsing RegEx from defined Apache access log configuration pattern string.
	 *
	 * @param apacheLogPattern
	 *            Apache access log configuration pattern string
	 *
	 * @return regular expression string, or {@code null} if can't make RegEx string from defined Apache access log
	 *         configuration pattern string
	 */
	private String makeRegexPattern(String apacheLogPattern) {
		Pattern pattern = Pattern.compile(APACHE_LOG_CONFIG_TOKEN_REPLACEMENT_REGEX);
		Matcher matcher = pattern.matcher(apacheLogPattern);
		StringBuilder logRegexBuff = new StringBuilder();
		int pos = 0;
		while (matcher.find()) {
			logRegexBuff.append(apacheLogPattern, pos, matcher.start());
			logRegexBuff.append(mapConfigTokenToRegex(matcher.group()));
			pos = matcher.end();
		}

		if (pos < apacheLogPattern.length()) {
			logRegexBuff.append(apacheLogPattern.substring(pos, apacheLogPattern.length()));
		}

		String logRegex = logRegexBuff.toString().trim();
		// return logRegex.isEmpty() ? null : "(?m)^" + logRegex;
		return logRegex.isEmpty() ? null : '^' + logRegex; // NON-NLS
	}

	/**
	 * Maps Apache access log configuration pattern token to user defined RegEx string. If no user defined mapping is
	 * found, then default mapping is used.
	 *
	 * @param configToken
	 *            Apache access log configuration pattern token string
	 *
	 * @return RegEx string matching configuration token
	 */
	private String mapConfigTokenToRegex(String configToken) {
		String confRegexMapping = findMapping(configToken);
		if (confRegexMapping != null) {
			return confRegexMapping;
		}

		return mapConfigTokenRegexDefault(configToken);
	}

	/**
	 * Finds user defined mapping of Apache access log configuration pattern token to RegEx string.
	 *
	 * @param configToken
	 *            Apache access log configuration pattern token string
	 *
	 * @return mapped RegEx string
	 */
	private String findMapping(String configToken) {
		String regex = findRegexMapping(configToken, userRegexMappings);

		if (StringUtils.isEmpty(regex)) {
			regex = findRegexMapping(configToken, configRegexMappings);
		}

		return StringUtils.isEmpty(regex) ? null : regex;
	}

	private static String findRegexMapping(String configToken, Map<String, String> regexMappings) {
		if (regexMappings != null) {
			for (Map.Entry<String, String> e : regexMappings.entrySet()) {
				if (isMatchingPattern(configToken, e.getKey())) {
					return e.getValue();
				}
			}
		}

		return null;
	}

	/**
	 * Returns default RegEx string for defined Apache access log configuration token.
	 *
	 * @param configToken
	 *            Apache access log configuration token string
	 *
	 * @return default RegEx string for configuration token
	 */
	private static String mapConfigTokenRegexDefault(String configToken) {
		if (isMatchingPattern(configToken, "%*s")) { // NON-NLS
			return STATUS_LOG_TOKEN_REGEX;
		} else if (isMatchingPattern(configToken, "%*r")) { // NON-NLS
			return REQUEST_LOG_TOKEN_REGEX;
		}

		return DEFAULT_LOG_TOKEN_REGEX;
	}

	/**
	 * Checks if Apache access log configuration token matches defined pattern.
	 *
	 * @param configToken
	 *            Apache access log configuration token string
	 * @param pattern
	 *            pattern string to match configuration token
	 *
	 * @return {@code true} if Apache access log configuration token matches defined pattern
	 */
	private static boolean isMatchingPattern(String configToken, String pattern) {
		String p = pattern.replace("*", "\\S*"); // NON-NLS

		return configToken.matches(p);
	}

	private static String makeNamedGroup(String name) {
		return "(?<" + name + ">\\S+)"; // NON-NLS
	}
}
