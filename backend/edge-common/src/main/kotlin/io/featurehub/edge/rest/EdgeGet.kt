package io.featurehub.edge.rest

import io.featurehub.edge.KeyParts
import io.featurehub.edge.features.DachaFeatureRequestSubmitter
import io.featurehub.edge.features.ETagSplitter
import io.featurehub.edge.features.FeatureRequestResponse
import io.featurehub.edge.model.*
import io.featurehub.edge.stats.StatRecorder
import io.featurehub.edge.strategies.ClientContext
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.sse.model.FeatureRolloutStrategy
import io.featurehub.sse.model.FeatureRolloutStrategyAttribute
import io.featurehub.sse.model.FeatureState
import io.featurehub.utils.UuidUtils
import io.prometheus.client.Histogram
import jakarta.inject.Inject
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.InternalServerErrorException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.ServerErrorException
import jakarta.ws.rs.container.AsyncResponse
import org.glassfish.hk2.api.IterableProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

interface EdgeGet {
  fun process(response: AsyncResponse, apiKeys: List<String>?, etagHeader: String?, queryParameters: Map<String, List<String>>?)
}

class EdgeGetProcessor  @Inject constructor(
  getOrchestrator: DachaFeatureRequestSubmitter,
  sourceResponseWrapper: IterableProvider<EdgeGetResponseWrapper>,
  statRecorder: StatRecorder
) : BaseFeatureGetProcessor(getOrchestrator, sourceResponseWrapper, statRecorder), EdgeGet {
  private val log: Logger = LoggerFactory.getLogger(EdgeGetProcessor::class.java)

  override fun process(
    response: AsyncResponse,
    apiKeys: List<String>?,
    etagHeader: String?,
    queryParameters: Map<String, List<String>>?
  ) {
    if (apiKeys.isNullOrEmpty()) {
      response.resume(BadRequestException())
      return
    }

    inout.inc()

    val realApiKeys = apiKeys.distinct().mapNotNull { KeyParts.fromString(it) }

    if (realApiKeys.isEmpty()) {
      response.resume(NotFoundException())
      inout.dec()
      return
    }

    val timer: Histogram.Timer = pollSpeedHistogram.startTimer()

    try {
      val clientContext = ClientContext.decode(queryParameters, realApiKeys)
      val etags = ETagSplitter.splitTag(etagHeader, realApiKeys, clientContext.makeEtag())

      val environments = getOrchestrator.request(realApiKeys, clientContext, etags)

      recordStatResponse(environments)

      createResponse(response, environments, etagHeader, etags)
    } catch (e: Exception) {
      log.error("Failed", e)
      response.resume(InternalServerErrorException())
    } finally {
      timer.observeDuration()

      inout.dec()
    }
  }

  companion object {

    fun mapStrategies(strategies: List<FeatureRolloutStrategy>?): List<EdgeRolloutStrategy> {
      if (strategies == null) return listOf()
      return strategies.map { s ->
        EdgeRolloutStrategy()
          .id(s.id)
          .value(s.value)
          .percentage(s.percentage)
          .percentageAttributes(s.percentageAttributes)
          .attributes(mapAttributes(s.attributes))
      }
    }

    fun mapAttributes(attributes: List<FeatureRolloutStrategyAttribute>?): List<EdgeFeatureRolloutStrategyAttribute> {
      if (attributes == null) return listOf()
      return attributes.map { a ->
        EdgeFeatureRolloutStrategyAttribute()
          .conditional(EdgeAttributeConditional.fromValue(a.conditional.value)!!)
          .fieldName(a.fieldName)
          .fieldType(EdgeRolloutStrategyFieldType.fromValue(a.type.value)!!)
          .values(a.values)
      }
    }

    fun mapFeatures(features: List<FeatureState>?): List<EdgeFeatureState> {
      if (features == null) return listOf()
      return features.map(::mapFeature)
    }

    fun mapFeature(f: FeatureState): EdgeFeatureState {
      return EdgeFeatureState()
        .id(f.id.toString().substring(0,4))
        .key(f.key)
        .value(if (f.type == FeatureValueType.BOOLEAN) (if (f.value == true) 1 else 0) else f.value)
        .version(f.version!!)
        .locked(if (f.l == true) 1 else 0)
        .strategies(mapStrategies(f.strategies))
        .type(EdgeFeatureValueType.fromValue(f.type!!.value)!!)
    }
  }

  override fun buildResponseObject(environments: List<FeatureRequestResponse>): Any {
    return EdgeFeatureEnvironments().environments(environments.map { e ->
      EdgeEnvironment().id(UuidUtils.shortUuiid(e.environment.id)).features(mapFeatures(e.environment.features))
    })
  }
}
