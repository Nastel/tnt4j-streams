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

package com.jkoolcloud.tnt4j.streams.fields;

/**
 * List the supported activity entities data aggregation types.
 * 
 * @version $Revision: 2 $
 */
public enum AggregationType {
	/**
	 * Activity entity data shall be merged into parent activity entity.
	 */
	Merge,

	/**
	 * Activity entity shall be added as a child into parent activity entity.
	 *
	 * @deprecated use {@link #Relate} instead
	 */
	@Deprecated
	Join,

	/**
	 * Activity entity shall be added as a child into parent activity entity.
	 */
	Relate,

	/**
	 * Activity entity shall be added as a child into root parent activity entity.
	 */
	Relate_Flat;

	/**
	 * Checks if this aggregation type is of "Relate" type.
	 * 
	 * @return {@code true} if aggregation type is {@link #Join}, {@link #Relate} or {@link #Relate_Flat}, {@code false}
	 *         - otherwise
	 * 
	 * @see #isRelate(AggregationType)
	 */
	public boolean isRelate() {
		return isRelate(this);
	}

	/**
	 * Checks if this aggregation type is of "Flatten" type.
	 * 
	 * @return {@code true} if aggregation type is {@link #Relate_Flat}, {@code false} - otherwise
	 * 
	 * @see #isFlatten(AggregationType)
	 */
	public boolean isFlatten() {
		return isFlatten(this);
	}

	/**
	 * Checks if provided aggregation type is of "Relate" type.
	 * 
	 * @param aggType
	 *            aggregation type to check
	 * @return {@code true} if aggregation type is {@link #Join}, {@link #Relate} or {@link #Relate_Flat}, {@code false}
	 *         - otherwise
	 */
	public static boolean isRelate(AggregationType aggType) {
		return aggType == Join || aggType == Relate || aggType == Relate_Flat;
	}

	/**
	 * Checks if provided aggregation type is of "Flatten" type.
	 * 
	 * @param aggType
	 *            aggregation type to check
	 * @return {@code true} if aggregation type is {@link #Relate_Flat}, {@code false} - otherwise
	 */
	public static boolean isFlatten(AggregationType aggType) {
		return aggType == Relate_Flat;
	}
}
