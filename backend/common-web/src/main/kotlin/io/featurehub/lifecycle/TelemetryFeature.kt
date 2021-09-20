package io.featurehub.lifecycle

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import cd.connect.jersey.common.LoggingConfiguration
import io.featurehub.jersey.config.CommonConfiguration
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.Context
import io.opentelemetry.context.Scope
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.context.propagation.TextMapSetter
import io.opentelemetry.exporters.logging.LoggingSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.client.Client
import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.client.ClientRequestContext
import jakarta.ws.rs.client.ClientResponseContext
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.hk2.api.ServiceLocator
import org.glassfish.jersey.client.spi.PostInvocationInterceptor
import org.glassfish.jersey.client.spi.PreInvocationInterceptor
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.glassfish.jersey.server.ContainerRequest
import org.glassfish.jersey.server.ExtendedUriInfo
import org.glassfish.jersey.server.monitoring.ApplicationEvent
import org.glassfish.jersey.server.monitoring.ApplicationEventListener
import org.glassfish.jersey.server.monitoring.RequestEvent
import org.glassfish.jersey.server.monitoring.RequestEventListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

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
class TelemetryInvocationInterceptor @Inject constructor(
  private val openTelemetry: OpenTelemetry,
  private val tracer: Tracer
) : PreInvocationInterceptor, PostInvocationInterceptor {
  private val injector = JaxRsClientRequestMap()

  override fun beforeRequest(request: ClientRequestContext) {
    val path = TelemetryApplicationEventListener.normalizePath(request.uri.path)
    val span = tracer.spanBuilder("${request.method} ${request.uri}").setSpanKind(SpanKind.CLIENT).startSpan()
    val scope = span.makeCurrent()

    openTelemetry.propagators.textMapPropagator.inject(Context.current(), request, injector)

    span.setAttribute("HTTP_METHOD", request.method)
    span.setAttribute("HTTP_SCHEME", request.uri.scheme)
    span.setAttribute("HTTP_HOST", request.uri.host)
    span.setAttribute("HTTP_RESOURCE", path)

    request.setProperty("span", span)
    request.setProperty("scope", scope)
  }

  override fun afterRequest(request: ClientRequestContext, response: ClientResponseContext) {
    close(request)
  }

  private fun close(request: ClientRequestContext) {
    (request.getProperty("span") as Span).end()
    (request.getProperty("scope") as Scope).close()
  }

  override fun onException(
    request: ClientRequestContext,
    exceptionContext: PostInvocationInterceptor.ExceptionContext
  ) {
    (request.getProperty("span") as Span).setStatus(StatusCode.ERROR)
    close(request)
  }

}

@Singleton
class TelemetryApplicationEventListener @Inject constructor(
  private val openTelemetry: OpenTelemetry,
  private val tracer: Tracer
) : ApplicationEventListener {
  private val extractor = JaxRsContainerRequestMap()

  override fun onEvent(event: ApplicationEvent?) {
  }

  override fun onRequest(requestEvent: RequestEvent): RequestEventListener? {
    return TelemetryRequestEventListener(openTelemetry, tracer, extractor)
  }

  internal class TelemetryRequestEventListener constructor(
    private val openTelemetry: OpenTelemetry,
    private val tracer: Tracer,
    private val extractor: JaxRsContainerRequestMap
  ) : RequestEventListener {
    private val log: Logger = LoggerFactory.getLogger(TelemetryRequestEventListener::class.java)

    var span: Span? = null
    var scope: Scope? = null

    override fun onEvent(event: RequestEvent) {
      if (event.type == RequestEvent.Type.REQUEST_MATCHED) {
        processRequest(event.containerRequest)
      } else if (event.type == RequestEvent.Type.ON_EXCEPTION) {
        span?.setStatus(StatusCode.ERROR)
      } else if (event.type == RequestEvent.Type.FINISHED) {
        span?.end()
        scope?.close()
      }
    }

    private fun processRequest(request: ContainerRequest) {
      val extractedContext = openTelemetry.propagators.textMapPropagator.extract(
        Context.current(),
        request, extractor
      )

      scope = extractedContext.makeCurrent()
      val extendedUriInfo = request.uriInfo
      val name: Optional<String> = extendedUriInfo.matchedTemplates.stream()
        .map { uriTemplate -> normalizePath(uriTemplate.template) }
        .reduce { a, b -> b + a }
      val path = if (name.isPresent) name.get() else "/"
      span = tracer.spanBuilder("${request.method} ${path}")
        .setSpanKind(SpanKind.SERVER)
        .startSpan()

      val baggage = Baggage.fromContext(Context.current())

      if (baggage.size() > 0) {
        baggage.forEach({ k, v -> log.info("baggage `{}`:`{}", k, v.value) } )
      } else {
        log.info("baggage is empty")
      }

      span!!.setAttribute("HTTP_METHOD", request.method)
      span!!.setAttribute("HTTP_SCHEME", request.requestUri.scheme)
      span!!.setAttribute("HTTP_HOST", request.requestUri.host)
      span!!.setAttribute("HTTP_RESOURCE", path)
      span!!.setStatus(StatusCode.OK)
    }
  }

  companion object {
    fun normalizePath(normPath: String?): String {
      var path = normPath
      // ensure that non-empty path starts with /
      if (path == null || "/" == path) {
        path = ""
      } else if (!path.startsWith("/")) {
        path = "/$path"
      }
      // remove trailing /
      if (path.endsWith("/")) {
        path = path.substring(0, path.length - 1)
      }
      return path
    }
  }
}


/**
 * Because the OpenTelemetry baggage is only available in a static context, there is no way we can mock
 * this out, we would have to create an entire in memory instance of the OpenTelemetry stack and then
 * inject a fake header into it. So we create an interface wrapper around it and this should be used in
 * all cases of checking for baggage.
 */
interface BaggageChecker {
  fun baggage(key: String): String?
  fun hasBaggage(key: String): Boolean
}

class OpenTelemetryBaggageChecker : BaggageChecker {
  override fun baggage(key: String): String? {
    return Baggage.current().getEntryValue(key)
  }

  override fun hasBaggage(key: String): Boolean {
    return baggage(key) != null
  }
}

class BaggageFeature: Feature {
  override fun configure(context: FeatureContext): Boolean {
    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(OpenTelemetryBaggageChecker::class.java).to(BaggageChecker::class.java).`in`(Singleton::class.java)
      }
    })

    return true
  }
}

class ClientTelemetryFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    context.register(TelemetryInvocationInterceptor::class.java)
    context.register(BaggageFeature::class.java)

    return true
  }
}

/**
 * When we need to use the OpenTelemetry from another DI context. How the client is bound in is quite complicated
 * as when we can only have one OpenTelemetry instance and it originates in a different context. The Client is also
 * its own context, so we have to pull the opentelemetry + tracer from its original context and bind it to this server
 * based one, and the new client one (which is in turn bound to this server based one).
 *
 * This feature only works with an injector that has completed its service cycle from a different DI context.
 */
class UseTelemetryFeature constructor(private val injector: ServiceLocator) : Feature {
  override fun configure(context: FeatureContext): Boolean {
    context.register(TelemetryApplicationEventListener::class.java)
    context.register(BaggageFeature::class.java)

    val telemetryBinder = object : AbstractBinder() {
      override fun configure() {
        bind(injector.getService(OpenTelemetry::class.java)).to(OpenTelemetry::class.java).`in`(Singleton::class.java)
        bind(injector.getService(Tracer::class.java)).to(Tracer::class.java).`in`(Singleton::class.java)
      }
    }

    context.register(telemetryBinder)

    val client = ClientBuilder.newClient()
      .register(ClientTelemetryFeature::class.java)
      .register(CommonConfiguration::class.java)
      .register(LoggingConfiguration::class.java)
      .register(telemetryBinder)

    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(client).to(Client::class.java).`in`(
          Singleton::class.java
        )
      }
    })

    return true
  }
}

class TelemetryFeature : Feature {
  private val log: Logger = LoggerFactory.getLogger(TelemetryFeature::class.java)

  @ConfigKey("telemetry.logging-enabled")
  var logTelemetry: Boolean? = false

  init {
    DeclaredConfigResolver.resolve(this)
  }

  override fun configure(context: FeatureContext): Boolean {
    context.register(TelemetryApplicationEventListener::class.java)
    context.register(BaggageFeature::class.java)

    val sdkTracerProvider = SdkTracerProvider.builder()

    if (logTelemetry == true)
      sdkTracerProvider.addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter()))

    val openTelemetry: OpenTelemetry = OpenTelemetrySdk.builder()
      .setTracerProvider(sdkTracerProvider.build())
      .setPropagators(
        ContextPropagators.create(
          TextMapPropagator.composite(
            W3CBaggagePropagator.getInstance(),
            W3CTraceContextPropagator.getInstance()
          )
        )
      )
      .buildAndRegisterGlobal()

    val tracer = openTelemetry.getTracer("featurehub")

    log.info("OpenTelemetry configured")

    val client = ClientBuilder.newClient()
      .register(ClientTelemetryFeature::class.java)
      .register(CommonConfiguration::class.java)
      .register(LoggingConfiguration::class.java)

    val telemetryBinder = object : AbstractBinder() {
      override fun configure() {
        bind(openTelemetry).to(OpenTelemetry::class.java).`in`(Singleton::class.java)
        bind(tracer).to(Tracer::class.java).`in`(Singleton::class.java)
      }
    }

    context.register(telemetryBinder)
    client.register(telemetryBinder)

    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(client).to(Client::class.java).`in`(
          Singleton::class.java
        )
      }
    })

    return true
  }
}
