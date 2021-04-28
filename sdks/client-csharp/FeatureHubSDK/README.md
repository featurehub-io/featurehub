# FeatureHubSDK

This library is the client SDK for the [https://featurehub.io](FeatureHub) open source project,
which brings you Cloud Native Feature Flag management, A/B experiments and remote configuration.

It is specifically targeted at server-side applications, REST servers, web applications and so forth. As such
it focuses on providing near-realtime updates to features rather than surfacing polling functionality that
would be normally used in Browsers and Mobile. We recommend that you always request *Client-Evaluated API Keys* when
requesting your API key via the `API Keys` section in FeatureHub's Management Repository console.

## SDK features 
Details about what general features are available in SDKs from FeatureHub are [available here](https://docs.featurehub.io/#sdks).

## Changelog

2.0.0 - client side evaluation support
1.1.0 - analytics support
1.0.0 - initial functionality with near-realtime event updates, full feature repository, server side rollout strategies.

## Using the EventSource SDK

There is a sample application included in the [solution as a console application](https://github.com/featurehub-io/featurehub/tree/master/sdks/client-csharp/ConsoleApp1).
You could implement it in the following way:

```c#
// start by creating a IFeatureHubConfig object and telling it where your host server is and your
// client-evaluated API Key 
var config = new FeatureHubConfig("http://localhost:8903",
  "default/82afd7ae-e7de-4567-817b-dd684315adf7/SJXBRyGCe1dZ*PNYGy7iOFeKE");
  
config.Init(); // tell it to asynchronously connect and start listening

You can optionally set an analytics provider on the config (see below).

// this will set up a ClientContext - which is a bucket of information about this user
// and then attempt to connect to the repository and retrieve your data. It will return once it
// has received your data.  
var context = await config.NewContext().UserKey("ideally-unique-id")
        .Country(StrategyAttributeCountryName.Australia)
        .Device(StrategyAttributeDeviceName.Desktop)
        .Build();


// listen for changes to the feature FLUTTER_COLOUR and let me know what they are
context["FLUTTER_COLOUR"].FeatureUpdateHandler += (object sender, IFeatureStateHolder holder) =>
{
  Console.WriteLine($"Received type {holder.Key}: {context[holder.Key].StringValue}");        
};

There are many more convenience methods on the `IClientContext`, including:

    - IsEnabled - is this feature enabled?
    - IsSet - does this feature have a value?
    - LogAnalyticEvent - logs an analytics event if you have set up an analytics provider.

``` 

### ASP.NET 

Wiring them into a ASP.NET application should also be fairly simple and it surfaces as an injectable service. Some example
code from our C# TodoServer in the `featurehub-examples` folder.

```c#
  private void AddFeatureHubConfiguration(IServiceCollection services)
  {
      IFeatureHubConfig config = new EdgeFeatureHubConfig(Configuration["FeatureHub:Host"], Configuration["FeatureHub:ApiKey"]);

      services.Add(ServiceDescriptor.Singleton(typeof(IFeatureHubConfig), config));

      config.Init();
  }
```

It is then available to be injected into your Controllers or Filters. 

### Rollout Strategies
Starting from version 1.1.0 FeatureHub supports _server side_ evaluation of complex rollout strategies
that are applied to individual feature values in a specific environment. This includes support of preset rules, e.g. per **_user key_**, **_country_**, **_device type_**, **_platform type_** as well as **_percentage splits_** rules and custom rules that you can create according to your application needs.

For more details on rollout strategies, targeting rules and feature experiments see the [core documentation](https://docs.featurehub.io/#_rollout_strategies_and_targeting_rules).

We are actively working on supporting client side evaluation of
strategies in the future releases as this scales better when you have 10000+ consumers.

#### Coding for Rollout strategies 
There are several preset strategies rules we track specifically: `user key`, `country`, `device` and `platform`. However, if those do not satisfy your requirements you also have an ability to attach a custom rule. Custom rules can be created as following types: `string`, `number`, `boolean`, `date`, `date-time`, `semantic-version`, `ip-address`

FeatureHub SDK will match your users according to those rules, so you need to provide attributes to match on in the SDK:

**Sending preset attributes:**

Provide the following attribute to support `userKey` rule:

```c#
    await context.UserKey("ideally-unique-id").Build(); 
```

to support `country` rule:
```c#
    await context.Country(StrategyAttributeCountryName.Australia).Build(); 
```

to support `device` rule:
```c#
    await context.Device(StrategyAttributeDeviceName.Desktop).Build(); 
```

to support `platform` rule:
```c#
    await context.Platform(StrategyAttributePlatformName.Android).Build(); 
```

to support `semantic-version` rule:
```c#
    await context.Version("1.2.0").Build(); 
```
or if you are using multiple rules, you can combine attributes as follows:

```c#
    await context.UserKey("ideally-unique-id")
      .Country(StrategyAttributeCountryName.NewZealand)
      .Device(StrategyAttributeDeviceName.Browser)
      .Platform(StrategyAttributePlatformName.Android)
      .Version("1.2.0")
      .Build(); 
```

For *Server Evaluated keys*, which we do _not_ recommend, the  `Build()` method will trigger the regeneration of a 
special header (`x-featurehub`). This in turn will automatically retrigger a refresh of your events if 
you have already connected.

For *Client Evaluated API keys*, which we do recommend for server side code, the `Build()` method does nothing, as all
the necessary decision making information is already available.

**Sending custom attributes:**

To add a custom key/value pair, use `Attr(key, value)`

```C#
    await context.Attr("first-language", "russian").Build();
```

Or with array of values (only applicable to custom rules):

```C#
   await context.Attrs("languages", new List<String> {"Russian", "English", "German"}).Build();
```

You can also use `featureHubRepository.ClientContext.Clear()` to empty your context.

In all cases, you need to call `Build()` to re-trigger passing of the new attributes to the server for recalculation.

### Analytics Support for C#

This allows you to connect your application and see your features performing in Google Analytics. The
adapter is generic but we provide specific support here for Google's Analytics platform at the moment.

When you log an event on the repository,
it will capture the value of all of the feature flags and featutre values (in case they change),
and log that event against your Google Analytics, once for each feature. This allows you to
slice and dice your events by state each of the features were in. We send them as a batch, so it
is only one request.

There is a plan to support other Analytics tools in the future. The only one we
currently support is Google Analytics, so you need:

- a Google analytics key - usually in the form `UA-123456`. You must provide this up front.
- a CID - a customer id this is associate with this. You can provide this up front or you can
provide it with each call, or you can set it later. 

1) You can set it in the constructor:

```c#
fhConfig.AddAnalyticCollector(new GoogleAnalyticsCollector("UA-example", "1234-5678-abcd-abcd",
new GoogleAnalyticsHttpClient()));
```

2) If you hold onto the Collector, you can set the CID on the collector later.

```c#
_collector.Cid = "some-value"; // you can set it here
```

3) When you log an event, you can pass it in the map:

```c#
var _data = new Dictionary<string, string>();
_data[GoogleConstants.Cid] = "some-cid";

context.LogAnalyticsEvent("event-name", _data);
```

Read more on how to interpret events in Google Analytics [here](https://docs.featurehub.io/analytics.html) 

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

All URIs are relative to *http://localhost*

Class | Method | HTTP request | Description
------------ | ------------- | ------------- | -------------
*FeatureServiceApi* | [**GetFeatureStates**](docs/FeatureServiceApi.md#getfeaturestates) | **GET** /features/ | 
*FeatureServiceApi* | [**SetFeatureState**](docs/FeatureServiceApi.md#setfeaturestate) | **PUT** /features/{sdkUrl}/{featureKey} | 
