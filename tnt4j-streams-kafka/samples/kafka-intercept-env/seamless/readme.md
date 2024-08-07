### How to seamlessly integrate Kafka-X-Ray into your Kafka environment

**NOTE:** most of shell script files uses variable `KAFKA_HOME` to make your Kafka environment reference over relative or (most preferable) 
absolute path. Check and change it accordingly.

1. One small change you'll have to do for your Kafka environment is to alter shell script file `kafka-run-class`:
    * *NIX: `<KAFKA_INSTALL_DIR>/bin/kafka-run-class.sh` by changing section after `# classpath addition for release` line:
    ```bash
    # classpath addition for release
    #for file in "$base_dir"/libs/*;
    #do
    #  if should_include_file "$file"; then
    #    CLASSPATH="$CLASSPATH":"$file"
    #  fi
    #done
    CLASSPATH="$CLASSPATH":"$base_dir/libs/*"
    ```
    * Windows: `<KAFKA_INSTALL_DIR>/bin/windows/kafka-run-class.bat` by changing section after `rem Classpath addition for release` line:
    ```cmd
    rem Classpath addition for release
    rem for %%i in ("%BASE_DIR%\libs\*") do (
    rem 	call :concat "%%i"
    rem )
    call :concat "%BASE_DIR%\libs\*"
    ```

    The change we made is needed to reduce final Kafka command parameters length. While for *nix systems it is more or less optional, for 
    Windows systems it is recommended, since Windows has limitation for commands length.

    That's it - no more changes in Kafka environment needed!

1. Kafka server (broker) starting and stopping:
    1. To start kafka server use shell script:
        * *nix: [nix/run-server.sh](./nix/run-server.sh)
        * Windows: [windows/run-server.bat](./windows/run-server.bat)

    **NOTE:** script also clears `<KAFKA_INSTALL_DIR>/logs` directory before etch start!

    1. To stop kafka server use shell scripts:
        * *nix: [nix/stop-server.sh](./nix/stop_server.sh)
        * Windows: [windows/stop-server.bat](./windows/stop-server.bat)

1. To start Kafka environment provided console consumer and producer use shell scripts:
    * *nix: [nix/run-cons_prod.sh](./nix/run-cons_prod.sh)
    * Windows: [windows/run-cons_prod.bat](./windows/run-cons_prod.bat)

1. (Optional) To configure Kafka messages trace interceptions at interceptors runtime, run configuration topic `tnt4j-trace-config-topic` 
producer:
    * *nix: [nix/run-cmd-prod.sh](./nix/run-cmd-prod.sh)
    * Windows: [windows/run-cmd-prod.bat](./windows/run-cmd-prod.bat)

    **NOTE:** see [Kafka trace control topic commands section](../../kafka-intercept/readme.md#kafka-trace-control-topic-commands) for more 
    details on trace configuration commands.

1. Run `Kafka-X-Ray` produced metrics consumer stream by executing:
    * *NIX: [nix/run-metrics_stream.sh](./nix/run-metrics_stream.sh)
    * Windows: [windows/run-metrics_stream.bat](./windows/run-metrics_stream.bat)