# OFREP — OpenFeature Remote Evaluation Protocol

FeatureHub's edge service implements the [OpenFeature Remote Evaluation
Protocol (OFREP)](https://openfeature.dev/docs/reference/other-technologies/ofrep/)
(v0.4.0). This lets **any** OpenFeature client use the community-maintained
[generic OFREP providers](https://openfeature.dev/docs/reference/protocols/ofrep)
against FeatureHub with no FeatureHub-specific SDK.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/ofrep/v1/evaluate/flags/{key}` | Evaluate a single flag for a context (server-side paradigm) |
| `POST` | `/ofrep/v1/evaluate/flags` | Evaluate all flags in the environment for a context (client-side paradigm); supports `If-None-Match`/`ETag` 304 |
| `GET` | `/ofrep/v1/configuration` | Provider metadata + capabilities |

## Authentication

Send a FeatureHub **server-evaluation** SDK API key via either header:

```
Authorization: Bearer <apiKey>
X-API-Key: <apiKey>
```

The key format is the standard SDK key: `[<named-cache>/]<environment-uuid>/<service-key>`.

- Missing/malformed keys, or keys that resolve to no environment/service
  account → `401`.
- **Client-evaluated keys** (containing `*`) are rejected with `403`: OFREP
  semantics require the server to evaluate, which client-evaluated keys
  cannot do.
- Unknown flag key (single evaluation) → `404` with body
  `{"key": "...", "errorCode": "FLAG_NOT_FOUND"}`.

## Evaluation semantics

- The request body is `{"context": {"targetingKey": ..., ...attributes}}`.
- `targetingKey` maps to FeatureHub's `userkey` (the default percentage key);
  a `session` or `sessionkey` attribute becomes FeatureHub's `sessionkey`.
  All other attributes are passed through as custom attributes (primitives or
  arrays of primitives; nested objects are rejected with `INVALID_CONTEXT`).
- Evaluation reuses the same engine as the edge `/features` endpoints
  (Dacha + `FeatureTransformer`), so rollout strategies, percentage
  rollouts, and attribute matchers behave identically.
- Reasons are synthesized: a matched rollout strategy produces
  `TARGETING_MATCH` (with the strategy id as `variant`), everything else
  `STATIC`. FeatureHub features with no value set produce a success entry
  with no `value` (OFREP `codeDefaultFlag`), telling the client to use its
  code default.
- JSON-typed flags are parsed into real JSON objects in the response.
- The bulk response carries an `ETag` of `"<environment-etag>//<context-hash>"`;
  send it back via `If-None-Match` to receive `304 Not Modified`.

## Implementation location

Everything lives in `backend/edge-common/src/main/kotlin/io/featurehub/edge/ofrep/`:

| File | Purpose |
|---|---|
| `OfrepResource.kt` | JAX-RS resource (`/ofrep/v1/*`) |
| `OfrepEvaluationService.kt` | Key parsing, auth decisions, Dacha + transformer calls |
| `OfrepMapper.kt` | OFREP↔FeatureHub context and response mapping, ETag |
| `OfrepDto.kt` | Wire models |
| `OfrepFeature.kt` | Jersey `Feature` + HK2 binder |

Registration touches two files: `EdgeCommonFeature` (registers the resource +
service) and `FeatureHubJerseyHost` (`jerseyPrefixes` now includes
`/ofrep/v1/*` so Grizzly routes those requests to Jersey).

It is registered once in `EdgeCommonFeature`, and the `/ofrep/v1/*` prefix is
added to `FeatureHubJerseyHost`'s `jerseyPrefixes` (the Grizzly→Jersey routing
list), so every deployment serves it: `edge-rest`, `edge-full`,
`party-server`, and `party-server-ish`.

Tests: `backend/edge-common/src/test/groovy/io/featurehub/edge/ofrep/`.
