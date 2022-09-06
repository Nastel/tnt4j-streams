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

package com.jkoolcloud.tnt4j.streams.utils;

import java.util.*;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

import org.apache.commons.collections4.CollectionUtils;

/**
 * Implements {@link NamespaceContext}, where every namespace prefix and URI relation is resolved from internally stored
 * {@link Map}.
 *
 * @version $Revision: 1 $
 */
public final class NamespaceMap implements NamespaceContext {
	private static final List<String> EMPTY_NS_LIST = Collections.singletonList(XMLConstants.DEFAULT_NS_PREFIX);

	private final Map<String, String> mapNS = new HashMap<>(10);
	private final Map<String, Set<String>> mapURI = new HashMap<>(10);

	/**
	 * Constructs a new namespaces map.
	 */
	public NamespaceMap() {
	}

	/**
	 * Sets mapping of namespace prefix to namespace URI.
	 *
	 * @param prefix
	 *            prefix to put into mapping
	 * @param uri
	 *            uri to put into mapping
	 */
	public String setPrefixUriMapping(String prefix, String uri) {
		mapURI(prefix, uri);
		return mapNS.put(prefix, uri);
	}

	/**
	 * Adds mapping of namespace prefix to namespace URI. If mapping already exists, does nothing.
	 *
	 * @param prefix
	 *            prefix to put into mapping
	 * @param uri
	 *            uri to put into mapping
	 * @return {@code true} if mapping was added, {@code false} - otherwise
	 */
	public boolean addPrefixUriMapping(String prefix, String uri) {
		if (!mapNS.containsKey(prefix)) {
			setPrefixUriMapping(prefix, uri);
			return true;
		}

		return false;
	}

	/**
	 * Returns mapping of namespace prefix to namespace URI.
	 *
	 * @param prefix
	 *            prefix to get mapping uri
	 * @return mapped uri, or {@code null} if there is no namespace prefix mapping defined
	 */
	public String getPrefixUriMapping(String prefix) {
		return mapNS.get(prefix);
	}

	/**
	 * Adds mappings of namespace prefix to namespace URI.
	 *
	 * @param nsMap
	 *            map containing namespace prefix to namespace URI mappings
	 */
	public void addPrefixUriMappings(Map<String, String> nsMap) {
		mapNS.putAll(nsMap);

		for (Map.Entry<String, String> nsms : nsMap.entrySet()) {
			mapURI(nsms.getKey(), nsms.getValue());
		}
	}

	private void mapURI(String prefix, String uri) {
		Set<String> prefixList = mapURI.get(uri);
		if (prefixList == null) {
			prefixList = new LinkedHashSet<>(5);
			mapURI.put(uri, prefixList);
		}
		prefixList.add(prefix);
	}

	@Override
	public String getNamespaceURI(String prefix) {
		String uri = mapNS.get(prefix);
		if (uri == null) {
			uri = XMLConstants.XML_NS_URI;
		}
		return uri;
	}

	@Override
	public String getPrefix(String namespaceURI) {
		Set<String> nsList = mapURI.get(namespaceURI);
		return CollectionUtils.isEmpty(nsList) ? XMLConstants.DEFAULT_NS_PREFIX : nsList.iterator().next();
	}

	@Override
	public Iterator<String> getPrefixes(String namespaceURI) {
		Set<String> nsList = mapURI.get(namespaceURI);
		return nsList == null ? EMPTY_NS_LIST.iterator() : nsList.iterator();
	}
}
