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

package com.jkoolcloud.tnt4j.streams.configure;

/**
 * This interface defines object having bound name.
 *
 * @version $Revision: 1 $
 */
public interface NamedObject {

	/**
	 * Sets object name.
	 *
	 * @param name
	 *            object name string
	 */
	void setName(String name);

	/**
	 * Returns object name.
	 *
	 * @return object name string
	 */
	String getName();
}
