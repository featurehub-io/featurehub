package io.featurehub.edge.stats

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import com.lmax.disruptor.EventHandler
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import jakarta.ws.rs.core.GenericType
import org.glassfish.hk2.api.ServiceLocator
import org.glassfish.jersey.server.spi.Container
import org.glassfish.jersey.server.spi.ContainerLifecycleListener

class StatsFeature : Feature {
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

        bind(StatDisruptor::class.java).to(StatRecorder::class.java).`in`(Singleton::class.java)

        if (whichStatsPublisherToUse == "nats") {
          bind(NATSStatPublisher::class.java).to(StatPublisher::class.java).`in`(Singleton::class.java)
        }

        bind(StatsCollectionOrchestrator::class.java).to(StatsOrchestrator::class.java).`in`(
          Singleton::class.java
        )

        bind(StatEventHandler::class.java).to(StatCollector::class.java).to(eventHandlerType).`in`(
          Singleton::class.java
        )

        bind(StatTimeTrigger::class.java).to(StatTimeTrigger::class.java).`in`(Singleton::class.java)
      }
    })
      .register(object : ContainerLifecycleListener {
        override fun onStartup(container: Container) {

          // access the ServiceLocator here
          val injector = container
            .applicationHandler
            .injectionManager
            .getInstance(ServiceLocator::class.java)
          // starts the stats time publisher if there are any

          // starts the stats time publisher if there are any
          injector.getService(StatTimeTrigger::class.java)
          injector.getService(StatRecorder::class.java)
        }

        override fun onReload(container: Container) {
        }

        override fun onShutdown(container: Container) {
        }
      })

    return true
  }
}
