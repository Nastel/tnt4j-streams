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

package com.jkoolcloud.tnt4j.streams.reference;

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;

import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Defines generic activity parser reference having optional array of tags bound to this reference.
 * 
 * @version $Revision: 1 $
 */
public class ParserReference implements Reference<ActivityParser> {
	private final ActivityParser parser;
	private String[] tags;

	/**
	 * Constructs a new ParserReference using defined referred parser instance.
	 * 
	 * @param parser
	 *            referred parser instance
	 */
	public ParserReference(ActivityParser parser) {
		this.parser = parser;
	}

	/**
	 * Returns referred parser instance.
	 *
	 * @return referred parser instance
	 */
	@Override
	public ActivityParser getReferent() {
		return parser;
	}

	/**
	 * Returns referenced parser instance.
	 *
	 * @return referenced parser instance
	 */
	public ActivityParser getParser() {
		return parser;
	}

	/**
	 * Returns tag strings array of activity parser
	 *
	 * @return tags strings array
	 */
	public String[] getTags() {
		return tags == null ? null : Arrays.copyOf(tags, tags.length);
	}

	/**
	 * Sets activity parser tags string. Tags are split into array using {@value Utils#TAG_DELIM}.
	 *
	 * @param tags
	 *            tags string
	 */
	public void setTags(String tags) {
		this.tags = Utils.getTags(tags);
	}

	/**
	 * Sets activity parser tag strings array.
	 *
	 * @param tags
	 *            tag strings array
	 */
	public void setTags(String... tags) {
		this.tags = tags;
	}

	/**
	 * Checks if activity data provided tags matches stacked parser reference bound tags.
	 * <p>
	 * If {@code dataTags} or parser reference bound tags are {@code null} or empty, then {@code true} is returned
	 * meaning no tags matching shall be applicable by any of both parties: data or parser.
	 *
	 * @param dataTags
	 *            data tags array
	 * @return {@code null} if any of parser or data tag arrays are {@code null} or empty, {@code true} if
	 *         {@code dataTags} or parser reference bound tags matches any element of both arrays, {@code false} -
	 *         otherwise
	 */
	public Boolean matchTags(String... dataTags) {
		if (ArrayUtils.isEmpty(dataTags) || ArrayUtils.isEmpty(tags)) {
			return null;
		}

		for (String tag : dataTags) {
			boolean tagMatch = ArrayUtils.contains(tags, tag);
			if (tagMatch) {
				return true;
			}
		}

		return false;
	}

	@Override
	public String toString() {
		return parser.getName();
	}

	/**
	 * Checks if this parser reference is continuous and shall not break stacked parsers chain for parsing provided
	 * entity data.
	 * 
	 * @return {@code true} if this reference is tagged as {@code "continuous"}, {@code false} - otherwise
	 */
	public boolean isContinuous() {
		return BooleanUtils.toBooleanDefaultIfNull(matchTags("continuous"), false); // NON-NLS
	}
}
