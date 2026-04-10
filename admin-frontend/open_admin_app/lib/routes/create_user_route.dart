import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/utils/utils.dart';
import 'package:open_admin_app/widgets/common/copy_to_clipboard_html.dart';
import 'package:open_admin_app/widgets/common/fh_card.dart';
import 'package:open_admin_app/widgets/common/fh_filled_input_decoration.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/common/fh_footer_button_bar.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/common/fh_loading_error.dart';
import 'package:open_admin_app/widgets/common/fh_loading_indicator.dart';
import 'package:open_admin_app/widgets/user/common/admin_checkbox.dart';
import 'package:open_admin_app/widgets/user/common/portfolio_group_selector_widget.dart';
import 'package:open_admin_app/widgets/user/create/create_user_bloc.dart';
import 'package:openapi_dart_common/openapi.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';

class CreateUserRoute extends StatelessWidget {
  final String title;

  const CreateUserRoute({super.key, required this.title});

  @override
  Widget build(BuildContext context) {
    return const FHCardWidget(width: 800, child: AddUserFormWidget());
  }
}

class AddUserFormWidget extends StatelessWidget {
  const AddUserFormWidget({super.key});

  @override
  Widget build(BuildContext context) {
    return const Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisAlignment: MainAxisAlignment.start,
      children: <Widget>[TopWidget(), BottomWidget()],
    );
  }
}

class TopWidget extends StatelessWidget {
  const TopWidget({super.key});

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<CreateUserBloc>(context);

    return StreamBuilder<CreateUserForm>(
        stream: bloc.formState,
        builder: (context, AsyncSnapshot<CreateUserForm> snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting ||
              snapshot.data == CreateUserForm.loadingState) {
            return const FHLoadingIndicator();
          } else if (snapshot.connectionState == ConnectionState.active ||
              snapshot.connectionState == ConnectionState.done) {
            if (snapshot.hasError) {
              return const FHLoadingError();
            } else if (snapshot.hasData) {
              if (snapshot.data == CreateUserForm.successState) {
                return const TopWidgetSuccess();
              }
              // ignore: prefer_const_constructors
              return const TopWidgetDefault();
            }
          }
          return const SizedBox.shrink();
        });
  }
}

class TopWidgetDefault extends StatefulWidget {
  const TopWidgetDefault({super.key});

  @override
  TopWidgetDefaultState createState() => TopWidgetDefaultState();
}

class TopWidgetDefaultState extends State<TopWidgetDefault> {
  final _email = TextEditingController();
  bool isAddButtonDisabled = true;

  @override
  void initState() {
    super.initState();
    final bloc = BlocProvider.of<CreateUserBloc>(context);
    bloc.formKey = GlobalKey<FormState>();
  }

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<CreateUserBloc>(context);
    final l10n = AppLocalizations.of(context)!;

    return Form(
        key: bloc.formKey,
        child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              FHHeader(title: l10n.createNewUser),
              Padding(
                padding: const EdgeInsets.only(top: 8.0, bottom: 8.0),
                child: Text(
                  l10n.createUserInstructions,
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ),
              Container(
                constraints: const BoxConstraints(maxWidth: 300),
                child: Column(
                  children: [
                    const SizedBox(height: 16.0),
                    TextFormField(
                      autofocus: true,
                      onFieldSubmitted: (_) =>
                          FocusScope.of(context).nextFocus(),
                      controller: _email,
                      decoration: fhFilledInputDecoration(
                        labelText: l10n.emailLabel,
                      ),
                      validator: (v) {
                        if (v?.isEmpty == true) {
                          return l10n.emailRequired;
                        }
                        if (!validateEmail(v)) {
                          return l10n.invalidEmailAddress;
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
                  l10n.addUserToGroupsHint,
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ),
              const PortfolioGroupSelector(),
              const AdminCheckboxWidget()
            ]));
  }
}

class TopWidgetSuccess extends StatelessWidget {
  const TopWidgetSuccess({super.key});

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<CreateUserBloc>(context);
    final l10n = AppLocalizations.of(context)!;
    final hasLocal =
        bloc.client.identityProviders.hasLocal && bloc.registrationUrl != null;

    return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Text(l10n.userCreated,
                  style: Theme.of(context).textTheme.titleLarge),
              Text(bloc.email ?? '',
                  style: Theme.of(context).textTheme.bodyLarge),
            ],
          ),
          if (hasLocal)
            Padding(
              padding: const EdgeInsets.only(top: 20.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Text(l10n.registrationUrl,
                      style: Theme.of(context).textTheme.titleSmall),
                  Text(
                    bloc.client.registrationUrl(bloc.registrationUrl!.token),
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ],
              ),
            ),
          if (hasLocal)
            Row(
              children: <Widget>[
                FHCopyToClipboardFlatButton(
                  text:
                      bloc.client.registrationUrl(bloc.registrationUrl!.token),
                  caption: l10n.copyUrlToClipboard,
                ),
              ],
            ),
          if (hasLocal)
            Text(
              l10n.sendRegistrationUrlInstructions,
              style: Theme.of(context).textTheme.bodySmall,
            ),
          if (!hasLocal)
            Text(
              l10n.userCanSignIn,
              style: Theme.of(context).textTheme.bodySmall,
            ),
          FHButtonBar(children: [
            FHFlatButtonTransparent(
                onPressed: () {
                  bloc.backToDefault();
                  ManagementRepositoryClientBloc.router
                      .navigateTo(context, '/users');
                },
                title: l10n.close,
                keepCase: true),
            FHFlatButton(
                onPressed: () {
                  bloc.backToDefault();
                },
                title: l10n.createAnotherUser,
                keepCase: true),
          ])
        ]);
  }
}

class BottomWidget extends StatelessWidget {
  const BottomWidget({super.key});

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
  const CreateUserFormButtons({super.key});

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<CreateUserBloc>(context);

    final l10n = AppLocalizations.of(context)!;
    return FHButtonBar(children: <Widget>[
      FHFlatButtonTransparent(
        onPressed: () {
          if (bloc.formKey != null) {
            bloc.formKey!.currentState!.reset;
          }
          ManagementRepositoryClientBloc.router.navigateTo(context, '/users');
        },
        title: l10n.cancel,
        keepCase: true,
      ),
      Padding(
          padding: const EdgeInsets.only(left: 8.0),
          child: FHFlatButton(
              onPressed: () async {
                if (bloc.formKey!.currentState!.validate()) {
                  bloc.formKey!.currentState!.save();
                  try {
                    await bloc.createUser(bloc.email!, null);
                  } catch (e, s) {
                    bloc.backToDefault();
                    if (e is ApiException && e.code == 409) {
                      await bloc.client.dialogError(e, s,
                          messageTitle:
                              l10n.userEmailAlreadyExists(bloc.email!),
                          showDetails: false);
                    } else {
                      await bloc.client.dialogError(e, s);
                    }
                  }
                }
              },
              title: l10n.create))
    ]);
  }
}
