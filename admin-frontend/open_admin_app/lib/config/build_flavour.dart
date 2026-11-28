/// True in the FeatureHub SaaS build (`--dart-define=featurehub.saas=true`). SaaS people sign in through the customer
/// service and never hold a FeatureHub password, so the code that sets or changes one is left out of that build: this
/// is a constant, so the compiler drops whatever it guards.
const bool saasBuild = bool.fromEnvironment('featurehub.saas');
