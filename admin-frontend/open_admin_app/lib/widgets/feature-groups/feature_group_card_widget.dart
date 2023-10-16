import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/feature-groups/feature-group-settings-side-sheet.dart';
import 'package:open_admin_app/widgets/feature-groups/feature-groups-bloc.dart';
import 'package:side_sheet/side_sheet.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'feature_group_bloc.dart';
import 'feature_group_delete_dialog_widget.dart';
import 'feature_group_update_dialog_widget.dart';

class FeatureGroupCard extends StatelessWidget {
  final FeatureGroupListGroup featureGroup;
  final FeatureGroupsBloc bloc;

  const FeatureGroupCard({
    Key? key,
    required this.featureGroup,
    required this.bloc,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(8.0),
      child: Card(
        elevation: 4.0,
        child: InkWell(
          mouseCursor: SystemMouseCursors.click,
          borderRadius: BorderRadius.circular(8.0),
          onTap: () {
            _openFeatureGroupEditSideSheet(bloc, featureGroup, context);
          },
          child: SizedBox(
            width: 240,
            height: 170,
            child: Column(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Padding(
                  padding:
                      const EdgeInsets.only(left: 16.0, bottom: 8, top: 8.0),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    crossAxisAlignment: CrossAxisAlignment.center,
                    children: [
                      Container(
                        constraints: const BoxConstraints(maxWidth: 150),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          mainAxisAlignment: MainAxisAlignment.start,
                          children: [
                            Text(featureGroup.name,
                                maxLines: 2,
                                style: Theme.of(context).textTheme.bodyLarge),
                            const SizedBox(height: 4.0),
                            if (featureGroup.description != null)
                              Text(featureGroup.description,
                                  maxLines: 2,
                                  overflow: TextOverflow.ellipsis,
                                  style: Theme.of(context)
                                      .textTheme
                                      .bodySmall
                                      ?.copyWith(
                                        color: Theme.of(context)
                                            .textTheme
                                            .bodySmall
                                            ?.color
                                            ?.withOpacity(0.5),
                                      )),
                          ],
                        ),
                      ),
                      StreamBuilder<List<RoleType>>(
                          stream: bloc.envRoleTypeStream,
                          builder: (context, snapshot) {
                            if (snapshot.data != null &&
                                (snapshot.data!
                                        .contains(RoleType.CHANGE_VALUE) ==
                                    true)) {
                              return _PopUpGroupAdminMenu(
                                bloc: bloc,
                                featureGroup: featureGroup,
                              );
                            } else {
                              return const SizedBox();
                            }
                          })
                    ],
                  ),
                ),
                Padding(
                  padding:
                      const EdgeInsets.only(left: 16.0, bottom: 8, right: 16.0),
                  child: Column(
                    children: [
                      const Divider(),
                      SizedBox(height: 4.0),
                      FeaturesCounter(
                        counter: featureGroup.features.length,
                        featureGroup: featureGroup,
                      )
                    ],
                  ),
                )
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _PopUpGroupAdminMenu extends StatelessWidget {
  final FeatureGroupsBloc bloc;
  final FeatureGroupListGroup featureGroup;
  const _PopUpGroupAdminMenu(
      {Key? key, required this.bloc, required this.featureGroup})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return PopupMenuButton(
      splashRadius: 20,
      tooltip: 'Show more',
      icon: const Icon(
        Icons.more_vert,
        size: 22.0,
      ),
      onSelected: (value) {
        if (value == 'edit') {
          bloc.mrClient.addOverlay(
              (BuildContext context) => FeatureGroupUpdateDialogWidget(
                    bloc: bloc,
                    featureGroup: featureGroup,
                  ));
        }
        if (value == 'delete') {
          bloc.mrClient.addOverlay((BuildContext context) {
            return FeatureGroupDeleteDialogWidget(
              bloc: bloc,
              featureGroup: featureGroup,
            );
          });
        }
        if (value == 'manage') {
          _openFeatureGroupEditSideSheet(bloc, featureGroup, context);
        }
      },
      itemBuilder: (BuildContext context) {
        var items = <PopupMenuItem>[
          PopupMenuItem(
              value: 'manage',
              child: Text('Manage Group',
                  style: Theme.of(context).textTheme.bodyMedium)),
          PopupMenuItem(
              value: 'edit',
              child:
                  Text('Edit', style: Theme.of(context).textTheme.bodyMedium)),
          PopupMenuItem(
              value: 'delete',
              child: Text('Delete',
                  style: Theme.of(context).textTheme.bodyMedium)),
        ];
        return items;
      },
    );
  }
}

_openFeatureGroupEditSideSheet(FeatureGroupsBloc bloc,
    FeatureGroupListGroup featureGroup, BuildContext context) {
  SideSheet.right(
      body: BlocProvider.builder(
        creator: (c, b) => FeatureGroupBloc(bloc, featureGroup),
        builder: (c, b) => FeatureGroupSettings(
          bloc: b,
          featureGroup: featureGroup,
        )),
      width: MediaQuery.of(context).size.width * 0.8,
      context: context);
}

class FeaturesCounter extends StatelessWidget {
  final int counter;
  final FeatureGroupListGroup featureGroup;
  const FeaturesCounter(
      {Key? key, required this.counter, required this.featureGroup})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    var iconSize = 28.0;
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Stack(
              alignment: Alignment.center,
              children: [
                Container(
                  width: iconSize + 12.0, // Adjust the size as needed
                  height: iconSize + 12.0, // Adjust the size as needed
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color:
                        Theme.of(context).colorScheme.primary.withOpacity(0.2),
                  ),
                ),
                Icon(
                  Icons.outlined_flag_rounded,
                  color: Theme.of(context).colorScheme.primary,
                  size: iconSize, // Customize the icon color
                ),
              ],
            ),
            const SizedBox(width: 8.0),
            Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(
                "Features",
                style: Theme.of(context).textTheme.labelSmall?.copyWith(
                    color: Theme.of(context)
                        .textTheme
                        .labelSmall
                        ?.color
                        ?.withOpacity(0.6)),
              ),
              Text(counter.toString(),
                  style: Theme.of(context).textTheme.titleLarge)
            ]),
          ],
        ),
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              "Strategy",
              style: Theme.of(context).textTheme.labelSmall?.copyWith(
                  color: Theme.of(context)
                      .textTheme
                      .labelSmall
                      ?.color
                      ?.withOpacity(0.6)),
            ),
            const SizedBox(
              height: 6.0,
            ),
            (featureGroup.features.isNotEmpty && featureGroup.hasStrategy)
                ? Row(children: [
                    Icon(Icons.check_circle_outline_rounded,
                        color: Colors.greenAccent.shade700, size: 18.0),
                    const SizedBox(
                      width: 4.0,
                    ),
                    Text("Active",
                        style: Theme.of(context)
                            .textTheme
                            .labelMedium
                            ?.copyWith(color: Colors.greenAccent.shade700))
                  ])
                : const Text("Not set"),
          ],
        )
      ],
    );
  }
}
