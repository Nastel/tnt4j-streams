# Installation of Apache Flume tnt4j-streams plugin

1. Copy `tnt4j-streams-core.jar` and `tnt4j-streams-flume-plugin.jar` to [flume_path]/lib directory
2. Copy Apache Flume tnt4j-streams plugin sample config files to [flume_path]/config/:

    * [my-flume.properties](./my-flume.properties) - located at `[tnt4j-streams-dir]/samples/apache-flume/`
    * [tnt4.properties](./../../../config/tnt4j.properties) - located at `[tnt4j-streams-dir]/config/`
    * [log4j.properties](./../../../config/log4j.properties)/[log4j2.xml](./../../../config/log4j2.xml) - located
      at `[tnt4j-streams-dir]/config/` **NOTE:** overwrite or merge this file into one provided by flume
    * [tnt-data-source.xml](./tnt-data-source.xml) - located at `[tnt4j-streams-dir]/samples/apache-flume/`

3. Adjust properties to your needs

    * change my-flume.properties:
      ```properties
      agent.sources.seqGenSrc.spoolDir=<LOGS DIR>
      ```
      to yours log directory or sink in your favor.
    * change tnt-data-source.xml config according your log format
    * change tnt4.properties:
      ```properties
      event.sink.factory.Token: ##############################
      ```
      by adding your jKoolCloud token

4. Run Flume:
   ```cmd
   [flume_path]/bin/flume-ng agent --conf ./conf/ -f conf/my-flume.properties -n agent
   ```

# Q & A

Q:    My log is not parsed correctly?

A:    Default sample is for on common Apache log format. In order to change ir you may need to change `<parser>` or its `<properties>`.
      Look in documentation for more examples.

Q:    Can I stream to different machine?

A:    Yes. Change host and/or port where TNT4J-Streams is running.
