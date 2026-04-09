import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/common/fh_delete_thing.dart';
import 'package:open_admin_app/widgets/feature-groups/feature_groups_bloc.dart';
import 'package:openapi_dart_common/openapi.dart';

class FeatureGroupDeleteDialogWidget extends StatelessWidget {
  final FeatureGroupsBloc bloc;
  final FeatureGroupListGroup featureGroup;

  const FeatureGroupDeleteDialogWidget(
      {Key? key, required this.bloc, required this.featureGroup})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return FHDeleteThingWarningWidget(
      bloc: bloc.mrClient,
      thing: l10n.featureThingLabel(featureGroup.name),
      content: l10n.featureGroupDeleteContent,
      deleteSelected: () async {
        try {
          await bloc.deleteFeatureGroup(featureGroup.id);
          bloc.mrClient.removeOverlay();
          bloc.mrClient.addSnackbar(
              Text(l10n.featureGroupDeleted(featureGroup.name)));
          return true;
        } catch (e) {
          if (e is ApiException && e.code == 401) {
            bloc.mrClient.customError(
                messageTitle: l10n.noPermissionsForOperation);
          } else {
            bloc.mrClient.customError(
                messageTitle: l10n.featureGroupDeleteError(featureGroup.name));
          }
          return false;
        }
      },
    );
  }
}
