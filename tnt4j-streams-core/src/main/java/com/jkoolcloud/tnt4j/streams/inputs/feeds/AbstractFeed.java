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

package com.jkoolcloud.tnt4j.streams.inputs.feeds;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Base class providing skeletal implementation of {@link Feed} interface to cover common features.
 *
 * @param <T>
 *            the type of raw activity data feed input
 *
 * @version $Revision: 1 $
 *
 * @see ReaderFeed
 * @see StreamFeed
 */
public abstract class AbstractFeed<T extends Closeable> implements Feed<T> {
	private boolean closed = false;
	private boolean error = false;

	private List<FeedListener> feedListeners;

	private T feedInput;

	/**
	 * Sets raw activity data feed input object.
	 *
	 * @param fs
	 *            raw activity data feed input object
	 */
	protected void setInput(T fs) {
		this.feedInput = fs;
	}

	@Override
	public T getInput() {
		return feedInput;
	}

	/**
	 * Sets error flag value.
	 * 
	 * @param error
	 *            error flag value
	 */
	protected void setError(boolean error) {
		this.error = error;
	}

	@Override
	public boolean hasError() {
		return error;
	}

	/**
	 * Closes feed and raw activity data feed input.
	 */
	@Override
	public void close() throws IOException {
		closeFeed();

		Utils.close(feedInput);
	}

	/**
	 * Marks feed closed and removes all feed listeners.
	 */
	protected void closeFeed() {
		if (closed) {
			return;
		}

		closed = true;

		if (feedListeners != null) {
			feedListeners.clear();
		}
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	/**
	 * Makes notification on amount of bytes successfully read from input source.
	 * 
	 * @param bCount
	 *            read bytes count
	 */
	protected void notifyBytesRead(int bCount) {
		if (feedListeners != null) {
			for (FeedListener fl : feedListeners) {
				fl.bytesReadFromInput(bCount);
			}
		}
	}

	@Override
	public void addFeedListener(FeedListener listener) {
		if (listener == null) {
			return;
		}

		if (feedListeners == null) {
			feedListeners = new ArrayList<>();
		}

		feedListeners.add(listener);
	}

	@Override
	public void removeFeedListener(FeedListener listener) {
		if (listener != null && feedListeners != null) {
			feedListeners.remove(listener);
		}
	}
}
