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

package com.jkoolcloud.tnt4j.streams.parsers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.jkoolcloud.tnt4j.streams.TestUtils;
import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.*;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.inputs.TNTParseableInputStream;
import com.jkoolcloud.tnt4j.streams.preparsers.XMLFromBinDataPreParser;
import com.jkoolcloud.tnt4j.streams.reference.MatchingParserReference;
import com.jkoolcloud.tnt4j.streams.utils.NamespaceMap;
import com.jkoolcloud.tnt4j.streams.utils.StreamsXMLUtils;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.streams.utils.UtilsTest;

/**
 * @author akausinis
 * @version 1.0
 */
public class ActivityXmlParserTest extends GenericActivityParserTestBase<ActivityXmlParser, Node> {

	private final static Object simpleString = "<MsgData format=\"string\" value=\"Message Body\"/>"; // NON-NLS
	private TNTInputStream<String, ActivityInfo> is;

	@Override
	@Before
	@SuppressWarnings("unchecked")
	public void prepare() {
		parser = new ActivityXmlParser();
		is = mock(TNTInputStream.class);
		ActivityField field = new ActivityField("Test");
		ActivityFieldLocator locator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "MsgData/@value");
		field.addLocator(locator);
		parser.addField(field);
		parser.setProperties(null);
	}

	@Test
	public void simpleStringParseTest() throws Exception {
		ActivityInfo ai = parser.parse(is, simpleString);
		assertNotNull(ai);
	}

	@Test
	public void simpleByteArrayParseTest() throws Exception {
		ActivityInfo ai = parser.parse(is, simpleString.toString().getBytes());
		assertNotNull(ai);
	}

	@Test
	public void simpleBufferedReaderParseTest() throws Exception {
		BufferedReader reader = new BufferedReader(new StringReader(simpleString.toString()));
		ActivityInfo ai = parser.parse(is, reader);
		assertNotNull(ai);
	}

	@Test
	public void simpleReaderParseTest() throws Exception {
		Reader reader = new StringReader(simpleString.toString());
		ActivityInfo ai = parser.parse(is, reader);
		assertNotNull(ai);
	}

	@Test
	public void simpleInputStreamParseTest() throws Exception {
		InputStream bis = new ByteArrayInputStream(simpleString.toString().getBytes());
		ActivityInfo ai = parser.parse(is, bis);
		assertNotNull(ai);
	}

	@Override
	@Test
	public void setPropertiesTest() throws Exception {
		setProperty(parser, ParserProperties.PROP_NAMESPACE, "NAMESPACE=asdcf"); // NON-NLS
		setProperty(parser, ParserProperties.PROP_REQUIRE_ALL, true);
	}

	@Ignore("Not completed")
	@Test
	public void getLocatorValue() throws Exception {
		TNTInputStream<?, ?> stream = mock(TNTInputStream.class);
		ActivityFieldLocator locator = new ActivityFieldLocator("Label", "MsgData/@value"); // NON-NLS
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document document = builder.parse(UtilsTest.toInputStream(simpleString.toString()));

		// when(locator.getLocator()).thenReturn("MsgData");
		// when(locator.formatValue(any(Object.class))).thenCallRealMethod();
		//
		Object locatorValue = parser.getLocatorValue(locator, makeContext(stream, document));
		assertNotNull(locatorValue);
		// TODO
	}

	@Test
	public void stackedParserMatchTest() throws Exception {
		TestUtils.configureConsoleLogger();
		Map<String, String> EMPTY_PROPS_MAP = new HashMap<>(0);

		String STACKER_PARSER_FIELD = "StackerParserField";

		ActivityXmlParser parser = new ActivityXmlParser();
		parser.setProperties(EMPTY_PROPS_MAP.entrySet());
		ActivityField field = new ActivityField(STACKER_PARSER_FIELD);
		ActivityFieldLocator stLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "/root",
				ActivityFieldDataType.AsInput);
		field.addLocator(stLocator);
		field.setTransparent(true);
		parser.addField(field);

		ActivityXmlParser stackedParser1 = new ActivityXmlParser();
		stackedParser1.setName("StackedParser1");
		stackedParser1.setProperties(EMPTY_PROPS_MAP.entrySet());

		String field1 = "StackedField1";

		ActivityField stField1 = new ActivityField(field1);
		stField1.addLocator(new ActivityFieldLocator(ActivityFieldLocatorType.Label, "/root/test"));
		stackedParser1.addField(stField1);

		ActivityXmlParser stackedParser2 = new ActivityXmlParser();
		stackedParser2.setName("StackedParser2");
		stackedParser2.setProperties(EMPTY_PROPS_MAP.entrySet());

		String field2 = "StackedField2";

		ActivityField stField2 = new ActivityField(field2);
		stField2.addLocator(new ActivityFieldLocator(ActivityFieldLocatorType.Label, "/root/test"));
		stackedParser2.addField(stField2);

		MatchingParserReference parserRef1 = new MatchingParserReference(stackedParser1);
		parserRef1.addMatchExpression("xpath:/root/test1");

		MatchingParserReference parserRef2 = new MatchingParserReference(stackedParser2);
		parserRef2.addMatchExpression("xpath:/root/test2");

		field.addStackedParser(parserRef1, "Merge", "Field");
		field.addStackedParser(parserRef2, "Merge", "Field");

		String testData1 = "<?xml version=\"1.0\" encoding=\"utf-8\"?><root><test>a</test><test1>1</test1></root>";
		String testData2 = "<?xml version=\"1.0\" encoding=\"utf-8\"?><root><test>b</test><test2>2</test2></root>";

		ActivityInfo result1 = parser.parse(new TestUtils.SimpleTestStream(), testData1);
		ActivityInfo result2 = parser.parse(new TestUtils.SimpleTestStream(), testData2);

		assertEquals("a", result1.getFieldValue(field1));
		assertNull(result1.getFieldValue(field2));
		assertNull(result2.getFieldValue(field1));
		assertEquals("b", result2.getFieldValue(field2));
	}

	@Test
	public void testNamespaceAwareXpathResolveWithPreparser() throws Exception {
		NamespaceTestSuite testSuite = new NamespaceTestSuite().invoke();
		DocumentBuilderFactory domFactory = testSuite.getDomFactory();
		String xmlString = testSuite.getXmlString();
		XPath xPath = testSuite.getxPath();
		Map<String, String> namespaces = testSuite.getNamespaces();

		DocumentBuilder builder = domFactory.newDocumentBuilder();
		// Document document = builder.parse(IOUtils.toInputStream(xmlString, Utils.UTF8));
		XMLFromBinDataPreParser xmlFromBinDataPreParser = new XMLFromBinDataPreParser();
		Document document = xmlFromBinDataPreParser.preParse(xmlString);

		NamespaceMap nsContext = new NamespaceMap();
		xPath.setNamespaceContext(nsContext);

		nsContext.addPrefixUriMappings(namespaces);

		Document tDoc = document.getOwnerDocument();
		Element docElem = tDoc == null ? null : tDoc.getDocumentElement();
		if (tDoc == null || StringUtils.isEmpty(tDoc.getNamespaceURI())) {
			document = builder
					.parse(ReaderInputStream.builder().setReader(new StringReader(Utils.documentToString(document)))
							.setCharset(StandardCharsets.UTF_8).get());
		}

		NamespaceMap documentNamespaces = new NamespaceMap();
		StreamsXMLUtils.resolveDocumentNamespaces(document, documentNamespaces, false);

		String evaluate = xPath.compile("/soapenv:Envelope/soapenv:Header/ch:TSYSprofileInq/ch:clientData")
				.evaluate(document);
		assertEquals("xxxxxx-343e-46af-86aa-634a3688cf30", evaluate);
		evaluate = xPath.compile("/Envelope/Header/TSYSprofileInq/clientData").evaluate(document);
		assertEquals("", evaluate);
	}

	@Test
	public void testNamespaceAwareXpathResolveWithGenuineParser() throws Exception {
		NamespaceTestSuite testSuite = new NamespaceTestSuite().invoke();
		DocumentBuilderFactory domFactory = testSuite.getDomFactory();
		String xmlString = testSuite.getXmlString();
		XPath xPath = testSuite.getxPath();
		Map<String, String> namespaces = testSuite.getNamespaces();

		DocumentBuilder builder = domFactory.newDocumentBuilder();
		Document document = builder.parse(IOUtils.toInputStream(xmlString, StandardCharsets.UTF_8));
		XMLFromBinDataPreParser xmlFromBinDataPreParser = new XMLFromBinDataPreParser();

		NamespaceMap nsContext = new NamespaceMap();
		xPath.setNamespaceContext(nsContext);

		nsContext.addPrefixUriMappings(namespaces);

		NamespaceMap documentNamespaces = new NamespaceMap();
		StreamsXMLUtils.resolveDocumentNamespaces(document, documentNamespaces, false);

		String evaluate = xPath.compile("/soapenv:Envelope/soapenv:Header/ch:TSYSprofileInq/ch:clientData")
				.evaluate(document);
		assertEquals("xxxxxx-343e-46af-86aa-634a3688cf30", evaluate);
		evaluate = xPath.compile("/Envelope/Header/TSYSprofileInq/clientData").evaluate(document);
		assertEquals("", evaluate);
	}

	private class NamespaceTestSuite {
		private Map<String, String> namespaces;
		private XPath xPath;
		private String xmlString;
		private DocumentBuilderFactory domFactory;

		public Map<String, String> getNamespaces() {
			return namespaces;
		}

		public XPath getxPath() {
			return xPath;
		}

		public String getXmlString() {
			return xmlString;
		}

		public DocumentBuilderFactory getDomFactory() {
			return domFactory;
		}

		public NamespaceTestSuite invoke() throws ParserConfigurationException {
			namespaces = new HashMap<> ()
			{
				private static final long serialVersionUID = 7566251836422017139L;

				{
					put ("soapenv", "http://schemas.xmlsoap.org/soap/envelope/");
					put ("ts", "http://github.com/Nastel/tnt4j-streams");
					put ("xml", "http://www.w3.org/XML/1998/namespace");
					put ("ch", "urn://tsys.com/xmlmessaging/CH");
					put ("xmlns", "http://schemas.microsoft.com/sharepoint/soap/");
					put ("xsi", "http://www.w3.org/2001/XMLSchema-instance");
				}
			};
			xPath = StreamsXMLUtils.getStreamsXPath();
			xmlString = "<soapenv:Envelope xmlns:ch=\"urn://tsys.com/xmlmessaging/CH\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
					+ "\t<br/>\n" + "\t<soapenv:Header>\n" + "\t\t<br/>\n" + "\t\t<ch:TSYSprofileInq>\n"
					+ "\t\t\t<ch:clientID>xxxx</ch:clientID>\n" + "\t\t\t<ch:userID>ARS</ch:userID>\n"
					+ "\t\t\t<ch:vendorID>00000000</ch:vendorID>\n"
					+ "\t\t\t<ch:clientData>xxxxxx-343e-46af-86aa-634a3688cf30</ch:clientData>\n"
					+ "\t\t</ch:TSYSprofileInq>\n" + "\t\t<br/>\n" + "\t</soapenv:Header>\n" + "\t<br/>\n"
					+ "\t<soapenv:Body>\n" + "\t\t<br/>\n" + "\t\t<ch:inqMultiResponse>\n" + "\t\t\t<br/>\n"
					+ "\t\t\t<ch:inqMultiResult>\n" + "\t\t\t\t<br/>\n" + "\n" + "\t\t\t\t<br/>\n"
					+ "\t\t\t\t<ch:inquireResult xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"2.14.0\" xsi:type=\"ch:inqGeneralAcctResponseType\">\n"
					+ "\n" + "\n" + "\t\t\t\t</ch:inquireResult>\n" + "\t\t\t</ch:inqMultiResult>\n"
					+ "\t\t</ch:inqMultiResponse>\n" + "\t</soapenv:Body>\n" + "</soapenv:Envelope>";

			domFactory = DocumentBuilderFactory.newInstance();
			domFactory.setNamespaceAware(true);
			// disabling obsolete and insecure DTD loading
			domFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			// Constants.XERCES_FEATURE_PREFIX + Constants.LOAD_DTD_GRAMMAR_FEATURE
			domFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false); // NON-NLS
			// Constants.XERCES_FEATURE_PREFIX + Constants.LOAD_EXTERNAL_DTD_FEATURE
			domFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false); // NON-NLS
			return this;
		}
	}

	@Test
	public void testStackedParser() throws Exception {
		ActivityXmlParser parser = new ActivityXmlParser() {
			@Override
			protected Object getTextOnDemand(Node node, ActivityFieldLocator locator, ActivityContext cData,
					AtomicBoolean formattingNeeded) throws ParseException {
				return node;
			}
		};

		ActivityXmlParser stackedParser = new ActivityXmlParser() {
			@Override
			protected Object getTextOnDemand(Node node, ActivityFieldLocator locator, ActivityContext cData,
					AtomicBoolean formattingNeeded) throws ParseException {
				return node;
			}
		};
		parser.initXmlParser(new HashMap<>());
		stackedParser.initXmlParser(new HashMap<>());
		String data = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
				+ "<root xmlns:foo=\"http://www.foo.org/\" xmlns:bar=\"http://www.bar.org\">\n" + "\t<employees>\n"
				+ "\t\t<employee id=\"1\">Johnny Dapp</employee>\n" + "\t\t<employee id=\"2\">Al Pacino</employee>\n"
				+ "\t\t<employee id=\"3\">Robert De Niro</employee>\n"
				+ "\t\t<employee id=\"4\">Kevin Spacey</employee>\n"
				+ "\t\t<employee id=\"5\">Denzel Washington</employee>\n" + "\t\t\n" + "\t</employees>\n"
				+ "\t<foo:companies>\n" + "\t\t<foo:company id=\"6\">Tata Consultancy Services</foo:company>\n"
				+ "\t\t<foo:company id=\"7\">Wipro</foo:company>\n"
				+ "\t\t<foo:company id=\"8\">Infosys</foo:company>\n"
				+ "\t\t<foo:company id=\"9\">Microsoft</foo:company>\n"
				+ "\t\t<foo:company id=\"10\">IBM</foo:company>\n" + "\t\t<foo:company id=\"11\">Apple</foo:company>\n"
				+ "\t\t<foo:company id=\"12\">Oracle</foo:company>\n" + "\t</foo:companies>\n" + "</root>";
		GenericActivityParser<Node>.ActivityContext activityContext = parser
				.prepareItem(mock(TNTParseableInputStream.class), data);

		ActivityFieldLocator locator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "/root/employees",
				ActivityFieldDataType.String);
		Object o = parser.resolveLocatorValue(locator, activityContext, new AtomicBoolean());

		GenericActivityParser<Node>.ActivityContext activityContext2 = parser
				.prepareItem(mock(TNTParseableInputStream.class), o);
		ActivityFieldLocator locator2 = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "/employees/*[1]",
				ActivityFieldDataType.String);
		ActivityFieldLocator locator3 = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "/root/employees/*[2]",
				ActivityFieldDataType.String);
		Object s = stackedParser.resolveLocatorValue(locator2, activityContext2, new AtomicBoolean());
		Object f = stackedParser.resolveLocatorValue(locator3, activityContext2, new AtomicBoolean());

		if (o instanceof Node && s instanceof Node && f instanceof Node) {
			assertTrue(((Node) o).getTextContent().length() > 1);
			assertTrue(((Node) o).getTextContent().length() > 1);
			assertTrue(((Node) o).getTextContent().length() > 1);
		} else {
			fail();
		}
	}
}
