import 'package:app_singleapp/widgets/common/fh_delete_thing.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:openapi_dart_common/openapi.dart';

import 'per_application_features_bloc.dart';

class FeatureDeleteDialogWidget extends StatelessWidget {
  final Feature feature;
  final PerApplicationFeaturesBloc bloc;

  const FeatureDeleteDialogWidget(
      {Key? key, required this.bloc, required this.feature})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return FHDeleteThingWarningWidget(
      bloc: bloc.mrClient,
      thing: "feature '${feature.name}'",
      content:
          'You need to make sure all your code is cleaned up and can deal without this feature!\n\nThis cannot be undone!',
      deleteSelected: () async {
        try {
          await bloc.deleteFeature(feature.key!);
          bloc.mrClient.removeOverlay();
          bloc.mrClient.addSnackbar(Text("Feature '${feature.name}' deleted!"));
          return true;
        } catch (e) {
          if (e is ApiException && e.code == 401) {
            bloc.mrClient.customError(
                messageTitle:
                    "You don't have permissions to perform this operation");
          } else {
            bloc.mrClient.customError(
                messageTitle: "Couldn't delete feature ${feature.name}");
          }
          return false;
        }
      },
    );
  }
}
