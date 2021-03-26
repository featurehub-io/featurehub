import 'package:codemod/test.dart';
import 'package:openapi_migrate/openapi_suggestor.dart';
import 'package:test/test.dart';

void main() {
  test('recognizes and fixes known type with new being removed', () async {
    final context = await fileContextForTest('test.dart', '''
import 'package:mrapi/api.dart';

class Test {

  Test() {
    final fred = new UserCredentials()
        ..email = 'fred'
        ..password = 'mary';
  }
}''');
    final expectedOutput = '''
import 'package:mrapi/api.dart';

class Test {

  Test() {
    final fred = UserCredentials(email: 'fred', password: 'mary', );
  }
}''';
    expectSuggestorGeneratesPatches(
        OpenPatchSuggestor(), context, expectedOutput);
  });

  test('recognizes and fixes known type with no new being removed', () async {
    final context = await fileContextForTest('test.dart', '''
import 'package:mrapi/api.dart';

class Test {

  Test() {
    final fred = UserCredentials()
        ..email = 'fred'
        ..password = 'mary';
  }
}''');
    final expectedOutput = '''
import 'package:mrapi/api.dart';

class Test {

  Test() {
    final fred = UserCredentials(email: 'fred', password: 'mary', );
  }
}''';
    expectSuggestorGeneratesPatches(
        OpenPatchSuggestor(), context, expectedOutput);
  });

  test('recognizes and fixes known type with complex expressions', () async {
    final context = await fileContextForTest('test.dart', '''
import 'package:mrapi/api.dart';

class Test {

  Test() {
    final email = 'fred';
    final fred = new UserCredentials()
        ..email = email
        ..password = 'mary' + email;
  }
}''');
    final expectedOutput = '''
import 'package:mrapi/api.dart';

class Test {

  Test() {
    final email = 'fred';
    final fred = UserCredentials(email: email, password: 'mary' + email, );
  }
}''';
    expectSuggestorGeneratesPatches(
        OpenPatchSuggestor(), context, expectedOutput);
  });
}
