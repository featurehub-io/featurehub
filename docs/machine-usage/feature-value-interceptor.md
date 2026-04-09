Modify the FeatureValueInterceptor so that is an interface or equivalent and has two methods:
1. Matched(string key, IFeatureRepositoryContext repository, FeatureState? featureState) - where  `IFeatureRepositoryContext` represents the internal access to the repository and its feature values, and FeatureState? is an optional representat of the FeatureState from the server if it exists.
2. Close() - which is called when the repository is being shut down and any allocated resources need to be closed.


Create a LocalYamlValueInterceptor class which is an implementation of FeatureValueInterceptor and reads from a YAML file.  
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
- If the FeatureValueType is JSON, if it is a string, try and convert it to json and if that is ok, return it as is, otherwise convert it to a JSON string and return it as a string. If it cannot be converted to a string, return null.

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
                                     
js:
Add a LocalYamlFeatureStore class which needs a FeatureHubConfig passed to it and an optional
filename which defaults to the same as the LocalYamlValueInterceptor. Further requirements are:
- It should read the file only once and send the features to the repository which it gets from the FeatureHubConfig using notify(SSEResultState.Features), using the source name of "local-yaml-store". The LocalYamlFeatureStore
- The class should use the environmentId for new features from the FeatureHubConfig
- It should read out its values from a file which is the same format as the LocalYamlValueInterceptor (they can share the code for this if it makes sense), and creates features with the key and value from the file, setting the version to 1, locked (l) to false, setting the environmentId to the FeatureHubConfig environmentId, but it should try and detect the FeatureValueType from the yaml type.
- The ID of the feature should be set to the short SHA256 hash (as a string) of the feature key for consistency.
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


Create a RedisSessionStore which stores the feature values from the core ClientFeatureRepository in Redis and reads them back on startup to ensure there is a backup location for features. 
- It needs to take a connection string to Redis, a database index for Redis (optional, defaults to 0). When the used redis library supports RedisCluster, it should offer a choice to pass in to the constructor the available Redis connection in whatever formats are supported. 
- All constructors must also include a FeatureHubConfig (`Config`). It should include optional configuration for a key `prefix` key for storing featurehub state (defaults to `featurehub`), a `backoff_timeout` (in milliseconds, defaults to 500), a `retry_update_count` (defaults to 10) and a `refresh_timeout` (in seconds, defaulting to 5 minutes). 
- All optionals should be stored in a Partial object passed in the constructor.
- It stores data in two keys. The features key `${prefix}_${environmentId}` which is a JSON encoded array of `FeatureState` objects and a field tracking if the features have changed - `${prefix}_${environmentId}_sha`. The `${prefix}_${environmentId}_sha` is a unique string that a SHA256 is generated from and stored. The unique string is in the format that takes each feature's id and version (or 0 if null) as `feature.id:feature.version` and then combines them with a `|` symbol and then generates a sha256 of them.
- On first start of the RedisSessionStore the values should be read from the features key (if any) and pushed into the repository using a source of `redis-store`. The feature changed field should be stored internally.
- Every time  the timer goes off, it should read the feature changed field and compare it against the internally stored value. If this is different the features should be loaded again, pushed into the repository and the features changed field updated. 
- If it receives an `updateFeatures` call, it should calculate the sha and compare it against the one in the database. If they are different, it should load the features in the database and compare the versions of each feature. If any of the versions of the 
updateFeatures call are newer, it should store the updates.
- to store the updates it should calculate the new SHA256 and have the JSON features encoded. We should update only if the newly calculated SHA256 is different from our stored one. If it is put a Redis WATCH on the `${prefix}_${environmentId}_sha` field, check if the sha in redis is different from our stored value. If it is the same as our stored one, we should proceed to updating redis. If it is different go into the comparison cycle again (checking for versions in case another update has come in with a later version of some feature). It should try `retry_update_count` times to do the update if it has newer versions, but should stop immediately it detects it has older versions it is trying to write. between retries it should back off at `backoff_timeout` intervals trying to see if the key has updated and either matches its own and if not. If it outright fails, we should log a warning.  
- when `updateFeature` is called it will need to read the features from redis and check if this feature (by id) has a newer version. If so it will replace it and go through the store updates process.
- when a `deleteFeature` is called, it should again read the features from redis, delete the feature by id if it hasn't already been deleted (in which case do nothing) and go through the updates process.

Update the Segment usage plugin so that it allows the user to specify a list of usage events (by eventName) that it will 
support, defaulting to supporting two - `feature` and `feature-collection`. It should allow transformers to be passed for each message and default to its own if they are not. for `feature` it should simply call `collectUsageRecord` and merge those
attributes in as it does now. For `feature-collection` it should call `collectUsageRecord` find the `fhub_keys` field (which is
an array of strings), and copy out the fields `${key}_raw` and all fields that do not match `${key}` and merge those in to the data it sends segment.  


