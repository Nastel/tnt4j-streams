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

package com.jkoolcloud.tnt4j.streams.matchers;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;

import com.jkoolcloud.tnt4j.streams.utils.StreamsXMLUtils;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Data string or {@link org.w3c.dom.Node} value match expression evaluation based on {@link javax.xml.xpath.XPath}
 * expressions.
 *
 * @version $Revision: 1 $
 */
public class XPathMatcher implements Matcher {

	private XPath xPath;
	private DocumentBuilder builder;

	private final Lock xPathLock = new ReentrantLock();
	private final Lock builderLock = new ReentrantLock();

	private static XPathMatcher instance;

	private XPathMatcher() throws Exception {
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setNamespaceAware(false);
		builder = domFactory.newDocumentBuilder();
		xPath = StreamsXMLUtils.getStreamsXPath();
	}

	static synchronized XPathMatcher getInstance() throws Exception {
		if (instance == null) {
			instance = new XPathMatcher();
		}

		return instance;
	}

	@Override
	public boolean isDataClassSupported(Object data) {
		return data instanceof String || data instanceof Node;
	}

	/**
	 * Evaluates match {@code expression} against provided {@code data} using XPath.
	 *
	 * @param expression
	 *            XPath expression to check
	 * @param data
	 *            data {@link String} or {@link org.w3c.dom.Node} to evaluate expression to
	 * @return true if expression returns any result
	 */
	@Override
	public boolean evaluate(String expression, Object data) throws Exception {
		Node xmlDoc;
		if (data instanceof Node) {
			xmlDoc = (Node) data;
		} else {
			String xmlString = Utils.toString(data);
			if (StringUtils.isEmpty(xmlString)) {
				return false;
			}
			builderLock.lock();
			try {
				xmlDoc = builder.parse(IOUtils.toInputStream(xmlString, StandardCharsets.UTF_8));
			} finally {
				builderLock.unlock();
			}
		}
		xPathLock.lock();
		try {
			String expressionResult = xPath.evaluate(expression, xmlDoc);

			if (StringUtils.equalsAnyIgnoreCase(expressionResult, "true", "false")) { // NON-NLS
				return Boolean.parseBoolean(expressionResult);
			}

			return StringUtils.isNotEmpty(expressionResult);
		} finally {
			xPathLock.unlock();
		}
	}
}
