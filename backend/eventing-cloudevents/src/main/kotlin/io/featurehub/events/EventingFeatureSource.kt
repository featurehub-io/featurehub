package io.featurehub.events

import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import jakarta.ws.rs.core.Feature
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.BufferedReader
import java.io.InputStreamReader

interface EventingFeatureSource {
  val featureSource: Class<out Feature>?
  val enabled: Boolean
}

