import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/common/fh_delete_thing.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/widgets/service-accounts/service_accounts_env_bloc.dart';


class ApiKeyResetDialogWidget extends StatelessWidget {
  final ServiceAccount sa;
  final ServiceAccountEnvBloc bloc;
  final bool isClientKey;

  const ApiKeyResetDialogWidget(
      {Key? key, required this.bloc, required this.isClientKey, required this.sa})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return FHDeleteThingWarningWidget(
      bloc: bloc.mrClient,
      wholeWarning:
      """Are you sure you want to reset all ${isClientKey ? 'client' : 'server'} eval API keys for this service account?
This will affect the keys across all environments for a given service account!""",
      isResetThing: true,
      deleteSelected: () async {
        final updatedSA = await bloc.resetApiKey(sa.id.toString(), isClientKey ? ResetApiKeyType.clientEvalOnly : ResetApiKeyType.serverEvalOnly);
        if (updatedSA != null) {
          bloc.mrClient
              .addSnackbar(Text("'${isClientKey ? 'Client' : 'Server'}' eval API Key has been reset!"));
        } else {
          bloc.mrClient.customError(
              messageTitle: "Couldn't reset API Key");
        }
        return updatedSA!=null;
      },
    );
  }
}
