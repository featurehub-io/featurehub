package io.featurehub.lifecycle

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.propagation.TextMapSetter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.client.ClientRequestContext
import jakarta.ws.rs.client.ClientRequestFilter
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class JaxRsContainerRequestMap : TextMapGetter<ContainerRequestContext> {
  override fun keys(carrier: ContainerRequestContext): MutableIterable<String> {
    return carrier.headers.keys
  }

  override fun get(carrier: ContainerRequestContext?, key: String): String? {
    return carrier?.getHeaderString(key)
  }
}

class JaxRsClientRequestMap : TextMapSetter<ClientRequestContext> {
  override fun set(carrier: ClientRequestContext?, key: String, value: String) {
    carrier?.headers?.putSingle(key, value)
  }
}

@Singleton
class TelemetryRequestFeature @Inject constructor(private val openTelemetry: OpenTelemetry) : ContainerRequestFilter {
  private val extractor = JaxRsContainerRequestMap()

  override fun filter(requestContext: ContainerRequestContext) {
    openTelemetry.propagators.textMapPropagator.extract(Context.current(),
      requestContext, extractor)
  }
}

@Singleton
class TelemetryClientRequestFeature @Inject constructor(private val openTelemetry: OpenTelemetry) : ClientRequestFilter {
  private val injector = JaxRsClientRequestMap()

  override fun filter(requestContext: ClientRequestContext?) {
    openTelemetry.propagators.textMapPropagator.inject(Context.current(), requestContext, injector)
  }
}

/**
 * When we need to use the OpenTelemetry from another context.
 */
class UseTelemetryFeature constructor(private val openTelemetry: OpenTelemetry) : Feature {
  override fun configure(context: FeatureContext): Boolean {
    context.register(TelemetryClientRequestFeature::class.java)
    context.register(TelemetryRequestFeature::class.java)

    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(openTelemetry).to(OpenTelemetry::class.java).`in`(Singleton::class.java)
      }
    })

    return true
  }
}

class TelemetryFeature: Feature {
  override fun configure(context: FeatureContext): Boolean {
    context.register(TelemetryClientRequestFeature::class.java)
    context.register(TelemetryRequestFeature::class.java)

    val sdkTracerProvider = SdkTracerProvider.builder()
//      .addSpanProcessor()
      .build()

    val openTelemetry: OpenTelemetry = OpenTelemetrySdk.builder()
      .setTracerProvider(sdkTracerProvider)
      .setPropagators(ContextPropagators.create(W3CBaggagePropagator.getInstance()))
      .buildAndRegisterGlobal()

    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(openTelemetry).to(OpenTelemetry::class.java).`in`(Singleton::class.java)
      }
    })

    return true
  }
}
