import 'package:app_singleapp/widgets/common/FHFlatButton.dart';
import 'package:app_singleapp/widgets/common/fh_flat_button_transparent.dart';
import 'package:app_singleapp/widgets/features/create-update-feature-dialog-widget.dart';
import 'package:app_singleapp/widgets/features/delete_feature_widget.dart';
import 'package:app_singleapp/widgets/features/feature_dashboard_constants.dart';
import 'package:app_singleapp/widgets/features/feature_status_bloc.dart';
import 'package:app_singleapp/widgets/features/tabs_bloc.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:mrapi/api.dart';
import 'package:speech_bubble/speech_bubble.dart';

class FeatureNamesLeftPanel extends StatelessWidget {
  final TabsBloc tabsBloc;
  final Feature feature;

  const FeatureNamesLeftPanel(
      {Key key, @required this.tabsBloc, @required this.feature})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<FeatureStatusBloc>(context);
    return StreamBuilder<Set<String>>(
        stream: tabsBloc.featureCurrentlyEditingStream,
        builder: (context, snapshot) {
          final amSelected =
              (snapshot.hasData && snapshot.data.contains(feature.key));
          return InkWell(
            canRequestFocus: false,
//            behavior: HitTestBehavior.opaque,
            mouseCursor: SystemMouseCursors.click,
            onTap: () => tabsBloc.hideOrShowFeature(feature),
            child: Container(
                decoration: BoxDecoration(
                    color: Colors.white,
                    border: Border(
                        top: BorderSide(color: Colors.black45, width: 0.5),
                        right: BorderSide(color: Colors.black45, width: 0.5)),
                    boxShadow: [
//                  BoxShadow(
//                      color: Color(0xffe5e7f1),
//                      offset: Offset(15.0, 15),
//                      blurRadius: 16),
                    ]),
                padding: EdgeInsets.only(top: 8.0, left: 8.0),
                height: (amSelected ? selectedRowHeight : unselectedRowHeight) +
                    0.5,
                width: MediaQuery.of(context).size.width > 600 ? 260.0 : 180,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
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
                                mainAxisAlignment: MainAxisAlignment.end,
                                children: [
                                  Expanded(
                                      flex: 1,
                                      child: Icon(
                                        amSelected
                                            ? Icons.keyboard_arrow_down
                                            : Icons.keyboard_arrow_right,
                                        size: 24.0,
                                      )),
                                  Expanded(
                                    flex: 4,
                                    child: Container(
                                      child: Column(
                                        crossAxisAlignment:
                                            CrossAxisAlignment.start,
                                        children: [
                                          Text('${feature.name}',
                                              overflow: TextOverflow.ellipsis,
                                              style: Theme.of(context)
                                                  .textTheme
                                                  .bodyText1),
                                          Text(
                                              '${feature.valueType.toString().split('.').last}',
                                              overflow: TextOverflow.ellipsis,
                                              style: TextStyle(
                                                  fontFamily: 'Source',
                                                  fontSize: 10,
                                                  letterSpacing: 1.0)),
                                        ],
                                      ),
                                    ),
                                  ),
                                  Expanded(
                                    flex: 1,
                                    child: Container(
                                      height: 40,
                                      width: 30,
                                      child: Material(
                                        shape: CircleBorder(),
                                        child: PopupMenuButton(
                                          tooltip: 'Show more',
                                          icon: Icon(Icons.more_vert),
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
                                          },
                                          itemBuilder: (BuildContext context) {
                                            return [
                                              PopupMenuItem(
                                                  value: 'edit',
                                                  child: Text('View details',
                                                      style: Theme.of(context)
                                                          .textTheme
                                                          .bodyText2)),
                                              if (bloc.mrClient
                                                  .userIsFeatureAdminOfCurrentApplication)
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
                                      ),
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
          return Expanded(
            child: ButtonBar(
              alignment: MainAxisAlignment.start,
              children: [
                FHFlatButtonTransparent(
                  title: 'Cancel',
                  keepCase: true,
                  onPressed: () => featureBloc.reset(),
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

        return SizedBox.shrink();
      },
    );
  }
}

class Bubble extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: Alignment.topRight,
      child: SpeechBubble(
        color: Theme.of(context).primaryColorLight,
        nipLocation: NipLocation.RIGHT,
        nipHeight: 12,
        child: Row(mainAxisSize: MainAxisSize.min, children: <Widget>[
          Flexible(
            child: Text(
              '''By default toggle is locked, to enable toggle, please unlock first by clicking "lock" icon.''',
              style: TextStyle(
//                color: Colors.white,
                fontSize: 12.0,
              ),
            ),
          )
        ]),
      ),
    );
  }
}
