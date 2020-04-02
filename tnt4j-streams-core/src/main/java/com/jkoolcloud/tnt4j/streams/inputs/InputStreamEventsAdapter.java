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

package com.jkoolcloud.tnt4j.streams.inputs;

import com.jkoolcloud.tnt4j.core.OpLevel;

/**
 * An abstract adapter class for receiving stream events. The methods in this class are empty. This class exists as
 * convenience for creating listener objects.
 *
 * @version $Revision: 1 $
 *
 * @see TNTInputStream#addStreamListener(InputStreamListener)
 */
public class InputStreamEventsAdapter implements InputStreamListener {
	@Override
	public void onProgressUpdate(TNTInputStream<?, ?> stream, int current, int total) {
	}

	@Override
	public void onSuccess(TNTInputStream<?, ?> stream) {
	}

	@Override
	public void onFailure(TNTInputStream<?, ?> stream, String msg, Throwable exc, String code) {
	}

	@Override
	public void onStatusChange(TNTInputStream<?, ?> stream, StreamStatus status) {
	}

	@Override
	public void onFinish(TNTInputStream<?, ?> stream, TNTInputStreamStatistics stats) {
	}

	@Override
	public void onStreamEvent(TNTInputStream<?, ?> stream, OpLevel level, String message, Object source) {
	}
}
