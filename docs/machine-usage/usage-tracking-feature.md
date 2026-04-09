# This covers the whole concept of the abiity to detect if features are being evaluated.

For the longest time, we have not added any tracking of feature evaluations into the SDKs are they lock the user
in and the cost to store this data is high, and difficult for users to run themselves. But it has become clear
with the addition of the Usage API in the SDKs that being able to determine that features are being evaluated in
specific environments is useful - and to know what they are, so the user can easily see their feature is actually
being used or when it is no longer being evaluated and is thus safe to remove.

Ideally, this should be done for only a specific time period - collecting this data outside of these times is
not really useful and is better done in other tools like Twilio.

The proposal is that a feature can have an extra attribute added which is `trackEvaluationsUntil: DateTime`, which
when set will send that information along with all feature values to clients `teu: DateTime` on the edge. This will
cause SDKs to start posting back evaluations in compressed streams of data which will be stored in the MR application
stack. The expectation is that when the `teu` has been reached, the SDKs will stop sending data. 

## UI considerations

### Setting up the data

The UI should be enhanced on editing a Feature to allow for setting a Track Evaluations. And end-time MUST be set, it
can be set to whatever the user chooses but it must be set. It cannot be future dated, it becomes active once set. 

The teu has feature history, audit, webhook and slack considerations.

### Visualising the data

Work with Zac and Stitch on a visualisation?

## MR considerations

### Setting up the data
As soon as the `teu` is set on a feature, all features values in all environments will be version bumped and their individual `teu` set and this information propagated to Dacha and out to Edge. With the version
bump it means it will propagate, and it will be captured in the audit. This must trigger the history, webhook and slack subsystems which may need to be modified for this information.

### Dealing with the incoming data
The payload will need to include the feature ID, environment ID and value. A key piece of information which is the matched strategy is missing which will need to be added to the Usage API. We want to:

- hold detailed information for the last, so, hour (maybe this could be a system config?) - which would include so we know what is going on and can actually visualise data flowing for the user
- hold summary data for the last X days (again system config - 30 by default?)

## #Edge considerations

- Edge would need to push the update messages back to MR
- Edge Rest would need to process them itself. 

# Further considerations

A company might want to offload the stream of data to elsewhere AS well. Consider allowing them to be able to define as many webhooks as they like to process that offloading.

# Fallthrough implications

Usage must add the strategy-id of which strategy (if any, null assumes none) was used to match the critera. 
