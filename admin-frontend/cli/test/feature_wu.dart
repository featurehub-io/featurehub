import 'package:fh_cli/features_wu.dart';
import 'package:mrapi/api.dart';
import 'package:test/test.dart';

main() {
  test('FeatureWu parses as expected', () {
    final parser = FeatureWuParser.raw('''
{
  "digital-featureflag": {
    "featureFlags": [
      {
        "name": "XYZ.enabled.countries",
        "value": "US,CA,AU"
      },
      {
        "name": "ABC.disabled.channels",
        "value": "IPH"
      },
      {
        "name": "RANDOM.enabled.countries",
        "value": "US,CA,AU,FR"
      },
      {
        "name": "SOME_FEATURE.enabled",
        "value": "ALL"
      },
      {
        "name": "SOME_OTHER_FEATURE.static.enabled",
        "value": "false"
      },
      {
        "name": "SOME_TRUE_FEATURE.static.enabled",
        "value": "true"
      }
    ],
    "flagVersion": "cs-config-version-stage"
  }
}
  ''');

    parser.parse();
    expect(parser.features.length, 6);
    expect(parser.features['XYZ'].sValue, 'disabled');
    expect(parser.features['XYZ'].strategies[0].defaultValue, 'enabled');
    expect(parser.features['XYZ'].strategies[0].name, 'countries');
    expect(parser.features['XYZ'].strategies[0].values, ['US', 'CA', 'AU']);
    expect(parser.features['XYZ'].type, FeatureValueType.STRING);
    expect(parser.features['ABC'].sValue, 'enabled');
    expect(parser.features['ABC'].strategies[0].defaultValue, 'disabled');
    expect(parser.features['ABC'].strategies[0].name, 'channels');
    expect(parser.features['ABC'].strategies[0].values, ['IPH']);
    expect(parser.features['ABC'].type, FeatureValueType.STRING);
    expect(parser.features['SOME_FEATURE'].sValue, 'enabled');
    expect(parser.features['SOME_FEATURE'].strategies.length, 0);
    expect(parser.features['SOME_FEATURE'].type, FeatureValueType.STRING);
    expect(
        parser.features['SOME_OTHER_FEATURE'].type, FeatureValueType.BOOLEAN);
    expect(parser.features['SOME_OTHER_FEATURE'].value, false);
    expect(parser.features['SOME_TRUE_FEATURE'].type, FeatureValueType.BOOLEAN);
    expect(parser.features['SOME_TRUE_FEATURE'].value, true);
  });
}
