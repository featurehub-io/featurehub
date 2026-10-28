import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/common/fh_alert_dialog.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:openapi_dart_common/openapi.dart';

import '../per_application_features_bloc.dart';

/// Confirmation dialog for restoring (unarchiving) an archived feature.
///
/// Restoring can fail with a 409 if a live feature with the same name already
/// exists (the archived feature's name was reused), so we surface that as a
/// friendly error rather than a generic failure.
class FeatureUnarchiveDialogWidget extends StatelessWidget {
  final Feature feature;
  final PerApplicationFeaturesBloc bloc;

  const FeatureUnarchiveDialogWidget(
      {super.key, required this.bloc, required this.feature});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return FHAlertDialog(
      title: Row(
        children: <Widget>[
          const Padding(
            padding: EdgeInsets.only(right: 8.0),
            child: Icon(Icons.unarchive_outlined),
          ),
          Expanded(child: Text(l10n.featureRestoreTitle(feature.name))),
        ],
      ),
      content: Padding(
        padding: const EdgeInsets.all(8.0),
        child: Text(l10n.featureRestoreContent),
      ),
      actions: <Widget>[
        FHFlatButtonTransparent(
          title: l10n.cancel,
          keepCase: true,
          onPressed: () => bloc.mrClient.removeOverlay(),
        ),
        FHFlatButton(
          title: l10n.restore,
          keepCase: true,
          onPressed: () async {
            try {
              await bloc.unarchiveFeature(feature);
              await bloc.updateApplicationFeatureValuesStream();
              bloc.mrClient.removeOverlay();
              bloc.mrClient
                  .addSnackbar(Text(l10n.featureRestored(feature.name)));
            } catch (e) {
              if (e is ApiException && e.code == 409) {
                bloc.mrClient.customError(
                    messageTitle:
                        l10n.featureRestoreNameCollision(feature.name));
              } else if (e is ApiException && e.code == 401) {
                bloc.mrClient
                    .customError(messageTitle: l10n.noPermissionsForOperation);
              } else {
                bloc.mrClient.customError(
                    messageTitle: l10n.featureRestoreError(feature.name));
              }
            }
          },
        ),
      ],
    );
  }
}
