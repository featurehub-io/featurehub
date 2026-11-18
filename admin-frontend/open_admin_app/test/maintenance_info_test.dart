import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:open_admin_app/maintenance/maintenance_info.dart';

void main() {
  const message = 'ling and orm are keeping silent between 10:15 and 11:15';
  const start = '2026-11-20T10:15:30.00Z';
  const end = '2026-11-20T11:15:30.00Z';

  Headers headers(Map<String, String> values) =>
      Headers.fromMap(values.map((k, v) => MapEntry(k, [v])));

  test('complete maintenance headers produce maintenance info', () {
    final info = MaintenanceInfo.fromHeaders(headers({
      'x-maintenance-start': start,
      'x-maintenance-end': end,
      'x-maintenance-message': message,
    }));

    expect(info, isNotNull);
    expect(info!.message, message);
    expect(info.start, DateTime.parse(start));
    expect(info.end, DateTime.parse(end));
  });

  test('no maintenance headers produce no maintenance info', () {
    expect(MaintenanceInfo.fromHeaders(headers({'etag': 'kwong-pastry'})), isNull);
  });

  test('incomplete maintenance headers produce no maintenance info', () {
    expect(
        MaintenanceInfo.fromHeaders(
            headers({'x-maintenance-end': end, 'x-maintenance-message': message})),
        isNull);
    expect(
        MaintenanceInfo.fromHeaders(headers({
          'x-maintenance-start': 'always wonder',
          'x-maintenance-end': end,
          'x-maintenance-message': message,
        })),
        isNull);
  });

  test('the same announcement from two servers is equal so streams do not re-emit', () {
    final fromCustomerService = MaintenanceInfo.fromHeaders(headers({
      'x-maintenance-start': start,
      'x-maintenance-end': end,
      'x-maintenance-message': message,
    }));
    final fromManagementRepository = MaintenanceInfo(
        message: message, start: DateTime.parse(start), end: DateTime.parse(end));

    expect(fromCustomerService, fromManagementRepository);
    expect(fromCustomerService.hashCode, fromManagementRepository.hashCode);
    expect(fromCustomerService == MaintenanceInfo(message: 'chago', start: DateTime.parse(start), end: DateTime.parse(end)),
        isFalse);
  });
}
