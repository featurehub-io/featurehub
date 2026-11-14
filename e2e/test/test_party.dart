import 'package:ogurets/ogurets.dart';

/// this file is manually maintained, if you add new stepdefs, you must add them to this list.
void main(List<String>? args) async {
  var def = new OguretsOpts()
    ..feature('test/features')
    ..steps('test/steps');

  await def.run(args: args);
}
