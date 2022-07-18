# ActiveMQ Artemis Interceptor

This sample shows how to stream ActiveMQ Artemis intercepted packets and connection data to jKool/XRay.

## Quick setup steps

### Broker

Consider your broker has default directories layout, where:
* configuration is stored in `etc` dir
* run scripts are in `bin` dir
* custom broker libraries goes to `lib` dir

1. Copy `tnt4j-streams-jms-<VERSION>-artemis-broker-interceptor.jar` to your broker lib dir `<broker>/lib`.
1. Create `<broker>/etc/tnt4j` dir to store TNT4J-Streams dedicated configuration:
   * Whole config dir:
      1. Copy [tnt4j](mybroker0/etc/tnt4j) dir content into that dir.
   * Files one-by-one:
      1. Copy [interceptors.properties](mybroker0/etc/tnt4j/interceptors.properties) file into that dir.
      1. Copy [logging.properties](mybroker0/etc/tnt4j/logging.properties) file into that dir.
      1. Copy [interceptor_parsers.xml](mybroker0/etc/tnt4j/interceptor_parsers.xml) file into that dir.
      1. Copy [msg_parsers.xml](mybroker0/etc/tnt4j/msg_parsers.xml) file into that dir.
      1. Copy [tntj4*.properties](mybroker0/etc/tnt4j/tnt4j.properties) files into that dir.
      1. Copy [tnt-data-source.xml](mybroker0/etc/tnt4j/tnt-data-source.xml) file into that dir.
1. Alter `<broker>/etc/tnt4j/tnt4j-streams.properties` file by setting your jKool token in line 
  `event.sink.factory.EventSinkFactory.prod.Token`: replace value `prod-access-token` with your token. 
1. Alter `<broker>/etc/broker.xml` by changing sections `remoting-incoming-interceptors` and `remoting-outgoing-interceptors`:
   ```xml 
   <remoting-incoming-interceptors>
       <class-name>com.jkoolcloud.tnt4j.streams.custom.interceptors.jms.artemis.PacketInterceptor</class-name>
   </remoting-incoming-interceptors>

   <remoting-outgoing-interceptors>
       <class-name>com.jkoolcloud.tnt4j.streams.custom.interceptors.jms.artemis.PacketInterceptor</class-name>
   </remoting-outgoing-interceptors>
   ```
1. Alter `<broker>/bin/artemis.cmd` (or `.sh`) by setting:
   1. change logger configuration path by changing `ARTEMIS_LOGGING_CONF` variable definition to:
      * MS Windows
        ```cmd
        set ARTEMIS_LOGGING_CONF=%ARTEMIS_INSTANCE_ETC_URI%\tnt4j\logging.properties
        ```
      * *nix
        ```bash
        ARTEMIS_LOGGING_CONF="$ARTEMIS_INSTANCE_ETC_URI/tnt4j/logging.properties"
        ```
   1. set TNT4J configuration system property by appending `JVM_ARGS` variable definition:
      * MS Windows
        ```cmd
        set JVM_ARGS=%JVM_ARGS% -Dtnt4j.config=%ARTEMIS_INSTANCE_ETC%\tnt4j\tnt4j.properties
        ```
      * *nix
        ```bash
        JVM_ARGS="$JVM_ARGS -Dtnt4j.config=$ARTEMIS_INSTANCE_ETC/tnt4j/tnt4j.properties"
        ```

# TL;DR

## Interceptor types

Now TNT4J-Streams provides individual interceptor for every ActiveMQ Artemis supported protocol:
* `Core` - `com.jkoolcloud.tnt4j.streams.custom.interceptors.jms.artemis.PacketInterceptor`
* `Amqp` - `com.jkoolcloud.tnt4j.streams.custom.interceptors.jms.artemis.AmqpInterceptor`
* `Mqtt` - `com.jkoolcloud.tnt4j.streams.custom.interceptors.jms.artemis.MQTTInterceptor`
* `OpenWire` - `com.jkoolcloud.tnt4j.streams.custom.interceptors.jms.artemis.OpenWireInterceptor`
* `Stomp` - `com.jkoolcloud.tnt4j.streams.custom.interceptors.jms.artemis.StompInterceptor`

## Configuration

TNT4J-Streams interceptors are configured over [interceptors.properties](interceptors.properties) configuration file. **Note**: this is just 
interceptors instances configuration - complete solution also requires TNT4J, loggers, datasource and parsers configuration.

Now interceptors configuration allows to configure:
* produced intercepted data map layout. Default is `FLAT`.
* interceptions include packet class name regex pattern. See Artemis API documentation for available packet implementation classes per 
  communication protocol. Default is `Session(Send|Receive)Message` to intercept packets transporting produced or consumed message. Change 
  to `.*` to intercept any available packet.
* interceptions exclude packet class name regex pattern. See Artemis API documentation for available packet implementation classes per
  communication protocol. Default is none.

Interceptors can be configured by scope (protocol). First configuration property name token defines scope: `amqp`, `mqtt`, `owire`, `core`,
`stomp`. Prefix `any` defines configuration for any (all) scope of interceptor.

## Intercepted data

Every TNT4J-Streams ActiveMQ Artemis interceptor produces map with attributes for these scopes:
* `packet` - packet implementation provided attributes
* `conn` - connection implementation provided attributes
* `ictx` - interceptor context provided attributes

Interceptors can produce such map layouts:
* `FLAT` - produces single depth level map (kind of properties list) where entry key is made (using `:` delimiter) by appending complex type
  property names path. This is **default** layout.
* `DEEP` - produces map constructed by making dedicated entry value map for complex type properties.

### Parsing message payload

#### `Core` protocol

If there is a need to parse intercepted message payload, you can bind your parser to `Message` field like this:
1. Define your message payload parser in `msg_parsers.xml` file like this:
   ```xml
   <!-- Sample Message XML payload parser -->
   <parser name="XML_Data_Parser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityXmlParser">
       <field name="EventType" value="NOOP"/>

       <field name="HostName" locator="/tracking_event/HostName" locator-type="Label"/>
   </parser>
   ```
1. Bind parser reference to `Message` field in `interceptor_parsers.xml` file like this:
   ```xml
   <field name="Message" locator="packet:message:stringBody" locator-type="Label">
       <!-- To parse message payload, put your parser reference here -->
       <parser-ref name="XML_Data_Parser" aggregation="Merge"/>
   </field>
   ```

### Message properties

#### `Core` protocol

Intercepted message properties are parsed by `PropertiesParser` parser:
```xml
<!-- Message properties parser -->
<parser name="PropertiesParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
    <field name="EventType" value="NOOP"/>

    <field name="AllProps" locator="#" locator-type="Label"/>
</parser>
```
By default, it performs simple direct properties mapping into produced TNT4J event.

But if there is known set of properties used in messages, here you can define additional mapping logic by defining meaningful property 
names, transforming values or aggregating multiple properties values.

## Binding interceptors

Same interceptors can be used for both ActiveMQ Artemis client (producer/consumer) and server (broker) side API interceptions. Naturally 
interceptor classes shall be in your client/server classpath.

### Client side API

Put `tnt4j-streams-jms-<VERSION>-artemis-client-interceptor.jar` into your client classpath.

To bind TNT4J-Streams ActiveMQ Artemis interceptors for client side API:
```java
System.setProperty(TrackerConfigStore.TNT4J_PROPERTIES_KEY,
        "./samples/artemis-interceptors/mybroker0/etc/tnt4j/tnt4j.properties");

ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory( //
        "tcp://localhost:61616?" //
                + "incomingInterceptorList=" + PacketInterceptor.class.getName() + "&" //
                + "outgoingInterceptorList=" + PacketInterceptor.class.getName() //
);
try (Connection connection = cf.createConnection()) {
  //DO YOUR ARTEMIS MESSAGES PRODUCTION/CONSUMPTION
} finally {
    StreamsAgent.waitForStreamsToComplete();
}
```
See [ClientInterceptorTest.java](../../src/test/java/com/jkoolcloud/tnt4j/streams/custom/jms/interceptors/artemis/ClientInterceptorTest.java)
for more details.

* Define system property `tnt4j.config` (through command line `-D` or over API call `System.setProperty`) to define where to look for TNT4J 
  configuration. All the rest of configuration (interceptors, logger, stream/parsers) shall be placed on the same path as TNT4J 
  configuration to be picked automatically. Logger configuration though can be defined individually by setting logger configuration path 
  system properties.
* Bind ActiveMQ Artemis interceptor classes over connection factory URL. `incomingInterceptorList` defines interceptor classes for consumer 
  and `outgoingInterceptorList` for producer APIs. These lists can have multiple classes defined using delimiter `,`.
* `finaly` block statement `StreamsAgent.waitForStreamsToComplete()` ensures JVM to stay alive until all intercepted packets are processed 
  and sent to jKool/XRay.

### Server side (broker)

Put `tnt4j-streams-jms-<VERSION>-artemis-broker-interceptor.jar` into your broker classpath bound dir e.g. `mybroker0/lib`.

* To bind TNT4J-Streams ActiveMQ Artemis interceptors for server side (broker) API edit your broker configuration file `broker.xml` by
  changing `remoting-incoming-interceptors` and `remoting-outgoing-interceptors` sections:
  ```xml
  <configuration xmlns="urn:activemq"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xmlns:xi="http://www.w3.org/2001/XInclude"
                 xsi:schemaLocation="urn:activemq /schema/artemis-configuration.xsd">

    <core xmlns="urn:activemq:core" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="urn:activemq:core ">
        <!--...-->

        <remoting-incoming-interceptors>
           <class-name>com.jkoolcloud.tnt4j.streams.custom.interceptors.jms.artemis.PacketInterceptor</class-name>
           <class-name>com.jkoolcloud.tnt4j.streams.custom.interceptors.jms.artemis.AmqpInterceptor</class-name>
           <class-name>com.jkoolcloud.tnt4j.streams.custom.interceptors.jms.artemis.MQTTInterceptor</class-name>
           <class-name>com.jkoolcloud.tnt4j.streams.custom.interceptors.jms.artemis.OpenWireInterceptor</class-name>
           <class-name>com.jkoolcloud.tnt4j.streams.custom.interceptors.jms.artemis.StompInterceptor</class-name>
        </remoting-incoming-interceptors>

        <remoting-outgoing-interceptors>
           <class-name>com.jkoolcloud.tnt4j.streams.custom.interceptors.jms.artemis.PacketInterceptor</class-name>
           <class-name>com.jkoolcloud.tnt4j.streams.custom.interceptors.jms.artemis.AmqpInterceptor</class-name>
           <class-name>com.jkoolcloud.tnt4j.streams.custom.interceptors.jms.artemis.MQTTInterceptor</class-name>
           <class-name>com.jkoolcloud.tnt4j.streams.custom.interceptors.jms.artemis.OpenWireInterceptor</class-name>
           <class-name>com.jkoolcloud.tnt4j.streams.custom.interceptors.jms.artemis.StompInterceptor</class-name>
        </remoting-outgoing-interceptors>
      </core>
  </configuration>
  ```
  Here we bind all available TNT4J-Streams interceptors, but you can pick just ones matching your broker enabled acceptor protocols.
* To define system property `tnt4j.config` pointing where to look for TNT4J configuration edit your broker run script file `artemis.cmd` 
  (or `artemis.sh`) by appending `JVM_ARGS` variable definition:
  * MS Windows
    ```cmd
    set JVM_ARGS=%JVM_ARGS% -Dtnt4j.config=%ARTEMIS_INSTANCE_ETC%\tnt4j\tnt4j.properties
    ```
  * *nix
    ```bash
    JVM_ARGS="$JVM_ARGS -Dtnt4j.config=$ARTEMIS_INSTANCE_ETC/tnt4j/tnt4j.properties"
    ```
* Since TNT4J-Streams Active MQ Artemis interceptors uses own logging it is recommended to use dedicated logging configuration, like one 
  defined in [logging.properties](mybroker0/etc/tnt4j/logging.properties):
  ```properties
  ##############################################################
  ### TNT4J Streams configuration section                    ###
  ##############################################################

  # Add additional streams loggers
  loggers=${loggers},com.jkoolcloud.tnt4j.streams,com.jkoolcloud.tnt4j.streams.activities_prod_log

  # Streams logging into common log file
  logger.com.jkoolcloud.tnt4j.streams.level=INFO

  # Streams logging into dedicated file
  logger.com.jkoolcloud.tnt4j.streams.handlers=STREAMS_FILE
  logger.com.jkoolcloud.tnt4j.streams.useParentHandlers=false

  #### streamed activity entities logger ####
  logger.com.jkoolcloud.tnt4j.streams.activities_prod_log.level=INFO
  logger.com.jkoolcloud.tnt4j.streams.activities_prod_log.handlers=ACTIVITIES_PROD
  logger.com.jkoolcloud.tnt4j.streams.activities_prod_log.useParentHandlers=false

  # Streams file logger
  handler.STREAMS_FILE=org.jboss.logmanager.handlers.PeriodicSizeRotatingFileHandler
  handler.STREAMS_FILE.level=INFO
  handler.STREAMS_FILE.properties=rotateSize,rotateOnBoot,maxBackupIndex,suffix,append,autoFlush,fileName
  handler.STREAMS_FILE.rotateSize=10485760
  handler.STREAMS_FILE.rotateOnBoot=false
  handler.STREAMS_FILE.maxBackupIndex=20
  handler.STREAMS_FILE.suffix=.yyyyMMdd.gz
  handler.STREAMS_FILE.append=true
  handler.STREAMS_FILE.autoFlush=true
  handler.STREAMS_FILE.fileName=${artemis.instance}/log/tnt4j-streams.log
  handler.STREAMS_FILE.formatter=STREAMS_PATTERN

  # Streams file logger pattern
  formatter.STREAMS_PATTERN=org.jboss.logmanager.formatters.PatternFormatter
  formatter.STREAMS_PATTERN.properties=pattern
  formatter.STREAMS_PATTERN.pattern=%d %-5p [%t!%c{1}] - %m%n

  # Streams file logger
  handler.ACTIVITIES_PROD=org.jboss.logmanager.handlers.PeriodicSizeRotatingFileHandler
  handler.ACTIVITIES_PROD.level=INFO
  handler.ACTIVITIES_PROD.properties=rotateSize,rotateOnBoot,maxBackupIndex,suffix,append,autoFlush,fileName
  handler.ACTIVITIES_PROD.rotateSize=10485760
  handler.ACTIVITIES_PROD.rotateOnBoot=false
  handler.ACTIVITIES_PROD.maxBackupIndex=20
  handler.ACTIVITIES_PROD.suffix=.yyyyMMdd.gz
  handler.ACTIVITIES_PROD.append=true
  handler.ACTIVITIES_PROD.autoFlush=true
  handler.ACTIVITIES_PROD.fileName=${artemis.instance}/log/tnt4j-streams-prod-activities.log
  handler.ACTIVITIES_PROD.formatter=ACTIVITIES_PATTERN

  # Streams activities logger pattern
  formatter.ACTIVITIES_PATTERN=org.jboss.logmanager.formatters.PatternFormatter
  formatter.ACTIVITIES_PATTERN.properties=pattern
  formatter.ACTIVITIES_PATTERN.pattern=%m%n

  ##############################################################
  ```
  If you decided to use dedicated logging configuration file instead of appending above section to default one 
  `<broker>/etc/logging.proeprties`, then do not forget to alter your broker run script file `artemis.cmd`
  (or `artemis.sh`) by changing `ARTEMIS_LOGGING_CONF` variable definition to:
  * MS Windows
    ```cmd
    set ARTEMIS_LOGGING_CONF=%ARTEMIS_INSTANCE_ETC_URI%\tnt4j\logging.properties
    ```
  * *nix
    ```bash
    ARTEMIS_LOGGING_CONF="$ARTEMIS_INSTANCE_ETC_URI/tnt4j/logging.properties"
    ```

#### Full configuration preset

See [mybroker0](mybroker0) dir content for complete interceptors solution configuration. To have all TNT4J in one place you can put it in 
your broker configuration dir (default is `etc`) sub-dir like `tnt4j`.

#### Logs

TNT4J-Streams ActiveMQ Artemis interceptors produced logs by default shall be found in `<broker>/log` directory.
