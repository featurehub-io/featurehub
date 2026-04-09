import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/utils/utils.dart';
import 'package:open_admin_app/widgets/common/fh_alert_dialog.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:openapi_dart_common/openapi.dart';

import '../per_application_features_bloc.dart';

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
      _featureKey.text = widget.feature!.key;
      _featureAlias.text = widget.feature!.alias ?? '';
      _featureLink.text = widget.feature!.link ?? '';
      _featureDesc.text = widget.feature!.description ?? '';
      _dropDownFeatureTypeValue = widget.feature!.valueType;
      isUpdate = true;
    }
  }

  @override
  Widget build(BuildContext context) {
    var isReadOnly = false;

    // only let this screen to be editable if right permissions exist
    if (isUpdate) {
      isReadOnly =
          !widget.bloc.mrClient.userHasFeatureEditRoleInCurrentApplication;
    } else {
      isReadOnly =
          !widget.bloc.mrClient.userHasFeatureCreationRoleInCurrentApplication;
    }

    final l10n = AppLocalizations.of(context)!;
    return Form(
      key: _formKey,
      child: FHAlertDialog(
        title: Text(widget.feature == null
            ? l10n.createNewFeature
            : (isReadOnly ? l10n.viewFeature : l10n.editFeature)),
        content: SizedBox(
          width: 500,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              TextFormField(
                  controller: _featureName,
                  decoration: InputDecoration(labelText: l10n.featureNameLabel),
                  readOnly: isReadOnly,
                  autofocus: true,
                  onFieldSubmitted: (_) => FocusScope.of(context).nextFocus(),
                  validator: ((v) {
                    if (v == null || v.isEmpty) {
                      return l10n.featureNameRequired;
                    }
                    if (v.length < 4) {
                      return l10n.featureNameTooShort;
                    }
                    return null;
                  })),
              TextFormField(
                  controller: _featureKey,
                  readOnly: isReadOnly,
                  decoration: InputDecoration(
                      labelText: l10n.featureKeyLabel,
                      hintText: l10n.featureKeyHint,
                      hintStyle: Theme.of(context).textTheme.bodySmall),
                  onFieldSubmitted: (_) => FocusScope.of(context).nextFocus(),
                  validator: ((v) {
                    if (v == null || v.isEmpty) {
                      return l10n.featureKeyRequired;
                    }
                    if (!validateFeatureKey(v)) {
                      return l10n.featureKeyNoWhitespace;
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
//                      hintStyle: Theme.of(context).textTheme.bodySmall),
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
                    labelText: l10n.featureDescriptionLabel,
                    hintText: l10n.featureDescriptionHint,
                    hintStyle: Theme.of(context).textTheme.bodySmall),
              ),
              TextFormField(
                readOnly: isReadOnly,
                controller: _featureLink,
                onFieldSubmitted: (_) => FocusScope.of(context).nextFocus(),
                decoration: InputDecoration(
                    labelText: l10n.featureLinkLabel,
                    hintText: l10n.featureLinkHint,
                    hintStyle: Theme.of(context).textTheme.bodySmall),
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
                                _transformValuesToString(dropDownStringItem, l10n),
                                style: Theme.of(context).textTheme.bodyMedium));
                      }).toList(),
                      hint: Text(l10n.selectFeatureType,
                          style: Theme.of(context).textTheme.titleSmall),
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
                  l10n.selectFeatureType,
                  style: Theme.of(context)
                      .textTheme
                      .bodyMedium!
                      .copyWith(color: Theme.of(context).colorScheme.error),
                ),
            ],
          ),
        ),
        actions: <Widget>[
          FHFlatButtonTransparent(
            title: l10n.cancel,
            keepCase: true,
            onPressed: () {
              widget.bloc.mrClient.removeOverlay();
            },
          ),
          if (!isReadOnly)
            FHFlatButton(
                title: isUpdate ? l10n.update : l10n.create,
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
                        await widget.bloc
                            .updateApplicationFeatureValuesStream();
                        widget.bloc.mrClient.addSnackbar(
                            Text(l10n.featureUpdated(_featureName.text)));
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
                          widget.bloc.updateApplicationFeatureValuesStream();
                          widget.bloc.mrClient.addSnackbar(
                              Text(l10n.featureCreated(_featureName.text)));
                        } else {
                          setState(() {
                            isError = true;
                          });
                        }
                      }
                    } catch (e, s) {
                      if (e is ApiException && e.code == 409) {
                        widget.bloc.mrClient.customError(
                            messageTitle: l10n.featureKeyAlreadyExists(_featureKey.text));
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

  String _transformValuesToString(FeatureValueType featureValueType, AppLocalizations l10n) {
    switch (featureValueType) {
      case FeatureValueType.STRING:
        return l10n.featureTypeString;
      case FeatureValueType.NUMBER:
        return l10n.featureTypeNumber;
      case FeatureValueType.BOOLEAN:
        return l10n.featureTypeBoolean;
      case FeatureValueType.JSON:
        return l10n.featureTypeJson;
    }
  }
}
