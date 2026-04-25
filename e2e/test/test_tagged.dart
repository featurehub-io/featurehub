import 'package:ogurets/ogurets.dart';

/// this file is manually maintained, if you add new stepdefs, you must add them to this list.
void main(List<String> arguments) async {
  var def = new OguretsOpts()
    ..feature('test/features')
    ..tags(arguments.join(","))
    ..steps('test/steps');

  await def.run();
}
