import 'dart:html';


import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_icons/flutter_icons.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/common/copy_to_clipboard_html.dart';

import 'package:open_admin_app/widgets/features/create_update_feature_dialog_widget.dart';
import 'package:open_admin_app/widgets/features/delete_feature_widget.dart';
import 'package:open_admin_app/widgets/features/per_application_features_bloc.dart';
import 'package:open_admin_app/widgets/features/set_feature_metadata.dart';


class FeatureCellHolder extends StatelessWidget {

  final Feature feature;

  const FeatureCellHolder({Key? key, required this.feature}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<PerApplicationFeaturesBloc>(context);
    return  Row(
      mainAxisAlignment:
      MainAxisAlignment.spaceBetween,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(left:8.0, top: 8.0),
          child: Column(
            crossAxisAlignment:
            CrossAxisAlignment.start,
            children: [
              Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,

                  children: [
                SelectionArea(
                  child: SizedBox(
                    width: 150, // given full column width is 200
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
                ),
                PopupMenuButton(
                  splashRadius: 20,
                  tooltip: 'Show more',
                  icon: const Icon(Icons.more_vert),
                  onSelected: (value) {
                    if (value == 'edit') {
                      bloc.mrClient.addOverlay(
                              (BuildContext context) =>
                              CreateFeatureDialogWidget(
                                  bloc: bloc,
                                  feature: feature));
                    }
                    if (value == 'delete') {
                      bloc.mrClient.addOverlay(
                              (BuildContext context) =>
                              FeatureDeleteDialogWidget(
                                  bloc: bloc,
                                  feature: feature));
                    }
                    if (value == 'metadata') {
                      bloc.getFeatureIncludingMetadata(
                          feature);
                      bloc.mrClient.addOverlay(
                              (BuildContext context) =>
                              SetFeatureMetadataWidget(
                                bloc: bloc,
                              ));
                    }
                  },
                  itemBuilder: (BuildContext context) {
                    var isEditor = bloc.mrClient
                        .userHasFeatureEditRoleInCurrentApplication;
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
              ]),
              const SizedBox(
                height: 4.0,
              ),

              Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,

                children: [
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
                          letterSpacing: 0.5
                      )),
                  Row(
                    children: [
                      FHCopyToClipboard(tooltipMessage: "Copy feature key to clipboard", copyString: feature.key!),
                      if (feature.link?.isNotEmpty == true)
                        IconButton(
                          tooltip: feature.link!,
                          splashRadius: 20,
                          icon: const Icon(
                              Feather.external_link),
                          onPressed: () {
                            window.open(feature.link!,
                                'new tab');
                          },
                        ),

                    ],
                  ),

                ],
              ),
            ],
          ),
        ),
      ],
    );
  }
}
