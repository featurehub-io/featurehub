import 'dart:convert';

import 'package:mrapi/api.dart';

import 'host.dart';

class StrategyWu {
  final String name;
  final List<String> values;
  final String defaultValue;

  StrategyWu(this.name, this.values, this.defaultValue);
}

class FeatureWu {
  String name;

  FeatureValueType type = FeatureValueType.STRING;
  bool value = false;
  String sValue = null;

  List<StrategyWu> strategies = [];

  FeatureWu(this.name);

  @override
  String toString() {
    return "feature: ${name} - type ${type} - (if bool) ${value} - strategies: ${strategies}";
  }
}

class FeatureWuParser {
  List<dynamic> json;
  final Map<String, FeatureWu> features = {};

  FeatureWuParser(this.json);

  FeatureWuParser.raw(String json) {
    this.json = [];

    (jsonDecode(json) as Map<String, dynamic>).forEach((key, value) {
      if (value['featureFlags'] != null) {
        this.json = value['featureFlags'];
      }
    });
  }

  void parse() {
    json.forEach((jFeature) {
      String name = jFeature['name'];
      String value = jFeature['value'];

      final parts = name.split("\.");
      if (parts.isNotEmpty) {
        final feature = features[parts[0]] ?? FeatureWu(parts[0]);

        features[feature.name] = feature;

        if (parts.length > 1) {
          if (parts[1] == 'static') {
            // boolean, known case
            feature.type = FeatureValueType.BOOLEAN;

            if (parts.length == 3) {
              feature.value = (parts[2] == 'enabled' && value == 'true');
            } else {
              feature.value = (value == 'true');
            }
          } else if (parts[1] == 'enabled') {
            // enabled strategy
            if (parts.length == 2 && value == 'ALL') {
              // strategy is fully enabled
              feature.sValue = 'enabled';
            } else if (parts.length == 3 && value != null) {
              feature.strategies.add(
                  StrategyWu(parts[2], value.toString().split(","), 'enabled'));
              feature.sValue = 'disabled';
            }
          } else if (parts[1] == 'disabled') {
            if (parts.length == 2 && value == 'ALL') {
              feature.sValue = 'disabled';
              // strategy is fully disabled
              feature.strategies
                  .add(StrategyWu('disable', ['matching'], 'enabled'));
            } else if (parts.length == 3 && value != null) {
              feature.strategies.add(StrategyWu(
                  parts[2], value.toString().split(","), 'disabled'));
              feature.sValue = 'enabled';
            }
          } else {
            print("don't know what to do with $name -> $value");
          }
        }
      }
    });
  }
}

class FeaturesWuCommand {
  final dynamic json;
  final Host host;
  final FeatureServiceApi api;

  FeaturesWuCommand(this.host, this.json)
      : api = FeatureServiceApi(host.apiClient);

  Future<void> process(Portfolio portfolio, Application application) async {}

  Future<Feature> createFeature(
      Application app, String featureName, FeatureValueType type) async {
    final newFeature = Feature()
      ..name = featureName
      ..key = featureName
      ..valueType = type;

    final features = await api.createFeaturesForApplication(app.id, newFeature);

    return features.firstWhere((f) => f.name == featureName,
        orElse: () => null);
  }

  Future<Feature> findFeature(Application app, String featureName) async {
    final features = await api.getAllFeaturesForApplication(app.id);

    return features.firstWhere((f) => f.name == featureName,
        orElse: () => null);
  }
}
