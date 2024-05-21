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

package com.jkoolcloud.tnt4j.streams.filters;

/**
 * This exception is thrown when stream entity value filtering process fails.
 *
 * @version $Revision: 1 $
 */
public class FilterException extends Exception {
	private static final long serialVersionUID = 5689215834352384498L;

	/**
	 * Constructs an {@code FilterException} with the specified detail message.
	 *
	 * @param message
	 *            the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
	 *            method.
	 */
	public FilterException(String message) {
		super(message);
	}

	/**
	 * Constructs an {@code FilterException} with the specified detail message and cause.
	 *
	 * @param message
	 *            the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
	 * @param cause
	 *            the cause (which is saved for later retrieval by the {@link #getCause()} method). (A {@code null}
	 *            value is permitted, and indicates that the cause is nonexistent or unknown.)
	 */
	public FilterException(String message, Throwable cause) {
		super(message, cause);
	}
}
