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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;

/**
 * RAW activity data pre-parser capable to deserialize incoming activity data from protobuf message
 * {@link com.google.protobuf.Message} to string keyed map.
 * 
 * @version $Revision: 1 $
 */
public class ProtoMessageToMapPreParser extends AbstractPreParser<Message, Map<String, ?>> {

	/**
	 * {@inheritDoc}
	 * <p>
	 * This pre-parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link Message}</li>
	 * </ul>
	 */
	@Override
	public boolean isDataClassSupported(Object data) {
		return data instanceof Message;
	}

	@Override
	public Map<String, ?> preParse(Message data) throws Exception {
		Map<Descriptors.FieldDescriptor, Object> allFields = data.getAllFields();
		Map<String, Object> msgMap = new LinkedHashMap<>();
		for (Map.Entry<Descriptors.FieldDescriptor, Object> field : allFields.entrySet()) {
			Descriptors.FieldDescriptor fieldDescriptor = field.getKey();
			Object fieldValue = field.getValue();
			Object simpleFieldValue = simplifyValue(data, fieldDescriptor, fieldValue);
			if (simpleFieldValue != null) {
				String fieldName = fieldDescriptor.getName();
				msgMap.put(fieldName, simpleFieldValue);
			}
		}
		return msgMap;
	}

	/**
	 * Converts complex field value type to more simple (atomic) type. In case field value is some protobuf message, map
	 * containing fields for that message is returned. If field value is repeating, list of values is returned.
	 * 
	 * @param message
	 *            protobuf message to use for value simplification
	 * @param fieldDescriptor
	 *            field descriptor
	 * @param fieldValue
	 *            field value
	 * @return field value converted to simple type value, map if field value is some protobuf message or list of values
	 *         if field is repeating
	 * 
	 * @throws Exception
	 *             if field value is some protobuf message and failure occurs while parsing it
	 * 
	 * @see #convertAtomicVal(com.google.protobuf.Descriptors.FieldDescriptor, Object)
	 */
	protected Object simplifyValue(Message message, Descriptors.FieldDescriptor fieldDescriptor, Object fieldValue)
			throws Exception {
		Object result = null;
		if (fieldValue != null) {
			if (fieldDescriptor.isRepeated()) {
				if (message.getRepeatedFieldCount(fieldDescriptor) > 0) {
					List<?> originals = (List<?>) fieldValue;
					List<Object> copies = new ArrayList<>(originals.size());
					for (Object original : originals) {
						copies.add(convertAtomicVal(fieldDescriptor, original));
					}
					result = copies;
				}
			} else {
				result = convertAtomicVal(fieldDescriptor, fieldValue);
			}
		}
		return result;
	}

	/**
	 * Converts complex field value type to more simple (atomic) type. In case field value is some protobuf message, map
	 * containing fields for that message is returned.
	 * 
	 * @param fieldDescriptor
	 *            field descriptor
	 * @param fieldValue
	 *            field value
	 * @return field value converted to simple type value or map if field value is some protobuf message
	 * 
	 * @throws Exception
	 *             if field value is some protobuf message and failure occurs while parsing it
	 */
	protected Object convertAtomicVal(Descriptors.FieldDescriptor fieldDescriptor, Object fieldValue) throws Exception {
		Object result = null;
		if (fieldValue != null) {
			switch (fieldDescriptor.getJavaType()) {
			case INT:
			case LONG:
			case FLOAT:
			case DOUBLE:
			case BOOLEAN:
			case STRING:
				result = fieldValue;
				break;
			case BYTE_STRING:
				result = ((ByteString) fieldValue).toByteArray();
				break;
			case ENUM:
				result = fieldValue.toString();
				break;
			case MESSAGE:
				result = preParse((Message) fieldValue);
				break;
			}
		}
		return result;
	}

	@Override
	public String dataTypeReturned() {
		return "MAP"; // NON-NLS
	}
}
