Modify the FeatureValueInterceptor so that is an interface or equivalent and has two methods:
1. Matched(string key, IFeatureRepositoryContext repository, FeatureState? featureState) - where  `IFeatureRepositoryContext` represents the internal access to the repository and its feature values, and FeatureState? is an optional representat of the FeatureState from the server if it exists.
2. Close() - which is called when the repository is being shut down and any allocated resources need to be closed.


Create a LocalYamlValueInterceptor class which is an implementation of FeatureValueInterceptor and reads from
a YAML file. The Yaml file should be read from an environment variable called FEATUREHUB_LOCAL_YAML or default
to `featurehub-features.yaml` in the local directory.  Have it expect a single field called "flagValues" which is a map of key, value pairs, where the
key is the key of the feature and the value will be the value. If the value field is true or false
it should be mapped to a FeatureValueType.BOOLEAN, if it is a number, to
a FeatureValueType.NUMBER, if a string to FeatureValueType.STRING and if it is a complex
further yaml structure, it should be converted to a JSON string and returned as a JSON string.

       
update the LocalYamlValueInterceptor and allow an option so that the file is watched and reloaded, update for any necessary
dependencies to enable this, add tests to ensure it works. Add a close method to stop the watching as per FeatureValueInterceptor close() method rules and ensure linting and formatting rules pass. 
               
Update the intercepted_value on a ValueInterceptor to return a  tuple where the first element is a bool and indicates that the matching was successful, and the second is the value if the first is bool otherwise nil. The value is a [bool? | String? | Float?] or is nil. The repository should iterate over the interceptors until it finds one that matches and return it otherwise it should return false, nil.
           
We need the value of the Module FeatureValueType to equal the field name because we want to use FeatureValueType.BOOLEAN instead of the text "BOOLEAN" throughout the code for instance. Do the same for STRING, JSON and NUMBER in the FeatureValueType module.
                        
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

FeatureHubConfig should take the edge_url and api_keys as optional, it should be able to pick them up from the environment variables FEATUREHUB_EDGE_URL and FEATUREHUB_CLIENT_API_KEY / FEATUREHUB_SERVER_API_KEY if they exist. If none of them exist, the edge service should never attempt to connect. This is useful in situations like
using the LocalYamlStorage where features are loaded solely from disk.
            
Add a value method to the Config and Repository that 
takes one required parameter (the feature key), and two optional ones - the `defaultValue` and the `attributes` and have it call `feature` method with the key and attributes and check `present?` and if true it returns the value of the feature and if false it returns the defaultValue.
