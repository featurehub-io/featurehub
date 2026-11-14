import 'dart:io';
import 'dart:math' show Random;
import 'package:dio/dio.dart' show LogInterceptor;
import 'package:openapi_dart_common/openapi.dart';

String baseUrl() {
  return Platform.environment['FEATUREHUB_BASE_URL'] ?? 'http://localhost:8903';
}

String makeId(int length) {
  var result = "";
  final characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
  final charLen = characters.length;
  final ran = Random();
  for(var i = 0; i < length; i++) {
    result += characters[ran.nextInt(charLen)];
  }
  return result;
}

void checkLoggingForApiClient(ApiClient client) {
  if (Platform.environment['HTTP_LOGGING'] == 'debug') {
    (client.apiClientDelegate as DioClientDelegate).client.interceptors.add(
      LogInterceptor(
        request: true,
        requestHeader: true,
        requestBody: true,
        responseHeader: true,
        responseBody: true,
        error: true,
      ),
    );
  }

}
