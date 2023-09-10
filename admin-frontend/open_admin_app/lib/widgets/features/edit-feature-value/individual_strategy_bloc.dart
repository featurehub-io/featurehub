import 'package:bloc_provider/bloc_provider.dart';
import 'package:collection/collection.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart';

import '../../../utils/utils.dart';

// this represents a single strategy and allows us to track its state outside of the widget
class IndividualStrategyBloc extends Bloc {
  final RolloutStrategy rolloutStrategy;
  final BehaviorSubject<List<RolloutStrategyAttribute>>
      _rolloutStrategyAttributeSource;

  final BehaviorSubject<List<RolloutStrategyViolation>> _violationSource;
  Stream<List<RolloutStrategyViolation>> get violationStream =>
      _violationSource.stream;

  Stream<List<RolloutStrategyAttribute>> get attributes =>
      _rolloutStrategyAttributeSource.stream;

  List<RolloutStrategyAttribute> get currentAttributes =>
      _rolloutStrategyAttributeSource.value;

  IndividualStrategyBloc(this.rolloutStrategy)
      : _violationSource =
            BehaviorSubject<List<RolloutStrategyViolation>>.seeded([]),
        _rolloutStrategyAttributeSource =
            BehaviorSubject<List<RolloutStrategyAttribute>>.seeded(
                rolloutStrategy.attributes) {
    // ensure all attributes have a unique id
    for (var a in rolloutStrategy.attributes) {
      a.id ??= makeStrategyId();
    }
  }

  bool get isUnsavedStrategy =>
      (rolloutStrategy.id == null || rolloutStrategy.id == 'created');

  void createAttribute({StrategyAttributeWellKnownNames? type}) {
    final rs = RolloutStrategyAttribute(
      id: makeStrategyId(),
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
    rs.id ??= makeStrategyId();
    rolloutStrategy.attributes = [...rolloutStrategy.attributes, rs];
    _rolloutStrategyAttributeSource.add(rolloutStrategy.attributes);
  }

  void deleteAttribute(RolloutStrategyAttribute rs) {
    rolloutStrategy.attributes.remove(rs);
    _rolloutStrategyAttributeSource.add(rolloutStrategy.attributes);
  }

  void updateStrategy(RolloutStrategyAttribute rs) {
    _rolloutStrategyAttributeSource.add(rolloutStrategy.attributes);
  }

  @override
  void dispose() {}

  /// updates our list of violations and updates the stream
  void updateStrategyViolations(
      RolloutStrategyValidationResponse validationCheck,
      RolloutStrategy strategy) {
    var _violations = <RolloutStrategyViolation>[];

    final customViolations = validationCheck.customStategyViolations
        .firstWhereOrNull(
            (rs) => rs.strategy != null && rs.strategy!.id == strategy.id);

    if (customViolations != null && customViolations.violations.isNotEmpty) {
      _violations.addAll(customViolations.violations);
    }

    _violationSource.add(_violations);
  }
}
