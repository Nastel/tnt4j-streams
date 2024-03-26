
# Azure SB metrics streaming over TNT4J

## Setup Azure SB monitoring application service principal

* Create a service principal and assign Reader role for the sp. (I use Azure CLI to do that)
  ```bash
  az login
  az account set --subscription "<your subscription id>"
  # SB resource group format is: /subscriptions/<SUBSCRIPTION_ID>/resourceGroups/<RESOURCE_GROUP_NAME>
  #                        like: /subscriptions/c3xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxf8/resourceGroups/AndriusSB-RG
  az ad sp create-for-rbac -n "readSBMetric" --role Reader --scope "<list of SB bound resource groups to read metrics>" 
  ```
  Last command shall produce output like this:
  ```json
  {
    "appId": "ec599183-xxxx-xxxx-xxxx-xxxxxxxxxc8f",
    "displayName": "readSBMetric",
    "password": "FGixxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxubwF",
    "tenant": "5029a0f1-xxxx-xxxx-xxxx-xxxxxxxxxx78"
  }
  ```

## Setup TNT4J stream

All required configuration shall be done in [tnt-data-source.xml](tnt-data-source.xml) file.

* Configure streamed data broadcasting:
  * Route streamed data to Autopilot only, set: `<property name="event.sink.factory.BroadcastSequence" value="ap"/>`
  * Route streamed data to XRay only, set: `<property name="event.sink.factory.BroadcastSequence" value="xray"/>`
  * Route streamed data to both Autopilot and XRay simultaneously, set: `<property name="event.sink.factory.BroadcastSequence" value="ap,xray"/>`   
* Configure XRay access (optional if not present in broadcasting sequence):
  * Set your XRay access token:
    ```xml
    <property name="event.sink.factory.EventSinkFactory.prod.Url" value="https://stream.meshiq.com"/>
    <property name="event.sink.factory.EventSinkFactory.prod.Token" value="388xxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxb3"/>
    ```
* Configure AutoPilot access (optional if not present in broadcasting sequence):
  * Set your AutoPilot CEP facts streaming endpoint:
    ```xml
    <property name="event.sink.factory.EventSinkFactory.ap.Host" value="<AP_CEP_IP/HOST>"/>
    <property name="event.sink.factory.EventSinkFactory.ap.Port" value="6060"/>
    ```
* Configure your Azure SB namespace access: 
  * Set your REST API access service principle credentials (ones provided by `az ad sp create-for-rbac`):
    ```xml
    <property name="AzureTenant" value="5029a0f1-xxxx-xxxx-xxxx-xxxxxxxxxx78"/>
    <property name="AzureAppId" value="ec599183-xxxx-xxxx-xxxx-xxxxxxxxxc8f"/>
    <property name="AzureSecret" value="FGixxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxubwF"/>
    ```
  * Set your SB cluster info to collect metrics:
    ```xml
    <property name="AzureSubscriptionId" value="c3cbb071-xxxx-xxxx-xxxx-xxxxxxxxxxf8"/>
    <property name="AzureResourceGroup" value="AndriusSB-RG"/>
    <property name="AzureSBNamespace" value="meshiq"/>
    ```
  * Set metrics collection request interval (default is `5 minutes`) by changing configuration entries below accordingly:
    ```xml 
    <!-- The interval (i.e. timegrain) of the query. Values may be: PT1M, PT5M, PT15M, PT30M, PT1H, PT6H, PT12H, P1D -->
    <property name="AzureMetricsInterval" value="PT5M"/>
    ...
    <!-- The interval of REST API calls to collect SB metrics -->
    <schedule-simple interval="5" units="Minutes" startDelay="10" startDelayUnits="Seconds" repeatCount="-1"/>
    ...
    <!-- Sets metrics timespan start date and time: groovy expression to calculate timestamp for 5 minutes back from now -->
    <req-param id="timespanStart" value="${groovy:5.minutes.ago}" format="yyyy-MM-dd'T'HH:mm:ss'Z'" timezone="UTC" transient="true"/>
    ```
* Configure metrics collection properties. There are tree Azure REST API requests named:
  * `GetNamespaceMetrics` - to collect your namespace scoped metrics 
  * `GetEntitiesMetrics` - to collect namespace bound entity scoped metrics 
  * `GetThrottleMetrics` - to collect `ThrottledRequests` scoped by `MessagingErrorSubCode`.

  REST API calls interval is configured over simple scheduler configuration:
  ```xml 
  <schedule-simple interval="5" units="Minutes" startDelay="10" startDelayUnits="Seconds" repeatCount="-1"/>
  ```
  * `interval` - defines REST API call interval
  * `units` - defines call interval time units
  * `startDelay` - defines how long to delay of the request after the application starts. We want to obtain access token before.
  * `startDelayUnits` - defines delay time units
  * `repeatCount` - defines how many requests to schedule. `-1` means infinite.

  Azure API call requests have similar parameters to configure:
  * `metricnames` - the names of the metrics (comma separated) to retrieve. Special case: if a metricname itself has a comma in it then use 
    `%2` to indicate it. Eg: `Metric,Name1` should be `Metric%2Name1`
  * `aggregation` - the list of aggregation types (comma separated) to retrieve
  * `timespan` - the timespan of the query. It is a string with the following format `startDateTime_ISO/endDateTime_ISO`. Supported ISO-8601 
     time interval format: `Datetime/Datetime`, `Datetime/Duration`, `Duration/Datetime`, `Duration` 
  * `interval` - the interval (i.e. timegrain) of the query. Values may be: `PT1M`, `PT5M`, `PT15M`, `PT30M`, `PT1H`, `PT6H`, `PT12H`, `P1D`
  * `$filter` - used to reduce the set of metric data returned. Example: Metric contains metadata A, B and C. 
    - Return all time series of C where A = a1 and B = b1 or b2 `$filter=A eq 'a1' and B eq 'b1' or B eq 'b2' and C eq '*'` 
    - Invalid variant: `$filter=A eq 'a1' and B eq 'b1' and C eq '*' or B = 'b2'`. This is invalid because the logical or operator cannot 
    separate two different metadata names. 
    - Return all time series where A = a1, B = b1 and C = c1: `$filter=A eq 'a1' and B eq 'b1' and C eq 'c1'` 
    - Return all time series where A = a1 `$filter=A eq 'a1' and B eq '' and C eq ''`.

    Special case: When dimension name or dimension value uses round brackets. Eg: When dimension name is dim (test) 1 Instead of using 
    `$filter= "dim (test) 1 eq '' "` use **$filter= "dim %2528test%2529 1 eq '' "** When dimension name is dim (test) 3 and dimension value 
    is dim3 (test) val Instead of using `$filter= "dim (test) 3 eq 'dim3 (test) val' "` use 
    `$filter= "dim %2528test%2529 3 eq 'dim3 %2528test%2529 val' "` 
  * `metricnamespace` - metric namespace to query metric definitions for
  * `orderby` - the aggregation to use for sorting results and the direction of the sort. Only one order can be specified. Example: `sum asc`.
  * `resultType` - reduces the set of data collected. The syntax allowed depends on the operation. See the operation's description for 
     details. Values may be: `Data`, `Metadata`
  * `top` - the maximum number of records to retrieve. Valid only if `$filter` is specified. Defaults to 10.

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

## Additional resources

### Create token to access Azure Rest API

See [How to get access token to pull Azure Monitor metrics for a specific subscription?](https://stackoverflow.com/questions/60516007/how-to-get-access-token-to-pull-azure-monitor-metrics-for-a-specific-subscriptio)

### Setup Microsoft Entra ID (formerly Azure Active Directory)

See [Get Microsoft Entra ID (formerly Azure AD) tokens for service principals](https://learn.microsoft.com/en-us/azure/databricks/dev-tools/service-prin-aad-token).

### REST API call configuration

See [Monitoring Azure Service Bus data reference](https://learn.microsoft.com/en-us/azure/service-bus-messaging/monitor-service-bus-reference#metrics).
See [Monitoring metrics documentation](https://learn.microsoft.com/en-us/rest/api/monitor/metrics/list?tabs=HTTP).
See [Metrics List](https://learn.microsoft.com/en-us/azure/azure-monitor/reference/supported-metrics/microsoft-servicebus-namespaces-metrics).
See[Azure monitoring REST API walkthrough](https://learn.microsoft.com/en-us/azure/azure-monitor/essentials/rest-api-walkthrough?tabs=portal).
