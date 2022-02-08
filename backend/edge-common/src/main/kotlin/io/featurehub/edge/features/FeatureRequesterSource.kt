package io.featurehub.edge.features

import io.featurehub.dacha.api.DachaApiKeyService
import io.featurehub.dacha.model.DachaKeyDetailsResponse
import io.featurehub.edge.KeyParts
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue

class FeatureRequesterSource(private val api: DachaApiKeyService, override val key: KeyParts, private val executor: EdgeConcurrentRequestPool, private val submitter: DachaFeatureRequestSubmitter) :
  FeatureRequester {
  private val log: Logger = LoggerFactory.getLogger(FeatureRequesterSource::class.java)

  val notifyListener: MutableCollection<FeatureRequestCompleteNotifier> = ConcurrentLinkedQueue()

  override var details: DachaKeyDetailsResponse? = null
  override var failure: Exception? = null

  override fun add(notifier: FeatureRequestCompleteNotifier) {
    var size: Int?

    synchronized(this) {
      notifyListener.add(notifier)
      size = notifyListener.size
    }

    // if we sre the first one, trigger it off
    if (size == 1) {
      executor.execute {
        try {
          details = api.getApiKeyDetails(key.environmentId, key.serviceKey, true)

          if (details != null)
            copyKeyDetails(key, details!!)
        } catch (e : Exception) {
          failure = e
          log.trace("failed to request details for key {}", key, e)
        }

        submitter.requestForKeyComplete(key)

        log.debug("concentrated {} requests", notifyListener.size)

        notifyListener.forEach { nl -> nl.complete(this) }
        notifyListener.clear()
      }
    }
  }

  private fun copyKeyDetails(key: KeyParts, details: DachaKeyDetailsResponse) {
    key.organisationId = details.organizationId
    key.portfolioId = details.portfolioId
    key.applicationId = details.applicationId
    key.serviceKeyId = details.serviceKeyId
  }
}
