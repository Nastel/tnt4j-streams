package com.jkoolcloud.tnt4j.streams.parsers;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import io.burt.jmespath.Expression;
import io.burt.jmespath.jackson.JacksonRuntime;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
//import com.jayway.jsonpath.*;
//import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
//import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldDataType;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

import io.burt.jmespath.JmesPath;
import io.burt.jmespath.RuntimeConfiguration;

/**
 * Implements an activity data parser that assumes each activity data item is an JSON format string. JSON parsing is
 * performed using JMESPath API over the Jackson deserialization framework under the hood. Activity fields
 * locator values are treated as JsonPath expressions.
 * <p>
 * See <a href="https://github.com/jayway/JsonPath">JsonPath API</a> for more details.
 * <p>
 * This parser supports the following configuration properties (in addition to those supported by
 * {@link GenericActivityParser}):
 * <ul>
 * <li>ReadLines - indicates that complete JSON data package is single line. Default value - '{@code true}'. (Optional,
 * deprecated - use 'ActivityDelim' instead)</li>
 * <li>List of DeserializationFeature.[FEATURE_NAME] - defines set of Jackson Object Mapper's deserialization
 * configuration features. See {@link com.fasterxml.jackson.databind.DeserializationFeature} for more details.
 * (Optional)</li>
 * <li>List of MapperFeature.[FEATURE_NAME] - defines set of Jackson Object Mapper's mapping configuration features. See
 * {@link com.fasterxml.jackson.databind.MapperFeature} for more details. (Optional)</li>
 * <li>List of JsonParser.[FEATURE_NAME] - defines set of Jackson Object Mapper's parser configuration features. See
 * {@link JsonParser.Feature} for more details. (Optional)</li>
 * <li>List of Option.[OPTION_NAME] - defines set of JsonPath configuration options. See
 * {@link com.jayway.jsonpath.Option} for more details. (Optional)</li>
 * </ul>
 * <p>
 * This activity parser supports those activity field locator types:
 * <ul>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Label}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#StreamProp}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Cache}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Activity}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Expression}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#ParserProp}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#SystemProp}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#EnvVariable}</li>
 * </ul>
 *
 * @version $Revision: 2 $
 */
public class ActivityJMESParser extends GenericActivityParser<JsonNode> {
    private static final EventSink LOGGER = LoggerUtils.getLoggerSink(ActivityJMESParser.class);

    private static final String JSON_PATH_ROOT = "$";// NON-NLS
    private static final String JSON_PATH_SEPARATOR = StreamsConstants.DEFAULT_PATH_DELIM;

    private static final String DESERIALIZATION_FEATURE = "DeserializationFeature."; // NON-NLS
    private static final String MAPPER_FEATURE = "MapperFeature."; // NON-NLS
    private static final String PARSER_FEATURE = "JsonParser."; // NON-NLS
    private static final String OPTION = "Option."; // NON-NLS
    private static final String[] PARSER_CFG_TOKENS = new String[] { DESERIALIZATION_FEATURE, MAPPER_FEATURE,
            PARSER_FEATURE, OPTION };

    private Map<String, String> parseProperties = new LinkedHashMap<>();
    private RuntimeConfiguration parseConfiguration;

    private JacksonRuntime runtime;

    /**
     * Constructs a new ActivityJMESParser.
     */
    public ActivityJMESParser() {
        super(ActivityFieldDataType.AsInput);
    }

    @Override
    protected EventSink logger() {
        return LOGGER;
    }

    @Override
    public void setProperties(Collection<Map.Entry<String, String>> props) {
        super.setProperties(props);

        ObjectMapper mapper = new ObjectMapper();
        if (parseProperties.isEmpty()) {
            parseConfiguration = RuntimeConfiguration.defaultConfiguration();
        } else {
            for (Map.Entry<String, String> pProp : parseProperties.entrySet()) {
                if (pProp.getKey().startsWith(DESERIALIZATION_FEATURE)) {
                    DeserializationFeature df = DeserializationFeature
                            .valueOf(pProp.getKey().substring(DESERIALIZATION_FEATURE.length()));
                    mapper.configure(df, Utils.toBoolean(pProp.getValue()));
                } else if (pProp.getKey().startsWith(MAPPER_FEATURE)) {
                    MapperFeature mf = MapperFeature.valueOf(pProp.getKey().substring(MAPPER_FEATURE.length()));
                    mapper.configure(mf, Utils.toBoolean(pProp.getValue()));
                } else if (pProp.getKey().startsWith(PARSER_FEATURE)) {
                    JsonParser.Feature pf = JsonParser.Feature
                            .valueOf(pProp.getKey().substring(PARSER_FEATURE.length()));
                    mapper.configure(pf, Utils.toBoolean(pProp.getValue()));
                }
            }

            parseConfiguration = RuntimeConfiguration.builder() //
                    .withSilentTypeErrors(false) //
                    .build();
        }

        runtime = new JacksonRuntime(parseConfiguration, mapper);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setProperty(String name, String value) {
        super.setProperty(name, value);

        if (ParserProperties.PROP_READ_LINES.equalsIgnoreCase(name)) {
            activityDelim = Utils.toBoolean(value) ? ActivityDelim.EOL.name() : ActivityDelim.EOF.name();

            logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
                    "ActivityParser.setting", name, value);
        } else if (StringUtils.startsWithAny(name, PARSER_CFG_TOKENS)) {
            parseProperties.put(name, value);

            logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
                    "ActivityParser.setting", name, value);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public Object getProperty(String name) {
        if (ParserProperties.PROP_READ_LINES.equalsIgnoreCase(name)) {
            return activityDelim;
        }

        Object pValue = super.getProperty(name);
        if (pValue != null) {
            return pValue;
        }

        return parseProperties.get(name);
    }

    /**
     * Returns whether this parser supports the given format of the activity data. This is used by activity streams to
     * determine if the parser can parse the data in the format that the stream has it.
     * <p>
     * This parser supports the following class types (and all classes extending/implementing any of these):
     * <ul>
     * <li>{@link java.lang.String}</li>
     * <li>{@code byte[]}</li>
     * <li>{@link java.nio.ByteBuffer}</li>
     * <li>{@link java.io.Reader}</li>
     * <li>{@link java.io.InputStream}</li>
     * </ul>
     *
     * @param data
     *            data object whose class is to be verified
     * @return {@code true} if this parser can process data in the specified format, {@code false} - otherwise
     */
    @Override
    protected boolean isDataClassSupportedByParser(Object data) {
        return data instanceof JsonNode || super.isDataClassSupportedByParser(data);
    }

    @Override
    public boolean canHaveDelimitedLocators() {
        return false;
    }

    @Override
    protected ActivityContext prepareItem(TNTInputStream<?, ?> stream, Object data) throws ParseException {
        JsonNode jsonDoc;
        String jsonString = null;
        try {
            if (data instanceof JsonNode) {
                jsonDoc = (JsonNode) data;
            } else if (data instanceof InputStream) {
                jsonString = getNextActivityString(data);
                jsonDoc = runtime.parseString(jsonString);
            } else {
                jsonString = getNextActivityString(data);
                if (StringUtils.isEmpty(jsonString)) {
                    return null;
                }
                jsonDoc = runtime.parseString(jsonString);
            }
        } catch (Exception e) {
            ParseException pe = new ParseException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
                    "ActivityJMESParser.jsonDocument.parse.error"), 0);
            pe.initCause(e);

            throw pe;
        }

        if (jsonString == null) {
            jsonString = jsonDoc.toString();
        }

        ActivityContext cData = new ActivityContext(stream, data, jsonDoc).setParser(this);
        cData.setMessage(jsonString);

        return cData;
    }

    /**
     * Reads RAW activity data JSON package string from {@link BufferedReader}.
     *
     * @param rdr
     *            reader to use for reading
     * @return non empty RAW activity data JSON package string, or {@code null} if the end of the stream has been
     *         reached
     */
    @Override
    protected String readNextActivity(BufferedReader rdr) {
        StringBuilder jsonStringBuilder = new StringBuilder(1024);
        String line;

        nextLock.lock();
        try {
            try {
                while ((line = rdr.readLine()) != null) {
                    jsonStringBuilder.append(line);
                    if (ActivityDelim.EOL.name().equals(activityDelim)) {
                        break;
                    }
                }
            } catch (EOFException eof) {
                Utils.logThrowable(logger(), OpLevel.DEBUG,
                        StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME), "ActivityParser.data.end",
                        getActivityDataType()[0], eof);
            } catch (IOException ioe) {
                Utils.logThrowable(logger(), OpLevel.WARNING,
                        StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
                        "ActivityParser.error.reading", getActivityDataType()[0], ioe);
            }
        } finally {
            nextLock.unlock();
        }

        return jsonStringBuilder.toString();
    }

    private static final String[] ACTIVITY_DATA_TYPES = { "JSON", "TEXT" }; // NON-NLS

    /**
     * Returns types of RAW activity data entries.
     *
     * @return types of RAW activity data entries - {@code "JSON"} and {@code "TEXT"}
     */
    @Override
    protected String[] getActivityDataType() {
        return ACTIVITY_DATA_TYPES;
    }

    /**
     * Gets field raw data value resolved by locator and formats it according locator definition.
     *
     * @param locator
     *            activity field locator
     * @param cData
     *            {@link com.jayway.jsonpath.JsonPath} document context to read
     * @param formattingNeeded
     *            flag to set if value formatting is not needed
     * @return value formatted based on locator definition or {@code null} if locator is not defined
     *
     * @throws ParseException
     *             if exception occurs while resolving raw data value or applying locator format properties to specified
     *             value
     *
     * @see ActivityFieldLocator#formatValue(Object)
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Object resolveLocatorValue(ActivityFieldLocator locator, ActivityContext cData,
                                         AtomicBoolean formattingNeeded) throws ParseException {
        Object val = null;
        String locStr = locator.getLocator();

        if (StringUtils.isNotEmpty(locStr)) {
            Object jsonValue;
            try {
                Expression<JsonNode> exp = runtime.compile(locStr);
                JsonNode jsonNode = cData.getData();
                jsonValue = exp.search(jsonNode);
            } catch (Exception exc) {
                jsonValue = null;
            }

            if (jsonValue != null) {
                if (jsonValue instanceof List) {
                    List<Object> jsonValuesList = (List<Object>) jsonValue;
                    List<Object> valuesList = new ArrayList<>(jsonValuesList.size());
                    for (Object jsonValues : jsonValuesList) {
                        valuesList.add(locator.formatValue(jsonValues));
                    }
                    val = valuesList;
                } else {
                    val = locator.formatValue(jsonValue);
                }
                formattingNeeded.set(false);
            }
        }

        return val;
    }

    @SuppressWarnings("deprecation")
    private static final EnumSet<ActivityFieldLocatorType> UNSUPPORTED_LOCATOR_TYPES = EnumSet
            .of(ActivityFieldLocatorType.Index, ActivityFieldLocatorType.Range, ActivityFieldLocatorType.REMatchId);

    /**
     * {@inheritDoc}
     * <p>
     * Unsupported activity locator types are:
     * <ul>
     * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Index}</li>
     * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Range}</li>
     * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#REMatchId}</li>
     * </ul>
     */
    @Override
    protected EnumSet<ActivityFieldLocatorType> getUnsupportedLocatorTypes() {
        return UNSUPPORTED_LOCATOR_TYPES;
    }
}

