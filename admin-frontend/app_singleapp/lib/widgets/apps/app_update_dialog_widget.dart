import 'package:app_singleapp/widgets/common/FHFlatButton.dart';
import 'package:app_singleapp/widgets/common/fh_alert_dialog.dart';
import 'package:app_singleapp/widgets/common/fh_flat_button_transparent.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:openapi_dart_common/openapi.dart';

import 'apps_bloc.dart';

class AppUpdateDialogWidget extends StatefulWidget {
  final Application application;
  final AppsBloc bloc;

  const AppUpdateDialogWidget({
    Key key,
    @required this.bloc,
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
      _appName.text = widget.application.name;
      _appDescription.text = widget.application.description;
      isUpdate = true;
    }
  }

  @override
  Widget build(BuildContext context) {
    return FHAlertDialog(
      title: Text(widget.application == null
          ? 'Create new application'
          : 'Edit application'),
      content: Form(
          key: _formKey,
          child: Container(
            width: 500,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: <Widget>[
                TextFormField(
                    controller: _appName,
                    autofocus: true,
                    textInputAction: TextInputAction.next,
                    decoration: InputDecoration(labelText: 'Application name'),
                    validator: ((v) {
                      if (v.isEmpty) {
                        return 'Please enter an application name';
                      }
                      if (v.length < 4) {
                        return 'Application name needs to be at least 4 characters long';
                      }
                      return null;
                    })),
                TextFormField(
                    controller: _appDescription,
                    textInputAction: TextInputAction.done,
                    decoration:
                        InputDecoration(labelText: 'Application description'),
                    validator: ((v) {
                      if (v.isEmpty) {
                        return 'Please enter app description';
                      }
                      if (v.length < 4) {
                        return 'Application description needs to be at least 4 characters long';
                      }
                      return null;
                    })),
              ],
            ),
          )),
      actions: [
        FHFlatButtonTransparent(
          title: 'Cancel',
          keepCase: true,
          onPressed: () {
            widget.bloc.mrClient.removeOverlay();
          },
        ),
        FHFlatButton(
            title: isUpdate ? 'Update' : 'Create',
            keepCase: true,
            onPressed: () => _busy ? null : _handleValidation()),
      ],
    );
  }

  void _handleValidation() async {
    if (_formKey.currentState.validate()) {
      try {
        setState(() {
          _busy = true;
        });
        if (isUpdate) {
          await widget.bloc.updateApplication(
              widget.application, _appName.text, _appDescription.text);
          widget.bloc.mrClient.removeOverlay();
          widget.bloc.mrClient
              .addSnackbar(Text('Application ${_appName.text} updated!'));
        } else {
          await widget.bloc
              .createApplication(_appName.text, _appDescription.text);
          widget.bloc.mrClient.removeOverlay();
          widget.bloc.mrClient
              .addSnackbar(Text('Application ${_appName.text} created!'));
        }
      } catch (e, s) {
        if (e is ApiException && e.code == 409) {
          widget.bloc.mrClient.customError(
              messageTitle: "Application '${_appName.text}' already exists");
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
