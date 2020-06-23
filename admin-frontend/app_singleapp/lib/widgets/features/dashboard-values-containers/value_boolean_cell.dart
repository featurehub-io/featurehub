import 'package:app_singleapp/widgets/features/dashboard-values-containers/value_not_set_container.dart';
import 'package:app_singleapp/widgets/features/feature_value_status_tags.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:mrapi/api.dart';

class BooleanCell extends StatelessWidget {
  final FeatureValue fv;
  final EnvironmentFeatureValues efv;
  final Feature feature;

  const BooleanCell(
      {Key key, @required this.fv, @required this.efv, @required this.feature})
      : assert(efv != null),
        assert(feature != null),
        super(key: key);

  @override
  Widget build(BuildContext context) {
    if (efv.roles != null && efv.roles.isNotEmpty) {
      if (fv != null && fv.isSet(feature)) {
        return BooleanContainer(feature, fv);
      } else {
        return NotSetContainer();
      }
    }
    if (fv == null && efv.roles.isEmpty) {
      return noAccessTag(null);
    }
    return SizedBox.shrink();
  }
}

class BooleanContainer extends StatelessWidget {
  final Feature feature;
  final FeatureValue fv;

  BooleanContainer(this.feature, this.fv);

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: Alignment.centerLeft,
      child: Container(
          width: 70,
          height: 30,
          padding: EdgeInsets.symmetric(vertical: 6.0, horizontal: 12.0),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.all(Radius.circular(16.0)),
            color: fv.valueBoolean ? Color(0xff11C8B5) : Color(0xffF44C49),
          ),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.center,
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              Text(fv.valueBoolean ? 'ON' : 'OFF',
                  style: GoogleFonts.openSans(
                      textStyle: Theme.of(context).primaryTextTheme.button)),
              fv.locked
                  ? Padding(
                      padding: const EdgeInsets.only(left: 4.0, top: 2.0),
                      child: Icon(
                        Icons.lock_outline,
                        color: Colors.black54,
                        size: 12.0,
                      ),
                    )
                  : Container(),
            ],
          )),
    );
  }
}
