Modify the FeatureValueInterceptor so that is an interface or equivalent and has two methods:
1. Matched(string key, IFeatureRepositoryContext repository, FeatureState? featureState) - where  `IFeatureRepositoryContext` represents the internal access to the repository and its feature values, and FeatureState? is an optional representat of the FeatureState from the server if it exists.
2. Close() - which is called when the repository is being shut down and any allocated resources need to be closed.


Create a LocalYamlValueInterceptor class which is an implementation of ExtendedFeatureValueInterceptor and reads from a YAML file. 
- 
- The Yaml filename, if not passed (nullable) should be read from an environment variable called FEATUREHUB_LOCAL_YAML, default
to `featurehub-features.yaml` in the local directory.  
- Have it expect a single field called "flagValues" which is a map of key, value pairs, where the
key is the key of the feature and the value will be the value.

Once it has the key, value pairs it should decode what the actual value should be
based on the FeatureValueType from the passed featureState if it can.

- If the FeatureValueType is a BOOLEAN and the value is null, return false. If it is a bool type,
return the value. If it can be converted to a string and that lower case string is `true`,
return true. Otherwise return false.
- Now if the value is null, return null regardless.
- If the FeatureValueType is a NUMBER and the value is a number, return  the number. If it is a string and the string can be converted to a number, convert it to a number and return it. Otherwise return null.
- If the FeaturevalueType is a String and  the value is a simple scalar values — string, number, boolean" to a string, return it as a string, otherwise return null.
- If the FeatureValueType is JSON, whatever the value is attempt to convert it to a JSON string and return it as a string. If it cannot be converted to a string, return null.

- If we don't know its type because the featureState is null, and it is a boolean, number, or string
return it as such. If it is an object, convert it into a string and return it.
- Allow an option so that the file is watched and reloaded, update for any necessary
dependencies to enable this, add tests to ensure it works. Add a close method to stop the watching as per FeatureValueInterceptor close().
- write tests
- ensure linting and formatting rules pass.

Add a LocalYamlFeatureStore class which needs a FeatureHubConfig passed to it and an optional
filename which defaults to the same as the LocalYamlValueInterceptor. Further requirements are:
- It should read the file only once and send the features to the repository which it gets from the FeatureHubConfig using updateFeatures, using the source name of "local-yaml-store". 
- The class should create a UUID on creation which it uses for the environmentId. 
- It should read out its values from a file which is the same format as the LocalYamlValueInterceptor (they can share the code for this if it makes sense), and creates features with the key and value from the file, setting the version to 1, locked (l) to false, setting the environmentId to the stored environmentId, but it should try and detect the FeatureValueType from the yaml type.
- The ID of the feature should be set to the short SHA hash (as a string) of the feature key.
- When reading the values, it should choose a BOOLEAN FeatureValueType if the value is boolean or, ignoring its case, it matches the words "true" or "false". BOOLEAN types never  
- If it is null, it should be a FeatureValueType.STRING
- If it is a number it should be a FeatureValueType.NUMBER, but ensure it is a BigDecimal
- If it is a string it should be a FeatureValueType.STRING
- If it is a non scalar type of any kind, it should be converted to a string but have the type FeatureValueType.JSON.
- it should include comprehensive set of tests

               
Update the intercepted_value on a ValueInterceptor to return a  tuple where the first element is a bool and indicates that the matching was successful, and the second is the value if the first is bool otherwise nil. The value is a [bool? | String? | Float?] or is nil. The repository should iterate over the interceptors until it finds one that matches and return it otherwise it should return false, nil.


          
-- browser only                        
Create a LocalSessionStore which needs a FeatureHubConfig passed to it. It stores as a JSON encoded string in the browser's session storage all 
the features from the ClientFeatureRepository in a Record indexed by id (typescript `Record<string, FeatureState>` where the key is the `featureState.id`). The key it uses for the session storage is the one returned from the
FeatureHubConfig's featureUrl. It needs to be implemented as a `RawUpdateFeatureListener`. When it starts
it will ask the config for the featureUrl, detect if there is stored state and if so, read it out, JSON decode it
and send it to the repository via the "notify" method with the first parameter of `SSEResultState.Features`, the second
as the decoded data and `source` as `local-session-store`. If it receives any calls on from the `RawUpdateFeatureListener` interface with the source `local-session-store` it will drop them to avoid a cyclic circle. With any other source, it will update its internal state, reading the features, updating them all, deleting individual ones or updating individual ones as per calls. If it gets a `configChanged()` event, it should check if the `featureUrl()` has changed from config and if so, check if there is data under the new key and repeat the init process.   
 
Create a RedisSessionStore which stores the feature values from the
core ClientFeatureRepository in Redis. It needs to take a connection
string to Redis, a namespace index for Redis (optional, defaults to 0), a FeatureHubConfig (`Config`), a `prefix` key for storing featurehub state (defaults to `featurehub`) and a `timeout` (in seconds, defaulting to 30 minutes). All optionals should be stored in a Partial object passed in the constructor. When it starts it will check if there is a 
`${prefix}_ids` and if so, read the ids, find the features at `${prefix}_id` which as `FeatureState` objects and call the repository with  
m all out and send them to the "notify" method with the first parameter of `SSEResultState.Features`, the source parameter is called `redis-store`. It will implement the
`RawUpdateFeatureListener` interface and not take back updates from
the same source. When timeout occurs, it will read all the feature states and fire them back at the repository, relying on it to ignore old versions. Warn users not to use this with server evaluated features
as those will overwrite each other's states.

Create a RedisSessionStore which stores the feature values from the core ClientFeatureRepository in Redis and reads them back on startup to ensure there is a backup location for features. 
- It needs to take a connection string to Redis, a namespace index for Redis (optional, defaults to 0). When the used redis library supports RedisCluster, it should offer a choice to pass in to the constructor the available Redis connection in whatever formats are supported. 
- All constructors must also include a FeatureHubConfig (`Config`), a optional `prefix` key for storing featurehub state (defaults to `featurehub`) and a `timeout` (in seconds, defaulting to 5 minutes). 
- All optionals should be stored in a Partial object passed in the constructor. When it starts it will check if there is a
`${prefix}_ids` and if so, read the ids, find the features at `${prefix}_id` which aare stored as JSON serialized `FeatureState` objects and call the repository with them. If the `notify` function is used to pass all state in, it should use that with the first parameter of `SSEResultState.Features` and with the source "redis-store", if updateFeatures is used it should pass that as an array with the source "redis-store".
- It will implement the `RawUpdateFeatureListener` interface to receive raw updates but ignore updates from itself (source "redis-store"). When timeout occurs, it will read all the feature states and fire them back at the repository, relying on it to ignore old versions. It should thus support updating individual features and deleting features 
- It should check that the config is defined to use client evaluated features or the api keys are empty. If not it will log an error message indicating that values can be overwritten in this combination and refuse to do anything.
- when it writes the state it should also write a key called `${prefix}_sha` which it should calculate from the concatened values of the feature ids (as strings) and versions. It should keep this value to detect if updates coming from other sources require it to write updates and also when the timeout occurs, it should check this value against the last copy it has and do nothing if it hasn't changed. This avoids unnecessary reads and writes when no changes have occurred.


