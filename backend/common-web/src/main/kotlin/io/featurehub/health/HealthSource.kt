package io.featurehub.health

interface HealthSource {
  val healthy: Boolean
  val sourceName: String
}
