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

package com.jkoolcloud.tnt4j.streams.configure.sax;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.Reader;
import java.util.*;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.AttributesImpl;

import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.inputs.TNTParseableInputStream;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * @author akausinis
 * @version 1.0
 */
public class ConfigParserHandlerTest {

	private static List<String> skipConfigurationsList;
	private static File samplesDir;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		initSamplesDir();
	}

	private static void initSamplesDir() throws Exception {
		skipConfigurationsList = new ArrayList<>();

		samplesDir = new File("./samples/");
		if (!samplesDir.isDirectory()) {
			samplesDir = new File("./tnt4j-streams-core/samples/");
			if (!samplesDir.isDirectory()) {
				fail("Samples root directory doesn't exist");
			}
		} else {
			skipConfigurationsList.add("java-stream"); // NON-NLS
		}
	}

	@Test
	public void streamsSamplesConfigTest() throws Exception {
		validateConfigs(samplesDir, "tnt-data-source*.xml", true, skipConfigurationsList); // NON-NLS
		validateConfigs(samplesDir, "parsers*.xml", false, null); // NON-NLS
	}

	protected void validateConfigs(File samplesDir, String configFileWildcard, boolean checkStreams,
			List<String> skipFiles) throws Exception {
		Collection<File> sampleConfigurations = FileUtils.listFiles(samplesDir,
				FileFilterUtils
						.asFileFilter((FilenameFilter) new WildcardFileFilter(configFileWildcard, IOCase.INSENSITIVE)),
				TrueFileFilter.INSTANCE);

		Collection<File> sampleConfigurationsFiltered = new ArrayList<>(sampleConfigurations);
		if (CollectionUtils.isNotEmpty(skipFiles)) {
			for (File sampleConfiguration : sampleConfigurations) {
				for (String skipFile : skipFiles) {
					if (sampleConfiguration.getAbsolutePath().contains(skipFile)) {
						sampleConfigurationsFiltered.remove(sampleConfiguration);
					}
				}
			}
		}

		for (File sampleConfiguration : sampleConfigurationsFiltered) {
			if (StringUtils.containsAny(sampleConfiguration.getAbsolutePath(), "_dev\\", "_dev/", "_dev.")) {
				continue;
			}

			System.out.println("Reading configuration file: " + sampleConfiguration.getAbsolutePath()); // NON-NLS
			StreamsConfigSAXParser.cfgFilePath = sampleConfiguration.getAbsolutePath();
			Reader testReader = new FileReader(sampleConfiguration);
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser parser = parserFactory.newSAXParser();
			ConfigParserHandler hndlr = new ConfigParserHandler();
			parser.parse(new InputSource(testReader), hndlr);

			assertNotNull("Parsed streams config data is null", hndlr.getStreamsConfigData());
			boolean parseable = true;
			if (checkStreams) {
				assertTrue("No configured streams", hndlr.getStreamsConfigData().isStreamsAvailable());

				parseable = false;
				for (TNTInputStream<?, ?> s : hndlr.getStreamsConfigData().getStreams()) {
					if (s instanceof TNTParseableInputStream) {
						parseable = true;
						break;
					}
				}
			}
			if (parseable) {
				assertTrue("No configured parsers", hndlr.getStreamsConfigData().isParsersAvailable());
			}

			Utils.close(testReader);
		}
	}

	@Test
	public void startElementTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		test.startDocument();
		attrs.addAttribute("", "", "name", "", "Stream attr name"); // NON-NLS
		attrs.addAttribute("", "", "class", "", "Stream attr class"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "java-object", attrs); // NON-NLS

		attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream attr name"); // NON-NLS
		attrs.addAttribute("", "", "type", "", "java.lang.String"); // NON-NLS
		attrs.addAttribute("", "", "value", "", "Stream attr value"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "param", attrs); // NON-NLS

		attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream attr name"); // NON-NLS
		attrs.addAttribute("", "", "type", "", "java.lang.String"); // NON-NLS
		attrs.addAttribute("", "", "filter", "", "Stream attr filter"); // NON-NLS
		attrs.addAttribute("", "", "tnt4j-properties", "", "Stream attr tnt4j-properties"); // NON-NLS
		attrs.addAttribute("", "", "java-object", "", "Stream attr java-object"); // NON-NLS
		attrs.addAttribute("", "", "param", "", "Stream attr param"); // NON-NLS
		attrs.addAttribute("", "", "tags", "", "Stream attr tags"); // NON-NLS
		attrs.addAttribute("", "", "value", "", "Stream attr value"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "tnt-data-source", attrs); // NON-NLS
	}

	@Test(expected = SAXParseException.class)
	public void processParserTest1() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream attr name"); // NON-NLS
		attrs.addAttribute("", "", "type", "", "Stream attr type"); // NON-NLS
		attrs.addAttribute("", "", "filter", "", "Stream attr filter"); // NON-NLS
		// attrs.addAttribute("", "", "rule", "", "Stream attr rule"); // NON-NLS
		// attrs.addAttribute("", "", "step", "", "Stream attr step"); // NON-NLS
		attrs.addAttribute("", "", "tnt4j-properties", "", "Stream attr tnt4j-properties"); // NON-NLS
		attrs.addAttribute("", "", "java-object", "", "Stream attr java-object"); // NON-NLS
		attrs.addAttribute("", "", "param", "", "Stream attr param"); // NON-NLS
		attrs.addAttribute("", "", "tags", "", "Stream attr tags"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser", attrs); // NON-NLS
	}

	@Test(expected = SAXParseException.class)
	public void processParserTest2() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "type", "", "Stream attr type"); // NON-NLS
		attrs.addAttribute("", "", "class", "", "Stream attr class"); // NON-NLS
		attrs.addAttribute("", "", "filter", "", "Stream attr filter"); // NON-NLS
		// attrs.addAttribute("", "", "rule", "", "Stream attr rule"); // NON-NLS
		// attrs.addAttribute("", "", "step", "", "Stream attr step"); // NON-NLS
		attrs.addAttribute("", "", "tnt4j-properties", "", "Stream attr tnt4j-properties"); // NON-NLS
		attrs.addAttribute("", "", "java-object", "", "Stream attr java-object"); // NON-NLS
		attrs.addAttribute("", "", "param", "", "Stream attr param"); // NON-NLS
		attrs.addAttribute("", "", "tags", "", "Stream attr tags"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser", attrs); // NON-NLS
	}

	@Test(expected = SAXException.class)
	public void processParserTryCatchTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		AttributesImpl attrs = new AttributesImpl();
		test.startDocument();
		attrs.addAttribute("", "", "name", "", "Stream attr name"); // NON-NLS
		attrs.addAttribute("", "", "type", "", "Stream attr type"); // NON-NLS
		attrs.addAttribute("", "", "class", "", "java.lang.String"); // NON-NLS
		attrs.addAttribute("", "", "filter", "", "Stream attr filter"); // NON-NLS
		// attrs.addAttribute("", "", "rule", "", "Stream attr rule"); // NON-NLS
		// attrs.addAttribute("", "", "step", "", "Stream attr step"); // NON-NLS
		attrs.addAttribute("", "", "tnt4j-properties", "", "Stream attr tnt4j-properties"); // NON-NLS
		attrs.addAttribute("", "", "java-object", "", "Stream attr java-object"); // NON-NLS
		attrs.addAttribute("", "", "param", "", "Stream attr param"); // NON-NLS
		attrs.addAttribute("", "", "tags", "", "Stream attr tags"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser", attrs); // NON-NLS
	}

	@Test
	public void processFieldTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream attr name"); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser", attrs); // NON-NLS

		attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream attr name"); // NON-NLS
		attrs.addAttribute("", "", "radix", "", "555"); // NON-NLS
		attrs.addAttribute("", "", "required", "", "Requered"); // NON-NLS
		attrs.addAttribute("", "", "value", "", "555"); // NON-NLS
		attrs.addAttribute("", "", "units", "", "Units"); // NON-NLS
		attrs.addAttribute("", "", "format", "", "yyyy-MM-dd HH:mm:ss"); // NON-NLS
		attrs.addAttribute("", "", "locale", "", "lt_LT"); // NON-NLS
		attrs.addAttribute("", "", "timezone", "", "Europe/Vilnius"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field", attrs); // NON-NLS
	}

	@Test(expected = SAXParseException.class)
	public void processFieldExceptionTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field", attrs); // NON-NLS
	}

	@Test(expected = SAXParseException.class)
	public void missingAttributeTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		AttributesImpl attrs = new AttributesImpl();
		test.startDocument();
		attrs.addAttribute("", "", "name", "", "Stream name value"); // NON-NLS
		attrs.addAttribute("", "", "value", "", ""); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser", attrs); // NON-NLS
		attrs.addAttribute("", "", "name", "", ""); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field", attrs); // NON-NLS
	}

	@Test
	public void locatorSplittingEmptyElementTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream attr name"); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser", attrs); // NON-NLS

		attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream attr name"); // NON-NLS
		attrs.addAttribute("", "", "locator", "", "|555"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field", attrs); // NON-NLS
	}

	@Test(expected = SAXParseException.class)
	public void processFieldLocatorTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream attr name"); // NON-NLS
		attrs.addAttribute("", "", "datatype", "", "Timestamp"); // NON-NLS
		attrs.addAttribute("", "", "radix", "", "555"); // NON-NLS
		attrs.addAttribute("", "", "required", "", "Requered"); // NON-NLS
		attrs.addAttribute("", "", "units", "", null); // NON-NLS
		attrs.addAttribute("", "", "format", "", "yyyy-MM-dd HH:mm:ss"); // NON-NLS
		attrs.addAttribute("", "", "locale", "", "lt_LT"); // NON-NLS
		attrs.addAttribute("", "", "timezone", "", "Europe/Vilnius"); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser", attrs); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field", attrs); // NON-NLS
		attrs.addAttribute("", "", "value", "", ""); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field-locator", attrs); // NON-NLS
	}

	@Test
	public void processFieldLocatorUnitsNotNullTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream attr name"); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser", attrs); // NON-NLS

		attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream attr name"); // NON-NLS
		attrs.addAttribute("", "", "datatype", "", "Timestamp"); // NON-NLS
		attrs.addAttribute("", "", "radix", "", "555"); // NON-NLS
		attrs.addAttribute("", "", "required", "", "Requered"); // NON-NLS
		attrs.addAttribute("", "", "units", "", "Kb"); // NON-NLS
		attrs.addAttribute("", "", "format", "", "yyyy-MM-dd HH:mm:ss"); // NON-NLS
		attrs.addAttribute("", "", "locale", "", "lt_LT"); // NON-NLS
		attrs.addAttribute("", "", "timezone", "", "Europe/Vilnius"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field", attrs); // NON-NLS

		attrs = new AttributesImpl();
		attrs.addAttribute("", "", "datatype", "", "Timestamp"); // NON-NLS
		attrs.addAttribute("", "", "radix", "", "555"); // NON-NLS
		attrs.addAttribute("", "", "required", "", "Requered"); // NON-NLS
		attrs.addAttribute("", "", "units", "", "Kb"); // NON-NLS
		attrs.addAttribute("", "", "format", "", "yyyy-MM-dd HH:mm:ss"); // NON-NLS
		attrs.addAttribute("", "", "locale", "", "lt_LT"); // NON-NLS
		attrs.addAttribute("", "", "timezone", "", "Europe/Vilnius"); // NON-NLS
		attrs.addAttribute("", "", "value", "", "TEST_VALUE"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field-locator", attrs); // NON-NLS
	}

	@Test(expected = SAXParseException.class)
	public void processFieldLocatorExceptionTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream attr name"); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser"); // NON-NLS
		attrs.addAttribute("", "", "datatype", "", "DateTime"); // NON-NLS
		attrs.addAttribute("", "", "format", "", null);
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser", attrs); // NON-NLS//NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field", attrs); // NON-NLS
		attrs.addAttribute("", "", "value", "", ""); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field-locator", attrs); // NON-NLS
	}

	@Test
	public void processFieldLocatorNotNullFormatTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream attr name");// NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser", attrs); // NON-NLS

		attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream attr name");// NON-NLS
		attrs.addAttribute("", "", "datatype", "", "DateTime"); // NON-NLS
		attrs.addAttribute("", "", "format", "", "yyyy-MM-dd"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field", attrs); // NON-NLS

		attrs = new AttributesImpl();
		attrs.addAttribute("", "", "datatype", "", "DateTime"); // NON-NLS
		attrs.addAttribute("", "", "format", "", "yyyy-MM-dd"); // NON-NLS
		attrs.addAttribute("", "", "value", "", null); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field-locator", attrs); // NON-NLS
	}

	@Test(expected = SAXParseException.class)
	public void processFieldMapSourceExceptionTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream attr name"); // NON-NLS
		attrs.addAttribute("", "", "source", "", null); // NON-NLS
		attrs.addAttribute("", "", "target", "", "Stream target value"); // NON-NLS
		attrs.addAttribute("", "", "value", "", ""); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser", attrs); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field", attrs); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field-map", attrs); // NON-NLS
	}

	@Test(expected = SAXParseException.class)
	public void processFieldMapTargetExceptionTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream attr name"); // NON-NLS
		attrs.addAttribute("", "", "source", "", "Stream source value"); // NON-NLS
		attrs.addAttribute("", "", "target", "", null); // NON-NLS
		attrs.addAttribute("", "", "value", "", ""); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser", attrs); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field", attrs); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field-map", attrs); // NON-NLS
	}

	@Test(expected = SAXException.class)
	public void processStreamExceptionTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream attr name"); // NON-NLS
		attrs.addAttribute("", "", "source", "", null); // NON-NLS
		attrs.addAttribute("", "", "target", "", "Stream target value"); // NON-NLS
		attrs.addAttribute("", "", "value", "", ""); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser", attrs); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field", attrs); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "stream", attrs); // NON-NLS
	}

	@Test(expected = SAXException.class)
	public void processStreamNotNullStreamTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		TNTInputStream<?, ?> my = Mockito.mock(TNTInputStream.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream attr name"); // NON-NLS
		attrs.addAttribute("", "", "source", "", null); // NON-NLS
		attrs.addAttribute("", "", "target", "", "Stream target value"); // NON-NLS
		attrs.addAttribute("", "", "value", "", ""); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser", attrs); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field", attrs); // NON-NLS
		my.setName("Stream attr name"); // NON-NLS
		test.getStreamsConfigData().addStream(my);
		test.startElement("TEST_URL", "TEST_LOCALNAME", "stream", attrs); // NON-NLS
	}

	@Test(expected = SAXParseException.class)
	public void processStreamIsEmptyClassExceptionTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream name value"); // NON-NLS
		attrs.addAttribute("", "", "source", "", "Stream source value"); // NON-NLS
		attrs.addAttribute("", "", "target", "", "Stream target value"); // NON-NLS
		attrs.addAttribute("", "", "value", "", ""); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser", attrs); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field", attrs); // NON-NLS
		attrs.addAttribute("", "", "class", "", ""); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "stream", attrs); // NON-NLS
	}

	@Test(expected = SAXParseException.class)
	public void processStreamIsEmptyNameExceptionTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream name value"); // NON-NLS
		attrs.addAttribute("", "", "source", "", "Stream source value"); // NON-NLS
		attrs.addAttribute("", "", "target", "", "Stream target value"); // NON-NLS
		attrs.addAttribute("", "", "value", "", ""); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser", attrs); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field", attrs); // NON-NLS
		attrs.addAttribute("", "", "class", "", ""); // NON-NLS
		attrs.addAttribute("", "", "name", "", ""); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "stream", attrs); // NON-NLS
	}

	@Test(expected = SAXParseException.class)
	public void processPropertyTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream name value"); // NON-NLS
		attrs.addAttribute("", "", "source", "", "Stream source value"); // NON-NLS
		attrs.addAttribute("", "", "target", "", "Stream target value"); // NON-NLS
		attrs.addAttribute("", "", "value", "", ""); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser", attrs); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field", attrs); // NON-NLS
		attrs.addAttribute("", "", "name", "", ""); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "property", attrs); // NON-NLS
	}

	@Test(expected = SAXParseException.class)
	public void processParserRefNoParserTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream name value"); // NON-NLS
		attrs.addAttribute("", "", "source", "", "Stream source value"); // NON-NLS
		attrs.addAttribute("", "", "target", "", "Stream target value"); // NON-NLS
		attrs.addAttribute("", "", "value", "", ""); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser", attrs); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field", attrs); // NON-NLS
		test.getStreamsConfigData().getParsers().clear(); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser-ref", attrs); // NON-NLS
	}

	@Test(expected = SAXParseException.class)
	public void processParserRefTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream name value"); // NON-NLS
		attrs.addAttribute("", "", "source", "", "Stream source value"); // NON-NLS
		attrs.addAttribute("", "", "target", "", "Stream target value"); // NON-NLS
		attrs.addAttribute("", "", "value", "", ""); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser", attrs); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field", attrs); // NON-NLS
		attrs.addAttribute("", "", "name", "", ""); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser-ref", attrs); // NON-NLS
	}

	@Test(expected = SAXParseException.class)
	public void processReferenceExceptionTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream name value"); // NON-NLS
		attrs.addAttribute("", "", "source", "", "Stream source value"); // NON-NLS
		attrs.addAttribute("", "", "target", "", "Stream target value"); // NON-NLS
		attrs.addAttribute("", "", "value", "", ""); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser", attrs); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field", attrs); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.inputs.RedirectTNT4JStream"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "stream", attrs); // NON-NLS
		attrs.addAttribute("", "", "name", "", ""); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "reference", attrs); // NON-NLS
	}

	@Test(expected = SAXParseException.class)
	public void processReferenceParserExceptionTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream name value"); // NON-NLS
		attrs.addAttribute("", "", "source", "", "Stream source value"); // NON-NLS
		attrs.addAttribute("", "", "target", "", "Stream target value"); // NON-NLS
		attrs.addAttribute("", "", "value", "", ""); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser", attrs); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field", attrs); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.inputs.RedirectTNT4JStream"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "stream", attrs); // NON-NLS
		test.getStreamsConfigData().getParsers().clear();
		test.getStreamsConfigData().getStreams().clear();
		test.startElement("TEST_URL", "TEST_LOCALNAME", "reference", attrs); // NON-NLS
	}

	@Test
	public void findReferenceTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream name value"); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser", attrs); // NON-NLS

		attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream name value"); // NON-NLS
		attrs.addAttribute("", "", "value", "", ""); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field", attrs); // NON-NLS

		attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream name value"); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.inputs.RedirectTNT4JStream"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "stream", attrs); // NON-NLS

		attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream name value"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "reference", attrs); // NON-NLS
	}

	@Test(expected = SAXParseException.class)
	public void processJavaObjectNameExceptionTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream name value"); // NON-NLS
		attrs.addAttribute("", "", "source", "", "Stream source value"); // NON-NLS
		attrs.addAttribute("", "", "target", "", "Stream target value"); // NON-NLS
		attrs.addAttribute("", "", "value", "", ""); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser", attrs); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field", attrs); // NON-NLS
		attrs.addAttribute("", "", "name", "", ""); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "java-object", attrs); // NON-NLS
	}

	@Test(expected = SAXParseException.class)
	public void processJavaObjectClassExceptionTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream name value"); // NON-NLS
		attrs.addAttribute("", "", "source", "", "Stream source value"); // NON-NLS
		attrs.addAttribute("", "", "target", "", "Stream target value"); // NON-NLS
		attrs.addAttribute("", "", "value", "", ""); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser", attrs); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field", attrs); // NON-NLS
		attrs.addAttribute("", "", "class", "", ""); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "java-object", attrs); // NON-NLS
	}

	@Test(expected = SAXParseException.class)
	public void processParamTypeExceptionTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream name value"); // NON-NLS
		attrs.addAttribute("", "", "source", "", "Stream source value"); // NON-NLS
		attrs.addAttribute("", "", "target", "", "Stream target value"); // NON-NLS
		attrs.addAttribute("", "", "value", "", ""); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser", attrs); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field", attrs); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "java-object", attrs); // NON-NLS
		attrs.addAttribute("", "", "type", "", "TEST TYPE"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "param", attrs); // NON-NLS
	}

	@Test(expected = SAXParseException.class)
	public void processParamNameExceptionTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream name value"); // NON-NLS
		attrs.addAttribute("", "", "source", "", "Stream source value"); // NON-NLS
		attrs.addAttribute("", "", "target", "", "Stream target value"); // NON-NLS
		attrs.addAttribute("", "", "value", "", ""); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser", attrs); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field", attrs); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "java-object", attrs); // NON-NLS
		attrs.addAttribute("", "", "type", "", "java.lang.String"); // NON-NLS
		attrs.addAttribute("", "", "name", "", ""); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "param", attrs); // NON-NLS
	}

	@Test(expected = SAXParseException.class)
	public void processParamEmptyTypeExceptionTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream name value"); // NON-NLS
		attrs.addAttribute("", "", "source", "", "Stream source value"); // NON-NLS
		attrs.addAttribute("", "", "target", "", "Stream target value"); // NON-NLS
		attrs.addAttribute("", "", "value", "", ""); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser", attrs); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field", attrs); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "java-object", attrs); // NON-NLS
		attrs.addAttribute("", "", "type", "", ""); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "param", attrs); // NON-NLS
	}

	@Test
	public void processParamEmptyValueExceptionTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream name value"); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser", attrs); // NON-NLS

		attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream name value"); // NON-NLS
		attrs.addAttribute("", "", "value", "", ""); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field", attrs); // NON-NLS

		attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream name value"); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "java-object", attrs); // NON-NLS

		attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream name value"); // NON-NLS
		attrs.addAttribute("", "", "type", "", "java.lang.String"); // NON-NLS
		attrs.addAttribute("", "", "value", "", ""); // NON-NLS
		attrs.addAttribute("", "", "value", "", ""); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "param", attrs); // NON-NLS
	}

	@Test(expected = SAXParseException.class)
	public void processTnt4jPropertiesExceptionTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream name value"); // NON-NLS
		attrs.addAttribute("", "", "source", "", "Stream source value"); // NON-NLS
		attrs.addAttribute("", "", "target", "", "Stream target value"); // NON-NLS
		attrs.addAttribute("", "", "value", "", ""); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser", attrs); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field", attrs); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.inputs.RedirectTNT4JStream"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "stream", attrs); // NON-NLS
		test.currStream = null;
		test.startElement("TEST_URL", "TEST_LOCALNAME", "tnt4j-properties", attrs); // NON-NLS
	}

	@Test
	public void charactersTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		test.elementDataStack.push(new StringBuilder());
		String cData = "abcde"; // NON-NLS
		test.characters(cData.toCharArray(), 0, cData.length());
		assertEquals("abcde", test.getElementData()); // NON-NLS
		assertEquals(0, test.elementDataStack.size());
	}

	@Test
	public void charactersNullTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		assertNull(test.getElementData());
	}

	@Test
	public void endElementTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.endElement("TEST_URL", "TEST_LOCALNAME", "field-locator"); // NON-NLS
	}

	@Test(expected = SAXException.class)
	public void endElementExceptionTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream name value"); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "parser", attrs); // NON-NLS

		attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream name value"); // NON-NLS
		test.startElement("TEST_URL", "TEST_LOCALNAME", "field", attrs); // NON-NLS

		test.endElement("TEST_URL", "TEST_LOCALNAME", "field"); // NON-NLS
	}

	@Test
	public void endElementHandleJavaObjectTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.endElement("TEST_URL", "TEST_LOCALNAME", "java-object"); // NON-NLS
	}

	@Test
	public void endElementHandlePropertyTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		test.elementDataStack.push(new StringBuilder());
		String cData = "TEST_STRING"; // NON-NLS
		test.characters(cData.toCharArray(), 0, cData.length());
		test.endElement("TEST_URL", "TEST_LOCALNAME", "property"); // NON-NLS
		assertEquals("TEST_STRING", test.getElementData());
		assertEquals(0, test.elementDataStack.size());
	}

	@Test(expected = SAXParseException.class)
	public void startElementExceptionTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		TNTInputStream<?, ?> my = Mockito.mock(TNTInputStream.class, Mockito.CALLS_REAL_METHODS);
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream name value"); // NON-NLS
		attrs.addAttribute("", "", "source", "", "Stream source value"); // NON-NLS
		attrs.addAttribute("", "", "target", "", "Stream target value"); // NON-NLS
		attrs.addAttribute("", "", "value", "", ""); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser"); // NON-NLS
		attrs.addAttribute("", "", "type", "", "java.lang.String"); // NON-NLS
		test.getStreamsConfigData().addStream(my);
		test.startElement("TEST_URL", "TEST_LOCALNAME", "tnt-data-source", attrs); // NON-NLS
	}

	@Test(expected = SAXParseException.class)
	public void startElementExceptionTwoTest() throws Exception {
		ConfigParserHandler test = Mockito.mock(ConfigParserHandler.class, Mockito.CALLS_REAL_METHODS);
		test.startDocument();
		TNTInputStream<?, ?> my = Mockito.mock(TNTInputStream.class, Mockito.CALLS_REAL_METHODS);
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute("", "", "name", "", "Stream name value"); // NON-NLS
		attrs.addAttribute("", "", "source", "", "Stream source value"); // NON-NLS
		attrs.addAttribute("", "", "target", "", "Stream target value"); // NON-NLS
		attrs.addAttribute("", "", "value", "", ""); // NON-NLS
		attrs.addAttribute("", "", "class", "", "com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser"); // NON-NLS
		attrs.addAttribute("", "", "type", "", "java.lang.String"); // NON-NLS
		test.getStreamsConfigData().addStream(my);
		test.startElement("TEST_URL", "TEST_LOCALNAME", "tw-direct-feed", attrs); // NON-NLS
	}

	@Test
	public void applyVariablePropertiesTest() {
		String VALUE = "${user.home}/abc/bcd";
		String VALUE1 = "file://${JAVA_HOME}/abc/bcd";

		HashMap<String, String> propertiesMap = new HashMap<String, String>() {
			private static final long serialVersionUID = 1L;
			{
				put("filename", "*");
				put("test", "${test}");
				put("path", VALUE);
				put("path2", VALUE1);
			}
		};
		System.setProperty("test", "best");
		Collection<Map.Entry<String, String>> entries = ConfigParserHandler.applyVariableProperties(propertiesMap);

		HashMap<String, String> propertiesAfterApply = new HashMap<>();
		for (Map.Entry<String, String> entry : entries) {
			propertiesAfterApply.put(entry.getKey(), entry.getValue());
		}

		assertEquals("*", propertiesAfterApply.get("filename"));
		assertEquals("best", propertiesAfterApply.get("test"));
		assertEquals(VALUE.replace("${user.home}", System.getProperty("user.home")), propertiesAfterApply.get("path"));
		assertEquals(VALUE1.replace("${JAVA_HOME}", System.getenv("JAVA_HOME")), propertiesAfterApply.get("path2"));
	}
}
