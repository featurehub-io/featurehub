import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/utils/utils.dart';
import 'package:open_admin_app/widgets/common/FHFlatButton.dart';
import 'package:open_admin_app/widgets/common/copy_to_clipboard_html.dart';
import 'package:open_admin_app/widgets/common/fh_card.dart';
import 'package:open_admin_app/widgets/common/fh_filled_input_decoration.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/common/fh_footer_button_bar.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/user/common/admin_checkbox.dart';
import 'package:open_admin_app/widgets/user/common/portfolio_group_selector_widget.dart';
import 'package:open_admin_app/widgets/user/create/create_user_bloc.dart';
import 'package:openapi_dart_common/openapi.dart';

class CreateUserRoute extends StatelessWidget {
  final String title;

  const CreateUserRoute({Key? key, required this.title}) : super(key: key);

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
  final _email = TextEditingController();
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
              const FHHeader(title: 'Create new user'),
              Padding(
                padding: const EdgeInsets.only(top: 8.0, bottom: 8.0),
                child: Text(
                  'To create a new user please first provide their name and email address',
                  style: Theme.of(context).textTheme.caption,
                ),
              ),
              Container(
                constraints: const BoxConstraints(maxWidth: 300),
                child: Column(
                  children: [
                    TextFormField(
                      controller: _name,
                      decoration: FHFilledInputDecoration(
                        labelText: 'Name',
                      ),
                      validator: (v) {
                        if (v?.isEmpty == true) {
                          return "Please enter user's name";
                        }
                        return null;
                      },
                      onSaved: (v) => bloc.name = v,
                    ),
                    const SizedBox(height: 16.0),
                    TextFormField(
                      controller: _email,
                      decoration: FHFilledInputDecoration(
                        labelText: 'Email',
                      ),
                      validator: (v) {
                        if (v?.isEmpty == true) {
                          return 'Please enter email address';
                        }
                        if (!validateEmail(v)) {
                          return 'Please enter a valid email address';
                        }
                        return null;
                      },
                      onSaved: (v) => bloc.email = v,
                    ),
                  ],
                ),
              ),
              Padding(
                padding: const EdgeInsets.only(top: 30.0),
                child: Text(
                  'Add user to some portfolio groups or leave it blank to add them later',
                  style: Theme.of(context).textTheme.caption,
                ),
              ),
              const PortfolioGroupSelector(),
              const AdminCheckboxWidget()
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
              Text('User created! \n',
                  style: Theme.of(context).textTheme.headline6),
              Text(bloc.email ?? '',
                  style: Theme.of(context).textTheme.bodyText1),
            ],
          ),
          if (hasLocal)
            Padding(
              padding: const EdgeInsets.only(top: 20.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Text('Registration Url',
                      style: Theme.of(context).textTheme.subtitle2),
                  Text(
                    bloc.registrationUrl!.registrationUrl,
                    style: Theme.of(context).textTheme.caption,
                  ),
                ],
              ),
            ),
          if (hasLocal)
            Row(
              children: <Widget>[
                FHCopyToClipboardFlatButton(
                  text: bloc.registrationUrl!.registrationUrl,
                  caption: ' Copy URL to clipboard',
                ),
              ],
            ),
          if (hasLocal)
            Text(
              'You will need to email this URL to the new user, so they can complete their registration and set their password.',
              style: Theme.of(context).textTheme.caption,
            ),
          if (!hasLocal)
            Text(
              'The user now needs to sign in with your external identity provider and they will be able to access the system.',
              style: Theme.of(context).textTheme.caption,
            ),
          FHButtonBar(children: [
            FHFlatButtonTransparent(
                onPressed: () {
                  bloc.backToDefault();
                  ManagementRepositoryClientBloc.router
                      .navigateTo(context, '/users');
                },
                title: 'Close'),
            FHFlatButton(
                onPressed: () {
                  bloc.backToDefault();
                },
                title: 'Create another user',
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
          ManagementRepositoryClientBloc.router.navigateTo(context, '/users');
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
                    await bloc.createUser(bloc.email!);
                  } catch (e, s) {
                    if (e is ApiException && e.code == 409) {
                      await bloc.client.dialogError(e, s,
                          messageTitle:
                              "User with email '${bloc.email}' already exists");
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
