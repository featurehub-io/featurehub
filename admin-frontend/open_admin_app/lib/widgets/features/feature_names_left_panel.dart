import 'dart:html';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter_icons/flutter_icons.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/features/create_update_feature_dialog_widget.dart';
import 'package:open_admin_app/widgets/features/delete_feature_widget.dart';
import 'package:open_admin_app/widgets/features/feature_dashboard_constants.dart';
import 'package:open_admin_app/widgets/features/set_feature_metadata.dart';
import 'package:open_admin_app/widgets/features/tabs_bloc.dart';

import 'per_application_features_bloc.dart';

class FeatureNamesLeftPanel extends StatelessWidget {
  final FeaturesOnThisTabTrackerBloc tabsBloc;
  final Feature feature;

  const FeatureNamesLeftPanel(
      {Key? key, required this.tabsBloc, required this.feature})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<PerApplicationFeaturesBloc>(context);
    return StreamBuilder<Set<String>?>(
        stream: tabsBloc.featureCurrentlyEditingStream,
        builder: (context, snapshot) {
          final amSelected =
              (snapshot.hasData && snapshot.data!.contains(feature.key));
          return InkWell(
            canRequestFocus: false,
//            behavior: HitTestBehavior.opaque,
            mouseCursor: SystemMouseCursors.click,
            onTap: () => tabsBloc.hideOrShowFeature(feature),
            child: Container(
                decoration: BoxDecoration(
                  border: Border(
                    bottom: BorderSide(
                        color: Theme.of(context)
                            .buttonTheme
                            .colorScheme!
                            .onSurface
                            .withOpacity(0.12),
                        width: 1.0),
                    right: BorderSide(
                        color: Theme.of(context)
                            .buttonTheme
                            .colorScheme!
                            .onSurface
                            .withOpacity(0.12),
                        width: 1.0),
                    left: BorderSide(
                        color: Theme.of(context)
                            .buttonTheme
                            .colorScheme!
                            .onSurface
                            .withOpacity(0.12),
                        width: 1.0),
                  ),
                ),
                padding: const EdgeInsets.only(top: 8.0, left: 8.0),
                height: tabsBloc.featureExtraCellHeight(feature) +
                    (amSelected ? selectedRowHeight : unselectedRowHeight),
                width: MediaQuery.of(context).size.width > 600 ? 260.0 : 130,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisAlignment: MainAxisAlignment.start,
                  children: <Widget>[
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            mainAxisAlignment: MainAxisAlignment.start,
                            children: [
                              Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceBetween,
                                children: [
                                  Expanded(
                                    child: Row(
                                      children: [
                                        Icon(
                                          amSelected
                                              ? Icons.keyboard_arrow_down
                                              : Icons.keyboard_arrow_right,
                                          size: 18.0,
                                        ),
                                        Expanded(
                                          child: Column(
                                            crossAxisAlignment:
                                                CrossAxisAlignment.start,
                                            children: [
                                              Row(children: [
                                                Flexible(
                                                  child: Text(feature.name,
                                                      overflow:
                                                          TextOverflow.ellipsis,
                                                      maxLines: 2,
                                                      // minFontSize: 8.0,
                                                      style: Theme.of(context)
                                                          .textTheme
                                                          .bodyText1!
                                                          .copyWith(
                                                              fontWeight:
                                                                  FontWeight
                                                                      .bold)),
                                                ),
                                                if (feature.link!.isNotEmpty)
                                                  Tooltip(
                                                    message: feature.link!,
                                                    child: IconButton(
                                                      splashRadius: 20,
                                                      icon: const Icon(
                                                          Feather.external_link),
                                                      onPressed: () {
                                                        window.open(feature.link!,
                                                            'new tab');
                                                      },
                                                    ),
                                                  )
                                              ]),
                                              const SizedBox(
                                                height: 4.0,
                                              ),
                                              Text(
                                                  feature.valueType
                                                      .toString()
                                                      .split('.')
                                                      .last,
                                                  overflow:
                                                      TextOverflow.ellipsis,
                                                  style: const TextStyle(
                                                      fontFamily:
                                                          'SourceCodePro',
                                                      fontSize: 10,
                                                      letterSpacing: 1.0)),
                                            ],
                                          ),
                                        ),
                                      ],
                                    ),
                                  ),
                                  Container(
                                    padding: const EdgeInsets.only(right: 4.0),
                                    child: PopupMenuButton(
                                      splashRadius: 20,
                                      tooltip: 'Show more',
                                      icon: const Icon(Icons.more_vert),
                                      onSelected: (value) {
                                        if (value == 'edit') {
                                          tabsBloc.mrClient.addOverlay(
                                              (BuildContext context) =>
                                                  CreateFeatureDialogWidget(
                                                      bloc: bloc,
                                                      feature: feature));
                                        }
                                        if (value == 'delete') {
                                          tabsBloc.mrClient.addOverlay(
                                              (BuildContext context) =>
                                                  FeatureDeleteDialogWidget(
                                                      bloc: bloc,
                                                      feature: feature));
                                        }
                                        if (value == 'metadata') {
                                          bloc.getFeatureIncludingMetadata(
                                              feature);
                                          tabsBloc.mrClient.addOverlay(
                                              (BuildContext context) =>
                                                  SetFeatureMetadataWidget(
                                                    bloc: bloc,
                                                  ));
                                        }
                                      },
                                      itemBuilder: (BuildContext context) {
                                        var isEditor = bloc.mrClient
                                            .userIsFeatureAdminOfCurrentApplication;
                                        return [
                                          PopupMenuItem(
                                              value: 'edit',
                                              child: Text(
                                                  isEditor
                                                      ? 'Edit details'
                                                      : 'View details',
                                                  style: Theme.of(context)
                                                      .textTheme
                                                      .bodyText2)),
                                          PopupMenuItem(
                                            value: 'metadata',
                                            child: Text(
                                                isEditor
                                                    ? 'Edit metadata'
                                                    : 'View metadata',
                                                style: Theme.of(context)
                                                    .textTheme
                                                    .bodyText2),
                                          ),
                                          if (isEditor)
                                            PopupMenuItem(
                                              value: 'delete',
                                              child: Text('Delete',
                                                  style: Theme.of(context)
                                                      .textTheme
                                                      .bodyText2),
                                            ),
                                        ];
                                      },
                                    ),
                                  )
                                ],
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                    if (amSelected)
                      _FeatureListenForUpdatedFeatureValues(
                          feature: feature, bloc: tabsBloc),
                  ],
                )),
          );
        });
  }
}

class _FeatureListenForUpdatedFeatureValues extends StatelessWidget {
  final Feature feature;
  final FeaturesOnThisTabTrackerBloc bloc;

  const _FeatureListenForUpdatedFeatureValues(
      {Key? key, required this.feature, required this.bloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    final featureBloc = bloc.featureValueBlocs[feature.key]!;

    return StreamBuilder<bool>(
      stream: featureBloc.anyDirty,
      builder: (context, snapshot) {
        if (snapshot.data == true) {
          return Flexible(
            child: ButtonBar(
              buttonHeight: 42,
              buttonMinWidth: 70,
              alignment: MainAxisAlignment.start,
              children: [
                FHFlatButtonTransparent(
                  title: 'Cancel',
                  keepCase: true,
                  onPressed: () => bloc.hideOrShowFeature(feature),
                ),
                FHFlatButton(
                  title: 'Save',
                  onPressed: () async {
                    if ((await featureBloc.updateDirtyStates())) {
                      bloc.hideOrShowFeature(feature);
                    }
                  },
                )
              ],
            ),
          );
        }

        return const SizedBox.shrink();
      },
    );
  }
}
