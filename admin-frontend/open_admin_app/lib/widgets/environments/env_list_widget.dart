import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/apps/manage_app_bloc.dart';
import 'package:open_admin_app/widgets/common/fh_alert_dialog.dart';
import 'package:open_admin_app/widgets/common/fh_delete_thing.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/common/fh_icon_button.dart';
import 'package:open_admin_app/widgets/common/fh_info_card.dart';
import 'package:open_admin_app/widgets/common/fh_reorderable_list_view.dart';
import 'package:openapi_dart_common/openapi.dart';

class EnvListWidget extends StatefulWidget {
  const EnvListWidget({Key? key}) : super(key: key);

  @override
  _EnvListState createState() => _EnvListState();
}

class _EnvListState extends State<EnvListWidget> {
  List<Environment>? _environments;

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ManageAppBloc>(context);

    return StreamBuilder<List<Environment>>(
        stream: bloc.environmentsStream,
        builder: (context, snapshot) {
          if (!snapshot.hasData || snapshot.data == null) {
            return Container();
          }
          //_environments = snapshot.data!.reversed.toList();
          _environments = _sortEnvironments(snapshot.data!);

          return SizedBox(
            //height:(snapshot.data!.length*50).toDouble(),
            height: 500.0,
            child: FHReorderableListView(
              onReorder: (int oldIndex, int newIndex) {
                _reorderEnvironments(oldIndex, newIndex, bloc);
              },
              children: <Widget>[
                for (Environment env in _environments!)
                  _EnvWidget(env: env, bloc: bloc, key: Key(env.id!))
              ],
            ),
          );
        });
  }

  List<Environment> _sortEnvironments(List<Environment> originalList,
      {String parentId = '', List<Environment>? passedSortedList}) {
    final sortedList = passedSortedList ?? <Environment>[];
    for (var env in originalList) {
      if (env.priorEnvironmentId == null && parentId == '') {
        sortedList.insert(0, env);
        _sortEnvironments(originalList,
            parentId: env.id!, passedSortedList: sortedList);
      } else if (env.priorEnvironmentId == parentId) {
        sortedList.add(env);
        _sortEnvironments(originalList,
            parentId: env.id!, passedSortedList: sortedList);
      }
    }
    return sortedList;
  }

  void _reorderEnvironments(
      int oldIndex, int newIndex, ManageAppBloc bloc) async {
    final _environments = this._environments!;

    setState(() {
      // These two lines are workarounds for ReorderableListView problems
      if (newIndex > _environments.length) newIndex = _environments.length;
      if (oldIndex < newIndex) newIndex--;

      final item = _environments[oldIndex];
      _environments.remove(item);
      _environments.insert(newIndex, item);

      // shuffle the previousIds and save the polar bears
      if (newIndex > 0) {
        _environments[newIndex].priorEnvironmentId =
            _environments[newIndex - 1].id;
      }
      if (newIndex < _environments.length - 1) {
        _environments[newIndex + 1].priorEnvironmentId = item.id;
      }
      if (oldIndex < _environments.length - 1 && oldIndex > 0) {
        _environments[oldIndex].priorEnvironmentId =
            _environments[oldIndex - 1].id;
      }
      if (newIndex < oldIndex && oldIndex < _environments.length - 1) {
        _environments[oldIndex + 1].priorEnvironmentId =
            _environments[oldIndex].id;
      }
    });
    _environments[0].priorEnvironmentId =
        null; // first environment should never have a parent
    await bloc.updateEnvs(bloc.applicationId!, _environments);
    bloc.mrClient.addSnackbar(const Text('Environment order updated!'));
  }

  List<Environment> swapPreviousIds(oldPid, newPid) {
    final updatedEnvs = <Environment>[];
    for (var env in _environments!) {
      if (env.priorEnvironmentId == oldPid) {
        env.priorEnvironmentId = newPid;
        updatedEnvs.add(env);
      }
    }
    return updatedEnvs;
  }
}

class _EnvWidget extends StatelessWidget {
  final Environment env;
  final ManageAppBloc bloc;

  const _EnvWidget({Key? key, required this.env, required this.bloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(30, 5, 30, 5),
      decoration: BoxDecoration(
          color: Theme.of(context).cardColor,
          border: Border.all(color: Theme.of(context).dividerColor)),
      child: SizedBox(
        height: 50,
        child: InkWell(
          mouseCursor: SystemMouseCursors.grab,
          child: Row(
            children: <Widget>[
              Container(
                  padding: const EdgeInsets.only(right: 30),
                  child: const Icon(
                    Icons.drag_handle,
                    size: 24.0,
                  )),
              Row(
                children: <Widget>[
                  Text(env.name),
                  Padding(
                      padding: const EdgeInsets.only(left: 8.0),
                      child: (env.production == true)
                          ? _ProductionEnvironmentIndicatorWidget()
                          : _NonProductionEnvironmentIndicatorWidget()),
                ],
              ),
              Expanded(child: Container()),
              bloc.mrClient.isPortfolioOrSuperAdmin(bloc.portfolio?.id)
                  ? _adminFunctions(context)
                  : Container()
            ],
          ),
        ),
      ),
    );
  }

  Widget _adminFunctions(BuildContext context) {
    return Row(children: [
      FHIconButton(
          icon: const Icon(Icons.edit),
          onPressed: () => bloc.mrClient.addOverlay((BuildContext context) =>
              EnvUpdateDialogWidget(bloc: bloc, env: env))),
      FHIconButton(
          icon: const Icon(Icons.delete),
          onPressed: () => bloc.mrClient.addOverlay((BuildContext context) {
                return EnvDeleteDialogWidget(
                  env: env,
                  bloc: bloc,
                );
              })),
    ]);
  }
}

class _ProductionEnvironmentIndicatorWidget extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    const bs = BorderSide(color: Colors.red);
    return Tooltip(
      message: 'Production environment',
      child: Container(
        width: 24.0,
        height: 24.0,
        decoration: const BoxDecoration(
            color: Colors.transparent,
            shape: BoxShape.circle,
            border: Border(bottom: bs, left: bs, right: bs, top: bs)),
        child: const Center(
          child: Padding(
              padding: EdgeInsets.only(left: 2.0),
              child: Text('P',
                  style: TextStyle(color: Colors.red, fontSize: 16.0))),
        ),
      ),
    );
  }
}

class _NonProductionEnvironmentIndicatorWidget extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return const SizedBox(
      width: 24.0,
      height: 24.0,
    );
  }
}

class EnvDeleteDialogWidget extends StatelessWidget {
  final Environment env;
  final ManageAppBloc bloc;

  const EnvDeleteDialogWidget({Key? key, required this.bloc, required this.env})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    // is there only one environment or are we the last one in the chain (no other has us as a prior environment)
    return FHDeleteThingWarningWidget(
      bloc: bloc.mrClient,
      extraWarning: env.production == true,
      wholeWarning: env.production == true
          ? 'The environment `${env.name}` is your production environment, are you sure you wish to remove it?'
          : null,
      thing: env.production == true ? null : "environment '${env.name}'",
      deleteSelected: () async {
        final success = await bloc.deleteEnv(env.id!);
        if (success) {
          bloc.mrClient.addSnackbar(Text("Environment '${env.name}' deleted!"));
        } else {
          bloc.mrClient.customError(
              messageTitle: "Couldn't delete environment ${env.name}");
        }
        return success;
      },
    );
  }
}

class EnvUpdateDialogWidget extends StatefulWidget {
  final Environment? env;
  final ManageAppBloc bloc;

  const EnvUpdateDialogWidget({
    Key? key,
    required this.bloc,
    this.env,
  }) : super(key: key);

  @override
  State<StatefulWidget> createState() {
    return _EnvUpdateDialogWidgetState();
  }
}

class _EnvUpdateDialogWidgetState extends State<EnvUpdateDialogWidget> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _envName = TextEditingController();
  bool isUpdate = false;
  bool _isProduction = false;

  @override
  void initState() {
    super.initState();
    if (widget.env != null) {
      _envName.text = widget.env!.name;
      isUpdate = true;
      _isProduction = widget.env!.production == true;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Form(
      key: _formKey,
      child: FHAlertDialog(
        title: Text(
            widget.env == null ? 'Create new environment' : 'Edit environment'),
        content: SizedBox(
          width: 500,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              TextFormField(
                  controller: _envName,
                  autofocus: true,
                  decoration:
                      const InputDecoration(labelText: 'Environment name'),
                  validator: ((v) {
                    if (v == null || v.isEmpty) {
                      return 'Please enter an environment name';
                    }
                    if (v.length < 2) {
                      return 'Environment name needs to be at least 2 characters long';
                    }
                    return null;
                  })),
              CheckboxListTile(
                title: Text('Mark as production environment',
                    style: Theme.of(context).textTheme.caption),
                value: _isProduction,
                onChanged: (bool? val) {
                  setState(() {
                    _isProduction = val == true;
                  });
                },
              )
            ],
          ),
        ),
        actions: <Widget>[
          FHFlatButtonTransparent(
            title: 'Cancel',
            keepCase: true,
            onPressed: () {
              widget.bloc.mrClient.removeOverlay();
            },
          ),
          FHFlatButton(
              title: isUpdate ? 'Update' : 'Create',
              onPressed: (() async {
                if (_formKey.currentState!.validate()) {
                  try {
                    if (isUpdate) {
                      await widget.bloc.updateEnv(
                          widget.env!..production = _isProduction,
                          _envName.text);
                      widget.bloc.mrClient.removeOverlay();
                      widget.bloc.mrClient.addSnackbar(
                          Text('Environment ${_envName.text} updated!'));
                    } else {
                      await widget.bloc.createEnv(_envName.text, _isProduction);
                      widget.bloc.mrClient.removeOverlay();
                      widget.bloc.mrClient.addSnackbar(
                          Text('Environment ${_envName.text} created!'));
                    }
                  } catch (e, s) {
                    if (e is ApiException && e.code == 409) {
                      widget.bloc.mrClient.customError(
                          messageTitle:
                              'Environment with name ${_envName.text} already exists');
                    } else {
                      await widget.bloc.mrClient.dialogError(e, s);
                    }
                  }
                }
              }))
        ],
      ),
    );
  }
}

Widget addEnvWidget(BuildContext context, ManageAppBloc bloc) {
  return Column(children: <Widget>[
    Container(
        padding: const EdgeInsets.fromLTRB(0, 10, 30, 10),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.start,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            if (bloc.mrClient.isPortfolioOrSuperAdmin(bloc.portfolio?.id))
              TextButton.icon(
                icon: const Icon(Icons.add),
                label: const Text('Create new environment'),
                onPressed: () =>
                    bloc.mrClient.addOverlay((BuildContext context) {
                  return EnvUpdateDialogWidget(
                    bloc: bloc,
                  );
                }),
              ),
            const FHInfoCardWidget(
              message: '''
Ordering your environments,
showing the path to production (top to bottom)
will be reflected on the "Features" dashboard.\n
It helps your teams see their changes
per environment in the correct order.''',
            ),
          ],
        ))
  ]);
}
