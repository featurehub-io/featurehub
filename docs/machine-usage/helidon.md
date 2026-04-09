# Migration Plan: Grizzly + HK2 → Helidon 4

## Strategy: Helidon SE 4 + Jersey (keep HK2)

Helidon SE 4 ships `helidon-webserver-jersey`, which wraps Helidon's Netty-based WebServer with a full JAX-RS (Jersey 3.x) layer. Critically, Jersey continues to use **HK2 as its DI container**, which means:

- All `AbstractBinder` bindings survive unchanged
- All `ServiceLocator` injections survive unchanged
- `@Immediate` scope and `ServiceLocatorUtilities.enableImmediateScope` survive unchanged
- All JAX-RS `Feature` / `FeatureContext` registration patterns survive unchanged
- All `@Path`, `@GET`, `@Inject` annotations survive unchanged
- `ContainerLifecycleListener` survives (Jersey SPI, not Grizzly)

The migration is **primarily a server-bootstrap replacement**, concentrated in `backend/common-web`. The DI layer, resource classes, messaging, database, and config layers are untouched.

---

## Phase 1 — Dependency Replacement

**Files:** `composite-app/pom.xml`, `composite-jersey/pom.xml`, `tile-app/tile.xml`, `composite-parent/pom.xml`

### 1a. Add Helidon BOM to `composite-parent/pom.xml`

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.helidon</groupId>
      <artifactId>helidon-bom</artifactId>
      <version>4.2.x</version>  <!-- pin to latest 4.x stable -->
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

Also add a `<helidon.version>` property.

### 1b. Replace Grizzly in `composite-app/pom.xml`

Remove:
```xml
grizzly-http-server, grizzly-http2, grizzly-npn-bootstrap, grizzly-npn-api
jersey-container-grizzly2-http (both occurrences)
```

Add:
```xml
<dependency>
  <groupId>io.helidon.webserver</groupId>
  <artifactId>helidon-webserver-jersey</artifactId>
</dependency>
<dependency>
  <groupId>io.helidon.webserver</groupId>
  <artifactId>helidon-webserver-static-content</artifactId>
</dependency>
```

Remove the `<grizzly.version>` and `<grizzly.npn.version>` properties.

### 1c. Replace in `composite-jersey/pom.xml`

Remove:
```xml
jersey-container-grizzly2-http
```

Helidon's Jersey integration brings its own container adapter — no Grizzly container needed.

### 1d. Update JVM flags in `tile-app/tile.xml`

Remove the Grizzly-era `--add-opens=java.base/java.nio=ALL-UNNAMED` and `--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED` flags (Grizzly needed these; Helidon on virtual threads with Netty does not require them). Verify with a test build — some may still be needed by other libraries.

---

## Phase 2 — Rewrite `FeatureHubJerseyHost`

**File:** `backend/common-web/src/main/kotlin/io/featurehub/jersey/FeatureHubJerseyHost.kt`

This is the only file with direct Grizzly API calls. Replace wholesale.

**Current behaviour to preserve:**
- Binds to `0.0.0.0:${server.port}` (default 8903)
- Mounts Jersey under configurable prefixes (default: `/mr-api/*`, `/features*`, etc.) plus always `/metrics` and `/health/*`
- Supports an optional URL path offset (`featurehub.url-path`) for sub-path deployments
- Optionally serves static web assets when `run.nginx` property is set
- Graceful shutdown via `ApplicationLifecycleManager`
- HTTP/2 enabled

**New implementation sketch:**

```kotlin
class FeatureHubJerseyHost(private val config: ResourceConfig) {
  @ConfigKey("server.port")
  var port: Int? = 8903
  @ConfigKey("server.gracePeriodInSeconds")
  var gracePeriod: Int? = 10
  var allowedWebHosting = true

  init {
    DeclaredConfigResolver.resolve(this)
    val prefixes = FallbackPropertyConfig.getConfig("jersey.prefixes")
    if (prefixes != null) jerseyPrefixes = prefixes.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    config.register(CommonFeatureHubFeatures::class.java)
    LifecycleListeners.shutdown(DrainExecutorPool::class.java, config)
  }

  fun start(overridePort: Int = port!!): FeatureHubJerseyHost {
    val offsetPath = FallbackPropertyConfig.getConfig("featurehub.url-path", "/")
    val contextPath = if (offsetPath.endsWith("/")) offsetPath.dropLast(1) else offsetPath

    val jerseySupport = JerseySupport.create(config)

    val routingBuilder = HttpRouting.builder()

    // Static assets (if enabled)
    if (allowedWebHosting && FallbackPropertyConfig.getConfig("run.nginx") != null) {
      val staticSupport = StaticContentSupport.builder("/web")
        .welcomeFileName("index.html")
        .build()
      routingBuilder.register("$contextPath/", staticSupport)
    }

    // Always expose metrics and health outside context path too if offset
    if (contextPath.isNotEmpty() && contextPath != "/") {
      routingBuilder.register("/metrics", jerseySupport)
      routingBuilder.register("/health", jerseySupport)
    }

    // Jersey under context path
    routingBuilder.register(contextPath.ifEmpty { "/" }, jerseySupport)

    val server = WebServer.builder()
      .address("0.0.0.0")
      .port(overridePort)
      .shutdownGracePeriod(Duration.ofSeconds(gracePeriod!!.toLong()))
      .addRouting(routingBuilder.build())
      .build()
      .start()

    ApplicationLifecycleManager.registerListener { trans ->
      if (trans.next == LifecycleStatus.TERMINATING) {
        server.stop()
      }
    }

    log.info("server started on http://0.0.0.0:{}{}", overridePort, offsetPath)
    return this
  }
}
```

**Key differences from current:**
- `JerseySupport.create(config)` replaces `HttpGrizzlyContainer.makeHandler(config)`
- `WebServer.builder()` replaces `HttpServer` + `NetworkListener`
- `server.stop()` replaces `server.shutdown(gracePeriod, TimeUnit.SECONDS).get()` — Helidon stop is synchronous with a grace period configurable on the WebServer builder
- HTTP/2: Helidon 4 enables h2c (cleartext HTTP/2) by default when the client negotiates it — no explicit addon required
- The Grizzly `HttpHandlerChain` multi-handler approach is replaced by Helidon routing which resolves paths in registration order

---

## Phase 3 — Replace Static Asset Handler

**File:** `backend/common-web/src/main/kotlin/io/featurehub/jersey/AdminAppStaticHttpHandler.kt`

Currently extends Grizzly's `StaticHttpHandler` to:
- Serve files from a configurable filesystem path
- Rewrite `Cache-Control` headers per asset type (`.js`, `.css` get long cache; `index.html` gets no-cache)
- Handle base-path rewrites for sub-path deployments

**New approach:** Helidon's `StaticContentSupport` handles basic serving, but the custom cache headers and path rewriting logic needs a thin Helidon `Handler` (or `ServerFilter`) wrapping it.

```kotlin
// Replace AdminAppStaticHttpHandler with a HelidonStaticContentHandler
// that wraps StaticContentSupport and post-processes Cache-Control
class FeatureHubStaticContentHandler(private val basePath: String) : HttpService {
  override fun routing(rules: HttpRules) {
    rules.any { req, res ->
      // apply cache-control header logic before delegating
      val path = req.path().rawPath()
      when {
        path.endsWith(".html") -> res.headers().set(CACHE_CONTROL, "no-cache, no-store")
        path.matches(Regex(".*\\.(js|css|woff2?)$")) ->
          res.headers().set(CACHE_CONTROL, "public, max-age=31536000, immutable")
      }
      req.next()
    }
    rules.register(StaticContentSupport.create(Path.of(basePath)))
  }
}
```

Delete `DelegatingHandler.kt` — it was only needed to wrap a Grizzly `StaticHttpHandler` inside a `HttpHandlerChain`.

---

## Phase 4 — Update `MetricsHealthRegistration`

**File:** `backend/common-web/src/main/kotlin/io/featurehub/health/MetricsHealthRegistration.kt`

The `startMetricsEndpoint` method creates a second `FeatureHubJerseyHost` on the monitor port. This works cleanly with the new `FeatureHubJerseyHost` because:

- `FeatureHubJerseyHost.start(port)` now accepts a port override
- Helidon starts two independent `WebServer` instances on two ports

No structural change to `MetricsHealthRegistration` is needed — it already calls `FeatureHubJerseyHost(resourceConfig).disallowWebHosting().start(port)`. That call works identically with the new implementation.

The one change: the `ContainerLifecycleListener` (lines 95–110) used to get the `ServiceLocator` from Jersey's `container.applicationHandler.injectionManager`. This is standard Jersey SPI and **still works** with `helidon-webserver-jersey` since Jersey's injection manager is still HK2. Verify it works in practice.

---

## Phase 5 — Audit Third-party Jersey Libraries

These cd.connect and openapi-support libraries wrap Jersey and must be verified for compatibility with the Helidon-hosted Jersey:

| Library | Role | Expected Status |
|---|---|---|
| `connect-jersey-common` (2.1) | Logging filters, CORS, `LoggingConfiguration` | Should work — pure JAX-RS filters |
| `connect-prometheus-jersey` (4.3) | Prometheus `JerseyPrometheusResource` | Should work — JAX-RS resource + filter |
| `openapi-jersey3-support` (2.5) | OpenAPI generated resource base classes | Should work — Jersey annotations only |
| `cd.connect app-config` | `@ConfigKey`, `DeclaredConfigResolver` | Unaffected — plain Java field injection |

The risk area is if any of these libraries instantiate or depend on a `GrizzlyHttpContainer` directly. Search after Phase 2 to confirm no stray Grizzly references remain:

```sh
grep -r "grizzly" backend/ --include="*.kt" --include="*.java" -l
```

---

## Phase 6 — SSE (Server-Sent Events)

The Edge server uses `jersey-media-sse` for the SSE streaming endpoint to SDK clients. Helidon SE 4 also has native SSE support, but since the code uses Jersey's `@ServerSentEvents` JAX-RS API (via `jersey-media-sse`), it should work unchanged with `helidon-webserver-jersey`. However, Helidon WebServer's backpressure handling differs from Grizzly's — **load test the SSE endpoint** specifically after migration. If issues arise, the fallback is migrating the SSE endpoint to Helidon's native `SseSupport`.

---

## Phase 7 — HTTP/2 Verification

Grizzly required `Http2AddOn` + Grizzly NPN to support HTTP/2 over cleartext (h2c). In Helidon 4 SE:

- HTTP/2 over TLS (h2 via ALPN) is automatic when TLS is configured
- HTTP/2 cleartext (h2c) is supported via `Http2Config` on the WebServer

Add to the `WebServer.builder()` if h2c is required:
```kotlin
.addProtocol(Http2Config.create())
```

FeatureHub is typically deployed behind a TLS-terminating proxy (nginx/Kubernetes ingress), so h2c inside the cluster is unlikely to be needed by external clients. Verify whether any downstream service actually sends h2c prior frames.

---

## Phase 8 — Graceful Shutdown Tuning

Current code: `server.shutdown(gracePeriod, TimeUnit.SECONDS).get()`

Helidon 4 WebServer has `shutdownTimeout` configurable at build time:
```kotlin
WebServer.builder()
  .shutdownGracePeriod(Duration.ofSeconds(gracePeriod!!.toLong()))
  ...
```

`server.stop()` then honours this grace period. The `ApplicationLifecycleManager` listener in `FeatureHubJerseyHost` calls `server.stop()` and already runs on a shutdown thread, so no change needed to the lifecycle framework itself.

---

## Summary of Files Changed

| File | Change Type | Effort |
|---|---|---|
| `composite-parent/pom.xml` | Add Helidon BOM | Trivial |
| `composite-app/pom.xml` | Remove Grizzly, add Helidon | Small |
| `composite-jersey/pom.xml` | Remove `jersey-container-grizzly2-http` | Trivial |
| `tile-app/tile.xml` | Remove Grizzly JVM flags | Trivial |
| `FeatureHubJerseyHost.kt` | Full rewrite (~170 → ~80 lines) | Medium |
| `AdminAppStaticHttpHandler.kt` | Full rewrite to Helidon HttpService | Small |
| `DelegatingHandler.kt` | Delete | Trivial |
| `MetricsHealthRegistration.kt` | Verify `ContainerLifecycleListener` still works | Small |

**Files with zero changes:** All `AbstractBinder` Feature classes, all JAX-RS resource classes, all lifecycle listener implementations, all Ebean/database code, all eventing/NATS code, all cd.connect config code, all `Application.kt` entry points.

---

## Risk Areas (in priority order)

1. **`ContainerLifecycleListener` getting `ServiceLocator`** — used in `MetricsHealthRegistration` to bootstrap the second monitoring port. Test this path explicitly.
2. **SSE backpressure** — heavy SSE load at the Edge server; load test before production cutover.
3. **Third-party Jersey wrappers** — `connect-prometheus-jersey`, `connect-jersey-common` — run integration tests after dep swap.
4. **URL path offset (`featurehub.url-path`)** — the routing registration order in Helidon matters for catch-all vs. specific routes; test sub-path deployments.
5. **h2c** — if any service-to-service communication relies on cleartext HTTP/2, explicitly enable `Http2Config` on the WebServer.
