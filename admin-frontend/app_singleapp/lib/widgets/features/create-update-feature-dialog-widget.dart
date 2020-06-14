import 'package:app_singleapp/utils/utils.dart';
import 'package:app_singleapp/widgets/common/FHFlatButton.dart';
import 'package:app_singleapp/widgets/common/fh_alert_dialog.dart';
import 'package:app_singleapp/widgets/common/fh_outline_button.dart';
import 'package:app_singleapp/widgets/features/feature_status_bloc.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:openapi_dart_common/openapi.dart';

class CreateFeatureDialogWidget extends StatefulWidget {
  final Feature feature;
  final FeatureStatusBloc bloc;

  const CreateFeatureDialogWidget({
    Key key,
    @required this.bloc,
    this.feature,
  }) : super(key: key);

  @override
  State<StatefulWidget> createState() {
    return _CreateFeatureDialogWidgetState();
  }
}

class _CreateFeatureDialogWidgetState extends State<CreateFeatureDialogWidget> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _featureName = TextEditingController();
  final TextEditingController _featureKey = TextEditingController();
  final TextEditingController _featureAlias = TextEditingController();
  final TextEditingController _featureLink = TextEditingController();

  bool isUpdate = false;
  bool isError = false;
  FeatureValueType _dropDownFeatureTypeValue;

  @override
  void initState() {
    super.initState();
    if (widget.feature != null) {
      _featureName.text = widget.feature.name;
      _featureKey.text = widget.feature.key;
      _featureAlias.text = widget.feature.alias;
      _featureLink.text = widget.feature.link;
      isUpdate = true;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Form(
      key: _formKey,
      child: FHAlertDialog(
        title: Text(
            widget.feature == null ? 'Create new feature' : 'Edit feature'),
        content: Container(
          width: 500,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              TextFormField(
                  controller: _featureName,
                  decoration: InputDecoration(labelText: 'Feature name'),
                  validator: ((v) {
                    if (v.isEmpty) {
                      return 'Please enter feature name';
                    }
                    if (v.length < 4) {
                      return 'Feature name needs to be at least 4 characters long';
                    }
                    return null;
                  })),
              TextFormField(
                  controller: _featureKey,
                  // initialValue: _featureName.toString(),
                  decoration: InputDecoration(
                      labelText: 'Feature key',
                      hintText: 'To be used in the code with FeatureHub SDK',
                      hintStyle: Theme.of(context).textTheme.caption),
                  validator: ((v) {
                    if (v.isEmpty) {
                      return 'Please enter feature key';
                    }
                    if (!validateFeatureKey(v)) {
                      return ('Can only include letters and underscores');
                    }
                    return null;
                  })),
//Comment out Alias key until we implement proper analytics
//              TextFormField(
//                  controller: _featureAlias,
//                  // initialValue: _featureName.toString(),
//                  decoration: InputDecoration(
//                      labelText: 'Alias key (optional)',
//                      hintText:
//                          "Use alias key as a 'secret' alternative to the feature key",
//                      hintStyle: Theme.of(context).textTheme.caption),
//                  validator: ((v) {
//                    if (v.isNotEmpty && !validateFeatureKey(v)) {
//                      return ('Can only include letters, numbers and underscores');
//                    }
//                    return null;
//                  })),
              TextFormField(
                controller: _featureLink,
                decoration: InputDecoration(
                    labelText: 'Reference link (optional)',
                    hintText:
                        'Optional link to external tracking system, e.g. Jira',
                    hintStyle: Theme.of(context).textTheme.caption),
              ),
              !isUpdate
                  ? Padding(
                      padding: const EdgeInsets.only(top: 14.0),
                      child: DropdownButton(
                        isExpanded: false,
                        items: FeatureValueType.values
                            .map((FeatureValueType dropDownStringItem) {
                          return DropdownMenuItem<FeatureValueType>(
                              value: dropDownStringItem,
                              child: Text(
                                  _transformValuesToString(dropDownStringItem),
                                  style:
                                      Theme.of(context).textTheme.bodyText2));
                        }).toList(),
                        hint: Text('Select feature type',
                            style: Theme.of(context).textTheme.subtitle2),
                        onChanged: (value) {
                          setState(() {
                            _dropDownFeatureTypeValue = value;
                          });
                        },
                        value: _dropDownFeatureTypeValue,
                      ),
                    )
                  : Container(),
              Container(
                  child: isError
                      ? Text(
                          'Select feature type',
                          style: Theme.of(context)
                              .textTheme
                              .bodyText2
                              .copyWith(color: Theme.of(context).errorColor),
                        )
                      : Container())
            ],
          ),
        ),
        actions: <Widget>[
          FHOutlineButton(
            title: 'Cancel',
            onPressed: () {
              widget.bloc.mrClient.removeOverlay();
            },
          ),
          FHFlatButton(
              title: isUpdate ? 'Update' : 'Create',
              onPressed: (() async {
                if (_formKey.currentState.validate()) {
                  try {
                    if (isUpdate) {
                      await widget.bloc.updateFeature(
                          widget.feature,
                          _featureName.text,
                          _featureKey.text,
                          _featureAlias.text,
                          _featureLink.text);
                      widget.bloc.mrClient.removeOverlay();
                      widget.bloc.mrClient.addSnackbar(
                          Text('Feature ${_featureName.text} updated!'));
                    } else {
                      if (_dropDownFeatureTypeValue != null) {
                        await widget.bloc.createFeature(
                            _featureName.text,
                            _featureKey.text,
                            _dropDownFeatureTypeValue,
                            _featureAlias.text,
                            _featureLink.text);
                        widget.bloc.mrClient.removeOverlay();
                        widget.bloc.mrClient.addSnackbar(
                            Text('Feature ${_featureName.text} created!'));
                      } else {
                        setState(() {
                          isError = true;
                        });
                      }
                    }
                  } catch (e, s) {
                    if (e is ApiException && e.code == 409) {
                      widget.bloc.mrClient.customError(
                          messageTitle:
                              "Feature '${_featureName.text}' already exists");
                    } else {
                      widget.bloc.mrClient.dialogError(e, s);
                    }
                  }
                }
              }))
        ],
      ),
    );
  }

  String _transformValuesToString(FeatureValueType featureValueType) {
    switch (featureValueType) {
      case FeatureValueType.STRING:
        return 'String';
      case FeatureValueType.NUMBER:
        return 'Number';
      case FeatureValueType.BOOLEAN:
        return 'Feature flag';
      case FeatureValueType.JSON:
        return 'Configuration (JSON)';
    }

    return '';
  }
}
