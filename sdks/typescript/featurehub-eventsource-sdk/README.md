# Javascript/Typescript Client SDK for FeatureHub

# NOTE: This library is deprecated

Please use the [Browser](https://www.npmjs.com/package/@featurehub/javascript-client-sdk) SDK for React/Angular/Vue applications,
or [node](https://www.npmjs.com/package/@featurehub/javascript-node-sdk) SDK for nodejs applications. 

The documentation is the same for
both and is in the [Browser](https://www.npmjs.com/package/@featurehub/javascript-client-sdk) SDK, just the implementations of http/https
related functionality differ. 

## Overview
Welcome to the Javascript/Typescript SDK for [FeatureHub.io](https://featurehub.io) - Open source Feature flags management, A/B testing and remote configuration platform.

Below explains how you can use the FeatureHub SDK in Javascript or Typescript for applications like Node.js
backend server, Web front-end (e.g. React) or Mobile apps (React Native, Ionic, etc.). 

To control the feature flags from the FeatureHub Admin console, either use our [demo](https://demo.featurehub.io) version for evaluation or install the app using our guide [here](http://docs.featurehub.io/#_installation)

There are 2 ways to request for feature updates via this SDK:

- **SSE (Server Sent Events) realtime updates mechanism** 
  
  In this mode, you will make a connection to the FeatureHub Edge server using the EventSource library which this SDK is based on, and any updates to any features will come through to you in near realtime, automatically updating the feature values in the repository.
Note, there is a known issues in the browsers with Kaspersky antivirus potentially blocking SSE events. [GitHub issue](https://github.com/featurehub-io/featurehub/issues/296) 

- **FeatureHub polling client (GET request updates)** 
  
  In this mode, you make a GET request, which you can choose to either do once, when specific things happen in your application,
  (such as navigation change) or on a regular basis (say every 5 minutes) and the changes will be passed into the FeatureHub repository for processing.

