import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/widgets/common/copy_to_clipboard_html.dart';
import 'package:open_admin_app/widgets/common/fh_card.dart';
import 'package:open_admin_app/widgets/common/fh_filled_input_decoration.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/common/fh_footer_button_bar.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/user/common/portfolio_group_selector_widget.dart';
import 'package:open_admin_app/widgets/user/create/create_user_bloc.dart';
import 'package:openapi_dart_common/openapi.dart';

class CreateAdminApiKeyRoute extends StatelessWidget {
  final String title;

  const CreateAdminApiKeyRoute({Key? key, required this.title})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return const FHCardWidget(width: 800, child: AddUserFormWidget());
  }
}

class AddUserFormWidget extends StatelessWidget {
  const AddUserFormWidget({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisAlignment: MainAxisAlignment.start,
      children: const <Widget>[TopWidget(), BottomWidget()],
    );
  }
}

class TopWidget extends StatelessWidget {
  const TopWidget({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<CreateUserBloc>(context);

    return StreamBuilder<CreateUserForm>(
        stream: bloc.formState,
        builder: (context, AsyncSnapshot<CreateUserForm> snapshot) {
          if (snapshot.hasData &&
              snapshot.data == CreateUserForm.successState) {
            return const TopWidgetSuccess();
          }
          // ignore: prefer_const_constructors
          return TopWidgetDefault();
        });
  }
}

class TopWidgetDefault extends StatefulWidget {
  const TopWidgetDefault({Key? key}) : super(key: key);

  @override
  _TopWidgetDefaultState createState() => _TopWidgetDefaultState();
}

class _TopWidgetDefaultState extends State<TopWidgetDefault> {
  final _name = TextEditingController();
  bool isAddButtonDisabled = true;

  @override
  void didUpdateWidget(TopWidgetDefault oldWidget) {
    super.didUpdateWidget(oldWidget);

    final bloc = BlocProvider.of<CreateUserBloc>(context);
    bloc.formKey = GlobalKey<FormState>();
  }

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<CreateUserBloc>(context);

    return Form(
        key: bloc.formKey,
        child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              const FHHeader(title: 'Create new Admin API Key'),
              Container(
                constraints: const BoxConstraints(maxWidth: 300),
                child: Column(
                  children: [
                    const SizedBox(height: 16.0),
                    TextFormField(
                      controller: _name,
                      decoration: fhFilledInputDecoration(
                        labelText: 'Name',
                      ),
                      validator: (v) {
                        if (v?.isEmpty == true) {
                          return 'Please provide a name for the Admin API Key';
                        }
                        return null;
                      },
                      onSaved: (v) => bloc.name = v,
                    ),
                  ],
                ),
              ),
              Padding(
                padding: const EdgeInsets.only(top: 30.0),
                child: Text(
                  'Assign to some portfolio groups or leave it blank to add them later',
                  style: Theme.of(context).textTheme.caption,
                ),
              ),
              const PortfolioGroupSelector(),
            ]));
  }
}

class TopWidgetSuccess extends StatelessWidget {
  const TopWidgetSuccess({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<CreateUserBloc>(context);
    final hasLocal =
        bloc.client.identityProviders.hasLocal && bloc.registrationUrl != null;

    return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Text('Admin API Key created! \n',
                  style: Theme.of(context).textTheme.headline6),
              Text(bloc.name ?? '',
                  style: Theme.of(context).textTheme.bodyText1),
            ],
          ),
          if (hasLocal)
            Padding(
              padding: const EdgeInsets.only(top: 20.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Text('Admin API Key',
                      style: Theme.of(context).textTheme.subtitle2),
                  Text(
                    bloc.registrationUrl!.token,
                    style: Theme.of(context).textTheme.caption,
                  ),
                ],
              ),
            ),
          if (hasLocal)
            Row(
              children: <Widget>[
                FHCopyToClipboardFlatButton(
                  text: bloc.registrationUrl!.token,
                  caption: ' Copy Admin API key to clipboard',
                ),
              ],
            ),
          FHButtonBar(children: [
            FHFlatButtonTransparent(
                onPressed: () {
                  bloc.backToDefault();
                  ManagementRepositoryClientBloc.router
                      .navigateTo(context, '/admin-api-keys');
                },
                title: 'Close'),
            FHFlatButton(
                onPressed: () {
                  bloc.backToDefault();
                },
                title: 'Create another API Key',
                keepCase: true),
          ])
        ]);
  }
}

class BottomWidget extends StatelessWidget {
  const BottomWidget({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<CreateUserBloc>(context);

    return StreamBuilder<CreateUserForm>(
        stream: bloc.formState,
        builder: (context, AsyncSnapshot<CreateUserForm> snapshot) {
          if (snapshot.hasData &&
              snapshot.data == CreateUserForm.successState) {
            return Container();
          }
          return const CreateUserFormButtons();
        });
  }
}

class CreateUserFormButtons extends StatelessWidget {
  const CreateUserFormButtons({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<CreateUserBloc>(context);

    return FHButtonBar(children: <Widget>[
      FHFlatButtonTransparent(
        onPressed: () {
          if (bloc.formKey != null) {
            bloc.formKey!.currentState!.reset;
          }
          ManagementRepositoryClientBloc.router
              .navigateTo(context, '/admin-api-keys');
        },
        title: 'Cancel',
        keepCase: true,
      ),
      Padding(
          padding: const EdgeInsets.only(left: 8.0),
          child: FHFlatButton(
              onPressed: () async {
                if (bloc.formKey!.currentState!.validate()) {
                  bloc.formKey!.currentState!.save();
                  try {
                    await bloc.createAdminApiServiceAccount(bloc.name!);
                  } catch (e, s) {
                    if (e is ApiException && e.code == 409) {
                      await bloc.client.dialogError(e, s,
                          messageTitle:
                              "API Key with name '${bloc.name}' already exists"); // will this ever happen?
                    } else {
                      await bloc.client.dialogError(e, s);
                    }
                  }
                }
              },
              title: 'Create'))
    ]);
  }
}
