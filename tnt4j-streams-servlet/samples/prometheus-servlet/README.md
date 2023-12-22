# Prometheus Remote Write HTTP endpoint

## Quick setup steps

1. Configure prometheus remote write endpoint in your `prometheus.yml`, e.g.:
   ```yaml
   remote_write:
   #  - url: "https://app.cybench.io/tnt4j-streams-prom"
   #    name: "cybench_write"
      - url: "http://localhost:8080/tnt4j-streams-servlet"
        name: "local_write"
   ```
1. Copy configuration files to your web server configuration dir, e.g. `<TOMCAT_DIR>/conf`. By default, TNT4J-Streams will try to use
   internal configuration dir path `/Catalina/localhost/tnt4j-streams`:
   1. [log4j2.xml](../../config/log4j2.xml) - log4j V2 configuration used by TNT4J-Streams
   1. [tnt4j.properties](../../config/tnt4j.properties) - base TNT4J configuration
   1. [tnt4j-common.properties](../../config/tnt4j-common.properties) - common TNT4J configuration
   1. [tnt4j-streams.properties](../../config/tnt4j-streams.properties) - TNT4J-Streams dedicated TNT4J configuration
   1. [tnt-data-source.xml](tnt-data-source.xml) - TNT4J-Streams data source (stream) configuration, having stream dedicated TNT4J
      properties configuration section:
      1. note that default configuration broadcasts CloudWatch metrics jKool/XRay (sink id `xray`).
         ```xml
         <tnt4j-properties>
            <...>
            <property name="event.sink.factory.BroadcastSequence" value="xray"/>
            <...>
         </tnt4j-properties>
         ```
      1. change `https://stream.meshiq.com` to your jKool/XRay streaming endpoint URL.
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

Prometheus Remote Write streaming HTTP endpoint (servlet) configuration is combination of:
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
* `PrometheusMetricsStream` of class `com.jkoolcloud.tnt4j.streams.inputs.HttpServletStream` picking HTTP POST transmitted request payload
  as stream input data
* `tnt4j-properties` section defining individual stream TNT4J configuration:
   * property `event.sink.factory.BroadcastSequence` defines produced activities broadcasting sinks. Default configuration contains 
     configuration for jKool/XRay (sink id `xray`) sink. Default set of sinks to broadcast stream produced activities is `xray`.
     ```xml
     <tnt4j-properties>
     <...>
     <property name="event.sink.factory.BroadcastSequence" value="xray"/>
     <...>
     </tnt4j-properties>
     ```
   * properties group starting `event.sink.factory.EventSinkFactory.xray` defines jKool/XRay dedicated sink configuration
      * change jKool/XRay sink (id `xray`) URL value (property `event.sink.factory.EventSinkFactory.xray.Url`) to match your jKool/XRay
        instance. Default is `https://stream.meshiq.com`.
      * change jKool/XRay sink (id `xray`) token placeholder value (property `event.sink.factory.EventSinkFactory.xray.Token`) to your
        jKool/XRay streaming token if you are willing to stream into that repo. Placeholder value is `xray-access-token`.
* `PrometheusParser` parser reference to bootstrap incoming metrics data package

### Parsers (`parsers.xml`)

Unwraps Prometheus sent metrics data package into time-series maps collection. Then allows metrics filtering (see
[Metrics filtering](#metrics-filtering) section) and performs time-series map fields values parsing into TNT4J activities and snapshots.

This file defines these parsers:
* `PrometheusParser` - bootstrap parser recognizing received metrics data package format: binary, text, etc.
* `MetricsParserStr` - parses Prometheus remote write request data if it was sent as JSON picking collection of time-series entries.
* `TimeSeriesParser` - aggregates all time-series entries provided by `MetricsParserStr` under single `ACTIVITY`. 
* `SnappyCompressedMetricsParser` - converts Prometheus sent Snappy compressed binary data to a collection of time-series maps and produces 
  `ACTIVITY` type entity containing snapshots for every time-series entry produced by `TimeSeriesEntryParser`.
* `TimeSeriesEntryParser` - builds time-series entry map parsed `SNAPSHOT` entity.
* `MetricsMetaDataParser` - time-series map fields group `metadata` parser.
* `ExemplarsParser` - time-series map fields group `exemplars` parser.
* `SamplesParser` - time-series map fields group `samples` parser.
* `LabelsParser` - time-series map fields group `labels` parser.
 
And these pre-parsers:
* `SnappyBinPreParser` - performs Snappy compressed binary data decompression.
* `PrometheusReqMapPreParser` - performs binary data parsing into Prometheus `Remote.WriteRequest` message and parsing it into time-series 
  entries map.

## Metrics filtering

You may want to pick just some set of provided time-series to be streamed. To filter time-series by label, filtering logic can be done in 
`SnappyCompressedMetricsParser` parser field (embedded activity) `MetricsDataProto` [transformation](parsers.xml#L150). Default 
transformation does not perform any filtering:
```groovy
Collection<Map<String, ?>> tsColl = new ArrayList<>();

for (Map.Entry<String, Collection<Map<String, ?>>> tsCollE : $fieldValue.entrySet()) {
    tsColl.addAll(tsCollE.getValue());
}

return tsColl;
```
But if you need to pick just some set of time-series by label e.g. to pick only `kafka_consumer` time-series entries:

```groovy
import org.apache.commons.lang3.StringUtils

Collection<Map<String, ?>> tsColl = new ArrayList<> ();

for (Map.Entry<String, Collection<Map<String, ?>>> tsCollE : $fieldValue.entrySet ()) {
    if (StringUtils.startsWithAny (tsCollE.getKey (), "kafka_consumer_")) {  //NON-NLS
        tsColl.addAll (tsCollE.getValue ());
    }
}

return tsColl;
```

## Steamed metrics data

* For Prometheus Remote-Write provided metrics package TNT4J-Streams produces `ACTIVITY` entity containing such fields:
    * `Name` has value `PrometheusTimeSeries`
    * `StartTime`/`EndTime` having current stream runtime timestamp
    
* Time-series entries are placed as child `SNAPSHOT`s of that `ACTIVITY` entity. Snapshot contains such set of fields:
    * Time-series entry field group `labels` produces such fields:
        * `Instance` value from group field `instance`
        * `Job` value from group field `job`
        * `Type` value from group field `type`
        * `Name` value from group field `name`
    * Time-series entry field group `samples` produces such fields:
        * `StartTime`/`EndTime` value from group field `timestamp`
        * `Value` value from group field `value`
    * Time-series entry field group `metadata` produces such fields:
        * `MetricType` value from group field `type`
        * `Help` value from group field `help`
        * `MetricFamilyName` value from group field `family`
        * `Unit` value from group field `unit`
    * `Category` value mapped from time-series entry name to JMX domain. Mapping is done in `TimeSeriesEntryParser` parser field `domain`
      [transformation](parsers.xml#L97)
    * `Name` value from `labels` group field `__name__`
    * Rest of metrics line fields are mapped into snapshot properties using original name `1:1`. Usually it may be new or very rare fields.
