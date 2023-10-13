import 'package:mrapi/api.dart';
import 'package:open_admin_app/utils/utils.dart';

extension EditingTransformRS on RolloutStrategy {
  EditingRolloutStrategy toEditing() {
    return EditingRolloutStrategy.fromRolloutStrategy(this);
  }
}

extension EditingTransformFS on GroupRolloutStrategy {
  EditingRolloutStrategy toEditing() {
    return EditingRolloutStrategy.fromGroupRolloutStrategy(this, null);
  }
}

/// this represents a rollout strategy attribute that we are _EDITING_ - as such, it
/// can have null for any and all of its fields. However the API version *cannot*
/// have null for any of its fields

class EditingRolloutStrategyAttribute {
  RolloutStrategyAttributeConditional? conditional;

  String? fieldName;
  /* the value(s) associated with this rule */
  List<dynamic> values = [];

  RolloutStrategyFieldType? type;
  /* A temporary id used only when validating. Saving strips these out as they are not otherwise necessary */
  String id;


  @override
  String toString() {
    return "ersa: field $fieldName, values: $values, type: $type, id: $id, conditional: $conditional";
  }

  EditingRolloutStrategyAttribute({
      this.conditional, this.fieldName, required this.values, this.type, required this.id});

  RolloutStrategyAttribute? toRolloutStrategyAttribute() {
    if (conditional == null || fieldName == null || type == null) return null;
    return RolloutStrategyAttribute(conditional: conditional!, fieldName: fieldName!, type: type!, id: id, values: values);
  }

  EditingRolloutStrategyAttribute copy() {
    return EditingRolloutStrategyAttribute(conditional: conditional, fieldName: fieldName, values: values.toList(), type: type, id: id);
  }

  List<RolloutStrategyViolation> violations() {
    final violations = <RolloutStrategyViolation>[];

    if (fieldName == null) {
      violations.add(RolloutStrategyViolation(violation:  RolloutStrategyViolationType.attrMissingFieldName, id: id));
    }
    if (conditional == null) {
      violations.add(RolloutStrategyViolation(violation:  RolloutStrategyViolationType.attrMissingConditional, id: id));
    }
    if (type == null) {
      violations.add(RolloutStrategyViolation(violation:  RolloutStrategyViolationType.attrMissingFieldType, id: id));
    }
    if (values.isEmpty) {
      violations.add(RolloutStrategyViolation(violation:  RolloutStrategyViolationType.attrMissingValue, id: id));
    }

    return violations;
  }


  static EditingRolloutStrategyAttribute fromRolloutStrategyAttribute(RolloutStrategyAttribute rsa) {
    return EditingRolloutStrategyAttribute(values: rsa.values, conditional: rsa.conditional, fieldName: rsa.fieldName, type: rsa.type, id: rsa.id!);
  }
}

class EditingRolloutStrategy {
  String id;
  /* value between 0 and 1000000 - for four decimal places */
  int? percentage;
  /* if you don't wish to apply percentage based on user id, you can use one or more attributes defined here */
  List<String>? percentageAttributes;

  List<EditingRolloutStrategyAttribute> attributes = [];
  /* names are unique in a case insensitive fashion */
  String? name;

  bool saved;

  EditingRolloutStrategy({required this.id, required this.saved, this.percentage, this.percentageAttributes,
      required this.attributes, this.name});

  double get maxPercentage => 1000000.0;
  double get percentageMultiplier => maxPercentage / 100.0;

  set percentageFromText(String p) => percentage = p.trim().isEmpty
      ? null
      : (double.parse(p) * percentageMultiplier).round();

  String get percentageText =>
      percentage == null ? '' : (percentage! / percentageMultiplier).toString();


  @override
  String toString() {
    return "name: $name, id: $id, percentage $percentage, percentage attr: $percentageAttributes, $attributes";
  }

  static EditingRolloutStrategy fromRolloutStrategy(RolloutStrategy rs) {
    return EditingRolloutStrategy(
        id: rs.id!,
        saved: true,
        percentage: rs.percentage, percentageAttributes: rs.percentageAttributes,
        name: rs.name,
        attributes: (rs.attributes ?? []).map((e) => EditingRolloutStrategyAttribute.fromRolloutStrategyAttribute(e)).toList()
    );
  }

  RolloutStrategy? toRolloutStrategy(dynamic value) {
    if (name == null || attributes.any((rsa) => rsa.toRolloutStrategyAttribute() == null)) return null;
    return RolloutStrategy(id: id, name: name!, value: value, percentage: percentage, percentageAttributes: percentageAttributes, attributes: attributes.map((e) => e.toRolloutStrategyAttribute()!).toList());
  }

  EditingRolloutStrategy copy({String? id, int? percentage, List<String>? percentageAttributes,
    dynamic value, List<EditingRolloutStrategyAttribute>? attributes, String? name}) {
    return EditingRolloutStrategy(id: id ?? this.id, saved: saved, attributes: (attributes ?? this.attributes).map((e) => e.copy()).toList(),
        percentage: percentage ?? this.percentage, percentageAttributes: percentageAttributes ?? this.percentageAttributes, name: name ?? this.name);
  }

  List<RolloutStrategyViolation> violations() {
    final violations = <RolloutStrategyViolation>[];

    if (name == null) {
      violations.add(RolloutStrategyViolation(violation:  RolloutStrategyViolationType.noName));
    }

    for (var rsa in attributes) { violations.addAll(rsa.violations()); }

    return violations;
  }

  static EditingRolloutStrategy fromGroupRolloutStrategy(GroupRolloutStrategy rs, dynamic value) {
    return EditingRolloutStrategy(
        id: rs.id!,
        saved: true,
        percentage: rs.percentage, percentageAttributes: rs.percentageAttributes,
        name: rs.name,
        attributes: (rs.attributes ?? []).map((e) => EditingRolloutStrategyAttribute.fromRolloutStrategyAttribute(e)).toList()
    );
  }

  GroupRolloutStrategy? toGroupRolloutStrategy() {
    return GroupRolloutStrategy(name: name!, id: id, percentage: percentage, percentageAttributes: percentageAttributes, attributes: attributes.map((e) => e.toRolloutStrategyAttribute()!).toList());
  }

  static EditingRolloutStrategy newStrategy({int? percentage, String? id, List<String>? percentageAttributes,
    List<EditingRolloutStrategyAttribute>? attributes, String? name}) {
    return EditingRolloutStrategy(id: id ?? makeStrategyId(), saved: false, attributes: attributes ?? [],
        name: name, percentageAttributes: percentageAttributes, percentage: percentage);
  }
}
