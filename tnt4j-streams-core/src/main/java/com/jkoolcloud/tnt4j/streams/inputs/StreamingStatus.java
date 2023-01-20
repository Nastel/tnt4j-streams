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

package com.jkoolcloud.tnt4j.streams.inputs;

/**
 * This interface defines stream status. It allows to define independent states in different enum's by implementing this
 * interface.
 *
 * @version $Revision: 1 $
 */
public interface StreamingStatus {
	/**
	 * Returns the name of stream status.
	 *
	 * @return the name of stream status
	 */
	String name();
}
