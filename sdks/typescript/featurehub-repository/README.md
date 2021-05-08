                                    
# NOTE: This library is deprecated

Please use the [Browser](https://www.npmjs.com/package/featurehub-javascript-client-sdk) SDK for React/Angular/Vue applications,
or [Node](https://www.npmjs.com/package/featurehub-javascript-node-sdk) SDK for nodejs applications.

The documentation is the same for
both and is in the [Browser](https://www.npmjs.com/package/featurehub-javascript-client-sdk) SDK, just the implementations of http/https
related functionality differ.

### Overview

This is the core library for Typescript and Javascript FeatureHub SDK implementation.

It provides the core functionality of the
repository which holds features and creates events.

It is an interdependency for FeatureHub Javascript/Typescript SDKs. 

Please refer to Readme for [eventsource SDK](https://www.npmjs.com/package/featurehub-eventsource-sdk) 
for more details on how to implement FeatureHub Javascript/Typescript SDK.   

### Changelog
                
### 2.0.5

- deprecation, replacement with new separation between client and nodejs to resolve issues with Angular/Vue.

### 2.0.4

- minor API mods

### 2.0.3 

- Expose analytic collector, readyness listener, readyness state, and value interceptor properties on the FeatureHubConfig
- Respect context when using feature listeners

### 2.0.1
- Client side evaluation support

### 1.2.1 
- Allow the release process for "catch & release" mode to turn the "catch" flag off once released. Ensure turning
"release" off also releases caught flags.
  
