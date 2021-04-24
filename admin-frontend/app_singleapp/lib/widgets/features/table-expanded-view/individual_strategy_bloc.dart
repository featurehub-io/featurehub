import 'package:bloc_provider/bloc_provider.dart';
import 'package:collection/collection.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart';

// this represents a single strategy and allows us to track its state outside of the widget
class IndividualStrategyBloc extends Bloc {
  final EnvironmentFeatureValues environmentFeatureValue;
  final RolloutStrategy rolloutStrategy;
  final BehaviorSubject<List<RolloutStrategyAttribute>>
      _rolloutStartegyAttributeSource;

  final BehaviorSubject<List<RolloutStrategyViolation>> _violationSource;
  Stream<List<RolloutStrategyViolation>> get violationStream =>
      _violationSource.stream;

  Stream<List<RolloutStrategyAttribute>> get attributes =>
      _rolloutStartegyAttributeSource.stream;

  List<RolloutStrategyAttribute> get currentAttributes =>
      _rolloutStartegyAttributeSource.value!;

  IndividualStrategyBloc(this.environmentFeatureValue, this.rolloutStrategy)
      : _violationSource =
            BehaviorSubject<List<RolloutStrategyViolation>>.seeded([]),
        _rolloutStartegyAttributeSource =
            BehaviorSubject<List<RolloutStrategyAttribute>>.seeded(
                rolloutStrategy.attributes) {
    var counter = 1;
    // ensure all attributes have a unique id
    rolloutStrategy.attributes.forEach((a) {
      a.id = (counter++).toString();
    });
  }

  bool get isUnsavedStrategy =>
      (rolloutStrategy.id == null || rolloutStrategy.id == 'created');

  void createAttribute({StrategyAttributeWellKnownNames? type}) {
    final rs = RolloutStrategyAttribute(
      id: DateTime.now().millisecond.toRadixString(16),
      fieldName: type?.name,
    );

    if (type != null) {
      switch (type) {
        case StrategyAttributeWellKnownNames.device:
        case StrategyAttributeWellKnownNames.country:
        case StrategyAttributeWellKnownNames.platform:
        case StrategyAttributeWellKnownNames.userkey:
        case StrategyAttributeWellKnownNames
            .session: //session is not used in the UI
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
    rs.id ??= DateTime.now().millisecond.toRadixString(16);
    rolloutStrategy.attributes.add(rs);
    _rolloutStartegyAttributeSource.add(rolloutStrategy.attributes);
  }

  void deleteAttribute(RolloutStrategyAttribute rs) {
    rolloutStrategy.attributes.remove(rs);
    _rolloutStartegyAttributeSource.add(rolloutStrategy.attributes);
  }

  void updateStrategy(RolloutStrategyAttribute rs) {
    _rolloutStartegyAttributeSource.add(rolloutStrategy.attributes);
  }

  @override
  void dispose() {}

  /// updates our list of violations and updates the stream
  void updateStrategyViolations(
      RolloutStrategyValidationResponse validationCheck) {
    var _violations = <RolloutStrategyViolation>[];

    final customViolations = validationCheck.customStategyViolations
        .firstWhereOrNull((rs) =>
            rs.strategy != null && rs.strategy!.id == rolloutStrategy.id);

    if (customViolations != null && customViolations.violations.isNotEmpty) {
      _violations.addAll(customViolations.violations);
    }

    _violationSource.add(_violations);
  }
}
