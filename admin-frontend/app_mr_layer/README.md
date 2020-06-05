## To make it work

Ensure the openapi-dart-plugin has been `mvn clean install` - it should be 5.1-SNAPSHOT. I have copied it into our
repo for the time being to make it easy.

Then you can `mvn clean generate-sources` as per normal. 

The e2e project has tests that preload users and the organisation. It has a run.sh file in it, so if you wish to preload
or write some more e2e steps to load up some data, its in there and should be fairly straightforward to understand.

My workflow is:

- run AppRunner
- run ./run.sh for e2e tests
- refresh the client, and now login.

First time through, make sure you run your `flutter packages get` in `app-singleapp` and force re-run the Dart Analysis.

## Code Generator for APi Layer

The code generator experimental and thus is in snapshot mode. The code generator will not work for the Dart 
command line as it is missing the dart:html package (which only works inside browsers). The changes include in the 5.x
series of the command line generator extract out the common API from the layer you are using from the API itself. 

This an ApiClient now has an `ApiClientDelegate` (for browser or cli) and a `DeserializeDelegate` (which is generated).

The cli client is missing support for File uploads (multi-part forms) and the whole multi-part functionality is now
a bit dodgy, but as we don't use it at the moment i'm not fussed. 

