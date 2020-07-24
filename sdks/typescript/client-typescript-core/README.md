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

See [FeatureHub](https://featurehub.io) for more details.

## Mechanisms for use

Unlike the Java SDK, there are four ways to use this library due to the more _user_ based interaction that your 
application will operate under.

### 1. All the Features, All the Time

In this mode, you will make a connection to the FeatureHub Edge server, and any updates to any events will come
through to you, updating the feature values in the repository. You can choose to listen for these updates and update
some UI immediate, but every `if` statement will use the updated value, so listening is usually a better choice.

A typical strategy will be:

. set up your UI state so it is in "loading" mode (only if in the Web).
. set up a readyness listener so you know when the features have loaded and you have an initial state for them. When
using a UI, this would lead to a transition to the actual UI rendering, on the server it would make it start listening
to the server port.
. set up your per feature listeners so you can react to their changes in state
. connect to the Feature Hub server. As soon as it has connected and received a list of features it will call your
readyness listener. 
. each time a feature changes, it will call your listener and allow your application to react.

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

. set up your UI state so it is in "loading" mode (only if in the Web).
. set up a readyness listener so you know when the features have loaded and you have an initial state for them. When
using a UI, this would lead to a transition to the actual UI rendering, on the server it would make it start listening
to the server port.
. tell the feature hub repository to operate in catch and release mode. `featureHubRepository.catchAndRelease = true;`
. add a _post feature changed` callback hook. This hook will be called when a feature has changed.
. `[optional]` set up your per feature listeners so you can react to their changes in state. You could also not do this and 
encourage you users to reload the whole application window (e.g. `window.location.reload()`).
. connect to the Feature Hub server. As soon as it has connected and received a list of features it will call your
readyness listener.
.


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

## Installation Instructions

Run the following commands: 

`npm install featurehub-repository`

We recommend however using the event-source artifact which will install this one. That is `featurehub-eventsource`.
