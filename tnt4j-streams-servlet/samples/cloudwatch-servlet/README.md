# AWS CloudWatch metrics HTTP endpoint

## Quick setup steps

1. See [CloudwatchMetrics document](docs/CloudwatchMetrics.md) how to configure AWS to feed metrics data.
1. You must have HTTPS enabled web server. AWS allows sending metrics only to **HTTPS enabled and having valid certificate** (not expired, 
   not self-signed) web endpoint.
1. Copy configuration files to your web server configuration dir, e.g. `<TOMCAT_DIR>/conf`. By default, TNT4J-Streams will try to use 
   internal configuration dir path `/Catalina/localhost/tnt4j-streams`:
   1. [log4j2.xml](../../config/log4j2.xml) - log4j V2 configuration used by TNT4J-Streams
   1. [tnt4j.properties](../../config/tnt4j.properties) - base TNT4J configuration
   1. [tnt4j-common.properties](../../config/tnt4j-common.properties) - common TNT4J configuration
   1. [tnt4j-streams.properties](../../config/tnt4j-streams.properties) - TNT4J-Streams dedicated TNT4J configuration
   1. [tnt-data-source.xml](tnt-data-source.xml) - TNT4J-Streams data source (stream) configuration, having stream dedicated TNT4J 
      properties configuration section:
      1. note that default configuration broadcasts CloudWatch metrics to AutoPilot (sink id `ap`) and jKool/XRay (sink id `xray`) 
         simultaneously. If you are willing to use just one of these sinks, then change `<tnt4j-properties>` configuration line:
         ```xml
         <tnt4j-properties>
            <...>
            <!-- FROM -->
            <property name="event.sink.factory.BroadcastSequence" value="ap,xray"/>
            <!-- TO for streaming just to AutoPilot -->
            <property name="event.sink.factory.BroadcastSequence" value="ap"/>
            <!-- TO for streaming just to jKool/XRay -->
            <property name="event.sink.factory.BroadcastSequence" value="xray"/>
            <...>
         </tnt4j-properties>
         ```
      1. change AutoPilot sink host value (property `event.sink.factory.EventSinkFactory.ap.Host`) to match your AutoPilot instance. Default
         is `localhost`.
      1. change AutoPilot sink port value (property `event.sink.factory.EventSinkFactory.ap.Port`) to match your AutoPilot instance. Default
         is `6060`.
      1. change `https://data.jkoolcloud.com` to your jKool/XRay streaming endpoint URL. 
      1. change `xray-access-token` placeholder to your jKool/XRay streaming token if you are willing to stream into that repo
   1. [parsers.xml](parsers.xml) - TNT4J-Streams parsers configuration

   **NOTE:** for most general case there is no need for you to change `log4j2.xml`, `tnt4j*.propeties` and `parsers.xml` files. The only
   file requiring to make your changes is `tnt-data-source.xml`.
1. Download TNT4J-Streams `war` package latest version release from [GitHub releases](https://github.com/Nastel/tnt4j-streams/releases). It 
   shall be available in the `assets` section of release.
1. Deploy `tnt4j-streams-<VERSION>.war` or `tnt4j-streams-servlet-<VERSION>.war` file to your web sever web-apps dir, e.g. 
   `<TOMCAT_DIR>/webapps`. **NOTE:** remove version token from `war` package file name to preserve web-app context on every deployment.
1. Start web sever, if it is not already running.

# TL;DR

## Build TNT4J-Streams servlet

There are two ways to build TNT4J-Streams `war` package:
1. When building whole `tnt4j-streams` project, maven will produce `tnt4j-streams-<VERSION>.war` package where all reactor enabled modules 
   and dependencies are built in into that `war` package. 
1. When individually building `tnt4j-streams-servlet` module, maven will produce `tnt4j-streams-servlet-<VERSION>.war` package where all 
   module dependencies are build in into that `war` package.

Building either way `war` package contains classes and resources (like `web.xml`) provided by `tnt4j-streams-servlet` module.

## Configuration

AWS CloudWatch metrics streaming HTTP endpoint (servlet) configuration is combination of:
* web-app deployment descriptor `web.xml` within `war` package (by default) or within your web server configuration dir, e.g.
  `<TOMCAT_DIR>/conf/Catalina/localhost/tnt4j-streams`
* `log4j2.xml` defining log4j V2 configuration used by TNT4J-Streams API
* `TNT4J` configuration properties files split into dedicated scopes (base, common, streams)
* `tnt-data-source.xml` defining stream configuration
* `parsers.xml` defining parsers used by TNT4-Streams to map metrics fields to TNT4J activities data model

**NOTE:** for most general case there is no need for you to change `web.xml`, `log4j2.xml`, `tnt4j*.propeties` and `parsers.xml` files.

### Web-app deployment descriptor (`web.xml` within war package)

Defines servlet initialization parameters set and servlet mapping:
* init params
  * `streams.configs.dir` - TNT4J-Streams configuration files location path. It shall contain these files: `tnt4j.properties`, `log4j2.xml` 
    and `tnt-data-source.xml`. Optional if setting individual configuration files with init params `tnt4j.config`, `log4j2.config` and
    `streams.config`. Default value - `${catalina.base}/conf/Catalina/localhost/tnt4j-streams`
  * `tnt4j.config` - TNT4J configuration file path. Default value - `tnt4j.properties`
  * `log4j2.config` - log4j V2 configuration file path. Default value - `log4j2.xml`
  * `streams.config` - TNT4J-Streams datasource/parsers configuration file path. Default value - `tnt-data-source.xml`
* servlet mapping
  * default URL pattern is `/`

### log4j2

In general, configuration is same as for common TNT4-Streams logging except this config uses `${sys:catalina.base}/logs` dir to locate 
produced log files.

Also note loggers are named to match stream configuration defined broadcasting sink ids: `ap` and `xray` instead of `prod` and `qa`.

### TNT4J

In general, configuration is same as common TNT4-Streams TNT4J configuration except streams scope (`tnt4j-streams.properties` file) defines 
broadcasting sink ids `ap` and `xray` to match sink target endpoint.

Individual TNT4J streams scope configuration is made in `tnt-data-source.xml` file section `<tnt4j-properties>`.

### Stream (`tnt-data-source.xml`)

Major entities in stream configuration are
* `CloudWatchMetricsStream` of class `com.jkoolcloud.tnt4j.streams.inputs.HttpServletStream` picks up the HTTP POST transmitted request 
  payload as stream input data
* `ResponseTemplate` property defines stream servlet response template. AWS Kinesis FireHose requires particular JSON response to ensure 
  successful communication
* `tnt4j-properties` section defines individual stream TNT4J configuration:
  * property `event.sink.factory.BroadcastSequence` defines produced activities broadcasting sinks. Default configuration contains 
    configuration for AutoPilot (sink id `ap`) and for jKool/XRay (sink id `xray`) sinks. Default set of sinks to broadcast stream produced
    activities is `ap,xray`. If you are willing to use just one of these sinks, then change configuration line as this:
    ```xml
    <tnt4j-properties>
    <...>
    <!-- FROM -->
    <property name="event.sink.factory.BroadcastSequence" value="ap,xray"/>
    <!-- TO for streaming just to AutoPilot -->
    <property name="event.sink.factory.BroadcastSequence" value="ap"/>
    <!-- TO for streaming just to jKool/XRay -->
    <property name="event.sink.factory.BroadcastSequence" value="xray"/>
    <...>
    </tnt4j-properties>
    ```
  * properties group starting `event.sink.factory.EventSinkFactory.ap` defines AutoPilot dedicated sink configuration
    * change AutoPilot sink (id `ap`) host value (property `event.sink.factory.EventSinkFactory.ap.Host`) to match your AutoPilot
      instance. Default is `localhost`.
    * change AutoPilot sink (id `ap`) port value (property `event.sink.factory.EventSinkFactory.ap.Port`) to match your AutoPilot
      instance. Default is `6060`.
  * properties group starting `event.sink.factory.EventSinkFactory.xray` defines jKool/XRay dedicated sink configuration
    * change jKool/XRay sink (id `xray`) URL value (property `event.sink.factory.EventSinkFactory.xray.Url`) to match your jKool/XRay
      instance. Default is `https://data.jkoolcloud.com`.
    * change jKool/XRay sink (id `xray`) token placeholder value (property `event.sink.factory.EventSinkFactory.xray.Token`) to your 
      jKool/XRay streaming token if you are willing to stream into that repo. Placeholder value is `xray-access-token`.
* `KinesisFirehoseParser` reference to bootstrap parser of incoming metrics data package

### Parsers (`parsers.xml`)

Unwraps WAS Kinesis FireHose sent metrics data package into metrics JSON lines. Then allows metrics filtering (see 
[Metrics filtering](#metrics-filtering) section) and performs metric lines fields values parsing into TNT4J activities and snapshots.

This file defines these parsers:
* `KinesisFirehoseParser` - bootstrap parser recognizing received metrics data package format.
* `KinesisFirehoseParserStr` - parses Kinesis FireHose JSON metrics batch package where metrics data package is Base64 encoded string.
* `MetricsParserStr` - performs metric lines filtering before passing them for further parsing.
* `MetricEntryParserAP` - builds metrics data wrapping `ACTIVITY` entity for AutoPilot sink, containing `SNAPSHOT`s representing individual 
  metrics line.
* `MetricLineParserAP` - builds metrics line parsed `SNAPSHOT` entity for AutoPilot sink.
* `MetricEntryParserXRay` - builds metrics data wrapping `ACTIVITY` entity for jKool/XRay sink, containing `SNAPSHOT`s representing
  individual metrics line.
* `MetricLineParserXRay` - builds metrics line parsed `SNAPSHOT` entity for jKool/XRay sink.
* `ValueParser` - metrics line `value` fields group parser.
* `DimensionsParserAP` - metrics line `dimensions` fields group parser for AutoPilot sink.
* `DimensionsParserXRay` - metrics line `dimensions` fields group parser for jKool/XRay sink.

## Metrics filtering

You may want to pick just some set of provided metrics to be streamed. To filter metrics, filtering logic can be done in `MetricsParserStr` 
parser field (embedded activity) `MetricsData` [transformation](parsers.xml#L198). Default filter removes all empty lines available:
```groovy
boolean pass = StringUtils.isNotEmpty(line);
```
Additionally, you can define some RegEx to match metrics line you want to stream, e.g. to pick only `kafka_server` metrics:
```groovy
pass &= Matchers.evaluate("regex:kafka_server_.+", line);
```
Or pick some metrics by name for particular AWS service, e.g. if service is `AWS/Kafka`, then pick set of metric names like this:
```groovy
String[] kafkaIncludeMetrics = new String[] {
        "ActiveControllerCount",
        "CpuIdle",
        "CpuSystem",
        "CpuUser",
        "GlobalPartitionCount",
        "GlobalTopicCount",
        "KafkaAppLogsDiskUsed",
        "KafkaDataLogsDiskUsed",
        "MemoryBuffered",
        "MemoryCached",
        "MemoryFree",
        "MemoryUsed",
        "NetworkRxDropped",
        "NetworkRxErrors",
        "NetworkRxPackets",
        "NetworkTxDropped",
        "NetworkTxErrors",
        "NetworkTxPackets",
        "OfflinePartitionsCount",
        "RootDiskUsed",
        "SwapFree",
        "SwapUsed",
        "ZooKeeperRequestLatencyMsMean",
        "ZooKeeperRequestLatencyMsMean"
};

if (StringUtils.contains(line, "AWS/Kafka")) {
   pass &= StringUtils.containsAny(line, kafkaIncludeMetrics);
}
```

## Steamed metrics data

### AutoPilot facts

* For CloudWatch provided metrics package TNT4J-Streams produces `ACTIVITY` entity containing such fields:
  * `Name` has value `AWSCloudWatchMetrics`
  * `RoutePath` has value `ap`
  * `StartTime`/`EndTime` having current stream runtime timestamp
  * `DataCenter` has value `Amazon_AWS`

* Metrics lines are placed as child `SNAPSHOT`s of that `ACTIVITY` entity. Snapshot contains such set of fields:
  * `StartTime`/`EndTime` value from metrics line field `timestamp`
  * `Unit` value from metrics line field `unit`
  * `EventName` provides stream name and set of properties key/value pairs used to layout AP tree nodes (see property 
    `event.sink.factory.EventSinkFactory.ap.Formatter.PathLevelAttributes`):
    * `MetricStreamName` value from metrics line field `metric_stream_name` as metrics `domain`
    * `namespace=${Namespace}` value from metrics line field `namespace`
    * `region=${Region}` value from metrics line field `region`
    * `cluster=${ClusterName}` value from `dimensions` group field `Cluster Name`
    * `broker=${BrokerId}` value from `dimensions` group field `Broker ID`. If value is numeric - then number is prefixed by `b-`  
    * `topic=${Topic}` value from `dimensions` group field `Topic`
    * `streamName=${DeliveryStreamName}` value from `dimensions` group field `DeliveryStreamName`
    * `cGroup=${ConsumerGroup}` value from `dimensions` group field `Consumer Group`
    * `cAuth=${ClientAuth}` value from `dimensions` group field `Client Authentication`
    * `metric=${MetricName}` value from metrics line field `metric_name`
  * Metrics line field group `value` produces such fields:
    * `Max` value from group field `max`
    * `Min` value from group field `min`
    * `Sum` value from group field `sum`
    * `Count` value from group field `count`
    * `P99` value from group field `p99`
    * `P99_9` value from group field `p99.9`
    * `Median` value from group field `TM(25%:75%)`

### jKool/XRay activities

* For CloudWatch provided metrics package TNT4J-Streams produces `ACTIVITY` entity containing such fields:
  * `Name` has value `AWSCloudWatchMetrics`
  * `RoutePath` has value `xray`
  * `StartTime`/`EndTime` having current stream runtime timestamp
  * `DataCenter` has value `Amazon_AWS`

* Metrics lines are placed as child `SNAPSHOT`s of that `ACTIVITY` entity. Snapshot contains such set of fields:
  * `MetricStreamName` value from metrics line field `metric_stream_name`
  * `UserName` value from metrics line field `account_id`
  * `Region` value from metrics line field `region`
  * `Namespace` value from metrics line field `namespace`
  * `MetricName` value from metrics line field `metric_name`
  * `StartTime`/`EndTime` value from metrics line field `timestamp`
  * `Unit` value from metrics line field `unit`
  * `Category` has value `AWSCloudWatchMetric` 
  * Metrics line field group `dimensions` produces such fields:
    * `BrokerId` value from group field `Broker ID`. If value is numeric - then number is prefixed by `b-`
    * `InstanceId` value from group field `InstanceId`
    * `ClusterName` value from group field `Cluster Name`
    * `Topic` value from group field `Topic`
    * `ConsumerGroup` value from group field `Consumer Group`
    * `ClientAuth` value from group field `Client Authentication`
    * `DeliveryStreamName` value from group field `DeliveryStreamName` 
  * Metrics line field group `value` produces such fields:
    * `Max` value from group field `max`
    * `Min` value from group field `min`
    * `Sum` value from group field `sum`
    * `Count` value from group field `count`
    * `P99` value from group field `p99`
    * `P99_9` value from group field `p99.9`
    * `Median` value from group field `TM(25%:75%)`
  * `Name` combines fields `Namespace`, `Region`, `ClusterName`, `BrokerId` and `MetricName` delimited by `|`
  * `Guid` combines fields `StartTime` formatted as `yyyy-MM-dd_HH:mm:ss` in `UTC` timezone with `Name` field values delimited by `|`
  * Rest of metrics line fields are mapped into snapshot properties using original name `1:1`. Usually it may be new or very rare fields.
