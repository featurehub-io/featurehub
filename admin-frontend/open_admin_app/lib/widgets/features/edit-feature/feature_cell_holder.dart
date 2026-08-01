import 'package:universal_html/html.dart';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/common/copy_to_clipboard_html.dart';
import 'package:open_admin_app/widgets/common/fh_icon_button.dart';
import 'package:open_admin_app/widgets/features/edit-feature/create_update_feature_dialog_widget.dart';
import 'package:open_admin_app/widgets/features/edit-feature/delete_feature_widget.dart';
import 'package:open_admin_app/widgets/features/edit-feature/set_feature_metadata.dart';
import 'package:open_admin_app/widgets/features/edit-feature/unarchive_feature_widget.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/features/per_application_features_bloc.dart';

class FeatureCellHolder extends StatelessWidget {
  final Feature feature;

  const FeatureCellHolder({super.key, required this.feature});

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<PerApplicationFeaturesBloc>(context);
    final l10n = AppLocalizations.of(context)!;
    final isArchived = feature.whenArchived != null;
    final isEditor = bloc.mrClient.userHasFeatureEditRoleInCurrentApplication;
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(
          child: Padding(
          padding: const EdgeInsets.only(left: 8.0, top: 8.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
                Expanded(
                  child: SelectionArea(
                    child: Align(
                      alignment: Alignment.topLeft,
                      child: Tooltip(
                        message: feature.name,
                        verticalOffset: 8.0,
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Flexible(
                              fit: FlexFit.loose,
                              child: Text(feature.name,
                                  overflow: TextOverflow.ellipsis,
                                  maxLines: 2,
                                  // minFontSize: 8.0,
                                  style: Theme.of(context)
                                      .textTheme
                                      .bodyMedium!
                                      .copyWith(
                                          fontWeight: FontWeight.bold,
                                          fontStyle: isArchived
                                              ? FontStyle.italic
                                              : null,
                                          color: isArchived
                                              ? Theme.of(context)
                                                  .disabledColor
                                              : null)),
                            ),
                            Row(
                              children: [
                                if (feature.link?.isNotEmpty == true)
                                  FHIconButton(
                                    tooltip: feature.link!,
                                    icon: const Icon(
                                        Icons.arrow_outward_outlined),
                                    onPressed: () {
                                      window.open(feature.link!, 'new tab');
                                    },
                                  ),
                                FHCopyToClipboard(
                                    tooltipMessage:
                                        l10n.copyFeatureKeyToClipboard,
                                    copyString: feature.key),
                              ],
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
                if (isArchived && isEditor) // don't show anything for non-editor
                  TextButton.icon(
                    onPressed: () => bloc.mrClient.addOverlay(
                        (BuildContext context) => FeatureUnarchiveDialogWidget(
                            bloc: bloc, feature: feature)),
                    icon: const Icon(Icons.unarchive_outlined,
                        size: 18, color: Colors.green),
                    label: Text(l10n.restore),
                  ),
                if (!isArchived)
                  PopupMenuButton(
                    tooltip: l10n.showMore,
                    icon: Icon(Icons.more_vert,
                        color: Theme.of(context).colorScheme.primary),
                    onSelected: (value) {
                      if (value == 'edit') {
                        bloc.mrClient.addOverlay((BuildContext context) =>
                            createFeatureDialog(bloc, feature));
                      }
                      if (value == 'delete') {
                        bloc.mrClient.addOverlay((BuildContext context) =>
                            FeatureDeleteDialogWidget(
                                bloc: bloc, feature: feature));
                      }
                      if (value == 'metadata') {
                        bloc.getFeatureIncludingMetadata(feature);
                        bloc.mrClient.addOverlay(
                            (BuildContext context) => SetFeatureMetadataWidget(
                                  bloc: bloc,
                                ));
                      }
                    },
                    itemBuilder: (BuildContext context) {
                      return [
                        PopupMenuItem(
                            value: 'edit',
                            child: Text(
                                isEditor ? l10n.editDetails : l10n.viewDetails,
                                style: Theme.of(context).textTheme.bodyMedium)),
                        PopupMenuItem(
                          value: 'metadata',
                          child: Text(
                              isEditor ? l10n.editMetadata : l10n.viewMetadata,
                              style: Theme.of(context).textTheme.bodyMedium),
                        ),
                        if (isEditor)
                          PopupMenuItem(
                            value: 'delete',
                            child: Text(l10n.delete,
                                style: Theme.of(context).textTheme.bodyMedium),
                          ),
                      ];
                    },
                  ),
              ]),
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(feature.valueType.toString().split('.').last,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                          fontFamily: 'SourceCodePro',
                          fontSize: 10,
                          letterSpacing: 0.5)),
                  if (isArchived)
                    Padding(
                      padding: const EdgeInsets.only(left: 8.0),
                      child: Tooltip(
                        message: l10n.archivedFeatureTooltip(
                            DateFormat('yyyy-MM-dd HH:mm:ss')
                                .format(feature.whenArchived!)),
                        child: Chip(
                          avatar: const Icon(Icons.archive_outlined,
                              size: 14, color: Colors.red),
                          label: Text(l10n.archivedFeatureBadge),
                          labelStyle: Theme.of(context).textTheme.labelSmall,
                          visualDensity: VisualDensity.compact,
                          materialTapTargetSize:
                              MaterialTapTargetSize.shrinkWrap,
                          padding: EdgeInsets.zero,
                        ),
                      ),
                    ),
                ],
              ),
            ],
          ),
        ),
        ),
      ],
    );
  }
}
