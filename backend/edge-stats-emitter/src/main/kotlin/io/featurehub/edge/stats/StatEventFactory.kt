package io.featurehub.edge.stats

import com.lmax.disruptor.EventFactory

/**
 * This is responsible for creating blank objects for filling into the ring buffer
 */
class StatEventFactory : EventFactory<Stat> {
  override fun newInstance(): Stat {
    return Stat()
  }
}
