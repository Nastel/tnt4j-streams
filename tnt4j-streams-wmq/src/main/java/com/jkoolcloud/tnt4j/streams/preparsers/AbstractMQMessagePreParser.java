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

package com.jkoolcloud.tnt4j.streams.preparsers;

import com.ibm.mq.MQMessage;

/**
 * Base class for abstract activity data pre-parser capable to convert incoming activity data as
 * {@link com.ibm.mq.MQMessage}.
 * 
 * @version $Revision: 1 $
 */
public abstract class AbstractMQMessagePreParser<O> extends AbstractPreParser<MQMessage, O> {

	/**
	 * {@inheritDoc}
	 * <p>
	 * This pre-parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link com.ibm.mq.MQMessage}</li>
	 * </ul>
	 */
	@Override
	public boolean isDataClassSupported(Object data) {
		return MQMessage.class.isInstance(data);
	}
}
