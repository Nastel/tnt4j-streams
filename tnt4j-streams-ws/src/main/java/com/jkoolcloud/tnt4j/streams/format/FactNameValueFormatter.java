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

package com.jkoolcloud.tnt4j.streams.format;

import java.util.*;

import javax.management.ObjectName;

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
 *
 * @version $Revision: 1 $
 *
 */
public class FactNameValueFormatter extends DefaultFormatter {
	public static final String LF = "\n";
	public static final String CR = "\r";
	public static final String FIELD_SEP = ",";
	public static final String END_SEP = LF;
	public static final String PATH_DELIM = "\\";
	public static final String EQ = "=";
	public static final String FS_REP = "!";
	public static final String UNIQUE_SUFFIX = "_";

	private static final String SELF_SNAP_NAME = "Self";
	private static final String SELF_SNAP_ID = SELF_SNAP_NAME + "@" + PropertySnapshot.CATEGORY_DEFAULT;

	protected String uniqueSuffix = UNIQUE_SUFFIX;

	protected Map<String, String> keyReplacements = new HashMap<>();
	protected Map<String, String> valueReplacements = new HashMap<>();

	public FactNameValueFormatter() {
		super("time.stamp={2},level={1},source={3},msg=\"{0}\"");

		// adding mandatory value symbols replacements
		valueReplacements.put(CR, "\\r");
		valueReplacements.put(LF, "\\n");
	}

	@Override
	public String format(TrackingEvent event) {
		StringBuilder nvString = new StringBuilder(1024);

		nvString.append("OBJ:Streams");
		// ------------------------------------------------------------- name
		toString(nvString, event.getSource()).append(PATH_DELIM).append("Events").append(FIELD_SEP);

		toString(nvString, getTrackableStr(event, StreamFieldType.EventName.name()), event.getOperation().getName());
		toString(nvString, getTrackableStr(event, StreamFieldType.Severity.name()), event.getSeverity());
		toString(nvString, getTrackableStr(event, StreamFieldType.StartTime.name()),
				event.getOperation().getStartTime());
		toString(nvString, getTrackableStr(event, StreamFieldType.EndTime.name()), event.getOperation().getEndTime());
		toString(nvString, getTrackableStr(event, StreamFieldType.Message.name()), event.getMessage());
		toString(nvString, getTrackableStr(event, StreamFieldType.Correlator.name()), event.getCorrelator());

		Collection<Property> pList = getProperties(event.getOperation());
		for (Property prop : pList) {
			toString(nvString, event, prop);
		}

		if (event.getOperation().getSnapshot(SELF_SNAP_ID) == null) {
			Snapshot selfSnapshot = getSelfSnapshot(event.getOperation());
			if (event.getTag() != null) {
				Set<String> tags = event.getTag();
				if (!tags.isEmpty()) {
					selfSnapshot.add("tag", tags);
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

		nvString.append("OBJ:Streams");
		toString(nvString, activity.getSource()).append(PATH_DELIM).append("Activities").append(FIELD_SEP);

		toString(nvString, getTrackableStr(activity, StreamFieldType.EventName.name()), activity.getName());
		toString(nvString, getTrackableStr(activity, StreamFieldType.Severity.name()), activity.getSeverity());
		toString(nvString, getTrackableStr(activity, StreamFieldType.StartTime.name()), activity.getStartTime());
		toString(nvString, getTrackableStr(activity, StreamFieldType.EndTime.name()), activity.getEndTime());
		toString(nvString, getTrackableStr(activity, StreamFieldType.ResourceName.name()), activity.getResource());
		toString(nvString, getTrackableStr(activity, StreamFieldType.Correlator.name()), activity.getCorrelator());

		Collection<Property> pList = getProperties(activity);
		for (Property prop : pList) {
			toString(nvString, activity, prop);
		}

		if (activity.getSnapshot(SELF_SNAP_ID) == null) {
			Snapshot selfSnapshot = getSelfSnapshot(activity);
			selfSnapshot.add("id.count", activity.getIdCount());

			activity.addSnapshot(selfSnapshot);
		}

		Collection<Snapshot> sList = getSnapshots(activity);
		for (Snapshot snap : sList) {
			toString(nvString, activity, snap);
		}

		return nvString.append(END_SEP).toString();
	}

	private String getTrackableStr(Trackable t, String pKey) {
		if (t == null) {
			return pKey;
		}

		return getKeyStr(getTrackablePath(t), pKey);
	}

	private String getTrackablePath(Trackable activity) {
		StringBuilder pathBuilder = new StringBuilder(128);
		Object pv;

		if (pathLevelAttrKeys != null) {
			for (Map.Entry<Condition, String[][]> entry : pathLevelAttrKeys.entrySet()) {
				if (entry.getKey().evaluate(Utils.toString(activity.getFieldValue(entry.getKey().variable)))) {
					for (String[] levelAttrKeys : entry.getValue()) {
						inner: for (String pKey : levelAttrKeys) {
							pv = activity.getFieldValue(pKey);
							if (pv != null) {
								appendPath(pathBuilder, pv);
								break inner;
							}
						}
					}
					break; // handle first
				}
			}
		}

		return pathBuilder.toString();
	}

	protected StringBuilder appendPath(StringBuilder pathBuilder, Object pathToken) {
		if (pathToken != null) {
			pathBuilder.append(pathBuilder.length() > 0 ? PATH_DELIM : "").append(Utils.toString(pathToken));
		}
		return pathBuilder;
	}

	private Snapshot getSelfSnapshot(Operation op) {
		Snapshot selfSnapshot = new PropertySnapshot(SELF_SNAP_NAME);

		if (op.getCorrelator() != null) {
			Set<String> cids = op.getCorrelator();
			if (!cids.isEmpty()) {
				selfSnapshot.add("corrid", cids);
			}
		}
		if (op.getUser() != null) {
			selfSnapshot.add("user", op.getUser());
		}
		if (op.getLocation() != null) {
			selfSnapshot.add("location", op.getLocation());
		}
		selfSnapshot.add("level", op.getSeverity());
		selfSnapshot.add("pid", op.getPID());
		selfSnapshot.add("tid", op.getTID());
		selfSnapshot.add("snap.count", op.getSnapshotCount());
		selfSnapshot.add("elapsed.usec", op.getElapsedTimeUsec());

		return selfSnapshot;
	}

	@Override
	public String format(Snapshot snapshot) {
		StringBuilder nvString = new StringBuilder(1024);

		// ------------------------------------------------------ category, id or name
		nvString.append("OBJ:Metrics").append(PATH_DELIM).append(snapshot.getCategory()).append(FIELD_SEP);
		toString(nvString, (Trackable) null, snapshot).append(END_SEP);

		return nvString.toString();
	}

	@Override
	public String format(long ttl, Source source, OpLevel level, String msg, Object... args) {
		StringBuilder nvString = new StringBuilder(1024);

		nvString.append("OBJ:Streams");
		toString(nvString, source).append(PATH_DELIM).append("Message").append(FIELD_SEP);
		nvString.append("Self").append(PATH_DELIM).append("level=").append(getValueStr(level)).append(FIELD_SEP);
		nvString.append("Self").append(PATH_DELIM).append("msg-text=");
		Utils.quote(Utils.format(msg, args), nvString).append(END_SEP);
		return nvString.toString();
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
		return snap.getSnapshot();
	}

	protected Collection<Property> getProperties(Operation operation) {
		return operation.getProperties();
	}

	/**
	 * Makes string representation of snapshot and appends it to provided string builder.
	 * <p>
	 * In case snapshot properties have same key for "branch" and "leaf" nodes at same path level, than "leaf" node
	 * property key value is appended by configuration defined (cfg. key {@code "DuplicateKeySuffix"}, default value
	 * {@value #UNIQUE_SUFFIX}) suffix.
	 *
	 * @param nvString
	 *            string builder instance to append
	 * @param snap
	 *            snapshot instance to represent as string
	 * @return appended string builder reference
	 *
	 * @see #getUniquePropertyKey(String, com.jkoolcloud.tnt4j.core.Property[], int)
	 */
	protected StringBuilder toString(StringBuilder nvString, Trackable t, Snapshot snap) {
		Collection<Property> list = getProperties(snap);
		Property[] pArray = new Property[list.size()];
		pArray = list.toArray(pArray);
		String sName = getTrackableStr(t, getSnapName(snap));
		for (int i = 0; i < pArray.length; i++) {
			Property p = pArray[i];
			if (p.isTransient()) {
				continue;
			}

			String pKey = getUniquePropertyKey(p.getKey(), pArray, i);
			Object value = p.getValue();

			nvString.append(getKeyStr(sName, pKey));
			nvString.append(EQ).append(getValueStr(value)).append(FIELD_SEP);
		}
		return nvString;
	}

	protected StringBuilder toString(StringBuilder nvString, Trackable t, Property prop) {
		if (!prop.isTransient()) {
			String pKey = getTrackableStr(t, prop.getKey());
			Object value = prop.getValue();

			return toString(nvString, pKey, value);

		}
		return nvString;
	}

	protected StringBuilder toString(StringBuilder nvString, String key, Object value) {
		nvString.append(key);
		nvString.append(EQ).append(getValueStr(value)).append(FIELD_SEP);
		return nvString;
	}

	/**
	 * Gets property key value and makes it to be unique on same path level among all array properties.
	 * <p>
	 * In case of duplicate keys uniqueness is made by adding configuration defined (cfg. key
	 * {@code "DuplicateKeySuffix"}, default value {@value #UNIQUE_SUFFIX}) suffix to property key value.
	 *
	 * @param pKey
	 *            property key value
	 * @param pArray
	 *            properties array
	 * @param pIdx
	 *            property index in array
	 * @return unique property key value
	 */
	protected String getUniquePropertyKey(String pKey, Property[] pArray, int pIdx) {
		String ppKey;
		for (int i = pIdx + 1; i < pArray.length; i++) {
			ppKey = pArray[i].getKey();

			if (ppKey.startsWith(pKey + PATH_DELIM)) {
				pKey += uniqueSuffix;
			}
		}

		return pKey;
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
	 * Makes decorated string representation of snapshot name by referenced object name using
	 * {@link ObjectName#getCanonicalName()}.
	 *
	 * @param objName
	 *            object name
	 * @return decorated string representation of snapshot name
	 *
	 * @see #getSnapNameStr(String)
	 */
	protected String getSnapNameStr(ObjectName objName) {
		return getSnapNameStr(objName.getCanonicalName());
	}

	private String getSnapNameStr(Object nameObj) {
		if (nameObj instanceof ObjectName) {
			return getSnapNameStr((ObjectName) nameObj);
		}

		return getSnapNameStr(Utils.toString(nameObj));
	}

	/**
	 * Makes decorated string representation of {@link Snapshot} name.
	 *
	 * @param trackable
	 *            snapshot instance
	 * @return decorated string representation of snapshot name
	 *
	 * @see #getSnapNameStr(String)
	 */
	protected String getSnapName(Trackable trackable) {
		return trackable.getName();
	}

	private boolean isEmpty(Property p) {
		return p == null || p.getValue() == null;
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
	 * @see com.jkoolcloud.tnt4j.utils.Utils#toString(Object)
	 * @see #initDefaultValueReplacements()
	 * @see Utils#replace(String, Map)
	 */
	protected String getValueStr(Object value) {
		String valStr = Utils.toString(value);

		return Utils.replace(valStr, valueReplacements);
	}

	@Override
	public void setConfiguration(Map<String, Object> settings) {
		super.setConfiguration(settings);

		String pValue = Utils.getString("KeyReplacements", settings, "");
		if (StringUtils.isEmpty(pValue)) {
			initDefaultKeyReplacements();
		} else {
			Utils.parseReplacements(pValue, keyReplacements);
		}

		pValue = Utils.getString("ValueReplacements", settings, "");
		if (StringUtils.isEmpty(pValue)) {
			initDefaultValueReplacements();
		} else {
			Utils.parseReplacements(pValue, valueReplacements);
		}

		uniqueSuffix = Utils.getString("DuplicateKeySuffix", settings, uniqueSuffix);

		// pValue = com.jkoolcloud.tnt4j.utils.Utils.getString("PathLevelAttributes", settings, "");
		Map<String, Object> pathLevelAttributes = Utils.getAttributes("PathLevelAttributes", settings);

		for (Map.Entry<String, Object> entry : pathLevelAttributes.entrySet()) {
			String[] split = entry.getKey().split("\\.");
			Condition condition;
			switch (split.length) {
			case 3:
				condition = new Condition(split[1], split[2]);
				break;
			default:
				condition = new Condition();
				break;
			}
			pathLevelAttrKeys.put(condition, initPathLevelAttrKeys(String.valueOf(entry.getValue())));
		}
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
		keyReplacements.put(" ", "_");
		keyReplacements.put("\"", "'");
		keyReplacements.put("/", "%");
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
		valueReplacements.put(";", "|");
		valueReplacements.put(",", "|");
		valueReplacements.put("[", "{(");
		valueReplacements.put("]", ")}");
		valueReplacements.put("\"", "'");
	}

	/**
	 * Finds snapshot contained property by defined property name ignoring case.
	 *
	 * @param snapshot
	 *            property snapshot instance
	 * @param propName
	 *            property name
	 * @return snapshot contained property
	 */
	public static Property getSnapPropertyIgnoreCase(Snapshot snapshot, String propName) {
		if (snapshot != null) {
			for (Property prop : snapshot.getSnapshot()) {
				if (prop.getKey().equalsIgnoreCase(propName)) {
					return prop;
				}
			}
		}

		return null;
	}

	private String[][] initPathLevelAttrKeys(String levelsStr) {
		String[][] pathLevelAttrKeys = null;
		List<List<String>> levelList = new ArrayList<>();
		List<String> attrsList;

		String[] levels = levelsStr.split(";");

		for (String level : levels) {
			level = level.trim();

			if (!level.isEmpty()) {
				String[] levelAttrs = level.split(",");
				attrsList = new ArrayList<>(levelAttrs.length);

				for (String lAttr : levelAttrs) {
					lAttr = lAttr.trim();

					if (!lAttr.isEmpty()) {
						attrsList.add(lAttr);
					}
				}

				if (!attrsList.isEmpty()) {
					levelList.add(attrsList);
				}
			}
		}

		pathLevelAttrKeys = new String[levelList.size()][];
		String[] levelAttrs;
		int i = 0;
		for (List<String> level : levelList) {
			levelAttrs = new String[level.size()];
			levelAttrs = level.toArray(levelAttrs);

			pathLevelAttrKeys[i++] = levelAttrs;
		}
		return pathLevelAttrKeys;
	}

	private class Condition implements Comparable<Condition> {
		boolean all = true;
		String variable;
		String value;

		public Condition() {
			all = true;
		}

		public Condition(String variable, String value) {
			this();
			if (value != null && variable != null) {
				this.variable = variable;
				this.value = value;
				all = false;
			}
		}

		public boolean evaluate(String variable) {
			if (all) {
				return true;
			}
			return value != null && value.equals(variable);
		}

		@Override
		public boolean equals(Object obj) {
			if (all) {
				return true;
			}
			return obj != null && obj instanceof Condition && ((Condition) obj).value.equals(value)
					&& ((Condition) obj).variable.equals(variable);
		}

		@Override
		public int compareTo(Condition o) {
			return o.all ? -1 : 1; // ensures "all" is last
		}
	}
}
