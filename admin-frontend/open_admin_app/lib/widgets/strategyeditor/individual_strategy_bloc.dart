import 'package:bloc_provider/bloc_provider.dart';
import 'package:collection/collection.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/utils/utils.dart';
import 'package:open_admin_app/widgets/strategyeditor/editing_rollout_strategy.dart';
import 'package:open_admin_app/widgets/strategyeditor/strategy_editor_provider.dart';
import 'package:rxdart/rxdart.dart';


// this represents a single strategy and allows us to track its state outside of the widget
class StrategyEditorBloc extends Bloc {
  final StrategyEditorProvider strategyEditorProvider;
  final EditingRolloutStrategy rolloutStrategy;
  final BehaviorSubject<List<EditingRolloutStrategyAttribute>>
      _rolloutStrategyAttributeSource;

  final BehaviorSubject<List<RolloutStrategyViolation>> _violationSource;
  Stream<List<RolloutStrategyViolation>> get violationStream =>
      _violationSource.stream;

  Stream<List<EditingRolloutStrategyAttribute>> get attributes =>
      _rolloutStrategyAttributeSource.stream;

  List<EditingRolloutStrategyAttribute> get currentAttributes =>
      _rolloutStrategyAttributeSource.value;

  StrategyEditorBloc(this.rolloutStrategy, this.strategyEditorProvider)
      : _violationSource =
            BehaviorSubject<List<RolloutStrategyViolation>>.seeded([]),
        _rolloutStrategyAttributeSource =
            BehaviorSubject<List<EditingRolloutStrategyAttribute>>.seeded(
                rolloutStrategy.attributes);

  void createAttribute({StrategyAttributeWellKnownNames? type}) {
    final rs = EditingRolloutStrategyAttribute(
      id: makeStrategyId(),
      fieldName: type?.name,
      values: []
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

  void addAttribute(EditingRolloutStrategyAttribute rs) {
    rs.id ??= makeStrategyId();
    rolloutStrategy.attributes = [...rolloutStrategy.attributes, rs];
    _rolloutStrategyAttributeSource.add(rolloutStrategy.attributes);
  }

  void deleteAttribute(EditingRolloutStrategyAttribute rs) {
    rolloutStrategy.attributes.remove(rs);
    _rolloutStrategyAttributeSource.add(rolloutStrategy.attributes);
  }

  void updateStrategy(EditingRolloutStrategyAttribute rs) {
    _rolloutStrategyAttributeSource.add(rolloutStrategy.attributes);
  }

  @override
  void dispose() {}

  void updateLocalViolations(List<RolloutStrategyViolation> violations) {
    _violationSource.add(violations);
  }

  /// updates our list of violations and updates the stream

  void updateStrategyViolations(
      RolloutStrategyValidationResponse validationCheck) {
    var _violations = <RolloutStrategyViolation>[];

    final customViolations = validationCheck.customStategyViolations
        .firstWhereOrNull(
            (rs) => rs.strategy.id == rolloutStrategy.id);

    if (customViolations != null && customViolations.violations.isNotEmpty) {
      _violations.addAll(customViolations.violations);
    }

    _violationSource.add(_violations);
  }
}
