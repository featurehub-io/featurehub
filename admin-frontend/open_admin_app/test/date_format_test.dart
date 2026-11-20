import 'package:flutter_test/flutter_test.dart';
import 'package:open_admin_app/utils/date_format.dart';

void main() {
  test('formats a UTC date in local time', () {
    final utc = DateTime.parse('2026-11-20T09:15:30.123Z');
    final local = utc.toLocal();

    String two(int n) => n.toString().padLeft(2, '0');
    final expected = '${local.year}-${two(local.month)}-${two(local.day)} '
        '${two(local.hour)}:${two(local.minute)}:${two(local.second)}';

    expect(formatLocalDateTime(utc), expected);
  });

  test('a local date is formatted unchanged', () {
    final local = DateTime(2026, 1, 2, 3, 4, 5);
    expect(formatLocalDateTime(local), '2026-01-02 03:04:05');
  });
}
