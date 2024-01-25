import 'dart:convert';

import 'package:collection/collection.dart';
import 'package:mrapi/api.dart';

import 'host.dart';

class StrategyWu {
  final String name;
  final List<String> values;
  final String defaultValue;
  final RolloutStrategyAttributeConditional conditional;

  StrategyWu(this.name, this.values, this.defaultValue, this.conditional);
}

class FeatureWu {
  String name;

  FeatureValueType type = FeatureValueType.STRING;
  bool value = false;
  String? sValue = null;

  List<StrategyWu> strategies = [];

  FeatureWu(this.name);

  @override
  String toString() {
    return "feature: ${name} - type ${type} - (if bool) ${value} - strategies: ${strategies}";
  }
}

class FeatureWuParser {
  List<dynamic> json = [];
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
            } else if (parts.length == 3) {
              feature.strategies.add(StrategyWu(
                  parts[2],
                  value.toString().split(","),
                  'enabled',
                  RolloutStrategyAttributeConditional.INCLUDES));
              feature.sValue = 'disabled';
            }
          } else if (parts[1] == 'disabled') {
            if (parts.length == 2 && value == 'ALL') {
              feature.sValue = 'disabled';
              // strategy is fully disabled
              feature.strategies.add(StrategyWu('disable', ['matching'],
                  'enabled', RolloutStrategyAttributeConditional.EXCLUDES));
            } else if (parts.length == 3) {
              feature.strategies.add(StrategyWu(
                  parts[2],
                  value.toString().split(","),
                  'disabled',
                  RolloutStrategyAttributeConditional.INCLUDES));
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
  final String json;
  final Host host;
  final FeatureServiceApi api;
  final EnvironmentFeatureServiceApi envFeatApi;
  final EnvironmentServiceApi envApi;

  FeaturesWuCommand(this.host, this.json)
      : api = FeatureServiceApi(host.apiClient),
        envApi = EnvironmentServiceApi(host.apiClient),
        envFeatApi = EnvironmentFeatureServiceApi(host.apiClient);

  Future<void> process(Portfolio portfolio, Application application) async {
    final parser = FeatureWuParser.raw(this.json);

    parser.parse();

    if (parser.features.length == 0) {
      print("There were no features to parse");
      return;
    }

    print("checking features for ${application.name}");
    // make sure all of the feature types exist first
    for (final feat in parser.features.values) {
      var feature = await findFeature(application, feat.name);

      if (feature == null) {
        print("--> new feature for ${application.name} -> ${feat.name}");
        feature = await createFeature(application, feat.name, feat.type);
      }
    }

    // now find all the environments and start pushing the values of the features
    final envs =
        await envApi.findEnvironments(application.id, includeFeatures: true);

    for (final env in envs) {
      final featureVals = (await envFeatApi.getFeaturesForEnvironment(env.id))
          .featureValues
          .toList();

      for (final feat in parser.features.values) {
        final fv = featureVals.firstWhere((f) => f.key == feat.name,
            orElse: () => FeatureValue(key: feat.name, locked: false, rolloutStrategies: []));

        if (fv.id == null) {
          featureVals.add(fv);
        }

        if (feat.type == FeatureValueType.BOOLEAN) {
          fv.valueBoolean = feat.value;
        } else {
          fv.valueString = feat.sValue;
        }

        if (feat.strategies.isNotEmpty) {
          feat.strategies.forEach((readStrategy) {
            RolloutStrategy? existingRS = fv.rolloutStrategies!.firstWhereOrNull(
                (rs) =>
                    rs.attributes!.length == 1 &&
                    rs.attributes![0].fieldName == readStrategy.name,
                );

            var updatingRS = existingRS ?? RolloutStrategy(name: readStrategy.name);

            if (existingRS == null) {
              fv.rolloutStrategies!.add(updatingRS);
            }

            updatingRS.value = readStrategy.defaultValue;
            updatingRS.attributes = [
              RolloutStrategyAttribute(
                fieldName: readStrategy.name,
                type:RolloutStrategyFieldType.STRING,
                values: readStrategy.values,
                conditional: readStrategy.conditional)
            ];
          });
        }
      }

      print(
          "updating features for environment ${env.name} in application ${application.name}");
      await envFeatApi.updateAllFeaturesForEnvironment(env.id, featureVals);
    }
  }

  Future<Feature?> createFeature(
      Application app, String featureName, FeatureValueType type) async {
    final newFeature = CreateFeature(name: featureName, key: featureName, valueType: type);

    final features = await api.createFeaturesForApplication(app.id, newFeature);

    return features.firstWhereOrNull((f) => f.name == featureName);
  }

  Future<Feature?> findFeature(Application app, String featureName) async {
    final features = await api.getAllFeaturesForApplication(app.id);

    return features.firstWhereOrNull((f) => f.name == featureName);
  }
}
