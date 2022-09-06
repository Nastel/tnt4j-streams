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

package com.jkoolcloud.tnt4j.streams.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.headers.CCSID;
import com.ibm.mq.headers.Charsets;
import com.ibm.mq.headers.MQDLH;
import com.ibm.mq.headers.MQXQH;
import com.ibm.mq.headers.pcf.MQCFGR;
import com.ibm.mq.headers.pcf.MQCFIN;
import com.ibm.mq.headers.pcf.PCFContent;
import com.ibm.mq.headers.pcf.PCFParameter;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.parsers.MessageType;

/**
 * WMQ utility methods used by TNT4J-Streams-WMQ module.
 *
 * @version $Revision: 2 $
 */
public class WmqUtils {

	private static final MessageDigest MSG_DIGEST = Utils.getMD5Digester();

	/**
	 * Constant for WMQ binary message data conversion flag indicating to preserve DLH and XQH headers data.
	 */
	public static final int MQ_BIN_STR_PRESERVE_DLH_XQH = 0;
	/**
	 * Constant for WMQ binary message data conversion flag indicating to strip DLH and XQH headers data.
	 */
	public static final int MQ_BIN_STR_STRIP_DLH_XQH = 1 << 0;
	// continue shifting 1<<1, 1<<2, 1<<3....

	// ---- R&D UTILITY CODE ---
	// private static Map<String, Set<String>> groupAttrsMap = new HashMap<String, Set<String>>();
	//
	// public static void collectAttrs(MQCFGR group) {
	// boolean changed = false;
	// String opKey = getOpName(group);
	//
	// Set<String> opParamSet = groupAttrsMap.get(opKey);
	// if (opParamSet == null) {
	// opParamSet = new HashSet<String>();
	// groupAttrsMap.put(opKey, opParamSet);
	// changed = true;
	// }
	//
	// Set<String> allOpsSet = groupAttrsMap.get("ALL_OPERATIONS_SET");
	// if (allOpsSet == null) {
	// allOpsSet = new HashSet<String>();
	// groupAttrsMap.put("ALL_OPERATIONS_SET", allOpsSet);
	// changed = true;
	// }
	//
	// Enumeration<?> prams = group.getParameters();
	// while (prams.hasMoreElements()) {
	// PCFParameter param = (PCFParameter) prams.nextElement();
	// String pString = PCFConstants.lookupParameter(param.getParameter());
	//
	// if (!opParamSet.contains(pString)) {
	// opParamSet.add(pString);
	// changed = true;
	// }
	//
	// if (!allOpsSet.contains(pString)) {
	// allOpsSet.add(pString);
	// changed = true;
	// }
	// }
	//
	// if (changed) {
	// write(groupAttrsMap);
	// }
	// }
	//
	// private static void write(Map<String, Set<String>> map) {
	// String str = "";
	//
	// for (Map.Entry<String, Set<String>> e : map.entrySet()) {
	// str += e.getKey() + "\n";
	// for (String s : e.getValue()) {
	// str += " " + s + "\n";
	// }
	// }
	//
	// File f = new File("GROUP_MAP1.log");
	// try {
	// FileUtils.write(f, str, Utils.UTF8);
	// } catch (Exception exc) {
	// exc.printStackTrace();
	// }
	// }
	// ---- R&D UTILITY CODE ---

	/**
	 * Checks whether provided PCF parameter contains MQ activity trace data.
	 *
	 * @param param
	 *            PCF parameter to check
	 * @return {@code true} if parameter is of type {@link com.ibm.mq.headers.pcf.MQCFGR} and parameter's parameter
	 *         field value is {@code MQGACF_ACTIVITY_TRACE}, {@code false} - otherwise
	 */
	public static boolean isTraceParameter(PCFParameter param) {
		return param.getParameter() == MQConstants.MQGACF_ACTIVITY_TRACE && param instanceof MQCFGR;
	}

	/**
	 * Resolves operation id parameter {@link MQConstants#MQIACF_OPERATION_ID} value from WMQ activity PCF data object
	 * and translates it to WMQ operation name constant {@code "MQXF_"}.
	 *
	 * @param pcf
	 *            wmq activity PCF data
	 * @return resolved operation name, or {@code null} if no operation id parameter found in PCF content
	 *
	 * @see MQConstants#lookup(int, String)
	 */
	public static String getOpName(PCFContent pcf) {
		String opName = null;
		PCFParameter op = pcf.getParameter(MQConstants.MQIACF_OPERATION_ID);
		if (op != null) {
			opName = MQConstants.lookup(((MQCFIN) op).getIntValue(), "MQXF_.*"); // NON-NLS
		}

		return opName;
	}

	/**
	 * Resolves reason code parameter {@link MQConstants#MQIACF_REASON_CODE} value from WMQ activity PCF data object.
	 * 
	 * @param pcf
	 *            wmq activity PCF data
	 * @return resolved reason code, or {@code null} if no reason code parameter found in PCF content
	 */
	public static Integer getRC(PCFContent pcf) {
		Integer mqRC = null;
		PCFParameter rc = pcf.getParameter(MQConstants.MQIACF_REASON_CODE);
		if (rc != null) {
			mqRC = ((MQCFIN) rc).getIntValue();
		}

		return mqRC;
	}

	/**
	 * Resolves all PCF parameters matching provided parameter id from provided PCF data. In most cases returned array
	 * will contain single item. Empty array is returned when no matching parameters available. Multiple items usually
	 * returned for {@link com.ibm.mq.headers.pcf.MQCFGR} parameters, e.g. traces, statistics, etc.
	 * 
	 * @param pcf
	 *            wmq activity PCF data
	 * @param paramId
	 *            PCF parameter identifier
	 * @return PCF parameters array: empty when go matching parameters available, multiple items usually returned for
	 *         {@link com.ibm.mq.headers.pcf.MQCFGR} type parameters
	 */
	public static PCFParameter[] getParameters(PCFContent pcf, int paramId) {
		List<PCFParameter> paramsList = new ArrayList<>();
		Enumeration<?> params = pcf.getParameters();
		while (params.hasMoreElements()) {
			PCFParameter param = (PCFParameter) params.nextElement();

			if (param.getParameter() == paramId) {
				paramsList.add(param);
			}
		}

		return paramsList.toArray(new PCFParameter[0]);
	}

	private static final Map<String, Integer> PCF_PARAMS_CACHE = new HashMap<>();

	/**
	 * Translates PCF parameter MQ constant name to constant numeric value.
	 *
	 * @param paramIdStr
	 *            PCF parameter MQ constant name
	 *
	 * @return PCF parameter MQ constant numeric value
	 */
	public static Integer getParamId(String paramIdStr) throws NoSuchElementException {
		Integer paramId = PCF_PARAMS_CACHE.get(paramIdStr);

		if (paramId == null) {
			try {
				paramId = Integer.parseInt(paramIdStr);
			} catch (NumberFormatException nfe) {
				paramId = MQConstants.getIntValue(paramIdStr);
			}

			PCF_PARAMS_CACHE.put(paramIdStr, paramId);
		}

		return paramId;
	}

	/**
	 * Generates a new unique message signature. This signature is expected to be used for creating a new message
	 * instance, and is intended to uniquely identify the message regardless of which application is processing it.
	 * <p>
	 * It is up to the individual stream to determine which of these attributes is available/required to uniquely
	 * identify a message. In order to identify a message within two different transports, the streams for each
	 * transport must provide the same values.
	 *
	 * @param elements
	 *            elements array to calculate signature
	 * @return unique message signature
	 */
	public static String computeSignature(Object... elements) {
		synchronized (MSG_DIGEST) {
			return computeSignature(MSG_DIGEST, elements);
		}
	}

	/**
	 * Generates a new unique message signature. This signature is expected to be used for creating a new message
	 * instance, and is intended to uniquely identify the message regardless of which application is processing it.
	 * <p>
	 * It is up to the individual stream to determine which of these attributes is available/required to uniquely
	 * identify a message. In order to identify a message within two different transports, the streams for each
	 * transport must provide the same values.
	 *
	 * @param _msgDigest
	 *            message type
	 * @param elements
	 *            elements array to calculate signature
	 * @return unique message signature
	 */
	public static String computeSignature(MessageDigest _msgDigest, Object... elements) {
		_msgDigest.reset();

		if (elements != null) {
			for (Object element : elements) {
				if (element == null) {
					continue;
				}

				if (element instanceof MessageType) {
					_msgDigest.update(String.valueOf(((MessageType) element).value()).getBytes());
				} else if (element instanceof byte[]) {
					_msgDigest.update((byte[]) element);
				} else if (element instanceof String) {
					_msgDigest.update(((String) element).trim().getBytes());
				} else if (element.getClass().isEnum()) {
					_msgDigest.update(((Enum<?>) element).name().getBytes());
				} else {
					String elemStr = Utils.toString(element);
					_msgDigest.update(elemStr.trim().getBytes());
				}
			}
		}

		return Utils.base64EncodeStr(_msgDigest.digest());
	}

	/**
	 * This method applies custom handling for setting field values. This method will construct the signature to use for
	 * the message from the specified value, which is assumed to be a string containing the inputs required for the
	 * message signature calculation, with each input separated by the delimiter specified using parameter
	 * {@code sigDelim}.
	 * 
	 * @param value
	 *            value object to retrieve signature fields data
	 * @param sigDelim
	 *            signature delimiter
	 * @param logger
	 *            logger to log signature calculation messages
	 * @return unique message signature, or {@code null} if {@code value} contained signature calculation items are
	 *         empty
	 *
	 * @see #computeSignature(Object...)
	 */
	public static Object computeSignature(Object value, String sigDelim, EventSink logger) {
		Object[] sigItems;
		if (value instanceof String) {
			String sigStr = (String) value;
			sigItems = sigStr.split(Pattern.quote(sigDelim));
		} else {
			sigItems = Utils.makeArray(value);
		}

		if (Utils.isEmptyContent(sigItems)) {
			logger.log(OpLevel.DEBUG, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
					"WmqUtils.msg.signature.null.elements");
			return null;
		}

		if (isEmptyItems(sigItems)) {
			logger.log(OpLevel.DEBUG, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
					"WmqUtils.msg.signature.empty.elements", sigItems);
			return null;
		}

		value = computeSignature(sigItems);
		logger.log(OpLevel.TRACE, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
				"WmqUtils.msg.signature", value, sigItems.length, Utils.toStringDeep(sigItems));

		if ("1B2M2Y8AsgTpgAmY7PhCfg==".equals(value)) { // NON-NLS
			logger.log(OpLevel.DEBUG, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
					"WmqUtils.msg.signature.md5.default.value", value);
			return null;
		}

		return value;
	}

	private static boolean isEmptyItems(Object... items) {
		if (items != null) {
			for (Object item : items) {
				if (!isEmptyItem(item)) {
					return false;
				}
			}
		}

		return true;
	}

	private static boolean isEmptyItem(Object obj) {
		if (obj == null) {
			return true;
		}
		if (obj instanceof String) {
			return StringUtils.isEmpty((String) obj);
		}
		if (obj instanceof byte[]) {
			return isEmptyId((byte[]) obj);
		}

		return false;
	}

	/**
	 * Checks whether identifier is empty: is {@code null} or contains only {@code 0} values.
	 *
	 * @param id
	 *            identifier to check
	 * @return {@code true} if {@code id} is {@code null} and contains only {@code 0} elements, {@code false} -
	 *         otherwise
	 */
	private static boolean isEmptyId(byte[] id) {
		if (id != null) {
			for (byte b : id) {
				if (b != 0) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Returns the charset corresponding to the specified {@code ccsid} Coded Charset Identifier.
	 *
	 * @param ccsid
	 *            coded charset identifier, or {@code null} to get default ({@code ccsid=0}) charset name
	 * @return charset name mapped from coded charset identifier
	 * @throws UnsupportedEncodingException
	 *             if there is no charset mapping for the supplied {@code ccsid}
	 */
	public static String getCharsetName(Object ccsid) throws UnsupportedEncodingException {
		return CCSID.getCodepage(getCCSID(ccsid));
	}

	/**
	 * Converts byte array content in the specified {@code ccsid} into a Java {@link java.lang.String}.
	 * <p>
	 * Does all the same as {@link #getString(byte[], int, int, Object)} setting {@code offset} to {@code 0} and
	 * {@code length} to {@code strBytes.length}
	 *
	 * @param strBytes
	 *            the byte array to convert
	 * @param ccsid
	 *            coded charset identifier, or {@code null} to use default ({@code ccsid=0}) charset
	 * @return the string made from provided bytes using defined {@code ccsid}, or {@code null} if {@code strBytes} is
	 *         {@code null}
	 * @throws UnsupportedEncodingException
	 *             if there is no charset mapping for the supplied {@code ccsid} value or the platform cannot convert
	 *             from the charset
	 *
	 * @see #getString(byte[], int, int, Object)
	 */
	public static String getString(byte[] strBytes, Object ccsid) throws UnsupportedEncodingException {
		if (strBytes == null) {
			return null;
		}
		if (strBytes.length == 0) {
			return "";
		}

		return getString(strBytes, 0, strBytes.length, ccsid);
	}

	/**
	 * Converts byte array content in the specified {@code ccsid} into a Java {@link java.lang.String}.
	 * <p>
	 * If {@code stripHeaders} flag is set to {@code true}, and DLH/XQH headers data has CCSID value defined
	 * ({@code > 0}) within, then that CCSID value is used to convert payload from binary to string. In all other cases,
	 * parameter {@code ccsid} defined value is used.
	 *
	 * @param strBytes
	 *            the byte array to convert
	 * @param ccsid
	 *            coded charset identifier, or {@code null} to use default ({@code ccsid=0}) charset
	 * @param stripHeaders
	 *            flag indicating to remove found DLH and XQH headers data leaving only actual message payload data
	 * @return the string made from provided bytes using defined {@code ccsid}, or {@code null} if {@code strBytes} is
	 *         {@code null}
	 * @throws UnsupportedEncodingException
	 *             if there is no charset mapping for the supplied {@code ccsid} value or the platform cannot convert
	 *             from the charset
	 *
	 * @see #getString(byte[], Object, int)
	 */
	public static String getString(byte[] strBytes, Object ccsid, boolean stripHeaders)
			throws UnsupportedEncodingException {
		return getString(strBytes, ccsid, stripHeaders ? MQ_BIN_STR_STRIP_DLH_XQH : MQ_BIN_STR_PRESERVE_DLH_XQH);
	}

	/**
	 * Converts byte array content in the specified {@code ccsid} into a Java {@link java.lang.String}.
	 * <p>
	 * If {@code conversionFlags} mask contains flag {@code MQ_BIN_STR_STRIP_DLH_XQH}, and DLH/XQH headers data has
	 * CCSID value defined ({@code > 0}) within, then that CCSID value is used to convert payload from binary to string.
	 * In all other cases, parameter {@code ccsid} defined value is used.
	 *
	 * @param strBytes
	 *            the byte array to convert
	 * @param ccsid
	 *            coded charset identifier, or {@code null} to use default ({@code ccsid=0}) charset
	 * @param conversionFlags
	 *            conversion flags mask, combination of {@link #MQ_BIN_STR_PRESERVE_DLH_XQH} and
	 *            {@link #MQ_BIN_STR_STRIP_DLH_XQH} constants
	 * @return the string made from provided bytes using defined {@code ccsid}, or {@code null} if {@code strBytes} is
	 *         {@code null}
	 * @throws UnsupportedEncodingException
	 *             if there is no charset mapping for the supplied {@code ccsid} value or the platform cannot convert
	 *             from the charset
	 *
	 * @see #getPayloadOffsetAndCCSID(byte[])
	 * @see #getString(byte[], int, int, Object)
	 */
	public static String getString(byte[] strBytes, Object ccsid, int conversionFlags)
			throws UnsupportedEncodingException {
		if (strBytes == null) {
			return null;
		}
		if (strBytes.length == 0) {
			return "";
		}

		int offset = 0;
		if (Utils.matchMask(conversionFlags, MQ_BIN_STR_STRIP_DLH_XQH)) {
			int[] hd = getPayloadOffsetAndCCSID(strBytes);
			offset = hd[0];
			if (offset > 0 && hd[1] > 0) {
				ccsid = hd[1];
			}
		}

		return getString(strBytes, offset, strBytes.length - offset, getCCSID(ccsid));
	}

	/**
	 * Converts byte array content in the specified {@code ccsid} into a Java {@link java.lang.String}.
	 * 
	 * @param strBytes
	 *            the byte array to convert
	 * @param offset
	 *            index of first byte to pick for conversion
	 * @param length
	 *            amount of bytes to convert
	 * @param ccsid
	 *            coded charset identifier, or {@code null} to use default ({@code ccsid=0}) charset
	 * @return he string made from provided bytes using defined {@code ccsid}, or {@code null} if {@code strBytes} is
	 *         {@code null}
	 * @throws UnsupportedEncodingException
	 *             if there is no charset mapping for the supplied {@code ccsid} value or the platform cannot convert
	 *             from the charset
	 * @throws ArrayIndexOutOfBoundsException
	 *             if {@code offset} is out of array bounds {@code offset &lt; 0 || offset &gt;= strBytes.length} or
	 *             {@code length} in addition to {@code offset} pops out of array bounds {@code offset + length &gt;
	 *             strBytes.length}
	 */
	public static String getString(byte[] strBytes, int offset, int length, Object ccsid)
			throws UnsupportedEncodingException, ArrayIndexOutOfBoundsException {
		if (strBytes == null) {
			return null;
		}
		if (strBytes.length == 0) {
			return "";
		}

		if (offset < 0 || offset >= strBytes.length) {
			throw new ArrayIndexOutOfBoundsException(StreamsResources.getStringFormatted(
					WmqStreamConstants.RESOURCE_BUNDLE_NAME, "WmqUtils.index.out.of.range", offset, strBytes.length));
		}

		if (offset + length > strBytes.length) {
			throw new ArrayIndexOutOfBoundsException(
					StreamsResources.getStringFormatted(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
							"WmqUtils.length.out.of.range", offset, length, strBytes.length));
		}

		return Charsets.convert(strBytes, offset, length, getCCSID(ccsid));
	}

	private static int getCCSID(Object ccsidObj) throws UnsupportedEncodingException {
		if (ccsidObj instanceof Number) {
			return ((Number) ccsidObj).intValue();
		} else if (ccsidObj instanceof String) {
			try {
				return Integer.parseInt((String) ccsidObj);
			} catch (NumberFormatException exc) {
				UnsupportedEncodingException te = new UnsupportedEncodingException();
				te.initCause(exc);
				throw te;
			}
		}

		return 0;
	}

	/**
	 * Returns message payload data offset index within binary message data. Returned {@code 0} indicates whole message
	 * data is payload, having no excessive DLH or XQH data within.
	 *
	 * @param msgData
	 *            message binary data
	 * @return message payload data offset index within binary message data
	 *
	 * @see #getPayloadOffsetAndCCSID(byte[])
	 */
	public static int getPayloadOffset(byte[] msgData) {
		return getPayloadOffsetAndCCSID(msgData)[0];
	}

	/**
	 * Returns message payload data offset index within binary message data, and CCSID defined in DLH/XQH structures
	 * used to encode payload data.
	 * <p>
	 * Returned offset {@code 0} indicates whole message data is payload, having no excessive DLH or XQH data within.
	 *
	 * @param msgData
	 *            message binary data
	 * @return array of two integers, where first element is payload data offset and second element is payload data
	 *         CCSID
	 */
	public static int[] getPayloadOffsetAndCCSID(byte[] msgData) {
		return getPayloadOffsetAndCCSID(msgData, 0, 0);
	}

	private static int[] getPayloadOffsetAndCCSID(byte[] msgData, int offset, int ccsid) {
		if (msgData == null || msgData.length < offset + 4) {
			return new int[] { offset, ccsid };
		}

		String strucId = new String(msgData, offset, 4, Charset.defaultCharset());

		switch (strucId) {
		case "XQH ": // NON-NLS
			return getPayloadOffsetAndCCSID(msgData, offset + MQXQH.SIZE, getInt(msgData, offset + 132));
		case "DLH ": // NON-NLS
			return getPayloadOffsetAndCCSID(msgData, offset + MQDLH.SIZE, getInt(msgData, offset + 112));
		default:
			return new int[] { offset, ccsid };
		}
	}

	private static int getInt(byte[] bytes, int offset) {
		return ((0xFF & bytes[offset]) << 24) | ((0xFF & bytes[offset + 1]) << 16) | ((0xFF & bytes[offset + 2]) << 8)
				| (0xFF & bytes[offset + 3]);
	}

	/**
	 * Strips off found DLH and XQH headers data from binary message data and returns only message payload binary data.
	 *
	 * @param msgData
	 *            message binary data
	 * @return message binary payload data
	 */
	public static byte[] getMsgPayload(byte[] msgData) {
		int structLength = getPayloadOffsetAndCCSID(msgData)[0];
		if (structLength > 0 && structLength < msgData.length) {
			return Arrays.copyOfRange(msgData, structLength, msgData.length);
		}

		return msgData;
	}

	private static final String EOL = System.getProperty("line.separator");
	private static final String _hexcodes = "0123456789ABCDEF"; // NON-NLS
	private static final int[] _shifts = new int[] { 28, 24, 20, 16, 12, 8, 4, 0 };
	private static final char SPACE = ' ';
	private static final char CTRL_CHAR = '.';
	private static final String HEX_TEXT_SPACER = "  "; // NON-NLS

	/**
	 * Makes a HEX dump string representation of provided {@code data} byte array. Bytes to chars conversion is done
	 * according provided {@code ccsid}.
	 * 
	 * @param data
	 *            byte array make HEX dump
	 * @param ccsid
	 *            coded charset identifier
	 * @return returns HEX dump representation of provided byte array
	 */
	public static String hexDump(byte[] data, int ccsid) {
		if (ArrayUtils.isEmpty(data)) {
			return "<EMPTY>"; // NON-NLS
		}

		String hexStr;
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length * 2)) {
			hexDump(data, 0, bos, 0, ccsid);
			hexStr = EOL + bos.toString(Utils.UTF8);
		} catch (Exception exc) {
			hexStr = "HEX FAIL: " + Utils.getExceptionMessages(exc); // NON-NLS
		}

		return hexStr;
	}

	/**
	 * Resolves MQ constant name for provided value using defined constant name mask.
	 * 
	 * @param value
	 *            MQ constant value
	 * @param mask
	 *            MQ constant name mask
	 * @return resolved MQ constant name
	 */
	public static String lookupMQConstantName(Number value, String mask) {
		return MQConstants.lookup(value.intValue(), mask);
	}

	private static void hexDump(byte[] data, long offset, OutputStream stream, int index, int ccsid)
			throws IOException, ArrayIndexOutOfBoundsException, IllegalArgumentException {
		if (index >= 0 && index < data.length) {
			if (stream == null) {
				throw new IllegalArgumentException(StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
						"WmqUtils.hex.dump.null.stream"));
			} else {
				long display_offset = offset + (long) index;
				StringBuilder buffer = new StringBuilder(74);

				for (int j = index; j < data.length; j += 16) {
					int chars_read = data.length - j;
					if (chars_read > 16) {
						chars_read = 16;
					}

					dumpOffset(buffer, display_offset).append(SPACE);

					int k;
					for (k = 0; k < 16; ++k) {
						if (k < chars_read) {
							dumpByte(buffer, data[k + j]);
						} else {
							buffer.append(HEX_TEXT_SPACER);
						}

						buffer.append(SPACE);
					}

					for (k = 0; k < chars_read; ++k) {
						String chStr = getString(data, k + j, 1, ccsid);
						char ch = chStr.charAt(0);
						if (isPrintableChar(ch)) {
							buffer.append(ch);
						} else {
							buffer.append(CTRL_CHAR);
						}
					}

					buffer.append(EOL);
					stream.write(buffer.toString().getBytes(Charset.defaultCharset()));
					stream.flush();
					buffer.setLength(0);
					display_offset += chars_read;
				}

			}
		} else {
			throw new ArrayIndexOutOfBoundsException(StreamsResources.getStringFormatted(
					WmqStreamConstants.RESOURCE_BUNDLE_NAME, "WmqUtils.index.out.of.range", index, data.length));
		}
	}

	private static boolean isPrintableChar(char ch) {
		return (ch >= 32 && ch < 127) || (ch >= 160 && ch < 65277);
	}

	private static StringBuilder dumpOffset(StringBuilder _lbuffer, long value) {
		for (int j = 0; j < 8; ++j) {
			_lbuffer.append(_hexcodes.charAt((int) (value >> _shifts[j]) & 15));
		}

		return _lbuffer;
	}

	private static StringBuilder dumpByte(StringBuilder _cbuffer, byte value) {
		for (int j = 0; j < 2; ++j) {
			_cbuffer.append(_hexcodes.charAt(value >> _shifts[j + 6] & 15));
		}

		return _cbuffer;
	}
}
