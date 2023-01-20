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

package com.jkoolcloud.tnt4j.streams.configure.state;

import javax.xml.bind.annotation.*;

/**
 * Persistable state of currently streamed file data for the purpose of restoring streaming after streaming process
 * stops/fails.
 * <p>
 * Uses file header CRC to point the file, instead of commonly used name, because name of streamed rolling log file is
 * changing. Last read line pointer has two attributes: line number and line CRC.
 *
 * @version $Revision: 1 $
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "tnt-file-access-state")
@XmlType(name = "FileAccessState")
public class FileAccessState {
	/**
	 * CRC value of file header last read.
	 */
	@XmlElement(required = true)
	public Long currentFileCrc;
	/**
	 * Line number of file last read.
	 */
	@XmlElement(required = true)
	public Integer currentLineNumber;
	/**
	 * CRC value of line last read.
	 */
	@XmlElement(required = true)
	public Long currentLineCrc;
	/**
	 * Timestamp value of last file read.
	 */
	@XmlElement
	public Long lastReadTime;

	/**
	 * Constructs a new ActivityJsonParser.
	 */
	public FileAccessState() {
	}
}