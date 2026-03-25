
 Add a Close method to the ClientFeatureHubRepository (and InternalRepository interface) which will close all interceptors and plugins and a close method on the Config as well.
 
if Close has been called on EdgeFeatureHubConfig it should remember and not try and create a new repository or edgeService and always respond NotReady for Readiness or Readyness. Any attempt to create a new Context should throw a ConfigurationInvalid exception for languages that support exceptions and null for those that don't.  

if Close has been called on EdgeFeatureHubConfig it should nil all references to the repository, edgeService and usageadapter
and remember Close has been called. It should not try and create a new repository or edgeService
and always respond NotReady for Readiness or Readyness. should throw a ConfigurationClosed exception for languages that support exceptions and null/nil/undefined for those that don't. Method calls that are delegated to the repository or edgeService should be silently ignored unless
they return a value in which case they should return null/nil/undefined. If you discover any that can't do that stop and
ask for feedback. Config should return a public IsClosed so it can be detected
that the Config has been closed. Adding a ReadinessListener via the EdgeFeatureHubConfig should also through a ConfigurationClosed exception.

FeatureHubConfig should take the edge_url and api_keys as optional. If none of them exist, the edge service should never attempt to connect and create a NoopEdgeService that does nothing but implements the basic interface. This is useful in situations like
using the LocalYamlStorage where features are loaded solely from disk.

it should be able to pick them up from the environment variables FEATUREHUB_EDGE_URL and FEATUREHUB_CLIENT_API_KEY / FEATUREHUB_SERVER_API_KEY if they exist

Add a value method to the Config and Repository that
takes one required parameter (the feature key), and two optional ones - the `defaultValue` and the `attributes` and have it call `feature` method with the key and attributes and check `present?` and if true it returns the value of the feature and if false it returns the defaultValue.
