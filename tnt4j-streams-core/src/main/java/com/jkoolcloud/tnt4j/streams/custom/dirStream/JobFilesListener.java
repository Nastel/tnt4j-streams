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

package com.jkoolcloud.tnt4j.streams.custom.dirStream;

import java.io.File;

/**
 * A streaming job configuration file status change notifications listener interface. This interface can be implemented
 * by classes that are interested in streaming process progress and status changes.
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.custom.dirStream.DirStreamingManager#addJobFileListener(JobFilesListener)
 */
public interface JobFilesListener {

	/**
	 * This method gets called when files watchdog detects changed state of changed streaming job configuration file.
	 *
	 * @param jobCfgFile
	 *            streaming job configuration file
	 * @param jobId
	 *            job identifier
	 * @param fileState
	 *            new watchdog detected streaming job configuration file state
	 */
	void onJobFileStateChanged(File jobCfgFile, String jobId, JobFileState fileState);
}
