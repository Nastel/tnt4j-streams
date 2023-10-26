# Prometheus metrics streaming over TNT4J

## Setup TNT4J stream

All required configuration shall be done in [tnt-data-source.xml](tnt-data-source.xml) file.

* Configure XRay access:
  * Set your XRay access token:
    ```xml
    <property name="event.sink.factory.EventSinkFactory.xray.Url" value="https://data.jkoolcloud.com"/>
    <property name="event.sink.factory.EventSinkFactory.xray.Token" value="388xxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxb3"/>
    ```
* To receive Prometheus metrics data TNT4J-Streams runs HTTP server. It requires such configuration:
  ```xml
  <!-- Stream run HTTP server configuration -->
  <property name="Host" value="127.0.0.1"/>
  <property name="Port" value="8787"/>
  <property name="UseSSL" value="false"/>
  <property name="Keystore" value="file://clientkeystore"/>
  <property name="KeyPass" value="pass123"/>
  <property name="KeystorePass" value="pass123"/>
  ```

  * `Host` - is hostname or IP address to run HTTP server
  * `Port` - is port number to open for HTTP connections
  * `UseSSL` - flag indicating whether to require SSL connections
    * `Keystore` - keystore file path to use for SSL connections
    * `KeyPass` - private RSA key password
    * `KeystorePass` - keystore password

### Run TNT4J stream

Execute shell script:
* Linux
  ```bash
  ./run.sh
  ```
* MS Windows
  ```cmd
  run.bat
  ```
