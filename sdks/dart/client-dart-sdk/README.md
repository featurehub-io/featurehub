# Dart Client SDK for FeatureHub


Welcome to the Dart SDK implementation for FeatureHub. It is the supported version, but it does not mean
you cannot write your own, the functionality is quite straightforward. For more information about the platform 
please visit our official web page https://www.featurehub.io/

![Code Coverage](coverage_badge.svg)

## Overview

This is the core library for Dart, and can be used for Flutter in all forms (Mobile, Desktop, Web).

It provides the core functionality of the
repository which holds features and creates events. It depends on our own fork of the EventSourcing library
for Dart (until the main library merges the changes in or we release our own). 

This library holds a Feature Repository that you will include in your code and use to determine the state of
specific features. It attempts to follow Dart based idioms.  

## Approaches to updating the repository

The library includes two different ways you can get the feature states from the FeatureHub
Edge Server into your feature repository:

- *GET* - this does a simple HTTP GET request and will retrieve the current state of the features for one
or more environments. You would use this for Mobile (e.g. Flutter) and periodically update your repository
by making this API call. 
- *EventSource* - this provides real time updates for features by keeping a link open to the FeatureHub
Edge Server, as you can imagine this is an expensive operation to do on a battery and we do not recommend it
for Mobile except for short periods.  
 
Because these two update methods are interchangeable, you can include them in the same application if you want. You
could swap between *GET* when your app swaps to the background and *EventSource* when your app swaps to the foreground 
if immediate updates are important.

This is discussed in more detail below under the heading _Mobile API_. 
 
## Approaches to using the Repository

Like the Typescript SDK, there are four ways to use this library due to the more _user_ based interaction that your
application will operate under. Unlike the Typescript library, the Dart library uses _streams_.

### 1. All the Features, All the Time

In this mode, you will make a connection to the FeatureHub Edge server, and any updates to any events will come
through to you, updating the feature values in the repository. You can choose to listen for these updates and update
some UI immediate, but every `if` statement will use the updated value, so listening to the stream is usually a better choice.

A typical strategy will be:

- set up your UI state so it is in "loading" mode.
- listen to the readyness stream so you know when the features have loaded and you have an initial state for them. When
using a UI, this would lead to a transition to the actual UI rendering, on the server it would make it start listening
to the server port.
- set up your per feature stream listeners so you can react to their changes in state
- connect to the Feature Hub server. As soon as it has connected and received a list of features .
- each time a feature changes, it will call your listener and allow your application to react.

Whether this instant reaction is ideal for your application depends. For mobile and servers, the answer is usually
yes, for Web the answer is often no as people don't expect that.

Note below the hostUrl is the address of where FeatureHub is running, e.g. `http://locahost:8085` for the Party
Server. The APIKey is the key from the Service Keys page. Please always ensure you choose a Server Evaluated Key
(*not* a Client Evaluated key) otherwise your rollout strategies will not work.

````dart
final _repository = ClientFeatureRepository();

_repository.readynessStream.listen((readyness) {
  if (readyness == Readyness.Ready) {
    print("repo is ready to use");
  }
});

// this will cause the event source listener to immediately start. It has a close()
// method to allow for shutdown. this is the EventSource listener and will give you immediate updates 
final _eventSource = EventSourceRepositoryListener(hostUrl, apiKey, _repository);

const featureXUnsubscribe = featureHubRepository.getFeatureState('FEATURE_X')
   .featureUpdateStream.listen((_fs) => do_something());
````

> Recommended for: servers

### 2. All the Features, Only Once

In this mode, you receive the connection and then you disconnect, ignoring any further connections. You would
use this mode only if you want to force the client to have a consistent UI experience through the lifetime of their
visit to your client application.

> Recommended for: Web only, and only when not intending to react to feature changes until you ask for the feature state again.

### 3. All the Features, Controlled Release

This mode is termed "catch-and-release" (yes, inspired by the Matt Simons song). It is intended to allow you get
an initial set of features but decide when the feature updates are released into your application.

A typical strategy would be:

. set up your UI state so it is in "loading" mode (only if in the Web).
. set up a readyness listener so you know when the features have loaded and you have an initial state for them. When
using a UI, this would lead to a transition to the actual UI rendering, on the server it would make it start listening
to the server port.
. tell the feature hub repository to operate in catch and release mode. `featureHubRepository.catchAndRelease = true;`
. listen to the new features stream. This stream will be triggered when a feature has changed.
. `[optional]` set up your per feature listeners so you can react to their changes in state. You could also not do this and
encourage you users to reload the whole application window (e.g. `window.location.reload()`).
. connect to the Feature Hub server. As soon as it has connected and received a list of features it will call your
readyness listener.
.


If you choose to not have listeners, when you call:

```dart
_repository.release();
```


then you should follow it with code to update your state with the appropriate changes in features. You
won't know which ones changed, but this can be a more efficient state update than using the listeners above.

## Failure

If for some reason the connection to the FeatureHub server fails - either initially or for some reason during
the process, you will get a readyness event to indicate that it has now failed.

```dart
enum Readyness {
  NotReady = 'NotReady',
  Ready = 'Ready',
  Failed = 'Failed'
}

```

## Mobile API

If you intend to use FeatureHub with Flutter for Mobile, we have an SDK that is based on REST API. 
The reason for this is that Mobile devices connection doesn't always stay on, so doing a GET request to get your 
features would be the right choice here. 

If you are running a Dart web server or Dart command line app or any other Flutter based application - you should 
use the Event Source above. For Flutter for Mobile, generally you should use this API.

It is simple to use, you need to specify the host base url and the environment(s) that you wish to pull into your 
application. Do not have features with the same keys otherwise you will encounter issues with versioning.

Construction is fairly simple, you need a repository (as discussed above) and there is an example 
in `example/dart_cli/get_main.dart`.

```dart
final es = FeatureHubSimpleApi(sdkHost, [sdkUrl], repo);
es.request();
```

`sdkHost` - this is the base address of the Edge host
`sdkUrl` - this is the part from the admin UI that identifies your particular environment and API key.

`request` is an async method and it will return its content directly to the repository. 
A failed call is caught and a Failure status is sent to the repository, which will have an updated error status
(such as FAILED, in the Readyness listener). 

If the request has no data or an SDK Url that doesn't exist, that is not considered an error because they may just
not yet be available and you don't want your application to fail.

## Rollout Strategies

Starting from version 1.1.0 FeatureHub supports _server side_ evaluation of complex rollout strategies
that are applied to individual feature values in a specific environment. This includes support of preset rules, e.g. per **_user key_**, **_country_**, **_device type_**, **_platform type_** as well as **_percentage splits_** rules and custom rules that you can create according to your application needs.

For more details on rollout strategies, targeting rules and feature experiments see the [core documentation](https://docs.featurehub.io/#_rollout_strategies_and_targeting_rules).

We are not planning on implementing Client Side evaluation for Dart without direct request as it is mostly used client side in Flutter
and console apps.

#### Coding for Rollout strategies 
There are several preset strategies rules we track specifically: `user key`, `country`, `device` and `platform`. However, if those do not satisfy your requirements you also have an ability to attach a custom rule. Custom rules can be created as following types: `string`, `number`, `boolean`, `date`, `date-time`, `semantic-version`, `ip-address`

FeatureHub SDK will match your users according to those rules, so you need to provide attributes to match on in the SDK:

**Sending preset attributes:**

Provide the following attribute to support `userKey` rule:

```dart
    _repository.clientContext.userKey('ideally-unique-id').build(); 
```

to support `country` rule:

```dart
    _repository.clientContext.country(StrategyAttributeCountryName.NewZealand).build(); 
```

to support `device` rule:

```dart
    _repository.clientContext.device(StrategyAttributeDeviceName.Browser).build(); 
```

to support `platform` rule:

```dart
    _repository.clientContext.platform(StrategyAttributePlatformName.Android).build(); 
```

to support `semantic-version` rule:

```dart
    _repository.clientContext.version('1.2.0').build(); 
```

or if you are using multiple rules, you can combine attributes as follows:

```dart
    _repository.clientContext.userKey('ideally-unique-id')
      .country(StrategyAttributeCountryName.NewZealand)
      .device(StrategyAttributeDeviceName.Browser)
      .platform(StrategyAttributePlatformName.Android)
      .version('1.2.0')
      .build(); 
```

The `build()` method will trigger the regeneration of a special header (`x-featurehub`). This in turn
will automatically retrigger a refresh of your events if you have already connected (unless you are using polling
client).

**Sending custom attributes:**

To add a custom key/value pair, use `attr(key, value)`

```dart
    _repository.clientContext.attr('first-language', 'russian').build();
```

Or with array of values (only applicable to custom rules):

```dart
   _repository.clientContext.attrs('languages', ['russian', 'english', 'german']).build();
```

You can also use `featureHubRepository.clientContext.clear()` to empty your context.

In all cases, you need to call `build()` to re-trigger passing of the new attributes to the server for recalculation.


**Coding for percentage splits:**
For percentage rollout you are only required to provide the `userKey` or `sessionKey`.

```dart
    featureHubRepository.clientContext.userKey('ideally-unique-id').build();
```
or

```dart
    featureHubRepository.clientContext.sessionKey('session-id').build();
```

For more details on percentage splits and feature experiments see [Percentage Split Rule](https://docs.featurehub.io/#_percentage_split_rule).

    
## FeatureHub Test API

The Featurehub test api is available in this SDK, but it is not broken out into a separate class. The purpose of the
test API is to allow your SDK-URL to update features primarily when writing automated integration tests. 

We provide a method to do this
using the `FeatureServiceApi.setFeatureState` method. Use of the API is based on the rights of your SDK Url. 
Generally you shouldonly give write access to service accounts in test environments.

When specifying the key, the Edge service will get the latest value of the feature and compare your changes against
it, compare them to your permissions and act accordingly.  

You need to pass in an instance of a FeatureStateUpdate, which takes three values, all of which are optional but
must make sense:

- `lock` - this is a boolean. If true it will attempt to lock, false - attempts to unlock. No value will not make any change.
- `value` - this is `dynamic` kind of value and is passed when you wish to _set_ a value. Do not pass it if you wish to unset the value.
For a flag this means setting it to false (if null), but for the others it will make it null (not passing it). 
- `updateValue` - set this to true if you wish to make the value field null. Otherwise, there is no way to distinguish
between not setting a value, and setting it to null.

We don't provide a wrapper class for this because most of the code comes directly from the `featurehub_client_api` and
you need to include that and its dependencies in your project to use this capability.

Sample code might look like this:

```dart
final _api = FeatureServiceApi(new ApiClient(basePath: sdkHost));
_api.setFeatureState(sdkUrl, key, FeatureStateUpdate()..lock = false ..value = 'TEST'); 
```   

Here the sdkHost and sdkUrl have the same meaning as above.

