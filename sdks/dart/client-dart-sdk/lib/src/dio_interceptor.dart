import 'dart:math';

import 'package:dio/dio.dart';
import 'package:featurehub_client_api/api.dart';
import 'package:featurehub_client_sdk/featurehub.dart';

// this is the standard W3C Trace Context header that most Tracing libraries
// support. https://www.w3.org/TR/trace-context-1/

class W3CTraceContextInterceptor extends InterceptorsWrapper {
  int _startTraceId;
  int _spanId;

  W3CTraceContextInterceptor() {
    _startTraceId =
        Random.secure().nextInt(1 << 27); // gives us space to expand
    _spanId = Random.secure().nextInt(1 << 14);
  }

  @override
  Future onRequest(RequestOptions options) {
    final traceId = _startTraceId.toRadixString(16).padLeft(16, '0');

    _startTraceId++;

    final spanId = _spanId.toRadixString(16).padLeft(8, '0');
    final flags = '01'; // all calls sampled

    options.headers['traceparent'] = '00-${traceId}-${spanId}-${flags}';

    return super.onRequest(options);
  }
}

/// use this class if you wish to pass the state of your features with
/// each request as an opentracing header that is compatible with Jaeger.
/// this is called Baggage in OpenTracing and DistributedContext in OpenTelemetry
class JaegerDioInterceptor extends W3CTraceContextInterceptor {
  final ClientFeatureRepository repository;

  JaegerDioInterceptor(this.repository);

  @override
  Future onRequest(RequestOptions options) {
    repository.availableFeatures.forEach((key) {
      final fs = repository.getFeatureState(key);
      if (fs.type != FeatureValueType.JSON && fs.value != null) {
        options.headers[
                'uberctx-fhub.${key.toLowerCase().replaceAll(':', '_')}'] =
            Uri.encodeQueryComponent(fs.value);
      }
    });

    return super.onRequest(options);
  }
}
