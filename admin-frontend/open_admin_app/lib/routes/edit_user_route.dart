import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widget_creator.dart';
import 'package:open_admin_app/widgets/common/fh_alert_dialog.dart';
import 'package:open_admin_app/widgets/common/fh_card.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/common/fh_footer_button_bar.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/user/common/admin_checkbox.dart';
import 'package:open_admin_app/widgets/user/common/portfolio_group_selector_widget.dart';
import 'package:open_admin_app/widgets/user/edit/edit_user_bloc.dart';

class EditUserRoute extends StatelessWidget {
  const EditUserRoute({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<EditUserBloc>(context);

    return FHCardWidget(
        width: 800,
        child: StreamBuilder(
            stream: bloc.formState,
            builder: (context, snapshot) {
              if (snapshot.data == EditUserForm.initialState) {
                return EditUserFormWidget(person: bloc.person!);
              }
              return Container();
            }));
  }
}

class EditUserFormWidget extends StatefulWidget {
  final Person person;

  const EditUserFormWidget({Key? key, required this.person}) : super(key: key);

  @override
  EditUserFormState createState() => EditUserFormState();
}

class EditUserFormState extends State<EditUserFormWidget> {
  final _formKey = GlobalKey<FormState>();
  final _name = TextEditingController();
  final _email = TextEditingController();

  bool isAddButtonDisabled = true;

  @override
  void initState() {
    super.initState();
    if (_email.text == '') {
      _email.text = widget.person.email!;
    }
    if (_name.text == '') {
      _name.text = widget.person.name!;
    }
  }

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<EditUserBloc>(context);
    final l10n = AppLocalizations.of(context)!;

    return Form(
      key: _formKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: <Widget>[
              FHHeader(
                title: l10n.editUser,
                children: <Widget>[
                  if (bloc.mrClient.identityProviders.hasLocal)
                    FHFlatButtonTransparent(
                      onPressed: () => bloc.mrClient.addOverlay(
                          (BuildContext context) =>
                              UserPasswordUpdateDialogWidget(bloc: bloc)),
                      title: l10n.resetPassword,
                      keepCase: true,
                    ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 8.0),
          Container(
            constraints: const BoxConstraints(maxWidth: 400),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                TextFormField(
                  autofocus: true,
                  onFieldSubmitted: (_) => FocusScope.of(context).nextFocus(),
                  controller: _email,
                  decoration: InputDecoration(
                    border: const OutlineInputBorder(),
                    labelText: l10n.emailLabel,
                  ),
                  validator: (v) =>
                      (v?.isEmpty == true) ? l10n.editEmailAddress : null,
                ),
                Padding(
                  padding: const EdgeInsets.only(top: 30),
                  child: TextFormField(
                    autofocus: true,
                    onFieldSubmitted: (_) => FocusScope.of(context).nextFocus(),
                    controller: _name,
                    decoration: InputDecoration(
                      border: const OutlineInputBorder(),
                      labelText: l10n.nameLabel,
                    ),
                    validator: (v) =>
                        (v?.isEmpty == true) ? l10n.editNames : null,
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.only(top: 30.0),
                  child: Text(
                    l10n.removeOrAddUserToGroup,
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ),
              ],
            ),
          ),
          const PortfolioGroupSelector(),
          if (bloc.mrClient.personState.userIsSuperAdmin)
            AdminCheckboxWidget(person: bloc.person),
          widgetCreator.setBillingAdminCheckbox(),
          FHButtonBar(children: <Widget>[
            FHFlatButtonTransparent(
                onPressed: () {
                  _formKey.currentState!.reset;
                  ManagementRepositoryClientBloc.router
                      .navigateTo(context, '/users');
                },
                title: l10n.cancel,
                keepCase: true),
            Padding(
                padding: const EdgeInsets.only(left: 8.0),
                child: FHFlatButton(
                    onPressed: () async {
                      if (_formKey.currentState!.validate()) {
                        _formKey.currentState!.save();
                        try {
                          await bloc.updatePersonDetails(
                              _email.text, _name.text);
                          bloc.mrClient.addSnackbar(Text(
                              l10n.userUpdated(bloc.person!.name!)));
                          ManagementRepositoryClientBloc.router
                              .navigateTo(context, '/users');
                        } catch (e, s) {
                          bloc.mrClient.dialogError(e, s);
                        }
                      }
                    },
                    title: l10n.saveAndClose,
                    keepCase: true))
          ]),
        ],
      ),
    );
  }
}

class UserPasswordUpdateDialogWidget extends StatefulWidget {
  final EditUserBloc bloc;
  const UserPasswordUpdateDialogWidget({Key? key, required this.bloc})
      : super(key: key);

  @override
  State<StatefulWidget> createState() {
    return _UserPasswordUpdateDialogWidgetState();
  }
}

class _UserPasswordUpdateDialogWidgetState
    extends State<UserPasswordUpdateDialogWidget> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _password = TextEditingController();
  final TextEditingController _passwordConfirm = TextEditingController();

  _UserPasswordUpdateDialogWidgetState();

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Form(
      key: _formKey,
      child: FHAlertDialog(
        title: Text(l10n.resetPassword),
        content: SizedBox(
          width: 500,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              Text(
                l10n.resetPasswordInstructions,
                style: Theme.of(context).textTheme.bodyMedium,
              ),
              TextFormField(
                  controller: _password,
                  decoration: InputDecoration(
                    border: const OutlineInputBorder(),
                    labelText: l10n.newPasswordLabel,
                  ),
                  validator: ((v) {
                    if (v?.isEmpty == true) {
                      return l10n.newPasswordRequired;
                    }
                    return null;
                  })),
              TextFormField(
                  controller: _passwordConfirm,
                  decoration: InputDecoration(
                    border: const OutlineInputBorder(),
                    labelText: l10n.confirmNewPasswordLabel,
                  ),
                  validator: ((v) {
                    if (v?.isEmpty == true) {
                      return l10n.confirmNewPasswordRequired;
                    }
                    if (v != _password.text) {
                      return l10n.passwordsDoNotMatch;
                    }
                    return null;
                  })),
            ],
          ),
        ),
        actions: <Widget>[
          FHFlatButtonTransparent(
            keepCase: true,
            title: l10n.cancel,
            onPressed: () {
              widget.bloc.mrClient.removeOverlay();
            },
          ),
          FHFlatButton(
              title: l10n.save,
              onPressed: (() async {
                if (_formKey.currentState!.validate()) {
                  try {
                    await widget.bloc.resetUserPassword(_password.text);
                    widget.bloc.mrClient.removeOverlay();
                  } catch (e, s) {
                    await widget.bloc.mrClient.dialogError(e, s);
                  }
                }
              }))
        ],
      ),
    );
  }
}
