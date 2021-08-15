package io.featurehub.edge.justget

import io.featurehub.dacha.api.DachaApiKeyService
import io.featurehub.edge.KeyParts
import io.featurehub.mr.model.DachaKeyDetailsResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue


class InflightGETRequest(private val api: DachaApiKeyService, val key: KeyParts, private val executor: EdgeConcurrentRequestPool, private val submitter: InflightGETSubmitter) {
  private val log: Logger = LoggerFactory.getLogger(InflightGETRequest::class.java)

  val notifyListener: MutableCollection<InflightGETNotifier> = ConcurrentLinkedQueue()
  var details: DachaKeyDetailsResponse? = null

  fun add(notifier: InflightGETNotifier) {
    var size: Int?

    synchronized(this) {
      notifyListener.add(notifier)
      size = notifyListener.size
    }

    // if we sre the first one, trigger it off
    if (size == 1) {
      executor.execute {
        try {
          details = api.getApiKeyDetails(key.environmentId, key.serviceKey)

          copyKeyDetails(key, details!!)
        } catch (e : Exception) {
          log.error("failed to request details for key {}", key, e)
        }

        submitter.removeGET(key)

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
