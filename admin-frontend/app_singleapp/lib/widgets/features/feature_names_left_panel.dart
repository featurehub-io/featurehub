import 'package:app_singleapp/widgets/common/fh_flat_button_transparent.dart';
import 'package:app_singleapp/widgets/features/feature_dashboard_constants.dart';
import 'package:app_singleapp/widgets/features/feature_value_row_generic.dart';
import 'package:app_singleapp/widgets/features/tabs_bloc.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

class FeatureNamesLeftPanel extends StatelessWidget {
  final TabsBloc tabsBloc;
  final Feature feature;

  const FeatureNamesLeftPanel(
      {Key key, @required this.tabsBloc, @required this.feature})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<Set<String>>(
        stream: tabsBloc.featureCurrentlyEditingStream,
        builder: (context, snapshot) {
          final amSelected =
              (snapshot.hasData && snapshot.data.contains(feature.key));
          return GestureDetector(
            behavior: HitTestBehavior.opaque,
            onTap: () => tabsBloc.hideOrShowFeature(feature),
            child: Container(
                padding: EdgeInsets.only(top: 8.0, left: 8.0),
//              color: Theme.of(context).backgroundColor,
//                padding: EdgeInsets.fromLTRB(0, 8, 0, 8),
                height: amSelected ? selectedRowHeight : unselectedRowHeight,
                width: 220.0,
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.start,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Padding(
                      //top padding above feature name
                      padding: const EdgeInsets.only(right: 2.0, top: 4.0),
                      child: Icon(
                        amSelected
                            ? Icons.keyboard_arrow_down
                            : Icons.keyboard_arrow_right,
                        size: 24.0,
                      ),
                    ),
                    Expanded(
                      child: Container(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: <Widget>[
                            Row(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      Row(
                                        mainAxisAlignment:
                                            MainAxisAlignment.spaceBetween,
                                        children: [
                                          Text('${feature.name}',
                                              overflow: TextOverflow.ellipsis,
                                              style: Theme.of(context)
                                                  .textTheme
                                                  .bodyText1),
                                          Container(
                                            height: 24,
                                            child: PopupMenuButton(
                                              icon: Icon(Icons.more_vert),
                                              onSelected: (value) {
                                                if (value == 'edit') {}
                                              },
                                              itemBuilder:
                                                  (BuildContext context) {
                                                return [
                                                  PopupMenuItem(
                                                      value: 'edit',
                                                      child: Text('Edit',
                                                          style:
                                                              Theme.of(context)
                                                                  .textTheme
                                                                  .bodyText2)),
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
                                      Text(
                                          '${feature.valueType.toString().split('.').last}',
                                          overflow: TextOverflow.ellipsis,
                                          style: TextStyle(
                                              fontFamily: 'Source',
                                              fontSize: 10)),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                            if (amSelected)
                              _FeatureListenForUpdatedFeatureValues(
                                  feature: feature, bloc: tabsBloc),
                            if (amSelected)
                              FeatureValueNameCell(feature: feature),
                            if (amSelected)
                              FeatureEditDeleteCell(feature: feature)
                          ],
                        ),
                      ),
                    ),
                  ],
                )),
          );
        });
  }
}

class _FeatureListenForUpdatedFeatureValues extends StatelessWidget {
  final Feature feature;
  final TabsBloc bloc;

  const _FeatureListenForUpdatedFeatureValues(
      {Key key, this.feature, this.bloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    final featureBloc = bloc.featureValueBlocs[feature.key];

    return StreamBuilder<bool>(
      stream: featureBloc.anyDirty,
      builder: (context, snapshot) {
        if (snapshot.data == true) {
          return Row(
            children: [
              FHFlatButtonTransparent(
                title: 'Reset',
                onPressed: () => featureBloc.reset(),
              ),
              FHFlatButtonTransparent(
                title: 'Save',
                onPressed: () => featureBloc.updateDirtyStates(),
              )
            ],
          );
        }

        return SizedBox.shrink();
      },
    );

    // TODO: implement build
    throw UnimplementedError();
  }
}
