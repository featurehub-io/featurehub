import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/utils/utils.dart';
import 'package:open_admin_app/widgets/common/fh_alert_dialog.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:openapi_dart_common/openapi.dart';

import 'per_application_features_bloc.dart';

class CreateFeatureDialogWidget extends StatefulWidget {
  final Feature? feature;
  final PerApplicationFeaturesBloc bloc;

  const CreateFeatureDialogWidget({
    Key? key,
    required this.bloc,
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
  final TextEditingController _featureDesc = TextEditingController();

  bool isUpdate = false;
  bool isError = false;
  FeatureValueType? _dropDownFeatureTypeValue;

  @override
  void initState() {
    super.initState();
    if (widget.feature != null) {
      _featureName.text = widget.feature!.name;
      _featureKey.text = widget.feature!.key ?? '';
      _featureAlias.text = widget.feature!.alias ?? '';
      _featureLink.text = widget.feature!.link ?? '';
      _featureDesc.text = widget.feature!.description ?? '';
      _dropDownFeatureTypeValue = widget.feature!.valueType!;
      isUpdate = true;
    }
  }

  @override
  Widget build(BuildContext context) {
    final isReadOnly =
        !widget.bloc.mrClient.userIsFeatureAdminOfCurrentApplication;
    return Form(
      key: _formKey,
      child: FHAlertDialog(
        title: Text(widget.feature == null
            ? 'Create new feature'
            : (isReadOnly ? 'View feature' : 'Edit feature')),
        content: SizedBox(
          width: 500,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              TextFormField(
                  controller: _featureName,
                  decoration: const InputDecoration(labelText: 'Feature name'),
                  readOnly: isReadOnly,
                  autofocus: true,
                  onFieldSubmitted: (_) => FocusScope.of(context).nextFocus(),
                  validator: ((v) {
                    if (v == null || v.isEmpty) {
                      return 'Please enter feature name';
                    }
                    if (v.length < 4) {
                      return 'Feature name needs to be at least 4 characters long';
                    }
                    return null;
                  })),
              TextFormField(
                  controller: _featureKey,
                  readOnly: isReadOnly,
                  decoration: InputDecoration(
                      labelText: 'Feature key',
                      hintText: 'To be used in the code with FeatureHub SDK',
                      hintStyle: Theme.of(context).textTheme.caption),
                  onFieldSubmitted: (_) => FocusScope.of(context).nextFocus(),
                  validator: ((v) {
                    if (v == null || v.isEmpty) {
                      return 'Please enter feature key';
                    }
                    if (!validateFeatureKey(v)) {
                      return ('Can only include alphanumeric characters and underscores');
                    }
                    return null;
                  })),
//Comment out Alias key until we implement proper analytics
//              TextFormField(
//                  controller: _featureAlias,
//              readOnly: isReadOnly,
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
                readOnly: isReadOnly,
                controller: _featureDesc,
                onFieldSubmitted: (_) => FocusScope.of(context).nextFocus(),
                decoration: InputDecoration(
                    labelText: 'Description (optional)',
                    hintText: 'Some information about feature',
                    hintStyle: Theme.of(context).textTheme.caption),
              ),
              TextFormField(
                readOnly: isReadOnly,
                controller: _featureLink,
                onFieldSubmitted: (_) => FocusScope.of(context).nextFocus(),
                decoration: InputDecoration(
                    labelText: 'Reference link (optional)',
                    hintText:
                        'Optional link to external tracking system, e.g. Jira',
                    hintStyle: Theme.of(context).textTheme.caption),
              ),
              if (!isUpdate)
                Padding(
                  padding: const EdgeInsets.only(top: 14.0),
                  child: InkWell(
                    mouseCursor: SystemMouseCursors.click,
                    child: DropdownButton(
                      icon: const Padding(
                        padding: EdgeInsets.only(left: 8.0),
                        child: Icon(
                          Icons.keyboard_arrow_down,
                          size: 18,
                        ),
                      ),
                      isExpanded: false,
                      items: FeatureValueType.values
                          .map((FeatureValueType dropDownStringItem) {
                        return DropdownMenuItem<FeatureValueType>(
                            value: dropDownStringItem,
                            child: Text(
                                _transformValuesToString(dropDownStringItem),
                                style: Theme.of(context).textTheme.bodyText2));
                      }).toList(),
                      hint: Text('Select feature type',
                          style: Theme.of(context).textTheme.subtitle2),
                      onChanged: (Object? value) {
                        if (!isReadOnly) {
                          setState(() {
                            _dropDownFeatureTypeValue =
                                value as FeatureValueType?;
                          });
                        }
                      },
                      value: _dropDownFeatureTypeValue,
                    ),
                  ),
                ),
              if (isError)
                Text(
                  'Select feature type',
                  style: Theme.of(context)
                      .textTheme
                      .bodyText2!
                      .copyWith(color: Theme.of(context).errorColor),
                ),
            ],
          ),
        ),
        actions: <Widget>[
          FHFlatButtonTransparent(
            title: 'Cancel',
            keepCase: true,
            onPressed: () {
              widget.bloc.mrClient.removeOverlay();
            },
          ),
          if (!isReadOnly)
            FHFlatButton(
                title: isUpdate ? 'Update' : 'Create',
                keepCase: true,
                onPressed: (() async {
                  if (_formKey.currentState!.validate()) {
                    try {
                      if (isUpdate) {
                        await widget.bloc.updateFeature(
                            widget.feature!,
                            _featureName.text,
                            _featureKey.text,
                            _featureAlias.text,
                            _featureLink.text,
                        _featureDesc.text);
                        widget.bloc.mrClient.removeOverlay();
                        widget.bloc.mrClient.addSnackbar(
                            Text('Feature ${_featureName.text} updated!'));
                      } else {
                        if (_dropDownFeatureTypeValue != null) {
                          await widget.bloc.createFeature(
                              _featureName.text,
                              _featureKey.text,
                              _dropDownFeatureTypeValue!,
                              _featureAlias.text,
                              _featureLink.text,
                              _featureDesc.text);
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
                                "Feature with key '${_featureKey.text}' already exists");
                      } else {
                        await widget.bloc.mrClient.dialogError(e, s);
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
        return 'Standard flag (boolean)';
      case FeatureValueType.JSON:
        return 'Remote configuration (JSON)';
    }
  }
}
