# TNT4J-Streams API use samples

## Custom

This sample shows how to integrate TNT4J-Streams into your custom API.

Sample files can be found in `com.jkoolcloud.tnt4j.streams.sample` package.

`SampleIntegration.java` shows how to make TNT4J-Streams integration into your API. Also integration could be made using
`StreamsAgent.runFromAPI(new CfgStreamsBuilder().setConfig(cfgFileName))` call.

`SampleParser.java` shows how to implement custom parser.

`SampleStream.java` shows how to implement custom stream.

## Builder

This sample shows how to make streaming application using plain Java API without using streams configuration `xml` file. Application does 
same as `tnt4j-streams-core` sample `single-log`. See ['Single Log file'](../README.md#single-log-file).

Sample files can be found in `com.jkoolcloud.tnt4j.streams.sample.builder` package.

`SampleStreamingApp.java` shows how to configure and run stream over plain API.

Application parameters:
 * path to `orders.log` file. If absent, then `orders.log` file must be in working directory.

Running application:
 * build module and use `run.bat`/`run.sh` from `samples/stream-builder` directory.

or

 * set this IDE `run configuration`:
    * Main class: `com.jkoolcloud.tnt4j.streams.sample.builder.SampleStreamingApp`
    * VM Options: `-Dtnt4j.config="./config/tnt4j.properties" -Dlog4j.configuration="file:./config/log4j.properties"`
    * Program arguments: `./tnt4j-streams-samples/samples/stream-builder/orders.log`
    * Working directory: `[YOUR_TNT4J_STREAMS_ROOT_DIR]`, e.g., `C:\PROJECTS\Nastel\tnt4j-streams`
    * Class path: `tnt4j-streams-samples` module