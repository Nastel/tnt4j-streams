# TNT4J-Streams
TNT4J Streams allows data streaming, parsing from various data sources into TNT4J event sinks.

Why TNT4J-Streams
======================================

* TNT4J-Streams can be run out of the box for a large set of data streaming without writing no additional code.
All you need is to define your data format mapping to TNT4J event mapping in TNT4J-Streams configuration.

* Supports the following data sources:
    * File
    * Characters/bytes feed stream from file or over TCP/IP
    * HDFS
    * MQTT
    * HTTP
    * JMS
    * Apache Kafka (as Consumer and as Server/Consumer)
    * Apache Flume
    * Logstash
    * WMQ (IBM MQ)
    * OS pipes
    * Zipped files (also from HDFS)
    * Standard Java InputStream/Reader
    * JAX-RS service (JSON/XML)
    * JAX-WS service
    * System command
    * MS Excel document
    * Elastic Beats
    * FileSystem (JSR-203 compliant) provided files (accessing remote files over SCP/SSH, SFTP, etc.)
    * JDBC

* Files (including provided by HDFS and JSR-203 FileSystem) can be streamed:
    * as "whole at once" - when a stream starts, it reads file contents line by line meaning a single file line holds data of a single 
    activity event. After file reading completes - the stream stops.
    * using file polling - when some application uses file to write data at runtime, stream waits for file changes. When file changes, 
    changed (appended) lines are read by stream and interpreted as single line is single activity event. Stream stops only when application 
    gets terminated or some critical runtime error occurs.

* Customized parser for Apache Access Logs.

* Customized IBM MQ Trace Events stream (and parser).
* Customized IBM MQ Error log parser (also supports JSON formatted logs).

* It can be integrated with:
    * Logstash
    * Apache Flume
    * Angulartics
    * AJAX
    * Node.js
    * Collectd
    * Nagios
    * Fluentd
    * Elastic Beats

    just by applying configuration and without additional coding.

* Redirect streamed data from different TNT4J based producer APIs like `tnt4j-stream-*` - to be TNT4J based streams concentrator.
* Run TNT4J-Streams as system daemon service.

Importing TNT4J-Streams project into IDE
======================================

## Eclipse
* Select File->Import...->Maven->Existing Maven Projects
* Click 'Next'
* In 'Root directory' field select path of directory where you have downloaded (checked out from git)
TNT4J-Streams project
* Click 'OK'
* Dialog fills in with project modules details
* Click 'Finish'

Running TNT4J-Streams
======================================

## Running TNT4J-Streams
* As standalone application
    * write streams configuration file. See ['Streams configuration'](#streams-configuration) chapter for more details
    * configure your loggers
    * use `bin/tnt4j-streams.bat` or `bin/tnt4j-streams.sh` to run standalone application

     **NOTE:** in case you are using Java 9 as your runtime JVM and getting `java.lang.NoClassDefFoundError: javax/xml/bind/JAXBException`, 
     add `java` command parameter `--add-modules java.xml.bind` to add JAXB classes to java classpath.
* As API integrated into your product
    * Write streams configuration file. See ['Streams configuration'](#streams-configuration) chapter for more details
    * use `StreamsAgent.runFromAPI(configFileName)` in your code
* As system daemon service. See ['TNT4J-Streams as System Service configuration'](./bin/service/readmeServices.md) for details how to run 
TNT4J-Streams as your system service.

## TNT4J Events field mappings

Mapping of streamed data to activity event fields are performed by parser. To map field value you have to define
`field` tag in parser configuration:
* attributes:
    * `name` - defines activity event field name
    * `locator` - defines location of data value from streamed data
    * `locator-type` - defines type of attribute `locator`. Set of supported values:
        * `StreamProp` 
        * `Index` 
        * `Label` 
        * `Range` 
        * `Cache` 
        * `Activity`
    * `format` - defines format type or representation that value is expected to be in (e.g. binary, date/time string, decimal format, etc.).
    Set of supported values:
        * `base64Binary`
        * `hexBinary` 
        * `string`
        * any decimal or time format pattern, i.e. `#####0.000`
        * one of number type enumerators: `integer`/`int`, `long`, `double`, `float`, `short`, `byte`, `biginteger`/`bigint`/`bint`, 
        `bigdecimal`/`bigdec`/`bdec` and `any`. `any` will resolve any possible numeric value out of provided string, e.g. string `"30hj00"` 
        will result value `30`.
    * `value` - defines predefined (hardcoded) value of field
    * `datatype` - defines how to interpret field resolved value. Set of supported values:
        * `String` 
        * `Binary`
        * `Number`
        * `DateTime`
        * `Timestamp`
        * `Generic`
        * `AsInput`
    * `radix` - for numeric values, defines the radix that the value is specified in (ignored if format is specified)
    * `units` - defines the units of value to be represented. Set of supported values:
        * `Days`
        * `Hours`
        * `Minutes`
        * `Seconds`
        * `Milliseconds`
        * `Microseconds`
        * `Nanoseconds` 
    * `timezone` - defines the time zone that a date/time string is represented in (when not specified, date/time string is assumed to be in 
    local time zone)
    * `locale` - defines locale for data formatter to use
    * `required` - indicates whether a non-null value resolution is required for this field. If present, it takes precedence over the parser 
    property `RequireDefault`.
    * `id` - field identifier
    * `cacheKey` - cache key to store resolved field value
    * `charset` - defines a [Java supported charset/encoding](https://docs.oracle.com/javase/7/docs/technotes/guides/intl/encoding.doc.html) 
    name (in string format) for field Raw binary data; used to convert between Unicode and a number of other character encodings. Default 
    value is the running streams JVM default charset (in most cases `UTF8`).
    * `emptyAsNull` - flag indicating an empty resolved value (i.e. string `""`, `0` size array/collection, array/collection having all 
    `null` or `""` elements) shall be mapped as `null`. Default value - `true`.

* tags:
    * `field-map` - tag is used to perform manual mapping from streamed data value `source` to field value `target.`

sample:
```xml
    <parser name="TokenParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser">
        <.../>
        <field name="StartTime" locator="1" format="dd MMM yyyy HH:mm:ss" locale="en-US"/>
        <field name="ServerIp" locator="2"/>
        <field name="ApplName" value="orders"/>
        <field name="Correlator" locator="3"/>
        <field name="UserName" locator="4"/>
        <field name="EventName" locator="5"/>
        <field name="EventType" locator="5">
            <field-map source="Order Placed" target="START"/>
            <field-map source="Order Received" target="RECEIVE"/>
            <field-map source="Order Processing" target="OPEN"/>
            <field-map source="Order Processed" target="SEND"/>
            <field-map source="Order Shipped" target="END"/>
        </field>
        <field name="MsgValue" locator="8"/>
    </parser>
```

### Predefined fields set

```java
    /**
     * Name of application associated with the activity.
     */
    ApplName(String.class),

    /**
     * Host name of server to associate with activity.
     */
    ServerName(String.class),

    /**
     * IP Address of server to associate with activity.
     */
    ServerIp(String.class),

    /**
     * Name to assign to activity entry. Examples are operation, method, API call, event, etc.
     */
    EventName(String.class),

    /**
     * Type of activity - value must match values in {@link com.jkoolcloud.tnt4j.core.OpType} enumeration.
     */
    EventType(Enum.class),

    /**
     * Status of activity - value must match values in {@link com.jkoolcloud.tnt4j.core.ActivityStatus} enumeration.
     */
    EventStatus(Enum.class),

    /**
     * Time action associated with activity started.
     */
    StartTime(UsecTimestamp.class),

    /**
     * Time action associated with activity ended.
     */
    EndTime(UsecTimestamp.class),

    /**
     * Elapsed time of the activity.
     */
    ElapsedTime(Long.class),

    /**
     * Identifier of process where activity event has occurred.
     */
    ProcessId(Integer.class),

    /**
     * Identifier of thread where activity event has occurred.
     */
    ThreadId(Integer.class),

    /**
     * Indicates completion status of the activity - value must match values in {@link com.jkoolcloud.tnt4j.core.OpCompCode} enumeration.
     */
    CompCode(Enum.class),

    /**
     * Numeric reason/error code associated with the activity.
     */
    ReasonCode(Integer.class),

    /**
     * Error/exception message associated with the activity.
     */
    Exception(String.class),

    /**
     * Indicates completion status of the activity - value can either be label from {@link com.jkoolcloud.tnt4j.core.OpLevel} enumeration 
     * or a numeric value.
     */
    Severity(Enum.class),

    /**
     * Location that activity occurred at.
     */
    Location(String.class),

    /**
     * Identifier used to correlate/relate activity entries to group them into logical entities.
     */
    Correlator(String[].class),

    /**
     * User-defined label to associate with the activity, generally for locating activity.
     */
    Tag(String[].class),

    /**
     * Name of user associated with the activity.
     */
    UserName(String.class),

    /**
     * Name of resource associated with the activity.
     */
    ResourceName(String.class),

    /**
     * User data to associate with the activity.
     */
    Message(String.class),

    /**
     * Identifier used to uniquely identify the data associated with this activity.
     */
    TrackingId(String.class),

    /**
     * Length of activity event message data.
     */
    MsgLength(Integer.class),

    /**
     * MIME type of activity event message data.
     */
    MsgMimeType(String.class),

    /**
     * Encoding of activity event message data.
     */
    MsgEncoding(String.class),

    /**
     * CharSet of activity event message data.
     */
    MsgCharSet(String.class),

    /**
     * Age of activity event message.
     */
    MessageAge(Long.class),

    /**
     * Activity event category name.
     */
    Category(String.class),

    /**
     * Identifier used to uniquely identify parent activity associated with this activity.
     */
    ParentId(String.class),

    /**
     * Identifier used to globally identify the data associated with this activity.
     */
    Guid(String.class);
```

**NOTE:** `EventType` field is mandatory and can't have value `null`. If this field value resolves to `null` then streams automatically sets 
value to `EVENT`.

**NOTE:** Custom fields values can be found as activity event properties:

sample:
```xml
    <field name="Transport" locator="ActivityTransport" locator-type="Label"/>
    <field name="Topic" locator="TopicName" locator-type="Label"/>
```

### Field locator types

```java
    /**
     * Indicates that raw data value is the value of a named property of the current stream.
     */
    StreamProp(String.class),

    /**
     * Indicates that raw data value is at a specified index location, offset, etc. This is a generic index/offset value whose 
     * interpretation is up to the specific parser applying the locator. It also can be index of RegEx group.
     */
    Index(Integer.class),

    /**
     * Indicates that raw data value is the value of a particular key or label. Examples of this are XPath expressions for XML elements, 
     * and where each element of a raw activity data string is a name/value pair. It also can be name of RegEx group.
     */
    Label(String.class),

    /**
     * Indicates that raw data value is the value of a specific regular expression match, for parsers that interpret the
     * raw activity data using a regular expression pattern defined as a sequence of repeating match patterns. Match
     * identifier can be group sequence number or name.
     * 
     * @deprecated use {@link #Label} instead. 
     */
    @Deprecated
    REMatchId(String.class),

    /**
     * Indicates that data value is the value from stream stored cache with specified cache entry key.
     */
    Cache(String.class),

    /**
     * Indicates that data value is the value from currently processed activity data entity with specified entity field name.
     */
    Activity(String.class);
```

**NOTE:** `Index` is default value and may be suppressed in field/locator definition:

this:
```xml
    <field name="UserName" locator="4"/>
```
is same as:
```xml
    <field name="UserName" locator="4" locator-type="Index"/>
```

**NOTE:** obsolete (yet backward compatible) locator types:
* `REGroupName` - now should be changed to `Label` 
* `REGroupNum` - now should be changed to `Index`
* `REMatchId` - now should be changed to `Label`
* `REMatchNum` - now should be changed to `Index`

### Stacked parsers

In stream parsers configuration you are allowed to use stacked parsers technique: it is when some field data parsed by
one parser can be forwarded to another parser to make more detailed parsing: envelope-message approach.

To define stacked parser(s) you have to define `parser-ref` tag(s) under parser `field` or `embedded-activity` definition.

**NOTE:** `embeded-activity` is tag `field` alias, used to define set of locator resolved data transparent to parent activity, but useful 
to make separate set of related child activities.

**NOTE:** if upper level parser resolved data is incompatible with stacked parser, stacked parser is not applied to parse that data.

sample:
```xml
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="tnt-data-source.xsd">

    <parser name="AccessLogParserCommon" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <.../>
    </parser>
    <parser name="AccessLogParserExt" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <.../>
    </parser>

    <parser name="SampleJMSParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJMSMessageParser">
        <.../>
        <field name="MsgBody" locator="ActivityData" locator-type="Label">
            <parser-ref name="AccessLogParserCommon"/>
        </field>
        <.../>
        <embedded-activity name="InternalActivity" locator="OtherActivityData" locator-type="Label">
            <parser-ref name="AccessLogParserExt" aggregation="Merge"/>
            <parser-ref name="AccessLogParserCommon" aggregation="Relate"/>
        </embedded-activity>
        <.../>
    </parser>

    <stream name="SampleJMStream" class="com.jkoolcloud.tnt4j.streams.inputs.JMSStream">
        <!--<property name="SplitRelatives" value="true"/>-->
        <.../>
        <parser-ref name="SampleJMSParser"/>
    </stream>
</tnt-data-source>
```
See ['JMS text message'](#jms-text-message) sample for full configuration definition.

In this sample stream named `SampleJMStream` has primary parser reference `SampleJMSParser`. It parses data received
as JMS message (envelope). Field `MsgBody` carries JMS message payload data (message). In this sample consider we are
sending Apache Access log entry as JMS message payload. So to parse that Apache access log entry we use stacked parser
named `AccessLogParserCommon`.

After processing one JMS message TNT4J activity event will contain fields mapped by both `SampleJMSParser` and
`AccessLogParserCommon` in the end.

**NOTE:** when using stacked parser, `Activity` type locator prefix `^.` can be used to access parent parser (one stacked parser was invoked 
from) produced activity entity field value. E.g.:
```xml
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="tnt-data-source.xsd">

    <parser name="AccessLogParserCommon" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <.../>
        <field name="ValueFromParent" locator="^.StaticValue" locator-type="Activity"/>
    </parser>

    <parser name="SampleJMSParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJMSMessageParser">
        <.../>
        <field name="StaticValue" value="SampleJMSParser_Value" transparent="true"/>
        <.../>
        <embedded-activity name="InternalActivity" locator="OtherActivityData" locator-type="Label">
            <parser-ref name="AccessLogParserCommon" aggregation="Relate"/>
        </embedded-activity>
        <.../>
    </parser>
    ...
</tnt-data-source>
```
`AccessLogParserCommon` uses field `ValueFromParent` locator `^.StaticValue` to fill in value defined in `SampleJMSParser` parser field 
`StaticValue`. After parsing, field `ValueFromParent` will have value `SampleJMSParser_Value`.

See [Parser matching data or parsing context](#parser-matching-data-or-parsing-context) for parser reference configuration details.

#### Resolved activity entities aggregation

Stacked parsers sample configuration tag `<parser-ref>` has attribute `aggregation`. This attribute defines method of resolved activity 
data aggregation into parent activity. Attribute has two possible values:
* `Merge` - resolved activity entity fields are merged into parent activity. **NOTE:** Parent activity entity will contain all fields 
processed by all stacked parsers. This is default value when attribute `aggregation` definition is missing in configuration.
* `Relate` - resolved activity entities are collected as children of parent activity. As a result there will be one parent activity entity 
having collection of child activities resolved by stacked parsers. **NOTE:** this value has alias `Join` left for backward compatibility, 
but it is not recommended to use it anymore and should be changed right away for existing configurations. Activity entities can have these 
relations:
    * `ACTIVITY` - can have any entity as child: `ACTIVITY`, `EVENT`, `SNAPSHOT`
    * `EVENT` - can have only `SNAPSHOT` as a child
    * `SNAPSHOT` - can't have any child entities

    Be sure that each parser or sub-parser that creates an activity entity to be sent to jKool/AP Insight as a JSON record has a field line:
    ```xml
        <field name="EventType" value="vvvv"/>
    ```
    where `vvvv` is one of values: `ACTIVITY`, `SNAPSHOT` or `EVENT` (`EVENT` also maps from values `OTHER`, `CALL`, `START`, `STOP`, 
    `OPEN`, `CLOSE`, `SEND`, `RECEIVE`, `INQUIRE`, `SET`, `BROWSE`, `ADD`, `UPDATE`, `REMOVE`, `CLEAR`, `DATAGRAM`).
    **NOTE:** If none of the above values applies to your case, you may use `NOOP` value. Then, however, the activity entity JSON record will not be 
    created and not sent to jKoolCloud, e.g.:
    ```xml
        <field name="EventType" value="NOOP"/>
    ```

For a `Relate` type aggregation there is related stream output parameter `SplitRelatives`:
```xml
    <stream name="SampleJMStream" class="com.jkoolcloud.tnt4j.streams.inputs.JMSStream">
        <property name="SplitRelatives" value="true"/>
        <.../>
        <parser-ref name="SampleJMSParser"/>

    </stream>
``` 
or
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <java-object name="StreamOutput" class="com.jkoolcloud.tnt4j.streams.outputs.JKCloudActivityOutput">
        <property name="SplitRelatives" value="true"/>
    </java-object>
    <.../>
    <stream name="SampleJMStream" class="com.jkoolcloud.tnt4j.streams.inputs.JMSStream">
        <.../>
        <parser-ref name="SampleJMSParser"/>
        <reference name="StreamOutput"/>
    </stream>
</tnt-data-source>
```
It allows to send as many activity entities to jKool as there are child activity entities resolved by stacked parsers, **merging** those 
child activity entities data with parent activity entity data. E.g. when parser builds activity entities relations like this (4 entities in 
total):
```
ParentActivity
    + ChildEvent1
    + ChildEvent2
    + ChildEvent3
```
stream output will send such entities to jKool (3 entities in total):
```
Event1 having ChilEvent1 + ParentActivity data
Event2 having ChilEvent2 + ParentActivity data
Event3 having ChilEvent3 + ParentActivity data
```

This is useful when streamed data is aggregated in one data package, like JSON/XML data having some header values and array of payload 
entries (see `mft-tracking` sample XML's), but you need only those payload entries maintaining some header data contained values to be set 
to jKool. Or like in `mft_fte` sample, when MFT `transaction` progress event `transferSet` node having `source` and `destination` 
definitions and you need to split them into separate events maintaining some `transaction` data values.

### Field value transformations

In streams configuration you can define field or locator resolved values transformations. In general transformations performs resolved 
activity value post-processing before sending it to [jKoolCloud](https://www.jkoolcloud.com/): e.g., extracts file name from resolved 
activity file path.

To pass a resolved field/locator value to a transformation script/expression, use the predefined variable placeholder `$fieldValue`. You can 
also use parser defined field names as script/expression variables having the format `${FIELD_NAME}` to access resolved activity entity 
fields like `${EventType}` or `${Trace.HighResTime}`. The referenced fields must be <u>within the same parser definition</u>.

#### Transformation definition

To define transformations stream configuration token `<field-transform>` shall be used. Attributes:
 * `name` - name of transformation (optional).
 * `lang` - transformation script/expression language. Can be one of `groovy`, `javascript` or `xpath`. Default value - `javascript`.
 * `beanRef` - transformation implementing bean reference.
 * `phase` - defines activity data resolution phase, when transformation has to be applied. Can be one of `raw`, `formatted` or `aggregated`.
 `raw` and `formatted` values are applicable for field locator resolved value transformation, while `aggregated` is default for field value 
 transformation.

Token body is used to define transformation script/expression code.

Valid transformation configuration should define `beanRef`, or have script/expression code defined in token body data (`<![CDATA[]]>`).

##### TNT4J-Streams predefined custom XPath functions

To use TNT4J-Streams predefined functions namespace `ts:` shall be used.

Streams predefined custom XPath functions to be used in transformation expressions:
* `ts:getFileName(filePath)` - implemented by transformation bean `com.jkoolcloud.tnt4j.streams.transform.FuncGetFileName`. Retrieves file 
name from provided file path.
* `ts:getObjectName(objectFQN, options)` - implemented by transformation bean `com.jkoolcloud.tnt4j.streams.transform.FuncGetObjectName`. 
Retrieves desired object name from provided fully qualified object name. Function supported options:
    * resolution options: `DEFAULT`, `BEFORE`, `AFTER`, `REPLACE`, `FULL`. (Optional)
    * search symbols. (Optional)
    * replacement symbols. (Optional)

You may also define your own customized XPath functions. To do this your API has to:
* implement interface `javax.xml.xpath.XPathFunction`
* register function by invoking `com.jkoolcloud.tnt4j.streams.utils.StreamsXMLUtils.registerCustomFunction(functionName, function)`.

e.g.:
```java
public class YourTransform implements XPathFunction {
  @Override
   public Object evaluate(List args) {
    // retrieve expression code provided arguments and make transformation here.
  }
}
...
StreamsXMLUtils.registerCustomFunction("yourTransformation", new YourTransform());
...
```
then you can use it from stream configuration:
```xml
<field name="InvoiceFileFromFunction" locator="7">
    <field-transform name="fileNameF" lang="xpath">
        ts:yourTransformation($fieldValue, "arg2", "arg3")
    </field-transform>
</field>
```

**NOTE:** those functions can be also used in XPath expressions of [ActivityXMLParser](#activity-xml-parser) (or [MessageActivityXMLParser](#message-activity-xml-parser)) 
field locators. For example: 
```xml
    <parser name="XMLParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityXmlParser">
        <.../>
        <field name="MFT_SRC_FILE_NAME" locator="ts:getFileName(/transaction/transferSet/item/source/file)" locator-type="Label"/>
        <.../>
    </parser>
```

Use samples of `ts:getObjectName()` (consider XPath `/transaction/transferSet/item/source/queue` points to queue named `SomeQueueName@Agent1`):
```xml
    <parser name="XMLParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityXmlParser">
        <.../>
        <!-- to get queue name without agent: 'SomeQueueName' -->
        <field name="MFT_SRC_QUEUE_NAME" locator="ts:getObjectName(/transaction/transferSet/item/source/queue)" locator-type="Label"/>
        <!-- to get queue name without agent (part before FQN delimiter): 'SomeQueueName'-->
        <field name="MFT_SRC_QUEUE_NAME" locator="ts:getObjectName(/transaction/transferSet/item/source/queue, 'BEFORE', '@')" locator-type="Label"/>
        <!-- to get agent name (part after FQN delimiter): 'Agent1'-->
        <field name="MFT_SRC_AGENT_NAME" locator="ts:getObjectName(/transaction/transferSet/item/source/queue, 'AFTER', '@')" locator-type="Label"/>
        <!-- to get queue fully qualified name with '@' replaced to '_': 'SomeQueueName_Agent1' -->
        <field name="MFT_SRC_QUEUE_FQN_REPLACED" locator="ts:getObjectName(/transaction/transferSet/item/source/queue, 'REPLACE', '@', '_')" locator-type="Label"/>
        <!-- to get queue fully qualified: 'SomeQueueName@Agent1'-->
        <field name="MFT_SRC_QUEUE_FQN" locator="ts:getObjectName(/transaction/transferSet/item/source/queue, 'FULL')" locator-type="Label"/>
        <.../>
    </parser>
```

#### Stream elements transformations

* Field value transformation
```xml
<field name="Field" separator=",">
    <field-transform name="toUpper" lang="groovy">
        $fieldValue.toUpperCase()
    </field-transform>
    <field-locator locator="loc1" locator-type="Label"/>
    <field-locator locator="loc2" locator-type="Label"/>
    <.../>
</field>
```
When transformation is defined for a field containing multiple locators, field value transformation is applied to all field locators 
aggregated field value.

Sample above states that field value combined from locators `loc1` and `loc2` and separated by `,` should be upper cased. For example 
locator `loc1` resolves value `value1`, locator `loc2` resolves value `vaLue2`. Then field value before transformations is `value1,vaLue2` 
and field value after transformation is `VALUE1,VALUE2`.

* Field locator value transformation
```xml
<field name="Field" separator=",">
    <field-locator locator="loc1" locator-type="Label">
        <field-transform name="toUpper" lang="groovy" phase="raw">
            $fieldValue.toUpperCase()
        </field-transform>
    </field-locator>
    <field-locator locator="loc2" locator-type="Label">
        <field-transform name="toUpperConcat" lang="groovy" phase="formatted">
            $fieldValue.toUpperCase() + "_transformed"
        </field-transform>
    </field-locator>
    <.../>
</field>
```

Sample above states that locator `loc1` resolved value should be upper cased and locator `loc2` resolved value should be upper cased and 
concatenated with string `_transformed`. Then those transformed values should be aggregated to field value separated by `,` symbol. For 
example locator `loc1` resolved value `value1`, then after transformation value is changed to `VALUE1`. Locator `loc2` resolved value 
`value2`, then after transformation value is changed to `VALIUE2_transformed`. Field aggregates those locators values to 
`VALUE1,VALUE2_transformed`.

* Field/locator value transformation using additional activity entity fields
 ```xml
 <field name="EventName" locator="enLoc" locator-type="Label"/>
 <field name="EventType" locator="etLoc" locator-type="Label"/>
 ... 
 <field name="Field" value="activity">
    <field-transform name="toUpperConcat" lang="groovy">
        $fieldValue.toUpperCase() + "=" + ${EventType} + " : " + ${EventName}
    </field-transform>
    <.../>
 </field>
 ```

Sample above defines fields `EventName` and `EventType` resolving values from streamed activity data. Then field named `Field` uses 
transformation to build value from predefined initial value `activity` and fields `EventName`, `EventType` resolved values. For 
example field `EventName` resolved value `Transcation_X` and `EventType` resolved value is `SEND`. Then after transformation field `Field` 
value becomes as `ACTIVITY=SEND:Transcation_X`.

**NOTE:** 
* locator defined transformations are applied before field value transformations.
* it is possible to define multiple transformations for same stream element (field/locator). In that case transformations are applied 
sequentially where input of applied transformation is output of previous transformation.
* it is allowed to combine field and locators transformations within same stream field scope setting transformations for both field and 
locators independently.
* when "short form" of field-locator configuration is used, transformation is bound to locator (because field-locator relation is 1:1 and 
resolved value is same for locator and for field). Example:
```xml
<field name="Correlator" locator="3">
    <field-transform name="concat">
        "corel_" + $fieldValue
    </field-transform>
</field>
```

#### Transformation definition samples

* Groovy script/expression
```xml
<field name="ApplName" value="orders">
    <field-transform name="toUpper" lang="groovy">
        $fieldValue.toUpperCase().center(30)
    </field-transform>
</field>
```
This sample upper case resolved value and makes "centred" (padded and tailed with space symbols) 30 symbols length string.

* JavaScript script/expression
```xml
<field name="UserName" locator="4">
    <field-transform name="toUpper" lang="javascript">
        $fieldValue.toUpperCase()
    </field-transform>
</field>
```
This sample upper case resolved value.

Or (using default attribute `lang` value) 
```xml
<field name="Correlator" locator="3">
    <field-transform name="concat">
        "corel_" + $fieldValue
    </field-transform>
</field>
```
This sample concatenates string `corel_` and resolved value.

`JavaScript`/`Groovy` based transformations default import packages:
 * `com.jkoolcloud.tnt4j.core`
 * `com.jkoolcloud.tnt4j.utils`
 * `com.jkoolcloud.tnt4j.uuid`
 * `com.jkoolcloud.tnt4j.streams.utils`
 * `com.jkoolcloud.tnt4j.streams.fields`
 * `org.apache.commons.lang3`
 * `org.apache.commons.collections4`

so you are allowed to directly use classes from those packages in script code, e.g.:
```xml
    <field-transform lang="groovy" name="FieldSubstring10">
        <![CDATA[
            StringUtils.substring($fieldValue, 0, 10)
        ]]>
    </field-transform>
```

* XPath expression
```xml
<field name="InvoiceFileFromFunction" locator="7">
    <field-transform name="fileNameF" lang="xpath">
        concat(ts:getFileName($fieldValue), "_cust")
    </field-transform>
</field>
```
This sample retrieves file name from resolved file path (contained in `$fieldValue`) and concatenates it with string `_cust`.

* Transformation bean

To make custom Java Bean based transformations your API should implement interface `com.jkoolcloud.tnt4j.streams.transform.ValueTransformation<V, T>`.

```xml
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="tnt-data-source.xsd">
    <.../>
    <java-object name="getFileNameTransform" class="com.jkoolcloud.tnt4j.streams.transform.FuncGetFileName"/>
    <.../>
    <field name="InvoiceFileFromBean">
        <field-locator locator="7">
            <field-transform beanRef="getFileNameTransform"/>
        </field-locator>
    </field>
    <.../>
```
This sample shows how to invoke Java Bean defined transformation. Class `com.jkoolcloud.tnt4j.streams.transform.FuncGetFileName` implements 
custom XPath function to get file name from provided file path. Field/locator value mapping to function arguments is performed automatically 
and there is no need to define additional mapping.

* Building field value from other activity entity fields values using transformations

Sometimes field value may be some combination of other activity entity fields values. To achieve such goal, transformations comes in hand.

```xml
    <field name="MQTrace_HighresTime" locator="MQGACF_ACTIVITY_TRACE.MQIAMO64_HIGHRES_TIME" locator-type="Label" datatype="Timestamp"
           units="Microseconds"/>
    <field name="ElapsedTime" locator="MQGACF_ACTIVITY_TRACE.MQIAMO64_QMGR_OP_DURATION" locator-type="Label"
           datatype="Number" units="Microseconds"/>
    <field name="StartTime" value="">
        <field-transform lang="groovy" name="StartTimeTransform">
            ${MQTrace_HighresTime} / 1000
        </field-transform>
    </field>
    <field name="EndTime" value="">
        <field-transform lang="groovy" name="EndTimeTransform">
           ${MQTrace_HighresTime} + ${ElapsedTime}
        </field-transform>
    </field>
```

Sample shows how to make activity entity fields `StartTime` and `EndTime` values using IBM MQ trace resolved high resolution values (in 
microseconds) of operation start and elapsed times.

`StartTime` is evaluated using Groovy transformation making field `MQTrace_HighresTime` field value dividing by `1000` and such getting time 
value in milliseconds.

`EndTime` is evaluated using Groovy transformation adding `MQTrace_HighresTime` and `ElapsedTime` fields values having final value in 
microseconds.

**NOTE:** initial value for those fields is set to `value=""`. But it is allowed to set there any default initial value or use value 
locator(s), if it may be useful for a transformation expression evaluation. In that case to access initial value use expression variable 
`$fieldValue`.

### Stream elements filtering

TODO

### Use of dynamic locators

`TNT4J-Streams` allows you to dynamically define `field`/`field-locator` parameters. A dynamic reference variable placeholder is defined 
using `${XXXXX}` format, where `XXXXX` is a name or identifier of another data source configuration entity <u>within the same parser 
definition</u>.

Defining dynamic `field` parameters sample:
```xml
    <.../>
    <parser name="CollectdStatsDataParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <field name="EventType" value="SNAPSHOT"/>
        <.../>
        <field name="${FieldNameLoc}" locator="values" locator-type="Label" value-type="${ValueTypeLoc}" split="true">
            <field-locator id="FieldNameLoc" locator="dsnames" locator-type="Label"/>
            <field-locator id="ValueTypeLoc" locator="dstypes" locator-type="Label"/>
        </field>
        <.../>
    </parser>
    <.../>
```

Sample shows how to dynamically define field `name` and `value-type` parameters referencing values resolved by `FieldNameLoc` and 
`ValueTypeLoc` locators.

There is also field attribute `split` stating that if field/locator resolved value is array/collection, then it should make as many 
activity fields as there are array/collection items available. In this case if field name is static (or makes same name from dynamically 
resolved values), name gets appended with sequential numbering to make fields names unique.

Defining combined `field` parameters values:
```xml
    <.../>
    <parser name="AttributesParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityXmlParser">
        <field name="${FieldNameLoc}_attr" locator="/entry/*[2]/text()" locator-type="Label" split="true">
            <field-locator id="FieldNameLoc" locator="/entry/*[1]/text()" locator-type="Label"/>
        </field>
    </parser>
    <.../>
```

In this sample field name value is combined from dynamic `${FieldNameLoc}` and static `_attr` parts.

Sample of using another `field` resolved value in locator definition:
```xml
    <.../>
     <parser name="TransferSetParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityXmlParser">
        <.../>
        <field name="Direction" locator="name(//*[1])" locator-type="Label" transparent="true"/>

        <field name="ResourceName" formattingPattern="{0}={1};Agent={2}">
            <!--resolves FILE or QUEUE-->
            <field-locator locator="name(//*/*)" locator-type="Label">
                <field-map-ref resource="MFT_MAPPINGS.Resource"/>
            </field-locator>
            <!--resolves file or queue name -->
            <field-locator locator="ts:getFileName(/${Direction}/file)" locator-type="Label" required="false"/>
            <field-locator locator="ts:getObjectName(/${Direction}/queue)" locator-type="Label" required="false"/>
            <!-- agent-->
            <field-locator locator="/transaction/${Direction}Agent/@agent" locator-type="Label" required="false"/>
        </field>
    </parser>
    <.../>
```

Sample configuration defines parser field `Direction` resolving value e.g., `source` or `destination`. Then field `ResourceName` locators 
use this value when constructing actual XPath expression e.g., `/transaction/${Direction}Agent/@agent` to resolve value from XML data.

**NOTE:** when using `field`/`field-locator` attribute `locator-type="Activity"` define field name as locator value without `${}`, i.e.:
```xml
    <.../>
    <parser name="TransferSetParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityXmlParser">
        <.../>
        <field name="Checksum" locator="//*/checksum" locator-type="Label" required="false"/>
        <field name="Correlator" locator="Checksum" locator-type="Activity" required="false"/>
        <.../>
    </parser>
    <.../>
```

### Caching of streamed data field values

`TNT4J-Streams` provides temporary storage (i.e. cache) for a resolved activity fields values. It is useful when there are some related 
activities streamed and particular jKool prepared activity entity requires data values form previously streamed activities.

Sample streamed values caching configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="EventParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJsonParser">
        <property name="ActivityDelim" value="EOF"/>

        <field name="EventType" value="EVENT"/>

        <field name="EventName" locator="$.event" locator-type="Label"/>
        <field name="LastEvent" locator="CachedEventName" locator-type="Cache"/>

        <field name="Correlator" locator="$.transaction" locator-type="Label"/>
        <field name="Transaction" locator="$.transaction" locator-type="Label"/>

        <field name="SecretValue" locator="$.secret" locator-type="Label"/>
        <field name="LastSecret" locator="SecretCache" locator-type="Cache"/>

        <field name="Message" locator="$.message" locator-type="Label"/>
        <field name="Resource" locator="$.resource" locator-type="Label"/>
    </parser>

    <cache>
        <property name="MaxSize" value="300"/>
        <property name="ExpireDuration" value="15"/>

        <entry id="CachedEventName">
            <key>EventName</key>
            <value>${EventName} in ${Transaction}</value>
        </entry>
        <entry id="SecretCache">
            <key>${Transaction}Secret</key>
            <value>${SecretValue}</value>
        </entry>
    </cache>

    <stream name="MultipleEvents" class="com.jkoolcloud.tnt4j.streams.inputs.CharacterStream">
        <property name="FileName" value="./tnt4j-streams-core/samples/cached-values/event*.json"/>

        <parser-ref name="EventParser"/>
    </stream>

</tnt-data-source>
```

Sample configuration defines stream `MultipleEvents` reading data from JSON files using filename mask `event*.json` and referencing parser 
`EventParser`. Stream defines two cache related properties `MaxSize` and `ExpireDuration`. Definitions of those properties can be found in 
chapter [Stream cache related parameters](#stream-cache-related-parameters).

Stream definition has `cache` section, defining stored cache entries. Entry key and value can be configured using static values (e.g., 
`<key>EventName</key>`), dynamic activity value references (e.g., `<value>${SecretValue}</value>`) referencing activity entity field name, 
or combined static and dynamic values (e.g., `<value>${EventName} in ${Transaction}</value>`). Using dynamic value references in `<key>` 
definition will result having multiple cache entries filled in with values resolved from streamed activity data.

Parser `EventParser` has two fields having `locator-type="Cache"`. This means that values from those fields are resolved from stream cache 
entries referenced by `locator="SecretCache"` where locator value points to cache entry identifier. Having reference to cache entry, stream 
takes entry key definition and pre-fills it with current activity entity fields values making actual cache key value and thus mapping to 
particular cached value.

## Samples

### Running samples
When release assemblies are built, samples are located in `samples` directory, e.g., `../build/tnt4j-streams/tnt4j-streams-1.0.0/samples`.
To run particular sample:
* go to sample directory
* run `run.bat` or `run.sh` depending on your OS

For more detailed explanation of streams and parsers configuration and usage see chapter ['Configuring TNT4J-Streams'](#configuring-tnt4j-streams)
and JavaDocs.

#### Single Log file

This sample shows how to stream activity events (orders) data from single log file.

Sample files can be found in `samples/single-log` directory.

`orders.log` file contains set of order activity events. Single file line defines data of single order activity event.

**NOTE:** Records in this file are from year `2011`, e.g. `12 Jul 2011`, so when sending the events data to 
[jKoolCloud](https://www.jkoolcloud.com/), please do not forget to adjust the dashboard time frame to that period!

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="TokenParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser">
        <property name="FieldDelim" value="|"/>
        <field name="StartTime" locator="1" format="dd MMM yyyy HH:mm:ss" locale="en-US"/>
        <field name="ServerIp" locator="2"/>
        <field name="ApplName" value="orders"/>
        <field name="Correlator" locator="3"/>
        <field name="UserName" locator="4"/>
        <field name="EventName" locator="5"/>
        <field name="EventType" locator="5">
            <field-map source="Order Placed" target="START"/>
            <field-map source="Order Received" target="RECEIVE"/>
            <field-map source="Order Processing" target="OPEN"/>
            <field-map source="Order Processed" target="SEND"/>
            <field-map source="Order Shipped" target="END"/>
        </field>
        <field name="MsgValue" locator="8"/>
    </parser>

    <stream name="FileStream" class="com.jkoolcloud.tnt4j.streams.inputs.FileLineStream">
        <property name="FileName" value="orders.log"/>
        <!--<property name="RangeToStream" value="1:"/>-->

        <parser-ref name="TokenParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `FileLineStream` referencing `TokenParser` shall be used.

`FileStream` reads data from `orders.log` file.

`TokenParser` uses `|` symbol as fields delimiter and maps fields to TNT4J event fields using field index locator.

**NOTE:** `StartTime` fields defines format and locale to correctly parse field data string. `EventType` uses manual
field string mapping to TNT4J event field value.

#### Multiple Log files

This sample shows how to stream activity events (orders) data from multiple log files using file name matching
wildcard pattern.

Sample files can be found in `samples/multiple-logs` directory.

`orders-in.log` and `orders-out.log` files contains set of order activity events. Single file line defines data of
single order activity event.

**NOTE:** Records in this file are from year `2011`, e.g. `12 Jul 2011`, so when sending the events data to 
[jKoolCloud](https://www.jkoolcloud.com/), please do not forget to adjust the dashboard time frame to that period!

Sample configuration and sample idea is same as ['Single Log file'](#single-log-file) with one single difference:
```xml
    <property name="FileName" value="orders-*.log"/>
```
meaning that stream should process not one single file, but file set matching `orders-*.log` wildcard pattern.

#### OS piped stream

This sample shows how to stream activity events (orders) data received over OS pipe from another application or OS
command.

Sample files can be found in `samples/piping-stream` directory.

`orders.log` file contains set of order activity events. Single file line defines data of single order activity event.

**NOTE:** Records in this file are from year `2011`, e.g. `12 Jul 2011`, so when sending the events data to 
[jKoolCloud](https://www.jkoolcloud.com/), please do not forget to adjust the dashboard time frame to that period!

`jk-pipe.bat` or `jk-pipe.sh` files are wrappers to `bin/tnt4j-streams` executables to minimize parameters. All what
 you need is to pass file name of stream parsers configuration, e.g., `parsers.xml`

`run.bat` or `run.sh` files uses OS piping to run sample.

Sample parser configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="TokenParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser">
        <property name="FieldDelim" value="|"/>
        <field name="StartTime" locator="1" format="dd MMM yyyy HH:mm:ss" locale="en-US"/>
        <field name="ServerIp" locator="2"/>
        <field name="ApplName" value="orders"/>
        <field name="Correlator" locator="3"/>
        <field name="UserName" locator="4"/>
        <field name="EventName" locator="5"/>
        <field name="EventType" locator="5">
            <field-map source="Order Placed" target="START"/>
            <field-map source="Order Received" target="RECEIVE"/>
            <field-map source="Order Processing" target="OPEN"/>
            <field-map source="Order Processed" target="SEND"/>
            <field-map source="Order Shipped" target="END"/>
        </field>
        <field name="MsgValue" locator="8"/>
    </parser>
</tnt-data-source>
```

For details on parser configuration see sample named ['Single Log file'](#single-log-file).

#### Zipped file lines

This sample shows how to stream activity events (Apache access log records) data from zipped file entries.

Sample files can be found in `samples/zip-stream` directory.

`sample.zip` and `sample.gz` files contains set of compressed Apache access log files.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="AccessLogParserExt" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b %D"/>
        <property name="ConfRegexMapping" value="%*r=(((\S+) (.*?)( (\S+))?)|(-))"/>
        <property name="ConfRegexMapping" value="%*i=(.*?)"/>

        <field name="Location" locator="1" locator-type="Index"/>
        <field name="UserName" locator="3" locator-type="Index"/>
        <field name="StartTime" locator="4" locator-type="Index" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="7" locator-type="Index"/>
        <field name="ResourceName" locator="8" locator-type="Index"/>
        <field name="CompCode" locator="12" locator-type="Index">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="12" locator-type="Index"/>
        <field name="MsgValue" locator="13" locator-type="Index"/>
        <field name="ElapsedTime" locator="15" locator-type="Index" datatype="Number" format="#####0.000"
               locale="en-US" units="Seconds"/>
    </parser>

    <parser name="AccessLogParserCommon" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b"/>
        <property name="ConfRegexMapping" value="%*r=(((\S+) (.*?)( (\S+))?)|(-))"/>
        <property name="ConfRegexMapping" value="%*i=(.*?)"/>

        <field name="Location" locator="1" locator-type="Index"/>
        <field name="UserName" locator="3" locator-type="Index"/>
        <field name="StartTime" locator="4" locator-type="Index" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="7" locator-type="Index"/>
        <field name="ResourceName" locator="8" locator-type="Index"/>
        <field name="CompCode" locator="12" locator-type="Index">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="12" locator-type="Index"/>
        <field name="MsgValue" locator="13" locator-type="Index"/>
    </parser>

    <stream name="SampleZipFileStream" class="com.jkoolcloud.tnt4j.streams.inputs.ZipLineStream">
        <property name="FileName" value="./tnt4j-streams-core/samples/zip-stream/sample.zip"/>
        <!--<property name="FileName" value="./tnt4j-streams-core/samples/zip-stream/sample.zip!2/*.txt"/>-->
        <!--<property name="FileName" value="./tnt4j-streams-core/samples/zip-stream/sample.gz"/>-->
        <!--<property name="ArchType" value="GZIP"/>-->

        <parser-ref name="AccessLogParserExt"/>
        <parser-ref name="AccessLogParserCommon"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `ZipLineStream` referencing `AccessLogParserExt` and `AccessLogParserCommon` shall be
used.

`ZipLineStream` reads all entries lines from `sample.zip` file.

To filter zip file entries use zip entry name wildcard pattern, e.g., `sample.zip!2/*.txt`. In this case stream will read
just zipped files having extension `txt` from internal zip directory named `2` (from sub-directories also).

Details on `AccessLogParserCommon` (or `ApacheAccessLogParser` in general) can be found in samples section 
['Apache Access log single file'](#apache-access-log-single-file) and parsers configuration section ['Apache access log parser'](#apache-access-log-parser).

#### Standard Java InputStream/Reader

This sample shows how to stream activity events (Apache access log records) data read from standard Java input.

Sample files can be found in `samples/java-stream` directory.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="AccessLogParserExt" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b %D"/>
        <property name="ConfRegexMapping" value="%*r=(((\S+) (.*?)( (\S+))?)|(-))"/>
        <property name="ConfRegexMapping" value="%*i=(.*?)"/>

        <field name="Location" locator="1" locator-type="Index"/>
        <field name="UserName" locator="3" locator-type="Index"/>
        <field name="StartTime" locator="4" locator-type="Index" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="7" locator-type="Index"/>
        <field name="ResourceName" locator="8" locator-type="Index"/>
        <field name="CompCode" locator="12" locator-type="Index">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="12" locator-type="Index"/>
        <field name="MsgValue" locator="13" locator-type="Index"/>
        <field name="ElapsedTime" locator="15" locator-type="Index" datatype="Number" format="#####0.000"
               locale="en-US" units="Seconds"/>
    </parser>

    <parser name="AccessLogParserCommon" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b"/>
        <property name="ConfRegexMapping" value="%*r=(((\S+) (.*?)( (\S+))?)|(-))"/>
        <property name="ConfRegexMapping" value="%*i=(.*?)"/>

        <field name="Location" locator="1" locator-type="Index"/>
        <field name="UserName" locator="3" locator-type="Index"/>
        <field name="StartTime" locator="4" locator-type="Index" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="7" locator-type="Index"/>
        <field name="ResourceName" locator="8" locator-type="Index"/>
        <field name="CompCode" locator="12" locator-type="Index">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="12" locator-type="Index"/>
        <field name="MsgValue" locator="13" locator-type="Index"/>
    </parser>

    <java-object name="SampleFileStream" class="java.io.FileInputStream">
        <param name="fileName" value="./tnt4j-streams-core/samples/zip-stream/sample.gz" type="java.lang.String"/>
    </java-object>
    <java-object name="SampleZipStream" class="java.util.zip.GZIPInputStream">
        <param name="stream" value="SampleFileStream" type="java.io.InputStream"/>
    </java-object>
    <!--java-object name="SampleFileReader" class="java.io.FileReader">
        <param name="fileName" value="./tnt4j-streams-core/samples/apache-access-single-log/access.log"
            type="java.lang.String"/>
    </java-object-->

    <stream name="SampleJavaInputStream" class="com.jkoolcloud.tnt4j.streams.inputs.JavaInputStream">
        <property name="HaltIfNoParser" value="false"/>

        <reference name="SampleZipStream"/>
        <!--reference name="SampleFileReader"/-->

        <reference name="AccessLogParserExt"/>
        <reference name="AccessLogParserCommon"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `SampleJavaInputStream` referencing `AccessLogParserExt` and `AccessLogParserCommon`
shall be used.

`SampleJavaInputStream` reads all lines from `SampleZipStream` referring FileInputStream `SampleFileStream` which reads
file `sample.gz`.

**NOTE:** that `java-object/param@value` value may be reference to configuration already defined object
like `SampleFileStream` in this particular sample.

To use `Reader` as input you should uncomment configuration lines defining and referring `SampleFileReader` and comment
out `SampleZipStream` reference.

Details on `AccessLogParserCommon` (or `ApacheAccessLogParser` in general) can be found in samples section 
['Apache Access log single file'](#apache-access-log-single-file) and parsers configuration section ['Apache access log parser'](#apache-access-log-parser).

#### Apache Access log single file

This sample shows how to stream Apache access log records as activity events from single log file.

Sample files can be found in `samples/apache-access-single-log` directory.

`access.log` is sample Apache access log file depicting some HTTP server activity.

**NOTE:** Records in this file are from year `2004`, e.g. `07/Mar/2004`, so when sending the events data to 
[jKoolCloud](https://www.jkoolcloud.com/), please do not forget to adjust the dashboard time frame to that period!

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="AccessLogParserExt" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b %D"/>
        <!--property name="Pattern"
                  value="^(\S+) (\S+) (\S+) \[([\w:/]+\s[+\-]\d{4})\] &quot;(((\S+) (.*?)( (\S+)|()))|(-))&quot; (\d{3}) (\d+|-)( (\S+)|$)"/-->
        <property name="ConfRegexMapping" value="%*r=(((\S+) (.*?)( (\S+)|()))|(-))"/>
        <property name="ConfRegexMapping" value="%*i=(.*?)"/>

        <field name="Location" locator="1" locator-type="Index"/>
        <field name="UserName" locator="3" locator-type="Index"/>
        <field name="StartTime" locator="4" locator-type="Index" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="7" locator-type="Index"/>
        <field name="ResourceName" locator="8" locator-type="Index"/>
        <field name="CompCode" locator="12" locator-type="Index">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="12" locator-type="Index"/>
        <field name="MsgValue" locator="13" locator-type="Index"/>
        <field name="ElapsedTime" locator="15" locator-type="Index" datatype="Number" format="#####0.000" locale="en-US" units="Seconds"/>

    </parser>

    <parser name="AccessLogParserCommon" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b"/>
        <property name="ConfRegexMapping" value="%*r=(((\S+) (.*?)( (\S+)|()))|(-))"/>
        <property name="ConfRegexMapping" value="%*i=(.*?)"/>

        <field name="Location" locator="1" locator-type="Index"/>
        <field name="UserName" locator="3" locator-type="Index"/>
        <field name="StartTime" locator="4" locator-type="Index" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="7" locator-type="Index"/>
        <field name="ResourceName" locator="8" locator-type="Index"/>
        <field name="CompCode" locator="12" locator-type="Index">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="12" locator-type="Index"/>
        <field name="MsgValue" locator="13" locator-type="Index"/>

    </parser>

    <stream name="FileStream" class="com.jkoolcloud.tnt4j.streams.inputs.FileLineStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="FileName" value="access.log"/>
        <!--<property name="RangeToStream" value="1:"/>-->

        <parser-ref name="AccessLogParserExt"/>
        <parser-ref name="AccessLogParserCommon"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `FileLineStream` referencing `AccessLogParserExt` and `AccessLogParserCommon` shall
be used. Note that multiple parsers can be used to parse stream entries data, meaning that activity event data will be
made by first parser capable to parse entry data.

`FileStream` reads data from `access.log` file. `HaltIfNoParser` property states that stream should skip unparseable
entries and don't stop if such situation occurs.

`AccessLogParserCommon` parser is dedicated to parse Apache access log entries made using default logging configuration.

`LogPattern` defines logger pattern used to log entries to log file. Using this property parser is capable to
automatically build RegEx to parse log entry fields.

User is also allowed to manually define RegEx for log entry line using `Pattern` property.

`ConfRegexMapping` properties are used to allow user override default log pattern token-RegEx mappings and define those
manually to improve automatically build entry line RegEx.

Activity event fields mapping is performed using locator identifying RegEx pattern group index.

`AccessLogParserExt` is differs from `AccessLogParserCommon` just by having one additional log token `%D` in `LogPattern`
property.

So if for example half of log file was made using log pattern defined in `AccessLogParserCommon` parser `LogPattern`
property and the second part using log pater defined in `AccessLogParserExt` parser `LogPattern` property - stream
should be able to handle whole log file with no problems.

**NOTE:** `StartTime` fields defines format and locale to correctly parse field data string. `CompCode` uses manual
field string mapping to TNT4J event field value.

#### Apache Access log single file - named RegEx groups mappings

Sample does all the same as [Apache Access log single file](#apache-access-log-single-file), but uses named RegEx group mapping for a 
parser defined fields.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="AccessLogParserExt" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b %D"/>
        <property name="ConfRegexMapping"><![CDATA[%*r=(?<request>((?<method>\S+) (?<uri>.*?)( (?<version>\S+))?)|(-))]]></property>

        <field name="Location" locator="hostname" locator-type="Label"/>
        <field name="UserName" locator="user" locator-type="Label"/>
        <field name="StartTime" locator="time" locator-type="Label" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="method" locator-type="Label"/>
        <field name="ResourceName" locator="uri" locator-type="Label"/>
        <field name="CompCode" locator="status" locator-type="Label">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="status" locator-type="Label"/>
        <field name="MsgValue" locator="sizeClf" locator-type="Label"/>
        <field name="ElapsedTime" locator="reqTime" locator-type="Label" datatype="Number" format="#####0.000" locale="en-US"
               units="Seconds"/>
    </parser>

    <parser name="AccessLogParserCommon" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b"/>
        <property name="ConfRegexMapping"><![CDATA[%*r=(?<request>((?<method>\S+) (?<uri>.*?)( (?<version>\S+))?)|(-))]]></property>

        <field name="Location" locator="hostname" locator-type="Label"/>
        <field name="UserName" locator="user" locator-type="Label"/>
        <field name="StartTime" locator="time" locator-type="Label" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="method" locator-type="Label"/>
        <field name="ResourceName" locator="uri" locator-type="Label"/>
        <field name="CompCode" locator="status" locator-type="Label">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="status" locator-type="Label"/>
        <field name="MsgValue" locator="sizeClf" locator-type="Label"/>
    </parser>

    <stream name="FileStream" class="com.jkoolcloud.tnt4j.streams.inputs.FileLineStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="FileName" value="./tnt4j-streams-core/samples/apache-access-single-log/access.log"/>
        <property name="RestoreState" value="false"/>
        <!--<property name="RangeToStream" value="1:"/>-->

        <!--<property name="UseExecutors" value="true"/>-->
        <!--<property name="ExecutorThreadsQuantity" value="5"/>-->
        <!--<property name="ExecutorsTerminationTimeout" value="20"/>-->
        <!--<property name="ExecutorsBoundedModel" value="false"/>-->
        <!--<property name="ExecutorRejectedTaskOfferTimeout" value="20"/>-->

        <parser-ref name="AccessLogParserExt"/>
        <parser-ref name="AccessLogParserCommon"/>
    </stream>
</tnt-data-source>
```
See parsers configuration section ['Apache access log parser'](#apache-access-log-parser) for a default RegEx group names used by 
`ApacheAccessLogParser`.

#### Apache Access log multiple files

This sample shows how to stream Apache access log records as activity events from multiple log files using file name
matching wildcard pattern.

Sample files can be found in `samples/apache-access-multi-log` directory.

`localhost_access_log.[DATE].txt` is sample Apache access log files depicting some HTTP server activity.

**NOTE:** Records in this file are from year `2015` ranging from April until November, so when sending the events data to 
[jKoolCloud](https://www.jkoolcloud.com/), please do not forget to adjust the dashboard time frame to that period!

Sample configuration and sample idea is same as ['Apache Access log single file'](#apache-access-log-single-file) with one single 
difference:
```xml
    <property name="FileName" value="*_access_log.2015-*.txt"/>
```
meaning that stream should process not one single file, but file set matching `*_access_log.2015-*.txt` wildcard
pattern.

#### Log file polling

This sample shows how to stream Apache access log records as activity events from file which is used for logging at
runtime. File polling technique may be used for any text file. File rolling is also supported.

Sample files can be found in `samples/log-file-polling` directory.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="AccessLogParserCommon" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b %D"/>

        <field name="Location" locator="1" locator-type="Index"/>
        <field name="UserName" locator="3" locator-type="Index"/>
        <field name="StartTime" locator="4" locator-type="Index" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="7" locator-type="Index"/>
        <field name="ResourceName" locator="8" locator-type="Index"/>
        <field name="CompCode" locator="12" locator-type="Index">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="12" locator-type="Index"/>
        <field name="MsgValue" locator="13" locator-type="Index"/>
        <field name="ElapsedTime" locator="14" locator-type="Index" datatype="Number" format="#####0.000" locale="en-US" units="Seconds"/>

    </parser>

    <stream name="SampleFilePollingStream" class="com.jkoolcloud.tnt4j.streams.inputs.FileLineStream">
        <property name="FileName"
                  value="[PATH_TO_LOGS_REPOSITORY]/logs/localhost_access_log.*.txt"/>
        <property name="FilePolling" value="true"/>
        <property name="FileReadDelay" value="20"/>
        <property name="StartFromLatest" value="true"/>
        <parser-ref name="AccessLogParserCommon"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `FilePollingStream` referencing `AccessLogParserCommon` shall be used.

`FileStream` reads data from `access.log` file. `HaltIfNoParser` property states that stream should skip unparseable
entries and don't stop if such situation occurs.

`AccessLogParserCommon` is same as in ['Apache Access log single file'](#apache-access-log-single-file) sample, so refer it for more details.

`FileName` property defines that stream should watch for files matching `localhost_access_log.*.txt` wildcard pattern.
 This is needed to properly handle file rolling.

`FilePolling` property indicates that stream polls files for changes.

`FileReadDelay` property indicates that file changes are streamed every 20 seconds.

`StartFromLatest` property indicates that stream should start from latest entry record in log file. Setting this
property to `false` would stream all log entries starting from oldest file matching wildcard pattern.

**NOTE:** Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### HDFS

These samples shows how to read or poll HDFS files contents. Samples are very similar to ['Log file polling'](#log-file-polling) or
['Apache Access log single file'](#apache-access-log-single-file). Difference is that specialized stream classes are used.

* Simple HDFS file streaming

Sample files can be found in `tnt4j-streams/tnt4j-streams-hdfs/samples/hdfs-file-stream` directory.

```xml
    <stream name="SampleHdfsFileLineStream" class="com.jkoolcloud.tnt4j.streams.inputs.HdfsFileLineStream">
        <property name="FileName" value="hdfs://127.0.0.1:19000/log.txt*"/>
        <.../>
    </stream>
```

To stream HDFS file lines `HdfsFileLineStream` shall be used. `FileName` is defined using URI starting `hdfs://`.

* HDFS file polling

Sample files can be found in `tnt4j-streams/tnt4j-streams-hdfs/samples/hdfs-log-file-polling` directory.

```xml
    <stream name="SampleHdfsFilePollingStream" class="com.jkoolcloud.tnt4j.streams.inputs.HdfsFileLineStream">
        <property name="FileName"
                  value="hdfs://[host]:[port]/[path]/logs/localhost_access_log.*.txt"/>
        <property name="FilePolling" value="true"/>
        <.../>
    </stream>
```

To poll HDFS file `HdfsFileLineStream` shall be used with property `FilePolling` value set to `true`. `FileName` is
defined using URI starting `hdfs://`.

**NOTE:** Stream stops only when critical runtime error/exception occurs or application gets terminated.

* Zipped HDFS file streaming

Sample files can be found in `tnt4j-streams/tnt4j-streams-hdfs/samples/hdfs-zip-stream` directory.

```xml
    <stream name="SampleHdfsZipLineStream" class="com.jkoolcloud.tnt4j.streams.inputs.HdfsZipLineStream">
        <property name="FileName"
                  value="hdfs://[host]:[port]/[path]/sample.zip!2/*.txt"/>
        <property name="ArchType" value="ZIP"/>
        <.../>
    </stream>
```

To stream HDFS zipped file lines `HdfsZipLineStream` shall be used. `FileName` is defined using URI starting `hdfs://`.

#### Apache Flume Raw data

This sample shows how to stream activity events from redirected Apache Flume output Raw data. Apache Flume output is
configured to send Raw output data as JSON to `localhost:9595`. Sample also shows how to use stacked parsers technique
to extract log entry data from JSON envelope.

Sample files can be found in `samples/apache-flume` directory.

How to install plugin into Apache Flume and configure, see [`samples/apache-flume/README.md`](tnt4j-streams-flume-plugin/samples/apache-flume/README.md)

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="AccessLogParserCommon" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b"/>

        <field name="Location" locator="1" locator-type="Index"/>
        <field name="UserName" locator="3" locator-type="Index"/>
        <field name="StartTime" locator="4" locator-type="Index" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="7" locator-type="Index"/>
        <field name="ResourceName" locator="8" locator-type="Index"/>
        <field name="CompCode" locator="12" locator-type="Index">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="12" locator-type="Index"/>
        <field name="MsgValue" locator="13" locator-type="Index"/>
    </parser>

    <parser name="JSONEnvelopeParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJsonParser">
        <property name="ActivityDelim" value="EOL"/>

        <field name="MsgBody" locator="$.body" locator-type="Label">
            <parser-ref name="AccessLogParserCommon"/>
        </field>
        <field name="sinkName" locator="$.sinkName" locator-type="Label"/>
        <field name="chanelName" locator="$.chanelName" locator-type="Label"/>
        <field name="headers" locator="$.headers" locator-type="Label"/>
    </parser>

    <stream name="SampleFlumeStream" class="com.jkoolcloud.tnt4j.streams.inputs.CharacterStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="Port" value="9595"/>
        <parser-ref name="JSONEnvelopeParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `CharacterStream` referencing `JSONEnvelopeParser` shall be used.

`CharacterStream` starts server socket on port defined using `Port` property. `HaltIfNoParser` property indicates that
stream should skip unparseable entries.

`JSONEnvelopeParser` transforms received JSON data package to Map with entries `MsgBody`, `sinkName`, `chanelName` and
`headers`. `MsgBody` entry value is passed to stacked parser named `AccessLogParserCommon`. `ActivityDelim` property
indicates that every line in parsed string represents single JSON data package.

Details on `AccessLogParserCommon` (or `ApacheAccessLogParser` in general) can be found in samples section 
['Apache Access log single file'](#apache-access-log-single-file) and parsers configuration section ['Apache access log parser'](#apache-access-log-parser).

**NOTE:** Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### Apache Flume Parsed Data

This sample shows how to stream activity events from Apache Flume parsed log entry output data. Apache Flume output is
configured to send parsed log entry data as JSON to `localhost:9595`.

Sample files can be found in `samples/apache-flume-parsed` directory.

How to install plugin into Apache Flume and configure, see [`samples/apache-flume-parsed/README.md`](tnt4j-streams-flume-plugin/samples/apache-flume-parsed/README.md)

`messages.json` file contains sample Apache Flume output JSON data package prepared using configuration of this sample.
This sample JSON is for you to see and better understand parsers mappings. Do not use it as Apache Flume input!

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="FlumeJSONParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJsonParser">
        <property name="ActivityDelim" value="EOL"/>

        <field name="Location" locator="$.headers.clientip" locator-type="Label"/>
        <field name="UserName" locator="$.headers.auth" locator-type="Label"/>
        <field name="StartTime" locator="$.headers.logtime" locator-type="Label" format="dd/MMM/yyyy:HH:mm:ss z"
               locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="$.headers.method" locator-type="Label"/>
        <field name="ResourceName" locator="$.headers.param" locator-type="Label"/>
        <field name="CompCode" locator="$.headers.response" locator-type="Label">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="$.headers.response" locator-type="Label"/>
        <field name="MsgValue" locator="$.headers.bytes" locator-type="Label"/>
        <field name="Message" locator="$.body" locator-type="Label"/>
        <field name="Tag" separator=",">
            <field-locator locator="$.sinkName" locator-type="Label"/>
            <field-locator locator="$.chanelName" locator-type="Label"/>
        </field>
    </parser>

    <stream name="SampleFlumeStream" class="com.jkoolcloud.tnt4j.streams.inputs.CharacterStream">
        <property name="Port" value="9595"/>
        <parser-ref name="FlumeJSONParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `CharacterStream` referencing `FlumeJSONParser` shall be used.

`CharacterStream` starts server socket on port defined using `Port` property.

`FlumeJSONParser` transforms received JSON data package to Map entries. Note that some entries like `headers` in map
has inner map as value. Fields of such entries can be accessed defining field name using `.` as field hierarchy
separator. `ActivityDelim` property indicates that every line in parsed string represents single JSON data package.

Details on `AccessLogParserCommon` (or `ApacheAccessLogParser` in general) can be found in samples section 
['Apache Access log single file'](#apache-access-log-single-file) and parsers configuration section ['Apache access log parser'](#apache-access-log-parser).

#### Logstash Raw data

This sample shows how to stream activity events from redirected Logstash output Raw data. Logstash output is
configured to send Raw output data as JSON to `localhost:9595`. Sample also shows how to use stacked parsers technique
to extract log entry data from JSON envelope.

Sample files can be found in `samples/logstash` directory.

How to setup sample environment see [`samples/logstash/README.md`](./tnt4j-streams-core/samples/logstash/README.md)

`messages.json` file contains sample Logstash output JSON data package prepared using configuration of this sample. This
sample JSON is for you to see and better understand parsers mappings. Do not use it as Logstash input!

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="AccessLogParserCommon" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b"/>

        <field name="Location" locator="1" locator-type="Index"/>
        <field name="UserName" locator="3" locator-type="Index"/>
        <field name="StartTime" locator="4" locator-type="Index" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="7" locator-type="Index"/>
        <field name="ResourceName" locator="8" locator-type="Index"/>
        <field name="CompCode" locator="12" locator-type="Index">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="12" locator-type="Index"/>
        <field name="MsgValue" locator="13" locator-type="Index"/>
    </parser>

    <parser name="JSONEnvelopeParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJsonParser">
        <property name="ActivityDelim" value="EOL"/>

        <field name="MsgBody" locator="$.message" locator-type="Label">
            <parser-ref name="AccessLogParserCommon" tags="Normal server,Delayed server"/>
        </field>
        <field name="path" locator="$.path" locator-type="Label"/>
        <field name="Tag" locator="$.tags" locator-type="Label"/>
        <field name="host" locator="$.host" locator-type="Label"/>
    </parser>

    <stream name="SampleLogstashStream" class="com.jkoolcloud.tnt4j.streams.inputs.CharacterStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="RestartOnInputClose" value="true"/>
        <property name="Port" value="9595"/>
        <parser-ref name="JSONEnvelopeParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `CharacterStream` referencing `JSONEnvelopeParser` shall be used.

`CharacterStream` starts server socket on port defined using `Port` property. `HaltIfNoParser` property indicates that
stream should skip unparseable entries.

`JSONEnvelopeParser` transforms received JSON data package to Map with entries `MsgBody`, `path`, `Tag` and `host`.
`MsgBody` entry value is passed to stacked parser named `AccessLogParserCommon`. `ActivityDelim` property indicates that
every line in parsed string represents single JSON data package.

Details on `AccessLogParserCommon` (or `ApacheAccessLogParser` in general) can be found in samples section 
['Apache Access log single file'](#apache-access-log-single-file) and parsers configuration section ['Apache access log parser'](#apache-access-log-parser).

**NOTE:** Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### Logstash parsed data

This sample shows how to stream activity events from parsed by Logstash. Logstash Grok output plugin is configured to
send parsed Apache Access log entry data as JSON to `localhost:9595`.

Sample files can be found in `samples/logstash-parsed` directory.

How to setup sample environment see [`samples/logstash-parsed/README.md`](./tnt4j-streams-core/samples/logstash-parsed/README.md)

`messages.json` file contains sample Logstash output JSON data package prepared using configuration of this sample. This
sample JSON is for you to see and better understand parsers mappings. Do not use it as Logstash input!

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="LogstashJSONParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJsonParser">
        <property name="ActivityDelim" value="EOL"/>

        <field name="Location" locator="$.clientip" locator-type="Label"/>
        <field name="UserName" locator="$.auth" locator-type="Label"/>
        <field name="StartTime" locator="$.timestamp" locator-type="Label" format="dd/MMM/yyyy:HH:mm:ss z"
               locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="$.verb" locator-type="Label"/>
        <field name="ResourceName" locator="$.request" locator-type="Label"/>
        <field name="CompCode" locator="$.response" locator-type="Label">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="$.response" locator-type="Label"/>
        <field name="MsgValue" locator="$.bytes" locator-type="Label"/>
        <field name="Message" locator="$.message" locator-type="Label"/>
        <field name="Tag" locator="$.tags" locator-type="Label"/>
    </parser>

    <stream name="SampleLogstashStream" class="com.jkoolcloud.tnt4j.streams.inputs.CharacterStream">
        <property name="Port" value="9595"/>
        <parser-ref name="LogstashJSONParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `CharacterStream` referencing `LogstashJSONParser` shall be used.

`CharacterStream` starts server socket on port defined using `Port` property.

`LogstashJSONParser` transforms the received JSON data package to a Map data structure and maps map entries to activity event fields using 
map entry key labels. The `ActivityDelim` property with value `EOL` indicates that every line in the parsed stream represents a single JSON 
data package.

#### Elastic Beats provided data

This sample shows how to stream activity events from Elastic Beats. **NOTE**: Elastic Beats environment has to be configured to output data 
to stream started Logstash server host and port, e.g.`localhost:5044`.

Sample files can be found in `samples/elastic-beats` directory.

`dashboards` directory contains exported jKool dashboard dedicated to visualize data for this sample. You can import it into your jKool 
repository.

How to setup Elastic Beats environment see [`samples/elastic-beats/readme.md`](tnt4j-streams-elastic-beats/samples/elastic-beats/readme.md)

`sampleMsg.json` file contains sample Elastic Beats provided Logstash message as JSON data prepared using configuration of this sample. This
sample JSON is for you to see and better understand parsers mappings. Do not use it as Logstash input!

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="BeatsMessageParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <field name="EventType" value="EVENT"/>

        <field name="StartTime">
            <field-locator locator="@timestamp" locator-type="Label" datatype="DateTime" format="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
                           timezone="GMT">
            </field-locator>
        </field>

        <field name="EventName" formattingPattern="{0} {1}">
            <field-locator locator="metricset.module" locator-type="Label"/>
            <field-locator locator="metricset.name" locator-type="Label"/>
        </field>

        <field name="all" locator="*" locator-type="Label"/>
    </parser>

    <stream name="SampleBeatsStream" class="com.jkoolcloud.tnt4j.streams.inputs.ElasticBeatsStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="RestartOnInputClose" value="true"/>

        <!--Binding host-->
        <!--<property name="Host" value="localhost"/>-->
        <property name="Port" value="5044"/>
        <!--Worker properties-->
        <!--<property name="Timeout" value="30"/>-->
        <!--<property name="ThreadCount" value="1"/>-->

        <!--SSL properties-->
        <!--<property name="SSLCertificateFilePath" value="/etc/pki/client/cert.key"/>-->
        <!--<property name="SSLKeyFilePath" value="/etc/pki/client/cert.pem"/>-->
        <!--<property name="PassPhrase" value="pass"/>-->

        <parser-ref name="BeatsMessageParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `ElasticBeatsStream` referencing `BeatsMessageParser` shall be used.

`ElasticBeatsStream` starts Logstash server on port defined using `Port` property. You can also define host for server to bind by using 
`Host` property.

`BeatsMessageParser` takes Map data structure provided by stream and maps map entries to activity event fields using map entry key labels. 
Since there is no particular set of predefined fields defined for Elastic Beats data, in this sample we map them directly into activity 
entity (`EVENT`) properties using locator `*` to have them all in jKool.

#### HTTP request file

This sample shows how to stream activity events received over HTTP request as file. Sample also shows how to use
stacked parsers technique to extract message payload data.

Sample files can be found in `samples/http-file` directory.

Over HTTP sent sample file is `log.txt` - snapshot of Apache access log depicting some HTTP server activity.

How to send file data over HTTP see [`samples/http-file/README.md`](tnt4j-streams-core/samples/http-file/README.md)

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="AccessLogParserCommon" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b"/>

        <field name="Location" locator="1" locator-type="Index"/>
        <field name="UserName" locator="3" locator-type="Index"/>
        <field name="StartTime" locator="4" locator-type="Index" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="7" locator-type="Index"/>
        <field name="ResourceName" locator="8" locator-type="Index"/>
        <field name="CompCode" locator="12" locator-type="Index">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="12" locator-type="Index"/>
        <field name="MsgValue" locator="13" locator-type="Index"/>
    </parser>

    <parser name="SampleHttpReqParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <field name="Transport" locator="ActivityTransport" locator-type="Label"/>
        <field name="MsgBody" locator="ActivityData" locator-type="Label">
            <parser-ref name="AccessLogParserCommon"/>
        </field>
    </parser>

    <stream name="SampleHttpStream" class="com.jkoolcloud.tnt4j.streams.inputs.HttpStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="Port" value="8080"/>
        <!--<property name="UseSSL" value="true"/>-->
        <!--<property name="Keystore" value="path_to_keystore_file"/>-->
        <!--<property name="KeystorePass" value="somePassword"/>-->
        <!--<property name="KeyPass" value="somePassword"/>-->
        <parser-ref name="SampleHttpReqParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `HttpStream` referencing `SampleHttpReqParser` shall be used.

`HttpStream` starts HTTP server on port defined using `Port` property. `HaltIfNoParser` property indicates that stream
should skip unparseable entries. Stream puts received request payload data as `byte[]` to map using key `ActivityData`.

`SampleHttpReqParser` by default converts `byte[]` for entry `ActivityData` to string and uses stacked parser named
`AccessLogParserCommon` to parse format.

`AccessLogParserCommon` is same as in ['Apache Access log single file'](#apache-access-log-single-file) sample, so refer it for more details.

**NOTE:** to parse some other data instead of Apache Access Log, replace `AccessLogParserCommon` with parser which
complies your data format.

**NOTE:** Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### HTTP request form

This sample shows how to stream activity events received over HTTP request as form data.

Sample files can be found in `samples/http-form` directory.

How to send HTTP form data see [`samples/http-form/README.md`](tnt4j-streams-core/samples/http-form/README.md)

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="SampleFormDataParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <field name="Location" locator="clientip" locator-type="Label"/>
        <field name="UserName" locator="auth" locator-type="Label"/>
        <field name="StartTime" locator="timestamp" locator-type="Label" format="dd/MMM/yyyy:HH:mm:ss z"
               locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="verb" locator-type="Label"/>
        <field name="ResourceName" locator="request" locator-type="Label"/>
        <field name="CompCode" locator="response" locator-type="Label">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="response" locator-type="Label"/>
        <field name="MsgValue" locator="bytes" locator-type="Label"/>
    </parser>

    <stream name="SampleHttpStream" class="com.jkoolcloud.tnt4j.streams.inputs.HttpStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="Port" value="8080"/>
        <!--<property name="UseSSL" value="true"/>-->
        <!--<property name="Keystore" value="path_to_keystore_file"/>-->
        <!--<property name="KeystorePass" value="somePassword"/>-->
        <!--<property name="KeyPass" value="somePassword"/>-->
        <parser-ref name="SampleFormDataParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `HttpStream` referencing `SampleFormDataParser` shall be used.

`HttpStream` starts HTTP server on port defined using `Port` property. `HaltIfNoParser` property indicates that stream
should skip unparseable entries. Stream puts received form parameters data to map and passes it to parser.

`SampleFormDataParser` performs form data mapping to TNT4J activity event data using form data parameters name labels.

**NOTE:** Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### JMS text message

This sample shows how to stream activity events received over JMS transport as text messages. Sample also shows
how to use stacked parsers technique to extract message payload data.

Sample files can be found in `samples/jms-textmessage` directory.

**NOTE:** in `run.bat/run.sh` file set variable `JMS_IMPL_LIBPATH` value to reference JMS implementation (`ActiveMQ`, `Solace`, `RabbitMQ` 
and etc.) libraries used by your environment.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

   <parser name="AccessLogParserCommon" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
       <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b"/>

       <field name="Location" locator="1" locator-type="Index"/>
       <field name="UserName" locator="3" locator-type="Index"/>
       <field name="StartTime" locator="4" locator-type="Index" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
       <field name="EventType" value="SEND"/>
       <field name="EventName" locator="7" locator-type="Index"/>
       <field name="ResourceName" locator="8" locator-type="Index"/>
       <field name="CompCode" locator="12" locator-type="Index">
           <field-map source="100:206" target="SUCCESS" type="Range"/>
           <field-map source="300:308" target="WARNING" type="Range"/>
           <field-map source="400:417" target="ERROR" type="Range"/>
           <field-map source="500:511" target="ERROR" type="Range"/>
       </field>
       <field name="ReasonCode" locator="12" locator-type="Index"/>
       <field name="MsgValue" locator="13" locator-type="Index"/>
   </parser>

   <parser name="SampleJMSParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJMSMessageParser">
       <field name="Transport" locator="ActivityTransport" locator-type="Label"/>
       <field name="MsgBody" locator="ActivityData" locator-type="Label">
           <parser-ref name="AccessLogParserCommon"/>
       </field>

       <!-- mapping common message metadata fields one by one -->
       <field name="Correlator" locator="MsgMetadata.Correlator" locator-type="Label"/>
       <field name="CorrelatorBytes" locator="MsgMetadata.CorrelatorBytes" locator-type="Label"/>
       <field name="DeliveryMode" locator="MsgMetadata.DeliveryMode" locator-type="Label"/>
       <field name="Destination" locator="MsgMetadata.Destination" locator-type="Label"/>
       <field name="Expiration" locator="MsgMetadata.Expiration" locator-type="Label"/>
       <field name="MessageId" locator="MsgMetadata.MessageId" locator-type="Label"/>
       <field name="Priority" locator="MsgMetadata.Priority" locator-type="Label"/>
       <field name="Redelivered" locator="MsgMetadata.Redelivered" locator-type="Label"/>
       <field name="ReplyTo" locator="MsgMetadata.ReplyTo" locator-type="Label"/>
       <field name="Timestamp" locator="MsgMetadata.Timestamp" locator-type="Label"/>
       <field name="Type" locator="MsgMetadata.Type" locator-type="Label"/>
       <!-- automatically puts all unmapped message metadata map entries as custom activity properties -->
       <field name="AllRestMsgMetadataProps" locator="MsgMetadata.#" locator-type="Label"/>
       <!-- automatically puts all resolved custom message properties map entries as custom activity properties -->
       <field name="CustomMsgProps" locator="MsgMetadata.CustomMsgProps" locator-type="Label"/>
       <!-- or mapping (some) custom message properties one by one-->
       <!--<field name="CustomProp1" locator="MsgMetadata.CustomMsgProps.Property1" locator-type="Label"/>-->
       <!--<field name="CustomProp2" locator="MsgMetadata.CustomMsgProps.Property2" locator-type="Label"/>-->
       <!--<field name="CustomProp3" locator="MsgMetadata.CustomMsgProps.Property3" locator-type="Label"/>-->
       <!-- and all what is left unmapped, map automatically -->
       <!--<field name="AllRestCustomMsgProps" locator="MsgMetadata.CustomMsgProps.#" locator-type="Label"/>-->

       <!-- mapping message metadata fields as map entries-->
       <!-- automatically puts all resolved map entries as custom activity properties -->
       <!--<field name="MsgMetadata" locator="MsgMetadata" locator-type="Label"/>-->
   </parser>

   <stream name="SampleJMStream" class="com.jkoolcloud.tnt4j.streams.inputs.JMSStream">
       <property name="HaltIfNoParser" value="false"/>
       <property name="java.naming.provider.url" value="tcp://localhost:61616"/>
       <property name="java.naming.factory.initial" value="org.apache.activemq.jndi.ActiveMQInitialContextFactory"/>
       <!--<property name="java.naming.security.username" value="[YOUR_USERNAME]"/>-->
       <!--<property name="java.naming.security.principal" value="[YOUR_PRINCIPAL]"/>-->
       <!--<property name="java.naming.security.credentials" value="[YOUR_PASSWORD]"/>-->
       <!--<property name="Queue" value="queue.SampleJMSQueue"/>-->
       <property name="Topic" value="TestTopic"/>
       <property name="JMSConnFactory" value="/jms/cf/another"/>

       <parser-ref name="SampleJMSParser"/>
   </stream>
</tnt-data-source>
```

Stream configuration states that `JMSStream` referencing `SampleJMSParser` shall be used.

`JMSStream` connects to server defined using `java.naming.provider.url` property, and takes messages from topic defined
`Topic` property. To define wanted queue use `Queue` property. `HaltIfNoParser` property indicates that stream
should skip unparseable entries. `java.naming.factory.initial` property defines that ActiveMQ shall be used.
Stream puts received message data to map and passes it to parser.

`SampleJMSParser` maps metadata to activity event data. `ActivityData` entry value is passed to stacked parser named
`AccessLogParserCommon`.

Details on `AccessLogParserCommon` (or `ApacheAccessLogParser` in general) can be found in samples section 
['Apache Access log single file'](#apache-access-log-single-file) and parser configuration section ['Apache access log parser'](#apache-access-log-parser).

**NOTE:** to parse some other data instead of Apache Access Log, replace `AccessLogParserCommon` with parser which
complies your data format.

**NOTE:** Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### JMS map message

This sample shows how to stream activity events received over JMS transport as map messages.

Sample files can be found in `samples/jms-mapmessage` directory.

**NOTE:** in `run.bat/run.sh` file set variable `JMS_IMPL_LIBPATH` value to reference JMS implementation (`ActiveMQ`, `Solace`, `RabbitMQ` 
and etc.) libraries used by your environment.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

     <parser name="SampleJMSParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJMSMessageParser">
        <field name="Transport" locator="ActivityTransport" locator-type="Label"/>
        <field name="Location" locator="clientip" locator-type="Label"/>
        <field name="UserName" locator="auth" locator-type="Label"/>
        <field name="StartTime" locator="timestamp" locator-type="Label" format="dd/MMM/yyyy:HH:mm:ss z"
               locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="verb" locator-type="Label"/>
        <field name="ResourceName" locator="request" locator-type="Label"/>
        <field name="CompCode" locator="response" locator-type="Label">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="response" locator-type="Label"/>
        <field name="MsgValue" locator="bytes" locator-type="Label"/>

        <!-- mapping common message metadata fields one by one -->
        <field name="Correlator" locator="MsgMetadata.Correlator" locator-type="Label"/>
        <field name="CorrelatorBytes" locator="MsgMetadata.CorrelatorBytes" locator-type="Label"/>
        <field name="DeliveryMode" locator="MsgMetadata.DeliveryMode" locator-type="Label"/>
        <field name="Destination" locator="MsgMetadata.Destination" locator-type="Label"/>
        <field name="Expiration" locator="MsgMetadata.Expiration" locator-type="Label"/>
        <field name="MessageId" locator="MsgMetadata.MessageId" locator-type="Label"/>
        <field name="Priority" locator="MsgMetadata.Priority" locator-type="Label"/>
        <field name="Redelivered" locator="MsgMetadata.Redelivered" locator-type="Label"/>
        <field name="ReplyTo" locator="MsgMetadata.ReplyTo" locator-type="Label"/>
        <field name="Timestamp" locator="MsgMetadata.Timestamp" locator-type="Label"/>
        <field name="Type" locator="MsgMetadata.Type" locator-type="Label"/>
        <!-- automatically puts all unmapped message metadata map entries as custom activity properties -->
        <field name="AllRestMsgMetadataProps" locator="MsgMetadata.#" locator-type="Label"/>
        <!-- automatically puts all resolved custom message properties map entries as custom activity properties -->
        <field name="CustomMsgProps" locator="MsgMetadata.CustomMsgProps" locator-type="Label"/>
        <!-- or mapping (some) custom message properties one by one-->
        <!--<field name="CustomProp1" locator="MsgMetadata.CustomMsgProps.Property1" locator-type="Label"/>-->
        <!--<field name="CustomProp2" locator="MsgMetadata.CustomMsgProps.Property2" locator-type="Label"/>-->
        <!--<field name="CustomProp3" locator="MsgMetadata.CustomMsgProps.Property3" locator-type="Label"/>-->
        <!-- and all what is left unmapped, map automatically -->
        <!--<field name="AllRestCustomMsgProps" locator="MsgMetadata.CustomMsgProps.#" locator-type="Label"/>-->

        <!-- mapping message metadata fields as map entries-->
        <!-- automatically puts all resolved map entries as custom activity properties -->
        <!--<field name="MsgMetadata" locator="MsgMetadata" locator-type="Label"/>-->
    </parser>

    <stream name="SampleJMStream" class="com.jkoolcloud.tnt4j.streams.inputs.JMSStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="java.naming.provider.url" value="tcp://localhost:61616"/>
        <property name="java.naming.factory.initial" value="org.apache.activemq.jndi.ActiveMQInitialContextFactory"/>
        <!--<property name="java.naming.security.username" value="[YOUR_USERNAME]"/>-->
        <!--<property name="java.naming.security.principal" value="[YOUR_PRINCIPAL]"/>-->
        <!--<property name="java.naming.security.credentials" value="[YOUR_PASSWORD]"/>-->
        <!--<property name="Queue" value="queue.SampleJMSQueue"/>-->
        <property name="Topic" value="TestTopic"/>
        <property name="JMSConnFactory" value="/jms/cf/another"/>

        <parser-ref name="SampleJMSParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `JMSStream` referencing `SampleJMSParser` shall be used.

`JMSStream` connects to server defined using `java.naming.provider.url` property, and takes messages from topic defined
`Topic` property. To define wanted queue use `Queue` property. `HaltIfNoParser` property indicates that stream
should skip unparseable entries. `java.naming.factory.initial` property defines that ActiveMQ shall be used.
Stream puts received message data to map and passes it to parser.

`SampleJMSParser` maps activity event data from JMS map message using map entries key labels.

**NOTE:** Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### JMS object message

This sample shows how to stream activity events received over JMS transport as serializable object messages. Sample
also shows how to use stacked parsers technique to extract message payload data.

Sample files can be found in `samples/jms-objectmessage` directory.

**NOTE:** in `run.bat/run.sh` file set variable `JMS_IMPL_LIBPATH` value to reference JMS implementation (`ActiveMQ`, `Solace`, `RabbitMQ` 
and etc.) libraries used by your environment.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="SampleObjectParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJavaObjectParser">
        <field name="Location" locator="clientip" locator-type="Label"/>
        <field name="UserName" locator="auth" locator-type="Label"/>
        <field name="StartTime" locator="timestamp" locator-type="Label" format="dd/MMM/yyyy:HH:mm:ss z"
               locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="verb" locator-type="Label"/>
        <field name="ResourceName" locator="request" locator-type="Label"/>
        <field name="CompCode" locator="response" locator-type="Label">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="response" locator-type="Label"/>
        <field name="MsgValue" locator="bytes" locator-type="Label"/>
    </parser>

    <parser name="SampleJMSParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJMSMessageParser">
        <field name="Transport" locator="ActivityTransport" locator-type="Label"/>
        <field name="MsgBody" locator="ActivityData" locator-type="Label">
            <parser-ref name="SampleObjectParser"/>
        </field>

        <!-- mapping common message metadata fields one by one -->
        <field name="Correlator" locator="MsgMetadata.Correlator" locator-type="Label"/>
        <field name="CorrelatorBytes" locator="MsgMetadata.CorrelatorBytes" locator-type="Label"/>
        <field name="DeliveryMode" locator="MsgMetadata.DeliveryMode" locator-type="Label"/>
        <field name="Destination" locator="MsgMetadata.Destination" locator-type="Label"/>
        <field name="Expiration" locator="MsgMetadata.Expiration" locator-type="Label"/>
        <field name="MessageId" locator="MsgMetadata.MessageId" locator-type="Label"/>
        <field name="Priority" locator="MsgMetadata.Priority" locator-type="Label"/>
        <field name="Redelivered" locator="MsgMetadata.Redelivered" locator-type="Label"/>
        <field name="ReplyTo" locator="MsgMetadata.ReplyTo" locator-type="Label"/>
        <field name="Timestamp" locator="MsgMetadata.Timestamp" locator-type="Label"/>
        <field name="Type" locator="MsgMetadata.Type" locator-type="Label"/>
        <!-- automatically puts all unmapped message metadata map entries as custom activity properties -->
        <field name="AllRestMsgMetadataProps" locator="MsgMetadata.#" locator-type="Label"/>
        <!-- automatically puts all resolved custom message properties map entries as custom activity properties -->
        <field name="CustomMsgProps" locator="MsgMetadata.CustomMsgProps" locator-type="Label"/>
        <!-- or mapping (some) custom message properties one by one-->
        <!--<field name="CustomProp1" locator="MsgMetadata.CustomMsgProps.Property1" locator-type="Label"/>-->
        <!--<field name="CustomProp2" locator="MsgMetadata.CustomMsgProps.Property2" locator-type="Label"/>-->
        <!--<field name="CustomProp3" locator="MsgMetadata.CustomMsgProps.Property3" locator-type="Label"/>-->
        <!-- and all what is left unmapped, map automatically -->
        <!--<field name="AllRestCustomMsgProps" locator="MsgMetadata.CustomMsgProps.#" locator-type="Label"/>-->

        <!-- mapping message metadata fields as map entries-->
        <!-- automatically puts all resolved map entries as custom activity properties -->
        <!--<field name="MsgMetadata" locator="MsgMetadata" locator-type="Label"/>-->
    </parser>

    <stream name="SampleJMStream" class="com.jkoolcloud.tnt4j.streams.inputs.JMSStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="java.naming.provider.url" value="tcp://localhost:61616"/>
        <property name="java.naming.factory.initial" value="org.apache.activemq.jndi.ActiveMQInitialContextFactory"/>
        <!--<property name="java.naming.security.username" value="[YOUR_USERNAME]"/>-->
        <!--<property name="java.naming.security.principal" value="[YOUR_PRINCIPAL]"/>-->
        <!--<property name="java.naming.security.credentials" value="[YOUR_PASSWORD]"/>-->
        <!--<property name="Queue" value="queue.SampleJMSQueue"/>-->
        <property name="Topic" value="TestTopic"/>
        <property name="JMSConnFactory" value="/jms/cf/another"/>

        <parser-ref name="SampleJMSParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `JMSStream` referencing `SampleJMSParser` shall be used.

`JMSStream` connects to server defined using `java.naming.provider.url` property, and takes messages from topic defined
`Topic` property. To define wanted queue use `Queue` property. `HaltIfNoParser` property indicates that stream
should skip unparseable entries. `java.naming.factory.initial` property defines that ActiveMQ shall be used.
Stream puts received message data to map and passes it to parser.

`SampleJMSParser` maps metadata to activity event data. `ActivityData` entry value is passed to stacked parser named
`SampleObjectParser`.

`SampleObjectParser` is able to map activity event fields values from serialized object declared fields using field
names as labels.

**NOTE:** Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### Kafka client stream

This sample shows how to stream activity events received over Apache Kafka transport as messages. Sample also shows
how to use stacked parsers technique to extract message payload data.

Stream runs as Apache Kafka consumer.

Sample files can be found in `samples/kafka` directory.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="AccessLogParserCommon" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b"/>

        <field name="Location" locator="1" locator-type="Index"/>
        <field name="UserName" locator="3" locator-type="Index"/>
        <field name="StartTime" locator="4" locator-type="Index" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="7" locator-type="Index"/>
        <field name="ResourceName" locator="8" locator-type="Index"/>
        <field name="CompCode" locator="12" locator-type="Index">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="12" locator-type="Index"/>
        <field name="MsgValue" locator="13" locator-type="Index"/>
    </parser>

    <parser name="KafkaMessageParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <field name="Topic" locator="ActivityTopic" locator-type="Label"/>
        <field name="Transport" locator="ActivityTransport" locator-type="Label"/>
        <field name="MsgBody" locator="ActivityData" locator-type="Label">
            <parser-ref name="AccessLogParserCommon"/>
        </field>
    </parser>

    <stream name="SampleKafkaStream" class="com.jkoolcloud.tnt4j.streams.inputs.KafkaStream">
            <property name="HaltIfNoParser" value="false"/>
            <property name="Topic" value="TNT4JStreams"/>

            <!-- Kafka consumer properties -->
            <property name="zookeeper.connect" value="127.0.0.1:2181/tnt4j_kafka"/>
            <property name="group.id" value="0"/>

            <parser-ref name="KafkaMessageParser"/>
        </stream>
</tnt-data-source>

```

Stream configuration states that `KafkaStream` referencing `KafkaMessageParser` shall be used.

`KafkaStream` connects to server defined using `zookeeper.connect` property, and takes messages from topic defined
`Topic` property. `HaltIfNoParser` property indicates that stream should skip unparseable entries.
Stream puts received message data to map and passes it to parser.

Details on ['Apache Kafka configuration'](https://kafka.apache.org/08/configuration.html).

`KafkaMessageParser` maps metadata to activity event data. `ActivityData` entry value is passed to stacked parser named
`AccessLogParserCommon`.

Details on `AccessLogParserCommon` (or `ApacheAccessLogParser` in general) can be found in samples section 
['Apache Access log single file'](#apache-access-log-single-file) and parsers configuration section ['Apache access log parser'](#apache-access-log-parser).

**NOTE:** to parse some other data instead of Apache Access Log, replace `AccessLogParserCommon` with parser which
complies your data format.

**NOTE:** Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### Kafka server stream

This sample shows how to stream activity events received over Apache Kafka transport as messages. Sample also shows
how to use stacked parsers technique to extract message payload data.

Stream starts Apache Kafka server and binds consumer to it to receive and process messages.

Sample files can be found in `samples/kafka-server` directory.

`tnt4j-streams-kafka/config` directory contains default configuration properties files for ZooKeeper (`zookeeper.propertoes`) and Kafka 
(`kafka-server.properties`) servers. Kafka stream is using those files referenced over Java system properties 
`-Dtnt4j.zookeeper.config=tnt4j-streams-kafka/config/zookeeper.properties` for ZooKeeper server and 
`-Dtnt4j.kafka.srv.config=tnt4j-streams-kafka/config/kafka-server.properties` for Kafka server (see `samples/kafka-server/run.bat` or `.sh` 
files as sample). **NOTE:** those file defined Kafka server properties gets merged with ones defined in stream configuration.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="AccessLogParserCommon" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b"/>

        <field name="Location" locator="1" locator-type="Index"/>
        <field name="UserName" locator="3" locator-type="Index"/>
        <field name="StartTime" locator="4" locator-type="Index" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="7" locator-type="Index"/>
        <field name="ResourceName" locator="8" locator-type="Index"/>
        <field name="CompCode" locator="12" locator-type="Index">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="12" locator-type="Index"/>
        <field name="MsgValue" locator="13" locator-type="Index"/>
    </parser>

    <parser name="KafkaMessageParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <field name="Topic" locator="ActivityTopic" locator-type="Label"/>
        <field name="Transport" locator="ActivityTransport" locator-type="Label"/>
        <field name="MsgBody" locator="ActivityData" locator-type="Label">
            <parser-ref name="AccessLogParserCommon"/>
        </field>
    </parser>

    <stream name="SampleKafkaStream" class="com.jkoolcloud.tnt4j.streams.inputs.KafkaStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="Topic" value="TNT4JStreams"/>
        <property name="StartServer" value="true"/>
        <property name="StartZooKeeper" value="true"/>

        <!-- Common Kafka server/consumer properties -->
        <property name="zookeeper.connect" value="127.0.0.1:2181/tnt4j_kafka"/>
        <property name="zookeeper.connection.timeout.ms" value="5000"/>

        <!-- Kafka consumer properties -->
        <property name="consumer:group.id" value="0"/>
        <!--<property name="consumer:zookeeper.connection.timeout.ms" value="6000"/>-->
        <property name="consumer:consumer.timeout.ms" value="1000"/>
        <property name="consumer:auto.commit.interval.ms" value="1000"/>

        <!-- Kafka server properties -->
        <property name="server:broker.id" value="684231"/>
        <property name="server:host.name" value="localhost"/>
        <property name="server:port" value="9092"/>
        <!--<property name="server:log.flush.interval.messages" value="1"/>-->
        <!--<property name="server:log.flush.interval.ms" value="1000"/>-->
        <property name="server:zookeeper.session.timeout.ms" value="4000"/>
        <!--<property name="server:zookeeper.connection.timeout.ms" value="3000"/>-->
        <property name="server:zookeeper.sync.time.ms" value="200"/>

        <parser-ref name="KafkaMessageParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration in general is mostly same as for ['Kafka client stream'](#kafka-client-stream).

Difference is `StartServer` stream property set to `true` stating that Apache Kafka server should be started by stream and messages consumer 
has to bind to it. `StartZooKeeper` stream property set to `true` states that ZooKeeper server shall be started on stream startup.

Also there is extended set of Apache Kafka configuration properties used by server and consumer. Kafka server dedicated properties starts 
with prefix `server:` while Kafka consumer properties - with `consumer:`. Properties having no prefix (or prefixes `:`, `common:`) are 
applied for both - server and consumer. Those properties gets merged with ones defined in properties files. **NOTE:** Stream configuration 
defined properties has priority over property files defined properties - thus property files defines **default** values used by stream.

Details on ['Apache Kafka configuration'](https://kafka.apache.org/08/configuration.html).

**NOTE:** Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### MQTT

This sample shows how to stream activity events received over MQTT transport as MQTT messages. Sample also shows how to
use stacked parsers technique to extract message payload data.

Sample files can be found in `samples/mqtt` directory.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="AccessLogParserCommon" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b"/>

        <field name="Location" locator="1" locator-type="Index"/>
        <field name="UserName" locator="3" locator-type="Index"/>
        <field name="StartTime" locator="4" locator-type="Index" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="7" locator-type="Index"/>
        <field name="ResourceName" locator="8" locator-type="Index"/>
        <field name="CompCode" locator="12" locator-type="Index">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="12" locator-type="Index"/>
        <field name="MsgValue" locator="13" locator-type="Index"/>
    </parser>

    <parser name="MqttMessageParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <field name="Topic" locator="ActivityTopic" locator-type="Label"/>
        <field name="Transport" locator="ActivityTransport" locator-type="Label"/>
        <field name="MsgBody" locator="ActivityData" locator-type="Label">
            <parser-ref name="AccessLogParserCommon"/>
        </field>
    </parser>

    <stream name="SampleMQTTStream" class="com.jkoolcloud.tnt4j.streams.inputs.MqttStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="ServerURI" value="tcp://localhost:1883"/>
        <property name="TopicString" value="TNT4JStreams"/>
        <!--<property name="UserName" value="someUser"/>-->
        <!--<property name="Password" value="somePassword"/>-->
        <!--<property name="UseSSL" value="true"/>-->
        <!--<property name="Keystore" value="path_to_keystore_file"/>-->
        <!--<property name="KeystorePass" value="somePassword"/>-->

        <parser-ref name="MqttMessageParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `MqttStream` referencing `MqttMessageParser` shall be used.

`MqttStream` connects to server defined using `ServerURI` property, and takes messages from topic defined
`TopicString` property. `HaltIfNoParser` property indicates that stream should skip unparseable entries.
Stream puts received message data to map and passes it to parser.

`MqttMessageParser` maps metadata to activity event data. `ActivityData` entry value is passed to stacked parser named
`AccessLogParserCommon`.

Details on `AccessLogParserCommon` (or `ApacheAccessLogParser` in general) can be found in samples section 
['Apache Access log single file'](#apache-access-log-single-file) and parsers configuration section ['Apache access log parser'](#apache-access-log-parser).

**NOTE:** to parse some other data instead of Apache Access Log, replace `AccessLogParserCommon` with parser which
complies your data format.

**NOTE:** Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### WMQ Message broker

This sample shows how to stream activity events received over WMQ as message broker event messages.

Sample files can be found in `samples/message-broker` directory.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <java-object name="MQMsgToStringPreParser" class="com.jkoolcloud.tnt4j.streams.preparsers.MQMessageToStringPreParser"/>

    <parser name="EventParser" class="com.jkoolcloud.tnt4j.streams.parsers.MessageActivityXmlParser">
        <property name="SignatureDelim" value="#!#"/>
        <property name="Namespace"
                  value="wmb=http://www.ibm.com/xmlns/prod/websphere/messagebroker/6.1.0/monitoring/event"/>

        <reference name="MQMsgToStringPreParser"/>

        <!--field name="ServerName" value="host-name-for-broker"/--> <!-- defaults to host name where jKool LLC TNT4J-Streams is running -->
        <!--field name="ServerName" locator="/wmb:event/wmb:eventPointData/wmb:messageFlowData/wmb:broker/@wmb:hostName" locator-type="Label"/--> <!-- when broker supports this -->
        <!--field name="ServerIp" locator="/wmb:event/wmb:eventPointData/ServerIp" locator-type="Label"/-->
        <field name="ApplName"
               locator="/wmb:event/wmb:eventPointData/wmb:messageFlowData/wmb:messageFlow/@wmb:uniqueFlowName"
               locator-type="Label"/>
        <field name="ResourceName" locator="/wmb:event/wmb:eventPointData/wmb:messageFlowData/wmb:node/@wmb:nodeLabel"
               locator-type="Label"/>
        <field name="Message" locator="/wmb:event/wmb:bitstreamData/wmb:bitstream" locator-type="Label"
               datatype="Binary"
               format="base64Binary"/>
        <field name="EventName" locator="/wmb:event/wmb:eventPointData/wmb:eventData/wmb:eventIdentity/@wmb:eventName"
               locator-type="Label"/>
        <field name="EventType" locator="/wmb:event/wmb:eventPointData/wmb:messageFlowData/wmb:node/@wmb:nodeType"
               locator-type="Label">
            <field-map source="ComIbmMQInputNode" target="RECEIVE"/>
            <field-map source="ComIbmMQOutputNode" target="SEND"/>
            <field-map source="ComIbmMQGetNode" target="RECEIVE"/>
            <field-map source="ComIbmJMSClientInputNode" target="RECEIVE"/>
            <field-map source="ComIbmJMSClientOutputNode" target="SEND"/>
            <field-map source="ComIbmJMSClientReplyNode" target="SEND"/>
            <field-map source="ComIbmJMSClientReceive" target="RECEIVE"/>
            <field-map source="ComIbmJMSHeader.msgnode" target="RECEIVE"/>
            <field-map source="ComIbmHTTPAsyncRequest" target="RECEIVE"/>
            <field-map source="ComIbmHTTPAsyncResponse" target="SEND"/>
            <field-map source="ComIbmHTTPHeader" target="RECEIVE"/>
            <field-map source="ComIbmWSInputNode" target="RECEIVE"/>
            <field-map source="ComIbmWSReplyNode" target="SEND"/>
            <field-map source="ComIbmWSRequestNode" target="RECEIVE"/>
            <field-map source="ComIbmSOAPInputNode" target="RECEIVE"/>
            <field-map source="ComIbmSOAPReplyNode" target="SEND"/>
            <field-map source="ComIbmSOAPRequestNode" target="RECEIVE"/>
            <field-map source="ComIbmSOAPAsyncRequestNode" target="RECEIVE"/>
            <field-map source="ComIbmSOAPAsyncResponseNode" target="SEND"/>
            <field-map source="ComIbmSOAPWrapperNode" target="CALL"/>
            <field-map source="ComIbmSOAPExtractNode" target="CALL"/>
            <field-map source="SRRetrieveEntityNode" target="CALL"/>
            <field-map source="SRRetrieveITServiceNode" target="CALL"/>
            <field-map source="ComIbmDatabaseInputNode" target="RECEIVE"/>
            <field-map source="ComIbmDatabaseNode" target="CALL"/>
            <field-map source="ComIbmDatabaseRetrieveNode" target="RECEIVE"/>
            <field-map source="ComIbmDatabaseRouteNode" target="SEND"/>
            <field-map source="ComIbmFileInputNode" target="RECEIVE"/>
            <field-map source="ComIbmFileReadNode" target="CALL"/>
            <field-map source="ComIbmFileOutputNode" target="SEND"/>
            <field-map source="ComIbmFTEInputNode" target="RECEIVE"/>
            <field-map source="ComIbmFTEOutputNode" target="SEND"/>
            <field-map source="ComIbmTCPIPClientInputNode" target="RECEIVE"/>
            <field-map source="ComIbmTCPIPClientOutputNode" target="SEND"/>
            <field-map source="ComIbmTCPIPClientRequestNode" target="RECEIVE"/>
            <field-map source="ComIbmTCPIPServerInputNode" target="RECEIVE"/>
            <field-map source="ComIbmTCPIPServerOutputNode" target="SEND"/>
            <field-map source="ComIbmTCPIPServerRequestNode" target="RECEIVE"/>
            <field-map source="ComIbmCORBARequestNode" target="RECEIVE"/>
            <field-map source="" target="CALL"/>
        </field>
        <field name="Correlator"
               locator="/wmb:event/wmb:eventPointData/wmb:eventData/wmb:eventCorrelation/@wmb:localTransactionId"
               locator-type="Label"/>
        <field name="ElapsedTime" value="0" datatype="Number"/>
        <field name="EndTime" locator="/wmb:event/wmb:eventPointData/wmb:eventData/wmb:eventSequence/@wmb:creationTime"
               locator-type="Label"
               datatype="DateTime" format="yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'" timezone="GMT"/>
        <!--field name="ReasonCode" locator="/wmb:event/wmb:eventPointData/ReasonCode" locator-type="Label" datatype="Number"/-->
        <!-- *** Use following signature definition for WMQ messages ***
        <field name="TrackingId" separator="#!#" value-type="signature">
          <field-locator locator="/wmb:event/wmb:applicationData/wmb:simpleContent[@wmb:name='MsgType']/@wmb:value" locator-type="Label" datatype="Number"/>
          <field-locator locator="/wmb:event/wmb:applicationData/wmb:simpleContent[@wmb:name='Format']/@wmb:value" locator-type="Label"/>
          <field-locator locator="/wmb:event/wmb:applicationData/wmb:simpleContent[@wmb:name='MsgId']/@wmb:value" locator-type="Label" datatype="Binary" format="hexBinary"/>
          <field-locator locator="/wmb:event/wmb:applicationData/wmb:simpleContent[@wmb:name='UserIdentifier']/@wmb:value" locator-type="Label">
              <field-transform name="UserIdLowerCase" lang="groovy">
                  StringUtils.lowerCase($fieldValue)
              </field-transform>
          </field-locator>
          <field-locator locator="/wmb:event/wmb:applicationData/wmb:simpleContent[@wmb:name='PutApplType']/@wmb:value" locator-type="Label"/>
          <field-locator locator="/wmb:event/wmb:applicationData/wmb:simpleContent[@wmb:name='PutApplName']/@wmb:value" locator-type="Label"/>
          <field-locator locator="/wmb:event/wmb:applicationData/wmb:simpleContent[@wmb:name='PutDate']/@wmb:value" locator-type="Label"/>
          <field-locator locator="/wmb:event/wmb:applicationData/wmb:simpleContent[@wmb:name='PutTime']/@wmb:value" locator-type="Label"/>
        </field>
        -->
        <!--field name="StartTime" locator="/wmb:event/wmb:eventPointData/wmb:eventData/wmb:eventSequence/@wmb:creationTime" locator-type="Label" datatype="DateTime" format="yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'" timestamp="GMT"/-->
        <field name="CompCode" locator="/wmb:event/wmb:eventPointData/wmb:eventData/wmb:eventIdentity/@wmb:eventName"
               locator-type="Label">
            <field-map source="FlowRollback" target="ERROR"/>
            <field-map source="" target="SUCCESS"/>
        </field>
        <!--field name="Tag" locator="/wmb:event/wmb:eventPointData/Tag" locator-type="Label"/-->
        <!--field name="UserName" locator="/wmb:event/wmb:eventPointData/UserName" locator-type="Label"/-->
    </parser>

    <stream name="EventStream" class="com.jkoolcloud.tnt4j.streams.inputs.WmqStream">
        <property name="QueueManager" value="QMGR"/>
        <property name="Queue" value="EVENT.QUEUE"/>
        <parser-ref name="EventParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `WmqStream` referencing `EventParser` shall be used. Stream de-serialize message to
string and passes it to parser.

`QueueManager` property defines name of queue manager and `Queue` property defines name of queue to get messages.

`EventParser` is of type `MessageActivityXmlParser` meaning that it will parse messages de-serialized into XML strings.

`SignatureDelim` property defines that `#!#` should be used as signature delimiter.

`Namespace` property adds `wmb` namespace definition mapping to mapping
`http://www.ibm.com/xmlns/prod/websphere/messagebroker/6.1.0/monitoring/event`.

#### WMQ PCF messages streaming

This sample shows how to stream activity events received over WMQ as PCF messages.

Sample files can be found in `samples/pcf-message` directory.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/tnt4j-streams-wmq/config/tnt-data-source-wmq_pcf.xsd">

    <parser name="PCFEventsParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityPCFParser">
        <property name="TranslateNumValues" value="true"/>

        <field name="EventType" value="EVENT"/>

        <!-- header fields -->
        <field name="Command" locator="MQCFH.Command" locator-type="Label"/>
        <field name="MsgSeqNumber" locator="MQCFH.MsgSeqNumber" locator-type="Label"/>
        <field name="Control" locator="MQCFH.Control" locator-type="Label"/>
        <field name="CompCode" locator="MQCFH.CompCode" locator-type="Label">
            <field-map source="0" target="SUCCESS"/>
            <field-map source="1" target="WARNING"/>
            <field-map source="MQCC_OK" target="SUCCESS"/>
            <field-map source="MQCC_WARNING" target="WARNING"/>
            <field-map source="" target="ERROR"/>
        </field>
        <field name="ReasonCode" locator="MQCFH.Reason" locator-type="Label" datatype="Number"/>
        <field name="ParameterCount" locator="MQCFH.ParameterCount" locator-type="Label"/>

        <!-- message fields -->
        <field name="QMgrName" locator="MQCA_Q_MGR_NAME" locator-type="Label"/>
        <field name="ServerName" locator="MQCACF_HOST_NAME" locator-type="Label"/>
        <field name="StartTime" separator=" " datatype="DateTime" format="yyyy-MM-dd HH:mm:ss">
            <field-locator locator="MQCAMO_START_DATE" locator-type="Label"/>
            <field-locator locator="MQCAMO_START_TIME" locator-type="Label"/>
        </field>
        <field name="EndTime" separator=" " datatype="DateTime" format="yyyy-MM-dd HH:mm:ss">
            <field-locator locator="MQCAMO_END_DATE" locator-type="Label"/>
            <field-locator locator="MQCAMO_END_TIME" locator-type="Label"/>
        </field>
        <field name="CommandLevel" locator="MQIA_COMMAND_LEVEL" locator-type="Label"/>
        <field name="SequenceNumber" locator="MQIACF_SEQUENCE_NUMBER" locator-type="Label"/>
        <field name="ApplName" locator="MQCACF_APPL_NAME" locator-type="Label"/>
        <field name="ApplType" locator="MQIA_APPL_TYPE" locator-type="Label"/>
        <field name="ProcessId" locator="MQIACF_PROCESS_ID" locator-type="Label"/>
        <field name="UserId" locator="MQCACF_USER_IDENTIFIER" locator-type="Label"/>
        <field name="ApiCallerType" locator="MQIACF_API_CALLER_TYPE" locator-type="Label"/>
        <field name="ApiEnv" locator="MQIACF_API_ENVIRONMENT" locator-type="Label"/>
        <field name="ApplFunction" locator="MQCACF_APPL_FUNCTION" locator-type="Label"/>
        <field name="ApplFunctionType" locator="MQIACF_APPL_FUNCTION_TYPE" locator-type="Label"/>
        <field name="TraceDetail" locator="MQIACF_TRACE_DETAIL" locator-type="Label"/>
        <field name="TraceDataLength" locator="MQIACF_TRACE_DATA_LENGTH" locator-type="Label"/>
        <field name="PointerSize" locator="MQIACF_POINTER_SIZE" locator-type="Label"/>
        <field name="Platform" locator="MQIA_PLATFORM" locator-type="Label"/>
        <field name="Correlator" locator="MQBACF_CORREL_ID" locator-type="Label" datatype="Binary"/>
    </parser>

    <stream name="WmqStreamPCF" class="com.jkoolcloud.tnt4j.streams.inputs.WmqStreamPCF">
        <property name="Host" value="[YOUR_MQ_HOST]"/>
        <property name="Port" value="[YOUR_MQ_PORT]"/>
        <property name="QueueManager" value="[YOUR_QM_NAME]"/>
        <property name="Queue" value="[YOUR_Q_NAME]"/>
        <property name="StripHeaders" value="false"/>

        <parser-ref name="PCFEventsParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `WmqStreamPCF` referencing `PCFEventsParser` shall be used. Stream takes MQ message from QM, transforms it 
to PCF message and passes it to parser.

`Host` property defines MQ server host name or IP. `Port` property defines MQ server port.

`QueueManager` property defines name of queue manager and `Queue` property defines name of queue to get messages.

`StripHeaders` property states that MQ message headers shall be preserved.

`PCFEventsParser` is of type `ActivityPCFParser` meaning that it will parse PCF messages.

`TranslateNumValues` property defines that parser should translate resolved numeric values to corresponding MQ constant names if possible.

#### WMQ Trace Events streaming

This sample shows how to stream activity events received over IBM MQ as MQ Trace Events.

When configuring mqat.ini application specific stanza or setting the default values for subscriptions, the following values are advised:
* TraceLevel should be set to MEDIUM or HIGH 
* ActivityInterval, ActivityCount, SubscriptionDelivery and StopOnGetTraceMsg default values are sufficient in most cases.
* TraceMessageData set to a value sufficient to capture required data (can be 0 for no payload capture).

Sample files can be found in `samples/ibm-mq-trace-events` directory (`tnt4j-streams-wmq` module).

See sample [data source configuration](./tnt4j-streams-wmq/samples/ibm-mq-trace-events/tnt-data-source.xml).

Stream configuration states that `WmqActivityTraceStream` referencing `TraceEventsParser` shall be used. Stream takes MQ message from QM, 
transforms it to PCF message and passes it to parser. If PCF message contains multiple MQ Trace Events, then multiple activity events will 
be made from it.

`Host` property defines MQ server host name or IP. `Port` property defines MQ server port.

`QueueManager` property defines name of queue manager.

`Queue` property defines name of queue to get messages.

`TopicString` property defines specific of generic channel or application to create a subscription without using a queue.

`StripHeaders` property states that MQ message headers shall be preserved.

`TraceOperations` property defines set of traced operation names (using RegEx or wildcard). MQ Trace Events referencing operations not 
covered by this set will be filtered out from activities stream.

`ExcludedRC` property defines set of excluded MQ traces reason codes (delimited using `|` character). MQ Trace Events having reason codes 
 defined by this set will be filtered out from activities stream. Set entries may be defined using both numeric and MQ constant name values.

`SuppressBrowseGets` property defines flag indicating whether to exclude WMQ **BROWSE** type **GET** operation traces from streaming.

`TraceEventsParser` is of type `ActivityPCFParser` meaning that it will parse PCF messages containing MQ Trace Events data contained within 
`MQCFGR` PCF structures.

`TranslateNumValues` property defines that parser should translate resolved numeric values to corresponding MQ constant names if possible.

`OpenOptions` property defines additional open options to be set, include `value="MQSO_WILDCARD_CHAR"` when using a generic value for 
`TopicString`.

#### Angulartics (AngularJS tracing)

This sample shows how to stream JavaScript events traces from Angulartics. TNT4J-Angulartics-plugin sends trace data over HTTP request 
`http://localhost:9595`. Thus to process this we will need `HttpStream` running on port `9595`.

Sample files can be found in `samples/angular-js-tracing` directory.

How to setup sample environment see [`samples/angular-js-tracing/readme.md`](tnt4j-streams-core/samples/angular-js-tracing/readme.md)

Sample trace data is available in `messages.json` file.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="JSONPayloadParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJsonParser">
        <property name="ActivityDelim" value="EOF"/>

        <field name="StartTime" locator="$.timestamp" locator-type="Label" datatype="Timestamp" units="Milliseconds"/>
        <field name="ResourceName" locator="$.url" locator-type="Label"/>
        <field name="Correlator" locator="$.sid" locator-type="Label"/>
        <field name="Correlator" locator="$.rid" locator-type="Label"/>
        <field name="EventName" locator="$.eventName" locator-type="Label"/>
        <field name="EventType" value="EVENT"/>
        <field name="ElapsedTime" locator="$.pageLoad" locator-type="Label" datatype="Number" format="#####0"/>
        <field name="Browser" locator="$.browser" locator-type="Label"/>
        <field name="EventProperties" locator="$.properties" locator-type="Label"/>
    </parser>

    <parser name="AngularticsReqParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <field name="Transport" locator="ActivityTransport" locator-type="Label"/>
        <field name="MsgBody" locator="ActivityData" locator-type="Label">
            <parser-ref name="JSONPayloadParser"/>
        </field>
    </parser>

    <stream name="AngularticsHttpStream" class="com.jkoolcloud.tnt4j.streams.inputs.HttpStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="Port" value="9595"/>
        <parser-ref name="AngularticsReqParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `HttpStream` referencing `AngularticsReqParser` shall be used.

`HttpStream` starts HTTP server on port defined using `Port` property. `HaltIfNoParser` property indicates that stream
should skip unparseable entries. Stream puts received request payload data as `byte[]` to map using key `ActivityData`.

`AngularticsReqParser` by default converts `byte[]` for entry `ActivityData` to JSON format string and uses stacked
parser named `JSONPayloadParser` to parse it.

`JSONPayloadParser` transforms received JSON data string to Map and fills in activity event fields values from that map.

#### AJAX

This sample shows how to stream JavaScript events traces from AJAX. TNT4J-AJAX-interceptor sends trace data over HTTP request 
`http://localhost:9595`. Thus to process this we will need `HttpStream` running on port `9595`.

Sample files can be found in `samples/ajax` directory.

How to setup sample environment see [`samples/ajax/readme.md`](tnt4j-streams-core/samples/ajax/readme.md)

Sample trace data is available in `messages.json` file.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="JSONPayloadParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJsonParser">
        <property name="ActivityDelim" value="EOF"/>

        <field name="StartTime" locator="$.startOfLoading" locator-type="Label" datatype="Timestamp"
               units="Nanoseconds"/>
        <field name="ResourceName" locator="$.url" locator-type="Label"/>
        <field name="EventName" locator="$.eventType" locator-type="Label"/>
        <field name="Message" locator="$.message" locator-type="Label"/>
        <field name="Tag" locator="$.eventType" locator-type="Label"/>

        <field name="CompCode" locator="$.statuss" locator-type="Label">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="$.statuss" locator-type="Label"/>

        <field name="EventType" value="EVENT"/>
        <field name="EndTime" locator="$.endOfLoading" locator-type="Label" datatype="Timestamp"
               units="Milliseconds"/>
        <field name="ElapsedTime" locator="$.elapsedTime" locator-type="Label" datatype="Number" format="#####0"/>
        <field name="ContentSize" locator="$.contentSize" locator-type="Label"/>
        <field name="IsError" locator="$.error" locator-type="Label"/>
        <field name="IsAborted" locator="$.abort" locator-type="Label"/>
    </parser>

    <parser name="AjaxEventParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <field name="Transport" locator="ActivityTransport" locator-type="Label"/>
        <field name="MsgBody" locator="ActivityData" locator-type="Label">
            <parser-ref name="JSONPayloadParser"/>
        </field>
    </parser>

    <stream name="AjaxEventStream" class="com.jkoolcloud.tnt4j.streams.inputs.HttpStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="Port" value="9595"/>
        <parser-ref name="AjaxEventParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `HttpStream` referencing `AjaxEventParser` shall be used.

`HttpStream` starts HTTP server on port defined using `Port` property. `HaltIfNoParser` property indicates that stream
should skip unparseable entries. Stream puts received request payload data as `byte[]` to map using key `ActivityData`.

`AjaxEventParser` by default converts `byte[]` for entry `ActivityData` to JSON format string and uses stacked
parser named `JSONPayloadParser` to parse it.

`JSONPayloadParser` transforms received JSON data string to Map and fills in activity event fields values from that map.

#### Node.js

This sample shows how to stream JavaScript events traces from Node.js. TNT4J-njsTrace-plugin sends trace data over HTTP request 
`http://localhost:9595`. Thus to process this we will need `HttpStream` running on port `9595`.

Sample files can be found in `samples/node.js` directory.

How to setup sample environment see [`samples/node.js/readme.md`](tnt4j-streams-core/samples/node.js/readme.md)

Sample trace data is available in `messages.json` file.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="JSONPayloadParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJsonParser">
        <property name="ActivityDelim" value="EOF"/>

        <field name="ElapsedTime" locator="$.span" locator-type="Label" datatype="Number" units="Milliseconds" required="false"/>
        <field name="ResourceName" separator=",">
            <field-locator locator="$.file" locator-type="Label"/>
            <field-locator locator="$.line" locator-type="Label"/>
        </field>

        <field name="Method" locator="$.name" locator-type="Label"/>
        <field name="Message" locator="$.returnValue" locator-type="Label" required="false"/>
        <field name="EventName" value="node.js Trace"/>
        <field name="EventType" locator="$.method" locator-type="Label"/>
        <field name="Correlator" locator="$.stack[*]" locator-type="Label" separator=","/>

        <field name="Exception" locator="$.exception" locator-type="Label"/>

        <field name="CompCode" locator="$.exception" locator-type="Label">
            <field-map source="false" target="SUCCESS"/>
            <field-map source="true" target="ERROR"/>
            <field-map source="Function timed out" target="ERROR"/>
        </field>

    </parser>

    <parser name="njstraceParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <field name="Transport" locator="ActivityTransport" locator-type="Label"/>
        <field name="MsgBody" locator="ActivityData" locator-type="Label">
            <parser-ref name="JSONPayloadParser"/>
        </field>
    </parser>

    <stream name="njstraceHttpStream" class="com.jkoolcloud.tnt4j.streams.inputs.HttpStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="Port" value="9596"/>
        <parser-ref name="njstraceParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `HttpStream` referencing `njstraceParser` shall be used.

`HttpStream` starts HTTP server on port defined using `Port` property. `HaltIfNoParser` property indicates that stream
should skip unparseable entries. Stream puts received request payload data as `byte[]` to map using key `ActivityData`.

`njstraceParser` by default converts `byte[]` for entry `ActivityData` to JSON format string and uses stacked
parser named `JSONPayloadParser` to parse it.

`JSONPayloadParser` transforms received JSON data string to Map and fills in activity event fields values from that map.

#### Node.js blocking event loop

This extended ['Node.js'](#nodejs) sample shows how to trace blocking event loop occurrences.

Sample files can be found in `samples/node.js-blocking-event-loop` directory.

How to setup sample environment see [`samples/node.js-blocking-event-loop/readme.md`](tnt4j-streams-core/samples/node.js-blocking-event-loop/readme.md)

Sample stream configuration is same as in ['Node.js'](#nodejs) sample.

#### JAX-WS

This sample shows how to stream responses from JAX-WS (SOAP) services as SOAP compliant XML data.

Sample files can be found in `samples/ws-stream` directory.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/tnt4j-streams-ws/config/tnt-data-source-ws.xsd">

    <parser name="WSResponseParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityXmlParser">
        <property name="Namespace" value="s=http://schemas.xmlsoap.org/soap/envelope/"/>
        <property name="Namespace" value="a=http://schemas.datacontract.org/2004/07/"/>
        <property name="Namespace" value="i=http://www.w3.org/2001/XMLSchema-instance"/>
        <property name="Namespace" value="b=http://tempuri.org/"/>

        <field name="EventType" value="Event"/>
        <field name="ApplName" value="weather"/>
        <field name="GeoLocation" separator=",">
            <field-locator
                    locator="/s:Envelope/s:Body/b:GetCurrentWeatherInformationByStationIDResponse/b:GetCurrentWeatherInformationByStationIDResult/a:Latitude"
                    locator-type="Label"/>
            <field-locator
                    locator="/s:Envelope/s:Body/b:GetCurrentWeatherInformationByStationIDResponse/b:GetCurrentWeatherInformationByStationIDResult/a:Longitude"
                    locator-type="Label"/>
        </field>
        <field name="Temperature"
               locator="/s:Envelope/s:Body/b:GetCurrentWeatherInformationByStationIDResponse/b:GetCurrentWeatherInformationByStationIDResult/a:TemperatureInFahrenheit"
               locator-type="Label"/>
        <field name="Humidity"
               locator="/s:Envelope/s:Body/b:GetCurrentWeatherInformationByStationIDResponse/b:GetCurrentWeatherInformationByStationIDResult/a:RelativeHumidity"
               locator-type="Label"/>
        <field name="Wind Speed"
               locator="/s:Envelope/s:Body/b:GetCurrentWeatherInformationByStationIDResponse/b:GetCurrentWeatherInformationByStationIDResult/a:WindSpeedInMPH"
               locator-type="Label"/>
    </parser>

    <stream name="WSSampleStream" class="com.jkoolcloud.tnt4j.streams.inputs.WsStream">
        <property name="HaltIfNoParser" value="false"/>

        <scenario name="Sample WS stream scenario">
            <step name="Step 1"
                  url="http://wsdot.wa.gov/traffic/api/WeatherInformation/WeatherInformation.svc">
                <schedule-simple interval="35" units="Seconds" repeatCount="10"/>
                <request>
                    <![CDATA[
                        SOAPAction:http://tempuri.org/IWeatherInformation/GetCurrentWeatherInformationByStationID
                        <tem:GetCurrentWeatherInformationByStationID xmlns:tem="http://tempuri.org/">
                            <tem:AccessCode>aeb652b7-f6f5-49e6-9bdb-e2b737ebd507</tem:AccessCode>
                            <tem:StationID>1909</tem:StationID>
                        </tem:GetCurrentWeatherInformationByStationID>
                    ]]>
                </request>
            </step>
        </scenario>

        <parser-ref name="WSResponseParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `WsStream` referencing `WSResponseParser` shall be used. Stream takes response received
SOAP compliant XML string and passes it to parser.

`HaltIfNoParser` property indicates that stream should skip unparseable entries.

Streaming scenario consists of one step defining service request URL, scheduler definition: `10 times with 35 seconds
interval`. `request` definition contains SOAP request header `SOAPAction` and SOAP request body data
`tem:GetCurrentWeatherInformationByStationID`.

`WSResponseParser` maps XML data values to activity event fields `GeoLocation`, `Temperature`, `Humidity` and
`Wind Speed`. Parser property `Namespace` adds additional namespaces required to parse received JAX-WS XML response
using XPath.

#### JAX-RS

##### JAX-RS JSON

This sample shows how to stream responses from JAX-RS as JSON data.

Sample files can be found in `samples/restful-stream-json` directory. See the [`readme.md`](./tnt4j-streams-ws/samples/restful-stream-json/readme.md) 
file in that directory for information on usage and modifying the parser for your use.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/tnt4j-streams-ws/config/tnt-data-source-ws.xsd">

    <parser name="RESTResponseParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJsonParser">
        <property name="ActivityDelim" value="EOF"/>

        <field name="EventType" value="Event"/>
        <field name="ApplName" value="weather"/>
        <field name="Location" locator="$.name" locator-type="Label"/>
        <field name="GeoLocation" separator=",">
            <field-locator locator="$.coord.lon" locator-type="Label"/>
            <field-locator locator="$.coord.lat" locator-type="Label"/>
        </field>
        <field name="Temperature" locator="$.main.temp" locator-type="Label"/>
        <field name="Humidity" locator="$.main.humidity" locator-type="Label"/>
        <field name="Wind Speed" locator="$.wind.speed" locator-type="Label"/>
    </parser>

    <stream name="RESTfulSampleJSONStream" class="com.jkoolcloud.tnt4j.streams.inputs.RestStream">
        <property name="HaltIfNoParser" value="false"/>

        <scenario name="Sample REST stream scenario">
            <step name="Step Kaunas"
                  url="http://api.openweathermap.org/data/2.5/weather?q=Kaunas&amp;APPID=fa1fede9cbd6e26efdea1cdcbc714069&amp;units=metric"
                  method="GET">
                <schedule-cron expression="0/15 * * * * ? *"/>
            </step>
            <step name="Step Vilnius"
                  url="http://api.openweathermap.org/data/2.5/weather?q=Vilnius&amp;APPID=fa1fede9cbd6e26efdea1cdcbc714069&amp;units=metric"
                  method="GET">
                <schedule-cron expression="0/30 * * * * ? *"/>
            </step>
            <step name="Step Klaipeda"
                  url="http://api.openweathermap.org/data/2.5/weather?q=Klaipeda&amp;APPID=fa1fede9cbd6e26efdea1cdcbc714069&amp;units=metric"
                  method="GET">
                <schedule-simple interval="45" units="Seconds" repeatCount="10"/>
            </step>
        </scenario>

        <parser-ref name="RESTResponseParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `RestStream` referencing `RESTResponseParser` shall be used. Stream takes response
received JSON string and passes it to parser.

`HaltIfNoParser` property indicates that stream should skip unparseable entries.

Streaming scenario consists of three steps defining service request URL's, request methods (all `GET`) and scheduler
definitions.
Step named `Step Kaunas` defines Cron scheduler expression stating: `invoke every 15 seconds`.
Step named `Step Vilnius` defines Cron scheduler expression stating: `invoke every 30 seconds`.
Step named `Step Klaipeda` defines simple scheduler expression stating: `10 times with 45 seconds interval`.

`RESTResponseParser` maps JSON data values to activity event fields `Location`, `GeoLocation`, `Temperature`, `Humidity`
and `Wind Speed`. Parser property `ActivityDelim` indicates that whole parsed string represents single JSON data package.

##### JAX-RS XML

This sample shows how to stream responses from JAX-RS as XML data.

Sample files can be found in `samples/restful-stream-xml` directory. See the [`readme.md`](./tnt4j-streams-ws/samples/restful-stream-xml/readme.md) 
file in that directory for information on usage and modifying the parser for your use.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/tnt4j-streams-ws/config/tnt-data-source-ws.xsd">

    <parser name="RESTResponseParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityXmlParser">
        <field name="EventType" value="Event"/>
        <field name="ApplName" value="weather"/>
        <field name="Location" locator="/current/city/@name" locator-type="Label"/>
        <field name="GeoLocation" separator=",">
            <field-locator locator="/current/city/coord/@lon" locator-type="Label"/>
            <field-locator locator="/current/city/coord/@lat" locator-type="Label"/>
        </field>
        <field name="Temperature" locator="/current/temperature/@value" locator-type="Label"/>
        <field name="Humidity" locator="/current/humidity/@value" locator-type="Label"/>
        <field name="Wind Speed" locator="/current/wind/speed/@value" locator-type="Label"/>
    </parser>

    <stream name="RESTfulSampleXMLStream" class="com.jkoolcloud.tnt4j.streams.inputs.RestStream">
        <property name="HaltIfNoParser" value="false"/>

        <scenario name="Sample REST stream scenario">
            <step name="Step Kaunas"
                  url="http://api.openweathermap.org/data/2.5/weather?q=Kaunas&amp;APPID=fa1fede9cbd6e26efdea1cdcbc714069&amp;units=metric&amp;mode=xml"
                  method="GET">
                <schedule-cron expression="0/15 * * * * ? *"/>
            </step>
            <step name="Step Vilnius"
                  url="http://api.openweathermap.org/data/2.5/weather?q=Vilnius&amp;APPID=fa1fede9cbd6e26efdea1cdcbc714069&amp;units=metric&amp;mode=xml"
                  method="GET">
                <schedule-cron expression="0/30 * * * * ? *"/>
            </step>
            <step name="Step Klaipeda"
                  url="http://api.openweathermap.org/data/2.5/weather?q=Klaipeda&amp;APPID=fa1fede9cbd6e26efdea1cdcbc714069&amp;units=metric&amp;mode=xml"
                  method="GET">
                <schedule-simple interval="45" units="Seconds" repeatCount="10"/>
            </step>
        </scenario>

        <parser-ref name="RESTResponseParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `RestStream` referencing `RESTResponseParser` shall be used. Stream takes response
received XML string and passes it to parser.

`HaltIfNoParser` property indicates that stream should skip unparseable entries.

Streaming scenario consists of three steps defining service request URL's, request methods (all `GET`) and scheduler
definitions.
Step named `Step Kaunas` defines Cron scheduler expression stating: `invoke every 15 seconds`.
Step named `Step Vilnius` defines Cron scheduler expression stating: `invoke every 30 seconds`.
Step named `Step Klaipeda` defines simple scheduler expression stating: `10 times with 45 seconds interval`.

`RESTResponseParser` maps XML data values to activity event fields `Location`, `GeoLocation`, `Temperature`, `Humidity`
and `Wind Speed`.

#### System command

##### Windows

This sample shows how to stream responses from executed Windows OS command.

Sample files can be found in `samples/win-cmd-stream` directory.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/tnt4j-streams-ws/config/tnt-data-source-ws.xsd">

    <parser name="ResponseParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityRegExParser">
        <property name="Pattern"
                  value="(\s*)&quot;\((.*)\)&quot;,&quot;(.*)&quot; &quot;(.*)&quot;,&quot;(.*)&quot;(.*)"/>

        <field name="EventType" value="SNAPSHOT"/>
        <field name="ProcessorTime" locator="5" locator-type="Index"/>
    </parser>

    <stream name="WinCmdStream" class="com.jkoolcloud.tnt4j.streams.inputs.CmdStream">
        <property name="HaltIfNoParser" value="false"/>

        <scenario name="Sample Win Cmd stream scenario">
            <step name="Step Windows">
                <request>typeperf "\Processor(_Total)\% Processor Time" -sc 1</request>
                <schedule-simple interval="25" units="Seconds" repeatCount="-1"/>
            </step>
        </scenario>

        <parser-ref name="ResponseParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `CmdStream` referencing `ResponseParser` shall be used. Stream takes command response
output string and passes it to parser.

`HaltIfNoParser` property indicates that stream should skip unparseable entries.

Streaming scenario consists of one step defining system command to execute as `request` tag data and scheduler
definition stating `execute endlessly every 25 seconds`.

`ResponseParser` parses command output string using `Pattern` property defined RegEx and produces activity snapshot
containing field `ProcessorTime`.

**NOTE:** Stream stops only when critical runtime error/exception occurs or application gets terminated.

##### *nix

This sample shows how to stream responses from executed *nix type OS command.

Sample files can be found in `samples/unix-cmd-stream` directory.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/tnt4j-streams-ws/config/tnt-data-source-ws.xsd">

    <parser name="ResponseParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityRegExParser">
        <property name="Pattern" value="(.*)"/>

        <field name="EventType" value="SNAPSHOT"/>
        <field name="TomcatActive" locator="1" locator-type="Index"/>
    </parser>

    <stream name="UnixCmdStream" class="com.jkoolcloud.tnt4j.streams.inputs.CmdStream">
        <property name="HaltIfNoParser" value="false"/>

        <scenario name="Sample *nix Cmd stream scenario">
            <step name="Step Is Active Tomcat">
                <request>systemctl is-active tomcat7.servicesystemctl is-active tomcat7.service</request>
                <schedule-simple interval="45" units="Seconds" repeatCount="-1"/>
            </step>
        </scenario>

        <parser-ref name="ResponseParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `CmdStream` referencing `ResponseParser` shall be used. Stream takes command response
output string and passes it to parser.

`HaltIfNoParser` property indicates that stream should skip unparseable entries.

Streaming scenario consists of one step defining system command to execute as `request` tag data and scheduler
definition stating `execute endlessly every 45 seconds`.

`ResponseParser` parses command output string using `Pattern` property defined RegEx and produces activity snapshot
containing field `TomcatActive`.

**NOTE:** Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### JDBC

This sample shows how to stream `JDBC` provided `ResultSet` rows as activity events.

Sample files can be found in `samples/b2bi-jdbc-stream` directory (`tnt4j-streams-ws` module).

**NOTE:** in `run.bat/run.sh` file set variable `JDBC_LIBPATH` value to reference JDBC implementation (`PostgreSQL`, `MySQL`, `DB2`, 
`Oracle` and etc.) libraries used by your environment.

See sample [data source configuration](./tnt4j-streams-ws/samples/b2bi-jdbc-stream/tnt-data-source.xml).

Fill in these configuration value placeholders:
 * `[HOST]` - define your host for B2Bi database. Optionally you can completely change JDBC URL yo match your JDBC driver, port and database.
 * `[USER_NAME]` - define your database user name.
 * `[USER_PASS]` - define your database user password.

#### Redirecting TNT4J streams

This sample shows how to redirect `tnt4j-stream-jmx` (may be from multiple running instances) produced trackables to [jKoolCloud](https://www.jkoolcloud.com/) 
over single `TNT4J-Streams` stream instance.

Sample files can be found in `samples/stream-jmx` directory.

To redirect `tnt4j-stream-jmx` (or any other TNT4J based producer) produced trackables, producer configuration file `tnt4j.properties` 
should contain such stanza:
```properties
    event.sink.factory.EventSinkFactory: com.jkoolcloud.tnt4j.sink.impl.SocketEventSinkFactory
    event.sink.factory.EventSinkFactory.LogSink: null
    event.sink.factory.EventSinkFactory.Host: IP_OF_STREAMS_RUNNING_MACHINE
    event.sink.factory.EventSinkFactory.Port: 9009
    event.formatter: com.jkoolcloud.tnt4j.format.JSONFormatter
```
**NOTE:** change `IP_OF_STREAMS_RUNNING_MACHINE` to IP of machine running `TNT4J-Streams` `RedirectTNT4JStream`.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <java-object name="JMXRedirectOutput" class="com.jkoolcloud.tnt4j.streams.outputs.JKCloudJsonOutput"/>

    <stream name="SampleJMXRoutingStream" class="com.jkoolcloud.tnt4j.streams.inputs.RedirectTNT4JStream">
        <property name="RestartOnInputClose" value="true"/>
        <property name="Port" value="9009"/>

        <reference name="JMXRedirectOutput"/>

        <tnt4j-properties>
            <property name="event.formatter" value="com.jkoolcloud.tnt4j.streams.utils.RedirectTNT4JStreamFormatter"/>
        </tnt4j-properties>
    </stream>
</tnt-data-source>
```

Stream configuration states that `SampleJMXRoutingStream` referencing `JMXRedirectOutput` shall be used. Stream takes starts server socket 
on port defined by stream property `Port`. Stream listens server socket for inbound connections (accepts multiple). When connection gets 
accepted, stream reads incoming data from connection dedicated socket.

`RestartOnInputClose` property indicates that stream should initiate new instance of server socket if listened one gets closed or fails to
accept inbound connection.

Stream referenced object `JMXRedirectOutput` sends JSON formatted data to [jKoolCloud](https://www.jkoolcloud.com/).

Stream also additionally sets one TNT4J framework property `event.formatter`. This allows us to use customized JSON formatter and avoid 
additional JSON reformatting in default TNT4J data flow.

**NOTE:** you may also re-stream any TNT4J based producer logged trackables from file. Only requirement - trackables must be serialized in JSON 
format.

To do re-streaming from file, change sample configuration by replacing `SampleJMXRoutingStream` stream property `Port` to `FileName` 
referring file containing logged trackables in JSON format:
```xml
    <stream name="SampleJMXRoutingStream" class="com.jkoolcloud.tnt4j.streams.inputs.RedirectTNT4JStream">
        <property name="FileName" value="tnt4j-stream-activities.log"/>

        <reference name="JMXRedirectOutput"/>

        <tnt4j-properties>
            <property name="event.formatter" value="com.jkoolcloud.tnt4j.streams.utils.RedirectTNT4JStreamFormatter"/>
        </tnt4j-properties>
    </stream>
```

#### MS Excel document

##### Rows

This sample shows how to stream MS Excel workbook rows as activity events.

Sample files can be found in `samples/xlsx-rows` directory.

**NOTE:** Records in this file are from year `2010`, e.g. `12 Jul 2010`, so when sending the events data to 
[jKoolCloud](https://www.jkoolcloud.com/), please do not forget to adjust the dashboard time frame to that period!

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="ExcelRowParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityExcelRowParser">
        <field name="StartTime" locator="A" locator-type="Label" format="dd MMM yyyy HH:mm:ss" locale="en-US"/>
        <field name="ServerIp" locator="B" locator-type="Label"/>
        <field name="ApplName" value="orders"/>
        <field name="Correlator" locator="C" locator-type="Label"/>
        <field name="UserName" locator="D" locator-type="Label"/>
        <field name="EventName" locator="E" locator-type="Label"/>
        <field name="EventType" locator="E" locator-type="Label">
            <field-map source="Order Placed" target="START"/>
            <field-map source="Order Received" target="RECEIVE"/>
            <field-map source="Order Processing" target="OPEN"/>
            <field-map source="Order Processed" target="SEND"/>
            <field-map source="Order Shipped" target="END"/>
        </field>
        <field name="MsgValue" locator="H" locator-type="Label"/>
    </parser>

    <stream name="SampleExcelRowsStream" class="com.jkoolcloud.tnt4j.streams.inputs.ExcelRowStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="FileName" value="./tnt4j-streams-msoffice/samples/xlsx-rows/sample.xlsx"/>
        <property name="RangeToStream" value="1:"/>
        <property name="SheetsToProcess" value="Sheet*"/>

        <parser-ref name="ExcelRowParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `SampleExcelRowsStream` referencing `ExcelRowsParser` shall be used. Stream takes
workbook sheet row and passes it to parser.

`HaltIfNoParser` property indicates that stream should skip unparseable rows.

`SampleExcelRowsStream` reads data from `./tnt4j-streams-msoffice/samples/xlsx-rows/sample.xlsx` file.

`RangeToStream` defines range of rows to be streamed from each matching sheet - `from first row to the end`.

`SheetsToProcess` property defines sheet name filtering mask using wildcard string. It is also allowed to use RegEx like
`Sheet(1|3|5)` (in this case just sheets with names `Sheet1`, `Sheet3` and `Sheet5` will be processed).

`ExcelRowParser` parser uses literal sheet column indicators as locators (e.g., `A`, `D`, `AB`).

**NOTE:** `StartTime` fields defines format and locale to correctly parse field data string. `EventType` uses manual
field string mapping to TNT4J event field value.

**NOTE:** `ExcelRowStream` uses DOM based MS Excel file reading, thus memory consumption for large file may be significant, but it allows 
random cells access and precise formula evaluation. In case memory consumption is critical factor, use `ExcelSXSSFRowStream` instead of 
`ExcelRowStream`. It uses Apache POI SXSSF API to read MS Excel as a stream consistently iterating over workbook sheets rows and cells. 
Thus it may have some drawback on cell formula evaluation. For more information see [Apache POI spreadsheet documentation](https://poi.apache.org/spreadsheet/).

##### Sheets

This sample shows how to stream MS Excel workbook sheets as activity events.

Sample files can be found in `samples/xlsx-sheets` directory.

**NOTE:** Records in this file are from year `2010`, e.g. `12 Jul 2010`, so when sending the events data to 
[jKoolCloud](https://www.jkoolcloud.com/), please do not forget to adjust the dashboard time frame to that period!

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="ExcelSheetParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityExcelSheetParser">
        <field name="StartTime" locator="B2" locator-type="Label" format="dd MMM yyyy HH:mm:ss" locale="en-US"/>
        <field name="ServerIp" locator="B3" locator-type="Label"/>
        <field name="ApplName" value="orders"/>
        <field name="Correlator" locator="B4" locator-type="Label"/>
        <field name="UserName" locator="B5" locator-type="Label"/>
        <field name="EventName" locator="B6" locator-type="Label"/>
        <field name="EventType" locator="B6" locator-type="Label">
            <field-map source="Order Placed" target="START"/>
            <field-map source="Order Received" target="RECEIVE"/>
            <field-map source="Order Processing" target="OPEN"/>
            <field-map source="Order Processed" target="SEND"/>
            <field-map source="Order Shipped" target="END"/>
        </field>
        <field name="MsgValue" locator="B9" locator-type="Label"/>
    </parser>

    <stream name="SampleExcelSheetsStream" class="com.jkoolcloud.tnt4j.streams.inputs.ExcelSheetStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="FileName" value="./tnt4j-streams-msoffice/samples/xlsx-sheets/sample.xlsx"/>
        <property name="SheetsToProcess" value="Sheet*"/>

        <parser-ref name="ExcelSheetParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `SampleExcelSheetsStream` referencing `ExcelSheetParser` shall be used. Stream takes
workbook sheet and passes it to parser.

`HaltIfNoParser` property indicates that stream should skip unparseable sheets.

`SampleExcelRowsStream` reads data from `./tnt4j-streams-msoffice/samples/xlsx-sheets/sample.xlsx` file.

`SheetsToProcess` property defines sheet name filtering mask using wildcard string. It is also allowed to use RegEx like
`Sheet(1|3|5)` (in this case just sheets with names `Sheet1`, `Sheet3` and `Sheet5` will be processed).

`ExcelSheetParser` parser uses literal sheet cell indicators as locators (e.g., `A1`, `D5`, `AB12` where letters
identifies column and number identifies row).

**NOTE:** `StartTime` fields defines format and locale to correctly parse field data string. `EventType` uses manual
field string mapping to TNT4J event field value.

#### Collectd performance metrics streaming

This sample shows how to stream `collectd` monitoring reports data as activity events.

Sample files can be found in `samples/collectd-json` directory (`tnt4j-streams-core` module).

**NOTE:** to use this sample `collectd` should be running `Write HTTP` plugin. See [Collectd Wiki](https://collectd.org/wiki/index.php/Plugin:Write_HTTP)
for details.

Sample report data is available in `stats.json` file.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="CollectdStatsDataParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <field name="EventType" value="SNAPSHOT"/>
        <field name="EventName" locator="type|type_instance" locator-type="Label" separator=" "/>
        <field name="Category" locator="plugin|plugin_instance" locator-type="Label" separator=" "/>
        <field name="ServerName" locator="host" locator-type="Label"/>

        <field name="${FieldNameLoc}" locator="values" locator-type="Label" value-type="${ValueTypeLoc}" split="true">
            <field-locator id="FieldNameLoc" locator="dsnames" locator-type="Label"/>
            <field-locator id="ValueTypeLoc" locator="dstypes" locator-type="Label"/>
        </field>

        <field name="StartTime" locator="time" locator-type="Label" datatype="Timestamp" units="Seconds"/>
    </parser>

     <parser name="CollectdReqBodyParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJsonParser">
        <property name="ActivityDelim" value="EOF"/>

        <field name="MsgBody" locator="$" locator-type="Label" transparent="true" split="true">
            <parser-ref name="CollectdStatsDataParser" aggregation="Relate"/>
        </field>
        <field name="EventType" value="Activity"/>
        <field name="ApplName" value="collectd"/>
    </parser>

    <parser name="CollectdReqParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <field name="Transport" locator="ActivityTransport" locator-type="Label"/>
        <field name="MsgBody" locator="ActivityData" locator-type="Label">
            <parser-ref name="CollectdReqBodyParser"/>
        </field>
    </parser>

    <stream name="CollectdStream" class="com.jkoolcloud.tnt4j.streams.inputs.HttpStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="Port" value="9595"/>

        <parser-ref name="CollectdReqParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `HttpStream` referencing `CollectdReqParser` shall be used. Stream takes HTTP request 
received from `collectd` server and passes it to parser.

`HttpStream` starts HTTP server on port defined using `Port` property. `HaltIfNoParser` property indicates that stream
should skip unparseable entries. Stream puts received request payload data as `byte[]` to map using key `ActivityData`.

`CollectdReqParser` by default converts `byte[]` for entry `ActivityData` to string and uses stacked parser named
`CollectdReqBodyParser` to parse HTTP request contained JSON format data.

`CollectdReqBodyParser` maps `collectd` report JSON data to activity (field `EventType`) containing set of snapshots (field `MsgBody`) 
carrying system metrics data. Each snapshot is parsed using stacked `CollectdStatsDataParser` parser (map parser because parent JSON parser 
already made map data structures from Raw `collectd` report JSON data).

`CollectdStatsDataParser` maps map entries to snapshot fields `EventName`, `Category`, `ServerName`, `StartTime`. Also this parser uses 
dynamic field named `${FieldNameLoc}`. Attribute values containing variable expressions `${}` indicates that these attributes can have 
dynamic values resolved from streamed data. Such variable expressions has to reference locators identifiers (`field-locator` attribute `id`) 
to resolve and fill actual data. Field attribute `split`, that if field value locator resolves collection (list or array) of values, values 
of that collection should be split into separate fields of produced activity.

#### Nagios reports streaming

This sample shows how to stream Nagios monitoring reports data as activity events.

Sample files can be found in `samples/nagios-nagios2json` directory (`tnt4j-streams-ws` module).

**NOTE:** to use this sample Nagios should be running extension `nagios2json`. See [Sample README](tnt4j-streams-ws/samples/nagios-nagios2json/readme.md)
for details. `nagios2json` extension works as `CGI` script, but can be handled as simple RESTful service.

Sample report data is available in `report.json` file.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/tnt4j-streams-ws/config/tnt-data-source-ws.xsd">

    <parser name="SnapshotParser"
            class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">

        <field name="EventType" value="Snapshot"/>
        <field name="ApplName" value="Nagios"/>
        <field name="EventName" locator="service" locator-type="Label"/>
        <field name="Status" locator="status" locator-type="Label" value-type="enum"/>
        <field name="Message" locator="plugin_output" locator-type="Label"/>
        <field name="Category" locator="hostname" locator-type="Label"/>
        <field name="Duration" locator="duration" locator-type="Label" value-type="age"/>
        <field name="StartTime" locator="last_state_change" locator-type="Label" datatype="Timestamp" units="Seconds"/>
    </parser>

    <parser name="ResponseParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJsonParser">
        <property name="ActivityDelim" value="EOF"/>

        <field name="MsgBody" locator="$.data" locator-type="Label" transparent="true" split="true">
            <parser-ref name="SnapshotParser" aggregation="Relate"/>
        </field>

        <field name="EventType" value="Activity"/>
        <field name="StartTime" locator="$.created" locator-type="Label" datatype="Timestamp" units="Seconds"/>
        <field name="ApplName" value="nagios2json"/>
        <field name="Message" separator=", ">
            <field-locator locator="$.version" locator-type="Label"/>
            <field-locator locator="$.running" locator-type="Label"/>
            <field-locator locator="$.servertime" locator-type="Label" datatype="Timestamp" units="Seconds"/>
            <field-locator locator="$.localtime" locator-type="Label"/>
        </field>
    </parser>

    <stream name="RESTfulSampleNagiosStream" class="com.jkoolcloud.tnt4j.streams.inputs.RestStream">
        <property name="HaltIfNoParser" value="false"/>

        <scenario name="Nagios2JSON stream scenario">
            <step name="Step 1"
                  url="http://[YOUR_NAGIOS_SERVER_IP]/nagios/cgi-bin/nagios2json.cgi?servicestatustypes=31"
                  method="GET"
                  username="myNagiosUserName"
                  password="myNagiosUserSecretPassword">
                <schedule-cron expression="0/15 * * * * ? *"/>
            </step>
        </scenario>

        <parser-ref name="ResponseParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `RestStream` referencing `ResponseParser` shall be used. Stream takes response
received JSON string and passes it to parser.

`HaltIfNoParser` property indicates that stream should skip unparseable entries.

Streaming scenario step defines:
 * Nagios service request URL and `nagios2json` parameters (e.g., `servicestatustypes=31`) 
 * request method `GET` to comply `cgi`.
 * Nagios service user credentials - user name and password
 * Scheduler is set to `Cron` expression `every 15 seconds`.

You may also add additional steps to retrieve different reports defining different `nagios2json` parameters.

Nagios sends report as activity wrapping multiple metrics (snapshots).

`ResponseParser` maps Nagios report JSON data to activity (field `EventType`) containing set of snapshots (field `MsgBody`) carrying system 
state/metrics data. Each snapshot is parsed using stacked `SnapshotParser` parser (map parser because parent JSON parser already made map 
data structures from Raw Nagios report JSON data).

`SnapshotParser` maps map entries to snapshot fields `ApplName`, `EventName`, `Status`, `Message`, `Category`, `Duration` and `StartTime`.
`Status` and `Duration` fields also defines value types: `Status` is `enum`, `Duration` is `age`.

#### IBM MQ error log streaming

This sample shows how to stream IBM MQ error log entries as activity events.

Sample files can be found in `samples/ibm-mq-err-log` directory (`tnt4j-streams-core` module).

Sample error log file is available in `AMQERR01.LOG` file.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="MQErrLogParser" class="com.jkoolcloud.tnt4j.streams.custom.parsers.IBMMQLogParser">
        <property name="UseActivityDataAsMessageForUnset" value="true"/>

        <field name="EventType" value="EVENT"/>
        <field name="ResourceName" locator="FileName" locator-type="StreamProp"/>

        <field name="StartTimeCommon" separator=" " format="MM/dd/yyyy HH:mm:ss" datatype="DateTime" transparent="true">
            <field-locator locator="Date" locator-type="Label"/>
            <field-locator locator="Time" locator-type="Label"/>
        </field>
        <field name="Process" locator="Process" locator-type="Label"/>
        <field name="ProcessId" locator="pid" locator-type="Label"/>
        <field name="ThreadId" locator="tid" locator-type="Label"/>
        <field name="UserName" locator="User" locator-type="Label"/>
        <field name="ApplName" locator="Program" locator-type="Label"/>
        <field name="ServerName" locator="Host" locator-type="Label"/>
        <field name="Location" locator="Installation" locator-type="Label"/>
        <field name="VRMF" locator="VRMF" locator-type="Label"/>
        <field name="QMGR" locator="QMgr" locator-type="Label"/>
        <field name="EventName" locator="ErrCode" locator-type="Label"/>
        <field name="Exception" locator="ErrText" locator-type="Label"/>
        <field name="Explanation" locator="Explanation" locator-type="Label"/>
        <field name="Action" locator="Action" locator-type="Label"/>
        <field name="Where" locator="Where" locator-type="Label"/>
        
        <!-- IBM MQ 9.1 additional attributes -->
        <field name="Severity" locator="Severity" locator-type="Label"/>
        <field name="StartTimeUTC" locator="TimeUTC" locator-type="Label" format="yyyy-MM-dd'T'HH:mm:ss.SSSX" datatype="DateTime"
               transparent="true"/>

        <!--        <field name="CommentInsert1" locator="CommentInsert1" locator-type="Label"/>-->
        <!--        <field name="CommentInsert1" locator="CommentInsert2" locator-type="Label"/>-->
        <!--        <field name="CommentInsert1" locator="CommentInsert3" locator-type="Label"/>-->

        <!--        <field name="ArithInsert1" locator="ArithInsert3" locator-type="Label"/>-->
        <!--        <field name="ArithInsert1" locator="ArithInsert3" locator-type="Label"/>-->
        <!--        <field name="ArithInsert1" locator="ArithInsert3" locator-type="Label"/>-->

        <field name="StartTime" value="">
            <field-transform lang="groovy">
                ${StartTimeUTC} == null ? ${StartTimeCommon} : ${StartTimeUTC}
            </field-transform>
        </field>

        <field name="AllRestLogEntryValues" locator="#" locator-type="Label"/>
    </parser>

    <stream name="FileStream" class="com.jkoolcloud.tnt4j.streams.inputs.FileLineStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="FileName" value="./tnt4j-streams-core/samples/ibm-mq-err-log/AMQERR*.LOG"/>
        <property name="RestoreState" value="false"/>
        <property name="FilePolling" value="true"/>
        <property name="FileReadDelay" value="20"/>
        <property name="StartFromLatest" value="false"/>
        <property name="ActivityDelim" value="-----"/>
        <property name="KeepLineSeparators" value="true"/>

        <parser-ref name="MQErrLogParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `FileStream` referencing `MQErrLogParser` shall be used. Stream reads IBM MQ error log entries from 
`./tnt4j-streams-core/samples/ibm-mq-err-log/AMQERR01.LOG` file contents and passes it to parser.

`HaltIfNoParser` property indicates that stream should skip unparseable entries.

`MQErrLogParser` maps IBM MQ error log entry resolved fields to activity event fields. There is additional field `ResourceName` resolved 
from stream configuration data and referring stream input (IBM MQ error log) file name.

#### JSON format IBM MQ error log streaming

This sample shows how to stream JSON formatted IBM MQ (version starting 9.0.5) error log entries as activity events.

Sample files can be found in `samples/ibm-mq-err-log` directory (`tnt4j-streams-core` module).

Sample error log file is available in `AMQERR01.JSON` file.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="MQErrLogJSONParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJsonParser">
        <property name="UseActivityDataAsMessageForUnset" value="true"/>

        <field name="EventType" value="EVENT"/>
        <field name="ResourceName" locator="FileName" locator-type="StreamProp"/>

        <field name="StartTime" locator="$.ibm_datetime" format="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" locator-type="Label"/>
        <field name="Process" locator="$.ibm_processName" locator-type="Label"/>
        <field name="ProcessId" locator="$.ibm_processId" locator-type="Label"/>
        <field name="ThreadId" locator="$.ibm_threadId" locator-type="Label"/>
        <field name="UserName" locator="$.ibm_userName" locator-type="Label"/>
        <field name="ServerName" locator="$.ibm_serverName" locator-type="Label"/>
        <field name="Location" locator="$.ibm_installationName" locator-type="Label"/>
        <field name="VRMF" locator="$.ibm_version" locator-type="Label"/>
        <field name="QMGR" locator="$.ibm_qmgrId" locator-type="Label"/>
        <field name="EventName" locator="$.ibm_messageId" locator-type="Label"/>
        <field name="Exception" locator="$.message" locator-type="Label"/>
        <field name="Where" locator="$.module" locator-type="Label"/>

        <field name="MsgVariable1" locator="$.ibm_arithInsert1" locator-type="Label"/>
        <field name="MsgVariable2" locator="$.ibm_arithInsert2" locator-type="Label"/>
        <field name="MsgComment1" locator="$.ibm_commentInsert1" locator-type="Label"/>
        <field name="MsgComment2" locator="$.ibm_commentInsert2" locator-type="Label"/>
        <field name="MsgComment3" locator="$.ibm_commentInsert3" locator-type="Label"/>
        <field name="Severity" locator="$.loglevel" locator-type="Label"/> <!--  INFO, WARNING, or ERROR -->
        <field name="Sequence" locator="$.ibm_sequence" locator-type="Label"/>
        <field name="RemoteHost" locator="$.ibm_remoteHost" locator-type="Label"/>
        <field name="Host" locator="$.host" locator-type="Label"/>
        <field name="InstallationDir" locator="$.ibm_installationDir" locator-type="Label"/>
    </parser>

    <stream name="FileStream" class="com.jkoolcloud.tnt4j.streams.inputs.FileLineStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="FileName" value="./tnt4j-streams-core/samples/ibm-mq-err-log/AMQERR*.JSON"/>
        <property name="RestoreState" value="false"/>
        <property name="FilePolling" value="true"/>
        <property name="FileReadDelay" value="20"/>
        <property name="StartFromLatest" value="false"/>
        <property name="ActivityDelim" value="EOL"/>

        <parser-ref name="MQErrLogJSONParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `FileStream` referencing `MQErrLogJSONParser` shall be used. Stream reads JSON formatted IBM MQ error log 
entries (one per line) from `./tnt4j-streams-core/samples/ibm-mq-err-log/AMQERR01.JSON` file contents and passes it to parser.

`HaltIfNoParser` property indicates that stream should skip unparseable entries.

`MQErrLogJSONParser` maps IBM MQ error JSON log entry resolved fields to activity event fields. There is additional field `ResourceName` 
resolved from stream configuration data and referring stream input (IBM MQ error log) file name.

**NOTE:** JSON formatted IBM MQ error log entries does not have some fields e.g. `Action`, `Explanation`, like ordinary log has.

See [JSON format diagnostic messages](https://www.ibm.com/support/knowledgecenter/en/SSFKSJ_9.0.0/com.ibm.mq.ref.doc/q130430_.htm) for more 
details.

#### String ranges streaming

This sample shows how to stream SOCGEN messages strings from file entries as activity events. Each file entry string represents activity 
event and fields of event are substring ranges.

Sample files can be found in `samples/string-ranges` directory (`tnt4j-streams-core` module).

Sample message data is available in `strings.txt` file. Now it contains only one entry.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="StrRangesParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityStringParser">
        <property name="ActivityDelim" value="EOF"/>

        <field name="EventType" value="EVENT"/>
        <field name="EventName" value="SOCGEN_Msg_Data"/>

        <field name="TransID">  <!-- offsets 0-13 inclusive, length=14, expected value: TID_TEST3B_456 -->
            <field-locator locator="0:14" locator-type="Range"/>
        </field>
        <field name="TransType">  <!-- offsets 20-35 inclusive, length=16, expected value: TYPE_TEST3B_SALE -->
            <field-locator locator="20:36" locator-type="Range"/>
        </field>
        <field name="TransValue">  <!-- offsets 70-89 inclusive, field length=20, expected value: AMT_TEST3B_USD123.45  -->
            <field-locator locator="70:90" locator-type="Range"/>
        </field>
        <field name="UserData">  <!-- offsets 123-145 inclusive, length=23, expected value: TEST3B_Model iSeries123 -->
            <field-locator locator="123:146" locator-type="Range"/>
        </field>
    </parser>

    <stream name="FileStream" class="com.jkoolcloud.tnt4j.streams.inputs.CharacterStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="FileName" value="./tnt4j-streams-core/samples/string-ranges/strings.txt"/>

        <parser-ref name="StrRangesParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `FileStream` referencing `StrRangesParser` shall be used. Stream reads message entries from 
`./tnt4j-streams-core/samples/string-ranges/strings.txt` file contents and passes it to parser.

`HaltIfNoParser` property indicates that stream should skip unparseable entries.

`StrRangesParser` picks range defined substrings and maps them to activity event fields. For example `TransID` is substring ranging 0-13 within 
provided string. **NOTE** that range lower bound is treated **inclusive** and upper bound - **exclusive**.

`Range` values can be defined as this:
 * `:x` - from the `beginning` of the string to character at the index `x` (**exclusive**)
 * `x:y` - from character at the index `x` (**inclusive**) to character at the index `y` (**exclusive**)
 * `x:` - from character at the index `x` (**inclusive**) to the `end` of the string

#### IBM MQ RFH2/JMS streaming

This sample shows how to stream IBM MQ RFH2/JMS binary data as activity events.

Sample files can be found in `samples/rfh2_jms` directory (`tnt4j-streams-wmq` module).

Sample RFH2/JMS data binary dump is available in `rfh2_jms.bin` file.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="RFH2FoldersParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityXmlParser">
        <field name="PayloadDataType" locator="/rfh2Folders/mcd/Msd" locator-type="Label"/>
        <field name="DestinationQ" locator="/rfh2Folders/jms/Dst" locator-type="Label"/>
        <field name="StartTime" locator="/rfh2Folders/jms/Tms" locator-type="Label" datatype="Timestamp" units="Milliseconds"/>
    </parser>

    <parser name="JMSPayloadParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <field name="UserData" locator="*" locator-type="Label"/>
    </parser>

    <parser name="RFH2Parser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityRFH2Parser">
        <property name="TranslateNumValues" value="true"/>
        <property name="UseActivityDataAsMessageForUnset" value="false"/>

        <field name="EventType" value="EVENT"/>
        <field name="EventName" value="IBM MQ RFH2/JMS Payload"/>
        <field name="FoldersData" locator="rfh2Folders" locator-type="Label" transparent="true">
            <parser-ref name="RFH2FoldersParser" aggregation="Merge"/>
        </field>

        <field name="JMS_Payload" locator="jmsMsgPayload" locator-type="Label" transparent="true">
            <parser-ref name="JMSPayloadParser" aggregation="Merge"/>
        </field>
    </parser>

    <stream name="RFH2JMSFileStream" class="com.jkoolcloud.tnt4j.streams.inputs.BytesInputStream">
        <property name="FileName" value="./tnt4j-streams-wmq/samples/rfh2_jms/rfh2_jms.bin"/>
        <property name="RestoreState" value="false"/>

        <parser-ref name="RFH2Parser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `RFH2JMSFileStream` referencing `RFH2Parser` shall be used. Stream reads message entries from 
`./tnt4j-streams-wmq/samples/rfh2_jms/rfh2_jms.bin` file contents and passes it to parser. Since `ActivityRFH2Parser` is extension of map 
parser it produces map having two entries with predefined keys: `rfh2Folders` for RFH2 folders data XML string, and `jmsMsgPayload` for JMS 
message payload de-serialized object or bytes if serialisation can't be done.

`RFH2Parser` resolves RFH2 folders and JMS message payload data into predefined fields `rfh2Folders` and `jmsMsgPayload`. Values of these 
fields are passed to `RFH2FoldersParser` and `JMSPayloadParser` parsers for further parsing into fields of `EVENT` named 
`IBM_MQ_RFH2/JMS_PAYLOAD`.

`RFH2FoldersParser` parser uses XPath expressions to resolve values.
`JMSPayloadParser` copies all JMS MapMessage entries as fields of `EVENT` named `IBM_MQ_RFH2/JMS_PAYLOAD`.

#### JSR-203 FileSystem

##### SFTP file feed

This sample shows how to stream `sftp:` file system provided file characters feed data as activity events.

Sample files can be found in `samples/sftp-file-feed` directory (`tnt4j-streams-fs` module).

See sample [data source configuration](./tnt4j-streams-fs/samples/sftp-file-feed/tnt-data-source.xml).

##### SCP(SSH) file feed

This sample shows how to stream `scp:` file system (over SSH) provided file characters feed data as activity events.

Sample files can be found in `samples/ssh-file-feed` directory (`tnt4j-streams-fs` module).

See sample [data source configuration](./tnt4j-streams-fs/samples/ssh-file-feed/tnt-data-source.xml).

##### SCP(SSH) file lines

This sample shows how to stream `scp:` file system (over SSH) provided file lines as activity events.

Sample files can be found in `samples/ssh-file-lines` directory (`tnt4j-streams-fs` module).

See sample [data source configuration](./tnt4j-streams-fs/samples/ssh-file-lines/tnt-data-source.xml).

##### Zip file lines

This sample shows how to stream `zip:` file system provided file lines as activity events.

Sample files can be found in `samples/zip-fs-file-lines` directory (`tnt4j-streams-fs` module).

`sample.zip` file contains set of compressed Apache access log files.

See sample [data source configuration](./tnt4j-streams-fs/samples/zip-fs-file-lines/tnt-data-source.xml).

#### Fluentd logs streaming

TODO

#### Integrating TNT4J-Streams into custom API

See [`Readme.md`](tnt4j-streams-samples/README.md) of `tnt4j-streams-samples` module.

## How to use TNT4J loggers

### tnt4j-log4j12

* in `config/log4j.properties` file change log appender to
`log4j.appender.tnt4j=com.jkoolcloud.tnt4j.logger.log4j.TNT4JAppender`. Note that there should be on line like
`log4j.appender.tnt4j=` in this file, so please comment or remove all others if available.
* in `pom.xml` file of `core` change dependencies - uncomment:
```xml
    <dependency>
        <groupId>com.jkoolcloud.tnt4j.logger</groupId>
        <artifactId>tnt4j-log4j12</artifactId>
        <version>0.3.1</version>
        <scope>runtime</scope>
    </dependency>
```
* when running `TNT4J-Streams` use system property `-Dlog4j.configuration` to define `log4j.properties` file location, e.g.: 
`-Dlog4j.configuration="file:./config/log4j.properties"`.

### tnt4j-logback

* make logback configuration file `config/logback.xml`.
* in `pom.xml` file of `core` change dependencies - uncomment:
```xml
    <dependency>
        <groupId>com.jkoolcloud.tnt4j.logger</groupId>
        <artifactId>tnt4j-logback</artifactId>
        <version>0.3</version>
        <scope>runtime</scope>
    </dependency>
```
and comment out log4j dependencies
* when running `TNT4J-Streams` use system property `-Dlogback.configurationFile` to define `logback.xml` file location, e.g.:
`-Dlog4j.configuration="file:./config/logback.xml"`. Change `bin/tnt-streams.bat` or `bin/tnt-streams.sh` file to pass logback configuration 
to Java:

`bat` file:
```
set LOGBACKOPTS=-Dlogback.configurationFile="file:%RUNDIR%..\config\logback.xml"
"%JAVA_HOME%\bin\java" %LOGBACKOPTS% %TNT4JOPTS% ...
```
`sh` file:
```
LOGBACKOPTS=-Dlogback.configurationFile="file:$RUNDIR/../config/logback.xml"
"$JAVA_HOME/bin/java" $LOGBACKOPTS $TNT4JOPTS
```

Configuring TNT4J-Streams
======================================

TNT4J-Streams configuration sources may be:
* configuration files - like `tnt4j.properties`, `log4j.properties`, `tnt-data-source.xml`, etc.
* ZooKeeper nodes data - see [ZooKeeper stored configuration](#zookeeper-stored-configuration) chapter for more details.

Configuration data format is same for all sources now.

## TNT4J configuration

Because TNT4J-Streams is based on TNT4J first you need to configure TNT4J (if have not done this yet).
Default location of `tnt4j.properties` file is in project `config` directory. At least you must make one change:
`event.sink.factory.Token:YOUR-TOKEN` replace `YOUR-TOKEN` with [jKoolCloud](https://www.jkoolcloud.com/) token assigned for you.

To define `tnt4j.properties` file location use system property `-Dtnt4j.config`, e.g., `-Dtnt4j.config="./config/tnt4j.properties"`.

For more information on TNT4J and `tnt4j.properties` see [TNT4J Wiki page](https://github.com/Nastel/TNT4J/wiki/Getting-Started) and 
[TNT4J README](https://github.com/Nastel/TNT4J/blob/master/README.md).

Details on JESL related configuration can be found in [JESL README](https://github.com/Nastel/JESL/blob/master/README.md).

### Basic TNT4J configuration of TNT4J-Streams

Filling in configuration template:
* Replace `<YOUR XXXX>` value placeholder with actual value corresponding to your environment.
* Replace `<YOUR EVENT SINK CONFIGURATION>` placeholder with actual configuration of streamed activity events sink you wish to use.

**NOTE:** It is **NOT RECOMMENDED** to use `BufferedEventSinkFactory` (or any other asynchronous sinks) with TNT4J-Streams. Streams and 
sinks are meant to act in sync, especially when sink (e.g., `JKCloud`, `Mqtt`, `Kafka`) consumer uses network communication.
```properties
# Stanza used for TNT4J-Streams sources
{
    source: com.jkoolcloud.tnt4j.streams
    source.factory: com.jkoolcloud.tnt4j.source.SourceFactoryImpl
    source.factory.GEOADDR: <YOUR GEO ADDRESS>
    source.factory.DATACENTER: <YOUR DATA CENTER NAME>
    source.factory.RootFQN: RUNTIME=?#SERVER=?#NETADDR=?#DATACENTER=?#GEOADDR=?
    source.factory.RootSSN: tnt4j-streams

    tracker.factory: com.jkoolcloud.tnt4j.tracker.DefaultTrackerFactory
    dump.sink.factory: com.jkoolcloud.tnt4j.dump.DefaultDumpSinkFactory
    tracker.default.snapshot.category: TNT4J-Streams-event-snapshot

    # event sink configuration: destination and data format
    <YOUR EVENT SINK CONFIGURATION: jKoolCloud, Kafka, MQTT, etc.>

    event.formatter: com.jkoolcloud.tnt4j.format.JSONFormatter
    #event.formatter.Newline: true

    # Configure default sink filter based on level and time (elapsed/wait)
    ##event.sink.factory.Filter: com.jkoolcloud.tnt4j.filters.EventLevelTimeFilter
    ##event.sink.factory.Filter.Level: TRACE
    # Uncomment lines below to filter out events based on elapsed time and wait time
    # Timed event/activities greater or equal to given values will be logged
    ##event.sink.factory.Filter.ElapsedUsec: 100
    ##event.sink.factory.Filter.WaitUsec: 100

    tracking.selector: com.jkoolcloud.tnt4j.selector.DefaultTrackingSelector
    tracking.selector.Repository: com.jkoolcloud.tnt4j.repository.FileTokenRepository
}
```

#### [jKoolCloud](https://www.jkoolcloud.com/) sink configuration

```properties
    #### jKoolCloud event sink factory configuration ####
    event.sink.factory: com.jkoolcloud.jesl.tnt4j.sink.JKCloudEventSinkFactory
    event.sink.factory.LogSink: file:./logs/tnt4j-streams-activities.log

    event.sink.factory.Url: http://data.jkoolcloud.com:6580
    #event.sink.factory.Url: https://data.jkoolcloud.com:6585
    #event.sink.factory.Url: https://data.jkoolcloud.com
    event.sink.factory.Token: <YOUR TOKEN>
    #### jKoolCloud event sink factory configuration end ####
```

##### Configuring activities log to be rolling (over SLF4J)

If your `TNT4J` event sink configuration has `Filename` property, e.g.:
```properties
#### jKoolCloud event sink factory configuration ####
event.sink.factory: com.jkoolcloud.jesl.tnt4j.sink.JKCloudEventSinkFactory
event.sink.factory.Filename: ./logs/tnt4j-streams-activities.log
...
```
then all streamed activities are logged into one text file just by adding logged activity to the end of that file. But over some time file 
can grow in size dramatically - gigabytes of log. To overcome this issue, for `SocketEventSink` and `JKCloudEventSink` it is possible to 
configure activities log to be logged over `LOG4J/SLF4J` making activities log to be rolled upon your demand - daily or hitting size limit.

Configuration requires two steps:
1. alter `log4j.properties` file:
    1. add dedicated appender
    ```properties
    ### branch for sink written activity entities logger ###
    log4j.appender.activities_log=org.apache.log4j.RollingFileAppender
    log4j.appender.activities_log.File=logs/tnt4j-streams-activities.log
    log4j.appender.activities_log.maxFileSize=10MB
    log4j.appender.activities_log.maxBackupIndex=3
    log4j.appender.activities_log.layout=org.apache.log4j.PatternLayout
    log4j.appender.activities_log.layout.ConversionPattern=%m%n
    ```

    2. setup logger
    ```properties
    #### streamed activity entities logger ####
    log4j.logger.com.jkoolcloud.tnt4j.streams.activities_log=INFO, activities_log
    log4j.additivity.com.jkoolcloud.tnt4j.streams.activities_log=false
    ```

2. alter `tnt4j.properties` file:
    1. define `LogSink` for `SocketEventSink` or `JKCloudEventSink` (replace previous `event.sink.factory.Filename` definition)
    ```properties
    #### jKoolCloud event sink factory configuration ####
    event.sink.factory: com.jkoolcloud.jesl.tnt4j.sink.JKCloudEventSinkFactory
    #event.sink.factory.Filename: ./logs/tnt4j-streams-activities.log
    ##### streamed activity entities logging over SLF4J sink #####
    ##### NOTE: logger name should match log4j.properties defined logger name mapped to use 'activities_log' appender #####
    event.sink.factory.LogSink: slf4j:com.jkoolcloud.tnt4j.streams.activities_log
    ```

#### Kafka sink configuration

```properties
    #### Kafka event sink factory configuration ####
    event.sink.factory: com.jkoolcloud.tnt4j.sink.impl.kafka.KafkaEventSinkFactory
    event.sink.factory.propFile: <YOUR PATH>/tnt4j-kafka.properties
    event.sink.factory.topic: <YOUR TOPIC>
    #### Kafka event sink factory configuration end ####
```

#### MQTT sink configuration

```properties
    #### MQTT event sink factory configuration ####
    event.sink.factory: com.jkoolcloud.tnt4j.sink.impl.mqtt.MqttEventSinkFactory
    event.sink.factory.mqtt-server-url: <YOUR MQTT SERVER ULR> #
    event.sink.factory.mqtt-topic: <YOUR TOPIC> #
    event.sink.factory.mqtt-user: <MQTT-USER> #
    event.sink.factory.mqtt-pwd: <MQTT-PWD>
    #### MQTT event sink factory configuration end ####
```

## Stream data source configuration

Streams can be configured using XML document having root element `tnt-data-source`. Definition of XML configuration
can be found in `tnt-data-source.xsd` file located in project `config` directory.

To define streams data source configuration file location, use program argument `-f:` e.g.:
```cmd
     tnt4j-streams -f:./tnt4j-streams-core/samples/single-log/tnt-data-source.xml
```
Or you can refer streams data source configuration file over System property named `tnt4j.streams.config` e.g.:
```properties
    -Dtnt4j.streams.config=./tnt4j-streams-core/samples/single-log/tnt-data-source.xml
```

**NOTE:** `TNT4J-Streams` can perform `tnt-data-source` configuration XML validation against XSD schema. Found validation failures are 
listed in `TNT4J-Streams` log as `WARNING` level entries. To enable this XML-XSD validation use system property 
`com.jkoolcloud.tnt4j.streams.validate.config`:
```properties
    -Dcom.jkoolcloud.tnt4j.streams.validate.config=true
```

Program argument `-p:` is used in common with `PipedStream` and only parsers configuration from `<tnt-data-source/>` definition is used. See 
[OS piped stream](#os-piped-stream).

Program argument `-z:` is used to define configuration file for ZooKeeper based TNT4J-Streams configuration. 
See [Loading ZooKeeper stored configuration data](#loading-zookeeper-stored-configuration-data) and [Streams registry](#streams-registry).

sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="TokenParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser">
        <property name="FieldDelim" value="|"/>
        <field name="StartTime" locator="1" format="dd MMM yyyy HH:mm:ss" locale="en-US"/>
        <field name="ServerIp" locator="2"/>
        <field name="ApplName" value="orders"/>
        <field name="Correlator" locator="3"/>
        <field name="UserName" locator="4"/>
        <field name="EventName" locator="5"/>
        <field name="EventType" locator="5">
            <field-map source="Order Placed" target="START"/>
            <field-map source="Order Received" target="RECEIVE"/>
            <field-map source="Order Processing" target="OPEN"/>
            <field-map source="Order Processed" target="SEND"/>
            <field-map source="Order Shipped" target="END"/>
        </field>
        <field name="MsgValue" locator="8"/>
    </parser>

    <stream name="FileStream" class="com.jkoolcloud.tnt4j.streams.inputs.FileLineStream">
        <property name="FileName" value="orders.log"/>
        <!--<property name="RangeToStream" value="1:"/>-->

        <parser-ref name="TokenParser"/>
    </stream>
</tnt-data-source>
```

As you can see from sample configuration, there are two major configuration elements defined `parser` and `stream`.
Because streams configuration is read using SAX parser referenced entities should be initialized before it is used.
Note that `stream` uses `parser` reference:
```xml
    <stream name="FileStream" class="com.jkoolcloud.tnt4j.streams.inputs.FileLineStream">
        <.../>
        <parser-ref name="TokenParser"/>
    </stream>
```
That is why sequence of configuration elements is critical and can't be swapped.

**NOTE:** streams data source configuration token `<property>` supports resolving dynamic value from these sources (and in that particular 
sequence of resolution, stopping on first non-null value):
* Java system properties
* OS environment variables (ignore property name case)
* TNT4J properties

To define dynamic property value resolution define property using pattern:
```xml
    <property name="PropName" value="${env.property.name}"/>
```
where `env.property.name` is property name from one of defined sources set, e.g.:
```xml
    <!-- Java system properties -->
    <property name="UsedJava" value="${java.version}"/>
    <!-- OS environment variables -->
    <property name="JavaHome" value="${JAVA_HOME}"/>
    <!-- TNT4J properties -->
    <property name="SinkLogFile" value="${event.sink.factory.Filename}"/>
```

It is also possible to use dynamic property value resolution for a fragment of property value:
```xml
    <property name="FileName" value="${user.dir}/data/access.log"/>
```

### Streams configuration

#### Generic streams parameters

##### Stream executors related parameters

These parameters are applicable to all types of streams.

 * `UseExecutors` - identifies whether stream should use executor service to process activities data items asynchronously
 or not. Default value - `false`. (Optional)
    * `ExecutorThreadsQuantity` - defines executor service thread pool size. Default value - `4`. (Optional) Actual only if `UseExecutors` 
    is set to `true`
    * `ExecutorsTerminationTimeout` - time to wait (in seconds) for a executor service to terminate. Default value - `20sec`. (Optional) 
    Actual only if `UseExecutors` is set to `true`
    * `ExecutorsBoundedModel` - identifies whether executor service should use bounded tasks queue model. Default value - `false`. (Optional) 
    Actual only if `UseExecutors` is set to `true`
        * `ExecutorRejectedTaskOfferTimeout` - time to wait (in seconds) for a task to be inserted into bounded queue if max. queue size is 
        reached. Default value - `20sec`. (Optional) Actual only if `ExecutorsBoundedModel` is set to `true`.
* `PingLogActivityCount` - defines repetitive number of streamed activity entities to put "ping" log entry with stream statistics. Default 
value - `-1` meaning `NEVER`. (Optional, can be OR'ed with `PingLogActivityDelay`.
* `PingLogActivityDelay` - defines repetitive interval in seconds between "ping" log entries with stream statistics. Default value - `-1` 
meaning `NEVER`. (Optional, can be OR'ed with `PingLogActivityCount`.

    sample:
```xml
    <property name="UseExecutors" value="true"/>
    <property name="ExecutorThreadsQuantity" value="5"/>
    <property name="ExecutorsTerminationTimeout" value="20"/>
    <property name="ExecutorsBoundedModel" value="true"/>
    <property name="ExecutorRejectedTaskOfferTimeout" value="20"/>
    <!-- to define "ping" log entry on every 200th streamed activity entity, or if 30sec. elapsed since last "ping" entry -->
    <property name="PingLogActivityCount" value="200"/>
    <property name="PingLogActivityDelay" value="30"/>
```

##### Stream cache related parameters

* `MaxSize` - max. capacity of stream resolved values cache. Default value - `100`. (Optional)
* `ExpireDuration` - stream resolved values cache entries expiration duration in minutes. Default value - `10`. (Optional)
* `Persisted` - flag indicating cache contents has to be persisted to file on close and loaded on initialization. Default value - `false`. 
(Optional)
* `FileName` - defines file name to persist cache entries as XML. Default value - `./persistedCache.xml`. (Optional)
* `PersistingPeriod` - cache persisting period in seconds. Value `<= 0` disables periodic persisting. Default value - `0`. (Optional)

    sample:
```xml
    <property name="MaxSize" value="500"/>
    <property name="ExpireDuration" value="30"/>
    <property name="Persisted" value="true"/>
    <property name="FileName" value="./storage/MyStreamCache.xml"/>
    <property name="PersistingPeriod" value="300"/>
```

##### Parseable streams parameters

These parameters are applicable to streams which uses parsers to parse incoming Raw activity data.

 * `HaltIfNoParser` - if set to `true`, stream will halt if none of the parsers can parse activity object Raw data.
 If set to `false` - puts log entry and continues. Default value - `false`. (Optional)
 * `GroupingActivityName` - name of ACTIVITY entity used to group excel workbook streamed events. (Optional)

    sample:
```xml
    <property name="HaltIfNoParser" value="true"/>
    <property name="GroupingActivityName" value="Events from XLSX file"/>
```

##### Buffered streams parameters

 * `BufferSize` - maximal buffer queue capacity. Default value - `1024`. (Optional)
 * `BufferDropWhenFull` - flag indicating to drop buffer queue offered Raw activity data entries when queue gets full. 
 Default value - `false`. (Optional)

     sample:
 ```xml
     <property name="BufferSize" value="2048"/>
     <property name="BufferDropWhenFull" value="true"/>
 ```

##### Stream output configuration parameters

Stream output can be configured using these configuration properties:

 * `TNT4JConfigFile` - path of `tnt4j.properties` file. May be used to override default TNT4J system property `tnt4j.config` defined value. 
Default value - `null`. (Optional)
 * `TNT4JProperty` - defines specific TNT4J configuration properties to be used by stream output. (Optional)
 * `TNT4JConfigZKNode` - defines ZooKeeper path where stream configuration is located. Default value - ``. (Optional)
 * `RetryStateCheck` - flag indicating whether tracker state check should be perform repeatedly. If `false`, then streaming process exits with 
 `java.lang.IllegalStateException`. Default value - `false`. (Optional)
 * `ResolveServerFromDNS` - flag indicating whether to resolve activity entity host name/IP from DNS server. Default value - `false`. (Optional)
 * `SplitRelatives` - flag indicating whether to send activity entity child entities independently merging data from both parent and child 
 entity fields into produced entity. Default value - `false`. (Optional). **NOTE**: This value has alias `TurnOutActivityChildren` left for 
 backward compatibility, but it is not recommended to use it anymore and it should be changed right away for existing configurations.
 * `BuildSourceFQNFromStreamedData` - flag indicating whether to set streamed activity entity `Source` FQN build from activity fields data 
 instead of default on configured in `tnt4j.properties`. Default value - `true`. (Optional)
 * `SourceFQN` - `Source` FQN pattern to be used when building it from streamed activity entity fields values. 
 Format is: `SourceType1=${FieldName1}#SourceType2=${FieldName2}#SourceType3=${FieldName3}...`. 
 Default value - `APPL=${ApplName}#USER=${UserName}#SERVER=${ServerName}#NETADDR=${ServerIp}#GEOADDR=${Location}`. (Optional)
 * `SendStreamStates` - flag indicating whether to send stream status change messages (`startup`/`shutdown`) to output endpoint e.g. 
 [jKoolCloud](https://www.jkoolcloud.com/). Default value - `true`. (Optional)

     sample:
 ```xml
     <property name="TNT4JConfigFile" value="../../configuration/tnt4j.properties"/>
     <tnt4j-properties>
         <property name="event.formatter" value="com.jkoolcloud.tnt4j.streams.utils.RedirectTNT4JStreamFormatter"/>
     </tnt4j-properties>
     <property name="TNT4JConfigZKNode" value="/samples/core/logstash"/>
     <property name="RetryStateCheck" value="true"/>
     <property name="ResolveServerFromDNS" value="true"/>
     <property name="SplitRelatives" value="true"/>
     <property name="BuildSourceFQNFromStreamedData" value="false"/>
     <property name="SourceFQN" value="APPL=${ApplName}#USER=${UserName}#SERVER=${ServerName}"/>
     <property name="SendStreamStates" value="false"/> 
 ```

**NOTE:** stream output configuration parameters can be defined under `stream` tag (will drill down to default stream output instance), or 
under `java-object` tag referring output type class and referred from `stream` like this:
```xml
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="ProgressEventParser">
            ...
        </parser>

    <stream name="WmqStream" class="com.jkoolcloud.tnt4j.streams.inputs.WmqStream">
        <property name="SplitRelatives" value="true"/>
        ...

        <parser-ref name="ProgressEventParser"/>
    </stream>
</tnt-data-source>
```
or
```xml
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <java-object name="WmqStreamOutput" class="com.jkoolcloud.tnt4j.streams.outputs.JKCloudActivityOutput">
        <property name="SplitRelatives" value="true"/>
    </java-object>

    <parser name="ProgressEventParser">
        ...
    </parser>

    <stream name="WmqStream" class="com.jkoolcloud.tnt4j.streams.inputs.WmqStream">
        ...

        <parser-ref name="ProgressEventParser"/>

        <reference name="WmqStreamOutput"/>
    </stream>
</tnt-data-source>
```

##### Parser reference related parameters

* Attributes:
    * `tags` - tags list (delimited using `,`) for a parser. Tags are used to map incoming parseable data package with parser instance 
    dedicated to parse it.

    sample:
```xml
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="ProgressEventParser">
        ...
    </parser>

    <stream name="WmqStream" class="com.jkoolcloud.tnt4j.streams.inputs.WmqStream">
        <property name="SplitRelatives" value="true"/>
        ...

        <parser-ref name="ProgressEventParser" tags="Kafka,Http"/>
    </stream>
</tnt-data-source>
```

* Tags:
    * `matchExp` - defines context or data match criteria (expression) for a parser reference. If match criteria does not match incoming 
    data or parsing context, that parser will be not applied to parse the data.

    sample:
```xml
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="ProgressEventParser">
        ...
    </parser>

    <stream name="WmqStream" class="com.jkoolcloud.tnt4j.streams.inputs.WmqStream">
        <property name="SplitRelatives" value="true"/>
        ...

        <parser-ref name="ProgressEventParser">
            <matchExp>regex:(([a-zA-Z]*):)?(.+)</matchExp>
        </parser-ref>
    </stream>
</tnt-data-source>
```

###### Parser matching data or parsing context

It is possible to define context or data match criteria for a parser reference. It allows to apply parser on provided data only when data or 
parsing context matches defined evaluation expression. One parser reference can have multiple match expressions. In that case **all of them 
must evaluate to positive match** for parser to be applied. **NOTE:** when parser reference has no match expressions defined, only 
incompatible data type prevents it from being applied.

Sample on field stacked parsers binding with match expressions:
```xml
    <field name="MessageFormats" locator="MQGACF_ACTIVITY_TRACE.MQBACF_MESSAGE_DATA" locator-type="Label" datatype="String"
           format="string" transparent="true">
        <parser-ref name="BNYM_XML_Msg_Data_Parser" aggregation="Merge">
            <matchExp>xpath:contains(/Request/messageDetails/tag21, 'VOLUME')</matchExp>
        </parser-ref>
        <parser-ref name="Test_7_Mixed_NV_Offsets_Lines_Parser" aggregation="Merge">
            <matchExp>jpath:[?(@.Request.header.receiver == 'SEB')]</matchExp>
            <matchExp>${ObjectName} == "PAYMENTS_QUEUE"</matchExp>
        </parser-ref>
        <parser-ref name="SWIFT_2_Parser" aggregation="Merge">
            <matchExp>contains(:)</matchExp>
        </parser-ref>
    </field>
```

**NOTE:** if match expression definition breaks configuration XML validity, surround it with `<![CDATA[]]>` e.g.:
```xml
        <parser-ref name="Test_7_Mixed_NV_Offsets_Lines_Parser">
            <matchExp><![CDATA[
                jpath:[?(@.Request.header.receiver == 'SEB')]
            ]]></matchExp>
            <matchExp><![CDATA[
                ${ObjectName} == "PAYMENTS_QUEUE"
            ]]></matchExp>
        </parser-ref>
```

Types of data match evaluation expressions:
* `String` (default) - it must contain a method name from the `StringUtils` class and arguments. The method's first parameter is implied 
and not specified; it's always bound to the value of the message `data` being parsed. Only subsequent parameters need to be defined in the 
expression e.g. `contains(PAYMENT)`, `isEmpty()`. To negate the expression result, start the expression definition with the `!` negate 
character.

Some samples: 
```xml
<parser-ref ...>
    <matchExp>isAlpha()</matchExp> <!-- NOTE: default type is applied-->
    <matchExp>string:startsWithIgnoreCase(SWIFT)</matchExp>
    <matchExp>string:!isEmpty()</matchExp>
    <matchExp>string:contains(SWIFT)</matchExp>
</parser-ref>
```
* `RegEx` - should contain any valid Regular Expression (RegEx). **Positive match** is got when RegEx expression pattern matches some 
subsequence within provided `data` string.
```xml
<parser-ref ...>
    <matchExp>regex:(:33A:)(.*)(:24B:)(.*)</matchExp>
    <matchExp>regex:((TID)=(.[^\s:,%]*))|((USER_DATA)=(.[^\s:,%]*))</matchExp>
    <matchExp>regex:(([a-zA-Z]*):)?(.+)</matchExp>
</parser-ref>
```
* `JsonPath` - should contain any valid [JsonPath](https://github.com/json-path/JsonPath) expression. If expression evaluates as `boolean`, 
then this value is returned. If expression evaluates as `collcetion`, then to get **positive match** it must be **not empty** . In other 
cases to get **positive match**, expression should be evaluated as **non-null object**.
```xml
<parser-ref ...>
    <matchExp>jpath:$.connected</matchExp>
    <matchExp>jpath:$.[?(@.id == 'sensor03')]</matchExp>
    <matchExp>jpath:$.[?(@.temperature > 70)]</matchExp>
</parser-ref>
```
* `XPath` - should contain any valid XPath expression. If expression evaluates as `boolean`, then this value is returned. In other cases to 
get **positive match**, expression should be evaluated as **non-empty string**.
```xml
<parser-ref ...>
    <matchExp>xpath:contains(/Request/messageDetails/tag21, 'VOLUME')</matchExp>
    <matchExp>xpath:/Request/genericTransFields/receiver</matchExp>
    <matchExp>xpath:/Request/header[currency='EUR']/messageId</matchExp>
</parser-ref>
```
* `Groovy` or `JavaScript` script based expressions. To access field value (data passed to parser) use predefined variable placeholder 
`$fieldValue`.
```xml
<parser-ref ...>
    <matchExp>groovy:$fieldValue instanceof Map</matchExp>
    <matchExp>javascript:$fieldValue.length > 300</matchExp>
</parser-ref>
```

Parsing context is treated as parsers resolved fields values. So that is why context expressions should contain variables referencing 
activity entity field names.

Types of context evaluation expressions:
* `Groovy` (default) - should contain valid `Groovy` language based **boolean** expression.
```xml
<parser-ref ...>
    <matchExp>${ObjectName}.startsWith("SYSTEM.")</matchExp> <!-- NOTE: default type is applied-->
    <matchExp>groovy:${ObjectName} == "PAYMENTS_QUEUE"</matchExp>
    <matchExp>groovy:${QueueDepth} >= 500</matchExp>
</parser-ref>
```
* `JavaScript` - should contain valid `JavaScript` language based **boolean** expression.
```xml
<parser-ref ...>
    <matchExp>js:${ObjectName}.toUpperCase().indexOf("PAYMENT") == 0</matchExp>
    <matchExp>jscript:${ObjectName} != null</matchExp>
    <matchExp>javascript:${QueueDepth} != 500</matchExp>
</parser-ref>
```
* `XPath` -  should contain valid `XPath` based **boolean** expression.
```xml
<parser-ref ...>
    <matchExp>xpath:boolean(${ObjectStatus})</matchExp>
    <matchExp>xpath:${EventName} = 'foo'</matchExp>
    <matchExp>xpath:${PaymentSum} >= 500</matchExp>
    <matchExp>xpath:boolean(ts:getFileName(${FilePath}))</matchExp>
</parser-ref>
``` 
The last match expression uses our custom XPath function `getFileName` wrapped with the XPath `boolean` function
to test if a file name was found.

There are many XPath functions that can be used. See section [TNT4J-Streams predefined custom XPath functions](#tnt4j-streams-predefined-custom-xpath-functions) 
and Oracle's [Using XPath Functions](https://docs.oracle.com/cd/E35413_01/doc.722/e35419/dev_xpath_functions.htm#autoId18) reference.

#### File line stream parameters (also from Hdfs)

 * `FileName` - the system-dependent file name or file name pattern defined using wildcard characters `*` and `?`. (Required)
 * `FilePolling` - flag `true/false` indicating whether files should be polled for changes or not. If not, then files
 are read from oldest to newest sequentially one single time. Default value - `false`. (Optional)
    * `FileReadDelay` - delay in seconds between file reading iterations. Actual only if `FilePolling` property is set to `true`. Default 
    value - `15sec`. (Optional)
 * `RestoreState` - flag `true/false` indicating whether files read state should be stored and restored on stream restart. Note, if 
 `StartFromLatest` is set to `false` - read state storing stays turned on, but previous stored read state is reset (no need to delete state 
 file manually). Default value - `false`. (Optional)
 * `StartFromLatest` - flag `true/false` indicating that streaming should be performed from latest file entry line. If `false` - then all 
 lines from available files are streamed on startup. Actual only if `FilePolling` or `RestoreState` properties are set to `true`. Default 
 value - `true`. (Optional)
 * `RangeToStream` - defines the colon-separated range of file line numbers that should be parsed and streamed to jKoolCloud. Default 
 value - `1:`. (Optional)
 * `ActivityDelim` - defines activities data delimiter used by stream. Value can be: `EOL` - end of line, `EOF` - end of file/stream, or any 
 user defined symbol or string. Default value - `EOL`. (Optional)
 * `KeepLineSeparators` - flag indicating whether to return line separators at the end of read line. Default value - `false`. (Optional)
 * `TruncatedFilePolicy` - defines truncated file (when size of the file decreases while it is streamed) access policy. Value can be: 
 `START_FROM_BEGINNING` - read file from beginning, `CONTINUE_FROM_LAST` - continue reading file from last line (skipping all available 
 lines). Default value - `START_FROM_BEGINNING`. (Optional)

    sample:
 ```xml
    <property name="FileName" value="C:/Tomcat_7_0_34/logs/localhost_access_log.*.txt"/>
    <property name="FileReadDelay" value="5"/>
    <property name="StartFromLatest" value="true"/>
    <property name="FilePolling" value="true"/>
    <property name="RestoreState" value="true"/>
    <property name="RangeToStream" value="12:125"/>
    <property name="ActivityDelim" value="-----"/>
    <property name="KeepLineSeparators" value="true"/>
 ```

In case using Hdfs file name is defined using URL like `hdfs://[host]:[port]/[path]`. Path may contain wildcards.

Also see ['Generic streams parameters'](#generic-streams-parameters) and ['Buffered streams parameters'](#buffered-streams-parameters).

#### Characters/Bytes feed stream parameters

 * `FileName` - the system-dependent file name. (Required - just one `FileName` or `Port`)
 * `Port` - port number to accept character stream over TCP/IP. (Required - just one `FileName` or `Port`)
 * `RestartOnInputClose` - flag indicating to restart stream if input socked gets closed. Default value - `false`. (Optional)

    sample:
```xml
    <property name="FileName" value="messages.json"/>
```
or
```xml
    <property name="Port" value="9595"/>
    <property name="RestartOnInputClose" value="true"/>
```

Also see ['Generic streams parameters'](#generic-streams-parameters).

**NOTE:** there can be ony one parser referenced to this stream.

#### JSR-203 FileSystem streams parameters

Supported file systems:
 * `SCP` - schemes `ssh.unix:`, `scp:` and `ssh:`
 * `SFTP` - scheme `sftp:`
 * `ZIP` - schemes `zip:` and `jar:`
 * `FILE` - scheme `file:`
 * any other JSR-203 compliant (requires manually add file system implementing libs to classpath), but those are not tested and may not 
 work "out of the box"

General stream configuration parameters:
 * `Host` - remote machine host name/IP address. Default value - `localhost`. (Optional - can be defined over stream property `FileName`, 
 e.g. `ftp://username:password@hostname:port/[FILE_PATH]`
 * `Port` - remote machine port number to accept connection. (Optional - can be defined over stream property `FileName`, e.g. 
 `ftp://username:password@hostname:port/[FILE_PATH]`
 * `UserName` - remote machine authorized user name. (Optional - can be defined over stream property `FileName`, e.g. 
 `ftp://username:password@hostname:port/[FILE_PATH]`
 * `Password` - remote machine authorized user password. (Optional - can be defined over stream property `FileName`, e.g. 
 `ftp://username:password@hostname:port/[FILE_PATH]`
 * `Scheme` - file access protocol scheme name. Default value - `scp`. (Optional - can be defined over stream property `FileName`, e.g. 
 `ssh.unix:///[FILE_PATH]`
 * `StrictHostKeyChecking` - flag indicating whether strictly check add remote host keys changes against ssh know hosts.
    * If flag value is set to `yes`, ssh will never automatically add host keys to the `~/.ssh/known_hosts` file and will refuse to connect 
    to a host whose host key has changed. This provides maximum protection against trojan horse attacks, but can be troublesome when the 
    `/etc/ssh/ssh_known_hosts` file is poorly maintained or connections to new hosts are frequently made. This option forces the user to 
    manually add all new hosts.
    * If flag value is set to `no`, ssh will automatically add new host keys to the user known hosts files.

    The host keys of known hosts will be verified automatically in all cases. The flag value must be set to `yes` or  `no`. Default value - 
    `no`. (Optional)
 * `IdentityFromPrivateKey` - private key file to be used for remote machine secure connection initialization. (Optional)
 * `KnownHosts` - known hosts file path. (Optional)
 * `ResolveAbsolutePath` - flag indicating whether to resolve absolute file path when relative one is provided. Default value - `false`. 
 (Optional)
 * set of target JSR-203 compliant `FileSystemProvider` supported properties

   sample:
```xml
    <property name="Host" value="172.16.6.26"/>
    <property name="Port" value="22"/>
    <property name="UserName" value="osboxes"/>
    <property name="Password" value="slabs"/>
    <property name="Scheme" value="sftp"/>
    <property name="StrictHostKeyChecking" value="no"/>
    <property name="KnownHosts" value="/home/joe/.ssh/known_hosts"/>
    <property name="IdentityFromPrivateKey" value="/home/joe/.ssh/id_dsa"/>
    <property name="ResolveAbsolutePath" value="true"/>
```

##### JSR-203 File Characters/Bytes feed stream parameters

JSR-203 File Characters feed stream class name is `com.jkoolcloud.tnt4j.streams.inputs.FileSystemCharacterStream`.

JSR-203 File Bytes feed stream class name is `com.jkoolcloud.tnt4j.streams.inputs.FileSystemByteInputStream`.

 * `FileName` - the system-dependent file name: path or URI. (Required)
 * `RestartOnInputClose` - flag indicating to restart stream if input socked gets closed. Default value - `false`. (Optional)

    sample:
```xml
    <property name="FileName" value="ssh.unix:///home/osboxes/single-log/orders.log"/>
    <property name="RestartOnInputClose" value="true"/>
```

Also see [JSR-203 FileSystem streams parameters](#jsr-203-filesystem-streams-parameters) and ['Generic streams parameters'](#generic-streams-parameters).

##### JSR-203 File line stream parameters

JSR-203 File Line stream class name is `com.jkoolcloud.tnt4j.streams.inputs.FileSystemLineStream`.

 * `FileName` - the system-dependent file name or file name pattern defined using wildcard character `*`: path or URI. (Required)

    sample:
```xml
    <property name="FileName" value="ssh.unix:///home/osboxes/single-log/orders.log"/>
```

Also see [JSR-203 FileSystem streams parameters](#jsr-203-filesystem-streams-parameters) and ['File line stream parameters (also from Hdfs)'](#file-line-stream-parameters-also-from-hdfs)

#### Standard Java input stream parameters

 * `InputCloseable` - flag indicating if stream has to close input when stream is closing. Default value - `true`. (Optional)

    sample:
```xml
    <property name="InputCloseable" value="false"/>
```

Also see ['Generic streams parameters'](#generic-streams-parameters) and ['Parseable streams parameters'](#parseable-streams-parameters).

#### OS Piped stream parameters

This stream does not have any additional configuration parameters.

Also see ['Standard Java input stream parameters'](#standard-java-input-stream-parameters).

#### Http stream parameters

 * `Port` - port number to run Http server. Default value - `8080`. (Optional)
 * `UseSSL` - flag indicating to use SSL. Default value - `false`. (Optional)
    * `Keystore` - keystore path. (Optional) Actual only if `UseSSL` is set to `true`.
    * `KeystorePass` - keystore password. (Optional) Actual only if `UseSSL` is set to `true`.
    * `KeyPass` - key password. (Optional) Actual only if `UseSSL` is set to `true`.

    sample:
```xml
    <property name="Port" value="8081"/>
    <property name="UseSSL" value="true"/>
    <property name="Keystore" value="path_to_keystore_file"/>
    <property name="KeystorePass" value="somePassword"/>
    <property name="KeyPass" value="somePassword"/>
```

Also see ['Generic streams parameters'](#generic-streams-parameters) and ['Buffered streams parameters'](#buffered-streams-parameters).

#### JMS stream parameters

 * `java.naming.provider.url` - JMS server URL. (Required)
 * `Queue` - queue destination name or names delimited using `,` char. (Required - at least one of `Queue` or `Topic`)
 * `Topic` - topic destination name or names delimited using `,` char. (Required - at least one of `Queue` or `Topic`)
 * `java.naming.factory.initial` - JNDI context factory name. (Required)
 * `JMSConnFactory` - JMS connection factory name. (Required)
 * list of JNDI context configuration properties supported by JMS server implementation. See `javax.naming.Context` for more details. 
 (Optional)

    sample:
```xml
    <property name="java.naming.provider.url" value="tcp://localhost:61616"/>
    <property name="Topic" value="topic.SampleJMSTopic,topic.OtherSampleJMSTopic"/>
    <property name="java.naming.factory.initial" value="org.apache.activemq.jndi.ActiveMQInitialContextFactory"/>
    <property name="JMSConnFactory" value="ConnectionFactory"/>
    <parser-ref name="SampleJMSParser"/>
```
or
```xml
    <property name="java.naming.provider.url" value="tcp://localhost:61616"/>
    <property name="Queue" value="queue.SampleJMSQueue,queue.OtherSampleJMSQueue"/>
    <property name="java.naming.factory.initial" value="org.apache.activemq.jndi.ActiveMQInitialContextFactory"/>
    <property name="JMSConnFactory" value="ConnectionFactory"/>
    <parser-ref name="SampleJMSParser"/>
```

Also see ['Generic streams parameters'](#generic-streams-parameters) and ['Buffered streams parameters'](#buffered-streams-parameters).

#### Kafka stream parameters

 * `Topic` - topic name to listen. (Required)
 * `StartServer` - flag indicating if stream has to start Kafka server on startup. Default value - `false`. (Optional)
 * `StartZooKeeper` - flag indicating if stream has to start ZooKeeper server on startup. Default value - `false`. (Optional)
 * List of properties used by Kafka API, e.g., `zookeeper.connect`, `group.id`. See `kafka.consumer.ConsumerConfig` class for more
 details on Kafka consumer properties.

    sample:
```xml
    <property name="Topic" value="TNT4JKafkaTestTopic"/>
    <property name="StartServer" value="true"/>
    <property name="StartZooKeeper" value="true"/>
    <property name="zookeeper.connect" value="127.0.0.1:2181/tnt4j_kafka"/>
    <property name="group.id" value="0"/>
```

Also see ['Generic streams parameters'](#generic-streams-parameters) and ['Parseable streams parameters'](#parseable-streams-parameters).

#### MQTT stream parameters

 * `ServerURI` - Mqtt server URI. (Required)
 * `TopicString` - the topic to subscribe to, which can include wildcards. (Required)
 * `UserName` - authentication user name. (Optional)
 * `Password` - user password. (Optional)
 * `UseSSL` - flag indicating to use SSL. Default value - `false`. (Optional)
    * `Keystore` - keystore path. (Optional) Actual only if `UseSSL` is set to `true`.
    * `KeystorePass` - keystore password. (Optional) Actual only if `UseSSL` is set to `true`.

    sample:
```xml
    <property name="ServerURI" value="tcp://localhost:1883"/>
    <property name="Topic" value="TNT4JStreams"/>
    <property name="UserName" value="someUser"/>
    <property name="Password" value="somePassword"/>
    <property name="UseSSL" value="true"/>
    <property name="Keystore" value="path_to_keystore_file"/>
    <property name="KeystorePass" value="somePassword"/>
```

Also see ['Generic streams parameters'](#generic-streams-parameters) and ['Buffered streams parameters'](#buffered-streams-parameters).

#### WMQ Stream parameters

 * `QueueManager` - Queue manager name. (Optional)
 * `Queue` - Queue name. (Required - at least one of `Queue`, `Topic`, `Subscription`, `TopicString`)
 * `Topic` - Topic name. (Required - at least one of `Queue`, `Topic`, `Subscription`, `TopicString`)
 * `Subscription` - Subscription name. (Required - at least one of `Queue`, `Topic`, `Subscription`, `TopicString`)
 * `TopicString` - Topic string. (Required - at least one of `Queue`, `Topic`, `Subscription`, `TopicString`)
 * `Host` - WMQ connection host name. Alias for `CMQC.HOST_NAME_PROPERTY` Queue Manager connection property. (Optional)
 * `Port` - WMQ connection port number. Alias for `CMQC.PORT_PROPERTY` Queue Manager connection property. Default value - `1414`. (Optional)
 * `UserName` - WMQ user identifier. Alias for `CMQC.USER_ID_PROPERTY` Queue Manager connection property. (Optional)
 * `Password` - WMQ user password. Alias for `CMQC.PASSWORD_PROPERTY` Queue Manager connection property. (Optional)
 * `Channel` - Server connection channel name. Alias for `CMQC.CHANNEL_PROPERTY` Queue Manager connection property. Default value - 
 `SYSTEM.DEF.SVRCONN`. (Optional)
 * `StripHeaders` - identifies whether stream should strip WMQ message headers. Default value - `true`. (Optional)
 * `StreamReconnectDelay` - delay in seconds between queue manager reconnection or failed queue GET iterations. Default value - `15sec`. 
 (Optional)
 * `OpenOptions` - defines open options value used to access queue or topic. It can define numeric options value or concatenation of MQ 
 constant names/values delimited by `|` symbol. If options definition starts with `!`, it means that this options set should be used as 
 complete and passed to Queue Manager without changes. By default these open options are appended to predefined set of:

    Predefined set of open options for queue:
    * MQOO_FAIL_IF_QUIESCING
    * MQOO_INPUT_AS_Q_DEF
    * MQOO_SAVE_ALL_CONTEXT
    * MQOO_INQUIRE

    Predefined set of open options for topic:
    * MQSO_FAIL_IF_QUIESCING
    * MQSO_CREATE
    * MQSO_MANAGED - if subscription name is empty
    * MQSO_RESUME - if subscription name is defined

    (Optional)

 * `CMQC.XXXXXXX_PROPERTY` - any `CMQC` defined Queue Manager connection property. You can define multiple `CMQC` connection properties per 
 `stream` definition, but only one per `property` definition. (Optional)

    sample:
```xml
    <property name="QueueManager" value="QMGR"/>
    <property name="Queue" value="SYSTEM.ADMIN.TRACE.ACTIVITY.QUEUE"/>
    <property name="Host" value="wmq.sample.com"/>
    <property name="Port" value="1420"/>
    <property name="UserName" value="Administrator"/>
    <property name="Password" value="someUserPass"/>
    <property name="Channel" value="SYSTEM.DEF.SVRCONN2"/>
    <property name="StripHeaders" value="false"/>
    <property name="StreamReconnectDelay" value="30"/>
    <property name="OpenOptions" value="!MQSO_FAIL_IF_QUIESCING|MQSO_CREATE|MQSO_MANAGED|MQSO_WILDCARD_CHAR"/>
    <property name="CMQC.USE_MQCSP_AUTHENTICATION_PROPERTY" value="true"/>
    <property name="CMQC.THREAD_AFFINITY_PROPERTY" value="false"/>
```

Also see ['Generic streams parameters'](#generic-streams-parameters).

##### WMQ Trace Events Stream parameters

 * `TraceOperations` - defines traced MQ operations name filter mask (wildcard or RegEx) to process only traces of MQ operations which names 
 matches this mask. Default value - `*`. (Optional)
 * `ExcludedRC` - defines set of excluded MQ trace events reason codes (delimited using `|` character) to process only MQ trace events having 
 reason codes not contained in this set. Set entries may be defined using both numeric and MQ constant name values. 
 Default value - ``. (Optional)
 * `SuppressBrowseGets` - flag indicating whether to exclude WMQ BROWSE type GET operation traces from streaming. Default value - `false`. 
 (Optional)

    sample:
```xml
    <property name="TraceOperations" value="MQXF_(GET|PUT|CLOSE)"/>
    <property name="ExcludedRC" value="MQRC_NO_MSG_AVAILABLE|30737"/>
    <property name="SuppressBrowseGets" value="true"/>
```

Also see ['WMQ Stream parameters'](#wmq-stream-parameters).

#### Zipped file line stream parameters (also from Hdfs)

 * `FileName` - defines zip file path and concrete zip file entry name or entry name pattern defined using characters `*`
 and `?`. Definition pattern is `zipFilePath!entryNameWildcard`. I.e.:
 `./tnt4j-streams-core/samples/zip-stream/sample.zip!2/*.txt`. (Required)
 * `ArchType` - defines archive type. Can be one of: `ZIP`, `GZIP`, `JAR`. Default value - `ZIP`. (Optional)

    sample:
```xml
    <property name="FileName" value="./tnt4j-streams-core/samples/zip-stream/sample.gz"/>
    <property name="ArchType" value="GZIP"/>
```

In case using Hdfs file name is defined using URL like `hdfs://[host]:[port]/[path]`. Zip entry name may contain
wildcards.

Also see ['Generic streams parameters'](#generic-streams-parameters).

#### WS (web service) Streams parameters

All WS streams have a special configuration section, called a `scenario`. Streaming scenarios allow for defining steps. A `step` defines 
request/invocation/execution parameters and scheduler. Steps are invoked/executed independently of each other.

 * `scenario` tag has required attribute - `name` (any string).
    * `step` tag has required attribute - `name` (any string) and optional attributes `url` (service request URL),
    `method` (`GET`/`POST` - default value `GET`).
        * `schedule-cron` tag has attributes:
            * `expression` - Cron expression. (Required)
            * `startDelay` - schedule start delay interval as positive integer (zero valid) numeric value. Default value - `0`. (Optional) 
            * `startDelayUnits` - schedule start delay time units name. Default value - `SECONDS`. (Optional)
        * `schedule-simple` tag has required attributes: 
            * `interval` - schedule interval as positive integer (non-zero) numeric value. (Required) 
            * `units` - schedule interval time units name. Default value - `MILLISECONDS`. (Optional) 
            * `repeatCount` - schedule repeats count as integer numeric value, where `-1` means endless. Default value - `1`. (Optional)
            * `startDelay` - schedule start delay interval as positive integer (zero valid) numeric value. Default value - `0`. (Optional) 
            * `startDelayUnits` - schedule start delay time units name. Default value - `SECONDS`. (Optional)
        * `request` is XML tag to define string represented request data (e.g., system command with parameters). To
        define XML contents it is recommended to use `<![CDATA[]]>`. It has optional attribute `parser-ref` to map received response data 
        and parser to parse it.

    sample:
```xml
    <!-- Sample scenario for RESTful services request -->
    <scenario name="Sample REST stream scenario">
        <step name="Step Kaunas"
            url="http://api.openweathermap.org/data/2.5/weather?q=Kaunas&amp;APPID=fa1fede9cbd6e26efdea1cdcbc714069&amp;units=metric"
            method="GET" >
            <schedule-cron expression="0/15 * * * * ? *"/>
        </step>
        <.../>
        <step name="Step Klaipeda"
              url="http://api.openweathermap.org/data/2.5/weather?q=Klaipeda&amp;APPID=fa1fede9cbd6e26efdea1cdcbc714069&amp;units=metric"
              method="GET">
            <schedule-simple interval="45" units="Seconds" repeatCount="10"/>
        </step>
    </scenario>
    <!-- Sample scenario for SOAP services request -->
    <scenario name="Sample WS stream scenario">
        <step name="Step 1"
              url="http://wsdot.wa.gov/traffic/api/WeatherInformation/WeatherInformation.svc">
            <schedule-simple interval="35" units="Seconds" repeatCount="10"/>
            <request>
                <![CDATA[
                    SOAPAction:http://tempuri.org/IWeatherInformation/GetCurrentWeatherInformationByStationID
                    <tem:GetCurrentWeatherInformationByStationID xmlns:tem="http://tempuri.org/">
                        <tem:AccessCode>aeb652b7-f6f5-49e6-9bdb-e2b737ebd507</tem:AccessCode>
                        <tem:StationID>1909</tem:StationID>
                    </tem:GetCurrentWeatherInformationByStationID>
                ]]>
            </request>
        </step>
    </scenario>
    <!-- Sample scenario for System Cmd invocation -->
    <scenario name="Sample CMD stream scenario">
        <step name="Step Windows">
            <request>typeperf "\Processor(_Total)\% Processor Time" -sc 1</request>
            <schedule-simple interval="25" units="Seconds" repeatCount="-1"/>
        </step>
    </scenario>
    <!-- Sample of request and parser mapping -->
    <scenario name="RabbitMQ Sampling scenario">
        <step name="All RabbitMQ metrics">
            <schedule-simple interval="30" units="Seconds" repeatCount="-1"/>

            <request>
                python rabbitmqadmin -f raw_json list users

                <parser-ref name="UsersRespParser"/>
            </request>
            <request>
                python rabbitmqadmin -f raw_json list vhosts

                <parser-ref name="HostsRespParser"/>
            </request>
            <request>
                python rabbitmqadmin -f raw_json list overview

                <parser-ref name="OverviewRespParser"/>
            </request>
        </step>
    </scenario>
    <!-- Sample scenario for JDBC query invocation -->
    <scenario name="Sample DB2-JDBC stream scenario">
        <step name="Step Query1" url="jdbc:db2://[HOST]:50000/SB2BIDB" username="[USER_NAME]" password="[USER_PASS]">
            <schedule-simple interval="60" units="Seconds" repeatCount="-1" startDelay="3" startDelayUnits="Minutes"/>

            <request>
                <![CDATA[
                    SELECT *
                    FROM TRANS_DATA
                    WHERE CREATION_DATE > ?
                    ORDER by CREATION_DATE DESC
                    FETCH FIRST 100 ROWS ONLY
                ]]>
                <req-param id="1" value="${LastRecordCDate}" type="TIMESTAMP"/>

                <parser-ref name="SampleResultSetParser"/>
            </request>
        </step>
    </scenario>
```

##### WsStream parameters

 * `DisableSSL` - flag indicating that stream should disable SSL context verification. Default value - `false`. (Optional)
 * `SynchronizeRequests` - flag indicating that stream issued WebService requests shall be synchronized and handled in configuration 
 defined sequence - waiting for prior request to complete before issuing next. Default value - `false`. (Optional)
 * List of custom WS Stream requests configuration properties. Put variable placeholder in request/step configuration (e.g. `${WsEndpoint}`) 
 and put property with same name into stream properties list (e.g. `<property name="WsEndpoint" value="https://192.168.3.3/ws"/>`) to have 
 value mapped into request data. (Optional)

    sample:
```xml
    <property name="DisableSSL" value="true"/>
    <property name="SynchronizeRequests" value="true"/>
    <!-- Custom WS request properties -->
    <property name="WsEndpoint" value="https://192.168.3.3/ws"/>
    ...
```

###### CastIronWsStream

 * `SecurityCachedTokenKey` - defines streams cache entry key referring `login` request received session ID token. Default value - `Token`.
 (Optional)
 * `SecurityResponseParserTag` - defines tag value used to map `login` request data and parser used to parse it. Default value - `login`. 
 (Optional)

    sample:
```xml
    <property name="SecurityCachedTokenKey" value="SessionIDToken"/>
    <property name="SecurityResponseParserTag" value="loginData"/>
```

Also see ['Generic streams parameters'](#generic-streams-parameters) and ['Buffered streams parameters'](#buffered-streams-parameters).

##### RestStream parameters

 * `MaxTotalPoolConnections` - defines the maximum number of total open connections in the HTTP connections pool. Default value - `5`. 
 (Optional)
 * `DefaultMaxPerRouteConnections` - defines the maximum number of concurrent connections per HTTP route. Default value - `2`. (Optional)

    sample:
```xml
    <property name="MaxTotalPoolConnections" value="10"/>
    <property name="DefaultMaxPerRouteConnections" value="4"/>
```

Also see ['Generic streams parameters'](#generic-streams-parameters) and ['Buffered streams parameters'](#buffered-streams-parameters).

##### CmdStream parameters

This stream does not have any additional configuration parameters.

Also see ['Generic streams parameters'](#generic-streams-parameters) and ['Buffered streams parameters'](#buffered-streams-parameters).

##### JDBCStream parameters

 * `DropRecurrentResultSets` - flag indicating whether to drop streaming stream input buffer contained recurring result sets, when stream 
 input scheduler invokes JDBC queries faster than they can be processed (parsed and sent to sink, e.g. because of sink/JKool limiter 
 throttling). Default value - `false`. (Optional)
 * List of `JDBC` driver supported properties used to invoke ['DriverManager.getConnection(String, Properties)'](https://docs.oracle.com/javase/8/docs/api/java/sql/DriverManager.html#getConnection-java.lang.String-java.util.Properties-). 
 (Optional)
 * when `UseExecutors` is set to `true` and `ExecutorThreadsQuantity` is greater than `1`, value for that property is reset to `1` since 
 `java.sql.ResultSet` can't be accessed in multi-thread manner.

    sample:
 ```xml
     <property name="DropRecurrentResultSets" value="true"/>
 ```

Also see ['Generic streams parameters'](#generic-streams-parameters) and ['Buffered streams parameters'](#buffered-streams-parameters).

#### Redirect TNT4J Stream parameters

 * `FileName` - the system-dependent file name. (Required - just one `FileName` or `Port`)
 * `Port` - port number to accept character stream over TCP/IP. (Required - just one `FileName` or `Port`)
 * `RestartOnInputClose` - flag indicating to restart Server Socket (open new instance) if listened one gets closed or fails to accept 
 connection. (Optional)
 * `BufferSize` - maximal buffer queue capacity. Default value - `1024`. (Optional)
 * `BufferDropWhenFull` - flag indicating to drop buffer queue offered Raw activity data entries when queue gets full. 
 Default value - `false`. (Optional)

    sample:
```xml
    <property name="FileName" value="tnt4j-stream-activities.log"/>
```
or
```xml
    <property name="Port" value="9009"/>
    <property name="RestartOnInputClose" value="true"/>
    <property name="BufferSize" value="2048"/>
    <property name="BufferDropWhenFull" value="true"/>
```

Also see ['Generic streams parameters'](#generic-streams-parameters).

#### Ms Excel Stream generic parameters

 * `FileName` - the system-dependent file name of MS Excel document. (Required)
 * `SheetsToProcess` - defines workbook sheets name filter mask (wildcard or RegEx) to process only sheets which names
 matches this mask. (Optional)
 * `WorkbookPassword` - excel workbook password. (Optional)

    sample:
```xml
    <property name="FileName" value="./tnt4j-streams-msoffice/samples/xlsx-rows/sample.xlsx"/>
    <property name="SheetsToProcess" value="Sheet(1|8|12)"/>
    <property name="WorkbookPassword" value="xlsPass"/>
```

Also see ['Generic streams parameters'](#generic-streams-parameters) and ['Parseable streams parameters'](#parseable-streams-parameters).

##### Ms Excel Rows Stream parameters

 * `RangeToStream` - defines the colon-separated range of spreadsheet row numbers that should be parsed and streamed to jKoolCloud. Default 
 value - `1:`. (Optional)

    sample:
```xml
    <property name="RangeToStream" value="5:30"/>
```

Also see ['Ms Excel Stream generic parameters'](#ms-excel-stream-generic-parameters).

#### Elastic Beats Stream parameters

 * `Host` - host name to bind for stream started Logstash server. Default value - `localhost`. (Optional)
 * `Port` - port number to bind for stream started Logstash server. Default value - `5044`. (Optional)
 * `SSLCertificateFilePath` - SSL certificate file path. (Optional)
 * `SSLKeyFilePath` - SSL key file path. (Optional)
 * `PassPhrase` - SSL key pass phrase. (Optional)
 * `Timeout` - connection timeout in seconds. Default value - `30`. (Optional)
 * `ThreadCount` - number of threads used by Logstash server. Default value - `1`. (Optional)

Also see ['Generic streams parameters'](#generic-streams-parameters) and ['Buffered streams parameters'](#buffered-streams-parameters).

### Parsers configuration

#### Generic parser parameters

##### Attributes

 * `manualFieldsOrder` - flag indicating whether configuration defined fields order shall be preserved in streaming process. By default 
 fields are ordered by value resolution type: RAW activity data values, activity entity fields values, cache entries values. Default 
 value - `false`.
 * `default-data-type` - specifies default `datatype` attribute value bound for all fields/locators of this parser. Default 
 value - `String`. See See ['TNT4J Events field mappings'](#tnt4j-events-field-mappings) for attribute `datatype` details.
 * `default-emptyAsNull` - specifies default `emptyAsNull` attribute value for all fields/locators of this parser. Default value - `true`.

    sample:
```xml
    <parser name="TestParser" class="com.my.company.ActivityTestParser" manualFieldsOrder="true" default-data-type="Generic" default-emptyAsNull="false">
    ...
    </parser>
```

##### Properties

 * `UseActivityDataAsMessageForUnset` - flag indicating weather Raw activity data shall be put into field `Message` if there is no mapping 
 defined for that field in stream parser configuration or value was not resolved by parser from Raw activity data. **NOTE:** it is 
 recommended to use it for **DEBUGGING** purposes only. For a production version of your software, remove this property form stream parser 
 configuration. Default value - `false`. (Optional)
 * `ActivityDelim` - defines activities delimiter symbol used by parsers. Value can be one of: `EOL` - end of line, or `EOF` - end of 
 file/stream. Default value - `EOL`. (Optional)
 * `RequireDefault` - indicates that all parser fields/locators by default are required to resolve to non-null values. Default value - 
 `false`. (Optional). The `field` attribute `required="true"` (or `false`) may be used to take precedence over the `RequireDefault` 
 property. See `required` attribute definition in ['TNT4J Events field mappings'](#tnt4j-events-field-mappings).
 * `AutoArrangeFields` - flag indicating parser fields shall be automatically ordered by parser to ensure references sequence. When `false`, 
 fields shall maintain user parser configuration defined order. **NOTE:** it is alias for parser configuration attribute 
 `manualFieldsOrder`. Default value - `true`. (Optional)

    sample:
```xml
    <property name="UseActivityDataAsMessageForUnset" value="true"/>
    <property name="ActivityDelim" value="EOF"/>
    <property name="RequireDefault" value="true"/>
    <property name="AutoArrangeFields" value="false"/>
```

#### Activity Name-Value parser

 * `FieldDelim` - fields separator. Default value - `,`. (Optional)
 * `ValueDelim` - value delimiter. Default value - `=`. (Optional)
 * `Pattern` - pattern used to determine which types of activity data string this parser supports. When `null`, all
 strings are assumed to match the format supported by this parser. Default value - `null`. (Optional)
 * `StripQuotes` - whether surrounding double quotes should be stripped from extracted data values. Default value -
 `true`. (Optional)
 * `EntryPattern` - pattern used to to split data into name/value pairs. It should define two RegEx groups named `key` and `value` used to 
 map data contained values to name/value pair. **NOTE:** this parameter takes preference on `FieldDelim` and `ValueDelim` parameters. 
 (Optional)

    sample:
```xml
    <property name="FieldDelim" value=";"/>
    <property name="ValueDelim" value="-"/>
    <property name="Pattern" value="(\S+)"/>
    <property name="StripQuotes" value="false"/>
    <property name="EntryPattern"><![CDATA[:(?<key>.*?):(?<value>.[^:]+)]]></property>
```

Also see [Generic parser parameters](#generic-parser-parameters).

#### Activity RegEx parser

 * `Pattern` - contains the regular expression pattern that each data item is assumed to match. (Required)
 * `MatchStrategy` - defines `pattern` created `matcher` comparison strategy used against input data string. Value can be one of: `MATCH` - 
 pattern should match complete input string, or `FIND` - pattern has to match sub-sequence within input string. Default value - `MATCH`. 
 (Optional)

    sample:
* index-capturing groups:
```xml
    <property name="Pattern" value="((\S+) (\S+) (\S+))"/>
    <property name="MatchStrategy" value="FIND"/>
```
* named-capturing groups:
```xml
    <property name="Pattern"><![CDATA[
        (?<CoID>.*)\.(?<ProcessArea>.*)\.(?<InterfaceID>.*)\.(?<HopNr>.*)
    ]]></property>
    <property name="MatchStrategy" value="MATCH"/>
```
**NOTE:** When `MatchStrategy=FIND` is used and regex returns multiple matches, it is possible to access an individual match group by 
defining the `Label` type locator match index (`1` can be omitted since it is the default) and a group descriptor (an index or name), 
following the match index after a period delimiter `.` e.g.,

* `locator="2.2" locator-type="Label"` will return the group indexed by `2` for match `2`
* `locator="3.groupName" locator-type="Label"` will return the group named `groupName` for match `3`

Note that the match index starts from `1`, while the group index starts from `0` (group `0` usually means `Full match`).

Also see [Generic parser parameters](#generic-parser-parameters).

#### Activity token parser

 * `FieldDelim` - fields separator. Default value - `,`. (Optional)
 * `Pattern` - pattern used to determine which types of activity data string this parser supports. When `null`, all
 strings are assumed to match the format supported by this parser. Default value - `null`. (Optional)
 * `StripQuotes` - whether surrounding double quotes should be stripped from extracted data values. Default value -
 `true`. (Optional)

    sample:
```xml
    <property name="FieldDelim" value=";"/>
    <property name="Pattern" value="(\S+)"/>
    <property name="StripQuotes" value="false"/>
```

Also see [Generic parser parameters](#generic-parser-parameters).

#### Activity XML parser

This parser uses XPath expressions as field locators. You may also use [TNT4J-Streams predefined custom XPath functions](#tnt4j-streams-predefined-custom-xpath-functions).

 * `Namespace` - additional XML namespace mappings. Default value - `null`. (Optional)
 * `NamespaceAware` - indicates that parser has to provide support for XML namespaces. Default value - `true`. (Optional)

    sample:
```xml
    <property name="Namespace" value="xsi=http://www.w3.org/2001/XMLSchema-instance"/>
    <property name="Namespace" value="tnt4j=https://jkool.jkoolcloud.com/jKool/xsds"/>
    <property name="NamespaceAware" value="false"/>
```

Also see [Generic parser parameters](#generic-parser-parameters).

#### Message activity XML parser

 * `SignatureDelim` - signature fields delimiter. Default value - `,`. (Optional)

    sample:
```xml
    <property name="SignatureDelim" value="#"/>
```

Also see ['Activity XML parser'](#activity-xml-parser) and [Generic parser parameters](#generic-parser-parameters).

##### MQ message signature calculation

For MQ messages it is possible to calculate message signature from message fields. To initiate signature calculation as a field value, 
`field` tag `value-type` attribute value has to be set to `signature`.

Sample of field definition for signature calculation:
```xml
    <field name="TrackingId" separator="#!#" value-type="signature"> 
        <field-locator locator="/messaging_operation/MsgType" locator-type="Label" datatype="Number"/> 
        <field-locator locator="/messaging_operation/MsgFormat" locator-type="Label"/> 
        <field-locator locator="/messaging_operation/MsgId" locator-type="Label" datatype="Binary" format="hexBinary"/> 
        <field-locator locator="/messaging_operation/MsgUser" locator-type="Label">
            <field-transform name="UserIdLowerCase" lang="groovy">
                StringUtils.lowerCase($fieldValue)
            </field-transform>
        </field-locator> 
        <field-locator locator="/messaging_operation/MsgPutApplType" locator-type="Label"/> 
        <field-locator locator="/messaging_operation/MsgPutApplName" locator-type="Label"/> 
        <field-locator locator="/messaging_operation/MsgPutDate" locator-type="Label"/> 
        <field-locator locator="/messaging_operation/MsgPutTime" locator-type="Label"/> 
        <field-locator locator="/messaging_operation/Correlator" locator-type="Label" datatype="Binary" format="hexBinary"/> 
    </field>
```

#### Apache access log parser

 * `LogPattern` - access log pattern. (Optional, if RegEx `Pattern` property is defined)
 * `ConfRegexMapping` - custom log pattern token and RegEx mapping. (Optional, actual only if `LogPattern` property is used)

    sample:
```xml
    <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b %D"/>
    <property name="ConfRegexMapping" value="%h=(\S+)"/>
    <property name="ConfRegexMapping" value="%*s=(\d{3})"/>
    <property name="ConfRegexMapping" value="%*r=(((\S+) (.*?)( (\S+)|()))|(-))"/>
    <property name="ConfRegexMapping" value="%*i=(.*?)"/>
```
 or defining complete RegEx pattern to match log lines:
```xml
    <property name="Pattern"
              value="^(\S+) (\S+) (\S+) \[([\w:/]+\s[+\-]\d{4})\] &quot;(((\S+) (.*?)( (\S+)|()))|(-))&quot; (\d{3}) (\d+|-)( (\S+)|$)"/>
```
 or defining named RegEx group mapping for log pattern token:
```xml
    <property name="ConfRegexMapping"><![CDATA[%*r=(?<request>((?<method>\S+) (?<uri>.*?)( (?<version>\S+))?)|(-))]]></property>
```

RegEx group names and log pattern tokens mapping:

|	Label (RegEx group name)	|	Format String	|	Description	|
|	---	|	---	|	---	|
|	address	|	%a	|	Client IP address of the request (see the mod_remoteip module).	|
|	local_address	|	%A	|	Local IP-address.	|
|	size	|	%B	|	Size of response in bytes, excluding HTTP headers.	|
|	size_clf	|	%b	|	Size of response in bytes, excluding HTTP headers. In CLF format, e.g., a '-' rather than a 0 when no bytes are sent.	|
|	cookie	|	%{VARNAME}C	|	The contents of cookie VARNAME in the request sent to the server. Only version 0 cookies are fully supported.	|
|	req_time	|	%D	|	The time taken to serve the request, in microseconds.	|
|	env_variable	|	%{VARNAME}e	|	The contents of the environment variable VARNAME.	|
|	filename	|	%f	|	Filename.	|
|	hostname	|	%h	|	Remote hostname. Will log the IP address if HostnameLookups is set to Off, which is the default. If it logs the hostname for only a few hosts, you probably have access control directives mentioning them by name. See the Require host documentation.	|
|	protocol	|	%H	|	The request protocol.	|
|	variable	|	%{VARNAME}i	|	The contents of VARNAME: header line(s) in the request sent to the server. Changes made by other modules (e.g. mod_headers) affect this. If you're interested in what the request header was prior to when most modules would have modified it, use mod_setenvif to copy the header into an internal environment variable and log that value with the %{VARNAME}edescribed above.	|
|	keep_alive	|	%k	|	Number of keepalive requests handled on this connection. Interesting if KeepAlive is being used, so that, for example, a '1' means the first keepalive request after the initial one, '2' the second, etc...; otherwise this is always 0 (indicating the initial request).	|
|	logname	|	%l	|	Remote logname (from identd, if supplied). This will return a dash unless mod_ident is present and IdentityCheck is set On.	|
|	method	|	%m	|	The request method.	|
|	note	|	%{VARNAME}n	|	The contents of note VARNAME from another module.	|
|	header_var	|	%{VARNAME}o	|	The contents of VARNAME: header line(s) in the reply.	|
|	port	|	%{format}p	|	The canonical port of the server serving the request, or the server's actual port, or the client's actual port. Valid formats are canonical, local, or remote.	|
|	process	|	%{format}P	|	The process ID or thread ID of the child that serviced the request. Valid formats are pid, tid, and hextid. hextid requires APR 1.2.0 or higher.	|
|	query	|	%q	|	The query string (prepended with a ? if a query string exists, otherwise an empty string).	|
|	line	|	%r	|	First line of request.	|
|	resp_handler	|	%R	|	The handler generating the response (if any).	|
|	status	|	%s	|	Status. For requests that have been internally redirected, this is the status of the original request. Use %>s for the final status.	|
|	time	|	%{format}t	|	The time, in the form given by format, which should be in an extended strftime(3) format (potentially localized). If the format starts with begin: (default) the time is taken at the beginning of the request processing. If it starts with end: it is the time when the log entry gets written, close to the end of the request processing. In addition to the formats supported by strftime(3), the following format tokens are supported:	|
|	req_time_ext	|	%{UNIT}T	|	The time taken to serve the request, in a time unit given by UNIT. Valid units are ms for milliseconds, us for microseconds, and s for seconds. Using s gives the same result as %Twithout any format; using us gives the same result as %D. Combining %T with a unit is available in 2.4.13 and later.	|
|	user	|	%u	|	Remote user if the request was authenticated. May be bogus if return status (%s) is 401 (unauthorized).	|
|	uri	|	%U	|	The URL path requested, not including any query string.	|
|	server_name	|	%v	|	The canonical ServerName of the server serving the request.	|
|	server_name_ext	|	%V	|	The server name according to the UseCanonicalName setting.	|
|	con_status	|	%X	|	Connection status when response is completed:	|
|	received	|	%I	|	Bytes received, including request and headers. Cannot be zero. You need to enable mod_logio to use this.	|
|	sent	|	%O	|	Bytes sent, including headers. May be zero in rare cases such as when a request is aborted before a response is sent. You need to enable mod_logio to use this.	|

Also see [Generic parser parameters](#generic-parser-parameters).

#### Activity map parser

 * `LocPathDelim` - locator path in map delimiter. Empty value means locator value should not be delimited into path elements. 
 Default value - `.`. (Optional)

    sample:
```xml
    <property name="LocPathDelim" value="/"/>
```

Also see [Generic parser parameters](#generic-parser-parameters).

##### Wildcard locators

**NOTE:** using locator path token value `*` (e.g. `locator="*"`) you can make parser to take all map entries from that level and output 
them as activity entity fields/properties by using map entry data as follows:
* map entry key  - field/property name
* map entry value - field/property value

Locator path token `*` can be omitted if last path token resolves to `java.util.Map` type value. However to get complete map for root path 
level you must define it `locator="*"` anyway, since locator value can't be empty.

**NOTE:** using locator path token value `#` (e.g. `locator="#"`) you can make parser to get all yet parser un-touched map entries from that 
level and output them as activity entity fields/properties by using map entry data as follows:
* map entry key  - field/property name
* map entry value - field/property value

This allows the user to map part of the entries manually and the remainder automatically. Consider a map having the following entries:
```
entry1: key=key1, value=value1
entry2: key=key2, value=value2
entry3: key=key3, value=value3
```
then using parser configuration:
```xml
<field name="Message" locator="key2" locator-type="Label"/> 
<field name="AllRestMapEntries" locator="#" locator-type="Label"/>
```
you'll get the following results:
```properties
Message=value2
key1=value1
key3=value3
```
Using `#` locator without any manual map entry mapping is equivalent to `*` locator.

#### Activity JSON parser

 * `ReadLines` - indicates that complete JSON data package is single line. Default value - `true`. (Optional, deprecated - use `ActivityDelim`
  of [Generic parser](#generic-parser-parameters) instead)

    sample:
```xml
    <property name="ReadLines" value="false"/>
```

Also see [Generic parser parameters](#generic-parser-parameters).

#### Activity JMS message parser

 * `ConvertToString` - flag indicating whether to convert message payload `byte[]` data to string. Applicable to `BytesMessage` and 
 `StreamMessage`. Default value - `false`. (Optional)

    sample:
```xml
    <property name="ConvertToString" value="true"/>
```

Additional JMS message fields and mappings are supported by these locators for this parser:
* `MsgMetadata` - JMS message metadata map containing these fields:
    * `Correlator` - message correlation identifier
    * `CorrelatorBytes` - message correlation identifier bytes value
    * `DeliveryMode` - message delivery mode number
    * `Destination` - destination name this message was received from
    * `Expiration` - message's expiration time
    * `MessageId` - message identifier string
    * `Priority` - message priority level number
    * `Redelivered` - indication flag of whether this message is being redelivered
    * `ReplyTo` - destination name to which a reply to this message should be sent
    * `Timestamp` - timestamp in milliseconds
    * `Type` - message type name supplied by the client when the message was sent
    * `CustomMsgProps` - map of custom JMS message properties added into JMS message while message was produced, so properties naming is 
    arbitrary upon message producer application.
* since JMS message parser is extension of ['Activity map parser'](#activity-map-parser), it supports map entry 
['Wildcard locators'](#wildcard-locators): 
    * `*` - maps all JMS message resolved map entries to activity entity data, e.g.:
    ```xml
          <field name="AllMsgCustomProps" locator="MsgMetadata.CustomMsgProps.*" locator-type="Label"/>
    ```
    this will add all `MsgMetadata.CustomMsgProps` map entries as JKool activity entity fields/properties without any additional manual 
    mapping, taking field/property name from map entry name and value from map entry value. The same way you can map all `MsgMetadata` map 
    entries into activity entity fields/properties (note omitted locator `.*` token, see ['Wildcard locators'](#wildcard-locators) for 
    details):
    ```xml
          <field name="MsgMetadata" locator="MsgMetadata" locator-type="Label"/>
    ```
    * `#` - maps set of unmapped JMS message resolved map entries to activity entity data, e.g.:
    ```xml
          <field name="Correlator" locator="MsgMetadata.Correlator" locator-type="Label"/>
          <field name="Destination" locator="MsgMetadata.Destination" locator-type="Label"/>
          <field name="MessageId" locator="MsgMetadata.MessageId" locator-type="Label"/>
          <field name="Priority" locator="MsgMetadata.Priority" locator-type="Label"/>
          <!-- automatically puts all unmapped message metadata map entries as custom activity properties -->
          <field name="AllRestMsgMetadataProps" locator="MsgMetadata.#" locator-type="Label"/>
    ```
    this will add all manually unmapped `MsgMetadata` map entries as JKool activity entity fields/properties, taking field/property name 
    from map entry name and value from map entry value. **NOTE:** Using `#` locator without any manual map entry mapping is equivalent to 
    `*` locator.

Also see ['Activity map parser'](#activity-map-parser) and [Generic parser parameters](#generic-parser-parameters).

#### Activity PCF parser

 * `TranslateNumValues` - indicates that parser should translate resolved numeric values to corresponding MQ constant names if possible and 
 field/locator data type is `String` (meaning translated value can be assigned to field). If value of particular field should be left as 
 number (e.g., `ReasonCode`), use field/locator attribute `datatype="Number"`. Default value - `true`. (Optional)
 * `SignatureDelim` - signature fields delimiter. Default value - `,`. (Optional)

     sample:
```xml
    <property name="TranslateNumValues" value="false"/>
    <property name="SignatureDelim" value="#"/>
```

**NOTE:** when PCF parameter contains binary (`byte[]`) value and locator data type is set to `String` having attribute `charset` undefined, 
conversion from binary to string value is performed using PCF parameter defined charset (`CCSID`). If PCF parameter provided charset 
parameter does not provide correct charset identifier (e.g. value is 0, but streams are run on different platform comparing to message 
source platform), then to convert binary value to String use `charset` attribute, e.g.:
```xml
    <field-locator locator="MQGACF_ACTIVITY_TRACE.MQBACF_MSG_ID" locator-type="Label" datatype="String" charset="IBM500"/>
```
and it would be equivalent to this transformation:
```xml
    <field name="Message" locator="MQGACF_ACTIVITY_TRACE.MQBACF_MSG_ID" locator-type="Label" datatype="Binary">
        <field-transform name="MsgIdToString" lang="groovy"><![CDATA[
            Utils.getString($fieldValue, "IBM500")
        ]]>
        </field-transform>
    </field>
```
Also to convert binary WMQ message payload to string using CCSIDs, use transformation like this:
```xml
    <field name="MQTrace_CodedCharsetId" locator="MQGACF_ACTIVITY_TRACE.MQIA_CODED_CHAR_SET_ID" locator-type="Label" datatype="Number"/>
    <field name="Message" locator="MQGACF_ACTIVITY_TRACE.MQBACF_MESSAGE_DATA" locator-type="Label" datatype="Binary">
        <field-transform name="MsgPayloadToString" lang="groovy"><![CDATA[
            WmqUtils.getString($fieldValue, ${MQTrace_CodedCharsetId})
        ]]>
        </field-transform>
    </field>
```
Or even more advanced case, stripping `DLH` and `XQH` structures data from binary data, leaving only message payload data as result:
```xml
   <field name="MQTrace_CodedCharsetId" locator="MQGACF_ACTIVITY_TRACE.MQIA_CODED_CHAR_SET_ID" locator-type="Label" datatype="Number"/>
   <field name="Message" locator="MQGACF_ACTIVITY_TRACE.MQBACF_MESSAGE_DATA" locator-type="Label" datatype="Binary">
       <field-transform name="MsgPayloadToString" lang="groovy"><![CDATA[
           WmqUtils.getString($fieldValue, ${MQTrace_CodedCharsetId}, WmqUtils.MQ_BIN_STR_STRIP_DLH_XQH)
       ]]>
       </field-transform>
   </field>
```
In this case if `DLH`/`XQH` headers data has CCSID value defined (` > 0`) within, then that CCSID value is used to convert payload from 
binary to String. If headers data does not define CCSID value, second parameter value is used.
Third function parameter is `DLH`/`XQH` headers handling options mask and be one of:
* `WmqUtils.MQ_BIN_STR_PRESERVE_DLH_XQH` - preserves `DLH`/`XQH` headers data
* `WmqUtils.MQ_BIN_STR_STRIP_DLH_XQH` - strips off `DLH`/`XQH` headers data

**NOTE:** `WmqUtils.getString` differs from `Utils.getString` over second parameter:
* `WmqUtils.getString` - second parameter is CCSID (numeric value) from WMQ defined set, or `null` to use WMQ system default encoding (in 
most cases it will be `UTF-8`)
* `Utils.getString` - second parameter is a [Java supported charset/encoding](https://docs.oracle.com/javase/7/docs/technotes/guides/intl/encoding.doc.html) 
name (in string format) for field Raw binary data; used to convert between Unicode and a number of other character encodings. If no second 
parameter is specified, the default value is the running streams JVM default charset (in most cases `UTF8`).

**NOTE:** A CCSID used in an expression should be a decimal number without a leading zero `0`, which in Java means an `octal` representation 
of the number; most likely not what you wanted. Example: Test if a received MQ message is `EBCDIC` code page `37`. If `037` (CCSID name 
alias) value is used, the comparison would be against decimal value `31`, not `37`, and would never match. Sample transformation:
```xml
    <field-transform name="MsgEbcdicOrNot" lang="groovy"><![CDATA[
        ${MQTrace_CodedCharsetId} == 37 ? $fieldValue : "MSG NOT EBCDIC"
    ]]>
    </field-transform>
```

Also see [Generic parser parameters](#generic-parser-parameters).

##### MQ message signature calculation

For MQ messages it is possible to calculate message signature from message fields. To initiate signature calculation as a field value, 
`field` tag `value-type` attribute value has to be set to `signature`.

Sample of field definition for signature calculation:
```xml
    <field name="Correlator" value-type="signature">
        <field-locator locator="MQGACF_ACTIVITY_TRACE.MQIACF_MSG_TYPE" locator-type="Label" datatype="Number"/>
        <field-locator locator="MQGACF_ACTIVITY_TRACE.MQCACH_FORMAT_NAME" locator-type="Label"/>
        <field-locator locator="MQGACF_ACTIVITY_TRACE.MQBACF_MSG_ID" locator-type="Label" datatype="String" format="bytes"/>
        <field-locator locator="MQCACF_USER_IDENTIFIER" locator-type="Label">
            <field-transform name="UserIdLowerCase" lang="groovy">
                StringUtils.lowerCase($fieldValue)
            </field-transform>
        </field-locator>
        <field-locator locator="MQIA_APPL_TYPE" locator-type="Label"/>
        <field-locator locator="MQCACF_APPL_NAME" locator-type="Label"/>
        <field-locator locator="MQGACF_ACTIVITY_TRACE.MQCACF_PUT_DATE" locator-type="Label"/>
        <field-locator locator="MQGACF_ACTIVITY_TRACE.MQCACF_PUT_TIME" locator-type="Label"/>
        <field-locator locator="MQGACF_ACTIVITY_TRACE.MQBACF_CORREL_ID" locator-type="Label" datatype="String" format="bytes"/>
    </field>
```

##### PCF parameter locators

PCF message can have grouped parameters - all messages will have header `MQCFH` and may have `MQCFGR` type parameters. 
* To access PCF message header fields use `MQCFH` expression with header field name delimited using `.` (e.g., `MQCFH.CompCode`).
* To access PCF message parameters use MQ constant name/value (e.g., `MQCACF_APPL_NAME` or `3024`).
* To access inner `MQCFGR` (or inner inner and so on) parameters use group parameter MQ constant name/value with grouped parameter MQ 
constant name/value delimited using `.` (e.g., `MQGACF_ACTIVITY_TRACE.MQIACF_COMP_CODE`).
* In case PCF parameter refers `MQI` structure, inner (or inner inner and so on) structure fields can be accessed by adding `.` delimited 
MQI structure fields names to locator path after root MQI structure parameter identifier (e.g., 
`MQGACF_ACTIVITY_TRACE.MQBACF_MQMD_STRUCT.MsgId` or `MQGACF_ACTIVITY_TRACE.MQBACF_MQCNO_STRUCT.clientConn.userIdentifier`).
* Additionally `PCFMessage` may contain `MQMD` data copied from transport `MQMessage`. To access `MQMD` values use `MQMD` expression with 
field name (or relative PCF parameter constant) delimited using `.` (e.g., `MQMD.PutApplName`, `MQMD.MQCACF_APPL_NAME` or `MQMD.3024`).

#### IBM MQ Error log entries parser

This parser has no additional configuration properties.

Also see [Activity map parser](#activity-map-parser) regarding higher level parser configuration.

This parser resolved data map may contain such entries:
* `Date` - resolved log entry date string
* `Time` - resolved log entry time string
* `Process` - resolved log entry process identifier
* `User` - resolved log entry user name
* `Program` - resolved log entry program (application) name
* `Host` - resolved log entry host name IBM MQ is running on
* `Installation` - resolved log entry IBM MQ installation name
* `VRMF` - resolved log entry running IBM MQ version descriptor
* `QMgr` - resolved log entry Queue manager error occurred on
* `ErrCode` - resolved log entry IBM MQ error code string
* `ErrText` - resolved log entry IBM MQ error message text
* `Explanation` - resolved log entry IBM MQ error explanation message text
* `Action` - resolved log entry IBM MQ error fix action message text
* `Where` - resolved log entry error descriptor location string containing source code file name and line number

IBM MQ versions starting `9.1` adds these additional entries:
* `Severity` - resolved log entry severity:
    * 'I' - INFO
    * 'W' - WARNING
    * 'E' - ERROR
    * 'S' - CRITICAL
    * 'T' - HALT
* `TimeUTC` - resolved log entry UTC time
* `CommentInsert(X)` - resolved log entry text attribute values, have been insert into log entry message, where `(X)` is attribute index
* `ArithInsert(X)` - resolved log entry numeric attribute values, have been insert into log entry message, where `(X)` is attribute index

#### Activity Java object (POJO) parser

* `SupportedClass` - defines class name of parser supported objects. Parser can have multiple definitions of this property. It is useful 
when just some specific set of objects has to be handled by this parser instead of all passed objects. (Optional)

    sample:
```xml
    <property name="SupportedClass" value="org.apache.kafka.common.TopicPartition"/>
    <property name="SupportedClass" value="com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.trace.KafkaTraceEventData"/>
```

Also see [Generic parser parameters](#generic-parser-parameters) regarding higher level parser configuration.

#### Activity String parser

This parser has no additional configuration properties.

Also see [Generic parser parameters](#generic-parser-parameters) regarding higher level parser configuration.

#### IBM MQ RFH2/JMS binary data parser

This parser has no additional configuration properties.

Also see [Activity map parser](#activity-map-parser) regarding higher level parser configuration.

This parser resolved data map may contain such entries:
* `rfh2Folders` - RFH2 folders data XML string. Root element for this XML is `<rfh2Folders>`. Further XPath based parsing can be processed 
by [Activity XML parser](#activity-xml-parser)
* `jmsMsgPayload` - JMS JMS message payload data: de-serialized object or bytes if serialisation can't be done.

#### Activity MS Excel parser

* `UseFormattedCellValue` - indicator flag stating to use formatted cell value (always `String`) as field/locator RAW data. When this flag 
is set to `true` - original cell value provided by Apache POI API is used e.g., making all numeric cells values as decimals (`double`) what 
is not very comfortable when entered cell value is `integer`. Default value - `false`. (Optional)

    sample:
```xml
    <property name="UseFormattedCellValue" value="true"/>
```

Also see [Generic parser parameters](#generic-parser-parameters) regarding higher level parser configuration.

#### Activity JDBC ResultSet parser

 * `SQLJavaMapping` - defines mapping from SQL type name (as `String`) to class (as `Class.forName(String)`) names in the Java programming 
 language, e.g. `NUMBER=java.lang.String`. Parser can have multiple definitions og this property. It is useful when default JDBC driver 
 mapping produces inaccurate result. **IMPORTANT:** if JDBC driver does not support `java.sql.ResultSet.getObject(int, Map)` or 
 `java.sql.ResultSet.getObject(String, Map)` implementation, leave SQL-Java types map empty! (Optional)

Also see [Generic parser parameters](#generic-parser-parameters) regarding higher level parser configuration.

### Pre-parsers

`TNT4J-Streams` architecture has entity `pre-parser` defining data transformation algorithm to convert Raw activity data to format, 
supported by stream used parser. Pre-parsers shall implement [`ActivityDataPreParser`](./tnt4j-streams-core/src/main/java/com/jkoolcloud/tnt4j/streams/preparsers/ActivityDataPreParser.java) 
interface.

It is possible to define multiple pre-parsers for same parser instance. In that case pre-parser are applied sequentially (sequence is 
defined by `reference` tags order within `parser` tag), where input of applied pre-parser is output of previous pre-parser.

#### XML From Binary Data Pre-parser

What is does:
* retrieves XML data from mixture of binary and string data (like `RFH2`)
* makes incomplete XML string fragment as complete (DOM valid) XML string

Sample stream configuration using `XMLFromBinDataPreParser` pre-parser:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <java-object name="XMLFromRFH2PreParser" class="com.jkoolcloud.tnt4j.streams.preparsers.XMLFromBinDataPreParser">
        <!--IF Raw ACTIVITY DATA STRING IS ENCODED - USE "format" PARAMETER. SEE BELOW -->
        <!--<param name="format" value="base64Binary" type="com.jkoolcloud.tnt4j.streams.fields.ActivityFieldFormatType"/>-->
        <!--<param name="format" value="base64Binary" type="java.lang.String"/>-->
    </java-object>

    <parser name="RFH2XMLParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityXmlParser">
        <reference name="XMLFromRFH2PreParser"/>

        <field name="EventType" value="Event"/>
        <field name="ApplName" value="Sample"/>
        <field name="ADPCount" locator="/root/usr/ADPSegCont" locator-type="Label"/>
    </parser>

    <stream name="FileStream" class="com.jkoolcloud.tnt4j.streams.inputs.BytesInputStream">
        <property name="FileName" value="./tnt4j-streams-core/samples/XML-from-bin-data/RFH2.dump"/>
        <property name="RestoreState" value="false"/>

        <parser-ref name="RFH2XMLParser"/>
    </stream>
</tnt-data-source>
```

Sample configuration defines `FileStream` reading binary data from `RFH2.dump` file. Stream refers activity xml parser `RFH2XMLParser`.

`RFH2XMLParser` parser has reference to `XMLFromRFH2PreParser`. This pre-parser builds valid XML from provided binary data having fragmented 
XML data like `RFH2` structure from IBM MQ.

**NOTE:** If Raw activity data is not `byte[]` but appears to be encoded (e.g., BASE64), use pre-parser property `foramat` to define Raw 
activity data format. It may be one of [`ActivityFieldFormatType`](./tnt4j-streams-core/src/main/java/com/jkoolcloud/tnt4j/streams/fields/ActivityFieldFormatType.java) 
enumeration values: 
* `base64Binary` - BASE64 encoded binary data
* `hexBinary` - binary data represented as bytes HEX codes sequence 
* `string` - binary data represented as plain string
* `bytes` - binary data as `byte[]` (default format value).

#### Transformation Pre-parser

What is does:
* applies user defined transformation bean or script code to convert/alter parser input Raw activity data

Used configuration properties:
* `id` - transformation identifier. (Optional)
* `lang` - transformation script language. (Optional)
* `script` - code of transformation script (can't be mixed with `beanRef`). (Required)
* `beanRef` - transformation bean reference (can't be mixed with `script`). (Required)

Sample stream configuration using `TransformationPreParser` pre-parser:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/tnt4j-streams-wmq/config/tnt-data-source-wmq_pcf.xsd">

    <java-object name="UnescapePreParser" class="com.jkoolcloud.tnt4j.streams.preparsers.TransformationPreParser">
        <property name="id" value="unescape"/>
        <property name="lang" value="groovy"/>
        <property name="script">
            <![CDATA[
               StringEscapeUtils.unescapeJava($fieldValue)
            ]]>
        </property>
    </java-object>

    <parser name="XML_Data_Parser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityXmlParser">
        <reference name="UnescapePreParser"/>

        <.../>
    </parser>
</tnt-data-source>
```

`XML_Data_Parser` parser has reference to `UnescapePreParser` to un-escape (remove excessive `\"`) stream provided Raw activity string data.

Also see [Field value transformations](#field-value-transformations) regarding use of streamed data transformations.
**NOTE:** `TransformationPreParser` does not support transformation property `phase`, since it is always applied on parser input Raw 
activity data before data gets parsed by enclosing parser.

### External resources import into stream configuration

`TNT4J-Streams` allows to have external resources importer to stream data source configuration.

External values mapping resources import sample:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <resource-ref id="MFT_MAPPINGS" type="ValuesMap" uri="./tnt4j-streams-wmq/samples/mft_fte/mft_mappings.json"/>
    <!--<resource-ref id="MFT_MAPPINGS_COMPCODE" type="ValuesMap" uri="./tnt4j-streams-wmq/samples/mft_fte/CompCode.csv" separator=","/>-->

    <parser name="ResMappingParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityXmlParser">
        <field name="EventType" locator="name(//*[1])" locator-type="Label">
            <field-map-ref resource="MFT_MAPPINGS.EventType"/>
        </field>
        <field name="EventName" locator="name(//*[1])" locator-type="Label">
            <field-map-ref resource="MFT_MAPPINGS.EventName"/>
        </field>

        <field name="Direction" locator="name(//*[1])" locator-type="Label" transparent="true"/>

        <field name="ResourceName" formattingPattern="{0}={1};Agent={2}">
            <!--resolves FILE or QUEUE-->
            <field-locator locator="name(//*/*)" locator-type="Label">
                <field-map-ref resource="MFT_MAPPINGS.Resource"/>
            </field-locator>
            <!--resolves file or queue name -->
            <field-locator locator="ts:getFileName(/${Direction}/file)" locator-type="Label" required="false"/>
            <field-locator locator="ts:getObjectName(/${Direction}/queue)" locator-type="Label" required="false"/>
            <!-- agent-->
            <field-locator locator="/transaction/${Direction}Agent/@agent" locator-type="Label" required="false"/>
        </field>
        <field name="CompCode" locator="/transaction/status/@resultCode" locator-type="Label">
            <field-map-ref resource="MFT_MAPPINGS.CompCode"/>
        </field>
        <field name="CompCode2" locator="/transaction/status/@resultCode" locator-type="Label">
            <field-map-ref resource="MFT_MAPPINGS.CompCode2"/>
        </field>
        <.../>
    </parser>

    <stream name="ResMappingStream">
        <.../>
        <parser-ref name="ResMappingParser"/>
    </stream>
</tnt-data-source>
```

Sample shows how stream data source configuration imports (over `resource-ref` tag) external resource file 
`./tnt4j-streams-wmq/samples/mft_fte/mft_mappings.json` using relative file path.

`resource-ref` tag attributes:
* `id` - is resource identifier and mus be unique. Required.
* `type` - defines type of imported resource. Required. Supported values are:
    * `ValuesMap` - resource file defines source-target values mapping.
    * `Parser` - resource file defines external parser configuration to be injected into current `tnt-data-source` configuration.
* `uri` - defines absolute URL or relative file path of referenced resource. Required. See [Referenced resource formats](#referenced-resource-formats).
* `separator` - defines source and target values delimiter and is applicable to `CSV` and `Properties` formatted resources. Optional.

#### Referenced resource formats

Supported external resource definition formats:
* `ValuesMap` type resources can be:
    * `JSON`
    ```json
    {
        "EventType": {
            "destination": "SEND",
            "source": "RECEIVE",
            "": "NOOP"
        },

        "EventName": {
            "destination": "process_destination",
            "source": "process_source",
            "": "N/A"
        },

        "Resource": {
            "file": "FILE",
            "queue": "QUEUE"
        },

        "CompCode": {
            "0": "SUCCESS",
            "40": "WARNING",
            "": "ERROR"
        },

        "CompCode2": {
            "0": "SUCCESS",
            "40": "WARNING",
            "1": "FAILURE",
            "": "ERROR"
        }
    }
    ```
    * `CSV`
    ```csv
    0,SUCCESS
    40,WARNING
    , ERROR
    ```
    * `Properties`
    ```properties
    0:SUCCESS
    40:WARNING
    : ERROR
    ```

    **NOTE:** `JSON` can have multiple mapping definition sets, while `CSV` and `Properties` formats supports only one mapping set definition.
* `Parser` type resources can be only `XML` format defined content.

## ZooKeeper stored configuration

`TNT4J-Streams` has ability to load configuration not only from configuration files, but either from ZooKeeper nodes stored data. ZooKeeper 
allows handle configuration data in more centralized way.

`TNT4J-Streams` is able to load ZooKeeper stored configuration on application startup and also to monitor referenced ZooKeeper nodes for 
configuration data changes, making it possible to apply new configuration on runtime.

### Uploading configuration data to ZooKeeper

To upload files contained `TNT4J-Streams` configuration to ZooKeeper nodes there is utility called `ZKConfigInit`. It has these program 
arguments:
* `-f:` - defines file reference containing uploader utility configuration. (Required)
* `-c` - indicates to clean ZooKeeper nodes contained TNT4J-Streams configuration. (Optional)

To run it use `./bin/zk-init-cfg.bat` or `./bin/zk-init-cfg.sh` files. It uses uploader configuration file `./config/zk-init-cfg.properties`.

Uploader configuration properties file contents:
```properties
# ZooKeeper server parameters
zk.conn=localhost:2181
#zk.conn.timeout=5000
zk.streams.path=/tnt4j-streams

# generic app configurations
config.logger=../config/log4j.properties
config.tnt4j=../config/tnt4j.properties
config.tnt4j-kafka=./config/tnt4j-kafka.properties

# core module samples configurations
samples.core.ajax=../samples/ajax/tnt-data-source.xml
samples.core.angular-js-tracing=../samples/angular-js-tracing/tnt-data-source.xml
samples.core.single-log=../samples/single-log/tnt-data-source.xml
...
```

`zk.*` properties defines ZK connection configuration settings.

**NOTE:** `zk.streams.path` property defines root ZK node of streams configuration. All other configuration data nodes will be uploaded 
under this initial ZK path.

**NOTE:** Do not start you configuration files referencing properties with `zk.`. Such streams configuration reference definition properties
will be ignored and files referenced by these properties wont be used in upload process.

`config.*` and `samples.*` properties defines streams configuration files data mapping to ZK nodes.

Mapping works this way: property name defines ZK node path (under root configuration node defined using `zk.streams.path` property) and 
property value is reference to a file containing configuration data to be uploaded to ZK.

So in upload process property name like `samples.core.single-log` turns to ZK node path `/samples/core/single-log` (full upload ZK path 
would be `/tnt4j-streams/samples/core/single-log`). You can freely name properties according to your environment organization, but it must 
comply properties naming specification to handle it as property and also must comply ZK node path specification after "translation".

### Loading ZooKeeper stored configuration data

To make `TNT4J-Streams` load configuration from ZooKeeper use program argument `-z:` referencing `TNT4J-Streams` ZooKeeper configuration 
file, e.g., `-z:./tnt4j-streams-core/samples/zookeeper-cfg/stream-zk.properties`.

Program argument `-z:` can be used in common with `-f:` argument. Then streams will try to load ZooKeeper stored configuration first and if
fails - then loads configuration from file referenced by `-f:` argument.

Sample streams ZooKeeper configuration contents:
```properties
# ZooKeeper server parameters
zk.conn=localhost:2181/tnt4j-streams
#zk.conn.timeout=5000

# logger configuration: log4j properties, logback xml/groovy
config.logger.zk.path=/config/logger

# TNT4J configuration: properties
config.tnt4j.zk.path=/config/tnt4j
#config.tnt4j-kafka.zk.path=/config/tnt4j-kafka

# Stream configuration: XML containing <tnt-data-source/>
config.stream.zk.path=/samples/core/single-log
```

`zk.*` properties defines ZK connection configuration settings.

`config.logger.zk.path` property defines ZK node path containing logger configuration data. If absent system property 
`-Dlog4j.configuration` (or `-Dlogback.configurationFile` - depending on logging framework used) defined configuration file will be used.
**NOTE:** currently supports `log4j`, `JUL` and `Logback` logging frameworks configuration handling.

`config.tnt4j.zk.path` property defines ZK node path containing TNT4J configuration data. If absent system property 
`-Dtnt4j.config` defined configuration file will be used.

`config.tnt4j-kafka.zk.path` property is reserved to define ZK node path containing TNT4J-Kafka configuration data. It is not currently 
used. //TBD

`config.stream.zk.path` property defines ZK node path containing stream configuration data. If absent program argument `-f:` defined 
configuration file will be used.

### Streams registry

`TNT4J-Streams` also able to contain all ZooKeeper configuration in one single file. See [`streams-zk.properties`](config/streams-zk.properties).

File consists of ZK configuration registry entities every entity having two properties (except `zk.*` properties set used to configure ZK 
connection):
* `cfg.entity.id` + `.zk.path` - defines ZK node path containing entity configuration data. **NOTE:** actual path in ZK ensemble may not 
necessarily exist and will be created on demand.
* `cfg.entity.id` + `.cfg.file` - defines configuration file path to upload configuration data to ZK node in case ZK node does not exist or 
is empty.

Sample streams ZooKeeper configuration registry stile contents:
```properties
########################### ZooKeeper server parameters ###########################
# ZooKeeper connection string
zk.conn=localhost:2181/tnt4j-streams
# ZooKeeper connection timeout
#zk.conn.timeout=5000
# ZooKeeper path of TNT4J-Streams root node location
zk.streams.path=/tnt4j-streams

########################### Generic app configurations ###########################
# Logger configuration: log4j properties, logback xml/groovy
config.logger.zk.path=/config/logger
config.logger.cfg.file=../config/log4j.properties

# TNT4J configuration
config.tnt4j.zk.path=/config/tnt4j
config.tnt4j.cfg.file=../config/tnt4j.properties

# TNT4J-Kafka configuration (used by KafkaEventSink)
config.tnt4j-kafka.zk.path=/config/tnt4j-kafka
config.tnt4j-kafka.cfg.file=../config/tnt4j-kafka.properties

########################### Sample Streams configurations ###########################
### core module samples configurations
# sample stream: samples.core.ajax
samples.core.ajax.zk.path=/samples/core/ajax
samples.core.ajax.cfg.file=../samples/ajax/tnt-data-source.xml

# sample stream: samples.core.multiple-logs
samples.core.multiple-logs.zk.path=/samples/core/multiple-logs
samples.core.multiple-logs.cfg.file=../samples/multiple-logs/tnt-data-source.xml

# sample stream: samples.core.piping-stream
samples.core.piping-stream.zk.path=/samples/core/piping-stream
samples.core.piping-stream.cfg.file=../samples/piping-stream/parsers.xml

# sample stream: samples.core.single-log
samples.core.single-log.zk.path=/samples/core/single-log
samples.core.single-log.cfg.file=../samples/single-log/tnt-data-source.xml
...
```

`zk.*` properties defines ZK connection configuration settings.

`config.*` properties defines configuration entities for `logger`, `tnt4j` and `tnt4j-kafka`.

`samples.*` properties defines configuration entities for sample steams.

Also see [Loading ZooKeeper stored configuration data](#loading-zookeeper-stored-configuration-data).

When using registry style ZooKeeper configuration, missing configuration entities ZK nodes data is automatically uploaded on demand from 
entity configuration file property (ending `.cfg.file`) referenced file.

To run `TNT4J-Streams` using registry style ZooKeeper configuration define these program arguments: `-z:<cfg_file> -sid:<stream_id>`, e.g.,
```cmd
-z:.\config\streams-zk.properties -sid:samples.core.single-log
```
Streams will run sample stream `single-log`. See [Single Log file](#single-log-file) for sample details.

How to Build TNT4J-Streams
=========================================
## Modules

* (M) marked modules are mandatory 
* (O) marked modules are optional 
* (U) marked modules are utility modules (e.g., performs compiled assemblies packaging)

Modules list:
   * `Core` (M) - major module implementing data streaming (collection and transformation) features.
   * `Elastic-Beats` (O) - Elastic Beats provided data streaming module.
   * `Flume-Plugin` (O) - Apache Flume provided data streaming module.
   * `Fs` (O) - JSR-203 compliant [FileSystem](https://docs.oracle.com/javase/7/docs/api/java/nio/file/FileSystem.html) provided files data 
   streaming module. 
   * `Hdfs` (O) - HDFS (Apache Hadoop) provided data streaming module. 
   * `JMS` (O) - JMS (Java Message Service) provided data streaming module.
   * `Kafka` (O) - Apache Kafka provided data streaming module.
   * `Mqtt` (O) - MQTT provided data streaming module.
   * `MsOffice` (O) - MS Office Excel provided data streaming module.
   * `WMQ` (O) - IBM MQ provided data streaming module.
   * `WS` (O) - web-service (or OS command) provided data streaming module.
   * `Samples` (O) - integration into custom API sample module.
   * `Distribution` (OU) - distributable package build module.

All optional modules (extensions) depends to `core` module and can't be build and run without it.

**NOTE:** `Samples` module provides no additional features to TNT4J streaming framework. It contains only streams API use samples.
**NOTE:** `Distribution` module performs `maven post build` release assemblies delivery to `../build/tnt4j-streams` directory.

## Requirements
* JDK 1.8+
* [Apache Maven 3](https://maven.apache.org/)
* [TNT4J](https://github.com/Nastel/TNT4J)
* [JESL](https://github.com/Nastel/JESL)

All other required dependencies are defined in project modules `pom.xml` files. If maven is running online mode it should download these 
defined dependencies automatically.

### Manually installed dependencies
Some of required and optional dependencies may be not available in public [Maven Repository](http://repo.maven.apache.org/maven2/). In this 
case we would recommend to download those dependencies manually into module's `lib` directory and install into local maven repository by 
running maven script `lib/pom.xml` with `install` goal. For example see [`tnt4j-streams/tnt4j-streams-elastic-beats/lib/pom.xml`](tnt4j-streams-elastic-beats/lib/pom.xml) 
how to do this.

#### `Elastic-Beats` module

**NOTE:** Because this module requires manually downloaded libraries, it is commented out in main project pom file `tnt4j-streams/pom.xml` 
by default. If you want to use it uncomment line `<module>tnt4j-streams-elastic-beats</module>` of `pom.xml` file. But `Elastic-Beats` 
module will be ready to build only when manually downloaded libraries will be installed to local maven repository.

What to download manually or copy from your existing IBM MQ installation:
* Logstash Beats Input Plugin 5.0.13

Download the above libraries and place into the `tnt4j-streams/tnt4j-streams-elastic-beats/lib` directory like this:
```
    lib
     |- logstash-input-beats-5.0.13.jar
```
(O) marked libraries are optional

## Building
   * to build project and make release assemblies run maven goals `clean package`
   * to build project, make release assemblies and install to local repo run maven goals `clean install`

By default maven will build all modules defined in `tnt4j-streams/pom.xml` file.

If you do not want to build some of optional modules, comment those out like `WMQ` module is. Or you can define maven to build your 
preferred set of modules using `-pl, --projects` argument (comma separated modules list) together with `-am, --also-make` argument, e.g.:

```cmd
mvn -pl tnt4j-streams-core,tnt4j-streams-samples,tnt4j-streams--distribution -am clean install
``` 
or
```cmd
mvn --projects tnt4j-streams-core,tnt4j-streams-samples,tnt4j-streams--distribution --also-make clean install
```

**NOTE:** modules list should be without spaces after comma!

Issuing these commands, maven will build only `tnt4j-streams-core`, `tnt4j-streams-samples` and `tnt4j-streams--distribution` modules.

Release assemblies are built to `../build/tnt4j-streams` directory.

**NOTE:** sometimes maven fails to correctly handle dependencies. If dependency configuration looks fine, but maven still complains about 
missing dependencies try to delete local maven repository by hand: e.g., on MS Windows delete contents of `c:\Users\[username]\.m2\repository` 
directory.

**NOTE:** in case you are using Java 9 as your compiler and getting `java.lang.NoClassDefFoundError: javax/xml/bind/JAXBException`, add 
`javac` command parameter `--add-modules java.xml.bind` to add JAXB classes to java classpath.

## Running samples

See 'Running TNT4J-Streams' chapter section ['Samples'](#samples).

Testing of TNT4J-Streams
=========================================

## Requirements
* [JUnit 4](http://junit.org/)
* [Mockito](http://mockito.org/)

## Testing using maven
Maven tests run is disabled by default. To enable Maven to run tests set Maven command line argument 
`-DskipTests=false`.

## Running manually from IDE
* in `core` module run JUnit test suite named `AllStreamsCoreTests`
* in `elastic-beats` module run JUnit test suite named `AllElasticBeatsTests`
* in `flume-plugin` module run JUnit test suite named `AllFlumeTests`
* in `fs` module run JUnit test suite named `AllFsStreamTests`
* in `hdfs` module run JUnit test suite named `AllHdfsStreamTests`
* in `jms` module run JUnit test suite named `AllJMSStreamTests`
* in `kafka` module run JUnit test suite named `AllKafkaStreamTests`
* in `mqtt` module run JUnit test suite named `AllMqttStreamTests`
* in `wmq` module run JUnit test suite named `AllWmqStreamTests`
* in `ws` module run JUnit test suite named `AllWsStreamTests`
* in `msoffice` module run JUnit test suite named `AllMsOfficeStreamTests`

Known Projects Using TNT4J-STREAMS
===============================================
* [TNT4J-Streams-Zorka](https://github.com/Nastel/tnt4j-streams-zorka)
* [TNT4J-Streams-Syslogd](https://github.com/Nastel/tnt4j-streams-syslogd)
* [TNT4J-Streams-IBM-B2Bi](https://github.com/Nastel/tnt4j-streams-ibm-b2bi)
