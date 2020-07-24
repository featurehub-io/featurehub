## Description
Provides an EventSource based SSE client that listens to important events
coming from the SSEEdge from FeatureHub and hands them off to the ClientFeatureRepository
in `featurehub-repository`. 

See [FeatureHub](https://featurehub.io) for more details.

Use this only if you are using `EventSource` - in a browser you will be, but in nodejs
you have other options, but there is an eventsource npm that mimics the browser so you
can use it in both places.

`EventSource` will auto-reconnect when the SSEEdge kicks it off and will refresh the list of
features at that time. Otherwise all updated features will automatically be pushed from the server to the client
and updated in realtime. To control this process there are various models the repository can operate in, please
read the `featurehub-repository` docs for how to use them. 

### use

```
// set up with url with sdk api
const source = new FeatureHubEventSource('http://.....'); 

// to start
source.init();

// when finished
source.close();
```

### prerequisites 
Node @12 or higher

Follow readme instructions to build  `client-typescript-core` module - only until this is
released into npmjs, in which case it won't require it (as it will be in npmjs). 

### installation instructions

Please read the instructions for use in the [`featurehub-repository`](https://www.npmjs.com/package/featurehub-repository) artifact.

----
npm install featurehub-eventsource-sdk 
----


