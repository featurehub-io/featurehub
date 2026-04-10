import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/common/fh_alert_dialog.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/common/fh_loading_indicator.dart';
import 'package:open_admin_app/widgets/feature-groups/feature_groups_bloc.dart';
import 'package:openapi_dart_common/openapi.dart';

class FeatureGroupUpdateDialogWidget extends StatefulWidget {
  final FeatureGroupListGroup? featureGroup;
  final FeatureGroupsBloc bloc;

  const FeatureGroupUpdateDialogWidget({
    super.key,
    required this.bloc,
    this.featureGroup,
  });

  @override
  State<StatefulWidget> createState() {
    return _FeatureGroupUpdateDialogWidgetState();
  }
}

class _FeatureGroupUpdateDialogWidgetState
    extends State<FeatureGroupUpdateDialogWidget> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _featureGroupName = TextEditingController();
  final TextEditingController _featureGroupDescription =
      TextEditingController();
  bool isUpdate = false;
  bool _busy = false;

  @override
  void initState() {
    super.initState();
    if (widget.featureGroup != null) {
      _featureGroupName.text = widget.featureGroup!.name;
      _featureGroupDescription.text = widget.featureGroup!.description;
      isUpdate = true;
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    if (_busy) {
      return const FHLoadingIndicator();
    } else {
      return FHAlertDialog(
        title: Text(widget.featureGroup == null
            ? l10n.createNewFeatureGroup
            : l10n.editFeatureGroup),
        content: Form(
            key: _formKey,
            child: SizedBox(
              width: 500,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  TextFormField(
                      controller: _featureGroupName,
                      autofocus: true,
                      textInputAction: TextInputAction.next,
                      decoration: InputDecoration(
                          labelText: l10n.featureGroupNameLabel),
                      validator: ((v) {
                        if (v == null || v.isEmpty) {
                          return l10n.featureGroupNameRequired;
                        }
                        if (v.length < 4) {
                          return l10n.featureGroupNameTooShort;
                        }
                        return null;
                      })),
                  TextFormField(
                      controller: _featureGroupDescription,
                      textInputAction: TextInputAction.done,
                      decoration: InputDecoration(
                          labelText: l10n.featureGroupDescriptionLabel),
                      validator: ((v) {
                        if (v == null || v.isEmpty) {
                          return l10n.featureGroupDescriptionRequired;
                        }
                        if (v.length < 4) {
                          return l10n.featureGroupDescriptionTooShort;
                        }
                        return null;
                      })),
                ],
              ),
            )),
        actions: [
          FHFlatButtonTransparent(
            title: l10n.cancel,
            keepCase: true,
            onPressed: () {
              widget.bloc.mrClient.removeOverlay();
            },
          ),
          FHFlatButton(
              title: isUpdate ? l10n.update : l10n.create,
              keepCase: true,
              onPressed: () => _handleValidation(l10n)),
        ],
      );
    }
  }

  void _handleValidation(AppLocalizations l10n) async {
    if (_formKey.currentState!.validate()) {
      try {
        setState(() {
          _busy = true;
        });
        if (isUpdate) {
          await widget.bloc.updateFeatureGroup(widget.featureGroup!,
              name: _featureGroupName.text,
              description: _featureGroupDescription.text);
          widget.bloc.mrClient.removeOverlay();
          widget.bloc.mrClient.addSnackbar(
              Text(l10n.featureGroupUpdated(_featureGroupName.text)));
        } else {
          await widget.bloc.createFeatureGroup(
              _featureGroupName.text, _featureGroupDescription.text);
          widget.bloc.mrClient.removeOverlay();
          widget.bloc.mrClient.addSnackbar(
              Text(l10n.featureGroupCreated(_featureGroupName.text)));
        }
      } catch (e, s) {
        if (e is ApiException && e.code == 409) {
          widget.bloc.mrClient.customError(
              messageTitle: l10n.featureGroupAlreadyExists(_featureGroupName.text));
        } else {
          await widget.bloc.mrClient.dialogError(e, s);
        }
      } finally {
        setState(() {
          _busy = false;
        });
      }
    }
  }
}
