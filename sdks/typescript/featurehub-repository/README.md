## Overview

This is the core library for Typescript and Javascript FeatureHub SDK implementation.

It provides the core functionality of the
repository which holds features and creates events.

It is an interdependency for FeatureHub Javascript/Typescript SDKs. 

Please refer to Readme for [eventsource SDK](https://www.npmjs.com/package/featurehub-eventsource-sdk) 
for more details on how to implement FeatureHub Javascript/Typescript SDK.   

## Changelog
            
- 2.0.3 - expose analytic collector, readyness listener, readyness state, and value interceptor capability on the FeatureHubConfig so no requirement to get the repository.
  Also added functionality to ensure that when adding a feature listener to the context that the feature that appears in the listener is the one with the context
  associated (i.e. as if you went context.getNumber('fred')). Make the creation of a new context more consistent so you can not pass one of the two parameters and it
  would still do the right thing. added new tests to cover this functionality.
- 2.0.0 - client side evaluation support
- 1.2.1 - allow the release process for catch & release to turn the catch flag off once released. Ensure turning
release off also releases caught flags.
  
