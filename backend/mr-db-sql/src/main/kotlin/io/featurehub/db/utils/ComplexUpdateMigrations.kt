package io.featurehub.db.utils

import io.featurehub.db.api.FeatureApi
import io.featurehub.db.api.ServiceAccountApi
import io.featurehub.db.model.DbAfterMigrationJob
import io.featurehub.db.model.query.QDbAfterMigrationJob
import org.glassfish.hk2.api.ServiceLocator
import org.glassfish.jersey.server.spi.Container
import org.glassfish.jersey.server.spi.ContainerLifecycleListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ComplexUpdateMigrations : ContainerLifecycleListener {
  private val log: Logger = LoggerFactory.getLogger(ComplexUpdateMigrations::class.java)

  override fun onStartup(container: Container) {
    val injector = container.applicationHandler
      .injectionManager.getInstance(ServiceLocator::class.java)
    val serviceAccountApi = injector.getService(ServiceAccountApi::class.java)
    serviceAccountApi.cleanupServiceAccountApiKeys()

    // we want only one server to pick up the post-migrate jobs because they will need to be done in order.
    // do not abuse this and run long jobs here! if there are big jobs, fire them onto the Jobs Queue and
    for (job in QDbAfterMigrationJob().completed.isFalse.orderBy().id.asc().forUpdateSkipLocked().findList()) {
      try {
        processJob(job, injector)
        job.isCompleted = true
        job.save()
      } catch (ex: Exception) {
        log.error("failed to process job {}", job.jobName)
        break // break our of job loop
      }
    }
  }

  private fun processJob(job: DbAfterMigrationJob, injector: ServiceLocator) {
    log.info("attempting to process job {}", job.jobName)
    if (job.jobName == "upgrade-rollout-strategies") {
      injector.getService(FeatureApi::class.java).release1_5_11_strategy_update()
    }
  }


  override fun onReload(container: Container) {}
  override fun onShutdown(container: Container) {}
}
