package io.featurehub.edge.features

import io.featurehub.edge.KeyParts
import io.featurehub.edge.strategies.ClientContext

/**
 * This knows how to request for a bunch of keys from Dacha and returns a list of environments. If
 * there is no access or no data for an environment the features are empty.
 */
interface DachaFeatureRequestSubmitter {
  /*
   * we have a result, don't use this request again. It is a callback used by the actual process for requesting.
   */
  fun requestForKeyComplete(key: KeyParts)

  /*
   * Requests a bunch of environment details from Dacha in the most efficient way possible
   */
  fun request(keys: List<KeyParts>, context: ClientContext): List<FeatureRequestResponse>
}
