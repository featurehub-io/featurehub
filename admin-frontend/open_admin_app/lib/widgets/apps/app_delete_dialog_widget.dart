import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/common/fh_delete_thing.dart';

import 'apps_bloc.dart';

class AppDeleteDialogWidget extends StatelessWidget {
  final Application application;
  final AppsBloc bloc;

  const AppDeleteDialogWidget(
      {Key? key, required this.bloc, required this.application})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return FHDeleteThingWarningWidget(
      bloc: bloc.mrClient,
      thing: l10n.appThingLabel(application.name),
      deleteSelected: () async {
        final success = await bloc.deleteApp(application.id);
        if (success) {
          bloc.mrClient
              .addSnackbar(Text(l10n.appDeleted(application.name)));
        } else {
          bloc.mrClient.customError(
              messageTitle: l10n.appDeleteError(application.name));
        }
        return success;
      },
    );
  }
}
