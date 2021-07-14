import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:mrapi/api.dart';

void main() {
  // this occasionally causes problems, so lets keep a test for it
  test('Two people should be equal', () {
    const json =
        '{"id":{"id":"931b68a1-8b87-43a2-abcd-7d16ee94075b"},"name":"Alex","email":"some@hgh.com","version":1,"passwordRequiresReset":false}';
    final decoded = jsonDecode(json);
    final person1 = Person.fromJson(decoded);
    final person2 = Person.fromJson(decoded);
    expect(person1, person2);
  });
}
