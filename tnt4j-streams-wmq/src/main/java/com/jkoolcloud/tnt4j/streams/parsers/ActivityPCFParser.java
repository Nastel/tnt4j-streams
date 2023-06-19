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

package com.jkoolcloud.tnt4j.streams.parsers;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.EnumSet;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.exits.MQCD;
import com.ibm.mq.exits.MQCSP;
import com.ibm.mq.headers.pcf.*;
import com.ibm.mq.jmqi.*;
import com.ibm.mq.jmqi.internal.JmqiStructureFormatter;
import com.ibm.mq.jmqi.internal.MqiStructure;
import com.ibm.mq.jmqi.system.JmqiTls;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.WmqParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.*;
import com.jkoolcloud.tnt4j.streams.utils.*;

/**
 * Implements an activity data parser that assumes each activity data item is an {@link PCFContent} ({@link PCFMessage}
 * or {@link MQCFGR}) where each field is represented by a PCF parameter and the PCF parameter MQ constant name/value is
 * used to map each field into its corresponding activity field. PCF parameter data contained Mqi structures are also
 * supported to retrieve values.
 * <p>
 * PCF message can have grouped parameters - all messages will have header {@link MQCFH} and set of PCF parameters. It
 * also may have {@link MQCFGR} type parameters and parameters containing binary data for MQI structures (named
 * 'MQBACF_XXXXX_STRUCT', where 'XXXXX'` is MQI structure name).
 * <p>
 * To access PCF message header fields use 'MQCFH' expression with header field name delimited using
 * {@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#DEFAULT_PATH_DELIM} (e.g., 'MQCFH.CompCode').
 * <p>
 * To access PCF message parameters use MQ constant name/value (e.g., 'MQCACF_APPL_NAME' or '3024').
 * <p>
 * To access inner {@link MQCFGR} (or inner inner and so on) parameters use group parameter MQ constant name/value with
 * grouped parameter MQ constant name/value delimited using
 * {@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#DEFAULT_PATH_DELIM} (e.g.,
 * 'MQGACF_ACTIVITY_TRACE.MQIACF_COMP_CODE').
 * <p>
 * In case PCF parameter refers MQI structure, inner (or inner inner and so on) structure fields can be accessed by
 * adding {@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#DEFAULT_PATH_DELIM} delimited MQI structure fields
 * names to locator path after root MQI structure parameter identifier (e.g.,
 * 'MQGACF_ACTIVITY_TRACE.MQBACF_MQMD_STRUCT.MsgId' or
 * 'MQGACF_ACTIVITY_TRACE.MQBACF_MQCNO_STRUCT.clientConn.userIdentifier')
 * <p>
 * Additionally {@link com.ibm.mq.headers.pcf.PCFMessage} may contain {@link com.ibm.mq.MQMD} data copied from transport
 * {@link com.ibm.mq.MQMessage}. To access transport message {@link com.ibm.mq.MQMD} header values use 'MQMD' expression
 * with field name (or relative PCF parameter constant) delimited using
 * {@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#DEFAULT_PATH_DELIM} (e.g., 'MQMD.PutApplName',
 * 'MQMD.MQCACF_APPL_NAME' or 'MQMD.3024').
 * <p>
 * This parser supports the following configuration properties (in addition to those supported by
 * {@link GenericActivityParser}):
 * <ul>
 * <li>TranslateNumValues - indicates that parser should translate resolved numeric values to corresponding MQ constant
 * names if possible and field/locator data type is 'String' (meaning translated value can be assigned to field). If
 * value of particular field should be left as number (e.g., {@code ReasonCode}), use field/locator attribute
 * {@code datatype="Number"}. Default value - {@code true}. (Optional)</li>
 * </ul>
 * <p>
 * This activity parser supports those activity field locator types:
 * <ul>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Label}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#StreamProp}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Cache}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Activity}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Expression}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#ParserProp}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#SystemProp}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#EnvVariable}</li>
 * </ul>
 *
 * @version $Revision: 2 $
 */
public class ActivityPCFParser extends GenericActivityParser<PCFContent> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(ActivityPCFParser.class);

	private static final String HEAD_MQCFH = "MQCFH"; // NON-NLS
	private static final String HEAD_MQMD = "MQMD"; // NON-NLS
	private static final Pattern STRUCT_ATTR_PATTERN = Pattern.compile("MQBACF_(\\w{4,5})_STRUCT"); // NON-NLS
	private static final String MQ_TMP_CTX_STRUCT_PREF = "MQ_TMP_CTX_"; // NON-NLS

	private boolean translateNumValues = true;

	/**
	 * Constructs a new ActivityPCFParser.
	 */
	public ActivityPCFParser() {
		super(ActivityFieldDataType.AsInput);
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (WmqParserProperties.PROP_TRANSLATE_NUM_VALUES.equalsIgnoreCase(name)) {
			translateNumValues = Utils.toBoolean(value);

			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.setting", name, value);
		}
	}

	@Override
	public Object getProperty(String name) {
		if (WmqParserProperties.PROP_TRANSLATE_NUM_VALUES.equalsIgnoreCase(name)) {
			return translateNumValues;
		}

		return super.getProperty(name);
	}

	/**
	 * Returns whether this parser supports the given format of the activity data. This is used by activity streams to
	 * determine if the parser can parse the data in the format that the stream has it.
	 * <p>
	 * This parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link com.ibm.mq.headers.pcf.PCFContent}</li>
	 * </ul>
	 *
	 * @param data
	 *            data object whose class is to be verified
	 * @return {@code true} if this parser can process data in the specified format, {@code false} - otherwise
	 */
	@Override
	protected boolean isDataClassSupportedByParser(Object data) {
		return data instanceof PCFContent;
	}

	/**
	 * Gets field raw data value resolved by locator.
	 *
	 * @param locator
	 *            activity field locator
	 * @param cData
	 *            PCF message parsing context data
	 * @param formattingNeeded
	 *            flag to set if value formatting is not needed
	 * @return raw value resolved by locator, or {@code null} if value is not resolved
	 *
	 * @throws ParseException
	 *             if exception occurs while resolving raw data value
	 */
	@Override
	protected Object resolveLocatorValue(ActivityFieldLocator locator, ActivityContext cData,
			AtomicBoolean formattingNeeded) throws ParseException {
		Object val = null;
		String locStr = locator.getLocator();
		String[] path = (String[]) getPreparedLocator(locStr,
				k -> Utils.getNodePath(k, StreamsConstants.DEFAULT_PATH_DELIM));
		val = getParamValue(locator, path, cData.getData(), 0, cData);

		logger().log(OpLevel.TRACE, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
				"ActivityPCFParser.resolved.pcf.value", locStr, toString(val));

		return val;
	}

	@SuppressWarnings("deprecation")
	private static final EnumSet<ActivityFieldLocatorType> UNSUPPORTED_LOCATOR_TYPES = EnumSet
			.of(ActivityFieldLocatorType.Index, ActivityFieldLocatorType.Range, ActivityFieldLocatorType.REMatchId);

	/**
	 * {@inheritDoc}
	 * <p>
	 * Unsupported activity locator types are:
	 * <ul>
	 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Index}</li>
	 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Range}</li>
	 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#REMatchId}</li>
	 * </ul>
	 */
	@Override
	protected EnumSet<ActivityFieldLocatorType> getUnsupportedLocatorTypes() {
		return UNSUPPORTED_LOCATOR_TYPES;
	}

	/**
	 * Resolves PCF parameter value from provided {@link PCFContent}: {@link PCFMessage} or {@link MQCFGR}.
	 * <p>
	 * Having last locator path token referring {@link MQCFGR} structure and field bound stacked parser supports
	 * {@link MQCFGR} type data, complete structure is returned. In other cases
	 * {@link com.ibm.mq.headers.pcf.PCFParameter#getValue()} is used to retrieve PCF parameter value.
	 * <p>
	 * PCF parameter data contained Mqi structures are also supported to retrieve values.
	 *
	 * @param locator
	 *            field locator activity field locator
	 * @param path
	 *            parameter path as array of MQ PCF/MQI parameter identifiers
	 * @param pcfContent
	 *            PCF content data (message or parameters group)
	 * @param i
	 *            processed locator path element index
	 * @param cData
	 *            PCF message parsing context data
	 * @return raw value resolved by locator, or {@code null} if value is not resolved
	 *
	 * @throws ParseException
	 *             if exception occurs while resolving raw data value
	 *
	 * @see #resolvePCFParamValue(com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator,
	 *      com.ibm.mq.headers.pcf.PCFParameter)
	 * @see #resolveMqiStructValue(com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator,
	 *      com.ibm.mq.headers.pcf.PCFParameter, String[], int, com.ibm.mq.headers.pcf.PCFContent,
	 *      com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser.ActivityContext)
	 */
	protected Object getParamValue(ActivityFieldLocator locator, String[] path, PCFContent pcfContent, int i,
			ActivityContext cData) throws ParseException {
		if (ArrayUtils.isEmpty(path) || pcfContent == null) {
			return null;
		}

		Object val = null;
		String paramStr = path[i];

		if (i == 0 && paramStr.equals(HEAD_MQCFH)) {
			val = resolvePCFHeaderValue(locator, path[i + 1], (PCFMessage) pcfContent);
		} else if (i == 0 && paramStr.equalsIgnoreCase(HEAD_MQMD)) {
			val = resolveMDMQHeaderValue(locator, path[i + 1], pcfContent);
		} else {
			try {
				Integer paramId = WmqUtils.getParamId(paramStr);
				PCFParameter[] params = WmqUtils.getParameters(pcfContent, paramId);

				if (ArrayUtils.isEmpty(params)) {
					params = WmqUtils.getParameters(pcfContent, WmqStreamConstants.PCF_MQMD_HEADER + paramId);
				}

				if (ArrayUtils.isEmpty(params)) {
					val = null;
				} else {
					if (params.length == 1 && !isLastPathToken(path, i)) {
						if (params[0] instanceof MQCFGR) {
							val = getParamValue(locator, path, (MQCFGR) params[0], ++i, cData);
						} else if (isMqiStructParam(paramStr)) {
							val = resolveMqiStructValue(locator, params[0], path, i, pcfContent, cData);
						}
					} else {
						if (params[0] instanceof MQCFGR
								&& isDataSupportedByStackedParser(cData.getField(), params[0])) {
							val = params;
						} else {
							val = resolvePCFParamsValue(locator, params);
						}
					}

					val = Utils.simplifyValue(val);
				}
			} catch (NoSuchElementException exc) {
				throw new ParseException(StreamsResources.getStringFormatted(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
						"ActivityPCFParser.unresolved.pcf.parameter", paramStr), getPCFPosition(pcfContent));
			}
		}

		return val;
	}

	private static boolean isLastPathToken(String[] path, int i) {
		return i >= path.length - 1;
	}

	private static boolean isMqiStructParam(String paramStr) {
		return STRUCT_ATTR_PATTERN.matcher(paramStr).matches();
	}

	/**
	 * Returns PCF content position identifier to know where event has occurred.
	 *
	 * @param pcfContent
	 *            PCF content to get position value
	 * @return PCF message sequence number or {@link MQCFGR} parameter value
	 */
	protected int getPCFPosition(PCFContent pcfContent) {
		return pcfContent instanceof PCFMessage ? ((PCFMessage) pcfContent).getMsgSeqNumber()
				: ((MQCFGR) pcfContent).getParameter();
	}

	private Object resolvePCFHeaderValue(ActivityFieldLocator locator, String hAttrName, PCFMessage pcfMsg) {
		Integer val = null;
		Object mappedValue = null;
		if ("command".equalsIgnoreCase(hAttrName)) { // NON-NLS
			val = pcfMsg.getHeader().getCommand();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQCMD_.*"); // NON-NLS
			}
		} else if ("msgseqnumber".equalsIgnoreCase(hAttrName)) { // NON-NLS
			val = pcfMsg.getHeader().getMsgSeqNumber();
		} else if ("control".equalsIgnoreCase(hAttrName)) { // NON-NLS
			val = pcfMsg.getHeader().getControl();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQCFC_.*"); // NON-NLS
			}
		} else if ("compcode".equalsIgnoreCase(hAttrName)) { // NON-NLS
			val = pcfMsg.getHeader().getCompCode();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookupCompCode(val);
			}
		} else if ("reason".equalsIgnoreCase(hAttrName)) { // NON-NLS
			val = pcfMsg.getHeader().getReason();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookupReasonCode(val);
			}
		} else if ("parametercount".equalsIgnoreCase(hAttrName)) { // NON-NLS
			val = pcfMsg.getHeader().getParameterCount();
		} else if ("type".equalsIgnoreCase(hAttrName)) { // NON-NLS
			val = pcfMsg.getHeader().getType();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQCFT_.*"); // NON-NLS
			}
		} else if ("version".equalsIgnoreCase(hAttrName)) { // NON-NLS
			val = pcfMsg.getHeader().getVersion();
			// if (isValueTranslatable(locator.getDataType())) {
			// mappedValue = MQConstants.lookup(val, "MQCFH_VERSION_.*"); // NON-NLS
			// }
		} else if ("struclength".equalsIgnoreCase(hAttrName)) { // NON-NLS
			val = pcfMsg.getHeader().getStrucLength();
		}

		return mappedValue != null ? mappedValue : val;
	}

	private boolean isValueTranslatable(ActivityFieldDataType fDataType) {
		return translateNumValues && isTextualLocatorDataType(fDataType);
	}

	private Object resolveMDMQHeaderValue(ActivityFieldLocator locator, String hAttrName, PCFContent pcfContent)
			throws ParseException {
		try {
			Integer paramId = getMQMDParamId(hAttrName);
			PCFParameter param = pcfContent.getParameter(WmqStreamConstants.PCF_MQMD_HEADER + paramId);

			return resolvePCFParamValue(locator, param);
		} catch (NoSuchElementException exc) {
			throw new ParseException(StreamsResources.getStringFormatted(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
					"ActivityPCFParser.unresolved.mqmd.parameter", hAttrName), getPCFPosition(pcfContent));
		}
	}

	private static Integer getMQMDParamId(String mqmdParamId) throws NoSuchElementException {
		if ("Report".equalsIgnoreCase(mqmdParamId)) { // NON-NLS
			return MQConstants.MQIACF_REPORT;
		} else if ("MsgType".equalsIgnoreCase(mqmdParamId)) { // NON-NLS
			return MQConstants.MQIACF_MSG_TYPE;
		} else if ("Expiry".equalsIgnoreCase(mqmdParamId)) { // NON-NLS
			return MQConstants.MQIACF_EXPIRY;
		} else if ("Feedback".equalsIgnoreCase(mqmdParamId)) { // NON-NLS
			return MQConstants.MQIACF_FEEDBACK;
		} else if ("Encoding".equalsIgnoreCase(mqmdParamId)) { // NON-NLS
			return MQConstants.MQIACF_ENCODING;
		} else if ("CodedCharSetId".equalsIgnoreCase(mqmdParamId)) { // NON-NLS
			return MQConstants.MQIA_CODED_CHAR_SET_ID;
		} else if ("Format".equalsIgnoreCase(mqmdParamId)) { // NON-NLS
			return MQConstants.MQCACH_FORMAT_NAME;
		} else if ("Priority".equalsIgnoreCase(mqmdParamId)) { // NON-NLS
			return MQConstants.MQIACF_PRIORITY;
		} else if ("Persistence".equalsIgnoreCase(mqmdParamId)) { // NON-NLS
			return MQConstants.MQIACF_PERSISTENCE;
		} else if ("MsgId".equalsIgnoreCase(mqmdParamId)) { // NON-NLS
			return MQConstants.MQBACF_MSG_ID;
		} else if ("CorrelId".equalsIgnoreCase(mqmdParamId)) { // NON-NLS
			return MQConstants.MQBACF_CORREL_ID;
		} else if ("BackoutCount".equalsIgnoreCase(mqmdParamId)) { // NON-NLS
			return MQConstants.MQIACF_BACKOUT_COUNT;
		} else if ("ReplyToQ".equalsIgnoreCase(mqmdParamId)) { // NON-NLS
			return MQConstants.MQCACF_REPLY_TO_Q;
		} else if ("ReplyToQMgr".equalsIgnoreCase(mqmdParamId)) { // NON-NLS
			return MQConstants.MQCACF_REPLY_TO_Q_MGR;
		} else if ("UserIdentifier".equalsIgnoreCase(mqmdParamId)) { // NON-NLS
			return MQConstants.MQCACF_USER_IDENTIFIER;
		} else if ("AccountingToken".equalsIgnoreCase(mqmdParamId)) { // NON-NLS
			return MQConstants.MQBACF_ACCOUNTING_TOKEN;
		} else if ("ApplIdentityData".equalsIgnoreCase(mqmdParamId)) { // NON-NLS
			return MQConstants.MQCACF_APPL_IDENTITY_DATA;
		} else if ("PutApplType".equalsIgnoreCase(mqmdParamId)) { // NON-NLS
			return MQConstants.MQIA_APPL_TYPE;
		} else if ("PutApplName".equalsIgnoreCase(mqmdParamId)) { // NON-NLS
			return MQConstants.MQCACF_APPL_NAME;
		} else if ("PutDate".equalsIgnoreCase(mqmdParamId)) { // NON-NLS
			return MQConstants.MQCACF_PUT_DATE;
		} else if ("PutTime".equalsIgnoreCase(mqmdParamId)) { // NON-NLS
			return MQConstants.MQCACF_PUT_TIME;
		} else if ("ApplOriginData".equalsIgnoreCase(mqmdParamId)) { // NON-NLS
			return MQConstants.MQCACF_APPL_ORIGIN_DATA;
		} else if ("GroupId".equalsIgnoreCase(mqmdParamId)) { // NON-NLS
			return MQConstants.MQBACF_GROUP_ID;
		} else if ("MsgSeqNumber".equalsIgnoreCase(mqmdParamId)) { // NON-NLS
			return MQConstants.MQIACH_MSG_SEQUENCE_NUMBER;
		} else if ("Offset".equalsIgnoreCase(mqmdParamId)) { // NON-NLS
			return MQConstants.MQIACF_OFFSET;
		} else if ("MsgFlags".equalsIgnoreCase(mqmdParamId)) { // NON-NLS
			return MQConstants.MQIACF_MSG_FLAGS;
		} else if ("OriginalLength".equalsIgnoreCase(mqmdParamId)) { // NON-NLS
			return MQConstants.MQIACF_ORIGINAL_LENGTH;
		} else {
			return WmqUtils.getParamId(mqmdParamId);
		}
	}

	/**
	 * Resolves PCF parameters values array.
	 * <p>
	 * This method iterates through parameters array elements calling
	 * {@link #resolvePCFParamValue(com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator, com.ibm.mq.headers.pcf.PCFParameter)}
	 * method and setting returned value into array. Produced array is always of same size as provided {@code params}
	 * array.
	 * 
	 * @param locator
	 *            activity field locator
	 * @param params
	 *            PCF parameters array to resolve values from
	 * @return resolved PCF parameters values array, or {@code null} if provided parameters array is {@code null}
	 * 
	 * @see #resolvePCFParamValue(com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator,
	 *      com.ibm.mq.headers.pcf.PCFParameter)
	 */
	protected Object[] resolvePCFParamsValue(ActivityFieldLocator locator, PCFParameter... params) {
		if (params == null) {
			return params;
		}

		Object[] pValues = new Object[params.length];
		for (int i = 0; i < params.length; i++) {
			pValues[i] = resolvePCFParamValue(locator, params[i]);
		}

		return pValues;
	}

	/**
	 * Resolves PCF parameter value.
	 * <p>
	 * If parser property 'TranslateNumValues' is set to {@code true} - then if possible, resolved numeric value gets
	 * translated to corresponding MQ constant name.
	 * <p>
	 * When PCF parameter contains binary ({@code byte[]}) value and locator data type is set to {@code "String"} having
	 * attribute "charset" undefined, conversion from binary to string value is performed using PCF parameter defined
	 * charset.
	 *
	 * @param locator
	 *            activity field locator
	 * @param param
	 *            PCF parameter to resolve value from
	 * @return resolved PCF parameter value, or {@code null} if provided parameter is {@code null}
	 *
	 * @see #getParamValue(com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator, String[],
	 *      com.ibm.mq.headers.pcf.PCFContent, int,
	 *      com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser.ActivityContext)
	 * @see com.jkoolcloud.tnt4j.streams.utils.WmqUtils#getString(byte[], Object)
	 */
	protected Object resolvePCFParamValue(ActivityFieldLocator locator, PCFParameter param) {
		if (param == null) {
			return param;
		}

		Object val = param.getValue();

		if (val instanceof String) {
			val = ((String) val).trim();
		} else if (val instanceof byte[] && isStringLocatorWithoutCharset(locator)) {
			try {
				logger().log(OpLevel.DEBUG,
						StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
								"ActivityPCFParser.converting.pcf.binary.value"),
						param.getParameterName(), param.getParameter(), WmqUtils.getCharsetName(param.characterSet()),
						param.characterSet());
				val = WmqUtils.getString((byte[]) val, param.characterSet());
			} catch (UnsupportedEncodingException exc) {
				logger().log(OpLevel.WARNING,
						StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
								"ActivityPCFParser.unsupported.encoding"),
						param.getParameterName(), param.getParameter(), param.characterSet());
			}
		}

		if (isValueTranslatable(locator.getDataType())) {
			Object mappedValue = null;
			switch (param.getParameter()) {
			case MQConstants.MQIA_APPL_TYPE:
				mappedValue = MQConstants.lookup(val, "MQAT_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_API_CALLER_TYPE:
				mappedValue = MQConstants.lookup(val, "MQXACT_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_API_ENVIRONMENT:
				mappedValue = MQConstants.lookup(val, "MQXE_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_APPL_FUNCTION_TYPE:
				mappedValue = MQConstants.lookup(val, "MQFUN_.*"); // NON-NLS
				break;
			case MQConstants.MQIA_PLATFORM:
				mappedValue = MQConstants.lookup(val, "MQPL_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_OPERATION_ID:
				mappedValue = MQConstants.lookup(val, "MQXF_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_OBJECT_TYPE:
			case MQConstants.MQIACF_RESOLVED_TYPE:
				mappedValue = MQConstants.lookup(val, "MQOT_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_COMP_CODE:
				mappedValue = MQConstants.lookup(val, "MQCC_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_MSG_TYPE:
				mappedValue = MQConstants.lookup(val, "MQMT_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_REASON_CODE:
				mappedValue = MQConstants.lookupReasonCode((Integer) val);
				break;
			case MQConstants.MQIA_CODED_CHAR_SET_ID:
				mappedValue = MQConstants.lookup(val, "MQCCSI_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_ENCODING:
				mappedValue = MQConstants.lookup(val, "MQENC_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_EXPIRY:
				mappedValue = MQConstants.lookup(val, "MQEI_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_FEEDBACK:
				mappedValue = MQConstants.lookup(val, "MQFB_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_MSG_FLAGS:
				mappedValue = MQConstants.decodeOptions((int) val, "MQMF_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_ORIGINAL_LENGTH:
				mappedValue = MQConstants.lookup(val, "MQOL_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_PERSISTENCE:
				mappedValue = MQConstants.lookup(val, "MQPER_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_PRIORITY:
				mappedValue = MQConstants.lookup(val, "MQPRI_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_REPORT:
				mappedValue = MQConstants.decodeOptions((int) val, "MQRO_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_VERSION:
				mappedValue = MQConstants.lookup(val, "MQMD_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_OPEN_OPTIONS:
				mappedValue = MQConstants.decodeOptions((int) val, "MQOO_.*"); // NON-NLS
				break;
			// case MQConstants.MQIACF_OPTIONS:
			// break;
			// case MQConstants.MQIACF_BROKER_OPTIONS:
			// break;
			case MQConstants.MQIACF_REGISTRATION_OPTIONS:
				mappedValue = MQConstants.decodeOptions((int) val, "MQREGO_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_PUBLICATION_OPTIONS:
				mappedValue = MQConstants.decodeOptions((int) val, "MQPUBO_.*"); // NON-NLS
				break;
			// case MQConstants.MQIACF_REG_REG_OPTIONS:
			// break;
			case MQConstants.MQIACF_DELETE_OPTIONS:
				mappedValue = MQConstants.decodeOptions((int) val, "MQDELO_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_CONNECT_OPTIONS:
				mappedValue = MQConstants.decodeOptions((int) val, "MQCNO_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_AUTH_OPTIONS:
				mappedValue = MQConstants.decodeOptions((int) val, "MQAUTHOPT_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_SUB_OPTIONS:
				mappedValue = MQConstants.decodeOptions((int) val, "MQSO_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_MQCB_OPTIONS:
				mappedValue = MQConstants.decodeOptions((int) val, "MQCBO_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_CLOSE_OPTIONS:
				mappedValue = MQConstants.decodeOptions((int) val, "MQCO_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_GET_OPTIONS:
				mappedValue = MQConstants.decodeOptions((int) val, "MQGMO_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_PUT_OPTIONS:
				mappedValue = MQConstants.decodeOptions((int) val, "MQPMO_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_SUBRQ_OPTIONS:
				mappedValue = MQConstants.decodeOptions((int) val, "MQSRO_.*"); // NON-NLS
				break;
			default:
				break;
			}

			if (mappedValue != null) {
				val = mappedValue;
			}
		}

		return val;
	}

	private boolean isStringLocatorWithoutCharset(ActivityFieldLocator locator) {
		return StringUtils.isEmpty(locator.getCharset()) && isTextualLocatorDataType(locator.getDataType());
	}

	private boolean isTextualLocatorDataType(ActivityFieldDataType fDataType) {
		switch (fDataType) {
		case String:
		case Generic:
		case AsInput:
			return true;
		}
		return false;
	}

	/**
	 * Builds MQI structure from PCF parameter contained binary data and resolves field locator referenced value from
	 * that structure.
	 *
	 * @param locator
	 *            activity field locator
	 * @param param
	 *            PCF parameter to use raw data to build MQI structure
	 * @param path
	 *            parameter path as array of MQ PCF/MQI parameter identifiers
	 * @param i
	 *            processed locator path element index
	 * @param pcfContent
	 *            PCF content data (message or parameters group)
	 * @param cData
	 *            PCF message parsing context data
	 * @return resolved MQI structure value
	 * @throws ParseException
	 *             if exception occurs while resolving raw data value
	 *
	 * @see #getParamValue(com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator, String[],
	 *      com.ibm.mq.headers.pcf.PCFContent, int,
	 *      com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser.ActivityContext)
	 */
	protected Object resolveMqiStructValue(ActivityFieldLocator locator, PCFParameter param, String[] path, int i,
			PCFContent pcfContent, ActivityContext cData) throws ParseException {
		if (param == null) {
			return null;
		}

		String ctxStructKey = MQ_TMP_CTX_STRUCT_PREF + param.getParameterName();
		MqiStructure mqiStruct = (MqiStructure) cData.get(ctxStructKey);
		boolean exception = false;
		if (mqiStruct == null) {
			try {
				mqiStruct = buildMqiStructureFromBinData(param);
				cData.put(ctxStructKey, mqiStruct);
			} catch (Exception exc) {
				Utils.logThrowable(logger(), OpLevel.ERROR,
						StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
						"ActivityPCFParser.structure.build.failed", exc);
				exception = true;
			}

			if (mqiStruct == null || exception) {
				throw new ParseException(
						StreamsResources.getStringFormatted(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
								"ActivityPCFParser.can.not.build.structure", getFullPath(path, i)),
						getPCFPosition(pcfContent));
			}
		}

		return resolveMqiStructParamValue(mqiStruct, path, i, param.getJmqiEnv(), locator);
	}

	private MqiStructure buildMqiStructureFromBinData(PCFParameter structParam) throws JmqiException {
		if (structParam == null) {
			return null;
		}

		byte[] structData = (byte[]) structParam.getValue();
		JmqiEnvironment env = structParam.getJmqiEnv();
		MqiStructure mqiStruct = null;

		switch (structParam.getParameter()) {
		case MQConstants.MQBACF_MQBO_STRUCT:
			mqiStruct = env.newMQBO();
			break;
		case MQConstants.MQBACF_MQCBC_STRUCT:
			mqiStruct = env.newMQCBC();
			break;
		case MQConstants.MQBACF_MQCBD_STRUCT:
			mqiStruct = env.newMQCBD();
			break;
		case MQConstants.MQBACF_MQCD_STRUCT:
			mqiStruct = env.newMQCD();
			break;
		case MQConstants.MQBACF_MQCNO_STRUCT:
			mqiStruct = env.newMQCNO();
			break;
		case MQConstants.MQBACF_MQGMO_STRUCT:
			mqiStruct = env.newMQGMO();
			break;
		case MQConstants.MQBACF_MQMD_STRUCT:
			mqiStruct = env.newMQMD();
			break;
		case MQConstants.MQBACF_MQPMO_STRUCT:
			mqiStruct = env.newMQPMO();
			break;
		case MQConstants.MQBACF_MQSD_STRUCT:
			mqiStruct = env.newMQSD();
			break;
		case MQConstants.MQBACF_MQSTS_STRUCT:
			mqiStruct = env.newMQSTS();
			break;
		default:
		}

		if (mqiStruct != null) {
			mqiStruct.readFromBuffer(structData, 0, 4, true, env.getNativeCharSet(), new JmqiTls());
			MqiStructure logMqiStruct = mqiStruct;
			LoggerUtils.log(logger(), OpLevel.DEBUG,
					StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
					"ActivityPCFParser.built.structure", () -> Utils.args(structParam.getParameterName(),
							structParam.getParameter(), mqiToString(logMqiStruct, env)));

			return mqiStruct;
		}

		return mqiStruct;
	}

	private static String mqiToString(MqiStructure mqiStruct, JmqiEnvironment env) {
		JmqiStructureFormatter fmt = new JmqiStructureFormatter(env, 24, 2, "\n"); // NON-NLS
		mqiStruct.addFieldsToFormatter(fmt);

		return fmt.toString();
	}

	private static String getFullPath(String[] path, int i) {
		StringBuilder sb = new StringBuilder();

		for (int pi = 0; pi <= i; pi++) {
			sb.append(path[pi]);

			if (pi < i) {
				sb.append(StreamsConstants.DEFAULT_PATH_DELIM);
			}
		}

		return sb.toString();
	}

	private Object resolveMqiStructParamValue(MqiStructure mqiStruct, String[] path, int i, JmqiEnvironment env,
			ActivityFieldLocator locator) throws ParseException {
		Object val = null;
		Matcher structMatcher = STRUCT_ATTR_PATTERN.matcher(path[i]);

		if (structMatcher.matches() && structMatcher.groupCount() > 0) {
			if (structMatcher.group(1).equalsIgnoreCase(mqiStruct.getClass().getSimpleName())) {
				if (isLastPathToken(path, i)) {
					val = mqiToString(mqiStruct, env);
				} else {
					val = resolveMqiStructParamValue(mqiStruct, path, i + 1, locator);

					if (val instanceof String) {
						val = ((String) val).trim();
					} else if (val instanceof MqiStructure) {
						val = mqiToString((MqiStructure) val, env);
					}
				}
			}
		}

		return val;
	}

	private Object resolveMqiStructParamValue(MqiStructure mqiSruct, String[] path, int i, ActivityFieldLocator locator)
			throws ParseException {
		Object val = null;
		if (mqiSruct instanceof MQMD) {
			val = resolveMQMDValue((MQMD) mqiSruct, path[i], locator);
		} else if (mqiSruct instanceof MQPMO) {
			val = resolveMQPMOValue((MQPMO) mqiSruct, path[i], locator);
		} else if (mqiSruct instanceof MQGMO) {
			val = resolveMQGMOValue((MQGMO) mqiSruct, path[i], locator);
		} else if (mqiSruct instanceof MQCNO) {
			val = resolveMQCNOValue((MQCNO) mqiSruct, path[i], locator);
		} else if (mqiSruct instanceof MQCD) {
			val = resolveMQCDValue((MQCD) mqiSruct, path[i], locator);
		} else if (mqiSruct instanceof MQCBD) {
			val = resolveMQCBDValue((MQCBD) mqiSruct, path[i], locator);
		} else if (mqiSruct instanceof MQCBC) {
			val = resolveMQCBCValue((MQCBC) mqiSruct, path[i], locator);
		} else if (mqiSruct instanceof MQBO) {
			val = resolveMQBOValue((MQBO) mqiSruct, path[i], locator);
		} else if (mqiSruct instanceof MQSD) {
			val = resolveMQSDValue((MQSD) mqiSruct, path[i], locator);
		} else if (mqiSruct instanceof MQSTS) {
			val = resolveMQSTSValue((MQSTS) mqiSruct, path[i], locator);
		} else if (mqiSruct instanceof MQCSP) {
			val = resolveMQCSPValue((MQCSP) mqiSruct, path[i], locator);
		} else if (mqiSruct instanceof MQSCO) {
			val = resolveMQSCOValue((MQSCO) mqiSruct, path[i], locator);
		} else {
			try {
				val = Utils.getFieldValue(path, mqiSruct, i);
			} catch (Exception exc) {
				Utils.logThrowable(logger(), OpLevel.ERROR,
						StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
						"ActivityPCFParser.structure.value.resolution.failed", exc);

				// throw new ParseException(
				// StreamsResources.getStringFormatted(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
				// "ActivityPCFParser.can.not.resolve.structure.value", getFullPath(path, i)),
				// getPCFPosition(pcfContent));
			}
		}

		return val;
	}

	private Object resolveMQMDValue(MQMD mqmd, String fName, ActivityFieldLocator locator) {
		Object val = null;
		Object mappedValue = null;

		switch (fName.toLowerCase()) {
		case "accountingtoken": // NON-NLS
			val = mqmd.getAccountingToken();
			break;
		case "applidentitydata": // NON-NLS
			val = mqmd.getApplIdentityData();
			break;
		case "applorigindata": // NON-NLS
			val = mqmd.getApplOriginData();
			break;
		case "backoutcount": // NON-NLS
			val = mqmd.getBackoutCount();
			break;
		case "codedcharsetid": // NON-NLS
			val = mqmd.getCodedCharSetId();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQCCSI_.*"); // NON-NLS
			}
			break;
		case "correlid": // NON-NLS
			val = mqmd.getCorrelId();
			break;
		case "encoding": // NON-NLS
			val = mqmd.getEncoding();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQENC_.*"); // NON-NLS
			}
			break;
		case "expiry": // NON-NLS
			val = mqmd.getExpiry();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQEI_.*"); // NON-NLS
			}
			break;
		case "feedback": // NON-NLS
			val = mqmd.getFeedback();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQFB_.*"); // NON-NLS
			}
			break;
		case "format": // NON-NLS
			val = mqmd.getFormat();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQFMT_.*"); // NON-NLS
			}
			break;
		case "groupid": // NON-NLS
			val = mqmd.getGroupId();

			break;
		case "msgflags": // NON-NLS
			val = mqmd.getMsgFlags();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.decodeOptions((int) val, "MQMF_.*"); // NON-NLS
			}
			break;
		case "msgid": // NON-NLS
			val = mqmd.getMsgId();
			break;
		case "msgseqnumber": // NON-NLS
			val = mqmd.getMsgSeqNumber();
			break;
		case "msgtype": // NON-NLS
			val = mqmd.getMsgType();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQMT_.*"); // NON-NLS
			}
			break;
		case "offset": // NON-NLS
			val = mqmd.getOffset();
			break;
		case "originallength": // NON-NLS
			val = mqmd.getOriginalLength();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQOL_.*"); // NON-NLS
			}
			break;
		case "persistence": // NON-NLS
			val = mqmd.getPersistence();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQPER_.*"); // NON-NLS
			}
			break;
		case "priority": // NON-NLS
			val = mqmd.getPriority();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQPRI_.*"); // NON-NLS
			}
			break;
		case "putapplname": // NON-NLS
			val = mqmd.getPutApplName();
			break;
		case "putappltype": // NON-NLS
			val = mqmd.getPutApplType();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQAT_.*"); // NON-NLS
			}
			break;
		case "putdate": // NON-NLS
			val = mqmd.getPutDate();
			break;
		case "puttime": // NON-NLS
			val = mqmd.getPutTime();
			break;
		case "replytoq": // NON-NLS
			val = mqmd.getReplyToQ();
			break;
		case "replytoqmgr": // NON-NLS
			val = mqmd.getReplyToQMgr();
			break;
		case "report": // NON-NLS
			val = mqmd.getReport();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.decodeOptions((int) val, "MQRO_.*"); // NON-NLS
			}
			break;
		case "useridentifier": // NON-NLS
			val = mqmd.getUserIdentifier();
			break;
		case "unmappableaction": // NON-NLS
			val = mqmd.getUnmappableAction();
			break;
		case "unmappablereplacement": // NON-NLS
			val = mqmd.getUnMappableReplacement();
			break;
		case "version": // NON-NLS
			val = mqmd.getVersion();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQMD_VERSION_.*"); // NON-NLS
			}
			break;
		default:
			break;
		}

		return mappedValue != null ? mappedValue : val;
	}

	private Object resolveMQPMOValue(MQPMO mqpmo, String fName, ActivityFieldLocator locator) {
		Object val = null;
		Object mappedValue = null;

		switch (fName.toLowerCase()) {
		case "action": // NON-NLS
			val = mqpmo.getAction();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQACTP_.*"); // NON-NLS
			}
			break;
		case "context": // NON-NLS
			val = mqpmo.getContext();
			// if (isValueTranslatable(locator.getDataType())) {
			// mappedValue = MQConstants.lookup(val, "MQACTP_.*"); // NON-NLS
			// }
			break;
		case "invaliddestcount": // NON-NLS
			val = mqpmo.getInvalidDestCount();
			break;
		case "knowndestcount": // NON-NLS
			val = mqpmo.getKnownDestCount();
			break;
		case "newmsghandle": // NON-NLS
			val = mqpmo.getNewMsgHandle();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQHM_.*"); // NON-NLS
			}
			break;
		case "options": // NON-NLS
			val = mqpmo.getOptions();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.decodeOptions((int) val, "MQPMO_.*"); // NON-NLS
			}
			break;
		case "originalmsghandle": // NON-NLS
			val = mqpmo.getOriginalMsgHandle();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQHM_.*"); // NON-NLS
			}
			break;
		case "sublevel": // NON-NLS
			val = mqpmo.getSubLevel();
			break;
		case "putmsgrecfields": // NON-NLS
			val = mqpmo.getPutMsgRecFields();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.decodeOptions((int) val, "MQPMRF_.*"); // NON-NLS
			}
			break;
		case "recspresent": // NON-NLS
			val = mqpmo.getRecsPresent();
			break;
		case "resolvedqmgrname": // NON-NLS
			val = mqpmo.getResolvedQMgrName();
			break;
		case "resolvedqname": // NON-NLS
			val = mqpmo.getResolvedQName();
			break;
		case "timeout": // NON-NLS
			// val = mqpmo.get();
			break;
		case "unknowndestcount": // NON-NLS
			val = mqpmo.getUnknownDestCount();
			break;
		case "version": // NON-NLS
			val = mqpmo.getVersion();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQPMO_VERSION_.*"); // NON-NLS
			}
			break;
		default:
			break;
		}

		return mappedValue != null ? mappedValue : val;
	}

	private Object resolveMQGMOValue(MQGMO mqgmo, String fName, ActivityFieldLocator locator) {
		Object val = null;
		Object mappedValue = null;

		switch (fName.toLowerCase()) {
		case "groupstatus": // NON-NLS
			val = mqgmo.getGroupStatus();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQGS_.*"); // NON-NLS
			}
			break;
		case "matchoptions": // NON-NLS
			val = mqgmo.getMatchOptions();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.decodeOptions((int) val, "MQMO_.*"); // NON-NLS
			}
			break;
		case "msghandle": // NON-NLS
			val = mqgmo.getMessageHandle();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQHM_.*"); // NON-NLS
			}
			break;
		case "msgtoken": // NON-NLS
			val = mqgmo.getMsgToken();
			break;
		case "options": // NON-NLS
			val = mqgmo.getOptions();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.decodeOptions((int) val, "MQGMO_.*"); // NON-NLS
			}
			break;
		case "resolvedqname": // NON-NLS
			val = mqgmo.getResolvedQName();
			break;
		case "returnedlength": // NON-NLS
			val = mqgmo.getReturnedLength();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQRL_.*"); // NON-NLS
			}
			break;
		case "segmentation": // NON-NLS
			val = mqgmo.getSegmentation();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQSEG_.*"); // NON-NLS
			}
			break;
		case "segmentstatus": // NON-NLS
			val = mqgmo.getSegmentStatus();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQSS_.*"); // NON-NLS
			}
			break;
		case "signal2": // NON-NLS
			val = mqgmo.getSignal2();
			break;
		case "waitinterval": // NON-NLS
			val = mqgmo.getWaitInterval();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQWI_.*"); // NON-NLS
			}
			break;
		case "version": // NON-NLS
			val = mqgmo.getVersion();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQGMO_VERSION_.*"); // NON-NLS
			}
			break;
		default:
			break;
		}

		return mappedValue != null ? mappedValue : val;
	}

	private Object resolveMQCNOValue(MQCNO mqcno, String fName, ActivityFieldLocator locator) {
		Object val = null;
		Object mappedValue = null;

		switch (fName.toLowerCase()) {
		case "clientconn": // NON-NLS
			val = mqcno.getClientConn();
			break;
		case "securityparams": // NON-NLS
			val = mqcno.getSecurityParms();
			break;
		case "sslconfig": // NON-NLS
			val = mqcno.getSslConfig();
			break;
		case "connectionid": // NON-NLS
			val = mqcno.getConnectionId();
			break;
		case "conntag": // NON-NLS
			val = mqcno.getConnTag();
			break;
		case "options": // NON-NLS
			val = mqcno.getOptions();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.decodeOptions((int) val, "MQCNO_.*"); // NON-NLS
			}
			break;
		case "version": // NON-NLS
			val = mqcno.getVersion();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQCNO_VERSION_.*"); // NON-NLS
			}
			break;
		case "ccdturl": // NON-NLS
			val = mqcno.getCCDTUrl();
			break;
		default:
			break;
		}

		return mappedValue != null ? mappedValue : val;
	}

	private Object resolveMQCDValue(MQCD mqcd, String fName, ActivityFieldLocator locator) {
		return mqcd; // TODO
	}

	private Object resolveMQCBDValue(MQCBD mqcbd, String fName, ActivityFieldLocator locator) {
		Object val = null;
		Object mappedValue = null;

		switch (fName.toLowerCase()) {
		case "callbacktype": // NON-NLS
			val = mqcbd.getCallbackType();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQCBT_.*"); // NON-NLS
			}
			break;
		case "callbackarea": // NON-NLS
			val = mqcbd.getCallbackArea();
			break;
		case "callbackfunction": // NON-NLS
			val = mqcbd.getCallbackFunction();
			break;
		case "callbackname": // NON-NLS
			val = mqcbd.getCallbackName();
			break;
		case "maxmsglength": // NON-NLS
			val = mqcbd.getMaxMsgLength();
			break;
		case "inhibitese": // NON-NLS
			val = mqcbd.inhibitESE();
			break;
		case "options": // NON-NLS
			val = mqcbd.getOptions();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.decodeOptions((int) val, "MQCBD0_.*"); // NON-NLS
			}
			break;
		case "version": // NON-NLS
			val = mqcbd.getVersion();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQCBD_VERSION_.*"); // NON-NLS
			}
			break;
		default:
			break;
		}

		return mappedValue != null ? mappedValue : val;
	}

	private Object resolveMQCBCValue(MQCBC mqcbc, String fName, ActivityFieldLocator locator) {
		Object val = null;
		Object mappedValue = null;

		switch (fName.toLowerCase()) {
		case "calltype": // NON-NLS
			val = mqcbc.getCallType();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQCBCT_.*"); // NON-NLS
			}
			break;
		case "hobj": // NON-NLS
			val = mqcbc.getHobj();
			break;
		case "callbackarea": // NON-NLS
			val = mqcbc.getCallbackArea();
			break;
		case "connectionarea": // NON-NLS
			val = mqcbc.getConnectionArea();
			break;
		case "compcode": // NON-NLS
			val = mqcbc.getCompCode();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookupCompCode((Integer) val); // NON-NLS
			}
			break;
		case "reason": // NON-NLS
			val = mqcbc.getReason();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookupReasonCode((Integer) val); // NON-NLS
			}
			break;
		case "state": // NON-NLS
			val = mqcbc.getState();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQCS_.*"); // NON-NLS
			}
			break;
		case "datalength": // NON-NLS
			val = mqcbc.getDataLength();
			break;
		case "bufferlength": // NON-NLS
			val = mqcbc.getBufferLength();
			break;
		case "flags": // NON-NLS
			val = mqcbc.getFlags();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQCBCF_.*"); // NON-NLS
			}
			break;
		case "reconnectdelay": // NON-NLS
			val = mqcbc.getReconnectDelay();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQRD_.*"); // NON-NLS
			}
			break;
		case "version": // NON-NLS
			val = mqcbc.getVersion();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQCBC_VERSION_.*"); // NON-NLS
			}
			break;
		default:
			break;
		}

		return mappedValue != null ? mappedValue : val;
	}

	private Object resolveMQBOValue(MQBO mqbo, String fName, ActivityFieldLocator locator) {
		Object val = null;
		Object mappedValue = null;

		switch (fName.toLowerCase()) {
		case "options": // NON-NLS
			val = mqbo.getOptions();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.decodeOptions((int) val, "MQBO_.*"); // NON-NLS
			}
			break;
		case "version": // NON-NLS
			val = mqbo.getVersion();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQBO_VERSION_.*"); // NON-NLS
			}
			break;
		default:
			break;
		}

		return mappedValue != null ? mappedValue : val;
	}

	private Object resolveMQSDValue(MQSD mqsd, String fName, ActivityFieldLocator locator) {
		return mqsd; // TODO
	}

	private Object resolveMQSTSValue(MQSTS mqsts, String fName, ActivityFieldLocator locator) {
		Object val = null;
		Object mappedValue = null;

		switch (fName.toLowerCase()) {
		case "putsuccesscount": // NON-NLS
			val = mqsts.getPutSuccessCount();
			break;
		case "putwarningcount": // NON-NLS
			val = mqsts.getPutWarningCount();
			break;
		case "putfailurecount": // NON-NLS
			val = mqsts.getPutFailureCount();
			break;
		case "objectType": // NON-NLS
			val = mqsts.getObjectType();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQOT_.*"); // NON-NLS
			}
			break;
		case "compcode": // NON-NLS
			val = mqsts.getCompCode();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookupCompCode((Integer) val); // NON-NLS
			}
			break;
		case "reason": // NON-NLS
			val = mqsts.getReason();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookupReasonCode((Integer) val); // NON-NLS
			}
			break;
		case "objectname": // NON-NLS
			val = mqsts.getObjectName();
			break;
		case "objectqmgrname": // NON-NLS
			val = mqsts.getObjectQMgrName();
			break;
		case "resolvedobjectname": // NON-NLS
			val = mqsts.getResolvedObjectName();
			break;
		case "resolvedqmgrname": // NON-NLS
			val = mqsts.getResolvedQMgrName();
			break;
		case "objectstring": // NON-NLS
			val = mqsts.getObjectString();
			if (val != null) {
				val = ((MQCHARV) val).getVsString();
			}
			break;
		case "subname": // NON-NLS
			val = mqsts.getSubName();
			if (val != null) {
				val = ((MQCHARV) val).getVsString();
			}
			break;
		case "openoptions": // NON-NLS
			val = mqsts.getOpenOptions();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.decodeOptions((int) val, "MQOO_.*"); // NON-NLS
			}
			break;
		case "suboptions": // NON-NLS
			val = mqsts.getSubOptions();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.decodeOptions((int) val, "MQSO_.*"); // NON-NLS
			}
			break;
		case "version": // NON-NLS
			val = mqsts.getVersion();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQSTS_VERSION_.*"); // NON-NLS
			}
			break;
		default:
			break;
		}

		return mappedValue != null ? mappedValue : val;
	}

	private Object resolveMQCSPValue(MQCSP mqcsp, String fName, ActivityFieldLocator locator) {
		Object val = null;
		Object mappedValue = null;

		switch (fName.toLowerCase()) {
		case "authenticationtype": // NON-NLS
			val = mqcsp.getAuthenticationType();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQCSP_AUTH_.*"); // NON-NLS
			}
			break;
		case "cspuserid": // NON-NLS
			val = mqcsp.getCspUserId();
			break;
		case "csppassword": // NON-NLS
			val = mqcsp.getCspPassword();
			break;
		case "version": // NON-NLS
			val = mqcsp.getVersion();
			if (isValueTranslatable(locator.getDataType())) {
				mappedValue = MQConstants.lookup(val, "MQCSP_VERSION_.*"); // NON-NLS
			}
			break;
		default:
			break;
		}

		return mappedValue != null ? mappedValue : val;

	}

	private Object resolveMQSCOValue(MQSCO mqsco, String fName, ActivityFieldLocator locator) {
		return mqsco; // TODO
	}

	private static final String[] ACTIVITY_DATA_TYPES = { "PCF MESSAGE" }; // NON-NLS

	/**
	 * Returns type of RAW activity data entries.
	 *
	 * @return type of RAW activity data entries - {@code "PCF MESSAGE"}
	 */
	@Override
	protected String[] getActivityDataType() {
		return ACTIVITY_DATA_TYPES;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method applies custom handling for setting field values. This method will construct the signature to use for
	 * the message from the specified value, which is assumed to be a string containing the inputs required for the
	 * message signature calculation, with each input delimited by field property
	 * {@link com.jkoolcloud.tnt4j.streams.fields.ActivityField#getSeparator()} defined delimiter.
	 * <p>
	 * To initiate signature calculation as a field value, {@code field} tag {@code value-type} attribute value has be
	 * set to {@value com.jkoolcloud.tnt4j.streams.utils.WmqStreamConstants#VT_SIGNATURE}.
	 *
	 * @see WmqUtils#computeSignature(Object, String, EventSink)
	 */
	@Override
	protected void applyFieldValue(ActivityInfo ai, ActivityField field, Object value) throws ParseException {
		if (WmqStreamConstants.VT_SIGNATURE.equalsIgnoreCase(field.getValueType())) {
			logger().log(OpLevel.DEBUG, StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
					"ActivityPCFParser.calculating.signature"), field);
			value = WmqUtils.computeSignature(value, field.getSeparator(), logger());
		}

		super.applyFieldValue(ai, field, value);
	}
}
