# Javascript/Typescript Client SDK for FeatureHub

Welcome to the Javascript/Typescript SDK implementation for FeatureHub. It is the supported version, but it does not mean
you cannot write your own, please follow [Contributing](https://github.com/featurehub-io/featurehub/blob/master/.github/CONTRIBUTING.adoc)
guidelines.

## Overview

Below explains how you can use the FeatureHub SDK in Javascript or Typescript for applications like Node.js
backend server or Web front-end (e.g. React). 

This SDK is based on EventSource (or Server Sent Events) interface and provides real-time connection to the FeatureHub Edge server. 

When you install the SDK it will also install the dependency [featurehub-repository](https://www.npmjs.com/package/featurehub-repository)
which is a core library that holds features and creates events. 

It is designed this way, so we can separate core functionality and add different implementations in the future, similar to EventSource.   

See [FeatureHub.io](https://featurehub.io) for more details. There you can find a link to try our demo version of FeatureHub Web Console. 
If you like you can experiment with this SDK in your code and play with the feature flags and control them from the FeatureHub Web Console.
You can use one of the service accounts SDK Url to substitute ```<sdk_url>``` in below examples.

## Installation Instructions

Run the following command: 

`npm install featurehub-eventsource-sdk`

## Service account SDK Url

Find and copy your SDK Url from the FeatureHub Web Console (you can find it also in the Demo version) - 
you will use this in your code to configure feature updates for your environments. 
It should look similar to this: ```https://vrtfs.demo.featurehub.io/features/default/71ed3c04-122b-4312-9ea8-06b2b8d6ceac/fsTmCrcZZoGyl56kPHxfKAkbHrJ7xZMKO3dlBiab5IqUXjgKvqpjxYdI8zdXiJqYCpv92Jrki0jY5taE```

![Service account](images/service-account-copy.png) 

## Mechanisms for use

Unlike the Java SDK, there are four ways to use this library due to the more _user_ based interaction that your 
application will operate under.

### 1. Real-time updates, recommended for servers

In this mode, you will make a connection to the FeatureHub Edge server, and any updates to any events will come
through to you, updating the feature values in the repository. We recommend this technique for servers, 
but for Web it may not be the best choice
as you may not want your users to see updates in the browser immediately but rather serve it on-demand. 

A typical strategy will be:

- set up a readyness listener so you know when the features have loaded and you have an initial state for them.
- set up your per feature listeners so you can react to their changes in state
- connect to the FeatureHub server. As soon as it has connected and received a list of features it will call your
readyness listener. 
- each time a feature changes, it will call your listener and allow your application to react in real-time.



```javascript

featureHubRepository.addReadynessListener((readyness) => {
  if (readyness === Readyness.Ready) {
       console.log("Features are available, starting server...");
   
       api.listen(port, function () {
         console.log('server is listening on port', port);
       })
  }
});

//paste your Service Account SDK URL here
this.eventSource = new FeatureHubEventSourceClient('<sdkUrl>'); 
this.eventSource.init();

featureHubRepository.getFeatureState('FEATURE_X').addListener((fs: FeatureStateHolder) => {
  // do something
});
```


### 2. On-demand updates, recommended for Web 

In this mode, you receive the connection and then you disconnect, ignoring any further connections. You would
use this mode only if you want to force the client to have a consistent UI experience through the lifetime of their
visit to your client application. Recommended for Web, when not intending to react to feature changes until 
you ask for the feature state again.

```javascript
featureHubRepository.addReadynessListener((readyness) => {
  if (readyness === Readyness.Ready) {
    this.eventSource.close();
  }
});

//paste your Service Account SDK URL here
this.eventSource = new FeatureHubEventSourceClient('<sdkUrl>'); 
this.eventSource.init();
```

### 3. Controlled updates, recommended for Web

This mode is termed "catch-and-release" (inspired by the Matt Simons song). It is intended to allow you get
an initial set of features but decide when the feature updates are released into your application.

A typical strategy would be:

- set up your UI state so it is in "loading" mode (only if in the Web).
- set up a readyness listener so you know when the features have loaded and you have an initial state for them. When
using a UI, this would lead to a transition to the actual UI rendering, on the server it would make it start listening
to the server port.
- tell the feature hub repository to operate in catch and release mode. `featureHubRepository.catchAndRelease = true;`
- add a _post feature changed` callback hook. This hook will be called when a feature has changed.
- `[optional]` set up your per feature listeners so you can react to their changes in state. You could also not 
do this and encourage you users to reload the whole application window (e.g. `window.location.reload()`).
- connect to the Feature Hub server. As soon as it has connected and received a list of features it will call your
readyness listener.


```javascript
featureHubRepository.addReadynessListener((readyness) => {
  if (readyness === Readyness.Ready) {
    // some state change and you have your features and config
  }
});

//paste your Service Account SDK URL here
this.eventSource = new FeatureHubEventSourceClient('<sdkUrl>'); 
this.eventSource.init();

// don't allow feature updates to come through
featureHubRepository.catchAndReleaseMode = true; 

featureHubRepository
  .addPostLoadNewFeatureStateAvailableListener((_) => {}); // do something 

// this is optional
featureHubRepository.getFeatureState('FEATURE_X').addListener((fs: FeatureStateHolder) => {
  // Do something. will trigger only once (first set of features). Won't trigger again until 
  // featureHubRepository.release() is called.
});
```

If you choose to not have listeners, when you call: 

```javascript
featureHubRepository.release();
```

then you should follow it with code to update your state with the appropriate changes in features. You
won't know which ones changed, but this can be a more efficient state update than using the listeners above.

### Failure

If for some reason the connection to the FeatureHub server fails - either initially or for some reason during
the process, you will get a readyness state callback to indicate that it has now failed.

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
it will capture the value of all of the feature flags and featutre values (in case they change),
and log that event against your Google Analytics, once for each feature. This allows you to
slice and dice your events by state each of the features were in. We send them as a batch, so it
is only one request.

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

featureHubRepository.logAnalyticsEvent('event-name', other: data)
```

4) For a NODE server, you can set as an environment variable named `GA_CID`.

```typescript
featureHubRepository.addAnalyticCollector(collector);
```

As you can see from above (in option 3), to log an event, you simply tell the repository to
log an analytics event. It will take care of bundling everything up, passing it off to the
Google Analytics collector which will post it off.

Read more on how to interpret events in Google Analytics [here](https://docs.featurehub.io/analytics.html) 

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
const fu = new FeatureUpdater('https://vrtfs.demo.featurehub.io/features/default/71ed3c04-122b-4312-9ea8-06b2b8d6ceac/fsTmCrcZZoGyl56kPHxfKAkbHrJ7xZMKO3dlBiab5IqUXjgKvqpjxYdI8zdXiJqYCpv92Jrki0jY5taE');

// this would work presuming the correct access rights
fu.updateKey('FEATURE_TITLE_TO_UPPERCASE', new FeatureStateUpdate({lock: false, value: true})).then((r) => console.log('result is', r));

// this would not as this key doesn't exist
fu.updateKey('meep', new FeatureStateUpdate({lock: false, value: true})).then((r) => console.log('result is', r));
```   

You can do this in the browser and in the sample React application in the examples folder, we have exposed this 
class to the `Window` object so you can run up the sample and play around with it. For example:

```javascript 
x = new window.FeatureUpdater('http://localhost:8553/features/default/ce6b5f90-2a8a-4b29-b10f-7f1c98d878fe/VNftuX5LV6PoazPZsEEIBujM4OBqA1Iv9f9cBGho2LJylvxXMXKGxwD14xt2d7Ma3GHTsdsSO8DTvAYF');

x.updateKey('meep', {lock: true}).then((r) => console.log('result was', r));
result was false
x.updateKey("FEATURE_TITLE_TO_UPPERCASE", {lock: false}).then((r) => console.log('result was', r));;

result was true
```

## FeatureHub Polling (GET) API

The Typescript API has a client that allows you to get the features from one or more environments using a GET
request rather than via the event source mechanism. It is intended that this would be a better API for use in Mobile,
as Server sent events keep the Mobile's radio on. 

Each request will bring all of the features down, which may or may not trigger events in the repository (depending on
changes). As each feature has a version number, this prevents accidental triggering of "update" events for features
in this case.

The API is a single class that works in both NodeJS and the Browser. It takes the repository, the host (such as
http://localhost:8553), an array of SDK URLs (as taken from the UI), and a polling frequency (0 if it should be single-fire).

```typescript
const fp = new FeatureHubPollingClient(
  featureHubRepository, // repository 
  'http://localhost:8553',  // host
  10000,    // every 10 seconds, 0 if only fire once
  ['default/ce6b5f90-2a8a-4b29-b10f-7f1c98d878fe/VNftuX5LV6PoazPZsEEIBujM4OBqA1Iv9f9cBGho2LJylvxXMXKGxwD14xt2d7Ma3GHTsdsSO8DTvAYF'] 
);

fp.start(); 
``` 

The above will cause the API to fire when `start()` is called, and then once every 10 seconds. The API will prevent
you from double polling on the same instance, so you can safely make it a global variable and trigger it in logical
places around your application (such as navigation change in a mobile app).

This API is currently simple - it will just get all of the features again. Future versions will allow passing of the
versions of features and limit data being returned.

You can stop polling using:

```typescript 
fp.stop();
```

and it will stop once completed.

### Errors

If a 400 error is returned, then it will stop. Otherwise it will keep polling even if there is no data on the assumption
it simply hasn't been granted access. The API does not leak information on valid vs invalid environments.
