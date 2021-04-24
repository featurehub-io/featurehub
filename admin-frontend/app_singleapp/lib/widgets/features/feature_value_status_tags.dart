import 'package:app_singleapp/utils/utils.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

import '../common/fh_tag.dart';

String _getFeatureValue(FeatureValue fv) {
  if (fv.valueString != null) {
    return fv.valueString.toString();
  }
  if (fv.valueBoolean != null) {
    return fv.valueBoolean.toString();
  }
  if (fv.valueJson != null) {
    return fv.valueJson.toString();
  }
  return fv.valueNumber.toString();
}

extension FeatureValueSet on FeatureValue {
  bool isSet(Feature feature) {
    switch (feature.valueType!) {
      case FeatureValueType.STRING:
        return valueString != null;
      case FeatureValueType.NUMBER:
        return valueNumber != null;
      case FeatureValueType.BOOLEAN:
        return valueBoolean != null;
      case FeatureValueType.JSON:
        return valueJson != null;
    }
  }
}

// this function is WRONG
@deprecated
bool isFVSet(FeatureValue fv) {
  return fv.valueString != null ||
      fv.valueJson != null ||
      fv.valueNumber != null ||
      fv.valueBoolean != null;
}

Widget inactiveTag(Feature feature, TextStyle textStyle) {
  return FHTagWidget(
      text: 'NOT SET', state: TagStatus.inactive, style: textStyle);
}

Widget activeTag(Feature feature, FeatureValue fv, TextStyle textStyle) {
  return FHTagWidget(
      text: '${condenseJson(_getFeatureValue(fv))}',
      state: TagStatus.active,
      style: textStyle);
}

Widget noAccessTag(TextStyle? textStyle) {
  return FHTagWidget(
      text: 'NO ACCESS', state: TagStatus.disabled, style: textStyle);
}
