import 'package:flutter_test/flutter_test.dart';
import 'package:open_admin_app/utils/password_strength.dart';

void main() {
  test('loads zxcvbn on first use and scores weak and strong passwords', () async {
    expect(await passwordStrength('lingling'), lessThan(2));
    expect(await passwordStrength('kwong pastry, omu&co and always wonder at chago'), 4);
  });

  test('scores again once loaded', () async {
    expect(await passwordStrength('orm'), 0);
  });
}
