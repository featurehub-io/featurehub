library featurehub_client_api.api;

/// This is the OpenAPI client for the FeatureHub SDK. It is not likely to be used on its own.

import 'dart:async';
import 'dart:convert';
import 'package:dio/dio.dart';
import 'package:openapi_dart_common/openapi.dart';
import 'package:collection/collection.dart';

part 'api_client.dart';

part 'api/feature_service_api.dart';

part 'model/environment.dart';
part 'model/feature_state.dart';
part 'model/feature_state_update.dart';
part 'model/feature_value_type.dart';
part 'model/role_type.dart';
part 'model/rollout_strategy.dart';
part 'model/rollout_strategy_attribute.dart';
part 'model/rollout_strategy_attribute_conditional.dart';
part 'model/rollout_strategy_field_type.dart';
part 'model/sse_result_state.dart';
part 'model/strategy_attribute_country_name.dart';
part 'model/strategy_attribute_device_name.dart';
part 'model/strategy_attribute_platform_name.dart';
part 'model/strategy_attribute_well_known_names.dart';
