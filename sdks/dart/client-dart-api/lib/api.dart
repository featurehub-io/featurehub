library featurehub_client_api.api;

/// This is the OpenAPI client for the FeatureHub SDK. It is not likely to be used on its own.

import 'dart:async';
import 'dart:convert';
import 'package:dio/dio.dart';
import 'package:openapi_dart_common/openapi.dart';
import 'package:collection/collection.dart';

part 'api_client.dart';

part 'api/feature_service_api.dart';

part 'model/feature_state.dart';
part 'model/feature_state_update.dart';
part 'model/feature_value_type.dart';
part 'model/role_type.dart';
part 'model/sse_result_state.dart';
part 'model/strategy.dart';
part 'model/strategy_name_type.dart';
part 'model/strategy_pair.dart';
