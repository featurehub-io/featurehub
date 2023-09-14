import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/common/fh_delete_thing.dart';
import 'package:open_admin_app/widgets/feature-groups/feature-groups-bloc.dart';
import 'package:openapi_dart_common/openapi.dart';

class FeatureGroupDeleteDialogWidget extends StatelessWidget {
  final FeatureGroupsBloc bloc;
  final FeatureGroupListGroup featureGroup;

  const FeatureGroupDeleteDialogWidget(
      {Key? key, required this.bloc, required this.featureGroup})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return FHDeleteThingWarningWidget(
      bloc: bloc.mrClient,
      thing: "feature '${featureGroup.name}'",
      content:
          'This action will delete a feature group and a strategy associated with it.'
          '\n\nThe features will not be deleted and remain present in your system.'
          '\n\nThis cannot be undone!',
      deleteSelected: () async {
        try {
          await bloc.deleteFeatureGroup(featureGroup.id);
          // await bloc.updateApplicationFeatureValuesStream();
          bloc.mrClient.removeOverlay();
          bloc.mrClient.addSnackbar(
              Text("Feature group '${featureGroup.name}' deleted!"));
          return true;
        } catch (e) {
          if (e is ApiException && e.code == 401) {
            bloc.mrClient.customError(
                messageTitle:
                    "You don't have permissions to perform this operation");
          } else {
            bloc.mrClient.customError(
                messageTitle:
                    "Couldn't delete feature group ${featureGroup.name}");
          }
          return false;
        }
      },
    );
  }
}
