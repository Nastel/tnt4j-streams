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
 * A streamed items processing notifications listener interface. This interface can be implemented by classes that are
 * interested in streamed items processing progress and status changes.
 * 
 * @version $Revision: 1 $
 *
 * @param <T>
 *            processing tracking item type
 * 
 * @see com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream#addItemProcessingListener(StreamItemProcessingListener)
 */
public interface StreamItemProcessingListener<T> {
	/**
	 * This method gets called when stream is going to get next item to process.
	 *
	 * @return processing progress/status tracking context
	 */
	Context<T> beforeNextItem();

	/**
	 * This method gets called when stream has got next item to process.
	 *
	 * @param context
	 *            processing progress/status tracking context
	 */
	void afterNextItem(Context<T> context);

	/**
	 * This method gets called when stream is starting activity item processing/parsing.
	 *
	 * @return processing progress/status tracking context
	 */
	Context<T> beforeProcessItem();

	/**
	 * This method gets called when stream has completed activity item processing (parsing).
	 *
	 * @param context
	 *            processing progress/status tracking context
	 */
	void afterProcessItem(Context<T> context);

	/**
	 * Stream item processing context used to track processing progress and status.
	 *
	 * @param <T>
	 *            processing tracking item type
	 */
	class Context<T> {
		private T item;

		/**
		 * Constructs a new bound processing progress and status tracking context.
		 *
		 * @param item
		 *            processing progress and status tracking item
		 */
		Context(T item) {
			this.item = item;
		}

		/**
		 * Returns context bound processing progress and status tracking item.
		 *
		 * @return processing progress and status tracking item
		 */
		public T getItem() {
			return item;
		}
	}
}
