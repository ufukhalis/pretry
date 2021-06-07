# Project Pretry
Pretty way of the retrying!

## Getting Started

`Pretry` is a standalone application that has aim to integrate with your system to make your retries more robust and easy way.

To be able to use `Pretry` application that you should need to run the application with below command.
Our application is available in the public docker repository. Here is the most crucial thing is you need to give a persistent volume to the application itself in your server.

`Pretry` uses `MapDB` as a storage, so it does not need to have any additional database stack.

```shell

```

After running the application, then you should need to push some configuration to be able to have retry feature.

`Pretry` exposes two end-points like below.

```json
http://{server-url}/v1/config

Request Body 

{
  "identifier" : "identifier-1", // a unique identifier for your application
  "maxRetry": 3, // Max retry count for your event or message
  "retryHours" : [1,2,3], // For each retry which time after it needs to be scheduled, so it should contain elements like maxRetry amount.
  "integrations": [
    { "type": "HTTP", "config" : {"url" : "url", "username" : "username", "password" : "password"} },
    { "type": "SQS", "config" : {"url" : "sqsUrl", "region" : "eu-west-1", "secretKey" : "secretKey", "accessKey" : "accessKey"} }
  ] 
}

```

Above request is an example configuration for retry operations.
`Integrations` part is important one for the `Pretry`, because when the retry happens, then `Pretry` will use those integration points to push your message or event.
So, at least you should have one integration point in your configuration.

Currently, `Pretry` supports `HTTP` and `SQS` integrations. In the future, there will be more integration.

And the last end-point.

```json
http://{server-url}/v1/event

Request Body

{
  "identifier" : "identifier-1",
  "eventBody" : {} // body of your message or event for retrying
}

```

With above end-point, you can push your messages or events for retrying with defined configurations.

To be able to understand `Pretry` application that we recommend to read below use cases.

### Use Case - 1
Imagine you have many microservices in your system. 
And those services are communicating with each-other via message or event or HTTP calls.
So, if some messages or calls are so crucial in your system then you should need some retries.

For example;

Service-A send a call to Service-B and it failed. 
This call was so important because `Service-B` can update its database for some user related data.
So in this case, the `Pretry` can be helpful about retrying this call later.
Imagine, Service-A failed and called `Pretry` end-point for retry operation with defined configuration.
From that time, `Pretry` will try to hit `Service-B`(depends of the configuration, maybe this can be designed to hit another service not directly to target service).


### Use Case - 2
Imagine that some data can be available after some hours.
It means when Service-A hits to Service-B the call failed due to lack of some data in Service-B database.
But, this data will be available in some hours so in that case, `Pretry` may help about retrying that same request with defined configuration.


### Use Case - 3
Imagine that your system has integration with another system.
So, Service-A is pushing the data to Service-C in the another system.
When this call failed due to anything again here `Pretry` can help about retrying that same request with defined configuration.
