import 'package:app_singleapp/widgets/features/tabs_bloc.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:mrapi/api.dart';

import 'per_application_features_bloc.dart';

class HiddenEnvironmentsList extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<FeaturesOnThisTabTrackerBloc>(context);
    return Container(
      margin: EdgeInsets.only(top: 24.0, bottom: 24.0, right: 24.0),
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
}

class HideEnvironmentContainer extends StatefulWidget {
  final String envId;
  final EnvironmentFeatureValues efv;

  const HideEnvironmentContainer({Key? key, this.envId, this.efv})
      : super(key: key);

  @override
  _HideEnvironmentContainerState createState() =>
      _HideEnvironmentContainerState();
}

class _HideEnvironmentContainerState extends State<HideEnvironmentContainer> {
  bool selected;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(right: 8.0),
      child: Row(children: [
        ChoiceChip(
            label: Text(
              widget.efv.environmentName.toUpperCase(),
              style: Theme.of(context).textTheme.overline,
              overflow: TextOverflow.ellipsis,
            ),
            selectedColor: Theme.of(context).primaryColorLight,
            selected: selected,
            onSelected: (bool newValue) {
              final bloc = BlocProvider.of<PerApplicationFeaturesBloc>(context);

              if (newValue) {
                bloc.addShownEnvironment(widget.efv.environmentId);
              } else {
                bloc.removeShownEnvironment(widget.efv.environmentId);
              }

              setState(() {
                selected = newValue;
              });
            }),
      ]),
    );
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    selected = BlocProvider.of<PerApplicationFeaturesBloc>(context)
        .environmentVisible(widget.efv.environmentId);
  }
}
