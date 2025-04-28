import 'dart:async';

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
  late StreamSubscription<String?> _appIdStream;

  @override
  void initState() {
    super.initState();
    _selectedEnvId = widget.envId;

    // when application changes, this stream will set appropriate ID or null
    _envStream =
        widget.bloc.mrClient.streamValley.currentEnvIdStream.listen((env) {
      if (mounted) {
        setState(() {
          _selectedEnvId = env;
        });
      }
    });

    // Listen to application changes
    _appIdStream =
        widget.bloc.mrClient.streamValley.currentAppIdStream.listen((appId) {
      if (mounted && appId != null) {
        // Reset environment selection when application changes
        setState(() {
          _selectedEnvId = null;
        });
        widget.bloc.currentEnvId = null;
        widget.bloc.mrClient.setCurrentEnvId(null);

        // Force refresh environments for the new application
        widget.bloc.mrClient.streamValley.getCurrentPortfolioApplications();
      }
    });
  }

  @override
  void dispose() {
    _envStream.cancel();
    _appIdStream.cancel();
    super.dispose();
  }

  bool _isValidEnvironmentId(String? envId, List<Environment> environments) {
    if (envId == null) return false;
    return environments.any((env) => env.id == envId);
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
              if (!snapshot.hasData || snapshot.data!.isEmpty) {
                return const Text('No environments available');
              }

              final environments = snapshot.data!;

              // Validate current selection against available environments
              if (!_isValidEnvironmentId(_selectedEnvId, environments)) {
                _selectedEnvId = environments[0].id;
                widget.bloc.currentEnvId = _selectedEnvId;
                widget.bloc.mrClient.setCurrentEnvId(_selectedEnvId);
                widget.bloc.getCurrentFeatureGroups(
                    widget.bloc.currentEnvId, widget.bloc.appId);
              }

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
                      items: environments.map((Environment environment) {
                        return DropdownMenuItem<String>(
                            value: environment.id,
                            child: Text(
                              environment.name,
                              style: Theme.of(context).textTheme.bodyMedium,
                              overflow: TextOverflow.ellipsis,
                            ));
                      }).toList(),
                      onChanged: (String? value) {
                        if (value != null) {
                          setState(() {
                            _selectedEnvId = value;
                          });
                          widget.bloc.currentEnvId = value;
                          widget.bloc.mrClient.setCurrentEnvId(value);
                          widget.bloc.getCurrentFeatureGroups(
                              widget.bloc.currentEnvId, widget.bloc.appId);
                        }
                      },
                      value: _selectedEnvId,
                    ),
                  ),
                ),
              );
            }),
      ),
    );
  }
}
