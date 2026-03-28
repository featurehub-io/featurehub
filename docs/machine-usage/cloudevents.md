# cloudevents

in FeatureHub - the publishing and consumption of cloud events is disconnected from 
how those events travel in channels between applications. All an application needs to do
is choose a registry (e.g. `common`) and listen for a Cloud Event Type and messages that turn
up in the system will be handed to the handler. Publishing events is the same, an application
simply needs to publish the event and the underlying code figures out on what channels it needs
to be publisher so that it turns up in the right application.

This is done by  a collection of projects in the `backend` folder that are actually responsible
for around sending CloudEvents throughout the system and knowing which applications need them. 

## structure of projects

The central project is `eventing-cloudevents` and then there are event platform specific sections. The event platforms which there are definitions are as below, with the project associated with it.

- NATS - `eventing-nats` - NATS is represented by https://nats.io/
- Google PubSub - `eventing-google-pubsub` 
- Amazon Kinesis - `eventing-kinesis`. 

Google PubSub is different from NATS and Kinesis because in NATs and Kinesis, the channel and its subscribers are a single stream and their naming is part of the subscription process to a stream. In Google Pubsub, subscribers are a distinct entity - so that subscribers need to be dynamically created when a process starts and destroyed when the process is complete. For example, in the `feature-only-channel`, Google PubSub has two different subscribers because it needs to receive the message in two different applications (edge and dacha2). Each instance of these applications will create their own new subscribers (and destroy them on completion). 

All of the channels that each one of them rely on are registered in the `src/main/resources/META-INF/cloud-events/cloud-events.yaml`.

This has the following properties

- defaults applicable to all platforms stored first,
- a section which defines the extra information required by each event platform.

The default and an event platform section are merged and processed.

When the application starts,
all platforms are passed to the Jersey HK2 platform as Features and each one detects if they are
enabled (e.g. Google PubSub uses `cloudevents.pubsub.project` and `cloudevents.pubsub.enabled` to be turned on, as per `PubsubEventFeature`). If a platform is enabled, it will register a LifecycleListener, which when started will be passed a `CloudEventConfigDiscovery`, and then
call `discover` on it with its own subsection from the `cloud-events.yaml` file. It typically
provides itself (which implements `CloudEventConfigDiscoveryProcessor`) as a callback so
that as the `CloudEventConfigDiscovery` figures out what needs to be created, it can be.

Kinesis and NATS work the same way, they are more simple because they are both streaming platforms.

NOTE: In the `cloud-events.yaml` definition, where you see `property`, that is a property that is requested
from `io.featurehub.utils.FallbackPropertyConfig` using `getConfig`. If a `defaultValue` is provided
it means this will be the fallback name of the channel.

## defaults

The defaults section defines an arbitrary channel name, which is just consistently used throughout to allow for further overriding of each event platform. `feature-only-channel` is an example of this.

In each channel definition is: 

* `description` - what the channel is for
* `cloudEvents` - a list of the cloudevents that will be published in this channel. This confirms to the `x-cloudevent-type` used in the OpenAPI documents in this repository.
* `subscribers` - what kind of subscribers are there for this channel
* `channelName` - is usually the definition for the default name for the channel and is usually defined by a property and a default field as part of it.
* `publisher` - this is a list of publishers of this event. 

In the subscriber definition is the following:

* `broadcast` - if true then every subscriber is expected to get a copy
* `ceRegistry` - which cloud event registry this is published into - normally it is `common` but there
 are specialist registries to ensure event messages in a single application are completely isolated. 
* `tags` - these indicate the applications that are responsible for processing them. If the `io.featurehub.rest.Info.applicationName()` matches one of these tags, the subscriber is created. If there is a `!` in front, it means do not do so for this application.
* `conditional` - optional, these can cause a client to not to subscribe to this channel at all
* `cloudEventsInclude` - optional, these can further filter the cloud event types that are listened for
* `config` - this indicates where it gets the subscription will get the name of the
channel from, it will use FallbackPropertyConfig to get the name based on the `property` field, and fall back to `default` if it cannot find it.
* `multiSupport` - this indicates that the `config` property can support multiple
comma separated subscription channels. As they are all cloud events, they will
just get fed into the same cloud events registry as per this definition.

In the publisher definition is the following:
* `tags` - these indicate the applications that are responsible for publishing them. If the `io.featurehub.rest.Info.applicationName()` matches one of these tags, the subscriber is created. If there is a `!` in front, it means do not do so for this application.
* `conditional` - optional, these can cause a client to not to subscribe to this channel at all
* `ceRegistry` - the register on which we expect these events to turn up and if they do, we publish them via the channel.
* `cloudEventsInclude` - which events to include when publishing.

and then each implementation then providing a way to connect to the underlying platform and publish and subscribe to events on those channels.  



## event platform sections


