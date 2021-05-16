package io.featurehub.edge.stats

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import com.lmax.disruptor.EventTranslatorThreeArg
import com.lmax.disruptor.RingBuffer
import com.lmax.disruptor.dsl.Disruptor
import com.lmax.disruptor.util.DaemonThreadFactory
import io.featurehub.edge.KeyParts
import io.featurehub.sse.stats.model.EdgeHitResultType
import io.featurehub.sse.stats.model.EdgeHitSourceType
import java.lang.RuntimeException
import javax.inject.Inject

class StatDisruptor @Inject constructor(val eventHandler: StatEventHandler) : StatRecorder {
  @ConfigKey("edge.stats.disruptor-buffer-size")
  var configBufferSize = "1024"

  private val disruptor: Disruptor<Stat>

  private val ringBuffer: RingBuffer<Stat>

  init {
    DeclaredConfigResolver.resolve(this)

    val bufferSize = Integer.parseInt(configBufferSize)

    if (bufferSize % 2 != 0) {
      throw RuntimeException("Edge disruptor buffer must be a factor of 2")
    }

    disruptor = Disruptor(StatEventFactory(), bufferSize, DaemonThreadFactory.INSTANCE)
    disruptor.handleEventsWith(eventHandler)
    disruptor.start()

    ringBuffer = disruptor.ringBuffer
  }

  private val TRANSLATOR: EventTranslatorThreeArg<Stat, KeyParts, EdgeHitResultType, EdgeHitSourceType> =
    EventTranslatorThreeArg<Stat, KeyParts, EdgeHitResultType, EdgeHitSourceType> { event, sequence, apiKey, resultType, hitType ->
      event.apiKey = apiKey
      event.resultType = resultType
      event.hitSourceType = hitType
    }

  override fun recordHit(apiKey: KeyParts, resultType: EdgeHitResultType, hitSourceType: EdgeHitSourceType) {
    ringBuffer.publishEvent(TRANSLATOR, apiKey, resultType, hitSourceType)
  }
}
