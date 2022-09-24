import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/common/stream_valley.dart';
import 'package:open_admin_app/widgets/common/fh_alert_dialog.dart';
import 'package:open_admin_app/widgets/common/fh_delete_thing.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/common/fh_icon_button.dart';
import 'package:open_admin_app/widgets/common/fh_loading_error.dart';
import 'package:open_admin_app/widgets/common/fh_loading_indicator.dart';
import 'package:open_admin_app/widgets/service-accounts/apikey_reset_dialog_widget.dart';
import 'package:openapi_dart_common/openapi.dart';

import 'manage_service_accounts_bloc.dart';

class ServiceAccountsListWidget extends StatelessWidget {
  const ServiceAccountsListWidget({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ManageServiceAccountsBloc>(context);
    final mrBloc = BlocProvider.of<ManagementRepositoryClientBloc>(context);

    return StreamBuilder<List<ServiceAccount>>(
        stream: bloc.serviceAccountsList,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const FHLoadingIndicator();
          } else if (snapshot.connectionState == ConnectionState.active ||
              snapshot.connectionState == ConnectionState.done) {
            if (snapshot.hasError) {
              return const FHLoadingError();
            } else if (snapshot.hasData) {
              return Column(
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  for (ServiceAccount sa in snapshot.data!)
                    _ServiceAccountWidget(
                      serviceAccount: sa,
                      mr: mrBloc,
                      bloc: bloc,
                    )
                ],
              );
            }
          }
          return const SizedBox.shrink();
        });
  }
}

class _ServiceAccountWidget extends StatelessWidget {
  final ServiceAccount serviceAccount;
  final ManagementRepositoryClientBloc mr;
  final ManageServiceAccountsBloc bloc;

  const _ServiceAccountWidget(
      {Key? key,
      required this.serviceAccount,
      required this.mr,
      required this.bloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {

    return Card(
      margin: const EdgeInsets.all(8.0),
      child: Padding(
        padding: const EdgeInsets.all(8.0),
        child: Flexible(
          fit: FlexFit.loose,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.end,
                mainAxisAlignment: MainAxisAlignment.start,
                children: <Widget>[
                  const SizedBox(width: 4.0),
                  _ServiceAccountDescription(serviceAccount: serviceAccount),
                  const SizedBox(width: 24.0),
                  StreamBuilder<ReleasedPortfolio?>(
                      stream: bloc.mrClient.streamValley.currentPortfolioStream,
                      builder: (context, snapshot) {
                        if (snapshot.hasData &&
                            snapshot.data!.currentPortfolioOrSuperAdmin) {
                          return _adminFunctions(context);
                        } else {
                          return Container();
                        }
                      }),
                ],
              ),
              const SizedBox(height: 8.0),
              ServiceAccountEnvironments(
                  serviceAccount: serviceAccount, serviceAccountBloc: bloc)
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
              ServiceAccountUpdateDialogWidget(
                  bloc: bloc, serviceAccount: serviceAccount))),
      FHIconButton(
          icon: const Icon(Icons.delete),
          onPressed: () => bloc.mrClient.addOverlay((BuildContext context) {
                return ServiceAccountDeleteDialogWidget(
                  serviceAccount: serviceAccount,
                  bloc: bloc,
                );
              })),
      _ResetApiKeyWidget(
        bloc: bloc,
        isClientKey: true,
        sa: serviceAccount,
      ),
      _ResetApiKeyWidget(
        bloc: bloc,
        isClientKey: false,
        sa: serviceAccount,
      ),
    ]);
  }
}

class ServiceAccountEnvironments extends StatelessWidget {
  final ServiceAccount serviceAccount;
  final ManageServiceAccountsBloc serviceAccountBloc;

  const ServiceAccountEnvironments(
      {Key? key,
      required this.serviceAccount,
      required this.serviceAccountBloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<Portfolio>(
        stream: serviceAccountBloc.applicationsList,
        builder: (context, snapshot) {
          if (!snapshot.hasData || snapshot.hasError) {
            return const SizedBox.shrink();
          }

          return Wrap(
            direction: Axis.horizontal,
            crossAxisAlignment: WrapCrossAlignment.start,
            children: snapshot.data!.applications
                .map((app) => _ServiceAccountEnvironment(
                      serviceAccount: serviceAccount,
                      application: app,
                    ))
                .toList(),
          );
        });
  }
}

class _ServiceAccountEnvironment extends StatelessWidget {
  final ServiceAccount serviceAccount;
  final Application application;

  const _ServiceAccountEnvironment(
      {Key? key, required this.serviceAccount, required this.application})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    final appEnvs = application.environments.map((e) => e.id).toList();
    final permEnvs =
        serviceAccount.permissions.map((e) => e.environmentId).toList();
    // are any app env ids in the perm envs?
    final found = appEnvs.any((appEnvId) => permEnvs.contains(appEnvId));
    return Card(
      color: Theme.of(context).backgroundColor,
      elevation: 4.0,
      child: SizedBox(
        width: 240,
        height: 130,
        child: Padding(
          padding: const EdgeInsets.all(8.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              SelectableText(application.name),
              Text(
                  found
                      ? 'This service account has permissions to one or more environments in this application.'
                      : 'This service account has no permissions to any environments in this application.',
                  style: Theme.of(context).textTheme.caption),
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  FHFlatButtonTransparent(
                    keepCase: true,
                    title: found ? 'Change access' : 'Add access',
                    onPressed: () {
                      BlocProvider.of<ManagementRepositoryClientBloc>(context)
                          .currentAid = application.id;

                      ManagementRepositoryClientBloc.router
                          .navigateTo(context, '/app-settings', params: {
                        'service-account': [serviceAccount.id!],
                        'tab': ['service-accounts']
                      });
                    },
                  )
                ],
              )
            ],
          ),
        ),
      ),
    );
  }
}

class _ServiceAccountDescription extends StatelessWidget {
  const _ServiceAccountDescription({
    Key? key,
    required this.serviceAccount,
  }) : super(key: key);

  final ServiceAccount serviceAccount;

  @override
  Widget build(BuildContext context) {
    var light = Theme.of(context).brightness == Brightness.light;
    return SelectionArea(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(serviceAccount.name,
              style: Theme.of(context).textTheme.subtitle1!.copyWith(
                  color: light
                      ? Theme.of(context).primaryColor
                      : Theme.of(context).colorScheme.secondary)),
          Text(
            serviceAccount.description ?? '',
            style: Theme.of(context).textTheme.caption,
          ),
        ],
      ),
    );
  }
}

class ServiceAccountDeleteDialogWidget extends StatelessWidget {
  final ServiceAccount serviceAccount;
  final ManageServiceAccountsBloc bloc;

  const ServiceAccountDeleteDialogWidget(
      {Key? key, required this.bloc, required this.serviceAccount})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return FHDeleteThingWarningWidget(
      thing: "service account '${serviceAccount.name}'",
      content:
          'All applications using this service account will no longer have access to features! \n\nThis cannot be undone!',
      bloc: bloc.mrClient,
      deleteSelected: () async {
        final success = await bloc
            .deleteServiceAccount(serviceAccount.id!)
            .catchError((e, s) {
          bloc.mrClient.dialogError(e, s,
              messageTitle:
                  "Couldn't delete service account ${serviceAccount.name}");
        });
        if (success) {
          bloc.mrClient.removeOverlay();
          bloc.mrClient.addSnackbar(
              Text("Service account '${serviceAccount.name}' deleted!"));
        }
        return success;
      },
    );
  }
}

class ServiceAccountUpdateDialogWidget extends StatefulWidget {
  final ServiceAccount? serviceAccount;
  final ManageServiceAccountsBloc bloc;

  const ServiceAccountUpdateDialogWidget({
    Key? key,
    required this.bloc,
    this.serviceAccount,
  }) : super(key: key);

  @override
  State<StatefulWidget> createState() {
    return _ServiceAccountUpdateDialogWidgetState();
  }
}

class _ServiceAccountUpdateDialogWidgetState
    extends State<ServiceAccountUpdateDialogWidget> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _name = TextEditingController();
  final TextEditingController _description = TextEditingController();
  bool isUpdate = false;

  @override
  void initState() {
    super.initState();
    if (widget.serviceAccount != null) {
      _name.text = widget.serviceAccount!.name;
      _description.text = widget.serviceAccount!.description!;
      isUpdate = true;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Form(
      key: _formKey,
      child: FHAlertDialog(
        title: Text(widget.serviceAccount == null
            ? 'Create new service account'
            : 'Edit service account'),
        content: SizedBox(
          width: 500,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              TextFormField(
                      controller: _name,
                      autofocus: true,
                      decoration: const InputDecoration(
                          labelText: 'Service account name'),
                      validator: ((v) {
                        if (v == null || v.isEmpty) {
                          return 'Please enter a service account name';
                        }
                        if (v.length < 4) {
                          return 'Service account name needs to be at least 4 characters long';
                        }
                        return null;
                      })),
              TextFormField(
                  controller: _description,
                  decoration: const InputDecoration(
                      labelText: 'Service account description'),
                  validator: ((v) {
                    if (v == null || v.isEmpty) {
                      return 'Please enter service account description';
                    }
                    if (v.length < 4) {
                      return 'Service account description needs to be at least 4 characters long';
                    }
                    return null;
                  })),
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
                      await widget.bloc.updateServiceAccount(
                          widget.serviceAccount!,
                          _name.text,
                          _description.text);
                      widget.bloc.mrClient.removeOverlay();
                      widget.bloc.mrClient.addSnackbar(
                          Text("Service account '${_name.text}' updated!"));
                    } else {
                      await widget.bloc
                          .createServiceAccount(_name.text, _description.text);
                      widget.bloc.mrClient.removeOverlay();
                      widget.bloc.mrClient.addSnackbar(
                          Text("Service account '${_name.text}' created!"));
                    }
                  } catch (e, s) {
                    if (e is ApiException && e.code == 409) {
                      widget.bloc.mrClient.customError(
                          messageTitle:
                              "Service account '${_name.text}' already exists");
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

class _ResetApiKeyWidget extends StatelessWidget {
  final ServiceAccount sa;
  final ManageServiceAccountsBloc bloc;
  final bool isClientKey;

  const _ResetApiKeyWidget({
    Key? key,
    required this.sa,
    required this.bloc,
    required this.isClientKey,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return FHIconButton(
      icon: const Icon(
        Icons.refresh,
        color: Colors.red,
      ),
      tooltip: isClientKey
          ? "Reset client eval API keys"
          : "Reset server eval API keys",
      onPressed: () => bloc.mrClient.addOverlay((BuildContext context) {
        return ApiKeyResetDialogWidget(
          sa: sa,
          bloc: bloc,
          isClientKey: isClientKey,
        );
      }),
      // child: Text(text),
      // style: TextButton.styleFrom(primary: Colors.red),
    );
  }
}
