/*
 * Copyright 2014-2019 JKOOL, LLC.
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
 * A streamed items accounting notifications listener interface. This interface can be implemented by classes that are
 * interested in streamed items accounting for lost/filtered/skipped/processed items and streamed bytes counts.
 * 
 * @version $Revision: 1 $
 * 
 * @see com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream#addStreamItemAccountingListener(StreamItemAccountingListener)
 */
public interface StreamItemAccountingListener {
	/**
	 * This method gets called when streamed item gets lost.
	 */
	void onItemLost();

	/**
	 * This method gets called when streamed item gets filtered.
	 */
	void onItemFiltered();

	/**
	 * This method gets called when streamed item gets skipped.
	 */
	void onItemSkipped();

	/**
	 * This method gets called when streamed item gets processed.
	 */
	void onItemProcessed();

	/**
	 * Updates streamed bytes count.
	 *
	 * @param bytes
	 *            streamed item bytes count
	 */
	void onBytesStreamed(long bytes);

	/**
	 * Updates total count of bytes to be streamed.
	 *
	 * @param bytes
	 *            total count of bytes to be streamed
	 */
	void updateTotal(long bytes);

}
