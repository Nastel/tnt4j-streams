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

package com.jkoolcloud.tnt4j.streams.utils;

import java.util.*;

/**
 * A class representing a directed graph.
 *
 * @param <T>
 *            type of graph nodes
 *
 * @author Keith Schwarz (htiek@cs.stanford.edu)
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.utils.TopologicalSort
 */
public class DirectedGraph<T> implements Iterable<T> {
	/*
	 * A map from nodes in the graph to sets of outgoing edges. Each set of edges is represented by a map from edges to
	 * doubles.
	 */
	private final Map<T, Set<T>> mGraph = new LinkedHashMap<>();

	/**
	 * Adds a new node to the graph. If the node already exists, this function is a no-op.
	 *
	 * @param node
	 *            the node to add
	 * @return whether or not the node was added
	 */
	public boolean addNode(T node) {
		/* If the node already exists, don't do anything. */
		if (mGraph.containsKey(node)) {
			return false;
		}

		/* Otherwise, add the node with an empty set of outgoing edges. */
		mGraph.put(node, new LinkedHashSet<>());
		return true;
	}

	/**
	 * Given a start node, and a destination, adds an arc from the start node to the destination. If an arc already
	 * exists, this operation is a no-op. If either endpoint does not exist in the graph, throws a
	 * {@link NoSuchElementException}.
	 *
	 * @param start
	 *            the start node
	 * @param dest
	 *            the destination node
	 * @throws NoSuchElementException
	 *             if either the start or destination nodes do not exist
	 */
	public void addEdge(T start, T dest) {
		checkNodes(start, dest);

		/* Add the edge. */
		mGraph.get(start).add(dest);
	}

	/**
	 * Removes the edge from start to dest from the graph. If the edge does not exist, this operation is a no-op. If
	 * either endpoint does not exist, this throws a {@link NoSuchElementException}.
	 *
	 * @param start
	 *            the start node
	 * @param dest
	 *            the destination node
	 * @throws NoSuchElementException
	 *             if either node is not in the graph
	 */
	public void removeEdge(T start, T dest) {
		checkNodes(start, dest);

		mGraph.get(start).remove(dest);
	}

	/**
	 * Given two nodes in the graph, returns whether there is an edge from the first node to the second node. If either
	 * node does not exist in the graph, throws a {@link NoSuchElementException}.
	 *
	 * @param start
	 *            the start node
	 * @param dest
	 *            the destination node
	 * @return whether there is an edge from start to end
	 * @throws NoSuchElementException
	 *             if either endpoint does not exist
	 */
	public boolean edgeExists(T start, T dest) {
		checkNodes(start, dest);

		return mGraph.get(start).contains(dest);
	}

	private void checkNodes(T start, T dest) {
		/* Confirm both endpoints exist. */
		if (!mGraph.containsKey(start) || !mGraph.containsKey(dest)) {
			throw new NoSuchElementException(
					"Both nodes must be defined in the graph: start=" + start + ", dest=" + dest); // NON-NLS
		}
	}

	/**
	 * Given a node in the graph, returns an immutable view of the edges leaving that node as a set of endpoints.
	 *
	 * @param node
	 *            the node whose edges should be queried
	 * @return an immutable view of the edges leaving that node
	 * @throws NoSuchElementException
	 *             if the node does not exist
	 */
	public Set<T> edgesFrom(T node) {
		/* Check that the node exists. */
		Set<T> arcs = mGraph.get(node);
		if (arcs == null) {
			throw new NoSuchElementException("Source node does not exist: " + node); // NON-NLS
		}

		return Collections.unmodifiableSet(arcs);
	}

	/**
	 * Returns an iterator that can traverse the nodes in the graph.
	 *
	 * @return an iterator that traverses the nodes in the graph
	 */
	@Override
	public Iterator<T> iterator() {
		return mGraph.keySet().iterator();
	}

	/**
	 * Returns the number of nodes in the graph.
	 *
	 * @return the number of nodes in the graph
	 */
	public int size() {
		return mGraph.size();
	}

	/**
	 * Returns whether the graph is empty.
	 *
	 * @return whether the graph is empty
	 */
	public boolean isEmpty() {
		return mGraph.isEmpty();
	}
}
