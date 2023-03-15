
# AWS CloudWatch Metric Streams with Kinesis Data Firehose

Using Amazon CloudWatch Metric Streams and Amazon Kinesis Data Firehose, you will get CloudWatch metrics into HTTP End point within 3-minute
latency.

1. Create these AWS resources in your AWS account and region for which you want to stream this metrics:
   * Create a Kinesis Data Firehose delivery stream that delivers metrics to HTTP endpoint, along with Destination error logs to Cloudwatch
     and Backup logs to S3 account if any failed metrics delivery.
   * Create a CloudWatch Metric Stream linked to Firehose delivery stream.
   * Optionally specify a limited set of namespaces to stream metrics like S3, Kafka, EC2 etc.
1. Once you create these resources, destination HTTP endpoint immediately starts receiving the streamed metrics.

## Create a new Kinesis Data Firehose delivery stream

* Choose your source and destination
 
  For source, select “**Direct PUT or other sources**”
 
  For destination, select “**HTTP Endpoint**”.

  ![](img/001.png)

* Delivery stream name. For example: **PUT-HTTP-STREAM-NASTEL**

  ![](img/002.png)

* Configure Kinesis Data Firehose to transform your record data. (Optional) 

  Default – Disabled

  ![](img/003.png)

* Specify the destination settings for your delivery stream
  * Specify HTTP endpoint name(optional)
  * Provide your HTTP endpoint URL
  * Provide Access key Contact the endpoint owner(optional)
  * Content encoding, kinesis Data Firehose uses the content encoding to compress the body of a request before sending the request to the 
    destination (Disabled by default)
  * Retry duration, time period during which Kinesis Data Firehose retries sending data to the selected HTTP endpoint.
  * Parameters, Kinesis Data Firehose includes these key-value pairs in each HTTP call. (Optional)
  * Buffer hints, Kinesis Data Firehose buffers incoming records before delivering them to your HTTP endpoint domain which accepts only 
    HTTPS protocol. Record delivery is triggered once the value of either of the specified buffering hints is reached.

  ![](img/004.png)

  ![](img/005.png)

* Specify Back up settings which ensures that the data can be recovered if record processing transformation does not produce the desired 
  results.

  ![](img/006.png)

  ![](img/007.png)

* Provide Advance settings includes server-side encryption, Amazon CloudWatch error logging and IAM Role permissions and Tags.

  ![](img/008.png)

* Click Create delivery stream

## Create your CloudWatch Metric Stream

* Choose whether you want to stream all CloudWatch metrics, or choose specific namespaces with “Include” or “Exclude” lists. For example, 
  selected specific namespaces like EC2, Kafka and S3.

  ![](img/009.png)

* Configuration Settings
  * Select the Firehose you created to use for sending the metrics to HTTP endpoint.

  ![](img/010.png)

* Create a new service role to put records in Kinesis Data Firehose.
* Change the output format to be JSON

  ![](img/011.png)

* Add additional statistics to include the AWS percentile metrics you would like to send to Destination. 

  ![](img/012.png)

* Enter name your metric stream
* Click Create metric stream

Once you see the Metric Stream resource has been successfully created, wait five minutes for Destination HTTP endpoint.

## Example: Create an Amazon MSK cluster using the AWS Management Console

* Sign in to the AWS Management Console, and open the Amazon MSK console and Create cluster
* For Cluster name, enter demo-cluster-1.

  ![](img/013.png)

* From the table under General cluster settings, choose following settings 
  * Cluster Type
  * Apache Version
  * Broker Type
  * Storage

  ![](img/014.png)

  ![](img/015.png)

* From the table under All cluster settings, copy the values of the following settings and save them because you need them later in this tutorial:
  * VPC
  * Subnets
  * Security groups associated with VPC

  ![](img/016.png)

  ![](img/017.png)

* Click Create cluster.

Check the cluster Status on the Cluster summary page. The status changes from Creating to Active as Amazon MSK provisions the cluster. When the status is Active, you can connect to the cluster.

![](img/018.png)
