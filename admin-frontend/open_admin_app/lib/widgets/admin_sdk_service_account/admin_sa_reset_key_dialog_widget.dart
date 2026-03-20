import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/common/fh_alert_dialog.dart';
import 'package:open_admin_app/widgets/common/fh_delete_thing.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/user/list/list_users_bloc.dart';

import 'admin_sa_access_key_display_widget.dart';

class AdminSAKeyResetDialogWidget extends StatelessWidget {
  final SearchPerson person;
  final ListUsersBloc bloc;

  const AdminSAKeyResetDialogWidget(
      {Key? key, required this.bloc, required this.person})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return FHDeleteThingWarningWidget(
      bloc: bloc.mrClient,
      removeOverlay: false,
      wholeWarning: l10n.adminSaResetTokenWarning,
      isResetThing: true,
      deleteSelected: () async {
        try {
          var token = await bloc.resetApiKey(person);

          bloc.mrClient.addOverlay((context) => FHAlertDialog(
                title: Text(l10n.adminSdkTokenReset),
                content: SizedBox(
                    height: 150,
                    child: AdminSAKeyShowDialogWidget(token: token)),
                actions: [
                  FHFlatButtonTransparent(
                    keepCase: true,
                    title: l10n.close,
                    onPressed: () {
                      bloc.mrClient.removeOverlay();
                    },
                  ),
                ],
              ));
          bloc.mrClient.addSnackbar(Text(l10n.adminSdkTokenResetSnackbar));
        } catch (e) {
          bloc.mrClient
              .customError(messageTitle: l10n.unableToResetToken);
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
