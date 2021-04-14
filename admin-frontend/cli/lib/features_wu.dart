import 'package:mrapi/api.dart';

import 'host.dart';

class FeaturesWu {
  final dynamic json;
  final Host host;
  final FeatureServiceApi api;

  FeaturesWu(this.host, this.json) : api = FeatureServiceApi(host.apiClient);

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
