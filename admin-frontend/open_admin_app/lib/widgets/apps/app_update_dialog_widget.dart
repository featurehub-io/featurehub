import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/common/fh_alert_dialog.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/common/fh_loading_indicator.dart';
import 'package:openapi_dart_common/openapi.dart';

import 'apps_bloc.dart';

class AppUpdateDialogWidget extends StatefulWidget {
  final Application? application;
  final AppsBloc bloc;

  const AppUpdateDialogWidget({
    Key? key,
    required this.bloc,
    this.application,
  }) : super(key: key);

  @override
  State<StatefulWidget> createState() {
    return _AppUpdateDialogWidgetState();
  }
}

class _AppUpdateDialogWidgetState extends State<AppUpdateDialogWidget> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _appName = TextEditingController();
  final TextEditingController _appDescription = TextEditingController();
  bool isUpdate = false;
  bool _busy = false;

  @override
  void initState() {
    super.initState();
    if (widget.application != null) {
      _appName.text = widget.application!.name;
      _appDescription.text = widget.application!.description!;
      isUpdate = true;
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    if (_busy) {
      return const FHLoadingIndicator();
    }
    else {
      return FHAlertDialog(
        title: Text(widget.application == null
            ? l10n.createNewApplication
            : l10n.editApplication),
        content: Form(
            key: _formKey,
            child: SizedBox(
              width: 500,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  TextFormField(
                      controller: _appName,
                      autofocus: true,
                      textInputAction: TextInputAction.next,
                      decoration:
                      InputDecoration(labelText: l10n.appNameLabel),
                      validator: ((v) {
                        if (v == null || v.isEmpty) {
                          return l10n.appNameRequired;
                        }
                        if (v.length < 4) {
                          return l10n.appNameTooShort;
                        }
                        return null;
                      })),
                  TextFormField(
                      controller: _appDescription,
                      textInputAction: TextInputAction.done,
                      decoration: InputDecoration(
                          labelText: l10n.appDescriptionLabel),
                      validator: ((v) {
                        if (v == null || v.isEmpty) {
                          return l10n.appDescriptionRequired;
                        }
                        if (v.length < 4) {
                          return l10n.appDescriptionTooShort;
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
          await widget.bloc.updateApplication(
              widget.application!, _appName.text, _appDescription.text);
          widget.bloc.mrClient.removeOverlay();
          widget.bloc.mrClient
              .addSnackbar(Text(l10n.appUpdated(_appName.text)));
        } else {
          await widget.bloc
              .createApplication(_appName.text, _appDescription.text);
          widget.bloc.mrClient.removeOverlay();
          widget.bloc.mrClient
              .addSnackbar(Text(l10n.appCreated(_appName.text)));
        }
      } catch (e, s) {
        if (e is ApiException && e.code == 409) {
          widget.bloc.mrClient.customError(
              messageTitle: l10n.appAlreadyExists(_appName.text));
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
