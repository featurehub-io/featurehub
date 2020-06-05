import 'package:app_singleapp/widgets/common/FHFlatButton.dart';
import 'package:app_singleapp/widgets/common/fh_alert_dialog.dart';
import 'package:app_singleapp/widgets/common/fh_outline_button.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:openapi_dart_common/openapi.dart';

import 'manage_app_bloc.dart';

class AppUpdateDialogWidget extends StatefulWidget {
  final Application application;
  final ManageAppBloc bloc;

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
    return Form(
      key: _formKey,
      child: Stack(

        children: [FHAlertDialog(
          title: Text(widget.application == null
              ? 'Create new application'
              : 'Edit application'),
          content: Container(
            width: 500,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: <Widget>[
                TextFormField(
                    controller: _appName,
                    decoration: InputDecoration(labelText: "Application name"),
                    validator: ((v) {
                      if (v.isEmpty) {
                        return "Please enter an application name";
                      }
                      if (v.length < 4) {
                        return "Application name needs to be at least 4 characters long";
                      }
                      return null;
                    })),
                TextFormField(
                    controller: _appDescription,
                    decoration:
                    InputDecoration(labelText: "Application description"),
                    validator: ((v) {
                      if (v.isEmpty) {
                        return "Please enter app description";
                      }
                      if (v.length < 4) {
                        return "Application description needs to be at least 4 characters long";
                      }
                      return null;
                    })),
              ],
            ),
          ),
          actions: [
            ButtonBar(
              children: <Widget>[
                FHOutlineButton(
                  title: "Cancel",
                  onPressed: () {
                    widget.bloc.mrClient.removeOverlay();
                  },
                ),
                FHFlatButton(
                    title: isUpdate ? "Update" : "Create",
                    onPressed: (() async {
                      if (_formKey.currentState.validate()) {
                        try {
                          if (isUpdate) {
                            await widget.bloc.updateApplication(widget.application,
                                _appName.text, _appDescription.text);
                            widget.bloc.mrClient.removeOverlay();
                            widget.bloc.mrClient.addSnackbar(
                                Text("Application ${_appName.text} updated!"));
                          } else {
                            await widget.bloc.createApplication(
                                _appName.text, _appDescription.text);
                            widget.bloc.mrClient.removeOverlay();
                            widget.bloc.mrClient.addSnackbar(
                                Text("Application ${_appName.text} created!"));
                          }
                        } catch (e, s) {
                          if (e is ApiException && e.code == 409) {
                            widget.bloc.mrClient.customError(messageTitle: "Application '${_appName.text}' already exists");
                          } else {
                            widget.bloc.mrClient.dialogError(e, s);
                          }
                        }
                      }
                    }))
              ],
            ),
          ],
        )],
      ),
    );
  }
}
