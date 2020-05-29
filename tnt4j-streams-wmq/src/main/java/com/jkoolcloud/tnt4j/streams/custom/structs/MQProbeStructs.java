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

	public static class TAMQAPINT extends MQStruct implements MQProbeRootStruct {
		public String strucId; // 8
		public TAMQINFO mqInfo;
		public TAAPINTINFO apiIntInfo;
		public String msgId; // 8
		public byte[] msg;

		public static TAMQAPINT read(ByteBuffer bb, int encoding, int charSet) throws UnsupportedEncodingException {
			TAMQAPINT tamqapint = new TAMQAPINT();
			tamqapint.encoding = encoding;
			tamqapint.charSet = charSet;

			tamqapint.strucId = getString(bb, 8, encoding, charSet);
			tamqapint.mqInfo = TAMQINFO.read(bb, encoding, charSet);
			tamqapint.apiIntInfo = TAAPINTINFO.read(bb, encoding, charSet);
			tamqapint.msgId = getStringRaw(bb, 8, encoding, charSet);
			tamqapint.msg = getBytes(bb, tamqapint.mqInfo.dataSize);

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

	public static class TAAPINTINFO extends MQStruct {
		public String strucId; // 8
		public String hostName; // 64
		public String applName; // 64
		public String GTID; // 320
		public String objName; // 256
		public String rslvObjName; // 64
		public String rslvQMgr; // 48+1
		public String ipAddress; // 64
		public String osType; // 256
		public int objType; /* for szObjName */
		public int cpuCount;
		byte[] unused; // 4
		public TIME_INFO exitTime;
		public APXDELTA exitDelta;
		public APXSIGNTR exitSigntr;
		public TAAXP exitParms;
		public TAAXC exitContext;

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
	}

	public static class TAMQINFO extends MQStruct {
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

		public static TAMQINFO read(ByteBuffer bb, int encoding, int charSet) throws UnsupportedEncodingException {
			TAMQINFO tamqinfo = new TAMQINFO();
			tamqinfo.encoding = encoding;
			tamqinfo.charSet = charSet;
			// bb.order(ByteOrder.LITTLE_ENDIAN);

			tamqinfo.strucId = getString(bb, 4, encoding, charSet);
			tamqinfo.apiType = bb.getInt();
			tamqinfo.apiCall = bb.getInt();
			tamqinfo.compCode = bb.getInt();
			tamqinfo.reason = bb.getInt();
			tamqinfo.dataSize = bb.getInt();
			tamqinfo.originalDataSize = bb.getInt();
			tamqinfo.pad = getBytes(bb, 4);
			tamqinfo.objDesc = TAOD.read(bb, encoding, charSet);
			tamqinfo.msgOpt = TAMSGOPT.read(bb, encoding, charSet);
			tamqinfo.msgAge = MSGAGE.read(bb, encoding, charSet);
			tamqinfo.msgDesc = TAMD.read(bb, encoding, charSet);

			return tamqinfo;
		}
	}

	public static class TAOD extends MQStruct {
		public String strucId; // 4
		public int objectType;
		public String objectName; // 48
		public String objectQMgrName; // 48
		public String dynamicQName; // 48
		public String alternateUserId; // 12
		public String resolvedQName; // 48
		public String resolvedQMgrName; // 48
		byte[] unused; // 28

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
	}

	public static class TAMSGOPT extends MQStruct {
		public String strucId; // 4
		public int version;
		public int options;
		public int waitInterval;
		public String resolvedQName; // 48
		public String resolvedQMgrName; // 48

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
	}

	/**
	 * MQ Probe Message Descriptor.
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

	public static class TIME_INFO extends MQStruct {
		public int time_sec;
		public int time_mls;
		public int time_usec;
		public int time_tz;
		public int offset_secs;
		public int offset_msecs;
		public double timer;

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
	}

	public static class APXDELTA extends MQStruct {
		public int delta_sec;
		public int delta_usec;

		public static APXDELTA read(ByteBuffer bb, int encoding, int charSet) throws UnsupportedEncodingException {
			APXDELTA apxdelta = new APXDELTA();
			apxdelta.encoding = encoding;
			apxdelta.charSet = charSet;

			apxdelta.delta_sec = bb.getInt();
			apxdelta.delta_usec = bb.getInt();

			return apxdelta;
		}
	}

	public static class APXSIGNTR extends MQStruct {
		public int readCount;
		public int writeCount;
		public int otherCount;
		public int luwType;
		public String sesSig; // 16
		public String luwSig; // 16
		public String msgSig; // 36+1
		byte[] unused; // 3 /* pad variable to next multiple of 8 bytes */
		public int sesSigLen;
		public int luwSigLen;
		public int msgSigLen;
		byte[] unused2; // 4 /* pad struct to next multiple of 8 bytes */

		public static APXSIGNTR read(ByteBuffer bb, int encoding, int charSet) throws UnsupportedEncodingException {
			APXSIGNTR apxsigntr = new APXSIGNTR();
			apxsigntr.encoding = encoding;
			apxsigntr.charSet = charSet;

			apxsigntr.readCount = bb.getInt();
			apxsigntr.writeCount = bb.getInt();
			apxsigntr.otherCount = bb.getInt();
			apxsigntr.luwType = bb.getInt();
			apxsigntr.sesSig = getStringRaw(bb, 16, encoding, charSet);
			apxsigntr.luwSig = getStringRaw(bb, 16, encoding, charSet);
			apxsigntr.msgSig = getStringRaw(bb, 37, encoding, charSet);
			apxsigntr.unused = getBytes(bb, 3);
			apxsigntr.sesSigLen = bb.getInt();
			apxsigntr.luwSigLen = bb.getInt();
			apxsigntr.msgSigLen = bb.getInt();
			apxsigntr.unused2 = getBytes(bb, 4);

			return apxsigntr;
		}
	}

	public static class MSGAGE extends MQStruct {
		public int msgage_sec;
		public int msgage_usec;

		static MSGAGE read(ByteBuffer bb, int encoding, int charSet) throws UnsupportedEncodingException {
			MSGAGE msgage = new MSGAGE();
			msgage.encoding = encoding;
			msgage.charSet = charSet;

			msgage.msgage_sec = bb.getInt();
			msgage.msgage_usec = bb.getInt();

			return msgage;
		}
	}

	public static class TAAXP extends MQStruct {
		public String strucId; // 4
		public int exitId;
		public int exitReason;
		public int exitResponse;
		public int exitResponse2;
		public int feedback;
		public int apiCallerType;
		public int function;
		public String qMgrName; // 48

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
	}

	public static class TAAXC extends MQStruct {
		public String strucId; // 4
		public int snvironment;
		public String userId; // 12
		public byte[] securityId; // 40
		public String connectionName; // 264
		public String applName; // 28
		public int applType;
		public int processId; // int?
		public int threadId; // int?
		byte[] unused; // 28 byte[]?

		public static TAAXC read(ByteBuffer bb, int encoding, int charSet) throws UnsupportedEncodingException {
			TAAXC taaxc = new TAAXC();
			taaxc.encoding = encoding;
			taaxc.charSet = charSet;

			taaxc.strucId = getString(bb, 4, encoding, charSet);
			taaxc.snvironment = bb.getInt();
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
	}

	/**
	 * zOS intercepted MQ call.
	 * <p>
	 * It provides such fields and types:
	 * <ul>
	 * <li>StrucId (String)</li>
	 * <li>BatchInfo ({@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TAMQBATCH})</li>
	 * <li>MqCallCtx ({@link com.jkoolcloud.tnt4j.streams.custom.structs.MQProbeStructs.TAMQCD})</li>
	 * <li>Message (byte[])</li>
	 * </ul>
	 */
	public static class TAZOS extends MQZOSStruct implements MQProbeRootStruct {
		public String strucId = "TAZOS"; // NON-NLS
		public TAMQBATCH batchInfo;
		public TAMQCD mqCallCtx;
		public byte[] msg;
		byte[] align; // 4

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
			tazos.mqCallCtx = TAMQCD.read(bb, encoding, charSet);
			tazos.msg = getBytes(bb, tazos.batchInfo.msgLength);
			tazos.align = getBytes(bb, 4);

			return tazos;
		}

		@Override
		public Map<String, Object> asMap() {
			Map<String, Object> sMap = super.asMap();

			sMap.put("StrucId", strucId); // NON-NLS
			sMap.put("BatchInfo", batchInfo.asMap()); // NON-NLS
			sMap.put("MqCallCtx", mqCallCtx.asMap()); // NON-NLS
			sMap.put("Message", msg); // NON-NLS

			return sMap;
		}
	}

	/**
	 * zOS Batch MQ interceptor data.
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
	 * <li>MsgSig (String)</li>
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
		public String luwType; // UICHAR
		byte[] align2; // 3
		public String msgSig; // 36
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
			tamqcd.msgSig = getStringRaw(bb, 36, encoding, charSet);
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
		 *            size of produced byte array
		 * @return byte array
		 */
		static byte[] getBytes(ByteBuffer bb, int length) {
			byte[] ba = new byte[length];
			bb.get(ba);

			return ba;
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
