# Javascript/Typescript Client SDK for FeatureHub

Welcome to the Javascript implementation for FeatureHub. It is the supported version, but it does not mean
you cannot write your own, the functionality is quite straightforward.

## Overview

This is the core library for Typescript and Javascript. 

It provides the core functionality of the 
repository which holds features and creates events. It does not connect to the backend server. That
functionality is provided using `eventsource` in another library, and means you can replace the `eventsource` mechanism
with some other library if you use it. 

`eventsource` is of course the only option when using the Web, so that is what we expect developers to use there,
but it can be replaced for backend or Mobile.

See [FeatureHub](https://featurehub.io) for more details. If you use the "Try now" demo environment you can try this
SDK out without installing anything.

## Installation Instructions

Run the following commands: 

`npm install featurehub-repository`

NOTE: However we strongly recommend using the eventsource-sdk artifact which will install this one. 

`npm install featurehub-eventsource-sdk`

## Mechanisms for use

Unlike the Java SDK, there are four ways to use this library due to the more _user_ based interaction that your 
application will operate under.

### 1. All the Features, All the Time

In this mode, you will make a connection to the FeatureHub Edge server, and any updates to any events will come
through to you, updating the feature values in the repository. You can choose to listen for these updates and update
some UI immediate, but every `if` statement will use the updated value, so listening is usually a better choice.

A typical strategy will be:

- set up your UI state so it is in "loading" mode (only if in the Web).
- set up a readyness listener so you know when the features have loaded and you have an initial state for them. When
using a UI, this would lead to a transition to the actual UI rendering, on the server it would make it start listening
to the server port.
- set up your per feature listeners so you can react to their changes in state
- connect to the Feature Hub server. As soon as it has connected and received a list of features it will call your
readyness listener. 
- each time a feature changes, it will call your listener and allow your application to react.

This kind of operation is perfect for servers. It can lead to instant change which could confuse users in a UI and
isn't something our team recommends.

```javascript

featureHubRepository.addReadynessListener((readyness) => {
  if (readyness === Readyness.Ready) {
    // some state change and you have your features and config
  }
});

this.eventSource = new FeatureHubEventSourceClient('<sdkUrl>');
this.eventSource.init();

featureHubRepository.getFeatureState('FEATURE_X').addListener((fs: FeatureStateHolder) => {
  // do something
});
```


NOTE: Recommended for: servers

### 2. All the Features, Only Once

In this mode, you receive the connection and then you disconnect, ignoring any further connections. You would
use this mode only if you want to force the client to have a consistent UI experience through the lifetime of their
visit to your client application.

```javascript
featureHubRepository.addReadynessListener((readyness) => {
  if (readyness === Readyness.Ready) {
    this.eventSource.close();
  }
});

this.eventSource = new FeatureHubEventSourceClient('<sdkUrl>');
this.eventSource.init();
```

NOTE: Recommended for: Web only, and only when not intending to react to feature changes until you ask for the feature state again.

### 3. All the Features, Controlled Release

This mode is termed "catch-and-release" (yes, inspired by the Matt Simons song). It is intended to allow you get
an initial set of features but decide when the feature updates are released into your application.

A typical strategy would be:

- set up your UI state so it is in "loading" mode (only if in the Web).
- set up a readyness listener so you know when the features have loaded and you have an initial state for them. When
using a UI, this would lead to a transition to the actual UI rendering, on the server it would make it start listening
to the server port.
- tell the feature hub repository to operate in catch and release mode. `featureHubRepository.catchAndRelease = true;`
- add a _post feature changed` callback hook. This hook will be called when a feature has changed.
- `[optional]` set up your per feature listeners so you can react to their changes in state. You could also not do this and 
encourage you users to reload the whole application window (e.g. `window.location.reload()`).
- connect to the Feature Hub server. As soon as it has connected and received a list of features it will call your
readyness listener.


```javascript
featureHubRepository.addReadynessListener((readyness) => {
  if (readyness === Readyness.Ready) {
    // some state change and you have your features and config
  }
});

this.eventSource = new FeatureHubEventSourceClient('<sdkUrl>');
this.eventSource.init();

featureHubRepository.catchAndReleaseMode = true; // don't allow feature updates to come through

featureHubRepository
  .addPostLoadNewFeatureStateAvailableListener((_) => // do something );

// this is optional
featureHubRepository.getFeatureState('FEATURE_X').addListener((fs: FeatureStateHolder) => {
  // do something. will trigger only once (first set of features). Won't trigger again until 
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

Google Analytics works the same way as the other SDKs. When you log an event on the repository,
it will capture the value of all of the feature flags and featutre values (in case they change),
and log that event against your analytics provider, once for each feature. This allows you to
slice and dice your events by state each of the features were in. We send them as a batch, so it
is only one request.

There are two different implementations, one for when you are in the browser and one for when you
are in nodejs. You don't need to worry about this, the code detects which one it is in and 
creates the correct instance. 

In either case, you need to register your implementation with the repository. The only one we
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

featureHubRepository.logAnalyticsEvent('event-name', other: data);
```

4) For a NODE server, you can set as an environment variable named `GA_CID`.

```typescript
featureHubRepository.addAnalyticCollector(collector);
```

As you can see from above (in option 3), to log an event, you simply tell the repository to
log an analytics event. It will take care of bundling everything up, passing it off to the
Google Analytics collector which will post it off.

## FeatureHub Test SDK

When doing tests, it is often desirable to update your feature values, particularly flags. We provide an method to do this
using the `FeatureUpdater` class. Use of the API is based on the rights of your SDK-URL. Generally you should
only give write access in test environments.

When specifying the key, the Edge service will get the latest value of the feature and compare your changes against
it, compare them to your permissions and act accordingly.  

You need to pass in an instance of a FeatureStateUpdate, which takes three values, all of which are optional but
must make sense:

- `lock` - this is a boolean. If true it will attempt to lock, false attempts to unlock. No value will not make any change.
- `value` - this is any kind of value and is passed when you wish to _set_ a value. Do not pass it if you wish to unset the value.
For a flag this means setting it to false (if null), but for the others it will make it null (not passing it). 
- `updateValue` - set this to true if you wish to make the value field null. Otherwise there is no way to distinguish between not setting a value,
and setting it to null.

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
