# Javascript/Typescript SDK for FeatureHub

## Overview
Welcome to the Javascript/Typescript SDK implementation for [FeatureHub.io](https://featurehub.io) - Open source Feature flags management, A/B testing and remote configuration platform.

This documentation covers both [featurehub-javascript-node-sdk](https://www.npmjs.com/featurehub-javascript-node-sdk) and [featurehub-javascript-client-sdk](https://www.npmjs.com/featurehub-javascript-client-sdk) and explains how you can use the FeatureHub SDK in Javascript or Typescript for applications like Node.js
backend server, Web front-end (e.g. React, Angular) or Mobile apps (React Native, Ionic, etc.). 

To control the feature flags from the FeatureHub Admin console, either use our [demo](https://demo.featurehub.io) version for evaluation or install the app using our guide [here](http://docs.featurehub.io/#_installation)

### **Important note:**

We have deprecated [FeatureHub Eventsource Javascript SDK](https://www.npmjs.com/package/featurehub-eventsource-sdk) which covers both client (browser) and server (node) applications in favor of splitting it into two separate NPM modules to enable support for additional browser frameworks like Angular and Vue. To transition to one of the new NPM modules, follow installation instructions below and change the imports in your code. The FeatureHub SDK API hasn't changed so you don't have to reimplement your SDK code.

## SDK installation

Run to install the dependency: 

if you are intending to use this SDK with React, Angular and other browser frameworks:

`npm install featurehub-javascript-client-sdk`
           
if you are using NodeJS use

`npm install featurehub-javascript-node-sdk`

(and further imports you see below should refer to this node library instead of the client library)


## Options to get feature updates  

There are 2 ways to request for feature updates via this SDK:


- **FeatureHub polling client (GET request updates)** 
  
  In this mode, you make a GET request, which you can choose to either do once, when specific things happen in your application,
  (such as navigation change) or on a regular basis (say every 5 minutes) and the changes will be passed into the FeatureHub repository for processing. This mode is recommended for browser type applications (React, Angular, Vue) and server applications (Node).

- **SSE (Server Sent Events) realtime updates mechanism**

  In this mode, you will make a connection to the FeatureHub Edge server using the EventSource, and any updates to any features will come through to you in _near realtime_, automatically updating the feature values in the repository. This method is recommended for server (Node) applications. If you decide to use it in the browser applications, there is a known issues in the browsers with Kaspersky antivirus potentially blocking SSE events. [GitHub issue](https://github.com/featurehub-io/featurehub/issues/296)

## Quick start

### Connecting to FeatureHub
There are 3 steps to connecting:
1) Copy FeatureHub API Key from the FeatureHub Admin Console
2) Create FeatureHub config
3) Check FeatureHub Repository readiness and request feature state

#### 1. API Key from the FeatureHub Admin Console
Find and copy your API Key from the FeatureHub Admin Console on the API Keys page - 
you will use this in your code to configure feature updates for your environments. 
It should look similar to this: ```default/71ed3c04-122b-4312-9ea8-06b2b8d6ceac/fsTmCrcZZoGyl56kPHxfKAkbHrJ7xZMKO3dlBiab5IqUXjgKvqpjxYdI8zdXiJqYCpv92Jrki0jY5taE```.
There are two options - a Server Evaluated API Key and a Client Evaluated API Key. More on this [here](https://docs.featurehub.io/#_client_and_server_api_keys) 

Client Side evaluation is intended for use in secure environments (such as microservices, e.g Node JS) and is intended for rapid client side evaluation, per request for example.

Server Side evaluation is more suitable when you are using an _insecure client_. (e.g. Browser or Mobile). This also means you evaluate one user per client.

#### 2. Create FeatureHub config:

Create an instance of `EdgeFeatureHubConfig`. You need to provide the API Key and the URL of the FeatureHub Edge server.

```typescript
import {
  EdgeFeatureHubConfig,
  ClientContext,
  Readyness,
} from 'featurehub-javascript-client-sdk';

const edgeUrl = 'http://localhost:8085/';
const apiKey = 'default/3f7a1a34-642b-4054-a82f-1ca2d14633ed/aH0l9TDXzauYq6rKQzVUPwbzmzGRqe*oPqyYqhUlVC50RxAzSmx';

const fhConfig = new EdgeFeatureHubConfig(edgeUrl, apiKey);
```

By default, this SDK will use SSE client. If you decide to use FeatureHub polling client, you can override it here:

```typescript
import { FeatureHubPollingClient } from 'featurehub-javascript-client-sdk';
const FREQUENCY = 5000; // 5 seconds
fhConfig.edgeServiceProvider((repo, config) => new FeatureHubPollingClient(repo, config, FREQUENCY));
```

in this case it is configured for requesting an update every 5 seconds.



#### 3. Check FeatureHub Repository readiness and request feature state

Feature flag rollout strategies and user targeting are all determined by the active _user context_. If you are not intending to use rollout strategies, you can pass empty context to the SDK.

**Client Side evaluation** 

```typescript
fhConfig.init();

let initialized = false;
console.log("Waiting for features...");
fhConfig.addReadynessListener(async (ready) => {
  if (!initialized) {
    if (ready == Readyness.Ready) {
      console.log("Features are available, starting server...");
      initialized = true;
      const fhClient = await fhConfig.newContext().build();
      if(fhClient.getFlag('FEATURE_KEY')) { 
          // do something
      }
      else {
          //do something else
      }
    }
  }
});
```

This is a simple scenario where you request for default context without passing information for each user. In production, you would normally create new context per each user and if you are applying flag variations, you would pass information about user context. If you are using percentage rollout, for example, you would set a `sessionId`, or some other identifier that you can set through `userKey`). 

Frameworks like express and restify work by implementing a middleware concept that allows wraparound logic for each request. In a Node JS server, we would typically add to the part that finds the user something that is able to create a new context, configure it for the detected user and stash it in the request ready for the actual
method that processes this request to use.

```typescript
import { FeatureHubConfig } from './feature_hub_config';

export function userMiddleware(fhConfig: FeatureHubConfig) {
  return (req: any, res: any, next: any) => {
    const user = detectUser(req); // function to analyse the Bearer token and determine who the user is
    
    let fhClient = fhConfig.newContext();
    
    if (user) {
    	fhClient = fhClient.userKey(user.email);
    	// add anything else relevant to the context
    }
    
    fhClient = fhClient.build().then(() => {
    	req.featureContext = fhClient;
    	
      next();
    });
  };
}
```

A simple GET method on / for example could now determine based on the user if they should send one message or
another:

```typescript
app.get('/', function (req, res) {
	if (req.featureContext.isEnabled('FEATURE_KEY')) {
		req.send('The feature is enabled');
  } else {
    res.send('The feature is disabled.');
  }
})
```


**Server side evaluation**

In the server side evaluation (e.g. browser app) the context is created once as you evaluate one user per client. 

```typescript
let initialized = false;
let fhClient: ClientContext;
const fhConfig = new EdgeFeatureHubConfig(edgeUrl, apiKey);

async initializeFeatureHub() {
  fhClient = await fhConfig.newContext().build();
  fhConfig.addReadynessListener((readyness) => {
    if (!initialized) {
      if (readyness === Readyness.Ready) {
        initialized = true;
        const value = fhClient.feature('FEATURE_KEY').str;
        console.log('Value is ', value);
      }
    }
  });

  // if using flag variations and setting rollout strategies,.e.g with a country rule
  fhClient
      .country(StrategyAttributeCountryName.Australia)
      .build();

  // react to incoming feature changes in real-time. With NodeJS apps it is recommended to 
  // use it as a global variable to avoid a memory leak
  fhClient.feature('FEATURE_KEY').addListener(fs => {
    console.log('Value is ', fs.str);
  });
}
 
this.initializeFeatureHub();
```

  Note, in a Single Page Application (SPA) situation, you will typically load and configure your FeatureHub configuration, but not discover information about a user until later. This would mean that you could progressively add extra information to the context over time, once the user logs in, etc. There are all sorts of different ways that Web applications find and
  provide information. In our [React example](https://github.com/featurehub-io/featurehub-examples/tree/master/todo-frontend-react-typescript) we show how once you have your connection you are able to start querying the repository immediately.


#### Supported feature state requests

* Get a raw feature value through the following methods:
  - `getFlag('FEATURE_KEY') | getBoolean('FEATURE_KEY')` returns a boolean feature (by key) or _undefined_ if the feature does not exist. Alternatively use `feature('FEATURE_KEY').flag`
  
  - `getNumber('FEATURE_KEY')` | `getString('FEATURE_KEY')` | `getJson('FEATURE_KEY')` returns the value or _undefined_ if the feature is empty or does not exist. Alternatively use `feature('FEATURE_KEY').num`, `feature('FEATURE_KEY').str`, `feature('FEATURE_KEY').rawJson`
* Use convenience functions:
  - `isEnabled('FEATURE_KEY')` - always returns a _true_ or _false_, _true_
    only if the feature is a boolean and is _true_, otherwise _false_. Alternatively use `feature('FEATURE_KEY').enabled`
  - `isSet('FEATURE_KEY')` - in case a feature value is not set (_null_) (this can only happen for strings, numbers and json types), this check returns _false_.
    If a feature doesn't exist - returns _false_. Otherwise, returns _true_.

  - `feature('FEATURE_KEY').exists` - returns _true_ if the flag is represented by a flag from FeatureHub. If a flag is requested that has not been created
    yet, this will be `false`.
  - `feature('FEATURE_KEY').locked` - returns _true_ if feature is locked, otherwise _false_
  - `feature('FEATURE_KEY').version` - returns feature update version number (every change on the feature causes its version to update).
  - `feature('FEATURE_KEY').type` - returns type of feature (boolean, string, number or json)
  - `feature('FEATURE_KEY').addListener` - see _Feature updates listener_ below.
  

## Rollout Strategies and Client Context

FeatureHub supports client and server side evaluation of complex rollout strategies
that are applied to individual feature values in a specific environment. This includes support of preset rules, e.g. per **_user key_**, **_country_**, **_device type_**, **_platform type_** as well as **_percentage splits_** rules and custom rules that you can create according to your application needs.


For more details on rollout strategies, targeting rules and feature experiments see the [core documentation](https://docs.featurehub.io/#_rollout_strategies_and_targeting_rules).

```typescript
const fhClient = await fhConfig.newContext().userKey('user.email@host.com').country(StrategyAttributeCountryName.NewZealand)
 	.build();

    if (fhClient.feature('FEATURE_KEY').enabled) {
        //do something
    };
```

#### Coding for rollout strategies
There are several preset strategies rules we track specifically: `user key`, `country`, `device` and `platform`. However, if those do not satisfy your requirements you also have an ability to attach a custom rule. Custom rules can be created as following types: `string`, `number`, `boolean`, `date`, `date-time`, `semantic-version`, `ip-address`

FeatureHub SDK will match your users according to those rules, so you need to provide attributes to match on in the SDK:

**Sending preset attributes:**

Provide the following attribute to support `userKey` rule:

```typescript
    const fhClient = await fhConfig.newContext().userKey('ideally-unique-id').build(); 
```

to support `country` rule:

```typescript
    const fhClient = await fhConfig.newContext().country(StrategyAttributeCountryName.NewZealand).build(); 
```

to support `device` rule:

```typescript
    const fhClient = await fhConfig.newContext().device(StrategyAttributeDeviceName.Browser).build(); 
```

to support `platform` rule:

```typescript
    const fhClient = await fhConfig.newContext().platform(StrategyAttributePlatformName.Android).build(); 
```

to support `semantic-version` rule:

```typescript
    const fhClient = await fhConfig.newContext().version('1.2.0').build(); 
```

or if you are using multiple rules, you can combine attributes as follows:

```typescript
    const fhClient = await fhConfig.newContext().userKey('ideally-unique-id')
      .country(StrategyAttributeCountryName.NewZealand)
      .device(StrategyAttributeDeviceName.Browser)
      .platform(StrategyAttributePlatformName.Android)
      .version('1.2.0')
      .build(); 
```

The `build()` method will trigger the regeneration of a special header (`x-featurehub`) or parameter (in NodeJS is it is a header, in the Browser it is a parameter as the SSE spec doesn’t allow for extra headers). This in turn
will automatically retrigger a refresh of your events if you have already connected (unless you are using polling
and your polling interval is set to 0).

**Sending custom attributes:**

To add a custom key/value pair, use `attribute_value(key, value)`

```typescript
    const fhClient = await fhConfig.newContext().attribute_value('first-language', 'russian').build();
```

Or with array of values (only applicable to custom rules):

```typescript
   const fhClient = await fhConfig.newContext().attribute_value('languages', ['russian', 'english', 'german']).build();
```

You can also use `fhClient.clear()` to empty your context.

In all cases, you need to call `build()` to re-trigger passing of the new attributes to the server for recalculation.


**Coding for percentage splits:**
For percentage rollout you are only required to provide the `userKey` or `sessionKey`.

```typescript
    await fhConfig.newContext().userKey('ideally-unique-id').build();
```
or

```typescript
    await fhConfig.newContext().sessionKey('session-id').build();
```

For more details on percentage splits and feature experiments see [Percentage Split Rule](https://docs.featurehub.io/#_percentage_split_rule).



#### Feature updates listener

If the SDK detects a feature update, you also have an option to attach listeners
to these updates. The feature value may not change, but you will be able to evaluate the feature
again and determine if it has changed for your _Context_:

```typescript
const fhClient = await fhConfig.newContext().build();
fhConfig.repository().feature('FEATURE_KEY').addListener((fs) => {
  console.log(fs.getKey(), 'is', fhClient.isEnabled(fs.getKey()));
});
```

What you are passed is the _raw_ feature without any enhancements (including context), so ideally
you would not use this directly, use it from the _Context_.

Note, how fast you get these updates depends on the client you use. If you are using the EventSource
client, it will be close to immediately after they have been updated. If you are using the Polling
client, it will be when the next update happens.

You can attach as many callbacks for each feature as you like.

### Logging

This client exposes a class called `FHLog` which has two methods, i.e.:

```typescript
export type FHLogMethod = (...args: any[]) => void;
export class FHLog {
  public log: FHLogMethod = (...args: any[]) => { console.log(args); };
  public error: FHLogMethod = (...args: any[]) => { console.error(args); };
}
```

You can replace these methods with whatever logger you use to ensure you get the right format logs (e.g. Winston, Bunyan, Log4js). 

There is a `.quiet()` method available on FHLog which will silence logs.

### Connecting to Google Analytics

To see feature analytics (events) fire in your Google Analytics, you will require to have valid GA Tracking ID, e.g. 'UA-XXXXXXXXX-X'.
You also need to specify a CID - a customer id this is associate with GA.
Read more about CID [here](https://stackoverflow.com/questions/14227331/what-is-the-client-id-when-sending-tracking-data-to-google-analytics-via-the-mea)

```typescript
// add an analytics adapter with a random or known CID
  fhConfig.addAnalyticCollector(new GoogleAnalyticsCollector('UA-1234', '1234-5678-abcd-1234'));   
```

To log an event in Google Analytics: 
 ```typescript
fhClient.logAnalyticsEvent('todo-add', new Map([['gaValue', '10']]));  //indicate value of the event through gaValue   
```

### NodeJS server usage

For the full example refer to the FeatureHub examples repo [here](https://github.com/featurehub-io/featurehub-examples/tree/master/todo-backend-typescript)

### React usage 

For the full example refer to the FeatureHub examples repo [here](https://github.com/featurehub-io/featurehub-examples/tree/master/todo-frontend-react-typescript)

##Detailed documentation

### The FeatureHub repository Overview

The FeatureHub repository is a single class that holds and tracks features in your system. It gets features delivered
to it to process, tracks changes, and allows you to find and act on features in a useful way. 
It also sends events out in certain circumstances.

### SSE connectivity 

SSE kills your connection regularly to ensure stale connections are removed. For this reason you will see the connection being dropped and then reconnected again every 30-60 seconds. This is expected and in the below snippet you can see how you can potentially deal with the server readiness check. If you would like to change the reconnection interval, you have an option of changing maxSlots in the Edge server.

Check FeatureHub Repository readiness and request feature state:

```typescript
fhConfig.init();
let failCounter = 0;
let fhInitialized = false;

fhConfig.addReadynessListener(async (readyness: Readyness): void => {
  if (!fhInitialized && readyness === Readyness.Ready) {
    logger.event('started_featurehub_event', Level.Info, 'Connected to FeatureHub');
    startServer();
    fhInitialized = true;
    const fhClient = await fhConfig.newContext().build();
    if (fhClient.feature('FEATURE_KEY').flag) {
      // do something
    }
  } else if (readyness === Readyness.Failed && failCounter > 5) {
    logger.event('featurehub_readyness_failed', Level.Error, 'Failed to connect to FeatureHub');
    process.exit(1);
  } else if (readyness === Readyness.Failed) {
    failCounter++;
  } else {
    failCounter = 0;
  }
});
```

 If it is important to your server instances that the connection to the feature server exists as a critical service, then the snippet above will ensure it will try and connect (say five times) and then kill the server process alerting you to a failure. If connection to the feature service is only important for initial starting of your server, then you can simply listen for the first readiness and start your server and ignore all subsequent notifications:


```typescript
fhConfig.init();

let initialized = false;
console.log("Waiting for features...");
fhConfig.addReadynessListener(async (ready) => {
  if (!initialized) {
    if (ready == Readyness.Ready) {
      console.log("Features are available, starting server...");
      initialized = true;
      const fhClient = await fhConfig.newContext().build();
      if(fhClient.feature('FEATURE_KEY').flag) { 
          // do something
      }
      else {
          //do something else
      }
    }
  }
});
```


### Meta-Events from the repository

There are two "meta events" from the FeatureHub repository, readiness and "new feature available". 

#### Readiness 

Readiness is triggered when your repository first receives a list of features or it fails on a subsequent update. In a
UI application this would indicate that you had all the state necessary to show the application. In a nodejs server,
this would indicate when you could start serving requests.

````typescript
fhConfig.addReadynessListener((readyness) => {
  if (readyness === Readyness.Ready) {
       console.log("Features are available, starting server...");
   
       api.listen(port, function () {
         console.log('server is listening on port', port);
       })
  }
});
````

#### New Feature State Available 

The repository tracks features and their states by version number. When a new version of a feature state arrives,
say a flag changes from off to on, then the repository will check this version is really newer, and if so, it will
(in the default, immediate mode) apply that change to the current feature state it is holding. From this it will
trigger any events on that particular feature, and it can also trigger a generic event - 
`postLoadNewFeatureStateAvailable`. This event gets triggered once no matter if a bundle of changes comes in, or a 
single change comes in. 

You would typically use this to know if events have occurred that mean you need to go back and get event states or
rerender a page or similar.

This event gets a little more complicated when using the second (non default) mode - _catch and release_ - discussed
in more detail with examples below. In this mode, the repository will receive the updates, and compare them, but it
will *not* apply them to the features in the repository. As such, in this mode, a change on the server that 
turns up in the repository (via GET or EventSource) will *not* be applied to the local feature state, it will be held.
And the effect of this is that this event will _not_ fire. When you tell the repository to process these "held" 
changes, then the event will fire. 

Attaching a listener for this hook is done like this:

```typescript
fhConfig.repository()
  .addPostLoadNewFeatureStateAvailableListener((_) => {
    // e.g. tell user to page is going to update and re-render page
});  
```



## Reacting to feature changes

Unlike the server focused APIs, Typescript/Javascript has two modes of operation.

### Immediate reaction (recommended for servers)

In this mode, as changes occur to features coming from the server, the states of the features will immediately change.
Events are fired. This kind of operation is normally best for servers, as they want to react to what has been asked for.

You do not have to write any code to get this mode as it is the default behaviour.

### Catch and Release (recommended for Web and Mobile)

This is a deliberate holding onto the updates to features until such a time as they are "released". This is separate
from them coming down from the source and being put in the repository. In _catch and release_ mode, the repository will
hold onto the changes (only the latest ones) and apply them when you chose to "release them". This means there will be
no delay while making a GET request for the latest features for example when you wish to "check" for new updates to 
features when shifting pages or similar.

This strategy is recommended for Web and Mobile applications as controlled visibility for the user is important.  

```javascript
// don't allow feature updates to come through
fhConfig.catchAndReleaseMode = true; 
```

If you choose to not have listeners, when you call: 

```javascript
fhConfig.release();
```

then you should follow it with code to update your UI with the appropriate changes in features. You
won't know which ones changed, but this can be a more efficient state update than using the listeners above.

## Failure

If for some reason the connection to the FeatureHub server fails - either initially or for some reason during
the process, you will get a readiness state callback to indicate that it has now failed.

```javascript
export enum Readyness {
  NotReady = 'NotReady',
  Ready = 'Ready',
  Failed = 'Failed'
}
```



## Analytics

Allows you to connect your application and see your features performing in Google Analytics.

When you log an event on the repository,
it will capture the value of all of the feature flags and feature values (in case they change),
and log that event against your Google Analytics, once for each feature. This allows you to
slice and dice your events by state each of the features were in. We send them as a batch, so it
is only one request.

Note that if you log the analytics event _on the client context_ (`ctx.logAnalyticsEvent`) it captures that user's features. If you log
them on the repository itself (`fhConfig.repository().logAnalyticsEvent...`) then it logs the features as they are
handed back from the server. If you are using a Server Evaluated Key, these will be the same, but you should try
and always use the Client Context to log analytics events.

There are two different implementations, one for when you are in the browser and one for when you
are in the server, like nodejs. You don't need to worry about this, the code detects which one it is in and 
creates the correct instance. 

There is a plan to support other Analytics tools in the future. The only one we
currently support is Google Analytics, so you need:

- a Google analytics key - usually in the form `UA-123456`. You must provide this up front.
- a CID - a customer id this is associate with this. You can provide this up front or you can
provide it with each call, or you can set it later. 

1) You can set it in the constructor:

```typescript
const collector = new GoogleAnalyticsCollector('UA-123456', 'some-CID');
```

2) You can tell the collector later.

```typescript
const collector = new GoogleAnalyticsCollector('UA-123456');
collector.cid = 'some-value'; // you can set it here
```

3) When you log an event, you can pass it in the map:

```typescript
const data = new Map<string, string>();
data.set('cid', 'some-cid');

ctx.logAnalyticsEvent('event-name', data);
```

4) For a NODE server, you can set as an environment variable named `GA_CID`.

```typescript
fhConfig.addAnalyticCollector(collector);
```

As you can see from above (in option 3), to log an event, you simply tell the repository to
log an analytics event. It will take care of bundling everything up, passing it off to the
Google Analytics collector which will post it off.

Read more on how to interpret events in Google Analytics [here](https://docs.featurehub.io/analytics.html)

## Feature consistency between client and server

There are a number of use cases where it makes sense that the features the client sees should be the same
as the features that the server sees. In any environment however where both the server and client are pulling (or
getting pushed) their features from the FeatureHub servers, both should update at the same time. 

With the _Catch and Release_ functionality however, the client may choose to stash those incoming changes and not 
apply them, but the _server will not know this_. We need a method of allowing the client to tell the server
what features it is using so it knows which ones to apply. This actually becomes more interesting when you consider
server to server communication down the line as well, where you ideally wish to pass the feature state through
http and event-streaming layers if possible. 

The second use case is when doing any kind of testing, being able to indicate on each request in a Mocha / Jest / Cucumber
test that a feature is in a particular state lets you _parallelize_ your testing. If you have to set the entire
environment to a particular state, you can only run tests that expect those features in those states and you can very
quickly get sufficiently complex in your feature set that testing becomes a nightmare.

There is an important caveat to this. You can only send features that exist and _are not locked_. Locked features 
cannot be overridden. The reason for this is that in "catch and release" mode, you may wish to keep overriding features
available even in your production application. However, this could lead to hackers trying to turn on un-ready features
so forcing features to be locked and false is an important security safeguard.

### W3C Baggage Standard

In FeatureHub we are using the [W3C Baggage standard](https://w3c.github.io/baggage/) to pass the feature states. 
This concept is not new, it has been used in tracing stacks
for a long time, and forms a crucial part of the CNCF's OpenTelemetry project. At time of writing the header name and
format is close to agreement, such that several significant open source projects have decided to use it. 
We have decided to use it as well. The benefit will be in a cloud native environment, more tools will recognize and
understand this header and you will end up getting a lot of extra value for having used it (such as distributed
logging, tracing and diagnostics).

It essentially lets you send key/value pairs between servers using any transport mechanism and there is a guarantee
that servers will pass that header on down the wire.

A caveat is that you need to make sure that the header `Baggage` is added to your allowed CORS headers on your
server.

### Using in a browser

In a browser, we expect that you will want to make sure that the server knows what features you are using. This is 
an example using Axios:

```typescript 
import {
  w3cBaggageHeader
} from 'featurehub-repository';

globalAxios.interceptors.request.use(function (config: AxiosRequestConfig) {
  const baggage = w3cBaggageHeader({repo: fhConfig.repository(), header: config.headers.baggage});
  if (baggage) {
    config.headers.baggage = baggage;
  }
  return config;
}, function (error: any) {
  // Do something with request error
  return Promise.reject(error);
});
```     

This just ensures that with every outgoing request, we take any existing `Baggage` header you may have you tell the 
w3cBaggageHeader method what your repository
is and what the existing baggage header is. Note we give you the option to pass the repository, if you are using
the default one, you can leave the repo out. The above example could just be:

```typescript
  const baggage = w3cBaggageHeader({});
```  

### Using in a test API

Another option lets you override the values, not even requiring a repository on your side. This is useful inside
an API oriented test where you want to define a test that specifies a particular feature value or values. To support this,
the other way of calling the `w3cBaggageHeader` method is to pass a name of keys and their values (which may be strings or 
undefined - for non flag values, undefined for a flag value is false). So

```typescript
  const baggage = w3cBaggageHeader({values: new Map([['FEATURE_FLAG', 'true'], ['FEATURE_STRING', undefined]])});
```  

Will generate a baggage header that your server will understand as overriding those values. 

### User testing in a browser

Sometimes it can be useful to allow the user to be able to turn features on and off, something a manual tester
or someone testing some functionality in a UAT environment. Being able to do this for _just them_ is particularly
useful. FeatureHub allows you to do this by the concept of a User Repository, where the normal feature repository
for an environment is wrapped and any overridden values are stored in local storage, so when you move from page 
to page (if using page based or a Single-Page-App), as long as the repository you use is the User Repository, 
it will honour the values you have set and pass them using the Baggage headers.


### Using on the server (nodejs)

Both express and restify use the concept of middleware - where you can give it a function that will be passed the
request, response and a next function that needs to be called. We take advantage of this to extract the baggage header,
determine if it has a featurehub set of overrides in it and create a `FeatureHubRepository` that holds onto these
overrides but keeps the normal repository as a fallback. It _overlays_ the normal repository with the overridden
values (unless they are locked) and puts this overlay repository into the request.

To use it in either express or restify, you need to `use` it.

```typescript
import {featurehubMiddleware} from 'featurehub-repository';

api.use(featurehubMiddleware(fhConfig.repository()));
```

In your own middleware where you create a context, you need to make sure you pass the repository when
creating a new context. So do this:

```typescript
  req.ctx = await fhConfig.newContext(req.featureHub, null).build();
```

this means when you are processing your request it will attempt to use the baggage override first and then
fall back onto the rules in your featurehub repository. Your code still looks the same inside your nodejs code. 

```typescript
if (req.ctx.feature('FEATURE_TITLE_TO_UPPERCASE').flag) {
}
```
                      
However it is recommended that you wrap your own user authentication middleware and create a user context and
stash that in your request. `newContext` allows you  to pass in the repository so you will be able to put in:

```typescript
req.context = await fhConfig.newContext(req.repo, null).userKey('user.name@email').build();
```

If you log an event against the analytics provider, we will preserve your per-request overrides as well so they will
get logged correctly. e.g.

```typescript
req.context.logAnalyticsEvent('todo-add', new Map([['gaValue', '10']]));
``` 

Will use the overlay values by preference over the ones in the repository.

### Node -> Node

If you are making a call from one node server to another FeatureHub integrated server (in any supported language)
where Baggage is wired in, you can use the per request repository to pass to the `w3BaggageContext` method.

This means you can go:

```typescript
const baggage = w3cBaggageHeader({repo: req.repo, header: req.header('baggage')});
```

And if defined, add the baggage header to your outgoing request.

## FeatureHub Test API

When writing automated integration tests, it is often desirable to update your feature values, particularly flags. 
We provide a method to do this
using the `FeatureUpdater` class. Use of the API is based on the rights of your SDK-URL. Generally you should
only give write access to service accounts in test environments.

When specifying the key, the Edge service will get the latest value of the feature and compare your changes against
it, compare them to your permissions and act accordingly.  

You need to pass in an instance of a FeatureStateUpdate, which takes three values, all of which are optional but
must make sense:

- `lock` - this is a boolean. If true it will attempt to lock, false - attempts to unlock. No value will not make any change.
- `value` - this is any kind of value and is passed when you wish to _set_ a value. Do not pass it if you wish to unset the value.
For a flag this means setting it to false (if null), but for the others it will make it null (not passing it). 
- `updateValue` - set this to true if you wish to make the value field null. Otherwise, there is no way to distinguish
between not setting a value, and setting it to null.

Sample code might look like this:

```typescript 
const fu = new FeatureUpdater(fhConfig);

// this would work presuming the correct access rights
fu.updateKey('FEATURE_TITLE_TO_UPPERCASE', new FeatureStateUpdate({lock: false, value: true})).then((r) => console.log('result is', r));

// this would not as this key doesn't exist
fu.updateKey('meep', new FeatureStateUpdate({lock: false, value: true})).then((r) => console.log('result is', r));
```   

You can do this in the browser and in the sample React application in the examples folder, we have exposed this 
class to the `Window` object so you can run up the sample and play around with it. For example:

```javascript 
const x = new window.FeatureUpdater(fhConfig);

x.updateKey('meep', {lock: true}).then((r) => console.log('result was', r));
result was false
x.updateKey("FEATURE_TITLE_TO_UPPERCASE", {lock: false}).then((r) => console.log('result was', r));;

result was true
```

### Errors

If a 4xx error is returned, then it will stop. Otherwise it will keep polling even if there is no data on the assumption
it simply hasn't been granted access. The API does not leak information on valid vs invalid environments.
      

## Angular

This library uses semver, which is a commonjs library. You will need to follow the recommended Angular documentation
on how to suppress the warning. 
