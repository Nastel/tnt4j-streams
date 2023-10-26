
# Azure ESB metrics streaming over TNT4J

## Setup Azure ESB monitoring application service principal

* Create a service principal and assign Reader role for the sp. (I use Azure CLI to do that)
  ```bash
  az login
  az account set --subscription "<your subscription id>"
  az ad sp create-for-rbac -n "readESBMetric" --role Reader --scope "<list of resource groups to read metrics>" 
  ```
  Last command shall produce output like this:
  ```json
  {
    "appId": "ec599183-xxxx-xxxx-xxxx-xxxxxxxxxc8f",
    "displayName": "readESBMetric",
    "password": "FGixxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxubwF",
    "tenant": "5029a0f1-xxxx-xxxx-xxxx-xxxxxxxxxx78"
  }
  ```

## Setup TNT4J stream

All required configuration shall be done in [tnt-data-source.xml](tnt-data-source.xml) file.

* Configure XRay access:
  * Set your XRay access token:
    ```xml
    <property name="event.sink.factory.EventSinkFactory.prod.Url" value="https://data.jkoolcloud.com"/>
    <property name="event.sink.factory.EventSinkFactory.prod.Token" value="388xxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxb3"/>
    ```
* Configure your Azure ESB namespace access: 
  * Set your REST API access service principle credentials (ones provided by `az ad sp create-for-rbac`):
    ```xml
    <property name="AzureTenant" value="5029a0f1-xxxx-xxxx-xxxx-xxxxxxxxxx78"/>
    <property name="AzureAppId" value="ec599183-xxxx-xxxx-xxxx-xxxxxxxxxc8f"/>
    <property name="AzureSecret" value="FGixxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxubwF"/>
    ```
  * Set your ESB cluster info to collect metrics:
    ```xml
    <property name="AzureSubscriptionId" value="c3cbb071-xxxx-xxxx-xxxx-xxxxxxxxxxf8"/>
    <property name="AzureResourceGroup" value="AndriusAKS-RG"/>
    <property name="AzureESBNamespace" value="meshiq"/>
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

Additionally to configure metrics collection period start change [parsers.xml](parsers.xml) file `<cache>` section entry 
`<entry id="MetricsStartTime">` `default` value to date you want start collecting metrics for your ESB.

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
