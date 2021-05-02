## Overview

This is the core library for Typescript and Javascript FeatureHub SDK implementation.

It provides the core functionality of the
repository which holds features and creates events.

It is an interdependency for FeatureHub Javascript/Typescript SDKs. 

Please refer to Readme for [eventsource SDK](https://www.npmjs.com/package/featurehub-eventsource-sdk) 
for more details on how to implement FeatureHub Javascript/Typescript SDK.   

## Changelog
            
### 2.0.3 

- Expose analytic collector, readyness listener, readyness state, and value interceptor properties on the FeatureHubConfig
- Respect context when using feature listeners

### 2.0.1
- Client side evaluation support

### 1.2.1 
- Allow the release process for "catch & release" mode to turn the "catch" flag off once released. Ensure turning
"release" off also releases caught flags.
  
