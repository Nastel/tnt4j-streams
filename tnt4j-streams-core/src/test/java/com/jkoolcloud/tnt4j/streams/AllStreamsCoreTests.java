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

package com.jkoolcloud.tnt4j.streams;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.jkoolcloud.tnt4j.streams.configure.AllConfigureTests;
import com.jkoolcloud.tnt4j.streams.fields.AllFieldsTests;
import com.jkoolcloud.tnt4j.streams.filters.AllFiltersTests;
import com.jkoolcloud.tnt4j.streams.inputs.AllInputsTests;
import com.jkoolcloud.tnt4j.streams.matchers.AllMatchersTests;
import com.jkoolcloud.tnt4j.streams.parsers.AllParsersTests;
import com.jkoolcloud.tnt4j.streams.preparsers.AllPreparsersTests;
import com.jkoolcloud.tnt4j.streams.utils.AllUtilsTests;

/**
 * @author akausinis
 * @version 1.0
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ StreamsAgentTest.class, AllConfigureTests.class, AllFieldsTests.class, AllFiltersTests.class,
		AllInputsTests.class, AllParsersTests.class, AllPreparsersTests.class, AllUtilsTests.class,
		AllMatchersTests.class })
public class AllStreamsCoreTests {
}
