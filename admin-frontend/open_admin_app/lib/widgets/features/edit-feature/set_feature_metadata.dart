import 'dart:async';

import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/common/fh_alert_dialog.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/common/fh_json_editor.dart';
import 'package:open_admin_app/widgets/features/per_application_features_bloc.dart';


class SetFeatureMetadataWidget extends StatefulWidget {
  final PerApplicationFeaturesBloc bloc;

  const SetFeatureMetadataWidget({Key? key, required this.bloc})
      : super(key: key);

  @override
  State<SetFeatureMetadataWidget> createState() =>
      _SetFeatureMetadataWidgetState();
}

class _SetFeatureMetadataWidgetState extends State<SetFeatureMetadataWidget> {
  TextEditingController tec = TextEditingController();
  final _formKey = GlobalKey<FormState>();
  Feature? _feature;
  late StreamSubscription<Feature?> streamSubscription;

  @override
  void dispose() {
    super.dispose();
    tec.dispose();
    streamSubscription.cancel();
  }

  @override
  void initState() {
    super.initState();
    streamSubscription = widget.bloc.featureMetadataStream.listen((feature) {
        setState(() {
          _feature = feature;
          if (feature != null && feature.metaData != null && mounted) {
          tec.text = feature.metaData!;
        }});
    });
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final isReadOnly = !widget.bloc.mrClient.userHasFeatureEditRoleInCurrentApplication;

    return (FHAlertDialog(
        title: Text(isReadOnly
            ? l10n.viewMetadataFor(_feature?.name ?? '')
            : l10n.editMetadataFor(_feature?.name ?? '')),
        actions: [
          FHFlatButtonTransparent(
            onPressed: () {
              widget.bloc.mrClient.removeOverlay();
            },
            title: l10n.cancel,
            keepCase: true,
          ),
          !isReadOnly
              ? FHFlatButton(
                  title: l10n.setValue,
                  onPressed: (() {
                    if (_formKey.currentState!.validate()) {
                      _changeValue(l10n);
                      widget.bloc.mrClient.removeOverlay();
                    }
                  }))
              : Container(),
        ],
        content: FHJsonEditorWidget(
            controller: tec, formKey: _formKey, onlyJsonValidation: false)));
  }

  Future<void> _changeValue(AppLocalizations l10n) async {
    try {
      await widget.bloc.updateFeatureMetadata(_feature!, tec.text);
      widget.bloc.mrClient.addSnackbar(
          Text(l10n.featureMetadataUpdated(_feature!.name)));
    } catch (e, s) {
      widget.bloc.mrClient.dialogError(e, s);
    }
  }
}
