package io.featurehub.events

/**
 * This serves as a registry for all known subjects. How individual technologies
 * publish those subjects is internally relevant (one or many topics, queues)
 */
class KnownEventSubjects {
  class Management {
    companion object {
      // an environment: PublishEnvironment
      val environmentUpdate = "io.featurehub.mr/environment"
      // a service account update: PublishServiceAccount
      val serviceAccountUpdate = "io.featurehub.mr/serviceAccount"
      // one or more feature updates: [PublishFeature]
      val featureUpdates = "io.featurehub.mr/featureUpdates"
    }
  }

  class ServiceAccountAction {
    companion object {
      // a PUT typically from Edge: StreamedFeatureUpdate
      val featureUpdates = "io.featurehub/serviceAccountFeatureUpdate"
    }
  }

  class Webhook {
    companion object {
      val webhookResult = "io.featurehub/webhookResult"
    }
  }
}
