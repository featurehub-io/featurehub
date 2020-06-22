import 'package:app_singleapp/widgets/common/fh_delete_thing.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

import 'apps_bloc.dart';

class AppDeleteDialogWidget extends StatelessWidget {
  final Application application;
  final AppsBloc bloc;

  const AppDeleteDialogWidget(
      {Key key, @required this.bloc, @required this.application})
      : assert(application != null),
        assert(bloc != null),
        super(key: key);

  @override
  Widget build(BuildContext context) {
    return FHDeleteThingWarningWidget(
      bloc: bloc.mrClient,
      thing: "application '${application.name}'",
      deleteSelected: () async {
        final success = await bloc.deleteApp(application.id);
        if (success) {
          bloc.mrClient
              .addSnackbar(Text("Application '${application.name}' deleted!"));
        } else {
          bloc.mrClient.customError(
              messageTitle: "Couldn't delete application ${application.name}");
        }
        return success;
      },
    );
  }
}
