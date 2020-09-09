import 'package:app_singleapp/widgets/features/tabs_bloc.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:mrapi/api.dart';

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
            child: Text('Hidden environments',
                style: Theme.of(context).textTheme.caption),
          ),
          Expanded(
            child: Container(
              child: StreamBuilder<List<String>>(
                  stream: bloc.featureStatusBloc.shownEnvironmentsStream,
                  builder: (context, snapshot) {
                    final shownEnvs = snapshot.hasData ? snapshot.data : [];
                    final hiddenEnvironments = bloc
                        .featureStatus.applicationEnvironments.keys
                        .where((envId) => !shownEnvs.contains(envId))
                        .toList(growable: false);
                    return ListView(
                      scrollDirection: Axis.horizontal,
                      children: [
                        if (snapshot.hasData)
                          ..._sortedEnvironments(bloc, hiddenEnvironments)
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
      TabsBloc bloc, List<String> envIds) {
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
          Text(
            name,
            overflow: TextOverflow.ellipsis,
          ),
          Padding(
            padding: const EdgeInsets.only(left: 2.0, right: 2.0),
            child: InkWell(
              canRequestFocus: false,
              mouseCursor: SystemMouseCursors.click,
              hoverColor: Theme.of(context).primaryColorLight,
              borderRadius: BorderRadius.circular(24),
              child: Container(
                width: 34,
                child: Icon(Icons.visibility,
                    size: 18.0, color: Theme.of(context).primaryColorDark),
              ),
              onTap: () {
                BlocProvider.of<TabsBloc>(context).hideEnvironment(envId);
              },
            ),
          )
        ]),
      );
    }

    return Row(mainAxisSize: MainAxisSize.min, children: [
      Flexible(
        child: Text(
          name,
          overflow: TextOverflow.ellipsis,
        ),
      ),
      Padding(
        padding: const EdgeInsets.only(left: 2.0, right: 2.0),
        child: InkWell(
          canRequestFocus: false,
          mouseCursor: SystemMouseCursors.click,
          hoverColor: Theme.of(context).primaryColorLight,
          borderRadius: BorderRadius.circular(24),
          child: Container(
            width: 34.0,
            child: Icon(Icons.visibility_off,
                size: 18.0, color: Theme.of(context).primaryColorDark),
          ),
          onTap: () {
            BlocProvider.of<TabsBloc>(context).hideEnvironment(envId);
          },
        ),
      )
    ]);
  }
}
