import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/common/fh_alert_dialog.dart';
import 'package:open_admin_app/widgets/common/fh_delete_thing.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/user/list/list_users_bloc.dart';

import 'admin_sa_access_key_display_widget.dart';

class AdminSAKeyResetDialogWidget extends StatelessWidget {
  final Person person;
  final ListUsersBloc bloc;

  const AdminSAKeyResetDialogWidget(
      {Key? key, required this.bloc, required this.person})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return FHDeleteThingWarningWidget(
      bloc: bloc.mrClient,
      removeOverlay: false,
      wholeWarning:
          """Are you sure you want to reset the access token for this service account?
This will invalidate the current token!""",
      isResetThing: true,
      deleteSelected: () async {
        var token = await bloc.resetApiKey(person);
        if (token != null) {
          bloc.mrClient.addOverlay((context) => FHAlertDialog(
                title: const Text("Admin SDK access token has been reset"),
                content: SizedBox(
                    height: 150,
                    child: AdminSAKeyShowDialogWidget(token: token)),
                actions: [
                  FHFlatButtonTransparent(
                    keepCase: true,
                    title: 'Close',
                    onPressed: () {
                      bloc.mrClient.removeOverlay();
                    },
                  ),
                ],
              ));
          bloc.mrClient.addSnackbar(
              const Text("Admin SDK access token has been reset!"));
        } else {
          bloc.mrClient
              .customError(messageTitle: "Unable to reset access token");
        }
        return true;
      },
    );
  }
}

class AdminSAKeyShowDialogWidget extends StatelessWidget {
  const AdminSAKeyShowDialogWidget({Key? key, required this.token})
      : super(key: key);
  final String token;

  @override
  Widget build(BuildContext context) {
    return AdminAccessKeyDisplayWidget(token: token);
  }
}
