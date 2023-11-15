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

package com.jkoolcloud.tnt4j.streams.custom.format.castiron;

import java.util.*;

import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.*;
import com.jkoolcloud.tnt4j.format.DefaultFormatter;
import com.jkoolcloud.tnt4j.source.Source;
import com.jkoolcloud.tnt4j.streams.fields.StreamFieldType;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.tracker.TrackingActivity;
import com.jkoolcloud.tnt4j.tracker.TrackingEvent;

/**
 * This class provides key/value formatting for tnt4j activities, events and snapshots. The output format follows the
 * following format:
 * <p>
 * {@code "OBJ:name-value-prefix,name1=value1,....,nameN=valueN"}.
 * </p>
 * Newline is added at the end of each line.
 * <p>
 * This formatter supports the following configuration properties (in addition to those supported by
 * {@link com.jkoolcloud.tnt4j.format.DefaultFormatter}):
 * <ul>
 * <li>KeyReplacements - configures produced property key replacement symbols. Format is:
 * {@code event.formatter.KeyReplacements: "s1"->"rs1" "s2"->"rs" ... "sn"->"rsN"}, where:
 * <ul>
 * <li>{@code sX} - symbol to be replaced</li>
 * <li>{@code rsX} - replacement symbol</li>
 * </ul>
 * E.g. {@code event.formatter.KeyReplacements: " "->"_" "\""->"'" "/"->"%"}. Default value -
 * {@code " "->"_" "\""->"'" "/"->"%" "="->"\" ","->"!"}. (Optional)</li>
 * <li>ValueReplacements - configures produced property value replacement symbols. Format is:
 * {@code event.formatter.ValueReplacements: "s1"->"rs1" "s2"->"rs" ... "sn"->"rsN"}, where:
 * <ul>
 * <li>{@code sX} - symbol to be replaced</li>
 * <li>{@code rsX} - replacement symbol</li>
 * </ul>
 * E.g. {@code event.formatter.ValueReplacements: "\r"->"\\r" "\n"->"\\n" ";"->"|" ","->"|" "["->"{(" "]"->")}"
 * "\""->"'"}. Default value - {@code "\r"->"\\r" "\n"->"\\n" ";"->"|" ","->"|" "["->"{(" "]"->")}" "\""->"'"}.
 * (Optional)</li>
 * </ul>
 *
 *
 * @version $Revision: 1 $
 */
public class FactNameValueFormatter extends DefaultFormatter {
	/**
	 * Line-feed symbol {@value}.
	 */
	protected static final String LF = "\n"; // NON-NLS
	/**
	 * Carriage-return symbol {@value}.
	 */
	protected static final String CR = "\r"; // NON-NLS
	/**
	 * Field separator symbol {@value}.
	 */
	protected static final String FIELD_SEP = ","; // NON-NLS
	/**
	 * Formatted data package end symbol {@value}.
	 */
	protected static final String END_SEP = LF;
	/**
	 * Property path delimiter symbol {@value}.
	 */
	protected static final String PATH_DELIM = "\\"; // NON-NLS
	/**
	 * Equality symbol {@value}.
	 */
	protected static final String EQ = "="; // NON-NLS
	/**
	 * Fields separator symbol {@value}.
	 */
	protected static final String FS_REP = "!"; // NON-NLS

	private static final String SELF_SNAP_NAME = "Self"; // NON-NLS
	private static final String SELF_SNAP_ID = SELF_SNAP_NAME + "@" + PropertySnapshot.CATEGORY_DEFAULT; // NON-NLS

	/**
	 * Mapping of attribute key string symbol replacements.
	 */
	protected Map<String, String> keyReplacements = new LinkedHashMap<>();
	/**
	 * Mapping of attribute value string symbol replacements.
	 */
	protected Map<String, String> valueReplacements = new LinkedHashMap<>();

	private boolean addSelfSnapshot = true;
	/**
	 * Flag indicating whether to add AutoPilot fact value type prefixes for fact names. Default {@value}.
	 */
	protected boolean addAPValueTypePrefix = false;

	/**
	 * Constructs a new instance of {@code FactNameValueFormatter}.
	 */
	public FactNameValueFormatter() {
		super("time.stamp={2},level={1},source={3},msg=\"{0}\""); // NON-NLS

		// adding mandatory value symbols replacements
		valueReplacements.put(CR, "\\r"); // NON-NLS
		valueReplacements.put(LF, "\\n"); // NON-NLS
	}

	@Override
	public String format(TrackingEvent event) {
		StringBuilder nvString = new StringBuilder(1024);

		nvString.append("OBJ:"); // NON-NLS
		// ------------------------------------------------------------- name
		toString(nvString, event.getSource()).append(PATH_DELIM).append("Events").append(FIELD_SEP); // NON-NLS

		toString(nvString, getTrackableKey(event, StreamFieldType.EventName.name()), event.getOperation().getName());
		toString(nvString, getTrackableKey(event, StreamFieldType.Severity.name()), event.getSeverity());
		toString(nvString, getTrackableKey(event, StreamFieldType.StartTime.name()),
				event.getOperation().getStartTime());
		toString(nvString, getTrackableKey(event, StreamFieldType.EndTime.name()), event.getOperation().getEndTime());
		toString(nvString, getTrackableKey(event, StreamFieldType.Message.name()), event.getMessage());
		toString(nvString, getTrackableKey(event, StreamFieldType.Correlator.name()), event.getCorrelator());

		Collection<Property> pList = getProperties(event.getOperation());
		for (Property prop : pList) {
			toString(nvString, event, prop);
		}

		if (event.getOperation().getSnapshot(SELF_SNAP_ID) == null) {
			Snapshot selfSnapshot = getSelfSnapshot(event.getOperation());
			if (event.getTag() != null) {
				Set<String> tags = event.getTag();
				if (!tags.isEmpty()) {
					selfSnapshot.add("tag", tags); // NON-NLS
				}
			}

			event.getOperation().addSnapshot(selfSnapshot);
		}

		Collection<Snapshot> sList = getSnapshots(event.getOperation());
		for (Snapshot snap : sList) {
			toString(nvString, event, snap);
		}

		return nvString.append(END_SEP).toString();
	}

	/**
	 * Returns operation contained snapshots collection.
	 *
	 * @param op
	 *            operation instance
	 * @return collection of operation snapshots
	 */
	protected Collection<Snapshot> getSnapshots(Operation op) {
		return op.getSnapshots();
	}

	@Override
	public String format(TrackingActivity activity) {
		StringBuilder nvString = new StringBuilder(1024);

		nvString.append("OBJ:"); // NON-NLS
		toString(nvString, activity.getSource()).append(PATH_DELIM).append("Activities").append(FIELD_SEP); // NON-NLS

		toString(nvString, getTrackableKey(activity, StreamFieldType.EventName.name()), activity.getName());
		toString(nvString, getTrackableKey(activity, StreamFieldType.Severity.name()), activity.getSeverity());
		toString(nvString, getTrackableKey(activity, StreamFieldType.StartTime.name()), activity.getStartTime());
		toString(nvString, getTrackableKey(activity, StreamFieldType.EndTime.name()), activity.getEndTime());
		toString(nvString, getTrackableKey(activity, StreamFieldType.ResourceName.name()), activity.getResource());
		toString(nvString, getTrackableKey(activity, StreamFieldType.Correlator.name()), activity.getCorrelator());

		Collection<Property> pList = getProperties(activity);
		for (Property prop : pList) {
			toString(nvString, activity, prop);
		}

		if (activity.getSnapshot(SELF_SNAP_ID) == null) {
			Snapshot selfSnapshot = getSelfSnapshot(activity);
			selfSnapshot.add("id.count", activity.getIdCount()); // NON-NLS

			activity.addSnapshot(selfSnapshot);
		}

		Collection<Snapshot> sList = getSnapshots(activity);
		for (Snapshot snap : sList) {
			toString(nvString, activity, snap);
		}

		return nvString.append(END_SEP).toString();
	}

	private Snapshot getSelfSnapshot(Operation op) {
		Snapshot selfSnapshot = new PropertySnapshot(SELF_SNAP_NAME);

		if (op.getCorrelator() != null) {
			Set<String> cids = op.getCorrelator();
			if (!cids.isEmpty()) {
				selfSnapshot.add("corrid", cids); // NON-NLS
			}
		}
		if (op.getUser() != null) {
			selfSnapshot.add("user", op.getUser());
		}
		if (op.getLocation() != null) {
			selfSnapshot.add("location", op.getLocation()); // NON-NLS
		}
		selfSnapshot.add("level", op.getSeverity()); // NON-NLS
		selfSnapshot.add("pid", op.getPID()); // NON-NLS
		selfSnapshot.add("tid", op.getTID()); // NON-NLS
		selfSnapshot.add("snap.count", op.getSnapshotCount()); // NON-NLS
		selfSnapshot.add("elapsed.usec", op.getElapsedTimeUsec()); // NON-NLS

		return selfSnapshot;
	}

	@Override
	public String format(Snapshot snapshot) {
		StringBuilder nvString = new StringBuilder(1024);

		// ------------------------------------------------------ category, id or name
		nvString.append("OBJ:");
		toString(nvString, snapshot.getSource()).append(PATH_DELIM).append(snapshot.getCategory()).append(FIELD_SEP); // NON-NLS
		toString(nvString, (Trackable) null, snapshot).append(END_SEP);

		return nvString.toString();
	}

	@Override
	public String format(long ttl, Source source, OpLevel level, String msg, Object... args) {
		StringBuilder nvString = new StringBuilder(1024);

		nvString.append("OBJ:"); // NON-NLS
		toString(nvString, source).append(PATH_DELIM).append("Message").append(FIELD_SEP); // NON-NLS
		nvString.append(SELF_SNAP_NAME).append(PATH_DELIM).append("level"); // NON-NLS
		formatValue(nvString, level, FIELD_SEP);
		nvString.append(SELF_SNAP_NAME).append(PATH_DELIM).append("msg-text"); // NON-NLS
		formatValue(nvString, Utils.format(msg, args), END_SEP);
		return nvString.toString();
	}

	/**
	 * Formats provided {@code value} and appends to provided string builder.
	 * 
	 * @param nvString
	 *            string builder instance to append
	 * @param value
	 *            value to append
	 * @param sep
	 *            fields separator symbol
	 * @return string builder instance
	 */
	protected StringBuilder formatValue(StringBuilder nvString, Object value, String sep) {
		return nvString.append(EQ).append(getValueStr(value)).append(sep);
	}

	/**
	 * Makes string representation of source and appends it to provided string builder.
	 *
	 * @param nvString
	 *            string builder instance to append
	 * @param source
	 *            source instance to represent as string
	 * @return appended string builder reference
	 */
	protected StringBuilder toString(StringBuilder nvString, Source source) {
		Source parent = source.getSource();
		if (parent != null) {
			toString(nvString, parent);
		}
		nvString.append(PATH_DELIM).append(getSourceNameStr(source.getName()));
		return nvString;
	}

	/**
	 * Makes decorated string representation of source name.
	 * <p>
	 * Source name representation string gets symbols replaced using ones defined in {@link #keyReplacements} map.
	 *
	 * @param sourceName
	 *            source name
	 * @return decorated string representation of source name
	 *
	 * @see Utils#replace(String, Map)
	 */
	protected String getSourceNameStr(String sourceName) {
		return Utils.replace(sourceName, keyReplacements);
	}

	/**
	 * Returns snapshot contained properties collection.
	 *
	 * @param snap
	 *            snapshot instance
	 * @return collection of snapshot properties
	 */
	protected Collection<Property> getProperties(Snapshot snap) {
		return snap.getProperties();
	}

	/**
	 * Returns operation contained properties collection.
	 *
	 * @param operation
	 *            operation instance
	 * @return collection of operation properties
	 */
	protected Collection<Property> getProperties(Operation operation) {
		return operation.getProperties();
	}

	/**
	 * Makes decorated string representation of argument trackable and property key.
	 *
	 * @param t
	 *            trackable instance
	 * @param pKey
	 *            field/property key string
	 * @return decorated string representation of trackable contained field/property
	 *
	 * @see #getTrackableName(com.jkoolcloud.tnt4j.core.Trackable)
	 * @see #getKeyStr(String, String)
	 */
	protected String getTrackableKey(Trackable t, String pKey) {
		if (t == null) {
			return pKey;
		}

		return getKeyStr(getTrackableName(t), pKey);
	}

	/**
	 * Returns name for provided {@code trackable} instance.
	 *
	 * @param trackable
	 *            trackable instance to get name for
	 * @return name of provided trackable
	 */
	protected String getTrackableName(Trackable trackable) {
		return trackable.getName();
	}

	/**
	 * Makes string representation of snapshot and appends it to provided string builder.
	 *
	 * @param nvString
	 *            string builder instance to append
	 * @param t
	 *            snapshot parent entity
	 * @param snap
	 *            snapshot instance to represent as string
	 * @return appended string builder reference
	 */
	protected StringBuilder toString(StringBuilder nvString, Trackable t, Snapshot snap) {
		Collection<Property> list = getProperties(snap);
		String sName = getTrackableKey(t, getSnapName(snap));
		for (Property p : list) {
			if (p.isTransient()) {
				continue;
			}

			String pKey = p.getKey();
			Object value = p.getValue();

			if (addAPValueTypePrefix) {
				nvString.append(getAPValueType(value));
			}
			nvString.append(getKeyStr(sName, pKey));
			formatValue(nvString, value, FIELD_SEP);
		}
		return nvString;
	}

	/**
	 * Makes string representation of property and appends it to provided string builder.
	 * 
	 * @param nvString
	 *            string builder instance to append
	 * @param t
	 *            trackable instance
	 * @param prop
	 *            property instance
	 * @return appended string builder reference
	 *
	 * @see #toString(StringBuilder, String, Object)
	 */
	protected StringBuilder toString(StringBuilder nvString, Trackable t, Property prop) {
		if (!prop.isTransient()) {
			String pKey = getTrackableKey(t, prop.getKey());
			Object value = prop.getValue();

			return toString(nvString, pKey, value);

		}
		return nvString;
	}

	/**
	 * Makes string representation of property by provided {@code key} and {@code value}.
	 * 
	 * @param nvString
	 *            string builder instance to append
	 * @param key
	 *            property key
	 * @param value
	 *            property value
	 * @return appended string builder reference
	 */
	protected StringBuilder toString(StringBuilder nvString, String key, Object value) {
		nvString.append(key);
		nvString.append(EQ).append(getValueStr(value)).append(FIELD_SEP);
		return nvString;
	}

	/**
	 * Returns provided value AutoPilot type string to prefix fact name.
	 * 
	 * @param value
	 *            value to determine type
	 * @return AutoPilot value type prefix string
	 */
	protected String getAPValueType(Object value) {
		if (value instanceof Integer) {
			return "$I$";
		}
		if (value instanceof Long) {
			return "$L$";
		}
		if (value instanceof Float) {
			return "$F$";
		}
		if (value instanceof Double) {
			return "$D$";
		}
		if (value instanceof Boolean) {
			return "$B$";
		}
		if (value instanceof String) {
			return "$S$";
		}
		if (value instanceof Date) {
			return "$T$";
		}
		// if (value instanceof Date) {
		// return "$M$"; //timestamp numeric long
		// }
		if (value instanceof byte[]) {
			return "$X$";
		}

		return "$V$";
	}

	/**
	 * Makes decorated string representation of snapshot name.
	 * <p>
	 * Snapshot name string gets symbols replaced using ones defined in {@link #keyReplacements} map.
	 *
	 * @param snapName
	 *            snapshot name
	 * @return decorated string representation of snapshot name
	 */
	protected String getSnapNameStr(String snapName) {
		return Utils.replace(snapName, keyReplacements);
	}

	/**
	 * Makes decorated string representation of {@link Snapshot} name.
	 *
	 * @param snap
	 *            snapshot instance
	 * @return decorated string representation of snapshot name
	 *
	 * @see #getSnapNameStr(String)
	 */
	protected String getSnapName(Snapshot snap) {
		return getSnapNameStr(snap.getName());
	}

	/**
	 * Makes decorated string representation of argument attribute key.
	 * <p>
	 * Key representation string gets symbols replaced using ones defined in {@link #keyReplacements} map.
	 *
	 * @param sName
	 *            snapshot name
	 * @param pKey
	 *            property key
	 * @return decorated string representation of attribute key
	 *
	 * @see #initDefaultKeyReplacements()
	 * @see Utils#replace(String, Map)
	 */
	protected String getKeyStr(String sName, String pKey) {
		String keyStr = sName + PATH_DELIM + pKey;

		return Utils.replace(keyStr, keyReplacements);
	}

	/**
	 * Makes decorated string representation of argument attribute value.
	 * <p>
	 * Value representation string containing {@code "\n"} or {@code "\r"} symbols gets those replaced by escaped
	 * representations {@code "\\n"} amd {@code "\\r"}.
	 * <p>
	 * Value representation string gets symbols replaced using ones defined in {@link #valueReplacements} map.
	 *
	 * @param value
	 *            attribute value
	 * @return decorated string representation of attribute value
	 *
	 * @see Utils#base64EncodeStr(byte[])
	 * @see Utils#toString(Object)
	 * @see #initDefaultValueReplacements()
	 * @see Utils#replace(String, Map)
	 */
	protected String getValueStr(Object value) {
		String valStr;
		if (value instanceof byte[]) {
			valStr = Utils.base64EncodeStr((byte[]) value);
		} else {
			valStr = Utils.toString(value);
		}

		return Utils.replace(valStr, valueReplacements);
	}

	@Override
	public void setConfiguration(Map<String, ?> settings) {
		super.setConfiguration(settings);

		String pValue = Utils.getString("KeyReplacements", settings, ""); // NON-NLS
		if (StringUtils.isEmpty(pValue)) {
			initDefaultKeyReplacements();
		} else {
			Utils.parseReplacements(pValue, keyReplacements);
		}

		pValue = Utils.getString("ValueReplacements", settings, ""); // NON-NLS
		if (StringUtils.isEmpty(pValue)) {
			initDefaultValueReplacements();
		} else {
			Utils.parseReplacements(pValue, valueReplacements);
		}

		addSelfSnapshot = Utils.getBoolean("AddSelfSnapshot", settings, addSelfSnapshot);
		addAPValueTypePrefix = Utils.getBoolean("AddAPValueTypePrefix", settings, addAPValueTypePrefix);
	}

	/**
	 * Initializes default set symbol replacements for a attribute keys.
	 * <p>
	 * Default keys string replacements mapping is:
	 * <ul>
	 * <li>{@code " "} to {@code "_"}</li>
	 * <li>{@code "\""} to {@code "'"}</li>
	 * <li>{@code "/"} to {@code "%"}</li>
	 * <li>{@value #EQ} to {@value #PATH_DELIM}</li>
	 * <li>{@value #FIELD_SEP} to {@value #FS_REP}</li>
	 * </ul>
	 */
	protected void initDefaultKeyReplacements() {
		keyReplacements.put(" ", "_"); // NON-NLS
		keyReplacements.put("\"", "'"); // NON-NLS
		keyReplacements.put("/", "%"); // NON-NLS
		keyReplacements.put(EQ, PATH_DELIM);
		keyReplacements.put(FIELD_SEP, FS_REP);
	}

	/**
	 * Initializes default set symbol replacements for a attribute values.
	 * <p>
	 * Default value string replacements mapping is:
	 * <ul>
	 * <li>{@code ";"} to {@code "|"}</li>
	 * <li>{@code ","} to {@code "|"}</li>
	 * <li>{@code "["} to {@code "{("}</li>
	 * <li>{@code "["} to {@code ")}"}</li>
	 * <li>{@code "\""} to {@code "'"}</li>
	 * </ul>
	 */
	protected void initDefaultValueReplacements() {
		valueReplacements.put(";", "|"); // NON-NLS
		valueReplacements.put(",", "|"); // NON-NLS
		valueReplacements.put("[", "{("); // NON-NLS
		valueReplacements.put("]", ")}"); // NON-NLS
		valueReplacements.put("\"", "'"); // NON-NLS
	}
}
