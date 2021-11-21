import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/common/fh_delete_thing.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/widgets/service-accounts/service_accounts_env_bloc.dart';


class ApiKeyResetDialogWidget extends StatelessWidget {
  final ServiceAccountPermission account;
  final ServiceAccountEnvBloc bloc;

  const ApiKeyResetDialogWidget(
      {Key? key, required this.bloc, required this.account})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return FHDeleteThingWarningWidget(
      bloc: bloc.mrClient,
      wholeWarning: "Are you sure you want to reset this API Key?",
      isResetThing: true,
      deleteSelected: () async {
        final sa = await bloc.resetApiKey(account.id.toString());
        if (sa != null) {
          bloc.mrClient
              .addSnackbar(Text("'${account.sdkUrlClientEval !=null ? 'Client' : 'Server'}' eval API Key has been reset!"));
        } else {
          bloc.mrClient.customError(
              messageTitle: "Couldn't reset API Key");
        }
        return sa!=null;
      },
    );
  }
}
