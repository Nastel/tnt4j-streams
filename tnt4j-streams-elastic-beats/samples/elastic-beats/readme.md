# Configuring Elastic Beats

* Open Elastic Beats configuration file, e.g. `metricbeat.yml`, `filebat.yml`, etc.
* Configure Beats output to Logstash:
```properties
    # The Logstash hosts
    hosts: ["localhost:5044"]
```

# Running Elastic Beats stream

* Configure `TNT4J-Streams` (set jKool token if not yet)
* Run `TNT4J-Streams` sample `elastic-beats` with your parser configuration
* Start `metricbeat`
```cmd
    /metricbeat -c metricbeat.yml -e
```

# Importing dashboard into your jKool repository

You can import provided Dashboards into jKool:
* Login your `https://xray.meshiq.com`
* menu -> import/export -> dashboards -> import -> import dashboards -> chose file
* select dashboard file from `samples/elastic-beats/dashboards` directory