package io.featurehub.edge.stats

import cd.connect.app.config.DeclaredConfigResolver
import com.lmax.disruptor.EventHandler
import io.featurehub.edge.KeyParts
import io.featurehub.lifecycle.LifecycleListeners
import io.featurehub.sse.stats.model.EdgeHitResultType
import io.featurehub.sse.stats.model.EdgeHitSourceType
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import jakarta.ws.rs.core.GenericType
import org.glassfish.hk2.api.Immediate
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class StatsFeature : Feature {
  private val log: Logger = LoggerFactory.getLogger(StatsFeature::class.java)
  private val eventHandlerType: GenericType<EventHandler<Stat>> = object : GenericType<EventHandler<Stat>>() {}

  init {
    DeclaredConfigResolver.resolve(this)
  }

  class EmptyStatsRecorder : StatRecorder {
    override fun recordHit(apiKey: KeyParts, resultType: EdgeHitResultType, hitSourceType: EdgeHitSourceType) {
    }
  }

  private fun recordFullStack(context: FeatureContext) {
    context.register(object : org.glassfish.jersey.internal.inject.AbstractBinder() {
      override fun configure() {
        bind(StatsCollectionOrchestrator::class.java)
          .to(StatsOrchestrator::class.java)
          .`in`(Singleton::class.java)

        bind(StatPublisherImpl::class.java).to(StatPublisher::class.java).`in`(Singleton::class.java)

        bind(StatsCollectionOrchestrator::class.java).to(StatsOrchestrator::class.java).`in`(
          Singleton::class.java
        )

        bind(StatEventHandler::class.java).to(StatCollector::class.java).to(eventHandlerType).`in`(
          Singleton::class.java
        )

        bind(StatDisruptor::class.java).to(StatRecorder::class.java).`in`(Singleton::class.java)
      }
    })

    LifecycleListeners.wrap(StatTimeTrigger::class.java, context)
  }

  private fun recordEmptyStack(context: FeatureContext) {
    context.register(object : org.glassfish.jersey.internal.inject.AbstractBinder() {
      override fun configure() {
        bind(EmptyStatsRecorder::class.java).to(StatRecorder::class.java).`in`(Immediate::class.java)
      }
    })
  }

  override fun configure(context: FeatureContext): Boolean {
    if (FallbackPropertyConfig.getConfig("edge.stats", "false") == "false") {
      recordEmptyStack(context)
    } else {
      recordFullStack(context)
    }

    return true
  }

  companion object {
    fun forceEnableStats() {
      System.setProperty("edge.stats", "true")
    }
  }
}
