package io.featurehub.edge.stats

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import com.lmax.disruptor.EventHandler
import io.featurehub.events.kinesis.KinesisEventFeature
import io.featurehub.events.kinesis.KinesisFactory
import io.featurehub.events.pubsub.GoogleEventFeature
import io.featurehub.lifecycle.LifecycleListeners
import io.featurehub.publish.NATSFeature
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import jakarta.ws.rs.core.GenericType
import org.glassfish.hk2.api.Immediate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException

class StatsFeature : Feature {
  private val log: Logger = LoggerFactory.getLogger(StatsFeature::class.java)
  @ConfigKey("edge.stats-publisher")
  var whichStatsPublisherToUse: String = "nats"

  private val eventHandlerType: GenericType<EventHandler<Stat>> = object : GenericType<EventHandler<Stat>>() {}

  init {
    DeclaredConfigResolver.resolve(this)
  }

  override fun configure(context: FeatureContext): Boolean {
    context.register(object : org.glassfish.jersey.internal.inject.AbstractBinder() {
      override fun configure() {
        bind(StatsCollectionOrchestrator::class.java)
          .to(StatsOrchestrator::class.java)
          .`in`(Singleton::class.java)

        if (NATSFeature.isNatsConfigured()) {
          bind(NATSStatPublisher::class.java).to(CloudEventStatPublisher::class.java).`in`(Singleton::class.java)
        }

        if (GoogleEventFeature.isEnabled()) {
          bind(PubsubStatsPublisher::class.java).to(CloudEventStatPublisher::class.java).`in`(Singleton::class.java)
        }

        if (KinesisEventFeature.isEnabled()) {
          bind(KinesisStatsPublisher::class.java).to(CloudEventStatPublisher::class.java).`in`(Singleton::class.java)
        }

        if (!NATSFeature.isNatsConfigured() && !GoogleEventFeature.isEnabled() && !KinesisEventFeature.isEnabled()) {
          log.error("No messaging platform configured for stat publishing")
          throw IllegalStateException("No messaging platform configured for stat publishing")
        }

        bind(StatPublisherImpl::class.java).to(StatPublisher::class.java).`in`(Singleton::class.java)

        bind(StatsCollectionOrchestrator::class.java).to(StatsOrchestrator::class.java).`in`(
          Singleton::class.java
        )

        bind(StatEventHandler::class.java).to(StatCollector::class.java).to(eventHandlerType).`in`(
          Singleton::class.java
        )

        bind(StatDisruptor::class.java).to(StatRecorder::class.java).`in`(Immediate::class.java)
      }
    })

    LifecycleListeners.wrap(StatTimeTrigger::class.java, context)

    return true
  }
}
