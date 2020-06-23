import 'package:app_singleapp/widgets/features/tabs_bloc.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

class HiddenEnvironmentsList extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<TabsBloc>(context);
    return Container(
      height: 40.0,
      child: Row(
        children: [
          Padding(
            padding: const EdgeInsets.all(8.0),
            child: Text('Hidden environments',
                style: Theme.of(context).textTheme.caption),
          ),
          Flexible(
            child: Container(
              height: 40.0,
              child: StreamBuilder<Set<String>>(
                  stream: bloc.hiddenEnvironments,
                  builder: (context, snapshot) {
                    return ListView(
                      scrollDirection: Axis.horizontal,
                      children: [
                        if (snapshot.hasData)
                          ..._sortedEnvironments(bloc, snapshot.data)
                              .map((env) {
                            return HideEnvironmentContainer(
                                rowLayout: true,
                                envId: env.environmentId,
                                name: env.environmentName);
                          }).toList(),
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
      TabsBloc bloc, Set<String> envIds) {
    final envs = bloc.featureStatus.applicationFeatureValues.environments
        .where((e) => envIds.contains(e.environmentId))
        .toList();
    envs.sort((e1, e2) => e1.environmentName.compareTo(e2.environmentName));
    return envs;
  }
}

class HideEnvironmentContainer extends StatelessWidget {
  final String name;
  final String envId;
  final bool rowLayout;

  const HideEnvironmentContainer(
      {Key key, this.name, this.envId, bool rowLayout})
      : rowLayout = rowLayout ?? false,
        super(key: key);

  @override
  Widget build(BuildContext context) {
    if (rowLayout) {
      return Padding(
        padding: const EdgeInsets.only(right: 8.0),
        child: Row(children: [
          Text(name),
          Padding(
            padding: const EdgeInsets.only(left: 2.0, right: 2.0),
            child: InkWell(
              child: Icon(Icons.visibility, size: 20.0),
              onTap: () {
                BlocProvider.of<TabsBloc>(context).hideEnvironment(envId);
              },
            ),
          )
        ]),
      );
    }

    return Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Text(name),
          Padding(
            padding: const EdgeInsets.only(left: 2.0, right: 2.0),
            child: InkWell(
              child: Icon(Icons.visibility_off, size: 20.0),
              onTap: () {
                BlocProvider.of<TabsBloc>(context).hideEnvironment(envId);
              },
            ),
          )
        ]);
  }
}
