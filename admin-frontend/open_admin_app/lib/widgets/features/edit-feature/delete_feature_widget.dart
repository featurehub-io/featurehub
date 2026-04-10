import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/common/fh_delete_thing.dart';
import 'package:openapi_dart_common/openapi.dart';

import '../per_application_features_bloc.dart';

class FeatureDeleteDialogWidget extends StatelessWidget {
  final Feature feature;
  final PerApplicationFeaturesBloc bloc;

  const FeatureDeleteDialogWidget(
      {super.key, required this.bloc, required this.feature});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return FHDeleteThingWarningWidget(
      bloc: bloc.mrClient,
      thing: l10n.featureThingLabel(feature.name),
      content: l10n.featureDeleteContent,
      deleteSelected: () async {
        try {
          await bloc.deleteFeature(feature.key);
          await bloc.updateApplicationFeatureValuesStream();
          bloc.mrClient.removeOverlay();
          bloc.mrClient.addSnackbar(Text(l10n.featureDeleted(feature.name)));
          return true;
        } catch (e) {
          if (e is ApiException && e.code == 401) {
            bloc.mrClient.customError(
                messageTitle: l10n.noPermissionsForOperation);
          } else {
            bloc.mrClient.customError(
                messageTitle: l10n.featureDeleteError(feature.name));
          }
          return false;
        }
      },
    );
  }
}
