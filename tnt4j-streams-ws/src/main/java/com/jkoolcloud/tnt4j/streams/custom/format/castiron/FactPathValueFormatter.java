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

import org.apache.commons.collections4.MapUtils;

import com.jkoolcloud.tnt4j.core.Operation;
import com.jkoolcloud.tnt4j.core.Property;
import com.jkoolcloud.tnt4j.core.Snapshot;
import com.jkoolcloud.tnt4j.core.Trackable;
import com.jkoolcloud.tnt4j.streams.utils.StreamsConstants;
import com.jkoolcloud.tnt4j.utils.Utils;

/**
 * This class provides key/value formatting for tnt4j activities, events and snapshots. The output format follows the
 * following format:
 * <p>
 * {@code "OBJ:object-path\name1=value1,...,object-path\nameN=valueN"}.
 * </p>
 * Newline is added at the end of each line.
 * <p>
 * This formatter supports the following configuration properties (in addition to those supported by
 * {@link FactNameValueFormatter}):
 * <ul>
 * <li>DuplicateKeySuffix - suffix value for duplicate path level keys. Default value - {@code "_"}. (Optional)</li>
 * <li>PathLevelAttributes - configures produced path tokens sequence. Format is:
 * {@code event.formatter.PathLevelAttributes[.CONDITION]: fieldName1; fieldName2, fieldName4; fieldName6;...;fieldNameN},
 * where:
 * <ul>
 * <li>{@code CONDITION} - path build condition having format {@code fieldName.fieldValue} (Optional)</li>
 * <li>{@code fieldNameX} - name of activity entity field or property</li>
 * <li>{@code ;} - path level delimiter</li>
 * <li>{@code ,} - path token field/property names delimiter (path adds first found non-null value)</li>
 * </ul>
 * E.g. {@code event.formatter.PathLevelAttributes.CastIronType.Jobs: CastIronType; CastIronStatus; Resource; Name}
 * {@code event.formatter.PathLevelAttributes.CastIronType.Logs: CastIronType; Name; Severity}. Default value -
 * {@code Name}. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.core.Trackable#getFieldValue(String)
 */
public class FactPathValueFormatter extends FactNameValueFormatter {

	/**
	 * Default duplicate path level key values suffix {@value}.
	 */
	public static final String UNIQUE_SUFFIX = "_"; // NON-NLS

	private String uniqueSuffix = UNIQUE_SUFFIX;
	private Map<Condition, String[][]> pathLevelAttrKeys = new TreeMap<>();

	private Comparator<Snapshot> snapshotComparator;
	private Comparator<Property> propertyComparator;

	/**
	 * Constructs a new instance of {@code FactPathValueFormatter}.
	 */
	public FactPathValueFormatter() {
		super();
	}

	@Override
	protected Collection<Snapshot> getSnapshots(Operation op) {
		Collection<Snapshot> sList = super.getSnapshots(op);

		return getSortedCollection(sList, getSnapshotComparator());
	}

	private Comparator<Snapshot> getSnapshotComparator() {
		if (snapshotComparator == null) {
			snapshotComparator = new Comparator<>() {
				@Override
				public int compare(Snapshot s1, Snapshot s2) {
					String s1Path = getSnapName(s1);
					String s2Path = getSnapName(s2);

					return s1Path.compareTo(s2Path);
				}
			};
		}

		return snapshotComparator;
	}

	@Override
	protected Collection<Property> getProperties(Snapshot snap) {
		Collection<Property> pList = super.getProperties(snap);

		return getSortedCollection(pList, getPropertyComparator());
	}

	@Override
	protected Collection<Property> getProperties(Operation operation) {
		Collection<Property> pList = super.getProperties(operation);

		return getSortedCollection(pList, getPropertyComparator());
	}

	private static <T> Collection<T> getSortedCollection(Collection<T> col, Comparator<T> comp) {
		List<T> cList;
		if (col instanceof List<?>) {
			cList = (List<T>) col;
		} else {
			cList = Collections.list(Collections.enumeration(col));
		}
		cList.sort(comp);

		return cList;
	}

	private Comparator<Property> getPropertyComparator() {
		if (propertyComparator == null) {
			propertyComparator = new Comparator<>() {
				@Override
				public int compare(Property p1, Property p2) {
					return p1.getKey().compareTo(p2.getKey());
				}
			};
		}

		return propertyComparator;
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
	@Override
	protected StringBuilder toString(StringBuilder nvString, Trackable t, Snapshot snap) {
		Collection<Property> list = getProperties(snap);
		Property[] pArray = list.toArray(new Property[list.size()]);
		String sName = getTrackableKey(t, getSnapName(snap));
		for (int i = 0; i < pArray.length; i++) {
			Property p = pArray[i];
			if (p.isTransient()) {
				continue;
			}

			String pKey = getUniquePropertyKey(p.getKey(), pArray, i);
			Object value = p.getValue();

			nvString.append(getKeyStr(sName, pKey));
			nvString.append(EQ).append(getValueStr(value)).append(StreamsConstants.DEFAULT_VALUES_DELIM);
		}
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
	 * Returns name for provided {@code trackable} instance.
	 * <p>
	 * Builds path using user configured ({@code "PathLevelAttributes"} property) path of trackable values.
	 *
	 * @param trackable
	 *            trackable instance to get name for
	 * @return name of provided trackable
	 */
	@Override
	protected String getTrackableName(Trackable trackable) {
		StringBuilder pathBuilder = new StringBuilder(128);
		Object pv;

		if (MapUtils.isNotEmpty(pathLevelAttrKeys)) {
			for (Map.Entry<Condition, String[][]> entry : pathLevelAttrKeys.entrySet()) {
				pv = trackable.getFieldValue(entry.getKey().variable);
				if (entry.getKey().evaluate(Utils.toString(pv))) {
					for (String[] levelAttrKeys : entry.getValue()) {
						inner: for (String pKey : levelAttrKeys) {
							pv = trackable.getFieldValue(pKey);
							if (pv != null) {
								appendPath(pathBuilder, pv);
								break inner;
							}
						}
					}
					break; // handle first
				}
			}
		} else {
			pathBuilder.append(trackable.getName());
		}

		return pathBuilder.toString();
	}

	/**
	 * Appends provided path string builder with path token string.
	 *
	 * @param pathBuilder
	 *            path string builder to append
	 * @param pathToken
	 *            path token data to add to path
	 * @return appended path string builder instance
	 */
	protected StringBuilder appendPath(StringBuilder pathBuilder, Object pathToken) {
		if (pathToken != null) {
			pathBuilder.append(pathBuilder.length() > 0 ? PATH_DELIM : "").append(Utils.toString(pathToken));
		}

		return pathBuilder;
	}

	@Override
	public void setConfiguration(Map<String, ?> settings) {
		super.setConfiguration(settings);

		uniqueSuffix = Utils.getString("DuplicateKeySuffix", settings, uniqueSuffix); // NON-NLS

		Map<String, ?> pathLevelAttributes = Utils.getAttributes("PathLevelAttributes", settings); // NON-NLS

		for (Map.Entry<String, ?> entry : pathLevelAttributes.entrySet()) {
			String[] split = entry.getKey().split("\\."); // NON-NLS
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

	private static String[][] initPathLevelAttrKeys(String levelsStr) {
		String[][] pathLevelAttrKeys = null;
		List<List<String>> levelList = new ArrayList<>();
		List<String> attrsList;

		String[] levels = levelsStr.split(";");

		for (String level : levels) {
			level = level.trim();

			if (!level.isEmpty()) {
				String[] levelAttrs = level.split(","); // NON-NLS
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
			levelAttrs = level.toArray(new String[level.size()]);

			pathLevelAttrKeys[i++] = levelAttrs;
		}
		return pathLevelAttrKeys;
	}

	private static class Condition implements Comparable<Condition> {
		boolean all = true;
		String variable;
		String value;

		Condition() {
			all = true;
		}

		Condition(String variable, String value) {
			this();
			if (value != null && variable != null) {
				this.variable = variable;
				this.value = value;
				all = false;
			}
		}

		boolean evaluate(String variable) {
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
