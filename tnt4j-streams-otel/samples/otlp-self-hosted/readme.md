# Configuring TNT4J-Streams

Make these changes in file `samples/otlp-self-hosted/tnt-data-source.xml`:
1. set stream properties to match your environment
   ```xml
   <property name="Host" value="127.0.0.1"/>
   <property name="Port" value="8787"/>
   <property name="UseSSL" value="false"/>
   <property name="Keystore" value="file://clientkeystore"/>
   <property name="KeyPass" value="pass123"/>
   <property name="KeystorePass" value="pass123"/>
   ```
1. pick a sink to use by stream - you can choose one or multiple of predefined: `file`, `xray` or `ap` (default)
   ```xml
   <property name="event.sink.factory.BroadcastSequence" value="ap,xray"/>
   ```
   1. For the `file` sink you can change default file location by changing value for property:
      ```xml
      <property name="event.sink.factory.EventSinkFactory.file.FileName" value="./logs/tnt4j-streams-otlp_samples.log"/>
      ```
   1. For the `xray` sink, set your streaming token:
      ```xml
      <property name="event.sink.factory.EventSinkFactory.xray.Token" value="<YOUR-JKOOL-TOKEN>"/>
      ```
   1. For the `ap` sink, set your AutoPilot CEP facts streaming endpoint:
      ```xml
      <property name="event.sink.factory.EventSinkFactory.ap.Host" value="<AP_CEP_IP/HOST>"/>
      <property name="event.sink.factory.EventSinkFactory.ap.Port" value="6060"/>
      ```

# Configuring OpenTelemetry Collector

To make OpenTelemetry (OTel) collector to feed TNT4J-Streams with data over `OTLP` protocol you'll need to make these changes in your 
`otel-collector-config.yaml` file:
1. add `otlphttp` exporter definition
   ```yaml
   ...
   exporters:
     # Data sources: traces, metrics, logs
     otlphttp:
       traces_endpoint: http://127.0.0.1:8787/traces
       metrics_endpoint: http://127.0.0.1:8787/metrics
       logs_endpoint: http://127.0.0.1:8787/logs
       tls:
         insecure: true
   ... 
   ```
   **NOTE:** change `otlphttp` exporter defined local IP address `127.0.0.1` and port `8787` to match your environment!
1. make your OTel service pipelines to export `traces`, `metrics` or `logs` entries over `otlphttp` exporter
   ```yaml
   ...
   service:
     extensions: [pprof, zpages, health_check]
     pipelines:
       traces:
         receivers: [otlp]
         processors: [batch]
         exporters: [otlphttp]
       metrics:
         receivers: [otlp]
         processors: [batch]
         exporters: [otlphttp]
       logs:
         receivers: [otlp]
         processors: [batch]
         exporters: [otlphttp]
   ...
   ```
