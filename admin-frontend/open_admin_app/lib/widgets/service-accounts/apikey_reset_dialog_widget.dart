import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/apps/manage_service_accounts_bloc.dart';
import 'package:open_admin_app/widgets/common/fh_delete_thing.dart';


class ApiKeyResetDialogWidget extends StatelessWidget {
  final ServiceAccount sa;
  final ManageServiceAccountsBloc bloc;
  final bool isClientKey;

  const ApiKeyResetDialogWidget(
      {super.key, required this.bloc, required this.isClientKey, required this.sa});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return FHDeleteThingWarningWidget(
      bloc: bloc.mrClient,
      wholeWarning: isClientKey ? l10n.resetClientApiKeysWarning : l10n.resetServerApiKeysWarning,
      isResetThing: true,
      deleteSelected: () async {
        var success = await bloc.resetApiKey(sa.id.toString(), isClientKey ? ResetApiKeyType.clientEvalOnly : ResetApiKeyType.serverEvalOnly);
        if (success) {
          bloc.mrClient
              .addSnackbar(Text(isClientKey ? l10n.clientApiKeysReset : l10n.serverApiKeysReset));
        } else {
          bloc.mrClient.customError(
              messageTitle: l10n.unableToResetApiKey);
        }
        return success;
      },
    );
  }
}
