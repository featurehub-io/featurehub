# FeatureHubSDK

This library is the client SDK for the [https://featurehub.io](FeatureHub) open source project,
which brings you Cloud Native Feature Flag management, A/B experiments and remote configuration. 

This package includes the client SDK for event source, which gives you realtime updates, the
core repository, and the API required for allowing you to change the state of features using
your Service Account URL. 

The repository is isolated from the EventSource mechanism, so you can alternatively fill it
from a file or a database if you wish - and it is likely what you would do in integration tests.

## SDK features 
Details about what general features are available in SDKs from FeatureHub are [available here](https://docs.featurehub.io/#sdks).

## Using the EventSource SDK

There is a sample application included in the [solution as a console application](https://github.com/featurehub-io/featurehub/tree/master/sdks/client-csharp/ConsoleApp1).
You could implement it in the following way:

```c#
var fh = new FeatureHubRepository(); // create a new repository

// listen for changes to the feature FLUTTER_COLOUR and let me know what they are
fh.FeatureState("FLUTTER_COLOUR").FeatureUpdateHandler += (object sender, IFeatureStateHolder holder) =>
{
  Console.WriteLine($"Received type {holder.Key}: {holder.StringValue}");        
};

// you can also query the fh.FeatureState("FLUTTER_COLOUR") directly to see what its current state is
// and use it in IF statements or their equivalent

// tell me when the features have appeared and we are ready to start
fh.ReadynessHandler += (sender, readyness) =>
{
  Console.WriteLine($"Readyness is {readyness}");
};

// tell me when any new features or changes come in
fh.NewFeatureHandler += (sender, repository) =>
{
  Console.WriteLine($"New features");
};

// create an event service listener and give it the Service Account URL from the Admin-UI
var esl = new EventServiceListener();
esl.Init("http://192.168.86.49:8553/features/default/ce6b5f90-2a8a-4b29-b10f-7f1c98d878fe/VNftuX5LV6PoazPZsEEIBujM4OBqA1Iv9f9cBGho2LJylvxXMXKGxwD14xt2d7Ma3GHTsdsSO8DTvAYF", fh);

// it will now keep listening and updating the repository when changes come in. The server will kick it
// off periodically to ensure the connection does not become stale, but it will automatically reconnect
// and update itself and the repository again

fh.ClientContext().UserKey('ideally-unique-id')
  .Country(StrategyAttributeCountryName.Australia)
  .Device(StrategyAttributeDeviceName.Desktop)
  .Build();
``` 

### Rollout Strategies
FeatureHub at its core now supports _server side_ evaluation of complex rollout strategies, both custom ones
that are applied to individual feature values in a specific environment and shared ones across multiple environments
in an application. Exposing that level fo configurability via a UI is going to take some time to get right, 
so rather than block until it is done, Milestone 1.0's goal was to expose the percentage based rollout functionality
for you to start using straight away. 

Future Milestones will expose more of the functionality via the UI and will support client side evaluation of 
strategies as this scales better when you have 10000+ consumers. For more details on how
experiments work with Rollout Strategies, see the [core documentation](https://docs.featurehub.io).
 
#### Coding for Rollout strategies 
To provide this ability for the strategy engine to know how to apply the strategies, you need to provide it
information. There are five things we track specifically: user key, session key, country, device and platform and
over time will be able to provide more intelligence over, but you can attach anything you like, both individual
attributes and arrays of attributes. 

Remember, as of Milestone 1.0 we only support percentage based strategies,
so only UserKey is required to support this. We do however recommend you adding in as much information as you have
so you don't have to change it in the future.

Example: 
```c#
    featureHubRepository.ClientContext().UserKey('ideally-unique-id')
      .Country(StrategyAttributeCountryName.Australia)
      .Device(StrategyAttributeDeviceName.Desktop)
      .Build(); 
```

The `Build()` method will trigger the regeneration of a special header (`x-featurehub`). This in turn
will automatically retrigger a refresh of your events if you have already connected (unless you are using polling
and your polling interval is set to 0).

To add a generic key/value pair, use `Attr(key, value)`, to use an array of values there is 
`Attrs(key, Array<value>)`. In later Milestones you will be able to match against your own attributes, among other 
things. You can also `Clear()` to remove all strategies.

In all cases, you need to call `Build()` to re-trigger passing of the new attributes to the server for recalculation.
By default, the _user key_ is used for percentage based calculations, and without it, you cannot participate in
percentage based Rollout Strategies ("experiments"). However, a more advanced feature does let you specify other
attributes (e.g. _company_, or _store_) that would allow you to specify your experiment on. 

### IO.FeatureHub.SSE

This describes the API clients use for accessing features

This C# SDK is automatically generated by the [OpenAPI Generator](https://openapi-generator.tech) project:

- API version: 1.1.2
- SDK version: 1.0.0
- Build package: org.openapitools.codegen.languages.CSharpNetCoreClientCodegen

<a name="frameworks-supported"></a>
#### Frameworks supported
- .NET Core >=1.0
- .NET Framework >=4.6
- Mono/Xamarin >=vNext

<a name="dependencies"></a>
#### Dependencies

- [RestSharp](https://www.nuget.org/packages/RestSharp) - 106.10.1 or later
- [Json.NET](https://www.nuget.org/packages/Newtonsoft.Json/) - 12.0.1 or later
- [JsonSubTypes](https://www.nuget.org/packages/JsonSubTypes/) - 1.5.2 or later

The DLLs included in the package may not be the latest version. We recommend using [NuGet](https://docs.nuget.org/consume/installing-nuget) to obtain the latest version of the packages:
```
Install-Package RestSharp
Install-Package Newtonsoft.Json
Install-Package JsonSubTypes
```

NOTE: RestSharp versions greater than 105.1.0 have a bug which causes file uploads to fail. See [RestSharp#742](https://github.com/restsharp/RestSharp/issues/742)

<a name="installation"></a>
## Installation
Generate the DLL using your preferred tool (e.g. `dotnet build`)

Then include the DLL (under the `bin` folder) in the C# project, and use the namespaces:
```csharp
using IO.FeatureHub.SSE.Api;
using IO.FeatureHub.SSE.Client;
using IO.FeatureHub.SSE.Model;
```
<a name="getting-started"></a>
#### REST Endpoint for GET

You should only use this if you do not want realtime updates. In a normal C# application this would 
unusual.

```csharp
using System.Collections.Generic;
using System.Diagnostics;
using IO.FeatureHub.SSE.Api;
using IO.FeatureHub.SSE.Client;
using IO.FeatureHub.SSE.Model;

namespace Example
{
    public class Example
    {
        public static void Main()
        {

            Configuration config = new Configuration();
            config.BasePath = "http://localhost";
            var apiInstance = new FeatureServiceApi(config);
            var sdkUrl = new List<string>(); // List<string> | The SDK urls

            try
            {
                List<Environment> result = apiInstance.GetFeatureStates(sdkUrl);
                Debug.WriteLine(result);
            }
            catch (ApiException e)
            {
                Debug.Print("Exception when calling FeatureServiceApi.GetFeatureStates: " + e.Message );
                Debug.Print("Status Code: "+ e.ErrorCode);
                Debug.Print(e.StackTrace);
            }

        }
    }
}
```

<a name="documentation-for-api-endpoints"></a>

#### Documentation for API Endpoints

All URIs are relative to *http://localhost*

Class | Method | HTTP request | Description
------------ | ------------- | ------------- | -------------
*FeatureServiceApi* | [**GetFeatureStates**](docs/FeatureServiceApi.md#getfeaturestates) | **GET** /features/ | 
*FeatureServiceApi* | [**SetFeatureState**](docs/FeatureServiceApi.md#setfeaturestate) | **PUT** /features/{sdkUrl}/{featureKey} | 
