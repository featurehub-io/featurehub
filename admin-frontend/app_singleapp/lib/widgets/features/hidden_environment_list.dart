import 'package:app_singleapp/widgets/features/tabs_bloc.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:mrapi/api.dart';

import 'feature_status_bloc.dart';

class HiddenEnvironmentsList extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<TabsBloc>(context);
    return Container(
      color: Theme.of(context).selectedRowColor,
      margin: EdgeInsets.all(24.0),
      height: 40,
      child: Row(
        children: [
          Padding(
            padding: const EdgeInsets.all(8.0),
            child: Text('Choose environments',
                style: Theme.of(context).textTheme.caption),
          ),
          Expanded(
            child: Container(
              child: StreamBuilder<FeatureStatusFeatures>(
                  stream: bloc.featureStatusBloc.appFeatureValues,
                  builder: (context, snapshot) {
                    return ListView(
                      scrollDirection: Axis.horizontal,
                      children: [
                        if (snapshot.hasData)
                          ...snapshot.data.sortedByNameEnvironmentIds
                              .map((e) => HideEnvironmentContainer(
                                  envId: e,
                                  efv:
                                      snapshot.data.applicationEnvironments[e]))
                              .toList()
                      ],
                    );
                  }),
            ),
          ),
        ],
      ),
    );
  }

  List<EnvironmentFeatureValues> _sortedEnvironments(
      TabsBloc bloc, List<String> envIds) {
    final envs =
        bloc.featureStatus.applicationFeatureValues.environments.toList();
    envs.sort((e1, e2) => e1.environmentName.compareTo(e2.environmentName));
    return envs;
  }
}

class HideEnvironmentContainer extends StatefulWidget {
  final String envId;
  final EnvironmentFeatureValues efv;

  const HideEnvironmentContainer({Key key, this.envId, this.efv})
      : super(key: key);

  @override
  _HideEnvironmentContainerState createState() =>
      _HideEnvironmentContainerState();
}

class _HideEnvironmentContainerState extends State<HideEnvironmentContainer> {
  bool visible;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(right: 8.0),
      child: Row(children: [
        ChoiceChip(
            label: Text(
              widget.efv.environmentName,
              overflow: TextOverflow.ellipsis,
            ),
            selected: visible,
            onSelected: (bool newValue) {
              final bloc = BlocProvider.of<FeatureStatusBloc>(context);

              if (newValue) {
                bloc.removeShownEnvironment(widget.efv.environmentId);
              } else {
                bloc.addShownEnvironment(widget.efv.environmentId);
              }

              setState(() {
                visible = newValue;
              });
            }),
      ]),
    );
  }

  @override
  void didChangeDependencies() {
    visible = !BlocProvider.of<FeatureStatusBloc>(context)
        .environmentVisible(widget.efv.environmentId);
  }
}
