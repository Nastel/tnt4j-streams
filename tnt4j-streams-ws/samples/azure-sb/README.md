
# Azure SB metrics streaming over TNT4J

## Set up Azure SB Monitoring Application Service Principal (SPN)

**NOTE:** This part is not needed if you already have your service principal set to access Azure SB metrics or if you are using Azure
Managed Identities for it.

Create a service principal and assign the Reader role to the service principal. This can be done using these CLI commands:
```bash
az login
az account set --subscription "<your subscription id>"
# Azure SB resource group format is: /subscriptions/<SUBSCRIPTION_ID>/resourceGroups/<RESOURCE_GROUP_NAME>
#                              like: /subscriptions/c3xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxf8/resourceGroups/AndriusSB-RG
az ad sp create-for-rbac -n "readSBMetric" --role Reader --scope "<list of Azure SB bound resource groups to read metrics>"
```
The last command in the example above produces output like this:
```json
{
  "appId": "ec599183-xxxx-xxxx-xxxx-xxxxxxxxxc8f",
  "displayName": "readSBMetric",
  "password": "FGixxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxubwF",
  "tenant": "5029a0f1-xxxx-xxxx-xxxx-xxxxxxxxxx78"
}
```

## Set up TNT4J stream

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
  * Set configuration to obtain Azure SB  metrics access token (only one of `GetAPITokenSPN`, `GetAPITokenIMDS` steps shall be enabled):
    * Set your REST API access service principal (SPN) credentials (step `GetAPITokenSPN`). (These are the credentials provided by the
      `az ad sp create-for-rbac` command executed above):
      ```xml
      <property name="AzureTenant" value="5029a0f1-xxxx-xxxx-xxxx-xxxxxxxxxx78"/>
      <property name="AzureAppId" value="ec599183-xxxx-xxxx-xxxx-xxxxxxxxxc8f"/>
      <property name="AzureSecret" value="FGixxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxubwF"/>
      ```
    * Set your configuration to obtain Azure Managed Identities (MI) token from Azure Instance Metadata Service (IMDS):
      * Set your IMDS URL:
        ```xml
        <request id="GetTokenIMDS"><![CDATA[
            http://169.254.169.254/metadata/identity/oauth2/token
        ]]>
        ```
      * Set request parameter `api-version` to match one run by your IMDS service:
        ```xml
        <!-- may be one of: `2018-02-01`, `2018-11-01`, `2019-06-01`, `2019-08-01`, `2020-06-01`, `2021-05-01`, `2021-12-13`, `2022-08-01`, `2023-01-01` -->
        <req-param id="api-version" value="2020-06-01"/>
        ```
      * Set optional request parameter if needed:
        ```xml
        <req-param id="object_id" value="xxxxxxx"/>
        <req-param id="client_id" value="xxxxxxx"/>
        <req-param id="msi_res_id" value="xxxxxxx"/>
        ```
  * Set your Azure SB cluster info to collect metrics (step `GetAzureSBMetrics`):
    ```xml
    <property name="AzureSubscriptionId" value="c3cbb071-xxxx-xxxx-xxxx-xxxxxxxxxxf8"/>
    <property name="AzureResourceGroup" value="AndriusSB-RG"/>
    <property name="AzureSBNamespace" value="meshiq"/>
    ```
  * Set the metrics collection request interval by changing the configuration entries below accordingly (the default interval is
    `5 minutes`):
    ```xml
    <!-- The interval (i.e. time grain) of the query. Values may be: PT1M, PT5M, PT15M, PT30M, PT1H, PT6H, PT12H, P1D -->
    <property name="AzureMetricsInterval" value="PT5M"/>
    ...
    <!-- The interval of REST API calls to collect Azure SB metrics -->
    <schedule-simple interval="5" units="Minutes" startDelay="10" startDelayUnits="Seconds" repeatCount="-1"/>
    ...
    <!-- Sets metrics timespan start date and time: groovy expression to calculate timestamp for 5 minutes back from now -->
    <req-param id="timespanStart" value="${groovy:5.minutes.ago}" format="yyyy-MM-dd'T'HH:mm:ss'Z'" timezone="UTC" transient="true"/>
    ```
* Configure metrics collection properties. There are three Azure REST API requests named:
  * `GetNamespaceMetrics` - to collect your namespace scoped metrics
  * `GetEntitiesMetrics` - to collect namespace bound entity scoped metrics
  * `GetThrottleMetrics` - to collect `ThrottledRequests` scoped by `MessagingErrorSubCode`.

  The REST API calls interval is configured through a simple scheduler configuration:
  ```xml
  <schedule-simple interval="5" units="Minutes" startDelay="10" startDelayUnits="Seconds" repeatCount="-1"/>
  ```
  * `interval` - defines REST API call interval
  * `units` - defines call interval time units
  * `startDelay` - defines how long to delay the request after the application starts. We want to obtain access token before.
  * `startDelayUnits` - defines delay time units
  * `repeatCount` - defines how many requests to schedule. `-1` means infinite.

  Azure API call requests have similar parameters to configure:
  * `metricnames` - the names of the metrics to retrieve (comma separated). **Special case:** if a metric name contains a comma, use `%2` to
    indicate the coma within the name. Example: `Metric,Name1` should be `Metric%2Name1`
  * `aggregation` - the list of aggregation types to retrieve (comma separated)
  * `timespan` - the timespan of the query. It is a string with the following format `startDateTime_ISO/endDateTime_ISO`. Supported ISO-8601
     time interval format: `Datetime/Datetime`, `Datetime/Duration`, `Duration/Datetime`, `Duration`
  * `interval` - the interval (i.e. time grain) of the query. Values may be: `PT1M`, `PT5M`, `PT15M`, `PT30M`, `PT1H`, `PT6H`, `PT12H`, `P1D`
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

### Run the TNT4J stream

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

* See [How to get access token to pull Azure Monitor metrics for a specific subscription?](https://stackoverflow.com/questions/60516007/how-to-get-access-token-to-pull-azure-monitor-metrics-for-a-specific-subscriptio)
* See [How to use managed identities for Azure resources on an Azure VM to acquire an access token](https://learn.microsoft.com/en-us/entra/identity/managed-identities-azure-resources/how-to-use-vm-token)

### Setup Microsoft Entra ID (formerly Azure Active Directory)

* See [Get Microsoft Entra ID (formerly Azure AD) tokens for service principals](https://learn.microsoft.com/en-us/azure/databricks/dev-tools/service-prin-aad-token).

### REST API call configuration

* See [Monitoring Azure Service Bus data reference](https://learn.microsoft.com/en-us/azure/service-bus-messaging/monitor-service-bus-reference#metrics).
* See [Monitoring metrics documentation](https://learn.microsoft.com/en-us/rest/api/monitor/metrics/list?tabs=HTTP).
* See [Metrics List](https://learn.microsoft.com/en-us/azure/azure-monitor/reference/supported-metrics/microsoft-servicebus-namespaces-metrics).
* See [Azure monitoring REST API walkthrough](https://learn.microsoft.com/en-us/azure/azure-monitor/essentials/rest-api-walkthrough?tabs=portal).
