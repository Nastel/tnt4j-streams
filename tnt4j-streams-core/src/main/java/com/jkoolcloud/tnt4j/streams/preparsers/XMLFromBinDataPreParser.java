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

package com.jkoolcloud.tnt4j.streams.preparsers;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Stack;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.text.StringEscapeUtils;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldFormatType;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Pre-parser to convert RAW binary activity data to valid XML parseable by actual activity XML parser. It skip's any of
 * unlike XML characters and tries to construct XML DOM structure for actual parser to continue. It uses SAX handler
 * capabilities of {@link com.jkoolcloud.tnt4j.streams.preparsers.XMLFromBinDataPreParser.XMLBinSAXHandler} to parse
 * input stream data and build XML DOM document.
 * <p>
 * This preparer is also capable to make valid XML document from RAW activity data having truncated XML structures.
 * <p>
 * If resolved XML has multiple nodes in root level, to make XML valid those nodes gets surrounded by single root node
 * named {@value com.jkoolcloud.tnt4j.streams.preparsers.XMLFromBinDataPreParser.XMLBinSAXHandler#ROOT_ELEMENT}.
 * <p>
 * Default {@link Charset} used to convert between binary and string data is
 * {@link java.nio.charset.StandardCharsets#UTF_8}. Custom charset can be defined using constructor parameter
 * {@code charsetName}.
 *
 * @version $Revision: 1 $
 */
public class XMLFromBinDataPreParser extends AbstractPreParser<Object, Document> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(XMLFromBinDataPreParser.class);

	private ActivityFieldFormatType format;
	private Charset charset = StandardCharsets.UTF_8;

	/**
	 * Constructs a new XMLFromBinDataPreParser.
	 *
	 * @throws ParserConfigurationException
	 *             if initialization of SAX parser fails
	 */
	public XMLFromBinDataPreParser() throws ParserConfigurationException {
		this(ActivityFieldFormatType.bytes);
	}

	/**
	 * Constructs a new XMLFromBinDataPreParser.
	 *
	 * @param format
	 *            RAW activity data format name from {@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldFormatType}
	 *            enumeration
	 *
	 * @throws ParserConfigurationException
	 *             if initialization of SAX parser fails
	 */
	public XMLFromBinDataPreParser(String format) throws ParserConfigurationException {
		this(ActivityFieldFormatType.valueOf(format));
	}

	/**
	 * Constructs a new XMLFromBinDataPreParser.
	 *
	 * @param format
	 *            RAW activity data format
	 *
	 * @throws ParserConfigurationException
	 *             if initialization of SAX parser fails
	 */
	public XMLFromBinDataPreParser(ActivityFieldFormatType format) throws ParserConfigurationException {
		this.format = format;
	}

	/**
	 * Constructs a new XMLFromBinDataPreParser.
	 *
	 * @param format
	 *            RAW activity data format name from {@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldFormatType}
	 *            enumeration
	 * @param charsetName
	 *            RAW activity data charset
	 *
	 * @throws ParserConfigurationException
	 *             if initialization of SAX parser fails
	 */
	public XMLFromBinDataPreParser(String format, String charsetName) throws ParserConfigurationException {
		this(ActivityFieldFormatType.valueOf(format), charsetName);
	}

	/**
	 * Constructs a new XMLFromBinDataPreParser.
	 *
	 * @param format
	 *            RAW activity data format
	 * @param charsetName
	 *            RAW activity data charset
	 *
	 * @throws ParserConfigurationException
	 *             if initialization of SAX parser fails
	 */
	public XMLFromBinDataPreParser(ActivityFieldFormatType format, String charsetName)
			throws ParserConfigurationException {
		this.format = format;
		this.charset = Charset.forName(charsetName);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Parsing input stream to prepare XML {@link Document} The method checks for acceptable input source and rethrows
	 * {@link ParseException} if something fails.
	 */
	@Override
	public Document preParse(Object data) throws ParseException {
		XMLBinSAXHandler documentHandler = XMLBinSAXHandler.initNewDocument();
		if (documentHandler == null) {
			throw new ParseException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"XMLFromBinDataPreParser.doc.init.failure"), 0);
		}
		InputStream is;
		boolean closeWhenDone = false;
		if (data instanceof String) {
			String dStr;
			switch (format) {
			case base64Binary:
				dStr = Utils.base64Decode((String) data, charset);
				break;
			case hexBinary:
				dStr = Utils.getString(Utils.decodeHex((String) data), charset);
				break;
			case bytes:
			case string:
			default:
				dStr = (String) data;
				break;
			}
			is = new ByteArrayInputStream(dStr.getBytes(charset));
			closeWhenDone = true;
		} else if (data instanceof byte[]) {
			is = new ByteArrayInputStream((byte[]) data);
			closeWhenDone = true;
		} else if (data instanceof ByteBuffer) {
			is = new ByteArrayInputStream(((ByteBuffer) data).array());
			closeWhenDone = true;
		} else if (data instanceof Reader) {
			try {
				Reader reader = (Reader) data;
				is = ReaderInputStream.builder().setReader(reader).setCharset(charset).get();
			} catch (Exception exc) {
				ParseException pe = new ParseException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"XMLFromBinDataPreParser.reader.init.failure"), 0);
				pe.initCause(exc);

				throw pe;
			}
		} else if (data instanceof InputStream) {
			is = (InputStream) data;
		} else {
			throw new ParseException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"XMLFromBinDataPreParser.input.unsupported"), 0);
		}

		if (!is.markSupported()) {
			try {
				byte[] copy = IOUtils.toByteArray(is); // Read all and make a copy, because stream does not support
				// rewind.
				is = new ByteArrayInputStream(copy);
				closeWhenDone = true;
			} catch (IOException e) {
				throw new ParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"XMLFromBinDataPreParser.data.read.failed", Utils.getExceptionMessages(e)), 0);
			}
		}

		try {
			documentHandler.parseBinInput(is, charset);
		} catch (IOException e) {
			ParseException pe = new ParseException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"XMLFromBinDataPreParser.bin.data.parse.failure"), 0);
			pe.initCause(e);

			throw pe;
		} finally {
			if (closeWhenDone) {
				Utils.close(is);
			}
		}

		return documentHandler.getDOM();
	}

	@Override
	public String dataTypeReturned() {
		return "XML"; // NON-NLS
	}

	/**
	 * SAX handler implementation allowing to make XML DOM document from provided binary data containing fragments of
	 * XML. It implements {@link ErrorHandler} interface to catch parsing error, on such event the parser skip's a
	 * character and continues to parse element.
	 * <p>
	 * This SAX handler is also capable to make valid XML document from RAW activity data having truncated XML
	 * structures.
	 */
	protected static class XMLBinSAXHandler extends DefaultHandler
			implements ContentHandler, LexicalHandler, ErrorHandler {
		private static final String XML_PREFIX = "<?xml version='1.0' encoding='UTF-8'?>\n"; // NON-NLS
		/**
		 * Constant for surrounding root level node name.
		 */
		public static final String ROOT_ELEMENT = "root"; // NON-NLS

		private Locator locator;

		private Node root = null;
		private Document document = null;
		private Stack<Node> _nodeStk = new Stack<>();

		private SAXParser parser;

		private int bPos;
		private Position errorPosition = new Position();
		private int absoluteLastGoodPosition;

		private Integer startSkip = null;
		private Integer lastSkip = null;

		private XMLBinSAXHandler() throws ParserConfigurationException {
			try {
				SAXParserFactory parserFactory = SAXParserFactory.newInstance();
				parser = parserFactory.newSAXParser();
				parser.getXMLReader().setErrorHandler(this);
			} catch (Exception e) {
				throw new ParserConfigurationException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"XMLFromBinDataPreParser.init.failure"));
			}
		}

		/**
		 * Init new document xml from bin data pre parser SAX handler.
		 *
		 * @return the xml from bin data pre parser SAX handler
		 */
		public static XMLBinSAXHandler initNewDocument() {
			try {
				XMLBinSAXHandler documentHandler = new XMLBinSAXHandler();
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				documentHandler.document = factory.newDocumentBuilder().newDocument();
				documentHandler.root = documentHandler.document.createElement(ROOT_ELEMENT);
				documentHandler._nodeStk.push(documentHandler.root);
				documentHandler.bPos = 0;
				return documentHandler;
			} catch (ParserConfigurationException e1) {
				return null;
				// can't initialize
			}
		}

		/**
		 * Actual RAW data parsing method. Field {@code bPos} represents actual position in source data. On the SAX
		 * exception {@code bPos} adjusted to error position and the parser continues from there until the end of
		 * stream.
		 *
		 * @param is
		 *            input stream to read
		 * @param charset
		 *            input stream charset
		 * @throws IOException
		 *             if I/O exception occurs while reading input stream
		 */
		public void parseBinInput(InputStream is, Charset charset) throws IOException {
			InputStream prefixIS = new ByteArrayInputStream(XML_PREFIX.getBytes(charset));
			SequenceInputStream sIS = new SequenceInputStream(Collections.enumeration(Arrays.asList(prefixIS, is)));
			InputSource parserInputSource = new InputSource(sIS);
			parserInputSource.setEncoding(charset.name());

			while (bPos <= is.available()) {
				try {
					is.skip(bPos > absoluteLastGoodPosition ? bPos : absoluteLastGoodPosition);
				} catch (IOException e) {
					Utils.logThrowable(LOGGER, OpLevel.ERROR,
							StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"XMLFromBinDataPreParser.skip.failed", e);
					return;
				}
				try {
					errorPosition.reset();
					parser.parse(parserInputSource, this);
				} catch (Exception e) {
					if (errorPosition.column < (absoluteLastGoodPosition - bPos)) {
						bPos += errorPosition.column;
					} else {
						bPos++;
					}

					prefixIS.reset();
					is.reset();

					if (bPos >= is.available()) {
						handleTrailingText(is, charset);
						break;
					}
				}
			}

			if (startSkip != null) {
				logSkipRange();
			}

			Utils.close(prefixIS);
		}

		private void logSkipRange() {
			int sIdx = absoluteLastGoodPosition == 0 ? 1 : absoluteLastGoodPosition;
			int eIdx = absoluteLastGoodPosition == 0 ? bPos + 1 : bPos;
			int sLength = eIdx - sIdx + 1; // NOTE: inclusive range
			LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"XMLFromBinDataPreParser.skipping", sIdx, eIdx, sLength);
		}

		/**
		 * In case of incomplete XML data, preserves last chunk of characters as last element text data.
		 *
		 * @param is
		 *            input stream to read
		 * @param charset
		 *            input stream charset
		 * @throws IOException
		 *             if I/O exception occurs while reading input stream
		 */
		protected void handleTrailingText(InputStream is, Charset charset) throws IOException {
			int lPos = absoluteLastGoodPosition - 1;
			int textLength = bPos - lPos;

			if (textLength > 0) {
				byte[] buffer = new byte[textLength];
				try {
					is.skip(lPos);
					is.read(buffer);
					String text = Utils.getString(buffer, charset);
					characters(text.toCharArray(), 0, text.length());
				} finally {
					is.reset();
				}
			}
		}

		/**
		 * Returns DOM document made from binary data.
		 *
		 * @return DOM document made from binary data
		 */
		public Document getDOM() {
			if (root.getChildNodes().getLength() == 1) {
				document.appendChild(root.getFirstChild());
			} else {
				document.appendChild(root);
			}

			return document;
		}

		@Override
		public void characters(char[] ch, int start, int length) {
			if (!_nodeStk.isEmpty()) {
				Node last = _nodeStk.peek();
				// No text nodes can be children of root (DOM006 exception)
				if (last != document) {
					String text = new String(ch, start, length);
					last.appendChild(document.createTextNode(StringEscapeUtils.escapeXml11(text)));
				}
			}
		}

		@Override
		public void startDocument() {
		}

		@Override
		public void endDocument() {
			if (!_nodeStk.isEmpty()) {
				_nodeStk.pop();
			}
		}

		@Override
		public void startElement(String namespace, String localName, String qName, Attributes attrs) {
			Element tmp = document.createElementNS(namespace, qName);

			// Add attributes to element
			int attrsCount = attrs.getLength();
			for (int i = 0; i < attrsCount; i++) {
				if (attrs.getLocalName(i).startsWith(XMLConstants.XMLNS_ATTRIBUTE)) {
					tmp.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, attrs.getQName(i), attrs.getValue(i));
				} else if (attrs.getLocalName(i) == null) {
					tmp.setAttribute(attrs.getQName(i), attrs.getValue(i));
				} else {
					tmp.setAttributeNS(attrs.getURI(i), attrs.getQName(i), attrs.getValue(i));
				}
			}

			// Append this new node into current stack node
			if (!_nodeStk.isEmpty()) {
				Node last = _nodeStk.peek();
				last.appendChild(tmp);
			}

			markLastGoodPosition();

			// Push this node into stack
			LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"XMLFromBinDataPreParser.found.element", qName);
			_nodeStk.push(tmp);
		}

		@Override
		public void endElement(String namespace, String localName, String qName) {
			if (!_nodeStk.isEmpty()) {
				_nodeStk.pop();
			}
			markLastGoodPosition();
		}

		private void markLastGoodPosition() {
			if (lastSkip != null && startSkip != null) {
				logSkipRange();
			}

			absoluteLastGoodPosition = bPos + locator.getColumnNumber();

			startSkip = null;
			lastSkip = null;
		}

		/**
		 * Adds processing instruction node to DOM.
		 */
		@Override
		public void processingInstruction(String target, String data) {
			if (!_nodeStk.isEmpty()) {
				Node last = _nodeStk.peek();
				ProcessingInstruction pi = document.createProcessingInstruction(target, data);
				if (pi != null) {
					last.appendChild(pi);
				}
			}
		}

		/**
		 * This class is only used internally so this method should never be called.
		 */
		@Override
		public void ignorableWhitespace(char[] ch, int start, int length) {
		}

		/**
		 * This class is only used internally so this method should never be called.
		 */
		@Override
		public void setDocumentLocator(Locator locator) {
			this.locator = locator;
		}

		/**
		 * This class is only used internally so this method should never be called.
		 */
		@Override
		public void skippedEntity(String name) {
		}

		/**
		 * {@inheritDoc}
		 *
		 * Lexical handler method to create comment node in DOM tree.
		 */
		@Override
		public void comment(char[] ch, int start, int length) {
			if (!_nodeStk.isEmpty()) {
				Node last = _nodeStk.peek();
				Comment comment = document.createComment(new String(ch, start, length));
				if (comment != null) {
					last.appendChild(comment);
				}
			}
		}

		@Override
		public void warning(SAXParseException e) throws SAXException {
			error(e);
		}

		@Override
		public void error(SAXParseException e) throws SAXException {
			errorPosition.line = e.getLineNumber();
			errorPosition.column = e.getColumnNumber();
			if (startSkip == null) {
				startSkip = bPos;
			}
			lastSkip = bPos;

		}

		@Override
		public void fatalError(SAXParseException e) throws SAXException {
			error(e);
		}

		// Lexical Handler methods - not required.
		@Override
		public void startCDATA() {
		}

		@Override
		public void endCDATA() {
		}

		@Override
		public void startEntity(java.lang.String name) {
		}

		@Override
		public void endEntity(String name) {
		}

		@Override
		public void startDTD(String name, String publicId, String systemId) throws SAXException {
		}

		@Override
		public void endDTD() {
		}

		@Override
		public void startPrefixMapping(String prefix, String uri) throws SAXException {
		}

		@Override
		public void endPrefixMapping(String prefix) {
		}

		private static class Position {
			@SuppressWarnings("unused")
			private int line;
			private int column;

			void reset() {
				line = 1;
				column = 1;
			}
		}
	}
}