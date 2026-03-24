
 Add a Close method to the ClientFeatureHubRepository (and InternalRepository interface) which will close all interceptors and plugins and a close method on the Config as well.
 
if Close has been called on EdgeFeatureHubConfig it should remember and not try and create a new repository or edgeService and always respond NotReady for Readiness or Readyness. Any attempt to create a new Context should throw a ConfigurationInvalid exception for languages that support exceptions and null for those that don't.  

if Close has been called on EdgeFeatureHubConfig it should nil all references to the repository, edgeService and usageadapter
and remember Close has been called. It should not try and create a new repository or edgeService
and always respond NotReady for Readiness or Readyness. should throw a ConfigurationClosed exception for languages that support exceptions and null/nil/undefined for those that don't. Method calls that are delegated to the repository or edgeService should be silently ignored unless
they return a value in which case they should return null/nil/undefined. If you discover any that can't do that stop and
ask for feedback. Config should return a public IsClosed so it can be detected
that the Config has been closed. Adding a ReadinessListener via the EdgeFeatureHubConfig should also through a ConfigurationClosed exception.
