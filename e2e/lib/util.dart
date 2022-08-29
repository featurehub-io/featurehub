import 'dart:io';

String baseUrl() {
  return Platform.environment['FEATUREHUB_BASE_URL'] ?? 'http://localhost:8903';
}
