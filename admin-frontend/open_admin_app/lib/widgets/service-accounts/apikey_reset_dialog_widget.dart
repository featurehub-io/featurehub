import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/apps/manage_service_accounts_bloc.dart';
import 'package:open_admin_app/widgets/common/fh_delete_thing.dart';
import 'package:flutter/material.dart';


class ApiKeyResetDialogWidget extends StatelessWidget {
  final ServiceAccount sa;
  final ManageServiceAccountsBloc bloc;
  final bool isClientKey;

  const ApiKeyResetDialogWidget(
      {Key? key, required this.bloc, required this.isClientKey, required this.sa})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return FHDeleteThingWarningWidget(
      bloc: bloc.mrClient,
      wholeWarning:
      """Are you sure you want to reset ALL ${isClientKey ? 'client' : 'server'} eval API keys for this service account?
This will affect the keys across all environments and all applications that this service account has access to!""",
      isResetThing: true,
      deleteSelected: () async {
        var success = await bloc.resetApiKey(sa.id.toString(), isClientKey ? ResetApiKeyType.clientEvalOnly : ResetApiKeyType.serverEvalOnly);
        if (success) {
          bloc.mrClient
              .addSnackbar(Text("'${isClientKey ? 'Client' : 'Server'}' eval API Key has been reset!"));
        } else {
          bloc.mrClient.customError(
              messageTitle: "Unable to reset API Key");
        }
        return success;
      },
    );
  }
}
