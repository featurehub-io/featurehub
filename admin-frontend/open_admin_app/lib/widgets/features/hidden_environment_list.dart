import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

import 'per_application_features_bloc.dart';

class HiddenEnvironmentsList extends StatelessWidget {
  const HiddenEnvironmentsList({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<PerApplicationFeaturesBloc>(context);
    return StreamBuilder<EnvironmentsInfo>(
        stream: bloc.environmentsStream,
        builder: (context, snapshot) {
          if (!snapshot.hasData || snapshot.data!.noApplications) {
            return const SizedBox.shrink();
          }

          return Container(
            margin: const EdgeInsets.only(top: 24.0, bottom: 24.0, right: 24.0),
            height: 40,
            child: Row(
              children: [
                Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: Text('Choose environments',
                      style: Theme.of(context).textTheme.caption),
                ),
                Expanded(
                  child: ListView(
                    scrollDirection: Axis.horizontal,
                    children: [
                      if (snapshot.hasData)
                        ...snapshot.data!.environments
                            .map((e) => HideEnvironmentContainer(
                                environment: e,
                                environmentInfo: snapshot.data!))
                            .toList()
                    ],
                  ),
                ),
              ],
            ),
          );
        });
  }
}

class HideEnvironmentContainer extends StatefulWidget {
  final Environment environment;
  final EnvironmentsInfo environmentInfo;

  const HideEnvironmentContainer(
      {Key? key, required this.environment, required this.environmentInfo})
      : super(key: key);

  @override
  _HideEnvironmentContainerState createState() =>
      _HideEnvironmentContainerState();
}

class _HideEnvironmentContainerState extends State<HideEnvironmentContainer> {
  bool selected = false;

  @override
  void initState() {
    super.initState();

    selected = widget.environmentInfo.isShown(widget.environment.id!);
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(right: 8.0),
      child: Row(children: [
        ChoiceChip(
            label: Text(
              widget.environment.name.toUpperCase(),
              style: Theme.of(context).textTheme.overline,
              overflow: TextOverflow.ellipsis,
            ),
            selectedColor: Theme.of(context).primaryColorLight,
            selected: selected,
            onSelected: (bool newValue) {
              final bloc = BlocProvider.of<PerApplicationFeaturesBloc>(context);

              if (newValue) {
                bloc.addShownEnvironment(widget.environment.id!);
              } else {
                bloc.removeShownEnvironment(widget.environment.id!);
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
        .environmentVisible(widget.environment.id!);
  }
}
