import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/tabs_bloc.dart';

import 'per_application_features_bloc.dart';

class HiddenEnvironmentsList extends StatelessWidget {
  const HiddenEnvironmentsList({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<PerApplicationFeaturesBloc>(context);
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
            child: StreamBuilder<EnvironmentsInfo>(
                stream: bloc.environmentsStream,
                builder: (context, snapshot) {
                  return ListView(
                    scrollDirection: Axis.horizontal,
                    children: [
                      if (snapshot.hasData)
                        ...snapshot.data!.environments
                            .map((e) => HideEnvironmentContainer(
                                environment: e))
                            .toList()
                    ],
                  );
                }),
          ),
        ],
      ),
    );
  }
}

class HideEnvironmentContainer extends StatefulWidget {
  final Environment environment;

  const HideEnvironmentContainer(
      {Key? key, required this.environment})
      : super(key: key);

  @override
  _HideEnvironmentContainerState createState() =>
      _HideEnvironmentContainerState();
}

class _HideEnvironmentContainerState extends State<HideEnvironmentContainer> {
  bool selected = false;

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
