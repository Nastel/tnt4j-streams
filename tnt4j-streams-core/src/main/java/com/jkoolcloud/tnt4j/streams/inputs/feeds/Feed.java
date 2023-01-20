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

package com.jkoolcloud.tnt4j.streams.inputs.feeds;

import java.io.Closeable;

/**
 * This interface defines activity data feed.
 *
 * @param <T>
 *            the type of raw activity data feed input
 *
 * @version $Revision: 1 $
 *
 * @see AbstractFeed
 * @see com.jkoolcloud.tnt4j.streams.inputs.FeedInputStream
 */
public interface Feed<T> extends Closeable {

	/**
	 * Returns raw activity data feed input object.
	 *
	 * @return raw activity data feed input object
	 */
	T getInput();

	/**
	 * Returns whether or not the input stream has been closed.
	 *
	 * @return {@code true} if stream is closed, {@code false} if still open
	 */
	boolean isClosed();

	/**
	 * Returns whether or not an error/exception occurred on the input stream.
	 *
	 * @return {@code true} if error/exception occurred, {@code false} if not
	 */
	boolean hasError();

	/**
	 * Adds defined {@code FeedListener} to feed listeners list.
	 *
	 * @param listener
	 *            the {@code FeedListener} to be added
	 */
	void addFeedListener(FeedListener listener);

	/**
	 * Removes defined {@code FeedListener} from feed listeners list.
	 *
	 * @param listener
	 *            the {@code FeedListener} to be removed
	 */
	void removeFeedListener(FeedListener listener);

	/**
	 * A feed stream read input notifications listener interface.
	 *
	 * @version $Revision: 1 $
	 *
	 * @see #addFeedListener(FeedListener)
	 */
	interface FeedListener {
		/**
		 * Makes notification on amount of bytes successfully read from input.
		 *
		 * @param bCount
		 *            read bytes count
		 */
		void bytesReadFromInput(int bCount);
	}
}
