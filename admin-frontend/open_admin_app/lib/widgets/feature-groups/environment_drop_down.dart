import 'dart:async';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/feature-groups/feature_groups_bloc.dart';

class EnvironmentDropDown extends StatefulWidget {
  final FeatureGroupsBloc bloc;
  final String? envId;
  const EnvironmentDropDown({Key? key, required this.bloc, this.envId})
      : super(key: key);

  @override
  State<EnvironmentDropDown> createState() => _EnvironmentDropDownState();
}

class _EnvironmentDropDownState extends State<EnvironmentDropDown> {
  String? _selectedEnvId;
  late StreamSubscription<String?> _envStream;

  @override
  void initState() {
    super.initState();

    final bloc = BlocProvider.of<FeatureGroupsBloc>(context);

    // when application changes, this stream will set appropriate ID or null
    _envStream = bloc.currentEnvironmentStream.listen((env) {
      setState(() {
        _selectedEnvId = env;
      });
    });
  }

  @override
  void dispose() {
    super.dispose();
    _envStream.cancel();
  }

  @override
  Widget build(BuildContext context) {
    return OutlinedButton(
      onPressed: () => {},
      child: Container(
        constraints: const BoxConstraints(maxWidth: 200),
        child: StreamBuilder<List<Environment>>(
            stream: widget.bloc.mrClient.streamValley
                .currentApplicationEnvironmentsStream,
            builder: (context, snapshot) {
              if (snapshot.hasData) {
                return Container(
                  constraints: const BoxConstraints(maxWidth: 300),
                  child: InkWell(
                    mouseCursor: SystemMouseCursors.click,
                    child: DropdownButtonHideUnderline(
                      child: DropdownButton<String>(
                        hint: const Text(
                          'Select environment',
                          textAlign: TextAlign.end,
                        ),
                        icon: const Padding(
                          padding: EdgeInsets.only(left: 8.0),
                          child: Icon(
                            Icons.keyboard_arrow_down,
                            size: 18,
                          ),
                        ),
                        isDense: true,
                        isExpanded: true,
                        items: snapshot.data!.map((Environment environment) {
                          return DropdownMenuItem<String>(
                              value: environment.id,
                              child: Text(
                                environment.name,
                                style: Theme.of(context).textTheme.bodyMedium,
                                overflow: TextOverflow.ellipsis,
                              ));
                        }).toList(),
                        onChanged: (String? value) {
                          setState(() {
                            widget.bloc.currentEnvId = value;
                            widget.bloc.getCurrentFeatureGroups();
                            _selectedEnvId = value;
                          });
                        },
                        value: _selectedEnvId,
                      ),
                    ),
                  ),
                );
              } else {
                return Container();
              }
            }),
      ),
    );
  }
}
