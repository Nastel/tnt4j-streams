/*
 * Copyright 2014-2020 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.custom.structs;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.streams.utils.WmqUtils;

/**
 * Defines WMQ specific MQ Probe related structures.
 * <p>
 * MQ Probe specific structures:
 * <ul>
 * <li>{@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TAMQAPINT} - MQ Probe top level data</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TAMQINFO}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TAAPINTINFO}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TIME_INFO}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.APXDELTA}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.APXSIGNTR}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TAAXP}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TAAXC}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TAOD}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TAMSGOPT}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.MSGAGE}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TAMD}</li>
 * </ul>
 * <p>
 * zOS intercepted MQ call specific structures:
 * <ul>
 * <li>{@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TAZOS} - zOS intercepted MQ call top level
 * data</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TAMQBATCH} - zOS Batch MQ interceptor data</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TAMQCD} - zOS intercepted MQ call contextual
 * information</li>
 * </ul>
 * 
 * @version $Revision: 1 $
 */
public class MQProbeStructs {

	/**
	 * API exit interface on UNIX, Windows and AS/400.
	 * <p>
	 * StrucId: {@value TAMQAPINT#STRUC_ID}
	 * <p>
	 * It provides such fields and types:
	 * <ul>
	 * <li>StrucId (String)</li>
	 * <li>MqInfo ({@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TAMQINFO})</li>
	 * <li>ApiIntInfo ({@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TAAPINTINFO})</li>
	 * <li>MsgId (String)</li>
	 * <li>Message (byte[])</li>
	 * </ul>
	 */
	public static class TAMQAPINT extends MQStruct implements MQProbeRootStruct {
		public static final String STRUC_ID = "APIEXIT "; // NON-NLS

		public String strucId; // 8
		public TAMQINFO mqInfo;
		public TAAPINTINFO apiIntInfo;
		public String msgId; // 8
		public byte[] msg;
		byte[] pad;

		/**
		 * Reads bytes brom provided byte buffer {@code bb} into {@code TAMQAPINT} data structure.
		 * 
		 * @param bb
		 *            byte buffer to pull bytes
		 * @param encoding
		 *            encoding to use for conversion
		 * @param charSet
		 *            character set to use conversion
		 * @return {@code TAMQAPINT} structure build from byte buffer {@code bb} bytes
		 * 
		 * @throws UnsupportedEncodingException
		 *             if there is no charset mapping for the supplied {@code charSet} value or the platform cannot
		 *             convert from the charset
		 */
		public static TAMQAPINT read(ByteBuffer bb, int encoding, int charSet) throws UnsupportedEncodingException {
			TAMQAPINT tamqapint = new TAMQAPINT();
			tamqapint.encoding = encoding;
			tamqapint.charSet = charSet;

			bb.order(ByteOrder.LITTLE_ENDIAN);

			tamqapint.strucId = getString(bb, 8, encoding, charSet);
			tamqapint.mqInfo = TAMQINFO.read(bb, encoding, charSet);
			tamqapint.apiIntInfo = TAAPINTINFO.read(bb, encoding, charSet);
			tamqapint.msgId = getStringRaw(bb, 8, encoding, charSet);
			tamqapint.msg = getBytes(bb, tamqapint.mqInfo.dataSize);
			tamqapint.pad = getBytes(bb, bb.remaining());

			return tamqapint;
		}

		@Override
		public Map<String, Object> asMap() {
			Map<String, Object> sMap = super.asMap();

			sMap.put("StrucId", strucId); // NON-NLS
			sMap.put("MqInfo", mqInfo.asMap()); // NON-NLS
			sMap.put("ApiIntInfo", apiIntInfo.asMap()); // NON-NLS
			sMap.put("MsgId", msgId); // NON-NLS
			sMap.put("Message", msg); // NON-NLS

			return sMap;
		}
	}

	/**
	 * API exit interface context information.
	 * <p>
	 * StrucId: {@value TAAPINTINFO#STRUC_ID}
	 * <p>
	 * It provides such fields and types:
	 * <ul>
	 * <li>StrucId (String)</li>
	 * <li>HostName (String)</li>
	 * <li>ApplName (String)</li>
	 * <li>GTID (String)</li>
	 * <li>ObjName (String)</li>
	 * <li>RslvObjName (String)</li>
	 * <li>RslvQMgr (String)</li>
	 * <li>RslvObjName (String)</li>
	 * <li>IpAddress (String)</li>
	 * <li>OsType (String)</li>
	 * <li>ObjType (int)</li>
	 * <li>CpuCount (int)</li>
	 * <li>ExitTime ({@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TIME_INFO})</li>
	 * <li>ExitDelta ({@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.APXDELTA})</li>
	 * <li>ExitSigntr ({@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.APXSIGNTR})</li>
	 * <li>ExitParms ({@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TAAXP})</li>
	 * <li>ExitContext ({@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TAAXC})</li>
	 * </ul>
	 */
	public static class TAAPINTINFO extends MQStruct {
		public static final String STRUC_ID = "APIPARMS"; // NON-NLS

		public String strucId; // 8
		public String hostName; // 64
		public String applName; // 64
		public String GTID; // 320
		public String objName; // 256
		public String rslvObjName; // 64
		public String rslvQMgr; // 48+1
		public String ipAddress; // 64
		public String osType; // 256
		byte[] unused0; // 3
		public int objType; /* for szObjName */
		public int cpuCount;
		byte[] unused; // 4
		public TIME_INFO exitTime;
		public APXDELTA exitDelta;
		public APXSIGNTR exitSigntr;
		public TAAXP exitParms;
		public TAAXC exitContext;

		/**
		 * Reads bytes brom provided byte buffer {@code bb} into {@code TAAPINTINFO} data structure.
		 * 
		 * @param bb
		 *            byte buffer to pull bytes
		 * @param encoding
		 *            encoding to use for conversion
		 * @param charSet
		 *            character set to use conversion
		 * @return {@code TAAPINTINFO} structure build from byte buffer {@code bb} bytes
		 * 
		 * @throws UnsupportedEncodingException
		 *             if there is no charset mapping for the supplied {@code charSet} value or the platform cannot
		 *             convert from the charset
		 */
		public static TAAPINTINFO read(ByteBuffer bb, int encoding, int charSet) throws UnsupportedEncodingException {
			TAAPINTINFO taapintinfo = new TAAPINTINFO();
			taapintinfo.encoding = encoding;
			taapintinfo.charSet = charSet;

			taapintinfo.strucId = getString(bb, 8, encoding, charSet);
			taapintinfo.hostName = getString(bb, 64, encoding, charSet);
			taapintinfo.applName = getString(bb, 64, encoding, charSet);
			taapintinfo.GTID = getString(bb, 320, encoding, charSet);
			taapintinfo.objName = getString(bb, 256, encoding, charSet);
			taapintinfo.rslvObjName = getString(bb, 64, encoding, charSet);
			taapintinfo.rslvQMgr = getString(bb, 49, encoding, charSet);
			taapintinfo.ipAddress = getString(bb, 64, encoding, charSet);
			taapintinfo.osType = getString(bb, 256, encoding, charSet);
			taapintinfo.unused0 = getBytes(bb, 3);
			taapintinfo.objType = bb.getInt();
			taapintinfo.cpuCount = bb.getInt();
			taapintinfo.unused = getBytes(bb, 4);
			taapintinfo.exitTime = TIME_INFO.read(bb, encoding, charSet);
			taapintinfo.exitDelta = APXDELTA.read(bb, encoding, charSet);
			taapintinfo.exitSigntr = APXSIGNTR.read(bb, encoding, charSet);
			taapintinfo.exitParms = TAAXP.read(bb, encoding, charSet);
			taapintinfo.exitContext = TAAXC.read(bb, encoding, charSet);

			return taapintinfo;
		}

		@Override
		public Map<String, Object> asMap() {
			Map<String, Object> sMap = super.asMap();

			sMap.put("StrucId", strucId); // NON-NLS
			sMap.put("HostName", hostName); // NON-NLS
			sMap.put("ApplName", applName); // NON-NLS
			sMap.put("GTID", GTID); // NON-NLS
			sMap.put("ObjName", objName); // NON-NLS
			sMap.put("RslvObjName", rslvObjName); // NON-NLS
			sMap.put("RslvQMgr", rslvQMgr); // NON-NLS
			sMap.put("IpAddress", ipAddress); // NON-NLS
			sMap.put("OsType", osType); // NON-NLS
			sMap.put("ObjType", objType); // NON-NLS
			sMap.put("CpuCount", cpuCount); // NON-NLS
			sMap.put("ExitTime", exitTime.asMap()); // NON-NLS
			sMap.put("ExitDelta", exitDelta.asMap()); // NON-NLS
			sMap.put("ExitSigntr", exitSigntr.asMap()); // NON-NLS
			sMap.put("ExitParms", exitParms.asMap()); // NON-NLS
			sMap.put("ExitContext", exitContext.asMap()); // NON-NLS

			return sMap;
		}
	}

	/**
	 * MQ call (CICS) contextual information.
	 * <p>
	 * StrucId: {@value TAMQINFO#STRUC_ID}
	 * <p>
	 * It provides such fields and types:
	 * <ul>
	 * <li>StrucId (String)</li>
	 * <li>ApiType (int)</li>
	 * <li>ApiCall (int)</li>
	 * <li>CompCode (int)</li>
	 * <li>Reason (int)</li>
	 * <li>DataSize (int)</li>
	 * <li>OriginalDataSize (int)</li>
	 * <li>ObjDesc ({@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TAOD})</li>
	 * <li>MsgOpt ({@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TAMSGOPT})</li>
	 * <li>MsgAge ({@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.MSGAGE})</li>
	 * <li>MsgDesc ({@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TAMD})</li>
	 * </ul>
	 */
	public static class TAMQINFO extends MQStruct {
		public static final String STRUC_ID = "mq  "; // NON-NLS

		public String strucId; // 4
		public int apiType;
		public int apiCall;
		public int compCode;
		public int reason;
		public int dataSize;
		public int originalDataSize; /* original msg length from APIs PUT,PUT1,GET */
		byte[] pad; // 4 /* pad to make multiple of 8 bytes */
		public TAOD objDesc;
		public TAMSGOPT msgOpt;
		public MSGAGE msgAge;
		public TAMD msgDesc;

		/**
		 * Reads bytes brom provided byte buffer {@code bb} into {@code TAMQINFO} data structure.
		 * 
		 * @param bb
		 *            byte buffer to pull bytes
		 * @param encoding
		 *            encoding to use for conversion
		 * @param charSet
		 *            character set to use conversion
		 * @return {@code TAMQINFO} structure build from byte buffer {@code bb} bytes
		 * 
		 * @throws UnsupportedEncodingException
		 *             if there is no charset mapping for the supplied {@code charSet} value or the platform cannot
		 *             convert from the charset
		 */
		public static TAMQINFO read(ByteBuffer bb, int encoding, int charSet) throws UnsupportedEncodingException {
			TAMQINFO tamqinfo = new TAMQINFO();
			tamqinfo.encoding = encoding;
			tamqinfo.charSet = charSet;

			tamqinfo.strucId = getString(bb, 4, encoding, charSet);
			tamqinfo.apiType = bb.getInt();
			tamqinfo.apiCall = bb.getInt();
			tamqinfo.compCode = bb.getInt();
			tamqinfo.reason = bb.getInt();
			tamqinfo.dataSize = bb.getInt();
			tamqinfo.originalDataSize = bb.getInt();
			tamqinfo.pad = getBytes(bb, 4);
			tamqinfo.objDesc = TAOD.read(bb, encoding, charSet);
			// bb.order(ByteOrder.LITTLE_ENDIAN);
			tamqinfo.msgOpt = TAMSGOPT.read(bb, encoding, charSet);
			tamqinfo.msgAge = MSGAGE.read(bb, encoding, charSet);
			tamqinfo.msgDesc = TAMD.read(bb, encoding, charSet);

			return tamqinfo;
		}

		@Override
		public Map<String, Object> asMap() {
			Map<String, Object> sMap = super.asMap();

			sMap.put("StrucId", strucId); // NON-NLS
			sMap.put("ApiType", apiType); // NON-NLS
			sMap.put("ApiCall", apiCall); // NON-NLS
			sMap.put("CompCode", compCode); // NON-NLS
			sMap.put("Reason", reason); // NON-NLS
			sMap.put("DataSize", dataSize); // NON-NLS
			sMap.put("OriginalDataSize", originalDataSize); // NON-NLS
			sMap.put("ObjDesc", objDesc.asMap()); // NON-NLS
			sMap.put("MsgOpt", msgOpt.asMap()); // NON-NLS
			sMap.put("MsgAge", msgAge.asMap()); // NON-NLS
			sMap.put("MsgDesc", msgDesc.asMap()); // NON-NLS

			return sMap;
		}
	}

	/**
	 * OBJ DESC structure (similar to MQOD).
	 * <p>
	 * StrucId: {@value TAOD#STRUC_ID}
	 * <p>
	 * It provides such fields and types:
	 * <ul>
	 * <li>StrucId (String)</li>
	 * <li>ObjectType (int)</li>
	 * <li>ObjectName (String)</li>
	 * <li>ObjectQMgrName (String)</li>
	 * <li>DynamicQName (String)</li>
	 * <li>AlternateUserId (String)</li>
	 * <li>ResolvedQName (String)</li>
	 * <li>ResolvedQMgrName (String)</li>
	 * </ul>
	 */
	public static class TAOD extends MQStruct {
		public static final String STRUC_ID = ""; // NON-NLS

		public String strucId; // 4
		public int objectType;
		public String objectName; // 48
		public String objectQMgrName; // 48
		public String dynamicQName; // 48
		public String alternateUserId; // 12
		public String resolvedQName; // 48
		public String resolvedQMgrName; // 48
		byte[] unused; // 28

		/**
		 * Reads bytes brom provided byte buffer {@code bb} into {@code TAOD} data structure.
		 * 
		 * @param bb
		 *            byte buffer to pull bytes
		 * @param encoding
		 *            encoding to use for conversion
		 * @param charSet
		 *            character set to use conversion
		 * @return {@code TAOD} structure build from byte buffer {@code bb} bytes
		 * 
		 * @throws UnsupportedEncodingException
		 *             if there is no charset mapping for the supplied {@code charSet} value or the platform cannot
		 *             convert from the charset
		 */
		public static TAOD read(ByteBuffer bb, int encoding, int charSet) throws UnsupportedEncodingException {
			TAOD taod = new TAOD();
			taod.encoding = encoding;
			taod.charSet = charSet;

			taod.strucId = getString(bb, 4, encoding, charSet);
			taod.objectType = bb.getInt();
			taod.objectName = getString(bb, 48, encoding, charSet);
			taod.objectQMgrName = getString(bb, 48, encoding, charSet);
			taod.dynamicQName = getString(bb, 48, encoding, charSet);
			taod.alternateUserId = getString(bb, 12, encoding, charSet);
			taod.resolvedQName = getString(bb, 48, encoding, charSet);
			taod.resolvedQMgrName = getString(bb, 48, encoding, charSet);
			taod.unused = getBytes(bb, 28);

			return taod;
		}

		@Override
		public Map<String, Object> asMap() {
			Map<String, Object> sMap = super.asMap();

			sMap.put("StrucId", strucId); // NON-NLS
			sMap.put("ObjectType", objectType); // NON-NLS
			sMap.put("ObjectName", objectName); // NON-NLS
			sMap.put("ObjectQMgrName", objectQMgrName); // NON-NLS
			sMap.put("DynamicQName", dynamicQName); // NON-NLS
			sMap.put("AlternateUserId", alternateUserId); // NON-NLS
			sMap.put("ResolvedQName", resolvedQName); // NON-NLS
			sMap.put("ResolvedQMgrName", resolvedQName); // NON-NLS

			return sMap;
		}
	}

	/**
	 * Message put /get options (PMO/GMO).
	 * <p>
	 * StrucId: {@value TAMSGOPT#STRUC_ID} or {@value TAMSGOPT#STRUC_ID2}
	 * <p>
	 * It provides such fields and types:
	 * <ul>
	 * <li>StrucId (String)</li>
	 * <li>Version (int)</li>
	 * <li>Options (int)</li>
	 * <li>WaitInterval (int)</li>
	 * <li>ResolvedQName (String)</li>
	 * <li>ResolvedQMgrName (String)</li>
	 * </ul>
	 */
	public static class TAMSGOPT extends MQStruct {
		public static final String STRUC_ID = "GMO "; // NON-NLS
		public static final String STRUC_ID2 = "PMO "; // NON-NLS

		public String strucId; // 4
		public int version;
		public int options;
		public int waitInterval;
		public String resolvedQName; // 48
		public String resolvedQMgrName; // 48

		/**
		 * Reads bytes brom provided byte buffer {@code bb} into {@code TAMSGOPT} data structure.
		 * 
		 * @param bb
		 *            byte buffer to pull bytes
		 * @param encoding
		 *            encoding to use for conversion
		 * @param charSet
		 *            character set to use conversion
		 * @return {@code TAMSGOPT} structure build from byte buffer {@code bb} bytes
		 * 
		 * @throws UnsupportedEncodingException
		 *             if there is no charset mapping for the supplied {@code charSet} value or the platform cannot
		 *             convert from the charset
		 */
		public static TAMSGOPT read(ByteBuffer bb, int encoding, int charSet) throws UnsupportedEncodingException {
			TAMSGOPT tamsgopt = new TAMSGOPT();
			tamsgopt.encoding = encoding;
			tamsgopt.charSet = charSet;

			tamsgopt.strucId = getString(bb, 4, encoding, charSet);
			tamsgopt.version = bb.getInt();
			tamsgopt.options = bb.getInt();
			tamsgopt.waitInterval = bb.getInt();
			tamsgopt.resolvedQName = getString(bb, 48, encoding, charSet);
			tamsgopt.resolvedQMgrName = getString(bb, 48, encoding, charSet);

			return tamsgopt;
		}

		@Override
		public Map<String, Object> asMap() {
			Map<String, Object> sMap = super.asMap();

			sMap.put("StrucId", strucId); // NON-NLS
			sMap.put("Version", version); // NON-NLS
			sMap.put("Options", options); // NON-NLS
			sMap.put("WaitInterval", waitInterval); // NON-NLS
			sMap.put("ResolvedQName", resolvedQName); // NON-NLS
			sMap.put("ResolvedQMgrName", resolvedQName); // NON-NLS

			return sMap;
		}
	}

	/**
	 * MQ Probe Message Descriptor.
	 * <p>
	 * StrucId: {@value TAMD#STRUC_ID}
	 * <p>
	 * It provides such fields and types:
	 * <ul>
	 * <li>StrucId (String)</li>
	 * <li>Report (int)</li>
	 * <li>MsgType (int)</li>
	 * <li>Expiry (int)</li>
	 * <li>Feedback (int)</li>
	 * <li>Encoding2 (int)</li>
	 * <li>CodedCharSetId (int)</li>
	 * <li>Format (String)</li>
	 * <li>Priority (int)</li>
	 * <li>Persistence (int)</li>
	 * <li>MsgId (byte[])</li>
	 * <li>CorrelId (byte[])</li>
	 * <li>BackoutCount (int)</li>
	 * <li>ReplyToQ (String)</li>
	 * <li>ReplyToQMgr (String)</li>
	 * <li>UserIdentifier (String)</li>
	 * <li>AccountingToken (byte[])</li>
	 * <li>ApplIdentityData (String)</li>
	 * <li>PutApplType (int)</li>
	 * <li>PutApplName (String)</li>
	 * <li>PutDate (String)</li>
	 * <li>PutTime (String)</li>
	 * <li>ApplOriginData (String)</li>
	 * </ul>
	 */
	public static class TAMD extends MQStruct {
		public static final String STRUC_ID = "MD "; // NON-NLS

		public String strucId; // 4
		public int report;
		public int msgType;
		public int expiry;
		public int feedback;
		public int encoding2;
		public int codedCharSetId;
		public String format; // 8
		public int priority;
		public int persistence;
		public byte[] msgId; // 24
		public byte[] correlId; // 24
		public int backoutCount;
		public String replyToQ; // 48
		public String replyToQMgr; // 48
		public String userIdentifier; // 12
		public byte[] accountingToken; // 32
		public String applIdentityData; // 32
		public int putApplType;
		public String putApplName; // 28
		public String putDate; // 8
		public String putTime; // 8
		public String applOriginData; // 4

		/**
		 * Reads bytes brom provided byte buffer {@code bb} into {@code TAMD} data structure.
		 * 
		 * @param bb
		 *            byte buffer to pull bytes
		 * @param encoding
		 *            encoding to use for conversion
		 * @param charSet
		 *            character set to use conversion
		 * @return {@code TAMD} structure build from byte buffer {@code bb} bytes
		 * 
		 * @throws UnsupportedEncodingException
		 *             if there is no charset mapping for the supplied {@code charSet} value or the platform cannot
		 *             convert from the charset
		 */
		public static TAMD read(ByteBuffer bb, int encoding, int charSet) throws UnsupportedEncodingException {
			TAMD tamd = new TAMD();
			tamd.encoding = encoding;
			tamd.charSet = charSet;

			tamd.strucId = getString(bb, 4, encoding, charSet);
			tamd.report = bb.getInt();
			tamd.msgType = bb.getInt();
			tamd.expiry = bb.getInt();
			tamd.feedback = bb.getInt();
			tamd.encoding2 = bb.getInt();
			tamd.codedCharSetId = bb.getInt();
			tamd.format = getString(bb, 8, encoding, charSet);
			tamd.priority = bb.getInt();
			tamd.persistence = bb.getInt();
			tamd.msgId = getBytes(bb, 24);
			tamd.correlId = getBytes(bb, 24);
			tamd.backoutCount = bb.getInt();
			tamd.replyToQ = getString(bb, 48, encoding, charSet);
			tamd.replyToQMgr = getString(bb, 48, encoding, charSet);
			tamd.userIdentifier = getString(bb, 12, encoding, charSet);
			tamd.accountingToken = getBytes(bb, 32);
			tamd.applIdentityData = getString(bb, 32, encoding, charSet);
			tamd.putApplType = bb.getInt();
			tamd.putApplName = getString(bb, 28, encoding, charSet);
			tamd.putDate = getString(bb, 8, encoding, charSet);
			tamd.putTime = getString(bb, 8, encoding, charSet);
			tamd.applOriginData = getString(bb, 4, encoding, charSet);

			return tamd;
		}

		@Override
		public Map<String, Object> asMap() {
			Map<String, Object> sMap = super.asMap();

			sMap.put("StrucId", strucId); // NON-NLS
			sMap.put("Report", report); // NON-NLS
			sMap.put("MsgType", msgType); // NON-NLS
			sMap.put("Expiry", expiry); // NON-NLS
			sMap.put("Feedback", feedback); // NON-NLS
			sMap.put("Encoding2", encoding2); // NON-NLS
			sMap.put("CodedCharSetId", codedCharSetId); // NON-NLS
			sMap.put("Format", format); // NON-NLS
			sMap.put("Priority", priority); // NON-NLS
			sMap.put("Persistence", persistence); // NON-NLS
			sMap.put("MsgId", msgId); // NON-NLS
			sMap.put("CorrelId", correlId); // NON-NLS
			sMap.put("BackoutCount", backoutCount); // NON-NLS
			sMap.put("ReplyToQ", replyToQ); // NON-NLS
			sMap.put("ReplyToQMgr", replyToQMgr); // NON-NLS
			sMap.put("UserIdentifier", userIdentifier); // NON-NLS
			sMap.put("AccountingToken", accountingToken); // NON-NLS
			sMap.put("ApplIdentityData", applIdentityData); // NON-NLS
			sMap.put("PutApplType", putApplType); // NON-NLS
			sMap.put("PutApplName", putApplName); // NON-NLS
			sMap.put("PutDate", putDate); // NON-NLS
			sMap.put("PutTime", putTime); // NON-NLS
			sMap.put("ApplOriginData", applOriginData); // NON-NLS

			return sMap;
		}
	}

	/**
	 * Exit time structure.
	 * <p>
	 * It provides such fields and types:
	 * <ul>
	 * <li>Time_sec (int)</li>
	 * <li>Time_mls (int)</li>
	 * <li>Time_usec (int)</li>
	 * <li>Time_tz (int)</li>
	 * <li>Offset_secs (int)</li>
	 * <li>Offset_msecs (int)</li>
	 * <li>Timer (double)</li>
	 * </ul>
	 */
	public static class TIME_INFO extends MQStruct {
		public int time_sec;
		public int time_mls;
		public int time_usec;
		public int time_tz;
		public int offset_secs;
		public int offset_msecs;
		public double timer;

		/**
		 * Reads bytes brom provided byte buffer {@code bb} into {@code TIME_INFO} data structure.
		 * 
		 * @param bb
		 *            byte buffer to pull bytes
		 * @param encoding
		 *            encoding to use for conversion
		 * @param charSet
		 *            character set to use conversion
		 * @return {@code TIME_INFO} structure build from byte buffer {@code bb} bytes
		 * 
		 * @throws UnsupportedEncodingException
		 *             if there is no charset mapping for the supplied {@code charSet} value or the platform cannot
		 *             convert from the charset
		 */
		public static TIME_INFO read(ByteBuffer bb, int encoding, int charSet) throws UnsupportedEncodingException {
			TIME_INFO time = new TIME_INFO();
			time.encoding = encoding;
			time.charSet = charSet;

			time.time_sec = bb.getInt();
			time.time_mls = bb.getInt();
			time.time_usec = bb.getInt();
			time.time_tz = bb.getInt();
			time.offset_secs = bb.getInt();
			time.offset_msecs = bb.getInt();
			time.timer = bb.getDouble();

			return time;
		}

		@Override
		public Map<String, Object> asMap() {
			Map<String, Object> sMap = super.asMap();

			sMap.put("Time_sec", time_sec); // NON-NLS
			sMap.put("Time_mls", time_mls); // NON-NLS
			sMap.put("Time_usec", time_usec); // NON-NLS
			sMap.put("Time_tz", time_tz); // NON-NLS
			sMap.put("Offset_secs", offset_secs); // NON-NLS
			sMap.put("Offset_msecs", offset_msecs); // NON-NLS
			sMap.put("Timer", timer); // NON-NLS

			return sMap;
		}
	}

	/**
	 * API EXIT time delta is difference in time between before/after exit.
	 * <p>
	 * It provides such fields and types:
	 * <ul>
	 * <li>Delta_sec (int)</li>
	 * <li>Delta_usec (int)</li>
	 * </ul>
	 */
	public static class APXDELTA extends MQStruct {
		public int delta_sec;
		public int delta_usec;

		/**
		 * Reads bytes brom provided byte buffer {@code bb} into {@code APXDELTA} data structure.
		 * 
		 * @param bb
		 *            byte buffer to pull bytes
		 * @param encoding
		 *            encoding to use for conversion
		 * @param charSet
		 *            character set to use conversion
		 * @return {@code APXDELTA} structure build from byte buffer {@code bb} bytes
		 * 
		 * @throws UnsupportedEncodingException
		 *             if there is no charset mapping for the supplied {@code charSet} value or the platform cannot
		 *             convert from the charset
		 */
		public static APXDELTA read(ByteBuffer bb, int encoding, int charSet) throws UnsupportedEncodingException {
			APXDELTA apxdelta = new APXDELTA();
			apxdelta.encoding = encoding;
			apxdelta.charSet = charSet;

			apxdelta.delta_sec = bb.getInt();
			apxdelta.delta_usec = bb.getInt();

			return apxdelta;
		}

		@Override
		public Map<String, Object> asMap() {
			Map<String, Object> sMap = super.asMap();

			sMap.put("Delta_sec", delta_sec); // NON-NLS
			sMap.put("Delta_usec", delta_usec); // NON-NLS

			return sMap;
		}
	}

	/**
	 * API EXIT signature (session and LUW).
	 * <p>
	 * It provides such fields and types:
	 * <ul>
	 * <li>ReadCount (int)</li>
	 * <li>WriteCount (int)</li>
	 * <li>OtherCount (int)</li>
	 * <li>LuwType (int)</li>
	 * <li>SesSig (byte[])</li>
	 * <li>LuwSig (byte[])</li>
	 * <li>MsgSig (byte[])</li>
	 * <li>SesSigLen (int)</li>
	 * <li>LuwSigLen (int)</li>
	 * <li>MsgSigLen (int)</li>
	 * </ul>
	 */
	public static class APXSIGNTR extends MQStruct {
		public int readCount;
		public int writeCount;
		public int otherCount;
		public int luwType;
		public byte[] sesSig; // 16
		public byte[] luwSig; // 16
		public byte[] msgSig; // 36+1
		byte[] unused; // 3 /* pad variable to next multiple of 8 bytes */
		public int sesSigLen;
		public int luwSigLen;
		public int msgSigLen;
		byte[] unused2; // 4 /* pad struct to next multiple of 8 bytes */

		/**
		 * Reads bytes brom provided byte buffer {@code bb} into {@code APXSIGNTR} data structure.
		 * 
		 * @param bb
		 *            byte buffer to pull bytes
		 * @param encoding
		 *            encoding to use for conversion
		 * @param charSet
		 *            character set to use conversion
		 * @return {@code APXSIGNTR} structure build from byte buffer {@code bb} bytes
		 * 
		 * @throws UnsupportedEncodingException
		 *             if there is no charset mapping for the supplied {@code charSet} value or the platform cannot
		 *             convert from the charset
		 */
		public static APXSIGNTR read(ByteBuffer bb, int encoding, int charSet) throws UnsupportedEncodingException {
			APXSIGNTR apxsigntr = new APXSIGNTR();
			apxsigntr.encoding = encoding;
			apxsigntr.charSet = charSet;

			apxsigntr.readCount = bb.getInt();
			apxsigntr.writeCount = bb.getInt();
			apxsigntr.otherCount = bb.getInt();
			apxsigntr.luwType = bb.getInt();
			apxsigntr.sesSig = getBytes(bb, 16);
			apxsigntr.luwSig = getBytes(bb, 16);
			apxsigntr.msgSig = getBytes(bb, 37);
			apxsigntr.unused = getBytes(bb, 3);
			apxsigntr.sesSigLen = bb.getInt();
			apxsigntr.luwSigLen = bb.getInt();
			apxsigntr.msgSigLen = bb.getInt();
			apxsigntr.unused2 = getBytes(bb, 4);

			return apxsigntr;
		}

		@Override
		public Map<String, Object> asMap() {
			Map<String, Object> sMap = super.asMap();

			sMap.put("ReadCount", readCount); // NON-NLS
			sMap.put("WriteCount", writeCount); // NON-NLS
			sMap.put("OtherCount", otherCount); // NON-NLS
			sMap.put("LuwType", luwType); // NON-NLS
			sMap.put("SesSig", sesSig); // NON-NLS
			sMap.put("LuwSig", luwSig); // NON-NLS
			sMap.put("MsgSig", msgSig); // NON-NLS
			sMap.put("SesSigLen", sesSigLen); // NON-NLS
			sMap.put("LuwSigLen", luwSigLen); // NON-NLS
			sMap.put("MsgSigLen", msgSigLen); // NON-NLS

			return sMap;
		}
	}

	/**
	 * API EXIT time delta between PUTDATE/TIME of message put and current time after message get.
	 * <p>
	 * It provides such fields and types:
	 * <ul>
	 * <li>MsgAge_sec (int)</li>
	 * <li>MsgAge_usec (int)</li>
	 * </ul>
	 */
	public static class MSGAGE extends MQStruct {
		public int msgage_sec;
		public int msgage_usec;

		/**
		 * Reads bytes brom provided byte buffer {@code bb} into {@code MSGAGE} data structure.
		 * 
		 * @param bb
		 *            byte buffer to pull bytes
		 * @param encoding
		 *            encoding to use for conversion
		 * @param charSet
		 *            character set to use conversion
		 * @return {@code MSGAGE} structure build from byte buffer {@code bb} bytes
		 * 
		 * @throws UnsupportedEncodingException
		 *             if there is no charset mapping for the supplied {@code charSet} value or the platform cannot
		 *             convert from the charset
		 */
		static MSGAGE read(ByteBuffer bb, int encoding, int charSet) throws UnsupportedEncodingException {
			MSGAGE msgage = new MSGAGE();
			msgage.encoding = encoding;
			msgage.charSet = charSet;

			msgage.msgage_sec = bb.getInt();
			msgage.msgage_usec = bb.getInt();

			return msgage;
		}

		@Override
		public Map<String, Object> asMap() {
			Map<String, Object> sMap = super.asMap();

			sMap.put("MsgAge_sec", msgage_sec); // NON-NLS
			sMap.put("MsgAge_usec", msgage_usec); // NON-NLS

			return sMap;
		}
	}

	/**
	 * API EXIT AXP structure (similar to MQAXP).
	 * <p>
	 * StrucId: {@value TAAXP#STRUC_ID}
	 * <p>
	 * It provides such fields and types:
	 * <ul>
	 * <li>StrucId (String)</li>
	 * <li>ExitId (int)</li>
	 * <li>ExitReason (int)</li>
	 * <li>ExitResponse (int)</li>
	 * <li>ExitResponse2 (int)</li>
	 * <li>Feedback (int)</li>
	 * <li>ApiCallerType (int)</li>
	 * <li>Function (int)</li>
	 * <li>QMgrName (String)</li>
	 * </ul>
	 */
	public static class TAAXP extends MQStruct {
		public static final String STRUC_ID = "AXP "; // NON-NLS

		public String strucId; // 4
		public int exitId;
		public int exitReason;
		public int exitResponse;
		public int exitResponse2;
		public int feedback;
		public int apiCallerType;
		public int function;
		public String qMgrName; // 48

		/**
		 * Reads bytes brom provided byte buffer {@code bb} into {@code TAAXP} data structure.
		 * 
		 * @param bb
		 *            byte buffer to pull bytes
		 * @param encoding
		 *            encoding to use for conversion
		 * @param charSet
		 *            character set to use conversion
		 * @return {@code TAAXP} structure build from byte buffer {@code bb} bytes
		 * 
		 * @throws UnsupportedEncodingException
		 *             if there is no charset mapping for the supplied {@code charSet} value or the platform cannot
		 *             convert from the charset
		 */
		public static TAAXP read(ByteBuffer bb, int encoding, int charSet) throws UnsupportedEncodingException {
			TAAXP taaxp = new TAAXP();
			taaxp.encoding = encoding;
			taaxp.charSet = charSet;

			taaxp.strucId = getString(bb, 4, encoding, charSet);
			taaxp.exitId = bb.getInt();
			taaxp.exitReason = bb.getInt();
			taaxp.exitResponse = bb.getInt();
			taaxp.exitResponse2 = bb.getInt();
			taaxp.feedback = bb.getInt();
			taaxp.apiCallerType = bb.getInt();
			taaxp.function = bb.getInt();
			taaxp.qMgrName = getString(bb, 48, encoding, charSet);

			return taaxp;
		}

		@Override
		public Map<String, Object> asMap() {
			Map<String, Object> sMap = super.asMap();

			sMap.put("StrucId", strucId); // NON-NLS
			sMap.put("ExitId", exitId); // NON-NLS
			sMap.put("ExitReason", exitReason); // NON-NLS
			sMap.put("ExitResponse", exitResponse); // NON-NLS
			sMap.put("ExitResponse2", exitResponse2); // NON-NLS
			sMap.put("Feedback", feedback); // NON-NLS
			sMap.put("ApiCallerType", apiCallerType); // NON-NLS
			sMap.put("Function", function); // NON-NLS
			sMap.put("QMgrName", qMgrName); // NON-NLS

			return sMap;
		}
	}

	/**
	 * API EXIT AXC structure (similar to MQAXC).
	 * <p>
	 * StrucId: {@value TAAXC#STRUC_ID}
	 * <p>
	 * It provides such fields and types:
	 * <ul>
	 * <li>StrucId (String)</li>
	 * <li>Environment (int)</li>
	 * <li>UserId (String)</li>
	 * <li>SecurityId (byte[])</li>
	 * <li>ConnectionName (String)</li>
	 * <li>ApplName (String)</li>
	 * <li>ApplType (int)</li>
	 * <li>ProcessId (int)</li>
	 * <li>ThreadId (int)</li>
	 * </ul>
	 */
	public static class TAAXC extends MQStruct {
		public static final String STRUC_ID = "AXC "; // NON-NLS

		public String strucId; // 4
		public int environment;
		public String userId; // 12
		public byte[] securityId; // 40
		public String connectionName; // 264
		public String applName; // 28
		public int applType;
		public int processId; // int?
		public int threadId; // int?
		byte[] unused; // 28 byte[]?

		/**
		 * Reads bytes brom provided byte buffer {@code bb} into {@code TAAXC} data structure.
		 * 
		 * @param bb
		 *            byte buffer to pull bytes
		 * @param encoding
		 *            encoding to use for conversion
		 * @param charSet
		 *            character set to use conversion
		 * @return {@code TAAXC} structure build from byte buffer {@code bb} bytes
		 * 
		 * @throws UnsupportedEncodingException
		 *             if there is no charset mapping for the supplied {@code charSet} value or the platform cannot
		 *             convert from the charset
		 */
		public static TAAXC read(ByteBuffer bb, int encoding, int charSet) throws UnsupportedEncodingException {
			TAAXC taaxc = new TAAXC();
			taaxc.encoding = encoding;
			taaxc.charSet = charSet;

			taaxc.strucId = getString(bb, 4, encoding, charSet);
			taaxc.environment = bb.getInt();
			taaxc.userId = getString(bb, 12, encoding, charSet);
			taaxc.securityId = getBytes(bb, 40);
			taaxc.connectionName = getString(bb, 264, encoding, charSet);
			taaxc.applName = getString(bb, 28, encoding, charSet);
			taaxc.applType = bb.getInt();
			taaxc.processId = bb.getInt();
			taaxc.threadId = bb.getInt();
			taaxc.unused = getBytes(bb, 28);

			return taaxc;
		}

		@Override
		public Map<String, Object> asMap() {
			Map<String, Object> sMap = super.asMap();

			sMap.put("StrucId", strucId); // NON-NLS
			sMap.put("Environment", environment); // NON-NLS
			sMap.put("UserId", userId); // NON-NLS
			sMap.put("SecurityId", securityId); // NON-NLS
			sMap.put("ConnectionName", connectionName); // NON-NLS
			sMap.put("ApplName", applName); // NON-NLS
			sMap.put("ApplType", applType); // NON-NLS
			sMap.put("ProcessId", processId); // NON-NLS
			sMap.put("ThreadId", threadId); // NON-NLS

			return sMap;
		}
	}

	/**
	 * zOS intercepted MQ call.
	 * <p>
	 * StrucId: {@value TAZOS#STRUC_ID}
	 * <p>
	 * It provides such fields and types:
	 * <ul>
	 * <li>StrucId (String)</li>
	 * <li>BatchInfo ({@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TAMQBATCH})</li>
	 * <li>Message (byte[])</li>
	 * </ul>
	 * and one of:
	 * <ul>
	 * <li>MqCallCtx ({@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TAMQCD})</li>
	 * <li>HostData ({@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TAHID})</li>
	 * <li>DB2Data ({@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TADB2})</li>
	 * <li>ExecCICSData ({@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TACCD})</li>
	 * <li>CICSMqData ({@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TAMQCICS})</li>
	 * </ul>
	 */
	public static class TAZOS extends MQZOSStruct implements MQProbeRootStruct {
		public static final String STRUC_ID = "TAZOS"; // NON-NLS

		private static final String TACON_MQH_TYPE_ZOS = "z"; // NON-NLS
		private static final String TACON_MQH_TYPE_BATCH = "b"; // NON-NLS
		private static final String TACON_MQH_TYPE_CICS = "c"; // NON-NLS
		private static final String TACON_MQH_TYPE_IMS = "i"; // NON-NLS
		private static final String TACON_MQH_TYPE_EXECCICS = "e"; // NON-NLS
		private static final String TACON_MQH_TYPE_CICSSQL = "s"; // NON-NLS
		private static final String TACON_MQH_TYPE_BATCHSQL = "t"; // NON-NLS
		private static final String TACON_MQH_TYPE_HID = "h"; // NON-NLS
		private static final String TACON_MQH_TYPE_CHEXIT = "x"; // NON-NLS

		public String strucId = STRUC_ID;
		public TAMQBATCH batchInfo;
		public MQZOSStruct interceptData;
		public byte[] msg;
		byte[] pad; // remaining buffer size

		/**
		 * Reads bytes brom provided byte buffer {@code bb} into {@code TAZOS} data structure.
		 * 
		 * @param bb
		 *            byte buffer to pull bytes
		 * @param encoding
		 *            encoding to use for conversion
		 * @param charSet
		 *            character set to use conversion
		 * @return {@code TAZOS} structure build from byte buffer {@code bb} bytes
		 * 
		 * @throws UnsupportedEncodingException
		 *             if there is no charset mapping for the supplied {@code charSet} value or the platform cannot
		 *             convert from the charset
		 */
		public static TAZOS read(ByteBuffer bb, int encoding, int charSet) throws UnsupportedEncodingException {
			TAZOS tazos = new TAZOS();
			tazos.encoding = encoding;
			tazos.charSet = charSet;

			tazos.batchInfo = TAMQBATCH.read(bb, encoding, charSet);
			switch (tazos.batchInfo.type) {
			case TACON_MQH_TYPE_BATCH:
				tazos.interceptData = TAMQCD.read(bb, encoding, charSet);
				tazos.msg = getBytes(bb, tazos.batchInfo.msgLength);
				break;
			case TACON_MQH_TYPE_HID:
				tazos.interceptData = TAHID.read(bb, encoding, charSet);
				break;
			case TACON_MQH_TYPE_ZOS:
				// tazos.interceptData = XXX.read(bb, encoding, charSet);
				break;
			case TACON_MQH_TYPE_CICS:
				bb.mark();
				String nextStrucId = getStringRaw(bb, 4, encoding, charSet);
				bb.reset();

				switch (nextStrucId) {
				case TAMQCD.STRUCT_ID:
					tazos.interceptData = TAMQCD.read(bb, encoding, charSet);
					tazos.msg = getBytes(bb, tazos.batchInfo.msgLength);
					break;
				case CMTA.STRUC_ID:
					tazos.interceptData = CMTA.read(bb, encoding, charSet);
					break;
				default:
				}
				break;
			case TACON_MQH_TYPE_CHEXIT:
				tazos.interceptData = TAMQCICS.read(bb, encoding, charSet);
				tazos.msg = getBytes(bb, ((TAMQCICS) tazos.interceptData).mqInfo.dataSize); // tazos.batchInfo.msgLength
																							// contains same value??
				break;
			case TACON_MQH_TYPE_IMS:
				// tazos.interceptData = XXX.read(bb, encoding, charSet);
				break;
			case TACON_MQH_TYPE_EXECCICS:
				tazos.interceptData = TACCD.read(bb, encoding, charSet);
				tazos.msg = getBytes(bb, ((TACCD) tazos.interceptData).origMsgLength); // tazos.batchInfo.msgLength
																						// contains same value??
				break;
			case TACON_MQH_TYPE_CICSSQL:
				tazos.interceptData = TADB2.read(bb, encoding, charSet);
				tazos.msg = getBytes(bb, ((TADB2) tazos.interceptData).sqlLen); // tazos.batchInfo.msgLength contains
																				// same value??
				break;
			case TACON_MQH_TYPE_BATCHSQL:
				// tazos.interceptData = XXX.read(bb, encoding, charSet);
				break;
			default:
			}

			tazos.pad = getBytes(bb, bb.remaining());

			return tazos;
		}

		@Override
		public Map<String, Object> asMap() {
			Map<String, Object> sMap = super.asMap();

			sMap.put("StrucId", strucId); // NON-NLS
			sMap.put("BatchInfo", batchInfo.asMap()); // NON-NLS
			switch (batchInfo.type) {
			case TACON_MQH_TYPE_BATCH:
				sMap.put("MqCallCtx", interceptData.asMap()); // NON-NLS
				sMap.put("Message", msg); // NON-NLS
				break;
			case TACON_MQH_TYPE_HID:
				sMap.put("HostData", interceptData.asMap()); // NON-NLS
				break;
			case TACON_MQH_TYPE_CICSSQL:
				sMap.put("DB2Data", interceptData.asMap()); // NON-NLS
				sMap.put("Message", msg); // NON-NLS
				break;
			case TACON_MQH_TYPE_EXECCICS:
				sMap.put("ExecCICSData", interceptData.asMap()); // NON-NLS
				sMap.put("Message", msg); // NON-NLS
				break;
			case TACON_MQH_TYPE_CICS:
				if (interceptData instanceof CMTA) {
					sMap.put("CICSMqTasks", interceptData.asMap()); // NON-NLS
				} else if (interceptData instanceof TAMQCD) {
					sMap.put("CICSMqCallCtx", interceptData.asMap()); // NON-NLS
					sMap.put("Message", msg); // NON-NLS
				}
				break;
			case TACON_MQH_TYPE_CHEXIT:
				sMap.put("ChExitData", interceptData.asMap()); // NON-NLS
				sMap.put("Message", msg); // NON-NLS
				break;
			default:
			}

			return sMap;
		}
	}

	/**
	 * zOS Batch MQ interceptor data.
	 * <p>
	 * StrucId: {@value TAMQBATCH#STRUC_ID}
	 * <p>
	 * It provides such fields and types:
	 * <ul>
	 * <li>StrucId (String)</li>
	 * <li>StructSize (int)</li>
	 * <li>Type (String)</li>
	 * <li>TranName (String)</li>
	 * <li>TranNumber (String)</li>
	 * <li>McdOffset (int)</li>
	 * <li>McdLength (int)</li>
	 * <li>MsgOffset (int)</li>
	 * <li>MsgLength (int)</li>
	 * <li>TimeSticker (long)</li>
	 * <li>CpuCount (int)</li>
	 * <li>MipsCount (int)</li>
	 * </ul>
	 */
	public static class TAMQBATCH extends MQZOSStruct {
		public static final String STRUC_ID = "mqh "; // NON-NLS

		public String strucId; // 4
		public int structSize;
		public String type; // 1
		byte[] unused; // 3
		public String tranName; // 8 /* only 4 bytes for CICS */
		public String tranNbr; // 8 /* only 4 bytes for CICS */
		public int mcdOffset;
		public int mcdLength;
		public int msgOffset;
		public int msgLength;
		public long timeSticker; // 8
		public int cpuCount;
		public int mipsCount;
		byte[] unused2; // 4

		/**
		 * Reads bytes brom provided byte buffer {@code bb} into {@code TAMQBATCH} data structure.
		 * 
		 * @param bb
		 *            byte buffer to pull bytes
		 * @param encoding
		 *            encoding to use for conversion
		 * @param charSet
		 *            character set to use conversion
		 * @return {@code TAMQBATCH} structure build from byte buffer {@code bb} bytes
		 * 
		 * @throws UnsupportedEncodingException
		 *             if there is no charset mapping for the supplied {@code charSet} value or the platform cannot
		 *             convert from the charset
		 */
		public static TAMQBATCH read(ByteBuffer bb, int encoding, int charSet) throws UnsupportedEncodingException {
			TAMQBATCH tamqbatch = new TAMQBATCH();
			tamqbatch.encoding = encoding;
			tamqbatch.charSet = charSet;

			tamqbatch.strucId = getString(bb, 4, encoding, charSet);
			tamqbatch.structSize = bb.getInt();
			tamqbatch.type = getString(bb, 1, encoding, charSet);
			tamqbatch.unused = getBytes(bb, 3);
			tamqbatch.tranName = getString(bb, 8, encoding, charSet);
			tamqbatch.tranNbr = getString(bb, 8, encoding, charSet);
			tamqbatch.mcdOffset = bb.getInt();
			tamqbatch.mcdLength = bb.getInt();
			tamqbatch.msgOffset = bb.getInt();
			tamqbatch.msgLength = bb.getInt();
			tamqbatch.timeSticker = timestamp8(bb);
			tamqbatch.cpuCount = bb.getInt();
			tamqbatch.mipsCount = bb.getInt();
			tamqbatch.unused2 = getBytes(bb, 4);

			return tamqbatch;
		}

		@Override
		public Map<String, Object> asMap() {
			Map<String, Object> sMap = super.asMap();

			sMap.put("StrucId", strucId); // NON-NLS
			sMap.put("StructSize", structSize); // NON-NLS
			sMap.put("Type", type); // NON-NLS
			sMap.put("TranName", tranName); // NON-NLS
			sMap.put("TranNumber", tranNbr); // NON-NLS
			sMap.put("McdOffset", mcdOffset); // NON-NLS
			sMap.put("McdLength", mcdLength); // NON-NLS
			sMap.put("MsgOffset", msgOffset); // NON-NLS
			sMap.put("MsgLength", msgLength); // NON-NLS
			sMap.put("TimeSticker", timeSticker); // NON-NLS
			sMap.put("CpuCount", cpuCount); // NON-NLS
			sMap.put("MipsCount", mipsCount); // NON-NLS

			return sMap;
		}
	}

	/**
	 * zOS intercepted MQ call contextual information.
	 * <p>
	 * StrucId: {@value TAMQCD#STRUCT_ID}
	 * <p>
	 * It provides such fields and types:
	 * <ul>
	 * <li>StrucId (String)</li>
	 * <li>ApiType (int)</li>
	 * <li>ApiCall (int)</li>
	 * <li>CompCode (int)</li>
	 * <li>Reason (int)</li>
	 * <li>HandleConn (int)</li>
	 * <li>HandleObj (int)</li>
	 * <li>Options (int)</li>
	 * <li>DataSize (int)</li>
	 * <li>QMgrName (String)</li>
	 * <li>ObjectName (String)</li>
	 * <li>ObjectType (int)</li>
	 * <li>UserId (String)</li>
	 * <li>ProcessId (String)</li>
	 * <li>ThreadId (String)</li>
	 * <li>ProgramName (String)</li>
	 * <li>Offset (long)</li>
	 * <li>JobName (String)</li>
	 * <li>JobType (String)</li>
	 * <li>ExitTime (long)</li>
	 * <li>ExitDelta (String)</li>
	 * <li>MsgAge (String)</li>
	 * <li>SessionTime (long)</li>
	 * <li>LuwTime (long)</li>
	 * <li>MsgSig (byte[])</li>
	 * <li>CpuTime (long)</li>
	 * <li>NetUowId (String)</li>
	 * <li>MqiCpuTime (long)</li>
	 * <li>ClientIp (String)</li>
	 * <li>FirstTransId (String)</li>
	 * <li>TransGrpId (String)</li>
	 * <li>ClientIpAddr (String)</li>
	 * <li>ClientPort (int)</li>
	 * <li>UserCorrelator (String)</li>
	 * <li>MDOffset (int)</li>
	 * <li>MsgDesc ({@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TAMD})</li>
	 * </ul>
	 */
	public static class TAMQCD extends MQZOSStruct {
		public static final String STRUCT_ID = "mcd "; // NON-NLS

		public String strucId; // 4
		public int apiType;
		public int apiCall;
		public int compCode;
		public int reason;
		public int hConn;
		public int hObj; // MQHOBJ
		public int options;
		public int dataSize;
		public String qMgrName; // 48, but only 4 used by zOS
		byte[] align0; // 4
		public String objectName; // 256
		public int objectType;
		byte[] align01; // 20
		public String userId; // 8
		public String processId; // 8
		public String threadId; // 16
		public String programName; // 20
		public long offset;
		public String jobName; // 8
		public String jobType; // 3
		byte[] align1; // 1
		public long exitTime; // 8
		public String exitDelta; // 8
		public String msgAge; // 8
		public long sessTime; // 8
		public long luwTime; // 8
		public String luwType;
		byte[] align2; // 3
		public byte[] msgSig; // 36
		byte[] align3; // 4
		public long cpuTime; // 8
		public String netUowId; // 27
		byte[] align4; // 1
		public long mqiCpuTime; // 8
		public String clientIp; // 46
		byte[] align5; // 2
		public String firstTransId; // 4
		public String transGrpId; // 28
		public String clientIpAddr; // 40
		public int clientPort;
		public String userCorrelator; // 64
		public int mdOffset;
		public TAMD msgDesc;

		/**
		 * Reads bytes brom provided byte buffer {@code bb} into {@code TAMQCD} data structure.
		 * 
		 * @param bb
		 *            byte buffer to pull bytes
		 * @param encoding
		 *            encoding to use for conversion
		 * @param charSet
		 *            character set to use conversion
		 * @return {@code TAMQCD} structure build from byte buffer {@code bb} bytes
		 * 
		 * @throws UnsupportedEncodingException
		 *             if there is no charset mapping for the supplied {@code charSet} value or the platform cannot
		 *             convert from the charset
		 */
		public static TAMQCD read(ByteBuffer bb, int encoding, int charSet) throws UnsupportedEncodingException {
			TAMQCD tamqcd = new TAMQCD();
			tamqcd.encoding = encoding;
			tamqcd.charSet = charSet;

			tamqcd.strucId = getString(bb, 4, encoding, charSet);
			tamqcd.apiType = bb.getInt();
			tamqcd.apiCall = bb.getInt();
			tamqcd.compCode = bb.getInt();
			tamqcd.reason = bb.getInt();
			tamqcd.hConn = bb.getInt();
			tamqcd.hObj = bb.getInt(); // ???
			tamqcd.options = bb.getInt();
			tamqcd.dataSize = bb.getInt();
			tamqcd.qMgrName = getString(bb, 48, encoding, charSet);
			tamqcd.align0 = getBytes(bb, 4);
			tamqcd.objectName = getString(bb, 256, encoding, charSet);
			tamqcd.objectType = bb.getInt();
			tamqcd.align01 = getBytes(bb, 20);
			tamqcd.userId = getString(bb, 8, encoding, charSet);
			tamqcd.processId = getStringRaw(bb, 8, encoding, charSet);
			tamqcd.threadId = getStringRaw(bb, 16, encoding, charSet);
			tamqcd.programName = getString(bb, 20, encoding, charSet);
			tamqcd.offset = bb.getInt();
			tamqcd.jobName = getString(bb, 8, encoding, charSet);
			tamqcd.jobType = getString(bb, 3, encoding, charSet);
			tamqcd.align1 = getBytes(bb, 1);
			tamqcd.exitTime = timestamp8(bb);
			tamqcd.exitDelta = getStringRaw(bb, 8, encoding, charSet);
			tamqcd.msgAge = getString(bb, 8, encoding, charSet);
			tamqcd.sessTime = timestamp8(bb);
			tamqcd.luwTime = timestamp8(bb);
			tamqcd.luwType = getString(bb, 1, encoding, charSet);
			tamqcd.align2 = getBytes(bb, 3);
			tamqcd.msgSig = getBytes(bb, 36);
			tamqcd.align3 = getBytes(bb, 4);
			tamqcd.cpuTime = timestamp8(bb);
			tamqcd.netUowId = getStringRaw(bb, 27, encoding, charSet);
			tamqcd.align4 = getBytes(bb, 1);
			tamqcd.mqiCpuTime = timestamp8(bb);
			tamqcd.clientIp = getString(bb, 46, encoding, charSet);
			tamqcd.align5 = getBytes(bb, 2);
			tamqcd.firstTransId = getStringRaw(bb, 4, encoding, charSet);
			tamqcd.transGrpId = getStringRaw(bb, 28, encoding, charSet);
			tamqcd.clientIpAddr = getString(bb, 40, encoding, charSet);
			tamqcd.clientPort = bb.getInt();
			tamqcd.userCorrelator = getString(bb, 64, encoding, charSet);
			tamqcd.mdOffset = bb.getInt();
			tamqcd.msgDesc = TAMD.read(bb, encoding, charSet);

			return tamqcd;
		}

		@Override
		public Map<String, Object> asMap() {
			Map<String, Object> sMap = super.asMap();

			sMap.put("StrucId", strucId); // NON-NLS
			sMap.put("ApiType", apiType); // NON-NLS
			sMap.put("ApiCall", apiCall); // NON-NLS
			sMap.put("CompCode", compCode); // NON-NLS
			sMap.put("Reason", reason); // NON-NLS
			sMap.put("HandleConn", hConn); // NON-NLS
			sMap.put("HandleObj", hObj); // NON-NLS
			sMap.put("Options", options); // NON-NLS
			sMap.put("DataSize", dataSize); // NON-NLS
			sMap.put("QMgrName", qMgrName); // NON-NLS
			sMap.put("ObjectName", objectName); // NON-NLS
			sMap.put("ObjectType", objectType); // NON-NLS
			sMap.put("UserId", userId); // NON-NLS
			sMap.put("ProcessId", processId); // NON-NLS
			sMap.put("ThreadId", threadId); // NON-NLS
			sMap.put("ProgramName", programName); // NON-NLS
			sMap.put("Offset", offset); // NON-NLS
			sMap.put("JobName", jobName); // NON-NLS
			sMap.put("JobType", jobType); // NON-NLS
			sMap.put("ExitTime", exitTime); // NON-NLS
			sMap.put("ExitDelta", exitDelta); // NON-NLS
			sMap.put("MsgAge", msgAge); // NON-NLS
			sMap.put("SessionTime", sessTime); // NON-NLS
			sMap.put("LuwTime", luwTime); // NON-NLS
			sMap.put("MsgSig", msgSig); // NON-NLS
			sMap.put("CpuTime", cpuTime); // NON-NLS
			sMap.put("NetUowId", netUowId); // NON-NLS
			sMap.put("MqiCpuTime", mqiCpuTime); // NON-NLS
			sMap.put("ClientIp", clientIp); // NON-NLS
			sMap.put("FirstTransId", firstTransId); // NON-NLS
			sMap.put("TransGrpId", transGrpId); // NON-NLS
			sMap.put("ClientIpAddr", clientIpAddr); // NON-NLS
			sMap.put("ClientPort", clientPort); // NON-NLS
			sMap.put("UserCorrelator", userCorrelator); // NON-NLS
			sMap.put("MDOffset", mdOffset); // NON-NLS
			sMap.put("MsgDesc", msgDesc.asMap()); // NON-NLS

			return sMap;
		}
	}

	/**
	 * z/OS Host Information Data.
	 * <p>
	 * StrucId: {@value TAHID#STRUC_ID}
	 * <p>
	 * It provides such fields and types:
	 * <ul>
	 * <li>StrucId (String)</li>
	 * <li>OsName (String)</li>
	 * <li>OsRelease (String)</li>
	 * <li>HostIpLen (short)</li>
	 * <li>HostIp (String)</li>
	 * <li>HostNameLen (short)</li>
	 * <li>HostName (String)</li>
	 * </ul>
	 */
	public static class TAHID extends MQZOSStruct {
		public static final String STRUC_ID = "hid "; // NON-NLS

		public String strucId; // 4
		public String osName; // 16
		public String osRelease; // 6
		public short hostIpLen;
		public String hostIp; // 48
		public short hostNameLen;
		public String hostName; // 256;

		/**
		 * Reads bytes brom provided byte buffer {@code bb} into {@code TAHID} data structure.
		 * 
		 * @param bb
		 *            byte buffer to pull bytes
		 * @param encoding
		 *            encoding to use for conversion
		 * @param charSet
		 *            character set to use conversion
		 * @return {@code TAHID} structure build from byte buffer {@code bb} bytes
		 * 
		 * @throws UnsupportedEncodingException
		 *             if there is no charset mapping for the supplied {@code charSet} value or the platform cannot
		 *             convert from the charset
		 */
		public static TAHID read(ByteBuffer bb, int encoding, int charSet) throws UnsupportedEncodingException {
			TAHID tahid = new TAHID();
			tahid.encoding = encoding;
			tahid.charSet = charSet;

			tahid.strucId = getString(bb, 4, encoding, charSet);
			tahid.osName = getString(bb, 16, encoding, charSet);
			tahid.osRelease = getString(bb, 6, encoding, charSet);
			tahid.hostIpLen = bb.getShort();
			tahid.hostIp = getString(bb, 48, encoding, charSet);
			tahid.hostNameLen = bb.getShort();
			tahid.hostName = getString(bb, 256, encoding, charSet);

			return tahid;
		}

		@Override
		public Map<String, Object> asMap() {
			Map<String, Object> sMap = super.asMap();

			sMap.put("StrucId", strucId); // NON-NLS
			sMap.put("OsName", osName); // NON-NLS
			sMap.put("OsRelease", osRelease); // NON-NLS
			sMap.put("HostIpLen", hostIpLen); // NON-NLS
			sMap.put("HostIp", hostIp); // NON-NLS
			sMap.put("HostNameLen", hostNameLen); // NON-NLS
			sMap.put("HostName", hostName); // NON-NLS

			return sMap;
		}
	}

	/**
	 * z/OS DB2 SQL command.
	 * <p>
	 * StrucId: {@value TADB2#STRUC_ID}
	 * <p>
	 * It provides such fields and types:
	 * <ul>
	 * <li>StrucId (String)</li>
	 * <li>StmtType (short)</li>
	 * <li>StmtFunc (String)</li>
	 * <li>CompCode (int)</li>
	 * <li>Reason (int)</li>
	 * <li>SubSysId (String)</li>
	 * <li>DatabaseName (String)</li>
	 * <li>UserId (String)</li>
	 * <li>ProcessId (String)</li>
	 * <li>ThreadId (String)</li>
	 * <li>ProgramName (String)</li>
	 * <li>CallOffset (int)</li>
	 * <li>JobName (String)</li>
	 * <li>JobType (String)</li>
	 * <li>ExitTime (long)</li>
	 * <li>ExitDelta (String)</li>
	 * <li>LuwTime (long)</li>
	 * <li>SessTime (long)</li>
	 * <li>NetUowId (String)</li>
	 * <li>DbrmName (String)</li>
	 * <li>PlanName (String)</li>
	 * <li>PrimAuth (String)</li>
	 * <li>ConnId (String)</li>
	 * <li>CorrId (byte[])</li>
	 * <li>StmtNo (short)</li>
	 * <li>SectNo (short)</li>
	 * <li>CpuTime (long)</li>
	 * <li>SqlCpuTime (long)</li>
	 * <li>FirstTransId (String)</li>
	 * <li>TransGrpId (String)</li>
	 * <li>ClientIpAddr (String)</li>
	 * <li>ClientPort (int)</li>
	 * <li>UserCorrelator (String)</li>
	 * <li>SqlLen (int)</li>
	 * </ul>
	 */
	public static class TADB2 extends MQZOSStruct {
		public static final String STRUC_ID = "db2 "; // NON-NLS

		public String strucId; // 4
		public short stmtType;
		byte[] align1; // 2
		public String stmtFunc; // 24
		public int compCode;
		public int reason;
		public String subSysId; // 8
		public String databaseName; // 8
		public String userId; // 8
		public String processId; // 8
		public String threadId; // 16
		public String programName; // 8
		public int callOffset;
		public String jobName; // 8
		public String jobType; // 3
		byte[] align2; // 1
		public long exitTime; // 8
		public String exitDelta; // 8
		public long luwTime; // 8
		public long sessTime; // 8
		public String netUowId; // 27
		byte[] align3; // 1
		public String dbrmName; // 8
		public String planName; // 8
		public String primAuth; // 8
		public String connId; // 8
		public byte[] corrId; // 12
		public short stmtNo;
		public short sectNo;
		public long cpuTime; // 8
		public long sqlCpuTime; // 8
		public String firstTransId; // 4
		public String transGrpId; // 28
		public String clientIpAddr; // 40
		public int clientPort;
		public String userCorrelator; // 64
		public int sqlLen;

		/**
		 * Reads bytes brom provided byte buffer {@code bb} into {@code TADB2} data structure.
		 * 
		 * @param bb
		 *            byte buffer to pull bytes
		 * @param encoding
		 *            encoding to use for conversion
		 * @param charSet
		 *            character set to use conversion
		 * @return {@code TADB2} structure build from byte buffer {@code bb} bytes
		 * 
		 * @throws UnsupportedEncodingException
		 *             if there is no charset mapping for the supplied {@code charSet} value or the platform cannot
		 *             convert from the charset
		 */
		public static TADB2 read(ByteBuffer bb, int encoding, int charSet) throws UnsupportedEncodingException {
			TADB2 tadb2 = new TADB2();
			tadb2.encoding = encoding;
			tadb2.charSet = charSet;

			tadb2.strucId = getString(bb, 4, encoding, charSet);
			tadb2.stmtType = bb.getShort();
			tadb2.align1 = getBytes(bb, 2);
			tadb2.stmtFunc = getString(bb, 24, encoding, charSet);
			tadb2.compCode = bb.getInt();
			tadb2.reason = bb.getInt();
			tadb2.subSysId = getString(bb, 8, encoding, charSet);
			tadb2.databaseName = getString(bb, 8, encoding, charSet);
			tadb2.userId = getString(bb, 8, encoding, charSet);
			tadb2.processId = getStringRaw(bb, 8, encoding, charSet);
			tadb2.threadId = getStringRaw(bb, 16, encoding, charSet);
			tadb2.programName = getString(bb, 8, encoding, charSet);
			tadb2.callOffset = bb.getInt();
			tadb2.jobName = getString(bb, 8, encoding, charSet);
			tadb2.jobType = getString(bb, 3, encoding, charSet);
			tadb2.align2 = getBytes(bb, 1);
			tadb2.exitTime = timestamp8(bb);
			tadb2.exitDelta = getStringRaw(bb, 8, encoding, charSet);
			tadb2.luwTime = timestamp8(bb);
			tadb2.sessTime = timestamp8(bb);
			tadb2.netUowId = getStringRaw(bb, 27, encoding, charSet);
			tadb2.align3 = getBytes(bb, 1);
			tadb2.dbrmName = getString(bb, 8, encoding, charSet);
			tadb2.planName = getString(bb, 8, encoding, charSet);
			tadb2.primAuth = getString(bb, 8, encoding, charSet);
			tadb2.connId = getString(bb, 8, encoding, charSet);
			tadb2.corrId = getBytes(bb, 12);
			tadb2.stmtNo = bb.getShort();
			tadb2.sectNo = bb.getShort();
			tadb2.cpuTime = timestamp8(bb);
			tadb2.sqlCpuTime = timestamp8(bb);
			tadb2.firstTransId = getStringRaw(bb, 4, encoding, charSet);
			tadb2.transGrpId = getStringRaw(bb, 28, encoding, charSet);
			tadb2.clientIpAddr = getString(bb, 40, encoding, charSet);
			tadb2.clientPort = bb.getInt();
			tadb2.userCorrelator = getString(bb, 64, encoding, charSet);
			tadb2.sqlLen = bb.getInt();

			return tadb2;
		}

		@Override
		public Map<String, Object> asMap() {
			Map<String, Object> sMap = super.asMap();

			sMap.put("StrucId", strucId); // NON-NLS
			sMap.put("StmtType", stmtType); // NON-NLS
			sMap.put("StmtFunc", stmtFunc); // NON-NLS
			sMap.put("CompCode", compCode); // NON-NLS
			sMap.put("Reason", reason); // NON-NLS
			sMap.put("SubSysId", subSysId); // NON-NLS
			sMap.put("DatabaseName", databaseName); // NON-NLS
			sMap.put("UserId", userId); // NON-NLS
			sMap.put("ProcessId", processId); // NON-NLS
			sMap.put("ThreadId", threadId); // NON-NLS
			sMap.put("ProgramName", programName); // NON-NLS
			sMap.put("CallOffset", callOffset); // NON-NLS
			sMap.put("JobName", jobName); // NON-NLS
			sMap.put("JobType", jobType); // NON-NLS
			sMap.put("ExitTime", exitTime); // NON-NLS
			sMap.put("ExitDelta", exitDelta); // NON-NLS
			sMap.put("LuwTime", luwTime); // NON-NLS
			sMap.put("SessTime", sessTime); // NON-NLS
			sMap.put("NetUowId", netUowId); // NON-NLS
			sMap.put("DbrmName", dbrmName); // NON-NLS
			sMap.put("PlanName", planName); // NON-NLS
			sMap.put("PrimAuth", primAuth); // NON-NLS
			sMap.put("ConnId", connId); // NON-NLS
			sMap.put("CorrId", corrId); // NON-NLS
			sMap.put("StmtNo", stmtNo); // NON-NLS
			sMap.put("SectNo", sectNo); // NON-NLS
			sMap.put("CpuTime", cpuTime); // NON-NLS
			sMap.put("SqlCpuTime", sqlCpuTime); // NON-NLS
			sMap.put("FirstTransId", firstTransId); // NON-NLS
			sMap.put("TransGrpId", transGrpId); // NON-NLS
			sMap.put("ClientIpAddr", clientIpAddr); // NON-NLS
			sMap.put("ClientPort", clientPort); // NON-NLS
			sMap.put("UserCorrelator", userCorrelator); // NON-NLS
			sMap.put("SqlLen", sqlLen); // NON-NLS

			return sMap;
		}
	}

	/**
	 * EXEC CICS operational information.
	 * <p>
	 * StrucId: {@value TACCD#STRUC_ID}
	 * <p>
	 * It provides such fields and types:
	 * <ul>
	 * <li>StrucId (String)</li>
	 * <li>FuncCode (String)</li>
	 * <li>SubCmds (String)</li>
	 * <li>CompCode (int)</li>
	 * <li>Reason (int)</li>
	 * <li>QmgrName (String)</li>
	 * <li>UserId (String)</li>
	 * <li>ProcessId (String)</li>
	 * <li>ThreadId (String)</li>
	 * <li>ProgramName (String)</li>
	 * <li>CallOffset (int)</li>
	 * <li>JobName (String)</li>
	 * <li>JobType (String)</li>
	 * <li>ExitTime (long)</li>
	 * <li>ExitDelta (String)</li>
	 * <li>LuwTime (long)</li>
	 * <li>SessTime (long)</li>
	 * <li>ResourceName (String)</li>
	 * <li>ResourceType (byte)</li>
	 * <li>CpuTime (long)</li>
	 * <li>NetUowId (String)</li>
	 * <li>ApiCpuTime (long)</li>
	 * <li>FirstTransId (String)</li>
	 * <li>TransGrpId (String)</li>
	 * <li>ClientIpAddr (String)</li>
	 * <li>ClientPort (int)</li>
	 * <li>UserCorrelator (String)</li>
	 * <li>OrigMsgLength (short)</li>
	 * <li>MsgOffset (short)</li>
	 * </ul>
	 */
	public static class TACCD extends MQZOSStruct {
		public static final String STRUC_ID = "ccd "; // NON-NLS

		public String strucId; // 4
		public String funcCode; // 2
		public String subCmds; // 4
		byte[] align1; // 2
		public int compCode;
		public int reason;
		public String qmgrName; // 4
		public String userId; // 8
		public String processId; // 8
		public String threadId; // 16
		public String programName; // 8
		public int callOffset;
		public String jobName; // 8
		public String jobType; // 3
		byte[] align2; // 1
		public long exitTime; // 8
		public String exitDelta; // 8
		public long luwTime; // 8
		public long sessTime; // 8
		public String resourceName; // 8
		public byte resourceType;
		byte[] align3; // 3
		byte[] align4; // 4
		public long cpuTime; // 8
		public String netUowId; // 27
		byte[] align5; // 1
		public long apiCpuTime; // 8
		public String firstTransId; // 4
		public String transGrpId; // 28
		public String clientIpAddr; // 40
		public int clientPort;
		public String userCorrelator; // 64
		public short origMsgLength;
		public short msgOffset;

		/**
		 * Reads bytes brom provided byte buffer {@code bb} into {@code TACCD} data structure.
		 * 
		 * @param bb
		 *            byte buffer to pull bytes
		 * @param encoding
		 *            encoding to use for conversion
		 * @param charSet
		 *            character set to use conversion
		 * @return {@code TACCD} structure build from byte buffer {@code bb} bytes
		 * 
		 * @throws UnsupportedEncodingException
		 *             if there is no charset mapping for the supplied {@code charSet} value or the platform cannot
		 *             convert from the charset
		 */
		public static TACCD read(ByteBuffer bb, int encoding, int charSet) throws UnsupportedEncodingException {
			TACCD taccd = new TACCD();
			taccd.encoding = encoding;
			taccd.charSet = charSet;

			taccd.strucId = getString(bb, 4, encoding, charSet);
			taccd.funcCode = getString(bb, 2, encoding, charSet);
			taccd.subCmds = getString(bb, 4, encoding, charSet);
			taccd.align1 = getBytes(bb, 2);
			taccd.compCode = bb.getInt();
			taccd.reason = bb.getInt();
			taccd.qmgrName = getString(bb, 4, encoding, charSet);
			taccd.userId = getString(bb, 8, encoding, charSet);
			taccd.processId = getStringRaw(bb, 8, encoding, charSet);
			taccd.threadId = getStringRaw(bb, 16, encoding, charSet);
			taccd.programName = getString(bb, 8, encoding, charSet);
			taccd.callOffset = bb.getInt();
			taccd.jobName = getString(bb, 8, encoding, charSet);
			taccd.jobType = getString(bb, 3, encoding, charSet);
			taccd.align2 = getBytes(bb, 1);
			taccd.exitTime = timestamp8(bb);
			taccd.exitDelta = getStringRaw(bb, 8, encoding, charSet);
			taccd.luwTime = timestamp8(bb);
			taccd.sessTime = timestamp8(bb);
			taccd.resourceName = getString(bb, 8, encoding, charSet);
			taccd.resourceType = bb.get();
			taccd.align3 = getBytes(bb, 3);
			taccd.align4 = getBytes(bb, 4);
			taccd.cpuTime = timestamp8(bb);
			taccd.netUowId = getStringRaw(bb, 27, encoding, charSet);
			taccd.align5 = getBytes(bb, 1);
			taccd.apiCpuTime = timestamp8(bb);
			taccd.firstTransId = getString(bb, 4, encoding, charSet);
			taccd.transGrpId = getString(bb, 28, encoding, charSet);
			taccd.clientIpAddr = getString(bb, 40, encoding, charSet);
			taccd.clientPort = bb.getInt();
			taccd.userCorrelator = getString(bb, 64, encoding, charSet);
			taccd.origMsgLength = bb.getShort();
			taccd.msgOffset = bb.getShort();

			return taccd;
		}

		@Override
		public Map<String, Object> asMap() {
			Map<String, Object> sMap = super.asMap();

			sMap.put("StrucId", strucId); // NON-NLS
			sMap.put("FuncCode", funcCode); // NON-NLS
			sMap.put("SubCmds", subCmds); // NON-NLS
			sMap.put("CompCode", compCode); // NON-NLS
			sMap.put("Reason", reason); // NON-NLS
			sMap.put("QmgrName", qmgrName); // NON-NLS
			sMap.put("UserId", userId); // NON-NLS
			sMap.put("ProcessId", processId); // NON-NLS
			sMap.put("ThreadId", threadId); // NON-NLS
			sMap.put("ProgramName", programName); // NON-NLS
			sMap.put("CallOffset", callOffset); // NON-NLS
			sMap.put("JobName", jobName); // NON-NLS
			sMap.put("JobType", jobType); // NON-NLS
			sMap.put("ExitTime", exitTime); // NON-NLS
			sMap.put("ExitDelta", exitDelta); // NON-NLS
			sMap.put("LuwTime", luwTime); // NON-NLS
			sMap.put("SessTime", sessTime); // NON-NLS
			sMap.put("ResourceName", resourceName); // NON-NLS
			sMap.put("ResourceType", resourceType); // NON-NLS
			sMap.put("CpuTime", cpuTime); // NON-NLS
			sMap.put("NetUowId", netUowId); // NON-NLS
			sMap.put("ApiCpuTime", apiCpuTime); // NON-NLS
			sMap.put("FirstTransId", firstTransId); // NON-NLS
			sMap.put("TransGrpId", transGrpId); // NON-NLS
			sMap.put("ClientIpAddr", clientIpAddr); // NON-NLS
			sMap.put("ClientPort", clientPort); // NON-NLS
			sMap.put("UserCorrelator", userCorrelator); // NON-NLS
			sMap.put("OrigMsgLength", origMsgLength); // NON-NLS
			sMap.put("MsgOffset", msgOffset); // NON-NLS

			return sMap;
		}
	}

	/**
	 * CICS crossing exit on MVS.
	 * <p>
	 * It provides such fields and types:
	 * <ul>
	 * <li>StrucId (String)</li>
	 * <li>MqInfo ({@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TAMQINFO})</li>
	 * <li>CicsInfo ({@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TACICSINFO})</li>
	 * <li>MsgId (String)</li>
	 * </ul>
	 */
	public static class TAMQCICS extends MQZOSStruct {
		public String strucId; // 8
		public TAMQINFO mqInfo;
		public TACICSINFO cicsInfo;
		public String msgId; // 8

		/**
		 * Reads bytes brom provided byte buffer {@code bb} into {@code TAMQCICS} data structure.
		 * 
		 * @param bb
		 *            byte buffer to pull bytes
		 * @param encoding
		 *            encoding to use for conversion
		 * @param charSet
		 *            character set to use conversion
		 * @return {@code TAMQCICS} structure build from byte buffer {@code bb} bytes
		 * 
		 * @throws UnsupportedEncodingException
		 *             if there is no charset mapping for the supplied {@code charSet} value or the platform cannot
		 *             convert from the charset
		 */
		public static TAMQCICS read(ByteBuffer bb, int encoding, int charSet) throws UnsupportedEncodingException {
			TAMQCICS tamqcics = new TAMQCICS();
			tamqcics.encoding = encoding;
			tamqcics.charSet = charSet;

			tamqcics.strucId = getString(bb, 8, encoding, charSet);
			tamqcics.mqInfo = TAMQINFO.read(bb, encoding, charSet);
			tamqcics.cicsInfo = TACICSINFO.read(bb, encoding, charSet);
			tamqcics.msgId = getStringRaw(bb, 8, encoding, charSet);

			return tamqcics;
		}

		@Override
		public Map<String, Object> asMap() {
			Map<String, Object> sMap = super.asMap();

			sMap.put("StrucId", strucId); // NON-NLS
			sMap.put("MqInfo", mqInfo.asMap()); // NON-NLS
			sMap.put("CicsInfo", cicsInfo.asMap()); // NON-NLS
			sMap.put("MsgId", msgId); // NON-NLS

			return sMap;
		}
	}

	/**
	 * CICS contextual information.
	 * <p>
	 * It provides such fields and types:
	 * <ul>
	 * <li>StrucId (String)</li>
	 * <li>SysplxNum (String)</li>
	 * <li>SysplxId (String)</li>
	 * <li>CicsVer (String)</li>
	 * <li>CicsName (String)</li>
	 * <li>CicsTask (String)</li>
	 * <li>TranName (String)</li>
	 * <li>TaskNum (int)</li>
	 * <li>TermId (String)</li>
	 * <li>UserId (String)</li>
	 * <li>Timer (long)</li>
	 * </ul>
	 */
	public static class TACICSINFO extends MQZOSStruct {
		public String strucId; // 4
		public String sysplxNum; // 8
		public String sysplxId; // 2
		public String cicsVer; // 2
		public String cicsName; // 8
		public String cicsTask; // 8
		public String tranName; // 4
		public int taskNum;
		public String termId; // 4
		public String userId; // 8
		public long timer; // 8

		/**
		 * Reads bytes brom provided byte buffer {@code bb} into {@code TACICSINFO} data structure.
		 * 
		 * @param bb
		 *            byte buffer to pull bytes
		 * @param encoding
		 *            encoding to use for conversion
		 * @param charSet
		 *            character set to use conversion
		 * @return {@code TACICSINFO} structure build from byte buffer {@code bb} bytes
		 * 
		 * @throws UnsupportedEncodingException
		 *             if there is no charset mapping for the supplied {@code charSet} value or the platform cannot
		 *             convert from the charset
		 */
		public static TACICSINFO read(ByteBuffer bb, int encoding, int charSet) throws UnsupportedEncodingException {
			TACICSINFO tacicsinfo = new TACICSINFO();
			tacicsinfo.encoding = encoding;
			tacicsinfo.charSet = charSet;

			tacicsinfo.strucId = getString(bb, 4, encoding, charSet);
			tacicsinfo.sysplxNum = getString(bb, 8, encoding, charSet);
			tacicsinfo.sysplxId = getString(bb, 2, encoding, charSet);
			tacicsinfo.cicsVer = getStringRaw(bb, 2, encoding, charSet);
			tacicsinfo.cicsName = getStringRaw(bb, 8, encoding, charSet);
			tacicsinfo.cicsTask = getStringRaw(bb, 8, encoding, charSet);
			tacicsinfo.tranName = getStringRaw(bb, 4, encoding, charSet);
			tacicsinfo.taskNum = bb.getInt();
			tacicsinfo.termId = getStringRaw(bb, 4, encoding, charSet);
			tacicsinfo.userId = getStringRaw(bb, 8, encoding, charSet);
			tacicsinfo.timer = timestamp8(bb);

			return tacicsinfo;
		}

		@Override
		public Map<String, Object> asMap() {
			Map<String, Object> sMap = super.asMap();

			sMap.put("StrucId", strucId); // NON-NLS
			sMap.put("SysplxNum", sysplxNum); // NON-NLS
			sMap.put("SysplxId", sysplxId); // NON-NLS
			sMap.put("CicsVer", cicsVer); // NON-NLS
			sMap.put("CicsName", cicsName); // NON-NLS
			sMap.put("CicsTask", cicsTask); // NON-NLS
			sMap.put("TranName", tranName); // NON-NLS
			sMap.put("TaskNum", taskNum); // NON-NLS
			sMap.put("TermId", termId); // NON-NLS
			sMap.put("UserId", userId); // NON-NLS
			sMap.put("Timer", timer); // NON-NLS

			return sMap;
		}
	}

	/**
	 * CICS MQ Task Area.
	 * <p>
	 * StrucId: {@value CMTA#STRUC_ID}
	 * <p>
	 * It provides such fields and types:
	 * <ul>
	 * <li>StrucId (String)</li>
	 * <li>TcbToken (String)</li>
	 * <li>DmbDataspace (String)</li>
	 * <li>TxQueue (String)</li>
	 * <li>AptmStatus (int)</li>
	 * <li>QmgrStatus (int)</li>
	 * <li>JobName (String)</li>
	 * <li>AddressSpaceId (String)</li>
	 * <li>Flags (String)</li>
	 * <li>MqCalls (int[])</li>
	 * <li>MqSkipCount (int)</li>
	 * <li>MqAbendCount (int)</li>
	 * <li>MqTotalCount (int)</li>
	 * <li>ExcCalls (int[])</li>
	 * <li>ExcSkipCount (int)</li>
	 * <li>ExcAbendCount (int)</li>
	 * <li>ExcTotalCount (int)</li>
	 * <li>SqlCalls (int[])</li>
	 * <li>SqlSkipCount (int)</li>
	 * <li>SqlAbendCount (int)</li>
	 * <li>SqlTotalCount (int)</li>
	 * <li>TcpCalls (int[])</li>
	 * <li>TcpSkipCount (int)</li>
	 * <li>TcpAbendCount (int)</li>
	 * <li>TcpTotalCount (int)</li>
	 * </ul>
	 */
	public static class CMTA extends MQZOSStruct {
		public static final String STRUC_ID = "cmta"; // NON-NLS

		public String strucId; // 4
		byte[] unused0; // 4
		public String tcbToken; // 16
		public String dmbDataspace; // 16
		public String txQueue; // 16
		public int aptmStatus;
		public int qmgrStatus;
		byte[] unused1; // 4
		byte[] unused2; // 4
		public String jobName; // 8
		public String addressSpaceId; // 2
		public String flags; // 1
		byte[] filler1; // 5
		public int[] mqCalls; // 11
		public int mqSkipCount;
		public int mqAbendCount;
		public int mqTotalCount;
		public int[] excCalls; // 29
		public int excSkipCount;
		public int excAbendCount;
		public int excTotalCount;
		public int[] sqlCalls; // 45
		public int sqlSkipCount;
		public int sqlAbendCount;
		public int sqlTotalCount;
		public int[] tcpCalls; // 26
		public int tcpSkipCount;
		public int tcpAbendCount;
		public int tcpTotalCount;
		byte[] filler2; // 4;
		public double cmtaend;

		/**
		 * Reads bytes brom provided byte buffer {@code bb} into {@code CMTA} data structure.
		 * 
		 * @param bb
		 *            byte buffer to pull bytes
		 * @param encoding
		 *            encoding to use for conversion
		 * @param charSet
		 *            character set to use conversion
		 * @return {@code CMTA} structure build from byte buffer {@code bb} bytes
		 * 
		 * @throws UnsupportedEncodingException
		 *             if there is no charset mapping for the supplied {@code charSet} value or the platform cannot
		 *             convert from the charset
		 */
		public static CMTA read(ByteBuffer bb, int encoding, int charSet) throws UnsupportedEncodingException {
			CMTA cmta = new CMTA();
			cmta.encoding = encoding;
			cmta.charSet = charSet;

			cmta.strucId = getString(bb, 4, encoding, charSet);
			cmta.unused0 = getBytes(bb, 4);
			cmta.tcbToken = getString(bb, 16, encoding, charSet);
			cmta.dmbDataspace = getStringRaw(bb, 16, encoding, charSet);
			cmta.txQueue = getString(bb, 16, encoding, charSet);
			cmta.aptmStatus = bb.getInt();
			cmta.qmgrStatus = bb.getInt();
			cmta.unused1 = getBytes(bb, 4);
			cmta.unused2 = getBytes(bb, 4);
			cmta.jobName = getString(bb, 8, encoding, charSet);
			cmta.addressSpaceId = getStringRaw(bb, 2, encoding, charSet);
			cmta.flags = getStringRaw(bb, 1, encoding, charSet);
			cmta.filler1 = getBytes(bb, 5);
			cmta.mqCalls = getIntArray(bb, 11);
			cmta.mqSkipCount = bb.getInt();
			cmta.mqAbendCount = bb.getInt();
			cmta.mqTotalCount = bb.getInt();
			cmta.excCalls = getIntArray(bb, 29);
			cmta.excSkipCount = bb.getInt();
			cmta.excAbendCount = bb.getInt();
			cmta.excTotalCount = bb.getInt();
			cmta.sqlCalls = getIntArray(bb, 45);
			cmta.sqlSkipCount = bb.getInt();
			cmta.sqlAbendCount = bb.getInt();
			cmta.sqlTotalCount = bb.getInt();
			cmta.tcpCalls = getIntArray(bb, 26);
			cmta.tcpSkipCount = bb.getInt();
			cmta.tcpAbendCount = bb.getInt();
			cmta.tcpTotalCount = bb.getInt();
			cmta.filler2 = getBytes(bb, 4);
			cmta.cmtaend = bb.getDouble();

			return cmta;
		}

		@Override
		public Map<String, Object> asMap() {
			Map<String, Object> sMap = super.asMap();

			sMap.put("StrucId", strucId); // NON-NLS
			sMap.put("TcbToken", tcbToken); // NON-NLS
			sMap.put("DmbDataspace", dmbDataspace); // NON-NLS
			sMap.put("TxQueue", txQueue); // NON-NLS
			sMap.put("AptmStatus", aptmStatus); // NON-NLS
			sMap.put("QmgrStatus", qmgrStatus); // NON-NLS
			sMap.put("JobName", jobName); // NON-NLS
			sMap.put("AddressSpaceId", addressSpaceId); // NON-NLS
			sMap.put("Flags", flags); // NON-NLS
			sMap.put("MqCalls", mqCalls); // NON-NLS
			sMap.put("MqSkipCount", mqSkipCount); // NON-NLS
			sMap.put("MqAbendCount", mqAbendCount); // NON-NLS
			sMap.put("MqTotalCount", mqTotalCount); // NON-NLS
			sMap.put("ExcCalls", excCalls); // NON-NLS
			sMap.put("ExcSkipCount", excSkipCount); // NON-NLS
			sMap.put("ExcAbendCount", excAbendCount); // NON-NLS
			sMap.put("ExcTotalCount", excTotalCount); // NON-NLS
			sMap.put("SqlCalls", sqlCalls); // NON-NLS
			sMap.put("SqlSkipCount", sqlSkipCount); // NON-NLS
			sMap.put("SqlAbendCount", sqlAbendCount); // NON-NLS
			sMap.put("SqlTotalCount", sqlTotalCount); // NON-NLS
			sMap.put("TcpCalls", tcpCalls); // NON-NLS
			sMap.put("TcpSkipCount", tcpSkipCount); // NON-NLS
			sMap.put("TcpAbendCount", tcpAbendCount); // NON-NLS
			sMap.put("TcpTotalCount", tcpTotalCount); // NON-NLS

			return sMap;
		}
	}

	/**
	 * Base class defining common operations and fields for MQ Probe structures.
	 */
	public static abstract class MQStruct {
		public int encoding;
		public int charSet;

		/**
		 * Makes string from provided byte buffer {@code bb} bytes picking defined {@code length} of bytes and using
		 * provided {@code encoding} and {@code charSet} to convert bytes to chars.
		 * <p>
		 * Produced string is trimmed to preserve only meaningful value without MQ structures padding (space) and
		 * non-printable special/control chars.
		 * 
		 * @param bb
		 *            byte buffer to pull bytes
		 * @param length
		 *            amount of bytes to use for a string
		 * @param encoding
		 *            encoding to use for conversion
		 * @param charSet
		 *            character set to use conversion
		 * @return trimmed string build from byte buffer contained bytes
		 * 
		 * @throws UnsupportedEncodingException
		 *             if there is no charset mapping for the supplied {@code charSet} value or the platform cannot
		 *             convert from the charset
		 * 
		 * @see #getStringRaw(java.nio.ByteBuffer, int, int, int)
		 */
		static String getString(ByteBuffer bb, int length, int encoding, int charSet)
				throws UnsupportedEncodingException {
			return StringUtils.trim(getStringRaw(bb, length, encoding, charSet));
		}

		/**
		 * Makes string from provided byte buffer {@code bb} bytes picking defined {@code length} of bytes and using
		 * provided {@code encoding} and {@code charSet} to convert bytes to chars.
		 * <p>
		 * Produced string preserves all MQ structures padding (space) and non-printable special/control chars.
		 * 
		 * @param bb
		 *            byte buffer to pull bytes
		 * @param length
		 *            amount of bytes to use for a string
		 * @param encoding
		 *            encoding to use for conversion
		 * @param charSet
		 *            character set to use conversion
		 * @return string build from byte buffer contained bytes
		 * 
		 * @throws UnsupportedEncodingException
		 *             if there is no charset mapping for the supplied {@code charSet} value or the platform cannot
		 *             convert from the charset
		 * 
		 * @see #getBytes(java.nio.ByteBuffer, int)
		 * @see com.jkoolcloud.tnt4j.streams.utils.WmqUtils#getString(byte[], Object)
		 */
		static String getStringRaw(ByteBuffer bb, int length, int encoding, int charSet)
				throws UnsupportedEncodingException {
			return WmqUtils.getString(getBytes(bb, length), charSet);
		}

		/**
		 * Returns provided {@code length} byte array, pulling bytes from provided byte buffer {@code bb}.
		 * 
		 * @param bb
		 *            byte buffer to pull bytes
		 * @param length
		 *            size of produced array
		 * @return byte array
		 */
		static byte[] getBytes(ByteBuffer bb, int length) {
			byte[] ba = new byte[length];
			bb.get(ba);

			return ba;
		}

		/**
		 * Returns provided {@code length} byte array, pulling integers from provided byte buffer {@code bb}.
		 * 
		 * @param bb
		 *            byte buffer to pull integers
		 * @param lenght
		 *            size of produced array
		 * @return integers array
		 */
		static int[] getIntArray(ByteBuffer bb, int lenght) {
			int[] ia = new int[lenght];

			for (int i = 0; i < lenght; i++) {
				ia[i] = bb.getInt();
			}

			return ia;
		}

		/**
		 * Makes string from provided byte buffer {@code bb} bytes picking defined {@code length} of bytes and using
		 * {@code encoding} and {@code charSet} set for this structure to convert bytes to chars.
		 * 
		 * @param bb
		 *            byte buffer to pull bytes
		 * @param lenght
		 *            amount of bytes to use for a string
		 * @return string build from byte buffer contained bytes
		 * 
		 * @throws UnsupportedEncodingException
		 *             if there is no charset mapping for the supplied {@code charSet} value or the platform cannot
		 *             convert from the charset
		 * 
		 * @see #getString(java.nio.ByteBuffer, int, int, int)
		 */
		String getString(ByteBuffer bb, int lenght) throws UnsupportedEncodingException {
			return getString(bb, lenght, encoding, charSet);
		}

		/**
		 * Returns structure fields as map.
		 * 
		 * @return map of structure fields.
		 */
		public Map<String, Object> asMap() {
			Map<String, Object> sMap = new LinkedHashMap<>();

			sMap.put("Encoding", encoding); // NON-NLS
			sMap.put("CharSet", charSet);// NON-NLS

			return sMap;
		}
	}

	/**
	 * Base class defining common operations and fields for zOS interceptors collected MQ calls.
	 */
	public static abstract class MQZOSStruct extends MQStruct {
		private static final char[] hexCode = "0123456789ABCDEF".toCharArray(); // NON-NLS

		/**
		 * Converts provided byte array to string.
		 * 
		 * @param bytes
		 *            byte array to convert to string
		 * @return string made from provided bytes
		 * 
		 * @see #toString(byte[], int, int)
		 */
		public static String toString(byte[] bytes) {
			if (bytes == null) {
				return null;
			}

			return toString(bytes, 0, bytes.length);
		}

		/**
		 * Converts provided byte array to string.
		 * 
		 * @param bytes
		 *            byte array to convert to string
		 * @param from
		 *            offset of string starting byte
		 * @param length
		 *            amount of bytes for a string
		 * @return string made from provided bytes
		 */
		public static String toString(byte[] bytes, int from, int length) {
			if (bytes == null) {
				return null;
			}

			int aLength = Math.min(length, bytes.length);
			int to = from + aLength;
			StringBuilder r = new StringBuilder(aLength * 2);

			for (int bi = from; bi < to; ++bi) {
				byte b = bytes[bi];
				r.append(hexCode[b >> 4 & 15]);
				r.append(hexCode[b & 15]);
			}

			return r.toString();
		}

		/**
		 * Makes timestamp value from 8 bytes of zOS TOD format value pulled from provided byte buffer.
		 * 
		 * @param bb
		 *            byte buffer to pull bytes
		 * @return timestamp value
		 * 
		 * @see #getBytes(java.nio.ByteBuffer, int)
		 * @see #toString(byte[])
		 * @see #timestamp(String)
		 */
		public static long timestamp8(ByteBuffer bb) {
			return timestamp(toString(getBytes(bb, 8)));
		}

		/**
		 * Makes timestamp value from provided zOS TOD bytes value.
		 * 
		 * @param zOSTODBytes
		 *            zOS TOD value bytes
		 * @return timestamp value
		 * 
		 * @see #toString(byte[])
		 * @see #timestamp(String)
		 */
		public static long timestamp(byte[] zOSTODBytes) {
			return timestamp(toString(zOSTODBytes));
		}

		/**
		 * Makes timestamp value from provided 16 zOS TOD bytes value.
		 * 
		 * @param zOSTODBytes
		 *            zOS TOD value bytes
		 * @return timestamp value
		 * 
		 * @see #toString(byte[], int, int)
		 * @see #timestamp(String)
		 */
		public static long timestamp16(byte[] zOSTODBytes) {
			if (zOSTODBytes == null || zOSTODBytes.length != 16) {
				return -1;
			}

			String todS = toString(zOSTODBytes, 1, 8);

			return timestamp(todS);
		}

		/**
		 * Makes timestamp value from provided zOS TOD string.
		 * 
		 * @param todS
		 *            zOS TOD value string
		 * @return timestamp value
		 */
		public static long timestamp(String todS) {
			BigInteger bi = new BigInteger(todS, 16); // 686 stripped off
			long tod = bi.longValue();

			if (tod != 0) {
				tod = tod >>> 12; // remove rightmost 3 bytes and replace with zeros
				tod = tod - 2208988800000000l; // subtract 1970
				tod = tod / 1000; // make millis out of micros
			}

			return tod;
		}
	}

	/**
	 * Interface for root level MQ Probe structure.
	 */
	public interface MQProbeRootStruct {
		/**
		 * Returns structure fields as map.
		 * 
		 * @return map of structure fields.
		 */
		public Map<String, ?> asMap();
	}

}
