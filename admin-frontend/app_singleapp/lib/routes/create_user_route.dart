import 'dart:html' as html;

import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/api/router.dart';
import 'package:app_singleapp/utils/utils.dart';
import 'package:app_singleapp/widgets/common/FHFlatButton.dart';
import 'package:app_singleapp/widgets/common/fh_card.dart';
import 'package:app_singleapp/widgets/common/fh_filled_input_decoration.dart';
import 'package:app_singleapp/widgets/common/fh_flat_button_transparent.dart';
import 'package:app_singleapp/widgets/common/fh_footer_button_bar.dart';
import 'package:app_singleapp/widgets/common/fh_header.dart';
import 'package:app_singleapp/widgets/user/common/admin_checkbox.dart';
import 'package:app_singleapp/widgets/user/common/portfolio_group_selector_widget.dart';
import 'package:app_singleapp/widgets/user/create/create_user_bloc.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:openapi_dart_common/openapi.dart';

class CreateUserRoute extends StatelessWidget {
  final String title;

  CreateUserRoute({Key key, @required this.title})
      : assert(title != null),
        super(key: key);

  @override
  Widget build(BuildContext context) {
    return FHCardWidget(width: 800, child: AddUserFormWidget());
  }
}

class AddUserFormWidget extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.start,
        children: <Widget>[TopWidget(), BottomWidget()],
      ),
    );
  }
}

class TopWidget extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<CreateUserBloc>(context);

    return StreamBuilder<CreateUserForm>(
        stream: bloc.formState,
        builder: (context, AsyncSnapshot<CreateUserForm> snapshot) {
          if (snapshot.hasData &&
              snapshot.data == CreateUserForm.successState) {
            return TopWidgetSuccess();
          }
          return TopWidgetDefault();
        });
  }
}

class TopWidgetDefault extends StatefulWidget {
  @override
  _TopWidgetDefaultState createState() => _TopWidgetDefaultState();
}

class _TopWidgetDefaultState extends State<TopWidgetDefault> {
  final _email = TextEditingController();
  final _name = TextEditingController();
  var selectedPortfolio;
  var selectedGroupID;
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
              FHHeader(title: 'Create new user'),
              Padding(
                padding: const EdgeInsets.only(top: 8.0, bottom: 8.0),
                child: Text(
                  'To create a new user please first provide their name and email address',
                  style: Theme.of(context).textTheme.caption,
                ),
              ),
              Container(
                constraints: BoxConstraints(maxWidth: 300),
                child: Column(
                  children: [
                    TextFormField(
                      controller: _name,
                      decoration: FHFilledInputDecoration(
                        labelText: 'Name',
                      ),
                      validator: (v) {
                        if (v.isEmpty) {
                          return "Please enter user's name";
                        }
                        return null;
                      },
                      onSaved: (v) => bloc.name = v,
                    ),
                    SizedBox(height: 16.0),
                    TextFormField(
                      controller: _email,
                      decoration: FHFilledInputDecoration(
                        labelText: 'Email',
                      ),
                      validator: (v) {
                        if (v.isEmpty) {
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
              PortfolioGroupSelector(),
              AdminCheckboxWidget()
            ]));
  }
}

class TopWidgetSuccess extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<CreateUserBloc>(context);

    return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Text('Registration URL created! \n',
                  style: Theme.of(context).textTheme.headline6),
              Text(bloc.email, style: Theme.of(context).textTheme.bodyText1),
            ],
          ),
          Padding(
            padding: const EdgeInsets.only(top: 20.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text('Registration Url',
                    style: Theme.of(context).textTheme.subtitle2),
                Text(
                  bloc.registrationUrl.registrationUrl,
                  style: Theme.of(context).textTheme.caption,
                ),
              ],
            ),
          ),
          Row(
            children: <Widget>[
              FlatButton(
                onPressed: () async {
                  await html.window.navigator.permissions
                      .query({'name': 'clipboard-write'});
                  await html.window.navigator.clipboard
                      .writeText(bloc.registrationUrl.registrationUrl);
                },
                child: Row(
                  children: <Widget>[
                    Icon(
                      Icons.content_copy,
                      size: 15.0,
                    ),
                    Text(' Copy URL to clipboard',
                        style: Theme.of(context).textTheme.subtitle2.merge(
                            TextStyle(color: Theme.of(context).buttonColor))),
                  ],
                ),
              ),
            ],
          ),
          Text(
            'You will need to email this URL to the new user, so they can complete their registration and set their password.',
            style: Theme.of(context).textTheme.caption,
          ),
          FHButtonBar(children: [
            FHFlatButtonTransparent(
                onPressed: () {
                  bloc.backToDefault();
                  ManagementRepositoryClientBloc.router.navigateTo(
                      context, '/manage-users',
                      transition: TransitionType.material);
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
          return CreateUserFormButtons();
        });
  }
}

class CreateUserFormButtons extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<CreateUserBloc>(context);

    return FHButtonBar(children: <Widget>[
      FHFlatButtonTransparent(
        onPressed: () {
          if (bloc.formKey != null) {
            bloc.formKey.currentState.reset;
          }
          ManagementRepositoryClientBloc.router.navigateTo(
              context, '/manage-users',
              transition: TransitionType.material);
        },
        title: 'Cancel',
        keepCase: true,
      ),
      Padding(
          padding: const EdgeInsets.only(left: 8.0),
          child: FHFlatButton(
              onPressed: () async {
                if (bloc.formKey.currentState.validate()) {
                  bloc.formKey.currentState.save();
                  try {
                    await bloc.createUser(bloc.email);
                  } catch (e, s) {
                    if (e is ApiException && e.code == 409) {
                      bloc.client.dialogError(e, s,
                          messageTitle:
                              "User with email '${bloc.email}' already exists");
                    } else {
                      bloc.client.dialogError(e, s);
                    }
                  }
                }
              },
              title: 'Create'))
    ]);
  }
}
