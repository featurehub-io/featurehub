import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart';

// this represents a single strategy and allows us to track its state outside of the widget
class IndividualStrategyBloc extends Bloc {
  final EnvironmentFeatureValues environmentFeatureValue;
  final RolloutStrategy rolloutStrategy;
  final BehaviorSubject<List<RolloutStrategyAttribute>>
      _rolloutStartegyAttributeSource;

  Stream<List<RolloutStrategyAttribute>> get attributes =>
      _rolloutStartegyAttributeSource.stream;

  List<RolloutStrategyAttribute> get currentAttributes =>
      _rolloutStartegyAttributeSource.value;

  IndividualStrategyBloc(this.environmentFeatureValue, this.rolloutStrategy)
      : assert(environmentFeatureValue != null),
        assert(rolloutStrategy != null),
        assert(rolloutStrategy.attributes != null),
        _rolloutStartegyAttributeSource =
            BehaviorSubject<List<RolloutStrategyAttribute>>.seeded(
                rolloutStrategy.attributes) {
    print("Attributes are ${rolloutStrategy.attributes}");
  }

  void createAttribute({StrategyAttributeWellKnownNames type}) {
    final rs = RolloutStrategyAttribute()..fieldName = type?.name;

    if (type != null) {
      switch (type) {
        case StrategyAttributeWellKnownNames.device:
        case StrategyAttributeWellKnownNames.country:
        case StrategyAttributeWellKnownNames.platform:
        case StrategyAttributeWellKnownNames.userkey:
        case StrategyAttributeWellKnownNames.session:
          rs.type = RolloutStrategyFieldType.STRING;
          break;
        case StrategyAttributeWellKnownNames.version:
          rs.type = RolloutStrategyFieldType.SEMANTIC_VERSION;
          break;
      }
    }

    addAttribute(rs);
  }

  void addAttribute(RolloutStrategyAttribute rs) {
    rolloutStrategy.attributes.add(rs);
    _rolloutStartegyAttributeSource.add(rolloutStrategy.attributes);
    print("Attributes are ${rolloutStrategy.attributes}");
  }

  void updateStrategy(RolloutStrategyAttribute rs) {
    _rolloutStartegyAttributeSource.add(rolloutStrategy.attributes);
  }

  @override
  void dispose() {}
}
