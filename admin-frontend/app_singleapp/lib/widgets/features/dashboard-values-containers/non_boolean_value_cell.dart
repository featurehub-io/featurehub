import 'package:app_singleapp/widgets/features/dashboard-values-containers/value_not_set_container.dart';
import 'package:app_singleapp/widgets/features/feature_value_status_tags.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:mrapi/api.dart';

class ValueCell extends StatelessWidget {
  final FeatureValue fv;
  final EnvironmentFeatureValues efv;
  final Feature feature;

  const ValueCell(
      {Key key, @required this.fv, @required this.efv, @required this.feature})
      : assert(efv != null),
        assert(feature != null),
        super(key: key);

  @override
  Widget build(BuildContext context) {
    if (efv.roles.isNotEmpty) {
      if (fv != null && fv.id != null && fv.isSet(feature)) {
        return _ConfigurationValueContainer(feature: feature, fv: fv);
      } else {
        return NotSetContainer();
      }
    }
    if ((fv == null || fv.id == null) && efv.roles.isEmpty) {
      return noAccessTag(null);
    }
    return SizedBox.shrink();
  }
}

class _ConfigurationValueContainer extends StatelessWidget {
  final Feature feature;
  final FeatureValue fv;

  const _ConfigurationValueContainer(
      {Key key, @required this.feature, @required this.fv})
      : assert(fv != null),
        assert(feature != null),
        super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
        width: 130,
        height: 30,
        padding: EdgeInsets.symmetric(vertical: 6.0, horizontal: 12.0),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.all(Radius.circular(16.0)),
          color: Colors.lightBlue,
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: <Widget>[
            Flexible(
              fit: FlexFit.tight,
              flex: 4,
              child: Text(_getValue(),
                  overflow: TextOverflow.ellipsis,
                  maxLines: 1,
                  style: GoogleFonts.openSans(
                      textStyle: Theme.of(context).primaryTextTheme.button)),
            ),
            fv.locked
                ? Flexible(
                    fit: FlexFit.tight,
                    flex: 1,
                    child: Padding(
                      padding: const EdgeInsets.only(left: 4.0, top: 2.0),
                      child: Icon(
                        Icons.lock_outline,
                        color: Colors.black54,
                        size: 12.0,
                      ),
                    ),
                  )
                : Container(),
          ],
        ));
  }

  String _getValue() {
    switch (feature.valueType) {
      case FeatureValueType.STRING:
        return fv.valueString ?? '';
      case FeatureValueType.NUMBER:
        return fv.valueNumber?.toString() ?? '';
      case FeatureValueType.BOOLEAN:
        return ''; // shouldn't happen
      case FeatureValueType.JSON:
        return fv.valueJson.replaceAll('\n', '') ?? '';
    }

    return '';
  }
}
